package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;

class RouteFindingUiTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteFindingUiTest.class);

    private FrameFixture window;

    private SwitchboardPanel panel;

    @BeforeEach
    void setUp() throws Exception {
        var model = new RailwayModel();

        GuiActionRunner.execute(() -> FlatDarkLaf.setup());

        panel = GuiActionRunner.execute(() -> new SwitchboardPanel(model));

        var url = RouteFindingUiTest.class.getResource("/test-data/switchboard3.json");
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
        window.cleanUp();
    }

    @Test
    void undoRouteCreationViaUI() {
        GuiActionRunner.execute(() -> {
            panel.testSetRouteSource(0, 0);
            panel.testFindRoute(10, 1);
        });

        assertThat(panel.getRouteModel().isEmpty()).isFalse();

        window.menuItemWithPath("Edit", "Undo").click();
        window.robot().waitForIdle();

        assertThat(panel.getRouteModel().isEmpty()).isTrue();
    }

    @Test
    void undoRouteReplaceViaUI() {
        GuiActionRunner.execute(() -> {
            panel.testSetRouteSource(0, 0);
            panel.testFindRoute(10, 1);
        });

        String routeId = panel.getRouteModel().getRoutes().keySet().iterator().next();

        GuiActionRunner.execute(() -> {
            panel.testSetRouteSource(0, 0);
            panel.testFindRoute(10, 1);
        });

        assertThat(panel.getRouteModel().isEmpty()).isFalse();

        window.menuItemWithPath("Edit", "Undo").click();
        window.robot().waitForIdle();

        assertThat(panel.getRouteModel().isEmpty()).isFalse();
        assertThat(panel.getRouteModel().getRoute(routeId)).isNotNull();
    }

    @Test
    void undoRouteClearViaUI() {
        GuiActionRunner.execute(() -> {
            panel.testSetRouteSource(0, 0);
            panel.testFindRoute(10, 1);
        });

        assertThat(panel.getRouteModel().isEmpty()).isFalse();

        GuiActionRunner.execute(() -> {
            panel.testTileContextAction(5, 0, null);
            panel.testSetRouteSource(0, 0);
            panel.testFindRoute(10, 1);
        });

        assertThat(panel.getRouteModel().isEmpty()).isTrue();

        window.menuItemWithPath("Edit", "Undo").click();
        window.robot().waitForIdle();

        assertThat(panel.getRouteModel().isEmpty()).isFalse();
    }

    @Test
    void undoTileCreationOnEmptyCellViaUI() {
        GuiActionRunner.execute(() -> {
            panel.testTileContextAction(15, 5, ElementType.STRAIGHT);
        });

        Tile afterCreate = panel.getTile(15, 5);
        assertThat(afterCreate).isNotNull();
        assertThat(afterCreate.getElementId()).isNotNull();
        assertThat(panel.getModel().getElement(afterCreate.getElementId())).isNotNull();

        window.menuItemWithPath("Edit", "Undo").click();
        window.robot().waitForIdle();

        assertThat(panel.getTile(15, 5)).isNull();
    }

    @Test
    void undoTileReplaceViaUI() {
        Tile original = panel.getTile(0, 0);
        assertThat(original).isNotNull();
        String originalId = original.getElementId();

        GuiActionRunner.execute(() -> {
            panel.testTileContextAction(0, 0, ElementType.CURVE_LEFT);
        });

        Tile replaced = panel.getTile(0, 0);
        assertThat(replaced.getElementId()).isNotEqualTo(originalId);

        window.menuItemWithPath("Edit", "Undo").click();
        window.robot().waitForIdle();

        Tile restored = panel.getTile(0, 0);
        assertThat(restored).isNotNull();
        assertThat(restored.getElementId()).isEqualTo(originalId);
        assertThat(panel.getModel().getElement(originalId)).isNotNull();
    }

    @Test
    void occupiedRouteTilesDetectedViaUI() {
        GuiActionRunner.execute(() -> {
            panel.testSetRouteSource(0, 0);
            panel.testFindRoute(10, 1);
        });

        List<int[]> path = panel.getRouteModel().getRoutes().values().iterator().next().getPath();

        GuiActionRunner.execute(() -> {
            for (int[] p : path) {
                assertThat(panel.isTileOccupied(p[0], p[1])).as("Tile (%d,%d) should not be occupied initially", p[0], p[1]).isFalse();
            }

            // int[] mid = path.get(path.size() / 2);

            int row = 0;

            for (int col = 5; col < 8; col++) {
                LOGGER.info("Get tile, col: {}, row: {}", col, row);
                Tile tile = panel.getTile(col, row);
                String elId = tile.getElementId();
                LOGGER.info("Current elId: {}", elId);

                Element el = panel.getModel().getElement(elId);

                if ("TR-003".equals(el.getId())) {
                    LOGGER.info("Current aspect: {}", el.getCurrentAspect());
                    el.setCurrentAspect(1);
                }

                if (el.getOccupancy() == null) {
                    LOGGER.info("Create new occupancy.");
                    Occupancy occ = Occupancy.create(1, 1, Occupancy.OccupancyState.OCCUPIED);
                    panel.getModel().addOccupancy(occ);
                    el.setOccupancy(occ);
                }
                else {
                    LOGGER.info("Update existing occupancy.");
                    el.getOccupancy().setState(Occupancy.OccupancyState.OCCUPIED);
                }

                assertThat(panel.isTileOccupied(col, row)).isTrue();
            }
        });

        LOGGER.info("After the test.");
    }
}
