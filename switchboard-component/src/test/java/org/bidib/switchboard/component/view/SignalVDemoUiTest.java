package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.config.TestOccupancyFactory;
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.SignalTile;
import org.bidib.switchboard.component.service.RouterService;
import org.bidib.switchboard.component.util.SvgIconLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.FlatDarkLaf;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.view.ViewBox;

/**
 * Visual check for the SBB distant signal (SIGNAL_V) icons.
 * Shows the element in the switchboard panel and enlarged so the geometry
 * (square body aspect ratio, track overlap, lamp states) can be inspected.
 * The window closes automatically after 5 seconds.
 */
// TEMP-DISABLED-REMOVED @Disabled
class SignalVDemoUiTest {

    private FrameFixture window;

    @Test
    void displayDistantSignalForVisualCheck() throws Exception {
        OccupancyFactory occupancyFactory = new TestOccupancyFactory();
        RailwayModel model = new RailwayModel();

        GuiActionRunner.execute(() -> FlatDarkLaf.setup());

        SwitchboardPanel panel = GuiActionRunner.execute(
            () -> new SwitchboardPanel(occupancyFactory,
                (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model, RouterService.createDefault(), 3, 2, 32));

        List<List<String>> rows = List.of(
            List.of("/icons/signals/sbb_l/signal_v_orange_left.svg", "/icons/signals/sbb_l/signal_v_yellow_left.svg", "/icons/signals/sbb_l/signal_v_green_left.svg", "/icons/signals/sbb_l/signal_v_aspect3_left.svg"),
            List.of("/icons/signals/sbb_l/signal_v_orange_right.svg", "/icons/signals/sbb_l/signal_v_yellow_right.svg", "/icons/signals/sbb_l/signal_v_green_right.svg", "/icons/signals/sbb_l/signal_v_aspect3_right.svg"));

        GuiActionRunner.execute(() -> {
            int id = 1;
            for (int r = 0; r < rows.size(); r++) {
                List<String> paths = rows.get(r);
                for (int c = 0; c < paths.size(); c++) {
                    String elementId = String.format("SV-%03d", id++);
                    panel.setTile(new SignalTile(c, r, elementId, ElementType.SIGNAL_V, paths));
                    Element element = new Element(elementId, 0, 0);
                    element.setCurrentAspect(c);
                    model.addElement(element);
                }
            }
        });
        assertThat(panel.getTiles()).hasSize(8);

        JPanel previewGrid = new JPanel(new GridLayout(2, 4));
        for (int r = 0; r < rows.size(); r++) {
            List<String> paths = rows.get(r);
            for (int c = 0; c < paths.size(); c++) {
                JPanel cell = new JPanel(new BorderLayout());
                cell.setBackground(new Color(45, 45, 50));
                cell.setBorder(BorderFactory.createTitledBorder(
                    (r == 0 ? "Swiss (left)  " : "German (right)  ") + aspectLabel(c)));
                cell.add(new JLabel(renderIcon(paths.get(c), 384), SwingConstants.CENTER), BorderLayout.CENTER);
                previewGrid.add(cell);
            }
        }

        CountDownLatch closed = new CountDownLatch(1);

        JFrame frame = GuiActionRunner.execute(() -> {
            JFrame f = new JFrame("SBB distant signal (SIGNAL_V) - visual check");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JPanel content = new JPanel(new BorderLayout());
            content.add(new JLabel("This window closes automatically in 3 seconds."), BorderLayout.NORTH);
            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.add(new JLabel("Switchboard context (32px tiles):"));
            center.add(panel);
            center.add(new JLabel("Enlarged (192px):"));
            center.add(previewGrid);
            content.add(center, BorderLayout.CENTER);
            f.add(content);
            f.pack();
            f.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    closed.countDown();
                }
            });
            return f;
        });

        window = new FrameFixture(frame);
        window.robot().showWindow(window.target(), new Dimension(frame.getWidth() + 40, frame.getHeight() + 40));
        window.robot().waitForIdle();

        closed.await(3, TimeUnit.SECONDS);
        GuiActionRunner.execute(() -> frame.dispose());
    }

    @AfterEach
    void tearDown() {
        if (window != null) {
            window.cleanUp();
        }
    }

    private static String aspectLabel(int aspect) {
        return switch (aspect) {
            case 0 -> "orange";
            case 1 -> "yellow";
            case 2 -> "green";
            case 3 -> "aspect 3";
            default -> "aspect " + aspect;
        };
    }

    private static ImageIcon renderIcon(String path, int size) {
        SVGDocument doc = SvgIconLoader.load(path);
        assertThat(doc).as("SVG %s should load", path).isNotNull();
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(45, 45, 50));
        g.fillRect(0, 0, size, size);
        doc.render(null, g, new ViewBox(0, 0, size, size));
        drawLampLabels(g, path.contains("_right"), size);
        g.dispose();
        return new ImageIcon(img);
    }

    /** Lamp identifiers and their positions (cx, cy) in the SVG's 200x200 signal space. */
    private static final Map<String, double[]> LAMP_POSITIONS = Map.of(
        "vs1_o1", new double[] { 150, 150 },
        "vs1_o2", new double[] { 50, 150 },
        "vs1_o3", new double[] { 50, 50 },
        "vs1_g1", new double[] { 50, 100 },
        "vs1_g2", new double[] { 150, 50 });

    /**
     * Maps a lamp position from the SVG signal space to the 32x32 tile space using the same
     * transform as the signal_v_*.svg files ({@code _left} rotate 270, {@code _right} rotate 90).
     */
    private static double[] tilePosition(double x, double y, boolean right) {
        return right
            ? new double[] { 24.79 - 0.085 * y, 13.7 + 0.085 * x }
            : new double[] { 7.21 + 0.085 * y, 18.3 - 0.085 * x };
    }

    private static void drawLampLabels(Graphics2D g, boolean right, int size) {
        double scale = size / 32.0;
        double lampRadiusPx = 20 * 0.085 * scale;
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        for (Map.Entry<String, double[]> e : LAMP_POSITIONS.entrySet()) {
            double[] p = tilePosition(e.getValue()[0], e.getValue()[1], right);
            String label = e.getKey();
            int x = (int) (p[0] * scale - fm.stringWidth(label) / 2.0);
            int y = (int) (p[1] * scale + lampRadiusPx + 4 + fm.getAscent());
            g.drawString(label, x, y);
        }
    }
}
