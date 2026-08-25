package org.bidib.switchboard.component.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
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
import org.bidib.switchboard.component.command.MoveTilesCommand;
import org.bidib.switchboard.component.command.SetElementAspectCommand;
import org.bidib.switchboard.component.command.TileCommand;
import org.bidib.switchboard.component.config.OccupancyFactory;
import org.bidib.switchboard.component.model.Block;
import org.bidib.switchboard.component.model.BlockModel;
import org.bidib.switchboard.component.model.Element;
import org.bidib.switchboard.component.model.ElementTile;
import org.bidib.switchboard.component.model.ElementType;
import org.bidib.switchboard.component.model.Occupancy;
import org.bidib.switchboard.component.model.RailwayModel;
import org.bidib.switchboard.component.model.Route;
import org.bidib.switchboard.component.model.RouteModel;
import org.bidib.switchboard.component.model.SignalSide;
import org.bidib.switchboard.component.model.SignalTile;
import org.bidib.switchboard.component.model.Tile;
import org.bidib.switchboard.component.model.TileDirection;
import org.bidib.switchboard.component.model.Train;
import org.bidib.switchboard.component.service.RouterService;
import org.bidib.switchboard.component.simulation.OccupancySimulation;
import org.bidib.switchboard.component.util.SvgIconLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.view.ViewBox;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

public class SwitchboardPanel extends JPanel implements Dockable, TileGrid, PropertyChangeListener {
    private final DockKey dockKey = new DockKey("switchboard");

    private static final Logger LOGGER = LoggerFactory.getLogger(SwitchboardPanel.class);

    public static final int DEFAULT_TILE_SIZE = 32;

    public static final int DEFAULT_COLS = 60;

    public static final int DEFAULT_ROWS = 30;

    public static final String BLOCK_ID_PREFIX = "blk";

    static final int REMOVE_LINKED_OPTION = 0;

    static final int KEEP_DISTANT_OPTION = 1;

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

    private final Map<String, Tile> tiles;

    private final RouteModel routeModel;

    private final Map<TileImageKey, BufferedImage> tileImageCache = new LinkedHashMap<>(64, 0.75f, true);

    private final Map<CombinedImageKey, BufferedImage> combinedImageCache = new LinkedHashMap<>(64, 0.75f, true);

    private static final int TILE_IMAGE_CACHE_MAX = 1024;

    private static final String SIGNAL_BASE_SVG = "/icons/tracks/straight.svg";

    private final Deque<Command> undoStack = new ArrayDeque<>();

    private boolean autoChangeSignal = false;

    private SignalSide globalSignalSide = SignalSide.LEFT;

    public void setGlobalSignalSide(SignalSide side) {
        this.globalSignalSide = side;
        repaint();
    }

    public SignalSide getGlobalSignalSide() {
        return globalSignalSide;
    }

    public void setExhaustiveRouting(boolean exhaustive) {
        routerService.setExhaustiveRouting(exhaustive);
    }

    public void setAutoChangeSignal(boolean autoChange) {
        this.autoChangeSignal = autoChange;
        for (SimulationEntry entry : simulations.values()) {
            entry.simulation().setAutoChangeSignal(autoChange);
        }
        
        if (routeSimulation!= null) {
        		routeSimulation.setAutoChangeSignal(autoChangeSignal);
        }
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

    private final Set<String> selectedTiles = new LinkedHashSet<>();

    private int selectionDragStartCol = -1;

    private int selectionDragStartRow = -1;

    private int selectionDragEndCol = -1;

    private int selectionDragEndRow = -1;

    private boolean isDraggingSelection;

    private boolean editMode;

    private ResourceBundle messages = ResourceBundle.getBundle("i18n.messages");

    private final BlockModel blockModel = new BlockModel();

    private int blockStartCol = -1;

    private int blockStartRow = -1;

    private static final Color COLOR_BLOCK = new Color(255, 220, 80);

    private static final Color COLOR_BLOCK_START = new Color(255, 180, 0);

    private static final int BLOCK_LINE_OFFSET = 4;

    private static final int BLOCK_TICK_LENGTH = 8;

    private static final int CORNER_PULL = 5;

    private final Map<String, SimulationEntry> simulations = new HashMap<>();

    private record SimulationEntry(OccupancySimulation simulation, Timer timer) {
        boolean isRunning() { return timer.isRunning(); }
        void stop() { timer.stop(); simulation.stop(); }
    }

    private int routeSourceCol = -1;

    private int routeSourceRow = -1;

    private boolean routeCreationMode = false;

    private final java.util.List<int[]> routeCreationPath = new java.util.ArrayList<>();

    private Route selectedRoute = null;

    private final java.util.Set<Integer> routeCreationStops = new java.util.LinkedHashSet<>();

    private List<int[]> routeCreationPendingPrimary = null;

    private List<List<int[]>> routeCreationPendingAlternatives = java.util.Collections.emptyList();

    private org.bidib.switchboard.component.simulation.RouteSimulation routeSimulation;

    private Timer routeTimer;

    public SwitchboardPanel(final OccupancyFactory occupancyFactory, final AssignOccupancyDialogFactory assignOccupancyDialogFactory, final RailwayModel model, RouterService routerService) {
        this(occupancyFactory, assignOccupancyDialogFactory, model, routerService, routerService.getCols(), routerService.getRows(), DEFAULT_TILE_SIZE);
    }

    public SwitchboardPanel(final OccupancyFactory occupancyFactory, final AssignOccupancyDialogFactory assignOccupancyDialogFactory, final RailwayModel model, RouterService routerService, int cols, int rows, int tileSize) {
    		this.occupancyFactory = occupancyFactory;
        this.assignOccupancyDialogFactory = assignOccupancyDialogFactory;
        this.model = model;
        this.routerService = routerService;
        this.cols = cols;
        this.rows = rows;
        this.tileSize = tileSize;
        this.tiles = routerService.getTiles();
        this.routeModel = routerService.getRouteModel();
        model.addPropertyChangeListener(this);
        setBackground(background());
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
                else if (e.getButton() == MouseEvent.BUTTON1 && editMode) {
                    int col = e.getX() / tileSize;
                    int row = e.getY() / tileSize;
                    if (col >= 0 && col < cols && row >= 0 && row < rows) {
                        selectionDragStartCol = col;
                        selectionDragStartRow = row;
                        selectionDragEndCol = col;
                        selectionDragEndRow = row;
                        requestFocusInWindow();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e.getX(), e.getY());
                }
                else if (isDraggingSelection && editMode) {
                    isDraggingSelection = false;
                    updateSelectedTilesFromDrag();
                    repaint();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!editMode || selectionDragStartCol < 0) {
                    return;
                }
                int col = Math.max(0, Math.min(e.getX() / tileSize, cols - 1));
                int row = Math.max(0, Math.min(e.getY() / tileSize, rows - 1));
                if (col != selectionDragEndCol || row != selectionDragEndRow) {
                    isDraggingSelection = true;
                    selectionDragEndCol = col;
                    selectionDragEndRow = row;
                    repaint();
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

        inputMap.put(KeyStroke.getKeyStroke("pressed UP"), "moveUp");
        getActionMap().put("moveUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelectedTiles(0, -1);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("pressed DOWN"), "moveDown");
        getActionMap().put("moveDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelectedTiles(0, 1);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("pressed LEFT"), "moveLeft");
        getActionMap().put("moveLeft", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelectedTiles(-1, 0);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("pressed RIGHT"), "moveRight");
        getActionMap().put("moveRight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelectedTiles(1, 0);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("pressed ESCAPE"), "clearSelection");
        inputMap.put(KeyStroke.getKeyStroke("pressed ENTER"), "clearSelection");
        getActionMap().put("clearSelection", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearMultiSelection();
                selectedCol = -1;
                selectedRow = -1;
                repaint();
            }
        });

        // Enable drop of Train objects onto block marker tiles
        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                int col = dtde.getLocation().x / tileSize;
                int row = dtde.getLocation().y / tileSize;
                if (isBlockMarkerAt(col, row)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                } else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                int col = dtde.getLocation().x / tileSize;
                int row = dtde.getLocation().y / tileSize;
                if (!isBlockMarkerAt(col, row)) {
                    dtde.rejectDrop();
                    return;
                }
                if (dtde.isDataFlavorSupported(TrainListPanel.TRAIN_FLAVOR)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    try {
                        Train train = (Train) dtde.getTransferable().getTransferData(TrainListPanel.TRAIN_FLAVOR);
                        handleTrainDrop(col, row, train);
                        dtde.dropComplete(true);
                    } catch (Exception ex) {
                        LOGGER.warn("Failed to drop train", ex);
                        dtde.dropComplete(false);
                    }
                } else {
                    dtde.rejectDrop();
                }
            }
        });
    }

    private boolean isBlockMarkerAt(int col, int row) {
        Tile tile = getTile(col, row);
        return tile instanceof ElementTile et && et.getElementType() == ElementType.BLOCK_MARKER;
    }

    // --- Tile management ---

    @Override
    public void setTile(Tile tile) {
        tiles.put(Tile.key(tile.getCol(), tile.getRow()), tile);
        clearTileImageCache();
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

    @Override
    public BlockModel getBlockModel() {
        return blockModel;
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
        blockModel.clear();
        blockStartCol = -1;
        blockStartRow = -1;
        routeSourceCol = -1;
        routeSourceRow = -1;
        clearMultiSelection();
        clearTileImageCache();
        repaint();
    }

    @Override
    public void removeTile(int col, int row) {
        tiles.remove(Tile.key(col, row));
        if (selectedCol == col && selectedRow == row) {
            selectedCol = -1;
            selectedRow = -1;
        }
        selectedTiles.remove(Tile.key(col, row));
        if (routeSourceCol == col && routeSourceRow == row) {
            routeSourceCol = -1;
            routeSourceRow = -1;
        }
        if (blockStartCol == col && blockStartRow == row) {
            blockStartCol = -1;
            blockStartRow = -1;
        }
        Block block = blockModel.getBlockForTile(col, row);
        if (block != null) {
            blockModel.removeBlock(block.getId());
        }
        clearTileImageCache();
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
            clearMultiSelection();
            routeCreationStops.clear();
        } else if (selectedRoute != null) {
            routeCreationStops.clear();
            for (Route.StationStop stop : selectedRoute.getStops()) {
                routeCreationStops.add(stop.getPathIndex());
            }
        }
        repaint();
    }

    public boolean isRouteCreationMode() {
        return routeCreationMode;
    }

    public void enterRouteCreationMode() {
        routeCreationMode = true;
        routeCreationPath.clear();
        routeCreationStops.clear();
        clearPendingRoute();
        repaint();
    }

    public java.util.List<int[]> exitRouteCreationMode() {
        java.util.List<int[]> result = new java.util.ArrayList<>(routeCreationPath);
        routeCreationMode = false;
        routeCreationPath.clear();
        routeCreationStops.clear();
        routeCreationPendingPrimary = null;
        routeCreationPendingAlternatives = java.util.Collections.emptyList();
        clearPendingRoute();
        repaint();
        return result;
    }

    public java.util.Set<Integer> getRouteCreationStops() {
        return java.util.Collections.unmodifiableSet(routeCreationStops);
    }

    public void cancelRouteCreationMode() {
        routeCreationMode = false;
        routeCreationPath.clear();
        routeCreationStops.clear();
        routeCreationPendingPrimary = null;
        routeCreationPendingAlternatives = java.util.Collections.emptyList();
        clearPendingRoute();
        repaint();
    }

    private boolean isTileInRouteCreationPath(int col, int row) {
        if (routeCreationMode) {
            for (int[] p : routeCreationPath) {
                if (p[0] == col && p[1] == row) {
                    return true;
                }
            }
        } else if (selectedRoute != null) {
            for (int[] p : selectedRoute.getPath()) {
                if (p[0] == col && p[1] == row) {
                    return true;
                }
            }
        }
        return false;
    }

    private int routeCreationPathIndex(int col, int row) {
        if (routeCreationMode) {
            for (int i = 0; i < routeCreationPath.size(); i++) {
                int[] p = routeCreationPath.get(i);
                if (p[0] == col && p[1] == row) {
                    return i;
                }
            }
        } else if (selectedRoute != null) {
            List<int[]> path = selectedRoute.getPath();
            for (int i = 0; i < path.size(); i++) {
                int[] p = path.get(i);
                if (p[0] == col && p[1] == row) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Switches the resource bundle used for all localized strings (context menus, tile info, etc.)
     * to the given locale. Future context menus and tooltips use the new language immediately.
     */
    public void setLocale(Locale locale) {
        this.messages = ResourceBundle.getBundle("i18n.messages", locale);
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

    private void findRouteForCreation(int targetCol, int targetRow) {
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

        List<int[]> path = routerService.bfsRoute(routeSourceCol, routeSourceRow, targetCol, targetRow);
        if (path == null || path.isEmpty()) {
            LOGGER.info("No route found for train route from ({},{}) to ({},{})",
                routeSourceCol, routeSourceRow, targetCol, targetRow);
            clearPendingRoute();
            repaint();
            return;
        }

        // Find alternatives
        List<List<int[]>> alts = routerService.bfsAlternativeRoutes(
            routeSourceCol, routeSourceRow, targetCol, targetRow, path);

        if (alts.isEmpty()) {
            // No alternatives, append directly
            appendRouteCreationPath(path);
            clearPendingRoute();
        } else {
            // Store pending primary + alternatives for context menu selection
            routeCreationPendingPrimary = path;
            routeCreationPendingAlternatives = alts;
            LOGGER.info("Train route segment: {} alternatives found, right-click to choose", alts.size());
        }
        repaint();
    }

    private void appendRouteCreationPath(List<int[]> path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        LOGGER.info("Train route segment added: {} tiles", path.size());

        List<int[]> toAdd = path;
        // Remove junction overlap
        if (!routeCreationPath.isEmpty()) {
            int[] lastExisting = routeCreationPath.get(routeCreationPath.size() - 1);
            if (lastExisting[0] == toAdd.get(0)[0] && lastExisting[1] == toAdd.get(0)[1]) {
                toAdd = toAdd.subList(1, toAdd.size());
            }
        }
        routeCreationPath.addAll(toAdd);
    }

    public boolean hasRouteCreationPendingAlternatives() {
        return routeCreationPendingPrimary != null && !routeCreationPendingAlternatives.isEmpty();
    }

    public List<int[]> getRouteCreationPendingPrimary() {
        return routeCreationPendingPrimary;
    }

    public List<List<int[]>> getRouteCreationPendingAlternatives() {
        return routeCreationPendingAlternatives;
    }

    public void selectRouteCreationPrimary() {
        if (routeCreationPendingPrimary != null) {
            appendRouteCreationPath(routeCreationPendingPrimary);
        }
        routeCreationPendingPrimary = null;
        routeCreationPendingAlternatives = java.util.Collections.emptyList();
        clearPendingRoute();
        repaint();
    }

    public void selectRouteCreationAlternative(int index) {
        if (index >= 0 && index < routeCreationPendingAlternatives.size()) {
            appendRouteCreationPath(routeCreationPendingAlternatives.get(index));
        }
        routeCreationPendingPrimary = null;
        routeCreationPendingAlternatives = java.util.Collections.emptyList();
        clearPendingRoute();
        repaint();
    }

    private List<org.bidib.switchboard.component.model.Route> findRoutesStartingAtBlock(Block block) {
        List<int[]> blockPath = block.getPath();
        if (blockPath.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        LOGGER.info("Finding train routes for block {} with {} tiles", block.getName(), blockPath.size());
        // A route matches if its FIRST tile is in the block
        java.util.Set<String> blockTileKeys = new java.util.HashSet<>();
        for (int[] p : blockPath) {
            blockTileKeys.add(Tile.key(p[0], p[1]));
        }
        List<org.bidib.switchboard.component.model.Route> result = new java.util.ArrayList<>();
        for (org.bidib.switchboard.component.model.Route tr : routeModel.getRoutes().values()) {
            if (tr.getName() == null) {
                continue;
            }
            boolean matches = false;
            if (!tr.getPath().isEmpty()) {
                int[] first = tr.getPath().get(0);
                if (blockTileKeys.contains(Tile.key(first[0], first[1]))) {
                    matches = true;
                }
            }
            LOGGER.info("  Route '{}': matches={}", tr.getName(), matches);
            if (matches) {
                result.add(tr);
            }
        }
        LOGGER.info("  Found {} matching routes", result.size());
        return result;
    }

    private void startRouteSimulation(org.bidib.switchboard.component.model.Route tr) {
        // Stop any existing simulation
        stopRouteSimulation();

        // Find the block where this route's first tile is located
        String trainId = null;
        Block startBlock = null;
        if (!tr.getPath().isEmpty()) {
            int[] firstTile = tr.getPath().get(0);
            startBlock = blockModel.getBlockForTile(firstTile[0], firstTile[1]);
            if (startBlock != null) {
                trainId = startBlock.getAssignedTrainId();
            }
        }

        if (trainId == null) {
            LOGGER.info("No train assigned to the starting block of train route '{}'", tr.getName());
            return;
        }

        routeSimulation = new org.bidib.switchboard.component.simulation.RouteSimulation(
            model, this, occupancyFactory);
        routeSimulation.setOnTick(this::repaint);
        routeSimulation.setAutoChangeSignal(autoChangeSignal);

        // Find the start index in the route path that overlaps with or is adjacent to the block
        int startIndex = 0;
        if (startBlock != null) {
            for (int i = 0; i < tr.getPath().size(); i++) {
                int[] p = tr.getPath().get(i);
                if (startBlock.containsTile(p[0], p[1])) {
                    startIndex = i;
                    break;
                }
                // Check adjacency
                for (int[] bp : startBlock.getPath()) {
                    int dx = Math.abs(p[0] - bp[0]);
                    int dy = Math.abs(p[1] - bp[1]);
                    if (dx + dy == 1) {
                        startIndex = i;
                        break;
                    }
                }
                if (startIndex > 0) break;
            }
        }
        routerService.setRouteAspects(tr.getPath(), model);
        // Reset signals on the route to red — the simulation manages them
        for (int[] p : tr.getPath()) {
            Tile tile = getTile(p[0], p[1]);
            if (tile instanceof ElementTile et && et.getElementId() != null) {
                ElementType type = et.getElementType();
                if (type == ElementType.SIGNAL_M3 || type == ElementType.SIGNAL_COMBINED) {
                    model.setElementAspect(et.getElementId(), 0);
                }
            }
        }
        routeSimulation.start(tr, trainId, startIndex);
        selectedRoute = null;

        // Drive simulation with a timer
        routeTimer = new Timer(200, e -> {
            if (routeSimulation != null && routeSimulation.isRunning()) {
                routeSimulation.tick();
            } else {
                stopRouteSimulation();
            }
        });
        routeTimer.start();
        repaint();
    }

    private void stopRouteSimulation() {
        if (routeTimer != null) {
            routeTimer.stop();
            routeTimer = null;
        }
        if (routeSimulation != null) {
            routeSimulation.reset();
            routeSimulation = null;
        }
        repaint();
    }

    public org.bidib.switchboard.component.simulation.RouteSimulation getRouteSimulation() {
        return routeSimulation;
    }

    private void clearPendingRoute() {
        routeSourceCol = -1;
        routeSourceRow = -1;
    }

    private void findRoute(int targetCol, int targetRow) {
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
            String routeName = srcId + " \u2192 " + dstId;
            Route route = new Route(routeName, srcId, dstId, path);
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
            List<List<int[]>> alts = routerService.bfsAlternativeRoutes(routeSourceCol, routeSourceRow, targetCol, targetRow, path);
            if (!alts.isEmpty()) {
                for (List<int[]> altPath : alts) {
                    Route alt = new Route(routeName, srcId, dstId, altPath);
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
            JMenuItem infoItem = new JMenuItem(messages.getString("context.info"));
            infoItem.addActionListener(e -> showTileInfo(tile));
            menu.add(infoItem);

            if (routeCreationMode && hasRouteCreationPendingAlternatives()) {
                if (menu.getComponentCount() > 0) {
                    menu.addSeparator();
                }
                JMenuItem primaryItem = new JMenuItem("Use primary route");
                primaryItem.addActionListener(e -> selectRouteCreationPrimary());
                menu.add(primaryItem);

                List<List<int[]>> alts = getRouteCreationPendingAlternatives();
                for (int i = 0; i < alts.size(); i++) {
                    final int idx = i;
                    int tiles = alts.get(i).size();
                    Color c = altPaletteColor(i);
                    JMenuItem altItem = new JMenuItem("Alternative " + (i + 1) + " (" + tiles + " tiles)");
                    altItem.setIcon(new javax.swing.Icon() {
                        @Override public void paintIcon(java.awt.Component comp, java.awt.Graphics g, int x, int y) {
                            g.setColor(c);
                            g.fillOval(x + 2, y + 2, 12, 12);
                        }
                        @Override public int getIconWidth() { return 16; }
                        @Override public int getIconHeight() { return 16; }
                    });
                    altItem.addActionListener(e -> selectRouteCreationAlternative(idx));
                    menu.add(altItem);
                }
            }

            if ((routeCreationMode || (editMode && selectedRoute != null)) && isTileInRouteCreationPath(col, row)) {
                boolean isStop = routeCreationStops.contains(routeCreationPathIndex(col, row));
                JMenuItem stopItem = new JMenuItem(isStop ? messages.getString("context.removeStationStop") : messages.getString("context.addStationStop"));
                stopItem.addActionListener(e -> {
                    int idx = routeCreationPathIndex(col, row);
                    if (idx >= 0) {
                        if (isStop) {
                            routeCreationStops.remove(idx);
                            if (selectedRoute != null) {
                                selectedRoute.removeStop(idx);
                            }
                        } else {
                            routeCreationStops.add(idx);
                            if (selectedRoute != null) {
                                selectedRoute.addStop(idx, 5000);
                            }
                        }
                        repaint();
                    }
                });
                menu.add(stopItem);
            }

            if (!editMode && tile instanceof ElementTile et && et.getElementType() == ElementType.BLOCK_MARKER) {
                String blockId = blockModel.blockIdForTile(col, row);
                if (blockId != null) {
                    Block block = blockModel.getBlock(blockId);
                    if (block != null && block.getAssignedTrainId() != null) {
                        // Find train routes that start at this block
                        List<org.bidib.switchboard.component.model.Route> matchingRoutes =
                            findRoutesStartingAtBlock(block);
                        if (!matchingRoutes.isEmpty()) {
                            if (matchingRoutes.size() == 1) {
                                org.bidib.switchboard.component.model.Route tr = matchingRoutes.get(0);
                                JMenuItem runItem = new JMenuItem("Run: " + tr.getName());
                                runItem.addActionListener(e -> startRouteSimulation(tr));
                                menu.add(runItem);
                            } else {
                                JMenu runMenu = new JMenu("Run Route");
                                for (org.bidib.switchboard.component.model.Route tr : matchingRoutes) {
                                    JMenuItem routeItem = new JMenuItem(tr.getName());
                                    routeItem.addActionListener(e -> startRouteSimulation(tr));
                                    runMenu.add(routeItem);
                                }
                                menu.add(runMenu);
                            }
                        }

                        if (routeSimulation != null && routeSimulation.isRunning()) {
                            JMenuItem stopItem = new JMenuItem("Stop Route");
                            stopItem.addActionListener(e -> stopRouteSimulation());
                            menu.add(stopItem);
                        }

                        JMenuItem clearTrainItem = new JMenuItem(messages.getString("context.blockClearTrain"));
                        clearTrainItem.addActionListener(e -> {
                            block.clearAssignedTrain();
                            LOGGER.info("Cleared train from block {} ({})", blockId, block.getName());
                            repaint();
                        });
                        menu.add(clearTrainItem);
                    }
                }
            }
        }

        if (editMode) {
            buildEditMenuItems(menu, col, row, tile);
        }

        buildRouteMenuItems(menu, col, row, tile);

        if (editMode && (tile != null || !selectedTiles.isEmpty()) && (selectedCol >= 0 || !selectedTiles.isEmpty())) {
            if (menu.getComponentCount() > 0) {
                menu.addSeparator();
            }
            JMenuItem clearSelectionItem = new JMenuItem(messages.getString("context.clearSelection"));
            clearSelectionItem.addActionListener(e -> {
                selectedCol = -1;
                selectedRow = -1;
                clearMultiSelection();
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
                    signalMenu = new JMenu(messages.getString("context.signals"));
                    menu.add(signalMenu);
                }
                JMenuItem item = new JMenuItem(createElementTypeMenuLabel(type));
                item.addActionListener(e -> onTileContextAction(col, row, type));
                signalMenu.add(item);
            }
            else {
                JMenuItem item = new JMenuItem(createElementTypeMenuLabel(type));
                item.addActionListener(e -> onTileContextAction(col, row, type));
                menu.add(item);
            }
        }

        if (tile != null) {
            if (tile instanceof ElementTile et && et.getElementId() != null) {
                Element el = model.getElement(et.getElementId());
                if (el != null) {
                    if (el.getOccupancy() != null) {
                        JMenuItem removeOccItem = new JMenuItem(messages.getString("context.removeOccupancy"));
                        removeOccItem.addActionListener(e -> el.setOccupancy(null));
                        menu.add(removeOccItem);
                    }
                    JMenuItem assignOccItem = new JMenuItem(messages.getString("context.assignOccupancy"));
                    assignOccItem.addActionListener(e -> showAssignOccupancyDialog(el));
                    menu.add(assignOccItem);
                }
                if (et.getElementType() == ElementType.STRAIGHT || et.getElementType() == ElementType.DIAGONAL) {
                    buildDirectionSubmenu(menu, tile);
                }
                if (et.getElementType() == ElementType.SIGNAL_M3
                    || et.getElementType() == ElementType.SIGNAL_V
                    || et.getElementType() == ElementType.SIGNAL_COMBINED) {
                    SignalTile st = (SignalTile) et;
                    JMenu sideMenu = new JMenu(messages.getString("context.signalSide"));
                    SignalSide currentSide = st.getSignalSide();
                    for (SignalSide side : SignalSide.values()) {
                        JCheckBoxMenuItem item = new JCheckBoxMenuItem(side.name(), side == currentSide);
                        item.addActionListener(e -> {
                            st.setSignalSide(side);
                            SignalSide effective = side == SignalSide.DEFAULT ? globalSignalSide : side;
                            st.applySignalSide(effective);
                            repaint();
                        });
                        sideMenu.add(item);
                    }
                    menu.add(sideMenu);
                }
                if (et.getElementType() == ElementType.SIGNAL_V
                    || et.getElementType() == ElementType.SIGNAL_COMBINED) {
                    buildAssignMainSignalSubmenu(menu, (SignalTile) et);
                }
                buildBlockMenuItems(menu, col, row, tile);
                menu.addSeparator();
            }
            JMenuItem clearItem = new JMenuItem(messages.getString("context.clear"));
            clearItem.addActionListener(e -> onTileContextAction(col, row, null));
            menu.add(clearItem);
        }
    }

    private String createElementTypeMenuLabel(ElementType type) {
        String key = "elementType." + type.name();
        try {
            String label = messages.getString(key);
            return label + " (" + type.getPrefix() + ")";
        }
        catch (MissingResourceException ex) {
            return type.getPrefix() + " (" + type.name() + ")";
        }
    }

    private void buildDirectionSubmenu(JPopupMenu menu, Tile tile) {
        JMenu dirMenu = new JMenu(messages.getString("context.direction"));
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

    /**
     * Adds an "Assign main signal" submenu to the distant signal's context menu. Lists every
     * placed main signal (SIGNAL_M3) plus "None". When no link is set yet, the main
     * signal suggested by {@link #suggestMainSignalForDistant(ElementTile)} is preselected.
     */
    private void buildAssignMainSignalSubmenu(JPopupMenu menu, SignalTile distantTile) {
        JMenu assignMenu = new JMenu(messages.getString("context.assignMainSignal"));
        String current = distantTile.getMainSignalId();
        String suggested = suggestMainSignalForDistant(distantTile);
        String defaultChecked = current != null ? current : suggested;

        JCheckBoxMenuItem noneItem = new JCheckBoxMenuItem(
            messages.getString("context.assignMainSignal.none"), defaultChecked == null);
        noneItem.addActionListener(e -> {
            distantTile.setMainSignalId(null);
            repaint();
        });
        assignMenu.add(noneItem);

        for (Tile t : tiles.values()) {
            if (!(t instanceof ElementTile met) || met.getElementId() == null) {
                continue;
            }
            ElementType type = met.getElementType();
            if (type != ElementType.SIGNAL_M3 && type != ElementType.SIGNAL_COMBINED) {
                continue;
            }
            String label = met.getElementId();
            if (current == null && met.getElementId().equals(suggested)) {
                label += messages.getString("context.assignMainSignal.suggested");
            }
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, met.getElementId().equals(defaultChecked));
            item.addActionListener(e -> {
                distantTile.setMainSignalId(met.getElementId());
                repaint();
            });
            assignMenu.add(item);
        }
        menu.add(assignMenu);
    }

    /**
     * Suggests the main signal the given distant signal previews by walking the physical track
     * from the signal's direction of travel (opposite to its facing port), following the current
     * aspect of each tile, until a main signal is found. Returns null when no main signal lies
     * along the connected track ahead.
     */
    String suggestMainSignalForDistant(SignalTile distantTile) {
        int rotSteps = (distantTile.getRotation() / 90) % 4;
        int facingPort = (ElementType.PORT_LEFT + rotSteps) % 4;
        int exitPort = (facingPort + 2) % 4;
        int dc = switch (exitPort) {
            case ElementType.PORT_LEFT -> -1;
            case ElementType.PORT_RIGHT -> 1;
            default -> 0;
        };
        int dr = switch (exitPort) {
            case ElementType.PORT_TOP -> -1;
            case ElementType.PORT_BOTTOM -> 1;
            default -> 0;
        };
        int c = distantTile.getCol() + dc;
        int r = distantTile.getRow() + dr;
        int prevCol = distantTile.getCol();
        int prevRow = distantTile.getRow();
        Set<String> visited = new HashSet<>();
        for (int i = 0; i < 100 && c >= 0 && c < cols && r >= 0 && r < rows; i++) {
            Tile t = getTile(c, r);
            if (!(t instanceof ElementTile et) || et.getElementId() == null) {
                return null;
            }
            ElementType type = et.getElementType();
            if (type == ElementType.SIGNAL_M3 || type == ElementType.SIGNAL_COMBINED) {
                return et.getElementId();
            }
            if (type == ElementType.SIGNAL_V) {
                return null;
            }
            String key = c + "," + r;
            if (!visited.add(key)) {
                return null;
            }
            int[] next = trackConnectedCell(c, r, prevCol, prevRow);
            if (next == null) {
                return null;
            }
            prevCol = c;
            prevRow = r;
            c = next[0];
            r = next[1];
        }
        return null;
    }

    /**
     * Returns the physical track cell reachable from ({@code col},{@code row}) in the direction of
     * travel, i.e. the connected neighbor that is not the tile we just came from. Connectivity is
     * computed from the tile's current-aspect active ports only, so turnouts follow the aspect they
     * are currently set to. Returns null when there is no onward physical connection.
     */
    private int[] trackConnectedCell(int col, int row, int fromCol, int fromRow) {
        for (int[] neighbor : trackConnectedNeighbors(col, row)) {
            if (neighbor[0] != fromCol || neighbor[1] != fromRow) {
                return neighbor;
            }
        }
        return null;
    }

    /**
     * Lists the physically connected neighbours of a tile. Mirrors the connectivity of
     * {@link RouterService} but uses the tile's current-aspect ports, so a turnout only exposes the
     * leg it is currently set to.
     */
    private List<int[]> trackConnectedNeighbors(int col, int row) {
        List<int[]> neighbors = new ArrayList<>();
        Tile tile = getTile(col, row);
        if (!(tile instanceof ElementTile et) || et.getElementId() == null) {
            return neighbors;
        }
        Integer aspect = model.getElementAspect(et.getElementId());
        int currentAspect = aspect != null ? aspect : 0;
        int[] ports = et.getElementType().getActivePorts(currentAspect, et.getRotation());
        Set<Integer> portSet = new HashSet<>();
        for (int p : ports) {
            portSet.add(p);
        }
        if (portSet.contains(ElementType.PORT_LEFT) && col > 0 && hasPhysicalPort(col - 1, row, ElementType.PORT_RIGHT)) {
            neighbors.add(new int[] { col - 1, row });
        }
        if (portSet.contains(ElementType.PORT_TOP) && row > 0 && hasPhysicalPort(col, row - 1, ElementType.PORT_BOTTOM)) {
            neighbors.add(new int[] { col, row - 1 });
        }
        if (portSet.contains(ElementType.PORT_RIGHT) && col < cols - 1 && hasPhysicalPort(col + 1, row, ElementType.PORT_LEFT)) {
            neighbors.add(new int[] { col + 1, row });
        }
        if (portSet.contains(ElementType.PORT_BOTTOM) && row < rows - 1 && hasPhysicalPort(col, row + 1, ElementType.PORT_TOP)) {
            neighbors.add(new int[] { col, row + 1 });
        }
        if ((portSet.contains(ElementType.PORT_RIGHT) || portSet.contains(ElementType.PORT_BOTTOM))
            && et.getElementType().hasValidDiagonal(ElementType.PORT_RIGHT, ElementType.PORT_BOTTOM, et.getRotation())
            && col < cols - 1 && row < rows - 1
            && (hasPhysicalPort(col + 1, row + 1, ElementType.PORT_LEFT) || hasPhysicalPort(col + 1, row + 1, ElementType.PORT_TOP))) {
            neighbors.add(new int[] { col + 1, row + 1 });
        }
        if ((portSet.contains(ElementType.PORT_LEFT) || portSet.contains(ElementType.PORT_BOTTOM))
            && et.getElementType().hasValidDiagonal(ElementType.PORT_LEFT, ElementType.PORT_BOTTOM, et.getRotation())
            && col > 0 && row < rows - 1
            && (hasPhysicalPort(col - 1, row + 1, ElementType.PORT_RIGHT) || hasPhysicalPort(col - 1, row + 1, ElementType.PORT_TOP))) {
            neighbors.add(new int[] { col - 1, row + 1 });
        }
        if ((portSet.contains(ElementType.PORT_RIGHT) || portSet.contains(ElementType.PORT_TOP))
            && et.getElementType().hasValidDiagonal(ElementType.PORT_RIGHT, ElementType.PORT_TOP, et.getRotation())
            && col < cols - 1 && row > 0
            && (hasPhysicalPort(col + 1, row - 1, ElementType.PORT_LEFT) || hasPhysicalPort(col + 1, row - 1, ElementType.PORT_BOTTOM))) {
            neighbors.add(new int[] { col + 1, row - 1 });
        }
        if ((portSet.contains(ElementType.PORT_LEFT) || portSet.contains(ElementType.PORT_TOP))
            && et.getElementType().hasValidDiagonal(ElementType.PORT_LEFT, ElementType.PORT_TOP, et.getRotation())
            && col > 0 && row > 0
            && (hasPhysicalPort(col - 1, row - 1, ElementType.PORT_RIGHT) || hasPhysicalPort(col - 1, row - 1, ElementType.PORT_BOTTOM))) {
            neighbors.add(new int[] { col - 1, row - 1 });
        }
        return neighbors;
    }

    private boolean hasPhysicalPort(int col, int row, int port) {
        Tile t = getTile(col, row);
        if (!(t instanceof ElementTile et) || et.getElementId() == null) {
            return false;
        }
        int[] ports = et.getElementType().getPhysicalPorts(et.getRotation());
        if (ports == null) {
            return false;
        }
        for (int p : ports) {
            if (p == port) {
                return true;
            }
        }
        return false;
    }

    private void buildBlockMenuItems(JPopupMenu menu, int col, int row, Tile tile) {
        if (menu.getComponentCount() > 0) {
            menu.addSeparator();
        }
        JMenu blockMenu = new JMenu(messages.getString("context.block"));
        JMenuItem startItem = new JMenuItem(messages.getString("context.blockStart"));
        startItem.addActionListener(e -> {
            blockStartCol = col;
            blockStartRow = row;
            repaint();
        });
        blockMenu.add(startItem);

        JMenuItem endItem = new JMenuItem(messages.getString("context.blockEnd"));
        endItem.setEnabled(blockStartCol >= 0 && blockStartRow >= 0);
        endItem.addActionListener(e -> createBlock(col, row));
        blockMenu.add(endItem);

        String existingBlockId = blockModel.blockIdForTile(col, row);
        if (existingBlockId != null) {
            Block block = blockModel.getBlock(existingBlockId);
            blockMenu.addSeparator();
            JMenuItem renameItem = new JMenuItem(messages.getString("context.blockRename"));
            renameItem.addActionListener(e -> renameBlock(existingBlockId));
            blockMenu.add(renameItem);
            JMenuItem linkItem = new JMenuItem(messages.getString("context.blockLinks"));
            linkItem.addActionListener(e -> editBlockLinks(existingBlockId));
            blockMenu.add(linkItem);
            // Show "Clear Train" if a train is assigned to this block
            if (block.getAssignedTrainId() != null) {
                JMenuItem clearTrainItem = new JMenuItem(messages.getString("context.blockClearTrain"));
                clearTrainItem.addActionListener(e -> {
                    block.clearAssignedTrain();
                    LOGGER.info("Cleared train from block {} ({})", existingBlockId, block.getName());
                    repaint();
                });
                blockMenu.add(clearTrainItem);
            }
            JMenuItem removeItem = new JMenuItem(messages.getString("context.blockRemove"));
            removeItem.addActionListener(e -> removeBlock(existingBlockId));
            blockMenu.add(removeItem);
        }
        menu.add(blockMenu);
    }

    private void removeBlock(String blockId) {
        Block block = blockModel.getBlock(blockId);
        if (block == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
            MessageFormat.format(messages.getString("block.confirmRemove"), block.getName(), blockId),
            messages.getString("block.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            blockModel.removeBlock(blockId);
            LOGGER.info("Block {} ({}) removed", blockId, block.getName());
            repaint();
        }
    }

    private void createBlock(int endCol, int endRow) {
        if (blockStartCol < 0 || blockStartRow < 0) {
            return;
        }
        if (blockStartCol == endCol && blockStartRow == endRow) {
            blockStartCol = -1;
            blockStartRow = -1;
            repaint();
            return;
        }

        Set<String> excludedTiles = new HashSet<>();
        for (Block b : blockModel.getBlocks().values()) {
            for (int[] p : b.getPath()) {
                excludedTiles.add(Tile.key(p[0], p[1]));
            }
        }

        List<int[]> path = routerService.bfsBlockPath(blockStartCol, blockStartRow, endCol, endRow, excludedTiles);
        if (path == null) {
            LOGGER.info("No block path found from ({},{}) to ({},{})", blockStartCol, blockStartRow, endCol, endRow);
            JOptionPane.showMessageDialog(this, messages.getString("block.noPath"), messages.getString("block.title"), JOptionPane.INFORMATION_MESSAGE);
            blockStartCol = -1;
            blockStartRow = -1;
            repaint();
            return;
        }

        String id = generateBlockId();
        Block block = new Block(id, id, path);
        boolean added = blockModel.addBlock(block);
        if (added) {
            LOGGER.info("Block {} added with {} tiles", id, path.size());
        } else {
            LOGGER.warn("Block {} rejected: tile conflict", id);
        }
        blockStartCol = -1;
        blockStartRow = -1;
        repaint();
    }

    private void renameBlock(String blockId) {
        Block block = blockModel.getBlock(blockId);
        if (block == null) {
            return;
        }
        String newName = (String) JOptionPane.showInputDialog(this,
            messages.getString("block.renamePrompt"),
            messages.getString("block.renameTitle"),
            JOptionPane.PLAIN_MESSAGE, null, null, block.getName());
        if (newName != null && !newName.isBlank()) {
            blockModel.renameBlock(blockId, newName.trim());
            repaint();
        }
    }

    private void editBlockLinks(String blockId) {
        Block block = blockModel.getBlock(blockId);
        if (block == null) {
            return;
        }
        new BlockLinkDialog(blockModel, routerService, messages).show(this, block);
    }

    private void handleTrainDrop(int col, int row, Train train) {
        // Find the block marker tile at this position
        Tile tile = getTile(col, row);
        if (!(tile instanceof ElementTile et) || et.getElementType() != ElementType.BLOCK_MARKER) {
            LOGGER.info("Drop target at ({}, {}) is not a block marker tile", col, row);
            return;
        }
        String blockId = blockModel.blockIdForTile(col, row);
        if (blockId == null) {
            LOGGER.info("No block at ({}, {})", col, row);
            return;
        }
        Block block = blockModel.getBlock(blockId);
        if (block == null) {
            return;
        }
        // Remove train from any previously assigned block
        for (Block b : blockModel.getBlocks().values()) {
            if (train.getId().equals(b.getAssignedTrainId())) {
                b.clearAssignedTrain();
                LOGGER.info("Removed train {} from block {} ({})", train.getId(), b.getId(), b.getName());
            }
        }
        block.setAssignedTrainId(train.getId());
        LOGGER.info("Assigned train {} ({}) to block {} ({})", train.getId(), train.getName(), blockId, block.getName());
        repaint();
    }

    private String generateBlockId() {
        int max = 0;
        for (String id : blockModel.getBlocks().keySet()) {
            if (id.startsWith(BLOCK_ID_PREFIX)) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(BLOCK_ID_PREFIX.length())));
                } catch (NumberFormatException e) {
                    // ignore non-numeric suffixes
                }
            }
        }
        return BLOCK_ID_PREFIX + String.format("%03d", max + 1);
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
            JMenuItem primaryItem = new JMenuItem(messages.getString("context.usePrimaryRoute"));
            primaryItem.addActionListener(e -> {
                routeModel.clearAlternatives(routeId);
                repaint();
            });
            menu.add(primaryItem);
            for (int i = 0; i < alts.size(); i++) {
                Route alt = alts.get(i);
                String label = MessageFormat.format(messages.getString("context.format.alternative"), i + 1, alt.getSourceElementId(), alt.getTargetElementId());
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
            JMenuItem useItem = new JMenuItem(messages.getString("context.useSelectedAlternative"));
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
        JMenuItem clearRouteItem = new JMenuItem(MessageFormat.format(messages.getString("context.format.clearRoute"), routeId));
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
                JMenuItem simItem = new JMenuItem(MessageFormat.format(messages.getString("context.format.simulateOccupancy"), routeId));
                simItem.setEnabled(!isRunning);
                simItem.addActionListener(e -> startRouteOccupancySimulation(r, 200));
                menu.add(simItem);
                if (isRunning) {
                    JMenuItem stopSimItem = new JMenuItem(MessageFormat.format(messages.getString("context.format.stopSimulation"), routeId));
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
                    JMenuItem clearSimItem = new JMenuItem(MessageFormat.format(messages.getString("context.format.clearSimulatedOccupancy"), routeId));
                    clearSimItem.setEnabled(!isRunning);
                    clearSimItem.addActionListener(e -> clearRouteOccupancy(r));
                    menu.add(clearSimItem);
                }
            }
        }
    }

    private void showTileInfo(Tile tile) {
        StringBuilder sb = new StringBuilder();
        sb.append(MessageFormat.format(messages.getString("info.position"), tile.getCol(), tile.getRow())).append("\n");
        sb.append(MessageFormat.format(messages.getString("info.rotation"), tile.getRotation())).append("\n");
        if (tile.getDirection() != TileDirection.BOTH) {
            sb.append(MessageFormat.format(messages.getString("info.direction"), tile.getDirection())).append("\n");
        }
        if (tile.getElementId() != null) {
            sb.append(MessageFormat.format(messages.getString("info.elementId"), tile.getElementId())).append("\n");
        }
        if (tile instanceof ElementTile et) {
            sb.append(MessageFormat.format(messages.getString("info.type"), et.getElementType().getPrefix())).append("\n");
            Element el = model.getElement(tile.getElementId());
            if (el != null) {
                sb.append(MessageFormat.format(messages.getString("info.currentAspect"), el.getCurrentAspect())).append("\n");
                sb.append(MessageFormat.format(messages.getString("info.nodeId"), el.getNodeId())).append("\n");
                sb.append(MessageFormat.format(messages.getString("info.accessoryId"), el.getAccessoryId())).append("\n");
                if (el.getOccupancy() != null) {
                    sb.append(MessageFormat.format(messages.getString("info.occupancy"), el.getOccupancy().getId(), el.getOccupancy().getState())).append("\n");
                }
            }
            sb.append(MessageFormat.format(messages.getString("info.aspects"), et.getAspectCount())).append("\n");
            if (et instanceof SignalTile st) {
                if (et.getElementType() == ElementType.SIGNAL_M3
                    || et.getElementType() == ElementType.SIGNAL_V
                    || et.getElementType() == ElementType.SIGNAL_COMBINED) {
                    SignalSide side = st.getSignalSide();
                    sb.append(MessageFormat.format(messages.getString("info.signalSide"), side));
                    if (side == SignalSide.DEFAULT) {
                        sb.append(MessageFormat.format(messages.getString("info.signalSideResolves"), globalSignalSide));
                    }
                    sb.append("\n");
                }
                if ((et.getElementType() == ElementType.SIGNAL_V
                    || et.getElementType() == ElementType.SIGNAL_COMBINED)
                    && st.getMainSignalId() != null) {
                    sb.append(MessageFormat.format(messages.getString("info.mainSignal"), st.getMainSignalId())).append("\n");
                }
                if (et.getElementType() == ElementType.SIGNAL_COMBINED) {
                    sb.append(MessageFormat.format(messages.getString("info.combinedPlateAspect"), st.getPlateAspect())).append("\n");
                }
            }
            if (et.getElementType() == ElementType.SIGNAL_M3
                || et.getElementType() == ElementType.SIGNAL_COMBINED) {
                List<SignalTile> linked = findDistantSignalsLinkedTo(et.getElementId());
                if (!linked.isEmpty()) {
                    String ids = linked.stream()
                        .map(ElementTile::getElementId)
                        .collect(Collectors.joining(", "));
                    sb.append(MessageFormat.format(messages.getString("info.linkedDistantSignals"), ids)).append("\n");
                }
            }
        }
        else {
            sb.append(messages.getString("info.typePlain")).append("\n");
        }
        Block block = blockModel.getBlockForTile(tile.getCol(), tile.getRow());
        if (block != null) {
            sb.append(MessageFormat.format(messages.getString("info.block"), block.getName(), block.getId())).append("\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), messages.getString("info.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAssignOccupancyDialog(Element el) {
        assignOccupancyDialogFactory.showAssignOccupancyDialog(this, model, el);
    }

    private void onTileContextAction(int col, int row, ElementType type) {
        Tile oldTile = getTile(col, row);
        String oldElementId = oldTile != null ? oldTile.getElementId() : null;
        if (type == null) {
            clearTileWithLinkCheck(col, row);
            return;
        }
        if (oldElementId != null) {
            model.removeElement(oldElementId);
        }
        String id = generateId(type);
        model.addElement(new Element(id, 0, 0));
        Tile newTile = createDefaultTile(col, row, id, type);
        setTile(newTile);
        setSelectedTile(col, row);
        undoStack.push(new TileCommand(this, model, col, row, oldTile, oldElementId, newTile, id));
    }

    /**
     * Clears the tile at the given position, asking the user how to handle any distant signals
     * linked to a main signal being removed. All removals are pushed onto the undo stack so that
     * {@link #undoLast()} restores everything in reverse order.
     */
    void clearTileWithLinkCheck(int col, int row) {
        Tile oldTile = getTile(col, row);
        String oldElementId = oldTile != null ? oldTile.getElementId() : null;
        if (oldTile instanceof ElementTile et && oldElementId != null
            && (et.getElementType() == ElementType.SIGNAL_M3
                || et.getElementType() == ElementType.SIGNAL_COMBINED)) {
            List<SignalTile> linked = findDistantSignalsLinkedTo(oldElementId);
            if (!linked.isEmpty()) {
                switch (confirmRemoveMainSignal(linked)) {
                    case REMOVE_LINKED_OPTION -> linked.forEach(d -> clearTileWithUndo(d.getCol(), d.getRow()));
                    case KEEP_DISTANT_OPTION -> linked.forEach(d -> d.setMainSignalId(null));
                    default -> { return; } // cancelled
                }
            }
        }
        clearTileWithUndo(col, row);
    }

    private void clearTileWithUndo(int col, int row) {
        Tile tile = getTile(col, row);
        String elementId = tile != null ? tile.getElementId() : null;
        if (elementId != null) {
            model.removeElement(elementId);
        }
        removeTile(col, row);
        undoStack.push(new TileCommand(this, model, col, row, tile, elementId, null, null));
    }

    private List<SignalTile> findDistantSignalsLinkedTo(String mainSignalId) {
        List<SignalTile> linked = new ArrayList<>();
        for (Tile t : tiles.values()) {
            if (t instanceof SignalTile det
                && det.getElementType() == ElementType.SIGNAL_V
                && mainSignalId.equals(det.getMainSignalId())) {
                linked.add(det);
            }
        }
        return linked;
    }

    int confirmRemoveMainSignal(List<SignalTile> linked) {
        String ids = linked.stream()
            .map(ElementTile::getElementId)
            .collect(Collectors.joining(", "));
        Object[] options = {
            messages.getString("signal.removeLinked"),
            messages.getString("signal.keep"),
            messages.getString("signal.cancel")
        };
        return JOptionPane.showOptionDialog(this,
            MessageFormat.format(messages.getString("signal.confirmRemoveLinked"), ids),
            messages.getString("signal.confirmRemoveTitle"),
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null, options, options[0]);
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
            case TURNOUT_LEFT -> new ElementTile(col, row, id, type, List.of("/icons/tracks/turnout_straight_left.svg", "/icons/tracks/turnout_diverted_left.svg"));
            case TURNOUT_RIGHT -> new ElementTile(col, row, id, type, List.of("/icons/tracks/turnout_straight_right.svg", "/icons/tracks/turnout_diverted_right.svg"));
            case TURNOUT_3WAY -> new ElementTile(col, row, id, type,
                List.of("/icons/tracks/turnout_3way_straight.svg", "/icons/tracks/turnout_3way_left.svg", "/icons/tracks/turnout_3way_right.svg"));
            case SIGNAL_M3 -> new SignalTile(col, row, id, type, List.of("/icons/signals/sbb_l/signal_m3_red_left.svg", "/icons/signals/sbb_l/signal_m3_green_left.svg", "/icons/signals/sbb_l/signal_m3_yellow_left.svg"));
            case SIGNAL_V -> new SignalTile(col, row, id, type, List.of("/icons/signals/sbb_l/signal_v_orange_left.svg", "/icons/signals/sbb_l/signal_v_yellow_left.svg", "/icons/signals/sbb_l/signal_v_green_left.svg", "/icons/signals/sbb_l/signal_v_aspect3_left.svg"));
            case SIGNAL_COMBINED -> new SignalTile(col, row, id, type, List.of("/icons/signals/sbb_l/signal_sm_head_red_left.svg", "/icons/signals/sbb_l/signal_sm_head_green_left.svg", "/icons/signals/sbb_l/signal_sm_head_orange_left.svg"));
            case STRAIGHT -> new ElementTile(col, row, id, type, List.of("/icons/tracks/straight.svg"));
            case CURVE_LEFT -> new ElementTile(col, row, id, type, List.of("/icons/tracks/curve_left.svg"));
            case CURVE_RIGHT -> new ElementTile(col, row, id, type, List.of("/icons/tracks/curve_right.svg"));
            case DIAGONAL -> new ElementTile(col, row, id, type, List.of("/icons/tracks/diagonal.svg"));
            case DIAGONAL_TURNOUT_RIGHT -> new ElementTile(col, row, id, type, List.of("/icons/tracks/diag_turnout_straight_right.svg", "/icons/tracks/diag_turnout_diverted_right.svg"));
            case DIAGONAL_TURNOUT_LEFT -> new ElementTile(col, row, id, type, List.of("/icons/tracks/diag_turnout_straight_left.svg", "/icons/tracks/diag_turnout_diverted_left.svg"));
            case BLOCK_MARKER -> new ElementTile(col, row, id, type, List.of("/icons/tracks/block_marker.svg"));
            case BUMPER -> new ElementTile(col, row, id, type, List.of("/icons/tracks/bumper_stop.svg"));
        };
    }

    // --- Rendering ---

    @Override
    public void updateUI() {
        super.updateUI();
        setBackground(background());
    }

    @Override
    public DockKey getDockKey() {
        return dockKey;
    }

    @Override
    public Component getComponent() {
        return this;
    }

    private static Color background() {
        Color c = javax.swing.UIManager.getColor("Panel.background");
        return c != null ? c : new Color(45, 45, 50);
    }

    private static Color gridColor() {
        Color c = javax.swing.UIManager.getColor("Component.borderColor");
        return c != null ? c : COLOR_GRID_LINE;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        drawTiles(g2);
        drawGrid(g2);
        drawDirectionMarkers(g2);
        drawBlocks(g2);
        drawSelection(g2);
        if (selectedTiles.isEmpty()) {
            drawRoute(g2);
            drawAlternatives(g2);
        }
        drawSignals(g2);
        drawSignalDirectionMarkers(g2);
        drawBlockMarkerLabels(g2);
        drawOccupancy(g2);
    }

    private void drawBlockMarkerLabels(Graphics2D g2) {
        Font labelFont = g2.getFont().deriveFont(Font.PLAIN, Math.max(8, tileSize / 4f));
        g2.setFont(labelFont);
        for (Tile tile : tiles.values()) {
            if (!(tile instanceof ElementTile et) || et.getElementType() != ElementType.BLOCK_MARKER) {
                continue;
            }
            String blockId = blockModel.blockIdForTile(tile.getCol(), tile.getRow());
            if (blockId == null) {
                continue;
            }
            Block block = blockModel.getBlock(blockId);
            if (block == null) {
                continue;
            }
            int px = tile.getCol() * tileSize;
            int py = tile.getRow() * tileSize;
            g2.setColor(getForeground());
            FontMetrics fm = g2.getFontMetrics();
            // Show train name if assigned, otherwise show block name
            String label = block.getName();
            if (block.getAssignedTrainId() != null) {
                Train train = model.getTrain(block.getAssignedTrainId());
                if (train != null) {
                    label = train.getName();
                }
            }
            int textX = px + tileSize / 2 - fm.stringWidth(label) / 2;
            int textY = py + tileSize - fm.getDescent() - 1;
            g2.drawString(label, textX, textY);
        }
    }

    private void drawBlocks(Graphics2D g2) {
        if (!editMode) {
            return;
        }
        int half = tileSize / 2;
        for (Block block : blockModel.getBlocks().values()) {
            List<int[]> path = block.getPath();
            if (path.isEmpty()) {
                continue;
            }
            g2.setColor(COLOR_BLOCK);
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            List<int[]> pts = new ArrayList<>();
            for (int i = 0; i < path.size(); i++) {
                int[] p = path.get(i);
                int[] dir = segmentDirection(path, i);
                if (isCurveTile(p[0], p[1])) {
                    int[] straight = straightSideDirection(path, i, p);
                    if (straight != null) {
                        dir = straight;
                    }
                }
                int ox = dir[0] == 0 && dir[1] != 0 ? -BLOCK_LINE_OFFSET : 0;
                int oy = dir[0] != 0 ? BLOCK_LINE_OFFSET : 0;
                int[] center = new int[] { p[0] * tileSize + half + ox, p[1] * tileSize + half + oy };
                if (isCurveTile(p[0], p[1]) && i > 0 && i < path.size() - 1) {
                    int[] cpt = curveGuidePoint(p[0], p[1], center, CORNER_PULL);
                    if (exitsThroughCorner(p, path.get(i + 1))) {
                        pts.add(center);
                        pts.add(cpt);
                    } else {
                        pts.add(cpt);
                        pts.add(center);
                    }
                } else {
                    pts.add(center);
                }
            }
            if (path.size() > 1) {
                List<int[]> poly = new ArrayList<>(pts.size() + 2);
                poly.add(blockEndpoint(path, 0));
                poly.addAll(pts);
                poly.add(blockEndpoint(path, path.size() - 1));
                pts = poly;
            }
            int[] xPoints = new int[pts.size()];
            int[] yPoints = new int[pts.size()];
            for (int i = 0; i < pts.size(); i++) {
                xPoints[i] = pts.get(i)[0];
                yPoints[i] = pts.get(i)[1];
            }
            g2.drawPolyline(xPoints, yPoints, pts.size());

            if (path.size() > 1) {
                drawBlockBoundaryTick(g2, path.get(0), path.get(1));
                drawBlockBoundaryTick(g2, path.get(path.size() - 1), path.get(path.size() - 2));
            }
        }

        if (blockStartCol >= 0 && blockStartRow >= 0) {
            int px = blockStartCol * tileSize + half;
            int py = blockStartRow * tileSize + half;
            g2.setColor(COLOR_BLOCK_START);
            g2.fillRect(px - 6, py - 6, 12, 12);
        }
    }

    private int[] segmentDirection(List<int[]> path, int i) {
        if (i < path.size() - 1) {
            return new int[] { path.get(i + 1)[0] - path.get(i)[0], path.get(i + 1)[1] - path.get(i)[1] };
        }
        return new int[] { path.get(i)[0] - path.get(i - 1)[0], path.get(i)[1] - path.get(i - 1)[1] };
    }

    private int[] straightSideDirection(List<int[]> path, int i, int[] p) {
        if (i > 0) {
            int[] prev = path.get(i - 1);
            if (prev[0] == p[0] || prev[1] == p[1]) {
                return new int[] { p[0] - prev[0], p[1] - prev[1] };
            }
        }
        if (i < path.size() - 1) {
            int[] next = path.get(i + 1);
            if (next[0] == p[0] || next[1] == p[1]) {
                return new int[] { next[0] - p[0], next[1] - p[1] };
            }
        }
        return null;
    }

    private int[] blockEndpoint(List<int[]> path, int i) {
        int[] p = path.get(i);
        if (isCurveTile(p[0], p[1])) {
            return curveEndpoint(p[0], p[1], path.get(i == 0 ? 1 : path.size() - 2));
        }
        int[] neighbor = path.get(i == 0 ? 1 : path.size() - 2);
        int dc = neighbor[0] - p[0];
        int dr = neighbor[1] - p[1];
        int half = tileSize / 2;
        if (dc != 0 && dr != 0) {
            return diagonalEndpoint(p[0], p[1], -dc, -dr);
        }
        if (dc != 0) {
            int edgeX = dc > 0 ? p[0] * tileSize : p[0] * tileSize + tileSize;
            int cy = p[1] * tileSize + half + BLOCK_LINE_OFFSET;
            return new int[] { edgeX, cy };
        }
        int edgeY = dr > 0 ? p[1] * tileSize : p[1] * tileSize + tileSize;
        int cx = p[0] * tileSize + half - BLOCK_LINE_OFFSET;
        return new int[] { cx, edgeY };
    }

    /**
     * Endpoint for a block that starts or ends on a diagonal tile: the line runs
     * parallel to the track diagonal (offset on the block side, BLOCK_LINE_OFFSET
     * below the track centre) to the tile edge the track exits through, instead of
     * cutting straight across to the side.
     *
     * @param sx sign of the exit column step (+1 right, -1 left)
     * @param sy sign of the exit row step (+1 down, -1 up)
     */
    private int[] diagonalEndpoint(int col, int row, int sx, int sy) {
        int half = tileSize / 2;
        double cx = col * tileSize + half;
        double cy = row * tileSize + half + BLOCK_LINE_OFFSET;
        double tRight = sx > 0 ? col * tileSize + tileSize - cx : Double.POSITIVE_INFINITY;
        double tLeft = sx < 0 ? cx - col * tileSize : Double.POSITIVE_INFINITY;
        double tBottom = sy > 0 ? row * tileSize + tileSize - cy : Double.POSITIVE_INFINITY;
        double tTop = sy < 0 ? cy - row * tileSize : Double.POSITIVE_INFINITY;
        double t = Math.min(Math.min(tRight, tLeft), Math.min(tBottom, tTop));
        return new int[] { (int) Math.round(cx + sx * t), (int) Math.round(cy + sy * t) };
    }

    /**
     * Endpoint for a block that starts or ends on a curve tile: the line would
     * otherwise run straight across the tile and collide with the track near the
     * corner. Instead it stops a few pixels before the corner, on the block side
     * of the track, so it stays visible next to the main line.
     */
    private int[] curveEndpoint(int col, int row, int[] neighbor) {
        int half = tileSize / 2;
        int dc = neighbor[0] - col;
        int dr = neighbor[1] - row;
        int ox = dc == 0 && dr != 0 ? -BLOCK_LINE_OFFSET : 0;
        int oy = dc != 0 ? BLOCK_LINE_OFFSET : 0;
        int[] blockCenter = new int[] { col * tileSize + half + ox, row * tileSize + half + oy };
        return curveGuidePoint(col, row, blockCenter, CORNER_PULL);
    }

    /**
     * Point on the block line through a curve tile, a few pixels short of the
     * curve corner. The line runs parallel to the track's corner-bound segment
     * (offset on the block side) and is pulled back so it never reaches the
     * corner pixel where the main line bends.
     */
    private int[] curveGuidePoint(int col, int row, int[] blockCenter, int pull) {
        int[] corner = curveCorner(col, row);
        int half = tileSize / 2;
        int[] center = new int[] { col * tileSize + half, row * tileSize + half };
        double dx = corner[0] - center[0];
        double dy = corner[1] - center[1];
        double len = Math.sqrt(dx * dx + dy * dy);
        double ux = dx / len;
        double uy = dy / len;
        double t = (corner[0] - blockCenter[0]) * ux + (corner[1] - blockCenter[1]) * uy;
        double s = Math.max(0, t - pull);
        return new int[] { (int) Math.round(blockCenter[0] + ux * s), (int) Math.round(blockCenter[1] + uy * s) };
    }

    private void drawBlockBoundaryTick(Graphics2D g2, int[] tile, int[] neighbor) {
        int col = tile[0];
        int row = tile[1];
        int dc = neighbor[0] - col;
        int dr = neighbor[1] - row;
        int tickHalf = BLOCK_TICK_LENGTH / 2;
        if (dc != 0 && dr != 0) {
            int[] ep = diagonalEndpoint(col, row, -dc, -dr);
            g2.drawLine(ep[0] - dr * tickHalf, ep[1] - dc * tickHalf, ep[0] + dr * tickHalf, ep[1] + dc * tickHalf);
            return;
        }
        if (dc != 0) {
            int edgeX = dc > 0 ? col * tileSize : col * tileSize + tileSize;
            int cy = row * tileSize + tileSize / 2 + BLOCK_LINE_OFFSET;
            g2.drawLine(edgeX, cy - tickHalf, edgeX, cy + tickHalf);
        } else {
            int edgeY = dr > 0 ? row * tileSize : row * tileSize + tileSize;
            int cx = col * tileSize + tileSize / 2 - BLOCK_LINE_OFFSET;
            g2.drawLine(cx - tickHalf, edgeY, cx + tickHalf, edgeY);
        }
    }

    private boolean isCurveTile(int col, int row) {
        Tile tile = tiles.get(Tile.key(col, row));
        if (!(tile instanceof ElementTile et)) {
            return false;
        }
        ElementType type = et.getElementType();
        return type == ElementType.CURVE_LEFT || type == ElementType.CURVE_RIGHT;
    }

    private int[] curveCorner(int col, int row) {
        Tile tile = tiles.get(Tile.key(col, row));
        int half = tileSize / 2;
        int baseDx = half;
        int baseDy = (tile instanceof ElementTile et && et.getElementType() == ElementType.CURVE_LEFT) ? -half : half;
        int[] d = rotateDelta(baseDx, baseDy, ((tile.getRotation() / 90) % 4));
        return new int[] { col * tileSize + half + d[0], row * tileSize + half + d[1] };
    }

    private static int[] rotateDelta(int dx, int dy, int rotSteps) {
        return switch (((rotSteps % 4) + 4) % 4) {
            case 1 -> new int[] { -dy, dx };
            case 2 -> new int[] { -dx, -dy };
            case 3 -> new int[] { dy, -dx };
            default -> new int[] { dx, dy };
        };
    }

    /**
     * True when the block leaves the curve tile through its corner, i.e. the next
     * tile lies in the quadrant of the curve corner (offset from the tile centre
     * in the same direction as the corner). The block line must then bend from the
     * tile centre toward the corner; otherwise it passes straight through and bends
     * toward the corner on the way in.
     */
    private boolean exitsThroughCorner(int[] p, int[] next) {
        int[] corner = curveCorner(p[0], p[1]);
        int half = tileSize / 2;
        int[] center = new int[] { p[0] * tileSize + half, p[1] * tileSize + half };
        int sx = Integer.signum(corner[0] - center[0]);
        int sy = Integer.signum(corner[1] - center[1]);
        int dc = next[0] - p[0];
        int dr = next[1] - p[1];
        return (dc == 0 || Integer.signum(dc) == sx) && (dr == 0 || Integer.signum(dr) == sy);
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(gridColor());
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
     * Draws a small arrow on each signal tile in edit mode showing the signal's driving
     * direction, i.e. the direction a train travels when it is influenced by the signal
     * (stopped at "halt"). This is independent of the track's direction markers.
     * <p>
     * Signal facing convention: at rotation 0 the signal faces LEFT and stops trains
     * entering from port LEFT (traveling LEFT→RIGHT), so the arrow points toward the exit
     * port {@code (facingPort + 2) % 4}.
     */
    private void drawSignalDirectionMarkers(Graphics2D g2) {
        if (!editMode) {
            return;
        }
        int half = tileSize / 2;
        int arrowSize = Math.max(4, tileSize / 5);
        g2.setColor(COLOR_DIRECTION);
        for (Tile tile : tiles.values()) {
            if (!(tile instanceof ElementTile et)) {
                continue;
            }
            ElementType type = et.getElementType();
            if (type != ElementType.SIGNAL_M3 && type != ElementType.SIGNAL_V
                && type != ElementType.SIGNAL_COMBINED) {
                continue;
            }
            int cx = tile.getCol() * tileSize + half;
            int cy = tile.getRow() * tileSize + half;
            int rotSteps = (tile.getRotation() / 90) % 4;
            double angle = rotSteps * Math.PI / 2;
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

    private void updateSelectedTilesFromDrag() {
        selectedTiles.clear();
        if (selectionDragStartCol < 0 || selectionDragStartRow < 0) {
            return;
        }
        int minCol = Math.min(selectionDragStartCol, selectionDragEndCol);
        int maxCol = Math.max(selectionDragStartCol, selectionDragEndCol);
        int minRow = Math.min(selectionDragStartRow, selectionDragEndRow);
        int maxRow = Math.max(selectionDragStartRow, selectionDragEndRow);
        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                if (getTile(c, r) != null) {
                    selectedTiles.add(Tile.key(c, r));
                }
            }
        }
        if (!selectedTiles.isEmpty()) {
            selectedCol = -1;
            selectedRow = -1;
        }
    }

    private void clearMultiSelection() {
        selectedTiles.clear();
        selectionDragStartCol = -1;
        selectionDragStartRow = -1;
        selectionDragEndCol = -1;
        selectionDragEndRow = -1;
        isDraggingSelection = false;
    }

    private void drawSelection(Graphics2D g2) {
        if (!editMode) {
            return;
        }
        g2.setColor(COLOR_SELECTION);
        g2.setStroke(new BasicStroke(2));
        // Draw single-tile selection
        if (selectedCol >= 0 && selectedRow >= 0) {
            int px = selectedCol * tileSize;
            int py = selectedRow * tileSize;
            g2.drawRect(px + 1, py + 1, tileSize - 2, tileSize - 2);
        }
        // Draw multi-selection highlight
        if (!selectedTiles.isEmpty()) {
            for (String key : selectedTiles) {
                String[] parts = key.split(",");
                int c = Integer.parseInt(parts[0]);
                int r = Integer.parseInt(parts[1]);
                int px = c * tileSize;
                int py = r * tileSize;
                g2.setColor(new Color(0, 200, 200, 40));
                g2.fillRect(px, py, tileSize, tileSize);
                g2.setColor(COLOR_SELECTION);
                g2.drawRect(px + 1, py + 1, tileSize - 2, tileSize - 2);
            }
        }
        // Draw rubber-band rectangle during drag
        if (isDraggingSelection && selectionDragStartCol >= 0 && selectionDragStartRow >= 0) {
            int minCol = Math.min(selectionDragStartCol, selectionDragEndCol);
            int maxCol = Math.max(selectionDragStartCol, selectionDragEndCol);
            int minRow = Math.min(selectionDragStartRow, selectionDragEndRow);
            int maxRow = Math.max(selectionDragStartRow, selectionDragEndRow);
            int px = minCol * tileSize;
            int py = minRow * tileSize;
            int w = (maxCol - minCol + 1) * tileSize;
            int h = (maxRow - minRow + 1) * tileSize;
            g2.setColor(new Color(0, 200, 200, 30));
            g2.fillRect(px, py, w, h);
            g2.setColor(COLOR_SELECTION);
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{4, 4}, 0));
            g2.drawRect(px, py, w, h);
        }
    }

    private void drawRoute(Graphics2D g2) {
        int half = tileSize / 2;

        // Draw all routes from model
        for (Route route : routeModel.getRoutes().values()) {
            List<int[]> path = route.getPath();
            if (path.isEmpty()) continue;

            boolean isNamedRoute = route.getSourceElementId() == null || route.getTargetElementId() == null;
            boolean isSelected = route == selectedRoute;

            // Hide named routes that are not selected
            if (isNamedRoute && !isSelected) continue;

            Color routeColor = isSelected ? COLOR_SELECTED_ROUTE : COLOR_ROUTE;
            Color stopColor = isSelected ? COLOR_SELECTED_ROUTE_STOP : COLOR_TRAIN_ROUTE_STOP;

            drawRouteLine(g2, path, route.getStops(), routeColor, stopColor, false);

            // Signal-to-signal routes: source/target dots + alt indicators
            if (route.getSourceElementId() != null && route.getTargetElementId() != null) {
                int n = path.size();
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
        }

        // Draw creation mode path
        if (routeCreationMode) {
            if (!routeCreationPath.isEmpty()) {
                java.util.List<Route.StationStop> creationStops = new java.util.ArrayList<>();
                for (int idx : routeCreationStops) {
                    creationStops.add(new Route.StationStop(idx, 0));
                }
                drawRouteLine(g2, routeCreationPath, creationStops,
                    COLOR_TRAIN_ROUTE, COLOR_TRAIN_ROUTE_STOP, true);
            }

            if (routeCreationPendingPrimary != null) {
                for (int i = 0; i < routeCreationPendingAlternatives.size(); i++) {
                    List<int[]> altPath = routeCreationPendingAlternatives.get(i);
                    drawPendingAltPath(g2, altPath, altPaletteColor(i));
                }
                drawPendingAltPath(g2, routeCreationPendingPrimary, COLOR_TRAIN_ROUTE);
            }
        }

        if (routeSourceCol >= 0 && routeSourceRow >= 0) {
            int px = routeSourceCol * tileSize + tileSize / 2;
            int py = routeSourceRow * tileSize + tileSize / 2;
            g2.setColor(COLOR_ROUTE_SOURCE);
            g2.fillOval(px - 6, py - 6, 12, 12);
        }
    }

    private static final Color COLOR_TRAIN_ROUTE = new Color(0, 180, 0, 160);
    private static final Color COLOR_TRAIN_ROUTE_STOP = new Color(255, 165, 0, 200);
    private static final Color COLOR_SELECTED_ROUTE = new Color(0, 120, 255, 180);
    private static final Color COLOR_SELECTED_ROUTE_STOP = new Color(255, 100, 200, 200);

    private void drawRouteLine(Graphics2D g2, List<int[]> path, java.util.Collection<Route.StationStop> stops,
                                Color routeColor, Color stopColor, boolean drawSourceMarker) {
        int half = tileSize / 2;
        int n = path.size();
        int[] xPoints = new int[n];
        int[] yPoints = new int[n];
        for (int i = 0; i < n; i++) {
            int[] p = path.get(i);
            xPoints[i] = p[0] * tileSize + half;
            yPoints[i] = p[1] * tileSize + half;
        }

        g2.setColor(routeColor);
        g2.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawPolyline(xPoints, yPoints, n);

        // Draw station stops
        for (Route.StationStop stop : stops) {
            int stopIdx = stop.getPathIndex();
            if (stopIdx >= 0 && stopIdx < n) {
                int[] p = path.get(stopIdx);
                int cx = p[0] * tileSize + half;
                int cy = p[1] * tileSize + half;
                g2.setColor(stopColor);
                int size = 10;
                int[] dx = {cx, cx + size, cx, cx - size};
                int[] dy = {cy - size, cy, cy + size, cy};
                g2.fillPolygon(dx, dy, 4);
            }
        }

        // Draw source marker (only for creation mode)
        if (drawSourceMarker && n > 0) {
            int[] first = path.get(0);
            g2.setColor(Color.GREEN.darker());
            g2.fillOval(first[0] * tileSize + half - 6, first[1] * tileSize + half - 6, 12, 12);
        }
    }

    private void drawPendingAltPath(Graphics2D g2, List<int[]> path, Color color) {
        if (path == null || path.isEmpty()) {
            return;
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
        g2.setColor(color);
        g2.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
            1f, new float[] { 6f, 4f }, 0f));
        g2.drawPolyline(xPoints, yPoints, n);
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

                    if (et.getElementType() == ElementType.DIAGONAL_TURNOUT_RIGHT
                        || et.getElementType() == ElementType.DIAGONAL_TURNOUT_LEFT) {
                        drawDiagonalTurnoutOccupancy(g2, cx, cy, d, rotSteps, et, el);
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
        drawCornerLine(g2, cx, cy, d, rotSteps, ElementType.PORT_LEFT, ElementType.PORT_BOTTOM);
        drawCornerLine(g2, cx, cy, d, rotSteps, ElementType.PORT_TOP, ElementType.PORT_RIGHT);
    }

    private static void drawCornerLine(Graphics2D g2, int cx, int cy, int d, int rotSteps, int portA, int portB) {
        int p1 = (portA + rotSteps) % 4;
        int p2 = (portB + rotSteps) % 4;
        int dx = (p1 == ElementType.PORT_RIGHT || p2 == ElementType.PORT_RIGHT) ? d
            : (p1 == ElementType.PORT_LEFT || p2 == ElementType.PORT_LEFT) ? -d : 0;
        int dy = (p1 == ElementType.PORT_BOTTOM || p2 == ElementType.PORT_BOTTOM) ? d
            : (p1 == ElementType.PORT_TOP || p2 == ElementType.PORT_TOP) ? -d : 0;
        g2.drawLine(cx, cy, cx + dx, cy + dy);
    }

    private void drawDiagonalTurnoutOccupancy(Graphics2D g2, int cx, int cy, int d, int rotSteps,
                                               ElementTile et, Element el) {
        boolean left = et.getElementType() == ElementType.DIAGONAL_TURNOUT_LEFT;
        if (el.getCurrentAspect() == 0) {
            if (left) {
                drawCornerLine(g2, cx, cy, d, rotSteps, ElementType.PORT_RIGHT, ElementType.PORT_BOTTOM);
                drawCornerLine(g2, cx, cy, d, rotSteps, ElementType.PORT_TOP, ElementType.PORT_LEFT);
            } else {
                drawDiagonalOccupancy(g2, cx, cy, d, rotSteps);
            }
            return;
        }
        int heelBase = left ? ElementType.PORT_RIGHT : ElementType.PORT_LEFT;
        int heel = (heelBase + rotSteps) % 4;
        int heelCorner = (ElementType.PORT_BOTTOM + rotSteps) % 4;
        int dx = (heel == ElementType.PORT_LEFT || heel == ElementType.PORT_RIGHT)
            ? (heel == ElementType.PORT_RIGHT ? d : -d) : 0;
        int dy = (heelCorner == ElementType.PORT_TOP || heelCorner == ElementType.PORT_BOTTOM)
            ? (heelCorner == ElementType.PORT_BOTTOM ? d : -d) : 0;
        g2.drawLine(cx, cy, cx + dx, cy + dy);
        int exit = ((left ? ElementType.PORT_LEFT : ElementType.PORT_RIGHT) + rotSteps) % 4;
        drawPortLine(g2, cx, cy, exit, tileSize);
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
            if (route.getSourceElementId() == null || route.getTargetElementId() == null) {
                continue;
            }
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

    private void startRouteOccupancySimulation(Route route, int delay) {
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

        Timer timer = new Timer(delay, e -> {
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
            if (isSignalTile(tile)) {
                drawTile(g2, tile, SIGNAL_BASE_SVG);
                continue;
            }
            drawTile(g2, tile);
        }
    }

    private void drawSignals(Graphics2D g2) {
        for (Tile tile : tiles.values()) {
            if (isSignalTile(tile)) {
                drawTile(g2, tile);
            }
        }
    }

    private boolean isSignalTile(Tile tile) {
        return tile instanceof SignalTile;
    }

    private void drawTile(Graphics2D g2, Tile tile) {
        if (tile instanceof SignalTile et && et.getElementType() == ElementType.SIGNAL_COMBINED) {
            drawCombinedSignalTile(g2, et);
            return;
        }
        String svgPath = resolveSvgResource(tile);
        if (svgPath == null) {
            return;
        }
        drawTile(g2, tile, svgPath);
    }

    /**
     * Renders a combined signal (SIGNAL_COMBINED) by composing its main head fragment (own
     * aspect) and its distant plate fragment (mirrored next-main aspect) into a single cached
     * image that spans two tiles: the head sits in the signal's own tile and the distant plate
     * in the horizontally adjacent tile. Both fragments keep their level aspect ratios; no
     * stretching is applied.
     */
    private void drawCombinedSignalTile(Graphics2D g2, SignalTile et) {
        String headPath = resolveSvgResource(et);
        if (headPath == null) {
            return;
        }
        String platePath = resolvePlateSvgResource(et, headPath);
        if (platePath == null) {
            return;
        }
        CombinedImageKey key = new CombinedImageKey(headPath, platePath, et.getRotation());
        BufferedImage img = combinedImageCache.get(key);
        if (img == null) {
            img = new BufferedImage(3 * tileSize, 3 * tileSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D ig = img.createGraphics();
            try {
                ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ig.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                if (et.getRotation() != 0) {
                    ig.rotate(Math.toRadians(et.getRotation()), 1.5 * tileSize, 1.5 * tileSize);
                }
                // head occupies the centre box, the distant plate the left/right neighbour box
                Graphics2D headCtx = (Graphics2D) ig.create();
                headCtx.translate(tileSize, tileSize);
                renderSvgInto(headCtx, headPath);
                headCtx.dispose();
                boolean plateRight = headPath.contains("_right.svg");
                Graphics2D plateCtx = (Graphics2D) ig.create();
                plateCtx.translate(plateRight ? 2 * tileSize : 0, tileSize);
                renderSvgInto(plateCtx, platePath);
                plateCtx.dispose();
            }
            finally {
                ig.dispose();
            }
            if (combinedImageCache.size() >= TILE_IMAGE_CACHE_MAX) {
                Iterator<CombinedImageKey> it = combinedImageCache.keySet().iterator();
                it.next();
                it.remove();
            }
            combinedImageCache.put(key, img);
        }
        int px = et.getCol() * tileSize - tileSize;
        int py = et.getRow() * tileSize - tileSize;
        g2.drawImage(img, px, py, null);
    }

    private void renderSvgInto(Graphics2D ig, String svgPath) {
        SVGDocument doc = SvgIconLoader.load(svgPath);
        if (doc != null) {
            doc.render(null, ig, new ViewBox(0, 0, tileSize, tileSize));
        }
    }

    /**
     * Resolves the distant plate fragment for a combined signal. When the signal is linked to a
     * main signal ({@code mainSignalId}), the plate live-mirrors that main's current aspect
     * (keeping the {@link OccupancySimulation#distantAspectForMainSignal} mapping, which is the
     * identity for the plate colour order). Otherwise the stored {@link SignalTile#getPlateAspect()}
     * (used by the route simulation to follow the next main ahead) is used, defaulting to orange.
     * The plate keeps the same side (left/right) as the head fragment.
     */
    private String resolvePlateSvgResource(SignalTile et, String headPath) {
        int plateAspect;
        String mainId = et.getMainSignalId();
        if (mainId != null) {
            Integer mainAspect = model.getElementAspect(mainId);
            plateAspect = mainAspect != null
                ? OccupancySimulation.distantAspectForMainSignal(ElementType.SIGNAL_M3, mainAspect)
                : et.getPlateAspect();
        } else {
            plateAspect = et.getPlateAspect();
        }
        String colour = switch (plateAspect) {
            case 1 -> "green";
            case 2 -> "orgreen";
            default -> "orange";
        };
        String side = headPath != null && headPath.contains("_right.svg") ? "_right.svg" : "_left.svg";
        return "/icons/signals/sbb_l/signal_sm_plate_" + colour + side;
    }

    private void drawTile(Graphics2D g2, Tile tile, String svgPath) {
        BufferedImage img = getTileImage(svgPath, tile.getRotation());
        if (img == null) {
            return;
        }
        int px = tile.getCol() * tileSize;
        int py = tile.getRow() * tileSize;
        g2.drawImage(img, px, py, null);
    }

    private BufferedImage getTileImage(String svgPath, int rotation) {
        TileImageKey key = new TileImageKey(svgPath, rotation);
        BufferedImage cached = tileImageCache.get(key);
        if (cached != null) {
            return cached;
        }
        SVGDocument doc = SvgIconLoader.load(svgPath);
        if (doc == null) {
            return null;
        }
        BufferedImage img = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ig = img.createGraphics();
        try {
            ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ig.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (rotation != 0) {
                ig.rotate(Math.toRadians(rotation), tileSize / 2.0, tileSize / 2.0);
            }
            doc.render(null, ig, new ViewBox(0, 0, tileSize, tileSize));
        }
        finally {
            ig.dispose();
        }
        if (tileImageCache.size() >= TILE_IMAGE_CACHE_MAX) {
            Iterator<TileImageKey> it = tileImageCache.keySet().iterator();
            it.next();
            it.remove();
        }
        tileImageCache.put(key, img);
        return img;
    }

    private void clearTileImageCache() {
        tileImageCache.clear();
        combinedImageCache.clear();
    }

    private record TileImageKey(String svgPath, int rotation) {
    }

    private record CombinedImageKey(String headPath, String platePath, int rotation) {
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

        clearMultiSelection();
        selectedCol = col;
        selectedRow = row;
        requestFocusInWindow();

        if (routeCreationMode) {
            if (routeSourceCol < 0) {
                routeSourceCol = col;
                routeSourceRow = row;
                LOGGER.info("Train route source set at ({},{})", col, row);
            }
            else {
                findRouteForCreation(col, row);
            }
            repaint();
            return;
        }

        if (e.isControlDown()) {
            if (!editMode) {
                return;
            }
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
            boolean linkedDistantSignal = et instanceof SignalTile st
                && st.getElementType() == ElementType.SIGNAL_V
                && st.getMainSignalId() != null;
            if (id != null && count > 1 && !linkedDistantSignal) {
                Command cmd = new CycleElementCommand(model, id, count);
                cmd.execute();
                undoStack.push(cmd);
                if (et instanceof SignalTile st
                    && (st.getElementType() == ElementType.SIGNAL_M3
                        || st.getElementType() == ElementType.SIGNAL_COMBINED)) {
                    mirrorLinkedDistantSignals(st);
                }
            }
        }
    }

    /**
     * Keeps every distant signal (SIGNAL_V) linked to the given main signal in sync with its
     * current aspect. Each linked distant signal is switched via a separate undoable command,
     * pushed after the main signal's command so undo restores the distant signal first.
     */
    private void mirrorLinkedDistantSignals(SignalTile mainSignal) {
        ElementType mainType = mainSignal.getElementType();
        String mainId = mainSignal.getElementId();
        int mainAspect = model.getElementAspect(mainId);
        for (Tile t : tiles.values()) {
            if (!(t instanceof SignalTile det)
                || det.getElementType() != ElementType.SIGNAL_V
                || det.getElementId() == null
                || !mainId.equals(det.getMainSignalId())) {
                continue;
            }
            int distantAspect = OccupancySimulation.distantAspectForMainSignal(mainType, mainAspect);
            Command cmd = new SetElementAspectCommand(model, det.getElementId(), distantAspect);
            cmd.execute();
            undoStack.push(cmd);
        }
    }

    private void rotateSelectedTile() {
        if (!editMode || selectedCol < 0 || selectedRow < 0) {
            return;
        }
        Tile tile = getTile(selectedCol, selectedRow);
        if (tile != null) {
            tile.setRotation(tile.getRotation() + 90);
            if (tile instanceof SignalTile st
                && (st.getElementType() == ElementType.SIGNAL_V
                    || st.getElementType() == ElementType.SIGNAL_COMBINED)) {
                autoAssignMainSignalToDistant(st);
            }
            repaint();
        }
    }

    private void moveSelectedTiles(int dCol, int dRow) {
        if (!editMode) {
            return;
        }
        List<String> keysToMove;
        if (!selectedTiles.isEmpty()) {
            keysToMove = new ArrayList<>(selectedTiles);
        }
        else if (selectedCol >= 0 && selectedRow >= 0) {
            keysToMove = List.of(Tile.key(selectedCol, selectedRow));
        }
        else {
            return;
        }
        // Validate all tiles can move to new positions
        for (String key : keysToMove) {
            String[] parts = key.split(",");
            int col = Integer.parseInt(parts[0]);
            int row = Integer.parseInt(parts[1]);
            int newCol = col + dCol;
            int newRow = row + dRow;
            if (newCol < 0 || newCol >= cols || newRow < 0 || newRow >= rows) {
                return;
            }
            String newKey = Tile.key(newCol, newRow);
            if (!keysToMove.contains(newKey) && getTile(newCol, newRow) != null) {
                return;
            }
        }
        MoveTilesCommand cmd = new MoveTilesCommand(this, keysToMove, dCol, dRow);
        cmd.execute();
        undoStack.push(cmd);
        // Update selection to follow moved tiles
        clearMultiSelection();
        for (String key : keysToMove) {
            String[] parts = key.split(",");
            int col = Integer.parseInt(parts[0]);
            int row = Integer.parseInt(parts[1]);
            selectedTiles.add(Tile.key(col + dCol, row + dRow));
        }
        clearTileImageCache();
        repaint();
    }

    /**
     * Auto-assigns a main signal to the given distant signal when one lies along the connected
     * track ahead in the direction of travel (as determined by its current rotation). If the
     * distant signal was linked to a different main signal, that link is replaced so it always
     * previews the main signal now ahead of it; both changes are logged.
     */
    private void autoAssignMainSignalToDistant(SignalTile distantTile) {
        String newMainId = suggestMainSignalForDistant(distantTile);
        if (newMainId == null) {
            return;
        }
        String oldMainId = distantTile.getMainSignalId();
        if (oldMainId != null && !oldMainId.equals(newMainId)) {
            LOGGER.info("Unassigned main signal {} from distant signal {} at ({},{})",
                oldMainId, distantTile.getElementId(), distantTile.getCol(), distantTile.getRow());
        }
        distantTile.setMainSignalId(newMainId);
        syncDistantSignalToAspect(distantTile, newMainId);
        if (oldMainId == null) {
            LOGGER.info("Auto-assigned main signal {} to distant signal {} at ({},{})",
                newMainId, distantTile.getElementId(), distantTile.getCol(), distantTile.getRow());
        } else if (!oldMainId.equals(newMainId)) {
            LOGGER.info("Reassigned distant signal {} at ({},{}) to main signal {}",
                distantTile.getElementId(), distantTile.getCol(), distantTile.getRow(), newMainId);
        }
    }

    /** Switches the distant signal to the preview aspect of the given main signal's current aspect. */
    private void syncDistantSignalToAspect(SignalTile distantTile, String newMainId) {
        ElementTile mainSignal = null;
        for (Tile t : tiles.values()) {
            if (t instanceof ElementTile met
                && (met.getElementType() == ElementType.SIGNAL_M3
                    || met.getElementType() == ElementType.SIGNAL_COMBINED)
                && newMainId.equals(met.getElementId())) {
                mainSignal = met;
                break;
            }
        }
        if (mainSignal == null) {
            return;
        }
        int mainAspect = model.getElementAspect(newMainId);
        int distantAspect = OccupancySimulation.distantAspectForMainSignal(mainSignal.getElementType(), mainAspect);
        if (distantTile.getElementType() == ElementType.SIGNAL_COMBINED) {
            distantTile.setPlateAspect(distantAspect);
            return;
        }
        new SetElementAspectCommand(model, distantTile.getElementId(), distantAspect).execute();
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

    public void setSelectedRoute(Route route) {
        this.selectedRoute = route;
        if (editMode && route != null) {
            routeCreationStops.clear();
            for (Route.StationStop stop : route.getStops()) {
                routeCreationStops.add(stop.getPathIndex());
            }
        }
        repaint();
    }

    public Route getSelectedRoute() {
        return selectedRoute;
    }

    public void testSetRouteSource(int col, int row) {
        routeSourceCol = col;
        routeSourceRow = row;
    }

    public void testFindRoute(int targetCol, int targetRow) {
        findRoute(targetCol, targetRow);
    }

    protected void testTileContextAction(int col, int row, ElementType type) {
        onTileContextAction(col, row, type);
    }

    protected void testRotateSelectedTile() {
        rotateSelectedTile();
    }

    protected void testSetBlockStart(int col, int row) {
        blockStartCol = col;
        blockStartRow = row;
    }

    protected Block testCreateBlock(int endCol, int endRow) {
        if (blockStartCol < 0 || blockStartRow < 0) {
            return null;
        }
        int startCol = blockStartCol;
        int startRow = blockStartRow;
        Set<String> excludedTiles = new HashSet<>();
        for (Block b : blockModel.getBlocks().values()) {
            for (int[] p : b.getPath()) {
                excludedTiles.add(Tile.key(p[0], p[1]));
            }
        }
        List<int[]> path = routerService.bfsBlockPath(startCol, startRow, endCol, endRow, excludedTiles);
        if (path == null) {
            return null;
        }
        String id = generateBlockId();
        Block block = new Block(id, id, path);
        return blockModel.addBlock(block) ? block : null;
    }

    protected void testSetRouteAspects(List<int[]> path) {
        routerService.setRouteAspects(path, model);
    }

    protected void testStartOccupancySimulation(Route route, int delay) {
        startRouteOccupancySimulation(route, delay);
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

    protected Set<String> testGetSelectedTiles() {
        return Set.copyOf(selectedTiles);
    }

    protected void testSelectRegion(int startCol, int startRow, int endCol, int endRow) {
        selectionDragStartCol = startCol;
        selectionDragStartRow = startRow;
        selectionDragEndCol = endCol;
        selectionDragEndRow = endRow;
        updateSelectedTilesFromDrag();
        repaint();
    }

    protected void testMoveSelectedTiles(int dCol, int dRow) {
        moveSelectedTiles(dCol, dRow);
    }

    protected void testShowContextMenu(int col, int row) {
        showContextMenu(col * tileSize, row * tileSize);
    }

    protected void testStartRouteSimulation(org.bidib.switchboard.component.model.Route tr) {
        startRouteSimulation(tr);
    }

    protected void testStopRouteSimulation() {
        stopRouteSimulation();
    }

    protected void testEnterRouteCreationMode() {
        enterRouteCreationMode();
    }

    protected void testFindRouteForCreation(int targetCol, int targetRow) {
        findRouteForCreation(targetCol, targetRow);
    }

    protected java.util.List<java.util.List<int[]>> testGetRouteCreationPendingAlternatives() {
        return getRouteCreationPendingAlternatives();
    }

    protected void testSelectRouteCreationAlternative(int index) {
        selectRouteCreationAlternative(index);
    }

    protected void testSelectRouteCreationPrimary() {
        selectRouteCreationPrimary();
    }

    protected java.util.List<int[]> testExitRouteCreationMode() {
        return exitRouteCreationMode();
    }

    protected java.util.List<int[]> testGetRouteCreationPath() {
        return java.util.Collections.unmodifiableList(routeCreationPath);
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
