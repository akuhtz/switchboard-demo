package org.bidib.switchboard.component.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.RouteModel;
import org.bidib.switchboard.component.model.Tile;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

public class RouteDetailsPanel extends JPanel implements Dockable {

    private static final long serialVersionUID = 1L;

    private final DockKey dockKey = new DockKey("routeDetails");

    private final TileGrid tileGrid;

    private final RouteModel routeModel;

    private final JTextField nameField;

    private final JButton saveButton;

    private final JButton cancelButton;

    private final JButton runButton;

    private final JLabel altLabel;

    private boolean editMode;

    private final JTree tree;

    private final DefaultTreeModel treeModel;

    private final DefaultMutableTreeNode rootNode;

    private final ResourceBundle messages;

    private Route displayedRoute;

    private final java.beans.PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);

    public RouteDetailsPanel(TileGrid tileGrid, RouteModel routeModel) {
        this.tileGrid = tileGrid;
        this.routeModel = routeModel;
        this.messages = ResourceBundle.getBundle("i18n.messages");

        dockKey.setName(messages != null ? messages.getString("routeDetails.title") : "Route Details");
        dockKey.setTooltip(messages != null ? messages.getString("routeDetails.tooltip") : "Details of the selected route");

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 200));

        // North: name field + alternatives badge
        String nameLabel = messages != null ? messages.getString("routeDetails.routeName") : "Route Name:";
        nameField = new JTextField();
        nameField.setEnabled(false);
        altLabel = new JLabel("");
        FormBuilder builder = FormBuilder.create()
            .columns("right:pref, 3dlu, 60dlu:grow")
            .rows("pref, 3dlu, pref, 3dlu, pref:grow:fill, 3dlu, pref");
        builder.border(Paddings.TABBED_DIALOG);

        builder.add(nameLabel).xy(1, 1);
        builder.add(nameField).xy(3, 1);
        builder.add(altLabel).xyw(1, 3, 3);

        // Center: tree
        rootNode = new DefaultMutableTreeNode("Root");
        treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        JScrollPane scrollPane = new JScrollPane(tree);
        builder.add(scrollPane).xyw(1, 5, 3);
        
        // South: buttons
        String saveText = messages != null ? messages.getString("routeDetails.save") : "Save";
        String cancelText = messages != null ? messages.getString("routeDetails.cancel") : "Cancel";
        String runText = messages != null ? messages.getString("routeDetails.run") : "Run";
        saveButton = new JButton(saveText);
        cancelButton = new JButton(cancelText);
        runButton = new JButton(runText);
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        runButton.setEnabled(false);
        runButton.setVisible(false);
        JPanel buttonPanel = FormBuilder.create()
            .columns("pref, 4dlu, pref, 4dlu, pref")
            .rows("pref")
            .add(saveButton).xy(1, 1)
            .add(cancelButton).xy(3, 1)
            .add(runButton).xy(5, 1)
            .build();
//        add(buttonPanel, BorderLayout.SOUTH);
        builder.add(buttonPanel).xyw(1, 7, 3);
        
        JPanel contentPanel = builder.build();
        JScrollPane panelScrollPane = new JScrollPane(contentPanel);
        add(panelScrollPane, BorderLayout.CENTER);

        saveButton.addActionListener(e -> onSave());
        cancelButton.addActionListener(e -> onCancel());
        runButton.addActionListener(e -> onRun());

        // Refresh the alternatives badge when the route model changes
        routeModel.addPropertyChangeListener(evt -> SwingUtilities.invokeLater(this::refreshAltBadge));
    }

    /**
     * Shows or hides the Run button. The Run button is only available in edit mode.
     *
     * @param editMode true to show the Run button
     */
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        runButton.setVisible(editMode && displayedRoute != null);
    }

    private void refreshAltBadge() {
        if (displayedRoute == null) {
            altLabel.setText("");
            return;
        }
        int altCount = routeModel.getAlternativeRoutes(displayedRoute.getId()).size();
        int selectedIdx = routeModel.getSelectedAlternativeIndex(displayedRoute.getId());
        String text;
        if (altCount == 0) {
            text = messages.getString("routeDetails.noAlternatives");
        }
        else {
            text = MessageFormat.format(messages.getString("routeDetails.alternatives"), altCount,
                selectedIdx >= 0 ? String.valueOf(selectedIdx + 1) : "-");
        }
        altLabel.setText(text);
    }

    private void onRun() {
        if (displayedRoute == null) {
            return;
        }
        pcs.firePropertyChange("routeRun", null, displayedRoute);
    }

    public void setRoute(Route route) {
        this.displayedRoute = route;
        rootNode.removeAllChildren();
        if (route == null) {
            nameField.setText("");
            nameField.setEnabled(false);
            saveButton.setEnabled(false);
            cancelButton.setEnabled(false);
            runButton.setEnabled(false);
            runButton.setVisible(false);
            altLabel.setText("");
            treeModel.reload();
            return;
        }

        nameField.setText(route.getName());
        nameField.setEnabled(true);
        saveButton.setEnabled(true);
        cancelButton.setEnabled(true);
        runButton.setEnabled(true);
        runButton.setVisible(editMode);
        refreshAltBadge();

        java.util.List<int[]> path = route.getPath();
        for (int i = 0; i < path.size(); i++) {
            int[] coord = path.get(i);
            int col = coord[0];
            int row = coord[1];
            Tile tile = tileGrid.getTile(col, row);

            boolean isLast = (i == path.size() - 1);
            boolean include = isLast;

            if (tile instanceof ElementTile et) {
                ElementType type = et.getElementType();
                if (isRelevant(type)) {
                    include = true;
                }
            }

            if (include) {
                String label = buildNodeLabel(col, row, tile, isLast);
                rootNode.add(new DefaultMutableTreeNode(label));
            }
        }
        treeModel.reload();
        expandAll();
    }

    private boolean isRelevant(ElementType type) {
        return switch (type) {
            case TURNOUT_LEFT, TURNOUT_RIGHT, TURNOUT_3WAY,
                 DIAGONAL_TURNOUT_RIGHT, DIAGONAL_TURNOUT_LEFT,
                 SIGNAL_M3, SIGNAL_COMBINED -> true;
            default -> false;
        };
    }

    private String buildNodeLabel(int col, int row, Tile tile, boolean isLast) {
        String pos = "(" + col + "," + row + ")";
        if (tile instanceof ElementTile et) {
            String id = et.getElementId() != null ? et.getElementId() : "";
            ElementType type = et.getElementType();
            String typeName = messages != null
                ? messages.getString("elementType." + type.name())
                : type.name();
            return pos + " " + id + " \u2014 " + typeName;
        }
        String id = tile != null && tile.getElementId() != null ? tile.getElementId() : "";
        return pos + " " + id;
    }

    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void onSave() {
        if (displayedRoute == null) {
            return;
        }
        String newName = nameField.getText().trim();
        if (newName.isEmpty()) {
            String errorKey = "routeDetails.error.nameEmpty";
            String error = messages != null ? messages.getString(errorKey) : "Route name must not be empty.";
            JOptionPane.showMessageDialog(this, error, dockKey.getName(), JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Check uniqueness: no other route with same name but different id
        for (Route r : routeModel.getRoutes().values()) {
            if (!r.getId().equals(displayedRoute.getId()) && newName.equals(r.getName())) {
                String errorKey = "routeDetails.error.nameDuplicate";
                String error = messages != null ? messages.getString(errorKey) : "A route with this name already exists.";
                JOptionPane.showMessageDialog(this, error, dockKey.getName(), JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        String oldName = displayedRoute.getName();
        displayedRoute.setName(newName);
        pcs.firePropertyChange("routeSaved", oldName, newName);
    }

    private void onCancel() {
        if (displayedRoute == null) {
            return;
        }
        nameField.setText(displayedRoute.getName());
        pcs.firePropertyChange("routeCancelled", null, displayedRoute);
    }

    @Override
    public DockKey getDockKey() {
        return dockKey;
    }

    @Override
    public Component getComponent() {
        return this;
    }

    public Route getDisplayedRoute() {
        return displayedRoute;
    }

    public void addRouteDetailsListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removeRouteDetailsListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}
