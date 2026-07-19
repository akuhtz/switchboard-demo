package org.bidib.switchboard.component.view;

import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.RouteModel;
import org.bidib.switchboard.component.model.Tile;

public interface TileGrid {

    void setTile(Tile tile);

    void removeTile(int col, int row);

    void clearTiles();

    Tile getTile(int col, int row);

    int getCols();

    int getRows();

    int getTileSize();

    RailwayModel getModel();

    RouteModel getRouteModel();
}
