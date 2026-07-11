package com.bidib.switchboard.command;

import com.bidib.switchboard.model.RailwayModel;

public class CycleElementCommand implements Command {

    private final RailwayModel model;
    private final String elementId;
    private final int oldAspect;
    private final int newAspect;

    public CycleElementCommand(RailwayModel model, String elementId, int aspectCount) {
        this.model = model;
        this.elementId = elementId;
        this.oldAspect = model.getElementAspect(elementId);
        this.newAspect = (oldAspect + 1) % aspectCount;
    }

    @Override
    public void execute() {
        model.setElementAspect(elementId, newAspect);
    }

    @Override
    public void undo() {
        model.setElementAspect(elementId, oldAspect);
    }
}
