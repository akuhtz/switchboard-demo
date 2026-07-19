package org.bidib.switchboard.component.model;

public class Tile {

    private final int col;
    private final int row;
    private final String elementId;
    private String svgResource;
    private int rotation;

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

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = ((rotation % 360) + 360) % 360;
    }
}
