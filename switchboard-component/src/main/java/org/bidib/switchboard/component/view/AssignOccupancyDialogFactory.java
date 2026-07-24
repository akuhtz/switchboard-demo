package org.bidib.switchboard.component.view;

import java.awt.Component;

import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.RailwayModel;

public interface AssignOccupancyDialogFactory {

    void showAssignOccupancyDialog(Component parent, RailwayModel model, Element el);
}
