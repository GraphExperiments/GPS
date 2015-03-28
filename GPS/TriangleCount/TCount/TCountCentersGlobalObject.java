package gps.examples.kmeans;

import gps.globalobjects.GlobalObject;
import gps.writable.IntArrayWritable;
import gps.writable.MinaWritable;

/**
 * Global Object that holds the cluster centers.
 *
 * @author semihsalihoglu
 */
public class KMeansClusterCentersGlobalObject  extends GlobalObject<IntArrayWritable> {

	public KMeansClusterCentersGlobalObject() {
		// Every Global Object needs a default empty constructor that writes a dummy thing for its
		// value.
		setValue(new IntArrayWritable(new int[0]));
	}

	public KMeansClusterCentersGlobalObject(int[] clusterCenters) {
		this.setValue(new IntArrayWritable(clusterCenters));
	}

	@Override
	public void update(MinaWritable minaWritable) {
		// Nothing to do. This should never be updated.
	}
}
