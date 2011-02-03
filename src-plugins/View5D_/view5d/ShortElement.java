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


// import java.io.*;

public class ShortElement extends AnElement {
  public short [] myData;        // holds the 3D 16 bit data
  int NumBytes=2;
  int SizeXY;
  
  ShortElement(int SX, int SY, int SZ) {
      super(SX,SY,SZ,256.0);
      myData = new short[Sizes[0]*Sizes[1]*Sizes[2]];
      SizeXY = Sizes[0]*Sizes[1];
      DataType = ShortType; 
      NumBytes = 2; 
  }
  
  void Clear() {
    for (int i = 0; i < Sizes[0]*Sizes[1]*Sizes[2];i++) 
            myData[i] = 0;
    } 

  void DeleteData() {
            myData = null;
  }
  
  int GetStdByteNum() {return 1;}

  void SetValueAt(int x, int y, int z, double val)
  {
      if (val < 0.0) val = 0.0;
      if (val > 255) val = 255;
      myData[x+Sizes[0]*y+SizeXY*z]= (short) val;
  }
  
  int GetIntValueAt(int x, int y, int z)  // scaled to 16 bit integer (for pseudocolor display)
  {
      int val = myData[x+Sizes[0]*y+SizeXY*z];
      // if (val < 0) val += 256;
      return (int)((val - shift) * scaleI);
  }
  int GetByteValueAt(int x, int y, int z)
  {
      int val = myData[x+Sizes[0]*y+SizeXY*z] & 0xff;
      // if (val < 0) val += 256;
      return (int) ((val-shift) * scaleB);
  }

  double GetRawValueAt(int x, int y, int z)
  {
      double val = myData[x+Sizes[0]*y+SizeXY*z];
      // if (val < 0 ) val += 256;
      return val;
  }

  double GetValueAt(int x, int y, int z)
  {
      int val = myData[x+Sizes[0]*y+SizeXY*z];
      // if (val < 0) val += 256;
      return val*ScaleV+OffsetV;
  }
        
  void ConvertSliceFromSimilar(int myslice, int bufslice, Object Ibuffer, int mstep, int moff) {
    // System.out.println("Byte Converting "+SizeXY+"\n");
    short [] mbuffer = (short []) Ibuffer;
    for (int i=0;i<SizeXY;i+=mstep)
        myData[i+Sizes[0]*Sizes[1]*myslice] = mbuffer[bufslice*SizeXY+i+moff]; 
    }
  
  void ConvertSliceFromByte(int myslice, int bufslice, byte [] Ibuffer, int mstep, int moff)
    {
      for (int i=0;i<SizeXY;i+=mstep)
        myData[i+Sizes[0]*Sizes[1]*myslice] = (short) Ibuffer[bufslice*SizeXY+i+moff]; 
    }

  void ConvertSliceFromRGB(int myslice, int bufslice, int [] Ibuffer, int mstep, int moff, int suboff)  // suboff defines which short to use
    {
      int bitshift=suboff*8;
      for (int i=0;i<SizeXY;i+=mstep)
          myData[i+Sizes[0]*Sizes[1]*myslice] = ((short) ((Ibuffer[bufslice*SizeXY+i+moff] >> bitshift) & 0xff)) ; 
    }
  
    void CopySliceToSimilar(int myslice, Object buffer)  
    {
      short [] mbuffer = (short[]) buffer;
      for (int i=0;i<SizeXY;i++)
          mbuffer[i]=myData[i+Sizes[0]*Sizes[1]*myslice]; 
    }
  }
