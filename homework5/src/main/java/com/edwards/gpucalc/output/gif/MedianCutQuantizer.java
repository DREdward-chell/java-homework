package com.edwards.gpucalc.output.gif;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MedianCutQuantizer {

    public record Result(byte[][] palette, byte[][] indices) {}

    private final int paletteSize;

    public MedianCutQuantizer(int paletteSize) {
        if (paletteSize < 2 || paletteSize > 256) {
            throw new IllegalArgumentException("paletteSize must be in [2,256]; got " + paletteSize);
        }
        this.paletteSize = paletteSize;
    }

    public Result quantize(BufferedImage[] frames) {
        List<int[]> unique = collectUnique(frames);
        int[][] palette = unique.size() <= paletteSize
                ? promote(unique)
                : medianCut(unique);
        byte[][] paletteBytes = toByteTriples(palette);
        byte[][] indices = mapFrames(frames, palette);
        return new Result(paletteBytes, indices);
    }

    private static List<int[]> collectUnique(BufferedImage[] frames) {
        java.util.HashSet<Integer> seen = new java.util.HashSet<>();
        List<int[]> unique = new ArrayList<>();
        for (BufferedImage img : frames) {
            int w = img.getWidth();
            int h = img.getHeight();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = img.getRGB(x, y) & 0xFFFFFF;
                    if (seen.add(rgb)) {
                        unique.add(new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF});
                    }
                }
            }
        }
        return unique;
    }

    private static int[][] promote(List<int[]> unique) {
        int[][] out = new int[unique.size()][];
        for (int i = 0; i < unique.size(); i++) out[i] = unique.get(i);
        return out;
    }

    private int[][] medianCut(List<int[]> unique) {
        List<int[][]> boxes = new ArrayList<>();
        boxes.add(unique.toArray(new int[0][]));
        while (boxes.size() < paletteSize) {
            int pickIdx = -1;
            int pickRange = 0;
            int pickChannel = 0;
            for (int i = 0; i < boxes.size(); i++) {
                int[][] box = boxes.get(i);
                if (box.length < 2) continue;
                int[] range = channelRange(box);
                int maxCh = 0;
                if (range[1] > range[maxCh]) maxCh = 1;
                if (range[2] > range[maxCh]) maxCh = 2;
                if (range[maxCh] > pickRange) {
                    pickRange = range[maxCh];
                    pickIdx = i;
                    pickChannel = maxCh;
                }
            }
            if (pickIdx < 0) break;
            int[][] src = boxes.remove(pickIdx);
            final int ch = pickChannel;
            java.util.Arrays.sort(src, Comparator.comparingInt(c -> c[ch]));
            int mid = src.length / 2;
            int[][] left = java.util.Arrays.copyOfRange(src, 0, mid);
            int[][] right = java.util.Arrays.copyOfRange(src, mid, src.length);
            boxes.add(left);
            boxes.add(right);
        }
        int[][] palette = new int[boxes.size()][];
        for (int i = 0; i < boxes.size(); i++) {
            palette[i] = mean(boxes.get(i));
        }
        return palette;
    }

    private static int[] channelRange(int[][] box) {
        int minR = 255, minG = 255, minB = 255;
        int maxR = 0, maxG = 0, maxB = 0;
        for (int[] c : box) {
            if (c[0] < minR) minR = c[0];
            if (c[0] > maxR) maxR = c[0];
            if (c[1] < minG) minG = c[1];
            if (c[1] > maxG) maxG = c[1];
            if (c[2] < minB) minB = c[2];
            if (c[2] > maxB) maxB = c[2];
        }
        return new int[]{maxR - minR, maxG - minG, maxB - minB};
    }

    private static int[] mean(int[][] box) {
        long r = 0, g = 0, b = 0;
        for (int[] c : box) {
            r += c[0];
            g += c[1];
            b += c[2];
        }
        int n = box.length;
        return new int[]{(int) (r / n), (int) (g / n), (int) (b / n)};
    }

    private static byte[][] toByteTriples(int[][] palette) {
        byte[][] out = new byte[palette.length][3];
        for (int i = 0; i < palette.length; i++) {
            out[i][0] = (byte) palette[i][0];
            out[i][1] = (byte) palette[i][1];
            out[i][2] = (byte) palette[i][2];
        }
        return out;
    }

    private static byte[][] mapFrames(BufferedImage[] frames, int[][] palette) {
        byte[][] out = new byte[frames.length][];
        for (int i = 0; i < frames.length; i++) {
            BufferedImage img = frames[i];
            int w = img.getWidth();
            int h = img.getHeight();
            byte[] buf = new byte[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    buf[y * w + x] = (byte) nearest(palette, r, g, b);
                }
            }
            out[i] = buf;
        }
        return out;
    }

    private static int nearest(int[][] palette, int r, int g, int b) {
        int best = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < palette.length; i++) {
            int dr = palette[i][0] - r;
            int dg = palette[i][1] - g;
            int db = palette[i][2] - b;
            int d = dr * dr + dg * dg + db * db;
            if (d < bestDist) {
                bestDist = d;
                best = i;
                if (d == 0) break;
            }
        }
        return best;
    }
}
