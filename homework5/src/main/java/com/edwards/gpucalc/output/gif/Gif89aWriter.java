package com.edwards.gpucalc.output.gif;

import java.io.IOException;
import java.io.OutputStream;

public final class Gif89aWriter {

    private static final byte[] HEADER = {'G', 'I', 'F', '8', '9', 'a'};
    private static final byte BLOCK_INTRODUCER_EXTENSION = 0x21;
    private static final byte GRAPHIC_CONTROL_LABEL = (byte) 0xF9;
    private static final byte APPLICATION_EXTENSION_LABEL = (byte) 0xFF;
    private static final byte IMAGE_SEPARATOR = 0x2C;
    private static final byte TRAILER = 0x3B;

    private final int width;
    private final int height;
    private final byte[][] palette;
    private final int paletteBits;
    private final int delayCentiseconds;

    public Gif89aWriter(int width, int height, byte[][] palette, int delayCentiseconds) {
        if (width <= 0 || width > 0xFFFF) {
            throw new IllegalArgumentException("width out of range: " + width);
        }
        if (height <= 0 || height > 0xFFFF) {
            throw new IllegalArgumentException("height out of range: " + height);
        }
        if (palette.length < 2 || palette.length > 256) {
            throw new IllegalArgumentException("palette size must be in [2,256]; got " + palette.length);
        }
        if (delayCentiseconds < 0 || delayCentiseconds > 0xFFFF) {
            throw new IllegalArgumentException("delayCentiseconds out of range: " + delayCentiseconds);
        }
        this.width = width;
        this.height = height;
        this.palette = palette;
        this.paletteBits = Math.max(1, ceilLog2(palette.length));
        this.delayCentiseconds = delayCentiseconds;
    }

    public void write(byte[][] frames, OutputStream out) throws IOException {
        writeHeader(out);
        writeLogicalScreenDescriptor(out);
        writeGlobalColorTable(out);
        if (frames.length > 1) {
            writeNetscapeLoopExtension(out);
        }
        for (byte[] frame : frames) {
            if (frame.length != width * height) {
                throw new IOException("frame length " + frame.length
                        + " does not match " + width + "x" + height);
            }
            writeGraphicControlExtension(out);
            writeImageDescriptor(out);
            writeImageData(frame, out);
        }
        out.write(TRAILER);
    }

    private void writeHeader(OutputStream out) throws IOException {
        out.write(HEADER);
    }

    private void writeLogicalScreenDescriptor(OutputStream out) throws IOException {
        writeLe16(out, width);
        writeLe16(out, height);
        int packed = 0x80 | ((paletteBits - 1) & 0x07) | (((paletteBits - 1) & 0x07) << 4);
        out.write(packed);
        out.write(0);
        out.write(0);
    }

    private void writeGlobalColorTable(OutputStream out) throws IOException {
        int entries = 1 << paletteBits;
        for (int i = 0; i < entries; i++) {
            if (i < palette.length) {
                out.write(palette[i][0] & 0xFF);
                out.write(palette[i][1] & 0xFF);
                out.write(palette[i][2] & 0xFF);
            } else {
                out.write(0);
                out.write(0);
                out.write(0);
            }
        }
    }

    private void writeNetscapeLoopExtension(OutputStream out) throws IOException {
        out.write(BLOCK_INTRODUCER_EXTENSION);
        out.write(APPLICATION_EXTENSION_LABEL);
        out.write(11);
        out.write(new byte[]{'N', 'E', 'T', 'S', 'C', 'A', 'P', 'E', '2', '.', '0'});
        out.write(3);
        out.write(1);
        writeLe16(out, 0);
        out.write(0);
    }

    private void writeGraphicControlExtension(OutputStream out) throws IOException {
        out.write(BLOCK_INTRODUCER_EXTENSION);
        out.write(GRAPHIC_CONTROL_LABEL);
        out.write(4);
        out.write(0);
        writeLe16(out, delayCentiseconds);
        out.write(0);
        out.write(0);
    }

    private void writeImageDescriptor(OutputStream out) throws IOException {
        out.write(IMAGE_SEPARATOR);
        writeLe16(out, 0);
        writeLe16(out, 0);
        writeLe16(out, width);
        writeLe16(out, height);
        out.write(0);
    }

    private void writeImageData(byte[] indices, OutputStream out) throws IOException {
        int minCodeSize = Math.max(2, paletteBits);
        LzwEncoder encoder = new LzwEncoder(minCodeSize);
        encoder.encode(indices, out);
    }

    private static void writeLe16(OutputStream out, int v) throws IOException {
        out.write(v & 0xFF);
        out.write((v >>> 8) & 0xFF);
    }

    private static int ceilLog2(int n) {
        int bits = 0;
        int v = n - 1;
        while (v > 0) {
            bits++;
            v >>>= 1;
        }
        return bits;
    }
}
