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
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.bidib.switchboard.component.util.ScreenRecorder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;

class OccupancyUiTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OccupancyUiTest.class);

    private FrameFixture window;

    private SwitchboardPanel panel;

    private static final int DELAY = 200;

    @BeforeEach
    void setUp() throws Exception {

        // System.setProperty("screen.recording", "true");

        ScreenRecorder.setEnabled(Boolean.getBoolean("screen.recording"));
        Files.createDirectories(Path.of("target", "surefire-reports"));

        var model = new RailwayModel();

        GuiActionRunner.execute(() -> FlatDarkLaf.setup());

        panel = GuiActionRunner.execute(() -> new SwitchboardPanel(model));

        var url = OccupancyUiTest.class.getResource("/test-data/switchboard5.json");
        Path path = Paths.get(url.toURI());
        GuiActionRunner.execute(() -> LayoutPersistence.load(panel, path));

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

        GuiActionRunner.execute(() -> {
            for (int i = 0; i < path.size(); i++) {
                int[] p = path.get(i);
                Tile tile = panel.getTile(p[0], p[1]);
                if (tile instanceof ElementTile et && et.getElementId() != null) {
                    Element el = panel.getModel().getElement(et.getElementId());
                    if (el != null) {
                        Occupancy occ = Occupancy.create(1, i, Occupancy.OccupancyState.FREE);
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
            int[] idx = { 1 };
            Semaphore tickComplete = new Semaphore(0);
            Timer timer = new Timer(DELAY, e -> GuiActionRunner.execute(() -> {
                if (idx[0] > path.size()) {
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

        GuiActionRunner.execute(() -> {
            for (int i = 0; i < path.size(); i++) {
                int[] p = path.get(i);
                Tile tile = panel.getTile(p[0], p[1]);
                if (tile instanceof ElementTile et && et.getElementId() != null) {
                    Element el = panel.getModel().getElement(et.getElementId());
                    if (el != null) {
                        Occupancy occ = Occupancy.create(1, i, Occupancy.OccupancyState.FREE);
                        panel.getModel().addOccupancy(occ);
                        LOGGER.info("Set free on element: {}", el.getId());
                        el.setOccupancy(occ);
                    }
                }
            }
            int[] first = path.get(0);
            Tile ft = panel.getTile(first[0], first[1]);
            if (ft instanceof ElementTile fet && fet.getElementId() != null) {
                Element fel = panel.getModel().getElement(fet.getElementId());
                if (fel != null) {
                    LOGGER.info("Set occupied on element: {}", fel.getId());
                    fel.getOccupancy().setState(Occupancy.OccupancyState.OCCUPIED);
                }
            }
        });

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
            int[] idx = { 1 };
            Semaphore tickComplete = new Semaphore(0);
            Timer timer = new Timer(DELAY, e -> GuiActionRunner.execute(() -> {
                if (idx[0] > path.size()) {
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
                        LOGGER.info("Set free on element: {}", pel.getId());
                        pel.getOccupancy().setState(Occupancy.OccupancyState.FREE);
                    }
                }

                int[] cp = path.get(curr);
                Tile ct = panel.getTile(cp[0], cp[1]);
                if (ct instanceof ElementTile cet && cet.getElementId() != null) {
                    Element cel = panel.getModel().getElement(cet.getElementId());
                    if (cel != null) {
                        LOGGER.info("Set occupied on element: {}", cel.getId());
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

        GuiActionRunner.execute(() -> {
            for (int i = 0; i < path.size(); i++) {
                int[] p = path.get(i);
                Tile tile = panel.getTile(p[0], p[1]);
                if (tile instanceof ElementTile et && et.getElementId() != null) {
                    Element el = panel.getModel().getElement(et.getElementId());
                    if (el != null) {
                        Occupancy occ = Occupancy.create(1, i, Occupancy.OccupancyState.FREE);
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

        assertThat(panel.isTileOccupied(path.get(0)[0], path.get(0)[1])).isTrue();
        for (int i = 1; i < limit; i++) {
            assertThat(panel.isTileOccupied(path.get(i)[0], path.get(i)[1])).isFalse();
        }

        int[] idx = { 1 };
        Semaphore tickComplete = new Semaphore(0);
        Timer timer = new Timer(DELAY, e -> GuiActionRunner.execute(() -> {
            if (idx[0] > path.size()) {
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

    @Test
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

        GuiActionRunner.execute(() -> {
            for (int i = 0; i < path.size(); i++) {
                int[] p = path.get(i);
                Tile tile = panel.getTile(p[0], p[1]);
                if (tile instanceof ElementTile et && et.getElementId() != null) {
                    Element el = panel.getModel().getElement(et.getElementId());
                    if (el != null) {
                        Occupancy occ = Occupancy.create(1, i, Occupancy.OccupancyState.FREE);
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
            int[] idx = { 1 };
            Semaphore tickComplete = new Semaphore(0);
            Timer timer = new Timer(DELAY, e -> GuiActionRunner.execute(() -> {
                if (idx[0] > path.size()) {
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
    void routeCR010ToP130() throws Exception {
        routeTest("CR-010-P-130", new int[] { 24, 17 }, new int[] { 10, 12 }, routeId -> {
        }, routeId -> panel.getRouteModel().getRoute(routeId).getPath());
    }

    @Test
    void routeP114ToP015() throws Exception {
        routeTest("P-114-P-015", new int[] { 25, 14 }, new int[] { 2, 3 }, routeId -> {
            assertThat(panel.getRouteModel().hasAlternativeRoute(routeId)).isTrue();
            assertThat(panel.getRouteModel().getAlternativeRoutes(routeId)).hasSize(2);
        }, routeId -> panel.getRouteModel().getRoute(routeId).getPath());
    }

    @Test
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
            return newPath;
        });
    }

    void routeTest(String routeId, int[] source, int[] target, final Consumer<String> validation, final Function<String, List<int[]>> routeSelector)
        throws Exception {
        LOGGER.info("Test route: {}", routeId);

        var url = OccupancyUiTest.class.getResource("/test-data/switchboard6.json");
        Path layoutPath = Paths.get(url.toURI());
        GuiActionRunner.execute(() -> LayoutPersistence.load(panel, layoutPath));

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

        GuiActionRunner.execute(() -> {
            for (int i = 0; i < path.size(); i++) {
                int[] p = path.get(i);
                Tile tile = panel.getTile(p[0], p[1]);
                if (tile instanceof ElementTile et && et.getElementId() != null) {
                    Element el = panel.getModel().getElement(et.getElementId());
                    if (el != null) {
                        Occupancy occ = Occupancy.create(1, i, Occupancy.OccupancyState.FREE);
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
            Path videoOutput = Path.of("target", "surefire-reports", "route-CR010-P130-" + System.currentTimeMillis() + ".mp4");
            recorder = ScreenRecorder.startIfEnabled(panelBounds, videoOutput);
        }
        try {
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

    private void waitAfterTest() {
        Semaphore tickCompleteWait = new Semaphore(0);
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        Timer timerWait = new Timer(DELAY, e -> GuiActionRunner.execute(() -> {
            LOGGER.info("Wait 1s after test.");
            countDownLatch.await(1, TimeUnit.SECONDS);
            LOGGER.info("Wait 1s after test passed.");

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
