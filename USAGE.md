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

- Right-click any cell → choose a tile type from the list (e.g. `P (STRAIGHT)`, `TR (TURNOUT_RIGHT)`, `TL (TURNOUT_LEFT)`, `S2 (SIGNAL_2)`).
- The tile appears immediately.
- **Rotate**: select the tile with a left-click (cyan border), then press **Ctrl+R** to rotate 90°.

The following tile types are available:

| Type | Prefix | Description       | Rotations |
|------|--------|-------------------|-----------|
| STRAIGHT       | P  | Straight track               | 0 / 90     |
| CURVE_LEFT     | CL | 90° curve to the left        | 0 / 90 / 180 / 270 |
| CURVE_RIGHT    | CR | 90° curve to the right       | 0 / 90 / 180 / 270 |
| DIAGONAL       | DG | Diagonal track (↗)           | 0 / 90 / 180 / 270 |
| TURNOUT_LEFT   | TL | Left turnout (2 aspects)     | 0 / 90 / 180 / 270 |
| TURNOUT_RIGHT  | TR | Right turnout (2 aspects)    | 0 / 90 / 180 / 270 |
| TURNOUT_3WAY   | T3 | Three-way turnout (3 aspects)| 0 / 90 / 180 / 270 |
| SIGNAL_2       | S2 | 2-aspect signal (red/green)  | 0 / 90 / 180 / 270 |
| SIGNAL_3       | S3 | 3-aspect signal (red/yellow/green) | 0 / 90 / 180 / 270 |
| BUMPER         | BS | Bumper stop (dead end)             | 0 / 90 / 180 / 270 |

## 4. Click elements to cycle aspects

In **normal mode** (Ctrl+E to toggle):
- Click a turnout or signal to cycle its aspect (straight ↔ diverted, red ↔ green, etc.).
- Turnouts auto-switch to the correct aspect when a route is found.

## 5. Create a route

- Make sure you are in **normal mode** (Ctrl+E to toggle off edit mode).
- **Ctrl+click** a source tile — a green marker appears.
- **Ctrl+click** a target tile — the shortest path is found and drawn as a blue polyline with green (source) and blue (target) markers.
- Turnouts along the route are automatically set to the correct aspect.
- Route finding uses BFS with physical port-connectivity checking. Routes respect turnout direction (no backwards frog-end traversal), tile direction markers, and avoid tiles already reserved by other routes.
- **Alternative routes**: When a route is created, BFS finds alternative paths by blocking each edge of the primary path. A white **"+"** badge appears next to the source and target markers when alternatives are available. Right-click any route tile to see them in the context menu:
  - **Alternative 1 / Alternative 2 / ...** — preview the alternative as a dotted line in its own color from a 16-color palette, on top of the main route. Each menu item shows a colored circle icon matching the route color. The main route remains visible.
  - **Use primary route** — discard alternatives and show the original blue route.
  - **Use selected alternative** — promote the previewed alternative to the primary route.
- **Exhaustive Route Search**: Enable in **File → Settings → Exhaustive Route Search**. When active, the BFS also blocks edges from found alternatives (k-shortest-paths iteration), finding more distinct routes. The setting is persisted in `switchboard-demo-app/settings.json`.

### Manage routes

- **Clear a single route**: right-click any tile on the route → **Clear route ({id})**.
- **Clear all routes**: click **Clear selection** from the context menu (edit mode only, deselects all) or programmatically via the model.
- Multiple non-overlapping routes can coexist — the BFS will find a path around existing route tiles.

## 6. Tile direction

Straight and diagonal tiles can have a **direction constraint** (FORWARD, BACKWARD, or BOTH).

- In **edit mode**, right-click a straight or diagonal tile → **Direction** submenu → choose Forward / Backward / Both.
- A small light-gray triangle marker appears at the tile center, pointing in the allowed direction.
- Route finding respects the direction: BFS will not traverse a tile against its direction.
- Default is **Both** (no constraint) — backward-compatible with existing layouts.

## 7. Signal side

Signal tiles can display their body above (Swiss, `_left`) or below (German, `_right`) the track.

- The global default is set via **File → Settings → Signal Side** (Swiss/German).
- Per‑tile override: in **edit mode**, right‑click a signal tile → **Signal Side** submenu → choose Left / Right / Default.
- When you change the signal side, the tile image updates immediately.
- The **Tile Info** dialog (left‑click a signal in normal mode) shows the resolved signal side.

## 8. Define blocks

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

## 9. Simulate occupancy

After a route is created, you can animate a train moving along it:

- Right-click the **green source circle** of a route → **Simulate occupancy ({id})**.
- The simulation creates occupancy markers on every tile along the route and slides the **OCCUPIED** state from the start to the end, one tile at a time (200ms per step).
- Turnouts along the route are automatically set to the correct position for the simulated path.
- **Signal stops**: When a train reaches a signal tile at aspect 0 (red), it stops and waits. Signals only block trains approaching from the front (the direction the signal faces based on rotation). Trains approaching a signal from behind ignore it.
- **Auto-change signal**: Enable via **Edit → Auto-change signal**. When active, a red signal that blocks a train auto-switches to green after 2 seconds, allowing the train to resume. Toggling this option immediately affects all running simulations.
- **Multiple simulations**: Each route can have its own independent simulation running concurrently.
- **Stop simulation**: Right-click the source tile of a running simulation → **Stop simulation ({id})** to stop it mid-route.
- **Simulate occupancy** is disabled while that route's simulation is already running.

To reset the simulation:

- Right-click any tile of a route that has OCCUPIED tiles → **Clear simulated occupancy ({id})**.
- Sets all occupancy states along the route back to FREE.
- **Clear simulated occupancy** is disabled while a simulation is in progress.

## 10. Language / Internationalization

The application supports **English** and **German** locales. UI language is determined by the system locale at startup.

- **Context menu** items (Info, Signal Side, Clear, Direction, route actions) use localized strings.
- **Tile Info dialog** labels are localized.
- **Main menu** and **toolbar** use localized strings (File, Edit, Settings, etc.).
- If your system locale is German (`de`), the UI automatically switches to German labels. You can also force the locale by passing `-Duser.language=de` or `-Duser.language=en` on the Java command line.

Translations are maintained in `i18n/messages.properties` (component) and `i18n/app-messages.properties` (demo app), each with a `_de` variant.

## 11. Save & load

- **Ctrl+S** — save to the current file (or open a save dialog if none).
- **Ctrl+L** — load a previously saved `.json` layout.
- On startup, the app remembers the last loaded file and restores it automatically.
- Settings are stored in `~/switchboard-demo-1/settings.json`.
