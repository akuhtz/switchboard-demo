package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.List;
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
import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.util.SvgIconLoader;
import org.junit.jupiter.api.AfterEach;
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
class SignalVDemoUiTest {

    private FrameFixture window;

    @Test
    void displayDistantSignalForVisualCheck() throws Exception {
        OccupancyFactory occupancyFactory = new TestOccupancyFactory();
        RailwayModel model = new RailwayModel();

        GuiActionRunner.execute(() -> FlatDarkLaf.setup());

        SwitchboardPanel panel = GuiActionRunner.execute(
            () -> new SwitchboardPanel(occupancyFactory,
                (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model, 3, 2, 32));

        List<List<String>> rows = List.of(
            List.of("/icons/signal_v_orange_left.svg", "/icons/signal_v_yellow_left.svg", "/icons/signal_v_green_left.svg"),
            List.of("/icons/signal_v_orange_right.svg", "/icons/signal_v_yellow_right.svg", "/icons/signal_v_green_right.svg"));

        GuiActionRunner.execute(() -> {
            int id = 1;
            for (int r = 0; r < rows.size(); r++) {
                List<String> paths = rows.get(r);
                for (int c = 0; c < paths.size(); c++) {
                    String elementId = String.format("SV-%03d", id++);
                    panel.setTile(new ElementTile(c, r, elementId, ElementType.SIGNAL_V, paths));
                    Element element = new Element(elementId, 0, 0);
                    element.setCurrentAspect(c);
                    model.addElement(element);
                }
            }
        });
        assertThat(panel.getTiles()).hasSize(6);

        JPanel previewGrid = new JPanel(new GridLayout(2, 3));
        for (int r = 0; r < rows.size(); r++) {
            List<String> paths = rows.get(r);
            for (int c = 0; c < paths.size(); c++) {
                JPanel cell = new JPanel(new BorderLayout());
                cell.setBackground(new Color(45, 45, 50));
                cell.setBorder(BorderFactory.createTitledBorder(
                    (r == 0 ? "Swiss (left)  " : "German (right)  ") + aspectLabel(c)));
                cell.add(new JLabel(renderIcon(paths.get(c), 192), SwingConstants.CENTER), BorderLayout.CENTER);
                previewGrid.add(cell);
            }
        }

        CountDownLatch closed = new CountDownLatch(1);

        JFrame frame = GuiActionRunner.execute(() -> {
            JFrame f = new JFrame("SBB distant signal (SIGNAL_V) - visual check");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JPanel content = new JPanel(new BorderLayout());
            content.add(new JLabel("This window closes automatically in 5 seconds."), BorderLayout.NORTH);
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

        closed.await(5, TimeUnit.SECONDS);
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
        g.dispose();
        return new ImageIcon(img);
    }
}
