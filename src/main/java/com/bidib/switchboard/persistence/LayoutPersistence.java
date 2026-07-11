package com.bidib.switchboard.persistence;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bidib.switchboard.model.RailwayModel;
import com.bidib.switchboard.model.SignalAspect;
import com.bidib.switchboard.model.SignalTile;
import com.bidib.switchboard.model.Tile;
import com.bidib.switchboard.model.TurnoutAspect;
import com.bidib.switchboard.model.TurnoutTile;
import com.bidib.switchboard.view.SwitchboardPanel;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Handles saving and loading the layout state (tiles + model) to/from JSON files.
 */
public class LayoutPersistence {

    private static final ObjectMapper MAPPER = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private LayoutPersistence() {
    }

    // --- Save ---

    /**
     * Saves the current panel state to a JSON file.
     */
    public static void save(SwitchboardPanel panel, Path path) throws IOException {
        LayoutData data = capture(panel);
        MAPPER.writeValue(path.toFile(), data);
    }

    /**
     * Captures the current panel state into a LayoutData DTO.
     */
    public static LayoutData capture(SwitchboardPanel panel) {
        LayoutData data = new LayoutData();
        data.setCols(panel.getCols());
        data.setRows(panel.getRows());
        data.setTileSize(panel.getTileSize());

        RailwayModel model = panel.getModel();

        // Tiles
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

        // Model state
        LayoutData.ModelStateData ms = new LayoutData.ModelStateData();

        for (Map.Entry<String, TurnoutAspect> e : model.getTurnouts().entrySet()) {
            ms.getTurnouts().put(e.getKey(), e.getValue().getAspect());
            ms.getTurnoutAspectCounts().put(e.getKey(), model.getTurnoutAspectCount(e.getKey()));
        }
        for (Map.Entry<String, SignalAspect> e : model.getSignals().entrySet()) {
            ms.getSignals().put(e.getKey(), e.getValue().getAspect());
            ms.getSignalAspectCounts().put(e.getKey(), model.getSignalAspectCount(e.getKey()));
        }
        data.setModelState(ms);

        return data;
    }

    private static LayoutData.TileData captureTile(Tile tile) {
        LayoutData.TileData td = new LayoutData.TileData();
        td.setCol(tile.getCol());
        td.setRow(tile.getRow());
        td.setElementId(tile.getElementId());

        if (tile instanceof TurnoutTile tt) {
            boolean is3Way = tt.getSvgByAspect().containsKey(TurnoutAspect.DIVERTED_RIGHT);
            td.setType(is3Way ? "turnout3" : "turnout2");
            td.setSvgStraight(tt.getSvgByAspect().get(TurnoutAspect.STRAIGHT));
            td.setSvgDivertedLeft(tt.getSvgByAspect().get(TurnoutAspect.DIVERTED_LEFT));
            if (is3Way) {
                td.setSvgDivertedRight(tt.getSvgByAspect().get(TurnoutAspect.DIVERTED_RIGHT));
            }
        }
        else if (tile instanceof SignalTile st) {
            int count = st.getSvgByAspect().size();
            td.setType(count == 2 ? "signal2" : "signal3");
            td.setSvgAspects(new ArrayList<>(st.getSvgByAspect().values()));
        }
        else {
            td.setType("plain");
            td.setSvgResource(tile.getSvgResource());
        }
        return td;
    }

    // --- Load ---

    /**
     * Loads panel state from a JSON file.
     */
    public static void load(SwitchboardPanel panel, Path path) throws IOException {
        LayoutData data = MAPPER.readValue(path.toFile(), LayoutData.class);
        apply(panel, data);
    }

    /**
     * Applies LayoutData to the given panel and its model.
     */
    public static void apply(SwitchboardPanel panel, LayoutData data) {
        RailwayModel model = panel.getModel();
        panel.clearTiles();
        model.clear();

        // Restore model state
        if (data.getModelState() != null) {
            LayoutData.ModelStateData ms = data.getModelState();
            if (ms.getTurnoutAspectCounts() != null) {
                for (Map.Entry<String, Integer> e : ms.getTurnoutAspectCounts().entrySet()) {
                    model.addTurnout(e.getKey(), e.getValue());
                }
            }
            if (ms.getTurnouts() != null) {
                for (Map.Entry<String, Integer> e : ms.getTurnouts().entrySet()) {
                    model.setTurnoutAspect(e.getKey(), TurnoutAspect.of(e.getValue()));
                }
            }
            if (ms.getSignalAspectCounts() != null) {
                for (Map.Entry<String, Integer> e : ms.getSignalAspectCounts().entrySet()) {
                    model.addSignal(e.getKey(), e.getValue());
                }
            }
            if (ms.getSignals() != null) {
                for (Map.Entry<String, Integer> e : ms.getSignals().entrySet()) {
                    model.setSignal(e.getKey(), SignalAspect.of(e.getValue()));
                }
            }
        }

        // Restore tiles
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
        switch (td.getType()) {
            case "turnout2":
                return new TurnoutTile(td.getCol(), td.getRow(), td.getElementId(), td.getSvgStraight(), td.getSvgDivertedLeft());
            case "turnout3":
                return new TurnoutTile(td.getCol(), td.getRow(), td.getElementId(), td.getSvgStraight(), td.getSvgDivertedLeft(), td.getSvgDivertedRight());
            case "signal2":
                return new SignalTile(td.getCol(), td.getRow(), td.getElementId(), td.getSvgAspects().get(0), td.getSvgAspects().get(1));
            case "signal3":
                return new SignalTile(td.getCol(), td.getRow(), td.getElementId(), td.getSvgAspects().get(0), td.getSvgAspects().get(1),
                    td.getSvgAspects().get(2));
            default:
                return new Tile(td.getCol(), td.getRow(), td.getElementId(), td.getSvgResource());
        }
    }
}
