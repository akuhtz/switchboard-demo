package org.bidib.switchboard.component.command;

import java.util.List;
import java.util.Map;

import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.RouteModel;

public class CreateRouteCommand implements Command {

    private final RouteModel routeModel;

    private final RailwayModel model;

    private final Route newRoute;

    private final Route previousRoute;

    private final List<Route> newAlternatives;

    private final Map<String, Integer> oldAspects;

    public CreateRouteCommand(RouteModel routeModel, RailwayModel model,
                              Route newRoute, Route previousRoute,
                              List<Route> newAlternatives,
                              Map<String, Integer> oldAspects) {
        this.routeModel = routeModel;
        this.model = model;
        this.newRoute = newRoute;
        this.previousRoute = previousRoute;
        this.newAlternatives = newAlternatives;
        this.oldAspects = oldAspects;
    }

    @Override
    public void execute() {
        if (previousRoute != null) {
            routeModel.removeRoute(previousRoute.getId());
        }
        if (newRoute != null) {
            for (Route alt : newAlternatives) {
                routeModel.addAlternativeRoute(newRoute.getId(), alt);
            }
            routeModel.addRoute(newRoute);
        }
        oldAspects.forEach((id, aspect) -> model.setElementAspect(id, aspect));
    }

    @Override
    public void undo() {
        if (newRoute != null) {
            routeModel.removeRoute(newRoute.getId());
        }
        if (previousRoute != null) {
            routeModel.addRoute(previousRoute);
        }
        oldAspects.forEach((id, aspect) -> model.setElementAspect(id, aspect));
    }
}
