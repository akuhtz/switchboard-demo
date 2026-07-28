package org.bidib.switchboard.component.command;

import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.model.TileDirection;

/**
 * Command to change the direction of a tile. Supports undo/redo.
 */
public class DirectionCommand implements Command {

    private final Tile tile;
    private final TileDirection oldDirection;
    private final TileDirection newDirection;

    public DirectionCommand(Tile tile, TileDirection newDirection) {
        this.tile = tile;
        this.oldDirection = tile.getDirection();
        this.newDirection = newDirection;
    }

    @Override
    public void execute() {
        tile.setDirection(newDirection);
    }

    @Override
    public void undo() {
        tile.setDirection(oldDirection);
    }
}
