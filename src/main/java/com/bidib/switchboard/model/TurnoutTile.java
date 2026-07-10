package com.bidib.switchboard.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A tile representing a turnout (points) on the switchboard.
 * Supports multiple aspects, each with its own SVG icon.
 * The active icon is determined by the turnout's current aspect in the model.
 */
public class TurnoutTile extends Tile {

    private final Map<TurnoutAspect, String> svgByAspect;

    /**
     * Creates a 2-way turnout tile.
     *
     * @param col              column index (0-based)
     * @param row              row index (0-based)
     * @param elementId        turnout identifier in the model (e.g. "W1")
     * @param svgStraight      SVG resource for the STRAIGHT aspect
     * @param svgDivertedLeft  SVG resource for the DIVERTED_LEFT aspect
     */
    public TurnoutTile(int col, int row, String elementId, String svgStraight, String svgDivertedLeft) {
        super(col, row, elementId, svgStraight);
        Map<TurnoutAspect, String> map = new LinkedHashMap<>();
        map.put(TurnoutAspect.STRAIGHT, svgStraight);
        map.put(TurnoutAspect.DIVERTED_LEFT, svgDivertedLeft);
        this.svgByAspect = Collections.unmodifiableMap(map);
    }

    /**
     * Creates a 3-way turnout tile.
     *
     * @param col              column index (0-based)
     * @param row              row index (0-based)
     * @param elementId        turnout identifier in the model (e.g. "W1")
     * @param svgStraight      SVG resource for the STRAIGHT aspect
     * @param svgDivertedLeft  SVG resource for the DIVERTED_LEFT aspect
     * @param svgDivertedRight SVG resource for the DIVERTED_RIGHT aspect
     */
    public TurnoutTile(int col, int row, String elementId, String svgStraight, String svgDivertedLeft, String svgDivertedRight) {
        super(col, row, elementId, svgStraight);
        Map<TurnoutAspect, String> map = new LinkedHashMap<>();
        map.put(TurnoutAspect.STRAIGHT, svgStraight);
        map.put(TurnoutAspect.DIVERTED_LEFT, svgDivertedLeft);
        map.put(TurnoutAspect.DIVERTED_RIGHT, svgDivertedRight);
        this.svgByAspect = Collections.unmodifiableMap(map);
    }

    /**
     * Returns the SVG resource matching the given turnout aspect.
     * Falls back to the STRAIGHT icon if the aspect is not mapped.
     */
    public String getSvgForAspect(TurnoutAspect aspect) {
        return svgByAspect.getOrDefault(aspect, svgByAspect.get(TurnoutAspect.STRAIGHT));
    }

    /**
     * Returns all aspect-to-SVG mappings.
     */
    public Map<TurnoutAspect, String> getSvgByAspect() {
        return svgByAspect;
    }
}
