package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
import org.bidib.switchboard.component.model.Block;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.bidib.switchboard.component.service.RouterService;
import org.bidib.switchboard.component.util.ScreenRecorder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;

@Disabled
class BlockUiTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockUiTest.class);

    private FrameFixture window;

    private SwitchboardPanel panel;

    private static final int DELAY = 200;

    private final OccupancyFactory occupancyFactory = new TestOccupancyFactory();

    @BeforeEach
    void setUp() throws Exception {
        ScreenRecorder.setEnabled(Boolean.getBoolean("screen.recording"));
        Files.createDirectories(Path.of("target", "surefire-reports"));

        var model = new RailwayModel();

        GuiActionRunner.execute(() -> FlatDarkLaf.setup());

        panel = GuiActionRunner.execute(
            () -> new SwitchboardPanel(occupancyFactory, (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model, RouterService.createDefault()));

        var url = BlockUiTest.class.getResource("/test-data/switchboard-block1.json");
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
    void createBlockFrom16x4To7x4InEditMode() throws Exception {
        window.menuItemWithPath("Edit", "Edit Mode").click();
        window.robot().waitForIdle();
        assertThat(panel.isEditMode()).isTrue();

        ScreenRecorder recorder = null;
        if (ScreenRecorder.isEnabled()) {
            java.awt.Rectangle panelBounds = GuiActionRunner.execute(() -> {
                java.awt.Point loc = window.target().getLocationOnScreen();
                return new java.awt.Rectangle(loc.x, loc.y, window.target().getWidth(), window.target().getHeight());
            });
            Path videoOutput = Path.of("target", "surefire-reports", "block-16x4-7x4-" + System.currentTimeMillis() + ".mp4");
            recorder = ScreenRecorder.startIfEnabled(panelBounds, videoOutput);
        }
        try {
            waitSeconds(2);

            GuiActionRunner.execute(() -> {
                panel.testSetBlockStart(16, 4);
                panel.repaint();
            });
            waitForRepaint();

            Block block = GuiActionRunner.execute(() -> panel.testCreateBlock(7, 4));

            assertThat(block).as("Block from (16,4) to (7,4) should be created").isNotNull();
            assertThat(block.getId()).isEqualTo("blk001");
            assertThat(panel.getBlockModel().size()).isEqualTo(1);
            assertThat(block.size()).as("Block should span the 10 tiles from (16,4) to (7,4)").isEqualTo(10);
            assertThat(panel.getBlockModel().blockIdForTile(16, 4)).isEqualTo("blk001");
            assertThat(panel.getBlockModel().blockIdForTile(7, 4)).isEqualTo("blk001");
            assertThat(panel.getBlockModel().blockIdForTile(11, 4)).isEqualTo("blk001");

            LOGGER.info("Block {} created with {} tiles from (16,4) to (7,4)", block.getId(), block.size());
            panel.repaint();

            waitSeconds(2);

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
    void createBlockThroughCurveRightRotation180InEditMode() throws Exception {
        window.menuItemWithPath("Edit", "Edit Mode").click();
        window.robot().waitForIdle();
        assertThat(panel.isEditMode()).isTrue();

        ScreenRecorder recorder = null;
        if (ScreenRecorder.isEnabled()) {
            java.awt.Rectangle panelBounds = GuiActionRunner.execute(() -> {
                java.awt.Point loc = window.target().getLocationOnScreen();
                return new java.awt.Rectangle(loc.x, loc.y, window.target().getWidth(), window.target().getHeight());
            });
            Path videoOutput = Path.of("target", "surefire-reports", "block-curve-7x5-5x4-" + System.currentTimeMillis() + ".mp4");
            recorder = ScreenRecorder.startIfEnabled(panelBounds, videoOutput);
        }
        try {
            waitSeconds(1);

            GuiActionRunner.execute(() -> {
                panel.testSetBlockStart(7, 5);
                panel.repaint();
            });
            waitForRepaint();

            Block block = GuiActionRunner.execute(() -> panel.testCreateBlock(5, 4));

            assertThat(block).as("Block from (7,5) to (5,4) should be created").isNotNull();
            assertThat(block.getId()).isEqualTo("blk001");
            assertThat(panel.getBlockModel().size()).isEqualTo(1);
            assertThat(block.size()).as("Block should span the 3 tiles through the rotated curve").isEqualTo(3);
            assertThat(block.containsTile(6, 5)).as("Block should pass through CR-002 at (6,5) rotation 180").isTrue();
            assertThat(block.getPath().get(1)).as("CR-002 should be the middle tile of the block path").containsExactly(6, 5);
            assertThat(panel.getBlockModel().blockIdForTile(6, 5)).isEqualTo("blk001");

            LOGGER.info("Block {} created with {} tiles from (7,5) to (5,4), crossing curve tile (6,5)", block.getId(), block.size());
            panel.repaint();

            waitSeconds(2);

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
    void createBlockEndingOnCurveRightRotation0InEditMode() throws Exception {
        window.menuItemWithPath("Edit", "Edit Mode").click();
        window.robot().waitForIdle();
        assertThat(panel.isEditMode()).isTrue();

        ScreenRecorder recorder = null;
        if (ScreenRecorder.isEnabled()) {
            java.awt.Rectangle panelBounds = GuiActionRunner.execute(() -> {
                java.awt.Point loc = window.target().getLocationOnScreen();
                return new java.awt.Rectangle(loc.x, loc.y, window.target().getWidth(), window.target().getHeight());
            });
            Path videoOutput = Path.of("target", "surefire-reports", "block-end-cr-15x1-16x1-" + System.currentTimeMillis() + ".mp4");
            recorder = ScreenRecorder.startIfEnabled(panelBounds, videoOutput);
        }
        try {
            waitSeconds(1);

            GuiActionRunner.execute(() -> {
                panel.testSetBlockStart(15, 1);
                panel.repaint();
            });
            waitForRepaint();

            Block block = GuiActionRunner.execute(() -> panel.testCreateBlock(16, 1));

            assertThat(block).as("Block from (15,1) to (16,1) should be created").isNotNull();
            assertThat(block.getId()).isEqualTo("blk001");
            assertThat(panel.getBlockModel().size()).isEqualTo(1);
            assertThat(block.size()).as("Block should span the 2 tiles ending on the curve").isEqualTo(2);
            assertThat(block.containsTile(16, 1)).as("Block should end on CR-003 at (16,1) rotation 0").isTrue();
            assertThat(block.getPath().get(1)).as("CR-003 should be the last tile of the block path").containsExactly(16, 1);

            LOGGER.info("Block {} created with {} tiles from (15,1) to (16,1), ending on curve tile (16,1)", block.getId(), block.size());
            panel.repaint();

            waitSeconds(2);

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

    private void waitForRepaint() {
        window.robot().waitForIdle();
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

    private void waitAfterTest() {
        Semaphore tickCompleteWait = new Semaphore(0);
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        Timer timerWait = new Timer(DELAY, e -> GuiActionRunner.execute(() -> {
            LOGGER.info("Wait 1 second after test.");
            countDownLatch.await(1, TimeUnit.SECONDS);
            LOGGER.info("Wait after test passed.");
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
