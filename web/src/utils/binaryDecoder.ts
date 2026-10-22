export const decodeLOBBinary = (buffer) => {
  const view = new DataView(buffer);
  const isBuySide = view.getUint8(0) === 1;
  const count = view.getInt32(1, true);
  const levels = [];
  
  for (let i = 0; i < count; i++) {
    const offset = 5 + (i * 12);
    const price = view.getBigInt64(offset, true);
    const qty = view.getInt32(offset + 8, true);
    levels.push({ price: Number(price), quantity: qty });
  }
  
  return { isBuySide, levels };
};
