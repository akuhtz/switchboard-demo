package com.bidib.switchboard.persistence;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bidib.switchboard.model.ElementTile;
import com.bidib.switchboard.model.ElementType;
import com.bidib.switchboard.model.RailwayModel;
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
        ms.setAspects(new java.util.LinkedHashMap<>(model.getElementAspects()));
        data.setModelState(ms);

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

        if (data.getModelState() != null && data.getModelState().getAspects() != null) {
            for (Map.Entry<String, Integer> e : data.getModelState().getAspects().entrySet()) {
                model.addElement(e.getKey());
                model.setElementAspect(e.getKey(), e.getValue());
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
