package org.bidib.switchboard.component.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
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

    public RouteListPanel(RouteModel routeModel, ResourceBundle messages) {
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

        JScrollPane scrollPane = new JScrollPane(routeList);
        add(scrollPane, BorderLayout.CENTER);

        syncFromModel();
    }

    public void setSelectionListener(Consumer<Route> listener) {
        this.selectionListener = listener;
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
