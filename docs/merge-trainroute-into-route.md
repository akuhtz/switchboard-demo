# Plan: Merge TrainRoute into Route

## Goal
Merge `TrainRoute` into `Route` so that one `Route` class handles both signal-to-signal connections AND timetable simulations. Route keeps all existing features (alternatives, source/target, signaling) and gains TrainRoute's stops feature.

## Files Affected (14)

### Delete (2 files)
1. `switchboard-component/src/main/java/org/bidib/switchboard/component/model/TrainRoute.java`
2. `switchboard-component/src/main/java/org/bidib/switchboard/component/model/TrainRouteListModel.java`

### Rename (2 files)
3. `TrainRouteSimulation.java` → `RouteSimulation.java`
4. `TrainRouteListPanel.java` → `RouteListPanel.java`

### Modify (10 files)
5. `Route.java` — add name, stops, StationStop inner class
6. `RouteModel.java` — no structural changes (already stores Routes)
7. `RailwayModel.java` — remove TrainRouteListModel field
8. `LayoutData.java` — merge TrainRouteData into RouteData, keep StationStopData
9. `LayoutPersistence.java` — update save/load for merged Route
10. `SwitchboardPanel.java` — replace all TrainRoute refs with Route (~100+ occurrences)
11. `SwitchboardApp.java` — replace TrainRouteListPanel with RouteListPanel
12. `TrainRouteRunUiTest.java` → rename to `RouteRunUiTest.java`, update refs

### Update i18n (4 files)
13. `app-messages.properties` / `app-messages_de.properties` — update keys
14. `messages.properties` / `messages_de.properties` — update keys

---

## Step-by-step Implementation

### Step 1: Add to `Route.java`

Add these fields and methods to the existing Route class:

```java
// New fields
private String name;  // optional, null for signal-to-signal routes
private final List<StationStop> stops = new ArrayList<>();

// New constructors (keep existing ones)
// For named routes (train routes): explicit ID, no source/target
public Route(String id, String name, List<int[]> path) {
    this.id = id;
    this.name = name;
    this.sourceElementId = null;
    this.targetElementId = null;
    this.path = new ArrayList<>(path);
}

// New methods
public String getName() { return name; }
public void setName(String name) { this.name = name; }
public List<StationStop> getStops() { return Collections.unmodifiableList(stops); }
public void addStop(int pathIndex, int dwellTimeMs) { ... } // same logic as TrainRoute
public void removeStop(int pathIndex) { ... }
public StationStop getStopAt(int pathIndex) { ... }
public boolean hasStops() { return !stops.isEmpty(); }

// Keep existing: getSourceElementId(), getTargetElementId(), getId(),
// containsTile(), shiftPath(), replacePath()

// StationStop inner class (moved from TrainRoute)
public static class StationStop {
    private final int pathIndex;
    private final int dwellTimeMs;
    // constructor, getPathIndex(), getDwellTimeMs()
}
```

### Step 2: Delete `TrainRoute.java` and `TrainRouteListModel.java`

Remove both files entirely.

### Step 3: Rename `TrainRouteSimulation.java` → `RouteSimulation.java`

- Rename class to `RouteSimulation`
- Replace all `TrainRoute` refs with `Route`
- `start(TrainRoute, String)` → `start(Route, String)`
- `start(TrainRoute, String, int)` → `start(Route, String, int)`
- `getTrainRoute()` → `getRoute()`
- `trainRoute` field → `route` field
- `TrainRoute.StationStop` → `Route.StationStop`

### Step 4: Rename `TrainRouteListPanel.java` → `RouteListPanel.java`

- Rename class to `RouteListPanel`
- Replace `TrainRoute` refs with `Route`
- Replace `TrainRouteListModel` refs with `RouteModel`
- Constructor: `RouteListPanel(RouteModel, ResourceBundle)`
- `getSelectedTrainRoute()` → `getSelectedRoute()`
- `setSelectionListener(Consumer<TrainRoute>)` → `setSelectionListener(Consumer<Route>)`
- Cell renderer: if `route.getName() != null` show name, else show `route.getId()`

### Step 5: Update `LayoutData.java`

Merge `TrainRouteData` fields into `RouteData`:

```java
public static class RouteData {
    private String id;
    private String sourceElementId;  // nullable
    private String targetElementId;  // nullable
    private String name;             // NEW: nullable
    private List<List<Integer>> tiles;
    private List<StationStopData> stops;  // NEW
    // getters/setters
}

// StationStopData stays as-is (pathIndex + dwellTimeMs)
```

Remove `TrainRouteData` class. Remove `trainRoutes` field from LayoutData.

### Step 6: Update `LayoutPersistence.java`

**Save (capture):**
- When saving routes, also save stops for each route
- Remove separate train route save block (lines 138-159)

**Load (apply):**
- When loading routes, restore stops from RouteData
- Remove separate train route load block (lines 288-303)

### Step 7: Update `RailwayModel.java`

- Remove `TrainRouteListModel` field
- Remove `getTrainRouteListModel()` method
- In `clear()`: remove `trainRouteListModel.setTrainRoutes(...)`

### Step 8: Update `SwitchboardPanel.java` (~100+ refs)

**Imports:**
- Remove `import TrainRoute`
- Keep `import Route` (already exists)

**Fields:**
- `private TrainRoute selectedTrainRoute` → `private Route selectedRoute`
- `trainRouteMode`, `trainRoutePath`, `trainRouteStops` → keep as-is (renaming is optional, these are internal to creation mode)

**Methods:**
- `setSelectedTrainRoute(TrainRoute)` → `setSelectedRoute(Route)`
- `getSelectedTrainRoute()` → `getSelectedRoute()`
- `findTrainRoutesStartingAtBlock(Block)` → `findRoutesStartingAtBlock(Block)`
  - Change `model.getTrainRouteListModel().getTrainRoutes()` → `routeModel.getRoutes().values()`
  - Match by route's first tile in block (keep existing logic)
- `startTrainRouteSimulation(TrainRoute)` → `startSimulation(Route)`
  - Replace `model.getTrainRouteListModel()` refs
- `stopTrainRouteSimulation()` → `stopSimulation()`
- `getTrainRouteSimulation()` → `getSimulation()`
- `isTileInTrainRoutePath()` — keep name (internal to creation mode)
- `trainRoutePathIndex()` — keep name (internal to creation mode)
- `drawTrainRoutePath()` — keep name, update to draw selected route + creation mode
- `drawTrainRoute(g2, path, Collection<StationStop>, ...)` — change `TrainRoute.StationStop` → `Route.StationStop`
- `drawTrainRoute(g2, path, Set<Integer>, ...)` — no change
- `testStartTrainRouteSimulation(TrainRoute)` → `testStartSimulation(Route)`
- `testStopTrainRouteSimulation()` → `testStopSimulation()`

**Context menu:**
- `if (trainRouteMode && ...)` → keep (creation mode)
- `if ((trainRouteMode || (editMode && selectedTrainRoute != null)) ...)` → `if ((trainRouteMode || (editMode && selectedRoute != null)) ...)`
- `selectedTrainRoute.removeStop(idx)` → `selectedRoute.removeStop(idx)`
- `selectedTrainRoute.addStop(idx, 5000)` → `selectedRoute.addStop(idx, 5000)`

**Drawing:**
- `if (selectedTrainRoute != null && ...)` → `if (selectedRoute != null && ...)`

### Step 9: Update `SwitchboardApp.java`

- `import TrainRouteListPanel` → `import RouteListPanel`
- `private TrainRouteListPanel trainRouteListPanel` → `private RouteListPanel routeListPanel`
- Constructor: `routeListPanel = new RouteListPanel(routeModel, messages)`
  - Wait — `RouteModel` is on `SwitchboardPanel`, not `RailwayModel`. Need to pass `switchboardPanel.getRouteModel()` or add a `RouteModel` to `RailwayModel`.
  - **Decision:** `RouteModel` stays on `SwitchboardPanel` (signal-level), but we need a separate `RouteModel` for train routes OR consolidate into one model. Since `RouteModel` already stores Routes keyed by ID, we can store ALL routes there. The list panel can filter to show only named routes (train routes) or all routes.
  - `routeListPanel = new RouteListPanel(switchboardPanel.getRouteModel(), messages)`
- `trainRouteListPanel.setSelectionListener(...)` → `routeListPanel.setSelectionListener(...)`
- `desktop.split(trainListPanel, trainRouteListPanel, ...)` → `desktop.split(trainListPanel, routeListPanel, ...)`
- `saveTrainRouteFromPanel()`: change `new TrainRoute(id, name, path)` → `new Route(id, name, path)`
  - `model.getTrainRouteListModel().addTrainRoute(route)` → `switchboardPanel.getRouteModel().addRoute(route)` or add to RouteModel

### Step 10: Update persistence JSON test data

The 3 JSON test files have `"trainRoutes"` array. This needs to be merged into the `"routes"` array. Each train route entry gets `sourceElementId: null`, `targetElementId: null`, plus the existing `name` and `stops` fields.

### Step 11: Update test `TrainRouteRunUiTest.java`

- Rename to `RouteRunUiTest.java`
- Replace `TrainRoute` refs with `Route`
- Replace `model.getTrainRouteListModel()` with `switchboardPanel.getRouteModel()`
- Update assertions

### Step 12: Update i18n keys

Component messages:
- `trainRouteList.title` → `routeList.title` (or keep as-is)
- `trainRouteList.tooltip` → `routeList.tooltip`
- `context.addStationStop` / `context.removeStationStop` — keep as-is

App messages:
- `menu.edit.defineTrainRoute` → `menu.edit.defineRoute` (or keep)
- `dialog.trainRoute.title` → `dialog.route.title`
- `dialog.trainRoute.name` → `dialog.route.name`

---

## Design Decisions

### Route storage: single RouteModel or separate?

**Option A: Single RouteModel** (Recommended)
- All routes (signal-to-signal + train routes) in one `RouteModel`
- `RouteModel.getRoutes()` returns all
- List panel filters to show only named routes (`route.getName() != null`)
- Signal-to-signal routes have `name == null`

**Option B: Separate models**
- Keep `RouteModel` for signal-to-signal
- Create `TrainRouteModel` for train routes
- More separation but more wiring

**I recommend Option A** — simpler, one model, one persistence path.

### Route ID for train routes

Train routes created via "Define Train Route" use `"TR-001"`, `"TR-002"` etc.
Signal-to-signal routes use `"sourceElementId-targetElementId"`.

No conflict since they use different ID schemes.

### How to distinguish route types

- `route.getName() != null` → train/named route (show in list, supports stops)
- `route.getName() == null` → signal-to-signal route (has source/target, supports alternatives)
- `route.getSourceElementId() != null` → has signal association

---

## Execution Order

1. Step 1: Modify Route.java (add name, stops, StationStop)
2. Step 2: Delete TrainRoute.java, TrainRouteListModel.java
3. Step 3: Rename TrainRouteSimulation → RouteSimulation
4. Step 4: Rename TrainRouteListPanel → RouteListPanel
5. Step 5: Update LayoutData (merge TrainRouteData into RouteData)
6. Step 6: Update LayoutPersistence
7. Step 7: Update RailwayModel (remove TrainRouteListModel)
8. Step 8: Update SwitchboardPanel (bulk ref replacement)
9. Step 9: Update SwitchboardApp
10. Step 10: Update JSON test data
11. Step 11: Update test
12. Step 12: Update i18n
13. Build and run tests
