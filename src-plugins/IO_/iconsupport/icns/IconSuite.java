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

public class IconSuite {
    private static final int SMALL_INDEX = 0;
    private static final int LARGE_INDEX = 1;
    private static final int HUGE_INDEX = 2;
    private static final int THUMBNAIL_INDEX = 3;

    private BufferedImage[] icons = new BufferedImage[4];

    /**
     * Returns the icon that mathces the given dimensions the closest.
     */
    public BufferedImage getBestMatchingIcon(int width, int height) {
        int bestMatch = -1;
        int bestWidth = -1;
        int bestHeight = -1;

        for (int i = 0; i < icons.length; i++) {
            BufferedImage icon = icons[i];
            if (icon != null) {
                int iconWidth = icon.getWidth();
                int iconHeight = icon.getHeight();
                if (iconWidth == width && iconHeight == height) {
                    bestMatch = i;
                    break;
                } else {
                    if (bestMatch == -1 || (iconWidth >= width && iconHeight >= height && bestWidth < width && bestHeight < height)) {
                        bestMatch = i;
                        bestWidth = iconWidth;
                        bestHeight = iconHeight;
                    }
                }
            }
        }

        if (bestMatch != -1) {
            return icons[bestMatch];
        } else {
            return null;
        }
    }

    /**
     * Returns the thumbnail icon or null if a thumbnail icon has not been set.
     */
    public BufferedImage getThumbnailIcon() {
        return icons[THUMBNAIL_INDEX];
    }

    /**
     * Sets the thumbnail icon. Thumbnail icons must have a dimension of 128x128.
     */
    public void setThumbnailIcon(BufferedImage thumbnailIcon) {
        verifyIcon(thumbnailIcon, IcnsCodec.THUMBNAIL_SIZE);
        icons[THUMBNAIL_INDEX] = thumbnailIcon;
    }

    /**
     * Returns the 'huge' icon or null if a 'huge' icon has not been set.
     */
    public BufferedImage getHugeIcon() {
        return icons[HUGE_INDEX];
    }

    /**
     * Sets the 'huge' icon. Huge icons must have a dimension of 48x48.
     */
    public void setHugeIcon(BufferedImage hugeIcon) {
        verifyIcon(hugeIcon, IcnsCodec.HUGE_SIZE);
        icons[HUGE_INDEX] = hugeIcon;
    }

    /**
     * Returns the 'large' icon or null if a 'large' icon has not been set.
     */
    public BufferedImage getLargeIcon() {
        return icons[LARGE_INDEX];
    }

    /**
     * Sets the 'large' icon. Large icons must have a dimension of 32x32.
     */
    public void setLargeIcon(BufferedImage largeIcon) {
        verifyIcon(largeIcon, IcnsCodec.LARGE_SIZE);
        icons[LARGE_INDEX] = largeIcon;
    }

    /**
     * Returns the 'small' icon or null if a 'small' icon has not been set.
     */
    public BufferedImage getSmallIcon() {
        return icons[SMALL_INDEX];
    }

    /**
     * Sets the 'small' icon. Huge icons must have a dimension of 16x16.
     */
    public void setSmallIcon(BufferedImage smallIcon) {
        verifyIcon(smallIcon, IcnsCodec.SMALL_SIZE);
        icons[SMALL_INDEX] = smallIcon;
    }

    private void verifyIcon(BufferedImage icon, int size) {
        if (icon != null) {
            if (icon.getWidth() != size || icon.getHeight() != size) {
                throw new IllegalArgumentException("Incorrect icon size [actual:" + icon.getWidth() + "x" + icon.getHeight() + ",expected:" + size + "x" + size + "]");
            }
        }
    }
}
