# Model Railway Switchboard – Component Specification

## Overview

A Java Swing-based switchboard component for controlling and visualising a model railway layout.
The component manages turnouts (points), signals, curves, and straight track via a unified
element model and responds to user interaction.

The switchboard is rendered as a 60x30 tile grid (each tile 32x32 px). Every tile displays an
SVG icon loaded via [jsvg](https://github.com/weisJ/jsvg) (`com.github.weisj:jsvg:2.1.0`).

---

## Architecture

### Design Patterns

| Pattern       | Purpose                                                                     |
|---------------|-----------------------------------------------------------------------------|
| **MVC**       | Separates layout state (Model), rendering (View), and user actions (Controller) |
| **Observer**  | Propagates model state changes to the UI via `PropertyChangeSupport`              |
| **Command**   | Encapsulates actions (cycle element) with undo/redo support                |
| **State**     | Models per-element aspects as integer ordinals (0..N-1)                    |
| **Composite** | Composes all tile types into a unified grid panel                          |

---

## Requirements

- **Java 21+** required. The project uses Java 21 features including
  `SequencedCollection`, `indexOfFirst`/`indexOfLast`, `Math.clamp`, and
  `String.repeat`/`String.stripIndent`.

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| `com.github.weisj:jsvg` | 2.1.0 | SVG rendering |
| `tools.jackson.core:jackson-databind` | 3.2.0 | JSON serialization (via jackson-bom) |
| `com.formdev:flatlaf` | 3.7.2 | Look and Feel (Light/Dark) |
| `org.slf4j:slf4j-api` | 2.0.18 | Logging facade |
| `ch.qos.logback:logback-classic` | 1.5.37 | Logging implementation |
| `tokyo.northside:assertj-swing-junit-jupiter` | 4.0.0-beta-3 | GUI testing (test scope) |
| `org.junit.jupiter:junit-jupiter-engine` | 6.0.3 | Test runner (test scope) |

---

## Components

### **Unified Element System**

All railway elements use int ordinals (0..N-1) for their aspect/state. There are no
type-specific enums — just element types distinguished by prefix.

#### `ElementType` (enum)
| Enum | Prefix | Visible | Aspects | Clickable |
|------|--------|---------|---------|-----------|
| `TURNOUT_LEFT` | `TL` | yes | 2 (straight, diverted left) | yes |
| `TURNOUT_RIGHT` | `TR` | yes | 2 (straight, diverted right) | yes |
| `TURNOUT_3WAY` | `T3` | yes | 3 (straight, left, right) | yes |
| `SIGNAL_2` | `S2` | yes | 2 (red, green) | yes |
| `SIGNAL_3` | `S3` | yes | 3 (red, yellow, green) | yes |
| `STRAIGHT` | `P` | yes | 1 | no |
| `CURVE_LEFT` | `CL` | yes | 1 | no |
| `CURVE_RIGHT` | `CR` | yes | 1 | no |
| `DIAGONAL` | `DG` | yes | 1 | no |

Route finding uses `isValidThroughPath(port1, port2, rotation)` which validates that
a train can traverse the tile from an entry port to an exit port. Turnouts only allow
common-heel→frog-end paths (e.g., LEFT↔RIGHT and LEFT↔BOTTOM for TURNOUT_RIGHT),
preventing frog-end→frog-end traversal. DIAGONAL elements allow all combinations
connecting bottom-left corner ports with top-right corner ports.
`hasValidDiagonal(port1, port2, rotation)` checks whether a tile's SVG track path has
an endpoint at the given corner, used for diagonal neighbor connections.

Element IDs follow the pattern `{prefix}-{number}`, e.g. `"TL-001"`, `"S2-001"`, `"P-001"`.
IDs are generated uniquely per prefix by scanning existing model elements for the highest suffix.

---

### `Element`
- Value class for a railway element.
- Fields: `id` (String), `nodeId` (long), `accessoryId` (long), `currentAspect` (int),
  `occupancy` (Occupancy, nullable).
- Constructed with `new Element(id, nodeId, accessoryId)` — aspect starts at 0.
- Properties exposed via getters; `currentAspect` and `occupancy` have setters.
- Extends `com.jgoodies.binding.beans.Model` — `setOccupancy()` fires `"occupancy"` property change.

### `RailwayModel`
- Single unified model holding all elements and occupancies.
- Uses `PropertyChangeSupport` (idiomatic Java Observer).
- **State**: `Map<String, Element> elements` — elementId → Element object.
  `Map<String, Occupancy> occupancies` — `nodeId:portId` → Occupancy object.
- Aspect counts live on the tile (`ElementTile.getAspectCount()`) rather than in the model.
- Fires `PropertyChangeEvent` on every state mutation.
- `addElement()` bridges the Element's property changes to the model's `PropertyChangeSupport`.
- `addOccupancy()` bridges the Occupancy's property changes similarly.
- Methods:
  - `addElement(Element element)`
  - `setElementAspect(String id, int aspect)`
  - `getElementAspect(String id)` / `getElement(String id)`
  - `getElements()` — unmodifiable snapshot `Map<String, Element>`
  - `addOccupancy(Occupancy occupancy)` / `removeOccupancy(long nodeId, int portId)`
  - `getOccupancy(long nodeId, int portId)` / `getOccupancies()` — unmodifiable `Map<String, Occupancy>`
  - `clear()` / `removeElement(String id)` / `containsElement(String id)`
  - `addPropertyChangeListener` / `removePropertyChangeListener`

### `Route`
- Immutable value class for a found route path.
- Fields: `id` (`"{sourceElementId}-{targetElementId}"`), `sourceElementId`, `targetElementId`, `path` (ordered `List<int[]>` of `[col, row]`).
- `containsTile(col, row)` — checks if a grid tile is part of the route.

### `RouteModel`
- Manages multiple simultaneous active routes.
- Uses `PropertyChangeSupport` for change notifications.
- **Alternative routes**: Each route ID can have a list of alternative paths found via BFS
  (short mode: block one primary edge at a time; exhaustive mode: also block edges of found alternatives).
  A `selectedAlternativeIndex` map tracks which alternative is currently previewed (-1 = none).
- Methods:
  - `addRoute(Route)` — adds a route.
  - `addAlternativeRoute(Route)` — adds an alternative for a route ID; sets index to 0 on first addition.
  - `removeRoute(String id)` — removes a route by ID (including its alternatives).
  - `getRoute(String id)` / `getRoutes()` — access routes.
  - `getAlternativeRoutes(String id)` — returns all alternatives for a route ID.
  - `getAlternativeRoute(String id)` — returns the alternative at the selected index, or null.
  - `setSelectedAlternativeIndex(String id, int index)` — sets preview index (-1 = no preview).
  - `clearAlternatives(String id)` — removes all alternatives and the index entry.
  - `swapWithAlternative(String id)` — promotes previewed alternative to primary, clears alternatives.
  - `isTileReserved(col, row, excludeRouteId)` — checks if a tile is used by any route (except the excluded one).
  - `routeIdForTile(col, row)` — returns the route ID using a tile, or null.
  - `clear()` / `size()` / `isEmpty()`
  - `addPropertyChangeListener` / `removePropertyChangeListener`

### `Occupancy`
- Represents a track occupancy sensor with `nodeId` (long), `portId` (int), and `state` (FREE/OCCUPIED).
- Created via static factories `create(nodeId, portId)` and `create(nodeId, portId, state)`.
- Stored in `RailwayModel.occupancies` keyed by `nodeId:portId`.
- Elements can reference an `Occupancy` via `getOccupancy()` / `setOccupancy()`.
- Persisted in `LayoutData.ModelStateData.occupancies` and restored on load.
- Visualised in the "Occupancies..." dialog (Edit menu).
- Extends `com.jgoodies.binding.beans.Model` — fires `"state"` property changes via `firePropertyChange` in `setState()`.

---
- Represents a single grid cell at `(col, row)`.
- Carries an optional `elementId` and a single `svgResource` path.
- Has a `rotation` field (0/90/180/270) applied as a transform during rendering.
- Used for decorative tiles with no elementId.
- Subclass: `ElementTile`.

### `ElementTile extends Tile`
- Unified tile for any railway element (turnout, signal, curve, straight).
- Contains a `List<String> svgPaths` indexed by aspect ordinal.
- Contains an `ElementType` for serialization/creation.
- `getSvgForAspect(int ordinal)` — returns the matching SVG path (falls back to index 0).
- `getAspectCount()` — returns `svgPaths.size()`.

---

### `SwitchboardPanel` (View)
- Extends `JPanel`, implements `PropertyChangeListener`.
- Default grid: 60 columns x 30 rows, 32 px per tile (1920×960 px total).
- Registers as observer on the `RailwayModel`.
- Delegates route finding to `RouterService`.
- **Modes**:
  - **Normal**: left-click cycles aspects on clickable tiles (aspectCount > 1).
  - **Edit**: left-click selects tiles (cyan border), Ctrl+R rotates selected tile 90°, right-click context menu to place/clear tiles. No aspect cycling. Selection clears when edit mode is turned off.
  - **Route Finding**:
  - Ctrl+click source tile, then Ctrl+click target tile.
  - Source marker (green filled oval) appears immediately on first Ctrl+click.
  - BFS finds path using physical port connectivity (orthogonal + diagonal).
  - BFS skips tiles already reserved by existing routes (conflict detection).
  - Diagonal port checks use OR (not AND) on corner ports, enabling symmetric traversal
    through curves and diagonals in both directions.
  - **Through-path validation**: BFS tracks entry port per tile via `entryPorts` map.
    Before adding a neighbor, `canTraverse()` checks `isValidThroughPath(entry, exit, rotation)`
    on the current tile. Turnouts block frog-end→frog-end (backwards) traversal.
  - Each connection validates BOTH sender and receiver ports (bidirectional).
  - Diagonal connections require `hasValidDiagonal()` on the sender corner.
  - Found routes are stored in a `RouteModel` supporting multiple simultaneous routes.
    Each route has an ID `{sourceElementId}-{targetElementId}`.
  - Routes render as red polylines (`(255,80,80)`, stroke-width 4) through tile centers,
    with a green filled oval at the source and a blue filled oval at the target.
   - Turnouts on found routes are auto-set via `aspectForRoute(entryPort, exitPort, rotation)`.
   - **Alternative routes**: When a route is created, BFS finds alternative paths by blocking each edge of the primary path one at a time and re-running. By default this finds short alternatives. When "Exhaustive Route Search" is enabled (File > Settings), alternatives are also found by blocking edges of previously found alternatives (k-shortest-paths iteration), up to `MAX_ALTERNATIVES` (10). All alternatives are stored in `RouteModel` as a list keyed by route ID.
      - Right-clicking a route tile shows "Use primary route", "Alternative 1/2/..." (preview), and "Use selected alternative" in the context menu.
     - Previewing an alternative draws it as a dotted green line (`(80,255,80)`); other alternatives as dotted blue lines (`(80,80,255)`).
     - "Use primary route" discards all alternatives and restores normal red rendering.
     - "Use selected alternative" promotes the previewed alternative to primary route and discards all alternatives.
     - Dotted lines are only visible during preview (index >= 0); they disappear after committing to primary or an alternative.
   - Context menu shows "Clear route ({id})" on tiles belonging to a route.
- **Occupancy rendering**: In `paintComponent`, `drawOccupancy()` is called last, after routes. For each tile with an OCCUPIED occupancy, it draws port-based line segments using the element's current aspect: `getActivePorts(el.getCurrentAspect(), tile.getRotation())`. Lines are drawn from tile center to each active port edge. `PORT_BOTTOM` draws to the lower-right corner `(cx + d, cy + d)` to match the physical track path of diverted turnouts. Color: `COLOR_OCCUPIED` = `(255, 80, 80)` with stroke-width 4.
- - `getPhysicalPorts(rotation)` returns all physical port indices for a tile.
   `getActivePorts(aspect, rotation)` returns only the ports active for a given aspect (1 port for straight/curve/diagonal, 2 for turnouts, 4 for crossings).
- **Rendering** (`paintComponent`):
  - Uses `Graphics2D` with antialiasing and bilinear interpolation.
  - Draws tiles first, then grid lines, then selection border (edit mode only).
  - Each tile renders its SVG icon via `SVGDocument.render(Component, Graphics2D, ViewBox)`.
  - Rotation is applied via `Graphics2D.rotate()` around the tile center.
  - For `ElementTile` tiles, resolves the SVG path from the model's current aspect.
- **Interaction**:
  - Left-click: selects position + cycles aspect (normal) or selects only (edit).
   - Right-click: context menu with Info (element data dialog), ElementTypes + Signals submenu,
     Assign Occupancy / Remove Occupancy (edit mode only), Clear route on route tiles.
  - Ctrl+R: rotates selected tile 90° (edit mode only).
  - Edit-mode tooltip shows element ID on hover.
- **Thread safety**:
  - All repaints triggered via `SwingUtilities.invokeLater`.
- Public API:
  - `setTile(Tile tile)` / `getTile(int col, int row)` / `removeTile(int col, int row)`
  - `clearTiles()` / `getModel()` / `undoLast()`
   - `isEditMode()` / `setEditMode(boolean)`
   - `setTileContextHandler(TileContextHandler)` — callback for context menu actions.
   - `testSetRouteAspects(List<int[]>)` — applies aspect-for-port/route logic to a given path (test helper).
- **Undo stack**: `Deque<Command> undoStack` — pushed by route finding, tile creation/clearing, aspect cycling. Accessible via `undoLast()`. Menu item Edit > Undo (Ctrl+Z).

---

### `Command` (Interface)
- `void execute()` / `void undo()`

### `CycleElementCommand`
- Implements `Command`.
- Computes `(oldAspect + 1) % aspectCount` in constructor, stores both old and new values.
- `execute()` calls `model.setElementAspect(id, newAspect)`.
- `undo()` calls `model.setElementAspect(id, oldAspect)`.
- Logs execute/undo via SLF4J.

### `CreateRouteCommand`
- Implements `Command`.
- Captures new route, previous route (if replacing), alternatives, and pre-route aspects.
- `execute()` removes previous route (if any), then adds new route + alternatives + sets old aspects.
- `undo()` removes new route, restores previous route, restores pre-route aspects.
- Handles `newRoute == null` for the case where a route is cleared (BFS failure on re-route).

### `TileCommand`
- Implements `Command`.
- Captures old tile and new tile state at a grid cell.
- `execute()` removes old tile/element, creates new tile/element.
- `undo()` removes new tile/element, restores old tile/element.

---

### `SvgIconLoader`
- Utility that loads and caches `SVGDocument` instances from classpath resources using jsvg.
- Thread-safe `ConcurrentHashMap` cache.

---

### `RouterService`
- Stateless service class encapsulating the route-finding logic.
- Constructed with `Map<String, Tile> tiles`, `int cols`, `int rows`, `RouteModel routeModel`.
- `bfsRoute(startCol, startRow, endCol, endRow)` — BFS-based shortest path using physical port connectivity. Returns `List<int[]>` path or `null`.
- `bfsAlternativeRoutes(startCol, startRow, endCol, endRow, primaryPath, exhaustive)` — finds alternative routes by blocking edges of the primary path (and of found alternatives when `exhaustive=true`). Returns `List<List<int[]>>`.
- `setRouteAspects(path, model)` — sets turnouts on a found route to the correct aspect using `aspectForRoute(entryPort, exitPort, rotation)`.
- `diagonalAwarePort(from, to, isEntry)` — computes which port a diagonal movement enters/exits through.
- Extracted from `SwitchboardPanel` to enable direct testing and reuse.

### `LayoutPersistence`
- Serializes the full switchboard state (tiles + model + occupancies) to JSON using Jackson 3.
- `capture(SwitchboardPanel)` / `save(SwitchboardPanel, Path)` — write state.
- `load(SwitchboardPanel, Path)` / `apply(SwitchboardPanel, LayoutData)` — read state.
- Tile type string format: `{prefix}{count}`, e.g. `"TL2"`, `"T32"`, `"S22"`, `"S32"`, `"P1"`, `"CL1"`, `"CR1"`, `"DG1"`.
- Type is matched by iterating `ElementType.values()` and testing `typeStr.startsWith(prefix)`.
- Occupancies are serialised in `ModelStateData.occupancies` and element→occupancy references via `OccupancyData` nodeId/portId on each `ElementData`.

### `SettingsManager`
- Manages `settings.json` at the project root, separate from the layout file.
- Stores the `lastLayoutFile` path and `lookAndFeel` setting.
- Loaded on startup; auto-saves on every change.

### `LayoutData` / `SettingsData`
- POJOs for Jackson serialization.
- `LayoutData` holds grid dimensions, tile list (with type, svgPaths, rotation), and `ModelStateData`.
- `ModelStateData` holds a `List<ElementData>` (each containing `id`, `nodeId`, `accessoryId`, `aspect`, `occupancyNodeId`, `occupancyPortId`) and a `List<OccupancyData>` (each containing `nodeId`, `portId`, `state`).
- `SettingsData` holds `lastLayoutFile` and `LookAndFeel` (LIGHT/DARK enum).

---

## Application (`SwitchboardApp`)

### Menu

| Menu | Item | Shortcut | Action |
|------|------|----------|--------|
| File | Load... | `Ctrl+L` | JFileChooser to load a `.json` layout |
| File | Save | `Ctrl+S` | Save to current file, or Save As if none |
| File | Save As... | `Ctrl+Shift+S` | JFileChooser to save to a new location |
 | File | Settings > Light Look and Feel | — | Switch to FlatLaf light theme |
| File | Settings > Dark Look and Feel | — | Switch to FlatLaf dark theme |
| File | Settings > Exhaustive Route Search | — | Toggle k-shortest-paths search for more alternative routes |
| File | Exit | — | Exit application |
| Edit | Undo | `Ctrl+Z` | Undo last tile or route operation |
| Edit | Edit Mode | `Ctrl+E` | Toggle normal/edit mode |
| Edit | Load Default Layout | — | Load the built-in default layout |
| Edit | Occupancies... | — | Show dialog with all occupancies sorted by nodeId/portId |

### Toolbar
- `[Edit Mode]` toggle button synced with the Edit menu item.

### On startup
1. Load `settings.json` from project root
2. Apply saved Look and Feel (Light or Dark)
3. Read the `lastLayoutFile` path → load layout from that file (if it exists)
4. Fall back to the hardcoded default layout if no settings or file is found

### Default layout
- `"TL-001"` (2-way left turnout at 2,3)
- `"TR-001"` (2-way right turnout at 3,3)
- `"T3-001"` (3-way turnout at 4,3)
- `"S2-001"` (2-aspect signal at 10,3)
- `"S3-001"` (3-aspect signal at 11,3)
- `"P-001"`..`"P-005"` (straight track at row 0, cols 0-4)

---

## SVG Icons (`src/main/resources/icons/`)

| File | Description |
|------|-------------|
| `empty.svg` | Dark background only |
| `straight.svg` | Full light gray horizontal line |
| `turnout_straight_left.svg` | Straight active (light gray+orange), diverted left gray |
| `turnout_straight_right.svg` | Straight active (light gray+orange), diverted right gray |
| `turnout_diverted_left.svg` | Left diverted active (light gray+orange), straight gray |
| `turnout_diverted_right.svg` | Right diverted active (light gray+orange), straight gray |
| `turnout_3way_straight.svg` | Straight active, both diverted gray |
| `turnout_3way_left.svg` | Left active, straight and right gray |
| `turnout_3way_right.svg` | Right active, straight and left gray |
| `curve_left.svg` | Horizontal to center then diagonal to top-right |
| `curve_right.svg` | Horizontal to center then diagonal to bottom-right |
| `diagonal.svg` | Diagonal from lower-left to upper-right corner |
| `signal_2_red.svg` | SBB signal shape — red active, green dim |
| `signal_2_green.svg` | SBB signal shape — green active, red dim |
| `signal_3_red.svg` | SBB signal shape — red active, yellow+green dim |
| `signal_3_yellow.svg` | SBB signal shape — yellow active, red+green dim |
| `signal_3_green.svg` | SBB signal shape — green active, red+yellow dim |

All icons are 32×32 viewBox with a dark background (#2d2d32). Track lines use light gray `#aaaaaa` for active paths, `#808080` for inactive paths, and `#ffa500` (orange) for the frog-end on turnouts.

### Additional resources
| Path | Description |
|------|-------------|
| `src/main/resources/signals/sbb_l/SBB-L-H01.svg` | Source SBB L signal shape (200x400, rotated for icons) |

---

## Tests

50 tests across six test classes:

### `SwitchboardAppTest` (7 tests)
| Test | Description |
|------|-------------|
| `frameTitleContainsSwitchboard` | Frame title includes "Model Railway Switchboard" |
| `fileMenuContainsLoadSaveSaveAsSettingsAndExit` | File menu items visible |
| `editMenuContainsEditModeLoadDefaultAndOccupancies` | Edit menu items visible |
| `toolbarContainsEditModeToggle` | Edit Mode toggle button visible |
| `settingsMenuHasLightAndDarkAndExhaustiveItems` | Light/Dark Look and Feel + Exhaustive Route Search items visible |
| `clearSelectionItemVisibleOnlyInEditMode` | Clear selection only appears in edit mode |
| `occupancyPersistenceRoundtrip` | Occupancies and element assignments survive `capture()`/`apply()` round-trip |

### `RouteFindingTest` (21 tests)
| Test | Description |
|------|-------------|
| `routeThroughDivertedTurnouts` | (0,0)→(10,1) via TR-003/TR-002 diverted, verifies aspect set |
| `routeFromRow3Col2ToRow5Col10` | (2,3)→(10,5) found |
| `routeFromRow3Col2ToRow4Col10` | (2,3)→(10,4) found |
| `routeFromRow3Col2ToRow0Col10` | (2,3)→(10,0) blocked by turnout through-path constraints |
| `routeFromRow1Col10ToRow3Col2` | (10,1)→(2,3) reverse-direction found |
| `twoNonOverlappingRoutesCoexist` | Two disjoint routes exist simultaneously in `RouteModel` |
| `routeConflictBlocksOverlappingRoute` | BFS skips tiles reserved by existing routes |
| `removeRouteById` | Route removed from model by ID |
| `routeModelClearRemovesAllRoutes` | Clearing `RouteModel` removes all routes |
| `routePersistenceRoundTrip` | Routes survive `capture()`/`apply()` round-trip |
| `routeModelIsTileReserved` | `isTileReserved()` correctness with/without exclusion |
| `routeIdFormat` | Route ID format `"{source}-{target}"` |
| `routeContainsTile` | Route includes source/target, excludes out-of-bounds |
| `alternativeRouteFoundForP015ToP065` | BFS finds 2 alternative routes via T3-001/T3-002 diagonals |
| `alternativeRouteFoundForP015ToTL004` | Exhaustive BFS finds 4 alternatives via T3 diagonals + row-11 corridor |
| `undoRouteCreation` | Route removed from model after undo |
| `undoRouteReplaceRestoresPreviousRoute` | Original route restored after undo of replacement |
| `undoRouteClearRestoresPreviousRoute` | Original route restored after undo of BFS-failed re-route |
| `undoTileCreationOnEmptyCell` | Empty cell and element removed from model after undo |
| `undoTileReplaceRestoresOriginalTile` | Original tile and element restored after undo |
| `occupiedTileOnRouteIsDetected` | Tile on a route detected as occupied when its occupancy is set to OCCUPIED |

### `RouterServiceTest` (11 tests)
| Test | Description |
|------|-------------|
| `bfsRouteWithStartOutsideGrid` | Start column or row out of bounds returns null |
| `bfsRouteWithEndOutsideGrid` | End column or row out of bounds returns null |
| `bfsRouteWithBothOutsideGrid` | Both start and end out of bounds returns null |
| `bfsRouteWithNullStartTile` | Tile not present at start coordinates returns null |
| `bfsRouteWithNullEndTile` | Tile not present at end coordinates returns null |
| `bfsRouteFindsValidPath` | BFS finds path from (2,3) to (10,5) on default layout |
| `bfsAlternativeRoutesReturnsAlternatives` | BFS finds 2 alternative paths from (2,3) to (24,6) |
| `bfsAlternativeRoutesExhaustive` | Exhaustive BFS finds 4 alternative paths from (2,3) to (7,11) |
| `diagonalAwarePort` | Correct port mapping for 8-direction neighbor offsets |
| `bfsRouteReturnsNullWhenBlocked` | BFS returns null when no path exists between valid tiles |
| `diagonalConnectsThroughDiagonalTiles` | Diagonal tiles connect via corner ports in both directions |

### `DebugTest` (1 test)
| Test | Description |
|------|-------------|
| `debugP015toTL004` | Convenience test with `System.out` output for manual debugging of route finding |

### `RouteFindingUiTest` (6 tests)
| Test | Description |
|------|-------------|
| `undoRouteCreationViaUI` | Route removed after undo via Edit > Undo menu |
| `undoRouteReplaceViaUI` | Original route restored after undo of UI replacement |
| `undoRouteClearViaUI` | Original route restored after undo of BFS-failed UI re-route |
| `undoTileCreationOnEmptyCellViaUI` | Empty cell restored after undo via Edit > Undo menu |
| `undoTileReplaceViaUI` | Original tile restored after undo of UI tile replacement |
| `occupiedRouteTilesDetectedViaUI` | Occupied route tiles show occupancy color via `drawOccupancy` |

### `OccupancyUiTest` (4 tests)
| Test | Description |
|------|-------------|
| `occupancyAdvancesAlongRoute` | Timer-driven occupancy animation along a route path, verifying sliding-window pattern |
| `routeFromTL003ToTR002` | Route found from TL-003 to TR-002 with correct source/target element IDs, TL-003 aspect 1 (diverted) |
| `routeFromTL003ToTR002Straight` | Primary route TL-003→P-001 along row 0, TL-003 aspect 0 (through), alternatives cleared |
| `alternativeRouteTL003ToP001` | Alternative route TL-003→P-001 via DG-003/CL-005/row-1 corridor, verified 23-tile path, TL-003 aspect 1 (diverted), TR-003 aspect 1 (diverted) |

Timer-driven tests use a `Semaphore` to synchronise the test thread with the Swing `Timer` tick,
replacing brittle `Thread.sleep()` delays that could miss steps due to timer coalescing.
`maven-surefire-plugin` is configured with `--add-opens java.base/java.util=ALL-UNNAMED`
to prevent `InaccessibleObjectException` from AssertJ Swing's `ProtectingTimerTask`.

Uses `switchboard3.json`, `switchboard4.json`, and `switchboard5.json` test layouts. All 50 tests pass.

---

## AI Agent Guidelines

See `AGENTS.md` in the project root for rules governing AI-generated contributions,
including attribution and co-authorship requirements.

---

## Build & Run

```
mvn compile exec:java -Dexec.mainClass=org.bidib.switchboard.SwitchboardApp
mvn test
```
