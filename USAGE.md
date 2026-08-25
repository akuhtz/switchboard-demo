# Usage Guide

## 1. Install the application

Download the latest Windows installer (MSI):

[![Installer for Windows](https://img.shields.io/badge/Download-Windows%20Installer-blue)](https://www.fichtelbahn.de/files/wizard-fw/switchboard-demo/getLatestLink.php?type=msi&version=1.0)

The installer bundles a JRE — no separate Java installation required. After installation, launch **Switchboard Demo** from the Start menu or desktop shortcut.

## 2. Reset to a blank grid

- Click the **wrench** toolbar button or press **Ctrl+E** to enter edit mode.
- From the menu: **File → Load...** and select an empty JSON layout (or start fresh by removing all tiles manually via **Clear** in the context menu).
- Alternatively, delete `switchboard-demo-app/settings.json` if present, then delete the last loaded layout file — the app will start with the hardcoded default. You can then clear tiles one by one.

## 3. Add tiles

- Right-click any cell → choose a tile type from the list (e.g. `P (STRAIGHT)`, `TR (TURNOUT_RIGHT)`, `TL (TURNOUT_LEFT)`, `SM3 (SIGNAL_M3)`, `SV (SIGNAL_V)`, `SM (SIGNAL_COMBINED)`).
- The tile appears immediately.
- **Rotate**: select the tile with a left-click (cyan border), then press **Ctrl+R** to rotate 90°.

The following tile types are available:

| Type | Prefix | Description       | Rotations |
|------|--------|-------------------|-----------|
| STRAIGHT       | P  | Straight track               | 0 / 90     |
| CURVE_LEFT     | CL | 90° curve to the left        | 0 / 90 / 180 / 270 |
| CURVE_RIGHT    | CR | 90° curve to the right       | 0 / 90 / 180 / 270 |
| DIAGONAL       | DG | Diagonal track (↗)           | 0 / 90 / 180 / 270 |
| DIAGONAL_TURNOUT_RIGHT | DTR | Diagonal turnout (2 aspects): straight ↗ or diverted to the right | 0 / 90 / 180 / 270 |
| DIAGONAL_TURNOUT_LEFT | DTL | Diagonal turnout (2 aspects): straight ↘ or diverted to the left | 0 / 90 / 180 / 270 |
| TURNOUT_LEFT   | TL | Left turnout (2 aspects)     | 0 / 90 / 180 / 270 |
| TURNOUT_RIGHT  | TR | Right turnout (2 aspects)    | 0 / 90 / 180 / 270 |
| TURNOUT_3WAY   | T3 | Three-way turnout (3 aspects)| 0 / 90 / 180 / 270 |
| SIGNAL_M3       | SM3 | 3-aspect signal (red/green/orange) | 0 / 90 / 180 / 270 |
| SIGNAL_V       | SV | Distant signal / Vorsignal (orange/green/orange+green/aspect 3) | 0 / 90 / 180 / 270 |
| SIGNAL_COMBINED | SM | Combined signal (main head + distant plate on one mast) | 0 / 90 / 180 / 270 |
| BUMPER         | BS | Bumper stop (dead end)             | 0 / 90 / 180 / 270 |
| BLOCK_MARKER   | BM | Block marker (straight, shows block name) | 0 / 90 |

## 4. Select and move tiles

In **edit mode** (Ctrl+E), you can select and move tiles:

- **Single tile**: left-click a tile to select it (cyan border).
- **Region selection**: click and drag to draw a selection rectangle over multiple tiles. All tiles within the rectangle are selected (translucent cyan highlight).
- **Move with arrow keys**: press **Up** / **Down** / **Left** / **Right** to move all selected tiles by one cell. Rotation and all tile properties are preserved. Routes passing through moved tiles are updated accordingly.
- **Undo**: press **Ctrl+Z** to undo the last move.
- **Clear selection**: press **Escape**, click elsewhere, or right-click → **Clear Selection**.

## 5. Click elements to cycle aspects

In **normal mode** (Ctrl+E to toggle):
- Click a turnout or signal to cycle its aspect (straight ↔ diverted, red ↔ green, orange ↔ green ↔ orange+green ↔ aspect 3 for the distant signal, etc.).
- Clicking a **main signal** also switches every **linked distant signal** to the matching preview aspect (see [section 10](#10-link-a-distant-signal-to-a-main-signal)). A linked distant signal itself cannot be clicked to change its aspect.
- A **combined signal** behaves like a main signal: clicking cycles its own head aspect (red/green/orange). Its distant plate is controlled by the linked main signal and cannot be clicked.
- Turnouts auto-switch to the correct aspect when a route is found.

## 6. Create a route

There is a single unified way to define routes: **route creation mode**.

1. Enter **edit mode** (**Ctrl+E**), then enable **Edit → Define Train Route** (**Ctrl+T**).
2. **Click** a source tile — a green source marker appears.
3. **Click** a target tile — the shortest path is found via BFS and appended to the route path.
   Turnouts along the segment are automatically set to the correct aspect.
4. Repeat steps 2–3 to build multi-segment paths; junction tiles are merged automatically.
5. Optionally right-click any tile of the collected path → **Add Station Stop** (5s dwell) or
   **Remove Station Stop**.
6. Disable **Define Train Route** — a dialog asks for the **route name** (mandatory).
   The route is saved as a named train route (`TR-001`, `TR-002`, ...) and auto-selected in the Routes list.
7. Turning off edit mode while route creation is active cancels the route creation.

### Segment alternatives during creation

When BFS finds alternative paths for a segment, right-click anywhere to choose:

- **Use primary route** — append the shortest segment.
- **Alternative 1 / Alternative 2 / ...** — append an alternative segment instead (shown in its own color).

### Alternative routes on saved routes

When a route is created, BFS also finds alternative paths for the whole route by blocking each edge of the primary path. A white **"+"** badge appears next to the source and target markers of the selected route when alternatives are available. Right-click any tile of the selected route:

- **Alternative 1 / Alternative 2 / ...** — preview the alternative. Each menu item shows a colored circle icon matching the route color.
- **Use primary route** — discard all alternatives.
- **Use selected alternative** — promote the previewed alternative to the primary route.
- **Exhaustive Route Search**: Enable in **File → Settings → Exhaustive Route Search**. When active, the BFS also blocks edges from found alternatives (k-shortest-paths iteration), finding more distinct routes. The setting is persisted in `switchboard-demo-app/settings.json`.

Routes are shown on the switchboard only while selected in the Routes list.

### Manage routes

- **Clear a single route**: select it in the Routes list, then right-click any tile on the route → **Clear route ({id})**.
- Multiple overlapping routes can coexist — the BFS does not skip tiles reserved by other routes.
- **Legacy layouts**: routes saved as signal-to-signal routes by older versions are automatically converted to named train routes when the layout is loaded (name preserved).

## 7. Route details

The **Route Details** tab appears alongside the **Routes** tab in the left panel. When you select a route in the Routes list, the Route Details panel shows:

- **Route Name** text field (top): editable, must be non-blank and unique across all routes.
- **Alternatives badge** (below the name): shows how many alternative paths are stored for this route and which one is currently selected ("-" = primary).
- **Tree** (center): lists turnouts and main signals along the route path in order. The last tile is always shown, even if it is not a turnout or main signal. Distant signals (SIGNAL_V) are excluded.
- **Save / Cancel / Run** buttons (bottom): Save validates the name (non-blank, unique) and applies it. Cancel reverts to the original name. Run starts the route simulation immediately — it is visible only in **edit mode**.

## 8. Tile direction

Straight and diagonal tiles can have a **direction constraint** (FORWARD, BACKWARD, or BOTH).

- In **edit mode**, right-click a straight or diagonal tile → **Direction** submenu → choose Forward / Backward / Both.
- A small light-gray triangle marker appears at the tile center, pointing in the allowed direction.
- Route finding respects the direction: BFS will not traverse a tile against its direction.
- Default is **Both** (no constraint) — backward-compatible with existing layouts.

## 9. Signal side

Signal tiles can display their body above (Swiss, `_left`) or below (German, `_right`) the track. This applies to all signal types (SIGNAL_M3, SIGNAL_V, SIGNAL_COMBINED).

- The global default is set via **File → Settings → Signal Side** (Swiss/German).
- Per‑tile override: in **edit mode**, right‑click a signal tile → **Signal Side** submenu → choose Left / Right / Default.
- When you change the signal side, the tile image updates immediately.
- The **Tile Info** dialog (left‑click a signal in normal mode) shows the resolved signal side.

## 10. Link a distant signal to a main signal

A distant signal (SIGNAL_V) can be linked to the main signal (SIGNAL_M3 or SIGNAL_COMBINED) it previews. A combined signal's (SIGNAL_COMBINED) distant plate uses the same link. Once linked:

- **Manual mirroring**: In normal mode, clicking the main signal also switches every linked distant signal to the matching preview aspect: red → orange ("Halt erwarten"), green → green ("Frei erwarten"), and for SIGNAL_M3 also orange → orange+green ("Langsamfahrt erwarten"). This works outside simulation runs, too.
- Clicking a **linked distant signal** itself does **not** change its aspect — its aspect is controlled by the linked main signal.
- **Assignment**: In edit mode, right-click the distant signal → **Assign Main Signal** → pick a main signal from the list (or **None**). The nearest main signal straight ahead in the travel direction is preselected and marked "(auto)".
- **Auto-assign on rotation**: Select the distant signal and press **Ctrl+R** to rotate it. The nearest main signal straight ahead in the new travel direction is auto-assigned. If the distant signal was already linked to a different main signal, the old link is replaced and the distant signal switches to the new main signal's current aspect. Both changes are written to the log.
- **Removal**: When you clear a main signal that has linked distant signals, you are asked whether to **Remove linked** (remove the distant signals too), **Keep** the distant signals (the link is removed), or **Cancel**.
- The link is saved with the layout and restored on load.
- The **Tile Info** dialog shows the link (Main signal / Distant signals) and, for combined signals, the current distant plate aspect.

## 11. Define blocks

A **block** is a connected path of tiles that forms a section of track. Blocks are useful for
modelling track occupancy sections.

- In **edit mode** (Ctrl+E), right-click the first tile → **Block → Set Block Start**.
- Right-click the last tile → **Block → Set Block End**. The connected path between the two
  tiles is found automatically and a block is created.
- **No turnouts**: blocks never pass through turnout tiles (TL/TR/T3). If no turnout-free
  connected path exists, no block is created.
- **No overlaps**: every tile belongs to at most one block. Tiles already assigned to another
  block are avoided when finding the path.
- Every block gets a **unique ID** and a default name `blk001`, `blk002`, ... (zero-padded).
- To change the name, right-click any tile of the block → **Block → Rename Block...** and enter
  a new name in the dialog.
- To remove a block, right-click any tile of the block → **Block → Remove Block**.
- Blocks are drawn as a 2px yellow line below the track along the path, with a short vertical
  tick at the outer edge of the start and end tiles. The pending block start is shown as an
  orange square marker.
- Blocks are saved with the layout and restored when it is loaded.

## 12. Block markers

Block markers (`BLOCK_MARKER` / `BM`) are straight-through tiles that display the block name as a centered text label. They help visually identify block boundaries on the switchboard.

- Place a block marker by right-clicking a cell in edit mode → **BM (BLOCK_MARKER)**.
- The marker label shows the block name in yellow (`(255,220,80)`) when the block is not occupied, or red when occupied.
- Block markers can be rotated (0° or 90°) like straight track tiles.
- A block marker can have a **train assigned** to it via drag-and-drop (see [section 13](#13-trains-and-drag-and-drop)). When a train is assigned, the marker displays the train name instead of the block name.

## 13. Trains and drag-and-drop

The application includes a train list panel on the left side of the window, showing all defined trains.

### Adding trains

Trains are defined in a `trains.json` file referenced by the current layout. When you load a layout that references a trains file, the train list panel is populated automatically. To add a new train, edit the `trains.json` file and reload the layout.

Each train has:
- **id** (required): unique identifier, e.g. `"train-1"`
- **name** (required): display name, e.g. `"ICE 701"`
- **address** (optional): DCC/NMRA address as an integer
- **image** (optional): path to an image file

### Assigning trains to blocks

1. In **normal mode**, drag a train from the train list panel and drop it on a **block marker** tile.
2. The block marker then displays the **train name** instead of the block name.
3. Each train can only be assigned to one block at a time — dropping a train on a different block automatically removes it from the previous block.
4. To clear a train assignment, right-click the block marker and choose **Clear Train**. This option is available in both edit and normal mode for block markers with an assigned train.

### Drag-and-drop details

- The **drop cursor** (arrow + plus sign) only appears when hovering over a block marker tile, not over regular track tiles.
- Dragging a train to a non-block-marker tile has no effect.

## 14. Simulate occupancy

After a route is created, you can animate a train moving along it:

- Right-click the **green source circle** of a route → **Simulate occupancy ({id})**.
- The simulation creates occupancy markers on every tile along the route and slides the **OCCUPIED** state from the start to the end, one tile at a time (200ms per step).
- Turnouts along the route are automatically set to the correct position for the simulated path.
- **Signal stops**: When a train reaches a main signal (SIGNAL_M3 or SIGNAL_COMBINED) at aspect 0 (red), it stops and waits. Signals only block trains approaching from the front (the direction the signal faces based on rotation). Trains approaching a signal from behind ignore it.
- **Distant signals (Vorsignal)**: The distant signal never stops the train. It mirrors the aspect of the next main signal ahead in the path, previewing the upcoming aspect: orange "Halt erwarten", green "Frei erwarten", orange+green "Langsamfahrt erwarten". A distant signal linked to a main signal (see [section 10](#10-link-a-distant-signal-to-a-main-signal)) also mirrors the main signal's aspect when you click it manually, outside the simulation.
- **Combined signals**: The main head stops the train like a main signal. During simulation the combined signal's distant plate mirrors the next main signal ahead in the path (`syncCombinedPlate`); outside a simulation it mirrors its linked main signal.
- **Auto-change signal**: Enable via **Edit → Auto-change signal**. When active, a main signal that blocks a train auto-switches to aspect 1 (green) after 2 seconds, allowing the train to resume. Toggling this option immediately affects all running simulations.
- **Multiple simulations**: Each route can have its own independent simulation running concurrently.
- **Stop simulation**: Right-click the source tile of a running simulation → **Stop simulation ({id})** to stop it mid-route.
- **Simulate occupancy** is disabled while that route's simulation is already running.

To reset the simulation:

- Right-click any tile of a route that has OCCUPIED tiles → **Clear simulated occupancy ({id})**.
- Sets all occupancy states along the route back to FREE.
- **Clear simulated occupancy** is disabled while a simulation is in progress.

## 15. Language / Internationalization

The application supports **English** and **German** locales. The UI language is determined at startup: it uses the language saved in **File → Settings → Language** when one is stored, otherwise the system locale. The selected language is persisted so it is applied again on the next start.

- **Context menu** items (Info, Signal Side, Assign Main Signal, Clear, Direction, route actions) use localized strings.
- **Tile Info dialog** labels are localized.
- **Main menu** and **toolbar** use localized strings (File, Edit, Settings, etc.).
- To change the language while running: **File → Settings → Language → English / Deutsch**. The choice is saved and applied immediately (menus, tooltips, and context menus update). If your system locale is German (`de`) and no language is stored, the UI automatically switches to German labels. You can also force the locale by passing `-Duser.language=de` or `-Duser.language=en` on the Java command line.

Translations are maintained in `i18n/messages.properties` (component) and `i18n/app-messages.properties` (demo app), each with a `_de` variant.

## 16. Save & load

- **Ctrl+S** — save to the current file (or open a save dialog if none).
- **Ctrl+L** — load a previously saved `.json` layout.
- On startup, the app remembers the last loaded file and restores it automatically.
- **Recent files**: The **File** menu shows a **Recent** submenu with up to 6 recently opened layouts. Click any entry to load it instantly. The most recent entry appears at the top.
- Settings are stored in `~/switchboard-demo-1/settings.json`.

## 17. Logging

The application writes log output both to the console and to a log file:

- **Windows**: `%USERPROFILE%\Documents\switchboard-demo\switchboard-demo-app.log`
- **Other operating systems**: `<java.io.tmpdir>/switchboard-demo-app.log`

The log directory is created automatically if it does not exist. To use a
different location, start the app with `-Dswitchboard.logfile=/path/to/logfile`.
The log file is useful for troubleshooting startup or layout-loading issues.
