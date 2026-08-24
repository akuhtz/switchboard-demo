package org.bidib.switchboard.component.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.config.TestOccupancyFactory;
import org.bidib.switchboard.component.model.Block;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.persistence.LayoutData;
import org.bidib.switchboard.component.persistence.LayoutPersistence;
import org.bidib.switchboard.component.service.RouterService;
import org.junit.jupiter.api.Test;

class BlockTest {

    private final OccupancyFactory occupancyFactory = new TestOccupancyFactory();

    private SwitchboardPanel newPanel() {
        return new SwitchboardPanel(occupancyFactory,
            (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el), new RailwayModel(), RouterService.createDefault());
    }

    private static RouterService routerService(SwitchboardPanel panel) {
        return new RouterService(panel.getTiles(), panel.getCols(), panel.getRows(), panel.getRouteModel());
    }

    /** Loads the switchboard3 test layout; row 0 (cols 0-6) is a turnout-free straight line. */
    private SwitchboardPanel setup() throws Exception {
        SwitchboardPanel panel = newPanel();
        var url = BlockTest.class.getResource("/test-data/switchboard3.json");
        new LayoutPersistence().load(panel, java.nio.file.Paths.get(url.toURI()));
        return panel;
    }

    @Test
    void blockCreatedFromStartAndEnd() throws Exception {
        SwitchboardPanel panel = setup();
        panel.testSetBlockStart(0, 0);

        Block block = panel.testCreateBlock(6, 0);

        assertThat(block).isNotNull();
        assertThat(block.getId()).isEqualTo("blk001");
        assertThat(block.getName()).isEqualTo("blk001");
        assertThat(block.size()).isEqualTo(7);
        assertThat(panel.getBlockModel().size()).isEqualTo(1);
        assertThat(panel.getBlockModel().blockIdForTile(3, 0)).isEqualTo("blk001");
        assertThat(panel.getBlockModel().blockIdForTile(0, 0)).isEqualTo("blk001");
    }

    @Test
    void blockIdIncrementsZeroPadded() throws Exception {
        SwitchboardPanel panel = setup();

        panel.testSetBlockStart(0, 0);
        Block first = panel.testCreateBlock(3, 0);
        assertThat(first.getId()).isEqualTo("blk001");

        panel.testSetBlockStart(4, 0);
        Block second = panel.testCreateBlock(6, 0);
        assertThat(second.getId()).isEqualTo("blk002");
        assertThat(second.getName()).isEqualTo("blk002");
        assertThat(panel.getBlockModel().size()).isEqualTo(2);
    }

    @Test
    void blockCannotPassThroughTurnout() throws Exception {
        SwitchboardPanel panel = setup();
        // (7,0) is TR-003 (turnout) — path must not include it
        panel.testSetBlockStart(0, 0);

        Block block = panel.testCreateBlock(7, 0);

        assertThat(block).as("block ending at a turnout must not be created").isNull();
        assertThat(panel.getBlockModel().isEmpty()).isTrue();
    }

    @Test
    void blockPathBfsExcludesTurnoutTiles() throws Exception {
        SwitchboardPanel panel = setup();
        RouterService rs = routerService(panel);

        // Row 0 straight line from (0,0) to (6,0) contains no turnout
        List<int[]> path = rs.bfsBlockPath(0, 0, 6, 0, Set.of());
        assertThat(path).isNotNull();
        assertThat(path.size()).isEqualTo(7);
        assertThat(path).extracting(p -> p[0]).containsExactly(0, 1, 2, 3, 4, 5, 6);
    }

    @Test
    void blockPathAvoidsExcludedTiles() throws Exception {
        SwitchboardPanel panel = setup();
        RouterService rs = routerService(panel);

        // Exclude (3,0) and (4,0) to force the search to give up
        Set<String> excluded = new HashSet<>();
        excluded.add("3,0");
        excluded.add("4,0");
        List<int[]> path = rs.bfsBlockPath(0, 0, 6, 0, excluded);
        assertThat(path).isNull();
    }

    @Test
    void tileBelongsToOnlyOneBlock() throws Exception {
        SwitchboardPanel panel = setup();

        panel.testSetBlockStart(0, 0);
        Block first = panel.testCreateBlock(3, 0);
        assertThat(first).isNotNull();

        // Overlapping block must be rejected
        panel.testSetBlockStart(2, 0);
        Block overlap = panel.testCreateBlock(4, 0);
        assertThat(overlap).as("overlapping block must not be created").isNull();

        // Non-overlapping block is allowed
        panel.testSetBlockStart(5, 0);
        Block disjoint = panel.testCreateBlock(6, 0);
        assertThat(disjoint).isNotNull();
        assertThat(panel.getBlockModel().size()).isEqualTo(2);
    }

    @Test
    void renameBlockUpdatesName() throws Exception {
        SwitchboardPanel panel = setup();

        panel.testSetBlockStart(0, 0);
        Block block = panel.testCreateBlock(3, 0);
        assertThat(block).isNotNull();

        panel.getBlockModel().renameBlock("blk001", "Main line");
        assertThat(panel.getBlockModel().getBlock("blk001").getName()).isEqualTo("Main line");
        assertThat(panel.getBlockModel().getBlock("blk001").getId()).isEqualTo("blk001");
    }

    @Test
    void removeBlockReleasesTiles() throws Exception {
        SwitchboardPanel panel = setup();

        panel.testSetBlockStart(0, 0);
        Block block = panel.testCreateBlock(3, 0);
        assertThat(block).isNotNull();
        assertThat(panel.getBlockModel().blockIdForTile(1, 0)).isEqualTo("blk001");

        panel.getBlockModel().removeBlock("blk001");
        assertThat(panel.getBlockModel().isEmpty()).isTrue();
        assertThat(panel.getBlockModel().blockIdForTile(1, 0)).isNull();
    }

    @Test
    void blockPersistenceRoundTrip() throws Exception {
        SwitchboardPanel panel = setup();
        panel.testSetBlockStart(0, 0);
        Block block = panel.testCreateBlock(6, 0);
        assertThat(block).isNotNull();
        panel.getBlockModel().renameBlock("blk001", "Station track");

        LayoutData data = new LayoutPersistence().capture(panel);
        assertThat(data.getBlocks()).hasSize(1);
        assertThat(data.getBlocks().get(0).getId()).isEqualTo("blk001");
        assertThat(data.getBlocks().get(0).getName()).isEqualTo("Station track");
        assertThat(data.getBlocks().get(0).getTiles()).hasSize(7);

        SwitchboardPanel restored = newPanel();
        new LayoutPersistence().apply(restored, data);

        assertThat(restored.getBlockModel().size()).isEqualTo(1);
        Block restoredBlock = restored.getBlockModel().getBlock("blk001");
        assertThat(restoredBlock).isNotNull();
        assertThat(restoredBlock.getName()).isEqualTo("Station track");
        assertThat(restoredBlock.size()).isEqualTo(7);
        assertThat(restored.getBlockModel().blockIdForTile(2, 0)).isEqualTo("blk001");
    }

    @Test
    void blockPathWithoutStartReturnsNull() throws Exception {
        SwitchboardPanel panel = setup();

        Block block = panel.testCreateBlock(3, 0);

        assertThat(block).isNull();
        assertThat(panel.getBlockModel().isEmpty()).isTrue();
    }

    @Test
    void curveEndpointStopsLeftOfCornerForCurveRightRotation0() throws Exception {
        SwitchboardPanel panel = newPanel();
        var url = BlockTest.class.getResource("/test-data/switchboard-block1.json");
        new LayoutPersistence().load(panel, java.nio.file.Paths.get(url.toURI()));

        java.lang.reflect.Method curveEndpoint = SwitchboardPanel.class.getDeclaredMethod("curveEndpoint", int.class, int.class,
            int[].class);
        curveEndpoint.setAccessible(true);
        int[] ep = (int[]) curveEndpoint.invoke(panel, 16, 1, new int[] { 15, 1 });
        java.lang.reflect.Method curveCorner = SwitchboardPanel.class.getDeclaredMethod("curveCorner", int.class, int.class);
        curveCorner.setAccessible(true);
        int[] corner = (int[]) curveCorner.invoke(panel, 16, 1);

        assertThat(corner).containsExactly(16 * 32 + 32, 1 * 32 + 32);
        assertThat(ep[0]).as("endpoint must terminate left of the bottom-right corner").isLessThan(corner[0] - 4);
        assertThat(ep[0]).as("endpoint should sit a few pixels before the corner").isGreaterThan(corner[0] - 9);
        // on the block side of the track (below the diagonal from center to corner)
        assertThat(ep[1]).as("endpoint must stay below the track diagonal").isGreaterThan(ep[0] - 16 * 32);
    }

    @Test
    void blockEndingOnCurveUsesCurveEndpoint() throws Exception {
        SwitchboardPanel panel = newPanel();
        var url = BlockTest.class.getResource("/test-data/switchboard-block1.json");
        new LayoutPersistence().load(panel, java.nio.file.Paths.get(url.toURI()));

        java.lang.reflect.Method blockEndpoint = SwitchboardPanel.class.getDeclaredMethod("blockEndpoint", List.class, int.class);
        blockEndpoint.setAccessible(true);
        List<int[]> path = List.of(new int[] { 15, 1 }, new int[] { 16, 1 });
        int[] end = (int[]) blockEndpoint.invoke(panel, path, 1);

        assertThat(end[0]).as("end of a block ending on CR-003 (rot 0) must be left of the corner x=544").isLessThan(540);
        assertThat(end[0]).as("endpoint should sit a few pixels before the corner").isGreaterThan(534);
        // on the block side of the track (below the diagonal y = x - 480 through (528,48) and (544,64))
        assertThat(end[1]).as("endpoint must stay below the track diagonal").isGreaterThan(end[0] - 16 * 32);
    }

    @Test
    void curveGuidePointStaysBesideTrackForCurveRightRotation180() throws Exception {
        SwitchboardPanel panel = newPanel();
        var url = BlockTest.class.getResource("/test-data/switchboard-block1.json");
        new LayoutPersistence().load(panel, java.nio.file.Paths.get(url.toURI()));

        java.lang.reflect.Method curveGuidePoint = SwitchboardPanel.class.getDeclaredMethod("curveGuidePoint", int.class, int.class,
            int[].class, int.class);
        curveGuidePoint.setAccessible(true);
        java.lang.reflect.Method curveCorner = SwitchboardPanel.class.getDeclaredMethod("curveCorner", int.class, int.class);
        curveCorner.setAccessible(true);

        // CR-002 at (6,5) rotation 180, block approaches from (7,5): block line at (208,180)
        int[] blockCenter = new int[] { 6 * 32 + 16, 5 * 32 + 16 + 4 };
        int[] gp = (int[]) curveGuidePoint.invoke(panel, 6, 5, blockCenter, 5);
        int[] corner = (int[]) curveCorner.invoke(panel, 6, 5);

        assertThat(corner).containsExactly(6 * 32, 5 * 32);
        // the guide point must stay on the block line (parallel to the track diagonal y = x - 28)
        assertThat(gp[1]).as("guide point must follow the offset track diagonal").isEqualTo(gp[0] - 28);
        // a few pixels before the corner, on the block side (below the track diagonal y = x - 32)
        assertThat(Math.abs(gp[0] - corner[0])).isLessThanOrEqualTo(8);
        assertThat(Math.abs(gp[1] - corner[1])).isLessThanOrEqualTo(8);
        assertThat(gp[1]).as("guide point must stay below the track diagonal").isGreaterThan(gp[0] - 32);
    }

    @Test
    void blockThroughCurveKeepsStraightRunAlignedWithOrthogonalNeighbor() throws Exception {
        SwitchboardPanel panel = newPanel();
        var url = BlockTest.class.getResource("/test-data/switchboard-block2.json");
        new LayoutPersistence().load(panel, java.nio.file.Paths.get(url.toURI()));

        java.lang.reflect.Method straightSide = SwitchboardPanel.class.getDeclaredMethod("straightSideDirection", List.class,
            int.class, int[].class);
        straightSide.setAccessible(true);
        java.lang.reflect.Method segDir = SwitchboardPanel.class.getDeclaredMethod("segmentDirection", List.class, int.class);
        segDir.setAccessible(true);

        // blk001 path; the curve tile (14,4) is entered vertically from (14,5)
        List<int[]> path = List.of(new int[] { 14, 6 }, new int[] { 14, 5 }, new int[] { 14, 4 }, new int[] { 13, 3 },
            new int[] { 12, 3 });
        int[] p = path.get(2);

        int[] raw = (int[]) segDir.invoke(panel, path, 2);
        int[] straight = (int[]) straightSide.invoke(panel, path, 2, p);

        // the raw exit direction is diagonal; the straight side is the vertical neighbour (14,5)
        assertThat(raw).containsExactly(-1, -1);
        assertThat(straight).containsExactly(0, -1);

        // offsetting by the straight side keeps the block line on the vertical run x=460
        int ox = straight[0] == 0 && straight[1] != 0 ? -4 : 0;
        int oy = straight[0] != 0 ? 4 : 0;
        assertThat(new int[] { p[0] * 32 + 16 + ox, p[1] * 32 + 16 + oy }).containsExactly(460, 144);

        // the run through (14,6) and (14,5) stays on the same vertical line x=460
        int[] above = path.get(1);
        int[] dir = (int[]) segDir.invoke(panel, path, 1);
        assertThat(new int[] { above[0] * 32 + 16 + (dir[0] == 0 ? -4 : 0), above[1] * 32 + 16 }).containsExactly(460, 176);
    }

    @Test
    void blockEndingOnDiagonalContinuesToUpperRightEdge() throws Exception {
        SwitchboardPanel panel = newPanel();
        var url = BlockTest.class.getResource("/test-data/switchboard-block3.json");
        new LayoutPersistence().load(panel, java.nio.file.Paths.get(url.toURI()));

        java.lang.reflect.Method blockEndpoint = SwitchboardPanel.class.getDeclaredMethod("blockEndpoint", List.class, int.class);
        blockEndpoint.setAccessible(true);

        // blk003 ends on the diagonal tile DG-006 at (24,18), entered from (23,19)
        List<int[]> path = List.of(new int[] { 21, 19 }, new int[] { 22, 19 }, new int[] { 23, 19 }, new int[] { 24, 18 });
        int[] end = (int[]) blockEndpoint.invoke(panel, path, 3);

        // the block line must follow the diagonal up-right to the tile edge, not
        // cut horizontally at the centre row
        assertThat(end[0]).as("endpoint must reach the right edge").isEqualTo(24 * 32 + 32);
        assertThat(end[1]).as("endpoint must stay above the centre row, on the diagonal").isEqualTo(580);
    }

    @Test
    void diagonalEndpointExitsThroughEdgeInExitDirection() throws Exception {
        SwitchboardPanel panel = newPanel();
        var url = BlockTest.class.getResource("/test-data/switchboard-block3.json");
        new LayoutPersistence().load(panel, java.nio.file.Paths.get(url.toURI()));

        java.lang.reflect.Method diagonalEndpoint = SwitchboardPanel.class.getDeclaredMethod("diagonalEndpoint", int.class,
            int.class, int.class, int.class);
        diagonalEndpoint.setAccessible(true);

        // block line sits 4px below the tile centre (col*32+16, row*32+20) and runs
        // parallel to the track diagonal until it crosses the edge in the exit direction
        assertThat((int[]) diagonalEndpoint.invoke(panel, 24, 18, 1, -1)).containsExactly(800, 580);
        assertThat((int[]) diagonalEndpoint.invoke(panel, 24, 18, 1, 1)).containsExactly(796, 608);
        assertThat((int[]) diagonalEndpoint.invoke(panel, 24, 18, -1, 1)).containsExactly(772, 608);
        assertThat((int[]) diagonalEndpoint.invoke(panel, 24, 18, -1, -1)).containsExactly(768, 580);
    }
}
