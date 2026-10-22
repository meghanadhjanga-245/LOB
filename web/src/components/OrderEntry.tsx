import React, { useState, useEffect } from 'react';
import { ArrowUpCircle, ArrowDownCircle, AlertCircle, Zap, Shield, EyeOff } from 'lucide-react';

interface OrderEntryProps {
  onOrderSubmit: (side: 'BUY' | 'SELL', price: number, qty: number, isMarket?: boolean, orderType?: string, peakSize?: number) => void;
  selectedPrice: number | null;
  isSubmitting?: boolean;
}

const QUICK_QTYS = [10, 25, 50, 100, 250];

export const OrderEntry: React.FC<OrderEntryProps> = ({ onOrderSubmit, selectedPrice, isSubmitting }) => {
  const [side, setSide] = useState<'BUY' | 'SELL'>('BUY');
  const [orderType, setOrderType] = useState<'LIMIT' | 'MARKET' | 'IOC' | 'ICEBERG'>('LIMIT');
  const [price, setPrice] = useState<string>('100.00');
  const [qty, setQty] = useState<string>('25');
  const [peakSize, setPeakSize] = useState<string>('5');
  const [error, setError] = useState<string | null>(null);

  // Sync price when user clicks a price level in the order book
  useEffect(() => {
    if (selectedPrice !== null) {
      setPrice(selectedPrice.toFixed(2));
      setError(null);
    }
  }, [selectedPrice]);

  const validate = (): string | null => {
    const q = parseInt(qty);
    if (isNaN(q) || q <= 0) return 'Enter a valid quantity';

    if (orderType !== 'MARKET') {
      const p = parseFloat(price);
      if (isNaN(p) || p <= 0) return 'Enter a valid price';
    }

    if (orderType === 'ICEBERG') {
      const peak = parseInt(peakSize);
      if (isNaN(peak) || peak <= 0 || peak >= q) {
        return 'Peak size must be > 0 and less than total quantity';
      }
    }

    return null;
  };

  const handleSubmit = () => {
    const err = validate();
    if (err) { setError(err); return; }
    setError(null);

    const priceTicks = orderType === 'MARKET' ? 0 : Math.round(parseFloat(price) * 100);
    const quantity = parseInt(qty);
    const isMarket = orderType === 'MARKET';
    const peak = orderType === 'ICEBERG' ? parseInt(peakSize) : undefined;

    onOrderSubmit(side, priceTicks, quantity, isMarket, orderType, peak);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSubmit();
  };

  const notional = (parseFloat(price) || 0) * (parseInt(qty) || 0);

  return (
    <div className="bg-terminal-panel/90 backdrop-blur-md border border-terminal-border/80 rounded-2xl p-5 font-mono shadow-2xl">
      {/* Side Selector (BUY vs SELL) */}
      <div className="flex gap-2.5 mb-4">
        <button
          onClick={() => { setSide('BUY'); setError(null); }}
          className={`flex-1 py-2.5 rounded-xl font-bold text-xs transition-all duration-200 flex items-center justify-center gap-2
            ${side === 'BUY'
              ? 'bg-terminal-bid text-black shadow-[0_0_18px_rgba(0,255,170,0.35)] scale-[1.01]'
              : 'bg-terminal-bg text-terminal-muted hover:bg-terminal-border/50 border border-terminal-border/60'}`}
        >
          <ArrowUpCircle size={15} /> BUY / BID
        </button>
        <button
          onClick={() => { setSide('SELL'); setError(null); }}
          className={`flex-1 py-2.5 rounded-xl font-bold text-xs transition-all duration-200 flex items-center justify-center gap-2
            ${side === 'SELL'
              ? 'bg-terminal-ask text-black shadow-[0_0_18px_rgba(255,51,102,0.35)] scale-[1.01]'
              : 'bg-terminal-bg text-terminal-muted hover:bg-terminal-border/50 border border-terminal-border/60'}`}
        >
          <ArrowDownCircle size={15} /> SELL / ASK
        </button>
      </div>

      {/* Order Type Tabs */}
      <div className="grid grid-cols-4 gap-1.5 p-1 bg-terminal-bg/80 border border-terminal-border/60 rounded-xl mb-4 text-[10px]">
        {(['LIMIT', 'MARKET', 'IOC', 'ICEBERG'] as const).map(type => (
          <button
            key={type}
            onClick={() => { setOrderType(type); setError(null); }}
            className={`py-1.5 rounded-lg font-bold transition-all
              ${orderType === type
                ? 'bg-terminal-accent text-black shadow'
                : 'text-terminal-muted hover:text-terminal-text'}`}
          >
            {type}
          </button>
        ))}
      </div>

      {/* Price input (hidden for market orders) */}
      {orderType !== 'MARKET' ? (
        <div className="flex flex-col gap-1 mb-3.5">
          <div className="flex justify-between items-center text-[10px]">
            <label className="uppercase text-terminal-muted font-bold tracking-wider">
              Price (USD)
            </label>
            <span className="text-terminal-muted text-[10px]">Click book level to fill</span>
          </div>
          <div className="relative">
            <span className="absolute left-3 top-2.5 text-terminal-muted text-xs">$</span>
            <input
              type="number"
              step="0.01"
              min="0"
              placeholder="100.00"
              value={price}
              onChange={e => { setPrice(e.target.value); setError(null); }}
              onKeyDown={handleKeyDown}
              className="w-full bg-terminal-bg border border-terminal-border/80 rounded-xl pl-7 pr-3 py-2 text-terminal-text text-sm focus:outline-none focus:border-terminal-accent transition-colors placeholder:text-terminal-muted/40 tabular-nums font-bold"
            />
          </div>
        </div>
      ) : (
        <div className="flex items-center gap-2 px-3 py-2.5 mb-3.5 bg-terminal-bg/60 border border-terminal-border/60 rounded-xl text-xs text-terminal-accent font-bold">
          <Zap size={14} /> Market Order executes against top of book immediately.
        </div>
      )}

      {/* Quantity input */}
      <div className="flex flex-col gap-1 mb-3.5">
        <div className="flex justify-between items-center text-[10px]">
          <label className="uppercase text-terminal-muted font-bold tracking-wider">
            Order Quantity
          </label>
          <span className="text-terminal-muted">Units</span>
        </div>
        <input
          type="number"
          min="1"
          placeholder="25"
          value={qty}
          onChange={e => { setQty(e.target.value); setError(null); }}
          onKeyDown={handleKeyDown}
          className="w-full bg-terminal-bg border border-terminal-border/80 rounded-xl px-3 py-2 text-terminal-text text-sm focus:outline-none focus:border-terminal-accent transition-colors placeholder:text-terminal-muted/40 tabular-nums font-bold"
        />

        {/* Quick select buttons */}
        <div className="flex gap-1.5 mt-2">
          {QUICK_QTYS.map(q => (
            <button
              key={q}
              onClick={() => { setQty(String(q)); setError(null); }}
              className={`flex-1 py-1 rounded-lg text-[10px] font-bold border transition-colors
                ${parseInt(qty) === q
                  ? 'border-terminal-accent text-terminal-accent bg-terminal-accent/10'
                  : 'border-terminal-border/60 text-terminal-muted hover:border-terminal-border hover:text-terminal-text'}`}
            >
              {q}
            </button>
          ))}
        </div>
      </div>

      {/* Iceberg Peak Size field */}
      {orderType === 'ICEBERG' && (
        <div className="flex flex-col gap-1 mb-3.5 p-3 bg-terminal-accent/5 border border-terminal-accent/30 rounded-xl">
          <div className="flex items-center gap-1.5 text-[10px] text-terminal-accent font-bold">
            <EyeOff size={13} /> Visible Peak Size (Book Display)
          </div>
          <input
            type="number"
            min="1"
            value={peakSize}
            onChange={e => setPeakSize(e.target.value)}
            className="w-full bg-terminal-bg border border-terminal-border/80 rounded-lg px-2.5 py-1.5 text-xs text-terminal-text mt-1 focus:outline-none focus:border-terminal-accent"
          />
          <span className="text-[9px] text-terminal-muted">
            Remaining {(parseInt(qty) || 0) - (parseInt(peakSize) || 0)} units remain hidden in pool.
          </span>
        </div>
      )}

      {/* Notional & Fees Estimate */}
      <div className="flex justify-between items-center px-3 py-2 bg-terminal-bg/50 rounded-xl border border-terminal-border/40 text-[11px] mb-4">
        <span className="text-terminal-muted">Est. Notional</span>
        <span className="font-bold text-terminal-text tabular-nums">
          ${notional.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
        </span>
      </div>

      {/* Error message */}
      {error && (
        <div className="flex items-center gap-2 text-terminal-ask text-xs bg-terminal-ask/10 border border-terminal-ask/30 rounded-xl px-3 py-2 mb-3.5">
          <AlertCircle size={14} className="shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Submit button */}
      <button
        onClick={handleSubmit}
        disabled={isSubmitting}
        className={`w-full py-3 rounded-xl font-bold text-sm tracking-wide transition-all duration-200 flex items-center justify-center gap-2
          ${side === 'BUY'
            ? 'bg-terminal-bid text-black hover:bg-terminal-bid/90 shadow-[0_0_20px_rgba(0,255,170,0.3)]'
            : 'bg-terminal-ask text-black hover:bg-terminal-ask/90 shadow-[0_0_20px_rgba(255,51,102,0.3)]'}
          ${isSubmitting ? 'opacity-50 cursor-not-allowed' : 'active:scale-98'}`}
      >
        {isSubmitting ? 'Routing Order...' : `${side} ${qty} @ ${orderType === 'MARKET' ? 'MKT' : `$${price}`}`}
      </button>
    </div>
  );
};

export default OrderEntry;
