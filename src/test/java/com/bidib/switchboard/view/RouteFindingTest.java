package com.bidib.switchboard.view;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(panel.hasActiveRoute()).as("Route should be found from (0,0) to (10,1) through diverted turnouts").isTrue();
        assertThat(panel.routeTileCount() > 0).as("Route should contain at least one tile").isTrue();

        int tr003aspect = model.getElementAspect("TR-003");
        assertThat(tr003aspect).as("TR-003 should be set to diverted (aspect=1)").isEqualTo(1);
    }

    @Test
    void routeFromRow3Col2ToRow5Col10() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(2, 3);
        panel.testFindRoute(10, 5);

        assertThat(panel.hasActiveRoute()).as("Route should be found from (2,3) to (10,5)").isTrue();
        assertThat(panel.routeTileCount() > 0).as("Route should contain at least one tile").isTrue();
    }

    @Test
    void routeFromRow3Col2ToRow4Col10() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(2, 3);
        panel.testFindRoute(10, 4);

        assertThat(panel.hasActiveRoute()).as("Route should be found from (2,3) to (10,5)").isTrue();
        assertThat(panel.routeTileCount() > 0).as("Route should contain at least one tile").isTrue();
    }

    @Test
    void routeFromRow3Col2ToRow0Col10() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(2, 3);
        panel.testFindRoute(10, 0);

        assertThat(panel.hasActiveRoute()).as("Route should NOT be found from (2,3) to (10,0)").isFalse();
    }

    @Test
    void routeFromRow1Col10ToRow3Col2() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(10, 1);
        panel.testFindRoute(2, 3);

        assertThat(panel.hasActiveRoute()).as("Route should be found from (10,1) to (2,3)").isTrue();
        assertThat(panel.routeTileCount() > 0).as("Route should contain at least one tile").isTrue();
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
        assertThat(panel.getRouteModel().size()).as("One route should exist after first find").isEqualTo(1);

        // Route B: (2,3) -> (10,5) via row 3+4+5 — non-overlapping
        panel.testSetRouteSource(2, 3);
        panel.testFindRoute(10, 5);
        assertThat(panel.getRouteModel().size()).as("Two routes should coexist").isEqualTo(2);

        int totalTiles = panel.routeTileCount();
        assertThat(totalTiles >= 4).as("Two routes should cover ≥4 tiles combined").isTrue();
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

        assertThat(panel.getRouteModel().size()).as("No new route should be added when blocked by conflict").isEqualTo(routeASize);
        assertThat(panel.routeTileCount()).as("Tile count should be unchanged").isEqualTo(routeATiles);
    }

    @Test
    void removeRouteById() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);
        assertThat(panel.hasActiveRoute()).isTrue();

        String routeId = panel.getRouteModel().getRoutes().keySet().iterator().next();
        panel.getRouteModel().removeRoute(routeId);

        assertThat(panel.hasActiveRoute()).as("Route should be removed").isFalse();
        assertThat(panel.getRouteModel().size()).isEqualTo(0);
        assertThat(panel.routeTileCount()).isEqualTo(0);
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

        assertThat(panel.getRouteModel().size()).isEqualTo(2);

        panel.getRouteModel().clear();

        assertThat(panel.getRouteModel().size()).isEqualTo(0);
        assertThat(panel.hasActiveRoute()).isFalse();
        assertThat(panel.routeTileCount()).isEqualTo(0);
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

        assertThat(panel1.getRouteModel().size()).isEqualTo(2);

        // Capture
        LayoutData data = LayoutPersistence.capture(panel1);
        assertThat(data.getRoutes()).isNotNull();
        assertThat(data.getRoutes()).hasSize(2);

        // Apply to fresh panel
        RailwayModel model2 = new RailwayModel();
        SwitchboardPanel panel2 = new SwitchboardPanel(model2);
        LayoutPersistence.apply(panel2, data);

        assertThat(panel2.getRouteModel().size()).as("Routes should survive round-trip").isEqualTo(2);
        assertThat(panel2.hasActiveRoute()).isTrue();

        Route r1 = panel2.getRouteModel().getRoute("P-001-P-011");
        assertThat(r1).as("Route P-001-P-011 should exist after load").isNotNull();

        Route r2 = panel2.getRouteModel().getRoute("P-015-P-024");
        assertThat(r2).as("Route P-015-P-024 should exist after load").isNotNull();
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
        assertThat(panel.getRouteModel().isTileReserved(5, 0, null)).isTrue();

        // Tile (99,99) is out of bounds — should not be reserved
        assertThat(panel.getRouteModel().isTileReserved(99, 99, null)).isFalse();

        // With excludeRouteId, tile should not be reserved
        String routeId = panel.getRouteModel().getRoutes().keySet().iterator().next();
        assertThat(panel.getRouteModel().isTileReserved(5, 0, routeId)).isFalse();
    }

    @Test
    void routeIdFormat() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);

        Route route = panel.getRouteModel().getRoutes().values().iterator().next();
        assertThat(route.getId()).isEqualTo("P-001-P-011");
        assertThat(route.getSourceElementId()).isEqualTo("P-001");
        assertThat(route.getTargetElementId()).isEqualTo("P-011");
    }

    @Test
    void routeContainsTile() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);

        Route route = panel.getRouteModel().getRoutes().values().iterator().next();
        assertThat(route.containsTile(0, 0)).isTrue();
        assertThat(route.containsTile(10, 1)).isTrue();
        assertThat(route.containsTile(99, 99)).isFalse();
    }

    @Test
    void alternativeRouteFoundForP015ToP065() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);

        var url = RouteFindingTest.class.getResource("/test-data/switchboard4.json");
        LayoutPersistence.load(panel, Paths.get(url.toURI()));

        panel.testSetRouteSource(2, 3);
        panel.testFindRoute(24, 6);

        assertThat(panel.hasActiveRoute()).isTrue();
        assertThat(panel.getRouteModel().size()).isEqualTo(2);

        // The new route is P-015-P-065
        String routeId = "P-015-P-065";
        Route r = panel.getRouteModel().getRoute(routeId);
        assertThat(r).isNotNull();
        assertThat(panel.getRouteModel().hasAlternativeRoute(routeId)).isTrue();
        assertThat(panel.getRouteModel().getAlternativeRoutes(routeId)).hasSize(2);
    }
}
