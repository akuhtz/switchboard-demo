package com.bidib.switchboard.persistence;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.bidib.switchboard.persistence.SettingsData.LookAndFeel;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Manages application settings stored in settings.json at the project root. Settings are separate from the layout file
 * and reference it by path.
 */
public class SettingsManager {

    private static final Path SETTINGS_PATH = Paths.get("settings.json").toAbsolutePath().normalize();

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
     * Sets and persists the last opened layout file path.
     */
    public void setLastLayoutFile(Path path) {
        data.setLastLayoutFile(path.toString());
        save();
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

    // --- Internal ---

    private static SettingsData load() {
        if (SETTINGS_PATH.toFile().exists()) {
            try {
                return MAPPER.readValue(SETTINGS_PATH.toFile(), SettingsData.class);
            }
            catch (Exception e) {
                // Fall through to default
            }
        }
        return new SettingsData();
    }

    private void save() {
        try {
            MAPPER.writeValue(SETTINGS_PATH.toFile(), data);
        }
        catch (Exception e) {
            // Ignore — settings save is best-effort
        }
    }
}
