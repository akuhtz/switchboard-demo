package com.bidib.switchboard.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RailwayModel {

    private final Map<String, Integer> elements = new LinkedHashMap<>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addElement(String id) {
        elements.put(id, 0);
    }

    public void setElementAspect(String id, int aspect) {
        Integer old = elements.get(id);
        if (old == null) {
            throw new IllegalArgumentException("Unknown element: " + id);
        }
        elements.put(id, aspect);
        pcs.firePropertyChange(id, (Object) old, (Object) aspect);
    }

    public Integer getElementAspect(String id) {
        return elements.get(id);
    }

    public Map<String, Integer> getElementAspects() {
        return Collections.unmodifiableMap(elements);
    }

    public void clear() {
        elements.clear();
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}
