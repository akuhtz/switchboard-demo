package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.Point;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import com.formdev.flatlaf.FlatDarkLaf;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.config.TestOccupancyFactory;
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.service.RouterService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class TurnoutClickUiTest {

    private static final int TILE_SIZE = 32;

    private FrameFixture window;
    private SwitchboardPanel panel;
    private RailwayModel model;

    private final OccupancyFactory occupancyFactory = new TestOccupancyFactory();

    private static final Map<ElementType, List<String>> TURNOUT_SVG = new java.util.LinkedHashMap<>();
    static {
        TURNOUT_SVG.put(ElementType.TURNOUT_LEFT,
            List.of("/icons/tracks/turnout_straight_left.svg", "/icons/tracks/turnout_diverted_left.svg"));
        TURNOUT_SVG.put(ElementType.TURNOUT_RIGHT,
            List.of("/icons/tracks/turnout_straight_right.svg", "/icons/tracks/turnout_diverted_right.svg"));
        TURNOUT_SVG.put(ElementType.TURNOUT_3WAY,
            List.of("/icons/tracks/turnout_3way_straight.svg", "/icons/tracks/turnout_3way_left.svg",
                "/icons/tracks/turnout_3way_right.svg"));
        TURNOUT_SVG.put(ElementType.DIAGONAL_TURNOUT_RIGHT,
            List.of("/icons/tracks/diag_turnout_straight_right.svg", "/icons/tracks/diag_turnout_diverted_right.svg"));
        TURNOUT_SVG.put(ElementType.DIAGONAL_TURNOUT_LEFT,
            List.of("/icons/tracks/diag_turnout_straight_left.svg", "/icons/tracks/diag_turnout_diverted_left.svg"));
    }

    @BeforeEach
    void setUp() {
        model = new RailwayModel();

        GuiActionRunner.execute(() -> FlatDarkLaf.setup());

        panel = GuiActionRunner.execute(() -> new SwitchboardPanel(occupancyFactory,
            (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model, RouterService.createDefault(),
            TURNOUT_SVG.size(), 1, TILE_SIZE));

        GuiActionRunner.execute(() -> {
            int col = 0;
            for (var entry : TURNOUT_SVG.entrySet()) {
                ElementType type = entry.getKey();
                String elementId = type.getPrefix() + "-001";
                panel.setTile(new ElementTile(col, 0, elementId, type, entry.getValue()));
                model.addElement(new Element(elementId, 0, 0));
                col++;
            }
        });

        JFrame frame = GuiActionRunner.execute(() -> {
            JFrame f = new JFrame("Model Railway Switchboard");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JMenuBar menuBar = new JMenuBar();
            JMenu editMenu = new JMenu("Edit");
            editMenu.setMnemonic('E');
            JMenuItem editModeItem = new JMenuItem("Edit Mode");
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
        GuiActionRunner.execute(() -> window.target().toFront());
        window.robot().waitForIdle();
        GuiActionRunner.execute(() -> panel.requestFocusInWindow());
        window.robot().waitForIdle();
    }

    @AfterEach
    void tearDown() {
        window.cleanUp();
    }

    @Test
    void clickingEachTurnoutTypeCyclesItsAspectViaUi() {
        int col = 0;
        for (var entry : TURNOUT_SVG.entrySet()) {
            ElementType type = entry.getKey();
            int aspectCount = entry.getValue().size();
            String elementId = type.getPrefix() + "-001";

            assertThat(model.getElementAspect(elementId)).as("%s starts at aspect 0", type).isEqualTo(0);

            for (int expected = 1; expected < aspectCount; expected++) {
                clickTile(col, 0);
                assertThat(model.getElementAspect(elementId))
                    .as("Clicking %s at (col=%d) advances it to aspect %d", type, col, expected)
                    .isEqualTo(expected);
            }

            clickTile(col, 0);
            assertThat(model.getElementAspect(elementId))
                .as("Clicking %s past its last aspect wraps back to aspect 0", type)
                .isEqualTo(0);

            col++;
        }
    }

    private void clickTile(int col, int row) {
        var point = new Point(col * TILE_SIZE + TILE_SIZE / 2, row * TILE_SIZE + TILE_SIZE / 2);
        window.robot().moveMouse(panel, point);
        window.robot().waitForIdle();
        window.robot().click(panel, point);
        window.robot().waitForIdle();
    }
}