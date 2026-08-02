package org.bidib.switchboard.component.model;

import java.util.ArrayList;
import java.util.List;

public class ElementTile extends Tile {

    private final List<String> svgPaths = new ArrayList<>();
    private final ElementType elementType;
    private String mainSignalId;

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

    public ElementType getElementType() {
        return elementType;
    }

    public String getMainSignalId() {
        return mainSignalId;
    }

    public void setMainSignalId(String mainSignalId) {
        this.mainSignalId = mainSignalId;
    }

    public void applySignalSide(SignalSide resolvedSide) {
        boolean useRight = resolvedSide == SignalSide.RIGHT;
        List<String> updated = new ArrayList<>(svgPaths.size());
        for (String path : svgPaths) {
            boolean isRight = path.contains("_right.svg");
            if (useRight && !isRight) {
                updated.add(path.replace("_left.svg", "_right.svg"));
            } else if (!useRight && isRight) {
                updated.add(path.replace("_right.svg", "_left.svg"));
            } else {
                updated.add(path);
            }
        }
        svgPaths.clear();
        svgPaths.addAll(updated);
        setSvgResource(svgPaths.get(0));
    }
}
