package customnode.u3d;

public class DataBlock {
	
	private long[] 
	    data = null,
	    metaData = null;
	private long 
		dataSize = 0, 
		metaDataSize = 0,
		priority = 0,
		blockType = 0;
	
	
	public DataBlock() {

	}

	public long getDataSize() {
		return this.dataSize;
	}

	public void setDataSize(long value) {
		this.dataSize = value;
		//allocate data buffer for block.
		//the data is generally aligned to byte values
		//but array is 4 bytes values . . .
		if ((this.dataSize & 0x3) == 0)
			this.data = new long[(int)value >> 2];
		else
			this.data = new long[((int)value >> 2) + 1];
	}

	public long[] getData() {
		return this.data;
	}

	public void setData(long[] value) {
		this.data = value;
	}

	public long getMetaDataSize() {
		return this.metaDataSize;
	}

	public void setMetaDataSize(long value) {
		this.metaDataSize = value;
		//allocate data buffer for block.
		//the data is generally aligned to byte values
		//but array is 4 bytes values . . .
		if ((this.metaDataSize & 0x3) == 0)
			this.metaData = new long[(int)value >> 2];
		else
			this.metaData = new long[((int)value >> 2) + 1];
	}

	public long[] getMetaData() {
		return this.metaData;
	}

	public void setMetaData(long[] value) {
		if (value.length == this.metaData.length) {
			System.arraycopy(value, 0, this.metaData, 0, value.length);
		}
	}

	public long getBlockType() {
		return this.blockType;
	}

	public void setBlockType(long value) {
		this.blockType = value;
	}

	public long getPriority() {
		return this.priority;
	}

	public void setPriority(long value) {
		this.priority = value;
	}
}
