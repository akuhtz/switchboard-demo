package com.bidib.switchboard.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RouteModel {

    public static final String PROP_ROUTES = "routes";

    private final Map<String, Route> routes = new LinkedHashMap<>();

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public boolean addRoute(Route route) {
        String id = route.getId();
        Route existing = routes.get(id);
        routes.put(id, route);
        pcs.firePropertyChange(PROP_ROUTES, existing, route);
        return true;
    }

    public Route removeRoute(String id) {
        Route removed = routes.remove(id);
        if (removed != null) {
            pcs.firePropertyChange(PROP_ROUTES, removed, null);
        }
        return removed;
    }

    public Route getRoute(String id) {
        return routes.get(id);
    }

    public Map<String, Route> getRoutes() {
        return Collections.unmodifiableMap(routes);
    }

    public boolean isTileReserved(int col, int row, String excludeRouteId) {
        for (Route r : routes.values()) {
            if (excludeRouteId != null && excludeRouteId.equals(r.getId())) {
                continue;
            }
            if (r.containsTile(col, row)) {
                return true;
            }
        }
        return false;
    }

    public String routeIdForTile(int col, int row) {
        for (Route r : routes.values()) {
            if (r.containsTile(col, row)) {
                return r.getId();
            }
        }
        return null;
    }

    public void clear() {
        routes.clear();
        pcs.firePropertyChange(PROP_ROUTES, null, null);
    }

    public int size() {
        return routes.size();
    }

    public boolean isEmpty() {
        return routes.isEmpty();
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}
