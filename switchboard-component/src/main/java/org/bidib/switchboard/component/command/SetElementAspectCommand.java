package org.bidib.switchboard.component.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bidib.switchboard.component.model.RailwayModel;

public class SetElementAspectCommand implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetElementAspectCommand.class);

    private final RailwayModel model;

    private final String elementId;

    private final int oldAspect;

    private final int newAspect;

    public SetElementAspectCommand(RailwayModel model, String elementId, int newAspect) {
        this.model = model;
        this.elementId = elementId;
        this.oldAspect = model.getElementAspect(elementId);
        this.newAspect = newAspect;
    }

    @Override
    public void execute() {
        LOGGER.info("Set aspect of {} from {} to {}", elementId, oldAspect, newAspect);
        model.setElementAspect(elementId, newAspect);
    }

    @Override
    public void undo() {
        model.setElementAspect(elementId, oldAspect);
    }
}
