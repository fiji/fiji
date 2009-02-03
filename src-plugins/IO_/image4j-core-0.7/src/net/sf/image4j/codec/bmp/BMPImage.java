/*
 * BMPImage.java
 *
 * Created on February 19, 2007, 8:08 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.image4j.codec.bmp;

/**
 * Contains a decoded BMP image, as well as information about the source encoded image.
 * @since 0.7
 * @author Ian McDonagh
 */
public class BMPImage {
  
  protected InfoHeader infoHeader;
  protected java.awt.image.BufferedImage image;
  
  /**
   * Creates a new instance of BMPImage
   * @param image the decoded image
   * @param infoHeader the InfoHeader structure providing information about the source encoded image
   */
  public BMPImage(java.awt.image.BufferedImage image, InfoHeader infoHeader) {
    this.image = image;
    this.infoHeader = infoHeader;
  }
  
  /**
   * The InfoHeader structure representing the encoded BMP image.
   */ 
  public InfoHeader getInfoHeader() {
    return infoHeader;
  }
  
  /**
   * Sets the InfoHeader structure used for encoding the BMP image.
   */ 
  public void setInfoHeader(InfoHeader infoHeader) {
    this.infoHeader = infoHeader;
  }
  
  /**
   * The decoded BMP image.
   */
  public java.awt.image.BufferedImage getImage() {
    return image;
  }
  
  /**
   * Sets the image to be encoded.
   */
  public void setImage(java.awt.image.BufferedImage image) {
    this.image = image;
  }
  
  /**
   * The width of the BMP image in pixels.
   * @return the width of the BMP image, or <tt>-1</tt> if unknown
   * @since 0.7alpha2
   */ 
  public int getWidth() {
    return infoHeader == null ? -1 : infoHeader.iWidth;
  }
  
  /**
   * The height of the BMP image in pixels.
   * @return the height of the BMP image, or <tt>-1</tt> if unknown.
   * @since 0.7alpha2
   */
  public int getHeight() {
    return infoHeader == null ? -1 : infoHeader.iHeight;
  }
  
  /**
   * The colour depth of the BMP image (bits per pixel).
   * @return the colour depth, or <tt>-1</tt> if unknown.
   * @since 0.7alpha2
   */
  public int getColourDepth() {
    return infoHeader == null ? -1 : infoHeader.sBitCount;
  }
  
  /**
   * The number of possible colours for the BMP image.
   * @return the number of colours, or <tt>-1</tt> if unknown.
   * @since 0.7alpha2 
   */
  public int getColourCount() {
    int bpp = infoHeader.sBitCount == 32 ? 24 : infoHeader.sBitCount;
    return bpp == -1 ? -1 : (int) (1 << bpp);
  } 
  
  /**
   * Specifies whether this BMP image is indexed, that is, the encoded bitmap uses a colour table.
   * If <tt>getColourDepth()</tt> returns <tt>-1</tt>, the return value has no meaning.
   * @return <tt>true</tt> if indexed, <tt>false</tt> if not.
   * @since 0.7alpha2
   */
  public boolean isIndexed() {
    return infoHeader == null ? false : infoHeader.sBitCount <= 8;
  }
}
