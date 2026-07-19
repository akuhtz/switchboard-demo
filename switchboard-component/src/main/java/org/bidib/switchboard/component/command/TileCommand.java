package org.bidib.switchboard.component.command;

import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.view.TileGrid;

public class TileCommand implements Command {

    private final TileGrid tileGrid;

    private final RailwayModel model;

    private final int col;

    private final int row;

    private final Tile oldTile;

    private final String oldElementId;

    private final Tile newTile;

    private final String newElementId;

    public TileCommand(TileGrid tileGrid, RailwayModel model, int col, int row,
                       Tile oldTile, String oldElementId,
                       Tile newTile, String newElementId) {
        this.tileGrid = tileGrid;
        this.model = model;
        this.col = col;
        this.row = row;
        this.oldTile = oldTile;
        this.oldElementId = oldElementId;
        this.newTile = newTile;
        this.newElementId = newElementId;
    }

    @Override
    public void execute() {
        if (oldElementId != null) {
            model.removeElement(oldElementId);
        }
        if (newElementId != null) {
            model.addElement(new Element(newElementId, 0, 0));
        }
        if (newTile != null) {
            tileGrid.setTile(newTile);
        }
        else {
            tileGrid.removeTile(col, row);
        }
    }

    @Override
    public void undo() {
        if (newElementId != null) {
            model.removeElement(newElementId);
        }
        if (oldTile != null) {
            tileGrid.setTile(oldTile);
        }
        else {
            tileGrid.removeTile(col, row);
        }
        if (oldElementId != null) {
            model.addElement(new Element(oldElementId, 0, 0));
        }
    }
}
