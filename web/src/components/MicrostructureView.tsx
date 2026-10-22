import React from 'react';
import { Activity, ShieldCheck, Cpu, Sliders, TrendingUp, TrendingDown } from 'lucide-react';

interface MicrostructureProps {
  bids: { price: number; quantity: number }[];
  asks: { price: number; quantity: number }[];
  spread: number;
}

export const MicrostructureView: React.FC<MicrostructureProps> = ({ bids, asks, spread }) => {
  const totalBidVol = bids.reduce((acc, b) => acc + b.quantity, 0);
  const totalAskVol = asks.reduce((acc, a) => acc + a.quantity, 0);
  const totalVol = totalBidVol + totalAskVol;

  const bidNotional = bids.reduce((acc, b) => acc + b.price * b.quantity, 0);
  const askNotional = asks.reduce((acc, a) => acc + a.price * a.quantity, 0);

  const bestBid = bids[0]?.price ?? 0;
  const bestAsk = asks[0]?.price ?? 0;
  const midPrice = bestBid > 0 && bestAsk > 0 ? (bestBid + bestAsk) / 2 : (bestBid || bestAsk || 100);

  const spreadBps = midPrice > 0 ? ((spread / midPrice) * 10000).toFixed(1) : '0';

  // Imbalance: > 50% means buy pressure; < 50% means sell pressure
  const bidRatio = totalVol > 0 ? (totalBidVol / totalVol) * 100 : 50;
  const askRatio = 100 - bidRatio;

  // Book slope / liquidity density (qty per $1 depth)
  const bidDepthSpan = bids.length > 1 ? Math.max(0.01, bestBid - bids[bids.length - 1].price) : 1;
  const askDepthSpan = asks.length > 1 ? Math.max(0.01, asks[asks.length - 1].price - bestAsk) : 1;
  const bidDensity = (totalBidVol / bidDepthSpan).toFixed(0);
  const askDensity = (totalAskVol / askDepthSpan).toFixed(0);

  return (
    <div className="flex flex-col h-full font-mono text-terminal-text gap-6">
      {/* Header */}
      <div className="flex justify-between items-center pb-3 border-b border-terminal-border/60">
        <div className="flex items-center gap-2">
          <Activity className="text-terminal-accent" size={16} />
          <span className="text-xs uppercase tracking-widest text-terminal-muted font-bold">
            Order Book Microstructure &amp; Analytics
          </span>
        </div>
        <span className="text-[10px] bg-terminal-accent/10 text-terminal-accent px-2 py-0.5 rounded border border-terminal-accent/30 font-bold">
          DETERMINISTIC L2
        </span>
      </div>

      {/* Book Imbalance Meter */}
      <div className="bg-terminal-bg/70 border border-terminal-border/60 rounded-xl p-4">
        <div className="flex justify-between items-center mb-2 text-xs">
          <span className="flex items-center gap-1.5 text-terminal-bid font-bold">
            <TrendingUp size={14} /> Bid Pressure: {bidRatio.toFixed(1)}%
          </span>
          <span className="text-terminal-muted text-[10px] uppercase tracking-wider">
            Order Flow Imbalance (OFI)
          </span>
          <span className="flex items-center gap-1.5 text-terminal-ask font-bold">
            {askRatio.toFixed(1)}% Ask Pressure <TrendingDown size={14} />
          </span>
        </div>

        {/* Dual Progress Bar */}
        <div className="h-2.5 w-full bg-terminal-border/40 rounded-full overflow-hidden flex shadow-inner">
          <div
            className="h-full bg-gradient-to-r from-terminal-bid/70 to-terminal-bid transition-all duration-300"
            style={{ width: `${bidRatio}%` }}
          />
          <div
            className="h-full bg-gradient-to-l from-terminal-ask/70 to-terminal-ask transition-all duration-300"
            style={{ width: `${askRatio}%` }}
          />
        </div>

        <div className="flex justify-between text-[10px] text-terminal-muted mt-2">
          <span>{totalBidVol.toLocaleString()} Units resting</span>
          <span>Delta: {(totalBidVol - totalAskVol > 0 ? '+' : '') + (totalBidVol - totalAskVol).toLocaleString()}</span>
          <span>{totalAskVol.toLocaleString()} Units resting</span>
        </div>
      </div>

      {/* 4-Box Metrics Grid */}
      <div className="grid grid-cols-2 gap-4">
        <div className="bg-terminal-bg/50 border border-terminal-border/60 rounded-xl p-3.5 flex flex-col gap-1">
          <span className="text-[10px] uppercase text-terminal-muted tracking-widest">Effective Spread</span>
          <div className="flex items-baseline gap-2">
            <span className="text-base font-bold text-terminal-accent">${spread.toFixed(2)}</span>
            <span className="text-[11px] text-terminal-muted">({spreadBps} bps)</span>
          </div>
          <span className="text-[10px] text-terminal-muted mt-1">Cost of crossing the spread</span>
        </div>

        <div className="bg-terminal-bg/50 border border-terminal-border/60 rounded-xl p-3.5 flex flex-col gap-1">
          <span className="text-[10px] uppercase text-terminal-muted tracking-widest">Total Resting Notional</span>
          <div className="flex items-baseline gap-2">
            <span className="text-base font-bold text-terminal-text">
              ${(bidNotional + askNotional).toLocaleString(undefined, { maximumFractionDigits: 0 })}
            </span>
          </div>
          <span className="text-[10px] text-terminal-muted mt-1">
            Bids: ${bidNotional.toLocaleString(undefined, { maximumFractionDigits: 0 })} · Asks: ${askNotional.toLocaleString(undefined, { maximumFractionDigits: 0 })}
          </span>
        </div>

        <div className="bg-terminal-bg/50 border border-terminal-border/60 rounded-xl p-3.5 flex flex-col gap-1">
          <span className="text-[10px] uppercase text-terminal-muted tracking-widest">Liquidity Density</span>
          <div className="flex items-baseline gap-2">
            <span className="text-sm font-bold text-terminal-bid">{bidDensity} / $</span>
            <span className="text-[10px] text-terminal-muted">vs</span>
            <span className="text-sm font-bold text-terminal-ask">{askDensity} / $</span>
          </div>
          <span className="text-[10px] text-terminal-muted mt-1">Average market depth absorption</span>
        </div>

        <div className="bg-terminal-bg/50 border border-terminal-border/60 rounded-xl p-3.5 flex flex-col gap-1">
          <span className="text-[10px] uppercase text-terminal-muted tracking-widest">Engine Invariants</span>
          <div className="flex items-center gap-1.5 text-terminal-bid text-xs font-bold mt-1">
            <ShieldCheck size={14} /> No Crossed Book (P1 &lt; P2)
          </div>
          <div className="flex items-center gap-1.5 text-terminal-bid text-[11px] font-bold">
            <Cpu size={13} /> Strict Price-Time FIFO
          </div>
        </div>
      </div>
    </div>
  );
};

export default MicrostructureView;
