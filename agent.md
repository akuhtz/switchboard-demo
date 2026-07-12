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

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| `com.github.weisj:jsvg` | 2.1.0 | SVG rendering |
| `tools.jackson.core:jackson-databind` | 3.2.0 | JSON serialization (via jackson-bom) |
| `org.slf4j:slf4j-api` | 2.0.18 | Logging facade |
| `ch.qos.logback:logback-classic` | 1.5.37 | Logging implementation |

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
| `SIGNAL` | `S` | yes | 2 (red, green) | yes |
| `STRAIGHT` | `P` | yes | 1 | no |
| `CURVE_LEFT` | `CL` | yes | 1 | no |
| `CURVE_RIGHT` | `CR` | yes | 1 | no |

Element IDs follow the pattern `{prefix}-{number}`, e.g. `"TL-001"`, `"TR-001"`, `"T3-001"`.
IDs are generated uniquely per prefix by scanning existing model elements for the highest suffix.

---

### `RailwayModel`
- Single unified model holding all elements.
- Uses `PropertyChangeSupport` (idiomatic Java Observer).
- **State**: `Map<String, Integer> elements` — elementId → current aspect ordinal.
- Aspect counts live on the tile (`ElementTile.getAspectCount()`) rather than in the model.
- Fires `PropertyChangeEvent` on every state mutation.
- Methods:
  - `addElement(String id)` — adds with aspect 0
  - `setElementAspect(String id, int aspect)`
  - `getElementAspect(String id)`
  - `getElementAspects()` — unmodifiable snapshot
  - `clear()` / `removeElement(String id)` / `containsElement(String id)`
  - `addPropertyChangeListener` / `removePropertyChangeListener`

---

### `Tile` (base class)
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
  - **Edit**: left-click selects tiles (cyan border), Ctrl+R rotates selected tile 90°, right-click context menu to place/clear tiles. No aspect cycling.
- **Rendering** (`paintComponent`):
  - Uses `Graphics2D` with antialiasing and bilinear interpolation.
  - Draws tiles first, then grid lines, then selection border (edit mode only).
  - Each tile renders its SVG icon via `SVGDocument.render(Component, Graphics2D, ViewBox)`.
  - Rotation is applied via `Graphics2D.rotate()` around the tile center.
  - For `ElementTile` tiles, resolves the SVG path from the model's current aspect.
- **Interaction**:
  - Left-click: selects position + cycles aspect (normal) or selects only (edit).
  - Right-click: context menu with visible ElementTypes + Clear (occupied only).
  - Ctrl+R: rotates selected tile 90° (edit mode only).
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

---

### `SvgIconLoader`
- Utility that loads and caches `SVGDocument` instances from classpath resources using jsvg.
- Thread-safe `ConcurrentHashMap` cache.

---

### `LayoutPersistence`
- Serializes the full switchboard state (tiles + model) to JSON using Jackson 3.
- `capture(SwitchboardPanel)` / `save(SwitchboardPanel, Path)` — write state.
- `load(SwitchboardPanel, Path)` / `apply(SwitchboardPanel, LayoutData)` — read state.
- Tile type string format: `{prefix}{count}`, e.g. `"TL2"`, `"TR2"`, `"T32"`, `"S2"`, `"P1"`, `"CL1"`, `"CR1"`.
- Type is matched by iterating `ElementType.values()` and testing `typeStr.startsWith(prefix)`.

### `SettingsManager`
- Manages `settings.json` at the project root, separate from the layout file.
- Stores the `lastLayoutFile` path referencing the user's chosen layout `.json`.
- Loaded on startup; auto-saves on every change.

### `LayoutData` / `SettingsData`
- POJOs for Jackson serialization.
- `LayoutData` holds grid dimensions, tile list (with type, svgPaths, rotation), and model aspect state.
- `ModelStateData` uses a flat `Map<String, Integer>` for element aspects only (counts derive from tiles).
- `SettingsData` holds application-level settings (extensible).

---

## Application (`SwitchboardApp`)

### Menu

| Menu | Item | Shortcut | Action |
|------|------|----------|--------|
| File | Load... | `Ctrl+L` | JFileChooser to load a `.json` layout |
| File | Save | `Ctrl+S` | Save to current file, or Save As if none |
| File | Save As... | `Ctrl+Shift+S` | JFileChooser to save to a new location |
| File | Exit | — | Exit application |
| Edit | Edit Mode | `Ctrl+E` | Toggle normal/edit mode |

### Toolbar
- `[Edit Mode]` toggle button synced with the Edit menu item.

### On startup
1. Load `settings.json` from project root
2. Read the `lastLayoutFile` path → load layout from that file (if it exists)
3. Fall back to the hardcoded default layout if no settings or file is found

### Default layout
- `"TL-001"` (2-way left turnout at 2,3)
- `"TR-001"` (2-way right turnout at 3,3)
- `"T3-001"` (3-way turnout at 4,3)
- `"S-001"` (2-aspect signal at 10,3)
- `"S-002"` (3-aspect signal at 11,3)
- `"P-001"`..`"P-005"` (straight track at row 0, cols 0-4)

---

## SVG Icons (`src/main/resources/icons/`)

| File | Description |
|------|-------------|
| `empty.svg` | Dark background only |
| `straight.svg` | Full cyan horizontal line (plain track) |
| `turnout_straight.svg` | Cyan left half + orange right half (turnout set straight) |
| `turnout_diverted_left.svg` | Cyan left half + orange diagonal to top-right corner |
| `turnout_diverted_right.svg` | Cyan left half + orange diagonal to bottom-right corner |
| `curve_left.svg` | Cyan left half + cyan diagonal to top-right corner |
| `curve_right.svg` | Cyan left half + cyan diagonal to bottom-right corner |
| `signal_red.svg` | Red filled circle |
| `signal_yellow.svg` | Yellow filled circle |
| `signal_green.svg` | Green filled circle |

All icons are 32×32 viewBox with a dark background (#2d2d32).

---

## Build & Run

```
mvn compile exec:java -Dexec.mainClass=com.bidib.switchboard.SwitchboardApp
```
