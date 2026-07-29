package org.bidib.switchboard.installer;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;

/**
 * Generates a 34x34 ICO file with a model railway switchboard logo.
 * The icon shows a stylized track switch/turnout symbol on a dark background.
 * <p>
 * Run with: {@code java GenerateIcon.java [output-path]}
 * <p>
 * Default output: {@code switchboard-logo-34x34.ico} in the current directory.
 */
public class GenerateIcon {

    public static void main(String[] args) throws Exception {
        int size = 34;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dark background (matches the tile grid color)
        g.setColor(new Color(0x2d, 0x2d, 0x32));
        g.fillRect(0, 0, size, size);

        // Draw a stylized turnout/switch symbol
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Main horizontal track line (light gray)
        g.setColor(new Color(0xaa, 0xaa, 0xaa));
        g.drawLine(3, 17, 31, 17);

        // Diverging track (orange frog-end, like our SVGs)
        g.setColor(new Color(0xff, 0xa5, 0x00));
        g.drawLine(17, 17, 28, 8);

        // Small filled circle at the switch point
        g.setColor(new Color(0xaa, 0xaa, 0xaa));
        g.fillOval(15, 15, 5, 5);

        // Direction triangle (matching our direction marker style)
        g.setColor(new Color(0xcc, 0xcc, 0xcc, 200));
        int[] tx = {7, 3, 3};
        int[] ty = {27, 24, 30};
        g.fillPolygon(tx, ty, 3);

        // Small signal dot (red)
        g.setColor(new Color(0xff, 0x50, 0x50));
        g.fillOval(26, 23, 5, 5);

        g.dispose();

        // Write ICO format
        byte[] ico = createIco(img);
        Path out = Path.of(args.length > 0 ? args[0] : "switchboard-logo-34x34.ico");
        Files.write(out, ico);
        System.out.println("Generated: " + out.toAbsolutePath() + " (" + ico.length + " bytes)");
    }

    static byte[] createIco(BufferedImage img) throws IOException {
        int w = img.getWidth();
        int h = img.getHeight();

        // Get raw ARGB pixels
        int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);

        // BMP info header (BITMAPINFOHEADER) — height is doubled for ICO (image + mask)
        int bmpHeaderSize = 40;
        int rowSize = w * 4; // 32-bit BGRA
        int maskRowSize = ((w + 31) / 32) * 4; // 1-bit mask, padded to 4 bytes
        int imageSize = rowSize * h + maskRowSize * h;

        ByteBuffer bmp = ByteBuffer.allocate(bmpHeaderSize + imageSize);
        bmp.order(ByteOrder.LITTLE_ENDIAN);

        // BITMAPINFOHEADER
        bmp.putInt(bmpHeaderSize);  // biSize
        bmp.putInt(w);              // biWidth
        bmp.putInt(h * 2);          // biHeight (doubled for ICO)
        bmp.putShort((short) 1);    // biPlanes
        bmp.putShort((short) 32);   // biBitCount
        bmp.putInt(0);              // biCompression (BI_RGB)
        bmp.putInt(imageSize);      // biSizeImage
        bmp.putInt(0);              // biXPelsPerMeter
        bmp.putInt(0);              // biYPelsPerMeter
        bmp.putInt(0);              // biClrUsed
        bmp.putInt(0);              // biClrImportant

        // Pixel data (bottom-up, BGRA)
        for (int y = h - 1; y >= 0; y--) {
            for (int x = 0; x < w; x++) {
                int argb = pixels[y * w + x];
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                bmp.put((byte) b);
                bmp.put((byte) g);
                bmp.put((byte) r);
                bmp.put((byte) a);
            }
        }

        // AND mask (all zeros = fully opaque, since we use alpha channel)
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < maskRowSize; x++) {
                bmp.put((byte) 0);
            }
        }

        byte[] bmpData = bmp.array();

        // ICO file header
        ByteBuffer ico = ByteBuffer.allocate(6 + 16 + bmpData.length);
        ico.order(ByteOrder.LITTLE_ENDIAN);

        // ICONDIR
        ico.putShort((short) 0);    // reserved
        ico.putShort((short) 1);    // type (1 = ICO)
        ico.putShort((short) 1);    // count

        // ICONDIRENTRY
        ico.put((byte) w);          // width (0 means 256)
        ico.put((byte) h);          // height
        ico.put((byte) 0);          // color count
        ico.put((byte) 0);          // reserved
        ico.putShort((short) 1);    // planes
        ico.putShort((short) 32);   // bit count
        ico.putInt(bmpData.length);  // bytes in resource
        ico.putInt(6 + 16);         // offset to data

        // BMP data
        ico.put(bmpData);

        return ico.array();
    }
}
