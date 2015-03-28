package gps.examples.randomgraphcoarsening;

import gps.writable.MinaWritable;

import org.apache.mina.core.buffer.IoBuffer;

public class RandomGraphCoarseningValue extends MinaWritable {

	public int[] weightedNeighborIds;
	public int[] weightedNeighborWeights;
	public int superNodeId;
	public int vertexSize;
	public int edgeSize;

	public RandomGraphCoarseningValue(int superNodeId) {
		this.superNodeId = superNodeId;
		this.vertexSize = 1;
		this.edgeSize = 0;
	}

	@Override
	public int numBytes() {
		// Do nothing
		return 0;
	}

	@Override
	public void write(IoBuffer ioBuffer) {
		// Do nothing
	}

	@Override
	public void read(IoBuffer ioBuffer) {
		// Do nothing
	}

	@Override
	public int read(byte[] byteArray, int index) {
		// Do nothing
		return 0;
	}

	@Override
	public int read(IoBuffer ioBuffer, byte[] byteArray, int index) {
		// Do nothing b/c this should never be serialized
		return 0;
	}

	@Override
	public void combine(byte[] messageQueue, byte[] tmpArray) {
		// Do nothing b/c this should never be sent as a message
	}
	
	@Override
	public String toString() {
		return "" + this.superNodeId;
	}
}