/*
 * ICOEncoder.java
 *
 * Created on 12 May 2006, 04:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.image4j.codec.ico;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageWriter;
import net.sf.image4j.io.LittleEndianOutputStream;
import net.sf.image4j.codec.bmp.BMPEncoder;
import net.sf.image4j.util.ConvertUtil;
import net.sf.image4j.codec.bmp.InfoHeader;

/**
 * Encodes images in ICO format.
 * @author Ian McDonagh
 */
public class ICOEncoder {
  
  /** Creates a new instance of ICOEncoder */
  private ICOEncoder() {
  }
  
  /**
   * Encodes and writes a single image to file without colour depth conversion.
   * @param image the source image to encode
   * @param file the output file to which the encoded image will be written
   * @throws java.io.IOException if an exception occurs
   */
  public static void write(BufferedImage image, java.io.File file) throws IOException {
    write(image, -1, file);
  }
  
  /**
   * Encodes and writes a single image without colour depth conversion.
   * @param image the source image to encode
   * @param os the output to which the encoded image will be written
   * @throws java.io.IOException if an exception occurs
   */
  public static void write(BufferedImage image, java.io.OutputStream os) throws IOException {
    write(image, -1, os);
  }
  
  /**
   * Encodes and writes multiple images without colour depth conversion.
   * @param images the list of source images to be encoded
   * @param os the output to which the encoded image will be written
   * @throws java.io.IOException if an error occurs
   */
  public static void write(List<BufferedImage> images, java.io.OutputStream os) throws IOException {
    write(images, null, null, os);
  }
  
  /**
   * Encodes and writes multiple images to file without colour depth conversion.
   * @param images the list of source images to encode
   * @param file the file to which the encoded images will be written
   * @throws java.io.IOException if an exception occurs
   */
  public static void write(List<BufferedImage> images, java.io.File file) throws IOException {
    write(images, null, file);
  }
  
  /**
   * Encodes and writes multiple images to file with the colour depth conversion using the specified values.
   * @param images the list of source images to encode
   * @param bpp array containing desired colour depths for colour depth conversion
   * @param file the output file to which the encoded images will be written
   * @throws java.io.IOException if an error occurs
   */
  public static void write(List<BufferedImage> images, int[] bpp, java.io.File file) throws IOException {
    write(images, bpp, new java.io.FileOutputStream(file));
  }
  
  /**
   * Encodes and outputs a list of images in ICO format.  The first image in the list will be at index #0 in the ICO file, the second at index #1, and so on.
   * @param images List of images to encode, which will be output in the order supplied in the list.
   * @param bpp Array containing the color depth (bits per pixel) for encoding the corresponding image at each index in the <tt>images</tt> list.  If the array is <tt>null</tt>, no colour depth conversion will be performed.  A colour depth value of <tt>-1</tt> at a particular index indicates that no colour depth conversion should be performed for that image.
   * @param compress Array containing the compression flag for the corresponding image at each index in the <tt>images</tt> list.  If the array is <tt>null</tt>, no compression will be peformed. A value of <tt>true</tt> specifies that compression should be performed, while a value of <tt>false</tt> specifies that no compression should be performed.
   * @param file the file to which the encoded images will be written.
   * @throws java.io.IOException if an error occurred.
   * @since 0.6
   */
  public static void write(List<BufferedImage> images, int[] bpp, boolean[] compress, java.io.File file) throws IOException {
    write(images, bpp, compress, new java.io.FileOutputStream(file));
  }
  
  /**
   * Encodes and writes a single image to file with colour depth conversion using the specified value.
   * @param image the source image to encode
   * @param bpp the colour depth (bits per pixel) for the colour depth conversion, or <tt>-1</tt> if no colour depth conversion should be performed
   * @param file the output file to which the encoded image will be written
   * @throws java.io.IOException if an error occurs
   */
  public static void write(BufferedImage image, int bpp, java.io.File file) throws IOException {
    write(image, bpp, new java.io.FileOutputStream(file));
  }
  
  /**
   * Encodes and outputs a single image in ICO format.
   * Convenience method, which calls {@link #write(java.util.List,int[],java.io.OutputStream) write(java.util.List,int[],java.io.OutputStream)}.
   * @param image The image to encode.
   * @param bpp Colour depth (in bits per pixel) for the colour depth conversion, or <tt>-1</tt> if no colour depth conversion should be performed.
   * @param os The output to which the encoded image will be written.
   * @throws java.io.IOException if an error occurs when trying to write the output.
   */
  public static void write(BufferedImage image, int bpp, java.io.OutputStream os) throws IOException {
    List<BufferedImage> list = new java.util.ArrayList<BufferedImage>(1);
    list.add(image);
    write(list, new int[] { bpp }, new boolean[] { false }, os);
  }
  
   
  /**
   * Encodes and outputs a list of images in ICO format.  The first image in the list will be at index #0 in the ICO file, the second at index #1, and so on.
   * @param images List of images to encode, which will be output in the order supplied in the list.
   * @param bpp Array containing the color depth (bits per pixel) for encoding the corresponding image at each index in the <tt>images</tt> list.  If the array is <tt>null</tt>, no colour depth conversion will be performed.  A colour depth value of <tt>-1</tt> at a particular index indicates that no colour depth conversion should be performed for that image.
   * @param os The output to which the encoded images will be written.
   * @throws java.io.IOException if an error occurred.
   */  
  public static void write(List<BufferedImage> images, int[] bpp, java.io.OutputStream os) throws IOException {
    write(images, bpp, null, os);
  }
  
  /**
   * Encodes and outputs a list of images in ICO format.  The first image in the list will be at index #0 in the ICO file, the second at index #1, and so on.
   * @param images List of images to encode, which will be output in the order supplied in the list.
   * @param bpp Array containing the color depth (bits per pixel) for encoding the corresponding image at each index in the <tt>images</tt> list.  If the array is <tt>null</tt>, no colour depth conversion will be performed.  A colour depth value of <tt>-1</tt> at a particular index indicates that no colour depth conversion should be performed for that image.
   * @param compress Array containing the compression flag for the corresponding image at each index in the <tt>images</tt> list.  If the array is <tt>null</tt>, no compression will be peformed. A value of <tt>true</tt> specifies that compression should be performed, while a value of <tt>false</tt> specifies that no compression should be performed.
   * @param os The output to which the encoded images will be written.
   * @throws java.io.IOException if an error occurred.
   * @since 0.6
   */
  public static void write(List<BufferedImage> images, int[] bpp, boolean[] compress, java.io.OutputStream os) throws IOException {
    LittleEndianOutputStream out = new LittleEndianOutputStream(os);
    
    int count = images.size();
    
    //file header 6
    writeFileHeader(count, ICOConstants.TYPE_ICON, out);
    
    //file offset where images start
    int fileOffset = 6 + count * 16;
    
    List<InfoHeader> infoHeaders = new java.util.ArrayList<InfoHeader>(count);
    
    List<BufferedImage> converted = new java.util.ArrayList<BufferedImage>(count);
    
    List<byte[]> compressedImages = null;
    if (compress != null) {
      compressedImages = new java.util.ArrayList<byte[]>(count);
    }
    
    javax.imageio.ImageWriter pngWriter = null;
    
    //icon entries 16 * count
    for (int i = 0; i < count; i++) {
      BufferedImage img = images.get(i);
      int b = bpp == null ? -1 : bpp[i];
      //convert image
      BufferedImage imgc = b == -1 ? img : convert(img, b);
      converted.add(imgc);
      //create info header
      InfoHeader ih = BMPEncoder.createInfoHeader(imgc);
      //create icon entry
      IconEntry e = createIconEntry(ih);
      
      if (compress != null) {
        if (compress[i]) {
          if (pngWriter == null) {
            pngWriter = getPNGImageWriter();            
          }
          byte[] compressedImage = encodePNG(pngWriter, imgc);
          compressedImages.add(compressedImage);
          e.iSizeInBytes = compressedImage.length;
        } else {
          compressedImages.add(null);
        }
      }
      
      ih.iHeight *= 2;
      
      e.iFileOffset = fileOffset;
      fileOffset += e.iSizeInBytes;
      
      e.write(out);
      
      infoHeaders.add(ih);
    }
    
    //images
    for (int i = 0; i < count; i++) {
      BufferedImage img = images.get(i);
      BufferedImage imgc = converted.get(i);
      
      if (compress == null || !compress[i]) {
        
        //info header
        InfoHeader ih = infoHeaders.get(i);
        ih.write(out);
        //color map
        if (ih.sBitCount <= 8) {
          IndexColorModel icm = (IndexColorModel) imgc.getColorModel();
          BMPEncoder.writeColorMap(icm, out);
        }
        //xor bitmap
        writeXorBitmap(imgc, ih, out);
        //and bitmap
        writeAndBitmap(img, out);
        
      }
      else {
        byte[] compressedImage = compressedImages.get(i);
        out.write(compressedImage);
      }
      
      //javax.imageio.ImageIO.write(imgc, "png", new java.io.File("test_"+i+".png"));
    }
    
  }
  
  /**
   * Writes the ICO file header for an ICO containing the given number of images.
   * @param count the number of images in the ICO
   * @param type one of {@link net.sf.image4j.codec.ico.ICOConstants#TYPE_ICON TYPE_ICON} or
   * {@link net.sf.image4j.codec.ico.ICOConstants#TYPE_CURSOR TYPE_CURSOR}
   * @param out the output to which the file header will be written
   * @throws java.io.IOException if an error occurs
   */
  public static void writeFileHeader(int count, int type, LittleEndianOutputStream out) throws IOException {
    //reserved 2
    out.writeShortLE((short) 0);
    //type 2
    out.writeShortLE((short) type);
    //count 2
    out.writeShortLE((short) count);
  }
  
  /**
   * Constructs an <tt>IconEntry</tt> from the given <tt>InfoHeader</tt>
   * structure.
   * @param ih the <tt>InfoHeader</tt> structure from which to construct the <tt>IconEntry</tt> structure.
   * @return the <tt>IconEntry</tt> structure constructed from the <tt>IconEntry</tt> structure.
   */
  public static IconEntry createIconEntry(InfoHeader ih)  {
    IconEntry ret = new IconEntry();
    //width 1
    ret.bWidth = ih.iWidth == 256 ? 0 : ih.iWidth;
    //height 1
    ret.bHeight = ih.iHeight == 256 ? 0 : ih.iHeight;
    //color count 1
    ret.bColorCount = ih.iNumColors >= 256 ? 0 : ih.iNumColors;
    //reserved 1
    ret.bReserved = 0;
    //planes 2 = 1
    ret.sPlanes = 1;
    //bit count 2
    ret.sBitCount = ih.sBitCount;
    //sizeInBytes 4 - size of infoHeader + xor bitmap + and bitbmap
    int cmapSize = BMPEncoder.getColorMapSize(ih.sBitCount);
    int xorSize = BMPEncoder.getBitmapSize(ih.iWidth, ih.iHeight, ih.sBitCount);
    int andSize = BMPEncoder.getBitmapSize(ih.iWidth, ih.iHeight, 1);
    int size = ih.iSize  + cmapSize + xorSize + andSize;
    ret.iSizeInBytes = size;
    //fileOffset 4
    ret.iFileOffset = 0;
    return ret;
  }
  
  /**
   * Encodes the <em>AND</em> bitmap for the given image according the its alpha channel (transparency) and writes it to the given output.
   * @param img the image to encode as the <em>AND</em> bitmap.
   * @param out the output to which the <em>AND</em> bitmap will be written
   * @throws java.io.IOException if an error occurs.
   */
  public static void writeAndBitmap(BufferedImage img, net.sf.image4j.io.LittleEndianOutputStream out) throws IOException {
    WritableRaster alpha = img.getAlphaRaster();
    
    //indexed transparency (eg. GIF files)
    if (img.getColorModel() instanceof IndexColorModel && img.getColorModel().hasAlpha()) {
      int w = img.getWidth();
      int h = img.getHeight();
      
      int bytesPerLine = BMPEncoder.getBytesPerLine1(w);
      
      byte[] line = new byte[bytesPerLine];
      
      IndexColorModel icm = (IndexColorModel) img.getColorModel();
      Raster raster = img.getRaster();
      
      for (int y = h - 1; y >= 0; y--) {
        
        for (int x = 0; x < w; x++) {
          int bi = x / 8;
          int i = x % 8;
          //int a = alpha.getSample(x, y, 0);
          int p = raster.getSample(x, y, 0);
          int a = icm.getAlpha(p);
          //invert bit since and mask is applied to xor mask
          int b = ~a & 1;
          line[bi] = setBit(line[bi], i, b);
        }
        
        out.write(line);
      }
    }
    //no transparency
    else if (alpha == null) {
      int h = img.getHeight();
      int w = img.getWidth();
      //calculate number of bytes per line, including 32-bit padding
      int bytesPerLine = BMPEncoder.getBytesPerLine1(w);
      
      byte[] line = new byte[bytesPerLine];
      for (int i = 0; i < bytesPerLine; i++) {
        line[i] = (byte) 0;
      }
      
      for (int y = h - 1; y >= 0; y--) {
        out.write(line);
      }
    }
    //transparency (ARGB, etc. eg. PNG)
    else {
      //BMPEncoder.write1(alpha, cmap, out);
      
      int w = img.getWidth();
      int h = img.getHeight();
      
      int bytesPerLine = BMPEncoder.getBytesPerLine1(w);
      
      byte[] line = new byte[bytesPerLine];
      
      for (int y = h - 1; y >= 0; y--) {
        
        for (int x = 0; x < w; x++) {
          int bi = x / 8;
          int i = x % 8;
          int a = alpha.getSample(x, y, 0);
          //invert bit since and mask is applied to xor mask
          int b = ~a & 1;
          line[bi] = setBit(line[bi], i, b);
        }
        
        out.write(line);
      }
      
    }
  }
  
  private static byte setBit(byte bits, int index, int bit) {
    int mask = 1 << (7 - index);
    bits &= ~mask;
    bits |= bit << (7 - index);
    return bits;
  }
  
  private static void writeXorBitmap(BufferedImage img, InfoHeader ih, LittleEndianOutputStream out) throws IOException {
    Raster raster = img.getRaster();
    switch (ih.sBitCount) {
      case 1:
        BMPEncoder.write1(raster, out);
        break;
      case 4:
        BMPEncoder.write4(raster, out);
        break;
      case 8:
        BMPEncoder.write8(raster, out);
        break;
      case 24:
        BMPEncoder.write24(raster, out);
        break;
      case 32:
        Raster alpha = img.getAlphaRaster();
        BMPEncoder.write32(raster, alpha, out);
        break;
    }
  }
  
  /**
   * Utility method, which converts the given image to the specified colour depth.
   * @param img the image to convert.
   * @param bpp the target colour depth (bits per pixel) for the conversion.
   * @return the given image converted to the specified colour depth.
   */
  public static BufferedImage convert(BufferedImage img, int bpp) {
    BufferedImage ret = null;
    switch (bpp) {
      case 1:
        ret = ConvertUtil.convert1(img);
        break;
      case 4:
        ret = ConvertUtil.convert4(img);
        break;
      case 8:
        ret = ConvertUtil.convert8(img);
        break;
      case 24:
        int b = img.getColorModel().getPixelSize();
        if (b == 24 || b == 32) {
          ret = img;
        } else {
          ret = ConvertUtil.convert24(img);
        }
        break;
      case 32:
        int b2 = img.getColorModel().getPixelSize();
        if (b2 == 24 || b2 == 32) {
          ret = img;
        } else {
          ret = ConvertUtil.convert32(img);
        }
        break;
    }
    return ret;
  }
  
  /**
   * @since 0.6
   */ 
  private static javax.imageio.ImageWriter getPNGImageWriter() {
    javax.imageio.ImageWriter ret = null;
    java.util.Iterator<javax.imageio.ImageWriter> itr = javax.imageio.ImageIO.getImageWritersByFormatName("png");
    if (itr.hasNext()) {
      ret = itr.next();
    }
    return ret;
  }
  
  /**
   * @since 0.6
   */
  private static byte[] encodePNG(ImageWriter pngWriter, BufferedImage img) throws IOException {
    java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
    javax.imageio.stream.ImageOutputStream output = javax.imageio.ImageIO.createImageOutputStream(bout);
    pngWriter.setOutput(output);
    pngWriter.write(img);
    bout.flush();
    byte[] ret = bout.toByteArray();
    return ret;
  }
  
}
