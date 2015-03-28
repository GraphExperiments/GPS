package gps.examples.randomgraphcoarsening;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.cli.CommandLine;

import static gps.examples.randomgraphcoarsening.RandomGraphCoarseningOptions.*;
import gps.examples.common.TwoIntWritable;
import gps.examples.randomgraphcoarsening.RandomGraphCoarseningComputationStageGlobalObject.ComputationStage;
import gps.globalobjects.GlobalObject;
import gps.globalobjects.IntSumGlobalObject;
import gps.graph.NullEdgeVertex;
import gps.graph.NullEdgeVertexFactory;
import gps.node.GPSJobConfiguration;
import gps.node.MinaWritableIterable;
import gps.writable.IntWritable;
import gps.writable.MinaWritable;

public class RandomGraphCoarseningVertex
	extends NullEdgeVertex<RandomGraphCoarseningValue, TwoIntWritable> {

	private RandomGraphCoarseningOptions options;
	private Random random = new Random();
	
	// These are used to make function-level variable assignments more efficient.
	private GlobalObject<? extends MinaWritable> stageGlobalObject;
	private ComputationStage computationStage;
	private int[] weightedNeighborIds;
	private int[] weightedNeighborWeights;
	private HashMap<Integer, Integer> tmpMap = new HashMap<Integer, Integer>();

	public RandomGraphCoarseningVertex(CommandLine commandLine) {
		options = new RandomGraphCoarseningOptions();
		options.parseOtherOpts(commandLine);
	}

	@Override
	public void compute(Iterable<TwoIntWritable> messageValues, int superstepNo) {
		if (superstepNo == 1) {
			moveAllNeighborsAndWeightsToState();
		}

		stageGlobalObject = getGlobalObjectsMap().getGlobalObject(COMPUTATION_STAGE_GO_KEY);
		if (stageGlobalObject == null) {
			return;
		}
		weightedNeighborIds = getValue().weightedNeighborIds;
		weightedNeighborWeights = getValue().weightedNeighborWeights;
		computationStage = ComputationStage.getComputationStageFromId(
			((IntWritable) stageGlobalObject.getValue()).getValue());
		switch (computationStage) {
			case VERTEX_PICKING_STEP:
				runVertexPickingStep();
				break;
			case TRANSITIVE_CLOSURE_STEP:
				runTransitiveClosureStep(messageValues);
				break;
			case SUB_VERTICES_NOTIFICATION_STEP_ONE:
				runSubVerticesNotificationStepOne();
				break;
			case SUB_VERTICES_NOTIFICATION_STEP_TWO:
				runSubVerticesNotificationStepTwo(messageValues);
				break;
			case SUB_VERTICES_NOTIFICATION_STEP_THREE:
				runSubVerticesNotificationStepThree(messageValues);
				break;
			case MERGING_STEP_ONE:
				runMergingStepOne();
				break;
			case MERGING_STEP_TWO:
				runMergingStepTwo(messageValues);
				break;
			case MERGING_STEP_THREE:
				runMergingStepThree(messageValues);
				break;
			default:
				System.err.println("Computation for stage: " + computationStage +
					" is not yet implemented!!");
		}
	}

	private void moveAllNeighborsAndWeightsToState() {
		RandomGraphCoarseningValue value = getValue();
		if (getNeighborIds().length == 0) {
			value.weightedNeighborIds = null;
			value.weightedNeighborWeights = null;
			getGlobalObjectsMap().putOrUpdateGlobalObject(NUM_SINGLETONS_REMOVED,
				new IntSumGlobalObject(1));
		} else {
			value.weightedNeighborIds = new int[getNeighborIds().length];
			value.weightedNeighborWeights = new int[getNeighborIds().length];
			int counter = 0;
			for (int neighborId : getNeighborIds()) {
				value.weightedNeighborIds[counter] = neighborId;
				value.weightedNeighborWeights[counter] = 1;
				++counter;
			}
			value.edgeSize = counter;
			removeEdges();
		}
	}

	private void runSubVerticesNotificationStepThree(Iterable<TwoIntWritable> messageValues) {
		int numMessages = 0;
		for (TwoIntWritable message : messageValues) {
			getValue().superNodeId = message.intValue1;
			numMessages++;
		}
	}

	private void runSubVerticesNotificationStepTwo(Iterable<TwoIntWritable> messageValues) {
		for (TwoIntWritable message : messageValues) {
//			System.out.println("vertexId: " + getId() + " is notifying old subNode its new " +
//				"supernodeId: " + getState().superNodeId);
			sendMessage(message.intValue1, new TwoIntWritable(getValue().superNodeId));
		}
	}

	private void runSubVerticesNotificationStepOne() {
		if (weightedNeighborIds == null) {
//			System.out.println("subNodeId: " + getId() + " is asking its supernode: "
//				+ getState().superNodeId + " its new supernodeId: " + getState().superNodeId);
			sendMessage(getValue().superNodeId, new TwoIntWritable(getId()));
		}
	}

	private void runVertexPickingStep() {
//		System.out.println("Running vertexPickingStep for vertexId: " + getId());
		if (weightedNeighborIds == null || (weightedNeighborIds.length == 0)) {
//			System.out.println("Skipping vertexPickingStep for vertex " + getId()
//				+ " because it's already coarsened into another vertex or has no neighbors: "
//				+ getState().superNodeId + " hasCoarsened: " + (weightedNeighborsMap == null));
			return;
		}
		if (weightedNeighborIds.length > options.numDegreesThreshold) {
//			System.out.println("Skipping vertex " + getId() + " because it's degree is more " +
//				"than numDegreesThreshold: " + numDegreesThreshold);
			return;
		}
		int randomNeighborIndex = random.nextInt(getTotalDegree(weightedNeighborWeights));
		int counter = 0;
		int mergedNeighborId = -1;
		int neighborId;
		int degree;
		for (int i = 0; i < weightedNeighborIds.length; ++i) {
			neighborId = weightedNeighborIds[i];
			degree = weightedNeighborWeights[i];
			for (int j = 0; j < degree; ++j) {
				if (counter++ == randomNeighborIndex) {
					mergedNeighborId = neighborId;
				}
			}
		}
		getValue().superNodeId = Math.min(mergedNeighborId, getValue().superNodeId);
//		System.out.println("vertexId: " + getId() + " has assigned itself to: " + mergedNeighborId
//			+ " its currentState is: " + getState().superNodeId + " sending a message");
		sendMessage(mergedNeighborId, new TwoIntWritable(getId()));
	}

	private int getTotalDegree(int[] weightedNeighborWeights2) {
		int sum = 0;
		for (int value : weightedNeighborWeights2) {
			sum += value; //values.get(i);
		}
		return sum;
	}

	private void runTransitiveClosureStep(Iterable<TwoIntWritable> messageValues) {
//		System.out.println("Running transitive closure step. id: " + getId());
		if (weightedNeighborIds == null || (weightedNeighborIds.length == 0)) {
//			System.out.println("Skipping runTransitiveClosureStep for vertex " + getId()
//				+ " because it's already coarsened into another vertex or has no neighbors: "
//				+ getState().superNodeId + " hasCoarsened: " + (weightedNeighborsMap == null));
			return;
		}

		int previousSupernodeId = getValue().superNodeId;
		int newSupernodeId = previousSupernodeId;
		for (TwoIntWritable message : messageValues) {
			newSupernodeId = Math.min(newSupernodeId, message.intValue1);
//			System.out.println("received message: " + message.intValue1);
		}
		getValue().superNodeId = newSupernodeId;

		if (newSupernodeId != getId()) {
//			System.out.println("vertex: " + getId() + " is assigned to another vertex: "
//				+ newSupernodeId + " it's notifying the messages that it has received.");
			((MinaWritableIterable) messageValues).reset();
			for (TwoIntWritable message : messageValues) {
//				System.out.println("sending a message to vertexId: " + message.intValue1
//					+ " messageValue: " + newSupernodeId);
				sendMessage(message.intValue1, new TwoIntWritable(newSupernodeId));
			}
		}
		if (previousSupernodeId != newSupernodeId) {
//			System.out.println("previousSupernodeId: " + previousSupernodeId + " is not equal " +
//				" to newSupernodeId: " + newSupernodeId + ". Incrementing num-tc-not-converged "
//				+ "bv and sending a message to previousSupernodeId with newSupernodeId if " +
//				"previousSupernodeId is not vertexId...");
			if (previousSupernodeId != getId()) {
				sendMessage(previousSupernodeId, new TwoIntWritable(newSupernodeId));
			}
			getGlobalObjectsMap().putOrUpdateGlobalObject(
				NUM_TC_NOT_CONVERGED_VERTICES, new IntSumGlobalObject(1));
		}
		if (newSupernodeId != getId()) {
			// If your value did not change, but your superNodeId is not yourself,
			// Notify your supernode with your vertexId, so that if it changes it
			// can notify you
//			System.out.println("superNodeId: " + newSupernodeId + " is not equal to vertexId: "
//				+ getId() + ". Sending a message to supernodeId with vertexId.");
			sendMessage(newSupernodeId, new TwoIntWritable(getId()));
		}
	}

	private void runMergingStepOne() {
		if (weightedNeighborIds == null || (weightedNeighborIds.length == 0)) {
//			System.out.println("Skipping runMergingStepOne for vertex " + getId()
//				+ " because it's already coarsened into another vertex or has no neighbors: "
//				+ getState().superNodeId + " hasCoarsened: " + (weightedNeighborsMap == null));
			return;
		}
		if (getValue().superNodeId != getId()) {
//			System.out.println("vertexId: " + getId() + " is sending its vertex size "
//				+ getState().vertexSize + " and edge size: "
//				+ getState().edgeSize + " to supernodeId: " + getState().superNodeId);
			sendMessage(getValue().superNodeId, new TwoIntWritable(
				getValue().vertexSize, getValue().edgeSize));
			getValue().vertexSize = -1;
			getValue().edgeSize = -1;
		}
		int neighborId;
		int degree;
		for (int i = 0; i < weightedNeighborIds.length; ++i) {
			neighborId = weightedNeighborIds[i];
			degree = weightedNeighborWeights[i];
			for (int j = 0; j < degree; ++j) {
				if (random.nextDouble() < options.sparsificationPercentage) {
//					System.out.println("vertexId: " + getId() + " is sending its superNodeId: "
//						+ getState().superNodeId + " to neighborId: " + neighborId);
					sendMessage(neighborId, new TwoIntWritable(getValue().superNodeId));
				}				
			}
		}
	}

	@Override
	public RandomGraphCoarseningValue getInitialValue(int id) {
		return new RandomGraphCoarseningValue(id);
	}

	private void runMergingStepTwo(Iterable<TwoIntWritable> messageValues) {
		if (weightedNeighborIds == null || (weightedNeighborIds.length == 0)) {
//			System.out.println("Skipping runMergingStepTwo for vertex " + getId()
//				+ " because it's already coarsened into another vertex or has no neighbors: "
//				+ getState().superNodeId + " hasCoarsened: " + (weightedNeighborsMap == null));
			return;
		}
		for (TwoIntWritable message : messageValues) {
			if (message.type == 1) {
				assert getValue().superNodeId == getId() : "in merging step 2, only the vertices" +
					" that have assigned themselves to themselves should receive type 1" +
					"messages. vertexId: " + getId() + " supernodeId: " + getValue().superNodeId;
//				System.out.println("vertexId: " + getId() + " received a merged-to-you message." +
//					" vertexSize: " + message.intValue1 + " edgeSize: " + message.intValue2);
				getValue().vertexSize += message.intValue1;
				getValue().edgeSize += message.intValue2;
			} else {
//				System.out.println("vertexId: " + getId() + " received a cluster-id message." +
//					" clusterId: " + message.intValue1 + " forwarding it to its own supernodeId: "
//					+ getState().superNodeId);
				sendMessage(getValue().superNodeId, message);
			}
		}
	}

	private void runMergingStepThree(Iterable<TwoIntWritable> messageValues) {
		if (weightedNeighborIds == null || (weightedNeighborIds.length == 0)) {
//			System.out.println("Skipping runMergingStepThree for vertex " + getId()
//				+ " because it's already coarsened into another vertex or has no neighbors: "
//				+ getState().superNodeId + " hasCoarsened: " + (weightedNeighborsMap == null));
			return;
		}
		tmpMap.clear();
		if (getValue().superNodeId != getId()) {
			getValue().weightedNeighborIds = null;
			getValue().weightedNeighborWeights = null;
			weightedNeighborIds = null;
			weightedNeighborWeights = null;
		} else {
			getGlobalObjectsMap().putOrUpdateGlobalObject(NUM_SUPERNODES,
				new IntSumGlobalObject(1));
		}
		for (TwoIntWritable message : messageValues) {
			assert getValue().superNodeId == getId() : "in merging step 3, only the vertices" +
				" that have assigned themselves to themselves should receive messages at all." +
				" vertexId: " + getId() + " supernodeId: " + getValue().superNodeId;
			if (message.intValue1 == getId()) {
				getGlobalObjectsMap().putOrUpdateGlobalObject(NUM_EDGES_WITHIN_CLUSTERS,
					new IntSumGlobalObject(1));
				continue;
			}
			getGlobalObjectsMap().putOrUpdateGlobalObject(NUM_EDGES_CROSSING_CLUSTERS,
				new IntSumGlobalObject(1));
			putOrUpdateMapByOne(tmpMap, message.intValue1);
		}
		if (weightedNeighborIds != null) {
			weightedNeighborIds = new int[tmpMap.size()];
			weightedNeighborWeights = new int[tmpMap.size()];
			int counter = 0;
			for (Entry<Integer, Integer> entry : tmpMap.entrySet()) {
				weightedNeighborIds[counter] = entry.getKey();
				weightedNeighborWeights[counter] = entry.getValue();
				counter++;
			}
			getValue().weightedNeighborIds = weightedNeighborIds;
			getValue().weightedNeighborWeights = weightedNeighborWeights;
		}
	}

	private void dumpWeightedNeigborsAndVertexAndEdgeSize() {
		System.out.println("Dumping weightedNeighborsMap for vertexId: " + getId()
			+ " vertexSize: " + getValue().vertexSize + " edgeSize: " + getValue().edgeSize);
		if (getValue().weightedNeighborIds == null) {
			System.out.println("weightedNeighborsMap is null!!");
		} else {
			for (int i = 0; i < weightedNeighborIds.length; ++i) {
				System.out.println(weightedNeighborIds[i] + " " + weightedNeighborWeights[i]);
			}
		}
		System.out.println("End of dumping weighted neighbors for vertexId: " + getId());
	}

	private void putOrUpdateMapByOne(HashMap<Integer, Integer> weightedNeighborsMap2, int key) {
		Integer mapValue = weightedNeighborsMap2.get(key);
		if (mapValue != null) {
			weightedNeighborsMap2.put(key, mapValue + 1);
		} else {
			weightedNeighborsMap2.put(key, 1);
		}
	}

	public static class GraphCoarseningVertexFactory
		extends NullEdgeVertexFactory<RandomGraphCoarseningValue, TwoIntWritable> {
		@Override
		public NullEdgeVertex<RandomGraphCoarseningValue, TwoIntWritable> newInstance(CommandLine commandLine) {
			return new RandomGraphCoarseningVertex(commandLine);
		}
	}
	
	public static class JobConfiguration extends GPSJobConfiguration {

		@Override
		public Class<?> getVertexFactoryClass() {
			return GraphCoarseningVertexFactory.class;
		}

		@Override
		public Class<?> getVertexClass() {
			return RandomGraphCoarseningVertex.class;
		}

		@Override
		public Class<?> getVertexValueClass() {
			return RandomGraphCoarseningValue.class;
		}

		@Override
		public Class<?> getMessageValueClass() {
			return TwoIntWritable.class;
		}
	}
}
