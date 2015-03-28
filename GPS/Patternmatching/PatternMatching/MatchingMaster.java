package gps.examples.maximummatching;

import org.apache.commons.cli.CommandLine;

import gps.examples.maximummatching.MatchingOptions.ComputationStage;
import gps.globalobjects.BooleanOverwriteGlobalObject;
import gps.globalobjects.IntOverwriteGlobalObject;
import gps.globalobjects.IntSumGlobalObject;
import gps.globalobjects.LongSumGlobalObject;
import gps.graph.Master;
import gps.writable.IntWritable;

public class MatchingMaster extends Master {

	protected MatchingOptions options;
	private int previousNumUnmatchedVertices = -1;
	private boolean isECOD = false;
	private int diffNumUnmatchedVertices;
	
	public MatchingMaster(CommandLine commandLine) {
		options = new MatchingOptions();
		options.parseOtherOpts(commandLine);
		isECOD = options.isECOD;
	}

	@Override
	public void compute(int superstepNo) {
		if (superstepNo == 1) {
			clearGlobalObjectsAndSetComputationStage(ComputationStage.MAX_EDGE_PICKING_AND_EDGE_CLEANING);
			return;
		}
		ComputationStage compStage = ComputationStage.getComputationStageFromId(
			((IntWritable) getGlobalObjectsMap().getGlobalObject(
			MatchingOptions.GOBJ_COMP_STAGE).getValue()).getValue());
		System.out.println("Previous stage: " + compStage);
		switch (compStage) {
		case MAX_EDGE_PICKING_AND_EDGE_CLEANING:
			IntSumGlobalObject numHighDegreeCleaningVerticesGO = (IntSumGlobalObject) globalObjectsMap.getGlobalObject(
				MatchingOptions.GOBJ_NUM_HIGH_DEGREE_CLEANING_VERTICES);
			if (numHighDegreeCleaningVerticesGO != null) {
				System.out.println("numHighDegreeCleaningVertices: " + numHighDegreeCleaningVerticesGO.getValue().value);
			}
			if (!terminateIfNumActiveVerticesIsZero()) {
				clearGlobalObjectsAndSetComputationStage(ComputationStage.MATCHING);
				getGlobalObjectsMap().putGlobalObject(MatchingOptions.GOBJ_SINGLE_EDGE_CLEANING,
					new BooleanOverwriteGlobalObject(isECOD));
			}
			System.out.println("previousSingleEdgeCleaning: " + isECOD);
			break;
		case MATCHING:
			IntSumGlobalObject numUnmatchedVerticesGO = (IntSumGlobalObject) globalObjectsMap.getGlobalObject(
				MatchingOptions.GOBJ_NUM_UNMATCHED_VERTICES);
			LongSumGlobalObject numEdgesOfUnmachedVerticesGO = (LongSumGlobalObject) 
				globalObjectsMap.getGlobalObject(MatchingOptions.GOBJ_NUM_EDGES_OF_UNMATCHED_VERTICES);
			if (numUnmatchedVerticesGO != null) {
				int numUnmatchedVertices = numUnmatchedVerticesGO.getValue().getValue();
				if (numUnmatchedVertices == 0) {
					System.err.println("num unmatched vertices is 0! current: "
						+ numUnmatchedVertices + " previous: " + previousNumUnmatchedVertices);
					this.continueComputation = false;
					return;
				} else {
					diffNumUnmatchedVertices = previousNumUnmatchedVertices > 0 ?
						previousNumUnmatchedVertices - numUnmatchedVertices : numUnmatchedVertices;
					System.out.println("new numUnmatchedVertices: " + numUnmatchedVertices
						+ " diff: " + diffNumUnmatchedVertices);
					if (numEdgesOfUnmachedVerticesGO != null) {
						System.out.println("numEdgesOfUnmatchedVertices: "
							+ numEdgesOfUnmachedVerticesGO.getValue().getValue());
					}
					previousNumUnmatchedVertices = numUnmatchedVertices;
				}
			} else {
				System.err.println("ERROR!!! numUnmatched vertices is null! This should never happen. Terminating...");
				this.continueComputation = false;
			}
			if (!terminateIfNumActiveVerticesIsZero()) {
				// We decide how the next cleaning should be done here but we put this into a GlobalObject
				// after the MAX_EDGE_PICKING stage above.
				if (diffNumUnmatchedVertices < options.edgeCleaningMethodSwitchingThreshold
					&& isECOD) {
					isECOD = false;
					clearGlobalObjectsAndSetComputationStage(ComputationStage.ECOD_FULL_EDGE_CLEANING_1);
				} else if (options.isECOD && diffNumUnmatchedVertices >= options.edgeCleaningMethodSwitchingThreshold
					&& !isECOD) {
					isECOD = true;
					clearGlobalObjectsAndSetComputationStage(ComputationStage.MAX_EDGE_PICKING_AND_EDGE_CLEANING);
				} else {
					clearGlobalObjectsAndSetComputationStage(ComputationStage.MAX_EDGE_PICKING_AND_EDGE_CLEANING);
				}
				System.out.println("isECOD: " + isECOD);
			}
			break;
		case ECOD_FULL_EDGE_CLEANING_1:
			clearGlobalObjectsAndSetComputationStage(ComputationStage.ECOD_FULL_EDGE_CLEANING_2);
			break;
		case ECOD_FULL_EDGE_CLEANING_2:
			clearGlobalObjectsAndSetComputationStage(ComputationStage.ECOD_FULL_EDGE_CLEANING_3);
			break;
		case ECOD_FULL_EDGE_CLEANING_3:
			clearGlobalObjectsAndSetComputationStage(ComputationStage.MAX_EDGE_PICKING_AND_EDGE_CLEANING);
			break;
		default:
			throw new RuntimeException("Unknown computation stage: " + compStage);
		}
	}

	protected void clearGlobalObjectsAndSetComputationStage(ComputationStage compStage) {
		getGlobalObjectsMap().clearNonDefaultObjects();
		getGlobalObjectsMap().putGlobalObject(MatchingOptions.GOBJ_COMP_STAGE,
			new IntOverwriteGlobalObject(compStage.getId()));
		System.out.println("Next compStage: " + compStage);
	}
}