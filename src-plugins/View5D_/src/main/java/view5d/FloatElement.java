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

public class FloatElement extends AnElement {
  public float [] myData;        // holds the 3D byte data
  int SizeXY;
  
  FloatElement(int SX, int SY, int SZ, float MaxVal) {
      super(SX,SY,SZ, MaxVal);
      myData = new float[Sizes[0]*Sizes[1]*Sizes[2]];
      SizeXY = Sizes[0]*Sizes[1];
      DataType = FloatType; 
  }
  
  void Clear() {
    for (int i = 0; i < Sizes[0]*Sizes[1]*Sizes[2];i++) 
            myData[i] = 0;
    } 

  void DeleteData() {
            myData = null;
  }

  int GetStdByteNum() {return 4;}
  
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
    int ival,val;
    float fval;
    int SliceSize = 4*SizeXY;
    // System.out.println("Float Converting "+SliceSize+", "+ SizeXY +"\n");
    for (int i=0;i<SizeXY;i+=mstep)
        {
        ival = 0;
        for (int b=1;b<4;b++)
        {
	val=Ibuffer[bufslice*SliceSize+4*(i+moff)+b] & 0xff;   // 4 Bytes !
        // if (val < 0) val += 256;
        ival |= val << (8*b); 
        }
        fval = Float.intBitsToFloat(ival);
        myData[i+SizeXY*myslice] = fval;
        }
  }
  void ConvertSliceFromRGB(int myslice, int bufslice, int [] Ibuffer, int mstep, int moff, int soff)    {throw new IllegalArgumentException("Int: Inapplicable conversion\n");}
  void CopySliceToSimilar(int myslice, Object buffer)  
    {
      float [] mbuffer = (float[]) buffer;
      for (int i=0;i<SizeXY;i++)
          mbuffer[i]=myData[i+Sizes[0]*Sizes[1]*myslice]; 
    }
}
