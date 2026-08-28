package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.Timer;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.config.TestOccupancyFactory;
import org.bidib.switchboard.component.model.Block;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.Train;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.bidib.switchboard.component.service.RouterService;
import org.bidib.switchboard.component.simulation.RouteSimulation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.FlatDarkLaf;

class MultiTrainSimulationUiTest {

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
                (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model, RouterService.createDefault()));

        // Add trains to the model
        GuiActionRunner.execute(() -> {
            model.getTrainListModel().addTrain(new Train("T002", "IC 2000", null));
            model.getTrainListModel().addTrain(new Train("T003", "Re 460 023", null));
        });

        var url = getClass().getResource("/test-data/switchboard-route-001.json");
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
    void twoTrainsRunConcurrentlyOnSameRoute() throws Exception {
        var blockModel = panel.getBlockModel();
        var routeModel = panel.getRouteModel();

        // Drop 'IC 2000' (T002) into BM-006 → blk001 (already assigned from layout)
        Block blk001 = blockModel.getBlock("blk001");
        assertThat(blk001).as("blk001 should exist").isNotNull();
        assertThat(blk001.isAssignedTo("T002")).as("blk001 should have T002 from layout").isTrue();

        // Drop 'Re 460 023' (T003) into BM-007 → blk006
        Block blk006 = blockModel.getBlock("blk006");
        assertThat(blk006).as("blk006 should exist").isNotNull();
        GuiActionRunner.execute(() -> blk006.addAssignedTrain("T003"));
        assertThat(blk006.isAssignedTo("T003")).as("blk006 should have T003 assigned").isTrue();

        // Verify routes exist
        Route tr001 = routeModel.getRoute("TR-001");
        Route tr002 = routeModel.getRoute("TR-002");
        assertThat(tr001).as("TR-001 should exist").isNotNull();
        assertThat(tr002).as("TR-002 should exist").isNotNull();

        // No simulations running yet
        assertThat(panel.testGetRouteSimulations()).as("No simulations should be running initially").isEmpty();

        // Start IC 2000 (T002) on TR-002
        GuiActionRunner.execute(() -> panel.testStartRouteSimulation(tr002));
        window.robot().waitForIdle();

        Map<String, RouteSimulation> simsAfterFirst = GuiActionRunner.execute(() -> panel.testGetRouteSimulations());
        assertThat(simsAfterFirst).as("One simulation should be running").hasSize(1);
        assertThat(simsAfterFirst).as("T002 simulation should exist").containsKey("T002");

        // Let first train run for 2 seconds
        waitSeconds(2);

        // Verify T002 simulation has advanced
        RouteSimulation t002Sim = simsAfterFirst.get("T002");
        int t002Index = GuiActionRunner.execute(() -> t002Sim.getCurrentIndex());
        assertThat(t002Index).as("T002 should have advanced past start").isGreaterThan(1);

        // Start Re 460 023 (T003) on TR-001 after 2 seconds
        GuiActionRunner.execute(() -> panel.testStartRouteSimulation(tr001));
        window.robot().waitForIdle();

        Map<String, RouteSimulation> simsAfterSecond = GuiActionRunner.execute(() -> panel.testGetRouteSimulations());
        assertThat(simsAfterSecond).as("Two simulations should be running").hasSize(2);
        assertThat(simsAfterSecond).as("T002 simulation should still exist").containsKey("T002");
        assertThat(simsAfterSecond).as("T003 simulation should exist").containsKey("T003");

        // Both trains should be running
        RouteSimulation t003Sim = simsAfterSecond.get("T003");
        RouteSimulation t002SimRunning = simsAfterSecond.get("T002");
        assertThat(GuiActionRunner.execute(() -> t002SimRunning.isRunning())).as("T002 should still be running").isTrue();
        assertThat(GuiActionRunner.execute(() -> t003Sim.isRunning())).as("T003 should be running").isTrue();

        // Wait for T003 to advance past start (polls up to 10s for the shared block to free up)
        boolean advanced = false;
        for (int i = 0; i < 20; i++) {
            waitSeconds(1);
            int idx = GuiActionRunner.execute(() -> t003Sim.getCurrentIndex());
            if (idx > 1) {
                advanced = true;
                break;
            }
        }
        assertThat(advanced).as("T003 should have advanced past start within 10s").isTrue();

        // Verify both are still running concurrently
        assertThat(GuiActionRunner.execute(() -> t002SimRunning.isRunning())).as("T002 should still be running").isTrue();
        assertThat(GuiActionRunner.execute(() -> t003Sim.isRunning())).as("T003 should still be running").isTrue();

        // Stop all simulations
        GuiActionRunner.execute(() -> panel.testStopRouteSimulation());
        window.robot().waitForIdle();

        // Verify all stopped
        Map<String, RouteSimulation> simsAfterStop = GuiActionRunner.execute(() -> panel.testGetRouteSimulations());
        assertThat(simsAfterStop).as("All simulations should be stopped").isEmpty();
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
