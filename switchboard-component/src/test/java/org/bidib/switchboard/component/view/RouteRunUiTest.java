package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Semaphore;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.Timer;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.config.TestOccupancyFactory;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.Train;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.FlatDarkLaf;

class RouteRunUiTest {

    private FrameFixture window;
    private SwitchboardPanel panel;
    private RailwayModel model;

    private final OccupancyFactory occupancyFactory = new TestOccupancyFactory();

    @BeforeEach
    void setUp() throws Exception {
        model = new RailwayModel();

        GuiActionRunner.execute(() -> FlatDarkLaf.setup());

        panel = GuiActionRunner.execute(
            () -> new SwitchboardPanel(occupancyFactory,
                (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model));

        // Add train T002 to the model (referenced by blk001's assignedTrainId)
        GuiActionRunner.execute(() -> model.getTrainListModel().addTrain(new Train("T002", "IC 2000", null)));

        var url = getClass().getResource("/test-data/switchboard-tr-001.json");
        Path path = Paths.get(url.toURI());
        var layoutPersistence = new LayoutPersistence();
        GuiActionRunner.execute(() -> layoutPersistence.load(panel, path));

        JFrame frame = GuiActionRunner.execute(() -> {
            JFrame f = new JFrame("Model Railway Switchboard");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JMenuBar menuBar = new JMenuBar();
            JMenu editMenu = new JMenu("Edit");
            editMenu.setMnemonic('E');
            menuBar.add(editMenu);
            f.setJMenuBar(menuBar);
            f.add(panel);
            f.pack();
            return f;
        });

        window = new FrameFixture(frame);
        window.robot().showWindow(window.target(), new Dimension(1280, 768));
        window.robot().waitForIdle();
        GuiActionRunner.execute(() -> panel.requestFocusInWindow());
        window.robot().waitForIdle();
    }

    @AfterEach
    void tearDown() {
        if (window != null) {
            window.cleanUp();
        }
    }

    @Test
    void runRouteFromBlock() throws Exception {
        // Verify blk001 exists and has train T002 assigned
        var blockModel = panel.getBlockModel();
        var blk001 = blockModel.getBlock("blk001");
        assertThat(blk001).as("blk001 should exist").isNotNull();
        assertThat(blk001.getAssignedTrainId()).as("blk001 should have train T002 assigned").isEqualTo("T002");

        // Verify TR-002 train route exists and has a path (starts in blk001)
        var routeModel = panel.getRouteModel();
        assertThat(routeModel.getRoutes()).as("Should have routes loaded").isNotEmpty();
        Route tr002 = routeModel.getRoute("TR-002");
        assertThat(tr002).as("TR-002 should exist").isNotNull();
        assertThat(tr002.getPath()).as("TR-002 should have a non-empty path").isNotEmpty();

        // Verify no simulation is running yet
        assertThat(panel.getRouteSimulation()).as("No simulation should be running initially").isNull();

        // Start the simulation directly using test helper
        GuiActionRunner.execute(() -> panel.testStartRouteSimulation(tr002));
        window.robot().waitForIdle();

        // Verify simulation started
        var simulation = GuiActionRunner.execute(() -> panel.getRouteSimulation());
        assertThat(simulation).as("Simulation should have started").isNotNull();
        assertThat(simulation.isRunning()).as("Simulation should be running").isTrue();
        assertThat(simulation.getTrainId()).as("Simulation should use train T002").isEqualTo("T002");
        assertThat(simulation.getRoute().getId()).as("Simulation should run TR-002").isEqualTo("TR-002");

        // Let the simulation run for a few ticks
        waitSeconds(2);

        // Verify simulation has advanced past the starting index
        int currentIndex = GuiActionRunner.execute(() -> panel.getRouteSimulation().getCurrentIndex());
        assertThat(currentIndex).as("Simulation should have advanced past start").isGreaterThan(1);

        // Stop the simulation
        GuiActionRunner.execute(() -> panel.testStopRouteSimulation());
        window.robot().waitForIdle();

        // Verify simulation is stopped
        var simAfterStop = GuiActionRunner.execute(() -> panel.getRouteSimulation());
        assertThat(simAfterStop).as("Simulation should be stopped").isNull();

        waitSeconds(1);
    }

    private void waitSeconds(int seconds) {
        Semaphore done = new Semaphore(0);
        Timer timer = new Timer(seconds * 1000, e -> {
            ((Timer) e.getSource()).stop();
            done.release();
        });
        GuiActionRunner.execute(() -> timer.start());
        try {
            done.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
