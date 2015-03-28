package gps.examples.randomgraphcoarsening;

import static gps.examples.randomgraphcoarsening.RandomGraphCoarseningOptions.*;
import gps.examples.randomgraphcoarsening.RandomGraphCoarseningComputationStageGlobalObject.ComputationStage;
import gps.globalobjects.GlobalObject;
import gps.globalobjects.IntSumGlobalObject;
import gps.graph.Master;
import gps.writable.IntWritable;
import gps.writable.MinaWritable;

import org.apache.commons.cli.CommandLine;

public class RandomGraphCoarseningMaster extends Master {

	private RandomGraphCoarseningOptions options;
	private int numTCNotConvergedVerticesFromPreviousRound = Integer.MAX_VALUE;
	private int iterationNo;
	private int numLatestEdgesWithinClusters;
	private int numLatestSupernodes;
	private int numLatestEdgesCrossingClusters;

	public RandomGraphCoarseningMaster(CommandLine commandLine) {
		options = new RandomGraphCoarseningOptions();
		options.parseOtherOpts(commandLine);
		this.iterationNo = 1;
	}

	@Override
	public void compute(int superstepNo) {
		System.out.println("Inside RandomGraphCoarseningMaster master.compute(). superstepNo: "
			+ superstepNo + " iterationNo: " + iterationNo);
		putGlobalObjectsToMachineStats();
		GlobalObject<? extends MinaWritable> stageGO =
			getGlobalObjectsMap().getGlobalObject(COMPUTATION_STAGE_GO_KEY);
		if (stageGO == null) {
			getGlobalObjectsMap().putGlobalObject(COMPUTATION_STAGE_GO_KEY,
				new RandomGraphCoarseningComputationStageGlobalObject(ComputationStage.VERTEX_PICKING_STEP));
		} else {
			ComputationStage computationStage = ComputationStage.getComputationStageFromId(
				((IntWritable) stageGO.getValue()).getValue());
			System.out.println("Ended stage: " + computationStage);
			switch (computationStage) {
			case VERTEX_PICKING_STEP:
				getGlobalObjectsMap().putGlobalObject(COMPUTATION_STAGE_GO_KEY,
					new RandomGraphCoarseningComputationStageGlobalObject(ComputationStage.TRANSITIVE_CLOSURE_STEP));
				getGlobalObjectsMap().putGlobalObject(NUM_TC_NOT_CONVERGED_VERTICES,
					new IntSumGlobalObject(0));
				break;
			case TRANSITIVE_CLOSURE_STEP:
				GlobalObject<? extends MinaWritable> numTCNotConvertedVerticesGO =
					getGlobalObjectsMap().getGlobalObject(
						NUM_TC_NOT_CONVERGED_VERTICES);
				int numNotConvergedVertices =
						((IntWritable) numTCNotConvertedVerticesGO.getValue()).getValue();
				// Override NUM_TC_NOT_CONVERGED with 0.
				getGlobalObjectsMap().putGlobalObject(NUM_TC_NOT_CONVERGED_VERTICES,
					new IntSumGlobalObject(0));
				System.out.println("numNotConvergedVertices: " + numNotConvergedVertices);
				System.out.println("numTCNotConvergedVerticesFromPreviousRound: "
					+ numTCNotConvergedVerticesFromPreviousRound);
				if ((numTCNotConvergedVerticesFromPreviousRound == 0) &&
					(numNotConvergedVertices == 0)) {
					getGlobalObjectsMap().removeGlobalObject(
						NUM_TC_NOT_CONVERGED_VERTICES);
					if (iterationNo == 1) {
						getGlobalObjectsMap().putGlobalObject(COMPUTATION_STAGE_GO_KEY,
							new RandomGraphCoarseningComputationStageGlobalObject(ComputationStage.MERGING_STEP_ONE));
					} else {
						getGlobalObjectsMap().putGlobalObject(COMPUTATION_STAGE_GO_KEY,
							new RandomGraphCoarseningComputationStageGlobalObject(ComputationStage.SUB_VERTICES_NOTIFICATION_STEP_ONE));
					}
				} else {
					numTCNotConvergedVerticesFromPreviousRound = numNotConvergedVertices;
				}
				break;
			case SUB_VERTICES_NOTIFICATION_STEP_ONE:
				getGlobalObjectsMap().putGlobalObject(COMPUTATION_STAGE_GO_KEY,
					new RandomGraphCoarseningComputationStageGlobalObject(ComputationStage.SUB_VERTICES_NOTIFICATION_STEP_TWO));
				break;
			case SUB_VERTICES_NOTIFICATION_STEP_TWO:
				getGlobalObjectsMap().putGlobalObject(COMPUTATION_STAGE_GO_KEY,
					new RandomGraphCoarseningComputationStageGlobalObject(ComputationStage.SUB_VERTICES_NOTIFICATION_STEP_THREE));
				break;
			case SUB_VERTICES_NOTIFICATION_STEP_THREE:
				getGlobalObjectsMap().putGlobalObject(COMPUTATION_STAGE_GO_KEY,
					new RandomGraphCoarseningComputationStageGlobalObject(ComputationStage.MERGING_STEP_ONE));
				break;
			case MERGING_STEP_ONE:
				getGlobalObjectsMap().putGlobalObject(COMPUTATION_STAGE_GO_KEY,
					new RandomGraphCoarseningComputationStageGlobalObject(ComputationStage.MERGING_STEP_TWO));
				break;
			case MERGING_STEP_TWO:
				getGlobalObjectsMap().putGlobalObject(COMPUTATION_STAGE_GO_KEY,
					new RandomGraphCoarseningComputationStageGlobalObject(ComputationStage.MERGING_STEP_THREE));
				getGlobalObjectsMap().putGlobalObject(NUM_EDGES_CROSSING_CLUSTERS,
					new IntSumGlobalObject(0));
				getGlobalObjectsMap().putGlobalObject(NUM_EDGES_WITHIN_CLUSTERS,
					new IntSumGlobalObject(0));
				getGlobalObjectsMap().putGlobalObject(NUM_SUPERNODES,
					new IntSumGlobalObject(0));
				break;
			case MERGING_STEP_THREE:
				fixGlobalObjects();
				if (iterationNo < options.numIterations) {
					getGlobalObjectsMap().putGlobalObject(COMPUTATION_STAGE_GO_KEY,
						new RandomGraphCoarseningComputationStageGlobalObject(ComputationStage.VERTEX_PICKING_STEP));
					iterationNo++;
				} else {
					System.out.println("Stopping computation");
					this.continueComputation = false;
				}
				break;
			default:
				System.err.println("Computation for stage: " + computationStage +
					" is not yet implemented!!");
			}				
		}
		System.out.println("numLatestSupernodes: " + numLatestSupernodes);
		System.out.println("numLatestEdgesWithinClusters: " + numLatestEdgesWithinClusters);
		System.out.println("numLatestEdgesCrossingClusters: " + numLatestEdgesCrossingClusters);
	}

	private void fixGlobalObjects() {
		// Remove num-supernodes
		// Remove num-edges-crossing-clusters.
		GlobalObject<? extends MinaWritable> numEdgesWithinClustersGO = getGlobalObjectsMap()
			.getGlobalObject(NUM_EDGES_WITHIN_CLUSTERS);
		numLatestEdgesWithinClusters += ((IntWritable) numEdgesWithinClustersGO.getValue()).getValue();
		getMachineStatsForMaster().putDoubleStat(NUM_LATEST_EDGES_WITHIN_CLUSTERS,
			(double) numLatestEdgesWithinClusters);
		globalObjectsMap.removeGlobalObject(NUM_EDGES_WITHIN_CLUSTERS);
		GlobalObject<? extends MinaWritable> numEdgesCrossingClustersGO = getGlobalObjectsMap()
			.getGlobalObject(NUM_EDGES_CROSSING_CLUSTERS);
		numLatestEdgesCrossingClusters = ((IntWritable) numEdgesCrossingClustersGO.getValue()).getValue();
		getMachineStatsForMaster().putDoubleStat(NUM_LATEST_EDGES_CROSSING_CLUSTERS,
			(double) numLatestEdgesCrossingClusters);
		globalObjectsMap.removeGlobalObject(NUM_EDGES_CROSSING_CLUSTERS);
		GlobalObject<? extends MinaWritable> numSupernodesGO = getGlobalObjectsMap()
			.getGlobalObject(NUM_SUPERNODES);
		numLatestSupernodes = ((IntWritable) numSupernodesGO.getValue()).getValue();
		getMachineStatsForMaster().putDoubleStat(NUM_LATEST_SUPERNODES,
			(double) numLatestSupernodes);
		globalObjectsMap.removeGlobalObject(NUM_SUPERNODES);
	}

	private void putGlobalObjectsToMachineStats() {
		addGlobalObjectsToMachineStats(COMPUTATION_STAGE_GO_KEY);
		addGlobalObjectsToMachineStats(NUM_SINGLETONS_REMOVED);
		addGlobalObjectsToMachineStats(NUM_TC_NOT_CONVERGED_VERTICES);
		addGlobalObjectsToMachineStats(NUM_SUPERNODES);
		addGlobalObjectsToMachineStats(NUM_EDGES_WITHIN_CLUSTERS);
		addGlobalObjectsToMachineStats(NUM_EDGES_CROSSING_CLUSTERS);
	}

	private void addGlobalObjectsToMachineStats(String bvKey) {
		GlobalObject<? extends MinaWritable> bv =
			getGlobalObjectsMap().getGlobalObject(bvKey);
		if (bv != null) {
			getMachineStatsForMaster().putDoubleStat(bvKey,
				(double) ((IntWritable) bv.getValue()).getValue());
		}
	}
}
