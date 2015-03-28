package gps.examples.maximummatching;

import java.util.HashMap;
import java.util.Map;

import gps.node.GPSNodeRunner;

import org.apache.commons.cli.CommandLine;

public class MatchingOptions {

	public static final String EDGE_CLEANING_METHOD_SWITCHING_THRESHOLD = "ecmst";
	public static final String ECOD = "ecod";
	public static final String MATCHED_VERTICES_SEND_IN_ECOD_EDGE_CLEANING = "mvs";
	// Global Objects
	public static final String GOBJ_COMP_STAGE = "stage";
	public static final String GOBJ_NUM_UNMATCHED_VERTICES = "nuv";
	public static final String GOBJ_NUM_HIGH_DEGREE_CLEANING_VERTICES = "nhdcv";
	public static final String GOBJ_SINGLE_EDGE_CLEANING = "gosec";
	public static final String GOBJ_NUM_EDGES_OF_UNMATCHED_VERTICES = "neuv";

	public boolean isECOD = false;
	public int edgeCleaningMethodSwitchingThreshold = Integer.MIN_VALUE;
	public boolean matchedVerticesSendIDsInECODEdgeCleaning = false;

	protected void parseOtherOpts(CommandLine commandLine) {
		String otherOptsStr = commandLine.getOptionValue(GPSNodeRunner.OTHER_OPTS_OPT_NAME);
		System.out.println("otherOptsStr: " + otherOptsStr);
		if (otherOptsStr != null) {
			String[] split = otherOptsStr.split("###");
			for (int index = 0; index < split.length; ) {
				String flag = split[index++];
				String value = split[index++];
				if (flag.equals(EDGE_CLEANING_METHOD_SWITCHING_THRESHOLD)) {
					edgeCleaningMethodSwitchingThreshold = Integer.parseInt(value);
				} else if (flag.equals(ECOD)) {
					isECOD = Boolean.parseBoolean(value);
				} else if (flag.equals(MATCHED_VERTICES_SEND_IN_ECOD_EDGE_CLEANING)) {
					matchedVerticesSendIDsInECODEdgeCleaning = Boolean.parseBoolean(value);
				}
			}
			System.out.println("isECOD: " + isECOD);
			System.out.println("matchedVerticesSend: " + matchedVerticesSendIDsInECODEdgeCleaning);
		}
	}

	public static enum ComputationStage {
		MAX_EDGE_PICKING_AND_EDGE_CLEANING(0),
		MATCHING(1),
		ECOD_FULL_EDGE_CLEANING_1(2),
		ECOD_FULL_EDGE_CLEANING_2(3),
		ECOD_FULL_EDGE_CLEANING_3(4);

		private static Map<Integer, ComputationStage> idComputationStateMap =
			new HashMap<Integer, ComputationStage>();
		static {
			for (ComputationStage type : ComputationStage.values()) {
				idComputationStateMap.put(type.id, type);
			}
		}

		private int id;

		private ComputationStage(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public static ComputationStage getComputationStageFromId(int id) {
			return idComputationStateMap.get(id);
		}
	}
}