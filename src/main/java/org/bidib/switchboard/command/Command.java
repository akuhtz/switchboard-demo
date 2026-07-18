package org.bidib.switchboard.command;

/**
 * Command pattern interface for encapsulating actions with undo/redo support.
 */
public interface Command {

    void execute();

    void undo();
}
