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

/**
 * Verifies that gap tiles are reserved when a train enters a new block
 * during movement (not just at signal blocking). Uses switchboard-route-002.json
 * where TR-001 and TR-002 share gap tiles DTL-001 (25,3) and TR-002 (26,4)
 * between blk006/blk001 and blk002, with no signal between blk006 and blk002
 * on TR-001's path.
 *
 * The fix: onTickPostMovement() now reserves gap tiles when the block changes
 * during normal movement, not just at signal blocking or simulation start.
 */
class GapTileReservationOnBlockChangeUiTest {

    private static final Logger LOG = LoggerFactory.getLogger(GapTileReservationOnBlockChangeUiTest.class);

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

        GuiActionRunner.execute(() -> {
            model.getTrainListModel().addTrain(new Train("T002", "IC 2000", null));
            model.getTrainListModel().addTrain(new Train("T003", "Re 460 023", null));
        });

        var url = getClass().getResource("/test-data/switchboard-route-002.json");
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
    void gapTilesReservedOnBlockTransition() throws Exception {
        var blockModel = panel.getBlockModel();
        var routeModel = panel.getRouteModel();

        // T002 in blk001 (already assigned from layout)
        Block blk001 = blockModel.getBlock("blk001");
        assertThat(blk001).as("blk001 should exist").isNotNull();
        assertThat(blk001.isAssignedTo("T002")).as("blk001 should have T002 from layout").isTrue();

        // Replace T001 with T003 in blk006 (layout assigns T001, we need T003)
        Block blk006 = blockModel.getBlock("blk006");
        assertThat(blk006).as("blk006 should exist").isNotNull();
        GuiActionRunner.execute(() -> {
            blk006.removeAssignedTrain("T001");
            blk006.addAssignedTrain("T003");
        });
        assertThat(blk006.isAssignedTo("T003")).as("blk006 should have T003").isTrue();

        Route tr001 = routeModel.getRoute("TR-001");
        Route tr002 = routeModel.getRoute("TR-002");
        assertThat(tr001).as("TR-001 should exist").isNotNull();
        assertThat(tr002).as("TR-002 should exist").isNotNull();

        assertThat(panel.testGetRouteSimulations()).as("No simulations running initially").isEmpty();

        // --- Phase 1: Start T002 on TR-002, wait for it to enter blk002 ---
        LOG.info("=== Starting T002 on TR-002 ===");
        GuiActionRunner.execute(() -> panel.testStartRouteSimulation(tr002));
        window.robot().waitForIdle();

        Map<String, RouteSimulation> sims = GuiActionRunner.execute(() -> panel.testGetRouteSimulations());
        assertThat(sims).hasSize(1);
        RouteSimulation t002Sim = sims.get("T002");

        // Wait for T002 to enter blk002
        Block blk002 = blockModel.getBlock("blk002");
        boolean t002EnteredBlk002 = false;
        for (int i = 0; i < 20; i++) {
            waitSeconds(1);
            boolean inBlk002 = GuiActionRunner.execute(() -> blk002.getAssignedTrainIds().contains("T002"));
            int idx = GuiActionRunner.execute(() -> t002Sim.getCurrentIndex());
            LOG.info("T002 currentIndex: {}, in blk002: {}", idx, inBlk002);
            if (inBlk002) {
                t002EnteredBlk002 = true;
                break;
            }
        }
        assertThat(t002EnteredBlk002).as("T002 should have entered blk002").isTrue();

        // Verify gap tiles DTL-001 and TR-002 are reserved by T002
        Map<String, String> t002GapTiles = GuiActionRunner.execute(() -> t002Sim.getReservedGapTiles());
        LOG.info("T002 reserved gap tiles: {}", t002GapTiles);
        assertThat(t002GapTiles).as("T002 should have reserved gap tiles").containsKey("25,3");
        assertThat(t002GapTiles).as("T002 should have reserved gap tiles").containsKey("26,4");

        // --- Phase 2: Start T003 on TR-001 ---
        // T003 will wait at start signal because blk002 is reserved by T002.
        // Then T002 leaves blk002, T003 proceeds through gap tiles into blk002.
        LOG.info("=== Starting T003 on TR-001 ===");
        GuiActionRunner.execute(() -> panel.testStartRouteSimulation(tr001));
        window.robot().waitForIdle();

        sims = GuiActionRunner.execute(() -> panel.testGetRouteSimulations());
        assertThat(sims).hasSize(2);
        RouteSimulation t003Sim = sims.get("T003");
        assertThat(t003Sim).as("T003 simulation should exist").isNotNull();

        // Wait for T002 to leave blk002 (departure cleanup ~3s)
        boolean t002LeftBlk002 = false;
        for (int i = 0; i < 15; i++) {
            waitSeconds(1);
            boolean stillInBlk002 = GuiActionRunner.execute(() -> blk002.getAssignedTrainIds().contains("T002"));
            LOG.info("T002 still in blk002: {}", stillInBlk002);
            if (!stillInBlk002) {
                t002LeftBlk002 = true;
                break;
            }
        }
        assertThat(t002LeftBlk002).as("T002 should have left blk002").isTrue();

        // Wait for T003 to enter blk002 (advances after T002 leaves)
        boolean t003EnteredBlk002 = false;
        for (int i = 0; i < 20; i++) {
            waitSeconds(1);
            boolean inBlk002 = GuiActionRunner.execute(() -> blk002.getAssignedTrainIds().contains("T003"));
            int idx = GuiActionRunner.execute(() -> t003Sim.getCurrentIndex());
            LOG.info("T003 currentIndex: {}, in blk002: {}", idx, inBlk002);
            if (inBlk002) {
                t003EnteredBlk002 = true;
                break;
            }
        }
        assertThat(t003EnteredBlk002).as("T003 should have entered blk002 after T002 left").isTrue();

        // KEY ASSERTION: T003 should have reserved gap tiles DTL-001 and TR-002
        // when entering blk002, even though there is no signal between blk006 and blk002.
        Map<String, String> t003GapTiles = GuiActionRunner.execute(() -> t003Sim.getReservedGapTiles());
        LOG.info("T003 reserved gap tiles: {}", t003GapTiles);

        assertThat(t003GapTiles)
            .as("T003 should have reserved DTL-001 (25,3) when entering blk002")
            .containsKey("25,3");
        assertThat(t003GapTiles.get("25,3"))
            .as("DTL-001 should be reserved by blk002")
            .isEqualTo("blk002");

        assertThat(t003GapTiles)
            .as("T003 should have reserved TR-002 element (26,4) when entering blk002")
            .containsKey("26,4");
        assertThat(t003GapTiles.get("26,4"))
            .as("TR-002 element should be reserved by blk002")
            .isEqualTo("blk002");

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
