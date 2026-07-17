package com.bidib.switchboard.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import com.bidib.switchboard.command.Command;
import com.bidib.switchboard.command.CycleElementCommand;
import com.bidib.switchboard.model.Element;
import com.bidib.switchboard.model.ElementTile;
import com.bidib.switchboard.model.ElementType;
import com.bidib.switchboard.model.Occupancy;
import com.bidib.switchboard.model.RailwayModel;
import com.bidib.switchboard.model.Route;
import com.bidib.switchboard.model.RouteModel;
import com.bidib.switchboard.model.Tile;
import com.bidib.switchboard.service.RouterService;
import com.bidib.switchboard.util.SvgIconLoader;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.view.ViewBox;

public class SwitchboardPanel extends JPanel implements PropertyChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwitchboardPanel.class);

    @FunctionalInterface
    public interface TileContextHandler {
        void handle(int col, int row, ElementType type);
    }

    public static final int DEFAULT_TILE_SIZE = 32;

    public static final int DEFAULT_COLS = 60;

    public static final int DEFAULT_ROWS = 30;

    private static final Color COLOR_BACKGROUND = new Color(45, 45, 50);

    private static final Color COLOR_GRID_LINE = new Color(60, 60, 65);

    private static final Color COLOR_SELECTION = new Color(0, 200, 200);

    private static final Color COLOR_ROUTE = new Color(255, 80, 80);
    private static final Color COLOR_ROUTE_ALT = new Color(80, 255, 80);
    private static final Color COLOR_ROUTE_ALT_OTHER = new Color(80, 80, 255);
    private static final Color COLOR_ROUTE_SOURCE = new Color(100, 200, 100);
    private static final Color COLOR_ROUTE_TARGET = new Color(80, 80, 255);

    private final RouterService routerService;

    private final int tileSize;

    private final int cols;

    private final int rows;

    private final RailwayModel model;

    private final Map<String, Tile> tiles = new LinkedHashMap<>();

    private final Deque<Command> undoStack = new ArrayDeque<>();

    private TileContextHandler tileContextHandler;

    private boolean exhaustiveRouting = false;

    public void setExhaustiveRouting(boolean exhaustive) {
        this.exhaustiveRouting = exhaustive;
    }

    private int selectedCol = -1;

    private int selectedRow = -1;

    private boolean editMode;

    private final RouteModel routeModel = new RouteModel();

    private int routeSourceCol = -1;

    private int routeSourceRow = -1;

    public SwitchboardPanel(RailwayModel model) {
        this(model, DEFAULT_COLS, DEFAULT_ROWS, DEFAULT_TILE_SIZE);
    }

    public SwitchboardPanel(RailwayModel model, int cols, int rows, int tileSize) {
        this.model = model;
        this.cols = cols;
        this.rows = rows;
        this.tileSize = tileSize;
        this.routerService = new RouterService(tiles, cols, rows, routeModel);
        model.addPropertyChangeListener(this);
        setBackground(COLOR_BACKGROUND);
        setPreferredSize(new Dimension(cols * tileSize, rows * tileSize));
        setFocusable(true);
        ToolTipManager.sharedInstance().registerComponent(this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    handleClick(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e.getX(), e.getY());
                }
            }
        });

        InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        inputMap.put(KeyStroke.getKeyStroke("control R"), "rotateTile");
        getActionMap().put("rotateTile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rotateSelectedTile();
            }
        });
    }

    // --- Tile management ---

    public void setTile(Tile tile) {
        tiles.put(tileKey(tile.getCol(), tile.getRow()), tile);
        repaint();
    }

    public Tile getTile(int col, int row) {
        return tiles.get(tileKey(col, row));
    }

    public Map<String, Tile> getTiles() {
        return tiles;
    }

    public int getTileSize() {
        return tileSize;
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public RailwayModel getModel() {
        return model;
    }

    public RouteModel getRouteModel() {
        return routeModel;
    }

    public void setSelectedTile(int col, int row) {
        selectedCol = col;
        selectedRow = row;
        repaint();
    }

    public void clearTiles() {
        tiles.clear();
        routeModel.clear();
        routeSourceCol = -1;
        routeSourceRow = -1;
        repaint();
    }

    public void removeTile(int col, int row) {
        tiles.remove(tileKey(col, row));
        if (selectedCol == col && selectedRow == row) {
            selectedCol = -1;
            selectedRow = -1;
        }
        if (routeSourceCol == col && routeSourceRow == row) {
            routeSourceCol = -1;
            routeSourceRow = -1;
        }
        repaint();
    }

    public void setTileContextHandler(TileContextHandler handler) {
        this.tileContextHandler = handler;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        if (!editMode) {
            selectedCol = -1;
            selectedRow = -1;
        }
        repaint();
    }

    // --- Tooltip ---

    @Override
    public String getToolTipText(MouseEvent e) {
        if (!editMode) {
            return null;
        }
        int col = e.getX() / tileSize;
        int row = e.getY() / tileSize;
        Tile tile = getTile(col, row);
        if (tile == null || tile.getElementId() == null) {
            return null;
        }
        return tile.getElementId();
    }

    // --- Route ---

    private void clearPendingRoute() {
        routeSourceCol = -1;
        routeSourceRow = -1;
    }

    private void findRoute(int targetCol, int targetRow) {
        findRoute(targetCol, targetRow, this.exhaustiveRouting);
    }

    private void findRoute(int targetCol, int targetRow, boolean exhaustive) {
        if (routeSourceCol < 0 || routeSourceRow < 0) {
            return;
        }
        if (routeSourceCol == targetCol && routeSourceRow == targetRow) {
            clearPendingRoute();
            repaint();
            return;
        }

        Tile srcTile = getTile(routeSourceCol, routeSourceRow);
        Tile dstTile = getTile(targetCol, targetRow);
        if (srcTile == null || dstTile == null) {
            clearPendingRoute();
            repaint();
            return;
        }
        String srcId = srcTile.getElementId();
        String dstId = dstTile.getElementId();
        if (srcId == null || dstId == null) {
            clearPendingRoute();
            repaint();
            return;
        }

        // Temporarily remove any existing route with this ID so its tiles
        // don't block the BFS for the replacement route.
        if (routeModel.getRoute(srcId + "-" + dstId) != null) {
            routeModel.removeRoute(srcId + "-" + dstId);
        }
        List<int[]> path = routerService.bfsRoute(routeSourceCol, routeSourceRow, targetCol, targetRow);
        if (path != null) {
            Route route = new Route(srcId, dstId, path);
            LOGGER.info("Route {} added: {} tiles {}", route.getId(), path.size(), pathToString(path));

            List<List<int[]>> alts = routerService.bfsAlternativeRoutes(routeSourceCol, routeSourceRow, targetCol, targetRow, path, exhaustive);
            if (!alts.isEmpty()) {
                for (List<int[]> altPath : alts) {
                    Route alt = new Route(srcId, dstId, altPath);
                    routeModel.addAlternativeRoute(route.getId(), alt);
                    LOGGER.info("Alternative route found: {} tiles {}", altPath.size(), pathToString(altPath));
                }
            } else {
                LOGGER.info("No alternative route found for {}", route.getId());
            }

            routeModel.addRoute(route);
            setRouteAspects(path);
        } else {
            LOGGER.info("No route found from ({},{}) to ({},{})", routeSourceCol, routeSourceRow, targetCol, targetRow);
        }
        clearPendingRoute();
        repaint();
    }

    private void setRouteAspects(List<int[]> path) {
        routerService.setRouteAspects(path, model);
    }

    private int tileRotation(Tile tile) {
        int r = tile.getRotation();
        return ((r % 360) + 360) % 360;
    }

    // --- Context menu ---

    private void showContextMenu(int x, int y) {
        int col = x / tileSize;
        int row = y / tileSize;
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return;
        }

        selectedCol = col;
        selectedRow = row;

        JPopupMenu menu = new JPopupMenu();

        Tile tile = getTile(col, row);
        if (tile != null) {
            JMenuItem infoItem = new JMenuItem("Info");
            infoItem.addActionListener(e -> showTileInfo(tile));
            menu.add(infoItem);
        }

        if (editMode) {
            if (menu.getComponentCount() > 0) {
                menu.addSeparator();
            }

            JMenu signalMenu = null;

            for (ElementType type : ElementType.values()) {
                if (!type.isVisible()) {
                    continue;
                }

                if (type.getPrefix().startsWith("S")) {
                    if (signalMenu == null) {
                        signalMenu = new JMenu("Signals");
                        menu.add(signalMenu);
                    }
                    JMenuItem item = new JMenuItem(type.getPrefix() + " (" + type.name() + ")");
                    item.addActionListener(e -> {
                        if (tileContextHandler != null) {
                            tileContextHandler.handle(col, row, type);
                        }
                    });
                    signalMenu.add(item);
                }
                else {
                    JMenuItem item = new JMenuItem(type.getPrefix() + " (" + type.name() + ")");
                    item.addActionListener(e -> {
                        if (tileContextHandler != null) {
                            tileContextHandler.handle(col, row, type);
                        }
                    });
                    menu.add(item);
                }
            }

            if (tile != null) {
                if (tile instanceof ElementTile et && et.getElementId() != null) {
                    Element el = model.getElement(et.getElementId());
                    if (el != null) {
                        if (el.getOccupancy() != null) {
                            JMenuItem removeOccItem = new JMenuItem("Remove Occupancy");
                            removeOccItem.addActionListener(e -> el.setOccupancy(null));
                            menu.add(removeOccItem);
                        }
                        JMenuItem assignOccItem = new JMenuItem("Assign Occupancy...");
                        assignOccItem.addActionListener(e -> showAssignOccupancyDialog(el));
                        menu.add(assignOccItem);
                        menu.addSeparator();
                    }
                }
                JMenuItem clearItem = new JMenuItem("Clear");
                clearItem.addActionListener(e -> {
                    if (tileContextHandler != null) {
                        tileContextHandler.handle(col, row, null);
                    }
                });
                menu.add(clearItem);
            }
        }

        String routeId = tile != null ? routeModel.routeIdForTile(col, row) : null;
        if (routeId != null) {
            if (menu.getComponentCount() > 0) {
                menu.addSeparator();
            }
            if (routeModel.hasAlternativeRoute(routeId)) {
                List<Route> alts = routeModel.getAlternativeRoutes(routeId);
                int selectedIdx = routeModel.getSelectedAlternativeIndex(routeId);
                JMenuItem primaryItem = new JMenuItem("Use primary route");
                primaryItem.addActionListener(e -> {
                    routeModel.clearAlternatives(routeId);
                    repaint();
                });
                menu.add(primaryItem);
                for (int i = 0; i < alts.size(); i++) {
                    Route alt = alts.get(i);
                    String label = "Alternative " + (i + 1)
                        + " (" + alt.getSourceElementId() + " → " + alt.getTargetElementId() + ")";
                    JMenuItem item = new JMenuItem(label);
                    int idx = i;
                    item.addActionListener(e -> {
                        routeModel.setSelectedAlternativeIndex(routeId, idx);
                        repaint();
                    });
                    menu.add(item);
                }
                JMenuItem useItem = new JMenuItem("Use selected alternative");
                useItem.addActionListener(e -> {
                    routeModel.swapWithAlternative(routeId);
                    repaint();
                });
                menu.add(useItem);
            }
            JMenuItem clearRouteItem = new JMenuItem("Clear route (" + routeId + ")");
            clearRouteItem.addActionListener(e -> {
                routeModel.removeRoute(routeId);
                repaint();
            });
            menu.add(clearRouteItem);
        }

        if (editMode && tile != null && selectedCol >= 0 && selectedRow >= 0) {
            if (menu.getComponentCount() > 0) {
                menu.addSeparator();
            }
            JMenuItem clearSelectionItem = new JMenuItem("Clear selection");
            clearSelectionItem.addActionListener(e -> {
                selectedCol = -1;
                selectedRow = -1;
                repaint();
            });
            menu.add(clearSelectionItem);
        }

        if (menu.getComponentCount() > 0) {
            menu.show(this, x, y);
        }
    }

    private void showTileInfo(Tile tile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Position: (").append(tile.getCol()).append(", ").append(tile.getRow()).append(")\n");
        sb.append("Rotation: ").append(tile.getRotation()).append("\n");
        if (tile.getElementId() != null) {
            sb.append("Element ID: ").append(tile.getElementId()).append("\n");
        }
        if (tile instanceof ElementTile et) {
            sb.append("Type: ").append(et.getElementType().getPrefix()).append("\n");
            Element el = model.getElement(tile.getElementId());
            if (el != null) {
                sb.append("Current aspect: ").append(el.getCurrentAspect()).append("\n");
                sb.append("Node ID: ").append(el.getNodeId()).append("\n");
                sb.append("Accessory ID: ").append(el.getAccessoryId()).append("\n");
                if (el.getOccupancy() != null) {
                    sb.append("Occupancy: ").append(el.getOccupancy().getNodeId())
                        .append(":").append(el.getOccupancy().getPortId()).append("\n");
                }
            }
            sb.append("Aspects: ").append(et.getAspectCount()).append("\n");
        } else {
            sb.append("Type: plain\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Tile Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAssignOccupancyDialog(Element el) {
        JTextField nodeIdField = new JTextField("0", 10);
        JTextField portIdField = new JTextField("0", 10);

        FormBuilder builder = FormBuilder.create()
            .columns("right:pref, 3dlu, 60dlu:grow")
            .rows("pref, 3dlu, pref");
        builder.addLabel("Node ID:").xy(1, 1);
        builder.add(nodeIdField).xy(3, 1);
        builder.addLabel("Port ID:").xy(1, 3);
        builder.add(portIdField).xy(3, 3);
        JPanel panel = builder.getPanel();

        int result = JOptionPane.showConfirmDialog(this, panel, "Assign Occupancy",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                long nodeId = Long.parseLong(nodeIdField.getText().trim());
                int portId = Integer.parseInt(portIdField.getText().trim());
                Occupancy occ = Occupancy.create(nodeId, portId);
                model.addOccupancy(occ);
                el.setOccupancy(occ);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid number", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- Rendering ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        drawTiles(g2);
        drawGrid(g2);
        drawSelection(g2);
        drawRoute(g2);
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(COLOR_GRID_LINE);
        int width = cols * tileSize;
        int height = rows * tileSize;
        for (int x = 0; x <= cols; x++) {
            g2.drawLine(x * tileSize, 0, x * tileSize, height);
        }
        for (int y = 0; y <= rows; y++) {
            g2.drawLine(0, y * tileSize, width, y * tileSize);
        }
    }

    private void drawSelection(Graphics2D g2) {
        if (!editMode || selectedCol < 0 || selectedRow < 0) {
            return;
        }
        int px = selectedCol * tileSize;
        int py = selectedRow * tileSize;
        g2.setColor(COLOR_SELECTION);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(px + 1, py + 1, tileSize - 2, tileSize - 2);
    }

    private void drawRoute(Graphics2D g2) {
        for (Route route : routeModel.getRoutes().values()) {
            List<int[]> path = route.getPath();
            if (path.isEmpty()) {
                continue;
            }

            int half = tileSize / 2;
            int n = path.size();
            int[] xPoints = new int[n];
            int[] yPoints = new int[n];
            for (int i = 0; i < n; i++) {
                int[] p = path.get(i);
                xPoints[i] = p[0] * tileSize + half;
                yPoints[i] = p[1] * tileSize + half;
            }

            g2.setColor(COLOR_ROUTE);
            g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawPolyline(xPoints, yPoints, n);

            int[] first = path.get(0);
            int sx = first[0] * tileSize + half;
            int sy = first[1] * tileSize + half;
            g2.setColor(COLOR_ROUTE_SOURCE);
            g2.fillOval(sx - 6, sy - 6, 12, 12);

            if (n > 1) {
                int[] last = path.get(n - 1);
                int tx = last[0] * tileSize + half;
                int ty = last[1] * tileSize + half;
                g2.setColor(COLOR_ROUTE_TARGET);
                g2.fillOval(tx - 6, ty - 6, 12, 12);
            }

            int selectedIdx = routeModel.getSelectedAlternativeIndex(route.getId());
            if (selectedIdx >= 0) {
                List<Route> alts = routeModel.getAlternativeRoutes(route.getId());
                for (int ai = 0; ai < alts.size(); ai++) {
                    Route alt = alts.get(ai);
                    List<int[]> altPath = alt.getPath();
                    if (!altPath.isEmpty()) {
                        int m = altPath.size();
                        int[] ax = new int[m];
                        int[] ay = new int[m];
                        for (int i = 0; i < m; i++) {
                            int[] p = altPath.get(i);
                            ax[i] = p[0] * tileSize + half;
                            ay[i] = p[1] * tileSize + half;
                        }
                        g2.setColor(ai == selectedIdx ? COLOR_ROUTE_ALT : COLOR_ROUTE_ALT_OTHER);
                        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                                1f, new float[] { 4f, 4f }, 0f));
                        g2.drawPolyline(ax, ay, m);
                    }
                }
            }
        }

        if (routeSourceCol >= 0 && routeSourceRow >= 0) {
            int px = routeSourceCol * tileSize + tileSize / 2;
            int py = routeSourceRow * tileSize + tileSize / 2;
            g2.setColor(COLOR_ROUTE_SOURCE);
            g2.fillOval(px - 6, py - 6, 12, 12);
        }
    }

    private void drawTiles(Graphics2D g2) {
        for (Tile tile : tiles.values()) {
            String svgPath = resolveSvgResource(tile);
            if (svgPath == null) {
                continue;
            }
            SVGDocument doc = SvgIconLoader.load(svgPath);
            if (doc == null) {
                continue;
            }

            int px = tile.getCol() * tileSize;
            int py = tile.getRow() * tileSize;

            Graphics2D tileG = (Graphics2D) g2.create(px, py, tileSize, tileSize);
            try {
                int rot = tile.getRotation();
                if (rot != 0) {
                    tileG.rotate(Math.toRadians(rot), tileSize / 2.0, tileSize / 2.0);
                }
                doc.render(null, tileG, new ViewBox(0, 0, tileSize, tileSize));
            }
            finally {
                tileG.dispose();
            }
        }
    }

    private String resolveSvgResource(Tile tile) {
        if (tile instanceof ElementTile et) {
            String id = et.getElementId();
            int aspect = 0;
            if (id != null) {
                Integer a = model.getElementAspect(id);
                if (a != null) {
                    aspect = a;
                }
            }
            return et.getSvgForAspect(aspect);
        }
        return tile.getSvgResource();
    }

    // --- Interaction ---

    private void handleClick(MouseEvent e) {
        int col = e.getX() / tileSize;
        int row = e.getY() / tileSize;
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return;
        }

        selectedCol = col;
        selectedRow = row;
        requestFocusInWindow();

        if (e.isControlDown()) {
            if (routeSourceCol < 0) {
                routeSourceCol = col;
                routeSourceRow = row;
                LOGGER.info("Route source set at ({},{})", col, row);
            } else {
                findRoute(col, row);
            }
            repaint();
            return;
        }

        Tile tile = getTile(col, row);
        if (tile != null) {
            onTileClicked(tile);
        }
        repaint();
    }

    protected void onTileClicked(Tile tile) {
        if (editMode) {
            return;
        }
        if (tile instanceof ElementTile et) {
            String id = et.getElementId();
            int count = et.getAspectCount();
            if (id != null && count > 1) {
                Command cmd = new CycleElementCommand(model, id, count);
                cmd.execute();
                undoStack.push(cmd);
            }
        }
    }

    private void rotateSelectedTile() {
        if (!editMode || selectedCol < 0 || selectedRow < 0) {
            return;
        }
        Tile tile = getTile(selectedCol, selectedRow);
        if (tile != null) {
            tile.setRotation(tile.getRotation() + 90);
            repaint();
        }
    }

    public void undoLast() {
        if (!undoStack.isEmpty()) {
            undoStack.pop().undo();
        }
    }

    // --- Test support ---

    boolean hasActiveRoute() {
        return !routeModel.isEmpty();
    }

    int routeTileCount() {
        int count = 0;
        for (Route r : routeModel.getRoutes().values()) {
            count += r.getPath().size();
        }
        return count;
    }

    public void testSetRouteSource(int col, int row) {
        routeSourceCol = col;
        routeSourceRow = row;
    }

    public void testFindRoute(int targetCol, int targetRow) {
        findRoute(targetCol, targetRow, this.exhaustiveRouting);
    }

    public void testFindRoute(int targetCol, int targetRow, boolean exhaustive) {
        findRoute(targetCol, targetRow, exhaustive);
    }

    // --- Observer ---

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        SwingUtilities.invokeLater(this::repaint);
    }

    // --- Internal ---

    private static String tileKey(int col, int row) {
        return col + "," + row;
    }

    private static String edgeKey(int fromCol, int fromRow, int toCol, int toRow) {
        return fromCol + "," + fromRow + "->" + toCol + "," + toRow;
    }

    private static String pathToString(List<int[]> path) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) sb.append(" -> ");
            sb.append("(").append(path.get(i)[0]).append(",").append(path.get(i)[1]).append(")");
        }
        sb.append("]");
        return sb.toString();
    }
}
