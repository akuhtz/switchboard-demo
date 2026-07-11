package com.bidib.switchboard.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO for full layout state serialization to/from JSON.
 */
public class LayoutData {

    private int cols;
    private int rows;
    private int tileSize;
    private List<TileData> tiles = new ArrayList<>();
    private ModelStateData modelState;

    public int getCols() { return cols; }
    public void setCols(int cols) { this.cols = cols; }

    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = rows; }

    public int getTileSize() { return tileSize; }
    public void setTileSize(int tileSize) { this.tileSize = tileSize; }

    public List<TileData> getTiles() { return tiles; }
    public void setTiles(List<TileData> tiles) { this.tiles = tiles; }

    public ModelStateData getModelState() { return modelState; }
    public void setModelState(ModelStateData modelState) { this.modelState = modelState; }

    // --- Tile data ---

    public static class TileData {
        private int col;
        private int row;
        private String elementId;
        private String type; // "plain", "turnout2", "turnout3", "signal2", "signal3"

        // plain and fallback
        private String svgResource;

        // turnout
        private String svgStraight;
        private String svgDivertedLeft;
        private String svgDivertedRight;

        // signal
        private List<String> svgAspects;

        public int getCol() { return col; }
        public void setCol(int col) { this.col = col; }

        public int getRow() { return row; }
        public void setRow(int row) { this.row = row; }

        public String getElementId() { return elementId; }
        public void setElementId(String elementId) { this.elementId = elementId; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getSvgResource() { return svgResource; }
        public void setSvgResource(String svgResource) { this.svgResource = svgResource; }

        public String getSvgStraight() { return svgStraight; }
        public void setSvgStraight(String svgStraight) { this.svgStraight = svgStraight; }

        public String getSvgDivertedLeft() { return svgDivertedLeft; }
        public void setSvgDivertedLeft(String svgDivertedLeft) { this.svgDivertedLeft = svgDivertedLeft; }

        public String getSvgDivertedRight() { return svgDivertedRight; }
        public void setSvgDivertedRight(String svgDivertedRight) { this.svgDivertedRight = svgDivertedRight; }

        public List<String> getSvgAspects() { return svgAspects; }
        public void setSvgAspects(List<String> svgAspects) { this.svgAspects = svgAspects; }
    }

    // --- Model state ---

    public static class ModelStateData {
        private Map<String, Integer> turnouts = new LinkedHashMap<>();
        private Map<String, Integer> turnoutAspectCounts = new LinkedHashMap<>();
        private Map<String, Integer> signals = new LinkedHashMap<>();
        private Map<String, Integer> signalAspectCounts = new LinkedHashMap<>();

        public Map<String, Integer> getTurnouts() { return turnouts; }
        public void setTurnouts(Map<String, Integer> turnouts) { this.turnouts = turnouts; }

        public Map<String, Integer> getTurnoutAspectCounts() { return turnoutAspectCounts; }
        public void setTurnoutAspectCounts(Map<String, Integer> turnoutAspectCounts) { this.turnoutAspectCounts = turnoutAspectCounts; }

        public Map<String, Integer> getSignals() { return signals; }
        public void setSignals(Map<String, Integer> signals) { this.signals = signals; }

        public Map<String, Integer> getSignalAspectCounts() { return signalAspectCounts; }
        public void setSignalAspectCounts(Map<String, Integer> signalAspectCounts) { this.signalAspectCounts = signalAspectCounts; }
    }
}
