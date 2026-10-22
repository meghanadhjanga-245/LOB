import React, { useState, useEffect, memo } from 'react';
import { Layers } from 'lucide-react';

interface Level {
  price: number;
  quantity: number;
}

interface ProOrderBookProps {
  bids: Level[];
  asks: Level[];
  onPriceSelect: (price: number, side: 'BUY' | 'SELL') => void;
}

interface RowData {
  levels: Level[];
  side: 'BUY' | 'SELL';
  maxQty: number;
  onPriceSelect: (price: number, side: 'BUY' | 'SELL') => void;
}

const LevelRow: React.FC<{
  level: Level;
  side: 'BUY' | 'SELL';
  maxQty: number;
  cumulativeQty: number;
  onPriceSelect: (price: number, side: 'BUY' | 'SELL') => void;
}> = memo(({ level, side, maxQty, cumulativeQty, onPriceSelect }) => {
  const isBid = side === 'BUY';
  const width = maxQty > 0 ? (cumulativeQty / maxQty) * 100 : 0;
  const [flash, setFlash] = useState(false);

  useEffect(() => {
    setFlash(true);
    const t = setTimeout(() => setFlash(false), 250);
    return () => clearTimeout(t);
  }, [level.quantity]);

  return (
    <div
      onClick={() => onPriceSelect(level.price, side)}
      className={`relative flex justify-between items-center px-3.5 py-1 text-xs cursor-pointer select-none font-mono transition-colors group
        ${isBid ? 'hover:bg-terminal-bid/10' : 'hover:bg-terminal-ask/10'}
        ${flash ? (isBid ? 'bg-terminal-bid/25' : 'bg-terminal-ask/25') : ''}
      `}
    >
      {/* Visual Depth Bar */}
      <div
        className={`absolute right-0 top-0 bottom-0 pointer-events-none transition-all duration-200 opacity-25
          ${isBid ? 'bg-gradient-to-l from-terminal-bid/80 to-transparent' : 'bg-gradient-to-l from-terminal-ask/80 to-transparent'}
        `}
        style={{ width: `${Math.min(100, width)}%` }}
      />

      {/* Price */}
      <span className={`relative z-10 font-bold tabular-nums ${isBid ? 'text-terminal-bid' : 'text-terminal-ask'}`}>
        {level.price.toFixed(2)}
      </span>

      {/* Quantity */}
      <span className="relative z-10 text-terminal-text tabular-nums text-right font-medium">
        {level.quantity.toLocaleString()}
      </span>

      {/* Cumulative total */}
      <span className="relative z-10 text-terminal-muted tabular-nums text-right text-[10px] group-hover:text-terminal-text transition-colors">
        {cumulativeQty.toLocaleString()}
      </span>
    </div>
  );
});

LevelRow.displayName = 'LevelRow';

export const ProOrderBook: React.FC<ProOrderBookProps> = ({ bids, asks, onPriceSelect }) => {
  const [grouping, setGrouping] = useState<number>(0.01);

  // Group adjacent prices when grouping > 0.01
  const aggregatedBids = React.useMemo(() => {
    if (grouping <= 0.01) return bids;
    const map = new Map<number, number>();
    for (const lvl of bids) {
      const bucketPrice = parseFloat((Math.floor(lvl.price / grouping) * grouping).toFixed(2));
      map.set(bucketPrice, (map.get(bucketPrice) || 0) + lvl.quantity);
    }
    const res: Level[] = [];
    map.forEach((quantity, price) => res.push({ price, quantity }));
    return res.sort((a, b) => b.price - a.price);
  }, [bids, grouping]);

  const aggregatedAsks = React.useMemo(() => {
    if (grouping <= 0.01) return asks;
    const map = new Map<number, number>();
    for (const lvl of asks) {
      const bucketPrice = parseFloat((Math.ceil(lvl.price / grouping) * grouping).toFixed(2));
      map.set(bucketPrice, (map.get(bucketPrice) || 0) + lvl.quantity);
    }
    const res: Level[] = [];
    map.forEach((quantity, price) => res.push({ price, quantity }));
    return res.sort((a, b) => a.price - b.price);
  }, [asks, grouping]);

  const visibleLevels = 10;
  const displayAsks = [...aggregatedAsks].slice(0, visibleLevels).reverse();
  const displayBids = aggregatedBids.slice(0, visibleLevels);

  // Cumulative depth sums
  let cumAsk = 0;
  const askCumList = [...aggregatedAsks].slice(0, visibleLevels).map(a => {
    cumAsk += a.quantity;
    return cumAsk;
  }).reverse();

  let cumBid = 0;
  const bidCumList = displayBids.map(b => {
    cumBid += b.quantity;
    return cumBid;
  });

  const maxTotal = Math.max(cumBid, cumAsk, 1);

  const bestBid = bids[0]?.price ?? null;
  const bestAsk = asks[0]?.price ?? null;
  const spread = bestBid !== null && bestAsk !== null ? (bestAsk - bestBid).toFixed(2) : '—';
  const spreadBps =
    bestBid !== null && bestAsk !== null && bestBid > 0
      ? (((bestAsk - bestBid) / bestBid) * 10000).toFixed(1)
      : null;

  return (
    <div className="flex flex-col bg-terminal-panel/90 backdrop-blur-md border border-terminal-border/80 rounded-2xl overflow-hidden shadow-2xl h-full font-mono">
      {/* Book Header */}
      <div className="flex justify-between items-center px-4 py-3 border-b border-terminal-border/80 bg-terminal-bg/80">
        <div className="flex items-center gap-2">
          <Layers size={14} className="text-terminal-accent" />
          <span className="text-xs uppercase tracking-widest text-terminal-muted font-bold">
            Order Book
          </span>
        </div>
        <div className="flex items-center gap-1.5">
          {/* Tick Grouping Selector */}
          <select
            value={grouping}
            onChange={e => setGrouping(Number(e.target.value))}
            className="bg-terminal-bg border border-terminal-border/70 rounded-lg px-2 py-0.5 text-[10px] text-terminal-accent font-bold focus:outline-none focus:border-terminal-accent cursor-pointer"
          >
            <option value={0.01}>0.01 Tick</option>
            <option value={0.05}>0.05 Tick</option>
            <option value={0.10}>0.10 Tick</option>
            <option value={0.25}>0.25 Tick</option>
            <option value={0.50}>0.50 Tick</option>
            <option value={1.00}>1.00 Tick</option>
          </select>
        </div>
      </div>

      {/* Column labels */}
      <div className="flex justify-between px-3.5 py-1.5 text-[10px] uppercase tracking-wider text-terminal-muted border-b border-terminal-border/40 bg-terminal-panel/60">
        <span>Price ($)</span>
        <span className="text-right">Size</span>
        <span className="text-right">Total</span>
      </div>

      {/* Asks (Sells) */}
      <div className="flex flex-col justify-end overflow-hidden flex-1 py-1">
        {displayAsks.length === 0 ? (
          <div className="text-center py-4 text-terminal-muted text-[11px]">No asks</div>
        ) : (
          displayAsks.map((level, idx) => (
            <LevelRow
              key={`ask-${level.price}`}
              level={level}
              side="SELL"
              maxQty={maxTotal}
              cumulativeQty={askCumList[idx] ?? level.quantity}
              onPriceSelect={onPriceSelect}
            />
          ))
        )}
      </div>

      {/* Spread Bar */}
      <div className="flex justify-between items-center px-4 py-2 bg-terminal-bg/90 border-y border-terminal-border/80">
        <div className="flex items-center gap-2">
          <span className="text-[10px] uppercase tracking-wider text-terminal-muted">Spread</span>
          <span className="text-xs font-bold text-terminal-accent tabular-nums">${spread}</span>
        </div>
        {spreadBps && (
          <span className="text-[10px] text-terminal-muted bg-terminal-border/40 px-1.5 py-0.5 rounded">
            {spreadBps} bps
          </span>
        )}
      </div>

      {/* Bids (Buys) */}
      <div className="flex flex-col overflow-hidden flex-1 py-1">
        {displayBids.length === 0 ? (
          <div className="text-center py-4 text-terminal-muted text-[11px]">No bids</div>
        ) : (
          displayBids.map((level, idx) => (
            <LevelRow
              key={`bid-${level.price}`}
              level={level}
              side="BUY"
              maxQty={maxTotal}
              cumulativeQty={bidCumList[idx] ?? level.quantity}
              onPriceSelect={onPriceSelect}
            />
          ))
        )}
      </div>
    </div>
  );
};

export default ProOrderBook;
