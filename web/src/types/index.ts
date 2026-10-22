export interface OrderBookData {
  bestBid: { price: number | null; quantity: number } | null;
  bestAsk: { price: number | null; quantity: number } | null;
  spread: number;
}

export interface MarketImpactPoint {
  size: number;
  cost: number;
}

export interface Trade {
  price: number;
  quantity: number;
  aggressorBuySide: boolean;
  makerOrderId: string;
  takerOrderId: string;
  timestamp: number;
}