package customnode.u3d;

public class ContextManager {

	private int[][] symbolCount; // an array of arrays that store the
	// number of occurrences of each
	// symbol for each dynamic context.
	private int[][] cumulativeCount; // an array of arrays that store the
	// cumulative frequency of each
	// symbol in each context. the value
	// is the number of occurences of a
	// symbol and every symbol with a
	// larger value.
	// The Elephant is a value that determines the number of
	// symbol occurences that are stored in each dynamic histogram.
	// Limiting the number of occurences avoids overflow of the U16 array
	// elements and allows the histogram to adapt to changing symbol
	// distributions in files.
	private final long Elephant = 0x00001fff;
	// the maximum value that is stored in a histogram
	private final long MaximumSymbolInHistogram = 0x0000FFFF;
	// the ammount to increase the size of an array when reallocating
	// an array.
	private final long ArraySizeIncr = 32;

	public ContextManager() {
		this.symbolCount = new int[Constants.StaticFull][];
		this.cumulativeCount = new int[Constants.StaticFull][];
	}

	public void AddSymbol(long context, long symbol) {
		if (context < Constants.StaticFull && context != Constants.Context8
				&& symbol < MaximumSymbolInHistogram) {
			int[] cumulativeCount = this.cumulativeCount[(int) context];
			int[] symbolCount = this.symbolCount[(int) context];
			if (cumulativeCount == null || cumulativeCount.length <= symbol) {
				cumulativeCount = new int[(int) (symbol + ArraySizeIncr)];
				symbolCount = new int[(int) (symbol + ArraySizeIncr)];
				if (cumulativeCount != null && symbolCount != null) {
					if (this.cumulativeCount[(int) context] == null) {
						this.cumulativeCount[(int) context] = cumulativeCount;
						this.cumulativeCount[(int) context][0] = 1;
						this.symbolCount[(int) context] = symbolCount;
						this.symbolCount[(int) context][0] = 1;
					} else {
						System.arraycopy(this.cumulativeCount[(int) context],
								0, cumulativeCount, 0, this.cumulativeCount[(int) context].length);
						System.arraycopy(this.symbolCount[(int) context], 0,
								symbolCount, 0, this.symbolCount[(int) context].length);
					}
				}
				this.cumulativeCount[(int) context] = cumulativeCount;
				this.symbolCount[(int) context] = symbolCount;
			}
			if (cumulativeCount[0] >= Elephant) {// if total number of
				// occurances is larger than
				// Elephant,
				// scale down the values to avoid overflow
				int len = cumulativeCount.length;
				int tempAccum = 0;
				for (int i = len - 1; i >= 0; i--) {
					symbolCount[i] >>= 1;
					tempAccum += symbolCount[i];
					cumulativeCount[i] = tempAccum;
				}
				// preserve the initial escape value of 1 for the symbol
				// count and cumulative count
				symbolCount[0]++;
				cumulativeCount[0]++;
			}
			symbolCount[(int) symbol]++;
			for (int i = 0; i <= symbol; i++) {
				cumulativeCount[i]++;
			}
		}
	}

	public long GetSymbolFrequency(long context, long symbol) {
		// the static case is 1.
		long rValue = 1;
		if (context < Constants.StaticFull && context != Constants.Context8) {
			// the default for the dynamic case is 0
			rValue = 0;
			if ((this.symbolCount[(int) context] != null)
					&& (symbol < this.symbolCount[(int) context].length)) {
				rValue = (long) this.symbolCount[(int) context][(int) symbol];
			} else if (symbol == 0) { // if the histogram hasn't been created
				// yet, the symbol 0 is
				// the escape value and should return 1
				rValue = 1;
			}
		}
		return rValue;
	}

	public long GetCumulativeSymbolFrequency(long context, long symbol) {
		// the static case is just the value of the symbol.
		long rValue = symbol - 1;
		if (context < Constants.StaticFull && context != Constants.Context8) {
			rValue = 0;
			if (this.cumulativeCount[(int) context] != null) {
				if (symbol < this.cumulativeCount[(int) context].length) {
					rValue = (long) (this.cumulativeCount[(int) context][0] - this.cumulativeCount[(int) context][(int) symbol]);
				} else
					rValue = (long) (this.cumulativeCount[(int) context][0]);
			}
		}
		return rValue;
	}

	public long GetTotalSymbolFrequency(long context) {
		if (context < Constants.StaticFull && context != Constants.Context8) {
			long rValue = 1;
			if (this.cumulativeCount[(int) context] != null)
				rValue = this.cumulativeCount[(int) context][0];
			return rValue;
		} else {
			if (context == Constants.Context8)
				return 256;
			else
				return context - Constants.StaticFull;
		}
	}

	public long GetSymbolFromFrequency(long context, long symbolFrequency) {
		long rValue = 0;
		if (context < Constants.StaticFull && context != Constants.Context8) {
			rValue = 0;
			if (this.cumulativeCount[(int) context] != null
					&& symbolFrequency != 0
					&& this.cumulativeCount[(int) context][0] >= symbolFrequency) {
				long i = 0;
				for (i = 0; i < this.cumulativeCount[(int) context].length; i++) {
					if (this.GetCumulativeSymbolFrequency(context, i) <= symbolFrequency)
						rValue = i;
					else
						break;
				}
			}
		} else {
			rValue = symbolFrequency + 1;
		}
		return rValue;
	}

}
