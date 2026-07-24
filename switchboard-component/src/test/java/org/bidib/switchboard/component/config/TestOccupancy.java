package org.bidib.switchboard.component.config;

import org.bidib.switchboard.component.model.Occupancy;

public class TestOccupancy extends Occupancy {

    private static final long serialVersionUID = 1L;

    private final String extReference;

    public TestOccupancy(String extReference, OccupancyState state) {
        super(state);
        this.extReference = extReference;
    }

    public String getExtReference() {
        return extReference;
    }
}
