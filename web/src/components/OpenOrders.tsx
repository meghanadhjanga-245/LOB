import React, { useState } from 'react';
import { X, Trash2, ListChecks, CheckCircle2, Clock } from 'lucide-react';

export interface UserOrder {
  orderId: string;
  side: 'BUY' | 'SELL';
  price: number;
  quantity: number;
  orderType: string;
  timestamp: number;
  status: 'OPEN' | 'FILLED' | 'CANCELLED';
  filledQty?: number;
}

interface OpenOrdersProps {
  orders: UserOrder[];
  onCancelOrder: (orderId: string) => Promise<void>;
  onCancelAll: () => Promise<void>;
}

export const OpenOrders: React.FC<OpenOrdersProps> = ({ orders, onCancelOrder, onCancelAll }) => {
  const [tab, setTab] = useState<'OPEN' | 'HISTORY'>('OPEN');
  const [cancellingId, setCancellingId] = useState<string | null>(null);

  const openOrders = orders.filter(o => o.status === 'OPEN');
  const historyOrders = orders.filter(o => o.status !== 'OPEN');

  const handleCancel = async (id: string) => {
    setCancellingId(id);
    try {
      await onCancelOrder(id);
    } finally {
      setCancellingId(null);
    }
  };

  return (
    <div className="bg-terminal-panel/90 backdrop-blur-md border border-terminal-border/80 rounded-2xl overflow-hidden font-mono shadow-2xl flex flex-col">
      {/* Header Tabs */}
      <div className="flex justify-between items-center px-4 py-3 border-b border-terminal-border/80 bg-terminal-bg/80">
        <div className="flex items-center gap-2">
          <ListChecks size={15} className="text-terminal-accent" />
          <button
            onClick={() => setTab('OPEN')}
            className={`text-xs font-bold transition-all px-2.5 py-1 rounded-lg flex items-center gap-1.5
              ${tab === 'OPEN'
                ? 'bg-terminal-border/80 text-white'
                : 'text-terminal-muted hover:text-terminal-text'}`}
          >
            <span>Open Orders</span>
            <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-bold
              ${openOrders.length > 0 ? 'bg-terminal-accent text-black' : 'bg-terminal-border text-terminal-muted'}`}>
              {openOrders.length}
            </span>
          </button>

          <button
            onClick={() => setTab('HISTORY')}
            className={`text-xs font-bold transition-all px-2.5 py-1 rounded-lg flex items-center gap-1.5
              ${tab === 'HISTORY'
                ? 'bg-terminal-border/80 text-white'
                : 'text-terminal-muted hover:text-terminal-text'}`}
          >
            <span>Order History</span>
            <span className="text-[10px] px-1.5 py-0.2 rounded-full bg-terminal-border text-terminal-muted font-bold">
              {historyOrders.length}
            </span>
          </button>
        </div>

        {tab === 'OPEN' && openOrders.length > 0 && (
          <button
            onClick={onCancelAll}
            className="text-[11px] font-bold text-terminal-ask hover:text-white bg-terminal-ask/10 hover:bg-terminal-ask/20 border border-terminal-ask/30 px-2.5 py-1 rounded-lg transition-colors flex items-center gap-1.5"
          >
            <Trash2 size={12} /> Cancel All ({openOrders.length})
          </button>
        )}
      </div>

      {/* Table Content */}
      <div className="overflow-x-auto max-h-[160px] custom-scrollbar">
        {tab === 'OPEN' ? (
          openOrders.length === 0 ? (
            <div className="py-7 text-center text-xs text-terminal-muted flex flex-col items-center gap-1">
              <span>No active resting orders in the book.</span>
              <span className="text-[10px] text-terminal-muted/60">Place a Limit or Iceberg order to track it live here.</span>
            </div>
          ) : (
            <table className="w-full text-left text-xs">
              <thead className="text-[10px] uppercase tracking-wider text-terminal-muted bg-terminal-panel/60 border-b border-terminal-border/40">
                <tr>
                  <th className="px-4 py-2">Order ID</th>
                  <th className="px-3 py-2">Type</th>
                  <th className="px-3 py-2">Side</th>
                  <th className="px-3 py-2">Price</th>
                  <th className="px-3 py-2">Quantity</th>
                  <th className="px-3 py-2">Time</th>
                  <th className="px-4 py-2 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-terminal-border/20">
                {openOrders.map(order => {
                  const isBuy = order.side === 'BUY';
                  return (
                    <tr key={order.orderId} className="hover:bg-white/5 transition-colors">
                      <td className="px-4 py-2 text-terminal-muted font-mono text-[11px]">
                        {order.orderId}
                      </td>
                      <td className="px-3 py-2">
                        <span className="text-[10px] bg-terminal-border/60 text-terminal-text px-1.5 py-0.5 rounded font-bold">
                          {order.orderType}
                        </span>
                      </td>
                      <td className="px-3 py-2">
                        <span className={`text-[10px] font-black px-2 py-0.5 rounded ${
                          isBuy ? 'text-terminal-bid bg-terminal-bid/10' : 'text-terminal-ask bg-terminal-ask/10'
                        }`}>
                          {order.side}
                        </span>
                      </td>
                      <td className="px-3 py-2 font-bold tabular-nums">
                        ${order.price.toFixed(2)}
                      </td>
                      <td className="px-3 py-2 tabular-nums">
                        {order.quantity.toLocaleString()}
                      </td>
                      <td className="px-3 py-2 text-terminal-muted text-[11px]">
                        {new Date(order.timestamp).toLocaleTimeString([], { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                      </td>
                      <td className="px-4 py-2 text-right">
                        <button
                          onClick={() => handleCancel(order.orderId)}
                          disabled={cancellingId === order.orderId}
                          className="px-2 py-0.5 rounded bg-terminal-ask/10 text-terminal-ask hover:bg-terminal-ask hover:text-black border border-terminal-ask/30 text-[10px] font-bold transition-all disabled:opacity-50"
                        >
                          {cancellingId === order.orderId ? 'Cancelling...' : 'Cancel'}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )
        ) : (
          historyOrders.length === 0 ? (
            <div className="py-7 text-center text-xs text-terminal-muted">
              No closed order history in this session.
            </div>
          ) : (
            <table className="w-full text-left text-xs">
              <thead className="text-[10px] uppercase tracking-wider text-terminal-muted bg-terminal-panel/60 border-b border-terminal-border/40">
                <tr>
                  <th className="px-4 py-2">Order ID</th>
                  <th className="px-3 py-2">Type</th>
                  <th className="px-3 py-2">Side</th>
                  <th className="px-3 py-2">Price</th>
                  <th className="px-3 py-2">Qty</th>
                  <th className="px-3 py-2">Status</th>
                  <th className="px-4 py-2 text-right">Time</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-terminal-border/20">
                {historyOrders.map(order => {
                  const isBuy = order.side === 'BUY';
                  return (
                    <tr key={order.orderId} className="hover:bg-white/5 transition-colors opacity-75">
                      <td className="px-4 py-2 text-terminal-muted font-mono text-[11px]">
                        {order.orderId}
                      </td>
                      <td className="px-3 py-2 text-[10px] text-terminal-muted">
                        {order.orderType}
                      </td>
                      <td className="px-3 py-2 font-bold text-[10px]">
                        <span className={isBuy ? 'text-terminal-bid' : 'text-terminal-ask'}>{order.side}</span>
                      </td>
                      <td className="px-3 py-2 tabular-nums">
                        ${order.price.toFixed(2)}
                      </td>
                      <td className="px-3 py-2 tabular-nums">
                        {order.quantity.toLocaleString()}
                      </td>
                      <td className="px-3 py-2">
                        <span className={`text-[10px] px-1.5 py-0.5 rounded font-bold ${
                          order.status === 'FILLED'
                            ? 'text-terminal-bid bg-terminal-bid/10'
                            : 'text-terminal-muted bg-terminal-border/40'
                        }`}>
                          {order.status}
                        </span>
                      </td>
                      <td className="px-4 py-2 text-right text-terminal-muted text-[11px]">
                        {new Date(order.timestamp).toLocaleTimeString([], { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )
        )}
      </div>
    </div>
  );
};

export default OpenOrders;
