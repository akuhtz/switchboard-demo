package org.bidib.switchboard.demoapp;

import java.awt.Dimension;
import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

import org.assertj.core.api.Assertions;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JPopupMenuFixture;
import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.SignalSide;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.bidib.switchboard.component.view.AssignOccupancyDialog;
import org.bidib.switchboard.component.view.SwitchboardPanel;
import org.bidib.switchboard.demoapp.config.DemoOccupancyFactory;
import org.bidib.switchboard.demoapp.persistence.SettingsData.LookAndFeel;
import org.bidib.switchboard.demoapp.persistence.SettingsManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SwitchboardAppTest {

	private FrameFixture window;

	private SwitchboardPanel panel;

	private final OccupancyFactory occupancyFactory = new DemoOccupancyFactory(); 

	@BeforeAll
	static void forceEnglishLocale() {
		Locale.setDefault(Locale.ENGLISH);
	}

	@BeforeEach
	void setUp() throws Exception {
		SettingsManager settings = Mockito.mock(SettingsManager.class);
		Mockito.when(settings.getLanguage()).thenReturn("en");
		Mockito.when(settings.getLookAndFeel()).thenReturn(LookAndFeel.DARK);
		Mockito.when(settings.isExhaustiveRouting()).thenReturn(false);
		Mockito.when(settings.getSignalSide()).thenReturn(SignalSide.LEFT);

		SwitchboardApp app = GuiActionRunner.execute(() -> new SwitchboardApp(settings, false));
		window = new FrameFixture(app.getFrame());
		panel = app.getPanel();
		window.robot().showWindow(window.target(), new Dimension(1024, 768));

		var url = SwitchboardAppTest.class.getResource("/test-data/switchboard3.json");
		Path path = Paths.get(url.toURI());
		GuiActionRunner.execute(() -> {
			var layoutPersistence = new LayoutPersistence();
			layoutPersistence.load(panel, path);
		});
	}

	@AfterEach
	void tearDown() {
		window.cleanUp();
	}

	@Test
	void frameTitleContainsSwitchboard() {
		Assertions.assertThat(window.target().getTitle()).contains("Model Railway Switchboard");
	}

	@Test
	void fileMenuContainsLoadSaveSaveAsSettingsAndExit() {
		window.menuItemWithPath("File", "Load...").requireVisible();
		window.menuItemWithPath("File", "Save").requireVisible();
		window.menuItemWithPath("File", "Save As...").requireVisible();
		window.menuItemWithPath("File", "Settings", "Light Look and Feel").requireVisible();
		window.menuItemWithPath("File", "Settings", "Dark Look and Feel").requireVisible();
		window.menuItemWithPath("File", "Settings", "Exhaustive Route Search").requireVisible();
		window.menuItemWithPath("File", "Exit").requireVisible();
	}

	@Test
	void editMenuContainsEditModeLoadDefaultAndOccupancies() {
		window.menuItemWithPath("Edit", "Edit Mode").requireVisible();
		window.menuItemWithPath("Edit", "Load Default Layout").requireVisible();
		window.menuItemWithPath("Edit", "Occupancies...").requireVisible();
	}

	@Test
	void toolbarContainsEditModeToggle() {
		window.toggleButton(new GenericTypeMatcher<>(JToggleButton.class) {
			@Override
			protected boolean isMatching(JToggleButton button) {
				return "Toggle Edit Mode".equals(button.getToolTipText());
			}
		}).requireVisible();
	}

	@Test
	void settingsMenuHasLightAndDarkAndExhaustiveItems() {
		window.menuItemWithPath("File", "Settings", "Light Look and Feel").requireVisible();
		window.menuItemWithPath("File", "Settings", "Dark Look and Feel").requireVisible();
		window.menuItemWithPath("File", "Settings", "Exhaustive Route Search").requireVisible();
	}

	@Test
	void clearSelectionItemVisibleOnlyInEditMode() {
		window.menuItemWithPath("Edit", "Edit Mode").click();
		window.robot().waitForIdle();

		JPopupMenu popup1 = window.robot().showPopupMenu(panel, new Point(16, 16));
		JPopupMenuFixture popupFixture1 = new JPopupMenuFixture(window.robot(), popup1);
		popupFixture1.menuItemWithPath("Clear selection").requireVisible();

		window.robot().pressAndReleaseKey(java.awt.event.KeyEvent.VK_ESCAPE);
		window.robot().waitForIdle();

		window.menuItemWithPath("Edit", "Edit Mode").click();
		window.robot().waitForIdle();

		JPopupMenu popup2 = window.robot().showPopupMenu(panel, new Point(16, 16));
		JPopupMenuFixture popupFixture2 = new JPopupMenuFixture(window.robot(), popup2);

		Assertions.assertThatExceptionOfType(org.assertj.swing.exception.ComponentLookupException.class)
			.isThrownBy(() -> popupFixture2.menuItemWithPath("Clear selection").requireNotVisible());
	}

	@Test
	void occupancyPersistenceRoundtrip() {
		var model = panel.getModel();
		var element = model.getElement("P-001");

		Occupancy occ = occupancyFactory.create(Occupancy.OccupancyState.FREE);
		model.addOccupancy(occ);
		element.setOccupancy(occ);

		var layoutPersistence = new LayoutPersistence();
		var data = layoutPersistence.capture(panel);

		var freshModel = new RailwayModel();
		var freshPanel = new SwitchboardPanel(occupancyFactory, (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), freshModel);
		layoutPersistence.apply(freshPanel, data);

		Assertions.assertThat(freshModel.getOccupancies()).hasSize(1);
		var restored = freshModel.getOccupancies().values().iterator().next();
		Assertions.assertThat(restored).isNotNull();
		Assertions.assertThat(restored.getId()).isEqualTo(occ.getId());
		Assertions.assertThat(restored.getState()).isEqualTo(Occupancy.OccupancyState.FREE);

		var restoredEl = freshModel.getElement("P-001");
		Assertions.assertThat(restoredEl).isNotNull();
		Assertions.assertThat(restoredEl.getOccupancy()).isSameAs(restored);
	}
}
