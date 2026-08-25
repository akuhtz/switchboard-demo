package org.bidib.switchboard.demoapp;

import java.awt.Dimension;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import javax.swing.JFrame;

import com.formdev.flatlaf.FlatLightLaf;
import com.vlsolutions.swing.docking.DockingDesktop;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.bidib.switchboard.component.config.TestOccupancyFactory;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Train;
import org.bidib.switchboard.component.model.TrainListModel;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.bidib.switchboard.component.service.RouterService;
import org.bidib.switchboard.component.view.AssignOccupancyDialog;
import org.bidib.switchboard.component.view.SwitchboardPanel;
import org.bidib.switchboard.component.view.TrainListPanel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * UI test that loads switchboard3d.json and displays the panels
 * with the train list on the left and switchboard on the right.
 */
// TEMP-DISABLED-REMOVED @Disabled
class TrainListUiTest {

    private FrameFixture window;

    @BeforeEach
    void setUp() {
        GuiActionRunner.execute(() -> FlatLightLaf.setup());

        RailwayModel model = new RailwayModel();
        SwitchboardPanel switchboardPanel = GuiActionRunner.execute(() -> new SwitchboardPanel(
            new TestOccupancyFactory(),
            (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el),
            model, RouterService.createDefault()));

        // Load switchboard3d.json
        URL url = getClass().getResource("/test-data/switchboard3d.json");
        GuiActionRunner.execute(() -> {
            try {
                new LayoutPersistence().load(switchboardPanel, Paths.get(url.toURI()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Create train list model with demo trains
        TrainListModel trainListModel = new TrainListModel();
        GuiActionRunner.execute(() -> {
            trainListModel.addTrain(new Train("T001", "Re 460 023", null));
            trainListModel.addTrain(new Train("T002", "IC 2000", null));
            trainListModel.addTrain(new Train("T003", "RABe 523", null));
        });

        ResourceBundle messages = ResourceBundle.getBundle("i18n.app-messages");
        TrainListPanel trainListPanel = GuiActionRunner.execute(() -> new TrainListPanel(trainListModel, messages));

        JFrame frame = GuiActionRunner.execute(() -> {
            JFrame f = new JFrame("Train List UI Test");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            DockingDesktop desktop = new DockingDesktop();
            f.getContentPane().add(desktop);

            desktop.addDockable(trainListPanel);
            desktop.split(trainListPanel, switchboardPanel, com.vlsolutions.swing.docking.DockingConstants.SPLIT_RIGHT);
            desktop.setDockableWidth(trainListPanel, 0.2d);

            f.setSize(1024, 768);
            return f;
        });

        window = new FrameFixture(frame);
        window.robot().showWindow(window.target(), new Dimension(1024, 768));
        window.robot().waitForIdle();
        GuiActionRunner.execute(() -> window.target().toFront());
        window.robot().waitForIdle();
    }

    @AfterEach
    void tearDown() {
        if (window != null) {
            window.cleanUp();
        }
    }

    @Test
    void switchboardLoadsAndTrainListIsVisible() {
        // Verify the frame is showing
        window.requireVisible();
        window.robot().waitForIdle();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
