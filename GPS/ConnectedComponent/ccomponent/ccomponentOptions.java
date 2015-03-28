package gps.examples.coloring;

import java.util.HashMap;
import java.util.Map;

import gps.node.GPSNodeRunner;

import org.apache.commons.cli.CommandLine;

public class ColoringOptions {

	public static final String MIS_FCS = "misfcs";
	public static final String THRESHOLD_FOR_MIS_FCS = "tmisfcs";
	public static final String COLORING_FCS = "coloringfcs";
	public static final String THRESHOLD_FOR_COLORING_FCS = "tcoloringfcs";

	// Global Objects
	public static final String GOBJ_COMP_STAGE = "stage";
	public static final String GOBJ_COLOR = "color";
	public static final String GOBJ_NUM_EDGES_OF_POTENTIALLY_ACTIVE_VERTICES = "nepav";
	public static final String GOBJ_NUM_NOT_COLORED_VERTICES = "nncv";
	public static final String GOBJ_NUM_EDGES_OF_NOT_COLORED_VERTICES = "nencv";
	public static final String GOBJ_GRAPH = "gr";
	public static final String GOBJ_RESULTS = "re";
	
	public boolean misFCS = false;
	public boolean coloringFCS = false;
	public int thresholdForMISFCS = 500000;
	public int thresholdForColoringFCS = 5000000;

	protected void parseOtherOpts(CommandLine commandLine) {
		String otherOptsStr = commandLine.getOptionValue(GPSNodeRunner.OTHER_OPTS_OPT_NAME);
		System.out.println("otherOptsStr: " + otherOptsStr);
		if (otherOptsStr != null) {
			String[] split = otherOptsStr.split("###");
			for (int index = 0; index < split.length; ) {
				String flag = split[index++];
				String value = split[index++];
				if (THRESHOLD_FOR_MIS_FCS.equals(flag)) {
					thresholdForMISFCS = Integer.parseInt(value);
				} else if (THRESHOLD_FOR_COLORING_FCS.equals(flag)) {
					thresholdForColoringFCS = Integer.parseInt(value);
				} else if (MIS_FCS.equals(flag)) {
					misFCS = Boolean.parseBoolean(value);
				} else if (COLORING_FCS.equals(flag)) {
					coloringFCS = Boolean.parseBoolean(value);
				}
			}
		}
	}

	public static enum ComputationStage {
		MIS_1(0),
		MIS_2(1),
		MIS_3(2),
		MIS_4(3),
		COLORING(4),
		FCS_MIS_GRAPH_FORMATION(5),
		// FCS to the MIS phase of the coloring algorithm
		FCS_MIS_RESULT_FINDING_1(6),
		FCS_MIS_RESULT_FINDING_2(7),
		FCS_COLORING_GRAPH_FORMATION(8),
		// FCS to the entire coloring algorithm
		FCS_COLORING_RESULT_FINDING(9);

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
