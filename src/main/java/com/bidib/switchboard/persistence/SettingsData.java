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

    private boolean exhaustiveRouting;

    public boolean isExhaustiveRouting() {
        return exhaustiveRouting;
    }

    public void setExhaustiveRouting(boolean exhaustiveRouting) {
        this.exhaustiveRouting = exhaustiveRouting;
    }

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
