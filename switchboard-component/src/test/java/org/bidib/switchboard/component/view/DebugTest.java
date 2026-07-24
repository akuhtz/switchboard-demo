package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.config.TestOccupancyFactory;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.junit.jupiter.api.Test;

class DebugTest {
	
	private final OccupancyFactory occupancyFactory = new TestOccupancyFactory(); 

    private static Path testLayout5() throws Exception {
        var url = DebugTest.class.getResource("/test-data/switchboard5.json");
        return Paths.get(url.toURI());
    }

    @Test
    void debugP015toTL004() throws Exception {
        RailwayModel model = new RailwayModel();
//        final AssignOccupancyDialogFactory assignOccupancyDialogFactory = new AssignOccupancyDialogFactory() {
//			@Override
//			public void showAssignOccupancyDialog(Component parent, RailwayModel model, Element el) {
//				new AssignOccupancyDialog().show(parent, model, el);
//			}
//        };
//        SwitchboardPanel panel = new SwitchboardPanel(occupancyFactory, assignOccupancyDialogFactory, model);
        SwitchboardPanel panel = new SwitchboardPanel(occupancyFactory, (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model);

        var layoutPersistence = new LayoutPersistence();
        layoutPersistence.load(panel, testLayout5());

        panel.testSetRouteSource(2, 3);
        panel.testFindRoute(7, 11);

        boolean hasRoute = panel.hasActiveRoute();
        System.out.println("Panel has active route: " + hasRoute);
        System.out.println("RouteModel size: " + panel.getRouteModel().size());
        for (String rid : panel.getRouteModel().getRoutes().keySet()) {
            Route r = panel.getRouteModel().getRoute(rid);
            System.out.println("  Route: " + rid + " (" + r.getSourceElementId() + "→" + r.getTargetElementId() + ") tiles=" + r.getPath().size());
            if (panel.getRouteModel().hasAlternativeRoute(rid)) {
                System.out.println("    Alternatives: " + panel.getRouteModel().getAlternativeRoutes(rid).size());
            }
        }
        assertThat(true).isTrue();
    }
}
