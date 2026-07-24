package org.bidib.switchboard.component.persistence;

import java.util.ArrayList;
import java.util.List;

public class LayoutData {

    private int cols;
    private int rows;
    private int tileSize;
    private List<TileData> tiles = new ArrayList<>();
    private ModelStateData modelState;
    private List<RouteData> routes = new ArrayList<>();

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

    public List<RouteData> getRoutes() { return routes; }
    public void setRoutes(List<RouteData> routes) { this.routes = routes; }

    // --- Tile data ---

    public static class TileData {
        private int col;
        private int row;
        private String elementId;
        private String type;
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
        private List<OccupancyData> occupancies = new ArrayList<>();

        public List<ElementData> getElements() { return elements; }
        public void setElements(List<ElementData> elements) { this.elements = elements; }

        public List<OccupancyData> getOccupancies() { return occupancies; }
        public void setOccupancies(List<OccupancyData> occupancies) { this.occupancies = occupancies; }
    }

    public static class OccupancyData {
        private String id;
        private long nodeId;
        private int portId;
        private String state;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public long getNodeId() { return nodeId; }
        public void setNodeId(long nodeId) { this.nodeId = nodeId; }

        public int getPortId() { return portId; }
        public void setPortId(int portId) { this.portId = portId; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
    }

    public static class ElementData {
        private String id;
        private long nodeId;
        private long accessoryId;
        private int aspect;
        private String occupancyId;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public long getNodeId() { return nodeId; }
        public void setNodeId(long nodeId) { this.nodeId = nodeId; }

        public long getAccessoryId() { return accessoryId; }
        public void setAccessoryId(long accessoryId) { this.accessoryId = accessoryId; }

        public int getAspect() { return aspect; }
        public void setAspect(int aspect) { this.aspect = aspect; }

        public String getOccupancyId() { return occupancyId; }
        public void setOccupancyId(String occupancyId) { this.occupancyId = occupancyId; }
    }

    // --- Route data ---

    public static class RouteData {
        private String id;
        private String sourceElementId;
        private String targetElementId;
        private List<List<Integer>> tiles = new ArrayList<>();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getSourceElementId() { return sourceElementId; }
        public void setSourceElementId(String sourceElementId) { this.sourceElementId = sourceElementId; }

        public String getTargetElementId() { return targetElementId; }
        public void setTargetElementId(String targetElementId) { this.targetElementId = targetElementId; }

        public List<List<Integer>> getTiles() { return tiles; }
        public void setTiles(List<List<Integer>> tiles) { this.tiles = tiles; }
    }
}
