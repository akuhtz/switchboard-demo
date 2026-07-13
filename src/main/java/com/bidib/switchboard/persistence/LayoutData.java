package com.bidib.switchboard.persistence;

import java.util.ArrayList;
import java.util.List;

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
        private String type;      // e.g. "TL2", "T32", "S22"
        private List<String> svgPaths;
        private int rotation;

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

        public int getRotation() { return rotation; }
        public void setRotation(int rotation) { this.rotation = rotation; }
    }

    // --- Model state ---

    public static class ModelStateData {
        private List<ElementData> elements = new ArrayList<>();

        public List<ElementData> getElements() { return elements; }
        public void setElements(List<ElementData> elements) { this.elements = elements; }
    }

    public static class ElementData {
        private String id;
        private long nodeId;
        private long accessoryId;
        private int aspect;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public long getNodeId() { return nodeId; }
        public void setNodeId(long nodeId) { this.nodeId = nodeId; }

        public long getAccessoryId() { return accessoryId; }
        public void setAccessoryId(long accessoryId) { this.accessoryId = accessoryId; }

        public int getAspect() { return aspect; }
        public void setAspect(int aspect) { this.aspect = aspect; }
    }
}
