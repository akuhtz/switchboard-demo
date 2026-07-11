# Model Railway Switchboard – Component Specification

## Overview

A Java Swing-based switchboard component for controlling and visualising a model railway layout.
The component manages turnouts (points) and signals, responds to user interaction, and can be
extended for DCC hardware integration.

The switchboard is rendered as a 60x30 tile grid (each tile 32x32 px). Every tile displays an
SVG icon loaded via [jsvg](https://github.com/weisJ/jsvg) (`com.github.weisj:jsvg:2.1.0`).

---

## Architecture

### Design Patterns

| Pattern       | Purpose                                                                     |
|---------------|-----------------------------------------------------------------------------|
| **MVC**       | Separates layout state (Model), rendering (View), and user actions (Controller) |
| **Observer**  | Propagates model state changes to the UI via `PropertyChangeSupport`              |
| **Command**   | Encapsulates actions (toggle turnout) with undo/redo support                |
| **State**     | Models per-element aspects (turnout: straight/diverted, signal: aspect 0-7) |
| **Composite** | Composes tiles (plain, turnout, signal) into a unified grid panel           |

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

### **Aspects**

Aspects define the discrete states an element can be in. Each aspect has an integer ordinal
(starting at 0) and can be looked up by name or by ordinal.

#### `TurnoutAspect`
- `STRAIGHT(0)`, `DIVERTED_LEFT(1)`, `DIVERTED_RIGHT(2)`
- 2-way turnouts use `STRAIGHT` and `DIVERTED_LEFT`
- 3-way turnouts use `STRAIGHT`, `DIVERTED_LEFT`, and `DIVERTED_RIGHT`
- Lookup: `TurnoutAspect.valueOf("STRAIGHT")`, `TurnoutAspect.of(0)`

#### `SignalAspect`
- 8 values: `ASPECT_0(0)` through `ASPECT_7(7)`
- A specific signal uses a subset (e.g. 2-aspect: `ASPECT_0`, `ASPECT_1`)
- Lookup: `SignalAspect.valueOf("ASPECT_3")`, `SignalAspect.of(3)`

---

### `RailwayModel`
- Central model class holding the state of all railway elements.
- Uses `PropertyChangeSupport` (idiomatic Java Observer).
- Manages:
  - **Turnouts** (`Map<String, TurnoutAspect>` + aspect count per turnout)
  - **Signals** (`Map<String, SignalAspect>` + aspect count per signal)
- Fires `PropertyChangeEvent` on every state mutation.
- Methods:
  - `addTurnout(String id)` / `addTurnout(String id, int aspectCount)`
  - `toggleTurnout(String id)` / `setTurnoutAspect(String id, TurnoutAspect)`
  - `getTurnoutAspect(String id)` / `getTurnoutAspectCount(String id)`
  - `addSignal(String id)` / `addSignal(String id, int aspectCount)`
  - `toggleSignal(String id)` / `setSignal(String id, SignalAspect)`
  - `getSignalAspect(String id)` / `getSignalAspectCount(String id)`
  - `clear()` — removes all elements
  - `addPropertyChangeListener(PropertyChangeListener l)`

---

### `Tile` (base class)
- Represents a single grid cell at `(col, row)`.
- Carries an optional `elementId` linking to a model element, and an SVG resource path.
- Subclasses: `TurnoutTile`, `SignalTile`.

### `TurnoutTile extends Tile`
- Maps each `TurnoutAspect` to an SVG resource path.
- 2-way constructor: `TurnoutTile(col, row, id, svgStraight, svgDivertedLeft)`
- 3-way constructor: `TurnoutTile(col, row, id, svgStraight, svgDivertedLeft, svgDivertedRight)`
- `getSvgForAspect(TurnoutAspect)` — returns the matching SVG path.

### `SignalTile extends Tile`
- Maps each `SignalAspect` to an SVG resource path.
- 2-aspect constructor: `SignalTile(col, row, id, svgAspect0, svgAspect1)`
- 3-aspect constructor: `SignalTile(col, row, id, svgAspect0, svgAspect1, svgAspect2)`
- General constructor: `SignalTile(col, row, id, Map<SignalAspect, String>)`
- `getSvgForAspect(SignalAspect)` — returns the matching SVG path.

---

### `SwitchboardPanel` (View)
- Extends `JPanel`, implements `PropertyChangeListener`.
- Default grid: 60 columns x 30 rows, 32 px per tile (1920×960 px total).
- Registers as observer on the `RailwayModel`.
- **Rendering** (`paintComponent`):
  - Uses `Graphics2D` with antialiasing and bilinear interpolation.
  - Draws tiles first, then grid lines on top so the grid is always visible.
  - Each tile renders its SVG icon via `SVGDocument.render(Component, Graphics2D, ViewBox)`.
- **Interaction**:
  - `MouseListener` converts pixel coordinates to grid `(col, row)` and delegates to `onTileClicked`.
  - Turnouts are toggled via `ToggleTurnoutCommand` (undoable).
  - Signals are toggled via `model.toggleSignal(id)`.
- **Thread safety**:
  - All repaints triggered via `SwingUtilities.invokeLater`.
- Public API:
  - `setTile(Tile tile)` — places a tile at its grid position.
  - `getTile(int col, int row)` — retrieves a tile.
  - `clearTiles()` — removes all tiles.
  - `getModel()` — returns the `RailwayModel`.
  - `undoLast()` — undoes the last turnout command.

---

### `Command` (Interface)
- `void execute()`
- `void undo()`

### `ToggleTurnoutCommand`
- Implements `Command`.
- Encapsulates a turnout toggle operation on `RailwayModel`.
- `execute()` and `undo()` both call `model.toggleTurnout(id)` (toggle is self-inverse).

---

### `SvgIconLoader`
- Utility that loads and caches `SVGDocument` instances from classpath resources using jsvg.
- Thread-safe `ConcurrentHashMap` cache.

---

### `LayoutPersistence`
- Serializes the full switchboard state (tiles + model) to/from JSON using Jackson 3.
- `capture(SwitchboardPanel)` / `save(SwitchboardPanel, Path)` — write state.
- `load(SwitchboardPanel, Path)` / `apply(SwitchboardPanel, LayoutData)` — read state.

### `SettingsManager`
- Manages `settings.json` at the project root, separate from the layout file.
- Stores the `lastLayoutFile` path referencing the user's chosen layout `.json`.
- Loaded on startup; auto-saves on every change.

### `LayoutData` / `SettingsData`
- POJOs for Jackson serialization.
- `LayoutData` holds grid dimensions, tile list (with type discriminator), and model aspect state.
- `SettingsData` holds application-level settings (extensible).

---

## Application (`SwitchboardApp`)

| Menu Item | Shortcut | Action |
|-----------|----------|--------|
| File > Load... | `Ctrl+L` | JFileChooser to load a `.json` layout |
| File > Save | `Ctrl+S` | Save to current file, or Save As if none |
| File > Save As... | `Ctrl+Shift+S` | JFileChooser to save to a new location |
| File > Exit | — | Exit application |

On startup:
1. Load `settings.json` from project root
2. Read the `lastLayoutFile` path → load layout from that file (if it exists)
3. Fall back to the hardcoded default layout if no settings or file is found

---

## SVG Icons (`src/main/resources/icons/`)

| File | Description |
|------|-------------|
| `empty.svg` | Dark background only |
| `turnout_straight.svg` | Cyan horizontal line (straight route) |
| `turnout_diverted_left.svg` | Cyan line splitting to upper-right orange branch |
| `turnout_diverted_right.svg` | Cyan line splitting to lower-right orange branch |
| `signal_red.svg` | Red filled circle |
| `signal_yellow.svg` | Yellow filled circle |
| `signal_green.svg` | Green filled circle |

All icons are 32×32 viewBox with a dark background (#2d2d32).

---

## Build & Run

```
mvn compile exec:java -Dexec.mainClass=com.bidib.switchboard.SwitchboardApp
```
