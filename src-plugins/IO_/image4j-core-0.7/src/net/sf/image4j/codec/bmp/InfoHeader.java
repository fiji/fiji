/*
 * InfoHeader.java
 *
 * Created on 10 May 2006, 08:10
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.image4j.codec.bmp;

import java.io.IOException;
import net.sf.image4j.io.LittleEndianOutputStream;

/**
 * Represents a bitmap <tt>InfoHeader</tt> structure, which provides header information.
 * @author Ian McDonagh
 */
public class InfoHeader {
  
  /**
   * The size of this <tt>InfoHeader</tt> structure in bytes.
   */
  public int iSize;
  /**
   * The width in pixels of the bitmap represented by this <tt>InfoHeader</tt>.
   */
  public int iWidth;
  /**
   * The height in pixels of the bitmap represented by this <tt>InfoHeader</tt>.
   */
  public int iHeight;
  /**
   * The number of planes, which should always be <tt>1</tt>.
   */
  public short sPlanes;
  /**
   * The bit count, which represents the colour depth (bits per pixel).
   * This should be either <tt>1</tt>, <tt>4</tt>, <tt>8</tt>, <tt>24</tt> or <tt>32</tt>.
   */
  public short sBitCount;
  /**
   * The compression type, which should be one of the following:
   * <ul>
   *  <li>{@link BMPConstants#BI_RGB BI_RGB} - no compression</li>
   *  <li>{@link BMPConstants#BI_RLE8 BI_RLE8} - 8-bit RLE compression</li>
   *  <li>{@link BMPConstants#BI_RLE4 BI_RLE4} - 4-bit RLE compression</li>
   * </ul>
   */
  public int iCompression;
  /**
   * The compressed size of the image in bytes, or <tt>0</tt> if <tt>iCompression</tt> is <tt>0</tt>.
   */
  public int iImageSize;
  /**
   * Horizontal resolution in pixels/m.
   */
  public int iXpixelsPerM;
  /**
   * Vertical resolution in pixels/m.
   */
  public int iYpixelsPerM;
  /**
   * Number of colours actually used in the bitmap.
   */
  public int iColorsUsed;
  /**
   * Number of important colours (<tt>0</tt> = all).
   */
  public int iColorsImportant;
  
  /**
   * Calculated number of colours, based on the colour depth specified by {@link #sBitCount sBitCount}.
   */
  public int iNumColors;
  
  /** 
   * Creates an <tt>InfoHeader</tt> structure from the source input.
   * @param in the source input
   * @throws java.io.IOException if an error occurs
   */
  public InfoHeader(net.sf.image4j.io.LittleEndianInputStream in) throws IOException {
    //Size of InfoHeader structure = 40
    iSize = in.readIntLE();
    
    init(in, iSize);
  }
  
  /**
   * @since 0.6
   */
  public InfoHeader(net.sf.image4j.io.LittleEndianInputStream in, int infoSize) throws IOException {
    init(in, infoSize);
  }
  
  /**
   * @since 0.6
   */
  protected void init(net.sf.image4j.io.LittleEndianInputStream in, int infoSize) throws IOException {
    this.iSize = infoSize;
    
    //Width
    iWidth = in.readIntLE();
    //Height
    iHeight = in.readIntLE();
    //Planes (=1)
    sPlanes = in.readShortLE();
    //Bit count
    sBitCount = in.readShortLE();
    
    //calculate NumColors
    iNumColors = (int) Math.pow(2, sBitCount);
    
    //Compression
    iCompression = in.readIntLE();
    //Image size - compressed size of image or 0 if Compression = 0
    iImageSize = in.readIntLE();
    //horizontal resolution pixels/meter
    iXpixelsPerM = in.readIntLE();
    //vertical resolution pixels/meter
    iYpixelsPerM = in.readIntLE();
    //Colors used - number of colors actually used
    iColorsUsed = in.readIntLE();
    //Colors important - number of important colors 0 = all
    iColorsImportant = in.readIntLE();
  }
  
  /**
   * Creates an <tt>InfoHeader</tt> with default values.
   */
  public InfoHeader() {
    //Size of InfoHeader structure = 40
    iSize = 40;
    //Width
    iWidth = 0;
    //Height
    iHeight = 0;
    //Planes (=1)
    sPlanes = 1;
    //Bit count
    sBitCount = 0;
    
    //caculate NumColors
    iNumColors = 0;
    
    //Compression
    iCompression = BMPConstants.BI_RGB;
    //Image size - compressed size of image or 0 if Compression = 0
    iImageSize = 0;
    //horizontal resolution pixels/meter
    iXpixelsPerM = 0;
    //vertical resolution pixels/meter
    iYpixelsPerM = 0;
    //Colors used - number of colors actually used
    iColorsUsed = 0;
    //Colors important - number of important colors 0 = all
    iColorsImportant = 0;
  }
  
  /**
   * Creates a copy of the source <tt>InfoHeader</tt>.
   * @param source the source to copy
   */
  public InfoHeader(InfoHeader source) {
    iColorsImportant = source.iColorsImportant;
    iColorsUsed = source.iColorsUsed;
    iCompression = source.iCompression;
    iHeight = source.iHeight;
    iWidth = source.iWidth;
    iImageSize = source.iImageSize;
    iNumColors = source.iNumColors;
    iSize = source.iSize;
    iXpixelsPerM = source.iXpixelsPerM;
    iYpixelsPerM = source.iYpixelsPerM;
    sBitCount = source.sBitCount;
    sPlanes = source.sPlanes;
        
  }
  
  /**
   * Writes the <tt>InfoHeader</tt> structure to output
   * @param out the output to which the structure will be written
   * @throws java.io.IOException if an error occurs
   */ 
  public void write(net.sf.image4j.io.LittleEndianOutputStream out) throws IOException {
      //Size of InfoHeader structure = 40
    out.writeIntLE(iSize);
    //Width
    out.writeIntLE(iWidth);
    //Height
    out.writeIntLE(iHeight);
    //Planes (=1)
    out.writeShortLE(sPlanes);
    //Bit count
    out.writeShortLE(sBitCount);
    
    //caculate NumColors
    //iNumColors = (int) Math.pow(2, sBitCount);
    
    //Compression
    out.writeIntLE(iCompression);
    //Image size - compressed size of image or 0 if Compression = 0
    out.writeIntLE(iImageSize);
    //horizontal resolution pixels/meter
    out.writeIntLE(iXpixelsPerM);
    //vertical resolution pixels/meter
    out.writeIntLE(iYpixelsPerM);
    //Colors used - number of colors actually used
    out.writeIntLE(iColorsUsed);
    //Colors important - number of important colors 0 = all
    out.writeIntLE(iColorsImportant);
  }
}
