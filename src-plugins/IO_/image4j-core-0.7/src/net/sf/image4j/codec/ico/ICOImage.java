/*
 * ICOImage.java
 *
 * Created on February 19, 2007, 8:11 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.image4j.codec.ico;

import net.sf.image4j.codec.bmp.InfoHeader;

/**
 * Contains a decoded ICO image, as well as information about the source encoded ICO image.
 * @since 0.7
 * @author Ian McDonagh
 */
public class ICOImage extends net.sf.image4j.codec.bmp.BMPImage {
  
  protected IconEntry iconEntry;
  protected boolean pngCompressed = false;
  protected int iconIndex = -1;
  
  /**
   * Creates a new instance of ICOImage
   * @param image the BufferedImage decoded from the source ICO image
   * @param infoHeader the BMP InfoHeader structure for the BMP encoded ICO image
   * @param iconEntry the IconEntry structure describing the ICO image
   */
  public ICOImage(java.awt.image.BufferedImage image, net.sf.image4j.codec.bmp.InfoHeader infoHeader,
      IconEntry iconEntry) {
    super(image, infoHeader);
    this.iconEntry = iconEntry;
  }
  
  /**
   * The IconEntry associated with this <tt>ICOImage</tt>, which provides information
   * about the image format and encoding.
   * @return the IconEntry structure
   */
  public IconEntry getIconEntry() {
    return iconEntry;
  }
  
  /**
   * Sets the IconEntry associated with this <tt>ICOImage</tt>.
   * @param iconEntry the new IconEntry structure to set
   */
  public void setIconEntry(IconEntry iconEntry) {
    this.iconEntry = iconEntry;
  }
  
  /**
   * Specifies whether the encoded image is PNG compressed.
   * @return <tt>true</tt> if the encoded image is PNG compressed, <tt>false</tt> if it is plain BMP encoded
   */
  public boolean isPngCompressed() {
    return pngCompressed;
  }
  
  /**
   * Sets whether the encoded image is PNG compressed.
   * @param pngCompressed <tt>true</tt> if the encoded image is PNG compressed, <tt>false</tt> if it is plain BMP encoded
   */
  public void setPngCompressed(boolean pngCompressed) {
    this.pngCompressed = pngCompressed;
  }

  /**
   * The InfoHeader structure representing the encoded ICO image.
   * @return the InfoHeader structure, or <tt>null</tt> if there is no InfoHeader structure, which is possible for PNG compressed icons.
   */
  public InfoHeader getInfoHeader() {
    return super.getInfoHeader();
  }
  
  /**
   * The zero-based index for this <tt>ICOImage</tt> in the source ICO file or resource.
   * @return the index in the source, or <tt>-1</tt> if it is unknown.
   */
  public int getIconIndex() {
    return iconIndex;
  }
  
  /**
   * Sets the icon index, which is zero-based.
   * @param iconIndex the zero-based icon index, or <tt>-1</tt> if unknown.
   */
  public void setIconIndex(int iconIndex) {
    this.iconIndex = iconIndex;
  }
  
  /**
   * The width of the ICO image in pixels.
   * @return the width of the ICO image, or <tt>-1</tt> if unknown
   * @since 0.7alpha2
   */ 
  public int getWidth() {
    return iconEntry == null ? -1 : (iconEntry.bWidth == 0 ? 256 : iconEntry.bWidth);
  }
  
  /**
   * The height of the ICO image in pixels.
   * @return the height of the ICO image, or <tt>-1</tt> if unknown.
   * @since 0.7alpha2
   */
  public int getHeight() {
    return iconEntry == null ? -1 : (iconEntry.bHeight == 0 ? 256 : iconEntry.bHeight);
  }
  
  /**
   * The colour depth of the ICO image (bits per pixel).
   * @return the colour depth, or <tt>-1</tt> if unknown.
   * @since 0.7alpha2
   */
  public int getColourDepth() {
    return iconEntry == null ? -1 : iconEntry.sBitCount;
  }
  
  /**
   * The number of possible colours for the ICO image.
   * @return the number of colours, or <tt>-1</tt> if unknown.
   * @since 0.7alpha2 
   */
  public int getColourCount() {
    int bpp = iconEntry.sBitCount == 32 ? 24 : iconEntry.sBitCount;
    return bpp == -1 ? -1 : (int) (1 << bpp);
  }
  
  /**
   * Specifies whether this ICO image is indexed, that is, the encoded bitmap uses a colour table.
   * If <tt>getColourDepth()</tt> returns <tt>-1</tt>, the return value has no meaning.
   * @return <tt>true</tt> if indexed, <tt>false</tt> if not.
   * @since 0.7alpha2
   */
  public boolean isIndexed() {
    return iconEntry == null ? false : iconEntry.sBitCount <= 8;
  }
}
