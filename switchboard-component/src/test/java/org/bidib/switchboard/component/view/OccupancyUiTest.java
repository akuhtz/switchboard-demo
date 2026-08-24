package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.Timer;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.config.TestOccupancyFactory;
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.bidib.switchboard.component.service.RouterService;
import org.bidib.switchboard.component.util.ScreenRecorder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;

class OccupancyUiTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OccupancyUiTest.class);

    private FrameFixture window;

    private SwitchboardPanel panel;

    private static final int DELAY = 80;

	private final OccupancyFactory occupancyFactory = new TestOccupancyFactory(); 

    @BeforeEach
    void setUp() throws Exception {

        // System.setProperty("screen.recording", "true");

        ScreenRecorder.setEnabled(Boolean.getBoolean("screen.recording"));
        Files.createDirectories(Path.of("target", "surefire-reports"));

        var model = new RailwayModel();

        GuiActionRunner.execute(() -> FlatDarkLaf.setup());

        panel = GuiActionRunner.execute(() -> new SwitchboardPanel(occupancyFactory, (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model, RouterService.createDefault()));

        var url = OccupancyUiTest.class.getResource("/test-data/switchboard5.json");
        Path path = Paths.get(url.toURI());
        var layoutPersistence = new LayoutPersistence();
        GuiActionRunner.execute(() -> layoutPersistence.load(panel, path));

        JFrame frame = GuiActionRunner.execute(() -> {
            JFrame f = new JFrame("Model Railway Switchboard");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JMenuBar menuBar = new JMenuBar();

            JMenu editMenu = new JMenu("Edit");
            editMenu.setMnemonic('E');

            JMenuItem undoItem = new JMenuItem("Undo");
            undoItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke("control Z"));
            undoItem.addActionListener(e -> panel.undoLast());
            editMenu.add(undoItem);
            editMenu.addSeparator();

            JCheckBoxMenuItem editModeItem = new JCheckBoxMenuItem("Edit Mode");
            editModeItem.addActionListener(e -> panel.setEditMode(editModeItem.isSelected()));
            editMenu.add(editModeItem);

            menuBar.add(editMenu);
            f.setJMenuBar(menuBar);

            f.add(panel);
            f.pack();
            return f;
        });

        window = new FrameFixture(frame);
        window.robot().showWindow(window.target(), new Dimension(1024, 768));
    }

    @AfterEach
    void tearDown() {
        if (window != null) {
            window.cleanUp();
        }
    }

    @Test
    void occupancyAdvancesAlongRoute() throws Exception {
        GuiActionRunner.execute(() -> {
            panel.getRouteModel().clear();
            panel.testSetRouteSource(0, 0);
            panel.testFindRoute(10, 1);
        });

        List<int[]> path = panel.getRouteModel().getRoutes().values().iterator().next().getPath();
        assertThat(path).isNotEmpty();

        int limit = path.size();
        LOGGER.info("Route has {} tiles, testing {} steps", path.size(), limit);

        assignOccupanciesToPath(path);

        assertThat(panel.isTileOccupied(path.get(0)[0], path.get(0)[1])).isTrue();
        for (int i = 1; i < limit; i++) {
            assertThat(panel.isTileOccupied(path.get(i)[0], path.get(i)[1])).isFalse();
        }

        ScreenRecorder recorder = null;
        if (ScreenRecorder.isEnabled()) {
            java.awt.Rectangle panelBounds = GuiActionRunner.execute(() -> {
                java.awt.Point loc = panel.getLocationOnScreen();
                return new java.awt.Rectangle(loc.x, loc.y, panel.getWidth(), panel.getHeight());
            });
            Path videoOutput = Path.of("target", "surefire-reports", "occupancy-route-" + System.currentTimeMillis() + ".mp4");
            recorder = ScreenRecorder.startIfEnabled(panelBounds, videoOutput);
        }
        try {
            simulateAndVerifyOccupancy(path);
            clearOccupancies(path);

            if (recorder != null) {
                waitAfterTest();
            }
        }
        finally {
            if (recorder != null) {
                recorder.close();
            }
        }
    }

    @Test
    void routeFromTL003ToTR002() throws Exception {
        GuiActionRunner.execute(() -> {
            panel.getRouteModel().clear();
            panel.testSetRouteSource(22, 0);
            panel.testFindRoute(8, 1);
        });

        List<int[]> path = panel.getRouteModel().getRoute("TL-003-TR-002").getPath();
        assertThat(path).isNotEmpty();

        Integer tl003aspect = panel.getModel().getElementAspect("TL-003");
        LOGGER.info("TL-003 aspect after route={}", tl003aspect);
        assertThat(tl003aspect).isEqualTo(1);

        int limit = path.size();
        LOGGER.info("Route has {} tiles, testing {} steps", path.size(), limit);

        assignOccupanciesToPath(path);

        assertThat(panel.isTileOccupied(path.get(0)[0], path.get(0)[1])).isTrue();
        for (int i = 1; i < limit; i++) {
            assertThat(panel.isTileOccupied(path.get(i)[0], path.get(i)[1])).isFalse();
        }

        ScreenRecorder recorder = null;
        if (ScreenRecorder.isEnabled()) {
            java.awt.Rectangle panelBounds = GuiActionRunner.execute(() -> {
                java.awt.Point loc = window.target().getLocationOnScreen();
                return new java.awt.Rectangle(loc.x, loc.y, window.target().getWidth(), window.target().getHeight());
            });
            Path videoOutput = Path.of("target", "surefire-reports", "route-TL003-TR002-" + System.currentTimeMillis() + ".mp4");
            recorder = ScreenRecorder.startIfEnabled(panelBounds, videoOutput);
        }
        try {
            simulateAndVerifyOccupancy(path);
            clearOccupancies(path);

            if (recorder != null) {
                waitAfterTest();
            }
        }
        finally {
            if (recorder != null) {
                recorder.close();
            }
        }
    }

    @Test
    @Tag("occupancy-ui")
    @Disabled
    void routeFromTL003ToTR002Straight() throws Exception {
        GuiActionRunner.execute(() -> {
            panel.getRouteModel().clear();
            panel.testSetRouteSource(22, 0);
            panel.testFindRoute(0, 0);
        });

        List<int[]> path = panel.getRouteModel().getRoute("TL-003-P-001").getPath();
        assertThat(path).isNotEmpty();

        GuiActionRunner.execute(() -> panel.getRouteModel().clearAlternatives("TL-003-P-001"));

        Integer tl003aspect = panel.getModel().getElementAspect("TL-003");
        LOGGER.info("TL-003 aspect after straight route={}", tl003aspect);
        assertThat(tl003aspect).isEqualTo(0);

        int limit = path.size();
        LOGGER.info("Route has {} tiles, testing {} steps", path.size(), limit);

        assignOccupanciesToPath(path);

        assertThat(panel.isTileOccupied(path.get(0)[0], path.get(0)[1])).isTrue();
        for (int i = 1; i < limit; i++) {
            assertThat(panel.isTileOccupied(path.get(i)[0], path.get(i)[1])).isFalse();
        }

        simulateAndVerifyOccupancy(path);
        clearOccupancies(path);
    }

    @Test
    @Tag("occupancy-ui")
    @Disabled
    void alternativeRouteTL003ToP001() throws Exception {
        GuiActionRunner.execute(() -> {
            panel.getRouteModel().clear();
            panel.testSetRouteSource(22, 0);
            panel.testFindRoute(0, 0);
        });

        String routeId = "TL-003-P-001";
        assertThat(panel.getRouteModel().hasAlternativeRoute(routeId)).isTrue();
        assertThat(panel.getRouteModel().getAlternativeRoutes(routeId)).hasSize(1);

        GuiActionRunner.execute(() -> {
            panel.getRouteModel().swapWithAlternative(routeId);
        });

        List<int[]> path = panel.getRouteModel().getRoute(routeId).getPath();
        assertThat(path).isNotEmpty();

        GuiActionRunner.execute(() -> panel.testSetRouteAspects(path));

        Integer tr003aspect = panel.getModel().getElementAspect("TR-003");
        LOGGER.info("TR-003 aspect after alternative route={}", tr003aspect);
        assertThat(tr003aspect).isEqualTo(1);

        int[][] expected =
            { { 22, 0 }, { 21, 1 }, { 20, 2 }, { 19, 2 }, { 18, 2 }, { 17, 2 }, { 16, 1 }, { 15, 1 }, { 14, 1 }, { 13, 1 }, { 12, 1 }, { 11, 1 }, { 10, 1 },
                { 9, 1 }, { 8, 1 }, { 7, 0 }, { 6, 0 }, { 5, 0 }, { 4, 0 }, { 3, 0 }, { 2, 0 }, { 1, 0 }, { 0, 0 } };
        assertThat(path).hasSize(expected.length);
        for (int i = 0; i < expected.length; i++) {
            assertThat(path.get(i)).as("Tile %d", i).containsExactly(expected[i][0], expected[i][1]);
        }

        int limit = path.size();

        assignOccupanciesToPath(path);

        assertThat(panel.isTileOccupied(path.get(0)[0], path.get(0)[1])).isTrue();
        for (int i = 1; i < limit; i++) {
            assertThat(panel.isTileOccupied(path.get(i)[0], path.get(i)[1])).isFalse();
        }

        ScreenRecorder recorder = null;
        if (ScreenRecorder.isEnabled()) {
            java.awt.Rectangle panelBounds = GuiActionRunner.execute(() -> {
                java.awt.Point loc = window.target().getLocationOnScreen();
                return new java.awt.Rectangle(loc.x, loc.y, window.target().getWidth(), window.target().getHeight());
            });
            Path videoOutput = Path.of("target", "surefire-reports", "route-alternative-TL003-P001-" + System.currentTimeMillis() + ".mp4");
            recorder = ScreenRecorder.startIfEnabled(panelBounds, videoOutput);
        }
        try {
            simulateAndVerifyOccupancy(path);
            clearOccupancies(path);

            if (recorder != null) {
                waitAfterTest();
            }
        }
        finally {
            if (recorder != null) {
                recorder.close();
            }
        }
    }

    @Test
    @Tag("occupancy-ui")
    @Disabled
    void routeP112ToCL013WithAndWithoutPreExistingRoutes() throws Exception {
        LOGGER.info("Test route: P-112-CL-013 with and without pre-existing CR-010-P-130");

        final String routeId = "P-112-CL-013";

        panel.setExhaustiveRouting(true);
        
        ScreenRecorder recorder = null;
        if (ScreenRecorder.isEnabled()) {
            java.awt.Rectangle panelBounds = GuiActionRunner.execute(() -> {
                java.awt.Point loc = window.target().getLocationOnScreen();
                return new java.awt.Rectangle(loc.x, loc.y, window.target().getWidth(), window.target().getHeight());
            });
            Path videoOutput = Path.of("target", "surefire-reports", "routeP112ToCL013WithAndWithoutPreExistingRoutes-" + System.currentTimeMillis() + ".mp4");
            recorder = ScreenRecorder.startIfEnabled(panelBounds, videoOutput);
        }
        
        try {
	        var url = OccupancyUiTest.class.getResource("/test-data/switchboard6.json");
	        Path layoutPath = Paths.get(url.toURI());
	        var layoutPersistence = new LayoutPersistence();
	        GuiActionRunner.execute(() -> layoutPersistence.load(panel, layoutPath));
	
	        GuiActionRunner.execute(() -> {
	            panel.testSetRouteSource(25, 12);
	            panel.testFindRoute(24, 13);
	        });
	
	        Route routeWithRoutes = panel.getRouteModel().getRoute(routeId);
	        LOGGER.info("With pre-existing routes: found={}", routeWithRoutes != null);
	        assertThat(routeWithRoutes).as("Route %s should be found with pre-existing routes", routeId).isNotNull();
	        
	        waitAfterTest(1, TimeUnit.SECONDS);
	
	        GuiActionRunner.execute(() -> panel.getRouteModel().clear());
	
	        GuiActionRunner.execute(() -> {
	            panel.testSetRouteSource(25, 12);
	            panel.testFindRoute(24, 13);
	        });
	
	        GuiActionRunner.execute(() -> {
		        Route routeAfterClear = panel.getRouteModel().getRoute(routeId);
		        LOGGER.info("After clearing routes: found={}", routeAfterClear != null);
		        assertThat(routeAfterClear).as("Route %s should be found after clearing pre-existing routes", routeId).isNotNull();
	        });
	        
	        waitAfterTest(1, TimeUnit.SECONDS);

            assertThat(panel.getRouteModel().hasAlternativeRoute(routeId)).isTrue();
            assertThat(panel.getRouteModel().getAlternativeRoutes(routeId)).hasSize(9);
            
            
            int selectedAlternative = 4;

	        GuiActionRunner.execute(() -> {
	
	            panel.getRouteModel().setSelectedAlternativeIndex(routeId, selectedAlternative);
	            panel.getRouteModel().swapWithAlternative(routeId);
	            
	            Route newRoute = panel.getRouteModel().getRoute(routeId);
                if (newRoute != null) {
                		panel.testSetRouteAspects(newRoute.getPath());
                }
                panel.repaint();
	        });
            
            LOGGER.info("Selected alternative route {}.", selectedAlternative);

	        waitAfterTest(1, TimeUnit.SECONDS);
	        
	        if (recorder != null) {
	            waitAfterTest();
	        }
	    }
	    finally {
	        if (recorder != null) {
	            recorder.close();
	        }
	    }

    }

    @Test
    @Tag("occupancy-ui")
    @Disabled
    void routeCR010ToP130() throws Exception {
        routeTest("CR-010-P-130", new int[] { 24, 17 }, new int[] { 10, 12 }, routeId -> {
        }, routeId -> panel.getRouteModel().getRoute(routeId).getPath());
    }

    @Test
    @Tag("occupancy-ui")
    @Disabled
    void routeP114ToP015() throws Exception {
        routeTest("P-114-P-015", new int[] { 25, 14 }, new int[] { 2, 3 }, routeId -> {
            assertThat(panel.getRouteModel().hasAlternativeRoute(routeId)).isTrue();
            assertThat(panel.getRouteModel().getAlternativeRoutes(routeId)).hasSize(2);
        }, routeId -> panel.getRouteModel().getRoute(routeId).getPath());
    }

    @Test
    @Tag("occupancy-ui")
    @Disabled
    void routeP114ToP015alternative2() throws Exception {

        panel.setExhaustiveRouting(true);

        routeTest("P-114-P-015", new int[] { 25, 14 }, new int[] { 2, 3 }, routeId -> {
            assertThat(panel.getRouteModel().hasAlternativeRoute(routeId)).isTrue();
            assertThat(panel.getRouteModel().getAlternativeRoutes(routeId)).hasSize(5);
        }, routeId -> {
            panel.getRouteModel().setSelectedAlternativeIndex(routeId, 1);
            panel.getRouteModel().swapWithAlternative(routeId);
            List<int[]> newPath = panel.getRouteModel().getRoute(routeId).getPath();
            panel.testSetRouteAspects(newPath);
            Integer tr013aspect = panel.getModel().getElementAspect("TR-013");
            assertThat(tr013aspect).as("TR-013 should be diverted (aspect=1) after alternative swap").isEqualTo(1);
            return newPath;
        });
    }

    @Test
    @Tag("occupancy-ui")
    @Disabled
    void simulationTerminatesAtBumperStop() throws Exception {
        // Load switchboard8.json which has BS-001 at (20,5)
        var url = OccupancyUiTest.class.getResource("/test-data/switchboard8.json");
        Path layoutPath = Paths.get(url.toURI());
        var layoutPersistence = new LayoutPersistence();
        GuiActionRunner.execute(() -> layoutPersistence.load(panel, layoutPath));

        // Create route from P-038 (14,5) to BS-001 (20,5)
        GuiActionRunner.execute(() -> {
            panel.getRouteModel().clear();
            panel.testSetRouteSource(14, 5);
            panel.testFindRoute(20, 5);
        });

        String routeId = "P-038-BS-001";
        Route route = panel.getRouteModel().getRoute(routeId);
        assertThat(route).as("Route %s should be found", routeId).isNotNull();
        assertThat(route.getPath().get(route.getPath().size() - 1))
            .as("Route should end at bumper stop (20,5)").containsExactly(20, 5);

        List<int[]> path = route.getPath();
        LOGGER.info("Route {} has {} tiles", routeId, path.size());

        // Set signal SM3-013 at (15,5) to green so the train can pass
        GuiActionRunner.execute(() -> panel.getModel().setElementAspect("SM3-013", 1));

        // Start the occupancy simulation
        GuiActionRunner.execute(() -> panel.testStartOccupancySimulation(route, DELAY));
        window.robot().waitForIdle();

        // Verify simulation started
        assertThat(panel.isAnySimulationRunning()).as("Simulation should be running after start").isTrue();

        // Wait for the simulation to complete (should terminate at bumper stop)
        int totalWaitMs = path.size() * 300 + 3000; // generous margin
        Semaphore done = new Semaphore(0);
        Timer watchdog = new Timer(100, e -> {
            if (!panel.isAnySimulationRunning()) {
                ((Timer) e.getSource()).stop();
                done.release();
            }
        });
        GuiActionRunner.execute(() -> watchdog.start());
        boolean finished = done.tryAcquire(totalWaitMs, TimeUnit.MILLISECONDS);
        GuiActionRunner.execute(() -> watchdog.stop());
        assertThat(finished).as("Simulation should complete within timeout").isTrue();

        // Verify the train reached the bumper stop at (20,5)
        assertThat(panel.isTileOccupied(20, 5))
            .as("Bumper stop tile (20,5) should be occupied at end of simulation").isTrue();

        LOGGER.info("Simulation terminated at bumper stop (20,5)");
    }

    @Test
    @Tag("occupancy-ui")
    @Disabled
    void simulationStopsAfter5Seconds() throws Exception {
        GuiActionRunner.execute(() -> {
            panel.getRouteModel().clear();
            panel.testSetRouteSource(0, 0);
            panel.testFindRoute(10, 1);
        });

        String routeId = panel.getRouteModel().getRoutes().keySet().iterator().next();
        Route route = panel.getRouteModel().getRoute(routeId);
        assertThat(route).isNotNull();

        // Start the occupancy simulation
        GuiActionRunner.execute(() -> panel.testStartOccupancySimulation(route, DELAY));

        // Verify simulation is running
        assertThat(panel.isAnySimulationRunning()).as("Simulation should be running").isTrue();

        // Wait 5 seconds
        Semaphore done = new Semaphore(0);
        Timer waitTimer = new Timer(5000, e -> {
            ((Timer) e.getSource()).stop();
            done.release();
        });
        waitTimer.setRepeats(false);
        GuiActionRunner.execute(() -> waitTimer.start());
        done.acquire();
        window.robot().waitForIdle();

        // Stop the simulation
        GuiActionRunner.execute(() -> {
            var sim = panel.getSimulation(routeId);
            if (sim != null && sim.isRunning()) {
                sim.stop();
            }
            // Also stop the timer
            Timer occupancyTimer = panel.getOccupancyTimer();
            if (occupancyTimer != null && occupancyTimer.isRunning()) {
                occupancyTimer.stop();
            }
        });

        window.robot().waitForIdle();

        // Verify simulation is no longer running
        assertThat(panel.isAnySimulationRunning()).as("Simulation should be stopped").isFalse();

        // Verify the train advanced but didn't reach the end (route is longer than 5s at 200ms/step = 25 steps max)
        var sim = panel.getSimulation(routeId);
        if (sim != null) {
            LOGGER.info("Simulation stopped at index {} of {} tiles", sim.getCurrentIndex(), route.getPath().size());
            assertThat(sim.getCurrentIndex()).as("Train should have advanced some steps").isGreaterThan(1);
        }
    }

    @Test
    @Tag("occupancy-ui")
    @Disabled
    void distantSignalDoesNotStopTrainAndMirrorsNextSignal() throws Exception {
        // Load switchboard3a.json: distant signal SV-001 (10,5) ahead of main signal SM3-003 (7,5)
        var url = OccupancyUiTest.class.getResource("/test-data/switchboard3a.json");
        Path layoutPath = Paths.get(url.toURI());
        var layoutPersistence = new LayoutPersistence();
        GuiActionRunner.execute(() -> layoutPersistence.load(panel, layoutPath));

        // Pre-existing route P-028-DG-002 runs R→L through SV-001 then SM3-003
        String routeId = "P-028-DG-002";
        Route route = panel.getRouteModel().getRoute(routeId);
        assertThat(route).as("Persisted route should be loaded").isNotNull();

        List<int[]> path = route.getPath();
        assertThat(path.stream().anyMatch(p -> p[0] == 10 && p[1] == 5)).as("Route should go via SV-001 (10,5)").isTrue();
        assertThat(path.stream().anyMatch(p -> p[0] == 7 && p[1] == 5)).as("Route should go via SM3-003 (7,5)").isTrue();

        // Enable auto-change so the train can pass the red main signal after 2s
        panel.setAutoChangeSignal(true);

        // Distant signal must never block the train, regardless of aspect
        Tile sv = panel.getTile(10, 5);
        assertThat(panel.isSignalAtRed(sv)).as("Distant signal is not a blocking (red) signal").isFalse();
        assertThat(panel.isSignalBlocking(sv, ElementType.PORT_RIGHT))
            .as("Distant signal must not block the train entering from RIGHT").isFalse();

        // Main signal at aspect 0 (red), rot 180 → faces RIGHT → blocks train entering from RIGHT
        Tile s2009 = panel.getTile(7, 5);
        assertThat(panel.isSignalAtRed(s2009)).as("Main signal should be at red").isTrue();
        assertThat(panel.isSignalBlocking(s2009, ElementType.PORT_RIGHT))
            .as("Main signal (rot 180) should block train entering from RIGHT").isTrue();

        // Start the simulation — the distant signal mirrors the next signal's aspect
        GuiActionRunner.execute(() -> panel.testStartOccupancySimulation(route, DELAY));

        // The file stored SV-001 at aspect 1; after start it must mirror SM3-003 (aspect 0)
        assertThat(panel.getModel().getElementAspect("SV-001"))
            .as("Distant signal should mirror the main signal's aspect after start")
            .isEqualTo(panel.getModel().getElementAspect("SM3-003"));

        // Wait for the simulation to complete
        int totalWaitMs = path.size() * 300 + 3000;
        Semaphore done = new Semaphore(0);
        Timer watchdog = new Timer(100, e -> {
            if (!panel.isAnySimulationRunning()) {
                ((Timer) e.getSource()).stop();
                done.release();
            }
        });
        GuiActionRunner.execute(() -> watchdog.start());
        boolean finished = done.tryAcquire(totalWaitMs, TimeUnit.MILLISECONDS);
        GuiActionRunner.execute(() -> watchdog.stop());
        assertThat(finished).as("Simulation should complete within timeout").isTrue();

        // Main signal was auto-changed to green (aspect 1); the distant signal mirrors it
        assertThat(panel.getModel().getElementAspect("SM3-003"))
            .as("Main signal should have been auto-changed to green").isEqualTo(1);
        assertThat(panel.getModel().getElementAspect("SV-001"))
            .as("Distant signal should mirror the main signal after auto-change")
            .isEqualTo(panel.getModel().getElementAspect("SM3-003"));

        // The train passed the distant signal and reached the route target
        assertThat(panel.isTileOccupied(5, 4)).as("Train should reach the route target DG-002 (5,4)").isTrue();
    }

    @Test
    @Tag("occupancy-ui")
    @Disabled
    void routeP087ToP015StopsAtFacingSignals() throws Exception {
        // Load switchboard7.json
        var url = OccupancyUiTest.class.getResource("/test-data/switchboard7.json");
        Path layoutPath = Paths.get(url.toURI());
        var layoutPersistence = new LayoutPersistence();
        GuiActionRunner.execute(() -> layoutPersistence.load(panel, layoutPath));

        // Enable auto-change signal so the train resumes after 2s at each stop
        panel.setAutoChangeSignal(true);

        // Create route P-087 (10,10) → P-015 (2,3)
        GuiActionRunner.execute(() -> {
            panel.getRouteModel().clear();
            panel.testSetRouteSource(10, 10);
            panel.testFindRoute(2, 3);
        });

        String routeId = "P-087-P-015";
        assertThat(panel.getRouteModel().getRoute(routeId)).as("Route should be found").isNotNull();
        assertThat(panel.getRouteModel().hasAlternativeRoute(routeId)).as("Alternatives should exist").isTrue();

        // Select Alternative 1 that goes via SM3-015, SM3-012, SM3-006, SM3-001
        GuiActionRunner.execute(() -> {
            panel.getRouteModel().setSelectedAlternativeIndex(routeId, 0);
            panel.getRouteModel().swapWithAlternative(routeId);
            List<int[]> newPath = panel.getRouteModel().getRoute(routeId).getPath();
            panel.testSetRouteAspects(newPath);
        });

        List<int[]> path = panel.getRouteModel().getRoute(routeId).getPath();
        assertThat(path).isNotEmpty();
        LOGGER.info("Route {} has {} tiles", routeId, path.size());

        // Verify route goes through all expected signals
        assertThat(path.stream().anyMatch(p -> p[0] == 19 && p[1] == 4)).as("Route should go via SM3-015 (19,4)").isTrue();
        assertThat(path.stream().anyMatch(p -> p[0] == 15 && p[1] == 4)).as("Route should go via SM3-012 (15,4)").isTrue();
        assertThat(path.stream().anyMatch(p -> p[0] == 8 && p[1] == 4)).as("Route should go via SM3-006 (8,4)").isTrue();
        assertThat(path.stream().anyMatch(p -> p[0] == 3 && p[1] == 3)).as("Route should go via SM3-001 (3,3)").isTrue();

        // Verify signal facing directions:
        // SM3-015 (19,4) rot 180 → faces RIGHT → blocks R→L (our direction) → SHOULD STOP
        // SM3-012 (15,4) rot 0   → faces LEFT  → blocks L→R (opposite) → SHOULD NOT STOP
        // SM3-006 (8,4)  rot 180 → faces RIGHT → blocks R→L (our direction) → SHOULD STOP
        // SM3-001 (3,3)  rot 0   → faces LEFT  → blocks L→R (opposite) → SHOULD NOT STOP
        Tile s2013 = panel.getTile(19, 4);
        Tile s2010 = panel.getTile(15, 4);
        Tile s2006 = panel.getTile(8, 4);
        Tile s3002 = panel.getTile(3, 3);

        // All signals start at aspect 0 (red)
        assertThat(panel.isSignalAtRed(s2013)).isTrue();
        assertThat(panel.isSignalAtRed(s2010)).isTrue();
        assertThat(panel.isSignalAtRed(s2006)).isTrue();
        assertThat(panel.isSignalAtRed(s3002)).isTrue();

        // Train moves R→L, so entry port is RIGHT for horizontal movement
        // SM3-015 rot 180: facing port = (LEFT + 2) % 4 = RIGHT → blocks entry from RIGHT → STOP
        assertThat(panel.isSignalBlocking(s2013, ElementType.PORT_RIGHT))
            .as("SM3-015 (rot 180) should block train entering from RIGHT").isTrue();
        // SM3-012 rot 0: facing port = LEFT → does NOT block entry from RIGHT → PASS
        assertThat(panel.isSignalBlocking(s2010, ElementType.PORT_RIGHT))
            .as("SM3-012 (rot 0) should NOT block train entering from RIGHT").isFalse();
        // SM3-006 rot 180: facing port = RIGHT → blocks entry from RIGHT → STOP
        assertThat(panel.isSignalBlocking(s2006, ElementType.PORT_RIGHT))
            .as("SM3-006 (rot 180) should block train entering from RIGHT").isTrue();
        // SM3-001 rot 0: facing port = LEFT → does NOT block entry from RIGHT → PASS
        assertThat(panel.isSignalBlocking(s3002, ElementType.PORT_RIGHT))
            .as("SM3-001 (rot 0) should NOT block train entering from RIGHT").isFalse();

        // Now run the actual occupancy simulation and verify stopping behavior
        assignOccupanciesToPath(path);

        // Find indices of the signal tiles in the path
        int idxS2013 = -1, idxS2010 = -1, idxS2006 = -1, idxS3002 = -1;
        for (int i = 0; i < path.size(); i++) {
            int[] p = path.get(i);
            if (p[0] == 19 && p[1] == 4) idxS2013 = i;
            if (p[0] == 15 && p[1] == 4) idxS2010 = i;
            if (p[0] == 8 && p[1] == 4) idxS2006 = i;
            if (p[0] == 3 && p[1] == 3) idxS3002 = i;
        }
        LOGGER.info("Signal indices in path: SM3-015={}, SM3-012={}, SM3-006={}, SM3-001={}", idxS2013, idxS2010, idxS2006, idxS3002);

        // Use the simulation timer (200ms per step, signals auto-change after 2s)
        // We wait long enough for the simulation to complete, including 2 signal stops of ~2s each
        // Total: path.size() * 200ms + 2 * 2000ms ≈ path.size()*0.2 + 4 seconds
        int totalWaitMs = path.size() * 200 + 2 * 2500 + 2000; // generous margin

        // Start the built-in simulation
        GuiActionRunner.execute(() -> panel.testStartOccupancySimulation(panel.getRouteModel().getRoute(routeId), DELAY));

        // Wait for completion — poll for the timer to stop
        Semaphore done = new Semaphore(0);
        Timer watchdog = new Timer(200, e -> {
            if (panel.getOccupancyTimer() == null || !panel.getOccupancyTimer().isRunning()) {
                ((Timer) e.getSource()).stop();
                done.release();
            }
        });
        GuiActionRunner.execute(() -> watchdog.start());
        boolean finished = done.tryAcquire(totalWaitMs, TimeUnit.MILLISECONDS);
        GuiActionRunner.execute(() -> watchdog.stop());
        assertThat(finished).as("Simulation should complete within timeout").isTrue();

        LOGGER.info("Simulation completed. Verifying signal auto-changes.");

        // After simulation, SM3-015 and SM3-006 should have been auto-changed to aspect 1
        assertThat(panel.getModel().getElementAspect("SM3-015"))
            .as("SM3-015 should have been auto-changed to green").isEqualTo(1);
        assertThat(panel.getModel().getElementAspect("SM3-006"))
            .as("SM3-006 should have been auto-changed to green").isEqualTo(1);
        // SM3-012 and SM3-001 should still be at aspect 0 (never auto-changed because train didn't stop)
        assertThat(panel.getModel().getElementAspect("SM3-012"))
            .as("SM3-012 should still be red (train passed without stopping)").isEqualTo(0);
        assertThat(panel.getModel().getElementAspect("SM3-001"))
            .as("SM3-001 should still be red (train passed without stopping)").isEqualTo(0);

        clearOccupancies(path);
    }

    void routeTest(String routeId, int[] source, int[] target, final Consumer<String> validation, final Function<String, List<int[]>> routeSelector)
        throws Exception {
        LOGGER.info("Test route: {}", routeId);

        ScreenRecorder recorder = null;
        if (ScreenRecorder.isEnabled()) {
            java.awt.Rectangle panelBounds = GuiActionRunner.execute(() -> {
                java.awt.Point loc = window.target().getLocationOnScreen();
                return new java.awt.Rectangle(loc.x, loc.y, window.target().getWidth(), window.target().getHeight());
            });
            Path videoOutput = Path.of("target", "surefire-reports", "route-" + routeId + "-" + System.currentTimeMillis() + ".mp4");
            recorder = ScreenRecorder.startIfEnabled(panelBounds, videoOutput);
        }

        try {
        var url = OccupancyUiTest.class.getResource("/test-data/switchboard6.json");
        Path layoutPath = Paths.get(url.toURI());
        var layoutPersistence = new LayoutPersistence();
        GuiActionRunner.execute(() -> layoutPersistence.load(panel, layoutPath));

        GuiActionRunner.execute(() -> {
            panel.getRouteModel().clear();
            panel.testSetRouteSource(source[0], source[1]);
            panel.testFindRoute(target[0], target[1]);
        });

        validation.accept(routeId);

        List<int[]> path = routeSelector.apply(routeId);
        assertThat(path).isNotEmpty();
        GuiActionRunner.execute(() -> panel.getRouteModel().clearAlternatives(routeId));
        int limit = path.size();
        LOGGER.info("Route has {} tiles", limit);

        assignOccupanciesToPath(path);

        assertThat(panel.isTileOccupied(path.get(0)[0], path.get(0)[1])).isTrue();
        for (int i = 1; i < limit; i++) {
            assertThat(panel.isTileOccupied(path.get(i)[0], path.get(i)[1])).isFalse();
        }

            simulateAndVerifyOccupancy(path);
            clearOccupancies(path);

            if (recorder != null) {
                waitAfterTest();
            }
        }
        finally {
            if (recorder != null) {
                recorder.close();
            }
        }
    }

    private void assignOccupanciesToPath(List<int[]> path) {
        GuiActionRunner.execute(() -> {
            for (int i = 0; i < path.size(); i++) {
                int[] p = path.get(i);
                Tile tile = panel.getTile(p[0], p[1]);
                if (tile instanceof ElementTile et && et.getElementId() != null) {
                    Element el = panel.getModel().getElement(et.getElementId());
                    if (el != null) {
                        Occupancy occ = occupancyFactory.create(Occupancy.OccupancyState.FREE);
                        panel.getModel().addOccupancy(occ);
                        el.setOccupancy(occ);
                    }
                }
            }
            int[] first = path.get(0);
            Tile ft = panel.getTile(first[0], first[1]);
            if (ft instanceof ElementTile fet && fet.getElementId() != null) {
                Element fel = panel.getModel().getElement(fet.getElementId());
                if (fel != null) {
                    fel.getOccupancy().setState(Occupancy.OccupancyState.OCCUPIED);
                }
            }
        });
    }

    private void simulateAndVerifyOccupancy(List<int[]> path) {
        int limit = path.size();
        int[] idx = { 1 };
        Semaphore tickComplete = new Semaphore(0);
        Timer timer = new Timer(DELAY, e -> GuiActionRunner.execute(() -> {
            if (idx[0] >= path.size()) {
                ((Timer) e.getSource()).stop();
                return;
            }
            int prev = idx[0] - 1;
            int curr = idx[0];

            int[] pp = path.get(prev);
            Tile pt = panel.getTile(pp[0], pp[1]);
            if (pt instanceof ElementTile pet && pet.getElementId() != null) {
                Element pel = panel.getModel().getElement(pet.getElementId());
                if (pel != null) {
                    pel.getOccupancy().setState(Occupancy.OccupancyState.FREE);
                }
            }

            int[] cp = path.get(curr);
            Tile ct = panel.getTile(cp[0], cp[1]);
            if (ct instanceof ElementTile cet && cet.getElementId() != null) {
                Element cel = panel.getModel().getElement(cet.getElementId());
                if (cel != null) {
                    cel.getOccupancy().setState(Occupancy.OccupancyState.OCCUPIED);
                }
            }

            idx[0]++;
            tickComplete.release();
        }));

        GuiActionRunner.execute(() -> timer.start());

        try {
            for (int step = 1; step < limit; step++) {
                tickComplete.acquire();
                window.robot().waitForIdle();

                for (int i = 0; i < limit; i++) {
                    if (i == step) {
                        assertThat(panel.isTileOccupied(path.get(i)[0], path.get(i)[1]))
                            .as("Tile %d (%d,%d) should be occupied at step %d", i, path.get(i)[0], path.get(i)[1], step).isTrue();
                    }
                    else {
                        assertThat(panel.isTileOccupied(path.get(i)[0], path.get(i)[1]))
                            .as("Tile %d (%d,%d) should be free at step %d", i, path.get(i)[0], path.get(i)[1], step).isFalse();
                    }
                }
            }
        }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        }
        finally {
            GuiActionRunner.execute(() -> timer.stop());
        }
    }

    private void clearOccupancies(List<int[]> path) {
        GuiActionRunner.execute(() -> {
            for (int[] p : path) {
                Tile tile = panel.getTile(p[0], p[1]);
                if (tile instanceof ElementTile et && et.getElementId() != null) {
                    Element el = panel.getModel().getElement(et.getElementId());
                    if (el != null && el.getOccupancy() != null) {
                        el.getOccupancy().setState(Occupancy.OccupancyState.FREE);
                    }
                }
            }
        });
    }

    private void waitAfterTest() {
    		waitAfterTest(1, TimeUnit.SECONDS);
    }
    
    private void waitAfterTest(long timeout, TimeUnit timeUnit) {
        Semaphore tickCompleteWait = new Semaphore(0);
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        Timer timerWait = new Timer(DELAY, e -> GuiActionRunner.execute(() -> {
            LOGGER.info("Wait {} {} after test.", timeout, timeUnit);
            countDownLatch.await(timeout, timeUnit);
            LOGGER.info("Wait {} {} after test passed.", timeout, timeUnit);

            tickCompleteWait.release();
        }));
        GuiActionRunner.execute(() -> timerWait.start());

        try {
            tickCompleteWait.acquire();
        }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        }
        finally {
            GuiActionRunner.execute(() -> timerWait.stop());
            LOGGER.info("Wait finished.");
        }
    }
}
