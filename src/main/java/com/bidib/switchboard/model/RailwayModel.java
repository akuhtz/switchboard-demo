package com.bidib.switchboard.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RailwayModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(RailwayModel.class);

    private final Map<String, Element> elements = new LinkedHashMap<>();

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addElement(Element element) {
        LOGGER.info("Add element: {} (nodeId={}, accessoryId={})",
                element.getId(), element.getNodeId(), element.getAccessoryId());
        elements.put(element.getId(), element);
    }

    public void setElementAspect(String id, int aspect) {
        Element el = elements.get(id);
        if (el == null) {
            throw new IllegalArgumentException("Unknown element: " + id);
        }
        int old = el.getCurrentAspect();
        el.setCurrentAspect(aspect);
        pcs.firePropertyChange(id, old, aspect);
    }

    public Integer getElementAspect(String id) {
        Element el = elements.get(id);
        return el != null ? el.getCurrentAspect() : null;
    }

    public Element getElement(String id) {
        return elements.get(id);
    }

    public Map<String, Element> getElements() {
        return Collections.unmodifiableMap(elements);
    }

    public void clear() {
        elements.clear();
    }

    public void removeElement(String id) {
        LOGGER.info("Remove element: {}", id);
        elements.remove(id);
    }

    public boolean containsElement(String id) {
        return elements.containsKey(id);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}
