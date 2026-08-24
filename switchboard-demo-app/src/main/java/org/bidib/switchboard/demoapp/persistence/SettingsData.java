package org.bidib.switchboard.demoapp.persistence;

import java.util.ArrayList;
import java.util.List;

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

    private boolean autoChangeSignal;

    private String signalSide = "LEFT"; // Swiss default

    private String language; // "en" or "de"; null = system default

    private List<String> recentFiles = new ArrayList<>();

    public String getSignalSide() { return signalSide; }
    public void setSignalSide(String signalSide) { this.signalSide = signalSide; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public boolean isExhaustiveRouting() {
        return exhaustiveRouting;
    }

    public void setExhaustiveRouting(boolean exhaustiveRouting) {
        this.exhaustiveRouting = exhaustiveRouting;
    }

    public boolean isAutoChangeSignal() {
        return autoChangeSignal;
    }

    public void setAutoChangeSignal(boolean autoChangeSignal) {
        this.autoChangeSignal = autoChangeSignal;
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

    public List<String> getRecentFiles() {
        return recentFiles;
    }

    public void setRecentFiles(List<String> recentFiles) {
        this.recentFiles = recentFiles;
    }
}
