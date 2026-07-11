package com.bidib.switchboard.command;

import com.bidib.switchboard.model.RailwayModel;

public class CycleElementCommand implements Command {

    private final RailwayModel model;
    private final String elementId;

    public CycleElementCommand(RailwayModel model, String elementId) {
        this.model = model;
        this.elementId = elementId;
    }

    @Override
    public void execute() {
        model.cycleElement(elementId);
    }

    @Override
    public void undo() {
        model.cycleElement(elementId);
    }
}
