package com.bidib.switchboard;

import com.bidib.switchboard.model.RailwayModel;
import com.bidib.switchboard.model.SignalTile;
import com.bidib.switchboard.model.Tile;
import com.bidib.switchboard.model.TurnoutTile;
import com.bidib.switchboard.persistence.LayoutPersistence;
import com.bidib.switchboard.persistence.SettingsManager;
import com.bidib.switchboard.view.SwitchboardPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main application class that wires the model and view together and launches the tile-based switchboard window.
 */
public class SwitchboardApp {

    private static final Logger log = LoggerFactory.getLogger(SwitchboardApp.class);

    private final RailwayModel model;
    private final SwitchboardPanel panel;
    private final JFrame frame;
    private final SettingsManager settings;
    private Path currentFilePath;

    private SwitchboardApp() {
        model = new RailwayModel();
        panel = new SwitchboardPanel(model);
        frame = new JFrame("Model Railway Switchboard");
        settings = new SettingsManager();
        buildDefaultLayout();
        tryAutoLoad();
        buildMenu();
        buildFrame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SwitchboardApp::new);
    }

    // --- Setup ---

    private void buildDefaultLayout() {
        model.addTurnout("W1");
        model.addTurnout("W2");
        model.addTurnout("W3", 3);
        model.addSignal("S1");
        model.addSignal("S2", 3);

        panel.setTile(new TurnoutTile(2, 3, "W1", "/icons/turnout_straight.svg", "/icons/turnout_diverted_left.svg"));
        panel.setTile(new TurnoutTile(3, 3, "W2", "/icons/turnout_straight.svg", "/icons/turnout_diverted_left.svg"));
        panel.setTile(new TurnoutTile(4, 3, "W3", "/icons/turnout_straight.svg", "/icons/turnout_diverted_left.svg", "/icons/turnout_diverted_right.svg"));
        panel.setTile(new SignalTile(10, 3, "S1", "/icons/signal_red.svg", "/icons/signal_green.svg"));
        panel.setTile(new SignalTile(11, 3, "S2", "/icons/signal_red.svg", "/icons/signal_yellow.svg", "/icons/signal_green.svg"));

        for (int col = 0; col < 5; col++) {
            panel.setTile(new Tile(col, 0, null, "/icons/empty.svg"));
        }
    }

    private void tryAutoLoad() {
        Path path = settings.getLastLayoutFile();
        if (path == null) {
            log.info("No layout file referenced in settings, using default layout");
            return;
        }
        log.info("Auto-loading layout from: {}", path);
        if (path.toFile().exists()) {
            try {
                LayoutPersistence.load(panel, path);
                currentFilePath = path;
                updateTitle();
                log.info("Layout loaded from {}", path);
            } catch (Exception e) {
                log.warn("Failed to load layout from {}, falling back to default", path, e);
            }
        } else {
            log.info("Layout file {} not found, using default layout", path);
        }
    }

    // --- Menu ---

    private void buildMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        JMenuItem loadItem = new JMenuItem("Load...");
        loadItem.setMnemonic('L');
        loadItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke("control L"));
        loadItem.addActionListener(this::onLoad);
        fileMenu.add(loadItem);

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setMnemonic('S');
        saveItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke("control S"));
        saveItem.addActionListener(this::onSave);
        fileMenu.add(saveItem);

        JMenuItem saveAsItem = new JMenuItem("Save As...");
        saveAsItem.setMnemonic('A');
        saveAsItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke("control shift S"));
        saveAsItem.addActionListener(this::onSaveAs);
        fileMenu.add(saveAsItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic('X');
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);
    }

    private void onLoad(ActionEvent e) {
        JFileChooser chooser = createFileChooser();
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            Path path = chooser.getSelectedFile().toPath();
            try {
                LayoutPersistence.load(panel, path);
                currentFilePath = path;
                settings.setLastLayoutFile(path);
                updateTitle();
                log.info("Loaded layout from {}", path);
            } catch (IOException ex) {
                log.error("Error loading layout from {}", path, ex);
                JOptionPane.showMessageDialog(frame, "Error loading file:\n" + ex.getMessage(),
                        "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onSave(ActionEvent e) {
        if (currentFilePath == null) {
            onSaveAs(e);
            return;
        }
        try {
            LayoutPersistence.save(panel, currentFilePath);
            log.info("Saved layout to {}", currentFilePath);
        } catch (IOException ex) {
            log.error("Error saving layout to {}", currentFilePath, ex);
            JOptionPane.showMessageDialog(frame, "Error saving file:\n" + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSaveAs(ActionEvent e) {
        JFileChooser chooser = createFileChooser();
        if (currentFilePath != null) {
            chooser.setSelectedFile(currentFilePath.toFile());
        }
        if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            Path path = chooser.getSelectedFile().toPath();
            if (!path.toString().endsWith(".json")) {
                path = Paths.get(path + ".json");
            }
            try {
                LayoutPersistence.save(panel, path);
                currentFilePath = path;
                settings.setLastLayoutFile(path);
                updateTitle();
                log.info("Saved layout to {}", path);
            } catch (IOException ex) {
                log.error("Error saving layout to {}", path, ex);
                JOptionPane.showMessageDialog(frame, "Error saving file:\n" + ex.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- Frame ---

    private void buildFrame() {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(32);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(scrollPane);
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(null);
        updateTitle();
        frame.setVisible(true);
    }

    private void updateTitle() {
        String name = (currentFilePath != null) ? currentFilePath.getFileName().toString() : "untitled";
        frame.setTitle("Model Railway Switchboard - " + name);
    }

    // --- Helpers ---

    private static JFileChooser createFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Switchboard Layout");
        chooser.setFileFilter(new FileNameExtensionFilter("Switchboard Layout (*.json)", "json"));
        chooser.setAcceptAllFileFilterUsed(true);
        return chooser;
    }
}
