package org.bidib.switchboard.component.model;

import java.util.ArrayList;
import java.util.List;

public class ElementTile extends Tile {

    private final List<String> svgPaths = new ArrayList<>();
    private final ElementType elementType;

    public ElementTile(int col, int row, String elementId,
                       ElementType elementType, List<String> svgPaths) {
        super(col, row, elementId, svgPaths.isEmpty() ? null : svgPaths.get(0));
        this.elementType = elementType;
        this.svgPaths.addAll(svgPaths);
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

    /** Provides mutable access to the SVG aspect paths for subclasses. */
    protected List<String> getSvgPaths() {
        return svgPaths;
    }

    public ElementType getElementType() {
        return elementType;
    }
}
