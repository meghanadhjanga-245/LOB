package com.lobsimulator.service.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

/**
 * Binary encoder for Order Book updates to reduce WebSocket payload size.
 * Format: [Side(1b)][Count(4b)][(Price(8b), Qty(4b), ...)]
 */
public class BinaryEncoder {
    public static byte[] encodeLevels(boolean isBuySide, Map<Long, Integer> levels) {
        int count = levels.size();
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + (count * (8 + 4)));
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        buffer.put((byte) (isBuySide ? 1 : 0));
        buffer.putInt(count);
        
        for (Map.Entry<Long, Integer> entry : levels.entrySet()) {
            buffer.putLong(entry.getKey());
            buffer.putInt(entry.getValue());
        }
        
        return buffer.array();
    }
}
