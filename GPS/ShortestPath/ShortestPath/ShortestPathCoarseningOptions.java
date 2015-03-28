package gps.examples.randomgraphcoarsening;

import gps.node.GPSNodeRunner;

import org.apache.commons.cli.CommandLine;

public class RandomGraphCoarseningOptions {

	protected int numDegreesThreshold = 100;
	protected int numIterations = 1;
	protected double sparsificationPercentage = 1.0;

	protected static final String NUM_EDGES_CROSSING_CLUSTERS = "go-num-edges-crossing-clusters";
	protected static final String NUM_LATEST_EDGES_CROSSING_CLUSTERS = "go-num-latest-edges-crossing-clusters";
	protected static final String NUM_EDGES_WITHIN_CLUSTERS = "go-num-edges-within-clusters";
	protected static final String NUM_LATEST_EDGES_WITHIN_CLUSTERS = "go-num-latest-edges-within-clusters";
	protected static final String NUM_SUPERNODES = "go-num-supernodes";
	protected static final String NUM_LATEST_SUPERNODES = "go-num-latest-supernodes";
	protected static final String NUM_TC_NOT_CONVERGED_VERTICES = "go-num-tc-not-converged-vertices";
	protected static final String NUM_SINGLETONS_REMOVED = "go-num-singletons-removed";
	protected static final String COMPUTATION_STAGE_GO_KEY = "go-stage";

	protected void parseOtherOpts(CommandLine commandLine) {
		String otherOptsStr = commandLine.getOptionValue(GPSNodeRunner.OTHER_OPTS_OPT_NAME);
		System.out.println("otherOptsStr: " + otherOptsStr);
		if (otherOptsStr != null) {
			String[] split = otherOptsStr.split("###");
			for (int index = 0; index < split.length; ) {
				String flag = split[index++];
				String value = split[index++];
				if ("-sp".equals(flag)) {
					sparsificationPercentage = Double.parseDouble(value);
				} else if ("-ni".equals(flag)) {
					numIterations = Integer.parseInt(value);
				} else if ("-ndt".equals(flag)) {
					numDegreesThreshold = Integer.parseInt(value);
				}
			}
		}

		System.out.println("numDegreesThreshold: " + numDegreesThreshold);
		System.out.println("numIterations: " + numIterations);
		System.out.println("sparsificationPercentage: " + sparsificationPercentage);
	}
}
