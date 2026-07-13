package com.bidib.switchboard.persistence;

/**
 * DTO for application settings serialized to settings.json.
 */
public class SettingsData {

    public enum LookAndFeel {
        LIGHT, DARK
    };

    private String lastLayoutFile;

    private LookAndFeel lookAndFeel = LookAndFeel.LIGHT;

    public String getLastLayoutFile() {
        return lastLayoutFile;
    }

    public void setLastLayoutFile(String lastLayoutFile) {
        this.lastLayoutFile = lastLayoutFile;
    }

    public LookAndFeel getLookAndFeel() {
        return lookAndFeel;
    }

    public void setLookAndFeel(LookAndFeel lookAndFeel) {
        this.lookAndFeel = lookAndFeel;
    }
}
