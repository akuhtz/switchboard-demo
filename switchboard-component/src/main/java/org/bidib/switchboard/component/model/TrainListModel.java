package org.bidib.switchboard.component.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.jgoodies.binding.beans.Model;

/**
 * Model holding a list of trains.
 */
public class TrainListModel extends Model {

    private static final long serialVersionUID = 1L;

    private final List<Train> trains = new ArrayList<>();

    public List<Train> getTrains() {
        return Collections.unmodifiableList(trains);
    }

    public void setTrains(List<? extends Train> newTrains) {
        List<Train> old = new ArrayList<>(this.trains);
        this.trains.clear();
        this.trains.addAll(newTrains);
        firePropertyChange("trains", old, getTrains());
    }

    public void addTrain(Train train) {
        Objects.requireNonNull(train, "train");
        trains.add(train);
        firePropertyChange("trains", null, getTrains());
    }

    public void removeTrain(Train train) {
        if (trains.remove(train)) {
            firePropertyChange("trains", null, getTrains());
        }
    }

    public Train getTrainById(String id) {
        return trains.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
    }
}
