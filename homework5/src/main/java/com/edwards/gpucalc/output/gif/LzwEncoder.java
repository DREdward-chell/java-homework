package com.edwards.gpucalc.output.gif;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public final class LzwEncoder {

    static final int MAX_CODE_WIDTH = 12;
    static final int MAX_CODE_VALUE = (1 << MAX_CODE_WIDTH) - 1;
    private static final int MAX_SUB_BLOCK = 255;

    private final int minCodeSize;
    private final int clearCode;
    private final int endOfInfo;

    private final byte[] subBlock = new byte[MAX_SUB_BLOCK];
    private int subBlockLen;

    private int bitBuffer;
    private int bitCount;

    private final Map<Long, Integer> dictionary = new HashMap<>();
    private int nextCode;
    private int codeWidth;

    public LzwEncoder(int minCodeSize) {
        if (minCodeSize < 2 || minCodeSize > 8) {
            throw new IllegalArgumentException("minCodeSize must be in [2,8]; got " + minCodeSize);
        }
        this.minCodeSize = minCodeSize;
        this.clearCode = 1 << minCodeSize;
        this.endOfInfo = clearCode + 1;
    }

    public void encode(byte[] data, OutputStream out) throws IOException {
        out.write(minCodeSize);

        resetDictionary();
        writeCode(clearCode, out);

        if (data.length == 0) {
            writeCode(endOfInfo, out);
            flushBits(out);
            flushSubBlock(out);
            out.write(0);
            return;
        }

        int prefix = data[0] & 0xFF;
        for (int i = 1; i < data.length; i++) {
            int k = data[i] & 0xFF;
            long key = ((long) prefix << 16) | k;
            Integer existing = dictionary.get(key);
            if (existing != null) {
                prefix = existing;
                continue;
            }
            writeCode(prefix, out);
            if (nextCode <= MAX_CODE_VALUE) {
                dictionary.put(key, nextCode);
                if (nextCode == (1 << codeWidth) && codeWidth < MAX_CODE_WIDTH) {
                    codeWidth++;
                }
                nextCode++;
            } else {
                writeCode(clearCode, out);
                resetDictionary();
            }
            prefix = k;
        }
        writeCode(prefix, out);
        writeCode(endOfInfo, out);
        flushBits(out);
        flushSubBlock(out);
        out.write(0);
    }

    private void resetDictionary() {
        dictionary.clear();
        nextCode = endOfInfo + 1;
        codeWidth = minCodeSize + 1;
    }

    private void writeCode(int code, OutputStream out) throws IOException {
        bitBuffer |= (code & ((1 << codeWidth) - 1)) << bitCount;
        bitCount += codeWidth;
        while (bitCount >= 8) {
            emitByte(bitBuffer & 0xFF, out);
            bitBuffer >>>= 8;
            bitCount -= 8;
        }
    }

    private void flushBits(OutputStream out) throws IOException {
        if (bitCount > 0) {
            emitByte(bitBuffer & 0xFF, out);
            bitBuffer = 0;
            bitCount = 0;
        }
    }

    private void emitByte(int b, OutputStream out) throws IOException {
        subBlock[subBlockLen++] = (byte) b;
        if (subBlockLen == MAX_SUB_BLOCK) {
            flushSubBlock(out);
        }
    }

    private void flushSubBlock(OutputStream out) throws IOException {
        if (subBlockLen == 0) return;
        out.write(subBlockLen);
        out.write(subBlock, 0, subBlockLen);
        subBlockLen = 0;
    }
}
