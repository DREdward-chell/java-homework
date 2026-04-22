package com.edwards.gpucalc.output.gif;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LzwEncoderTest {

    @Test
    void roundTripsSmallAsciiSequence() throws IOException {
        byte[] data = "ABABABABABABCCCAAA".getBytes();
        byte[] decoded = encodeAndDecode(data, 8);
        assertThat(decoded).isEqualTo(data);
    }

    @Test
    void roundTripsEmptyInput() throws IOException {
        byte[] decoded = encodeAndDecode(new byte[0], 2);
        assertThat(decoded).isEmpty();
    }

    @Test
    void roundTripsSingleByte() throws IOException {
        byte[] decoded = encodeAndDecode(new byte[]{0x03}, 2);
        assertThat(decoded).containsExactly(0x03);
    }

    @Test
    void roundTripsLargeRandomInputForcingDictionaryReset() throws IOException {
        Random rng = new Random(42);
        byte[] data = new byte[20_000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) rng.nextInt(256);
        }
        byte[] decoded = encodeAndDecode(data, 8);
        assertThat(decoded).isEqualTo(data);
    }

    @Test
    void rejectsMinCodeSizeBelowTwo() {
        assertThatThrownBy(() -> new LzwEncoder(1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMinCodeSizeAboveEight() {
        assertThatThrownBy(() -> new LzwEncoder(9))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void outputIsValidGifSubBlockStream() throws IOException {
        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i & 0xFF);
        LzwEncoder encoder = new LzwEncoder(8);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        encoder.encode(data, buf);
        byte[] out = buf.toByteArray();
        assertThat(out[0]).isEqualTo((byte) 8);
        int p = 1;
        int totalPayload = 0;
        while (true) {
            int len = out[p++] & 0xFF;
            if (len == 0) break;
            assertThat(len).isLessThanOrEqualTo(255);
            p += len;
            totalPayload += len;
        }
        assertThat(p).isEqualTo(out.length);
        assertThat(totalPayload).isGreaterThan(0);
    }

    private static byte[] encodeAndDecode(byte[] data, int minCodeSize) throws IOException {
        LzwEncoder encoder = new LzwEncoder(minCodeSize);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        encoder.encode(data, buf);
        return GifLzwDecoder.decode(new ByteArrayInputStream(buf.toByteArray()), data.length);
    }

    /**
     * Reference GIF-flavored LZW decoder used only by the test. Mirrors the encoder exactly.
     */
    static final class GifLzwDecoder {
        static byte[] decode(InputStream in, int expectedLen) throws IOException {
            int minCodeSize = in.read();
            int clearCode = 1 << minCodeSize;
            int eoi = clearCode + 1;

            byte[] data = readSubBlocks(in);

            int codeWidth = minCodeSize + 1;
            int nextCode = eoi + 1;
            int[] prefix = new int[4096];
            int[] suffix = new int[4096];
            for (int i = 0; i < clearCode; i++) {
                prefix[i] = -1;
                suffix[i] = i;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream(expectedLen);
            int bitBuffer = 0;
            int bitCount = 0;
            int byteIdx = 0;
            int prevCode = -1;

            while (true) {
                while (bitCount < codeWidth) {
                    if (byteIdx >= data.length) throw new IOException("stream truncated");
                    bitBuffer |= (data[byteIdx++] & 0xFF) << bitCount;
                    bitCount += 8;
                }
                int code = bitBuffer & ((1 << codeWidth) - 1);
                bitBuffer >>>= codeWidth;
                bitCount -= codeWidth;

                if (code == eoi) break;
                if (code == clearCode) {
                    codeWidth = minCodeSize + 1;
                    nextCode = eoi + 1;
                    prevCode = -1;
                    continue;
                }

                byte[] emit;
                if (code < nextCode) {
                    emit = expand(code, prefix, suffix);
                } else if (code == nextCode && prevCode >= 0) {
                    byte[] prev = expand(prevCode, prefix, suffix);
                    byte[] joined = new byte[prev.length + 1];
                    System.arraycopy(prev, 0, joined, 0, prev.length);
                    joined[prev.length] = prev[0];
                    emit = joined;
                } else {
                    throw new IOException("invalid code " + code);
                }
                out.write(emit, 0, emit.length);

                if (prevCode >= 0 && nextCode < 4096) {
                    prefix[nextCode] = prevCode;
                    suffix[nextCode] = emit[0] & 0xFF;
                    nextCode++;
                    if (nextCode == (1 << codeWidth) && codeWidth < 12) codeWidth++;
                }
                prevCode = code;
            }
            return out.toByteArray();
        }

        private static byte[] expand(int code, int[] prefix, int[] suffix) {
            byte[] stack = new byte[4096];
            int sp = 0;
            int c = code;
            while (c >= 0) {
                stack[sp++] = (byte) suffix[c];
                c = prefix[c];
            }
            byte[] out = new byte[sp];
            for (int i = 0; i < sp; i++) out[i] = stack[sp - 1 - i];
            return out;
        }

        private static byte[] readSubBlocks(InputStream in) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (true) {
                int len = in.read();
                if (len <= 0) break;
                byte[] buf = new byte[len];
                int off = 0;
                while (off < len) {
                    int r = in.read(buf, off, len - off);
                    if (r < 0) throw new IOException("truncated sub-block");
                    off += r;
                }
                out.write(buf);
            }
            return out.toByteArray();
        }
    }
}
