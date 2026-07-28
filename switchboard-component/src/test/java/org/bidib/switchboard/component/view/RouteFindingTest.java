package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.config.TestOccupancyFactory;
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.RouteModel;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.model.TileDirection;
import org.bidib.switchboard.component.persistence.LayoutData;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.bidib.switchboard.component.service.RouterService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RouteFindingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteFindingTest.class);
    
    private final OccupancyFactory occupancyFactory = new TestOccupancyFactory(); 

    private static Path testLayout() throws Exception {
        var url = RouteFindingTest.class.getResource("/test-data/switchboard3.json");
        return Paths.get(url.toURI());
    }

    private static RouterService routerService(SwitchboardPanel panel) {
        return new RouterService(panel.getTiles(), panel.getCols(), panel.getRows(), panel.getRouteModel());
    }

    /** Test fixture holding model, panel, and router service. */
    private record TestFixture(RailwayModel model, SwitchboardPanel panel, RouterService rs) {
        RouteModel routeModel() { return panel.getRouteModel(); }
    }

    private TestFixture setup() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(occupancyFactory,
            (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model);
        new LayoutPersistence().load(panel, testLayout());
        RouterService rs = routerService(panel);
        return new TestFixture(model, panel, rs);
    }

    private static void addRouteToModel(RouteModel routeModel, RouterService routerService, SwitchboardPanel panel, List<int[]> path, RailwayModel model) {
        String srcId = panel.getTile(path.get(0)[0], path.get(0)[1]).getElementId();
        String dstId = panel.getTile(path.get(path.size() - 1)[0], path.get(path.size() - 1)[1]).getElementId();
        routerService.setRouteAspects(path, model);
        routeModel.addRoute(new Route(srcId, dstId, path));
    }

    private static void findAndAddRoute(
        RouteModel routeModel, RouterService routerService, SwitchboardPanel panel, int srcCol, int srcRow, int dstCol, int dstRow, RailwayModel model) {
        String srcId = panel.getTile(srcCol, srcRow).getElementId();
        String dstId = panel.getTile(dstCol, dstRow).getElementId();
        String routeId = srcId + "-" + dstId;
        if (routeModel.getRoute(routeId) != null) {
            routeModel.removeRoute(routeId);
        }
        List<int[]> path = routerService.bfsRoute(srcCol, srcRow, dstCol, dstRow);
        if (path == null) {
            return;
        }
        routerService.setRouteAspects(path, model);
        List<List<int[]>> alts = routerService.bfsAlternativeRoutes(srcCol, srcRow, dstCol, dstRow, path, false);
        Route route = new Route(srcId, dstId, path);
        for (List<int[]> altPath : alts) {
            routeModel.addAlternativeRoute(route.getId(), new Route(srcId, dstId, altPath));
        }
        routeModel.addRoute(route);
    }

    @Test
    void routeThroughDivertedTurnouts() throws Exception {
        var f = setup();

        f.model().setElementAspect("TR-003", 1);
        f.model().setElementAspect("TR-002", 1);

        List<int[]> path = f.rs().bfsRoute(0, 0, 10, 1);
        assertThat(path).as("Route should be found from (0,0) to (10,1) through diverted turnouts").isNotNull();
        assertThat(path.size() > 0).as("Route should contain at least one tile").isTrue();

        f.rs().setRouteAspects(path, f.model());
        int tr003aspect = f.model().getElementAspect("TR-003");
        assertThat(tr003aspect).as("TR-003 should be set to diverted (aspect=1)").isEqualTo(1);
    }

    @Test
    void routeFromRow3Col2ToRow5Col10() throws Exception {
        var f = setup();

        List<int[]> path = f.rs().bfsRoute(2, 3, 10, 5);
        assertThat(path).as("Route should be found from (2,3) to (10,5)").isNotNull();
        assertThat(path.size() > 0).as("Route should contain at least one tile").isTrue();
    }

    @Test
    void routeFromRow3Col2ToRow4Col10() throws Exception {
        var f = setup();

        List<int[]> path = f.rs().bfsRoute(2, 3, 10, 4);
        assertThat(path).as("Route should be found from (2,3) to (10,5)").isNotNull();
        assertThat(path.size() > 0).as("Route should contain at least one tile").isTrue();
    }

    @Test
    void routeFromRow3Col2ToRow0Col10() throws Exception {
        var f = setup();

        List<int[]> path = f.rs().bfsRoute(2, 3, 10, 0);
        assertThat(path).as("Route should NOT be found from (2,3) to (10,0)").isNull();
    }

    @Test
    void routeFromRow1Col10ToRow3Col2() throws Exception {
        var f = setup();

        List<int[]> path = f.rs().bfsRoute(10, 1, 2, 3);
        assertThat(path).as("Route should be found from (10,1) to (2,3)").isNotNull();
        assertThat(path.size() > 0).as("Route should contain at least one tile").isTrue();
    }

    @Test
    void twoNonOverlappingRoutesCoexist() throws Exception {
        var f = setup();

        f.model().setElementAspect("TR-003", 1);
        f.model().setElementAspect("TR-002", 1);

        List<int[]> pathA = f.rs().bfsRoute(0, 0, 10, 1);
        assertThat(pathA).isNotNull();
        addRouteToModel(f.routeModel(), f.rs(), f.panel(), pathA, f.model());
        assertThat(f.routeModel().size()).as("One route should exist after first find").isEqualTo(1);

        List<int[]> pathB = f.rs().bfsRoute(2, 3, 10, 5);
        assertThat(pathB).isNotNull();
        addRouteToModel(f.routeModel(), f.rs(), f.panel(), pathB, f.model());
        assertThat(f.routeModel().size()).as("Two routes should coexist").isEqualTo(2);

        int totalTiles = f.routeModel().getRoutes().values().stream().mapToInt(r -> r.getPath().size()).sum();
        assertThat(totalTiles >= 4).as("Two routes should cover \u22654 tiles combined").isTrue();
    }

    @Test
    void routeConflictBlocksOverlappingRoute() throws Exception {
        var f = setup();

        f.model().setElementAspect("TR-003", 1);
        f.model().setElementAspect("TR-002", 1);

        List<int[]> pathA = f.rs().bfsRoute(0, 0, 10, 1);
        assertThat(pathA).isNotNull();
        addRouteToModel(f.routeModel(), f.rs(), f.panel(), pathA, f.model());
        int routeASize = f.routeModel().size();
        int routeATiles = f.routeModel().getRoutes().values().stream().mapToInt(r -> r.getPath().size()).sum();

        List<int[]> pathB = f.rs().bfsRoute(10, 0, 0, 0);
        assertThat(pathB).as("Route B should be blocked by conflict").isNull();
        assertThat(f.routeModel().size()).as("No new route should be added when blocked by conflict").isEqualTo(routeASize);
        assertThat(f.routeModel().getRoutes().values().stream().mapToInt(r -> r.getPath().size()).sum())
            .as("Tile count should be unchanged").isEqualTo(routeATiles);
    }

    @Test
    void removeRouteById() throws Exception {
        var f = setup();

        List<int[]> path = f.rs().bfsRoute(0, 0, 10, 1);
        assertThat(path).isNotNull();
        addRouteToModel(f.routeModel(), f.rs(), f.panel(), path, f.model());
        assertThat(f.routeModel().isEmpty()).as("Route should exist").isFalse();

        String routeId = f.routeModel().getRoutes().keySet().iterator().next();
        f.routeModel().removeRoute(routeId);

        assertThat(f.routeModel().isEmpty()).as("Route should be removed").isTrue();
        assertThat(f.routeModel().size()).isEqualTo(0);
    }

    @Test
    void routeModelClearRemovesAllRoutes() throws Exception {
        var f = setup();

        f.model().setElementAspect("TR-003", 1);
        f.model().setElementAspect("TR-002", 1);

        addRouteToModel(f.routeModel(), f.rs(), f.panel(), f.rs().bfsRoute(0, 0, 10, 1), f.model());
        addRouteToModel(f.routeModel(), f.rs(), f.panel(), f.rs().bfsRoute(2, 3, 10, 5), f.model());

        assertThat(f.routeModel().size()).isEqualTo(2);

        f.routeModel().clear();

        assertThat(f.routeModel().size()).isEqualTo(0);
        assertThat(f.routeModel().isEmpty()).isTrue();
    }

    @Test
    void routePersistenceRoundTrip() throws Exception {
        var f1 = setup();

        f1.model().setElementAspect("TR-003", 1);
        f1.model().setElementAspect("TR-002", 1);

        findAndAddRoute(f1.routeModel(), f1.rs(), f1.panel(), 0, 0, 10, 1, f1.model());
        findAndAddRoute(f1.routeModel(), f1.rs(), f1.panel(), 2, 3, 10, 5, f1.model());

        assertThat(f1.routeModel().size()).isEqualTo(2);

        LayoutData data = new LayoutPersistence().capture(f1.panel());
        assertThat(data.getRoutes()).isNotNull();
        assertThat(data.getRoutes()).hasSize(2);

        RailwayModel model2 = new RailwayModel();
        SwitchboardPanel panel2 = new SwitchboardPanel(occupancyFactory, (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model2);
        new LayoutPersistence().apply(panel2, data);

        assertThat(panel2.getRouteModel().size()).as("Routes should survive round-trip").isEqualTo(2);
        assertThat(panel2.getRouteModel().isEmpty()).isFalse();

        Route r1 = panel2.getRouteModel().getRoute("P-001-P-011");
        assertThat(r1).as("Route P-001-P-011 should exist after load").isNotNull();

        Route r2 = panel2.getRouteModel().getRoute("P-015-P-024");
        assertThat(r2).as("Route P-015-P-024 should exist after load").isNotNull();
    }

    @Test
    void routeModelIsTileReserved() throws Exception {
        var f = setup();

        f.model().setElementAspect("TR-003", 1);
        f.model().setElementAspect("TR-002", 1);

        addRouteToModel(f.routeModel(), f.rs(), f.panel(), f.rs().bfsRoute(0, 0, 10, 1), f.model());

        assertThat(f.routeModel().isTileReserved(5, 0, null)).isTrue();

        assertThat(f.routeModel().isTileReserved(99, 99, null)).isFalse();

        String routeId = f.routeModel().getRoutes().keySet().iterator().next();
        assertThat(f.routeModel().isTileReserved(5, 0, routeId)).isFalse();
    }

    @Test
    void routeIdFormat() throws Exception {
        var f = setup();

        List<int[]> path = f.rs().bfsRoute(0, 0, 10, 1);
        assertThat(path).isNotNull();
        addRouteToModel(f.routeModel(), f.rs(), f.panel(), path, f.model());

        Route route = f.routeModel().getRoutes().values().iterator().next();
        assertThat(route.getId()).isEqualTo("P-001-P-011");
        assertThat(route.getSourceElementId()).isEqualTo("P-001");
        assertThat(route.getTargetElementId()).isEqualTo("P-011");
    }

    @Test
    void routeContainsTile() throws Exception {
        var f = setup();

        List<int[]> path = f.rs().bfsRoute(0, 0, 10, 1);
        assertThat(path).isNotNull();
        addRouteToModel(f.routeModel(), f.rs(), f.panel(), path, f.model());

        Route route = f.routeModel().getRoutes().values().iterator().next();
        assertThat(route.containsTile(0, 0)).isTrue();
        assertThat(route.containsTile(10, 1)).isTrue();
        assertThat(route.containsTile(99, 99)).isFalse();
    }

    @Test
    void alternativeRouteFoundForP015ToP065() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(occupancyFactory, (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model);
        var url = RouteFindingTest.class.getResource("/test-data/switchboard4.json");
        new LayoutPersistence().load(panel, Paths.get(url.toURI()));
        RouterService rs = routerService(panel);

        findAndAddRoute(panel.getRouteModel(), rs, panel, 2, 3, 24, 6, model);

        assertThat(panel.getRouteModel().getRoutes().isEmpty()).isFalse();
        assertThat(panel.getRouteModel().size()).isEqualTo(2);

        String routeId = "P-015-P-065";
        Route r = panel.getRouteModel().getRoute(routeId);
        assertThat(r).isNotNull();
        assertThat(panel.getRouteModel().hasAlternativeRoute(routeId)).isTrue();
        assertThat(panel.getRouteModel().getAlternativeRoutes(routeId)).hasSize(2);
    }

    @Test
    void alternativeRouteFoundForP015ToTL004() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(occupancyFactory, (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model);
        var url = RouteFindingTest.class.getResource("/test-data/switchboard5.json");
        new LayoutPersistence().load(panel, Paths.get(url.toURI()));
        RouterService rs = routerService(panel);

        Route existing = panel.getRouteModel().getRoute("TL-003-S2-003");
        assertThat(existing).isNotNull();

        String srcId = panel.getTile(2, 3).getElementId();
        String dstId = panel.getTile(7, 11).getElementId();
        String routeId = srcId + "-" + dstId;
        if (panel.getRouteModel().getRoute(routeId) != null) {
            panel.getRouteModel().removeRoute(routeId);
        }
        List<int[]> path = rs.bfsRoute(2, 3, 7, 11);
        assertThat(path).isNotNull();
        rs.setRouteAspects(path, model);
        List<List<int[]>> alts = rs.bfsAlternativeRoutes(2, 3, 7, 11, path, true);
        Route route = new Route(srcId, dstId, path);
        for (List<int[]> altPath : alts) {
            panel.getRouteModel().addAlternativeRoute(route.getId(), new Route(srcId, dstId, altPath));
        }
        panel.getRouteModel().addRoute(route);

        assertThat(panel.getRouteModel().isEmpty()).isFalse();

        Route r = panel.getRouteModel().getRoute(routeId);
        assertThat(r).as("Route %s should exist", routeId).isNotNull();

        assertThat(panel.getRouteModel().hasAlternativeRoute(routeId)).as("Route %s should have alternatives", routeId).isTrue();
        assertThat(panel.getRouteModel().getAlternativeRoutes(routeId)).as("Route %s should have 4 alternatives", routeId).hasSize(4);
    }

    @Test
    void alternativeRouteFoundForP114ToP015() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(occupancyFactory, (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model);
        var url = RouteFindingTest.class.getResource("/test-data/switchboard6.json");
        new LayoutPersistence().load(panel, Paths.get(url.toURI()));
        RouterService rs = routerService(panel);
        panel.getRouteModel().clear();

        String srcId = panel.getTile(25, 14).getElementId();
        String dstId = panel.getTile(2, 3).getElementId();
        String routeId = srcId + "-" + dstId;

        LOGGER.info("Search route: {}", routeId);

        if (panel.getRouteModel().getRoute(routeId) != null) {
            panel.getRouteModel().removeRoute(routeId);
        }
        List<int[]> path = rs.bfsRoute(25, 14, 2, 3);
        assertThat(path).isNotNull();
        rs.setRouteAspects(path, model);
        List<List<int[]>> alts = rs.bfsAlternativeRoutes(25, 14, 2, 3, path, true);
        Route route = new Route(srcId, dstId, path);
        for (List<int[]> altPath : alts) {
            panel.getRouteModel().addAlternativeRoute(route.getId(), new Route(srcId, dstId, altPath));
        }
        panel.getRouteModel().addRoute(route);

        assertThat(panel.getRouteModel().isEmpty()).isFalse();

        Route r = panel.getRouteModel().getRoute(routeId);
        assertThat(r).as("Route %s should exist", routeId).isNotNull();

        assertThat(panel.getRouteModel().hasAlternativeRoute(routeId)).as("Route %s should have alternatives", routeId).isTrue();
        assertThat(panel.getRouteModel().getAlternativeRoutes(routeId)).as("Route %s should have 5 alternatives", routeId).hasSize(5);
    }

    @Test
    void undoRouteCreation() throws Exception {
        var f = setup();

        f.panel().testSetRouteSource(0, 0);
        f.panel().testFindRoute(10, 1);

        assertThat(f.panel().getRouteModel().isEmpty()).as("Route should exist after creation").isFalse();
        assertThat(f.panel().hasActiveRoute()).isTrue();

        f.panel().undoLast();

        assertThat(f.panel().getRouteModel().isEmpty()).as("Route should be removed after undo").isTrue();
    }

    @Test
    void undoRouteReplaceRestoresPreviousRoute() throws Exception {
        var f = setup();

        f.panel().testSetRouteSource(0, 0);
        f.panel().testFindRoute(10, 1);
        String routeId = f.panel().getRouteModel().getRoutes().keySet().iterator().next();

        f.panel().testSetRouteSource(0, 0);
        f.panel().testFindRoute(10, 1);

        assertThat(f.panel().getRouteModel().isEmpty()).as("Route should still exist after replacement").isFalse();

        f.panel().undoLast();

        assertThat(f.panel().getRouteModel().isEmpty()).as("Route should still exist after undo of replacement").isFalse();
        assertThat(f.panel().getRouteModel().getRoute(routeId)).as("Original route should be restored").isNotNull();
    }

    @Test
    void undoRouteClearRestoresPreviousRoute() throws Exception {
        var f = setup();

        f.panel().testSetRouteSource(0, 0);
        f.panel().testFindRoute(10, 1);

        assertThat(f.panel().getRouteModel().isEmpty()).as("Route should exist after creation").isFalse();

        f.panel().testTileContextAction(5, 0, null);

        f.panel().testSetRouteSource(0, 0);
        f.panel().testFindRoute(10, 1);

        assertThat(f.panel().getRouteModel().isEmpty()).as("Route should be cleared when no path found").isTrue();

        f.panel().undoLast();

        assertThat(f.panel().getRouteModel().isEmpty()).as("Route should be restored after undo of cleared route").isFalse();
    }

    @Test
    void undoTileCreationOnEmptyCell() throws Exception {
        var f = setup();

        int col = 15;
        int row = 5;
        Tile before = f.panel().getTile(col, row);
        assertThat(before).as("Cell should be empty initially").isNull();

        f.panel().testTileContextAction(col, row, ElementType.STRAIGHT);

        Tile afterCreate = f.panel().getTile(col, row);
        assertThat(afterCreate).as("Tile should exist after creation").isNotNull();
        assertThat(afterCreate.getElementId()).as("Tile should have an element ID").isNotNull();
        assertThat(f.model().getElement(afterCreate.getElementId())).as("Element should exist in model").isNotNull();

        f.panel().undoLast();

        assertThat(f.panel().getTile(col, row)).as("Cell should be empty after undo").isNull();
        assertThat(f.model().getElement(afterCreate.getElementId())).as("Element should be removed from model after undo").isNull();
    }

    @Test
    void undoTileReplaceRestoresOriginalTile() throws Exception {
        var f = setup();

        int col = 0;
        int row = 0;
        Tile original = f.panel().getTile(col, row);
        assertThat(original).as("Cell should have a tile initially").isNotNull();
        String originalId = original.getElementId();

        f.panel().testTileContextAction(col, row, ElementType.STRAIGHT);

        Tile replaced = f.panel().getTile(col, row);
        assertThat(replaced.getElementId()).as("Tile should be replaced").isNotEqualTo(originalId);

        f.panel().undoLast();

        Tile restored = f.panel().getTile(col, row);
        assertThat(restored).as("Original tile should be restored after undo").isNotNull();
        assertThat(restored.getElementId()).as("Original element ID should be restored").isEqualTo(originalId);
        assertThat(f.model().getElement(originalId)).as("Original element should exist in model after undo").isNotNull();
    }

    @Test
    void occupiedTileOnRouteIsDetected() throws Exception {
        var f = setup();

        f.panel().testSetRouteSource(0, 0);
        f.panel().testFindRoute(10, 1);

        List<int[]> path = f.panel().getRouteModel().getRoutes().values().iterator().next().getPath();
        int[] targetTile = path.get(path.size() / 2);
        Tile tile = f.panel().getTile(targetTile[0], targetTile[1]);
        assertThat(tile).isInstanceOf(ElementTile.class);
        String elId = tile.getElementId();
        Element el = f.model().getElement(elId);
        assertThat(el).isNotNull();

        assertThat(el.getOccupancy()).as("Should have no occupancy initially").isNull();

        Occupancy occ = occupancyFactory.create(Occupancy.OccupancyState.OCCUPIED);
        f.model().addOccupancy(occ);
        el.setOccupancy(occ);

        assertThat(f.panel().isTileOccupied(targetTile[0], targetTile[1])).isTrue();

        el.setOccupancy(null);

        assertThat(f.panel().isTileOccupied(targetTile[0], targetTile[1])).isFalse();
    }

    @Test
    void routeFromP112ToCL013WithAndWithoutPreExistingRoutes() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(occupancyFactory, (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model);
        var url = RouteFindingTest.class.getResource("/test-data/switchboard6.json");
        new LayoutPersistence().load(panel, Paths.get(url.toURI()));
        RouterService rs = routerService(panel);

        List<int[]> pathWithRoutes = rs.bfsRoute(25, 12, 24, 13);
        LOGGER.info("With pre-existing routes: path={}",
            pathWithRoutes != null ? pathWithRoutes.stream().map(p -> "(" + p[0] + "," + p[1] + ")").toList() : "null");

        panel.getRouteModel().clear();
        List<int[]> pathAfterClear = rs.bfsRoute(25, 12, 24, 13);
        LOGGER.info("After clearing routes: path={}",
            pathAfterClear != null ? pathAfterClear.stream().map(p -> "(" + p[0] + "," + p[1] + ")").toList() : "null");

        assertThat(pathWithRoutes).as("Route should be found with pre-existing routes").isNotNull();
        assertThat(pathAfterClear).as("Route should be found after clearing pre-existing routes").isNotNull();
    }

    @Test
    void routeFromP114ToP137MustNotUseInvalidTurnoutPath() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(occupancyFactory, (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model);
        var url = RouteFindingTest.class.getResource("/test-data/switchboard6.json");
        new LayoutPersistence().load(panel, Paths.get(url.toURI()));
        RouterService rs = routerService(panel);

        panel.getRouteModel().clear();

        List<int[]> path = rs.bfsRoute(25, 14, 17, 12);
        assertThat(path).isNotNull();

        for (int i = 0; i < path.size() - 1; i++) {
            int[] a = path.get(i);
            int[] b = path.get(i + 1);
            boolean isViaInvalidTurnout = a[0] == 25 && a[1] == 13 && b[0] == 24 && b[1] == 14;
            assertThat(isViaInvalidTurnout)
                .as("Route must not go via (25,13)->(24,14) — TR-009 can only route from left to diverted when not rotated")
                .isFalse();
        }
    }

    @Test
    void directedStraightTileBlocksReverseRoute() throws Exception {
        var f = setup();
        // Tile at (1,0) is P-002 (STRAIGHT, rotation 0). Set it to FORWARD (LEFT→RIGHT).
        Tile tile = f.panel().getTile(1, 0);
        tile.setDirection(TileDirection.FORWARD);

        // Route from (2,0) to (0,0) would need to go RIGHT→LEFT through (1,0) — blocked
        RouterService rs = routerService(f.panel());
        List<int[]> path = rs.bfsRoute(2, 0, 0, 0);
        assertThat(path).as("Route should be blocked by FORWARD direction on (1,0)").isNull();
    }

    @Test
    void directedStraightTileAllowsForwardRoute() throws Exception {
        var f = setup();
        // Tile at (1,0) is P-002 (STRAIGHT, rotation 0). Set it to FORWARD (LEFT→RIGHT).
        Tile tile = f.panel().getTile(1, 0);
        tile.setDirection(TileDirection.FORWARD);

        // Route from (0,0) to (2,0) goes LEFT→RIGHT through (1,0) — allowed
        RouterService rs = routerService(f.panel());
        List<int[]> path = rs.bfsRoute(0, 0, 2, 0);
        assertThat(path).as("Route should be found in FORWARD direction through (1,0)").isNotNull();
        assertThat(path).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void directionPersistenceRoundTrip() throws Exception {
        var f = setup();
        Tile tile = f.panel().getTile(1, 0);
        tile.setDirection(TileDirection.FORWARD);

        LayoutData data = new LayoutPersistence().capture(f.panel());

        RailwayModel model2 = new RailwayModel();
        SwitchboardPanel panel2 = new SwitchboardPanel(occupancyFactory,
            (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), model2);
        new LayoutPersistence().apply(panel2, data);

        Tile loaded = panel2.getTile(1, 0);
        assertThat(loaded.getDirection()).as("Direction should survive round-trip").isEqualTo(TileDirection.FORWARD);
    }

    @Test
    void signalAtRedBlocksSimulation() throws Exception {
        var f = setup();
        // S2-002 is at (6, 0) — it's a SIGNAL_2
        Tile signalTile = f.panel().getTile(6, 0);
        assertThat(signalTile).isNotNull();

        // Default aspect is 0 (red)
        assertThat(f.panel().isSignalAtRed(signalTile)).as("Signal at aspect 0 should be red").isTrue();

        // Change to aspect 1 (green)
        f.model().setElementAspect("S2-002", 1);
        assertThat(f.panel().isSignalAtRed(signalTile)).as("Signal at aspect 1 should not be red").isFalse();
    }

    @Test
    void nonSignalTileIsNotRedSignal() throws Exception {
        var f = setup();
        // P-001 is at (0, 0) — STRAIGHT tile
        Tile straightTile = f.panel().getTile(0, 0);
        assertThat(f.panel().isSignalAtRed(straightTile)).as("Straight tile should not be a red signal").isFalse();
    }
}
