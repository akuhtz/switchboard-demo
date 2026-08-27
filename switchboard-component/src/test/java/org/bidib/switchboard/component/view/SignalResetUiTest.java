package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.Timer;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.config.TestOccupancyFactory;
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.model.Train;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.bidib.switchboard.component.service.RouterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;

class SignalResetUiTest {

    private static final Logger LOG = LoggerFactory.getLogger(SignalResetUiTest.class);

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
    void signalsTurnGreenThenResetToRedAlongRoute() throws Exception {
        Route tr002 = GuiActionRunner.execute(() -> panel.getRouteModel().getRoute("TR-002"));
        assertThat(tr002).as("TR-002 should exist").isNotNull();

        List<int[]> signalPositions = findSignalPositionsOnRoute(tr002);
        assertThat(signalPositions.size()).as("Should find signals on route").isGreaterThanOrEqualTo(2);

        // Verify all signals start at red (aspect 0)
        for (int[] pos : signalPositions) {
            int aspect = getSignalAspect(pos[0], pos[1]);
            LOG.info("Signal at ({},{}) initial aspect: {}", pos[0], pos[1], aspect);
            assertThat(aspect).as("Signal at (%d,%d) should start at red", pos[0], pos[1]).isEqualTo(0);
        }

        // Start simulation
        GuiActionRunner.execute(() -> panel.testStartRouteSimulation(tr002));
        window.robot().waitForIdle();

        // Wait for 2s start delay + train to begin moving
        waitSeconds(3);

        int currentIndex = GuiActionRunner.execute(() -> panel.getRouteSimulation().getCurrentIndex());
        LOG.info("currentIndex after 3s: {}", currentIndex);
        assertThat(currentIndex).as("Train should have started moving").isGreaterThan(1);

        // Wait for train to reach first signal along route and for auto-change
        waitSeconds(4);
        currentIndex = GuiActionRunner.execute(() -> panel.getRouteSimulation().getCurrentIndex());
        LOG.info("currentIndex after 7s: {}", currentIndex);

        // Collect signals that have been green at least once
        List<boolean[]> signalLifecycle = new ArrayList<>();
        for (int[] pos : signalPositions) {
            signalLifecycle.add(new boolean[]{ false, false }); // [wasGreen, currentlyGreen]
        }

        // Poll signal states every 200ms until we see at least one signal turn green and then red
        boolean sawGreen = false;
        boolean sawGreenThenRed = false;
        for (int poll = 0; poll < 200 && !sawGreenThenRed; poll++) {
            waitMillis(200);
            var sim = GuiActionRunner.execute(() -> panel.getRouteSimulation());
            if (sim == null) {
                LOG.info("Poll {}: simulation completed", poll);
                break;
            }

            for (int i = 0; i < signalPositions.size(); i++) {
                int[] pos = signalPositions.get(i);
                int aspect = getSignalAspect(pos[0], pos[1]);
                boolean[] lifecycle = signalLifecycle.get(i);

                if (aspect == 1) {
                    lifecycle[0] = true;
                    lifecycle[1] = true;
                    if (!sawGreen) {
                        LOG.info("Poll {}: Signal at ({},{}) turned GREEN", poll, pos[0], pos[1]);
                        sawGreen = true;
                    }
                } else if (lifecycle[0] && lifecycle[1]) {
                    lifecycle[1] = false;
                    if (!sawGreenThenRed) {
                        LOG.info("Poll {}: Signal at ({},{}) turned back to RED after being green", poll, pos[0], pos[1]);
                        sawGreenThenRed = true;
                    }
                } else {
                    lifecycle[1] = false;
                }
            }
        }

        assertThat(sawGreen)
            .as("At least one signal along the route should have turned green").isTrue();
        assertThat(sawGreenThenRed)
            .as("At least one signal should have turned green then back to red").isTrue();

        // Stop simulation
        GuiActionRunner.execute(() -> panel.testStopRouteSimulation());
        window.robot().waitForIdle();
    }

    private List<int[]> findSignalPositionsOnRoute(Route route) {
        List<int[]> signals = new ArrayList<>();
        for (int[] pos : route.getPath()) {
            Tile tile = GuiActionRunner.execute(() -> panel.getTile(pos[0], pos[1]));
            if (tile instanceof org.bidib.switchboard.component.model.ElementTile et
                && et.getElementType() == ElementType.SIGNAL_M3
                && et.getElementId() != null) {
                signals.add(pos);
            }
        }
        return signals;
    }

    private int getSignalAspect(int col, int row) {
        return GuiActionRunner.execute(() -> {
            Tile tile = panel.getTile(col, row);
            if (tile instanceof org.bidib.switchboard.component.model.ElementTile et && et.getElementId() != null) {
                Element el = panel.getModel().getElement(et.getElementId());
                if (el != null) {
                    return el.getCurrentAspect();
                }
            }
            return -1;
        });
    }

    private void waitSeconds(int seconds) {
        waitMillis(seconds * 1000);
    }

    private void waitMillis(int millis) {
        Semaphore done = new Semaphore(0);
        Timer timer = new Timer(millis, e -> {
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
