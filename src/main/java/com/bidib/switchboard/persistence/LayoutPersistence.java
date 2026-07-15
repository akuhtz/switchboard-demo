package com.bidib.switchboard.persistence;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.bidib.switchboard.model.Element;
import com.bidib.switchboard.model.ElementTile;
import com.bidib.switchboard.model.ElementType;
import com.bidib.switchboard.model.Occupancy;
import com.bidib.switchboard.model.RailwayModel;
import com.bidib.switchboard.model.Route;
import com.bidib.switchboard.model.RouteModel;
import com.bidib.switchboard.model.Tile;
import com.bidib.switchboard.view.SwitchboardPanel;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

public class LayoutPersistence {

    private static final ObjectMapper MAPPER = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private LayoutPersistence() {
    }

    // --- Save ---

    public static void save(SwitchboardPanel panel, Path path) throws IOException {
        LayoutData data = capture(panel);
        MAPPER.writeValue(path.toFile(), data);
    }

    public static LayoutData capture(SwitchboardPanel panel) {
        LayoutData data = new LayoutData();
        data.setCols(panel.getCols());
        data.setRows(panel.getRows());
        data.setTileSize(panel.getTileSize());

        RailwayModel model = panel.getModel();

        List<LayoutData.TileData> tileList = new ArrayList<>();
        for (int col = 0; col < panel.getCols(); col++) {
            for (int row = 0; row < panel.getRows(); row++) {
                Tile tile = panel.getTile(col, row);
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
                ed.setOccupancyNodeId(occ.getNodeId());
                ed.setOccupancyPortId(occ.getPortId());
            }
            elementList.add(ed);
        }
        ms.setElements(elementList);

        List<LayoutData.OccupancyData> occList = new ArrayList<>();
        for (Occupancy occ : model.getOccupancies().values()) {
            LayoutData.OccupancyData od = new LayoutData.OccupancyData();
            od.setNodeId(occ.getNodeId());
            od.setPortId(occ.getPortId());
            od.setState(occ.getState().name());
            occList.add(od);
        }
        ms.setOccupancies(occList);
        data.setModelState(ms);

        List<LayoutData.RouteData> routeList = new ArrayList<>();
        for (Route r : panel.getRouteModel().getRoutes().values()) {
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

        return data;
    }

    private static LayoutData.TileData captureTile(Tile tile) {
        LayoutData.TileData td = new LayoutData.TileData();
        td.setCol(tile.getCol());
        td.setRow(tile.getRow());
        td.setElementId(tile.getElementId());
        td.setRotation(tile.getRotation());

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

    public static void load(SwitchboardPanel panel, Path path) throws IOException {
        LayoutData data = MAPPER.readValue(path.toFile(), LayoutData.class);
        apply(panel, data);
    }

    public static void apply(SwitchboardPanel panel, LayoutData data) {
        RailwayModel model = panel.getModel();
        panel.clearTiles();
        model.clear();
        panel.getRouteModel().clear();

        if (data.getModelState() != null) {
            if (data.getModelState().getOccupancies() != null) {
                for (LayoutData.OccupancyData od : data.getModelState().getOccupancies()) {
                    Occupancy occ = Occupancy.create(od.getNodeId(), od.getPortId(),
                            Occupancy.OccupancyState.valueOf(od.getState()));
                    model.addOccupancy(occ);
                }
            }

            if (data.getModelState().getElements() != null) {
                for (LayoutData.ElementData ed : data.getModelState().getElements()) {
                    Element element = new Element(ed.getId(), ed.getNodeId(), ed.getAccessoryId());
                    element.setCurrentAspect(ed.getAspect());
                    if (ed.getOccupancyPortId() >= 0) {
                        Occupancy occ = model.getOccupancy(ed.getOccupancyNodeId(), ed.getOccupancyPortId());
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
                    panel.setTile(tile);
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
                panel.getRouteModel().addRoute(route);
            }
        }
    }

    private static Tile reconstructTile(LayoutData.TileData td) {
        if (td.getSvgPaths() == null || td.getSvgPaths().isEmpty()) {
            return null;
        }

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
        return tile;
    }
}
