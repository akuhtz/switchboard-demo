package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.config.TestOccupancyFactory;
import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.persistence.LayoutData;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.junit.jupiter.api.Test;

class SignalLinkTest {

    private final OccupancyFactory occupancyFactory = new TestOccupancyFactory();

    /** Panel with a scripted removal confirmation so tests avoid a modal dialog. */
    private static class TestablePanel extends SwitchboardPanel {
        int removeChoice = 2;
        TestablePanel(OccupancyFactory occupancyFactory, RailwayModel model) {
            super(occupancyFactory, (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model);
        }
        @Override
        int confirmRemoveMainSignal(List<ElementTile> linked) {
            return removeChoice;
        }
    }

    private TestablePanel loadSignalLayout() throws Exception {
        var url = SignalLinkTest.class.getResource("/test-data/switchboard3a.json");
        Path path = Paths.get(url.toURI());
        RailwayModel model = new RailwayModel();
        TestablePanel panel = new TestablePanel(occupancyFactory, model);
        new LayoutPersistence().load(panel, path);
        return panel;
    }

    @Test
    void linkSurvivesLoad() throws Exception {
        TestablePanel panel = loadSignalLayout();
        Tile sv = panel.getTile(10, 5);
        assertThat(sv).as("Distant signal SV-001 at (10,5) should be loaded").isNotNull();
        assertThat(((ElementTile) sv).getMainSignalId()).isEqualTo("S3-003");
    }

    @Test
    void linkSurvivesPersistenceRoundTrip() throws Exception {
        TestablePanel panel = loadSignalLayout();
        LayoutData data = new LayoutPersistence().capture(panel);

        RailwayModel model2 = new RailwayModel();
        TestablePanel panel2 = new TestablePanel(occupancyFactory, model2);
        new LayoutPersistence().apply(panel2, data);

        Tile sv = panel2.getTile(10, 5);
        assertThat(sv).as("Distant signal should be restored").isNotNull();
        assertThat(((ElementTile) sv).getMainSignalId()).as("Link should survive round-trip").isEqualTo("S3-003");
    }

    @Test
    void switchingMainSignalToZeroSwitchesLinkedDistantSignalToZero() throws Exception {
        TestablePanel panel = loadSignalLayout();
        Tile s3003 = panel.getTile(7, 5);
        assertThat(panel.getModel().getElementAspect("S3-003")).isEqualTo(0);

        // Click the main signal once: 0 -> 1 (green), the distant signal mirrors to 1
        panel.onTileClicked(s3003);
        assertThat(panel.getModel().getElementAspect("S3-003")).isEqualTo(1);
        assertThat(panel.getModel().getElementAspect("SV-001")).isEqualTo(1);

        // Click again: 1 -> 2 (yellow), the distant signal mirrors to 2
        panel.onTileClicked(s3003);
        assertThat(panel.getModel().getElementAspect("S3-003")).isEqualTo(2);
        assertThat(panel.getModel().getElementAspect("SV-001")).isEqualTo(2);

        // Click a third time: 2 -> 0 (red), the distant signal must switch to 0 too
        panel.onTileClicked(s3003);
        assertThat(panel.getModel().getElementAspect("S3-003")).isEqualTo(0);
        assertThat(panel.getModel().getElementAspect("SV-001")).isEqualTo(0);
    }

    @Test
    void clickingLinkedDistantSignalDoesNotChangeItsAspect() throws Exception {
        TestablePanel panel = loadSignalLayout();
        Tile sv = panel.getTile(10, 5);
        assertThat(panel.getModel().getElementAspect("SV-001")).isEqualTo(1);

        // Clicking the linked distant signal must leave its aspect untouched
        panel.onTileClicked(sv);
        assertThat(panel.getModel().getElementAspect("SV-001"))
            .as("Clicking a linked distant signal must not change its aspect").isEqualTo(1);

        // Clicking the main signal still changes the linked distant signal
        Tile s3003 = panel.getTile(7, 5);
        panel.onTileClicked(s3003);
        panel.onTileClicked(s3003);
        panel.onTileClicked(s3003);
        assertThat(panel.getModel().getElementAspect("S3-003")).isEqualTo(0);
        assertThat(panel.getModel().getElementAspect("SV-001")).isEqualTo(0);
    }

    @Test
    void mirrorUndoRestoresBothSignals() throws Exception {
        TestablePanel panel = loadSignalLayout();
        Tile s3003 = panel.getTile(7, 5);
        // 3-aspect main signal cycles 0 -> 1 -> 2 -> 0, mirroring each step
        panel.onTileClicked(s3003);
        panel.onTileClicked(s3003);
        panel.onTileClicked(s3003);
        assertThat(panel.getModel().getElementAspect("S3-003")).isEqualTo(0);
        assertThat(panel.getModel().getElementAspect("SV-001")).isEqualTo(0);

        panel.undoLast();
        assertThat(panel.getModel().getElementAspect("SV-001")).as("Undo distant signal mirror").isEqualTo(2);
        panel.undoLast();
        assertThat(panel.getModel().getElementAspect("S3-003")).as("Undo main signal cycle").isEqualTo(2);
        panel.undoLast();
        panel.undoLast();
        panel.undoLast();
        panel.undoLast();
        assertThat(panel.getModel().getElementAspect("S3-003")).as("Full undo restores main aspect").isEqualTo(0);
        assertThat(panel.getModel().getElementAspect("SV-001")).as("Full undo restores distant aspect").isEqualTo(1);
    }

    @Test
    void unlinkedDistantSignalIsNotAffectedByOtherMainSignals() throws Exception {
        TestablePanel panel = loadSignalLayout();
        Tile s2008 = panel.getTile(8, 2);
        panel.onTileClicked(s2008);
        assertThat(panel.getModel().getElementAspect("S3-004")).isEqualTo(1);
        assertThat(panel.getModel().getElementAspect("SV-001")).as("Unlinked distant signal unchanged").isEqualTo(1);
    }

    @Test
    void suggestMainSignalFindsSignalAhead() throws Exception {
        TestablePanel panel = loadSignalLayout();
        ElementTile sv = (ElementTile) panel.getTile(10, 5);
        assertThat(panel.suggestMainSignalForDistant(sv)).isEqualTo("S3-003");
    }

    @Test
    void removeLinkedChoiceRemovesDistantSignalWithUndo() throws Exception {
        TestablePanel panel = loadSignalLayout();
        panel.removeChoice = SwitchboardPanel.REMOVE_LINKED_OPTION;
        panel.clearTileWithLinkCheck(7, 5);

        assertThat(panel.getTile(7, 5)).as("Main signal removed").isNull();
        assertThat(panel.getTile(10, 5)).as("Linked distant signal removed").isNull();
        assertThat(panel.getModel().getElement("S3-003")).isNull();
        assertThat(panel.getModel().getElement("SV-001")).isNull();

        panel.undoLast();
        assertThat(panel.getTile(7, 5)).as("Undo restores main signal").isNotNull();
        panel.undoLast();
        assertThat(panel.getTile(10, 5)).as("Undo restores distant signal").isNotNull();
        assertThat(((ElementTile) panel.getTile(10, 5)).getMainSignalId())
            .as("Undo restores the link").isEqualTo("S3-003");
    }

    @Test
    void keepChoiceKeepsDistantSignalUnlinked() throws Exception {
        TestablePanel panel = loadSignalLayout();
        panel.removeChoice = SwitchboardPanel.KEEP_DISTANT_OPTION;
        panel.clearTileWithLinkCheck(7, 5);

        assertThat(panel.getTile(7, 5)).as("Main signal removed").isNull();
        assertThat(panel.getTile(10, 5)).as("Distant signal kept").isNotNull();
        assertThat(((ElementTile) panel.getTile(10, 5)).getMainSignalId()).isNull();
    }

    @Test
    void cancelChoiceAbortsRemoval() throws Exception {
        TestablePanel panel = loadSignalLayout();
        panel.removeChoice = 2; // cancel
        panel.clearTileWithLinkCheck(7, 5);

        assertThat(panel.getTile(7, 5)).as("Main signal kept on cancel").isNotNull();
        assertThat(panel.getTile(10, 5)).as("Distant signal kept on cancel").isNotNull();
        assertThat(panel.getModel().getElement("S3-003")).isNotNull();
        assertThat(panel.getModel().getElement("SV-001")).isNotNull();
    }
}
