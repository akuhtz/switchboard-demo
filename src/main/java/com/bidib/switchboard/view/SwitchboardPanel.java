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
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidib.switchboard.command.Command;
import com.bidib.switchboard.command.CycleElementCommand;
import com.bidib.switchboard.model.ElementTile;
import com.bidib.switchboard.model.ElementType;
import com.bidib.switchboard.model.RailwayModel;
import com.bidib.switchboard.model.Tile;
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

    private final int tileSize;

    private final int cols;

    private final int rows;

    private final RailwayModel model;

    private final Map<String, Tile> tiles = new LinkedHashMap<>();

    private final Deque<Command> undoStack = new ArrayDeque<>();

    private TileContextHandler tileContextHandler;

    private int selectedCol = -1;

    private int selectedRow = -1;

    private boolean editMode;

    public SwitchboardPanel(RailwayModel model) {
        this(model, DEFAULT_COLS, DEFAULT_ROWS, DEFAULT_TILE_SIZE);
    }

    public SwitchboardPanel(RailwayModel model, int cols, int rows, int tileSize) {
        this.model = model;
        this.cols = cols;
        this.rows = rows;
        this.tileSize = tileSize;
        model.addPropertyChangeListener(this);
        setBackground(COLOR_BACKGROUND);
        setPreferredSize(new Dimension(cols * tileSize, rows * tileSize));
        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    handleClick(e.getX(), e.getY());
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

    public void clearTiles() {
        tiles.clear();
        repaint();
    }

    public void removeTile(int col, int row) {
        tiles.remove(tileKey(col, row));
        if (selectedCol == col && selectedRow == row) {
            selectedCol = -1;
            selectedRow = -1;
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

    // --- Context menu ---

    private void showContextMenu(int x, int y) {
        int col = x / tileSize;
        int row = y / tileSize;
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return;
        }

        JPopupMenu menu = new JPopupMenu();
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

        if (getTile(col, row) != null) {
            menu.addSeparator();
            JMenuItem clearItem = new JMenuItem("Clear");
            clearItem.addActionListener(e -> {
                if (tileContextHandler != null) {
                    tileContextHandler.handle(col, row, null);
                }
            });
            menu.add(clearItem);
        }

        menu.show(this, x, y);
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

    private void handleClick(int x, int y) {
        int col = x / tileSize;
        int row = y / tileSize;
        if (col >= 0 && col < cols && row >= 0 && row < rows) {
            selectedCol = col;
            selectedRow = row;
            requestFocusInWindow();
            Tile tile = getTile(col, row);

            LOGGER.info("Click on col: {}, row: {}, tile: {}", col, row, tile);

            if (tile != null) {
                onTileClicked(tile);
            }
            repaint();
        }
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

    // --- Observer ---

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        SwingUtilities.invokeLater(this::repaint);
    }

    // --- Internal ---

    private static String tileKey(int col, int row) {
        return col + "," + row;
    }
}
