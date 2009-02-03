/*
 * ColorEntry.java
 *
 * Created on 10 May 2006, 08:29
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.image4j.codec.bmp;

import java.io.IOException;

/**
 * Represents an RGB colour entry used in the palette of an indexed image (colour depth <= 8).
 * @author Ian McDonagh
 */
public class ColorEntry {
  
  /**
   * The red component, which should be in the range <tt>0..255</tt>.
   */
  public int bRed;
  /**
   * The green component, which should be in the range <tt>0..255</tt>.
   */
  public int bGreen;
  /**
   * The blue component, which should be in the range <tt>0..255</tt>.
   */
  public int bBlue;
  /**
   * Unused.
   */
  public int bReserved;
  
  /** 
   * Reads and creates a colour entry from the source input.
   * @param in the source input
   * @throws java.io.IOException if an error occurs
   */
  public ColorEntry(net.sf.image4j.io.LittleEndianInputStream in) throws IOException {
    bBlue = in.readUnsignedByte();
    bGreen = in.readUnsignedByte();
    bRed = in.readUnsignedByte();
    bReserved = in.readUnsignedByte();
  }
  
  /**
   * Creates a colour entry with colour components initialized to <tt>0</tt>.
   */
  public ColorEntry() {
    bBlue = 0;
    bGreen = 0;
    bRed = 0;
    bReserved = 0;
  }
  
  /**
   * Creates a colour entry with the specified colour components.
   * @param r red component
   * @param g green component
   * @param b blue component
   * @param a unused
   */
  public ColorEntry(int r, int g, int b, int a) {
    bBlue = b;
    bGreen = g;
    bRed = r;
    bReserved = a;
  }

  
  
}
