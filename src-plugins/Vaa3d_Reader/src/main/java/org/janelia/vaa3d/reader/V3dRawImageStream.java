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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.DataFormatException;

/*
 * Reads an InputStream of v3draw/v3dpbd format, and delivers images slices.
 * 
 * This class automatically determines whether the data are 8 or 16 bit,
 * whether compressed or uncompressed, and processes the input stream
 * accordingly.
 * 
 * In an effort to conserve memory, only one Z-slice is processed at one time.
 * 
 * This class does NOT implement InputStream, but uses and InputStream.
 */
public class V3dRawImageStream 
{
	/**
	 * Three subformats of v3draw are understood.  
	 * 
	 * <code>FORMAT_MYERS_PBD</code> is not supported by this reader.
	 * 
	 * @author Christopher M. Bruns
	 *
	 */
	public enum Format {
		FORMAT_PENG_RAW, // Original uncompressed format
		FORMAT_MURPHY_PBD, // Compressed data region, same header
		FORMAT_MYERS_PBD; // Modification by Gene Myers
	}
	
	/**
	 * The first 24-bytes of a v3draw file declare the particular subformat
	 */
	public static final String[] V3DRAW_MAGIC_COOKIE = {
		"raw_image_stack_by_hpeng",
		"v3d_volume_pkbitdf_encod",
		"v3d_stack_pkbit_by_gene1"
	};
	
	private InputStream inStream;
	// File metadata fields
	private String headerKey;
	private Format format;
	private int pixelBytes = 0;
	private ByteOrder endian = ByteOrder.LITTLE_ENDIAN;
	private int[] dimensions = {0,0,0,0};
	// Keep one slice in memory for streaming
	private Slice currentSlice;

	/**
	 * Creates a new <code>V3dRawImageStream</code> from a v3draw file.
	 * 
	 * The v3draw file header is read from the InputStream during construction.
	 * 
	 * @param input <code>InputStream</code> representing the input v3draw volume image.
	 */
	public V3dRawImageStream(InputStream input) {
		inStream = input;
		try {
			loadHeader();
		}
		catch (IOException exc) {
			throw new IllegalArgumentException(exc);
		}
		catch (DataFormatException exc) {
			throw new IllegalArgumentException(exc);
		}
	}
	
	/**
	 * 
	 * @return little or big endian byte order
	 */
	public ByteOrder getByteOrder() {
		return endian;
	}
	
	/**
	 * 
	 * @param index Zero(0) for x-axis, 1 for y-axis, 2 for z-axis, 3 for color channel.
	 * @return number of pixels in a particular axial direction
	 */
	public int getDimension(int index) {
		return dimensions[index];
	}
	
	/**
	 * 
	 * @return number of bytes in each pixel, per color channel
	 */
	public int getPixelBytes() {
		return pixelBytes;
	}
	
	private void loadHeader() 
	throws IOException, DataFormatException
	{
		// header is 43 bytes long
		byte[] buffer0 = new byte[43];
		ByteBuffer buffer = ByteBuffer.wrap(buffer0);
		inStream.read(buffer.array(), 0, 43);
		buffer.rewind();
		// Parse file type header string (24 bytes)
		headerKey = new String(buffer.array(), 0, 24);
		format = null;
		for (Format f : Format.values()) {
			if (headerKey.equals(V3DRAW_MAGIC_COOKIE[f.ordinal()])) {
				format = f;
				break;
			}
		}
		if (format == null)
		{
			throw new DataFormatException(
					"Vaa3D raw file header mismatch: " + headerKey);
		}
		// Parse data endian (one byte)
		buffer.position(24);
		char endianChar = (char)buffer.get(); // read endianness
		if (endianChar == 'B')
			endian = ByteOrder.BIG_ENDIAN;
		else if (endianChar == 'L')
			endian = ByteOrder.LITTLE_ENDIAN;
		else
			throw new DataFormatException(
					"Unrecognized endian field: " + endianChar);
		buffer.order(endian); // affects interpretation of subsequent multi-byte numbers
		// Parse number of bytes per pixel
		pixelBytes = buffer.getShort();
		if ( (pixelBytes <= 0) || (pixelBytes > 4) )
			throw new DataFormatException(
					"Illegal number of pixel bytes: " + pixelBytes);
		// Parse dimensions of volume - four four-byte values = 16 bytes
		dimensions = new int[]{
				buffer.getInt(),
				buffer.getInt(),
				buffer.getInt(),
				buffer.getInt()};
		// End of header.
		// Allocate slice
		currentSlice = new Slice(dimensions[0], dimensions[1], 
				pixelBytes, endian);
		
		// wrap inStream, if compressed format
		if (format == Format.FORMAT_MURPHY_PBD) {
			if (pixelBytes == 1)
				inStream = new Pbd8InputStream(inStream);
			else
				inStream = new Pbd16InputStream(inStream, endian);
		}
		else if (format == Format.FORMAT_MYERS_PBD) {
			// TODO
			throw new IllegalArgumentException("Loading Myers' pbd is not yet implemented");
			// inStream = new PbdMyers1InputStream(inStream);
		}
		else if (format == Format.FORMAT_PENG_RAW) {
			// leave instream alone. it is not compressed.
			// inStream = new BufferedInputStream(inStream); // for testing
		}
	}
	
	/**
	 * 
	 * @return a reference to the current Z-slice image buffer.
	 * 
	 * Becomes invalid after calling <code>loadNextSlice</code>
	 */
	public Slice getCurrentSlice() {
		return currentSlice;
	}

	/**
	 * Replaces the current slice buffer with a new Z-slice of image data
	 * from the input volume.
	 * 
	 * @throws IOException
	 */
	public void loadNextSlice() 
	throws IOException
	{
		currentSlice.read(inStream);
	}
	
	/**
	 * Represents a single z-plane of the input volume in a single color channel.
	 * 
	 * @author Christopher M. Bruns
	 */
	class Slice 
	{
		private int sliceByteCount;
		private ByteBuffer sliceBuffer;
		private int sliceIndex;
		private int sx, pixelBytes;
		
		/**
		 * Creates a new Z-slice single color channel image buffer.
		 * 
		 * @param sizeX image width in pixels
		 * @param sizeY image height in pixels
		 * @param pixelBytes number of bytes per pixel (in one color channel)
		 * @param byteOrder little or big endian
		 */
		public Slice(int sizeX, int sizeY, int pixelBytes, ByteOrder byteOrder) 
		{
			sliceByteCount = sizeX * sizeY * pixelBytes;
			byte[] buffer0 = new byte[sliceByteCount];
			sliceBuffer = ByteBuffer.wrap(buffer0);
			sliceBuffer.order(byteOrder);
			sliceIndex = -1;
			sx = sizeX;
			this.pixelBytes = pixelBytes;
		}
		
		/**
		 * 
		 * @return zero-based index of the current slice.
		 * 
		 * Starts at -1 before any slice data have loaded.
		 */
		public int getSliceIndex() {
			return sliceIndex;
		}
		
		/**
		 * 
		 * @return the raw pixel data for this slice.
		 */
		public ByteBuffer getByteBuffer()
		{
			return sliceBuffer;
		}
		
		/**
		 * 
		 * @param x x-coordinate of the desired pixel
		 * @param y y-coordinate of the desired pixel
		 * @return intensity of the desired pixel
		 */
		public int getValue(int x, int y) 
		{
			int index = x + sx * y;
			if (pixelBytes == 1) {
				return sliceBuffer.get(index);
			}
			else if (pixelBytes == 2)
				return sliceBuffer.getShort(index);
			else
				return sliceBuffer.getInt(index);
		}
		
		/**
		 * Populates one slice from an open InputStream containing v3draw 
		 * 3D volume image data.
		 * 
		 * @param inStream <code>InputStream</code> representing the input volume
		 * @throws IOException
		 */
		public void read(InputStream inStream) 
		throws IOException
		{
			inStream.read(sliceBuffer.array(), 0, sliceByteCount);
			++sliceIndex;
		}
	}

}
