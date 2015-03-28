package gps.examples.coloring;

import gps.node.GPSJobConfiguration;

public class JobConfiguration extends GPSJobConfiguration {

	@Override
	public Class<?> getVertexFactoryClass() {
		return ColoringVertex.ColoringVertexFactory.class;
	}

	@Override
	public Class<?> getVertexClass() {
		return ColoringVertex.class;
	}

	@Override
	public Class<?> getVertexValueClass() {
		return ColoringVertexValue.class;
	}

	@Override
	public Class<?> getMessageValueClass() {
		return ColoringMessage.class;
	}
	
	@Override
	public Class<?> getMasterClass() {
		return ColoringMaster.class;
	}
}