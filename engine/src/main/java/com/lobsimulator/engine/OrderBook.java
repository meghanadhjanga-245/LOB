package com.lobsimulator.engine;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core limit order book maintaining bid and ask price levels with price-time priority.
 * Optimised with fastutil primitive collections to minimise GC pressure.
 */
public class OrderBook {
    private final Long2ObjectRBTreeMap<PriceLevel> bidLevels = new Long2ObjectRBTreeMap<>((p1, p2) -> Long.compare(p2, p1));
    private final Long2ObjectRBTreeMap<PriceLevel> askLevels = new Long2ObjectRBTreeMap<>();
    private final HashMap<String, Order> orderMap = new HashMap<>();
    private final AtomicLong orderIdGenerator = new AtomicLong(1);
    private long totalBidVolume = 0;
    private long totalAskVolume = 0;

    // ─────────────────── Snapshot / Restore ───────────────────

    public void clear() {
        this.bidLevels.clear();
        this.askLevels.clear();
        this.orderMap.clear();
        this.totalBidVolume = 0;
        this.totalAskVolume = 0;
    }

    public void restore(OrderBook snapshot) {
        if (snapshot == null) return;
        this.bidLevels.clear();
        this.askLevels.clear();
        this.orderMap.clear();
        this.orderIdGenerator.set(snapshot.orderIdGenerator.get());
        this.bidLevels.putAll(snapshot.bidLevels);
        this.askLevels.putAll(snapshot.askLevels);
        this.orderMap.putAll(snapshot.orderMap);
        this.updateTotalVolumes();
    }

    public OrderBook snapshot() {
        OrderBook copy = new OrderBook();
        copy.orderIdGenerator.set(this.orderIdGenerator.get());
        for (Long2ObjectMap.Entry<PriceLevel> entry : bidLevels.long2ObjectEntrySet()) {
            long price = entry.getLongKey();
            PriceLevel level = entry.getValue();
            PriceLevel targetLevel = new PriceLevel(price);
            copy.bidLevels.put(price, targetLevel);
            for (Order cur = level.getFirstOrder(); cur != null; cur = cur.getNext()) {
                Order copied = new Order(cur.getOrderId(), cur.isBuySide(), cur.getPrice(), cur.getQuantity(), cur.getTimestamp(), cur.getType());
                copied.setHiddenQuantity(cur.getHiddenQuantity());
                // FIX: pass actual peakSize (was 0) so iceberg orders refill correctly after restore
                copied.setIceberg(cur.isIceberg(), cur.getPeakSize());
                targetLevel.addOrder(copied);
                copy.orderMap.put(copied.getOrderId(), copied);
            }
        }
        for (Long2ObjectMap.Entry<PriceLevel> entry : askLevels.long2ObjectEntrySet()) {
            long price = entry.getLongKey();
            PriceLevel level = entry.getValue();
            PriceLevel targetLevel = new PriceLevel(price);
            copy.askLevels.put(price, targetLevel);
            for (Order cur = level.getFirstOrder(); cur != null; cur = cur.getNext()) {
                Order copied = new Order(cur.getOrderId(), cur.isBuySide(), cur.getPrice(), cur.getQuantity(), cur.getTimestamp(), cur.getType());
                copied.setHiddenQuantity(cur.getHiddenQuantity());
                // FIX: pass actual peakSize (was 0)
                copied.setIceberg(cur.isIceberg(), cur.getPeakSize());
                targetLevel.addOrder(copied);
                copy.orderMap.put(copied.getOrderId(), copied);
            }
        }
        copy.updateTotalVolumes();
        return copy;
    }

    private void updateTotalVolumes() {
        totalBidVolume = 0;
        for (PriceLevel l : bidLevels.values()) totalBidVolume += l.getTotalQuantity();
        totalAskVolume = 0;
        for (PriceLevel l : askLevels.values()) totalAskVolume += l.getTotalQuantity();
    }

    // ─────────────────── CRUD Operations ───────────────────

    public String addOrder(boolean buySide, long price, int quantity, boolean isMarket) {
        if (isMarket) throw new IllegalArgumentException("Market orders cannot be added to the book");
        return addOrder(buySide, price, quantity);
    }

    public String addOrder(boolean buySide, long price, int quantity) {
        String orderId = "O" + orderIdGenerator.getAndIncrement();
        Order order = new Order(orderId, buySide, price, quantity, System.nanoTime(), OrderType.LIMIT);
        addOrder(order);
        return orderId;
    }

    public String addOrder(Order order) {
        if (order == null) throw new IllegalArgumentException("Order cannot be null");
        if (order.getQuantity() <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (order.isMarket()) throw new IllegalArgumentException("Market orders cannot be added to the book");
        if (wouldCross(order.isBuySide(), order.getPrice())) throw new IllegalArgumentException("Order would cross the book");

        String orderId = order.getOrderId();
        if (orderId == null || orderId.trim().isEmpty()) {
            orderId = "O" + orderIdGenerator.getAndIncrement();
            order = new Order(orderId, order.isBuySide(), order.getPrice(), order.getQuantity(), order.getTimestamp(), order.getType());
        }

        orderMap.put(orderId, order);
        long price = order.getPrice();
        if (order.isBuySide()) {
            PriceLevel level = bidLevels.get(price);
            if (level == null) {
                level = new PriceLevel(price);
                bidLevels.put(price, level);
            }
            level.addOrder(order);
            totalBidVolume += order.getQuantity();
        } else {
            PriceLevel level = askLevels.get(price);
            if (level == null) {
                level = new PriceLevel(price);
                askLevels.put(price, level);
            }
            level.addOrder(order);
            totalAskVolume += order.getQuantity();
        }
        return orderId;
    }

    public boolean cancelOrder(String orderId) {
        Order order = orderMap.get(orderId);
        if (order == null) return false;
        long qty = order.getQuantity();
        removeOrderFromLevel(order);
        orderMap.remove(orderId);
        if (order.isBuySide()) totalBidVolume -= qty;
        else totalAskVolume -= qty;
        return true;
    }

    public boolean reduceOrder(String orderId, int delta) {
        Order order = orderMap.get(orderId);
        if (order == null) return false;
        int newQty = order.getQuantity() - delta;
        if (newQty < 0) throw new IllegalArgumentException("Reduction results in negative qty");
        if (newQty == 0) return cancelOrder(orderId);
        order.setQuantity(newQty);
        PriceLevel level = (order.isBuySide() ? bidLevels : askLevels).get(order.getPrice());
        if (level != null) level.updateOrderQuantity(order, -delta);
        if (order.isBuySide()) totalBidVolume -= delta;
        else totalAskVolume -= delta;
        return true;
    }

    /**
     * Changes the price of an order in-place, preserving its ID.
     * FIX: cross-check is done BEFORE removing the order to prevent silent order loss.
     * The old implementation removed the order first, then checked for crossing — if
     * the new price was invalid the order was already gone with no way to recover it.
     */
    public boolean changeOrderPricePreserveId(String orderId, long newPrice) {
        Order order = orderMap.get(orderId);
        if (order == null) return false;
        boolean buySide = order.isBuySide();
        // FIX: check BEFORE removing
        if (wouldCross(buySide, newPrice)) return false;
        removeOrderFromLevel(order);
        order.setPrice(newPrice);
        if (buySide) {
            PriceLevel level = bidLevels.get(newPrice);
            if (level == null) {
                level = new PriceLevel(newPrice);
                bidLevels.put(newPrice, level);
            }
            level.addOrder(order);
        } else {
            PriceLevel level = askLevels.get(newPrice);
            if (level == null) {
                level = new PriceLevel(newPrice);
                askLevels.put(newPrice, level);
            }
            level.addOrder(order);
        }
        return true;
    }

    public String changeOrderPrice(String orderId, long newPrice) {
        Order order = orderMap.get(orderId);
        if (order == null) return null;
        if (wouldCross(order.isBuySide(), newPrice)) return null;
        boolean buySide = order.isBuySide();
        int qty = order.getQuantity();
        boolean isIceberg = order.isIceberg();
        int peakSize = order.getPeakSize();
        int hiddenQty = order.getHiddenQuantity();
        cancelOrder(orderId);
        String newId = addOrder(buySide, newPrice, qty);
        if (isIceberg) {
            Order newOrder = orderMap.get(newId);
            if (newOrder != null) {
                newOrder.setHiddenQuantity(hiddenQty);
                newOrder.setIceberg(true, peakSize);
            }
        }
        return newId;
    }

    // ─────────────────── Market Data ───────────────────

    public long[] getBestBid() {
        if (bidLevels.isEmpty()) return null;
        for (Map.Entry<Long, PriceLevel> entry : bidLevels.entrySet()) {
            if (entry != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                return new long[]{entry.getKey(), entry.getValue().getTotalQuantity()};
            }
        }
        return null;
    }

    public long[] getBestAsk() {
        if (askLevels.isEmpty()) return null;
        for (Map.Entry<Long, PriceLevel> entry : askLevels.entrySet()) {
            if (entry != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                return new long[]{entry.getKey(), entry.getValue().getTotalQuantity()};
            }
        }
        return null;
    }

    public boolean isCrossed() {
        long[] bestBid = getBestBid();
        long[] bestAsk = getBestAsk();
        if (bestBid == null || bestAsk == null) return false;
        return bestBid[0] >= bestAsk[0];
    }

    public boolean wouldCross(boolean buySide, long price) {
        if (buySide) return !askLevels.isEmpty() && price >= askLevels.firstKey();
        else return !bidLevels.isEmpty() && price <= bidLevels.firstKey();
    }

    public int getTotalQuantity() {
        return (int)(totalBidVolume + totalAskVolume);
    }

    public int getTotalQuantityAtOrBetter(boolean buySide, long price) {
        int total = 0;
        if (buySide) {
            for (PriceLevel level : askLevels.values()) {
                if (level.getPrice() > price) break;
                total += level.getTotalQuantity();
            }
        } else {
            for (PriceLevel level : bidLevels.values()) {
                if (level.getPrice() < price) break;
                total += level.getTotalQuantity();
            }
        }
        return total;
    }

    /**
     * Returns the first (oldest) resting order at the given price level.
     * Used by MatchingEngine to walk the price queue during order matching.
     */
    public Order getFirstOrder(boolean buySide, long price) {
        PriceLevel level = (buySide ? bidLevels : askLevels).get(price);
        return level != null ? level.getFirstOrder() : null;
    }

    /**
     * Updates fee accumulators on a resting order after a fill.
     *
     * @param orderId        resting order to update
     * @param makerFeeRebate maker fee delta (negative = rebate)
     * @param takerFee       taker fee delta
     */
    public void updateOrderFees(String orderId, long makerFeeRebate, long takerFee) {
        Order order = orderMap.get(orderId);
        if (order == null) return;
        if (makerFeeRebate != 0) order.addMakerFeeRebate(makerFeeRebate);
        if (takerFee != 0) order.addTakerFeePaid(takerFee);
    }

    /**
     * Validates all internal book invariants. Called after every operation in tests.
     * Invariants checked:
     * <ol>
     *   <li>Book is not crossed (best bid < best ask)</li>
     *   <li>No empty price levels remain in the maps</li>
     *   <li>Cached volume totals equal actual sum of all level quantities</li>
     * </ol>
     *
     * @return true if all invariants hold
     */
    public boolean checkInvariants() {
        if (isCrossed()) return false;
        for (PriceLevel l : bidLevels.values()) if (l.isEmpty()) return false;
        for (PriceLevel l : askLevels.values()) if (l.isEmpty()) return false;
        long calcBidVol = 0;
        for (PriceLevel l : bidLevels.values()) calcBidVol += l.getTotalQuantity();
        if (calcBidVol != totalBidVolume) return false;
        long calcAskVol = 0;
        for (PriceLevel l : askLevels.values()) calcAskVol += l.getTotalQuantity();
        return calcAskVol == totalAskVolume;
    }

    /** Returns up to {@code depth} bid levels sorted best-price-first (highest bid first). */
    public long[][] getBidLevels(int depth) {
        java.util.List<long[]> list = new java.util.ArrayList<>();
        for (Map.Entry<Long, PriceLevel> e : bidLevels.entrySet()) {
            if (list.size() >= depth) break;
            if (e != null && e.getValue() != null && !e.getValue().isEmpty()) {
                list.add(new long[]{e.getKey(), e.getValue().getTotalQuantity()});
            }
        }
        return list.toArray(new long[0][]);
    }

    /** Returns up to {@code depth} ask levels sorted best-price-first (lowest ask first). */
    public long[][] getAskLevels(int depth) {
        java.util.List<long[]> list = new java.util.ArrayList<>();
        for (Map.Entry<Long, PriceLevel> e : askLevels.entrySet()) {
            if (list.size() >= depth) break;
            if (e != null && e.getValue() != null && !e.getValue().isEmpty()) {
                list.add(new long[]{e.getKey(), e.getValue().getTotalQuantity()});
            }
        }
        return list.toArray(new long[0][]);
    }

    // ─────────────────── Private Helpers ───────────────────

    private void removeOrderFromLevel(Order order) {
        // FIX: was typed as TreeMap<Long,PriceLevel> which doesn't compile with Long2ObjectRBTreeMap
        Long2ObjectRBTreeMap<PriceLevel> levels = order.isBuySide() ? bidLevels : askLevels;
        PriceLevel level = levels.get(order.getPrice());
        if (level != null) {
            level.removeOrder(order);
            if (level.isEmpty()) levels.remove(order.getPrice());
        }
    }
}
