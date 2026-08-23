package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.Semaphore;

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
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.RouteModel;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.FlatDarkLaf;

class RegionSelectMoveUiTest {

    private FrameFixture window;
    private SwitchboardPanel panel;
    private RailwayModel model;
    private RouteModel routeModel;

    private final OccupancyFactory occupancyFactory = new TestOccupancyFactory();

    @BeforeEach
    void setUp() throws Exception {
        model = new RailwayModel();

        GuiActionRunner.execute(() -> FlatDarkLaf.setup());

        panel = GuiActionRunner.execute(
            () -> new SwitchboardPanel(occupancyFactory,
                (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model));

        routeModel = panel.getRouteModel();

        var url = getClass().getResource("/test-data/switchboard3e.json");
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
    void selectRegionAndMoveTilesDown3Right2RoutesPersist() throws Exception {
        // Verify route exists before move
        assertThat(routeModel.getRoutes()).as("Route should exist before move").isNotEmpty();
        Route route = routeModel.getRoutes().values().iterator().next();
        String routeId = route.getId();
        int routeSizeBefore = route.getPath().size();

        // Enter edit mode
        window.menuItemWithPath("Edit", "Edit Mode").click();
        window.robot().waitForIdle();
        assertThat(panel.isEditMode()).isTrue();

        // Select region covering part of the route (cols 8-12, rows 4-6)
        GuiActionRunner.execute(() -> panel.testSelectRegion(8, 4, 12, 6));
        window.robot().waitForIdle();
        
        Set<String> selected = GuiActionRunner.execute(() -> panel.testGetSelectedTiles());
        assertThat(selected).as("Should have selected tiles in the region").isNotEmpty();

        waitSeconds(1);

        // Move tiles down by 3
        for (int i = 0; i < 3; i++) {
            GuiActionRunner.execute(() -> panel.getActionMap().get("moveDown")
                .actionPerformed(new java.awt.event.ActionEvent(panel, 0, "")));
            window.robot().waitForIdle();
        }
        // Move tiles right by 2
        for (int i = 0; i < 2; i++) {
            GuiActionRunner.execute(() -> panel.getActionMap().get("moveRight")
                .actionPerformed(new java.awt.event.ActionEvent(panel, 0, "")));
            window.robot().waitForIdle();
        }

        // Verify route still exists and has been shifted
        assertThat(routeModel.getRoutes()).as("Route should still exist after move").isNotEmpty();
        Route routeAfter = routeModel.getRoutes().get(routeId);
        assertThat(routeAfter).as("Route %s should still exist", routeId).isNotNull();
        assertThat(routeAfter.getPath().size())
            .as("Route path length should be preserved")
            .isEqualTo(routeSizeBefore);

        // Verify some route coordinates have shifted
        // Col 8 row 5 -> col 10 row 8
        boolean foundShiftedTile = false;
        for (int[] coord : routeAfter.getPath()) {
            if (coord[0] == 10 && coord[1] == 8) {
                foundShiftedTile = true;
                break;
            }
        }
        assertThat(foundShiftedTile)
            .as("Route should contain a shifted tile coordinate (10,8)")
            .isTrue();
        
        
        waitSeconds(2);

        // Clear selection and verify route is drawn again
        GuiActionRunner.execute(() -> panel.getActionMap().get("clearSelection")
            .actionPerformed(new java.awt.event.ActionEvent(panel, 0, "")));
        window.robot().waitForIdle();

        Set<String> selectedAfter = GuiActionRunner.execute(() -> panel.testGetSelectedTiles());
        assertThat(selectedAfter).as("Selection should be cleared after Escape").isEmpty();
        
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
        }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        }
    }
}
