import React, { useState, useEffect } from 'react';
import { useMarketData, Trade } from './hooks/useMarketData';
import ProOrderBook from './components/ProOrderBook';
import TradeTape from './components/TradeTape';
import OrderEntry from './components/OrderEntry';
import MarketImpactView from './components/MarketImpactView';
import DepthVisualizer from './components/DepthVisualizer';
import MicrostructureView from './components/MicrostructureView';
import TradingViewChart from './components/TradingViewChart';
import OpenOrders, { UserOrder } from './components/OpenOrders';
import AlgoExecutionLab from './components/AlgoExecutionLab';
import { sounds } from './utils/soundEffects';
import {
  Play,
  Pause,
  RefreshCw,
  Zap,
  TrendingUp,
  Layers,
  Activity,
  ShieldCheck,
  CheckCircle2,
  BarChart3,
  Cpu,
  Volume2,
  VolumeX,
  Trash2,
} from 'lucide-react';

export function App() {
  const marketData = useMarketData();
  const [tradeHistory, setTradeHistory] = useState<Trade[]>([]);
  const [userOrders, setUserOrders] = useState<UserOrder[]>([]);
  const [selectedPrice, setSelectedPrice] = useState<number | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [activeTab, setActiveTab] = useState<'CHART' | 'ALGO' | 'IMPACT' | 'DEPTH' | 'ANALYTICS'>('CHART');
  const [toast, setToast] = useState<string | null>(null);
  const [isMuted, setIsMuted] = useState<boolean>(sounds.getMuted());

  // Listen for new trades -> update history and sound effects
  useEffect(() => {
    if (marketData.lastTrade) {
      const trade = marketData.lastTrade;
      setTradeHistory(prev => [trade, ...prev].slice(0, 100));

      // Trigger audio cue
      if (trade.quantity >= 100) {
        sounds.playWhaleSound();
      } else {
        sounds.playFillSound(trade.side === 'BUY');
      }
    }
  }, [marketData.lastTrade]);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 3200);
  };

  const toggleSound = () => {
    const muted = sounds.toggleMute();
    setIsMuted(muted);
    showToast(muted ? '🔇 Audio muted' : '🔊 Audio enabled');
  };

  // ── Order Placement ──────────────────────────────────────────────────
  const handleOrderSubmit = async (
    side: 'BUY' | 'SELL',
    priceTicks: number,
    qty: number,
    isMarket = false,
    orderType = 'LIMIT',
    peakSize?: number
  ) => {
    setIsSubmitting(true);
    const orderId = `ORD_${Date.now()}_${Math.floor(Math.random() * 1000)}`;
    const priceDisplay = priceTicks / 100;

    try {
      const response = await fetch('/api/orderbook', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          orderId,
          buySide: side === 'BUY',
          price: priceTicks,
          quantity: qty,
          timestamp: Date.now(),
          isMarket,
          orderType,
          peakSize,
        }),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || 'Submission failed');
      }

      const data = await response.json();

      if (data.trades && data.trades.length > 0) {
        showToast(`⚡ Filled ${qty} units @ ${side} (Trade Executed!)`);
        const t = data.trades[0];
        const newTrade: Trade = {
          id: `T_${Date.now()}`,
          price: t.price / 100,
          quantity: t.quantity,
          side: side,
          timestamp: Date.now(),
          slippage: t.slippage,
          vwap: t.vwap ? t.vwap / 100 : undefined,
        };
        setTradeHistory(prev => [newTrade, ...prev]);

        // Add to history as filled
        setUserOrders(prev => [
          {
            orderId,
            side,
            price: priceDisplay,
            quantity: qty,
            orderType,
            timestamp: Date.now(),
            status: 'FILLED',
            filledQty: qty,
          },
          ...prev,
        ]);
      } else {
        showToast(`✔ Resting ${side} order placed at $${priceDisplay.toFixed(2)}`);
        // Add to active open orders
        setUserOrders(prev => [
          {
            orderId,
            side,
            price: priceDisplay,
            quantity: qty,
            orderType,
            timestamp: Date.now(),
            status: 'OPEN',
            filledQty: 0,
          },
          ...prev,
        ]);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      showToast(`✖ Order failed: ${msg}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  // ── Cancel Order ─────────────────────────────────────────────────────
  const handleCancelOrder = async (orderId: string) => {
    try {
      const res = await fetch(`/api/orderbook/${orderId}`, { method: 'DELETE' });
      if (res.ok) {
        setUserOrders(prev =>
          prev.map(o => (o.orderId === orderId ? { ...o, status: 'CANCELLED' } : o))
        );
        showToast(`❌ Cancelled order ${orderId}`);
      } else {
        throw new Error('Cancel failed');
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      showToast(`✖ ${msg}`);
    }
  };

  // ── Cancel All Orders ────────────────────────────────────────────────
  const handleCancelAll = async () => {
    try {
      await fetch('/api/orderbook', { method: 'DELETE' });
      setUserOrders(prev => prev.map(o => ({ ...o, status: 'CANCELLED' })));
      marketData.clearBook();
      showToast('🧹 Cancelled all active orders & cleared book');
    } catch {
      showToast('✖ Failed to cancel all');
    }
  };

  const bestBid = marketData.bids[0]?.price ?? null;
  const bestAsk = marketData.asks[0]?.price ?? null;
  const midPrice = bestBid !== null && bestAsk !== null ? (bestBid + bestAsk) / 2 : (bestBid ?? bestAsk ?? 100);
  const spreadBps = bestBid !== null && bestAsk !== null && bestBid > 0 ? (((bestAsk - bestBid) / bestBid) * 10000).toFixed(1) : '—';

  return (
    <div className="min-h-screen bg-[#050507] bg-grid text-terminal-text font-sans flex flex-col p-4 md:p-5 selection:bg-terminal-accent/30 selection:text-terminal-accent">
      {/* ── Top Header Bar ─────────────────────────────────────────── */}
      <header className="flex flex-wrap justify-between items-center bg-terminal-panel/80 backdrop-blur-xl border border-terminal-border/80 rounded-2xl px-5 py-3.5 mb-5 shadow-2xl gap-4">
        {/* Brand & Market Ticker */}
        <div className="flex items-center gap-4">
          <div className="w-10 h-10 bg-gradient-to-br from-terminal-accent to-yellow-500 rounded-xl flex items-center justify-center font-black text-black text-sm shadow-[0_0_24px_rgba(250,204,21,0.4)] tracking-tighter">
            LOB
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base font-black tracking-tight text-white font-mono">
                LOB/USD
              </h1>
              <span className="text-[10px] bg-terminal-bid/10 text-terminal-bid px-2 py-0.5 rounded-full border border-terminal-bid/30 font-bold font-mono">
                SPOT LIVE
              </span>
            </div>
            <p className="text-[10px] text-terminal-muted font-mono mt-0.5">
              Deterministic High-Frequency Exchange Engine
            </p>
          </div>
        </div>

        {/* Live Metrics Ticker */}
        <div className="flex items-center gap-6 font-mono text-xs">
          <div className="flex flex-col items-end">
            <span className="text-terminal-muted uppercase text-[9px] tracking-widest">Mid Price</span>
            <span className="text-base font-black text-white tabular-nums">
              ${midPrice.toFixed(2)}
            </span>
          </div>

          <div className="h-7 w-px bg-terminal-border/60" />

          <div className="flex flex-col items-end">
            <span className="text-terminal-muted uppercase text-[9px] tracking-widest">Spread</span>
            <div className="flex items-center gap-1.5">
              <span className="text-sm font-bold text-terminal-accent tabular-nums">
                ${marketData.spread.toFixed(2)}
              </span>
              <span className="text-[10px] text-terminal-muted">({spreadBps} bps)</span>
            </div>
          </div>

          <div className="h-7 w-px bg-terminal-border/60" />

          <div className="flex flex-col items-end">
            <span className="text-terminal-muted uppercase text-[9px] tracking-widest">Best Bid / Ask</span>
            <div className="flex items-center gap-1.5 text-xs font-bold tabular-nums">
              <span className="text-terminal-bid">${bestBid !== null ? bestBid.toFixed(2) : '—'}</span>
              <span className="text-terminal-muted">/</span>
              <span className="text-terminal-ask">${bestAsk !== null ? bestAsk.toFixed(2) : '—'}</span>
            </div>
          </div>

          <div className="h-7 w-px bg-terminal-border/60" />

          {/* Engine Status & Latency */}
          <div className="flex flex-col items-end">
            <span className="text-terminal-muted uppercase text-[9px] tracking-widest">Engine Status</span>
            <div className="flex items-center gap-1.5 text-xs font-bold">
              <span className={`w-2 h-2 rounded-full ${marketData.connected ? 'bg-terminal-bid animate-pulse shadow-[0_0_8px_#00ffaa]' : 'bg-terminal-ask'}`} />
              <span className="text-terminal-text">{marketData.connected ? 'ONLINE' : 'CONNECTING'}</span>
              <span className="text-[10px] text-terminal-muted">({marketData.latencyMs}ms)</span>
            </div>
          </div>
        </div>

        {/* Interactive Action Controls */}
        <div className="flex items-center gap-2 font-mono">
          {/* Sound Toggle */}
          <button
            onClick={toggleSound}
            title={isMuted ? 'Unmute Audio' : 'Mute Audio'}
            className="p-2 bg-terminal-panel hover:bg-terminal-border/80 text-terminal-muted hover:text-terminal-text border border-terminal-border rounded-xl transition-colors"
          >
            {isMuted ? <VolumeX size={15} /> : <Volume2 size={15} className="text-terminal-accent" />}
          </button>

          <button
            onClick={marketData.toggleSimulation}
            className={`px-3.5 py-2 rounded-xl text-xs font-bold transition-all duration-200 flex items-center gap-2 shadow-lg
              ${marketData.isSimulating
                ? 'bg-terminal-bid text-black shadow-[0_0_18px_rgba(0,255,170,0.35)] scale-[1.02]'
                : 'bg-terminal-panel hover:bg-terminal-border/80 text-terminal-text border border-terminal-border'}`}
          >
            {marketData.isSimulating ? (
              <>
                <Pause size={13} className="fill-current" /> Pause Live Flow
              </>
            ) : (
              <>
                <Play size={13} className="fill-current text-terminal-bid" /> Run Live Flow
              </>
            )}
          </button>

          <button
            onClick={() => {
              marketData.seedLiquidity();
              showToast('🌱 24 Liquidity Levels Injected into Book');
            }}
            title="Prepopulate 24 limit orders into both sides"
            className="px-3 py-2 bg-terminal-panel hover:bg-terminal-border/80 text-terminal-muted hover:text-terminal-text border border-terminal-border rounded-xl text-xs font-bold flex items-center gap-1.5 transition-colors"
          >
            <RefreshCw size={12} /> Seed Depth
          </button>

          <button
            onClick={handleCancelAll}
            title="Clear all orders in the book"
            className="px-3 py-2 bg-terminal-panel hover:bg-terminal-ask/20 text-terminal-muted hover:text-terminal-ask border border-terminal-border rounded-xl text-xs font-bold flex items-center gap-1.5 transition-colors"
          >
            <Trash2 size={12} /> Clear Book
          </button>
        </div>
      </header>

      {/* ── Main Workspace Grid ────────────────────────────────────── */}
      <main className="grid grid-cols-12 gap-5 flex-1 mb-5">
        {/* Left Column: Pro Order Book + Order Entry (3 cols) */}
        <div className="col-span-12 lg:col-span-3 flex flex-col gap-5">
          <div className="flex-1 min-h-[460px]">
            <ProOrderBook
              bids={marketData.bids}
              asks={marketData.asks}
              onPriceSelect={(p) => setSelectedPrice(p)}
            />
          </div>
          <div>
            <OrderEntry
              selectedPrice={selectedPrice}
              onOrderSubmit={handleOrderSubmit}
              isSubmitting={isSubmitting}
            />
          </div>
        </div>

        {/* Center Stage: Interactive Tabbed Analytics Lab (6 cols) */}
        <div className="col-span-12 lg:col-span-6 flex flex-col gap-5">
          <div className="bg-terminal-panel/90 backdrop-blur-md border border-terminal-border/80 rounded-2xl overflow-hidden p-5 shadow-2xl flex-1 flex flex-col">
            {/* Center Tab Navigation */}
            <div className="flex items-center justify-between pb-3.5 border-b border-terminal-border/60 mb-4 font-mono overflow-x-auto custom-scrollbar">
              <div className="flex gap-1.5">
                <button
                  onClick={() => setActiveTab('CHART')}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 shrink-0
                    ${activeTab === 'CHART'
                      ? 'bg-terminal-accent text-black shadow-[0_0_12px_rgba(250,204,21,0.3)]'
                      : 'text-terminal-muted hover:text-terminal-text bg-terminal-bg/60 border border-terminal-border/50'}`}
                >
                  <BarChart3 size={13} /> Candlestick Chart
                </button>

                <button
                  onClick={() => setActiveTab('ALGO')}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 shrink-0
                    ${activeTab === 'ALGO'
                      ? 'bg-terminal-accent text-black shadow-[0_0_12px_rgba(250,204,21,0.3)]'
                      : 'text-terminal-muted hover:text-terminal-text bg-terminal-bg/60 border border-terminal-border/50'}`}
                >
                  <Cpu size={13} /> Algo Execution Lab
                </button>

                <button
                  onClick={() => setActiveTab('IMPACT')}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 shrink-0
                    ${activeTab === 'IMPACT'
                      ? 'bg-terminal-accent text-black shadow-[0_0_12px_rgba(250,204,21,0.3)]'
                      : 'text-terminal-muted hover:text-terminal-text bg-terminal-bg/60 border border-terminal-border/50'}`}
                >
                  <TrendingUp size={13} /> Slippage Model
                </button>

                <button
                  onClick={() => setActiveTab('DEPTH')}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 shrink-0
                    ${activeTab === 'DEPTH'
                      ? 'bg-terminal-accent text-black shadow-[0_0_12px_rgba(250,204,21,0.3)]'
                      : 'text-terminal-muted hover:text-terminal-text bg-terminal-bg/60 border border-terminal-border/50'}`}
                >
                  <Layers size={13} /> Depth Curve
                </button>

                <button
                  onClick={() => setActiveTab('ANALYTICS')}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 shrink-0
                    ${activeTab === 'ANALYTICS'
                      ? 'bg-terminal-accent text-black shadow-[0_0_12px_rgba(250,204,21,0.3)]'
                      : 'text-terminal-muted hover:text-terminal-text bg-terminal-bg/60 border border-terminal-border/50'}`}
                >
                  <Activity size={13} /> Microstructure
                </button>
              </div>

              <div className="hidden xl:flex items-center gap-1.5 text-[10px] text-terminal-muted shrink-0 pl-3">
                <ShieldCheck size={13} className="text-terminal-bid" />
                <span>Deterministic Tick FIFO</span>
              </div>
            </div>

            {/* Active Tab Content */}
            <div className="flex-1 min-h-[300px]">
              {activeTab === 'CHART' && (
                <TradingViewChart trades={tradeHistory} currentPrice={midPrice} />
              )}
              {activeTab === 'ALGO' && (
                <AlgoExecutionLab
                  onOrderSubmit={handleOrderSubmit}
                  bestBid={bestBid}
                  bestAsk={bestAsk}
                  midPrice={midPrice}
                />
              )}
              {activeTab === 'IMPACT' && <MarketImpactView />}
              {activeTab === 'DEPTH' && (
                <DepthVisualizer
                  bids={marketData.bids}
                  asks={marketData.asks}
                  onPriceSelect={(p) => setSelectedPrice(p)}
                />
              )}
              {activeTab === 'ANALYTICS' && (
                <MicrostructureView
                  bids={marketData.bids}
                  asks={marketData.asks}
                  spread={marketData.spread}
                />
              )}
            </div>
          </div>
        </div>

        {/* Right Column: Live Executed Trades Tape (3 cols) */}
        <div className="col-span-12 lg:col-span-3 flex flex-col">
          <TradeTape trades={tradeHistory} />
        </div>
      </main>

      {/* ── Bottom Section: Active Open Orders & Fills History ──────── */}
      <section className="w-full">
        <OpenOrders
          orders={userOrders}
          onCancelOrder={handleCancelOrder}
          onCancelAll={handleCancelAll}
        />
      </section>

      {/* ── Toast Notification ────────────────────────────────────────── */}
      {toast && (
        <div className="fixed bottom-6 right-6 z-50 bg-terminal-panel border border-terminal-accent/50 text-white font-mono text-xs px-4 py-3 rounded-xl shadow-[0_0_24px_rgba(0,0,0,0.8)] flex items-center gap-2.5 animate-in fade-in slide-in-from-bottom-3 duration-200">
          <CheckCircle2 size={16} className="text-terminal-accent shrink-0" />
          <span>{toast}</span>
        </div>
      )}
    </div>
  );
}

export default App;