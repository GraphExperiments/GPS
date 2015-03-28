package gps.examples.randomgraphcoarsening;

import gps.globalobjects.GlobalObject;
import gps.writable.IntWritable;
import gps.writable.MinaWritable;

import java.util.HashMap;
import java.util.Map;


public class RandomGraphCoarseningComputationStageGlobalObject extends GlobalObject<IntWritable> {

	public RandomGraphCoarseningComputationStageGlobalObject() {
		// Every Global Object needs a default empty constructor that writes a dummy thing for its
		// value.
		setValue(new IntWritable(-1));
	}

	public RandomGraphCoarseningComputationStageGlobalObject(ComputationStage computationState) {
		this.setValue(new IntWritable(computationState.id));
	}

	@Override
	public void update(MinaWritable otherValue) {
		// Nothing to do. This should never be updated.
	}

	public static enum ComputationStage {
		VERTEX_PICKING_STEP(0),
		TRANSITIVE_CLOSURE_STEP(1),
		SUB_VERTICES_NOTIFICATION_STEP_ONE(2),
		SUB_VERTICES_NOTIFICATION_STEP_TWO(3),
		SUB_VERTICES_NOTIFICATION_STEP_THREE(4),
		MERGING_STEP_ONE(5),
		MERGING_STEP_TWO(6),
		MERGING_STEP_THREE(7);

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