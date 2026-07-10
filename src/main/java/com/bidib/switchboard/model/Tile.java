package com.bidib.switchboard.model;

/**
 * Represents a single tile on the switchboard grid.
 * Each tile occupies one cell and displays an SVG icon.
 */
public class Tile {

    private final int col;
    private final int row;
    private final String elementId;
    private String svgResource;

    /**
     * @param col         column index (0-based)
     * @param row         row index (0-based)
     * @param elementId   identifier that maps this tile to a model element (e.g. "W1", "S1"), may be null
     * @param svgResource classpath resource path to the SVG icon (e.g. "/icons/straight.svg")
     */
    public Tile(int col, int row, String elementId, String svgResource) {
        this.col = col;
        this.row = row;
        this.elementId = elementId;
        this.svgResource = svgResource;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public String getElementId() {
        return elementId;
    }

    public String getSvgResource() {
        return svgResource;
    }

    public void setSvgResource(String svgResource) {
        this.svgResource = svgResource;
    }
}
