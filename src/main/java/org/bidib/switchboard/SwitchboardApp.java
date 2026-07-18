package org.bidib.switchboard;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bidib.switchboard.model.Occupancy;
import org.bidib.switchboard.model.RailwayModel;
import org.bidib.switchboard.persistence.SettingsData.LookAndFeel;
import org.bidib.switchboard.persistence.SettingsManager;
import org.bidib.switchboard.service.LayoutService;
import org.bidib.switchboard.view.SwitchboardPanel;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

public class SwitchboardApp {

    private static final Logger log = LoggerFactory.getLogger(SwitchboardApp.class);

    private final RailwayModel model;

    private final SwitchboardPanel panel;

    private final JFrame frame;

    private final SettingsManager settings;

    private final LayoutService layoutService;

    SwitchboardApp() {
        this(true);
    }

    SwitchboardApp(boolean autoLoad) {
        model = new RailwayModel();
        panel = new SwitchboardPanel(model);
        settings = new SettingsManager();

        if (LookAndFeel.DARK == settings.getLookAndFeel()) {
            FlatDarkLaf.setup();
        }
        else {
            FlatLightLaf.setup();
        }

        frame = new JFrame("Model Railway Switchboard");
        layoutService = new LayoutService(panel, settings, frame);
        if (autoLoad) {
            layoutService.tryAutoLoad();
            updateTitle();
        }
        buildMenu();
        buildFrame();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SwitchboardApp::new);
    }

    // --- Menu ---

    private JCheckBoxMenuItem editModeItem;

    private JToggleButton editToggle;

    private void buildMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        JMenuItem loadItem = new JMenuItem("Load...");
        loadItem.setMnemonic('L');
        loadItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke("control L"));
        loadItem.addActionListener(e -> { layoutService.onLoad(); updateTitle(); });
        fileMenu.add(loadItem);

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setMnemonic('S');
        saveItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke("control S"));
        saveItem.addActionListener(e -> layoutService.onSave());
        fileMenu.add(saveItem);

        JMenuItem saveAsItem = new JMenuItem("Save As...");
        saveAsItem.setMnemonic('A');
        saveAsItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke("control shift S"));
        saveAsItem.addActionListener(e -> { layoutService.onSaveAs(); updateTitle(); });
        fileMenu.add(saveAsItem);

        fileMenu.addSeparator();

        JMenu settingsMenu = new JMenu("Settings");
        settingsMenu.setMnemonic('S');
        ButtonGroup lafGroup = new ButtonGroup();
        JRadioButtonMenuItem lightItem = new JRadioButtonMenuItem("Light Look and Feel");
        lightItem.setSelected(settings.getLookAndFeel() == LookAndFeel.LIGHT);
        lightItem.addActionListener(e -> applyLookAndFeel(LookAndFeel.LIGHT));
        lafGroup.add(lightItem);
        settingsMenu.add(lightItem);
        JRadioButtonMenuItem darkItem = new JRadioButtonMenuItem("Dark Look and Feel");
        darkItem.setSelected(settings.getLookAndFeel() == LookAndFeel.DARK);
        darkItem.addActionListener(e -> applyLookAndFeel(LookAndFeel.DARK));
        lafGroup.add(darkItem);
        settingsMenu.add(darkItem);

        settingsMenu.addSeparator();
        JCheckBoxMenuItem exhaustiveItem = new JCheckBoxMenuItem("Exhaustive Route Search");
        exhaustiveItem.setSelected(settings.isExhaustiveRouting());
        exhaustiveItem.addActionListener(e -> {
            boolean selected = exhaustiveItem.isSelected();
            panel.setExhaustiveRouting(selected);
            settings.setExhaustiveRouting(selected);
        });
        settingsMenu.add(exhaustiveItem);
        panel.setExhaustiveRouting(settings.isExhaustiveRouting());

        fileMenu.add(settingsMenu);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic('X');
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');
        editModeItem = new JCheckBoxMenuItem("Edit Mode");
        editModeItem.setMnemonic('M');
        editModeItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke("control E"));
        editModeItem.addActionListener(e -> setEditMode(editModeItem.isSelected()));
        editMenu.add(editModeItem);

        editMenu.addSeparator();
        JMenuItem loadDefaultItem = new JMenuItem("Load Default Layout");
        loadDefaultItem.addActionListener(e -> { layoutService.loadDefaultLayout(); updateTitle(); });
        editMenu.add(loadDefaultItem);

        editMenu.addSeparator();
        JMenuItem occupanciesItem = new JMenuItem("Occupancies...");
        occupanciesItem.addActionListener(e -> showOccupanciesDialog());
        editMenu.add(occupanciesItem);

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
        editToggle = new JToggleButton("Edit Mode");
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
        String name = layoutService.getCurrentFilePath() != null ? layoutService.getCurrentFilePath().getFileName().toString() : "untitled";
        frame.setTitle("Model Railway Switchboard - " + name);
    }

    // --- Helpers ---

    private void showOccupanciesDialog() {
        Map<String, Occupancy> occs = model.getOccupancies();
        List<Occupancy> sorted = occs.values().stream()
            .sorted(Comparator.comparingLong(Occupancy::getNodeId)
                .thenComparingInt(Occupancy::getPortId))
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
                    case 0 -> o.getNodeId();
                    case 1 -> o.getPortId();
                    case 2 -> o.getState();
                    default -> null;
                };
            }

            @Override
            public String getColumnName(int col) {
                return columns[col];
            }
        });

        JDialog dialog = new JDialog(frame, "Occupancies", false);
        dialog.add(new JScrollPane(table));
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

}
