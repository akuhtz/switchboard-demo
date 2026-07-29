package org.bidib.switchboard.installer;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;

/**
 * Generates a 34x34 ICO file with a model railway switchboard logo.
 * Renders a 2x2 tile section from the switchboard layout (tiles at (19,2),(20,2),(19,3),(20,3)
 * from switchboard7.json):
 * <ul>
 *   <li>(19,2) TR-007: turnout_straight_right, rotation 0</li>
 *   <li>(20,2) CL-005: curve_left, rotation 0</li>
 *   <li>(19,3) S2-012: signal_2_red, rotation 180</li>
 *   <li>(20,3) TR-006: turnout_straight_right, rotation 180</li>
 * </ul>
 * Run with: {@code java GenerateIcon.java [output-path]}
 */
public class GenerateIcon {

    private static final int SIZE = 34;
    private static final int TILE = SIZE / 2; // 17px per tile
    private static final Color BG = new Color(0x2d, 0x2d, 0x32);
    private static final Color TRACK = new Color(0xaa, 0xaa, 0xaa);
    private static final Color INACTIVE = new Color(0x80, 0x80, 0x80);
    private static final Color FROG = new Color(0xff, 0xa5, 0x00);

    public static void main(String[] args) throws Exception {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill background
        g.setColor(BG);
        g.fillRect(0, 0, SIZE, SIZE);

        // Draw each tile in its grid position
        drawTile(g, 0, 0, 0, GenerateIcon::drawTurnoutStraightRight);   // (19,2) rot 0
        drawTile(g, 1, 0, 0, GenerateIcon::drawCurveLeft);              // (20,2) rot 0
        drawTile(g, 0, 1, 180, GenerateIcon::drawSignal2Red);           // (19,3) rot 180
        drawTile(g, 1, 1, 180, GenerateIcon::drawTurnoutStraightRight); // (20,3) rot 180

        g.dispose();

        byte[] ico = createIco(img);
        Path out = Path.of(args.length > 0 ? args[0] : "switchboard-logo-34x34.ico");
        Files.write(out, ico);
        System.out.println("Generated: " + out.toAbsolutePath() + " (" + ico.length + " bytes)");
    }

    @FunctionalInterface
    interface TileRenderer {
        void draw(Graphics2D g, int size);
    }

    private static void drawTile(Graphics2D g, int gridX, int gridY, int rotation, TileRenderer renderer) {
        int px = gridX * TILE;
        int py = gridY * TILE;
        Graphics2D tg = (Graphics2D) g.create(px, py, TILE, TILE);
        tg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (rotation != 0) {
            tg.rotate(Math.toRadians(rotation), TILE / 2.0, TILE / 2.0);
        }
        renderer.draw(tg, TILE);
        tg.dispose();
    }

    // --- Tile renderers (each draws in a 0,0..size,size coordinate space) ---

    private static void drawTurnoutStraightRight(Graphics2D g, int size) {
        float sw = size / 8f; // stroke width scaled from 4/32
        int mid = size / 2;
        g.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Diverted path (gray): center to bottom-right
        g.setColor(INACTIVE);
        g.drawLine(mid, mid, size, size);

        // Through path left half (light gray)
        g.setColor(TRACK);
        g.drawLine(0, mid, mid, mid);

        // Through path right half (orange frog-end)
        g.setColor(FROG);
        g.drawLine(mid, mid, size, mid);
    }

    private static void drawCurveLeft(Graphics2D g, int size) {
        float sw = size / 8f;
        int mid = size / 2;
        g.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(TRACK);
        // Horizontal to center
        g.drawLine(0, mid, mid, mid);
        // Center to top-right
        g.drawLine(mid, mid, size, 0);
    }

    private static void drawSignal2Red(Graphics2D g, int size) {
        float sw = size / 8f;
        int mid = size / 2;
        g.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Track line
        g.setColor(TRACK);
        g.drawLine(0, mid, size, mid);

        // Signal body (small rectangle + colored dots)
        int sigW = size / 4;
        int sigH = size / 3;
        int sx = mid - sigW / 2 + size / 4;
        int sy = mid + 1;
        g.setColor(Color.BLACK);
        g.fillRoundRect(sx, sy, sigW, sigH, 2, 2);
        g.setColor(Color.WHITE);
        g.drawRoundRect(sx, sy, sigW, sigH, 2, 2);
        // Red dot (active)
        int dotR = Math.max(1, size / 12);
        g.setColor(new Color(0xFF, 0x00, 0x20));
        g.fillOval(sx + sigW / 2 - dotR, sy + sigH * 2 / 3 - dotR, dotR * 2, dotR * 2);
        // Green dot (dim)
        g.setColor(new Color(0x00, 0xFF, 0x40, 128));
        g.fillOval(sx + sigW / 2 - dotR, sy + sigH / 3 - dotR, dotR * 2, dotR * 2);
    }

    // --- ICO file format ---

    static byte[] createIco(BufferedImage img) throws IOException {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);

        int bmpHeaderSize = 40;
        int rowSize = w * 4;
        int maskRowSize = ((w + 31) / 32) * 4;
        int imageSize = rowSize * h + maskRowSize * h;

        ByteBuffer bmp = ByteBuffer.allocate(bmpHeaderSize + imageSize);
        bmp.order(ByteOrder.LITTLE_ENDIAN);

        bmp.putInt(bmpHeaderSize);
        bmp.putInt(w);
        bmp.putInt(h * 2);
        bmp.putShort((short) 1);
        bmp.putShort((short) 32);
        bmp.putInt(0);
        bmp.putInt(imageSize);
        bmp.putInt(0);
        bmp.putInt(0);
        bmp.putInt(0);
        bmp.putInt(0);

        for (int y = h - 1; y >= 0; y--) {
            for (int x = 0; x < w; x++) {
                int argb = pixels[y * w + x];
                bmp.put((byte) (argb & 0xFF));
                bmp.put((byte) ((argb >> 8) & 0xFF));
                bmp.put((byte) ((argb >> 16) & 0xFF));
                bmp.put((byte) ((argb >> 24) & 0xFF));
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < maskRowSize; x++) {
                bmp.put((byte) 0);
            }
        }

        byte[] bmpData = bmp.array();
        ByteBuffer ico = ByteBuffer.allocate(6 + 16 + bmpData.length);
        ico.order(ByteOrder.LITTLE_ENDIAN);
        ico.putShort((short) 0);
        ico.putShort((short) 1);
        ico.putShort((short) 1);
        ico.put((byte) w);
        ico.put((byte) h);
        ico.put((byte) 0);
        ico.put((byte) 0);
        ico.putShort((short) 1);
        ico.putShort((short) 32);
        ico.putInt(bmpData.length);
        ico.putInt(6 + 16);
        ico.put(bmpData);

        return ico.array();
    }
}
