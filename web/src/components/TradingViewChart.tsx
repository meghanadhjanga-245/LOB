import React, { useEffect, useRef, useState, useMemo } from 'react';
import {
  createChart,
  IChartApi,
  ISeriesApi,
  CandlestickSeries,
  HistogramSeries,
  CandlestickData,
  HistogramData,
  ColorType,
  Time,
} from 'lightweight-charts';
import { BarChart3, Clock, Maximize2 } from 'lucide-react';
import { Trade } from '../hooks/useMarketData';

interface TradingViewChartProps {
  trades: Trade[];
  currentPrice: number;
}

export const TradingViewChart: React.FC<TradingViewChartProps> = ({ trades, currentPrice }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const candleSeriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
  const volumeSeriesRef = useRef<ISeriesApi<'Histogram'> | null>(null);

  const [intervalSec, setIntervalSec] = useState<number>(1); // 1s, 5s, 15s

  // Candles & volume cache
  const candleMapRef = useRef<Map<number, { open: number; high: number; low: number; close: number; volume: number; isBuy: boolean }>>(new Map());

  // Initialize chart
  useEffect(() => {
    if (!containerRef.current) return;

    const chart = createChart(containerRef.current, {
      layout: {
        background: { type: ColorType.Solid, color: 'transparent' },
        textColor: '#8b8b9e',
        fontFamily: "'JetBrains Mono', monospace",
        fontSize: 11,
      },
      grid: {
        vertLines: { color: 'rgba(30, 30, 36, 0.45)' },
        horzLines: { color: 'rgba(30, 30, 36, 0.45)' },
      },
      crosshair: {
        vertLine: { color: 'rgba(250, 204, 21, 0.4)', width: 1, style: 2 },
        horzLine: { color: 'rgba(250, 204, 21, 0.4)', width: 1, style: 2 },
      },
      rightPriceScale: {
        borderColor: '#1e1e24',
        scaleMargins: { top: 0.1, bottom: 0.25 },
        autoScale: true,
      },
      timeScale: {
        borderColor: '#1e1e24',
        timeVisible: true,
        secondsVisible: true,
      },
      width: containerRef.current.clientWidth,
      height: containerRef.current.clientHeight || 280,
    });

    const candleSeries = chart.addSeries(CandlestickSeries, {
      upColor: '#00ffaa',
      downColor: '#ff3366',
      borderVisible: false,
      wickUpColor: '#00ffaa',
      wickDownColor: '#ff3366',
    });

    const volumeSeries = chart.addSeries(HistogramSeries, {
      priceFormat: { type: 'volume' },
      priceScaleId: '', // overlay on same pane
    });

    volumeSeries.priceScale().applyOptions({
      scaleMargins: { top: 0.8, bottom: 0 },
    });

    chartRef.current = chart;
    candleSeriesRef.current = candleSeries;
    volumeSeriesRef.current = volumeSeries;

    // Generate initial baseline seed candles if empty
    const nowSec = Math.floor(Date.now() / 1000);
    const initialCandles: CandlestickData<Time>[] = [];
    const initialVolumes: HistogramData<Time>[] = [];
    let p = currentPrice > 0 ? currentPrice : 100;

    for (let i = 40; i >= 0; i--) {
      const t = (nowSec - i * intervalSec) as Time;
      const variation = (Math.random() - 0.49) * 0.4;
      const open = p;
      const close = parseFloat((open + variation).toFixed(2));
      const high = parseFloat((Math.max(open, close) + Math.random() * 0.2).toFixed(2));
      const low = parseFloat((Math.min(open, close) - Math.random() * 0.2).toFixed(2));
      const vol = Math.floor(Math.random() * 60) + 15;
      const isUp = close >= open;

      candleMapRef.current.set(t as number, { open, high, low, close, volume: vol, isBuy: isUp });

      initialCandles.push({ time: t, open, high, low, close });
      initialVolumes.push({
        time: t,
        value: vol,
        color: isUp ? 'rgba(0, 255, 170, 0.4)' : 'rgba(255, 51, 102, 0.4)',
      });

      p = close;
    }

    candleSeries.setData(initialCandles);
    volumeSeries.setData(initialVolumes);

    const handleResize = () => {
      if (containerRef.current && chartRef.current) {
        chartRef.current.applyOptions({
          width: containerRef.current.clientWidth,
          height: containerRef.current.clientHeight || 280,
        });
      }
    };

    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      chart.remove();
      chartRef.current = null;
      candleSeriesRef.current = null;
      volumeSeriesRef.current = null;
    };
  }, [intervalSec]);

  // Push new trades to candle series
  useEffect(() => {
    if (!candleSeriesRef.current || !volumeSeriesRef.current || trades.length === 0) return;

    const latest = trades[0];
    if (!latest) return;

    const tradeSec = Math.floor(latest.timestamp / 1000);
    const bucketTime = (tradeSec - (tradeSec % intervalSec)) as Time;
    const bucketKey = bucketTime as number;

    const existing = candleMapRef.current.get(bucketKey);

    if (existing) {
      existing.high = Math.max(existing.high, latest.price);
      existing.low = Math.min(existing.low, latest.price);
      existing.close = latest.price;
      existing.volume += latest.quantity;
      existing.isBuy = latest.side === 'BUY';

      candleSeriesRef.current.update({
        time: bucketTime,
        open: existing.open,
        high: existing.high,
        low: existing.low,
        close: existing.close,
      });

      volumeSeriesRef.current.update({
        time: bucketTime,
        value: existing.volume,
        color: existing.isBuy ? 'rgba(0, 255, 170, 0.4)' : 'rgba(255, 51, 102, 0.4)',
      });
    } else {
      const prevClose = currentPrice > 0 ? currentPrice : latest.price;
      const newCandle = {
        open: prevClose,
        high: Math.max(prevClose, latest.price),
        low: Math.min(prevClose, latest.price),
        close: latest.price,
        volume: latest.quantity,
        isBuy: latest.side === 'BUY',
      };
      candleMapRef.current.set(bucketKey, newCandle);

      candleSeriesRef.current.update({
        time: bucketTime,
        open: newCandle.open,
        high: newCandle.high,
        low: newCandle.low,
        close: newCandle.close,
      });

      volumeSeriesRef.current.update({
        time: bucketTime,
        value: newCandle.volume,
        color: newCandle.isBuy ? 'rgba(0, 255, 170, 0.4)' : 'rgba(255, 51, 102, 0.4)',
      });
    }
  }, [trades, currentPrice, intervalSec]);

  return (
    <div className="flex flex-col h-full font-mono text-terminal-text gap-3">
      {/* Chart Top Toolbar */}
      <div className="flex justify-between items-center pb-2.5 border-b border-terminal-border/60">
        <div className="flex items-center gap-2">
          <BarChart3 className="text-terminal-accent" size={16} />
          <span className="text-xs uppercase tracking-widest text-terminal-muted font-bold">
            Real-Time Candlestick &amp; Volume Engine
          </span>
        </div>

        {/* Timeframe Interval Selector */}
        <div className="flex items-center gap-1.5 bg-terminal-bg/80 border border-terminal-border/60 rounded-xl p-1 text-[10px]">
          <Clock size={12} className="text-terminal-muted ml-1" />
          {[1, 5, 15].map(sec => (
            <button
              key={sec}
              onClick={() => {
                candleMapRef.current.clear();
                setIntervalSec(sec);
              }}
              className={`px-2.5 py-1 rounded-lg font-bold transition-all
                ${intervalSec === sec
                  ? 'bg-terminal-accent text-black shadow'
                  : 'text-terminal-muted hover:text-terminal-text'}`}
            >
              {sec}s
            </button>
          ))}
        </div>
      </div>

      {/* Chart Container */}
      <div className="relative flex-1 min-h-[260px] bg-terminal-bg/50 rounded-xl border border-terminal-border/40 overflow-hidden p-2">
        <div ref={containerRef} className="w-full h-full min-h-[250px]" />
      </div>

      {/* Chart Footer Info */}
      <div className="flex justify-between items-center text-[10px] text-terminal-muted px-1">
        <span>Series: <strong className="text-terminal-text">LOB/USD Continuous Execution</strong></span>
        <div className="flex items-center gap-3">
          <span className="flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-terminal-bid" /> Bullish Candle
          </span>
          <span className="flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-terminal-ask" /> Bearish Candle
          </span>
        </div>
      </div>
    </div>
  );
};

export default TradingViewChart;
