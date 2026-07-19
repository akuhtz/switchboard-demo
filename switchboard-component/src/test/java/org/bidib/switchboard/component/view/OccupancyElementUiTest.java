package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

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
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Tile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;

class OccupancyElementUiTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OccupancyElementUiTest.class);

    private FrameFixture window;

    private SwitchboardPanel panel;

    private static final int DELAY = 200;

    private static Map<ElementType, List<String>> svgByType() {
        Map<ElementType, List<String>> map = new LinkedHashMap<>();
        map.put(ElementType.TURNOUT_LEFT, List.of("/icons/turnout_straight_left.svg", "/icons/turnout_diverted_left.svg"));
        map.put(ElementType.TURNOUT_RIGHT, List.of("/icons/turnout_straight_right.svg", "/icons/turnout_diverted_right.svg"));
        map.put(ElementType.TURNOUT_3WAY, List.of("/icons/turnout_3way_straight.svg", "/icons/turnout_3way_left.svg", "/icons/turnout_3way_right.svg"));
        map.put(ElementType.SIGNAL_2, List.of("/icons/signal_2_red.svg", "/icons/signal_2_green.svg"));
        map.put(ElementType.SIGNAL_3, List.of("/icons/signal_3_red.svg", "/icons/signal_3_yellow.svg", "/icons/signal_3_green.svg"));
        map.put(ElementType.STRAIGHT, List.of("/icons/straight.svg"));
        map.put(ElementType.CURVE_LEFT, List.of("/icons/curve_left.svg"));
        map.put(ElementType.CURVE_RIGHT, List.of("/icons/curve_right.svg"));
        map.put(ElementType.DIAGONAL, List.of("/icons/diagonal.svg"));
        return map;
    }

    @BeforeEach
    void setUp() throws Exception {
        var model = new RailwayModel();

        GuiActionRunner.execute(() -> FlatDarkLaf.setup());

        panel = GuiActionRunner.execute(() -> new SwitchboardPanel(model));

        GuiActionRunner.execute(() -> {
            int row = 0;
            int[] rotations = { 0, 90, 180, 270 };
            int[] cols = { 0, 3, 6, 9 };
            int elementCounter = 1;
            for (var entry : svgByType().entrySet()) {
                ElementType type = entry.getKey();
                List<String> svgPaths = entry.getValue();
                int aspectCount = svgPaths.size();
                String prefix = type.getPrefix();
                for (int aspect = 0; aspect < aspectCount; aspect++) {
                    for (int i = 0; i < 4; i++) {
                        String elementId = prefix + "-" + String.format("%03d", elementCounter++);
                        ElementTile tile = new ElementTile(cols[i], row, elementId, type, svgPaths);
                        tile.setRotation(rotations[i]);
                        panel.setTile(tile);
                        Element element = new Element(elementId, 0, 0);
                        element.setCurrentAspect(aspect);
                        panel.getModel().addElement(element);
                    }
                    row++;
                }
            }
        });

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
        window.cleanUp();
    }

    @Test
    void occupancyCyclesThroughAllElements() throws Exception {
        List<Element> elements = new ArrayList<>(panel.getModel().getElements().values());
        assertThat(elements).hasSize(64);

        Map<String, int[]> elementPositions = new LinkedHashMap<>();
        for (Tile tile : panel.getTiles().values()) {
            if (tile instanceof ElementTile et && et.getElementId() != null) {
                elementPositions.put(et.getElementId(), new int[] { tile.getCol(), tile.getRow() });
            }
        }
        assertThat(elementPositions).hasSize(64);

        int limit = elements.size();
        LOGGER.info("Testing {} elements across all types, aspects, and rotations", limit);

        GuiActionRunner.execute(() -> {
            for (int i = 0; i < elements.size(); i++) {
                Element el = elements.get(i);
                Occupancy occ = Occupancy.create(1, i, Occupancy.OccupancyState.FREE);
                panel.getModel().addOccupancy(occ);
                el.setOccupancy(occ);
            }
            Element first = elements.get(0);
            first.getOccupancy().setState(Occupancy.OccupancyState.OCCUPIED);
        });

        int[] idx = { 1 };
        Semaphore tickComplete = new Semaphore(0);
        Timer timer = new Timer(DELAY, e -> GuiActionRunner.execute(() -> {
            if (idx[0] >= limit) {
                ((Timer) e.getSource()).stop();
                return;
            }
            int prev = idx[0] - 1;
            int curr = idx[0];

            Element pel = elements.get(prev);
            if (pel.getOccupancy() != null) {
                LOGGER.info("Set free on element: {}", pel.getId());

                pel.getOccupancy().setState(Occupancy.OccupancyState.FREE);
            }

            Element cel = elements.get(curr);
            if (cel.getOccupancy() != null) {
                LOGGER.info("Set occupied on element: {}", cel.getId());
                cel.getOccupancy().setState(Occupancy.OccupancyState.OCCUPIED);
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
                    Element el = elements.get(i);
                    int[] pos = elementPositions.get(el.getId());
                    if (i == step) {
                        assertThat(panel.isTileOccupied(pos[0], pos[1])).as("Element %s should be occupied at step %d", el.getId(), step).isTrue();
                    }
                    else {
                        assertThat(panel.isTileOccupied(pos[0], pos[1])).as("Element %s should be free at step %d", el.getId(), step).isFalse();
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
            for (Element el : elements) {
                if (el.getOccupancy() != null) {
                    el.getOccupancy().setState(Occupancy.OccupancyState.FREE);
                }
            }
        });
    }

    @Test
    @Disabled("Curve occupancy drawing is being redesigned")
    void occupancyAtCurveRotations() throws Exception {
        List<String> targetIds = List.of("CL-053", "CL-054", "CL-055", "CL-056", "CR-058", "CR-060");
        // List<String> targetIds = List.of("T3-021", "T3-022", "T3-023", "T3-024", "T3-025", "T3-026", "T3-027",
        // "T3-028");
        List<Element> elements = new ArrayList<>();
        Map<String, int[]> positions = new LinkedHashMap<>();
        for (String id : targetIds) {
            Element el = panel.getModel().getElement(id);
            assertThat(el).as("Element %s not found", id).isNotNull();
            elements.add(el);
        }
        for (Tile tile : panel.getTiles().values()) {
            if (tile instanceof ElementTile et && et.getElementId() != null && targetIds.contains(et.getElementId())) {
                positions.put(et.getElementId(), new int[] { tile.getCol(), tile.getRow() });
            }
        }
        assertThat(positions).hasSize(targetIds.size());

        int limit = elements.size();
        LOGGER.info("Testing {} curve rotation elements", limit);

        GuiActionRunner.execute(() -> {
            for (int i = 0; i < elements.size(); i++) {
                Element el = elements.get(i);
                Occupancy occ = Occupancy.create(1, i, Occupancy.OccupancyState.FREE);
                panel.getModel().addOccupancy(occ);
                el.setOccupancy(occ);
            }
            Element first = elements.get(0);
            first.getOccupancy().setState(Occupancy.OccupancyState.OCCUPIED);
        });

        assertThat(panel.isTileOccupied(positions.get(targetIds.get(0))[0], positions.get(targetIds.get(0))[1])).isTrue();
        for (int i = 1; i < limit; i++) {
            int[] p = positions.get(targetIds.get(i));
            assertThat(panel.isTileOccupied(p[0], p[1])).isFalse();
        }

        int[] idx = { 1 };
        Semaphore tickComplete = new Semaphore(0);
        Timer timer = new Timer(DELAY, e -> GuiActionRunner.execute(() -> {
            if (idx[0] >= limit) {
                ((Timer) e.getSource()).stop();
                return;
            }
            int prev = idx[0] - 1;
            int curr = idx[0];

            Element pel = elements.get(prev);
            if (pel.getOccupancy() != null) {
                pel.getOccupancy().setState(Occupancy.OccupancyState.FREE);
            }

            Element cel = elements.get(curr);
            if (cel.getOccupancy() != null) {
                cel.getOccupancy().setState(Occupancy.OccupancyState.OCCUPIED);
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
                    int[] pos = positions.get(targetIds.get(i));
                    if (i == step) {
                        assertThat(panel.isTileOccupied(pos[0], pos[1])).as("Element %s should be occupied at step %d", targetIds.get(i), step).isTrue();
                    }
                    else {
                        assertThat(panel.isTileOccupied(pos[0], pos[1])).as("Element %s should be free at step %d", targetIds.get(i), step).isFalse();
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
            for (Element el : elements) {
                if (el.getOccupancy() != null) {
                    el.getOccupancy().setState(Occupancy.OccupancyState.FREE);
                }
            }
        });
    }
}
