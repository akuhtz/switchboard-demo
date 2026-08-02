package org.bidib.switchboard.component.persistence;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.bidib.switchboard.component.model.Block;
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.SignalSide;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.view.TileGrid;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

public class LayoutPersistence {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final OccupancySerializer occupancySerializer;

    public LayoutPersistence() {
        this(new DefaultOccupancySerializer());
    }

    public LayoutPersistence(final OccupancySerializer occupancySerializer) {
        this.occupancySerializer = occupancySerializer;
    }

    // --- Save ---

    public void save(TileGrid grid, Path path) throws IOException {
        LayoutData data = capture(grid);
        MAPPER.writeValue(path.toFile(), data);
    }

    public LayoutData capture(TileGrid grid) {
        LayoutData data = new LayoutData();
        data.setCols(grid.getCols());
        data.setRows(grid.getRows());
        data.setTileSize(grid.getTileSize());

        RailwayModel model = grid.getModel();

        List<LayoutData.TileData> tileList = new ArrayList<>();
        for (int col = 0; col < grid.getCols(); col++) {
            for (int row = 0; row < grid.getRows(); row++) {
                Tile tile = grid.getTile(col, row);
                if (tile != null) {
                    tileList.add(captureTile(tile));
                }
            }
        }
        data.setTiles(tileList);

        LayoutData.ModelStateData ms = new LayoutData.ModelStateData();
        List<LayoutData.ElementData> elementList = new ArrayList<>();
        for (Element el : model.getElements().values()) {
            LayoutData.ElementData ed = new LayoutData.ElementData();
            ed.setId(el.getId());
            ed.setNodeId(el.getNodeId());
            ed.setAccessoryId(el.getAccessoryId());
            ed.setAspect(el.getCurrentAspect());
            Occupancy occ = el.getOccupancy();
            if (occ != null) {
                ed.setOccupancyId(occ.getId());
            }
            elementList.add(ed);
        }
        ms.setElements(elementList);

        List<LayoutData.OccupancyData> occList = new ArrayList<>();
        for (Occupancy occ : model.getOccupancies().values()) {
            LayoutData.OccupancyData od = new LayoutData.OccupancyData();
            od.setId(occ.getId());
            od.setState(occ.getState().name());
            occupancySerializer.writeOccupancy(occ, od);
            occList.add(od);
        }
        ms.setOccupancies(occList);
        data.setModelState(ms);

        List<LayoutData.RouteData> routeList = new ArrayList<>();
        for (Route r : grid.getRouteModel().getRoutes().values()) {
            LayoutData.RouteData rd = new LayoutData.RouteData();
            rd.setId(r.getId());
            rd.setSourceElementId(r.getSourceElementId());
            rd.setTargetElementId(r.getTargetElementId());
            List<List<Integer>> tileKeys = new ArrayList<>();
            for (int[] p : r.getPath()) {
                tileKeys.add(List.of(p[0], p[1]));
            }
            rd.setTiles(tileKeys);
            routeList.add(rd);
        }
        data.setRoutes(routeList);

        List<LayoutData.BlockData> blockList = new ArrayList<>();
        for (Block b : grid.getBlockModel().getBlocks().values()) {
            LayoutData.BlockData bd = new LayoutData.BlockData();
            bd.setId(b.getId());
            bd.setName(b.getName());
            List<List<Integer>> tileKeys = new ArrayList<>();
            for (int[] p : b.getPath()) {
                tileKeys.add(List.of(p[0], p[1]));
            }
            bd.setTiles(tileKeys);
            blockList.add(bd);
        }
        data.setBlocks(blockList);

        return data;
    }

    private static LayoutData.TileData captureTile(Tile tile) {
        LayoutData.TileData td = new LayoutData.TileData();
        td.setCol(tile.getCol());
        td.setRow(tile.getRow());
        td.setElementId(tile.getElementId());
        td.setRotation(tile.getRotation());
        if (tile.getDirection() != org.bidib.switchboard.component.model.TileDirection.BOTH) {
            td.setDirection(tile.getDirection().name());
        }
        if (tile.getSignalSide() != SignalSide.DEFAULT) {
            td.setSignalSide(tile.getSignalSide().name());
        }

        if (tile instanceof ElementTile et) {
            td.setType(et.getElementType().getPrefix() + et.getAspectCount());
            td.setSvgPaths(new ArrayList<>());
            for (int i = 0; i < et.getAspectCount(); i++) {
                td.getSvgPaths().add(et.getSvgForAspect(i));
            }
        } else {
            td.setType("plain");
            td.setSvgPaths(List.of(tile.getSvgResource()));
        }
        return td;
    }

    // --- Load ---

    public void load(TileGrid grid, Path path) throws IOException {
        LayoutData data = MAPPER.readValue(path.toFile(), LayoutData.class);
        apply(grid, data);
    }

    public void apply(TileGrid grid, LayoutData data) {
        RailwayModel model = grid.getModel();
        grid.clearTiles();
        model.clear();
        grid.getRouteModel().clear();

        if (data.getModelState() != null) {
            if (data.getModelState().getOccupancies() != null) {
                for (LayoutData.OccupancyData od : data.getModelState().getOccupancies()) {
                    Occupancy occ = occupancySerializer.createOccupancy(od,
                            Occupancy.OccupancyState.valueOf(od.getState()));
                    if (od.getId() != null) {
                        occ.setId(od.getId());
                    }
                    model.addOccupancy(occ);
                }
            }

            if (data.getModelState().getElements() != null) {
                for (LayoutData.ElementData ed : data.getModelState().getElements()) {
                    Element element = new Element(ed.getId(), ed.getNodeId(), ed.getAccessoryId());
                    element.setCurrentAspect(ed.getAspect());
                    if (ed.getOccupancyId() != null) {
                        Occupancy occ = model.getOccupancy(ed.getOccupancyId());
                        if (occ != null) {
                            element.setOccupancy(occ);
                        }
                    }
                    model.addElement(element);
                }
            }
        }

        if (data.getTiles() != null) {
            for (LayoutData.TileData td : data.getTiles()) {
                Tile tile = reconstructTile(td);
                if (tile != null) {
                    grid.setTile(tile);
                }
            }
        }

        if (data.getRoutes() != null) {
            for (LayoutData.RouteData rd : data.getRoutes()) {
                List<int[]> path = new ArrayList<>();
                for (List<Integer> tileCoord : rd.getTiles()) {
                    path.add(new int[] { tileCoord.get(0), tileCoord.get(1) });
                }
                Route route = new Route(rd.getId(), rd.getSourceElementId(), rd.getTargetElementId(), path);
                grid.getRouteModel().addRoute(route);
            }
        }

        if (data.getBlocks() != null) {
            for (LayoutData.BlockData bd : data.getBlocks()) {
                List<int[]> path = new ArrayList<>();
                for (List<Integer> tileCoord : bd.getTiles()) {
                    path.add(new int[] { tileCoord.get(0), tileCoord.get(1) });
                }
                Block block = new Block(bd.getId(), bd.getName() != null ? bd.getName() : bd.getId(), path);
                grid.getBlockModel().addBlock(block);
            }
        }
    }

    private static Tile reconstructTile(LayoutData.TileData td) {
        if (td.getSvgPaths() == null || td.getSvgPaths().isEmpty()) {
            return null;
        }

        // Migrate old signal SVG paths (without _left/_right suffix) to _left
        List<String> svgPaths = td.getSvgPaths().stream()
            .map(LayoutPersistence::migrateSignalSvgPath)
            .toList();
        td.setSvgPaths(svgPaths);

        Tile tile;
        if ("plain".equals(td.getType())) {
            tile = new Tile(td.getCol(), td.getRow(), td.getElementId(), td.getSvgPaths().get(0));
        } else {
            String typeStr = td.getType();
            if (typeStr == null || typeStr.isEmpty()) {
                return null;
            }
            ElementType elementType = null;
            for (ElementType et : ElementType.values()) {
                if (typeStr.startsWith(et.getPrefix())) {
                    elementType = et;
                    break;
                }
            }
            if (elementType == null) {
                return null;
            }
            tile = new ElementTile(td.getCol(), td.getRow(), td.getElementId(),
                    elementType, td.getSvgPaths());
        }
        tile.setRotation(td.getRotation());
        if (td.getDirection() != null) {
            try {
                tile.setDirection(org.bidib.switchboard.component.model.TileDirection.valueOf(td.getDirection()));
            } catch (IllegalArgumentException ignored) {
                // unknown direction value — keep default BOTH
            }
        }
        if (td.getSignalSide() != null) {
            try {
                tile.setSignalSide(SignalSide.valueOf(td.getSignalSide()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return tile;
    }

    /**
     * Migrates old signal SVG paths (pre _left/_right suffix and old /icons directory)
     * to the current location and naming.
     * E.g. "/icons/signal_2_red.svg" → "/icons/signals/sbb_l/signal_2_red_left.svg"
     */
    private static String migrateSignalSvgPath(String path) {
        if (path == null) return null;
        if (path.startsWith("/icons/signal_")) {
            path = path.replace("/icons/signal_", "/icons/signals/sbb_l/signal_");
        }
        if (path.contains("_left.svg") || path.contains("_right.svg")) {
            return path; // already migrated
        }
        if (path.startsWith("/icons/signals/sbb_l/signal_") && path.endsWith(".svg")) {
            return path.replace(".svg", "_left.svg");
        }
        return path;
    }
}
