package gps.examples.coloring;

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;

import gps.writable.MinaWritable;

public class ColoringVertexValue extends MinaWritable {

	protected short color;
	protected ColoringVertexType type;
	protected int numRemainingNeighbors;
	
	public ColoringVertexValue() {
		this.type = ColoringVertexType.UNDECIDED;
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
		return "type: " + type + " color: " + color + " numRemainingNeighbors: " + numRemainingNeighbors;
	}

	public static enum ColoringVertexType {
		COLORED((byte) 0),
		IN_SET((byte) 1),
		NOT_IN_SET((byte) 2),
		SELECTED_AS_POSSIBLE_IN_SET((byte) 3),
		UNDECIDED((byte) 4);

		private static Map<Byte, ColoringVertexType> idMISVertexTypeMap = new HashMap<Byte, ColoringVertexType>();
		static {
			for (ColoringVertexType type : ColoringVertexType.values()) {
				idMISVertexTypeMap.put(type.id, type);
			}
		}

		private byte id;

		private ColoringVertexType(byte id) {
			this.id = id;
		}

		public byte getId() {
			return id;
		}

		public static ColoringVertexType getMISVertexTypeFromId(byte id) {
			return idMISVertexTypeMap.get(id);
		}
	}
}