package com.bidib.switchboard;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidib.switchboard.model.Element;
import com.bidib.switchboard.model.ElementTile;
import com.bidib.switchboard.model.ElementType;
import com.bidib.switchboard.model.RailwayModel;
import com.bidib.switchboard.model.Tile;
import com.bidib.switchboard.persistence.LayoutPersistence;
import com.bidib.switchboard.persistence.SettingsData.LookAndFeel;
import com.bidib.switchboard.persistence.SettingsManager;
import com.bidib.switchboard.view.SwitchboardPanel;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

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
        settings = new SettingsManager();

        if (LookAndFeel.DARK == settings.getLookAndFeel()) {
            FlatDarkLaf.setup();
        }
        else {
            FlatLightLaf.setup();
        }

        frame = new JFrame("Model Railway Switchboard");
        buildDefaultLayout();
        panel.setTileContextHandler(this::onTileContextAction);
        tryAutoLoad();
        buildMenu();
        buildFrame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SwitchboardApp::new);
    }

    // --- Setup ---

    private void buildDefaultLayout() {
        model.addElement(new Element("TL-001", 0, 0));
        model.addElement(new Element("TR-001", 0, 0));
        model.addElement(new Element("T3-001", 0, 0));
        model.addElement(new Element("S2-001", 0, 0));
        model.addElement(new Element("S3-001", 0, 0));

        panel
            .setTile(
                new ElementTile(2, 3, "TL-001", ElementType.TURNOUT_LEFT, List.of("/icons/turnout_straight_left.svg", "/icons/turnout_diverted_left.svg")));
        panel
            .setTile(
                new ElementTile(3, 3, "TR-001", ElementType.TURNOUT_RIGHT, List.of("/icons/turnout_straight_right.svg", "/icons/turnout_diverted_right.svg")));
        panel
            .setTile(new ElementTile(4, 3, "T3-001", ElementType.TURNOUT_3WAY,
                List.of("/icons/turnout_3way_straight.svg", "/icons/turnout_3way_left.svg", "/icons/turnout_3way_right.svg")));
        panel.setTile(new ElementTile(10, 3, "S2-001", ElementType.SIGNAL_2, List.of("/icons/signal_2_red.svg", "/icons/signal_2_green.svg")));
        panel
            .setTile(new ElementTile(11, 3, "S3-001", ElementType.SIGNAL_3,
                List.of("/icons/signal_3_red.svg", "/icons/signal_3_yellow.svg", "/icons/signal_3_green.svg")));

        for (int col = 0; col < 5; col++) {
            String id = "P-" + String.format("%03d", col + 1);
            model.addElement(new Element(id, 0, 0));
            panel.setTile(new ElementTile(col, 0, id, ElementType.STRAIGHT, List.of("/icons/straight.svg")));
        }

        for (int col = 5; col < SwitchboardPanel.DEFAULT_COLS; col++) {
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
            }
            catch (Exception e) {
                log.warn("Failed to load layout from {}, falling back to default", path, e);
            }
        }
        else {
            log.info("Layout file {} not found, using default layout", path);
        }
    }

    // --- Tile context menu ---

    private void onTileContextAction(int col, int row, ElementType type) {
        Tile existing = panel.getTile(col, row);
        if (existing != null && existing.getElementId() != null) {
            model.removeElement(existing.getElementId());
        }
        if (type == null) {
            panel.removeTile(col, row);
            return;
        }
        String id = generateId(type);
        model.addElement(new Element(id, 0, 0));
        panel.setTile(createDefaultTile(col, row, id, type));
    }

    private String generateId(ElementType type) {
        String prefix = type.getPrefix();
        int max = 0;
        for (String existingId : model.getElements().keySet()) {
            if (existingId.startsWith(prefix + "-")) {
                try {
                    int num = Integer.parseInt(existingId.substring(prefix.length() + 1));
                    if (num > max) {
                        max = num;
                    }
                }
                catch (NumberFormatException ignored) {
                }
            }
        }
        return prefix + "-" + String.format("%03d", max + 1);
    }

    private static Tile createDefaultTile(int col, int row, String id, ElementType type) {
        return switch (type) {
            case TURNOUT_LEFT -> new ElementTile(col, row, id, type, List.of("/icons/turnout_straight_left.svg", "/icons/turnout_diverted_left.svg"));
            case TURNOUT_RIGHT -> new ElementTile(col, row, id, type, List.of("/icons/turnout_straight_right.svg", "/icons/turnout_diverted_right.svg"));
            case TURNOUT_3WAY -> new ElementTile(col, row, id, type,
                List.of("/icons/turnout_3way_straight.svg", "/icons/turnout_3way_left.svg", "/icons/turnout_3way_right.svg"));
            case SIGNAL_2 -> new ElementTile(col, row, id, type, List.of("/icons/signal_2_red.svg", "/icons/signal_2_green.svg"));
            case SIGNAL_3 -> new ElementTile(col, row, id, type, List.of("/icons/signal_3_red.svg", "/icons/signal_3_yellow.svg", "/icons/signal_3_green.svg"));
            case STRAIGHT -> new ElementTile(col, row, id, type, List.of("/icons/straight.svg"));
            case CURVE_LEFT -> new ElementTile(col, row, id, type, List.of("/icons/curve_left.svg"));
            case CURVE_RIGHT -> new ElementTile(col, row, id, type, List.of("/icons/curve_right.svg"));
            case DIAGONAL -> new ElementTile(col, row, id, type, List.of("/icons/diagonal.svg"));
        };
    }

    // --- Menu ---

    private JCheckBoxMenuItem editModeItem;

    private JToggleButton editToggle;

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

        JMenu settingsMenu = new JMenu("Settings");
        settingsMenu.setMnemonic('S');
        ButtonGroup lafGroup = new ButtonGroup();
        JRadioButtonMenuItem lightItem = new JRadioButtonMenuItem("Light Look and Feel");
        lightItem.setSelected(settings.getLookAndFeel() == LookAndFeel.LIGHT);
        lightItem.addActionListener(e -> applyLookAndFeel(LookAndFeel.LIGHT));
        lafGroup.add(lightItem);
        settingsMenu.add(lightItem);
        JRadioButtonMenuItem darkItem = new JRadioButtonMenuItem("Dark Look and Feel");
        darkItem.setSelected(settings.getLookAndFeel() == LookAndFeel.DARK);
        darkItem.addActionListener(e -> applyLookAndFeel(LookAndFeel.DARK));
        lafGroup.add(darkItem);
        settingsMenu.add(darkItem);
        fileMenu.add(settingsMenu);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic('X');
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');
        editModeItem = new JCheckBoxMenuItem("Edit Mode");
        editModeItem.setMnemonic('M');
        editModeItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke("control E"));
        editModeItem.addActionListener(e -> setEditMode(editModeItem.isSelected()));
        editMenu.add(editModeItem);
        menuBar.add(editMenu);

        frame.setJMenuBar(menuBar);
    }

    private void setEditMode(boolean enabled) {
        panel.setEditMode(enabled);
        editModeItem.setSelected(enabled);
        editToggle.setSelected(enabled);
    }

    private void applyLookAndFeel(LookAndFeel laf) {
        settings.setLookAndFeel(laf);
        if (laf == LookAndFeel.DARK) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        SwingUtilities.updateComponentTreeUI(frame);
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
            }
            catch (IOException ex) {
                log.error("Error loading layout from {}", path, ex);
                JOptionPane.showMessageDialog(frame, "Error loading file:\n" + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
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
        }
        catch (IOException ex) {
            log.error("Error saving layout to {}", currentFilePath, ex);
            JOptionPane.showMessageDialog(frame, "Error saving file:\n" + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
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
            }
            catch (IOException ex) {
                log.error("Error saving layout to {}", path, ex);
                JOptionPane.showMessageDialog(frame, "Error saving file:\n" + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- Frame ---

    private void buildFrame() {
        editToggle = new JToggleButton("Edit Mode");
        editToggle.addActionListener(e -> setEditMode(editToggle.isSelected()));

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(editToggle);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(32);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(toolbar, BorderLayout.PAGE_START);
        frame.add(scrollPane, BorderLayout.CENTER);
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
