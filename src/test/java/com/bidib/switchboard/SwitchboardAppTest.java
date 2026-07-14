package com.bidib.switchboard;

import javax.swing.JToggleButton;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SwitchboardAppTest {

    private FrameFixture window;

    @BeforeEach
    void setUp() {
        SwitchboardApp app = GuiActionRunner.execute(() -> new SwitchboardApp());
        window = new FrameFixture(app.getFrame());
        window.show();
    }

    @AfterEach
    void tearDown() {
        window.cleanUp();
    }

    @Test
    void frameTitleContainsSwitchboard() {
        org.assertj.core.api.Assertions.assertThat(window.target().getTitle())
            .contains("Model Railway Switchboard");
    }

    @Test
    void fileMenuContainsLoadSaveSaveAsSettingsAndExit() {
        window.menuItemWithPath("File", "Load...").requireVisible();
        window.menuItemWithPath("File", "Save").requireVisible();
        window.menuItemWithPath("File", "Save As...").requireVisible();
        window.menuItemWithPath("File", "Settings", "Light Look and Feel").requireVisible();
        window.menuItemWithPath("File", "Settings", "Dark Look and Feel").requireVisible();
        window.menuItemWithPath("File", "Exit").requireVisible();
    }

    @Test
    void editMenuContainsEditMode() {
        window.menuItemWithPath("Edit", "Edit Mode").requireVisible();
    }

    @Test
    void toolbarContainsEditModeToggle() {
        window.toggleButton(new GenericTypeMatcher<>(JToggleButton.class) {
            @Override
            protected boolean isMatching(JToggleButton button) {
                return "Edit Mode".equals(button.getText());
            }
        }).requireVisible();
    }

    @Test
    void settingsMenuHasLightAndDarkItems() {
        window.menuItemWithPath("File", "Settings", "Light Look and Feel").requireVisible();
        window.menuItemWithPath("File", "Settings", "Dark Look and Feel").requireVisible();
    }
}
