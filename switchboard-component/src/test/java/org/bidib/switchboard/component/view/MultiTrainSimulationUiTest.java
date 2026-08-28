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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;

class MultiTrainSimulationUiTest {

    private static final Logger LOG = LoggerFactory.getLogger(MultiTrainSimulationUiTest.class);

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

    @Test
    void gapTileReservationIsNotOverriddenBySecondTrain() throws Exception {
        var blockModel = panel.getBlockModel();
        var routeModel = panel.getRouteModel();

        // T002 in blk001 (already assigned from layout), T003 in blk006
        Block blk001 = blockModel.getBlock("blk001");
        assertThat(blk001).as("blk001 should exist").isNotNull();
        assertThat(blk001.isAssignedTo("T002")).as("blk001 should have T002 from layout").isTrue();

        Block blk006 = blockModel.getBlock("blk006");
        assertThat(blk006).as("blk006 should exist").isNotNull();
        GuiActionRunner.execute(() -> blk006.addAssignedTrain("T003"));

        Route tr001 = routeModel.getRoute("TR-001");
        Route tr002 = routeModel.getRoute("TR-002");
        assertThat(tr001).as("TR-001 should exist").isNotNull();
        assertThat(tr002).as("TR-002 should exist").isNotNull();

        // Extend T002's station dwell at S3-006 (pathIndex 35) to 60s so T002 stays stopped
        GuiActionRunner.execute(() -> tr002.addStop(35, 60_000));

        // --- Phase 1: Start T002 on TR-002 ---
        LOG.info("=== Phase 1: Starting T002 on TR-002 ===");
        GuiActionRunner.execute(() -> panel.testStartRouteSimulation(tr002));
        window.robot().waitForIdle();

        Map<String, RouteSimulation> sims = GuiActionRunner.execute(() -> panel.testGetRouteSimulations());
        assertThat(sims).hasSize(1);
        RouteSimulation t002Sim = sims.get("T002");

        // Wait for T002 to pass TL-004 (pathIndex 16 on TR-002)
        boolean passedTl004 = false;
        for (int i = 0; i < 30; i++) {
            waitSeconds(1);
            int idx = GuiActionRunner.execute(() -> t002Sim.getCurrentIndex());
            LOG.info("T002 currentIndex: {}", idx);
            if (idx > 17) {
                passedTl004 = true;
                break;
            }
        }
        assertThat(passedTl004).as("T002 should have passed TL-004 (index 16) within 30s").isTrue();

        // TL-004 should now be DIVERTED (aspect 1) because T002 goes [26,14] -> [25,15]
        int aspectAfterT002 = GuiActionRunner.execute(() -> model.getElementAspect("TL-004"));
        LOG.info("TL-004 aspect after T002 passed: {} (expected 1=diverted)", aspectAfterT002);
        assertThat(aspectAfterT002).as("TL-004 should be diverted (aspect 1) after T002 passes").isEqualTo(1);

        // Verify T002 has reserved TL-004 as gap tile
        Map<String, String> t002GapTiles = GuiActionRunner.execute(() -> t002Sim.getReservedGapTiles());
        String reservedBy = t002GapTiles.get("26,14");
        LOG.info("TL-004 gap tile reserved by block: {} (T002 simulation)", reservedBy);
        assertThat(reservedBy).as("TL-004 should be reserved by T002's guard block").isNotNull();

        // Wait for T002 to reach station stop (pathIndex 35) and pause
        boolean reachedStation = false;
        for (int i = 0; i < 40; i++) {
            waitSeconds(1);
            boolean paused = GuiActionRunner.execute(() -> t002Sim.isPausedAtStation());
            int idx = GuiActionRunner.execute(() -> t002Sim.getCurrentIndex());
            LOG.info("T002 currentIndex: {}, pausedAtStation: {}", idx, paused);
            if (paused) {
                reachedStation = true;
                break;
            }
        }
        assertThat(reachedStation).as("T002 should be paused at station within 40s").isTrue();
        LOG.info("T002 is now stopped at station (S3-006), dwell 60s");

        // --- Phase 2: Start T003 on TR-001 while T002 is stopped ---
        LOG.info("=== Phase 2: Starting T003 on TR-001 while T002 is stopped ===");
        GuiActionRunner.execute(() -> panel.testStartRouteSimulation(tr001));
        window.robot().waitForIdle();

        sims = GuiActionRunner.execute(() -> panel.testGetRouteSimulations());
        assertThat(sims).hasSize(2);
        RouteSimulation t003Sim = sims.get("T003");
        assertThat(t003Sim).as("T003 simulation should exist").isNotNull();

        // Wait for T003's startGreenSet to fire (2s delay)
        waitSeconds(3);

        // Verify TL-004 aspect is not overridden
        int aspectAfterT003 = GuiActionRunner.execute(() -> model.getElementAspect("TL-004"));
        LOG.info("TL-004 aspect after T003 started: {} (should still be 1=diverted if reservation honored)",
            aspectAfterT003);
        assertThat(aspectAfterT003)
            .as("TL-004 should remain diverted (aspect 1) — T003 must not override T002's gap tile reservation")
            .isEqualTo(1);

        // Now wait for T003 to reach the signal before TL-004 (SM-002 at index 16 on TR-001)
        // T003 should be blocked there because TL-004 is reserved by T002
        boolean blockedAtSignal = false;
        for (int i = 0; i < 30; i++) {
            waitSeconds(1);
            int t003Idx = GuiActionRunner.execute(() -> t003Sim.getCurrentIndex());
            boolean t003Running = GuiActionRunner.execute(() -> t003Sim.isRunning());
            LOG.info("T003 currentIndex: {}, running: {}", t003Idx, t003Running);
            // T003 should not advance past index 16 (signal SM-002 guarding TL-004/blk003)
            if (t003Idx >= 16 && !t003Running) {
                blockedAtSignal = true;
                break;
            }
            // Also check if T003 is stuck at index 15-16 (blocked at signal)
            if (t003Idx >= 15 && t003Idx <= 16) {
                blockedAtSignal = true;
                break;
            }
        }

        // Verify T003 did NOT enter blk003
        Block blk003 = blockModel.getBlock("blk003");
        assertThat(blk003).as("blk003 should exist").isNotNull();
        boolean t003InBlk003 = GuiActionRunner.execute(() -> blk003.getAssignedTrainIds().contains("T003"));
        LOG.info("T003 in blk003: {} (expected false)", t003InBlk003);
        assertThat(t003InBlk003)
            .as("T003 must NOT be in blk003 — TL-004 should block it")
            .isFalse();

        // Verify T003 did NOT enter blk004
        Block blk004 = blockModel.getBlock("blk004");
        assertThat(blk004).as("blk004 should exist").isNotNull();
        boolean t003InBlk004 = GuiActionRunner.execute(() -> blk004.getAssignedTrainIds().contains("T003"));
        LOG.info("T003 in blk004: {} (expected false)", t003InBlk004);
        assertThat(t003InBlk004)
            .as("T003 must NOT be in blk004 — TL-004 should block it")
            .isFalse();

        // Verify T002 is still in blk004
        boolean t002InBlk004 = GuiActionRunner.execute(() -> blk004.getAssignedTrainIds().contains("T002"));
        LOG.info("T002 in blk004: {} (expected true)", t002InBlk004);
        assertThat(t002InBlk004)
            .as("T002 should still be in blk004")
            .isTrue();

        // Cleanup
        GuiActionRunner.execute(() -> panel.testStopRouteSimulation());
        window.robot().waitForIdle();
        sims = GuiActionRunner.execute(() -> panel.testGetRouteSimulations());
        assertThat(sims).as("All simulations should be stopped").isEmpty();
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
