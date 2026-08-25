package org.bidib.switchboard.component.simulation;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import org.bidib.switchboard.component.service.RouterService;
import org.bidib.switchboard.component.view.TileGrid;

/**
 * Tracks a train's occupied tiles with configurable length.
 * The train consists of a head (current path position) and up to {@code maxLength - 1} cars
 * trailing behind along the physical track.
 */
public class TrainMovement {

    private final int maxLength;
    private final Deque<int[]> positions = new ArrayDeque<>();

    public TrainMovement(int maxLength) {
        if (maxLength < 1) {
            throw new IllegalArgumentException("Train length must be at least 1");
        }
        this.maxLength = maxLength;
    }

    /**
     * Initializes the train at the given head position and walks backward along the physical track
     * to populate up to {@code maxLength - 1} trailing cars.
     *
     * @param head        the starting head position (path index 0)
     * @param grid        the tile grid for physical track connectivity
     * @param router      the router service for finding connected neighbors (may be null)
     */
    public void initialize(int[] head, TileGrid grid, RouterService router) {
        Objects.requireNonNull(head, "head position cannot be null");
        positions.clear();
        positions.addLast(head);

        if (maxLength == 1 || router == null) {
            return;
        }

        int[] cur = head;
        int[] cameFrom = null;

        for (int i = 1; i < maxLength; i++) {
            int[] next = router.pickBackwardNeighbor(grid, cur[0], cur[1], cameFrom);
            if (next == null) {
                break;
            }
            positions.addLast(next);
            cameFrom = cur;
            cur = next;
        }
    }

    /**
     * Advances the train by one step: pushes a new head position and trims the tail
     * if the train exceeds {@code maxLength}.
     *
     * @param newHead the new head position
     * @return list of tile coordinates that were freed (removed from the tail)
     */
    public List<int[]> advance(int[] newHead) {
        Objects.requireNonNull(newHead, "newHead cannot be null");
        positions.addFirst(newHead);
        java.util.List<int[]> freed = new java.util.ArrayList<>();
        while (positions.size() > maxLength) {
            freed.add(positions.removeLast());
        }
        return freed;
    }

    /**
     * Returns an unmodifiable list of currently occupied tile coordinates, head first.
     */
    public List<int[]> getPositions() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(positions));
    }

    /** Returns the head position (first element), or null if empty. */
    public int[] getHead() {
        return positions.peekFirst();
    }

    /** Returns the tail position (last element), or null if empty. */
    public int[] getTail() {
        return positions.peekLast();
    }

    /** Returns the current number of occupied tiles (up to maxLength). */
    public int length() {
        return positions.size();
    }

    /** Returns the configured maximum train length. */
    public int getMaxLength() {
        return maxLength;
    }

    /** Clears all positions. */
    public void clear() {
        positions.clear();
    }

    /** Returns true if the train has no positions. */
    public boolean isEmpty() {
        return positions.isEmpty();
    }
}