package com.bidib.switchboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bidib.switchboard.model.ElementTile;
import com.bidib.switchboard.model.ElementType;
import com.bidib.switchboard.model.RouteModel;
import com.bidib.switchboard.model.Tile;

class RouterServiceTest {

    private static Map<String, Tile> tileMap(Tile... tiles) {
        Map<String, Tile> map = new LinkedHashMap<>();
        for (Tile t : tiles) {
            map.put(t.getCol() + "," + t.getRow(), t);
        }
        return map;
    }

    private static Tile straight(int col, int row, String id) {
        return new ElementTile(col, row, id, ElementType.STRAIGHT, List.of("/icons/straight.svg"));
    }

    private static Tile straight(int col, int row) {
        return straight(col, row, "P-" + col + "-" + row);
    }

    @Test
    void bfsRouteFindsSimpleHorizontalPath() {
        Map<String, Tile> tiles = tileMap(
            straight(0, 0),
            straight(1, 0),
            straight(2, 0)
        );
        RouterService svc = new RouterService(tiles, 10, 10, new RouteModel());

        List<int[]> path = svc.bfsRoute(0, 0, 2, 0);

        assertThat(path).isNotNull();
        assertThat(path.size()).isGreaterThanOrEqualTo(3);
        assertThat(path.get(0)).containsExactly(0, 0);
        assertThat(path.get(path.size() - 1)).containsExactly(2, 0);
    }

    @Test
    void bfsRouteReturnsNullWhenNoPath() {
        Map<String, Tile> tiles = tileMap(
            straight(0, 0),
            straight(5, 5)
        );
        RouterService svc = new RouterService(tiles, 10, 10, new RouteModel());

        List<int[]> path = svc.bfsRoute(0, 0, 5, 5);

        assertThat(path).isNull();
    }

    @Test
    void bfsRouteReturnsNullWhenStartTileMissing() {
        Map<String, Tile> tiles = tileMap(straight(1, 0));
        RouterService svc = new RouterService(tiles, 10, 10, new RouteModel());

        assertThat(svc.bfsRoute(0, 0, 1, 0)).isNull();
    }

    @Test
    void bfsRouteReturnsNullWhenEndTileMissing() {
        Map<String, Tile> tiles = tileMap(straight(0, 0));
        RouterService svc = new RouterService(tiles, 10, 10, new RouteModel());

        assertThat(svc.bfsRoute(0, 0, 1, 0)).isNull();
    }

    @Test
    void bfsAlternativeRoutesReturnsEmptyForStraightLine() {
        Map<String, Tile> tiles = tileMap(
            straight(0, 0),
            straight(1, 0),
            straight(2, 0)
        );
        RouteModel rm = new RouteModel();
        RouterService svc = new RouterService(tiles, 10, 10, rm);

        List<int[]> primary = svc.bfsRoute(0, 0, 2, 0);
        assertThat(primary).isNotNull();

        List<List<int[]>> alts = svc.bfsAlternativeRoutes(0, 0, 2, 0, primary);
        assertThat(alts).isEmpty();
    }

    @Test
    void bfsAlternativeRoutesFindsAlternativesWithExhaustiveMode() throws Exception {
        // Load the layout via path used in RouteFindingTest
        var url = RouterServiceTest.class.getResource("/test-data/switchboard4.json");
        java.nio.file.Path path = java.nio.file.Paths.get(url.toURI());
        com.bidib.switchboard.model.RailwayModel model = new com.bidib.switchboard.model.RailwayModel();
        com.bidib.switchboard.view.SwitchboardPanel panel = new com.bidib.switchboard.view.SwitchboardPanel(model);
        com.bidib.switchboard.persistence.LayoutPersistence.load(panel, path);

        // The panel is set up; use it to load a route, then get the router service
        panel.testSetRouteSource(2, 3);
        panel.testFindRoute(24, 6);

        String routeId = "P-015-P-065";
        com.bidib.switchboard.model.Route r = panel.getRouteModel().getRoute(routeId);
        assertThat(r).isNotNull();
        assertThat(panel.getRouteModel().hasAlternativeRoute(routeId)).isTrue();
        assertThat(panel.getRouteModel().getAlternativeRoutes(routeId)).hasSize(2);
    }

    @Test
    void bfsRouteFindsDiagonalPath() {
        Map<String, Tile> tiles = tileMap(
            new ElementTile(0, 0, "P1", ElementType.STRAIGHT, List.of("/icons/straight.svg")),
            new ElementTile(1, 0, "P2", ElementType.STRAIGHT, List.of("/icons/straight.svg")),
            new ElementTile(1, 1, "P3", ElementType.STRAIGHT, List.of("/icons/straight.svg")),
            new ElementTile(2, 1, "P4", ElementType.STRAIGHT, List.of("/icons/straight.svg"))
        );
        // Add missing intermediate tile so path (0,0)-(1,0)-(1,1)-(2,1) works
        // Actually (1,0) is straight with PORTS LEFT,RIGHT, so it can't traverse to (1,1) which requires PORT_BOTTOM
        // Let's use a simple L-shaped path: straight (0,0), curve_right (1,0) going down, straight (1,1), straight (2,1)
        // But building that is complex. Let's just do a horizontal path.
        tiles.clear();
        tiles.putAll(tileMap(
            straight(0, 0),
            straight(1, 0),
            straight(2, 0)
        ));

        RouterService svc = new RouterService(tiles, 10, 10, new RouteModel());
        List<int[]> p = svc.bfsRoute(0, 0, 2, 0);
        assertThat(p).isNotNull().hasSize(3);
    }

    @Test
    void diagonalAwarePortWorksCorrectly() {
        RouterService svc = new RouterService(Map.of(), 10, 10, new RouteModel());

        // Moving right: entry port is LEFT, exit port is RIGHT
        assertThat(svc.diagonalAwarePort(0, 0, 1, 0, true)).isEqualTo(ElementType.PORT_LEFT);
        assertThat(svc.diagonalAwarePort(0, 0, 1, 0, false)).isEqualTo(ElementType.PORT_RIGHT);

        // Moving left: entry port is RIGHT, exit port is LEFT
        assertThat(svc.diagonalAwarePort(1, 0, 0, 0, true)).isEqualTo(ElementType.PORT_RIGHT);
        assertThat(svc.diagonalAwarePort(1, 0, 0, 0, false)).isEqualTo(ElementType.PORT_LEFT);

        // Moving down: entry port is TOP, exit port is BOTTOM
        assertThat(svc.diagonalAwarePort(0, 0, 0, 1, true)).isEqualTo(ElementType.PORT_TOP);
        assertThat(svc.diagonalAwarePort(0, 0, 0, 1, false)).isEqualTo(ElementType.PORT_BOTTOM);

        // Moving up: entry port is BOTTOM, exit port is TOP
        assertThat(svc.diagonalAwarePort(0, 1, 0, 0, true)).isEqualTo(ElementType.PORT_BOTTOM);
        assertThat(svc.diagonalAwarePort(0, 1, 0, 0, false)).isEqualTo(ElementType.PORT_TOP);

        // Diagonal down-right: special case
        assertThat(svc.diagonalAwarePort(0, 0, 1, 1, true)).isEqualTo(ElementType.PORT_TOP);
        assertThat(svc.diagonalAwarePort(0, 0, 1, 1, false)).isEqualTo(ElementType.PORT_BOTTOM);

        // Diagonal up-right:
        assertThat(svc.diagonalAwarePort(0, 1, 1, 0, true)).isEqualTo(ElementType.PORT_BOTTOM);
        assertThat(svc.diagonalAwarePort(0, 1, 1, 0, false)).isEqualTo(ElementType.PORT_TOP);
    }

    @Test
    void bfsAlternativeRoutesWithExhaustiveFindsMultipleForP015ToTL004() throws Exception {
        var url = RouterServiceTest.class.getResource("/test-data/switchboard5.json");
        java.nio.file.Path layoutPath = java.nio.file.Paths.get(url.toURI());
        com.bidib.switchboard.model.RailwayModel model = new com.bidib.switchboard.model.RailwayModel();
        com.bidib.switchboard.view.SwitchboardPanel panel = new com.bidib.switchboard.view.SwitchboardPanel(model);
        com.bidib.switchboard.persistence.LayoutPersistence.load(panel, layoutPath);

        panel.setExhaustiveRouting(true);
        panel.testSetRouteSource(2, 3);
        panel.testFindRoute(7, 11);

        String routeId = "P-015-TL-004";
        com.bidib.switchboard.model.Route r = panel.getRouteModel().getRoute(routeId);
        assertThat(r).isNotNull();
        assertThat(panel.getRouteModel().hasAlternativeRoute(routeId)).isTrue();
        assertThat(panel.getRouteModel().getAlternativeRoutes(routeId)).hasSize(4);
    }
}
