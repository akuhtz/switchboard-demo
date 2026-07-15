package com.bidib.switchboard.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.bidib.switchboard.model.RailwayModel;
import com.bidib.switchboard.model.Route;
import com.bidib.switchboard.persistence.LayoutData;
import com.bidib.switchboard.persistence.LayoutPersistence;

class RouteFindingTest {

    private static Path testLayout() throws Exception {
        var url = RouteFindingTest.class.getResource("/test-data/switchboard3.json");
        return Paths.get(url.toURI());
    }

    @Test
    void routeThroughDivertedTurnouts() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        model.setElementAspect("TR-003", 1);
        model.setElementAspect("TR-002", 1);

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);

        assertTrue(panel.hasActiveRoute(), "Route should be found from (0,0) to (10,1) through diverted turnouts");
        assertTrue(panel.routeTileCount() > 0, "Route should contain at least one tile");

        int tr003aspect = model.getElementAspect("TR-003");
        assertEquals(1, tr003aspect, "TR-003 should be set to diverted (aspect=1)");
    }

    @Test
    void routeFromRow3Col2ToRow5Col10() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

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

        LayoutPersistence.load(panel, testLayout());

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

        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(2, 3);
        panel.testFindRoute(10, 0);

        assertFalse(panel.hasActiveRoute(),
                "Route should NOT be found from (2,3) to (10,0)");
    }

    @Test
    void routeFromRow1Col10ToRow3Col2() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(10, 1);
        panel.testFindRoute(2, 3);

        assertTrue(panel.hasActiveRoute(),
                "Route should be found from (10,1) to (2,3)");
        assertTrue(panel.routeTileCount() > 0,
                "Route should contain at least one tile");
    }

    @Test
    void twoNonOverlappingRoutesCoexist() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        model.setElementAspect("TR-003", 1);
        model.setElementAspect("TR-002", 1);

        // Route A: (0,0) -> (10,1) through diverted turnouts
        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);
        assertEquals(1, panel.getRouteModel().size(), "One route should exist after first find");

        // Route B: (2,3) -> (10,5) via row 3+4+5 — non-overlapping
        panel.testSetRouteSource(2, 3);
        panel.testFindRoute(10, 5);
        assertEquals(2, panel.getRouteModel().size(), "Two routes should coexist");

        int totalTiles = panel.routeTileCount();
        assertTrue(totalTiles >= 4, "Two routes should cover ≥4 tiles combined");
    }

    @Test
    void routeConflictBlocksOverlappingRoute() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        model.setElementAspect("TR-003", 1);
        model.setElementAspect("TR-002", 1);

        // Route A: occupies row 0 tiles
        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);
        int routeASize = panel.getRouteModel().size();
        int routeATiles = panel.routeTileCount();

        // Route B: (10,0) -> (0,0) would need to go through route A's row 0 tiles — blocked
        panel.testSetRouteSource(10, 0);
        panel.testFindRoute(0, 0);

        assertEquals(routeASize, panel.getRouteModel().size(), "No new route should be added when blocked by conflict");
        assertEquals(routeATiles, panel.routeTileCount(), "Tile count should be unchanged");
    }

    @Test
    void removeRouteById() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);
        assertTrue(panel.hasActiveRoute());

        String routeId = panel.getRouteModel().getRoutes().keySet().iterator().next();
        panel.getRouteModel().removeRoute(routeId);

        assertFalse(panel.hasActiveRoute(), "Route should be removed");
        assertEquals(0, panel.getRouteModel().size());
        assertEquals(0, panel.routeTileCount());
    }

    @Test
    void routeModelClearRemovesAllRoutes() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        model.setElementAspect("TR-003", 1);
        model.setElementAspect("TR-002", 1);

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);

        panel.testSetRouteSource(2, 3);
        panel.testFindRoute(10, 5);

        assertEquals(2, panel.getRouteModel().size());

        panel.getRouteModel().clear();

        assertEquals(0, panel.getRouteModel().size());
        assertFalse(panel.hasActiveRoute());
        assertEquals(0, panel.routeTileCount());
    }

    @Test
    void routePersistenceRoundTrip() throws Exception {
        RailwayModel model1 = new RailwayModel();
        SwitchboardPanel panel1 = new SwitchboardPanel(model1);

        LayoutPersistence.load(panel1, testLayout());

        model1.setElementAspect("TR-003", 1);
        model1.setElementAspect("TR-002", 1);

        panel1.testSetRouteSource(0, 0);
        panel1.testFindRoute(10, 1);

        panel1.testSetRouteSource(2, 3);
        panel1.testFindRoute(10, 5);

        assertEquals(2, panel1.getRouteModel().size());

        // Capture
        LayoutData data = LayoutPersistence.capture(panel1);
        assertNotNull(data.getRoutes());
        assertEquals(2, data.getRoutes().size());

        // Apply to fresh panel
        RailwayModel model2 = new RailwayModel();
        SwitchboardPanel panel2 = new SwitchboardPanel(model2);
        LayoutPersistence.apply(panel2, data);

        assertEquals(2, panel2.getRouteModel().size(), "Routes should survive round-trip");
        assertTrue(panel2.hasActiveRoute());

        Route r1 = panel2.getRouteModel().getRoute("P-001-P-011");
        assertNotNull(r1, "Route P-001-P-011 should exist after load");

        Route r2 = panel2.getRouteModel().getRoute("P-015-P-024");
        assertNotNull(r2, "Route P-015-P-024 should exist after load");
    }

    @Test
    void routeModelIsTileReserved() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        model.setElementAspect("TR-003", 1);
        model.setElementAspect("TR-002", 1);

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);

        // Tile (5,0) is on the route path — should be reserved
        assertTrue(panel.getRouteModel().isTileReserved(5, 0, null));

        // Tile (99,99) is out of bounds — should not be reserved
        assertFalse(panel.getRouteModel().isTileReserved(99, 99, null));

        // With excludeRouteId, tile should not be reserved
        String routeId = panel.getRouteModel().getRoutes().keySet().iterator().next();
        assertFalse(panel.getRouteModel().isTileReserved(5, 0, routeId));
    }

    @Test
    void routeIdFormat() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);

        Route route = panel.getRouteModel().getRoutes().values().iterator().next();
        assertEquals("P-001-P-011", route.getId());
        assertEquals("P-001", route.getSourceElementId());
        assertEquals("P-011", route.getTargetElementId());
    }

    @Test
    void routeContainsTile() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);

        Route route = panel.getRouteModel().getRoutes().values().iterator().next();
        assertTrue(route.containsTile(0, 0));
        assertTrue(route.containsTile(10, 1));
        assertFalse(route.containsTile(99, 99));
    }
}
