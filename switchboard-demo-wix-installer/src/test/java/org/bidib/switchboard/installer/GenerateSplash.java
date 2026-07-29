package org.bidib.switchboard.installer;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import javax.imageio.*;

/**
 * Generates the application splash screen image (333x156 PNG).
 * Shows a stylized switchboard layout with the application title.
 * <p>
 * Run with: {@code java GenerateSplash.java [output-path]}
 */
public class GenerateSplash {

    public static void main(String[] args) throws Exception {
        int w = 333;
        int h = 156;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Dark background gradient
        GradientPaint bg = new GradientPaint(0, 0, new Color(0x23, 0x23, 0x28), 0, h, new Color(0x1a, 0x1a, 0x1e));
        g.setPaint(bg);
        g.fillRect(0, 0, w, h);

        // Draw subtle grid pattern (like the switchboard)
        g.setColor(new Color(0x3a, 0x3a, 0x40));
        int gridSize = 24;
        for (int x = 0; x <= w; x += gridSize) {
            g.drawLine(x, 0, x, h);
        }
        for (int y = 0; y <= h; y += gridSize) {
            g.drawLine(0, y, w, y);
        }

        // Draw track elements to represent the switchboard
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Horizontal track lines
        g.setColor(new Color(0xaa, 0xaa, 0xaa));
        g.drawLine(20, 96, 160, 96);
        g.drawLine(200, 96, 313, 96);

        // Turnout diverging (orange)
        g.setColor(new Color(0xff, 0xa5, 0x00));
        g.drawLine(160, 96, 200, 72);
        g.drawLine(200, 72, 240, 72);

        // Second track line
        g.setColor(new Color(0xaa, 0xaa, 0xaa));
        g.drawLine(240, 72, 313, 72);

        // Turnout point
        g.setColor(new Color(0xcc, 0xcc, 0xcc));
        g.fillOval(157, 93, 7, 7);

        // Signal (red)
        g.setColor(new Color(0xff, 0x50, 0x50));
        g.fillOval(80, 88, 8, 8);

        // Signal (green)
        g.setColor(new Color(0x50, 0xcc, 0x50));
        g.fillOval(270, 88, 8, 8);

        // Direction triangle
        g.setColor(new Color(0xcc, 0xcc, 0xcc, 180));
        int[] tx = {45, 38, 38};
        int[] ty = {96, 91, 101};
        g.fillPolygon(tx, ty, 3);

        // Occupancy marker (red line segment)
        g.setColor(new Color(0xff, 0x50, 0x50));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
        g.drawLine(108, 96, 132, 96);

        // Title text
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.setColor(new Color(0xee, 0xee, 0xee));
        String title = "Model Railway Switchboard";
        FontMetrics fm = g.getFontMetrics();
        int textX = (w - fm.stringWidth(title)) / 2;
        g.drawString(title, textX, 38);

        // Subtitle
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(0x99, 0x99, 0x99));
        String subtitle = "Control \u2022 Visualise \u2022 Simulate";
        fm = g.getFontMetrics();
        textX = (w - fm.stringWidth(subtitle)) / 2;
        g.drawString(subtitle, textX, 56);

        // Version text (bottom right)
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(new Color(0x66, 0x66, 0x66));
        String version = "v1.0-SNAPSHOT";
        fm = g.getFontMetrics();
        g.drawString(version, w - fm.stringWidth(version) - 10, h - 10);

        // Border
        g.setColor(new Color(0x55, 0x55, 0x60));
        g.setStroke(new BasicStroke(1f));
        g.drawRect(0, 0, w - 1, h - 1);

        g.dispose();

        Path out = Path.of(args.length > 0 ? args[0] : "SwitchboardSplash.png");
        ImageIO.write(img, "PNG", out.toFile());
        System.out.println("Generated: " + out.toAbsolutePath() + " (" + Files.size(out) + " bytes)");
    }
}
