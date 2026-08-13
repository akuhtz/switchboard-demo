package org.bidib.switchboard.demoapp;

import java.io.IOException;

import java.awt.BorderLayout;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.SignalSide;
import org.bidib.switchboard.component.view.SwitchboardPanel;
import org.bidib.switchboard.demoapp.config.DemoOccupancy;
import org.bidib.switchboard.demoapp.config.DemoOccupancyFactory;
import org.bidib.switchboard.demoapp.persistence.DefaultSettingsManager;
import org.bidib.switchboard.demoapp.persistence.DemoOccupancySerializer;
import org.bidib.switchboard.demoapp.persistence.SettingsData.LookAndFeel;
import org.bidib.switchboard.demoapp.persistence.SettingsManager;
import org.bidib.switchboard.demoapp.service.LayoutService;
import org.bidib.switchboard.demoapp.view.DemoAssignOccupancyDialogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

public class SwitchboardApp {

    static {
        Path logPath = isWindows()
            ? Paths.get(System.getProperty("user.home"), "Documents", "switchboard-demo", "switchboard-demo-app.log")
            : Paths.get(System.getProperty("java.io.tmpdir"), "switchboard-demo-app.log");
        try {
            Files.createDirectories(logPath.getParent());
        }
        catch (IOException e) {
            System.err.println("Could not create log directory " + logPath.getParent() + ": " + e.getMessage());
        }
        System.setProperty("switchboard.logfile", logPath.toString());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
    }

    private static final Logger log = LoggerFactory.getLogger(SwitchboardApp.class);

    private ResourceBundle messages;

    private final RailwayModel model;

    private final SwitchboardPanel panel;

    private final JFrame frame;

    private final SettingsManager settings;

    private final LayoutService layoutService;

	private final DemoOccupancyFactory occupancyFactory = new DemoOccupancyFactory();

    SwitchboardApp() {
        this(new DefaultSettingsManager(), true);
    }

    SwitchboardApp(boolean autoLoad) {
        this(new DefaultSettingsManager(), autoLoad);
    }

    SwitchboardApp(SettingsManager settings, boolean autoLoad) {
        log.info("Launch the SwitchboardApp.");

        // Load settings first: the stored language determines the locale used for every
        // resource bundle the UI reads afterwards.
        this.settings = settings;
        Locale locale = resolveLocale(settings.getLanguage());
        Locale.setDefault(locale);
        ResourceBundle.clearCache();
        messages = ResourceBundle.getBundle("i18n.app-messages");

        model = new RailwayModel();

        if (LookAndFeel.DARK == settings.getLookAndFeel()) {
            FlatDarkLaf.setup();
        }
        else {
            FlatLightLaf.setup();
        }

        panel = new SwitchboardPanel(occupancyFactory, new DemoAssignOccupancyDialogFactory(), model);
        panel.setLocale(locale);

        frame = new JFrame(messages.getString("frame.title"));
        layoutService = new LayoutService(new DemoOccupancySerializer(), panel, settings, frame);
        if (autoLoad) {
            layoutService.tryAutoLoad();
            updateTitle();
        }
        buildMenu();
        buildFrame();
    }

    /** Maps the stored language code ("en", "de", or null) to a Ui locale, defaulting to the system locale. */
    private static Locale resolveLocale(String language) {
        if (language == null) {
            return Locale.getDefault();
        }
        return switch (language.toLowerCase()) {
            case "en" -> Locale.ENGLISH;
            case "de" -> Locale.GERMAN;
            default -> Locale.getDefault();
        };
    }

    /** Applies a new UI language: persists it, reloads bundles, and rebuilds the menus and frame title. */
    private void applyLanguage(String language) {
        settings.setLanguage(language);
        Locale locale = resolveLocale(language);
        Locale.setDefault(locale);
        ResourceBundle.clearCache();
        messages = ResourceBundle.getBundle("i18n.app-messages");
        panel.setLocale(locale);
        buildMenu();
        if (editToggle != null) {
            editToggle.setToolTipText(messages.getString("toolbar.editMode.tooltip"));
        }
        updateTitle();
        SwingUtilities.updateComponentTreeUI(frame);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SwitchboardApp::new);
    }

    // --- Menu ---

    private JCheckBoxMenuItem editModeItem;

    private JToggleButton editToggle;

    private void buildMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu(messages.getString("menu.file"));
        fileMenu.setMnemonic('F');

        JMenuItem loadItem = new JMenuItem(messages.getString("menu.file.load"));
        loadItem.setMnemonic('L');
        loadItem.setAccelerator(KeyStroke.getKeyStroke("control L"));
        loadItem.addActionListener(e -> {
            layoutService.onLoad();
            updateTitle();
        });
        fileMenu.add(loadItem);

        JMenuItem saveItem = new JMenuItem(messages.getString("menu.file.save"));
        saveItem.setMnemonic('S');
        saveItem.setAccelerator(KeyStroke.getKeyStroke("control S"));
        saveItem.addActionListener(e -> layoutService.onSave());
        fileMenu.add(saveItem);

        JMenuItem saveAsItem = new JMenuItem(messages.getString("menu.file.saveAs"));
        saveAsItem.setMnemonic('A');
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke("control shift S"));
        saveAsItem.addActionListener(e -> {
            layoutService.onSaveAs();
            updateTitle();
        });
        fileMenu.add(saveAsItem);

        fileMenu.addSeparator();

        JMenu settingsMenu = new JMenu(messages.getString("menu.file.settings"));
        settingsMenu.setMnemonic('S');
        ButtonGroup lafGroup = new ButtonGroup();
        JRadioButtonMenuItem lightItem = new JRadioButtonMenuItem(messages.getString("menu.file.settings.lightLookAndFeel"));
        lightItem.setSelected(settings.getLookAndFeel() == LookAndFeel.LIGHT);
        lightItem.addActionListener(e -> applyLookAndFeel(LookAndFeel.LIGHT));
        lafGroup.add(lightItem);
        settingsMenu.add(lightItem);
        JRadioButtonMenuItem darkItem = new JRadioButtonMenuItem(messages.getString("menu.file.settings.darkLookAndFeel"));
        darkItem.setSelected(settings.getLookAndFeel() == LookAndFeel.DARK);
        darkItem.addActionListener(e -> applyLookAndFeel(LookAndFeel.DARK));
        lafGroup.add(darkItem);
        settingsMenu.add(darkItem);

        settingsMenu.addSeparator();
        JCheckBoxMenuItem exhaustiveItem = new JCheckBoxMenuItem(messages.getString("menu.file.settings.exhaustiveRouteSearch"));
        exhaustiveItem.setSelected(settings.isExhaustiveRouting());
        exhaustiveItem.addActionListener(e -> {
            boolean selected = exhaustiveItem.isSelected();
            panel.setExhaustiveRouting(selected);
            settings.setExhaustiveRouting(selected);
        });
        settingsMenu.add(exhaustiveItem);
        panel.setExhaustiveRouting(settings.isExhaustiveRouting());

        settingsMenu.addSeparator();
        JCheckBoxMenuItem signalLeftItem = new JCheckBoxMenuItem(messages.getString("menu.file.settings.signalSideLeft"), settings.getSignalSide() == SignalSide.LEFT);
        JCheckBoxMenuItem signalRightItem = new JCheckBoxMenuItem(messages.getString("menu.file.settings.signalSideRight"), settings.getSignalSide() == SignalSide.RIGHT);
        signalLeftItem.addActionListener(e -> {
            settings.setSignalSide(SignalSide.LEFT);
            panel.setGlobalSignalSide(SignalSide.LEFT);
            signalLeftItem.setSelected(true);
            signalRightItem.setSelected(false);
        });
        signalRightItem.addActionListener(e -> {
            settings.setSignalSide(SignalSide.RIGHT);
            panel.setGlobalSignalSide(SignalSide.RIGHT);
            signalRightItem.setSelected(true);
            signalLeftItem.setSelected(false);
        });
        settingsMenu.add(signalLeftItem);
        settingsMenu.add(signalRightItem);

        settingsMenu.addSeparator();
        JMenu languageMenu = new JMenu(messages.getString("menu.file.settings.language"));
        ButtonGroup languageGroup = new ButtonGroup();
        Locale currentLocale = resolveLocale(settings.getLanguage());
        JRadioButtonMenuItem enItem = new JRadioButtonMenuItem(messages.getString("menu.file.settings.language.en"));
        enItem.setSelected(currentLocale.getLanguage().equals("en"));
        enItem.addActionListener(e -> applyLanguage("en"));
        languageGroup.add(enItem);
        languageMenu.add(enItem);
        JRadioButtonMenuItem deItem = new JRadioButtonMenuItem(messages.getString("menu.file.settings.language.de"));
        deItem.setSelected(currentLocale.getLanguage().equals("de"));
        deItem.addActionListener(e -> applyLanguage("de"));
        languageGroup.add(deItem);
        languageMenu.add(deItem);
        settingsMenu.add(languageMenu);

        panel.setGlobalSignalSide(settings.getSignalSide());

        fileMenu.add(settingsMenu);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem(messages.getString("menu.file.exit"));
        exitItem.setMnemonic('X');
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu(messages.getString("menu.edit"));
        editMenu.setMnemonic('E');
        JMenuItem undoItem = new JMenuItem(messages.getString("menu.edit.undo"));
        undoItem.setAccelerator(KeyStroke.getKeyStroke("control Z"));
        undoItem.addActionListener(e -> panel.undoLast());
        editMenu.add(undoItem);
        editMenu.addSeparator();
        editModeItem = new JCheckBoxMenuItem(messages.getString("menu.edit.editMode"));
        editModeItem.setMnemonic('M');
        editModeItem.setAccelerator(KeyStroke.getKeyStroke("control E"));
        editModeItem.setSelected(panel.isEditMode());
        editModeItem.addActionListener(e -> setEditMode(editModeItem.isSelected()));
        editMenu.add(editModeItem);

        editMenu.addSeparator();
        JMenuItem loadDefaultItem = new JMenuItem(messages.getString("menu.edit.loadDefaultLayout"));
        loadDefaultItem.addActionListener(e -> {
            layoutService.loadDefaultLayout();
            updateTitle();
        });
        editMenu.add(loadDefaultItem);

        editMenu.addSeparator();
        JMenuItem occupanciesItem = new JMenuItem(messages.getString("menu.edit.occupancies"));
        occupanciesItem.addActionListener(e -> showOccupanciesDialog());
        editMenu.add(occupanciesItem);

        JCheckBoxMenuItem autoChangeSignalItem = new JCheckBoxMenuItem(messages.getString("menu.edit.autoChangeSignal"));
        autoChangeSignalItem.addActionListener(e -> panel.setAutoChangeSignal(autoChangeSignalItem.isSelected()));
        editMenu.add(autoChangeSignalItem);

        menuBar.add(editMenu);

        frame.setJMenuBar(menuBar);
    }

    private void setEditMode(boolean enabled) {
        panel.setEditMode(enabled);
        editModeItem.setSelected(enabled);
        editToggle.setSelected(enabled);
    }

    private void applyLookAndFeel(LookAndFeel laf) {
        settings.setLookAndFeel(laf);
        if (laf == LookAndFeel.DARK) {
            FlatDarkLaf.setup();
        }
        else {
            FlatLightLaf.setup();
        }
        SwingUtilities.updateComponentTreeUI(frame);
    }

    // --- Frame ---

    private void buildFrame() {
        var wrenchIcon = new javax.swing.ImageIcon(getClass().getResource("/toolbar/wrench.png"));
        var wrenchSelectedIcon = new javax.swing.ImageIcon(getClass().getResource("/toolbar/wrench_selected.png"));
        editToggle = new JToggleButton(wrenchIcon);
        editToggle.setSelectedIcon(wrenchSelectedIcon);
        editToggle.setToolTipText(messages.getString("toolbar.editMode.tooltip"));
        editToggle.addActionListener(e -> setEditMode(editToggle.isSelected()));

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(editToggle);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(32);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(32);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(toolbar, BorderLayout.PAGE_START);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(null);
        updateTitle();

        frame.setVisible(true);
    }

    JFrame getFrame() {
        return frame;
    }

    SwitchboardPanel getPanel() {
        return panel;
    }

    private void updateTitle() {
        String name = layoutService.getCurrentFilePath() != null
            ? layoutService.getCurrentFilePath().getFileName().toString()
            : messages.getString("frame.title.untitled");
        frame.setTitle(messages.getString("frame.title") + " - " + name);
    }

    // --- Helpers ---

    private void showOccupanciesDialog() {
        Map<String, Occupancy> occs = model.getOccupancies();
        List<Occupancy> sorted = occs.values().stream()
                .sorted(Comparator.<Occupancy, Long>comparing(
                        o -> o instanceof DemoOccupancy d ? d.getNodeId() : 0L)
                        .thenComparingInt(o -> o instanceof DemoOccupancy d ? d.getPortId() : 0))
                .toList();

        JTable table = new JTable(new AbstractTableModel() {
            private final String[] columns = { "Node ID", "Port ID", "State" };

            @Override
            public int getRowCount() {
                return sorted.size();
            }

            @Override
            public int getColumnCount() {
                return columns.length;
            }

            @Override
            public Object getValueAt(int row, int col) {
                Occupancy o = sorted.get(row);
                return switch (col) {
                    case 0 -> o instanceof DemoOccupancy d ? d.getNodeId() : "-";
                    case 1 -> o instanceof DemoOccupancy d ? d.getPortId() : "-";
                    case 2 -> o.getState();
                    default -> null;
                };
            }

            @Override
            public String getColumnName(int col) {
                return columns[col];
            }
        });

        JDialog dialog = new JDialog(frame, messages.getString("menu.edit.occupancies"), false);
        dialog.add(new JScrollPane(table));
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }
}
