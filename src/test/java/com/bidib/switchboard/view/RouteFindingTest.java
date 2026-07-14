package com.bidib.switchboard.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.bidib.switchboard.model.RailwayModel;
import com.bidib.switchboard.persistence.LayoutPersistence;

class RouteFindingTest {

    @Test
    void routeThroughDivertedTurnouts() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        var path = Paths.get("src/test/resources/test-data/switchboard3.json");
        LayoutPersistence.load(panel, path);

        model.setElementAspect("TR-003", 1);
        model.setElementAspect("TR-002", 1);

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);

        assertTrue(panel.hasActiveRoute(),
                "Route should be found from (0,0) to (10,1) through diverted turnouts");
        assertTrue(panel.routeTileCount() > 0, "Route should contain at least one tile");

        int tr003aspect = model.getElementAspect("TR-003");
        assertEquals(1, tr003aspect, "TR-003 should be set to diverted (aspect=1)");
    }

    @Test
    void routeFromRow3Col2ToRow5Col10() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        var path = Paths.get("src/test/resources/test-data/switchboard3.json");
        LayoutPersistence.load(panel, path);

        panel.testSetRouteSource(2, 3);
        panel.testFindRoute(10, 5);

        assertTrue(panel.hasActiveRoute(),
                "Route should be found from (2,3) to (10,5)");
        assertTrue(panel.routeTileCount() > 0,
                "Route should contain at least one tile");
    }

    @Test
    void routeFromRow3Col2ToRow4Col10() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        var path = Paths.get("src/test/resources/test-data/switchboard3.json");
        LayoutPersistence.load(panel, path);

        panel.testSetRouteSource(2, 3);
        panel.testFindRoute(10, 4);

        assertTrue(panel.hasActiveRoute(),
                "Route should be found from (2,3) to (10,4)");
        assertTrue(panel.routeTileCount() > 0,
                "Route should contain at least one tile");
    }

    @Test
    void routeFromRow3Col2ToRow0Col10() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        var path = Paths.get("src/test/resources/test-data/switchboard3.json");
        LayoutPersistence.load(panel, path);

        panel.testSetRouteSource(2, 3);
        panel.testFindRoute(10, 0);

        assertFalse(panel.hasActiveRoute(),
                "Route should NOT be found from (2,3) to (10,0)");
    }
}
