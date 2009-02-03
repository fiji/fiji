/*
 * ConvertUtil.java
 *
 * Created on 12 May 2006, 09:22
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.image4j.util;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

/**
 * Provides useful methods for converting images from one colour depth to another.
 * @author Ian McDonagh
 */
public class ConvertUtil {
  
  /**
   * Converts the source to 1-bit colour depth (monochrome).
   * No transparency.
   * @param src the source image to convert
   * @return a copy of the source image with a 1-bit colour depth.
   */
  public static BufferedImage convert1(BufferedImage src) {
    IndexColorModel icm = new IndexColorModel(
        1, 2, new byte[] { (byte) 0, (byte) 0xFF },
        new byte[] { (byte) 0, (byte) 0xFF },
        new byte[] { (byte) 0, (byte) 0xFF }
    );
    
    BufferedImage dest = new BufferedImage(
        src.getWidth(), src.getHeight(),
        BufferedImage.TYPE_BYTE_BINARY,
        icm
        );
    
    ColorConvertOp cco = new ColorConvertOp(
        src.getColorModel().getColorSpace(),
        dest.getColorModel().getColorSpace(),
        null
        );
    
    cco.filter(src, dest);
    
    return dest;
  }
  
  /**
   * Converts the source image to 4-bit colour
   * using the default 16-colour palette:
   * <ul>
   *  <li>black</li><li>dark red</li><li>dark green</li>
   *  <li>dark yellow</li><li>dark blue</li><li>dark magenta</li>
   *  <li>dark cyan</li><li>dark grey</li><li>light grey</li>
   *  <li>red</li><li>green</li><li>yellow</li><li>blue</li>
   *  <li>magenta</li><li>cyan</li><li>white</li>
   * </ul>
   * No transparency.
   * @param src the source image to convert
   * @return a copy of the source image with a 4-bit colour depth, with the default colour pallette
   */
  public static BufferedImage convert4(BufferedImage src) {
    int[] cmap = new int[] {
      0x000000, 0x800000, 0x008000, 0x808000,
      0x000080, 0x800080, 0x008080, 0x808080,
      0xC0C0C0, 0xFF0000, 0x00FF00, 0xFFFF00,
      0x0000FF, 0xFF00FF, 0x00FFFF, 0xFFFFFF
    };
    return convert4(src, cmap);
  }
    
  /**
   * Converts the source image to 4-bit colour
   * using the given colour map.  No transparency.
   * @param src the source image to convert
   * @param cmap the colour map, which should contain no more than 16 entries
   * The entries are in the form RRGGBB (hex).
   * @return a copy of the source image with a 4-bit colour depth, with the custom colour pallette
   */
  public static BufferedImage convert4(BufferedImage src, int[] cmap) {
    IndexColorModel icm = new IndexColorModel(
        4, cmap.length, cmap, 0, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE
        );
    BufferedImage dest = new BufferedImage(
        src.getWidth(), src.getHeight(),
        BufferedImage.TYPE_BYTE_BINARY,
        icm
        );
    ColorConvertOp cco = new ColorConvertOp(
        src.getColorModel().getColorSpace(),
        dest.getColorModel().getColorSpace(),
        null
        );
    cco.filter(src, dest);
    
    return dest;
  }
  
  /**
   * Converts the source image to 8-bit colour
   * using the default 256-colour palette. No transparency.
   * @param src the source image to convert
   * @return a copy of the source image with an 8-bit colour depth
   */
  public static BufferedImage convert8(BufferedImage src) {
    BufferedImage dest = new BufferedImage(
        src.getWidth(), src.getHeight(),
        BufferedImage.TYPE_BYTE_INDEXED
        );
    ColorConvertOp cco = new ColorConvertOp(
        src.getColorModel().getColorSpace(),
        dest.getColorModel().getColorSpace(),
        null
        );
    cco.filter(src, dest);
    return dest;
  }
  
  /**
   * Converts the source image to 24-bit colour (RGB). No transparency.
   * @param src the source image to convert
   * @return a copy of the source image with a 24-bit colour depth
   */
  public static BufferedImage convert24(BufferedImage src) {
    BufferedImage dest = new BufferedImage(
        src.getWidth(), src.getHeight(),
        BufferedImage.TYPE_INT_RGB
        );
    ColorConvertOp cco = new ColorConvertOp(
        src.getColorModel().getColorSpace(),
        dest.getColorModel().getColorSpace(),
        null
        );
    cco.filter(src, dest);
    return dest;
  }
  
  /**
   * Converts the source image to 32-bit colour with transparency (ARGB).
   * @param src the source image to convert
   * @return a copy of the source image with a 32-bit colour depth.
   */
  public static BufferedImage convert32(BufferedImage src) {
    BufferedImage dest = new BufferedImage(
        src.getWidth(), src.getHeight(),
        BufferedImage.TYPE_INT_ARGB
        );
    ColorConvertOp cco = new ColorConvertOp(
        src.getColorModel().getColorSpace(),
        dest.getColorModel().getColorSpace(),
        null
        );
    cco.filter(src, dest);
    return dest;
  }    
}
