package org.bidib.switchboard.component.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Locale;

import javax.imageio.ImageIO;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.view.ViewBox;

/**
 * Helper that renders the combined signal tiles to render.png.
 * <p>
 * Run from the component module directory (working directory = switchboard-component)
 * so the default output path {@code target/render/render.png} resolves.
 * Optionally pass an absolute output path as the first argument.
 */
public final class RenderAll {

    private static final String BASE = "/icons/signals/sbb_l/";

    private static void draw(Graphics2D g, String path, int w, int h) {
        SVGDocument doc = SvgIconLoader.load(path);
        if (doc == null) {
            System.out.println("NULL: " + path);
            return;
        }
        doc.render(null, g, new ViewBox(0, 0, w, h));
    }

    public static void main(String[] args) throws IOException {
        String output = args.length >= 1 ? args[0] : "target/render/render.png";

        String[] heads = {"red", "green", "orange"};
        String[] plates = {"orange", "green", "orgreen"};

        BufferedImage img = new BufferedImage(114, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bg = img.createGraphics();
        bg.setColor(new Color(45, 45, 50));
        bg.fillRect(0, 0, 114, 64);
        bg.dispose();
        for (String side : new String[] {"left", "right"}) {
            for (int i = 0; i < 3; i++) {
                Graphics2D t = img.createGraphics();
                t.translate(i * 38, side.equals("left") ? 0 : 32);
                draw(t, BASE + "signal_sm_head_" + heads[i] + "_" + side + ".svg", 38, 32);
                draw(t, BASE + "signal_sm_plate_" + plates[i] + "_" + side + ".svg", 38, 32);
                t.dispose();
            }
        }

        File target = Paths.get(output).toFile().getAbsoluteFile();
        if (target.getParentFile() != null) {
            target.getParentFile().mkdirs();
        }
        ImageIO.write(img, "png", target);
        System.out.println(String.format(Locale.ROOT, "wrote render.png (%dx%d) -> %s",
            img.getWidth(), img.getHeight(), target));
    }

    private RenderAll() {
    }
}