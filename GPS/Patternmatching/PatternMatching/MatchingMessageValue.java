package gps.examples.maximummatching;

import org.apache.mina.core.buffer.IoBuffer;

import gps.writable.MinaWritable;

public class MatchingMessageValue extends MinaWritable {

	public byte type;
	public int int1;

	public MatchingMessageValue() {
		type = 0;
		int1 = -1;
	}

	public MatchingMessageValue(int int1) {
		this.type = 0;
		this.int1 = int1;
	}
	
	public MatchingMessageValue(byte type) {
		this.type = 1;
	}

	@Override
	public int numBytes() {
		if (type == 0) {
			return 1 + 4;
		} else if (type == 1) {
			return 1;
		} else {
			return -1;
		}
	}

	@Override
	public void write(IoBuffer ioBuffer) {
		ioBuffer.put(type);
		if (type == 0) {
			ioBuffer.putInt(int1);
		}
	}

	@Override
	public void read(IoBuffer ioBuffer) {
		type = ioBuffer.get();
		if (type == 0) {
			int1 = ioBuffer.getInt();
		}
	}

	@Override
	public int read(IoBuffer ioBuffer, byte[] byteArray, int index) {
        byteArray[index] = ioBuffer.get();
        type = byteArray[index];
        if (type == 0) {
        	ioBuffer.get(byteArray, index + 1, 4);
        	return 1 + 4;
		} else if (type == 1) {
        	return 1;
		}  else {
			return -1;
		}
	}

	@Override
	public int read(byte[] byteArray, int index) {
        type = byteArray[index];
        if (type == 0) {
			int1 = readIntegerFromByteArray(byteArray, index + 1);
			return 1 + 4;
        } else if (type == 1) {
			return 1;
        }
		return -1;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("type: " + type + "\n");
		if (type == 0) {
			stringBuilder.append("int1: " + int1 + "\n");
		}
		return stringBuilder.toString();
	}
}