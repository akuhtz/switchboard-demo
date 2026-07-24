package org.bidib.switchboard.component.config;

import org.bidib.switchboard.component.model.Occupancy.OccupancyState;

public class TestOccupancyFactory implements OccupancyFactory {

    private int counter = 0;

    public TestOccupancy create(OccupancyState state) {
        return new TestOccupancy("ref-" + counter++, state);
    }
}
