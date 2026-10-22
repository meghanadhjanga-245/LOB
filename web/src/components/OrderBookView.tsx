import React, { useState, useEffect } from 'react';
import { Paper, Box, Typography, Table, TableHead, TableRow, TableCell, TableBody } from '@mui/material';

const OrderBookView: React.FC = () => {
  const [bestBid, setBestBid] = useState<{ price: number | null; quantity: number } | null>(null);
  const [bestAsk, setBestAsk] = useState<{ price: number | null; quantity: number } | null>(null);
  const [spread, setSpread] = useState<number>(0);

  useEffect(() => {
    // In a real app, we would fetch data from the backend API or WebSocket
    // For now, we'll simulate some data
    const updateOrderBookData = () => {
      // Simulate fetching order book data
      // This would be replaced with actual API calls
      const bidPrice = Math.floor(Math.random() * 10) + 95; // 95-104
      const bidQty = Math.floor(Math.random() * 100) + 10; // 10-109
      const askPrice = bidPrice + Math.floor(Math.random() * 5) + 1; // at least 1 tick above bid
      const askQty = Math.floor(Math.random() * 100) + 10; // 10-109

      setBestBid({ price: bidPrice, quantity: bidQty });
      setBestAsk({ price: askPrice, quantity: askQty });
      setSpread(askPrice - bidPrice);
    };

    const interval = setInterval(updateOrderBookData, 1000); // Update every second
    updateOrderBookData(); // Initial update

    return () => clearInterval(interval);
  }, []);

  return (
    <Paper sx={{ p: 3, mb: 3 }}>
      <Typography variant="h5" gutterBottom>
        Order Book
      </Typography>
      <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
        <Box sx={{ flex: 1, minWidth: 200 }}>
          <Typography variant="h6">Bids</Typography>
          {bestBid ? (
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Price</TableCell>
                  <TableCell>Quantity</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                <TableRow>
                  <TableCell>{bestBid.price}</TableCell>
                  <TableCell>{bestBid.quantity}</TableCell>
                </TableRow>
              </TableBody>
            </Table>
          ) : (
            <Typography>No bids</Typography>
          )}
        </Box>

        <Box sx={{ flex: 1, minWidth: 200 }}>
          <Typography variant="h6">Asks</Typography>
          {bestAsk ? (
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Price</TableCell>
                  <TableCell>Quantity</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                <TableRow>
                  <TableCell>{bestAsk.price}</TableCell>
                  <TableCell>{bestAsk.quantity}</TableCell>
                </TableRow>
              </TableBody>
            </Table>
          ) : (
            <Typography>No asks</Typography>
          )}
        </Box>
      </Box>

      <Box sx={{ mt: 2 }}>
        <Typography variant="h6">Spread: {spread} ticks</Typography>
      </Box>
    </Paper>
  );
};

export default OrderBookView;