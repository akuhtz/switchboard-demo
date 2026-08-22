package org.bidib.switchboard.component.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
            // Clean up links from/to the removed block.
            for (String predId : removed.getPredecessorIds()) {
                Block pred = blocks.get(predId);
                if (pred != null) {
                    pred.removeSuccessor(id);
                }
            }
            for (String succId : removed.getSuccessorIds()) {
                Block succ = blocks.get(succId);
                if (succ != null) {
                    succ.removePredecessor(id);
                }
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

    /**
     * Links two blocks as predecessor → successor.
     *
     * @throws IllegalArgumentException if either block does not exist or if the IDs are the same.
     */
    public void linkBlocks(String predecessorId, String successorId) {
        if (predecessorId.equals(successorId)) {
            throw new IllegalArgumentException("A block cannot be linked to itself");
        }
        Block pred = blocks.get(predecessorId);
        Block succ = blocks.get(successorId);
        if (pred == null || succ == null) {
            throw new IllegalArgumentException("Block not found");
        }
        pred.addSuccessor(successorId);
        succ.addPredecessor(predecessorId);
        pcs.firePropertyChange(PROP_BLOCKS, null, pred);
    }

    /**
     * Removes the predecessor → successor link between two blocks.
     */
    public void unlinkBlocks(String predecessorId, String successorId) {
        Block pred = blocks.get(predecessorId);
        Block succ = blocks.get(successorId);
        if (pred != null) {
            pred.removeSuccessor(successorId);
        }
        if (succ != null) {
            succ.removePredecessor(predecessorId);
        }
        pcs.firePropertyChange(PROP_BLOCKS, null, pred);
    }

    /** Returns the predecessor blocks of the given block. */
    public List<Block> getPredecessors(Block block) {
        List<Block> result = new ArrayList<>();
        for (String id : block.getPredecessorIds()) {
            Block b = blocks.get(id);
            if (b != null) {
                result.add(b);
            }
        }
        return result;
    }

    /** Returns the successor blocks of the given block. */
    public List<Block> getSuccessors(Block block) {
        List<Block> result = new ArrayList<>();
        for (String id : block.getSuccessorIds()) {
            Block b = blocks.get(id);
            if (b != null) {
                result.add(b);
            }
        }
        return result;
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
