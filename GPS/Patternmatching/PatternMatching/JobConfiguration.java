package gps.examples.maximummatching;

import gps.node.GPSJobConfiguration;
import gps.writable.DoubleWritable;

public class JobConfiguration extends GPSJobConfiguration {

	@Override
	public Class<?> getVertexFactoryClass() {
		return MatchingVertex.MatchingVertexFactory.class;
	}

	@Override
	public Class<?> getVertexClass() {
		return MatchingVertex.class;
	}

	@Override
	public Class<?> getVertexValueClass() {
		return MatchingVertexValue.class;
	}

	@Override
	public Class<?> getMessageValueClass() {
		return MatchingMessageValue.class;
	}
	
	@Override
	public Class<?> getMasterClass() {
		return MatchingMaster.class;
	}
	
	@Override
	public Class<?> getEdgeValueClass() {
		return DoubleWritable.class;
	}
}