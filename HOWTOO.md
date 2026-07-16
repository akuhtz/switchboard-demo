# How to start from an empty switchboard

## 1. Run the application

```sh
mvn compile exec:java -Dexec.mainClass=com.bidib.switchboard.SwitchboardApp
```

Or build and run the executable JAR:

```sh
mvn clean package -DskipTests
java -jar target/switchboard-demo-1.0-SNAPSHOT.jar
```

## 2. Reset to a blank grid

- Press **Ctrl+E** to enter edit mode.
- From the menu: **File → Load...** and select an empty JSON layout (or start fresh by removing all tiles manually via **Clear** in the context menu).
- Alternatively, delete `settings.json` if present, then delete the last loaded layout file — the app will start with the hardcoded default. You can then clear tiles one by one.

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

## 4. Click elements to cycle aspects

In **normal mode** (Ctrl+E to toggle):
- Click a turnout or signal to cycle its aspect (straight ↔ diverted, red ↔ green, etc.).
- Turnouts auto-switch to the correct aspect when a route is found.

## 5. Create a route

- Make sure you are in **normal mode** (Ctrl+E to toggle off edit mode).
- **Ctrl+click** a source tile — a green marker appears.
- **Ctrl+click** a target tile — the shortest path is found and drawn as a red polyline with green (source) and blue (target) markers.
- Turnouts along the route are automatically set to the correct aspect.
- Route finding uses BFS with physical port-connectivity checking. Routes respect turnout direction (no backwards frog-end traversal) and avoid tiles already reserved by other routes.
- **Alternative routes**: When a route is created, BFS finds alternative paths by blocking each edge of the primary path. Right-click any route tile to see them in the context menu:
  - **Alternative 1 / Alternative 2 / ...** — preview the alternative as a dotted green line.
  - **Use primary route** — discard alternatives and show the original red route.
  - **Use selected alternative** — promote the previewed alternative to the primary route.
- **Exhaustive Route Search**: Enable in **File → Settings → Exhaustive Route Search**. When active, the BFS also blocks edges from found alternatives (k-shortest-paths iteration), finding more distinct routes. The setting is persisted in `settings.json`.

### Manage routes

- **Clear a single route**: right-click any tile on the route → **Clear route ({id})**.
- **Clear all routes**: click **Clear selection** from the context menu (edit mode only, deselects all) or programmatically via the model.
- Multiple non-overlapping routes can coexist — the BFS will find a path around existing route tiles.

## 6. Save & load

- **Ctrl+S** — save to the current file (or open a save dialog if none).
- **Ctrl+L** — load a previously saved `.json` layout.
- On startup, the app remembers the last loaded file and restores it automatically.
