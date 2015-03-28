package gps.examples.maximummatching;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;

import gps.examples.maximummatching.MatchingOptions.ComputationStage;
import gps.globalobjects.BooleanOverwriteGlobalObject;
import gps.globalobjects.IntSumGlobalObject;
import gps.globalobjects.LongSumGlobalObject;
import gps.graph.Edge;
import gps.graph.Vertex;
import gps.graph.VertexFactory;
import gps.writable.DoubleWritable;
import gps.writable.IntWritable;
import gps.writable.LongWritable;

public class MatchingVertex extends Vertex<MatchingVertexValue, DoubleWritable, MatchingMessageValue> {

	protected MatchingOptions options;
	private static MatchingVertexValue value;
	private static Set<Integer> removedNeighbors = new HashSet<Integer>();
	protected static int superstepGlobalMapsAreFrom = -1;
	private static ComputationStage computationStage;
	private IntWritable numUnmatchedVerticesWritable = new IntWritable();
	private IntWritable numHighDegreeCleaningIntWritable = new IntWritable();
	private LongWritable numEdgesOfUnmatchedVerticesWritable = new LongWritable();
	private IntSumGlobalObject numUnmatchedVerticesGlobalObject;
	private IntSumGlobalObject numHighDegreeCleaningGlobalObject;
	private LongSumGlobalObject numEdgesOfUnmatchedVerticesGlobalObject;
	// WARNING: MatchingVertex does not look at the singleEdgeCleaning value in MSTOptions!! It looks
	// at its own private variable which is set by the global objects that are maintained by the master.
	private boolean isECOD = false;
	public static HashSet<Integer> intSet = new HashSet<Integer>();
	public MatchingVertex(CommandLine line) {
		options = new MatchingOptions();
		options.parseOtherOpts(line);
	}

	@Override
	public void compute(Iterable<MatchingMessageValue> messageValues, int superstepNo) {
		if (superstepGlobalMapsAreFrom < superstepNo) {
			setGlobalVariablesAndDataStructures();
			superstepGlobalMapsAreFrom = superstepNo;
		}
		value = getValue();
		if (superstepNo == 1 && getNeighborsSize() == 0) {
			voteToHalt();
			return;
		}
		if (isECOD && value.foundAMatch &&
			(computationStage != ComputationStage.ECOD_FULL_EDGE_CLEANING_2 ||
			!options.matchedVerticesSendIDsInECODEdgeCleaning)) {
			voteToHalt();
			return;
		} 
		switch (computationStage) {
		case MAX_EDGE_PICKING_AND_EDGE_CLEANING:
			if (value.foundAMatch) {
				voteToHalt();
				return;
			}
			int numNeighborsRemaining = 0;
			if (isECOD) {
				if (messageValues.iterator().hasNext()) {
					sendMessage(value.matchedId, new MatchingMessageValue(getId()));
					return;
				} else {
					if (superstepNo > 1 && value.matchedId >= 0) {
						removeNeighbor(value.matchedId);
					}
					numNeighborsRemaining = pickMaxWeightEdge();
				}
			} else {
				numNeighborsRemaining = removeNeighborsBySet(messageValues);
			}
			if (numNeighborsRemaining == 0) {
				setValueToNegativeOneAndHalt();
				return;
			} else {
				sendMessage(value.matchedId, new MatchingMessageValue(getId()));
			}
			break;
		case MATCHING:
			if (!value.foundAMatch) {
				for (MatchingMessageValue message : messageValues) {
					if (value.matchedId == message.int1) {
						value.foundAMatch = true;
						break;
					}
				}
			}
			if (value.foundAMatch) {
				if (!isECOD) {
					MatchingMessageValue messageToSend = new MatchingMessageValue(getId());
					if (getNeighborsSize() > 0) {
						for (int neighborId : getNeighborIds()) {
							if (neighborId >= 0 && neighborId != value.matchedId) {
								sendMessage(neighborId, messageToSend);
							}
						}
					}
				}
				removeEdges();
				voteToHalt();
			} else {
				numUnmatchedVerticesWritable.value++;
				// Warning: Note that this is not all the number of edges. The total number of edges will be
				// even less because some neighbors that are deleted are not counted.
				numEdgesOfUnmatchedVerticesWritable.value += getNeighborsSize();
				if (!isECOD) {
					value.matchedId = -1;
				} else {
					MatchingMessageValue messageToSend = new MatchingMessageValue((byte) 1);
					for (MatchingMessageValue message : messageValues) {
						sendMessage(message.int1, messageToSend);
					}
				}
			}
			break;
		case ECOD_FULL_EDGE_CLEANING_1:
			if (!value.foundAMatch) {
				MatchingMessageValue messageToSend = new MatchingMessageValue(getId());
				for (int neighborId : getNeighborIds()) {
					if (neighborId >= 0) {
						sendMessage(neighborId, messageToSend);
					}
				}
			}
			break;
		case ECOD_FULL_EDGE_CLEANING_2:
			if (messageValues.iterator().hasNext()) {
				if ((options.matchedVerticesSendIDsInECODEdgeCleaning && value.foundAMatch)) {
					sendAnswerMessagesToNeighbors(messageValues);
					voteToHalt();
				} else if (!options.matchedVerticesSendIDsInECODEdgeCleaning && !value.foundAMatch) {
					sendAnswerMessagesToNeighbors(messageValues);
				}
			} else {
				if (!value.foundAMatch) {
					setValueToNegativeOneAndHalt();
				}
			}
			break;
		case ECOD_FULL_EDGE_CLEANING_3:
			doEcodFullEdgeCleaning3Computation(messageValues);
			break;
		default:
			printToStdErrAndThrowARuntimeException("Unknown computation stage: " + computationStage);
		}
	}

	private void sendAnswerMessagesToNeighbors(Iterable<MatchingMessageValue> messageValues) {
		MatchingMessageValue messageToSend = new MatchingMessageValue(getId());
		for (MatchingMessageValue messageValue : messageValues) {
			sendMessage(messageValue.int1, messageToSend);
		}
	}

	private void doEcodFullEdgeCleaning3Computation(Iterable<MatchingMessageValue> messageValues) {
		if (value.foundAMatch) {
			System.err.println("ERROR!!! Matched vertices should not execute ECOD_FULL_EDGE_CLEANING_3.");
			return;
		}
		if (!options.matchedVerticesSendIDsInECODEdgeCleaning && !messageValues.iterator().hasNext()) {
			setValueToNegativeOneAndHalt();
			return;
		}
		intSet.clear();
		for (MatchingMessageValue messageValue : messageValues) {
			intSet.add(messageValue.int1);
		}
		int neighborIndex = -1;
		int totalNeighbors = 0;
		for (int neighborId : getNeighborIds()) {
			neighborIndex++;
			if (neighborId >= 0) {
				if (!options.matchedVerticesSendIDsInECODEdgeCleaning) {
					if (!intSet.contains(neighborId)) {
						relabelIdOfNeighbor(neighborIndex, -1);
					} else {
						totalNeighbors++;
					}
				} else {
					if (intSet.contains(neighborId)) {
						relabelIdOfNeighbor(neighborIndex, -1);
					} else {
						totalNeighbors++;
					}
				}
			}
		}
		if (totalNeighbors == 0) {
			setValueToNegativeOneAndHalt();
		} else {
			value.matchedId = -1;
		}
	}

	private void setValueToNegativeOneAndHalt() {
		value.foundAMatch = true;
		value.matchedId = -1;
		removeEdges();
		voteToHalt();
	}

	private void removeNeighbor(int neighborIdToRemove) {
		int neighborIdIndex = -1;
		for (Edge<DoubleWritable> outgoingEdge : getOutgoingEdges()) {
			neighborIdIndex++;
			if (outgoingEdge.getNeighborId() == neighborIdToRemove) {
				relabelIdOfNeighbor(neighborIdIndex, -1);
				return;
			}
		}
	}
	
	private int pickMaxWeightEdge() {
		int numNeighborsRemaining = 0;
		double maxWeight = Double.NEGATIVE_INFINITY;
		int maxWeightNeighborId = -1;
		int tmpNeighborId = -1;
		double tmpWeight = -1;
		for (Edge<DoubleWritable> outgoingEdge : getOutgoingEdges()) {
			tmpNeighborId = outgoingEdge.getNeighborId();
			if (tmpNeighborId < 0) {
				continue;
			}
			tmpWeight = outgoingEdge.getEdgeValue().getValue();
			if ((tmpWeight > maxWeight)
				|| ((tmpWeight == maxWeight) && (tmpNeighborId < maxWeightNeighborId))) {
				maxWeight = tmpWeight;
				maxWeightNeighborId = tmpNeighborId;
			}
			numNeighborsRemaining++;
		}
		value.matchedId = maxWeightNeighborId;
		return numNeighborsRemaining;
	}

	private int removeNeighborsBySet(Iterable<MatchingMessageValue> messageValues) {
		removedNeighbors.clear();
		for (MatchingMessageValue message : messageValues) {
			removedNeighbors.add(message.int1);
		}
		if (removedNeighbors.size() > 500) {
			numHighDegreeCleaningIntWritable.value++;
		}
		int numNeighborsRemaining = 0;
		int neighborIdIndex = -1;
		double maxWeight = Double.NEGATIVE_INFINITY;
		int maxWeightNeighborId = -1;
		int tmpNeighborId = -1;
		double tmpWeight = -1;
		for (Edge<DoubleWritable> outgoingEdge : getOutgoingEdges()) {
			neighborIdIndex++;
			tmpNeighborId = outgoingEdge.getNeighborId();
			if (tmpNeighborId >= 0) {
				if (removedNeighbors.contains(tmpNeighborId)) {
					relabelIdOfNeighbor(neighborIdIndex, -1);
				} else {
					tmpWeight = outgoingEdge.getEdgeValue().getValue();
					if ((tmpWeight > maxWeight) ||
						((tmpWeight == maxWeight) && (tmpNeighborId < maxWeightNeighborId))) {
						maxWeight = tmpWeight;
						maxWeightNeighborId = tmpNeighborId;
					}
					numNeighborsRemaining++;
				}
			} else {
				continue;
			}
		}
		value.matchedId = maxWeightNeighborId;
		return numNeighborsRemaining;
	}

	private void setGlobalVariablesAndDataStructures() {
		computationStage = MatchingOptions.ComputationStage.getComputationStageFromId(
			((IntWritable) getGlobalObjectsMap().getGlobalObject(
			MatchingOptions.GOBJ_COMP_STAGE).getValue()).getValue());
		BooleanOverwriteGlobalObject singleEdgeCleaningGO = (BooleanOverwriteGlobalObject)
			getGlobalObjectsMap().getGlobalObject(MatchingOptions.GOBJ_SINGLE_EDGE_CLEANING);
		if (singleEdgeCleaningGO != null) {
			isECOD = singleEdgeCleaningGO.getValue().getValue();
		}
		numUnmatchedVerticesGlobalObject = new IntSumGlobalObject();
		numUnmatchedVerticesWritable.value = 0;
		numUnmatchedVerticesGlobalObject.setValue(numUnmatchedVerticesWritable);
		numEdgesOfUnmatchedVerticesWritable.value = 0;
		numEdgesOfUnmatchedVerticesGlobalObject = new LongSumGlobalObject();
		numEdgesOfUnmatchedVerticesGlobalObject.setValue(numEdgesOfUnmatchedVerticesWritable);
		numHighDegreeCleaningGlobalObject = new IntSumGlobalObject();
		numHighDegreeCleaningIntWritable.value = 0;
		if (ComputationStage.MATCHING == computationStage) {
			getGlobalObjectsMap().putGlobalObject(MatchingOptions.GOBJ_NUM_UNMATCHED_VERTICES,
				numUnmatchedVerticesGlobalObject);
			getGlobalObjectsMap().putGlobalObject(
				MatchingOptions.GOBJ_NUM_EDGES_OF_UNMATCHED_VERTICES,
				numEdgesOfUnmatchedVerticesGlobalObject);
		} else if (ComputationStage.MAX_EDGE_PICKING_AND_EDGE_CLEANING == computationStage) {
			getGlobalObjectsMap().putGlobalObject(MatchingOptions.GOBJ_NUM_HIGH_DEGREE_CLEANING_VERTICES,
				numHighDegreeCleaningGlobalObject);
		}
	}

	@Override
	public MatchingVertexValue getInitialValue(int id) {
		MatchingVertexValue matchingVertexValue = new MatchingVertexValue();
		matchingVertexValue.matchedId = -1;
		matchingVertexValue.foundAMatch = false;
		return matchingVertexValue;
	}
	
	/**
	 * Factory class for {@link MatchingVertex}.
	 * 
	 * @author semihsalihoglu
	 */
	public static class MatchingVertexFactory
		extends VertexFactory<MatchingVertexValue, DoubleWritable, MatchingMessageValue> {

		@Override
		public Vertex<MatchingVertexValue, DoubleWritable, MatchingMessageValue> newInstance(
			CommandLine commandLine) {
			return new MatchingVertex(commandLine);
		}
	}
}