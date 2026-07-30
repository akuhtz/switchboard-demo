package org.bidib.switchboard.component.model;

/**
 * Controls which side of the track the signal body is displayed on.
 * LEFT = Swiss default (signal above track for horizontal tiles).
 * RIGHT = German default (signal below track for horizontal tiles).
 * DEFAULT = use the global application setting.
 */
public enum SignalSide {
    LEFT,
    RIGHT,
    DEFAULT
}
