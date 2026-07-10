package com.bidib.switchboard.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A tile representing a signal on the switchboard.
 * Supports multiple aspects (2–8), each with its own SVG icon.
 * The active icon is determined by the signal's current aspect in the model.
 */
public class SignalTile extends Tile {

    private final Map<SignalAspect, String> svgByAspect;

    /**
     * Creates a signal tile with the given aspect-to-SVG mappings.
     *
     * @param col         column index (0-based)
     * @param row         row index (0-based)
     * @param elementId   signal identifier in the model (e.g. "S1")
     * @param svgByAspect ordered map of aspect to SVG resource path
     */
    public SignalTile(int col, int row, String elementId, Map<SignalAspect, String> svgByAspect) {
        super(col, row, elementId, svgByAspect.values().iterator().next());
        this.svgByAspect = Collections.unmodifiableMap(new LinkedHashMap<>(svgByAspect));
    }

    /**
     * Creates a 2-aspect signal tile.
     *
     * @param col        column index (0-based)
     * @param row        row index (0-based)
     * @param elementId  signal identifier in the model (e.g. "S1")
     * @param svgAspect0 SVG resource for ASPECT_0
     * @param svgAspect1 SVG resource for ASPECT_1
     */
    public SignalTile(int col, int row, String elementId, String svgAspect0, String svgAspect1) {
        this(col, row, elementId, buildMap(svgAspect0, svgAspect1));
    }

    /**
     * Creates a 3-aspect signal tile.
     *
     * @param col        column index (0-based)
     * @param row        row index (0-based)
     * @param elementId  signal identifier in the model (e.g. "S1")
     * @param svgAspect0 SVG resource for ASPECT_0
     * @param svgAspect1 SVG resource for ASPECT_1
     * @param svgAspect2 SVG resource for ASPECT_2
     */
    public SignalTile(int col, int row, String elementId, String svgAspect0, String svgAspect1, String svgAspect2) {
        this(col, row, elementId, buildMap(svgAspect0, svgAspect1, svgAspect2));
    }

    /**
     * Returns the SVG resource matching the given signal aspect.
     * Falls back to the ASPECT_0 icon if the aspect is not mapped.
     */
    public String getSvgForAspect(SignalAspect aspect) {
        return svgByAspect.getOrDefault(aspect, svgByAspect.get(SignalAspect.ASPECT_0));
    }

    /**
     * Returns all aspect-to-SVG mappings.
     */
    public Map<SignalAspect, String> getSvgByAspect() {
        return svgByAspect;
    }

    private static Map<SignalAspect, String> buildMap(String... svgResources) {
        Map<SignalAspect, String> map = new LinkedHashMap<>();
        for (int i = 0; i < svgResources.length; i++) {
            map.put(SignalAspect.of(i), svgResources[i]);
        }
        return map;
    }
}
