package com.edwards.gpucalc.output.gif;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Gif89aWriterTest {

    @Test
    void singleFrameGifHasCorrectHeaderAndParsesViaImageIO() throws IOException {
        byte[][] palette = {{0, 0, 0}, {(byte) 255, 0, 0}};
        byte[] frame = new byte[4 * 4];
        for (int i = 0; i < frame.length; i++) frame[i] = (byte) (i % 2);
        byte[] gif = writeGif(4, 4, palette, 10, new byte[][]{frame});

        assertThat(gif[0]).isEqualTo((byte) 'G');
        assertThat(gif[1]).isEqualTo((byte) 'I');
        assertThat(gif[2]).isEqualTo((byte) 'F');
        assertThat(new String(gif, 0, 6)).isEqualTo("GIF89a");
        assertThat(gif[gif.length - 1]).isEqualTo((byte) 0x3B);

        int frameCount = countFramesViaImageIO(gif);
        assertThat(frameCount).isEqualTo(1);
    }

    @Test
    void twoFrameGifEmitsNetscapeLoopExtensionAndTwoFrames() throws IOException {
        byte[][] palette = {{0, 0, 0}, {(byte) 128, (byte) 128, (byte) 128}, {(byte) 255, (byte) 255, (byte) 255}};
        byte[] f1 = new byte[9];
        byte[] f2 = new byte[9];
        for (int i = 0; i < 9; i++) {
            f1[i] = 0;
            f2[i] = 2;
        }
        byte[] gif = writeGif(3, 3, palette, 5, new byte[][]{f1, f2});

        String asString = new String(gif, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertThat(asString).contains("NETSCAPE2.0");

        assertThat(countFramesViaImageIO(gif)).isEqualTo(2);
    }

    @Test
    void readBackPixelMatchesPaletteColor() throws IOException {
        byte[][] palette = {{0, 0, 0}, {(byte) 255, 0, 0}};
        byte[] frame = new byte[2 * 2];
        frame[0] = 1;
        frame[1] = 1;
        frame[2] = 1;
        frame[3] = 1;
        byte[] gif = writeGif(2, 2, palette, 10, new byte[][]{frame});
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(gif));
        assertThat(img).isNotNull();
        int rgb = img.getRGB(0, 0) & 0xFFFFFF;
        assertThat(rgb).isEqualTo(0xFF0000);
    }

    @Test
    void rejectsWidthZero() {
        assertThatThrownBy(() -> new Gif89aWriter(0, 10, new byte[][]{{0, 0, 0}, {1, 1, 1}}, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOversizedPalette() {
        byte[][] palette = new byte[257][3];
        assertThatThrownBy(() -> new Gif89aWriter(4, 4, palette, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsFrameLengthMismatch() {
        byte[][] palette = {{0, 0, 0}, {1, 1, 1}};
        Gif89aWriter w = new Gif89aWriter(3, 3, palette, 10);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        assertThatThrownBy(() -> w.write(new byte[][]{new byte[8]}, buf))
                .isInstanceOf(IOException.class);
    }

    private static byte[] writeGif(int w, int h, byte[][] palette, int delay, byte[][] frames) throws IOException {
        Gif89aWriter writer = new Gif89aWriter(w, h, palette, delay);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        writer.write(frames, buf);
        return buf.toByteArray();
    }

    private static int countFramesViaImageIO(byte[] gif) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(gif))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) throw new IOException("no GIF reader available");
            ImageReader reader = readers.next();
            reader.setInput(iis);
            return reader.getNumImages(true);
        }
    }
}
