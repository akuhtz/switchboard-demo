package org.bidib.switchboard.demoapp.persistence;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.bidib.switchboard.component.model.SignalSide;
import org.bidib.switchboard.demoapp.persistence.SettingsData.LookAndFeel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Manages application settings stored in {@code ~/switchboard-demo-1/settings.json}.
 * Settings are separate from the layout file and reference it by path.
 */
public class SettingsManager {

    private static final Logger LOG = LoggerFactory.getLogger(SettingsManager.class);

    private static final Path SETTINGS_DIR = Paths.get(System.getProperty("user.home"), "switchboard-demo-1");
    private static final Path SETTINGS_PATH = SETTINGS_DIR.resolve("settings.json");

    private static final ObjectMapper MAPPER = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private final SettingsData data;

    public SettingsManager() {
        this.data = load();
    }

    /**
     * Returns the path of the last opened layout file, or null.
     */
    public Path getLastLayoutFile() {
        String path = data.getLastLayoutFile();
        return (path != null && !path.isBlank()) ? Paths.get(path) : null;
    }

    /**
     * Sets and persists the last opened layout file path, together with the
     * directory it lives in.
     */
    public void setLastLayoutFile(Path path) {
        data.setLastLayoutFile(path.toString());
        File parent = path.toFile().getParentFile();
        if (parent != null) {
            data.setLastLayoutDirectory(parent.toString());
            LOG.info("Persisting last layout directory {}", parent);
        }
        save();
    }

    /**
     * Returns the directory of the last opened layout file, or null.
     */
    public Path getLastLayoutDirectory() {
        String dir = data.getLastLayoutDirectory();
        if (dir != null && !dir.isBlank()) {
            LOG.info("Reading last layout directory {}", dir);
            return Paths.get(dir);
        }
        LOG.info("No last layout directory available.");
        return null;
    }

    /**
     * Returns the current look and feel setting (LIGHT or DARK).
     */
    public LookAndFeel getLookAndFeel() {
        return data.getLookAndFeel();
    }

    /**
     * Sets and persists the look and feel setting.
     */
    public void setLookAndFeel(LookAndFeel lookAndFeel) {
        this.data.setLookAndFeel(lookAndFeel);
        save();
    }

    public boolean isExhaustiveRouting() {
        return data.isExhaustiveRouting();
    }

    public void setExhaustiveRouting(boolean exhaustive) {
        data.setExhaustiveRouting(exhaustive);
        save();
    }

    public SignalSide getSignalSide() {
        String val = data.getSignalSide();
        if (val == null) return SignalSide.LEFT;
        try { return SignalSide.valueOf(val); }
        catch (IllegalArgumentException e) { return SignalSide.LEFT; }
    }

    public void setSignalSide(SignalSide side) {
        data.setSignalSide(side.name());
        save();
    }

    // --- Internal ---

    private static SettingsData load() {
        if (SETTINGS_PATH.toFile().exists()) {
            try {
                LOG.info("Loading settings from {}", SETTINGS_PATH);
                return MAPPER.readValue(SETTINGS_PATH.toFile(), SettingsData.class);
            }
            catch (Exception e) {
                LOG.warn("Failed to load settings from {}", SETTINGS_PATH, e);
            }
        }
        return new SettingsData();
    }

    private void save() {
        try {
            Files.createDirectories(SETTINGS_DIR);
            MAPPER.writeValue(SETTINGS_PATH.toFile(), data);
        }
        catch (IOException e) {
            LOG.warn("Failed to save settings to {}", SETTINGS_PATH, e);
        }
    }
}
