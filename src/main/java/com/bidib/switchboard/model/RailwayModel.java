package com.bidib.switchboard.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central model class holding the state of all railway elements.
 * Uses {@link PropertyChangeSupport} to notify listeners of state mutations.
 */
public class RailwayModel {

    private final Map<String, TurnoutAspect> turnouts = new LinkedHashMap<>();
    private final Map<String, Integer> turnoutAspectCounts = new LinkedHashMap<>();
    private final Map<String, SignalAspect> signals = new LinkedHashMap<>();
    private final Map<String, Integer> signalAspectCounts = new LinkedHashMap<>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    // --- Turnout operations ---

    /**
     * Adds a 2-way turnout (STRAIGHT / DIVERTED_LEFT).
     */
    public void addTurnout(String id) {
        addTurnout(id, 2);
    }

    /**
     * Adds a turnout with the specified number of aspects.
     *
     * @param id          turnout identifier
     * @param aspectCount number of supported aspects (2 for 2-way, 3 for 3-way)
     */
    public void addTurnout(String id, int aspectCount) {
        if (aspectCount < 2 || aspectCount > TurnoutAspect.values().length) {
            throw new IllegalArgumentException("Turnout aspect count must be between 2 and "
                    + TurnoutAspect.values().length + ", got: " + aspectCount);
        }
        turnouts.put(id, TurnoutAspect.STRAIGHT);
        turnoutAspectCounts.put(id, aspectCount);
    }

    /**
     * Cycles the turnout to the next aspect.
     */
    public void toggleTurnout(String id) {
        TurnoutAspect old = turnouts.get(id);
        if (old == null) {
            throw new IllegalArgumentException("Unknown turnout: " + id);
        }
        int count = turnoutAspectCounts.get(id);
        int nextOrdinal = (old.getAspect() + 1) % count;
        TurnoutAspect next = TurnoutAspect.of(nextOrdinal);
        turnouts.put(id, next);
        pcs.firePropertyChange(id, old, next);
    }

    public void setTurnoutAspect(String id, TurnoutAspect aspect) {
        TurnoutAspect old = turnouts.get(id);
        if (old == null) {
            throw new IllegalArgumentException("Unknown turnout: " + id);
        }
        int count = turnoutAspectCounts.get(id);
        if (aspect.getAspect() >= count) {
            throw new IllegalArgumentException("Aspect " + aspect + " exceeds turnout " + id
                    + " aspect count (" + count + ")");
        }
        turnouts.put(id, aspect);
        pcs.firePropertyChange(id, old, aspect);
    }

    public TurnoutAspect getTurnoutAspect(String id) {
        return turnouts.get(id);
    }

    public int getTurnoutAspectCount(String id) {
        return turnoutAspectCounts.getOrDefault(id, 0);
    }

    public Map<String, TurnoutAspect> getTurnouts() {
        return Collections.unmodifiableMap(turnouts);
    }

    // --- Signal operations ---

    /**
     * Adds a 2-aspect signal.
     */
    public void addSignal(String id) {
        addSignal(id, 2);
    }

    /**
     * Adds a signal with the specified number of aspects.
     *
     * @param id          signal identifier
     * @param aspectCount number of supported aspects (2–8)
     */
    public void addSignal(String id, int aspectCount) {
        if (aspectCount < 2 || aspectCount > SignalAspect.values().length) {
            throw new IllegalArgumentException("Signal aspect count must be between 2 and "
                    + SignalAspect.values().length + ", got: " + aspectCount);
        }
        signals.put(id, SignalAspect.ASPECT_0);
        signalAspectCounts.put(id, aspectCount);
    }

    public void setSignal(String id, SignalAspect aspect) {
        SignalAspect old = signals.get(id);
        if (old == null) {
            throw new IllegalArgumentException("Unknown signal: " + id);
        }
        int count = signalAspectCounts.get(id);
        if (aspect.getAspect() >= count) {
            throw new IllegalArgumentException("Aspect " + aspect + " exceeds signal " + id
                    + " aspect count (" + count + ")");
        }
        signals.put(id, aspect);
        pcs.firePropertyChange(id, old, aspect);
    }

    /**
     * Cycles the signal to the next aspect.
     */
    public void toggleSignal(String id) {
        SignalAspect old = signals.get(id);
        if (old == null) {
            throw new IllegalArgumentException("Unknown signal: " + id);
        }
        int count = signalAspectCounts.get(id);
        int nextOrdinal = (old.getAspect() + 1) % count;
        SignalAspect next = SignalAspect.of(nextOrdinal);
        signals.put(id, next);
        pcs.firePropertyChange(id, old, next);
    }

    public SignalAspect getSignalAspect(String id) {
        return signals.get(id);
    }

    public int getSignalAspectCount(String id) {
        return signalAspectCounts.getOrDefault(id, 0);
    }

    public Map<String, SignalAspect> getSignals() {
        return Collections.unmodifiableMap(signals);
    }

    // --- Observer support ---

    /**
     * Removes all elements from the model.
     */
    public void clear() {
        turnouts.clear();
        turnoutAspectCounts.clear();
        signals.clear();
        signalAspectCounts.clear();
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}
