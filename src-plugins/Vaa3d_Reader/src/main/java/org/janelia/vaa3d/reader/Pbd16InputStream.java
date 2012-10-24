package org.janelia.vaa3d.reader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Decompresses a binary InputStream using Sean Murphy's fast PBD 
 * pack-bits plus difference encoding.
 * 
 * Overrides the FilterInputStream.read method.
 * Adapted from ImageLoader.cpp in Vaa3d project.
 * Used by V3dRawImageStream class.
 * 
 * @author Christopher M. Bruns
 *
 */
public class Pbd16InputStream extends PbdInputStream 
{
	private ByteOrder byteOrder= ByteOrder.BIG_ENDIAN;
	private byte[] bytes = new byte[2];
	private ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
	private ShortBuffer shortBuffer;
	private short repeatValue;
	private short decompressionPrior;
	private byte d0,d1,d2,d3;
	private byte sourceChar;
	private byte carryOver;
	private byte oooooool = 1;
	// private byte oooooolo = 2;
	private byte ooooooll = 3;
	// private byte oooooloo = 4;
	// private byte ooooolol = 5;
	// private byte ooooollo = 6;
	private byte ooooolll = 7;

	
	public Pbd16InputStream(InputStream in, ByteOrder byteOrder) {
		super(in);
		this.byteOrder = byteOrder;
		byteBuffer.order(byteOrder);
		shortBuffer = byteBuffer.asShortBuffer();
	}

	@Override
	public int read(byte[] b, int off, int len) 
	throws IOException
	{
		if (len < 1) return 0;
		
		ByteBuffer byteOut = ByteBuffer.wrap(b, off, len);
		byteOut.order(this.byteOrder);
		ShortBuffer out = byteOut.asShortBuffer();
		
		while (out.hasRemaining())
		{
			if (state == State.STATE_BEGIN)
			{
				// Read one byte
				int code = in.read(); // unsigned
				assert(code >= 0);
				if (code < 32) { // literal 0-31
					state = State.STATE_LITERAL;
					leftToFill = code + 1;
				}
				else if (code < 80) { // Difference 3-bit 32-79
					state = State.STATE_DIFFERENCE;
					leftToFill = code - 31;
				}
				else if (code < 223) { // Repeat 223-255
					throw new IOException("Received unimplemented code of " + code);
				}
				else { // Repeat 223-255
					state = State.STATE_REPEAT;
					leftToFill = code - 222;
					in.read(bytes, 0, 2);
					shortBuffer.rewind();
					repeatValue = shortBuffer.get();
				}
			}
			else if (state == State.STATE_LITERAL)
			{
				int numShortsToRead = Math.min(out.remaining(), leftToFill);
				in.read(byteOut.array(), off + 2*out.position(), 2*numShortsToRead); // copy block
				out.position(out.position() + numShortsToRead);
				leftToFill -= numShortsToRead;
				if (leftToFill == 0) {
					state = State.STATE_BEGIN;
					leftToFill = 1;
				}
				decompressionPrior = out.get(out.position() - 1);
			}
			else if (state == State.STATE_DIFFERENCE)
			{
				while ( (leftToFill > 0) && out.hasRemaining() ) 
				{
	                // 332
	                d0=d1=d2=d3=0;
	                sourceChar=(byte)in.read();
	                d0=sourceChar;
	                d0 >>>= 5;
					short value = (short)(decompressionPrior+(d0<5?d0:4-d0));
	                out.put(value);
	                //if (debug) qDebug() << "debug: position " << (dp-1) << " diff value=" << target16Data[dp-1] << " d0=" << d0;
	                leftToFill--;
	                if (leftToFill==0) {
	                    break;
	                }
	                d1=sourceChar;
	                d1 >>>= 2;
	                d1 &= ooooolll;
	                value = (short)(value + (d1<5?d1:4-d1));
	                out.put(value);
	                //if (debug) qDebug() << "debug: position " << (dp-1) << " diff value=" << target16Data[dp-1];
	                leftToFill--;
	                if (leftToFill==0) {
	                    break;
	                }
	                d2=sourceChar;
	                d2 &= ooooooll;
	                carryOver=d2;

	                // 1331
	                d0=d1=d2=d3=0;
	                sourceChar=(byte)in.read();
	                d0=sourceChar;
	                carryOver <<= 1;
	                d0 >>>= 7;
	                d0 |= carryOver;
	                value = (short)(value + (d0<5?d0:4-d0));
	                out.put(value);
	                //if (debug) qDebug() << "debug: position " << (dp-1) << " diff value=" << target16Data[dp-1];
	                leftToFill--;
	                if (leftToFill==0) {
	                    break;
	                }
	                d1=sourceChar;
	                d1 >>>= 4;
	                d1 &= ooooolll;
	                value = (short)(value + (d1<5?d1:4-d1));
	                out.put(value);
	                //if (debug) qDebug() << "debug: position " << (dp-1) << " diff value=" << target16Data[dp-1];
	                leftToFill--;
	                if (leftToFill==0) {
	                    break;
	                }
	                d2=sourceChar;
	                d2 >>>= 1;
	                d2 &= ooooolll;
	                value = (short)(value + (d2<5?d2:4-d2));
	                out.put(value);
	                //if (debug) qDebug() << "debug: position " << (dp-1) << " diff value=" << target16Data[dp-1];
	                leftToFill--;
	                if (leftToFill==0) {
	                    break;
	                }
	                d3=sourceChar;
	                d3 &= oooooool;
	                carryOver=d3;

	                // 233
	                d0=d1=d2=d3=0;
	                sourceChar=(byte)in.read();
	                d0=sourceChar;
	                d0 >>>= 6;
	                carryOver <<= 2;
	                d0 |= carryOver;
	                value = (short)(value + (d0<5?d0:4-d0));
	                out.put(value);
	                //if (debug) qDebug() << "debug: position " << (dp-1) << " diff value=" << target16Data[dp-1];
	                leftToFill--;
	                if (leftToFill==0) {
	                    break;
	                }
	                d1=sourceChar;
	                d1 >>>= 3;
	                d1 &= ooooolll;
	                value = (short)(value + (d1<5?d1:4-d1));
	                out.put(value);
	                //if (debug) qDebug() << "debug: position " << (dp-1) << " diff value=" << target16Data[dp-1];
	                leftToFill--;
	                if (leftToFill==0) {
	                    break;
	                }
	                d2=sourceChar;
	                d2 &= ooooolll;
	                value = (short)(value + (d2<5?d2:4-d2));
	                out.put(value);
	                //if (debug) qDebug() << "debug: position " << (dp-1) << " diff value=" << target16Data[dp-1];
	                leftToFill--;
	                if (leftToFill==0) {
	                    break;
	                }					
					decompressionPrior = out.get(out.position() - 1);
				}
				decompressionPrior = out.get(out.position() - 1);
				if (leftToFill < 1)
					state = State.STATE_BEGIN;
			}
			else if (state == State.STATE_REPEAT)
			{
				int repeatCount = Math.min(leftToFill, out.remaining());
				for (int j = 0; j < repeatCount; ++j)
					out.put(repeatValue);
				leftToFill -= repeatCount;
				if (leftToFill < 1)
					state = State.STATE_BEGIN;
				decompressionPrior = repeatValue;
			}
			else {
				throw new IOException("Unexpected state");
			}
		}
		return 2 * out.position();
	}
}
