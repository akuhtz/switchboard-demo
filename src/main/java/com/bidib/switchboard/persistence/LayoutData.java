package com.bidib.switchboard.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        private String type;      // e.g. "T2", "T3", "S2", "S3", "P1"
        private List<String> svgPaths;

        public int getCol() { return col; }
        public void setCol(int col) { this.col = col; }

        public int getRow() { return row; }
        public void setRow(int row) { this.row = row; }

        public String getElementId() { return elementId; }
        public void setElementId(String elementId) { this.elementId = elementId; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public List<String> getSvgPaths() { return svgPaths; }
        public void setSvgPaths(List<String> svgPaths) { this.svgPaths = svgPaths; }
    }

    // --- Model state ---

    public static class ModelStateData {
        private Map<String, Integer> aspects = new LinkedHashMap<>();
        private Map<String, Integer> aspectCounts = new LinkedHashMap<>();

        public Map<String, Integer> getAspects() { return aspects; }
        public void setAspects(Map<String, Integer> aspects) { this.aspects = aspects; }

        public Map<String, Integer> getAspectCounts() { return aspectCounts; }
        public void setAspectCounts(Map<String, Integer> aspectCounts) { this.aspectCounts = aspectCounts; }
    }
}
