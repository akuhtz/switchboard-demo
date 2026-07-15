package com.bidib.switchboard;

import java.awt.Dimension;
import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

import org.assertj.core.api.Assertions;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JPopupMenuFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidib.switchboard.model.Occupancy;
import com.bidib.switchboard.model.RailwayModel;
import com.bidib.switchboard.persistence.LayoutPersistence;
import com.bidib.switchboard.view.SwitchboardPanel;

class SwitchboardAppTest {

    private FrameFixture window;

    private SwitchboardPanel panel;

    @BeforeEach
    void setUp() throws Exception {
        SwitchboardApp app = GuiActionRunner.execute(() -> new SwitchboardApp(false));
        window = new FrameFixture(app.getFrame());
        panel = app.getPanel();
        window.robot().showWindow(window.target(), new Dimension(1024, 768));

        var url = SwitchboardAppTest.class.getResource("/test-data/switchboard3.json");
        Path path = Paths.get(url.toURI());
        GuiActionRunner.execute(() -> {
            LayoutPersistence.load(panel, path);
        });
    }

    @AfterEach
    void tearDown() {
        window.cleanUp();
    }

    @Test
    void frameTitleContainsSwitchboard() {
        Assertions.assertThat(window.target().getTitle()).contains("Model Railway Switchboard");
    }

    @Test
    void fileMenuContainsLoadSaveSaveAsSettingsAndExit() {
        window.menuItemWithPath("File", "Load...").requireVisible();
        window.menuItemWithPath("File", "Save").requireVisible();
        window.menuItemWithPath("File", "Save As...").requireVisible();
        window.menuItemWithPath("File", "Settings", "Light Look and Feel").requireVisible();
        window.menuItemWithPath("File", "Settings", "Dark Look and Feel").requireVisible();
        window.menuItemWithPath("File", "Exit").requireVisible();
    }

    @Test
    void editMenuContainsEditModeLoadDefaultAndOccupancies() {
        window.menuItemWithPath("Edit", "Edit Mode").requireVisible();
        window.menuItemWithPath("Edit", "Load Default Layout").requireVisible();
        window.menuItemWithPath("Edit", "Occupancies...").requireVisible();
    }

    @Test
    void toolbarContainsEditModeToggle() {
        window.toggleButton(new GenericTypeMatcher<>(JToggleButton.class) {
            @Override
            protected boolean isMatching(JToggleButton button) {
                return "Edit Mode".equals(button.getText());
            }
        }).requireVisible();
    }

    @Test
    void settingsMenuHasLightAndDarkItems() {
        window.menuItemWithPath("File", "Settings", "Light Look and Feel").requireVisible();
        window.menuItemWithPath("File", "Settings", "Dark Look and Feel").requireVisible();
    }

    @Test
    void clearSelectionItemVisibleOnlyInEditMode() {
        window.menuItemWithPath("Edit", "Edit Mode").click();
        window.robot().waitForIdle();

        JPopupMenu popup1 = window.robot().showPopupMenu(panel, new Point(16, 16));
        JPopupMenuFixture popupFixture1 = new JPopupMenuFixture(window.robot(), popup1);
        popupFixture1.menuItemWithPath("Clear selection").requireVisible();

        window.robot().pressAndReleaseKey(java.awt.event.KeyEvent.VK_ESCAPE);
        window.robot().waitForIdle();

        window.menuItemWithPath("Edit", "Edit Mode").click();
        window.robot().waitForIdle();

        JPopupMenu popup2 = window.robot().showPopupMenu(panel, new Point(16, 16));
        JPopupMenuFixture popupFixture2 = new JPopupMenuFixture(window.robot(), popup2);
        try {
            popupFixture2.menuItemWithPath("Clear selection").requireNotVisible();
        }
        catch (org.assertj.swing.exception.ComponentLookupException ex) {
            // Item not found — correct for normal mode
        }
    }

    @Test
    void occupancyPersistenceRoundtrip() {
        var model = panel.getModel();
        var el = model.getElement("P-001");

        Occupancy occ = Occupancy.create(42, 7);
        model.addOccupancy(occ);
        el.setOccupancy(occ);

        var data = LayoutPersistence.capture(panel);

        var freshModel = new RailwayModel();
        var freshPanel = new SwitchboardPanel(freshModel);
        LayoutPersistence.apply(freshPanel, data);

        Assertions.assertThat(freshModel.getOccupancies()).hasSize(1);
        var restored = freshModel.getOccupancy(42, 7);
        Assertions.assertThat(restored).isNotNull();
        Assertions.assertThat(restored.getNodeId()).isEqualTo(42);
        Assertions.assertThat(restored.getPortId()).isEqualTo(7);
        Assertions.assertThat(restored.getState()).isEqualTo(Occupancy.OccupancyState.FREE);

        var restoredEl = freshModel.getElement("P-001");
        Assertions.assertThat(restoredEl).isNotNull();
        Assertions.assertThat(restoredEl.getOccupancy()).isSameAs(restored);
    }
}
