package com.bidib.switchboard.persistence;

/**
 * DTO for application settings serialized to settings.json.
 */
public class SettingsData {

    private String lastLayoutFile;

    public String getLastLayoutFile() {
        return lastLayoutFile;
    }

    public void setLastLayoutFile(String lastLayoutFile) {
        this.lastLayoutFile = lastLayoutFile;
    }
}
