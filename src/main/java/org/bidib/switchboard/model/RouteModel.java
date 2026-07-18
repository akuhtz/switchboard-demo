package org.bidib.switchboard.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RouteModel {

    public static final String PROP_ROUTES = "routes";

    private final Map<String, Route> routes = new LinkedHashMap<>();
    private final Map<String, List<Route>> alternativeRoutes = new LinkedHashMap<>();
    private final Map<String, Integer> selectedAlternativeIndex = new LinkedHashMap<>();

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
        alternativeRoutes.remove(id);
        selectedAlternativeIndex.remove(id);
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

    public void addAlternativeRoute(String id, Route alt) {
        List<Route> alts = alternativeRoutes.computeIfAbsent(id, k -> new ArrayList<>());
        if (alts.isEmpty()) {
            selectedAlternativeIndex.put(id, 0);
        }
        alts.add(alt);
    }

    public Route getAlternativeRoute(String id) {
        List<Route> alts = alternativeRoutes.get(id);
        if (alts == null || alts.isEmpty()) return null;
        int idx = selectedAlternativeIndex.getOrDefault(id, -1);
        if (idx < 0 || idx >= alts.size()) return null;
        return alts.get(idx);
    }

    public int getSelectedAlternativeIndex(String id) {
        return selectedAlternativeIndex.getOrDefault(id, -1);
    }

    public void setSelectedAlternativeIndex(String id, int index) {
        if (index < 0) {
            selectedAlternativeIndex.remove(id);
        } else {
            List<Route> alts = alternativeRoutes.get(id);
            if (alts != null && index < alts.size()) {
                selectedAlternativeIndex.put(id, index);
            }
        }
    }

    public List<Route> getAlternativeRoutes(String id) {
        List<Route> alts = alternativeRoutes.get(id);
        return alts != null ? Collections.unmodifiableList(alts) : List.of();
    }

    public boolean hasAlternativeRoute(String id) {
        List<Route> alts = alternativeRoutes.get(id);
        return alts != null && !alts.isEmpty();
    }

    public void clearAlternatives(String id) {
        alternativeRoutes.remove(id);
        selectedAlternativeIndex.remove(id);
    }

    public void swapWithAlternative(String id) {
        List<Route> alts = alternativeRoutes.get(id);
        Route primary = routes.get(id);
        if (primary != null && alts != null && !alts.isEmpty()) {
            int idx = selectedAlternativeIndex.getOrDefault(id, 0);
            if (idx < 0 || idx >= alts.size()) idx = 0;
            Route next = alts.remove(idx);
            routes.put(id, next);
            alternativeRoutes.remove(id);
            selectedAlternativeIndex.remove(id);
            pcs.firePropertyChange(PROP_ROUTES, primary, next);
        }
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
        alternativeRoutes.clear();
        selectedAlternativeIndex.clear();
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
