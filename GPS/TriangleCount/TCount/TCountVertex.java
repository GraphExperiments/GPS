package gps.examples.kmeans;

import org.apache.commons.cli.CommandLine;

import gps.examples.common.TwoIntWritable;
import gps.examples.kmeans.KMeansComputationStageGlobalObject.ComputationStage;
import static gps.examples.kmeans.KMeansGlobalObjectNames.*;
import gps.globalobjects.GlobalObject;
import gps.globalobjects.IntSumGlobalObject;
import gps.graph.NullEdgeVertex;
import gps.graph.NullEdgeVertexFactory;
import gps.node.GPSJobConfiguration;
import gps.writable.DoubleWritable;
import gps.writable.IntArrayWritable;
import gps.writable.IntWritable;
import gps.writable.MinaWritable;

/**
 * A simple implementation of the k-means algorithm. Pseudocode below:
 * Input G(V, E), k, numEdgesThreshold, maxIterations
 * int numEdgesCrossingClusters = Integer.MAX_INT;
 * while ((numEdgesCrossingCluster > numEdgesThreshold) && iterationNo < maxIterations) {
 *    int[] clusterCenters = pickKClusterCenters(k, G);
 *    findClusterCenters(G, clusterCenters);
 *    numEdgesCrossingClusters = countNumEdgesCrossingClusters();
 * }
 *
 * @author semihsalihoglu
 */
public class KMeansVertex extends NullEdgeVertex<TwoIntWritable, TwoIntWritable> {

	private GlobalObject<? extends MinaWritable> stageGlobalObject;
	private ComputationStage computationStage;

	@Override
	public void compute(Iterable<TwoIntWritable> incomingMessages, int superstepNo) {
		stageGlobalObject =
			getGlobalObjectsMap().getGlobalObject(COMPUTATION_STAGE_GO_KEY);
		computationStage = ComputationStage.getComputationStageFromId(
			((IntWritable) stageGlobalObject.getValue()).getValue());
		switch (computationStage) {
		case CLUSTER_FINDING_1:
			setValue(new TwoIntWritable(-1 /* cluster id */, -1 /* distance */));
			int[] clusterCenters = ((IntArrayWritable) globalObjectsMap.getGlobalObject(
				CLUSTER_CENTERS_GO_KEY).getValue()).value;
			for (int clusterCenter : clusterCenters) {
				if (clusterCenter == getId()) {
					setValue(new TwoIntWritable(getId() /* cluster id */, 0 /* distance */));
					sendMessages(getNeighborIds(), getValue());
				}
			}
			break;
		case CLUSTER_FINDING_2:
			int clusterId = getValue().intValue1;
			int previousClusterId = clusterId;
			int clusterDistance = getValue().intValue2 >= 0 ? getValue().intValue2 : Integer.MAX_VALUE;
			for (TwoIntWritable message : incomingMessages) {
				if ((message.intValue1 >= 0) && ((message.intValue2 + 1) < clusterDistance)) {
					clusterId = message.intValue1;
					clusterDistance = message.intValue2 + 1;
				}
			}
			if (previousClusterId != clusterId) {
				setValue(new TwoIntWritable(clusterId, clusterDistance));
				getGlobalObjectsMap().putOrUpdateGlobalObject(
					NUM_CLUSTER_CENTERS_NOT_CONVERGED_VERTICES, new IntSumGlobalObject(1));
				sendMessages(getNeighborIds(), getValue());
			}
			break;
		case EDGE_COUNTING_1:
			sendMessages(getNeighborIds(), new TwoIntWritable(getValue().intValue1));
			break;
		case EDGE_COUNTING_2:
			for (TwoIntWritable message : incomingMessages) {
				if (message.intValue1 != getValue().intValue1) {
					getGlobalObjectsMap().putGlobalObject(NUM_EDGES_CROSSING_CLUSTERS,
						new IntSumGlobalObject(1));
				}
			}
			break;
		default:
			System.err.println("Unexpected computation stage for vertex.");
		}
//		System.out.println("value for vertexId: " + getId() + " is: " + getValue().intValue1 + " "
//			+ getValue().intValue2);
	}

	@Override
	public TwoIntWritable getInitialValue(int id) {
		return new TwoIntWritable(-1, -1);
	}

	/**
	 * Factory class for {@link PageRankVertex}.
	 * 
	 * @author semihsalihoglu
	 */
	public static class KMeansVertexFactory extends NullEdgeVertexFactory<TwoIntWritable, TwoIntWritable> {

		@Override
		public NullEdgeVertex<TwoIntWritable, TwoIntWritable> newInstance(CommandLine commandLine) {
			return new KMeansVertex();
		}
	}
	
	public static class JobConfiguration extends GPSJobConfiguration {

		@Override
		public Class<?> getVertexFactoryClass() {
			return KMeansVertexFactory.class;
		}

		@Override
		public Class<?> getVertexClass() {
			return KMeansVertex.class;
		}
		
		@Override
		public Class<?> getMasterClass() {
			return KMeansMaster.class;
		}

		@Override
		public Class<?> getVertexValueClass() {
			return TwoIntWritable.class;
		}

		@Override
		public Class<?> getMessageValueClass() {
			return TwoIntWritable.class;
		}
	}
}
