package customnode.u3d;

import static java.lang.Float.floatToRawIntBits;

import java.awt.Color;
import java.io.UnsupportedEncodingException;

/// <summary>BitStreamWrite.cs
/// BitStreamWrite is the implementation of IBitStreamWrite.
/// </summary>
/// <remarks>
/// <para>All uncompressed writes are converted to unsigned integers and
/// broken down into a sequence of U8 values that are written with the
/// private method WriteSymbol in the static context Context8.
/// </para>
/// <para> All compressed writes are for unsigned integers and are passed
/// through to the private method WriteSymbol with the associated context.
/// </para>
/// </remarks>
public class BitStreamWrite {

	private ContextManager contextManager; //the context manager handles
	// the updates to the histograms for the compression contexts.
	private long high; //high and low are the upper and lower
	private long low; //limits on the probability
	private long underflow; //stores the number of bits of underflow
	//caused by the limited range of high and
	//low
	private boolean compressed; //this is true if a compressed value was
	//written. when the datablock is retrieved,
	//a 32 bit 0 is written to reset the values of
	//high, low, and underflow.
	private long[] data; //the data section of the datablock to write.
	private int dataPosition; //the position currently to write in the
	//datablock specified in 32 bit increments.
	private long dataLocal; //the local value of the data corresponding
	//to dataposition
	private long dataLocalNext; //the 32 bits in data after dataLocal
	private int dataBitOffset; //the offset into dataLocal that the next
	//write will occur
	private final int DataSizeIncrement = 0x000023F8;

	
	public BitStreamWrite() {
		this.contextManager = new ContextManager();
		this.high = 0x0000FFFF;
		this.data = new long[DataSizeIncrement];
		this.compressed = false;
	}

	public void WriteString(String s) {
		WriteU16((short)s.length());
		byte[] bytes = null;
		try {
			bytes = s.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
		for (int i = 0; i < bytes.length; i++)
			WriteU8(bytes[i]);
	}
	
	
	public void WriteColor(Color c) {
		WriteF32(c.getRed() / 255.0f);
		WriteF32(c.getGreen() / 255.0f);
		WriteF32(c.getBlue() / 255.0f);
	}
	
	
	public void WriteU8(short uValue) {
		long symbol = SwapBits8(uValue);
		WriteSymbol(Constants.Context8, symbol);
	}

	public void WriteU16(int uValue) {
		WriteU8((short) (0x00FF & uValue));
		WriteU8((short) (0x00FF & (uValue >> 8)));
	}

	public void WriteU32(long uValue) {
		WriteU16((int) (0x0000FFFF & uValue));
		WriteU16((int) (0x0000FFFF & (uValue >> 16)));
	}

	public void WriteU64(long uValue) {
		WriteU32((0x00000000FFFFFFFF & uValue));
		WriteU32((0x00000000FFFFFFFF & (uValue >> 32)));
	}
			
	public void WriteI32(int iValue) {
		WriteU32(iValue);
	}
	
	public void WriteI16(short iValue) {
		WriteU16(iValue);
	}

	public void WriteF32(float fValue) {
		WriteU32((long)floatToRawIntBits(fValue));
	}

	public void WriteCompressedU32(long context, long uValue) {
		compressed = true;
		boolean escape = false;
		if ((context != 0) && (context < Constants.MaxRange)) {
			escape = WriteSymbol(context, uValue);
			if (escape == true) {
				WriteU32(uValue);
				this.contextManager.AddSymbol(context, uValue + 1);
			}
		} else {
			WriteU32(uValue);
		}
	}

	public void WriteCompressedU16(long context, int uValue) {
		compressed = true;
		boolean escape = false;
		if ((context != 0) && (context < Constants.MaxRange)) {
			escape = WriteSymbol(context, uValue);
			if (escape == true) {
				WriteU16(uValue);
				this.contextManager.AddSymbol(context, uValue + 1);
			}
		} else {
			WriteU16(uValue);
		}
	}

	public void WriteCompressedU8(long context, short uValue) {
		compressed = true;
		boolean escape = false;
		if ((context != 0) && (context < Constants.MaxRange)) {
			escape = WriteSymbol(context, uValue);
			if (escape == true) {
				WriteU8(uValue);
				this.contextManager.AddSymbol(context, uValue + 1);
			}
		} else {
			WriteU8(uValue);
		}
	}

	
	public void WriteDataBlock(DataBlock b) {
		int dataSize = (int)Math.ceil(b.getDataSize() / 4.0); // include padding
		int metaDataSize = (int)Math.ceil(b.getMetaDataSize() / 4.0); // include padding
		WriteU32(b.getBlockType());
		WriteU32(b.getDataSize());
		WriteU32(b.getMetaDataSize());
		for (int i = 0; i < dataSize; i++)
			WriteU32(b.getData()[i]);
		for (int i = 0; i < metaDataSize; i++)
			WriteU32(b.getMetaData()[i]);
	}
	
	
	public DataBlock GetDataBlock() {
		if (compressed) //Flush the arithmetic coder
		{
			this.WriteU32(0);
		}
		AlignToByte();
		long numBytes = ((long) this.dataPosition << 2)
				+ ((long) this.dataBitOffset >> 3);
		DataBlock rDataBlock = new DataBlock();
		PutLocal();
		rDataBlock.setDataSize(numBytes);
		long[] tempData = rDataBlock.getData();
		System.arraycopy(this.data, 0, tempData, 0, tempData.length);
		rDataBlock.setData(tempData);
		return rDataBlock;
	}

	public void AlignToByte() {
		// Check input(s)
		int uBitCount = GetBitCount();
		uBitCount = (8 - (uBitCount & 7)) & 7;
		this.dataBitOffset += uBitCount;
		if (this.dataBitOffset >= 32) {
			this.dataBitOffset -= 32;
			IncrementPosition();
		}
	}

	public void AlignTo4Byte() {
		if (this.dataBitOffset > 0) {
			this.dataBitOffset = 0;
			IncrementPosition();
		}
	}

	/*
	 * WriteSymbol
	 * Write the given symbol to the datablock in the specified context.
	 * rEscape returns as false if the symbol was written successfully.
	 * rEscape will return true when writing in dynamically compressed
	 * contexts when the symbol to write has not appeared yet in the
	 * context's histogram. In this case, the escape symbol, 0, is
	 * written.
	 */
	private boolean WriteSymbol(long context, long symbol) {
		symbol++;
		boolean rEscape = false;
		long totalCumFreq = 0;
		long symbolCumFreq = 0;
		long symbolFreq = 0;
		totalCumFreq = this.contextManager.GetTotalSymbolFrequency(context);
		symbolCumFreq = this.contextManager.GetCumulativeSymbolFrequency(
				context, symbol);
		symbolFreq = this.contextManager.GetSymbolFrequency(context, symbol);
		if (0 == symbolFreq) { //the symbol has not occurred yet.
			//Write out the escape symbol, 0.
			symbol = 0;
			symbolCumFreq = this.contextManager.GetCumulativeSymbolFrequency(
					context, symbol);
			symbolFreq = this.contextManager
					.GetSymbolFrequency(context, symbol);
		}
		if (0 == symbol) { //the symbol is the escape symbol.
			rEscape = true;
		}
		long range = this.high + 1 - this.low;
		this.high = this.low - 1 + range * (symbolCumFreq + symbolFreq) / totalCumFreq;
		this.low = this.low + range * symbolCumFreq / totalCumFreq;
		this.contextManager.AddSymbol(context, symbol);
		//write bits
		long bit = this.low >> 15;
		//long highmask = this.high & Constants.HalfMask;
		//long lowmask = this.low & Constants.HalfMask;
		while ((this.high & Constants.HalfMask) == (this.low & Constants.HalfMask)) {
			this.high &= ~Constants.HalfMask;
			this.high += this.high + 1;
			WriteBit(bit);
			while (this.underflow > 0) {
				this.underflow--;
				WriteBit((~bit) & 1);
			}
			this.low &= ~Constants.HalfMask;
			this.low += this.low;
			bit = this.low >> 15;
		}
		//check for underflow
		// Underflow occurs when the values in this.low and this.high
		// approach each other, without leaving the lower resp. upper
		// half of the scaling interval. The range is not large enough
		// to code the next symbol. To avoid this, the interval is
		// artificially enlarged once the this.low is larger than the
		// first quarter and this.high is lower than the third quarter.
		while ((0 == (this.high & Constants.QuarterMask))
				&& (Constants.QuarterMask == (this.low & Constants.QuarterMask))) {
			this.high &= ~Constants.HalfMask;
			this.high <<= 1;
			this.low <<= 1;
			this.high |= Constants.HalfMask;
			this.high |= 1;
			this.low &= ~Constants.HalfMask;
			this.underflow++;
		}
		return rEscape;
	}

	/*
	 * SwapBits8
	 * reverses the order of the bits of an 8 bit value.
	 * E.g. abcdefgh -> hgfedcba
	 */
	private long SwapBits8(long rValue) {
		return (Constants.Swap8[(int) ((rValue) & 0xf)] << 4)
				| (Constants.Swap8[(int) ((rValue) >> 4)]);
	}

	/*
	 * WriteBit
	 * Write the given bit to the datablock.
	 */
	private void WriteBit(long bit) {
		long mask = 1;
		bit &= mask;
		this.dataLocal &= ~(mask << this.dataBitOffset);
		this.dataLocal |= (bit << this.dataBitOffset);
		this.dataBitOffset += 1;
		if (this.dataBitOffset >= 32) {
			this.dataBitOffset -= 32;
			IncrementPosition();
		}
	}

	/*
	 * IncrementPosition
	 * Updates the values of the datablock stored in dataLocal and
	dataLocalNext
	 * to the next values in the datablock.
	 */
	private void IncrementPosition() {
		this.dataPosition++;
		CheckPosition();
		this.data[this.dataPosition - 1] = this.dataLocal;
		this.dataLocal = this.dataLocalNext;
		this.dataLocalNext = this.data[this.dataPosition + 1];
	}

	/*
	 * GetLocal
	 * store the initial 64 bits of the datablock in dataLocal and
	 * dataLocalNext
	 */
	//		private void GetLocal()
	//		{
	//		CheckPosition();
	//		this.dataLocal = this.data[this.dataPosition];
	//		this.dataLocalNext = this.data[this.dataPosition+1];
	//		}
	/*
	 * PutLocal
	 * stores the local values of the data to the data array
	 *
	 */
	private void PutLocal() {
		this.data[this.dataPosition] = dataLocal;
		this.data[this.dataPosition + 1] = dataLocalNext;
	}

	/*
	 * CheckPosition
	 * checks that the array allocated for writing is large
	 * enough. Reallocates if necessary.
	 */
	private void CheckPosition() {
		if (this.dataPosition + 2 > this.data.length) {
			AllocateDataBuffer(this.dataPosition + 2 + DataSizeIncrement);
		}
	}

	/*
	 * AllocateDataBuffer
	 * Creates and new array for storing the data written. Copies
	 * values of the old array to the new arry.
	 */
	private void AllocateDataBuffer(int size) {
		// Store an old buffer if it exists
		if (null != this.data) {
			long[] oldData = this.data;
			this.data = new long[size];
			for (int i = 0; i < oldData.length; i++) {
				this.data[i] = oldData[i];
			}
		} else {
			this.data = new long[size];
		}
	}

	/*
	 * GetBitCount
	 * returns the number of bits written in rCount
	 */
	int GetBitCount() {
		return (this.dataPosition << 5) + this.dataBitOffset;
	}
	

}
