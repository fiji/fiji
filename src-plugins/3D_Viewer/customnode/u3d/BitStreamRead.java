package customnode.u3d;

/// <summary> BitStreamRead.cs
/// BitStreamRead is the implementation of IBitStreamRead.</summary>
///
/// <remarks>
/// <para> All uncompressed reads are read in as a sequence of U8s
/// with the private method ReadSymbol in context Context8 and then built
/// up to the appropriate size and cast to the appropriate type for
/// the read call. are converted to unsigned integers and broken down
/// into a sequence of U8 values that are writen with the private method
/// WriteSymbol in the static context Context8.
/// </para>
///
/// <para> All compressed reads are for unsigned integers and are passed
/// through to the private method ReadSymbol with the associated context.
/// </para>
/// </remarks>
public class BitStreamRead {

	private ContextManager contextManager; //the context manager handles
	//the updates to the histograms
	//for the compression contexts.
	private long high; //high and low are the upper and
	private long low; //lower limits on the
	//probability
	private long underflow; //stores the number of bits of
	//underflow caused by the
	//limited range of high and low
	private long code; //the value as represented in
	//the datablock
	private long[] data; //the data section of the
	//datablock to read from.
	private long dataPosition; //the position currently read in
	//the datablock specified in 32
	//bit increments.
	private long dataLocal; //the local value of the data
	//corresponding to dataposition.
	private long dataLocalNext; //the 32 bits in data after
	//dataLocal
	private int dataBitOffset; //the offset into dataLocal that
	// the next read will occur
	private static final long[] FastNotMask = { 0x0000FFFF, 0x00007FFF,
			0x00003FFF, 0x00001FFF, 0x00000FFF };
	private static final long[] ReadCount = { 4, 3, 2, 2, 1, 1, 1, 1, 0, 0, 0,
			0, 0, 0, 0, 0 };

	public BitStreamRead() {
		this.contextManager = new ContextManager();
		this.high = 0x0000FFFF;
	}

	public short ReadU8() {
		short rValue;
		long uValue = ReadSymbol(Constants.Context8);
		uValue--;
		uValue = SwapBits8(uValue);
		rValue = (short) uValue;
		return rValue;
	}

	public int ReadU16() {
		short low = ReadU8();
		short high = ReadU8();
		return (int) (((int) low) | (((int) high) << 8));
	}

	public long ReadU32() {
		long rValue;
		int low = ReadU16();
		int high = ReadU16();
		rValue = ((long) low) | ((long) (high << 16));
		return rValue;
	}

	//		public void ReadU64(out UInt64 rValue)
	//		{
	//		long low = 0;
	//		long high = 0;
	//		ReadU32(out low);
	//		ReadU32(out high);
	//		rValue = ((UInt64) low) | (((UInt64) high) << 32);
	//		}
	public int ReadI32() {
		long uValue = ReadU32();
		return (int) (uValue);
	}

	public float ReadF32() {
		long uValue = ReadU32();
		//		rValue = BitConverter.ToSingle(BitConverter.Getshorts(uValue), 0);
		return Float.intBitsToFloat((int) uValue);
	}

	public long ReadCompressedU32(long context) {
		long rValue;
		long symbol = 0;
		if (context != Constants.Context8 && context < Constants.MaxRange) { //the context is a compressed context
			symbol = ReadSymbol(context);
			if (symbol != 0) { //the symbol is compressed
				rValue = symbol - 1;
			} else { //escape character, the symbol was not compressed
				rValue = ReadU32();
				this.contextManager.AddSymbol(context, rValue + 1);
			}
		} else { //The context specified is uncompressed.
			rValue = ReadU32();
		}
		return rValue;
	}

	public int ReadCompressedU16(long context) {
		int rValue;
		long symbol = 0;
		if (context != 0 && context < Constants.MaxRange) { //the context is a compressed context
			symbol = ReadSymbol(context);
			if (symbol != 0) { //the symbol is compressed
				rValue = (int) (symbol - 1);
			} else { //the symbol is uncompressed
				rValue = ReadU16();
				this.contextManager.AddSymbol(context, rValue + 1);
			}
		} else { //the context specified is not compressed
			rValue = ReadU16();
		}
		return rValue;
	}

	public short ReadCompressedU8(long context) {
		short rValue;
		long symbol = 0;
		if (context != 0 && context < Constants.MaxRange) { //the context is a compressed context
			symbol = ReadSymbol(context);
			if (symbol != 0) { //the symbol is compressed
				rValue = (short) (symbol - 1);
			} else { //the symbol is not compressed
				rValue = ReadU8();
				this.contextManager.AddSymbol(context, rValue + (long) 1);
			}
		} else { //the context specified is not compressed
			rValue = ReadU8();
		}
		return rValue;
	}

	public void SetDataBlock(DataBlock dataBlock) { //set the data to be read to data and get the first part of the data
		//into local variables
		long[] tempData = dataBlock.getData();
		this.data = new long[tempData.length];
		System.arraycopy(tempData, 0, this.data, 0, tempData.length);
		this.dataPosition = 0;
		this.dataBitOffset = 0;
		GetLocal();
	}

	/* internally the BitStreamRead object stores 64 bits from the
	DataBlock's
	 * data section in dataLocal and dataLocalNext.
	 */
	/* SwapBits8
	 * reverses the order of the bits of an 8 bit value.
	 * E.g. abcdefgh -> hgfedcba
	 */
	private long SwapBits8(long rValue) {
		return (Constants.Swap8[(int) ((rValue) & 0xf)] << 4)
				| (Constants.Swap8[(int) ((rValue) >> 4)]);
	}

	/* ReadSymbol
	 * Read a symbol from the datablock using the specified context.
	 * The symbol 0 represents the escape value and signifies that the
	 * next symbol read will be uncompressed.
	 */
	private long ReadSymbol(long context) {
		long rSymbol;
		long uValue = 0;
		// Fill in the code word
		long position = GetBitCount();
		this.code = ReadBit();
		this.dataBitOffset += (int) this.underflow;
		while (this.dataBitOffset >= 32) {
			this.dataBitOffset -= 32;
			IncrementPosition();
		}
		long temp = Read15Bits();
		this.code <<= 15;
		this.code |= temp;
		SeekToBit(position);
		// Get total count to calculate probabilites
		long totalCumFreq = this.contextManager
				.GetTotalSymbolFrequency(context);
		// Get the cumulative frequency of the current symbol
		long range = this.high + 1 - this.low;
		// The relationship:
		// codeCumFreq <= (totalCumFreq * (this.code - this.low)) / range
		// is used to calculate the cumulative frequency of the current
		// symbol. The +1 and -1 in the line below are used to counteract
		// finite word length problems resulting from the division by range.
		long codeCumFreq = ((totalCumFreq) * (1 + this.code - this.low) - 1)
				/ (range);
		// Get the current symbol
		uValue = this.contextManager.GetSymbolFromFrequency(context,
				codeCumFreq);
		// Update state and context
		long valueCumFreq = this.contextManager.GetCumulativeSymbolFrequency(
				context, uValue);
		long valueFreq = this.contextManager
				.GetSymbolFrequency(context, uValue);
		long low = this.low;
		long high = this.high;
		high = low - 1 + range * (valueCumFreq + valueFreq) / totalCumFreq;
		low = low + range * (valueCumFreq) / totalCumFreq;
		this.contextManager.AddSymbol(context, uValue);
		int bitCount;
		long maskedLow;
		long maskedHigh;
		// Count bits to read
		// Fast count the first 4 bits
		//compare most significant 4 bits of low and high
		bitCount = (int) ReadCount[(int) (((low >> 12) ^ (high >> 12)) & 0x0000000F)];
		low &= FastNotMask[bitCount];
		high &= FastNotMask[bitCount];
		high <<= bitCount;
		low <<= bitCount;
		high |= (long) ((1 << bitCount) - 1);
		// Regular count the rest
		maskedLow = Constants.HalfMask & low;
		maskedHigh = Constants.HalfMask & high;
		while (((maskedLow | maskedHigh) == 0)
				|| ((maskedLow == Constants.HalfMask) && maskedHigh == Constants.HalfMask)) {
			low = (Constants.NotHalfMask & low) << 1;
			high = ((Constants.NotHalfMask & high) << 1) | 1;
			maskedLow = Constants.HalfMask & low;
			maskedHigh = Constants.HalfMask & high;
			bitCount++;
		}
		long savedBitsLow = maskedLow;
		long savedBitsHigh = maskedHigh;
		if (bitCount > 0) {
			bitCount += (int) this.underflow;
			this.underflow = 0;
		}
		// Count underflow bits
		maskedLow = Constants.QuarterMask & low;
		maskedHigh = Constants.QuarterMask & high;
		long underflow = 0;
		while ((maskedLow == 0x4000) && (maskedHigh == 0)) {
			low &= Constants.NotThreeQuarterMask;
			high &= Constants.NotThreeQuarterMask;
			low += low;
			high += high;
			high |= 1;
			maskedLow = Constants.QuarterMask & low;
			maskedHigh = Constants.QuarterMask & high;
			underflow++;
		}
		// Store the state
		this.underflow += underflow;
		low |= savedBitsLow;
		high |= savedBitsHigh;
		this.low = low;
		this.high = high;
		// Update bit read position
		this.dataBitOffset += bitCount;
		while (this.dataBitOffset >= 32) {
			this.dataBitOffset -= 32;
			IncrementPosition();
		}
		// Set return value
		rSymbol = uValue;
		return rSymbol;
	}

	/*
	 * GetBitCount
	 * returns the number of bits read in rCount
	 */
	private long GetBitCount() {
		return (long) ((this.dataPosition << 5) + this.dataBitOffset);
	}

	/* ReadBit
	 * Read the next bit in the datablock. The value is returned in
	 * rValue.
	 */
	private long ReadBit() {
		long rValue;
		long uValue = 0;
		uValue = this.dataLocal >> this.dataBitOffset;
		uValue &= 1;
		this.dataBitOffset++;
		if (this.dataBitOffset >= 32) {
			this.dataBitOffset -= 32;
			IncrementPosition();
		}
		rValue = uValue;
		return rValue;
	}

	/* Read15Bits
	 * Read the next 15 bits from the datablock. the value is returned
	 * in rValue.
	 */
	private long Read15Bits() {
		long rValue;
		long uValue = this.dataLocal >> this.dataBitOffset;
		if (this.dataBitOffset > 17) {
			uValue |= (this.dataLocalNext << (32 - this.dataBitOffset));
		}
		uValue += uValue;
		uValue = (Constants.Swap8[(int) ((uValue >> 12) & 0xf)])
				| ((Constants.Swap8[(int) ((uValue >> 8) & 0xf)]) << 4)
				| ((Constants.Swap8[(int) ((uValue >> 4) & 0xf)]) << 8)
				| ((Constants.Swap8[(int) (uValue & 0xf)]) << 12);
		rValue = uValue;
		this.dataBitOffset += 15;
		if (this.dataBitOffset >= 32) {
			this.dataBitOffset -= 32;
			IncrementPosition();
		}
		return rValue;
	}

	/*
	 * IncrementPosition
	 * Updates the values of the datablock stored in dataLocal and
	dataLocalNext
	 * to the next values in the datablock.
	 */
	private void IncrementPosition() {
		this.dataPosition++;
		this.dataLocal = this.data[(int) dataPosition];
		if (this.data.length > this.dataPosition + 1) {
			this.dataLocalNext = this.data[(int) this.dataPosition + 1];
		} else {
			this.dataLocalNext = 0;
		}
	}

	/* SeekToBit
	 * Sets the dataLocal, dataLocalNext and bitOffSet values so that
	 * the next read will occur at position in the datablock.
	 */
	private void SeekToBit(long position) {
		this.dataPosition = position >> 5;
		this.dataBitOffset = (int) (position & 0x0000001F);
		GetLocal();
	}

	/*
	 * GetLocal
	 * store the initial 64 bits of the datablock in dataLocal and
	 * dataLocalNext
	 */
	private void GetLocal() {
		this.dataLocal = this.data[(int) this.dataPosition];
		if (this.data.length > this.dataPosition + 1) {
			this.dataLocalNext = this.data[(int) this.dataPosition + 1];
		}
	}

}
