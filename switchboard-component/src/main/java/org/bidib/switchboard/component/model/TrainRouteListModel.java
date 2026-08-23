package org.bidib.switchboard.component.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.jgoodies.binding.beans.Model;

public class TrainRouteListModel extends Model {

    private static final long serialVersionUID = 1L;

    private final List<TrainRoute> trainRoutes = new ArrayList<>();

    public List<TrainRoute> getTrainRoutes() {
        return Collections.unmodifiableList(trainRoutes);
    }

    public void setTrainRoutes(List<? extends TrainRoute> newRoutes) {
        List<TrainRoute> old = new ArrayList<>(this.trainRoutes);
        this.trainRoutes.clear();
        this.trainRoutes.addAll(newRoutes);
        firePropertyChange("trainRoutes", old, getTrainRoutes());
    }

    public void addTrainRoute(TrainRoute route) {
        Objects.requireNonNull(route, "route");
        trainRoutes.add(route);
        firePropertyChange("trainRoutes", null, getTrainRoutes());
    }

    public void removeTrainRoute(TrainRoute route) {
        if (trainRoutes.remove(route)) {
            firePropertyChange("trainRoutes", null, getTrainRoutes());
        }
    }

    public TrainRoute getTrainRouteById(String id) {
        return trainRoutes.stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
    }

    public int size() {
        return trainRoutes.size();
    }

    public boolean isEmpty() {
        return trainRoutes.isEmpty();
    }
}
