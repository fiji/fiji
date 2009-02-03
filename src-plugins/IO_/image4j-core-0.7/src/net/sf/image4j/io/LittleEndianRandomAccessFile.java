/*
 * LittleEndianRandomAccessFile.java
 *
 * Created on 07 November 2006, 03:04
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.image4j.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Provides endian conversions for input and output with a <tt>RandomAccessFile</tt>.
 *
 * This class is currently not in use and has not been tested.
 *
 * @author Ian McDonagh
 */
public class LittleEndianRandomAccessFile extends RandomAccessFile {
  
  public LittleEndianRandomAccessFile(java.io.File file, String mode) throws FileNotFoundException {
    super(file, mode);
  }
  
  public LittleEndianRandomAccessFile(String name, String mode) throws FileNotFoundException {
    super(name, mode);
  }
  
  public short readShortLE() throws IOException {
    short ret = super.readShort();
    ret = EndianUtils.swapShort(ret);
    return ret;
  }
  
  public int readIntLE() throws IOException {
    int ret = super.readInt();
    ret = EndianUtils.swapInteger(ret);
    return ret;
  }
  
  public float readFloatLE() throws IOException {
    float ret = super.readFloat();
    ret = EndianUtils.swapFloat(ret);
    return ret;
  }
  
  public long readLongLE() throws IOException {
    long ret = super.readLong();
    ret = EndianUtils.swapLong(ret);
    return ret;
  }
  
  public double readDoubleLE() throws IOException {
    double ret = super.readDouble();
    ret = EndianUtils.swapDouble(ret);
    return ret;
  }
  
  public void writeShortLE(short value) throws IOException {
    value = EndianUtils.swapShort(value);
    super.writeShort(value);
  }
  
  public void writeIntLE(int value) throws IOException {
    value = EndianUtils.swapInteger(value);
    super.writeInt(value);
  }
  
  public void writeFloatLE(float value) throws IOException {
    value = EndianUtils.swapFloat(value);
    super.writeFloat(value);
  }
  
  public void writeLongLE(long value) throws IOException {
    value = EndianUtils.swapLong(value);
    super.writeLong(value);
  }
  
  public void writeDoubleLE(double value) throws IOException {
    value = EndianUtils.swapDouble(value);
    super.writeDouble(value);
  }
  
}
