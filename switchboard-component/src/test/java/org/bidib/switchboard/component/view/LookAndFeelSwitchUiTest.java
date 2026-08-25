package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Dimension;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.config.TestOccupancyFactory;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.bidib.switchboard.component.service.RouterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

// TEMP-DISABLED-REMOVED @Disabled
class LookAndFeelSwitchUiTest {

    private FrameFixture window;

    private SwitchboardPanel panel;

    private JFrame frame;

    private final OccupancyFactory occupancyFactory = new TestOccupancyFactory();

    @BeforeEach
    void setUp() throws Exception {
        var model = new RailwayModel();

        GuiActionRunner.execute(() -> FlatDarkLaf.setup());

        panel = GuiActionRunner.execute(
            () -> new SwitchboardPanel(occupancyFactory,
                (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model, RouterService.createDefault(), 60, 30, 32));

        var url = LookAndFeelSwitchUiTest.class.getResource("/test-data/switchboard3d.json");
        Path path = Paths.get(url.toURI());
        var layoutPersistence = new LayoutPersistence();
        GuiActionRunner.execute(() -> layoutPersistence.load(panel, path));

        frame = GuiActionRunner.execute(() -> {
            JFrame f = new JFrame("Model Railway Switchboard");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JMenuBar menuBar = new JMenuBar();
            JMenu fileMenu = new JMenu("File");
            fileMenu.setMnemonic('F');
            JMenu settingsMenu = new JMenu("Settings");
            settingsMenu.setMnemonic('S');
            JRadioButtonMenuItem lightItem = new JRadioButtonMenuItem("Light Look and Feel");
            lightItem.addActionListener(e -> applyLookAndFeel(FlatLightLaf.class, f));
            settingsMenu.add(lightItem);
            JRadioButtonMenuItem darkItem = new JRadioButtonMenuItem("Dark Look and Feel");
            darkItem.setSelected(true);
            darkItem.addActionListener(e -> applyLookAndFeel(FlatDarkLaf.class, f));
            settingsMenu.add(darkItem);
            fileMenu.add(settingsMenu);
            menuBar.add(fileMenu);
            f.setJMenuBar(menuBar);
            f.add(panel);
            f.pack();
            return f;
        });

        window = new FrameFixture(frame);
        window.robot().showWindow(window.target(), new Dimension(1024, 768));
        window.robot().waitForIdle();
    }

    private static void applyLookAndFeel(Class<?> lafClass, JFrame f) {
        if (lafClass == FlatLightLaf.class) {
            FlatLightLaf.setup();
        }
        else {
            FlatDarkLaf.setup();
        }
        SwingUtilities.updateComponentTreeUI(f);
    }

    @AfterEach
    void tearDown() {
        window.cleanUp();
    }

    @Test
    void panelBackgroundFollowsSelectedLookAndFeel() {
        var darkBackground = GuiActionRunner.execute(() -> panel.getBackground());
        assertThat(darkBackground).isEqualTo(panelColor());

        window.menuItemWithPath("File", "Settings", "Light Look and Feel").click();
        window.robot().waitForIdle();

        var lightBackground = GuiActionRunner.execute(() -> panel.getBackground());
        assertThat(lightBackground).isEqualTo(panelColor());
        assertThat(brightness(lightBackground)).isGreaterThan(brightness(darkBackground));

        window.menuItemWithPath("File", "Settings", "Dark Look and Feel").click();
        window.robot().waitForIdle();

        var darkAgain = GuiActionRunner.execute(() -> panel.getBackground());
        assertThat(darkAgain).isEqualTo(panelColor());
        assertThat(brightness(darkAgain)).isLessThan(brightness(lightBackground));
    }

    private static Color panelColor() {
        return GuiActionRunner.execute(() -> javax.swing.UIManager.getColor("Panel.background"));
    }

    private static int brightness(Color c) {
        return (int) (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
    }
}