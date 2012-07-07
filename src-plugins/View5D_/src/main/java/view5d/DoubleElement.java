/****************************************************************************
 *   Copyright (C) 1996-2007 by Rainer Heintzmann                          *
 *   heintzmann@gmail.com                                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************
*/
// By making the appropriate class "View5D" or "View5D_" public and renaming the file, this code can be toggled between Applet and ImageJ respectively
package view5d;


//import java.io.*;

public class DoubleElement extends AnElement {
  public double [] myData;        // holds the 3D byte data
  int SizeXY;
  DoubleElement(int SX, int SY, int SZ, double MaxVal) {
      super(SX,SY,SZ, MaxVal);
      myData = new double[Sizes[0]*Sizes[1]*Sizes[2]];
      SizeXY = Sizes[0]*Sizes[1];
      DataType = DoubleType; 
  }
  
  void Clear() {
    for (int i = 0; i < Sizes[0]*Sizes[1]*Sizes[2];i++) 
            myData[i] = 0;
    } 

  void DeleteData() {
            myData = null;
  }

  int GetStdByteNum() {return 8;}
  
  void SetValueAt(int x, int y, int z, double val)
  {
      myData[x+Sizes[0]*y+SizeXY*z]= (float) val;
  }
  
  int GetIntValueAt(int x, int y, int z)  // scaled to 16 bit
  {
      return (int) ((myData[x+Sizes[0]*y+SizeXY*z]-shift) * scaleI);
  }
  
  int GetByteValueAt(int x, int y, int z)
  {
      return (int) ((myData[x+Sizes[0]*y+SizeXY*z]-shift)*scaleB);
  }

  double GetRawValueAt(int x, int y, int z)
  {
      return (double) myData[x+Sizes[0]*y+SizeXY*z];
  }

  double GetValueAt(int x, int y, int z)
  {
      return myData[x+Sizes[0]*y+SizeXY*z]*ScaleV+OffsetV;
  }
  
 void ConvertSliceFromSimilar(int myslice, int bufslice, Object Ibuffer, int mstep, int moff) {
    // System.out.println("Byte Converting "+SizeXY+"\n");
    float [] mbuffer = (float []) Ibuffer;
    for (int i=0;i<SizeXY;i+=mstep)
        myData[i+Sizes[0]*Sizes[1]*myslice] = mbuffer[bufslice*SizeXY+i+moff]; 
  }
 
void ConvertSliceFromByte(int myslice, int bufslice, byte [] Ibuffer, int mstep, int moff) {
    long ival,val;
    double fval;
    int SliceSize = 8*SizeXY;
    //System.out.println("Double Converting "+myslice+", "+bufslice+", "+SliceSize+", "+ SizeXY +", "+mstep+"\n");
    for (int i=0;i<SizeXY;i+=mstep)
        {
        ival = 0;
        for (int b=1;b<8;b++)
        {
	val=Ibuffer[bufslice*SliceSize+8*(i+moff)+b] & 0xff;   // 4 Bytes !
        // if (val < 0) val += 256;
        ival |= val << (8*b); 
        }
        fval = Double.longBitsToDouble(ival);
        myData[i+SizeXY*myslice] = fval;
        }
  }
  void ConvertSliceFromRGB(int myslice, int bufslice, int [] Ibuffer, int mstep, int moff, int soff)    {throw new IllegalArgumentException("Int: Inapplicable conversion\n");}
  void CopySliceToSimilar(int myslice, Object buffer)  // ImageJ cannot handle proper double images, thus conversion to float
    {
      float [] mbuffer = (float[]) buffer;
      for (int i=0;i<SizeXY;i++)
          mbuffer[i]=(float) myData[i+Sizes[0]*Sizes[1]*myslice]; 
    }
}
