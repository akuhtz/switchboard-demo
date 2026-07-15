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

### `RailwayModel`
- Single unified model holding all elements and occupancies.
- Uses `PropertyChangeSupport` (idiomatic Java Observer).
- **State**: `Map<String, Element> elements` — elementId → Element object.
  `Map<String, Occupancy> occupancies` — `nodeId:portId` → Occupancy object.
- Aspect counts live on the tile (`ElementTile.getAspectCount()`) rather than in the model.
- Fires `PropertyChangeEvent` on every state mutation.
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
- Methods:
  - `addRoute(Route)` — adds a route.
  - `removeRoute(String id)` — removes a route by ID.
  - `getRoute(String id)` / `getRoutes()` — access routes.
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
  - Context menu shows "Clear route ({id})" on tiles belonging to a route.
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

---

### `Command` (Interface)
- `void execute()` / `void undo()`

### `CycleElementCommand`
- Implements `Command`.
- Computes `(oldAspect + 1) % aspectCount` in constructor, stores both old and new values.
- `execute()` calls `model.setElementAspect(id, newAspect)`.
- `undo()` calls `model.setElementAspect(id, oldAspect)`.
- Logs execute/undo via SLF4J.

---

### `SvgIconLoader`
- Utility that loads and caches `SVGDocument` instances from classpath resources using jsvg.
- Thread-safe `ConcurrentHashMap` cache.

---

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
| File | Exit | — | Exit application |
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

20 tests across two test classes:

### `SwitchboardAppTest` (7 tests)
| Test | Description |
|------|-------------|
| `frameTitleContainsSwitchboard` | Frame title includes "Model Railway Switchboard" |
| `fileMenuContainsLoadSaveSaveAsSettingsAndExit` | File menu items visible |
| `editMenuContainsEditModeLoadDefaultAndOccupancies` | Edit menu items visible |
| `toolbarContainsEditModeToggle` | Edit Mode toggle button visible |
| `settingsMenuHasLightAndDarkItems` | Light/Dark Look and Feel items visible |
| `clearSelectionItemVisibleOnlyInEditMode` | Clear selection only appears in edit mode |
| `occupancyPersistenceRoundtrip` | Occupancies and element assignments survive `capture()`/`apply()` round-trip |

### `RouteFindingTest` (13 tests)
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

Uses `switchboard3.json` test layout (2 turnouts, curves, diagonals, signals on a 60×30 grid).

---

## AI Agent Guidelines

See `AGENTS.md` in the project root for rules governing AI-generated contributions,
including attribution and co-authorship requirements.

---

## Build & Run

```
mvn compile exec:java -Dexec.mainClass=com.bidib.switchboard.SwitchboardApp
mvn test
```
