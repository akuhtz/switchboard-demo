package com.bidib.switchboard.view;

import com.bidib.switchboard.command.Command;
import com.bidib.switchboard.command.CycleElementCommand;
import com.bidib.switchboard.model.ElementTile;
import com.bidib.switchboard.model.RailwayModel;
import com.bidib.switchboard.model.Tile;
import com.bidib.switchboard.util.SvgIconLoader;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.view.ViewBox;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

public class SwitchboardPanel extends JPanel implements PropertyChangeListener {

    public static final int DEFAULT_TILE_SIZE = 32;
    public static final int DEFAULT_COLS = 60;
    public static final int DEFAULT_ROWS = 30;

    private static final Color COLOR_BACKGROUND = new Color(45, 45, 50);
    private static final Color COLOR_GRID_LINE = new Color(60, 60, 65);

    private final int tileSize;
    private final int cols;
    private final int rows;
    private final RailwayModel model;

    private final Map<String, Tile> tiles = new LinkedHashMap<>();
    private final Deque<Command> undoStack = new ArrayDeque<>();

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

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
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

    // --- Rendering ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        drawTiles(g2);
        drawGrid(g2);
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
                doc.render(null, tileG, new ViewBox(0, 0, tileSize, tileSize));
            } finally {
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
            Tile tile = getTile(col, row);
            if (tile != null) {
                onTileClicked(tile);
            }
        }
    }

    protected void onTileClicked(Tile tile) {
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
