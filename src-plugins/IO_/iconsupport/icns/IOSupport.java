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

import java.io.*;

class IOSupport {
    public static final int LONG_INT_SIZE = 4;
    public static final int INT_SIZE = 2;

    private IOSupport() {
    }

    public static String readLiteralLongInt(InputStream inputStream) throws IOException {
        byte[] data = new byte[LONG_INT_SIZE];
        readFully(inputStream, data);
        return new String(data);
    }

    public static void writeLiteralLongInt(OutputStream outputStream, String s) throws IOException {
        byte[] bytes = s.getBytes();
        outputStream.write(bytes, 0, Math.min(LONG_INT_SIZE, bytes.length));
        if (bytes.length < LONG_INT_SIZE) {
            int padding = LONG_INT_SIZE - bytes.length;
            for (int i = 0; i < padding; i++) {
                outputStream.write(' ');
            }
        }
    }

    public static int readLongInt(InputStream inputStream) throws IOException {
        byte[] data = new byte[LONG_INT_SIZE];
        readFully(inputStream, data);
        return ((data[0] & 0xFF) << 24) +
                ((data[1] & 0xFF) << 16) +
                ((data[INT_SIZE] & 0xFF) << 8) +
                (data[3] & 0xFF);
    }

    public static void writeLongInt(OutputStream outputStream, int i) throws IOException {
        outputStream.write((i & 0xFF000000) >> 24);
        outputStream.write((i & 0x00FF0000) >> 16);
        outputStream.write((i & 0x0000FF00) >> 8);
        outputStream.write(i & 0x000000FF);
    }

    public static long readInt(InputStream inputStream) throws IOException {
        byte[] data = new byte[INT_SIZE];
        readFully(inputStream, data);
        return ((data[0] & 0xFF) << 8) + data[1];
    }

    public static void writeInt(OutputStream outputStream, int i) throws IOException {
        outputStream.write((i & 0x0000FF00) >> 8);
        outputStream.write(i & 0x000000FF);
    }

    public static void readFully(InputStream inputStream, byte b[]) throws IOException {
        int nbBytesToRead = b.length;

        int nbBytesRead = 0;
        while (nbBytesRead < nbBytesToRead) {
            int nbBytesActuallyRead = inputStream.read(b, nbBytesRead, nbBytesToRead - nbBytesRead);
            if (nbBytesActuallyRead < 0)
                throw new EOFException();
            nbBytesRead += nbBytesActuallyRead;
        }
    }

    public static void skip(InputStream inputStream, long nbBytesToSkip) throws IOException {
        long nbBytesSkipped = 0;
        while (nbBytesSkipped < nbBytesToSkip) {
            long nbBytesActuallySkipped = inputStream.skip(nbBytesToSkip - nbBytesSkipped);
            if (nbBytesActuallySkipped < 0)
                throw new EOFException();
            nbBytesSkipped += nbBytesActuallySkipped;
        }
    }
}
