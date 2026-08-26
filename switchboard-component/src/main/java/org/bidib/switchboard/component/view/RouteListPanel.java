package org.bidib.switchboard.component.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.RouteModel;

import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockKey;

public class RouteListPanel extends JPanel implements Dockable, PropertyChangeListener {

    private static final long serialVersionUID = 1L;

    private final DockKey dockKey = new DockKey("routeList");

    private final RouteModel routeModel;

    private final DefaultListModel<Route> listModel = new DefaultListModel<>();

    private final JList<Route> routeList;

    private Consumer<Route> selectionListener;

    private Consumer<Route> deleteAction;

    private boolean editMode;

    private final ResourceBundle messages = ResourceBundle.getBundle("i18n.messages");

    public RouteListPanel(RouteModel routeModel) {
        this.routeModel = routeModel;
        routeModel.addPropertyChangeListener(this);

        dockKey.setName(messages != null ? messages.getString("routeList.title") : "Routes");
        dockKey.setTooltip(messages != null ? messages.getString("routeList.tooltip") : "List of routes");

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 200));

        routeList = new JList<>(listModel);
        routeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        routeList.setCellRenderer(new RouteCellRenderer());
        routeList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    Route selected = routeList.getSelectedValue();
                    if (selectionListener != null) {
                        selectionListener.accept(selected);
                    }
                }
            }
        });
        routeList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showContextMenu(e);
            }
        });

        JScrollPane scrollPane = new JScrollPane(routeList);
        add(scrollPane, BorderLayout.CENTER);

        syncFromModel();
    }

    public void setSelectionListener(Consumer<Route> listener) {
        this.selectionListener = listener;
    }

    public void setDeleteAction(Consumer<Route> action) {
        this.deleteAction = action;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    @Override
    public DockKey getDockKey() {
        return dockKey;
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        SwingUtilities.invokeLater(this::syncFromModel);
    }

    public Route getSelectedRoute() {
        return routeList.getSelectedValue();
    }

    /**
     * Programmatically selects the route with the given ID (if present in the list).
     *
     * @param routeId the route ID to select
     */
    public void selectRoute(String routeId) {
        for (int i = 0; i < listModel.size(); i++) {
            if (listModel.get(i).getId().equals(routeId)) {
                routeList.setSelectedIndex(i);
                routeList.ensureIndexIsVisible(i);
                return;
            }
        }
    }

    private void showContextMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int index = routeList.locationToIndex(e.getPoint());
        if (index < 0) {
            return;
        }
        routeList.setSelectedIndex(index);
        Route route = listModel.getElementAt(index);
        if (route == null) {
            return;
        }

        JPopupMenu menu = new JPopupMenu();
        if (editMode && deleteAction != null) {
            String label = messages.getString("routeList.context.deleteRoute");
            javax.swing.JMenuItem deleteItem = new javax.swing.JMenuItem(label);
            deleteItem.addActionListener(ev -> confirmDelete(route));
            menu.add(deleteItem);
        }
        if (menu.getComponentCount() > 0) {
            menu.show(routeList, e.getX(), e.getY());
        }
    }

    private void confirmDelete(Route route) {
        String title = messages.getString("route.title");
        String message = MessageFormat.format(messages.getString("route.confirmDelete"),
            route.getName(), route.getId());
        int choice = JOptionPane.showConfirmDialog(this, message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION && deleteAction != null) {
            deleteAction.accept(route);
        }
    }

    private void syncFromModel() {
        Route oldSelection = routeList.getSelectedValue();
        listModel.clear();
        for (Route route : routeModel.getRoutes().values()) {
            if (route.getName() != null) {
                listModel.addElement(route);
            }
        }
        // Try to restore selection
        if (oldSelection != null) {
            for (int i = 0; i < listModel.size(); i++) {
                if (listModel.get(i).getId().equals(oldSelection.getId())) {
                    routeList.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private static class RouteCellRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, "", index, isSelected, cellHasFocus);
            if (value instanceof Route route) {
                int stopCount = route.getStops().size();
                int totalDwellMs = route.getStops().stream().mapToInt(Route.StationStop::getDwellTimeMs).sum();
                String suffix;
                if (stopCount == 0) {
                    suffix = "";
                } else {
                    suffix = " (" + stopCount + " stop" + (stopCount == 1 ? "" : "s")
                        + (totalDwellMs > 0 ? ", " + (totalDwellMs / 1000) + "s" : "") + ")";
                }
                setText(route.getName() + suffix);
            }
            return this;
        }
    }
}
