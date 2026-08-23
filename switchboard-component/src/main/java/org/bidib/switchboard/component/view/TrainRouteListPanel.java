package org.bidib.switchboard.component.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

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

        JScrollPane scrollPane = new JScrollPane(routeList);
        add(scrollPane, BorderLayout.CENTER);

        syncFromModel();
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
        listModel.clear();
        for (TrainRoute route : trainRouteListModel.getTrainRoutes()) {
            listModel.addElement(route);
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
