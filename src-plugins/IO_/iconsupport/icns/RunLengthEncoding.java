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

import java.util.Arrays;

class RunLengthEncoding {
    private RunLengthEncoding() {
    }

    /**
     * Decompresses data that was compressed using the PackBits variant used in icon resources.
     * @see #packIconData(byte[])
     */
    public static void unpackIconData(byte[] packedData, byte[] unpackedData) {
        int in = 0;
        int out = 0;

        while (in < packedData.length && out < unpackedData.length) {
            int header = packedData[in++] & 0xFF;
            if ((header & 0x80) == 0) {
                int nbLiteralBytes = header + 1;
                System.arraycopy(packedData, in, unpackedData, out, nbLiteralBytes);

                in += nbLiteralBytes;
                out += nbLiteralBytes;
            } else {
                int nbTimesToRepeat = header - 125;
                byte data = packedData[in++];
                Arrays.fill(unpackedData, out, out + nbTimesToRepeat, data);
                out += nbTimesToRepeat;
            }
        }
    }

    /**
     * Compresses data using the PackBits variant used in icon resources.
     * 32-bit icon resources use an undocumented run length compression scheme that is not
     * documented by Apple. It resembles that PackBits algorithm, however the semantics
     * of the header byte values are slightly modified. A pseudo code description was found
     * at http://www.macdisk.com/maciconen.php3
     * A compressed stream consists of a sequence of header and data pairs.
     * The header is a single unsigned byte. A header value smaller than 127 indicates that
     * the next (header value + 1) bytes are data bytes and should be copied verbatim. A header
     * value greater than 127 indicates that their is only a single data byte that follows and
     * it should be repeated (header value - 125) times.
     */
    public static byte[] packIconData(byte[] unpackedData) {
        byte[] resultBuffer = new byte[unpackedData.length + (unpackedData.length + 128) / 128];
        int resultSize = RunLengthEncoding.packIconData(unpackedData, resultBuffer);

        byte[] packedData = new byte[resultSize];
        System.arraycopy(resultBuffer, 0, packedData, 0, resultSize);
        return packedData;
    }

    /**
     * Compresses data using the PackBits variant used in icon resources.
     * @see #packIconData(byte[])
     */
    public static int packIconData(byte[] unpackedData, byte[] packedData) {
        int in = 0;
        int out = 0;


        while (in < unpackedData.length) {
            int literalStart = in;
            byte data = unpackedData[in++];

            // Read up to 128 literal bytes
            // Stop if 3 or more consecutive bytes are equal or EOF is reached
            int nbBytesRead = 1;
            int nbRepeatedBytes = 0;
            while(in < unpackedData.length && nbBytesRead < 128 && nbRepeatedBytes < 3) {
                byte nextData = unpackedData[in++];
                if (nextData == data) {
                    if (nbRepeatedBytes == 0) {
                        nbRepeatedBytes = 2;
                    } else {
                        nbRepeatedBytes++;
                    }
                } else {
                    nbRepeatedBytes = 0;
                }

                nbBytesRead++;
                data = nextData;
            }

            int nbLiteralBytes;
            if (nbRepeatedBytes < 3) {
                nbLiteralBytes = nbBytesRead;
                nbRepeatedBytes = 0;
            } else {
                nbLiteralBytes = nbBytesRead - nbRepeatedBytes;
            }

            // Write the literal bytes that were read
            if (nbLiteralBytes > 0) {
                packedData[out++] = (byte)(nbLiteralBytes - 1);
                System.arraycopy(unpackedData, literalStart, packedData, out, nbLiteralBytes);
                out += nbLiteralBytes;
            }

            // Read up to 130 consecutive bytes that are equal
            while(in < unpackedData.length && unpackedData[in] == data && nbRepeatedBytes < 130) {
                nbRepeatedBytes++;
                in++;
            }

            if (nbRepeatedBytes >= 3) {
                // Write the repeated bytes if there are 3 or more
                packedData[out++] = (byte)(nbRepeatedBytes + 125);
                packedData[out++] = data;
            } else {
                // Else move back the in pointer to ensure the repeated bytes
                // are included in the next literal string
                in -= nbRepeatedBytes;
            }
        }

        return out;
    }

    /**
     * Decompresses data compressed using the PackBits algorithm.
     */
    public static void unpackBits(byte[] packedData, byte[] unpackedData) {
        int in = 0;
        int out = 0;

        while (in < packedData.length) {
            byte header = packedData[in++];
            if (header > 0) {
                int nbLiteralBytes = 1 + header;
                System.arraycopy(packedData, in, unpackedData, out, nbLiteralBytes);
                in += nbLiteralBytes;
                out += nbLiteralBytes;
            } else if (header < 0 && header != -128) {
                int nbTimesToRepeat = 1 - header;
                byte data = packedData[in++];
                Arrays.fill(unpackedData, out, out + nbTimesToRepeat, data);
                out += nbTimesToRepeat;
            }
        }
    }
}
