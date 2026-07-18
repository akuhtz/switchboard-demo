package org.bidib.switchboard.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.bidib.switchboard.model.Element;
import org.bidib.switchboard.model.ElementTile;
import org.bidib.switchboard.model.ElementType;
import org.bidib.switchboard.model.Occupancy;
import org.bidib.switchboard.model.RailwayModel;
import org.bidib.switchboard.model.Route;
import org.bidib.switchboard.model.RouteModel;
import org.bidib.switchboard.model.Tile;
import org.bidib.switchboard.persistence.LayoutData;
import org.bidib.switchboard.persistence.LayoutPersistence;
import org.bidib.switchboard.service.RouterService;

class RouteFindingTest {

    private static Path testLayout() throws Exception {
        var url = RouteFindingTest.class.getResource("/test-data/switchboard3.json");
        return Paths.get(url.toURI());
    }

    private static RouterService routerService(SwitchboardPanel panel) {
        return new RouterService(panel.getTiles(), panel.getCols(), panel.getRows(), panel.getRouteModel());
    }

    private static void addRouteToModel(RouteModel routeModel, RouterService routerService,
            SwitchboardPanel panel, List<int[]> path, RailwayModel model) {
        String srcId = panel.getTile(path.get(0)[0], path.get(0)[1]).getElementId();
        String dstId = panel.getTile(path.get(path.size() - 1)[0], path.get(path.size() - 1)[1]).getElementId();
        routerService.setRouteAspects(path, model);
        routeModel.addRoute(new Route(srcId, dstId, path));
    }

    private static void findAndAddRoute(RouteModel routeModel, RouterService routerService,
            SwitchboardPanel panel, int srcCol, int srcRow, int dstCol, int dstRow, RailwayModel model) {
        String srcId = panel.getTile(srcCol, srcRow).getElementId();
        String dstId = panel.getTile(dstCol, dstRow).getElementId();
        String routeId = srcId + "-" + dstId;
        if (routeModel.getRoute(routeId) != null) {
            routeModel.removeRoute(routeId);
        }
        List<int[]> path = routerService.bfsRoute(srcCol, srcRow, dstCol, dstRow);
        if (path == null) return;
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
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());
        RouterService rs = routerService(panel);

        model.setElementAspect("TR-003", 1);
        model.setElementAspect("TR-002", 1);

        List<int[]> path = rs.bfsRoute(0, 0, 10, 1);
        assertThat(path).as("Route should be found from (0,0) to (10,1) through diverted turnouts").isNotNull();
        assertThat(path.size() > 0).as("Route should contain at least one tile").isTrue();

        rs.setRouteAspects(path, model);
        int tr003aspect = model.getElementAspect("TR-003");
        assertThat(tr003aspect).as("TR-003 should be set to diverted (aspect=1)").isEqualTo(1);
    }

    @Test
    void routeFromRow3Col2ToRow5Col10() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());
        RouterService rs = routerService(panel);

        List<int[]> path = rs.bfsRoute(2, 3, 10, 5);
        assertThat(path).as("Route should be found from (2,3) to (10,5)").isNotNull();
        assertThat(path.size() > 0).as("Route should contain at least one tile").isTrue();
    }

    @Test
    void routeFromRow3Col2ToRow4Col10() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());
        RouterService rs = routerService(panel);

        List<int[]> path = rs.bfsRoute(2, 3, 10, 4);
        assertThat(path).as("Route should be found from (2,3) to (10,5)").isNotNull();
        assertThat(path.size() > 0).as("Route should contain at least one tile").isTrue();
    }

    @Test
    void routeFromRow3Col2ToRow0Col10() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());
        RouterService rs = routerService(panel);

        List<int[]> path = rs.bfsRoute(2, 3, 10, 0);
        assertThat(path).as("Route should NOT be found from (2,3) to (10,0)").isNull();
    }

    @Test
    void routeFromRow1Col10ToRow3Col2() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());
        RouterService rs = routerService(panel);

        List<int[]> path = rs.bfsRoute(10, 1, 2, 3);
        assertThat(path).as("Route should be found from (10,1) to (2,3)").isNotNull();
        assertThat(path.size() > 0).as("Route should contain at least one tile").isTrue();
    }

    @Test
    void twoNonOverlappingRoutesCoexist() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());
        RouterService rs = routerService(panel);
        RouteModel routeModel = panel.getRouteModel();

        model.setElementAspect("TR-003", 1);
        model.setElementAspect("TR-002", 1);

        List<int[]> pathA = rs.bfsRoute(0, 0, 10, 1);
        assertThat(pathA).isNotNull();
        addRouteToModel(routeModel, rs, panel, pathA, model);
        assertThat(routeModel.size()).as("One route should exist after first find").isEqualTo(1);

        List<int[]> pathB = rs.bfsRoute(2, 3, 10, 5);
        assertThat(pathB).isNotNull();
        addRouteToModel(routeModel, rs, panel, pathB, model);
        assertThat(routeModel.size()).as("Two routes should coexist").isEqualTo(2);

        int totalTiles = routeModel.getRoutes().values().stream().mapToInt(r -> r.getPath().size()).sum();
        assertThat(totalTiles >= 4).as("Two routes should cover \u22654 tiles combined").isTrue();
    }

    @Test
    void routeConflictBlocksOverlappingRoute() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());
        RouterService rs = routerService(panel);
        RouteModel routeModel = panel.getRouteModel();

        model.setElementAspect("TR-003", 1);
        model.setElementAspect("TR-002", 1);

        List<int[]> pathA = rs.bfsRoute(0, 0, 10, 1);
        assertThat(pathA).isNotNull();
        addRouteToModel(routeModel, rs, panel, pathA, model);
        int routeASize = routeModel.size();
        int routeATiles = routeModel.getRoutes().values().stream().mapToInt(r -> r.getPath().size()).sum();

        List<int[]> pathB = rs.bfsRoute(10, 0, 0, 0);
        assertThat(pathB).as("Route B should be blocked by conflict").isNull();
        assertThat(routeModel.size()).as("No new route should be added when blocked by conflict").isEqualTo(routeASize);
        assertThat(routeModel.getRoutes().values().stream().mapToInt(r -> r.getPath().size()).sum())
                .as("Tile count should be unchanged").isEqualTo(routeATiles);
    }

    @Test
    void removeRouteById() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());
        RouterService rs = routerService(panel);
        RouteModel routeModel = panel.getRouteModel();

        List<int[]> path = rs.bfsRoute(0, 0, 10, 1);
        assertThat(path).isNotNull();
        addRouteToModel(routeModel, rs, panel, path, model);
        assertThat(routeModel.isEmpty()).as("Route should exist").isFalse();

        String routeId = routeModel.getRoutes().keySet().iterator().next();
        routeModel.removeRoute(routeId);

        assertThat(routeModel.isEmpty()).as("Route should be removed").isTrue();
        assertThat(routeModel.size()).isEqualTo(0);
    }

    @Test
    void routeModelClearRemovesAllRoutes() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());
        RouterService rs = routerService(panel);
        RouteModel routeModel = panel.getRouteModel();

        model.setElementAspect("TR-003", 1);
        model.setElementAspect("TR-002", 1);

        addRouteToModel(routeModel, rs, panel, rs.bfsRoute(0, 0, 10, 1), model);
        addRouteToModel(routeModel, rs, panel, rs.bfsRoute(2, 3, 10, 5), model);

        assertThat(routeModel.size()).isEqualTo(2);

        routeModel.clear();

        assertThat(routeModel.size()).isEqualTo(0);
        assertThat(routeModel.isEmpty()).isTrue();
    }

    @Test
    void routePersistenceRoundTrip() throws Exception {
        RailwayModel model1 = new RailwayModel();
        SwitchboardPanel panel1 = new SwitchboardPanel(model1);
        LayoutPersistence.load(panel1, testLayout());
        RouterService rs1 = routerService(panel1);

        model1.setElementAspect("TR-003", 1);
        model1.setElementAspect("TR-002", 1);

        findAndAddRoute(panel1.getRouteModel(), rs1, panel1, 0, 0, 10, 1, model1);
        findAndAddRoute(panel1.getRouteModel(), rs1, panel1, 2, 3, 10, 5, model1);

        assertThat(panel1.getRouteModel().size()).isEqualTo(2);

        LayoutData data = LayoutPersistence.capture(panel1);
        assertThat(data.getRoutes()).isNotNull();
        assertThat(data.getRoutes()).hasSize(2);

        RailwayModel model2 = new RailwayModel();
        SwitchboardPanel panel2 = new SwitchboardPanel(model2);
        LayoutPersistence.apply(panel2, data);

        assertThat(panel2.getRouteModel().size()).as("Routes should survive round-trip").isEqualTo(2);
        assertThat(panel2.getRouteModel().isEmpty()).isFalse();

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
        RouterService rs = routerService(panel);
        RouteModel routeModel = panel.getRouteModel();

        model.setElementAspect("TR-003", 1);
        model.setElementAspect("TR-002", 1);

        addRouteToModel(routeModel, rs, panel, rs.bfsRoute(0, 0, 10, 1), model);

        assertThat(routeModel.isTileReserved(5, 0, null)).isTrue();

        assertThat(routeModel.isTileReserved(99, 99, null)).isFalse();

        String routeId = routeModel.getRoutes().keySet().iterator().next();
        assertThat(routeModel.isTileReserved(5, 0, routeId)).isFalse();
    }

    @Test
    void routeIdFormat() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());
        RouterService rs = routerService(panel);

        List<int[]> path = rs.bfsRoute(0, 0, 10, 1);
        assertThat(path).isNotNull();
        addRouteToModel(panel.getRouteModel(), rs, panel, path, model);

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
        RouterService rs = routerService(panel);

        List<int[]> path = rs.bfsRoute(0, 0, 10, 1);
        assertThat(path).isNotNull();
        addRouteToModel(panel.getRouteModel(), rs, panel, path, model);

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
        RouterService rs = routerService(panel);
        RouteModel routeModel = panel.getRouteModel();

        findAndAddRoute(routeModel, rs, panel, 2, 3, 24, 6, model);

        assertThat(routeModel.getRoutes().isEmpty()).isFalse();
        assertThat(routeModel.size()).isEqualTo(2);

        String routeId = "P-015-P-065";
        Route r = routeModel.getRoute(routeId);
        assertThat(r).isNotNull();
        assertThat(routeModel.hasAlternativeRoute(routeId)).isTrue();
        assertThat(routeModel.getAlternativeRoutes(routeId)).hasSize(2);
    }

    @Test
    void alternativeRouteFoundForP015ToTL004() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        var url = RouteFindingTest.class.getResource("/test-data/switchboard5.json");
        LayoutPersistence.load(panel, Paths.get(url.toURI()));
        RouterService rs = routerService(panel);
        RouteModel routeModel = panel.getRouteModel();

        Route existing = routeModel.getRoute("TL-003-S2-003");
        assertThat(existing).isNotNull();

        String srcId = panel.getTile(2, 3).getElementId();
        String dstId = panel.getTile(7, 11).getElementId();
        String routeId = srcId + "-" + dstId;
        if (routeModel.getRoute(routeId) != null) {
            routeModel.removeRoute(routeId);
        }
        List<int[]> path = rs.bfsRoute(2, 3, 7, 11);
        assertThat(path).isNotNull();
        rs.setRouteAspects(path, model);
        List<List<int[]>> alts = rs.bfsAlternativeRoutes(2, 3, 7, 11, path, true);
        Route route = new Route(srcId, dstId, path);
        for (List<int[]> altPath : alts) {
            routeModel.addAlternativeRoute(route.getId(), new Route(srcId, dstId, altPath));
        }
        routeModel.addRoute(route);

        assertThat(routeModel.isEmpty()).isFalse();

        Route r = routeModel.getRoute(routeId);
        assertThat(r).as("Route %s should exist", routeId).isNotNull();

        assertThat(routeModel.hasAlternativeRoute(routeId))
            .as("Route %s should have alternatives", routeId).isTrue();
        assertThat(routeModel.getAlternativeRoutes(routeId))
            .as("Route %s should have 4 alternatives", routeId).hasSize(4);
    }

    @Test
    void undoRouteCreation() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);

        assertThat(panel.getRouteModel().isEmpty()).as("Route should exist after creation").isFalse();
        assertThat(panel.hasActiveRoute()).isTrue();

        panel.undoLast();

        assertThat(panel.getRouteModel().isEmpty()).as("Route should be removed after undo").isTrue();
    }

    @Test
    void undoRouteReplaceRestoresPreviousRoute() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);
        String routeId = panel.getRouteModel().getRoutes().keySet().iterator().next();

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);

        assertThat(panel.getRouteModel().isEmpty()).as("Route should still exist after replacement").isFalse();

        panel.undoLast();

        assertThat(panel.getRouteModel().isEmpty()).as("Route should still exist after undo of replacement").isFalse();
        assertThat(panel.getRouteModel().getRoute(routeId)).as("Original route should be restored").isNotNull();
    }

    @Test
    void undoRouteClearRestoresPreviousRoute() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);

        assertThat(panel.getRouteModel().isEmpty()).as("Route should exist after creation").isFalse();

        panel.testTileContextAction(5, 0, null);

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);

        assertThat(panel.getRouteModel().isEmpty()).as("Route should be cleared when no path found").isTrue();

        panel.undoLast();

        assertThat(panel.getRouteModel().isEmpty()).as("Route should be restored after undo of cleared route").isFalse();
    }

    @Test
    void undoTileCreationOnEmptyCell() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());

        int col = 15;
        int row = 5;
        Tile before = panel.getTile(col, row);
        assertThat(before).as("Cell should be empty initially").isNull();

        panel.testTileContextAction(col, row, ElementType.STRAIGHT);

        Tile afterCreate = panel.getTile(col, row);
        assertThat(afterCreate).as("Tile should exist after creation").isNotNull();
        assertThat(afterCreate.getElementId()).as("Tile should have an element ID").isNotNull();
        assertThat(model.getElement(afterCreate.getElementId())).as("Element should exist in model").isNotNull();

        panel.undoLast();

        assertThat(panel.getTile(col, row)).as("Cell should be empty after undo").isNull();
        assertThat(model.getElement(afterCreate.getElementId())).as("Element should be removed from model after undo").isNull();
    }

    @Test
    void undoTileReplaceRestoresOriginalTile() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());

        int col = 0;
        int row = 0;
        Tile original = panel.getTile(col, row);
        assertThat(original).as("Cell should have a tile initially").isNotNull();
        String originalId = original.getElementId();

        panel.testTileContextAction(col, row, ElementType.STRAIGHT);

        Tile replaced = panel.getTile(col, row);
        assertThat(replaced.getElementId()).as("Tile should be replaced").isNotEqualTo(originalId);

        panel.undoLast();

        Tile restored = panel.getTile(col, row);
        assertThat(restored).as("Original tile should be restored after undo").isNotNull();
        assertThat(restored.getElementId()).as("Original element ID should be restored").isEqualTo(originalId);
        assertThat(model.getElement(originalId)).as("Original element should exist in model after undo").isNotNull();
    }

    @Test
    void occupiedTileOnRouteIsDetected() throws Exception {
        RailwayModel model = new RailwayModel();
        SwitchboardPanel panel = new SwitchboardPanel(model);
        LayoutPersistence.load(panel, testLayout());

        panel.testSetRouteSource(0, 0);
        panel.testFindRoute(10, 1);

        List<int[]> path = panel.getRouteModel().getRoutes().values().iterator().next().getPath();
        int[] targetTile = path.get(path.size() / 2);
        Tile tile = panel.getTile(targetTile[0], targetTile[1]);
        assertThat(tile).isInstanceOf(ElementTile.class);
        String elId = ((ElementTile) tile).getElementId();
        Element el = model.getElement(elId);
        assertThat(el).isNotNull();

        assertThat(el.getOccupancy()).as("Should have no occupancy initially").isNull();

        Occupancy occ = Occupancy.create(1, 1, Occupancy.OccupancyState.OCCUPIED);
        model.addOccupancy(occ);
        el.setOccupancy(occ);

        assertThat(panel.isTileOccupied(targetTile[0], targetTile[1])).isTrue();

        el.setOccupancy(null);

        assertThat(panel.isTileOccupied(targetTile[0], targetTile[1])).isFalse();
    }
}
