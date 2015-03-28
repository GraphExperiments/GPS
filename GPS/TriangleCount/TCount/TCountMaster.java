package gps.examples.kmeans;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Random;

import gps.examples.kmeans.KMeansComputationStageGlobalObject.ComputationStage;
import static gps.examples.kmeans.KMeansGlobalObjectNames.*;
import gps.globalobjects.GlobalObject;
import gps.globalobjects.IntSumGlobalObject;
import gps.graph.Master;
import gps.node.GPSNodeRunner;
import gps.writable.IntWritable;
import gps.writable.MinaWritable;

import org.apache.commons.cli.CommandLine;

public class KMeansMaster extends Master {

	private int iterationNo;
	private int maxIterations;
	private int numEdgesThreshold;
	private int numVertices;
	private int k;
	private int numEdgesCrossingClusters;

	public KMeansMaster(CommandLine commandLine) {
		parseOtherOpts(commandLine);
		this.iterationNo = 1;
	}

	@Override
	public void compute(int superstepNo) {
		System.out.println("Inside KMeansMaster master.compute(). superstepNo: "
			+ superstepNo + " iterationNo: " + iterationNo);
		GlobalObject<? extends MinaWritable> stageBV =
			getGlobalObjectsMap().getGlobalObject(COMPUTATION_STAGE_GO_KEY);
		if (stageBV == null) {
			pickClusterCentersAndPutCompStageIntoGlobalObjects();
		} else {
			ComputationStage computationStage = ComputationStage.getComputationStageFromId(
				((IntWritable) stageBV.getValue()).getValue());
			System.out.println("Ended stage: " + computationStage);
			switch (computationStage) {
			case CLUSTER_FINDING_1:
				getGlobalObjectsMap().putGlobalObject(COMPUTATION_STAGE_GO_KEY,
					new KMeansComputationStageGlobalObject(ComputationStage.CLUSTER_FINDING_2));
				getGlobalObjectsMap().putGlobalObject(NUM_CLUSTER_CENTERS_NOT_CONVERGED_VERTICES,
					new IntSumGlobalObject(0));
				break;
			case CLUSTER_FINDING_2:
				getGlobalObjectsMap().putGlobalObject(COMPUTATION_STAGE_GO_KEY,
					new KMeansComputationStageGlobalObject(ComputationStage.CLUSTER_FINDING_2));
				GlobalObject<? extends MinaWritable> numCCNotConvertedVerticesGO =
					getGlobalObjectsMap().getGlobalObject(NUM_CLUSTER_CENTERS_NOT_CONVERGED_VERTICES);
				int numNotConvergedVertices =
						((IntWritable) numCCNotConvertedVerticesGO.getValue()).getValue();
				System.out.println("numNotConvergedVertices: " + numNotConvergedVertices);
				// Overwrite NUM_CLUSTER_CENTERS_NOT_CONVERGED_VERTICES
				getGlobalObjectsMap().putGlobalObject(NUM_CLUSTER_CENTERS_NOT_CONVERGED_VERTICES,
					new IntSumGlobalObject(0));
				if (numNotConvergedVertices == 0) {
					getGlobalObjectsMap().putGlobalObject(COMPUTATION_STAGE_GO_KEY,
						new KMeansComputationStageGlobalObject(ComputationStage.EDGE_COUNTING_1));
				}
				break;
			case EDGE_COUNTING_1:
				getGlobalObjectsMap().putGlobalObject(COMPUTATION_STAGE_GO_KEY,
					new KMeansComputationStageGlobalObject(ComputationStage.EDGE_COUNTING_2));
				getGlobalObjectsMap().putGlobalObject(NUM_EDGES_CROSSING_CLUSTERS,
					new IntSumGlobalObject(0));
				break;
			case EDGE_COUNTING_2:
				numEdgesCrossingClusters = ((IntWritable) getGlobalObjectsMap().getGlobalObject(
					NUM_EDGES_CROSSING_CLUSTERS).getValue()).getValue();
				if (numEdgesCrossingClusters < numEdgesThreshold && iterationNo < maxIterations) {
					iterationNo++;
					pickClusterCentersAndPutCompStageIntoGlobalObjects();
				} else {
					this.continueComputation = false;
				}
				break;
			default:
				System.err.println("Unexpected computation stage for master.");
			}
		}		
		System.out.println("iterationNo: " + iterationNo);
		System.out.println("numEdgesCrossingClusters: " + numEdgesCrossingClusters);
	}

	@Override
	public void writeOutput(BufferedWriter bw) throws IOException {
		bw.write("numEdgesCrossing\t" + numEdgesCrossingClusters + "\n");
		super.writeOutput(bw);
	}

	private void pickClusterCentersAndPutCompStageIntoGlobalObjects() {
		int[] clusterCenters = pickKClusterCenters();
		getGlobalObjectsMap().putGlobalObject(CLUSTER_CENTERS_GO_KEY,
			new KMeansClusterCentersGlobalObject(clusterCenters));
		getGlobalObjectsMap().putGlobalObject(COMPUTATION_STAGE_GO_KEY,
			new KMeansComputationStageGlobalObject(ComputationStage.CLUSTER_FINDING_1));
	}

	private int[] pickKClusterCenters() {
		int[] clusterCenters = new int[k];
		Random random = new Random();
		for (int i = 0; i < k; ++i) {
			clusterCenters[i] = random.nextInt(numVertices);
		}
		return clusterCenters;
	}

	protected void parseOtherOpts(CommandLine commandLine) {
		String otherOptsStr = commandLine.getOptionValue(GPSNodeRunner.OTHER_OPTS_OPT_NAME);
		System.out.println("otherOptsStr: " + otherOptsStr);
		if (otherOptsStr != null) {
			String[] split = otherOptsStr.split("###");
			for (int index = 0; index < split.length; ) {
				String flag = split[index++];
				String value = split[index++];
				if ("-mi".equals(flag)) {
					maxIterations = Integer.parseInt(value);
				} else if ("-k".equals(flag)) {
					k = Integer.parseInt(value);
				} else if ("-net".equals(flag)) {
					numEdgesThreshold = Integer.parseInt(value);
				} else if ("-nv".equals(flag)) {
					numVertices = Integer.parseInt(value);
				}
			}
		}
		System.out.println("maxIterations: " + maxIterations);
		System.out.println("numEdgesThreshold: " + numEdgesThreshold);
		System.out.println("k: " + k);
		System.out.println("numVertices: " + numVertices);
	}
}