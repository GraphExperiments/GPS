package gps.examples.coloring;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;

import gps.examples.coloring.ColoringOptions.ComputationStage;
import gps.examples.coloring.ColoringVertexValue.ColoringVertexType;
import gps.globalobjects.NullValueGraphGObj;
import gps.globalobjects.IntOverwriteGlobalObject;
import gps.globalobjects.IntSumGlobalObject;
import gps.globalobjects.LongSumGlobalObject;
import gps.graph.NullEdgeVertex;
import gps.graph.NullEdgeVertexFactory;
import gps.writable.IntSetWritable;
import gps.writable.IntWritable;
import gps.writable.IntegerIntegerMapWritable;
import gps.writable.LongWritable;
import gps.writable.NodeWritable;
import gps.writable.NullValueGraphWritable;
import gps.writable.NullWritable;

public class ColoringVertex extends NullEdgeVertex<ColoringVertexValue, ColoringMessage> {

	protected ColoringOptions options;
	private static ColoringVertexValue value;
	private static Set<Integer> removedNeighbors = new HashSet<Integer>();
	protected static int superstepGlobalMapsAreFrom = -1;
	private static ComputationStage computationStage;
	private static short latestColor;
	private LongWritable numEdgesOfPotentiallyActiveVertices;
	protected static NullValueGraphWritable graph;
	protected static NullWritable nullWritable = new NullWritable();
	private Set<Integer> misResults;
	private Map<Integer, Integer> coloringResults;
	
	public ColoringVertex(CommandLine line) {
		options = new ColoringOptions();
		options.parseOtherOpts(line);
	}

	@Override
	public void compute(Iterable<ColoringMessage> messageValues, int superstepNo) {
		if (superstepGlobalMapsAreFrom < superstepNo) {
			setGlobalVariablesAndDataStructures();
			superstepGlobalMapsAreFrom = superstepNo;
		}
		value = getValue();
		if (superstepNo == 1) {
			value.numRemainingNeighbors = getNeighborsSize();
		}
		if (ColoringVertexType.COLORED == value.type) {
			voteToHalt();
			return;
		}
		switch (computationStage) {
		case MIS_1:
			if (ColoringVertexType.NOT_IN_SET == value.type
				|| ColoringVertexType.IN_SET == value.type) {
				return;
			}
			double probability = getNeighborsSize() > 0 ? 1.0 / ((double) 2*value.numRemainingNeighbors) : 1;
			if (Math.random() <= probability) {
				value.type = ColoringVertexType.SELECTED_AS_POSSIBLE_IN_SET;
				if (value.numRemainingNeighbors > 0) {
					ColoringMessage newSelectedAsPossibleMessage = ColoringMessage
						.newNeighborSelectedAsPossibleMessage(getId());
					for (int neighborId : getNeighborIds()) {
						if (neighborId >= 0) {
							sendMessage(neighborId, newSelectedAsPossibleMessage);
						}
					}
				}
			}
			break;
		case MIS_2:
			if (ColoringVertexType.SELECTED_AS_POSSIBLE_IN_SET != value.type) {
				break;
			}
			for (ColoringMessage message : messageValues) {
				if (message.int1 < getId()) {
					value.type = ColoringVertexType.UNDECIDED;
					numEdgesOfPotentiallyActiveVertices.value += getNeighborsSize();
					return;
				}
			}
			setValueToInSetAndNotifyNeighbors();
			break;
		case MIS_3:
			errorIfVertexTypeIsSelectedAsPossibleInSet();
			if (ColoringVertexType.IN_SET == value.type) {
				return;
			}
			removeNeighborsAndPossiblySetTypeToNotInSet(messageValues);
			break;
		case MIS_4:
			errorIfVertexTypeIsSelectedAsPossibleInSet();
			if (ColoringVertexType.IN_SET == value.type
				|| ColoringVertexType.NOT_IN_SET == value.type) {
				return;
			}
			if (ColoringVertexType.UNDECIDED != value.type) {
				printToStdErrAndThrowARuntimeException("vertex id : " + getId() + " is executing in " +
					"stage MIS_4 but has a type different than UNDECIDED. type: " + value.type);
			}
			for (ColoringMessage message : messageValues) {
				value.numRemainingNeighbors--;
			}
			break;
		case COLORING:
			if (value.type == ColoringVertexType.IN_SET) {
				value.color = latestColor;
				value.type = ColoringVertexType.COLORED;
				removeEdges();
				voteToHalt();
			} else {
				value.type = ColoringVertexType.UNDECIDED;
				getGlobalObjectsMap().putOrUpdateGlobalObject(ColoringOptions.GOBJ_NUM_NOT_COLORED_VERTICES, 
					new IntSumGlobalObject(1));
				getGlobalObjectsMap().putOrUpdateGlobalObject(
					ColoringOptions.GOBJ_NUM_EDGES_OF_NOT_COLORED_VERTICES,
					new IntSumGlobalObject(value.numRemainingNeighbors));
				value.numRemainingNeighbors = countNumRemainingNeighbors();
			}
			break;
		case FCS_MIS_GRAPH_FORMATION:
			addVertexIntoGraph();
			break;
		case FCS_MIS_RESULT_FINDING_1:
			if (ColoringVertexType.UNDECIDED == value.type) {
				if (misResults.contains(getId())) {
					setValueToInSetAndNotifyNeighbors();
					return;
				}
			}
			break;
		case FCS_MIS_RESULT_FINDING_2:
			removeNeighborsAndPossiblySetTypeToNotInSet(messageValues);
			break;
		case FCS_COLORING_GRAPH_FORMATION:
			addVertexIntoGraph();
			break;
		case FCS_COLORING_RESULT_FINDING:
			if (value.type == ColoringVertexType.UNDECIDED || value.type == ColoringVertexType.SELECTED_AS_POSSIBLE_IN_SET) {
				int intColor = coloringResults.get(getId());
				value.color = (short) intColor;
			} else {
				System.err.println("There shouldn't be any vertex that is not undecided in coloring" +
					" results finding. vertex id: " + getId());
			}
			break;
		default:
			printToStdErrAndThrowARuntimeException("Unknown computation stage: " + computationStage);
		}
		if (ColoringVertexType.UNDECIDED == value.type
			|| ColoringVertexType.SELECTED_AS_POSSIBLE_IN_SET == value.type) {
			numEdgesOfPotentiallyActiveVertices.value += getNeighborsSize();
		}
	}

	private void removeNeighborsAndPossiblySetTypeToNotInSet(Iterable<ColoringMessage> messageValues) {
		if (messageValues.iterator().hasNext()) {
			if (ColoringVertexType.IN_SET == value.type) {
				printToStdErrAndThrowARuntimeException("Vertex with id: " + getId() + " has type: "
					+ value.type + " and has messages in phase 3 where vertices receive" +
					" messages from neighbors who have put them in their sets.");
			}
			removeNeighbors(messageValues);
			if (value.type == ColoringVertexType.UNDECIDED) {
				value.type = ColoringVertexType.NOT_IN_SET;
				for (int neighborId : getNeighborIds()) {
					if (neighborId >= 0) {
						sendMessage(neighborId,
							ColoringMessage.newDecrementNumNeighborsMessage());
					}
				}
			}
		}
	}

	private void setValueToInSetAndNotifyNeighbors() {
		for (int neighborId : getNeighborIds()) {
			if (neighborId >= 0) {
				sendMessage(neighborId, ColoringMessage.removeNeighborMessage(getId()));
			}
		}
		value.type = ColoringVertexType.IN_SET;
	}

	private void addVertexIntoGraph() {
		if (ColoringVertexType.UNDECIDED == value.type ||
			ColoringVertexType.SELECTED_AS_POSSIBLE_IN_SET == value.type) {
			graph.nodes.add(new NodeWritable(getId(), getNeighborIds()));
		}
	}

	private int countNumRemainingNeighbors() {
		int count = 0;
		for (int neighborId : getNeighborIds()) {
			if (neighborId >= 0) {
				count++;
			}
		}
		return count;
	}

	private void removeNeighbors(Iterable<ColoringMessage> messageValues) {
		removedNeighbors.clear();
		for (ColoringMessage message : messageValues) {
			removedNeighbors.add(message.int1);
		}
		int numNeighborsRemoved = 0;
		int neighborIdIndex = 0;
		for (int neighborId : getNeighborIds()) {
			if (neighborId >= 0 && removedNeighbors.contains(neighborId)) {
				numNeighborsRemoved++;
				relabelIdOfNeighbor(neighborIdIndex, -1);
			}
			neighborIdIndex++;
		}
		value.numRemainingNeighbors -= numNeighborsRemoved;
	}

	private void errorIfVertexTypeIsSelectedAsPossibleInSet() {
		if (ColoringVertexType.SELECTED_AS_POSSIBLE_IN_SET == value.type) {
			printToStdErrAndThrowARuntimeException("in phase 3 or 0 there cannot be any SELECTED_AS_POSSIBLE_IN_SET vertices.");
		}
	}

	private void setGlobalVariablesAndDataStructures() {
		computationStage = ColoringOptions.ComputationStage.getComputationStageFromId(
			((IntWritable) getGlobalObjectsMap().getGlobalObject(
			ColoringOptions.GOBJ_COMP_STAGE).getValue()).getValue());
		IntOverwriteGlobalObject color = (IntOverwriteGlobalObject) getGlobalObjectsMap().getGlobalObject(
			ColoringOptions.GOBJ_COLOR);
		if (color != null) {
			latestColor = (short) color.getValue().getValue();
		}
		numEdgesOfPotentiallyActiveVertices = new LongWritable(0);
		LongSumGlobalObject numEdgesOfPotentiallyActiveVerticesGO = new LongSumGlobalObject();
		numEdgesOfPotentiallyActiveVerticesGO.setValue(numEdgesOfPotentiallyActiveVertices);
		getGlobalObjectsMap().putOrUpdateGlobalObject(
			ColoringOptions.GOBJ_NUM_EDGES_OF_POTENTIALLY_ACTIVE_VERTICES,
			numEdgesOfPotentiallyActiveVerticesGO);
		if (ComputationStage.FCS_MIS_GRAPH_FORMATION == computationStage
			|| ComputationStage.FCS_COLORING_GRAPH_FORMATION == computationStage) {
			graph = new NullValueGraphWritable();
			getGlobalObjectsMap().putOrUpdateGlobalObject(
				ColoringOptions.GOBJ_GRAPH,
				new NullValueGraphGObj(graph));
		} else if (ComputationStage.FCS_MIS_RESULT_FINDING_1 == computationStage) {
			misResults = ((IntSetWritable) getGlobalObjectsMap().getGlobalObject(
				ColoringOptions.GOBJ_RESULTS).getValue()).value;
		} else if (ComputationStage.FCS_COLORING_RESULT_FINDING == computationStage) {
			coloringResults = ((IntegerIntegerMapWritable) getGlobalObjectsMap().getGlobalObject(
				ColoringOptions.GOBJ_RESULTS).getValue()).integerIntegerMap;
		}
	}

	@Override
	public ColoringVertexValue getInitialValue(int id) {
		return new ColoringVertexValue();
	}
	
	/**
	 * Factory class for {@link ColoringVertex}.
	 * 
	 * @author semihsalihoglu
	 */
	public static class ColoringVertexFactory
		extends NullEdgeVertexFactory<ColoringVertexValue, ColoringMessage> {

		@Override
		public NullEdgeVertex<ColoringVertexValue, ColoringMessage> newInstance(CommandLine commandLine) {
			return new ColoringVertex(commandLine);
		}
	}
}