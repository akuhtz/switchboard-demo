package org.bidib.switchboard.component.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages all blocks on a switchboard and enforces that every tile belongs to
 * at most one block.
 */
public class BlockModel {

    public static final String PROP_BLOCKS = "blocks";

    private final Map<String, Block> blocks = new LinkedHashMap<>();

    private final Map<String, String> tileToBlock = new HashMap<>();

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * Adds a block. Returns {@code false} if any tile of the block already
     * belongs to a different block.
     */
    public boolean addBlock(Block block) {
        for (int[] p : block.getPath()) {
            String key = Tile.key(p[0], p[1]);
            String existing = tileToBlock.get(key);
            if (existing != null && !existing.equals(block.getId())) {
                return false;
            }
        }
        Block previous = blocks.put(block.getId(), block);
        for (int[] p : block.getPath()) {
            tileToBlock.put(Tile.key(p[0], p[1]), block.getId());
        }
        pcs.firePropertyChange(PROP_BLOCKS, previous, block);
        return true;
    }

    public Block removeBlock(String id) {
        Block removed = blocks.remove(id);
        if (removed != null) {
            for (int[] p : removed.getPath()) {
                tileToBlock.remove(Tile.key(p[0], p[1]));
            }
            pcs.firePropertyChange(PROP_BLOCKS, removed, null);
        }
        return removed;
    }

    public void renameBlock(String id, String newName) {
        Block block = blocks.get(id);
        if (block != null) {
            block.setName(newName);
            pcs.firePropertyChange(PROP_BLOCKS, null, block);
        }
    }

    public Block getBlock(String id) {
        return blocks.get(id);
    }

    public Map<String, Block> getBlocks() {
        return Collections.unmodifiableMap(blocks);
    }

    /** Returns the id of the block a tile belongs to, or null. */
    public String blockIdForTile(int col, int row) {
        return tileToBlock.get(Tile.key(col, row));
    }

    /** Returns the block a tile belongs to, or null. */
    public Block getBlockForTile(int col, int row) {
        String id = blockIdForTile(col, row);
        return id != null ? blocks.get(id) : null;
    }

    public void clear() {
        blocks.clear();
        tileToBlock.clear();
        pcs.firePropertyChange(PROP_BLOCKS, null, null);
    }

    public int size() {
        return blocks.size();
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}
