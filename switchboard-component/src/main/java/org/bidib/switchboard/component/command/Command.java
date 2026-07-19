package org.bidib.switchboard.component.command;

/**
 * Command pattern interface for encapsulating actions with undo/redo support.
 */
public interface Command {

    void execute();

    void undo();
}
