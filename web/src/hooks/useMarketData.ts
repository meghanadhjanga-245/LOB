import { useState, useEffect, useRef, useCallback } from 'react';

export interface OrderBookLevel {
  price: number;
  quantity: number;
}

export interface Trade {
  id: string;
  price: number;
  quantity: number;
  side: 'BUY' | 'SELL';
  timestamp: number;
  slippage?: number;
  vwap?: number;
  makerFee?: number;
  takerFee?: number;
}

export interface MarketState {
  bids: OrderBookLevel[];
  asks: OrderBookLevel[];
  lastTrade: Trade | null;
  spread: number;
  connected: boolean;
  error: string | null;
  latencyMs: number;
  isSimulating: boolean;
  toggleSimulation: () => void;
  seedLiquidity: () => Promise<void>;
  clearBook: () => Promise<void>;
}

const API_BASE = import.meta.env.VITE_API_URL || '';
const POLL_INTERVAL_MS = 400;

export const useMarketData = (): MarketState => {
  const [bids, setBids] = useState<OrderBookLevel[]>([]);
  const [asks, setAsks] = useState<OrderBookLevel[]>([]);
  const [lastTrade, setLastTrade] = useState<Trade | null>(null);
  const [spread, setSpread] = useState<number>(0);
  const [connected, setConnected] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [latencyMs, setLatencyMs] = useState<number>(12);
  const [isSimulating, setIsSimulating] = useState<boolean>(false);

  const failCountRef = useRef(0);
  const hasAutoSeededRef = useRef(false);
  const simIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // ── Seed liquidity helper ───────────────────────────────────────────────
  const seedLiquidity = useCallback(async () => {
    const baseMid = 10000; // $100.00 in ticks
    const orders: Promise<Response>[] = [];

    // Place 12 bids below mid
    for (let i = 1; i <= 12; i++) {
      const price = baseMid - i * 25; // 99.75, 99.50, 99.25...
      const quantity = Math.floor(Math.random() * 80) + 20;
      orders.push(
        fetch(`${API_BASE}/api/orderbook`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            orderId: `SEED_B_${Date.now()}_${i}`,
            buySide: true,
            price,
            quantity,
            timestamp: Date.now(),
            isMarket: false,
          }),
        })
      );
    }

    // Place 12 asks above mid
    for (let i = 1; i <= 12; i++) {
      const price = baseMid + i * 25; // 100.25, 100.50, 100.75...
      const quantity = Math.floor(Math.random() * 80) + 20;
      orders.push(
        fetch(`${API_BASE}/api/orderbook`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            orderId: `SEED_A_${Date.now()}_${i}`,
            buySide: false,
            price,
            quantity,
            timestamp: Date.now(),
            isMarket: false,
          }),
        })
      );
    }

    await Promise.allSettled(orders);
  }, []);

  const clearBook = useCallback(async () => {
    // We don't have a mass-cancel API, but clearing state gives immediate visual reset
    setBids([]);
    setAsks([]);
    setSpread(0);
  }, []);

  // ── Live market order generator (interactive simulation) ────────────────
  const toggleSimulation = useCallback(() => {
    setIsSimulating(prev => !prev);
  }, []);

  useEffect(() => {
    if (!isSimulating) {
      if (simIntervalRef.current) {
        clearInterval(simIntervalRef.current);
        simIntervalRef.current = null;
      }
      return;
    }

    simIntervalRef.current = setInterval(async () => {
      try {
        const isBuy = Math.random() > 0.48;
        const isMarket = Math.random() < 0.25; // 25% aggressive market orders

        const bestBid = bids[0]?.price ?? 100;
        const bestAsk = asks[0]?.price ?? 100.5;
        const currentMid = (bestBid + bestAsk) / 2;

        let priceTicks: number;
        if (isMarket) {
          priceTicks = Math.round((isBuy ? bestAsk : bestBid) * 100);
        } else {
          // Limit order around the spread
          const offset = (Math.random() - 0.5) * 4;
          priceTicks = Math.round((currentMid + offset) * 100);
        }

        const quantity = Math.floor(Math.random() * 40) + 5;

        const res = await fetch(`${API_BASE}/api/orderbook`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            orderId: `FLOW_${Date.now()}_${Math.floor(Math.random() * 1000)}`,
            buySide: isBuy,
            price: priceTicks,
            quantity,
            timestamp: Date.now(),
            isMarket,
          }),
        });

        if (res.ok) {
          const data = await res.json();
          if (data.trades && data.trades.length > 0) {
            const t = data.trades[0];
            setLastTrade({
              id: `T_${Date.now()}`,
              price: t.price / 100,
              quantity: t.quantity,
              side: isBuy ? 'BUY' : 'SELL',
              timestamp: Date.now(),
              slippage: t.slippage,
              vwap: t.vwap ? t.vwap / 100 : undefined,
            });
          }
        }
      } catch {
        // silent fail in simulation tick
      }
    }, 600);

    return () => {
      if (simIntervalRef.current) {
        clearInterval(simIntervalRef.current);
        simIntervalRef.current = null;
      }
    };
  }, [isSimulating, bids, asks]);

  // ── Polling backend depth ───────────────────────────────────────────────
  useEffect(() => {
    let pollTimer: ReturnType<typeof setTimeout>;
    let cancelled = false;

    const poll = async () => {
      const startTime = performance.now();
      try {
        const res = await fetch(`${API_BASE}/api/orderbook/depth?levels=20`, {
          signal: AbortSignal.timeout(3000),
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);

        const data = await res.json();
        const duration = Math.round(performance.now() - startTime);
        if (cancelled) return;

        setLatencyMs(duration);
        failCountRef.current = 0;
        setConnected(true);
        setError(null);

        const newBids: OrderBookLevel[] = (data.bids ?? []).map((l: { price: number; quantity: number }) => ({
          price: l.price / 100,
          quantity: l.quantity,
        }));
        const newAsks: OrderBookLevel[] = (data.asks ?? []).map((l: { price: number; quantity: number }) => ({
          price: l.price / 100,
          quantity: l.quantity,
        }));

        setBids(newBids);
        setAsks(newAsks);

        const bestBid = newBids[0]?.price ?? null;
        const bestAsk = newAsks[0]?.price ?? null;
        setSpread(bestBid !== null && bestAsk !== null ? parseFloat((bestAsk - bestBid).toFixed(2)) : 0);

        // Auto-seed on first load if the book is totally empty
        if (!hasAutoSeededRef.current && newBids.length === 0 && newAsks.length === 0) {
          hasAutoSeededRef.current = true;
          seedLiquidity();
        }
      } catch (err: unknown) {
        if (cancelled) return;
        failCountRef.current++;
        setConnected(false);
        setError(failCountRef.current > 2 ? 'Reconnecting to LOB Engine...' : null);
      }

      if (!cancelled) {
        pollTimer = setTimeout(poll, POLL_INTERVAL_MS);
      }
    };

    poll();

    return () => {
      cancelled = true;
      clearTimeout(pollTimer);
    };
  }, [seedLiquidity]);

  return {
    bids,
    asks,
    lastTrade,
    spread,
    connected,
    error,
    latencyMs,
    isSimulating,
    toggleSimulation,
    seedLiquidity,
    clearBook,
  };
};
