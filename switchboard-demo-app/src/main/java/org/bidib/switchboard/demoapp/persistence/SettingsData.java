package org.bidib.switchboard.demoapp.persistence;

/**
 * DTO for application settings serialized to settings.json.
 */
public class SettingsData {

    public enum LookAndFeel {
        LIGHT, DARK
    };

    private String lastLayoutFile;

    private String lastLayoutDirectory;

    private LookAndFeel lookAndFeel = LookAndFeel.LIGHT;

    private boolean exhaustiveRouting;

    private String signalSide = "LEFT"; // Swiss default

    public String getSignalSide() { return signalSide; }
    public void setSignalSide(String signalSide) { this.signalSide = signalSide; }

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

    public String getLastLayoutDirectory() {
        return lastLayoutDirectory;
    }

    public void setLastLayoutDirectory(String lastLayoutDirectory) {
        this.lastLayoutDirectory = lastLayoutDirectory;
    }

    public LookAndFeel getLookAndFeel() {
        return lookAndFeel;
    }

    public void setLookAndFeel(LookAndFeel lookAndFeel) {
        this.lookAndFeel = lookAndFeel;
    }
}
