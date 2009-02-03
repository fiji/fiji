/*
Copyright (c) 2006, Pepijn Van Eeckhoudt
All rights reserved.

Redistribution and use in source and binary forms,
with or without modification, are permitted provided
that the following conditions are met:
    * Redistributions of source code must retain the above
      copyright notice, this list of conditions and the
      following disclaimer.
    * Redistributions in binary form must reproduce the
      above copyright notice, this list of conditions and
      the following disclaimer in the documentation and/or
      other materials provided with the distribution.
    * Neither the name of the author nor the names
      of any contributors may be used to endorse or promote
      products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/
package iconsupport.icns;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.*;
import java.io.*;

public class IcnsCodec {
    private static final String ICNS = "icns";

    private static final String ICS_BW = "ics#";
    private static final int ICS_BW_SIZE = 16;

    private static final String ICN_BW = "ICN#";
    private static final int ICN_BW_SIZE = 32;

    private static final String SMALL_32_BIT_RGB = "is32";
    private static final String SMALL_8_BIT_MASK = "s8mk";
    static final int SMALL_SIZE = 16;

    private static final String LARGE_32_BIT_RGB = "il32";
    private static final String LARGE_8_BIT_MASK = "l8mk";
    static final int LARGE_SIZE = 32;

    private static final String HUGE_32_BIT_RGB = "ih32";
    private static final String HUGE_8_BIT_MASK = "h8mk";
    static final int HUGE_SIZE = 48;

    private static final String THUMBNAIL_32_BIT_RGB = "it32";
    private static final String THUMBNAIL_8_BIT_MASK = "t8mk";
    static final int THUMBNAIL_SIZE = 128;

    public void encode(IconSuite suite, OutputStream outputStream) throws IOException {
        byte[] small = encode32bitIcon(suite.getSmallIcon(), SMALL_32_BIT_RGB, SMALL_8_BIT_MASK);
        byte[] large = encode32bitIcon(suite.getLargeIcon(), LARGE_32_BIT_RGB, LARGE_8_BIT_MASK);
        byte[] huge = encode32bitIcon(suite.getHugeIcon(), HUGE_32_BIT_RGB, HUGE_8_BIT_MASK);
        byte[] thumbnail = encode32bitIcon(suite.getThumbnailIcon(), THUMBNAIL_32_BIT_RGB, IOSupport.LONG_INT_SIZE, THUMBNAIL_8_BIT_MASK);
        byte[] icsBW = encodeIcsBW(suite);
        byte[] icnBW = encodeIcnBW(suite);

        int totalSize = icsBW.length + icnBW.length + small.length + huge.length + large.length + thumbnail.length;

        DataOutputStream stream = new DataOutputStream(outputStream);

        IOSupport.writeLiteralLongInt(stream, ICNS);
        IOSupport.writeLongInt(stream, totalSize + 2 * IOSupport.LONG_INT_SIZE);

        stream.write(icsBW);
        stream.write(small);
        stream.write(icnBW);
        stream.write(large);
        stream.write(huge);
        stream.write(thumbnail);
    }

    private byte[] encodeIcsBW(IconSuite suite) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] data = encodeAsBWData(suite, ICS_BW_SIZE, ICS_BW_SIZE);
        IOSupport.writeLiteralLongInt(out, ICS_BW);
        IOSupport.writeLongInt(out, data.length + 2 * IOSupport.LONG_INT_SIZE);
        out.write(data);

        return out.toByteArray();
    }

    private byte[] encodeIcnBW(IconSuite suite) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] data = encodeAsBWData(suite, ICN_BW_SIZE, ICN_BW_SIZE);
        IOSupport.writeLiteralLongInt(out, ICN_BW);
        IOSupport.writeLongInt(out, 2 * data.length + 2 * IOSupport.LONG_INT_SIZE);
        out.write(data);
        // Reuse the pixel data for the mask
        out.write(data);

        return out.toByteArray();
    }

    private byte[] encodeAsBWData(IconSuite suite, int width, int height) {
        BufferedImage icon = suite.getBestMatchingIcon(width, height);
        if (icon == null) {
            return new byte[0];
        }

        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics graphics = scaledImage.getGraphics();
        graphics.drawImage(icon, 0, 0, width, height, 0, 0, icon.getWidth(), icon.getHeight(), null);
        graphics.dispose();

        DataBufferByte dataBuffer = ((DataBufferByte) scaledImage.getData().getDataBuffer());
        byte[] data = dataBuffer.getData();

        assert data.length == (width * height / 8) : "Incorrect data size [actual:" + data.length + ",expected:" + (width * height / 8) + "]";

        return data;
    }

    private byte[] encode32bitIcon(BufferedImage image, String rgbHeader, String maskHeader) throws IOException {
        return encode32bitIcon(image, rgbHeader, 0, maskHeader);
    }

    private byte[] encode32bitIcon(BufferedImage image, String rgbHeader, int rgbPrefixSize, String maskHeader) throws IOException {
        if (image == null) {
            return new byte[0];
        }

        int width = image.getWidth();
        int height = image.getHeight();
        byte[] bytesR = new byte[width * height];
        byte[] bytesG = new byte[width * height];
        byte[] bytesB = new byte[width * height];
        byte[] mask = new byte[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                byte a = (byte) ((pixel & 0xFF000000) >> 24);
                byte r = (byte) ((pixel & 0x00FF0000) >> 16);
                byte g = (byte) ((pixel & 0x0000FF00) >> 8);
                byte b = (byte) ((pixel & 0x000000FF));

                int pixelOffset = (y * width) + x;
                mask[pixelOffset] = a;
                bytesR[pixelOffset] = r;
                bytesG[pixelOffset] = g;
                bytesB[pixelOffset] = b;
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] packedR = RunLengthEncoding.packIconData(bytesR);
        byte[] packedG = RunLengthEncoding.packIconData(bytesG);
        byte[] packedB = RunLengthEncoding.packIconData(bytesB);
        int packedLength = packedR.length + packedG.length + packedB.length;
        int resourceSize = rgbPrefixSize + packedLength + 2 * IOSupport.LONG_INT_SIZE;

        IOSupport.writeLiteralLongInt(out, rgbHeader);
        IOSupport.writeLongInt(out, resourceSize);
        // The rgbPrefixSize allows the unknown value at the beginning of
        // the thumbnail icons to be added.
        out.write(new byte[rgbPrefixSize]);
        out.write(packedR);
        out.write(packedG);
        out.write(packedB);

        IOSupport.writeLiteralLongInt(out, maskHeader);
        IOSupport.writeLongInt(out, mask.length + 2 * IOSupport.LONG_INT_SIZE);
        out.write(mask);

        return out.toByteArray();
    }

    public IconSuite decode(InputStream inputStream) throws IOException {
        /*
          Literal:
            4 bytes, each byte is an ASCII character

          Long_int:
            4 bytes representing a 32-bit big endian integer

          Icns:
            'ICNS'     Literal
            size       Long_int; includes 'ICNS', size and all resources
            resource*  variable

          Resource:
            type       Literal
            size       Long_int; includes type, size and data
            data       Variable, dependes on type

          ics#:
            16x16 black and white icon
            64 bytes
            Each bit is a pixel

          ICN#:
            32x32 black and white icon + 32x32 1-bit mask
            256 bytes
            Each bit is a pixel

          is32:
            16x16 32-bit icon
            Variable size
            Data is compressed with PackBits variant
            Uncompressed data contains r, g and b channel; each byte is a value

          s8mk:
            16x16 mask
            256 bytes
            Each byte is an alpha value for the alpha channel of the is32 icon resource

          il32:
            32x32 32-bit icon
            see is32

          l8mk:
            32x32 mask
            1024 bytes
            Each byte is an alpha value for the alpha channel of the il32 icon resource

          ih32:
            48x48 32-bit icon
            see is32

          h8mk:
            48x48 mask
            2304 bytes
            Each byte is an alpha value for the alpha channel of the ih32 icon resource

          it32:
            128x128 32-bit icon
            Contains an extra Long_int before the rgb data that is always set to 0. The purpose
            of this value is unknown.
            see is32

          t8mk:
            128x128 mask
            16384 bytes
            Each byte is an alpha value for the alpha channel of the it32 icon resource
        */

        String header = IOSupport.readLiteralLongInt(inputStream);
        if (!header.equals(ICNS)) {
            throw new IOException("Unexpected header encountered: " + header);
        }

        int[] small = null;
        int[] large = null;
        int[] huge = null;
        int[] thumb = null;

        int fileSize = IOSupport.readLongInt(inputStream);
        int bytesLeft = fileSize - (2 * IOSupport.LONG_INT_SIZE);

        while (bytesLeft > 0) {
            String elementType = IOSupport.readLiteralLongInt(inputStream);
            int elementSize = IOSupport.readLongInt(inputStream);
            int elementDataSize = elementSize - (2 * IOSupport.LONG_INT_SIZE);

            if (elementType.equals(SMALL_32_BIT_RGB)) {
                byte[] elementData = new byte[elementDataSize];
                IOSupport.readFully(inputStream, elementData);
                small = decode32bitIcon(elementData, small, SMALL_SIZE);
            } else if (elementType.equals(LARGE_32_BIT_RGB)) {
                byte[] elementData = new byte[elementDataSize];
                IOSupport.readFully(inputStream, elementData);
                large = decode32bitIcon(elementData, large, LARGE_SIZE);
            } else if (elementType.equals(HUGE_32_BIT_RGB)) {
                byte[] elementData = new byte[elementDataSize];
                IOSupport.readFully(inputStream, elementData);
                huge = decode32bitIcon(elementData, huge, HUGE_SIZE);
            } else if (elementType.equals(THUMBNAIL_32_BIT_RGB)) {
                // The thumbnail icons contain an extra 4 bytes which
                // always seem to be set to 0. I don't know what this
                // data means, so for now simply skip it.
                IOSupport.skip(inputStream, IOSupport.LONG_INT_SIZE);

                byte[] elementData = new byte[elementDataSize - IOSupport.LONG_INT_SIZE];
                IOSupport.readFully(inputStream, elementData);
                thumb = decode32bitIcon(elementData, thumb, THUMBNAIL_SIZE);
            } else if (elementType.equals(SMALL_8_BIT_MASK)) {
                byte[] elementData = new byte[elementDataSize];
                IOSupport.readFully(inputStream, elementData);
                small = decode8bitMask(elementData, small, SMALL_SIZE);
            } else if (elementType.equals(LARGE_8_BIT_MASK)) {
                byte[] elementData = new byte[elementDataSize];
                IOSupport.readFully(inputStream, elementData);
                large = decode8bitMask(elementData, large, LARGE_SIZE);
            } else if (elementType.equals(HUGE_8_BIT_MASK)) {
                byte[] elementData = new byte[elementDataSize];
                IOSupport.readFully(inputStream, elementData);
                huge = decode8bitMask(elementData, huge, HUGE_SIZE);
            } else if (elementType.equals(THUMBNAIL_8_BIT_MASK)) {
                byte[] elementData = new byte[elementDataSize];
                IOSupport.readFully(inputStream, elementData);
                thumb = decode8bitMask(elementData, thumb, THUMBNAIL_SIZE);
            } else {
                IOSupport.skip(inputStream, elementDataSize);
            }

            bytesLeft -= elementSize;
        }

        IconSuite suite = new IconSuite();
        if (small != null) {
            suite.setSmallIcon(createImage(SMALL_SIZE, small));
        }
        if (large != null) {
            suite.setLargeIcon(createImage(LARGE_SIZE, large));
        }
        if (huge != null) {
            suite.setHugeIcon(createImage(HUGE_SIZE, huge));
        }
        if (thumb != null) {
            suite.setThumbnailIcon(createImage(THUMBNAIL_SIZE, thumb));
        }

        return suite;
    }

    private BufferedImage createImage(int size, int[] pixels) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, size, size, pixels, 0, size);
        return image;
    }

    private int[] decode32bitIcon(byte[] packedData, int[] destination, int size) {
        int nbPixels = size * size;
        byte[] unpackedData;

        if (packedData.length == nbPixels * 4) {
            unpackedData = packedData;
        } else {
            unpackedData = new byte[nbPixels * 3];
            RunLengthEncoding.unpackIconData(packedData, unpackedData);
        }

        int[] pixels;
        if (destination == null) {
            pixels = new int[nbPixels];
            // Fill in the alpha channel to make all pixels opaque
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = 0xFF000000;
            }
        } else {
            pixels = destination;
        }

        assert pixels.length == size * size : "Incorrect pixel buffer size";

        int unpackedIndex = 0;
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] |= (unpackedData[unpackedIndex++] & 0xFF) << 16;
        }
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] |= (unpackedData[unpackedIndex++] & 0xFF) << 8;
        }
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] |= (unpackedData[unpackedIndex++] & 0xFF);
        }


        return pixels;
    }

    private int[] decode8bitMask(byte[] data, int[] destination, int size) {
        int[] pixels;
        int arraySize = size * size;
        if (destination == null) {
            pixels = new int[arraySize];
        } else {
            pixels = destination;
        }

        assert pixels.length == arraySize : "Incorrect pixel buffer size [actual:" + pixels.length + ",expected:" + arraySize + "]";
        assert data.length == arraySize : "Incorrect data buffer size [actual:" + data.length + ",expected:" + arraySize + "]";

        for (int i = 0; i < pixels.length; i++) {
            // Clear old alpha value
            pixels[i] &= 0x00FFFFFF;
            // Write new alpha value
            pixels[i] |= (data[i] & 0xFF) << 24;
        }


        return pixels;
    }
}
