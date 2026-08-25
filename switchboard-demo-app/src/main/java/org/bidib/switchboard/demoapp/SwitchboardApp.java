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
import javax.swing.JOptionPane;
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
import org.bidib.switchboard.component.service.RouterService;
import org.bidib.switchboard.component.model.Train;
import org.bidib.switchboard.component.view.SwitchboardPanel;
import org.bidib.switchboard.component.view.TrainListPanel;
import org.bidib.switchboard.component.view.RouteListPanel;
import org.bidib.switchboard.component.view.RouteDetailsPanel;
import org.bidib.switchboard.demoapp.config.DemoOccupancy;
import org.bidib.switchboard.demoapp.config.DemoOccupancyFactory;
import org.bidib.switchboard.demoapp.persistence.DefaultSettingsManager;
import org.bidib.switchboard.demoapp.persistence.DemoOccupancySerializer;
import org.bidib.switchboard.demoapp.persistence.SettingsData.LookAndFeel;
import org.bidib.switchboard.demoapp.persistence.SettingsManager;
import org.bidib.switchboard.demoapp.persistence.TrainSerializer;
import org.bidib.switchboard.demoapp.service.LayoutService;
import org.bidib.switchboard.demoapp.view.DemoAssignOccupancyDialogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.vlsolutions.swing.docking.DockingDesktop;

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

    private final SwitchboardPanel switchboardPanel;

    private final JFrame frame;

    private final SettingsManager settings;

    private final LayoutService layoutService;

	private final DemoOccupancyFactory occupancyFactory = new DemoOccupancyFactory();

    private final DockingDesktop desktop;

    private TrainListPanel trainListPanel;

    private RouteListPanel routeListPanel;

    private RouteDetailsPanel routeDetailsPanel;

    private JMenu recentMenu;

    private final TrainSerializer trainSerializer = new TrainSerializer();

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
        
        desktop = new DockingDesktop();

        switchboardPanel = new SwitchboardPanel(occupancyFactory, new DemoAssignOccupancyDialogFactory(), model, RouterService.createDefault());
        switchboardPanel.setLocale(locale);

        frame = new JFrame(messages.getString("frame.title"));
        layoutService = new LayoutService(new DemoOccupancySerializer(), switchboardPanel, settings, frame);
        if (autoLoad) {
            layoutService.tryAutoLoad();
            loadTrainsForCurrentLayout();
            updateTitle();
        }
        trainListPanel = new TrainListPanel(model.getTrainListModel(), messages);
        routeListPanel = new RouteListPanel(switchboardPanel.getRouteModel(), messages);
        routeDetailsPanel = new RouteDetailsPanel(switchboardPanel, model, switchboardPanel.getRouteModel(), messages);
        routeListPanel.setSelectionListener(route -> {
            switchboardPanel.setSelectedRoute(route);
            routeDetailsPanel.setRoute(route);
        });

        buildMenu();
        buildFrame();
    }

    /**
     * Loads the trains file referenced by the current layout, or creates demo trains
     * if no layout is loaded or the referenced file doesn't exist.
     */
    private void loadTrainsForCurrentLayout() {
        Path layoutPath = layoutService.getCurrentFilePath();
        String trainsFile = layoutService.getTrainsFile();
        if (layoutPath != null) {
            Path trainsPath = TrainSerializer.resolveTrainsPath(layoutPath, trainsFile);
            trainSerializer.loadInto(model.getTrainListModel(), trainsPath);
        }
        if (model.getTrainListModel().getTrains().isEmpty()) {
            addDemoTrains();
            if (layoutPath != null) {
                Path trainsPath = TrainSerializer.resolveTrainsPath(layoutPath, "trains.json");
                trainSerializer.saveFrom(model.getTrainListModel(), trainsPath);
                layoutService.setTrainsFile("trains.json");
            }
        }
    }

    private void addDemoTrains() {
        model.getTrainListModel().addTrain(new Train("T001", "Re 460 023", null));
        model.getTrainListModel().addTrain(new Train("T002", "IC 2000", null));
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
        switchboardPanel.setLocale(locale);
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

    private JCheckBoxMenuItem routeMenuItem;

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
            rebuildRecentMenu();
        });
        fileMenu.add(loadItem);

        recentMenu = new JMenu(messages.getString("menu.file.recent"));
        fileMenu.add(recentMenu);
        rebuildRecentMenu();

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
            switchboardPanel.setExhaustiveRouting(selected);
            settings.setExhaustiveRouting(selected);
        });
        settingsMenu.add(exhaustiveItem);
        switchboardPanel.setExhaustiveRouting(settings.isExhaustiveRouting());

        settingsMenu.addSeparator();
        JCheckBoxMenuItem signalLeftItem = new JCheckBoxMenuItem(messages.getString("menu.file.settings.signalSideLeft"), settings.getSignalSide() == SignalSide.LEFT);
        JCheckBoxMenuItem signalRightItem = new JCheckBoxMenuItem(messages.getString("menu.file.settings.signalSideRight"), settings.getSignalSide() == SignalSide.RIGHT);
        signalLeftItem.addActionListener(e -> {
            settings.setSignalSide(SignalSide.LEFT);
            switchboardPanel.setGlobalSignalSide(SignalSide.LEFT);
            signalLeftItem.setSelected(true);
            signalRightItem.setSelected(false);
        });
        signalRightItem.addActionListener(e -> {
            settings.setSignalSide(SignalSide.RIGHT);
            switchboardPanel.setGlobalSignalSide(SignalSide.RIGHT);
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

        switchboardPanel.setGlobalSignalSide(settings.getSignalSide());

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
        undoItem.addActionListener(e -> switchboardPanel.undoLast());
        editMenu.add(undoItem);
        editMenu.addSeparator();
        editModeItem = new JCheckBoxMenuItem(messages.getString("menu.edit.editMode"));
        editModeItem.setMnemonic('M');
        editModeItem.setAccelerator(KeyStroke.getKeyStroke("control E"));
        editModeItem.setSelected(switchboardPanel.isEditMode());
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
        autoChangeSignalItem.setSelected(settings.isAutoChangeSignal());
        autoChangeSignalItem.addActionListener(e -> {
            boolean selected = autoChangeSignalItem.isSelected();
            switchboardPanel.setAutoChangeSignal(selected);
            settings.setAutoChangeSignal(selected);
        });
        editMenu.add(autoChangeSignalItem);
        switchboardPanel.setAutoChangeSignal(settings.isAutoChangeSignal());

        editMenu.addSeparator();
        JCheckBoxMenuItem routeItem = new JCheckBoxMenuItem(messages.getString("menu.edit.defineTrainRoute"));
        routeItem.setMnemonic('T');
        routeItem.setAccelerator(KeyStroke.getKeyStroke("control T"));
        routeItem.addActionListener(e -> {
            if (routeItem.isSelected()) {
                switchboardPanel.enterRouteCreationMode();
            } else {
                saveRouteFromPanel();
            }
        });
        editMenu.add(routeItem);
        this.routeMenuItem = routeItem;

        menuBar.add(editMenu);

        frame.setJMenuBar(menuBar);
    }

    private void setEditMode(boolean enabled) {
        switchboardPanel.setEditMode(enabled);
        editModeItem.setSelected(enabled);
        editToggle.setSelected(enabled);
        routeMenuItem.setEnabled(enabled);
        if (!enabled && routeMenuItem.isSelected()) {
            routeMenuItem.setSelected(false);
            switchboardPanel.cancelRouteCreationMode();
        }
    }

    private void saveRouteFromPanel() {
        java.util.List<int[]> path = switchboardPanel.exitRouteCreationMode();
        if (path.isEmpty()) {
            routeMenuItem.setSelected(false);
            return;
        }
        java.util.Set<Integer> stops = switchboardPanel.getRouteCreationStops();
        String name = javax.swing.JOptionPane.showInputDialog(frame,
            messages.getString("dialog.trainRoute.name"),
            messages.getString("dialog.trainRoute.title"),
            javax.swing.JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.trim().isEmpty()) {
            routeMenuItem.setSelected(false);
            return;
        }
        int routeCount = (int) switchboardPanel.getRouteModel().getRoutes().values().stream()
            .count();
        String id = "TR-" + String.format("%03d", routeCount + 1);
        org.bidib.switchboard.component.model.Route namedRoute =
            new org.bidib.switchboard.component.model.Route(id, name.trim(), null, null, path);
        for (int stopIdx : stops) {
            namedRoute.addStop(stopIdx, 5000);
        }
        switchboardPanel.getRouteModel().addRoute(namedRoute);
        routeMenuItem.setSelected(false);
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

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(toolbar, BorderLayout.PAGE_START);
        frame.add(desktop, BorderLayout.CENTER);

        // prepare the dockables
        desktop.addDockable(trainListPanel);
        desktop.split(trainListPanel, switchboardPanel, com.vlsolutions.swing.docking.DockingConstants.SPLIT_RIGHT);
        desktop.setDockableWidth(this.trainListPanel, 0.2d);
        desktop.split(trainListPanel, routeListPanel, com.vlsolutions.swing.docking.DockingConstants.SPLIT_BOTTOM);

        // add route details as a tab alongside route list
        desktop.addDockable(routeDetailsPanel);
        com.vlsolutions.swing.docking.TabbedDockableContainer container =
            com.vlsolutions.swing.docking.DockingUtilities.findTabbedDockableContainer(routeListPanel);
        int order = (container != null) ? container.getTabCount() : 1;
        desktop.createTab(routeListPanel, routeDetailsPanel, order, false);

        frame.setSize(1280, 768);
        frame.setLocationRelativeTo(null);
        updateTitle();

        frame.setVisible(true);
    }

    JFrame getFrame() {
        return frame;
    }

    SwitchboardPanel getPanel() {
        return switchboardPanel;
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

    private void rebuildRecentMenu() {
        recentMenu.removeAll();
        List<String> recentFiles = settings.getRecentFiles();
        if (recentFiles.isEmpty()) {
            recentMenu.setEnabled(false);
            return;
        }
        recentMenu.setEnabled(true);
        for (int i = 0; i < recentFiles.size(); i++) {
            String filePath = recentFiles.get(i);
            Path path = Path.of(filePath);
            String displayName = path.getFileName().toString();
            JMenuItem item = new JMenuItem((i + 1) + ". " + displayName);
            item.setToolTipText(filePath);
            item.addActionListener(e -> {
                try {
                    org.bidib.switchboard.component.persistence.LayoutData data = layoutService.loadLayout(path);
                    updateTitle();
                    rebuildRecentMenu();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error loading file:\n" + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            recentMenu.add(item);
        }
    }
}
