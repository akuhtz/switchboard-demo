package com.bidib.switchboard.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidib.switchboard.model.RailwayModel;

public class CycleElementCommand implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(CycleElementCommand.class);

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

        LOGGER.info("Execute on elementId: {}, oldAspect: {}, newAspect: {}", elementId, oldAspect, newAspect);

        model.setElementAspect(elementId, newAspect);
    }

    @Override
    public void undo() {
        model.setElementAspect(elementId, oldAspect);
    }
}
