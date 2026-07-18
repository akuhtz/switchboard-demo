package org.bidib.switchboard.service;

import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bidib.switchboard.model.Element;
import org.bidib.switchboard.model.ElementTile;
import org.bidib.switchboard.model.ElementType;
import org.bidib.switchboard.model.Tile;
import org.bidib.switchboard.persistence.LayoutPersistence;
import org.bidib.switchboard.persistence.SettingsManager;
import org.bidib.switchboard.view.TileGrid;

public class LayoutService {

    private static final Logger log = LoggerFactory.getLogger(LayoutService.class);

    private final TileGrid tileGrid;

    private final SettingsManager settings;

    private final Component parentComponent;

    private Path currentFilePath;

    public LayoutService(TileGrid tileGrid, SettingsManager settings, Component parentComponent) {
        this.tileGrid = tileGrid;
        this.settings = settings;
        this.parentComponent = parentComponent;
    }

    public Path getCurrentFilePath() {
        return currentFilePath;
    }

    public void buildDefaultLayout() {
        var model = tileGrid.getModel();
        model.addElement(new Element("TL-001", 0, 0));
        model.addElement(new Element("TR-001", 0, 0));
        model.addElement(new Element("T3-001", 0, 0));
        model.addElement(new Element("S2-001", 0, 0));
        model.addElement(new Element("S3-001", 0, 0));

        tileGrid.setTile(
            new ElementTile(2, 3, "TL-001", ElementType.TURNOUT_LEFT, List.of("/icons/turnout_straight_left.svg", "/icons/turnout_diverted_left.svg")));
        tileGrid.setTile(
            new ElementTile(3, 3, "TR-001", ElementType.TURNOUT_RIGHT, List.of("/icons/turnout_straight_right.svg", "/icons/turnout_diverted_right.svg")));
        tileGrid.setTile(new ElementTile(4, 3, "T3-001", ElementType.TURNOUT_3WAY,
            List.of("/icons/turnout_3way_straight.svg", "/icons/turnout_3way_left.svg", "/icons/turnout_3way_right.svg")));
        tileGrid.setTile(new ElementTile(10, 3, "S2-001", ElementType.SIGNAL_2, List.of("/icons/signal_2_red.svg", "/icons/signal_2_green.svg")));
        tileGrid.setTile(new ElementTile(11, 3, "S3-001", ElementType.SIGNAL_3,
            List.of("/icons/signal_3_red.svg", "/icons/signal_3_yellow.svg", "/icons/signal_3_green.svg")));

        for (int col = 0; col < 5; col++) {
            String id = "P-" + String.format("%03d", col + 1);
            model.addElement(new Element(id, 0, 0));
            tileGrid.setTile(new ElementTile(col, 0, id, ElementType.STRAIGHT, List.of("/icons/straight.svg")));
        }

        int defaultCols = 60;
        for (int col = 5; col < defaultCols; col++) {
            tileGrid.setTile(new Tile(col, 0, null, "/icons/empty.svg"));
        }
    }

    public void tryAutoLoad() {
        Path path = settings.getLastLayoutFile();
        if (path == null) {
            log.info("No layout file referenced in settings");
            return;
        }
        log.info("Auto-loading layout from: {}", path);
        if (path.toFile().exists()) {
            try {
                LayoutPersistence.load(tileGrid, path);
                currentFilePath = path;
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

    public void loadDefaultLayout() {
        tileGrid.clearTiles();
        tileGrid.getModel().clear();
        currentFilePath = null;
        buildDefaultLayout();
    }

    public void onLoad() {
        JFileChooser chooser = createFileChooser();
        if (chooser.showOpenDialog(parentComponent) == JFileChooser.APPROVE_OPTION) {
            Path path = chooser.getSelectedFile().toPath();
            try {
                LayoutPersistence.load(tileGrid, path);
                currentFilePath = path;
                settings.setLastLayoutFile(path);
                log.info("Loaded layout from {}", path);
            }
            catch (IOException ex) {
                log.error("Error loading layout from {}", path, ex);
                JOptionPane.showMessageDialog(parentComponent, "Error loading file:\n" + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void onSave() {
        if (currentFilePath == null) {
            onSaveAs();
            return;
        }
        try {
            LayoutPersistence.save(tileGrid, currentFilePath);
            log.info("Saved layout to {}", currentFilePath);
        }
        catch (IOException ex) {
            log.error("Error saving layout to {}", currentFilePath, ex);
            JOptionPane.showMessageDialog(parentComponent, "Error saving file:\n" + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void onSaveAs() {
        JFileChooser chooser = createFileChooser();
        if (currentFilePath != null) {
            chooser.setSelectedFile(currentFilePath.toFile());
        }
        if (chooser.showSaveDialog(parentComponent) == JFileChooser.APPROVE_OPTION) {
            Path path = chooser.getSelectedFile().toPath();
            if (!path.toString().endsWith(".json")) {
                path = Paths.get(path + ".json");
            }
            try {
                LayoutPersistence.save(tileGrid, path);
                currentFilePath = path;
                settings.setLastLayoutFile(path);
                log.info("Saved layout to {}", path);
            }
            catch (IOException ex) {
                log.error("Error saving layout to {}", path, ex);
                JOptionPane.showMessageDialog(parentComponent, "Error saving file:\n" + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static JFileChooser createFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Switchboard Layout");
        chooser.setFileFilter(new FileNameExtensionFilter("Switchboard Layout (*.json)", "json"));
        chooser.setAcceptAllFileFilterUsed(true);
        return chooser;
    }
}
