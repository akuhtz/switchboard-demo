package org.bidib.switchboard.component.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.SignalTile;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.view.TileGrid;

/**
 * Command that moves a set of tiles by a given delta (dCol, dRow).
 * Also shifts route paths that pass through moved tiles.
 * Supports undo to restore tiles and routes to their original state.
 */
public class MoveTilesCommand implements Command {

    private final TileGrid tileGrid;

    private final int dCol;

    private final int dRow;

    /** Original key → tile, ordered by insertion. */
    private final Map<String, Tile> originalTiles = new LinkedHashMap<>();

    /** New key → tile after move. */
    private final Map<String, Tile> movedTiles = new LinkedHashMap<>();

    /** Saved route paths for undo: route ID → original path copy. */
    private final Map<String, List<int[]>> savedRoutePaths = new LinkedHashMap<>();

    public MoveTilesCommand(TileGrid tileGrid, List<String> tileKeys, int dCol, int dRow) {
        this.tileGrid = tileGrid;
        this.dCol = dCol;
        this.dRow = dRow;
        for (String key : tileKeys) {
            String[] parts = key.split(",");
            int col = Integer.parseInt(parts[0]);
            int row = Integer.parseInt(parts[1]);
            Tile tile = tileGrid.getTile(col, row);
            if (tile != null) {
                originalTiles.put(key, tile);
            }
        }
    }

    @Override
    public void execute() {
        movedTiles.clear();
        savedRoutePaths.clear();

        // Build coordinate map for route shifting
        Map<String, String> coordMap = new LinkedHashMap<>();
        for (String key : originalTiles.keySet()) {
            String[] parts = key.split(",");
            int oldCol = Integer.parseInt(parts[0]);
            int oldRow = Integer.parseInt(parts[1]);
            coordMap.put(key, Tile.key(oldCol + dCol, oldRow + dRow));
        }

        // Save and shift route paths (primary routes + alternatives)
        shiftAllRoutePaths(coordMap, true);

        // Remove from original positions
        for (Map.Entry<String, Tile> entry : originalTiles.entrySet()) {
            String[] parts = entry.getKey().split(",");
            int col = Integer.parseInt(parts[0]);
            int row = Integer.parseInt(parts[1]);
            tileGrid.removeTile(col, row);
        }
        // Place at new positions
        for (Map.Entry<String, Tile> entry : originalTiles.entrySet()) {
            Tile tile = entry.getValue();
            int newCol = tile.getCol() + dCol;
            int newRow = tile.getRow() + dRow;
            Tile newTile = cloneTileAt(tile, newCol, newRow);
            tileGrid.setTile(newTile);
            movedTiles.put(Tile.key(newCol, newRow), newTile);
        }
    }

    @Override
    public void undo() {
        // Remove from moved positions
        for (Map.Entry<String, Tile> entry : movedTiles.entrySet()) {
            String[] parts = entry.getKey().split(",");
            int col = Integer.parseInt(parts[0]);
            int row = Integer.parseInt(parts[1]);
            tileGrid.removeTile(col, row);
        }
        // Restore at original positions
        for (Map.Entry<String, Tile> entry : originalTiles.entrySet()) {
            tileGrid.setTile(entry.getValue());
        }

        // Restore route paths using saved originals
        restoreAllRoutePaths();
    }

    private void shiftAllRoutePaths(Map<String, String> coordMap, boolean save) {
        for (Route route : tileGrid.getRouteModel().getRoutes().values()) {
            if (save) {
                savedRoutePaths.put(route.getId(), copyPath(route.getPath()));
            }
            route.shiftPath(coordMap, dCol, dRow);
            // Also shift alternatives for this route
            for (Route alt : tileGrid.getRouteModel().getAlternativeRoutes(route.getId())) {
                if (save) {
                    savedRoutePaths.put(alt.getId(), copyPath(alt.getPath()));
                }
                alt.shiftPath(coordMap, dCol, dRow);
            }
        }
    }

    private void restoreAllRoutePaths() {
        for (Route route : tileGrid.getRouteModel().getRoutes().values()) {
            List<int[]> saved = savedRoutePaths.get(route.getId());
            if (saved != null) {
                replacePath(route, saved);
            }
            for (Route alt : tileGrid.getRouteModel().getAlternativeRoutes(route.getId())) {
                List<int[]> altSaved = savedRoutePaths.get(alt.getId());
                if (altSaved != null) {
                    replacePath(alt, altSaved);
                }
            }
        }
    }

    private static void replacePath(Route route, List<int[]> saved) {
        // Build reverse coord map: for each point in current path, if it was shifted,
        // shift it back. Since we have the saved original, we can directly replace.
        // The route's internal path is a mutable ArrayList exposed via unmodifiable view.
        // We modify it in place.
        List<int[]> current = route.getPath();
        // getPath() returns unmodifiable view, but the underlying list is mutable
        // We need to access the internal list. Since Route stores it as new ArrayList<>,
        // we reconstruct by clearing and re-adding through the mutable reference.
        // Alternative: use the fact that we know the exact saved coordinates.
        // Simplest: clear and refill the internal list
        // We can't do that with unmodifiable view. Let's modify Route to allow this.
        // For now, use shiftPath with reverse coordinates.
        // Actually, let's just replace the content via the mutable internal list.
        // The path field in Route is private but we can access it through the constructor pattern.
        // Best approach: add a replacePath method to Route.
        route.replacePath(saved);
    }

    private static List<int[]> copyPath(List<int[]> path) {
        List<int[]> copy = new ArrayList<>(path.size());
        for (int[] p : path) {
            copy.add(new int[] { p[0], p[1] });
        }
        return copy;
    }

    /**
     * Creates a copy of the given tile at a new grid position, preserving all tile-specific
     * properties (rotation, direction, element type, SVG paths, signal state).
     */
    static Tile cloneTileAt(Tile tile, int newCol, int newRow) {
        if (tile instanceof SignalTile st) {
            List<String> svgPaths = new ArrayList<>(st.getAspectCount());
            for (int i = 0; i < st.getAspectCount(); i++) {
                svgPaths.add(st.getSvgForAspect(i));
            }
            SignalTile copy = new SignalTile(newCol, newRow, st.getElementId(),
                    st.getElementType(), svgPaths);
            copy.setMainSignalId(st.getMainSignalId());
            copy.setPlateAspect(st.getPlateAspect());
            copy.setSignalSide(st.getSignalSide());
            copy.setRotation(st.getRotation());
            copy.setDirection(st.getDirection());
            return copy;
        }
        if (tile instanceof ElementTile et) {
            List<String> svgPaths = new ArrayList<>(et.getAspectCount());
            for (int i = 0; i < et.getAspectCount(); i++) {
                svgPaths.add(et.getSvgForAspect(i));
            }
            ElementTile copy = new ElementTile(newCol, newRow, et.getElementId(),
                    et.getElementType(), svgPaths);
            copy.setRotation(et.getRotation());
            copy.setDirection(et.getDirection());
            return copy;
        }
        Tile copy = new Tile(newCol, newRow, tile.getElementId(), tile.getSvgResource());
        copy.setRotation(tile.getRotation());
        copy.setDirection(tile.getDirection());
        return copy;
    }
}
