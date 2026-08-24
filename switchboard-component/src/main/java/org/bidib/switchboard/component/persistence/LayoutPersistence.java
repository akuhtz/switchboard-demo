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
import org.bidib.switchboard.component.model.SignalTile;
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
        save(grid, path, null);
    }

    public void save(TileGrid grid, Path path, String trainsFile) throws IOException {
        LayoutData data = capture(grid);
        data.setTrainsFile(trainsFile);
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
            rd.setName(r.getName());
            List<List<Integer>> tileKeys = new ArrayList<>();
            for (int[] p : r.getPath()) {
                tileKeys.add(List.of(p[0], p[1]));
            }
            rd.setTiles(tileKeys);
            if (r.hasStops()) {
                List<LayoutData.StationStopData> stopList = new ArrayList<>();
                for (Route.StationStop stop : r.getStops()) {
                    LayoutData.StationStopData sd = new LayoutData.StationStopData();
                    sd.setPathIndex(stop.getPathIndex());
                    sd.setDwellTimeMs(stop.getDwellTimeMs());
                    stopList.add(sd);
                }
                rd.setStops(stopList);
            }
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
            if (!b.getPredecessorIds().isEmpty()) {
                bd.setPredecessors(new ArrayList<>(b.getPredecessorIds()));
            }
            if (!b.getSuccessorIds().isEmpty()) {
                bd.setSuccessors(new ArrayList<>(b.getSuccessorIds()));
            }
            if (b.getAssignedTrainId() != null) {
                bd.setAssignedTrainId(b.getAssignedTrainId());
            }
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
        if (tile instanceof SignalTile st) {
            if (st.getSignalSide() != SignalSide.DEFAULT) {
                td.setSignalSide(st.getSignalSide().name());
            }
            if (st.getMainSignalId() != null) {
                td.setMainSignalId(st.getMainSignalId());
            }
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

    public LayoutData load(TileGrid grid, Path path) throws IOException {
        LayoutData data = MAPPER.readValue(path.toFile(), LayoutData.class);
        apply(grid, data);
        return data;
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
                Route route = new Route(rd.getId(), rd.getName(), rd.getSourceElementId(), rd.getTargetElementId(), path);
                if (rd.getStops() != null) {
                    for (LayoutData.StationStopData sd : rd.getStops()) {
                        route.addStop(sd.getPathIndex(), sd.getDwellTimeMs());
                    }
                }
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
            // Restore block links after all blocks are loaded.
            for (LayoutData.BlockData bd : data.getBlocks()) {
                Block block = grid.getBlockModel().getBlock(bd.getId());
                if (block == null) {
                    continue;
                }
                if (bd.getPredecessors() != null) {
                    for (String predId : bd.getPredecessors()) {
                        block.addPredecessor(predId);
                    }
                }
                if (bd.getSuccessors() != null) {
                    for (String succId : bd.getSuccessors()) {
                        block.addSuccessor(succId);
                    }
                }
                if (bd.getAssignedTrainId() != null) {
                    block.setAssignedTrainId(bd.getAssignedTrainId());
                }
            }
        }
    }

    private static Tile reconstructTile(LayoutData.TileData td) {
        if (td.getSvgPaths() == null || td.getSvgPaths().isEmpty()) {
            return null;
        }

        // Migrate old signal SVG paths (without _left/_right suffix and old /icons directory)
        // to their current locations, and the old SIGNAL_3 (S3) naming to SIGNAL_M3 (SM3)
        List<String> svgPaths = td.getSvgPaths().stream()
            .map(LayoutPersistence::migrateSignalSvgPath)
            .map(LayoutPersistence::migrateTrackSvgPath)
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
            typeStr = migrateSignalType(typeStr);
            ElementType elementType = null;
            int bestPrefixLen = -1;
            for (ElementType et : ElementType.values()) {
                String prefix = et.getPrefix();
                if (typeStr.startsWith(prefix)) {
                    String remainder = typeStr.substring(prefix.length());
                    if (!remainder.isEmpty() && remainder.chars().allMatch(Character::isDigit)
                            && prefix.length() > bestPrefixLen) {
                        elementType = et;
                        bestPrefixLen = prefix.length();
                    }
                }
            }
            if (elementType == null) {
                return null;
            }
            tile = switch (elementType) {
                case SIGNAL_M3, SIGNAL_V, SIGNAL_COMBINED -> new SignalTile(td.getCol(), td.getRow(),
                        td.getElementId(), elementType, td.getSvgPaths());
                default -> new ElementTile(td.getCol(), td.getRow(), td.getElementId(),
                        elementType, td.getSvgPaths());
            };
        }
        tile.setRotation(td.getRotation());
        if (td.getDirection() != null) {
            try {
                tile.setDirection(org.bidib.switchboard.component.model.TileDirection.valueOf(td.getDirection()));
            } catch (IllegalArgumentException ignored) {
                // unknown direction value — keep default BOTH
            }
        }
        if (tile instanceof SignalTile st) {
            if (td.getSignalSide() != null) {
                try {
                    st.setSignalSide(SignalSide.valueOf(td.getSignalSide()));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (td.getMainSignalId() != null) {
                st.setMainSignalId(td.getMainSignalId());
            }
        }
        return tile;
    }

    /**
     * Migrates old signal SVG paths (pre _left/_right suffix and old /icons directory)
     * to the current location and naming.
     * E.g. "/icons/signal_3_red.svg" → "/icons/signals/sbb_l/signal_m3_red_left.svg"
     * and "/icons/signals/sbb_l/signal_3_red_left.svg" → "/icons/signals/sbb_l/signal_m3_red_left.svg".
     */
    private static String migrateSignalSvgPath(String path) {
        if (path == null) return null;
        if (path.startsWith("/icons/signal_")) {
            path = path.replace("/icons/signal_", "/icons/signals/sbb_l/signal_");
        }
        path = path.replace("/signal_3_", "/signal_m3_").replace("/signal_3.", "/signal_m3.");
        if (path.contains("_left.svg") || path.contains("_right.svg")) {
            return path; // already migrated
        }
        if (path.startsWith("/icons/signals/sbb_l/signal_") && path.endsWith(".svg")) {
            return path.replace(".svg", "_left.svg");
        }
        return path;
    }

    /**
     * Migrates the old SIGNAL_3 persisted type prefix "S3" (e.g. "S33") to the
     * renamed SIGNAL_M3 prefix "SM3" (e.g. "SM33").
     */
    private static String migrateSignalType(String type) {
        if (type != null && type.startsWith("S3") && !type.startsWith("SM3")) {
            return "SM3" + type.substring(2);
        }
        return type;
    }

    private static final List<String> TRACK_SVG_FILES = List.of(
        "bumper_stop.svg", "curve_left.svg", "curve_right.svg", "diagonal.svg",
        "straight.svg", "turnout_3way_left.svg", "turnout_3way_right.svg",
        "turnout_3way_straight.svg", "turnout_diverted_left.svg",
        "turnout_diverted_right.svg", "turnout_straight_left.svg",
        "turnout_straight_right.svg");

    /**
     * Migrates old track SVG paths (pre /icons/tracks directory) to the current location.
     * E.g. "/icons/turnout_straight_left.svg" → "/icons/tracks/turnout_straight_left.svg"
     */
    private static String migrateTrackSvgPath(String path) {
        if (path == null) return null;
        for (String file : TRACK_SVG_FILES) {
            if (path.equals("/icons/" + file)) {
                return "/icons/tracks/" + file;
            }
        }
        return path;
    }
}
