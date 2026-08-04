package org.bidib.switchboard.demoapp.service;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bidib.switchboard.component.persistence.OccupancySerializer;
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.bidib.switchboard.demoapp.persistence.SettingsManager;
import org.bidib.switchboard.component.view.TileGrid;

public class LayoutService {

    private static final Logger log = LoggerFactory.getLogger(LayoutService.class);

    private final TileGrid tileGrid;

    private final SettingsManager settings;

    private final Component parentComponent;

    private Path currentFilePath;

    private final LayoutPersistence layoutPersistence;

    public LayoutService(final OccupancySerializer occupancySerializer, TileGrid tileGrid, SettingsManager settings, Component parentComponent) {
    	this.layoutPersistence = new LayoutPersistence(occupancySerializer);
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
        model.addElement(new Element("S3-001", 0, 0));
        model.addElement(new Element("SV-001", 0, 0));

        tileGrid.setTile(
            new ElementTile(2, 3, "TL-001", ElementType.TURNOUT_LEFT, List.of("/icons/tracks/turnout_straight_left.svg", "/icons/tracks/turnout_diverted_left.svg")));
        tileGrid.setTile(
            new ElementTile(3, 3, "TR-001", ElementType.TURNOUT_RIGHT, List.of("/icons/tracks/turnout_straight_right.svg", "/icons/tracks/turnout_diverted_right.svg")));
        tileGrid.setTile(new ElementTile(4, 3, "T3-001", ElementType.TURNOUT_3WAY,
            List.of("/icons/tracks/turnout_3way_straight.svg", "/icons/tracks/turnout_3way_left.svg", "/icons/tracks/turnout_3way_right.svg")));
        tileGrid.setTile(new ElementTile(11, 3, "S3-001", ElementType.SIGNAL_3,
            List.of("/icons/signals/sbb_l/signal_3_red_left.svg", "/icons/signals/sbb_l/signal_3_green_left.svg", "/icons/signals/sbb_l/signal_3_yellow_left.svg")));
        tileGrid.setTile(new ElementTile(12, 3, "SV-001", ElementType.SIGNAL_V,
            List.of("/icons/signals/sbb_l/signal_v_orange_left.svg", "/icons/signals/sbb_l/signal_v_yellow_left.svg", "/icons/signals/sbb_l/signal_v_green_left.svg", "/icons/signals/sbb_l/signal_v_aspect3_left.svg")));

        for (int col = 0; col < 5; col++) {
            String id = "P-" + String.format("%03d", col + 1);
            model.addElement(new Element(id, 0, 0));
            tileGrid.setTile(new ElementTile(col, 0, id, ElementType.STRAIGHT, List.of("/icons/tracks/straight.svg")));
        }

        for (int col = 5; col < tileGrid.getCols(); col++) {
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
                layoutPersistence.load(tileGrid, path);
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
        enterLastLayoutDirectory(chooser);
        if (chooser.showOpenDialog(parentComponent) == JFileChooser.APPROVE_OPTION) {
            Path path = chooser.getSelectedFile().toPath();
            try {
                layoutPersistence.load(tileGrid, path);
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
            layoutPersistence.save(tileGrid, currentFilePath);
            settings.setLastLayoutFile(currentFilePath);
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
        } else {
            enterLastLayoutDirectory(chooser);
        }
        if (chooser.showSaveDialog(parentComponent) == JFileChooser.APPROVE_OPTION) {
            Path path = chooser.getSelectedFile().toPath();
            if (!path.toString().endsWith(".json")) {
                path = Paths.get(path + ".json");
            }
            try {
                layoutPersistence.save(tileGrid, path);
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

    /**
     * Points the chooser at the directory of the active layout, falling back to
     * the directory of the last layout stored in the settings.
     */
    private void enterLastLayoutDirectory(JFileChooser chooser) {
        if (currentFilePath != null) {
            File parent = currentFilePath.toFile().getParentFile();
            if (parent != null) {
                chooser.setCurrentDirectory(parent);
                return;
            }
        }
        Path dir = settings.getLastLayoutDirectory();
        if (dir != null) {
            chooser.setCurrentDirectory(dir.toFile());
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
