package com.bidib.switchboard.model;

/**
 * A tile representing a turnout (points) on the switchboard.
 * Has two SVG icons: one for the straight aspect and one for the diverted aspect.
 * The active icon is determined by the turnout's current aspect in the model.
 */
public class TurnoutTile extends Tile {

    private final String svgStraight;
    private final String svgDiverted;

    /**
     * @param col         column index (0-based)
     * @param row         row index (0-based)
     * @param elementId   turnout identifier in the model (e.g. "W1")
     * @param svgStraight classpath resource path to the SVG icon for the STRAIGHT aspect
     * @param svgDiverted classpath resource path to the SVG icon for the DIVERTED aspect
     */
    public TurnoutTile(int col, int row, String elementId, String svgStraight, String svgDiverted) {
        super(col, row, elementId, svgStraight);
        this.svgStraight = svgStraight;
        this.svgDiverted = svgDiverted;
    }

    public String getSvgStraight() {
        return svgStraight;
    }

    public String getSvgDiverted() {
        return svgDiverted;
    }

    /**
     * Returns the SVG resource matching the given turnout aspect.
     */
    public String getSvgForAspect(TurnoutAspect aspect) {
        return (aspect == TurnoutAspect.DIVERTED) ? svgDiverted : svgStraight;
    }
}
