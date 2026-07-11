package com.bidib.switchboard.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RailwayModel {

    private final Map<String, Integer> elements = new LinkedHashMap<>();
    private final Map<String, Integer> aspectCounts = new LinkedHashMap<>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addElement(String id, int aspectCount) {
        if (aspectCount < 1) {
            throw new IllegalArgumentException("Aspect count must be at least 1, got: " + aspectCount);
        }
        elements.put(id, 0);
        aspectCounts.put(id, aspectCount);
    }

    public void cycleElement(String id) {
        Integer old = elements.get(id);
        if (old == null) {
            throw new IllegalArgumentException("Unknown element: " + id);
        }
        int count = aspectCounts.get(id);
        int next = (old + 1) % count;
        elements.put(id, next);
        pcs.firePropertyChange(id, (Object) old, (Object) next);
    }

    public void setElementAspect(String id, int aspect) {
        Integer old = elements.get(id);
        if (old == null) {
            throw new IllegalArgumentException("Unknown element: " + id);
        }
        int count = aspectCounts.get(id);
        if (aspect < 0 || aspect >= count) {
            throw new IllegalArgumentException("Aspect " + aspect + " out of range for element "
                    + id + " (count=" + count + ")");
        }
        elements.put(id, aspect);
        pcs.firePropertyChange(id, (Object) old, (Object) aspect);
    }

    public Integer getElementAspect(String id) {
        return elements.get(id);
    }

    public int getElementAspectCount(String id) {
        return aspectCounts.getOrDefault(id, 0);
    }

    public Map<String, Integer> getElementAspects() {
        return Collections.unmodifiableMap(elements);
    }

    public Map<String, Integer> getElementAspectCounts() {
        return Collections.unmodifiableMap(aspectCounts);
    }

    public void clear() {
        elements.clear();
        aspectCounts.clear();
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}
