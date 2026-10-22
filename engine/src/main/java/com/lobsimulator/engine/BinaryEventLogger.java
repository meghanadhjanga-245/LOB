package com.lobsimulator.engine;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;

/**
 * High-performance binary logger for TradeEvents.
 * Format per event (45 bytes):
 *   Price     (long):  8 bytes
 *   Quantity  (int):   4 bytes
 *   Side      (byte):  1 byte  (1=Buy, 0=Sell)
 *   MakerID   (long):  8 bytes (hashed)
 *   TakerID   (long):  8 bytes (hashed)
 *   Timestamp (long):  8 bytes
 *   MakerFee  (long):  8 bytes
 *   TakerFee  (long):  8 bytes  → total 53 bytes per record
 */
public class BinaryEventLogger implements AutoCloseable {
    private static final int RECORD_SIZE = 53;
    private static final int BUFFER_SIZE = 64 * 1024; // 64 KB

    private final FileChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

    public BinaryEventLogger(String filePath) throws IOException {
        this.channel = FileChannel.open(
            Paths.get(filePath),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND);
    }

    public synchronized void logTrade(TradeEvent event) throws IOException {
        if (buffer.remaining() < RECORD_SIZE) {
            flush();
        }
        buffer.putLong(event.getPrice());
        buffer.putInt(event.getQty());                       // fixed: was getQuantity()
        buffer.put((byte) (event.isBuySide() ? 1 : 0));     // fixed: was isAggressorBuySide()
        buffer.putLong(hashOrderId(event.getMakerId()));     // fixed: was getMakerOrderId()
        buffer.putLong(hashOrderId(event.getTakerId()));     // fixed: was getTakerOrderId()
        buffer.putLong(event.getTimestamp());
        buffer.putLong(event.getMakerFee());
        buffer.putLong(event.getTakerFee());
    }

    private static long hashOrderId(String id) {
        if (id == null) return 0L;
        // FNV-1a-inspired inline hash — faster than String.hashCode() and 64-bit
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < id.length(); i++) {
            h ^= id.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }

    public synchronized void flush() throws IOException {
        buffer.flip();
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
        buffer.clear();
    }

    @Override
    public void close() throws IOException {
        flush();
        channel.close();
    }
}
