package org.bidib.switchboard.demoapp.persistence;

import java.nio.file.Path;

import org.bidib.switchboard.component.model.SignalSide;
import org.bidib.switchboard.demoapp.persistence.SettingsData.LookAndFeel;

/**
 * Application settings abstraction. Implementations provide the stored values,
 * typically backed by a settings file or by fixed values for tests.
 */
public interface SettingsManager {

    /**
     * Returns the stored UI language code ("en" or "de"), or null when the system default is used.
     */
    String getLanguage();

    /**
     * Sets and persists the UI language code ("en" or "de"), or null to use the system default.
     */
    void setLanguage(String language);

    /**
     * Returns the path of the last opened layout file, or null.
     */
    Path getLastLayoutFile();

    /**
     * Sets and persists the last opened layout file path, together with the
     * directory it lives in.
     */
    void setLastLayoutFile(Path path);

    /**
     * Returns the directory of the last opened layout file, or null.
     */
    Path getLastLayoutDirectory();

    /**
     * Returns the current look and feel setting (LIGHT or DARK).
     */
    LookAndFeel getLookAndFeel();

    /**
     * Sets and persists the look and feel setting.
     */
    void setLookAndFeel(LookAndFeel lookAndFeel);

    boolean isExhaustiveRouting();

    void setExhaustiveRouting(boolean exhaustive);

    SignalSide getSignalSide();

    void setSignalSide(SignalSide side);
}
