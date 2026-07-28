package org.bidib.switchboard.component.model;

/**
 * Direction constraint for tile traversal.
 * <p>
 * FORWARD means trains can only move from the "entry" port to the "exit" port:
 * <ul>
 *   <li>STRAIGHT rotation 0: LEFT → RIGHT</li>
 *   <li>STRAIGHT rotation 90: TOP → BOTTOM</li>
 *   <li>DIAGONAL rotation 0: lower-left → upper-right</li>
 * </ul>
 * BACKWARD is the reverse. BOTH allows traversal in either direction (default).
 */
public enum TileDirection {
    FORWARD,
    BACKWARD,
    BOTH
}
