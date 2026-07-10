package com.bidib.switchboard.command;

import com.bidib.switchboard.model.RailwayModel;

/**
 * Encapsulates a turnout toggle operation on the {@link RailwayModel}.
 * Since toggling is self-inverse, both {@code execute()} and {@code undo()}
 * call {@code model.toggleTurnout(id)}.
 */
public class ToggleTurnoutCommand implements Command {

    private final RailwayModel model;
    private final String turnoutId;

    public ToggleTurnoutCommand(RailwayModel model, String turnoutId) {
        this.model = model;
        this.turnoutId = turnoutId;
    }

    @Override
    public void execute() {
        model.toggleTurnout(turnoutId);
    }

    @Override
    public void undo() {
        model.toggleTurnout(turnoutId);
    }
}
