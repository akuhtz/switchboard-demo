# Plan: TrainRoute Feature

## Overview
Add a TrainRoute concept: a named sequence of tile coordinates with station stops (dwell times). A TrainRoute is self-contained (stores its path as coordinates), can reference any train, and persists with the layout.

## 1. Data Model — `TrainRoute.java` (new)
**Path:** `switchboard-component/src/main/java/org/bidib/switchboard/component/model/TrainRoute.java`

```java
public class TrainRoute {
    private final String id;                      // unique ID
    private String name;                          // user-editable name
    private final List<int[]> path;               // ordered tile coordinates
    private final List<StationStop> stops;        // station stops along the path

    public static class StationStop {
        private final int pathIndex;              // index into path list
        private final int dwellTimeMs;            // time to wait at this station
    }
}
```

Key methods:
- `addStop(int pathIndex, int dwellTimeMs)` / `removeStop(int pathIndex)`
- `getStopAt(int pathIndex)` — returns stop or null
- `containsTile(int col, int row)` — checks if tile is on this route's path
- `getId()`, `getName()`, `setName()`, `getPath()`, `getStops()`

## 2. List Model — `TrainRouteListModel.java` (new)
**Path:** `switchboard-component/src/main/java/org/bidib/switchboard/component/model/TrainRouteListModel.java`

Follow `TrainListModel` pattern:
- Extends `Model` (JGoodies) for property change support
- `List<TrainRoute>` internally
- `addTrainRoute(TrainRoute)`, `removeTrainRoute(String id)`, `getTrainRoute(String id)`, `getTrainRoutes()`, `size()`, `isEmpty()`
- Fires `PropertyChangeEvent("trainRoutes", ...)` on mutation

Held by `RailwayModel` (add `TrainRouteListModel trainRouteModel` field + getter).

## 3. Persistence — `TrainRouteData` inner class in `LayoutData.java`
**Path:** `switchboard-component/src/main/java/org/bidib/switchboard/component/persistence/LayoutData.java`

```java
public static class TrainRouteData {
    String id;
    String name;
    List<List<Integer>> tiles;          // [[col,row], ...]
    List<StationStopData> stops;

    public static class StationStopData {
        int pathIndex;
        int dwellTimeMs;
    }
}
```

Add to `LayoutData`:
- `List<TrainRouteData> trainRoutes` field + getter/setter
- Default empty list

Update `LayoutPersistence`:
- In `capture()`: serialize `TrainRouteListModel` → `List<TrainRouteData>`
- In `apply()`: deserialize `List<TrainRouteData>` → add to `TrainRouteListModel`

## 4. UI Panel — `TrainRouteListPanel.java` (new)
**Path:** `switchboard-component/src/main/java/org/bidib/switchboard/component/view/TrainRouteListPanel.java`

Follow `TrainListPanel` pattern:
- Extends `JPanel`, implements `Dockable`, `PropertyChangeListener`
- `DockKey("trainRouteList")`
- `JList<TrainRoute>` with custom renderer showing: `"name (N stops, Xs total)"` 
- Single selection mode
- Listens to `TrainRouteListModel` property changes, syncs via `SwingUtilities.invokeLater()`

Renderer shows per row:
- TrainRoute name
- Number of stops and total dwell time in parentheses

## 5. Integration — `SwitchboardApp.java`
**Path:** `switchboard-demo-app/src/main/java/org/bidib/switchboard/demoapp/SwitchboardApp.java`

Changes:
- Create `TrainRouteListModel` and add to `RailwayModel`
- Create `TrainRouteListPanel` dockable
- Split below `TrainListPanel` using `DockingConstants.SPLIT_BOTTOM`
- Save/load `trainRoutes` via `LayoutPersistence` (already handled if we update the persistence)

Layout structure after change:
```
+------------------+----------------------------------+
| TrainListPanel   |                                  |
| (20% width)      |                                  |
+------------------+    SwitchboardPanel              |
| TrainRouteList   |                                  |
| Panel            |                                  |
+------------------+----------------------------------+
```

## 6. i18n — `messages.properties` / `messages_de.properties`
Add keys for:
- `trainRouteList.title` / `trainRouteList.tooltip`
- `trainRoute.stop` / `trainRoute.stops` / `trainRoute.dwellTime`

## Files Changed
| File | Action |
|------|--------|
| `TrainRoute.java` | **New** — model class |
| `TrainRouteListModel.java` | **New** — list model |
| `RailwayModel.java` | **Edit** — add `TrainRouteListModel` field |
| `LayoutData.java` | **Edit** — add `TrainRouteData` inner class + field |
| `LayoutPersistence.java` | **Edit** — save/load train routes |
| `TrainRouteListPanel.java` | **New** — dockable panel |
| `SwitchboardApp.java` | **Edit** — create panel, split layout |
| `messages.properties` | **Edit** — add i18n keys |
| `messages_de.properties` | **Edit** — add i18n keys |

## Out of Scope (Future)
- UI for creating/editing TrainRoutes (clicking tiles to define path and stops)
- Running a TrainRoute (simulation with station stops)
- TrainRoute context menu actions
