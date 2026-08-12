package org.bidib.switchboard.component.model;

import java.util.ArrayList;
import java.util.List;

public class SignalTile extends ElementTile {

    private String mainSignalId;
    private int plateAspect = 0;
    private SignalSide signalSide = SignalSide.DEFAULT;

    public SignalTile(int col, int row, String elementId,
                      ElementType elementType, List<String> svgPaths) {
        super(col, row, elementId, elementType, svgPaths);
    }

    public String getMainSignalId() {
        return mainSignalId;
    }

    public void setMainSignalId(String mainSignalId) {
        this.mainSignalId = mainSignalId;
    }

    public int getPlateAspect() {
        return plateAspect;
    }

    public void setPlateAspect(int plateAspect) {
        this.plateAspect = plateAspect;
    }

    public SignalSide getSignalSide() {
        return signalSide;
    }

    public void setSignalSide(SignalSide signalSide) {
        this.signalSide = signalSide != null ? signalSide : SignalSide.DEFAULT;
    }

    public void applySignalSide(SignalSide resolvedSide) {
        boolean useRight = resolvedSide == SignalSide.RIGHT;
        List<String> paths = getSvgPaths();
        List<String> updated = new ArrayList<>(paths.size());
        for (String path : paths) {
            boolean isRight = path.contains("_right.svg");
            if (useRight && !isRight) {
                updated.add(path.replace("_left.svg", "_right.svg"));
            } else if (!useRight && isRight) {
                updated.add(path.replace("_right.svg", "_left.svg"));
            } else {
                updated.add(path);
            }
        }
        paths.clear();
        paths.addAll(updated);
        setSvgResource(paths.get(0));
    }
}
