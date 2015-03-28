package gps.examples.coloring;

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;

import gps.writable.MinaWritable;

public class ColoringMessage extends MinaWritable {

	protected MISMessageType type;
	protected int int1;

	public ColoringMessage() {
		this(MISMessageType.NEIGHBOR_SELECTED_AS_POSSIBLE);
	}

	public ColoringMessage(MISMessageType type, int selectedNeighborId) {
		this(type);
		this.int1 = selectedNeighborId;
	}

	public ColoringMessage(MISMessageType type) {
		this.type = type;
	}

	@Override
	public int numBytes() {
		if (type == MISMessageType.NEIGHBOR_SELECTED_AS_POSSIBLE ||
			type == MISMessageType.REMOVE_NEIGHBOR) {
			return 1 + 4;
		} else if (type == MISMessageType.HAS_A_SELECTED_NEIGHBOR) {
			return 1;
		} else {
			return -1;
		}
	}

	@Override
	public void write(IoBuffer ioBuffer) {
		ioBuffer.put(type.id);
		if (type == MISMessageType.NEIGHBOR_SELECTED_AS_POSSIBLE ||
			type == MISMessageType.REMOVE_NEIGHBOR) {
			ioBuffer.putInt(this.int1);
		}
	}

	@Override
	public void read(IoBuffer ioBuffer) {
		type = MISMessageType.getMSTMessageTypeFromId(ioBuffer.get());
		if (type == MISMessageType.NEIGHBOR_SELECTED_AS_POSSIBLE ||
			type == MISMessageType.REMOVE_NEIGHBOR) {
			this.int1 = ioBuffer.getInt();
		}
	}

	@Override
	public void read(String string1) {
		throw new UnsupportedOperationException("This method should not be called because MISMessages " +
			"should not be parsed from a file.");
	}

	@Override
	public int read(IoBuffer ioBuffer, byte[] byteArray, int index) {
        byteArray[index] = ioBuffer.get();
        type = MISMessageType.getMSTMessageTypeFromId(byteArray[index]);
        if (type == MISMessageType.NEIGHBOR_SELECTED_AS_POSSIBLE ||
        	type == MISMessageType.REMOVE_NEIGHBOR) {
        	ioBuffer.get(byteArray, index + 1, 4);
        	return 1 + 4;
		} else if (type == MISMessageType.HAS_A_SELECTED_NEIGHBOR) {
			return 1;
		}
		return -1;
	}

	@Override
	public int read(byte[] byteArray, int index) {
        type = MISMessageType.getMSTMessageTypeFromId(byteArray[index]);
        if (type == MISMessageType.NEIGHBOR_SELECTED_AS_POSSIBLE ||
        	type == MISMessageType.REMOVE_NEIGHBOR) {
			this.int1 = readIntegerFromByteArray(byteArray, index + 1);
        	return 1 + 4;
        } else if (type == MISMessageType.HAS_A_SELECTED_NEIGHBOR) {
        	return 1;
		}
		return -1;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("type: " + type + "\n");
		if (type == MISMessageType.NEIGHBOR_SELECTED_AS_POSSIBLE ||
			type == MISMessageType.REMOVE_NEIGHBOR) {
			stringBuilder.append("int1: " + int1);
			stringBuilder.append("\n");
		}
		return stringBuilder.toString();
	}

	public static enum MISMessageType {
		NEIGHBOR_SELECTED_AS_POSSIBLE((byte) 0),
		HAS_A_SELECTED_NEIGHBOR((byte) 1),
		REMOVE_NEIGHBOR((byte) 2),;

		private static Map<Byte, MISMessageType> idMSTMessageTypeMap =
			new HashMap<Byte, MISMessageType>();
		static {
			for (MISMessageType type : MISMessageType.values()) {
				idMSTMessageTypeMap.put(type.id, type);
			}
		}

		private byte id;

		private MISMessageType(byte id) {
			this.id = id;
		}

		public byte getId() {
			return id;
		}

		public static MISMessageType getMSTMessageTypeFromId(byte id) {
			return idMSTMessageTypeMap.get(id);
		}
	}
	
	public static ColoringMessage removeNeighborMessage(int neighborId) {
		return new ColoringMessage(MISMessageType.REMOVE_NEIGHBOR, neighborId);
		
	}

	public static ColoringMessage newNeighborSelectedAsPossibleMessage(int neighborId) {
		return new ColoringMessage(MISMessageType.NEIGHBOR_SELECTED_AS_POSSIBLE, neighborId);
	}

	public static ColoringMessage newDecrementNumNeighborsMessage() {
		return new ColoringMessage(MISMessageType.HAS_A_SELECTED_NEIGHBOR);
	}
}