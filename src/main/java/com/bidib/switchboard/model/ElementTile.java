package com.bidib.switchboard.model;

import java.util.List;

public class ElementTile extends Tile {

    private final List<String> svgPaths;
    private final ElementType elementType;

    public ElementTile(int col, int row, String elementId,
                       ElementType elementType, List<String> svgPaths) {
        super(col, row, elementId, svgPaths.isEmpty() ? null : svgPaths.get(0));
        this.elementType = elementType;
        this.svgPaths = List.copyOf(svgPaths);
    }

    public String getSvgForAspect(int aspect) {
        if (aspect >= 0 && aspect < svgPaths.size()) {
            return svgPaths.get(aspect);
        }
        return svgPaths.isEmpty() ? null : svgPaths.get(0);
    }

    public int getAspectCount() {
        return svgPaths.size();
    }

    public ElementType getElementType() {
        return elementType;
    }
}
