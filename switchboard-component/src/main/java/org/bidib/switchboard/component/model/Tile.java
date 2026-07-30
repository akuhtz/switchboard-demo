package org.bidib.switchboard.component.model;

public class Tile {

    private final int col;
    private final int row;
    private final String elementId;
    private String svgResource;
    private int rotation;
    private TileDirection direction = TileDirection.BOTH;
    private SignalSide signalSide = SignalSide.DEFAULT;

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

    public TileDirection getDirection() {
        return direction;
    }

    public void setDirection(TileDirection direction) {
        this.direction = direction != null ? direction : TileDirection.BOTH;
    }

    public SignalSide getSignalSide() {
        return signalSide;
    }

    public void setSignalSide(SignalSide signalSide) {
        this.signalSide = signalSide != null ? signalSide : SignalSide.DEFAULT;
    }

    /** Returns the map key for a tile at the given grid position. */
    public static String key(int col, int row) {
        return col + "," + row;
    }
}
