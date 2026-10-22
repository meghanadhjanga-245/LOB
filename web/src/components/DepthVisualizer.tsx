import React, { useMemo } from 'react';
import { Layers } from 'lucide-react';

interface DepthVisualizerProps {
  bids: { price: number; quantity: number }[];
  asks: { price: number; quantity: number }[];
  onPriceSelect?: (price: number) => void;
}

export const DepthVisualizer: React.FC<DepthVisualizerProps> = ({ bids, asks, onPriceSelect }) => {
  // Sort bids descending by price for cumulative calc, but ascending for chart left-to-right
  const { bidPoints, askPoints, maxVolume, minPrice, maxPrice, midPrice } = useMemo(() => {
    if (bids.length === 0 && asks.length === 0) {
      return { bidPoints: [], askPoints: [], maxVolume: 1, minPrice: 90, maxPrice: 110, midPrice: 100 };
    }

    // Cumulative bids: from best bid downwards
    const sortedBids = [...bids].sort((a, b) => b.price - a.price);
    let cumBid = 0;
    const bidCum = sortedBids.map(b => {
      cumBid += b.quantity;
      return { price: b.price, cumVolume: cumBid };
    }).reverse(); // Now sorted ascending price for SVG left-to-right

    // Cumulative asks: from best ask upwards
    const sortedAsks = [...asks].sort((a, b) => a.price - b.price);
    let cumAsk = 0;
    const askCum = sortedAsks.map(a => {
      cumAsk += a.quantity;
      return { price: a.price, cumVolume: cumAsk };
    });

    const maxVol = Math.max(cumBid, cumAsk, 1);
    const minP = bidCum[0]?.price ?? (askCum[0]?.price ? askCum[0].price * 0.95 : 90);
    const maxP = askCum[askCum.length - 1]?.price ?? (bidCum[bidCum.length - 1]?.price ? bidCum[bidCum.length - 1].price * 1.05 : 110);
    const bestBid = sortedBids[0]?.price;
    const bestAsk = sortedAsks[0]?.price;
    const midP = bestBid && bestAsk ? (bestBid + bestAsk) / 2 : (bestBid ?? bestAsk ?? 100);

    return {
      bidPoints: bidCum,
      askPoints: askCum,
      maxVolume: maxVol,
      minPrice: minP,
      maxPrice: maxP,
      midPrice: midP,
    };
  }, [bids, asks]);

  const width = 600;
  const height = 260;
  const padding = { top: 20, bottom: 30, left: 10, right: 10 };
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;

  const priceToX = (price: number) => {
    if (maxPrice === minPrice) return chartWidth / 2;
    return padding.left + ((price - minPrice) / (maxPrice - minPrice)) * chartWidth;
  };

  const volumeToY = (vol: number) => {
    return padding.top + chartHeight - (vol / maxVolume) * chartHeight;
  };

  // Generate SVG path strings
  const bidPath = useMemo(() => {
    if (bidPoints.length === 0) return '';
    const startX = priceToX(bidPoints[0].price);
    const bottomY = padding.top + chartHeight;
    let d = `M ${startX} ${bottomY}`;

    bidPoints.forEach(pt => {
      const x = priceToX(pt.price);
      const y = volumeToY(pt.cumVolume);
      d += ` L ${x} ${y}`;
    });

    // Close down to baseline
    const lastX = priceToX(bidPoints[bidPoints.length - 1].price);
    d += ` L ${lastX} ${bottomY} Z`;
    return d;
  }, [bidPoints, maxVolume, minPrice, maxPrice]);

  const askPath = useMemo(() => {
    if (askPoints.length === 0) return '';
    const bottomY = padding.top + chartHeight;
    const firstX = priceToX(askPoints[0].price);
    let d = `M ${firstX} ${bottomY}`;

    askPoints.forEach(pt => {
      const x = priceToX(pt.price);
      const y = volumeToY(pt.cumVolume);
      d += ` L ${x} ${y}`;
    });

    const lastX = priceToX(askPoints[askPoints.length - 1].price);
    d += ` L ${lastX} ${bottomY} Z`;
    return d;
  }, [askPoints, maxVolume, minPrice, maxPrice]);

  return (
    <div className="flex flex-col h-full font-mono">
      <div className="flex justify-between items-center mb-3">
        <div className="flex items-center gap-2">
          <Layers className="text-terminal-accent" size={16} />
          <span className="text-xs uppercase tracking-widest text-terminal-muted font-bold">
            Cumulative Market Depth
          </span>
        </div>
        <div className="flex items-center gap-4 text-[11px]">
          <span className="flex items-center gap-1.5 text-terminal-bid">
            <span className="w-2.5 h-2.5 rounded-full bg-terminal-bid/20 border border-terminal-bid inline-block" />
            Bids (Buy Pressure)
          </span>
          <span className="flex items-center gap-1.5 text-terminal-ask">
            <span className="w-2.5 h-2.5 rounded-full bg-terminal-ask/20 border border-terminal-ask inline-block" />
            Asks (Sell Resistance)
          </span>
        </div>
      </div>

      <div className="relative w-full flex-1 bg-terminal-bg/60 border border-terminal-border/60 rounded-xl overflow-hidden p-2">
        {bids.length === 0 && asks.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-48 text-terminal-muted text-xs gap-2">
            <span>Order book has no resting liquidity.</span>
            <span className="text-[10px] text-terminal-accent">Click &quot;Seed Liquidity&quot; or place an order to visualize depth.</span>
          </div>
        ) : (
          <svg viewBox={`0 0 ${width} ${height}`} className="w-full h-full preserve-3d">
            <defs>
              <linearGradient id="bidGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#00ffaa" stopOpacity="0.45" />
                <stop offset="100%" stopColor="#00ffaa" stopOpacity="0.02" />
              </linearGradient>
              <linearGradient id="askGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#ff3366" stopOpacity="0.45" />
                <stop offset="100%" stopColor="#ff3366" stopOpacity="0.02" />
              </linearGradient>
            </defs>

            {/* Grid lines */}
            <line
              x1={padding.left}
              y1={padding.top + chartHeight / 2}
              x2={width - padding.right}
              y2={padding.top + chartHeight / 2}
              stroke="#1e1e24"
              strokeDasharray="3 3"
            />
            <line
              x1={priceToX(midPrice)}
              y1={padding.top}
              x2={priceToX(midPrice)}
              y2={padding.top + chartHeight}
              stroke="#facc15"
              strokeDasharray="2 2"
              strokeOpacity="0.6"
            />

            {/* Bid area & line */}
            {bidPath && (
              <>
                <path d={bidPath} fill="url(#bidGrad)" />
                <path
                  d={bidPath.replace(/ L [^ ]+ [^ ]+ Z$/, '')}
                  fill="none"
                  stroke="#00ffaa"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                />
              </>
            )}

            {/* Ask area & line */}
            {askPath && (
              <>
                <path d={askPath} fill="url(#askGrad)" />
                <path
                  d={askPath.replace(/ L [^ ]+ [^ ]+ Z$/, '')}
                  fill="none"
                  stroke="#ff3366"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                />
              </>
            )}

            {/* Price markers on X axis */}
            <text x={padding.left} y={height - 8} fill="#6b6b7c" fontSize="10" textAnchor="start">
              ${minPrice.toFixed(2)}
            </text>
            <text x={priceToX(midPrice)} y={height - 8} fill="#facc15" fontSize="10" textAnchor="middle" fontWeight="bold">
              Mid: ${midPrice.toFixed(2)}
            </text>
            <text x={width - padding.right} y={height - 8} fill="#6b6b7c" fontSize="10" textAnchor="end">
              ${maxPrice.toFixed(2)}
            </text>

            {/* Volume marker on Y axis */}
            <text x={padding.left + 6} y={padding.top + 10} fill="#6b6b7c" fontSize="9" textAnchor="start">
              Max Vol: {maxVolume.toLocaleString()}
            </text>
          </svg>
        )}
      </div>
    </div>
  );
};

export default DepthVisualizer;
