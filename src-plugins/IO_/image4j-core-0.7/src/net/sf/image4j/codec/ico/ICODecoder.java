/*
 * ICODecoder.java
 *
 * Created on May 9, 2006, 9:31 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.image4j.codec.ico;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import net.sf.image4j.codec.bmp.BMPConstants;
import net.sf.image4j.codec.bmp.BMPDecoder;
import net.sf.image4j.codec.bmp.ColorEntry;
import net.sf.image4j.codec.bmp.InfoHeader;

/**
 * Decodes images in ICO format.
 * @author Ian McDonagh
 */
public class ICODecoder {
  
  private static final int PNG_MAGIC = 0x89504E47;
  private static final int PNG_MAGIC_LE = 0x474E5089;
  private static final int PNG_MAGIC2 = 0x0D0A1A0A;
  private static final int PNG_MAGIC2_LE = 0x0A1A0A0D;
  
  //private java.util.List<BufferedImage> img;
  
  private ICODecoder() { }
  
  /**
   * Reads and decodes the given ICO file.  Convenience method equivalent to {@link #read(java.io.InputStream) read(new java.io.FileInputStream(file))}.
   * @param file the source file to read
   * @return the list of images decoded from the ICO data
   * @throws java.io.IOException if an error occurs
   */
  public static java.util.List<BufferedImage> read(java.io.File file) throws IOException {
    return read(new java.io.FileInputStream(file));
  }
  
  /**
   * Reads and decodes the given ICO file, together with all metadata.  
   * Convenience method equivalent to {@link #readExt(java.io.InputStream) readExt(new java.io.FileInputStream(file))}.
   * @param file the source file to read
   * @return the list of images decoded from the ICO data
   * @throws java.io.IOException if an error occurs
   * @since 0.7
   */
  public static java.util.List<ICOImage> readExt(java.io.File file) throws IOException {
    return readExt(new java.io.FileInputStream(file));
  }
  
  /**
   * Reads and decodes ICO data from the given source.
   * The returned list of images is in the order in which they appear in the source ICO data.
   * @param is the source <tt>InputStream</tt> to read
   * @return the list of images decoded from the ICO data
   * @throws java.io.IOException if an error occurs
   */
  public static java.util.List<BufferedImage> read(java.io.InputStream is) throws IOException {
    java.util.List<ICOImage> list = readExt(is);
    java.util.List<BufferedImage> ret = new java.util.ArrayList<BufferedImage>(list.size());
    for (int i = 0; i < list.size(); i++) {
      ICOImage icoImage = list.get(i);
      BufferedImage image = icoImage.getImage();
      ret.add(image);
    }
    return ret;
  }
  
  /**
   * Reads and decodes ICO data from the given source, together with all metadata.
   * The returned list of images is in the order in which they appear in the source ICO data.
   * @param is the source <tt>InputStream</tt> to read
   * @return the list of images decoded from the ICO data
   * @throws java.io.IOException if an error occurs
   * @since 0.7
   */
  public static java.util.List<ICOImage> readExt(java.io.InputStream is) throws IOException {
    //long t = System.currentTimeMillis();
    
    net.sf.image4j.io.LittleEndianInputStream in = new net.sf.image4j.io.LittleEndianInputStream(is);
    
    //Reserved 	2 byte 	=0
    short sReserved = in.readShortLE();
    //Type 	2 byte 	=1
    short sType = in.readShortLE();
    //Count 	2 byte 	Number of Icons in this file
    short sCount = in.readShortLE();
    
    //Entries   Count * 16  list of icons
    IconEntry[] entries = new IconEntry[sCount];
    for (short s = 0; s < sCount; s++) {
      entries[s] = new IconEntry(in);
    }
    
    int i = 0;
    //images    list of bitmap structures in BMP format
    java.util.List<ICOImage> ret = new java.util.ArrayList<ICOImage>(sCount);
    
    try {
      for (i = 0; i < sCount; i++) {
       
        int info = in.readIntLE();
        if (info == 40) {
          
          //read XOR bitmap
          //BMPDecoder bmp = new BMPDecoder(is);
          InfoHeader infoHeader = BMPDecoder.readInfoHeader(in, info);
          InfoHeader andHeader = new InfoHeader(infoHeader);
          andHeader.iHeight = (int) (infoHeader.iHeight / 2);
          InfoHeader xorHeader = new InfoHeader(infoHeader);
          xorHeader.iHeight = andHeader.iHeight;
          
          andHeader.sBitCount = 1;
          andHeader.iNumColors = 2;
          
          //for now, just read all the raster data (xor + and)
          // and store as separate images
          
          BufferedImage xor = BMPDecoder.read(xorHeader, in);
          //img.add(xor);
          
          BufferedImage img = new BufferedImage(
              xorHeader.iWidth, xorHeader.iHeight,
              BufferedImage.TYPE_INT_ARGB
              );
                   
          ColorEntry[] andColorTable = new ColorEntry[] {
            new ColorEntry(255, 255, 255, 255),
            new ColorEntry(0, 0, 0, 0)
          };
          
          if (infoHeader.sBitCount == 32) {
            //transparency from alpha
            //ignore bytes after XOR bitmap
            int size = entries[i].iSizeInBytes;
            int infoHeaderSize = infoHeader.iSize;
            // data size = w * h * 4
            int dataSize = xorHeader.iWidth * xorHeader.iHeight * 4;
            int skip = size - infoHeaderSize - dataSize;
            
            //ignore AND bitmap since alpha channel stores transparency
            int skipped = in.skipBytes(skip);
            int s = skip;
            while (skipped < s) {
              if (skipped < 0) {
                throw new IOException("Failed to read [skip]");
              }
              s = skip - skipped;
              skipped = in.skipBytes(s);
            }
            ////read AND bitmap
            //BufferedImage and = BMPDecoder.read(andHeader, in, andColorTable);
            //this.img.add(and);
            
            WritableRaster srgb = xor.getRaster();
            WritableRaster salpha = xor.getAlphaRaster();
            WritableRaster rgb = img.getRaster();
            WritableRaster alpha = img.getAlphaRaster();
            
            for (int y = xorHeader.iHeight - 1; y >= 0; y--) {
              for (int x = 0; x < xorHeader.iWidth; x++) {
                int r = srgb.getSample(x, y, 0);
                int g = srgb.getSample(x, y, 1);
                int b = srgb.getSample(x, y, 2);
                int a = salpha.getSample(x, y, 0);
                rgb.setSample(x, y, 0, r);
                rgb.setSample(x, y, 1, g);
                rgb.setSample(x, y, 2, b);
                alpha.setSample(x, y, 0, a);
              }
            }
            
          } else {
            BufferedImage and = BMPDecoder.read(andHeader, in, andColorTable);
            //img.add(and);
            
            //copy rgb
            WritableRaster srgb = xor.getRaster();
            WritableRaster rgb = img.getRaster();
            //copy alpha
            WritableRaster alpha = img.getAlphaRaster();
            WritableRaster salpha = and.getRaster();
            
            for (int y = 0; y < xorHeader.iHeight; y++) {
              for (int x = 0; x < xorHeader.iWidth; x++) {
                int r, g, b;
                int c = xor.getRGB(x, y);
                r = (c >> 16) & 0xFF;
                g = (c >> 8) & 0xFF;
                b = (c) & 0xFF;
                //red
                rgb.setSample(x, y, 0, r);
                //green
                rgb.setSample(x, y, 1, g);
                //blue
                rgb.setSample(x, y, 2, b);
                //System.out.println(x+","+y+"="+Integer.toHexString(c));
                //img.setRGB(x, y, c);
                
                //alpha
                int a = and.getRGB(x, y);
                alpha.setSample(x, y, 0, a);
              }
            }
          }
          // create ICOImage
          IconEntry iconEntry = entries[i];
          ICOImage icoImage = new ICOImage(img, infoHeader, iconEntry);
          icoImage.setPngCompressed(false);
          icoImage.setIconIndex(i);
          ret.add(icoImage);
        }
        //check for PNG magic header and that image height and width = 0 = 256 -> Vista format
        else if (info == PNG_MAGIC_LE) {
          
          int info2 = in.readIntLE();
          
          if (info2 != PNG_MAGIC2_LE) {
            throw new IOException("Unrecognized icon format for image #"+i);
          }
          
          IconEntry e = entries[i];
          byte[] pngData = new byte[e.iSizeInBytes - 8];
          int count = in.read(pngData);
          if (count != pngData.length) {
            throw new IOException("Unable to read image #"+i+" - incomplete PNG compressed data");
          }
          java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
          java.io.DataOutputStream dout = new java.io.DataOutputStream(bout);
          dout.writeInt(PNG_MAGIC);
          dout.writeInt(PNG_MAGIC2);
          dout.write(pngData);
          byte[] pngData2 = bout.toByteArray();
          java.io.ByteArrayInputStream bin = new java.io.ByteArrayInputStream(pngData2);
          javax.imageio.stream.ImageInputStream input = javax.imageio.ImageIO.createImageInputStream(bin);
          javax.imageio.ImageReader reader = getPNGImageReader();
          reader.setInput(input);
          java.awt.image.BufferedImage img = reader.read(0);
                    
          // create ICOImage          
          IconEntry iconEntry = entries[i];
          ICOImage icoImage = new ICOImage(img, null, iconEntry);
          icoImage.setPngCompressed(true);
          icoImage.setIconIndex(i);
          ret.add(icoImage);
        }
        else {
          throw new IOException("Unrecognized icon format for image #"+i);
        }
        
        
        
      /*
      InfoHeader andInfoHeader = new InfoHeader();
      andInfoHeader.iColorsImportant = 0;
      andInfoHeader.iColorsUsed = 0;
      andInfoHeader.iCompression = BMPConstants.BI_RGB;
      andInfoHeader.iHeight = xorInfoHeader.iHeight / 2;
      andInfoHeader.iWidth = xorInfoHeader.
       */
      }
    } catch (IOException ex) {
      throw new IOException("Failed to read image # "+i);
    }
    
    //long t2 = System.currentTimeMillis();
    //System.out.println("Loaded ICO file in "+(t2 - t)+"ms");
    
    return ret;
  }
  
  private static javax.imageio.ImageReader getPNGImageReader() {
    javax.imageio.ImageReader ret = null;
    java.util.Iterator<javax.imageio.ImageReader> itr = javax.imageio.ImageIO.getImageReadersByFormatName("png");
    if (itr.hasNext()) {
      ret = itr.next();
    }
    return ret;
  }
}
