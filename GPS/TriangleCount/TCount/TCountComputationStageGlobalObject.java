package gps.examples.kmeans;

import gps.globalobjects.GlobalObject;
import gps.writable.IntWritable;
import gps.writable.MinaWritable;

import java.util.HashMap;
import java.util.Map;


public class KMeansComputationStageGlobalObject extends GlobalObject<IntWritable> {

	public KMeansComputationStageGlobalObject() {
		// Every Global Object needs a default empty constructor that writes a dummy thing for its
		// value.
		setValue(new IntWritable(-1));
	}

	public KMeansComputationStageGlobalObject(ComputationStage computationState) {
		this.setValue(new IntWritable(computationState.id));
	}

	@Override
	public void update(MinaWritable otherValue) {
		// Nothing to do. This should never be updated.
	}

	public static enum ComputationStage {
		CLUSTER_FINDING_1(0),
		CLUSTER_FINDING_2(1),
		EDGE_COUNTING_1(2),
		EDGE_COUNTING_2(3);

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