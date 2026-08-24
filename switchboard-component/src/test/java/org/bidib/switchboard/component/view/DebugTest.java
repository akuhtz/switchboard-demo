package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.config.TestOccupancyFactory;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.bidib.switchboard.component.service.RouterService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Disabled
class DebugTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebugTest.class);

	private final OccupancyFactory occupancyFactory = new TestOccupancyFactory(); 

    private static Path testLayout5() throws Exception {
        var url = DebugTest.class.getResource("/test-data/switchboard5.json");
        return Paths.get(url.toURI());
    }

    @Test
    void debugP015toTL004() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(occupancyFactory, (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model, RouterService.createDefault());

        var layoutPersistence = new LayoutPersistence();
        layoutPersistence.load(panel, testLayout5());

        panel.testSetRouteSource(2, 3);
        panel.testFindRoute(7, 11);

        assertThat(panel.hasActiveRoute()).as("Route should be found from P-015 to TL-004").isTrue();
        LOGGER.info("RouteModel size: {}", panel.getRouteModel().size());
        for (String rid : panel.getRouteModel().getRoutes().keySet()) {
            Route r = panel.getRouteModel().getRoute(rid);
            LOGGER.info("  Route: {} ({}→{}) tiles={}", rid, r.getSourceElementId(), r.getTargetElementId(), r.getPath().size());
            if (panel.getRouteModel().hasAlternativeRoute(rid)) {
                LOGGER.info("    Alternatives: {}", panel.getRouteModel().getAlternativeRoutes(rid).size());
            }
        }
    }
}
