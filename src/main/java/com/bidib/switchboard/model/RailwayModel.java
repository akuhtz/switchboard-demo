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
    private final Map<String, SignalAspect> signals = new LinkedHashMap<>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    // --- Turnout operations ---

    public void addTurnout(String id) {
        turnouts.put(id, TurnoutAspect.STRAIGHT);
    }

    public void toggleTurnout(String id) {
        TurnoutAspect old = turnouts.get(id);
        if (old == null) {
            throw new IllegalArgumentException("Unknown turnout: " + id);
        }
        TurnoutAspect next = (old == TurnoutAspect.STRAIGHT) ? TurnoutAspect.DIVERTED : TurnoutAspect.STRAIGHT;
        turnouts.put(id, next);
        pcs.firePropertyChange(id, old, next);
    }

    public TurnoutAspect getTurnoutAspect(String id) {
        return turnouts.get(id);
    }

    public Map<String, TurnoutAspect> getTurnouts() {
        return Collections.unmodifiableMap(turnouts);
    }

    // --- Signal operations ---

    public void addSignal(String id) {
        signals.put(id, SignalAspect.RED);
    }

    public void setSignal(String id, SignalAspect aspect) {
        SignalAspect old = signals.get(id);
        if (old == null) {
            throw new IllegalArgumentException("Unknown signal: " + id);
        }
        signals.put(id, aspect);
        pcs.firePropertyChange(id, old, aspect);
    }

    public SignalAspect getSignalAspect(String id) {
        return signals.get(id);
    }

    public Map<String, SignalAspect> getSignals() {
        return Collections.unmodifiableMap(signals);
    }

    // --- Observer support ---

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}
