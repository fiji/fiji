/*
Copyright (c) 2012, Christopher M. Bruns and Howard Hughes Medical Institute
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.janelia.vaa3d.reader;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

/**
 * Base class for Pbd8InputStream and Pbd16InputStream.
 * 
 * Converts a compressed v3dpdb input stream to an uncompressed v3draw
 * input stream.  But only after the 43-byte header section has already been
 * read from the stream.
 * 
 * @author Christopher M. Bruns
 *
 */
public class PbdInputStream extends FilterInputStream 
{
	/**
	 * Finite state representation of the current decompressor state
	 * 
	 * @author Christopher M. Bruns
	 *
	 */
	enum State {
		STATE_BEGIN, // Ready to start new run of bytes
		STATE_LITERAL, // Within a run of direct copying
		STATE_DIFFERENCE, // Within a run of difference encoding
		STATE_DIFFERENCE_SUBPIXEL, // Part way through unpacking a single compressed difference byte
		STATE_REPEAT // Within a run of runlength encoding
	}
	protected State state = State.STATE_BEGIN;
	protected int leftToFill = 0; // How many bytes left in the current run

	/**
	 * Factory method to create a PbdInputStream with a particular bit-depth
	 * byte order.
	 * 
	 * @param in
	 * @param bytesPerPixel
	 * @param byteOrder
	 * @return
	 */
	public static PbdInputStream createPbdInputStream(InputStream in, int bytesPerPixel, ByteOrder byteOrder)
	{
		if (bytesPerPixel == 1)
			return new Pbd8InputStream(in);
		else if (bytesPerPixel == 2)
			return new Pbd16InputStream(in, byteOrder);
		throw new IllegalArgumentException("Unsupported bytes per pixel "+bytesPerPixel);
	}

	/** 
	 * Protected constructor to encourage use of createPbdInputStream factory.
	 * 
	 * @param in compressed input stream.
	 */
	protected PbdInputStream(InputStream in) {
		super(in);
	}

	@Override
	public void mark(int readLimit) {}
	
	@Override
	public boolean markSupported() {
		return false;
	}
	
	@Override
	public int read() 
	throws IOException
	{
		byte[] b = new byte[1];
		try {
			read(b, 0, 1);
			return b[0] & 0xff;
		} catch (IOException exc) {
			return -1;
		}
	}
	
	@Override
	public int read(byte[] b) 
	throws IOException
	{
		return read(b, 0, b.length);
	}
	
	
	@Override
	public void reset() 
	throws IOException
	{
		throw new IOException();
	}
}
