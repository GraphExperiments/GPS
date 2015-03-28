package gps.examples.coloring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;

import gps.examples.coloring.ColoringOptions.ComputationStage;
import gps.globalobjects.IntOverwriteGlobalObject;
import gps.globalobjects.IntSetGlobalObject;
import gps.globalobjects.IntSumGlobalObject;
import gps.globalobjects.IntegerIntegerSumMapGObj;
import gps.graph.Master;
import gps.writable.IntSetWritable;
import gps.writable.IntWritable;
import gps.writable.LongWritable;
import gps.writable.NodeWritable;
import gps.writable.NullValueGraphWritable;

public class ColoringMaster extends Master {

	protected ColoringOptions options;
	protected int latestColor = 0;
	private int lengthOfColoringCycle = 0;
	private boolean firstMIS1 = false;

	public ColoringMaster(CommandLine commandLine) {
		options = new ColoringOptions();
		options.parseOtherOpts(commandLine);
	}

	@Override
	public void compute(int superstepNo) {
		if (superstepNo == 1) {
			clearGlobalObjectsAndSetComputationStage(ComputationStage.MIS_1);
			firstMIS1 = true;
			return;
		}
		ComputationStage compStage = ComputationStage.getComputationStageFromId(
			((IntWritable) getGlobalObjectsMap().getGlobalObject(
			ColoringOptions.GOBJ_COMP_STAGE).getValue()).getValue());
		System.out.println("Previous stage: " + compStage);
		long numEdgesOfPotentiallyActiveVertices = -1;
		if (getGlobalObjectsMap().getGlobalObject(
			ColoringOptions.GOBJ_NUM_EDGES_OF_POTENTIALLY_ACTIVE_VERTICES) != null) {
			numEdgesOfPotentiallyActiveVertices = ((LongWritable) getGlobalObjectsMap().getGlobalObject(
				ColoringOptions.GOBJ_NUM_EDGES_OF_POTENTIALLY_ACTIVE_VERTICES).getValue()).value;
			System.out.println("numEdgesOfPotentiallyActiveVertices: " + numEdgesOfPotentiallyActiveVertices);
		}
		lengthOfColoringCycle++;
		switch (compStage) {
		case MIS_1:
			if (firstMIS1 && options.coloringFCS && numEdgesOfPotentiallyActiveVertices < options.thresholdForColoringFCS) {
				clearGlobalObjectsAndSetComputationStage(ComputationStage.FCS_COLORING_GRAPH_FORMATION);
			} else {
				clearGlobalObjectsAndSetComputationStage(ComputationStage.MIS_2);
			}
			break;
		case MIS_2:
			clearGlobalObjectsAndSetComputationStage(ComputationStage.MIS_3);
			break;
		case MIS_3:
			clearGlobalObjectsAndSetComputationStage(ComputationStage.MIS_4);
			break;
		case MIS_4:
			if (numEdgesOfPotentiallyActiveVertices <= 0) {
				System.out.println("numEdgesOfPotentiallyActiveVertices is 0");
				setPhaseToColoringAndPutColorInGlobalObjects();
			} else {
				if (options.misFCS && numEdgesOfPotentiallyActiveVertices < options.thresholdForMISFCS) {
					clearGlobalObjectsAndSetComputationStage(ComputationStage.FCS_MIS_GRAPH_FORMATION);
				} else {
					clearGlobalObjectsAndSetComputationStage(ComputationStage.MIS_1);
					firstMIS1 = false;
				}
			}
			break;
		case COLORING:
			IntSumGlobalObject notColoredVertices = (IntSumGlobalObject) getGlobalObjectsMap().getGlobalObject(
				ColoringOptions.GOBJ_NUM_NOT_COLORED_VERTICES);
			System.out.println("lengthOfColoringCycle: " + lengthOfColoringCycle);
			lengthOfColoringCycle = 0;
			if (notColoredVertices != null) {
				System.out.println("numNotColoredVertices: "
					+ notColoredVertices.getValue().getValue());
				clearGlobalObjectsAndSetComputationStage(ComputationStage.MIS_1);
				firstMIS1 = true; 
			} else {
				this.continueComputation = false;
			}
			break;
		case FCS_MIS_GRAPH_FORMATION:
			NullValueGraphWritable graph = (NullValueGraphWritable) getGlobalObjectsMap()
				.getGlobalObject(ColoringOptions.GOBJ_GRAPH).getValue();
			Set<Integer> results = performSerialMIS(graph);
			System.out.println("graph size: " + graph.nodes.size());
			clearGlobalObjectsAndSetComputationStage(ComputationStage.FCS_MIS_RESULT_FINDING_1);
			getGlobalObjectsMap().putGlobalObject(ColoringOptions.GOBJ_RESULTS,
				new IntSetGlobalObject(new IntSetWritable(results)));
			break;
		case FCS_MIS_RESULT_FINDING_1:
			clearGlobalObjectsAndSetComputationStage(ComputationStage.FCS_MIS_RESULT_FINDING_2);
			break;
		case FCS_MIS_RESULT_FINDING_2:
			setPhaseToColoringAndPutColorInGlobalObjects();
			break;
		case FCS_COLORING_GRAPH_FORMATION:
			graph = (NullValueGraphWritable) getGlobalObjectsMap().getGlobalObject(
				ColoringOptions.GOBJ_GRAPH).getValue();
			Map<Integer, Integer> resultColors = performSerialColoring(graph);
			clearGlobalObjectsAndSetComputationStage(ComputationStage.FCS_COLORING_RESULT_FINDING);
			getGlobalObjectsMap().putGlobalObject(ColoringOptions.GOBJ_RESULTS,
				new IntegerIntegerSumMapGObj(resultColors));
			break;
		case FCS_COLORING_RESULT_FINDING:
			terminateComputation();
			break;
		default:
			throw new RuntimeException("Unknown computation stage: " + compStage);
		}
	}

	private void setPhaseToColoringAndPutColorInGlobalObjects() {
		clearGlobalObjectsAndSetComputationStage(ComputationStage.COLORING);				
		latestColor++;
		getGlobalObjectsMap().putGlobalObject(ColoringOptions.GOBJ_COLOR,
			new IntOverwriteGlobalObject(latestColor));
	}

	private Map<Integer, Integer> performSerialColoring(NullValueGraphWritable graph) {
		Map<Integer, Integer> results = new HashMap<Integer, Integer>();
		List<Integer> neighborColors;
		Integer neighborColor;
		latestColor++;
		for (NodeWritable node : graph.nodes) {
			neighborColors = new ArrayList<Integer>(node.neighbors.length);
			for (int neighborId : node.neighbors) {
				neighborColor = results.get(neighborId);
				if (neighborColor != null) {
					neighborColors.add(neighborColor);
				}
			}
			Collections.sort(neighborColors);
			int previousNeighborColor = -1;
			for (int neigborColor : neighborColors) {
				if (previousNeighborColor < 0) {
					previousNeighborColor = neigborColor;
				} else if (previousNeighborColor == neigborColor) {
					continue;
				} else if (neigborColor == (previousNeighborColor + 1)) {
					previousNeighborColor = neigborColor;
				} else {
					break;
				}
			}
			if (previousNeighborColor == -1) {
				results.put(node.vertexId, latestColor);
			} else {
				results.put(node.vertexId, previousNeighborColor + 1);				
			}
		}
		return results;
	}

	private Set<Integer> performSerialMIS(NullValueGraphWritable graph) {
		System.out.println("performing serial mis...");
		Set<Integer> retVal = new HashSet<Integer>();
		boolean putInSet;
		for (NodeWritable node : graph.nodes) {
			putInSet = true;
			for (int neighborId : node.neighbors) {
				if (neighborId < 0) {
					continue;
				}
				if (retVal.contains(neighborId)) {
					putInSet = false;
					break;
				}
			}
			if (putInSet) {
				retVal.add(node.vertexId);
			}
		}
		return retVal;
	}

	protected void clearGlobalObjectsAndSetComputationStage(ComputationStage compStage) {
		getGlobalObjectsMap().clearNonDefaultObjects();
		getGlobalObjectsMap().putGlobalObject(ColoringOptions.GOBJ_COMP_STAGE,
			new IntOverwriteGlobalObject(compStage.getId()));
		System.out.println("Next compStage: " + compStage);
	}
}