package org.bidib.switchboard.component.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;

import org.bidib.switchboard.component.command.Command;
import org.bidib.switchboard.component.command.CreateRouteCommand;
import org.bidib.switchboard.component.command.CycleElementCommand;
import org.bidib.switchboard.component.command.DirectionCommand;
import org.bidib.switchboard.component.command.TileCommand;
import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.RouteModel;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.model.TileDirection;
import org.bidib.switchboard.component.simulation.OccupancySimulation;
import org.bidib.switchboard.component.service.RouterService;
import org.bidib.switchboard.component.util.SvgIconLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.view.ViewBox;

public class SwitchboardPanel extends JPanel implements TileGrid, PropertyChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwitchboardPanel.class);

    public static final int DEFAULT_TILE_SIZE = 32;

    public static final int DEFAULT_COLS = 60;

    public static final int DEFAULT_ROWS = 30;

    private static final Color COLOR_BACKGROUND = new Color(45, 45, 50);

    private static final Color COLOR_GRID_LINE = new Color(60, 60, 65);

    private static final Color COLOR_SELECTION = new Color(0, 200, 200);

    private static final Color COLOR_ROUTE = new Color(80, 80, 160);

    private static final Color COLOR_OCCUPIED = new Color(255, 80, 80);

    private static final Color[] COLOR_ALT_PALETTE = {
        new Color(255, 165, 0),    // orange
        new Color(220, 50, 50),    // red
        new Color(180, 50, 180),   // purple
        new Color(0, 180, 180),    // teal
        new Color(255, 100, 150),  // pink
        new Color(255, 200, 50),   // amber
        new Color(180, 50, 50),    // maroon
        new Color(50, 80, 150),    // steel blue
        new Color(150, 210, 0),    // lime
        new Color(255, 140, 80),   // coral
        new Color(100, 60, 180),   // indigo
        new Color(0, 210, 190),    // turquoise
        new Color(220, 180, 20),   // gold
        new Color(220, 80, 120),   // rose
        new Color(160, 100, 220),  // violet
        new Color(200, 160, 80),   // sand
    };

    private static Color altPaletteColor(int index) {
        return COLOR_ALT_PALETTE[index % COLOR_ALT_PALETTE.length];
    }

    private static final Color COLOR_ROUTE_SOURCE = new Color(100, 200, 100);

    private static final Color COLOR_ROUTE_TARGET = new Color(100, 160, 255);

    private final OccupancyFactory occupancyFactory;

    private final AssignOccupancyDialogFactory assignOccupancyDialogFactory;

    private final RouterService routerService;

    private final int tileSize;

    private final int cols;

    private final int rows;

    private final RailwayModel model;

    private final Map<String, Tile> tiles = new LinkedHashMap<>();

    private final Deque<Command> undoStack = new ArrayDeque<>();

    private boolean exhaustiveRouting = false;

    private boolean autoChangeSignal = false;

    public void setExhaustiveRouting(boolean exhaustive) {
        this.exhaustiveRouting = exhaustive;
    }

    public void setAutoChangeSignal(boolean autoChange) {
        this.autoChangeSignal = autoChange;
    }

    public boolean isAutoChangeSignal() {
        return autoChangeSignal;
    }

    private boolean showOtherAlternatives = false;

    public void setShowOtherAlternatives(boolean show) {
        this.showOtherAlternatives = show;
    }

    private int selectedCol = -1;

    private int selectedRow = -1;

    private boolean editMode;

    private final RouteModel routeModel = new RouteModel();

    private final Map<String, SimulationEntry> simulations = new HashMap<>();

    private record SimulationEntry(OccupancySimulation simulation, Timer timer) {
        boolean isRunning() { return timer.isRunning(); }
        void stop() { timer.stop(); simulation.stop(); }
    }

    private int routeSourceCol = -1;

    private int routeSourceRow = -1;
    
    public SwitchboardPanel(final OccupancyFactory occupancyFactory, final AssignOccupancyDialogFactory assignOccupancyDialogFactory, final RailwayModel model) {
        this(occupancyFactory, assignOccupancyDialogFactory, model, DEFAULT_COLS, DEFAULT_ROWS, DEFAULT_TILE_SIZE);
    }

    public SwitchboardPanel(final OccupancyFactory occupancyFactory, final AssignOccupancyDialogFactory assignOccupancyDialogFactory, final RailwayModel model, int cols, int rows, int tileSize) {
    		this.occupancyFactory = occupancyFactory;
        this.assignOccupancyDialogFactory = assignOccupancyDialogFactory;
//            (parent, m, el) -> new AssignOccupancyDialog().show(parent, m, el);
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

    @Override
    public void setTile(Tile tile) {
        tiles.put(Tile.key(tile.getCol(), tile.getRow()), tile);
        repaint();
    }

    @Override
    public Tile getTile(int col, int row) {
        return tiles.get(Tile.key(col, row));
    }

    public Map<String, Tile> getTiles() {
        return tiles;
    }

    @Override
    public int getTileSize() {
        return tileSize;
    }

    @Override
    public int getCols() {
        return cols;
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public RailwayModel getModel() {
        return model;
    }

    @Override
    public RouteModel getRouteModel() {
        return routeModel;
    }

    public void setSelectedTile(int col, int row) {
        selectedCol = col;
        selectedRow = row;
        repaint();
    }

    @Override
    public void clearTiles() {
        tiles.clear();
        routeModel.clear();
        routeSourceCol = -1;
        routeSourceRow = -1;
        repaint();
    }

    @Override
    public void removeTile(int col, int row) {
        tiles.remove(Tile.key(col, row));
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

        String routeId = srcId + "-" + dstId;
        Route previousRoute = routeModel.getRoute(routeId);
        if (previousRoute != null) {
            routeModel.removeRoute(routeId);
        }
        List<int[]> path = routerService.bfsRoute(routeSourceCol, routeSourceRow, targetCol, targetRow);
        if (path != null) {
            Route route = new Route(srcId, dstId, path);
            LOGGER.info("Route {} added: {} tiles {}", route.getId(), path.size(), pathToString(path));

            Map<String, Integer> oldAspects = new HashMap<>();
            for (int[] p : path) {
                Tile tile = getTile(p[0], p[1]);
                if (tile instanceof ElementTile et && et.getElementId() != null) {
                    Integer a = model.getElementAspect(et.getElementId());
                    if (a != null) {
                        oldAspects.put(et.getElementId(), a);
                    }
                }
            }

            List<Route> altRoutes = new ArrayList<>();
            List<List<int[]>> alts = routerService.bfsAlternativeRoutes(routeSourceCol, routeSourceRow, targetCol, targetRow, path, exhaustive);
            if (!alts.isEmpty()) {
                for (List<int[]> altPath : alts) {
                    Route alt = new Route(srcId, dstId, altPath);
                    altRoutes.add(alt);
                    routeModel.addAlternativeRoute(route.getId(), alt);
                    LOGGER.info("Alternative route found: {} tiles {}", altPath.size(), pathToString(altPath));
                }
            }
            else {
                LOGGER.info("No alternative route found for {}", route.getId());
            }

            routeModel.addRoute(route);
            routerService.setRouteAspects(path, model);

            undoStack.push(new CreateRouteCommand(routeModel, model, route, previousRoute, altRoutes, oldAspects));
        }
        else {
            LOGGER.info("No route found from ({},{}) to ({},{})", routeSourceCol, routeSourceRow, targetCol, targetRow);
            if (previousRoute != null) {
                undoStack.push(new CreateRouteCommand(routeModel, model, null, previousRoute, List.of(), Map.of()));
            }
        }
        clearPendingRoute();
        repaint();
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
            buildEditMenuItems(menu, col, row, tile);
        }

        buildRouteMenuItems(menu, col, row, tile);

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

    private void buildEditMenuItems(JPopupMenu menu, int col, int row, Tile tile) {
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
                item.addActionListener(e -> onTileContextAction(col, row, type));
                signalMenu.add(item);
            }
            else {
                JMenuItem item = new JMenuItem(type.getPrefix() + " (" + type.name() + ")");
                item.addActionListener(e -> onTileContextAction(col, row, type));
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
                }
                if (et.getElementType() == ElementType.STRAIGHT || et.getElementType() == ElementType.DIAGONAL) {
                    buildDirectionSubmenu(menu, tile);
                }
                menu.addSeparator();
            }
            JMenuItem clearItem = new JMenuItem("Clear");
            clearItem.addActionListener(e -> onTileContextAction(col, row, null));
            menu.add(clearItem);
        }
    }

    private void buildDirectionSubmenu(JPopupMenu menu, Tile tile) {
        JMenu dirMenu = new JMenu("Direction");
        TileDirection current = tile.getDirection();
        for (TileDirection dir : TileDirection.values()) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(dir.name(), dir == current);
            item.addActionListener(e -> {
                DirectionCommand cmd = new DirectionCommand(tile, dir);
                cmd.execute();
                undoStack.push(cmd);
                repaint();
            });
            dirMenu.add(item);
        }
        menu.add(dirMenu);
    }

    private void buildRouteMenuItems(JPopupMenu menu, int col, int row, Tile tile) {
        String routeId = tile != null ? routeModel.routeIdForTile(col, row) : null;
        if (routeId == null) {
            return;
        }
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
                String label = "Alternative " + (i + 1) + " (" + alt.getSourceElementId() + " → " + alt.getTargetElementId() + ")";
                Color altColor = altPaletteColor(i);
                Icon icon = new Icon() {
                    @Override public void paintIcon(Component comp, Graphics g, int x, int y) {
                        g.setColor(altColor);
                        g.fillOval(x, y, getIconWidth(), getIconHeight());
                    }
                    @Override public int getIconWidth() { return 10; }
                    @Override public int getIconHeight() { return 10; }
                };
                JMenuItem item = new JMenuItem(label, icon);
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
                Route newRoute = routeModel.getRoute(routeId);
                if (newRoute != null) {
                    routerService.setRouteAspects(newRoute.getPath(), model);
                }
                repaint();
            });
            menu.add(useItem);
        }
        JMenuItem clearRouteItem = new JMenuItem("Clear route (" + routeId + ")");
        clearRouteItem.addActionListener(e -> {
            SimulationEntry entry = simulations.remove(routeId);
            if (entry != null && entry.isRunning()) {
                entry.stop();
            }
            routeModel.removeRoute(routeId);
            repaint();
        });
        menu.add(clearRouteItem);

        Route r = routeModel.getRoute(routeId);
        if (r != null && !r.getPath().isEmpty()) {
            int[] first = r.getPath().get(0);
            if (first[0] == col && first[1] == row) {
                menu.addSeparator();
                SimulationEntry simEntry = simulations.get(routeId);
                boolean isRunning = simEntry != null && simEntry.isRunning();
                JMenuItem simItem = new JMenuItem("Simulate occupancy (" + routeId + ")");
                simItem.setEnabled(!isRunning);
                simItem.addActionListener(e -> startRouteOccupancySimulation(r));
                menu.add(simItem);
                if (isRunning) {
                    JMenuItem stopSimItem = new JMenuItem("Stop simulation (" + routeId + ")");
                    stopSimItem.addActionListener(e -> {
                        SimulationEntry entry = simulations.remove(routeId);
                        if (entry != null) {
                            entry.stop();
                        }
                        repaint();
                    });
                    menu.add(stopSimItem);
                }
                if (hasRouteOccupancy(r)) {
                    JMenuItem clearSimItem = new JMenuItem("Clear simulated occupancy (" + routeId + ")");
                    clearSimItem.setEnabled(!isRunning);
                    clearSimItem.addActionListener(e -> clearRouteOccupancy(r));
                    menu.add(clearSimItem);
                }
            }
        }
    }

    private void showTileInfo(Tile tile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Position: (").append(tile.getCol()).append(", ").append(tile.getRow()).append(")\n");
        sb.append("Rotation: ").append(tile.getRotation()).append("\n");
        if (tile.getDirection() != TileDirection.BOTH) {
            sb.append("Direction: ").append(tile.getDirection()).append("\n");
        }
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
                    sb.append("Occupancy: ").append(el.getOccupancy().getId()).append(" (").append(el.getOccupancy().getState()).append(")\n");
                }
            }
            sb.append("Aspects: ").append(et.getAspectCount()).append("\n");
        }
        else {
            sb.append("Type: plain\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Tile Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAssignOccupancyDialog(Element el) {
        assignOccupancyDialogFactory.showAssignOccupancyDialog(this, model, el);
    }

    private void onTileContextAction(int col, int row, ElementType type) {
        Tile oldTile = getTile(col, row);
        String oldElementId = oldTile != null ? oldTile.getElementId() : null;
        if (oldElementId != null) {
            model.removeElement(oldElementId);
        }
        if (type == null) {
            removeTile(col, row);
            undoStack.push(new TileCommand(this, model, col, row, oldTile, oldElementId, null, null));
            return;
        }
        String id = generateId(type);
        model.addElement(new Element(id, 0, 0));
        Tile newTile = createDefaultTile(col, row, id, type);
        setTile(newTile);
        setSelectedTile(col, row);
        undoStack.push(new TileCommand(this, model, col, row, oldTile, oldElementId, newTile, id));
    }

    private String generateId(ElementType type) {
        String prefix = type.getPrefix();
        int max = model.getElements().keySet().stream()
            .filter(id -> id.startsWith(prefix + "-"))
            .mapToInt(id -> {
                try { return Integer.parseInt(id.substring(prefix.length() + 1)); }
                catch (NumberFormatException e) { return 0; }
            })
            .max().orElse(0);
        return prefix + "-" + String.format("%03d", max + 1);
    }

    private static Tile createDefaultTile(int col, int row, String id, ElementType type) {
        return switch (type) {
            case TURNOUT_LEFT -> new ElementTile(col, row, id, type, List.of("/icons/turnout_straight_left.svg", "/icons/turnout_diverted_left.svg"));
            case TURNOUT_RIGHT -> new ElementTile(col, row, id, type, List.of("/icons/turnout_straight_right.svg", "/icons/turnout_diverted_right.svg"));
            case TURNOUT_3WAY -> new ElementTile(col, row, id, type,
                List.of("/icons/turnout_3way_straight.svg", "/icons/turnout_3way_left.svg", "/icons/turnout_3way_right.svg"));
            case SIGNAL_2 -> new ElementTile(col, row, id, type, List.of("/icons/signal_2_red.svg", "/icons/signal_2_green.svg"));
            case SIGNAL_3 -> new ElementTile(col, row, id, type, List.of("/icons/signal_3_red.svg", "/icons/signal_3_yellow.svg", "/icons/signal_3_green.svg"));
            case STRAIGHT -> new ElementTile(col, row, id, type, List.of("/icons/straight.svg"));
            case CURVE_LEFT -> new ElementTile(col, row, id, type, List.of("/icons/curve_left.svg"));
            case CURVE_RIGHT -> new ElementTile(col, row, id, type, List.of("/icons/curve_right.svg"));
            case DIAGONAL -> new ElementTile(col, row, id, type, List.of("/icons/diagonal.svg"));
        };
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
        drawDirectionMarkers(g2);
        drawSelection(g2);
        drawRoute(g2);
        drawOccupancy(g2);
        drawAlternatives(g2);
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

    private static final Color COLOR_DIRECTION = new Color(200, 200, 200, 180);

    private void drawDirectionMarkers(Graphics2D g2) {
        int half = tileSize / 2;
        int arrowSize = Math.max(4, tileSize / 5);
        g2.setColor(COLOR_DIRECTION);
        for (Tile tile : tiles.values()) {
            if (tile.getDirection() == TileDirection.BOTH) {
                continue;
            }
            if (!(tile instanceof ElementTile et)) {
                continue;
            }
            ElementType type = et.getElementType();
            if (type != ElementType.STRAIGHT && type != ElementType.DIAGONAL) {
                continue;
            }
            int cx = tile.getCol() * tileSize + half;
            int cy = tile.getRow() * tileSize + half;
            int rotSteps = (tile.getRotation() / 90) % 4;

            // Determine the angle for the triangle (pointing toward exit port)
            double angle = computeDirectionAngle(type, rotSteps, tile.getDirection());
            drawTriangle(g2, cx, cy, arrowSize, angle);
        }
    }

    /**
     * Computes the angle (in radians) the direction triangle should point.
     * 0 = right, PI/2 = down, PI = left, 3PI/2 = up.
     */
    private static double computeDirectionAngle(ElementType type, int rotSteps, TileDirection dir) {
        if (type == ElementType.STRAIGHT) {
            // Forward at rot 0 = LEFT→RIGHT, so triangle points RIGHT (angle 0)
            double baseAngle = 0;
            double rotAngle = rotSteps * Math.PI / 2;
            double angle = baseAngle + rotAngle;
            return dir == TileDirection.FORWARD ? angle : angle + Math.PI;
        }
        // DIAGONAL: forward at rot 0 = lower-left → upper-right, triangle points upper-right (-PI/4)
        double baseAngle = -Math.PI / 4;
        double rotAngle = rotSteps * Math.PI / 2;
        double angle = baseAngle + rotAngle;
        return dir == TileDirection.FORWARD ? angle : angle + Math.PI;
    }

    private static void drawTriangle(Graphics2D g2, int cx, int cy, int size, double angle) {
        int[] xp = new int[3];
        int[] yp = new int[3];
        // Tip of triangle
        xp[0] = cx + (int) (size * Math.cos(angle));
        yp[0] = cy + (int) (size * Math.sin(angle));
        // Two base corners (120° apart from tip direction)
        double baseAngle1 = angle + 2.4; // ~137°
        double baseAngle2 = angle - 2.4;
        int baseSize = size * 2;
        xp[1] = cx + (int) (baseSize * Math.cos(baseAngle1));
        yp[1] = cy + (int) (baseSize * Math.sin(baseAngle1));
        xp[2] = cx + (int) (baseSize * Math.cos(baseAngle2));
        yp[2] = cy + (int) (baseSize * Math.sin(baseAngle2));
        g2.fillPolygon(xp, yp, 3);
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
        int half = tileSize / 2;
        for (Route route : routeModel.getRoutes().values()) {
            List<int[]> path = route.getPath();
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

            if (routeModel.hasAlternativeRoute(route.getId())) {
                g2.setColor(Color.WHITE);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
                boolean startHorizontal = n > 1 && path.get(0)[1] == path.get(1)[1];
                g2.drawString("+", startHorizontal ? sx - 6 : sx + 10, startHorizontal ? sy + 18 : sy + 6);
            }

            if (n > 1) {
                int[] last = path.get(n - 1);
                int tx = last[0] * tileSize + half;
                int ty = last[1] * tileSize + half;
                g2.setColor(COLOR_ROUTE_TARGET);
                g2.fillOval(tx - 6, ty - 6, 12, 12);

                if (routeModel.hasAlternativeRoute(route.getId())) {
                    g2.setColor(Color.WHITE);
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
                    boolean endHorizontal = n > 1 && path.get(n - 2)[1] == path.get(n - 1)[1];
                    g2.drawString("+", endHorizontal ? tx - 6 : tx + 10, endHorizontal ? ty + 18 : ty + 6);
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

    private void drawOccupancy(Graphics2D g2) {
        int half = tileSize / 2;
        g2.setColor(COLOR_OCCUPIED);
        g2.setStroke(new BasicStroke(4, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
        for (Tile tile : tiles.values()) {
            if (tile instanceof ElementTile et && et.getElementId() != null) {
                Element el = model.getElement(et.getElementId());
                if (el != null && el.getOccupancy() != null && el.getOccupancy().getState() == Occupancy.OccupancyState.OCCUPIED) {
                    int cx = tile.getCol() * tileSize + half;
                    int cy = tile.getRow() * tileSize + half;
                    int d = (tileSize - 2) / 2;
                    int rotSteps = (tile.getRotation() / 90) % 4;

                    if (et.getElementType() == ElementType.DIAGONAL) {
                        drawDiagonalOccupancy(g2, cx, cy, d, rotSteps);
                        continue;
                    }

                    int[] ports = et.getElementType().getActivePorts(el.getCurrentAspect(), tile.getRotation());

                    if (ports.length == 2
                        && (et.getElementType() == ElementType.CURVE_LEFT
                        || et.getElementType() == ElementType.CURVE_RIGHT
                        || et.getElementType() == ElementType.TURNOUT_3WAY)) {
                        drawCurveOccupancy(g2, cx, cy, d, ports);
                        continue;
                    }

                    drawPortsOccupancy(g2, cx, cy, d, rotSteps, et, el, ports);
                }
            }
        }
    }

    private static void drawDiagonalOccupancy(Graphics2D g2, int cx, int cy, int d, int rotSteps) {
        int[][] pairs = { { ElementType.PORT_LEFT, ElementType.PORT_BOTTOM },
            { ElementType.PORT_TOP, ElementType.PORT_RIGHT } };
        for (int[] pair : pairs) {
            int p1 = (pair[0] + rotSteps) % 4;
            int p2 = (pair[1] + rotSteps) % 4;
            int dx = (p1 == ElementType.PORT_RIGHT || p2 == ElementType.PORT_RIGHT) ? d
                : (p1 == ElementType.PORT_LEFT || p2 == ElementType.PORT_LEFT) ? -d : 0;
            int dy = (p1 == ElementType.PORT_BOTTOM || p2 == ElementType.PORT_BOTTOM) ? d
                : (p1 == ElementType.PORT_TOP || p2 == ElementType.PORT_TOP) ? -d : 0;
            g2.drawLine(cx, cy, cx + dx, cy + dy);
        }
    }

    private void drawCurveOccupancy(Graphics2D g2, int cx, int cy, int d, int[] ports) {
        for (int i = 0; i < ports.length; i++) {
            int port = ports[i];
            if (i == 0) {
                drawPortLine(g2, cx, cy, port, tileSize);
            } else {
                int firstPort = ports[0];
                boolean secondIsVertical = port == ElementType.PORT_TOP || port == ElementType.PORT_BOTTOM;
                int dx, dy;
                if (secondIsVertical) {
                    dx = firstPort == ElementType.PORT_LEFT ? d
                        : firstPort == ElementType.PORT_RIGHT ? -d : 0;
                    dy = port == ElementType.PORT_TOP ? -d : d;
                } else {
                    dx = port == ElementType.PORT_LEFT ? -d
                        : port == ElementType.PORT_RIGHT ? d : 0;
                    dy = firstPort == ElementType.PORT_TOP ? d
                        : firstPort == ElementType.PORT_BOTTOM ? -d : 0;
                }
                g2.drawLine(cx, cy, cx + dx, cy + dy);
            }
        }
    }

    private void drawPortsOccupancy(Graphics2D g2, int cx, int cy, int d, int rotSteps,
                                     ElementTile et, Element el, int[] ports) {
        for (int port : ports) {
            if (el.getCurrentAspect() == 1
                && (et.getElementType() == ElementType.TURNOUT_RIGHT || et.getElementType() == ElementType.TURNOUT_LEFT)) {
                int divertBase = et.getElementType() == ElementType.TURNOUT_RIGHT
                    ? ElementType.PORT_BOTTOM : ElementType.PORT_TOP;
                int divertExit = (divertBase + rotSteps) % 4;
                if (port == divertExit) {
                    int throughPort = (ElementType.PORT_RIGHT + rotSteps) % 4;
                    boolean divertIsHorizontal = divertExit == ElementType.PORT_LEFT || divertExit == ElementType.PORT_RIGHT;
                    int dx = divertIsHorizontal
                        ? (divertExit == ElementType.PORT_RIGHT ? d : -d)
                        : (throughPort == ElementType.PORT_RIGHT ? d
                            : throughPort == ElementType.PORT_LEFT ? -d : 0);
                    int dy = divertIsHorizontal
                        ? (throughPort == ElementType.PORT_BOTTOM ? d
                            : throughPort == ElementType.PORT_TOP ? -d : 0)
                        : (divertExit == ElementType.PORT_BOTTOM ? d : -d);
                    g2.drawLine(cx, cy, cx + dx, cy + dy);
                    continue;
                }
            }
            drawPortLine(g2, cx, cy, port, tileSize);
        }
    }

    private void drawAlternatives(Graphics2D g2) {
        for (Route route : routeModel.getRoutes().values()) {
            int selectedIdx = routeModel.getSelectedAlternativeIndex(route.getId());
            if (selectedIdx < 0) continue;
            List<Route> alts = routeModel.getAlternativeRoutes(route.getId());
            if (showOtherAlternatives) {
                for (int ai = 0; ai < alts.size(); ai++) {
                    if (ai == selectedIdx) continue;
                    drawAltPath(g2, alts.get(ai), tileSize, altPaletteColor(ai));
                }
            }
            drawAltPath(g2, alts.get(selectedIdx), tileSize, altPaletteColor(selectedIdx));
        }
    }

    private static void drawAltPath(Graphics2D g2, Route alt, int tileSize, Color color) {
        List<int[]> altPath = alt.getPath();
        if (altPath.isEmpty()) return;
        int m = altPath.size();
        int half = tileSize / 2;
        int[] ax = new int[m];
        int[] ay = new int[m];
        for (int i = 0; i < m; i++) {
            int[] p = altPath.get(i);
            ax[i] = p[0] * tileSize + half;
            ay[i] = p[1] * tileSize + half;
        }
        g2.setColor(color);
        g2.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[] { 4f, 4f }, 0f));
        g2.drawPolyline(ax, ay, m);
    }

    private static void drawPortLine(Graphics2D g2, int cx, int cy, int port, int tileSize) {
        int d = (tileSize - 2) / 2;
        switch (port) {
            case ElementType.PORT_LEFT -> g2.drawLine(cx - d, cy, cx, cy);
            case ElementType.PORT_TOP -> g2.drawLine(cx, cy - d, cx, cy);
            case ElementType.PORT_RIGHT -> g2.drawLine(cx, cy, cx + d, cy);
            case ElementType.PORT_BOTTOM -> g2.drawLine(cx, cy, cx, cy + d);
        }
    }

    /** Returns the Element for a tile at (col, row), or null if not an element tile. */
    private Element elementAt(int col, int row) {
        Tile tile = getTile(col, row);
        if (tile instanceof ElementTile et && et.getElementId() != null) {
            return model.getElement(et.getElementId());
        }
        return null;
    }

    /** Returns true if the tile is a signal element with aspect 0 (red). */
    boolean isSignalAtRed(Tile tile) {
        return OccupancySimulation.isSignalAtRed(tile, model);
    }

    /**
     * Returns true if the tile is a signal at red AND the train entered from the signal's facing direction.
     */
    boolean isSignalBlocking(Tile tile, int entryPort) {
        return OccupancySimulation.isSignalBlocking(tile, entryPort, model);
    }

    /**
     * Computes the port through which a train enters a tile, given the movement delta.
     */
    static int portFromDelta(int dc, int dr) {
        return OccupancySimulation.portFromDelta(dc, dr);
    }

    boolean isTileOccupied(int col, int row) {
        Element el = elementAt(col, row);
        return el != null && el.getOccupancy() != null
            && el.getOccupancy().getState() == Occupancy.OccupancyState.OCCUPIED;
    }

    private boolean hasRouteOccupancy(Route route) {
        return route.getPath().stream().anyMatch(p -> isTileOccupied(p[0], p[1]));
    }

    private void clearRouteOccupancy(Route route) {
        for (int[] p : route.getPath()) {
            Element el = elementAt(p[0], p[1]);
            if (el != null && el.getOccupancy() != null) {
                el.getOccupancy().setState(Occupancy.OccupancyState.FREE);
            }
        }
    }

    private void startRouteOccupancySimulation(Route route) {
        if (route.getPath().isEmpty()) {
            return;
        }

        String routeId = route.getId();

        // Stop existing simulation for this route if any
        SimulationEntry existing = simulations.get(routeId);
        if (existing != null && existing.isRunning()) {
            existing.stop();
        }

        OccupancySimulation sim = new OccupancySimulation(model, this, occupancyFactory);
        sim.setAutoChangeSignal(autoChangeSignal);
        sim.setOnTick(this::repaint);
        sim.start(route);

        Timer timer = new Timer(200, e -> {
            sim.tick();
            if (!sim.isRunning()) {
                ((Timer) e.getSource()).stop();
            }
        });
        timer.setRepeats(true);
        timer.start();

        simulations.put(routeId, new SimulationEntry(sim, timer));
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
            }
            else {
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
        return routeModel.getRoutes().values().stream().mapToInt(r -> r.getPath().size()).sum();
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

    public void testTileContextAction(int col, int row, ElementType type) {
        onTileContextAction(col, row, type);
    }

    public void testSetRouteAspects(List<int[]> path) {
        routerService.setRouteAspects(path, model);
    }

    public void testStartOccupancySimulation(Route route) {
        startRouteOccupancySimulation(route);
    }

    public Timer getOccupancyTimer() {
        // Return the first running timer, or any timer, for backward compatibility
        return simulations.values().stream()
            .filter(SimulationEntry::isRunning)
            .map(SimulationEntry::timer)
            .findFirst()
            .orElse(simulations.values().stream().map(SimulationEntry::timer).findFirst().orElse(null));
    }

    public OccupancySimulation getSimulation() {
        return simulations.values().stream()
            .map(SimulationEntry::simulation)
            .findFirst()
            .orElse(null);
    }

    public OccupancySimulation getSimulation(String routeId) {
        SimulationEntry entry = simulations.get(routeId);
        return entry != null ? entry.simulation() : null;
    }

    public boolean isAnySimulationRunning() {
        return simulations.values().stream().anyMatch(SimulationEntry::isRunning);
    }

    // --- Observer ---

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        SwingUtilities.invokeLater(this::repaint);
    }

    // --- Internal ---

    private static String pathToString(List<int[]> path) {
        return path.stream()
            .map(p -> "(" + p[0] + "," + p[1] + ")")
            .collect(Collectors.joining(" -> ", "[", "]"));
    }
}
