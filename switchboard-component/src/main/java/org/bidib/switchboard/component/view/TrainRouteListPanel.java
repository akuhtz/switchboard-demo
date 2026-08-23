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

import org.bidib.switchboard.component.model.TrainRoute;
import org.bidib.switchboard.component.model.TrainRouteListModel;

import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockKey;

public class TrainRouteListPanel extends JPanel implements Dockable, PropertyChangeListener {

    private static final long serialVersionUID = 1L;

    private final DockKey dockKey = new DockKey("trainRouteList");

    private final TrainRouteListModel trainRouteListModel;

    private final DefaultListModel<TrainRoute> listModel = new DefaultListModel<>();

    private final JList<TrainRoute> routeList;

    private Consumer<TrainRoute> selectionListener;

    public TrainRouteListPanel(TrainRouteListModel trainRouteListModel, ResourceBundle messages) {
        this.trainRouteListModel = trainRouteListModel;
        trainRouteListModel.addPropertyChangeListener(this);

        dockKey.setName(messages != null ? messages.getString("trainRouteList.title") : "Train Routes");
        dockKey.setTooltip(messages != null ? messages.getString("trainRouteList.tooltip") : "List of train routes");

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 200));

        routeList = new JList<>(listModel);
        routeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        routeList.setCellRenderer(new TrainRouteCellRenderer());
        routeList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    TrainRoute selected = routeList.getSelectedValue();
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

    public void setSelectionListener(Consumer<TrainRoute> listener) {
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

    public TrainRoute getSelectedTrainRoute() {
        return routeList.getSelectedValue();
    }

    private void syncFromModel() {
        TrainRoute oldSelection = routeList.getSelectedValue();
        listModel.clear();
        for (TrainRoute route : trainRouteListModel.getTrainRoutes()) {
            listModel.addElement(route);
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

    private static class TrainRouteCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, "", index, isSelected, cellHasFocus);
            if (value instanceof TrainRoute route) {
                int stopCount = route.getStops().size();
                int totalDwellMs = route.getStops().stream().mapToInt(TrainRoute.StationStop::getDwellTimeMs).sum();
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
