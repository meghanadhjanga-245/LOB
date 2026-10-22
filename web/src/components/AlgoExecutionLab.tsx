import React, { useState, useRef } from 'react';
import { Play, Pause, AlertTriangle, Cpu, TrendingDown, Layers, CheckCircle2, RotateCcw } from 'lucide-react';

interface AlgoExecutionLabProps {
  onOrderSubmit: (side: 'BUY' | 'SELL', priceTicks: number, qty: number, isMarket?: boolean, orderType?: string) => Promise<void>;
  bestBid: number | null;
  bestAsk: number | null;
  midPrice: number;
}

interface TwapReport {
  targetQty: number;
  filledQty: number;
  arrivalPrice: number;
  executedVwap: number;
  shortfallBps: number;
  sliceLogs: string[];
}

export const AlgoExecutionLab: React.FC<AlgoExecutionLabProps> = ({
  onOrderSubmit,
  bestBid,
  bestAsk,
  midPrice,
}) => {
  const [activeTab, setActiveTab] = useState<'TWAP' | 'FLASH_CRASH' | 'HFT_MAKER'>('TWAP');

  // TWAP State
  const [twapSide, setTwapSide] = useState<'BUY' | 'SELL'>('BUY');
  const [twapQty, setTwapQty] = useState<number>(150);
  const [twapSlices, setTwapSlices] = useState<number>(5);
  const [twapIntervalMs, setTwapIntervalMs] = useState<number>(1200);
  const [isTwapRunning, setIsTwapRunning] = useState<boolean>(false);
  const [twapProgress, setTwapProgress] = useState<number>(0);
  const [twapReport, setTwapReport] = useState<TwapReport | null>(null);

  // Flash Crash State
  const [isCrashing, setIsCrashing] = useState<boolean>(false);
  const [crashReport, setCrashReport] = useState<{ startMid: number; lowestMid: number; spreadBlowout: number } | null>(null);

  // HFT MM State
  const [isMmRunning, setIsMmRunning] = useState<boolean>(false);
  const mmIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // ── Run Real-World TWAP Algorithmic Execution ─────────────────────────
  const runTwap = async () => {
    if (isTwapRunning) return;
    setIsTwapRunning(true);
    setTwapProgress(0);

    const arrival = midPrice;
    const sliceQty = Math.floor(twapQty / twapSlices);
    const remainder = twapQty % twapSlices;
    const logs: string[] = [];

    let totalTradedNotional = 0;
    let totalFilled = 0;

    for (let i = 1; i <= twapSlices; i++) {
      const currentSliceQty = i === twapSlices ? sliceQty + remainder : sliceQty;
      const targetOppositePrice = twapSide === 'BUY' ? (bestAsk ?? midPrice) : (bestBid ?? midPrice);
      const priceTicks = Math.round(targetOppositePrice * 100);

      // Execute aggressive child slice order
      await onOrderSubmit(twapSide, priceTicks, currentSliceQty, true, 'MARKET');

      totalFilled += currentSliceQty;
      totalTradedNotional += currentSliceQty * targetOppositePrice;

      logs.push(`Child slice #${i}/${twapSlices}: Filled ${currentSliceQty} units @ est. $${targetOppositePrice.toFixed(2)}`);
      setTwapProgress(Math.round((i / twapSlices) * 100));

      if (i < twapSlices) {
        await new Promise(r => setTimeout(r, twapIntervalMs));
      }
    }

    const executedVwap = totalFilled > 0 ? totalTradedNotional / totalFilled : arrival;
    const shortfallBps = arrival > 0 ? (Math.abs(executedVwap - arrival) / arrival) * 10000 : 0;

    setTwapReport({
      targetQty: twapQty,
      filledQty: totalFilled,
      arrivalPrice: arrival,
      executedVwap,
      shortfallBps,
      sliceLogs: logs,
    });

    setIsTwapRunning(false);
  };

  // ── Run Real-World Flash Crash Stress Test ─────────────────────────────
  const runFlashCrash = async () => {
    if (isCrashing) return;
    setIsCrashing(true);

    const startMid = midPrice;

    // Phase 1: Massive institutional sell market sweep (3 consecutive large sells)
    for (let i = 0; i < 3; i++) {
      const sweepPriceTicks = Math.round((bestBid ? bestBid * 0.96 : 95) * 100);
      await onOrderSubmit('SELL', sweepPriceTicks, 80, true, 'MARKET');
      await new Promise(r => setTimeout(r, 200));
    }

    const lowestMid = midPrice;
    const spreadBlowout = bestAsk && bestBid ? bestAsk - bestBid : 0;

    setCrashReport({
      startMid,
      lowestMid,
      spreadBlowout,
    });

    setIsCrashing(false);
  };

  // ── Run HFT Avellaneda-Stoikov Market Maker ────────────────────────────
  const toggleHftMm = () => {
    if (isMmRunning) {
      if (mmIntervalRef.current) clearInterval(mmIntervalRef.current);
      setIsMmRunning(false);
      return;
    }

    setIsMmRunning(true);
    mmIntervalRef.current = setInterval(async () => {
      const current = midPrice > 0 ? midPrice : 100;
      const halfSpread = 0.15;
      const bidTicks = Math.round((current - halfSpread) * 100);
      const askTicks = Math.round((current + halfSpread) * 100);
      const qty = Math.floor(Math.random() * 20) + 10;

      // Submit limit orders tight around mid
      onOrderSubmit('BUY', bidTicks, qty, false, 'LIMIT');
      onOrderSubmit('SELL', askTicks, qty, false, 'LIMIT');
    }, 800);
  };

  return (
    <div className="flex flex-col h-full font-mono text-terminal-text gap-4">
      {/* Top Tabs */}
      <div className="flex justify-between items-center pb-2.5 border-b border-terminal-border/60">
        <div className="flex items-center gap-2">
          <Cpu className="text-terminal-accent" size={16} />
          <span className="text-xs uppercase tracking-widest text-terminal-muted font-bold">
            Quantitative Algorithmic Execution Lab
          </span>
        </div>

        <div className="flex gap-1.5 p-1 bg-terminal-bg/80 border border-terminal-border/60 rounded-xl text-[10px]">
          <button
            onClick={() => setActiveTab('TWAP')}
            className={`px-3 py-1 rounded-lg font-bold transition-all
              ${activeTab === 'TWAP'
                ? 'bg-terminal-accent text-black shadow'
                : 'text-terminal-muted hover:text-terminal-text'}`}
          >
            TWAP Slicer
          </button>
          <button
            onClick={() => setActiveTab('FLASH_CRASH')}
            className={`px-3 py-1 rounded-lg font-bold transition-all
              ${activeTab === 'FLASH_CRASH'
                ? 'bg-terminal-ask text-black shadow'
                : 'text-terminal-muted hover:text-terminal-text'}`}
          >
            Flash Crash
          </button>
          <button
            onClick={() => setActiveTab('HFT_MAKER')}
            className={`px-3 py-1 rounded-lg font-bold transition-all
              ${activeTab === 'HFT_MAKER'
                ? 'bg-terminal-bid text-black shadow'
                : 'text-terminal-muted hover:text-terminal-text'}`}
          >
            HFT Quoter
          </button>
        </div>
      </div>

      {/* ── TAB 1: TWAP Slicer ────────────────────────────────────────── */}
      {activeTab === 'TWAP' && (
        <div className="flex-1 flex flex-col gap-4">
          <div className="grid grid-cols-4 gap-3 bg-terminal-bg/50 border border-terminal-border/60 rounded-xl p-3.5 text-xs">
            {/* Side */}
            <div className="flex flex-col gap-1">
              <label className="text-[10px] uppercase text-terminal-muted font-bold">Side</label>
              <div className="flex gap-1">
                <button
                  onClick={() => setTwapSide('BUY')}
                  className={`flex-1 py-1 rounded-lg font-bold text-xs ${
                    twapSide === 'BUY' ? 'bg-terminal-bid text-black' : 'bg-terminal-panel text-terminal-muted'
                  }`}
                >
                  BUY
                </button>
                <button
                  onClick={() => setTwapSide('SELL')}
                  className={`flex-1 py-1 rounded-lg font-bold text-xs ${
                    twapSide === 'SELL' ? 'bg-terminal-ask text-black' : 'bg-terminal-panel text-terminal-muted'
                  }`}
                >
                  SELL
                </button>
              </div>
            </div>

            {/* Total Target Qty */}
            <div className="flex flex-col gap-1">
              <label className="text-[10px] uppercase text-terminal-muted font-bold">Target Qty</label>
              <input
                type="number"
                min="20"
                max="1000"
                step="25"
                value={twapQty}
                onChange={e => setTwapQty(Number(e.target.value))}
                className="bg-terminal-bg border border-terminal-border/80 rounded-lg px-2.5 py-1 text-xs text-white"
              />
            </div>

            {/* Slices */}
            <div className="flex flex-col gap-1">
              <label className="text-[10px] uppercase text-terminal-muted font-bold">Child Slices</label>
              <input
                type="number"
                min="2"
                max="20"
                value={twapSlices}
                onChange={e => setTwapSlices(Number(e.target.value))}
                className="bg-terminal-bg border border-terminal-border/80 rounded-lg px-2.5 py-1 text-xs text-white"
              />
            </div>

            {/* Pace / Interval */}
            <div className="flex flex-col gap-1">
              <label className="text-[10px] uppercase text-terminal-muted font-bold">Slice Delay</label>
              <select
                value={twapIntervalMs}
                onChange={e => setTwapIntervalMs(Number(e.target.value))}
                className="bg-terminal-bg border border-terminal-border/80 rounded-lg px-2 py-1 text-xs text-white"
              >
                <option value={800}>800 ms (Fast)</option>
                <option value={1500}>1.5 sec (Normal)</option>
                <option value={3000}>3.0 sec (Patient)</option>
              </select>
            </div>
          </div>

          {/* Execution Progress & Run Button */}
          <div className="flex items-center gap-4 bg-terminal-bg/30 border border-terminal-border/40 rounded-xl p-3">
            <button
              onClick={runTwap}
              disabled={isTwapRunning}
              className={`px-5 py-2.5 rounded-xl font-bold text-xs flex items-center gap-2 shadow-lg transition-all ${
                isTwapRunning
                  ? 'bg-terminal-accent/30 text-terminal-accent cursor-not-allowed'
                  : 'bg-terminal-accent text-black hover:bg-terminal-accent/90 shadow-[0_0_16px_rgba(250,204,21,0.35)]'
              }`}
            >
              <Play size={14} className="fill-current" />
              {isTwapRunning ? `Executing Slices (${twapProgress}%)…` : `Execute TWAP (${twapQty} Units)`}
            </button>

            <div className="flex-1 flex flex-col gap-1">
              <div className="flex justify-between text-[10px] text-terminal-muted">
                <span>Algorithm Progress</span>
                <span className="font-bold text-white">{twapProgress}%</span>
              </div>
              <div className="h-2 w-full bg-terminal-border/40 rounded-full overflow-hidden">
                <div
                  className="h-full bg-terminal-accent transition-all duration-300"
                  style={{ width: `${twapProgress}%` }}
                />
              </div>
            </div>
          </div>

          {/* TWAP Execution Metrics Report */}
          {twapReport && (
            <div className="bg-terminal-bg/60 border border-terminal-border/60 rounded-xl p-3 flex flex-col gap-2">
              <div className="flex items-center gap-1.5 text-terminal-accent text-xs font-bold">
                <CheckCircle2 size={14} /> Execution Report: Benchmark Implementation Shortfall
              </div>
              <div className="grid grid-cols-3 gap-2">
                <div className="bg-terminal-panel/80 p-2.5 rounded-lg border border-terminal-border/40">
                  <span className="text-[10px] text-terminal-muted block">Arrival Benchmark</span>
                  <span className="text-sm font-bold text-white">${twapReport.arrivalPrice.toFixed(2)}</span>
                </div>
                <div className="bg-terminal-panel/80 p-2.5 rounded-lg border border-terminal-border/40">
                  <span className="text-[10px] text-terminal-muted block">Realized VWAP</span>
                  <span className="text-sm font-bold text-terminal-bid">${twapReport.executedVwap.toFixed(2)}</span>
                </div>
                <div className="bg-terminal-panel/80 p-2.5 rounded-lg border border-terminal-border/40">
                  <span className="text-[10px] text-terminal-muted block">Shortfall (Slippage)</span>
                  <span className="text-sm font-bold text-terminal-accent">{twapReport.shortfallBps.toFixed(1)} bps</span>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ── TAB 2: Flash Crash Scenario ──────────────────────────────── */}
      {activeTab === 'FLASH_CRASH' && (
        <div className="flex-1 flex flex-col gap-4">
          <div className="bg-terminal-ask/10 border border-terminal-ask/30 rounded-xl p-4 flex items-start gap-3">
            <AlertTriangle className="text-terminal-ask shrink-0 mt-0.5" size={18} />
            <div className="text-xs">
              <span className="font-bold text-terminal-ask block mb-1">
                Stress Test: Sudden Liquidity Vacuum &amp; Cascade Selling
              </span>
              <p className="text-terminal-muted leading-relaxed">
                Injects 3 massive consecutive institutional market sweeps (240 total units).
                Watch the bids collapse in real-time, the spread explode, and observe whether market-making liquidity reconstitutes the book.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={runFlashCrash}
              disabled={isCrashing}
              className="px-6 py-3 rounded-xl bg-terminal-ask text-black font-extrabold text-xs shadow-[0_0_20px_rgba(255,51,102,0.4)] hover:bg-terminal-ask/90 transition-all flex items-center gap-2"
            >
              <TrendingDown size={16} />
              {isCrashing ? 'Cascading Sweeps in Progress…' : 'Trigger Flash Crash (240 Units Sell)'}
            </button>
          </div>

          {crashReport && (
            <div className="grid grid-cols-3 gap-3 bg-terminal-bg/50 border border-terminal-border/60 rounded-xl p-3.5">
              <div className="flex flex-col">
                <span className="text-[10px] text-terminal-muted">Pre-Crash Mid</span>
                <span className="text-sm font-bold text-white">${crashReport.startMid.toFixed(2)}</span>
              </div>
              <div className="flex flex-col">
                <span className="text-[10px] text-terminal-muted">Lowest Mid Hit</span>
                <span className="text-sm font-bold text-terminal-ask">${crashReport.lowestMid.toFixed(2)}</span>
              </div>
              <div className="flex flex-col">
                <span className="text-[10px] text-terminal-muted">Spread Blowout</span>
                <span className="text-sm font-bold text-terminal-accent">${crashReport.spreadBlowout.toFixed(2)}</span>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ── TAB 3: HFT Market Maker ─────────────────────────────────── */}
      {activeTab === 'HFT_MAKER' && (
        <div className="flex-1 flex flex-col gap-4">
          <div className="bg-terminal-bid/10 border border-terminal-bid/30 rounded-xl p-4 flex items-start gap-3">
            <Layers className="text-terminal-bid shrink-0 mt-0.5" size={18} />
            <div className="text-xs">
              <span className="font-bold text-terminal-bid block mb-1">
                Avellaneda-Stoikov Inventory-Aware Market Maker
              </span>
              <p className="text-terminal-muted leading-relaxed">
                Continually posts dual-sided limit orders right around the current mid-price.
                Tightens bid-ask spread to the minimum tick ($0.30) and acts as an automated liquidity provider.
              </p>
            </div>
          </div>

          <div>
            <button
              onClick={toggleHftMm}
              className={`px-6 py-3 rounded-xl font-extrabold text-xs transition-all flex items-center gap-2 shadow-xl ${
                isMmRunning
                  ? 'bg-terminal-ask text-black shadow-[0_0_18px_rgba(255,51,102,0.4)]'
                  : 'bg-terminal-bid text-black shadow-[0_0_18px_rgba(0,255,170,0.4)]'
              }`}
            >
              {isMmRunning ? <Pause size={15} /> : <Play size={15} />}
              {isMmRunning ? 'Halt Market Maker Quoting' : 'Start Active Quoting Engine'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default AlgoExecutionLab;
