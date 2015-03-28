package gps.examples.maximummatching;

import org.apache.mina.core.buffer.IoBuffer;

import gps.writable.MinaWritable;

public class MatchingVertexValue extends MinaWritable {

	protected int matchedId;
	protected boolean foundAMatch;
	
	public MatchingVertexValue() {
	}

	@Override
	public int numBytes() {
		return 0;
	}

	@Override
	public void write(IoBuffer ioBuffer) {}

	@Override
	public void read(IoBuffer ioBuffer) {}

	@Override
	public int read(byte[] byteArray, int index) {
		return 0;
	}

	@Override
	public int read(IoBuffer ioBuffer, byte[] byteArray, int index) {
		return 0;
	}

	@Override
	public void combine(byte[] messageQueue, byte[] tmpArray) {}

	@Override
	public String toString() {
		return " matchedId: " + (foundAMatch ? matchedId : -1);// + " foundAMatch: " + foundAMatch;
	}
}