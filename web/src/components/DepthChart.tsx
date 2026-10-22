import React, { useEffect, useRef } from 'react';
import {
  createChart,
  IChartApi,
  ISeriesApi,
  LineData,
  ColorType,
} from 'lightweight-charts';

interface DepthChartProps {
  bids: { price: number; quantity: number }[];
  asks: { price: number; quantity: number }[];
}

const DepthChart: React.FC<DepthChartProps> = ({ bids, asks }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const bidSeriesRef = useRef<ISeriesApi<'Line'> | null>(null);
  const askSeriesRef = useRef<ISeriesApi<'Line'> | null>(null);

  // Create chart once
  useEffect(() => {
    if (!containerRef.current) return;

    const chart = createChart(containerRef.current, {
      layout: {
        background: { type: ColorType.Solid, color: 'transparent' },
        textColor: '#6b6b7c',
      },
      grid: {
        vertLines: { visible: false },
        horzLines: { color: '#1e1e24' },
      },
      rightPriceScale: { borderColor: '#1e1e24' },
      timeScale: { borderColor: '#1e1e24' },
      width: containerRef.current.clientWidth,
      height: containerRef.current.clientHeight || 240,
    });

    // lightweight-charts v5 uses addLineSeries instead of addAreaSeries for custom rendering
    const bidSeries = chart.addLineSeries({
      color: '#00ffaa',
      lineWidth: 2,
      priceLineVisible: false,
      lastValueVisible: false,
    });

    const askSeries = chart.addLineSeries({
      color: '#ff3366',
      lineWidth: 2,
      priceLineVisible: false,
      lastValueVisible: false,
    });

    chartRef.current = chart;
    bidSeriesRef.current = bidSeries;
    askSeriesRef.current = askSeries;

    const handleResize = () => {
      if (containerRef.current) {
        chart.applyOptions({ width: containerRef.current.clientWidth });
      }
    };
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      chart.remove();
      chartRef.current = null;
      bidSeriesRef.current = null;
      askSeriesRef.current = null;
    };
  }, []);

  // Update data whenever bids/asks change
  useEffect(() => {
    if (!bidSeriesRef.current || !askSeriesRef.current) return;
    if (bids.length === 0 && asks.length === 0) return;

    // Cumulative bid volume: sorted ascending by price
    let cumBidVol = 0;
    const bidData: LineData[] = [...bids]
      .sort((a, b) => a.price - b.price)
      .map(b => {
        cumBidVol += b.quantity;
        // lightweight-charts time axis expects number | string; use price as x-axis value
        return { time: b.price as unknown as number, value: cumBidVol };
      });

    // Cumulative ask volume: sorted ascending by price
    let cumAskVol = 0;
    const askData: LineData[] = [...asks]
      .sort((a, b) => a.price - b.price)
      .map(a => {
        cumAskVol += a.quantity;
        return { time: a.price as unknown as number, value: cumAskVol };
      });

    try {
      if (bidData.length > 0) bidSeriesRef.current.setData(bidData);
      if (askData.length > 0) askSeriesRef.current.setData(askData);
    } catch {
      // Silently ignore if chart was disposed during update
    }
  }, [bids, asks]);

  return <div ref={containerRef} className="w-full h-full min-h-[200px]" />;
};

export default DepthChart;
