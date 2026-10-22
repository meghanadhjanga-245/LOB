import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Activity, Zap } from 'lucide-react';

interface Trade {
  id: string;
  price: number;
  quantity: number;
  side: 'BUY' | 'SELL';
  timestamp: number;
  slippage?: number;
  vwap?: number;
}

interface TradeTapeProps {
  trades: Trade[];
}

export const TradeTape: React.FC<TradeTapeProps> = ({ trades }) => {
  const buyTrades = trades.filter(t => t.side === 'BUY');
  const sellTrades = trades.filter(t => t.side === 'SELL');
  const buyVolume = buyTrades.reduce((acc, t) => acc + t.quantity, 0);
  const sellVolume = sellTrades.reduce((acc, t) => acc + t.quantity, 0);
  const totalVolume = buyVolume + sellVolume;
  const buyPct = totalVolume > 0 ? (buyVolume / totalVolume) * 100 : 50;

  return (
    <div className="flex flex-col h-full bg-terminal-panel/90 backdrop-blur-md border border-terminal-border/80 rounded-2xl overflow-hidden font-mono shadow-2xl">
      {/* Header */}
      <div className="px-4 py-3 border-b border-terminal-border/80 bg-terminal-bg/80 flex justify-between items-center">
        <div className="flex items-center gap-2">
          <Activity size={14} className="text-terminal-accent" />
          <span className="text-xs uppercase tracking-widest text-terminal-muted font-bold">
            Live Tape
          </span>
        </div>
        <div className="flex items-center gap-1.5 text-[10px] text-terminal-accent bg-terminal-accent/10 px-2 py-0.5 rounded border border-terminal-accent/20 font-bold">
          <span className="w-1.5 h-1.5 rounded-full bg-terminal-accent animate-ping" />
          EXEC
        </div>
      </div>

      {/* Volume Ratio Bar */}
      <div className="px-4 py-2 border-b border-terminal-border/40 bg-terminal-bg/40">
        <div className="flex justify-between text-[10px] mb-1">
          <span className="text-terminal-bid font-bold">Buy: {buyPct.toFixed(0)}%</span>
          <span className="text-terminal-muted">Vol: {totalVolume.toLocaleString()}</span>
          <span className="text-terminal-ask font-bold">Sell: {(100 - buyPct).toFixed(0)}%</span>
        </div>
        <div className="h-1.5 w-full bg-terminal-border/50 rounded-full overflow-hidden flex">
          <div className="h-full bg-terminal-bid transition-all duration-300" style={{ width: `${buyPct}%` }} />
          <div className="h-full bg-terminal-ask transition-all duration-300" style={{ width: `${100 - buyPct}%` }} />
        </div>
      </div>

      {/* Column Headers */}
      <div className="flex px-4 py-1.5 text-[10px] uppercase tracking-wider text-terminal-muted border-b border-terminal-border/40 bg-terminal-panel/60">
        <span className="flex-1">Price ($)</span>
        <span className="w-16 text-right">Size</span>
        <span className="w-20 text-right">Time</span>
      </div>

      {/* Trade Rows */}
      <div className="flex-1 overflow-y-auto custom-scrollbar">
        {trades.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-48 text-terminal-muted text-xs gap-2 p-4 text-center">
            <span className="text-terminal-accent/60">Waiting for market fills…</span>
            <span className="text-[10px]">Execute a market order or turn on Live Flow to see trades stream here.</span>
          </div>
        ) : (
          <AnimatePresence initial={false}>
            {trades.map(trade => {
              const isWhale = trade.quantity >= 100;
              const isBuy = trade.side === 'BUY';

              return (
                <motion.div
                  key={trade.id}
                  initial={{ x: 12, opacity: 0, backgroundColor: isBuy ? 'rgba(0,255,170,0.18)' : 'rgba(255,51,102,0.18)' }}
                  animate={{ x: 0, opacity: 1, backgroundColor: 'rgba(0,0,0,0)' }}
                  transition={{ duration: 0.2 }}
                  className={`flex items-center px-4 py-1.5 border-b border-terminal-border/20 text-xs tabular-nums transition-colors hover:bg-white/5
                    ${isBuy ? 'text-terminal-bid' : 'text-terminal-ask'}`}
                >
                  <span className="flex-1 font-bold flex items-center gap-1">
                    {trade.price.toFixed(2)}
                    {isWhale && (
                      <span className="text-[9px] px-1 py-0.2 rounded bg-terminal-accent text-black font-extrabold flex items-center gap-0.5">
                        <Zap size={9} /> WHALE
                      </span>
                    )}
                  </span>
                  <span className="w-16 text-right font-medium text-terminal-text">
                    {trade.quantity.toLocaleString()}
                  </span>
                  <span className="w-20 text-right text-terminal-muted text-[10px]">
                    {new Date(trade.timestamp).toLocaleTimeString([], {
                      hour12: false,
                      hour: '2-digit',
                      minute: '2-digit',
                      second: '2-digit',
                    })}
                  </span>
                </motion.div>
              );
            })}
          </AnimatePresence>
        )}
      </div>

      {/* Footer Details */}
      <div className="px-4 py-2 border-t border-terminal-border/80 bg-terminal-bg/90 flex justify-between text-[10px] text-terminal-muted">
        <span>Total Trades: <strong className="text-terminal-text">{trades.length}</strong></span>
        <span>Engine: <strong className="text-terminal-bid">Price-Time FIFO</strong></span>
      </div>
    </div>
  );
};

export default TradeTape;
