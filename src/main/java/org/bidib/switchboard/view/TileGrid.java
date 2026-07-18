package org.bidib.switchboard.view;

import org.bidib.switchboard.model.RailwayModel;
import org.bidib.switchboard.model.RouteModel;
import org.bidib.switchboard.model.Tile;

public interface TileGrid {

    void setTile(Tile tile);

    void clearTiles();

    Tile getTile(int col, int row);

    int getCols();

    int getRows();

    int getTileSize();

    RailwayModel getModel();

    RouteModel getRouteModel();
}
