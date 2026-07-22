package org.bidib.switchboard.component.config;

import org.bidib.switchboard.component.model.Occupancy.OccupancyState;

public class TestOccupancyFactory implements OccupancyFactory {

	public TestOccupancy create(long nodeId, int portId, OccupancyState state) {
		return new TestOccupancy(nodeId, portId, state);
	}

}
