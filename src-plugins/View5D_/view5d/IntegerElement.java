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


public class IntegerElement extends AnElement {
  public int [] myData;        // holds the 3D integer data
  int SizeXY;
  int NumBytes;
  IntegerElement(int SX, int SY, int SZ, int Bts, double MaxVal) {
      super(SX,SY,SZ, MaxVal);
      NumBytes = Bts;
      myData = new int[Sizes[0]*Sizes[1]*Sizes[2]];
      SizeXY = Sizes[0]*Sizes[1];
      DataType = IntegerType; 
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
      myData[x+Sizes[0]*y+SizeXY*z]= (int) val;
  }
  
  int GetIntValueAt(int x, int y, int z)  // scaled to 16 bit
  {
      return (int) ((myData[x+Sizes[0]*y+SizeXY*z]-shift) * scaleI);
  }
  
  int GetByteValueAt(int x, int y, int z)
  {
      return (int) ((myData[x+Sizes[0]*y+SizeXY*z]-shift) * scaleB);
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
    short [] mbuffer = (short []) Ibuffer;
    for (int i=0;i<SizeXY;i+=mstep)
        myData[i+Sizes[0]*Sizes[1]*myslice] = (int) (mbuffer[bufslice*SizeXY+i+moff] & 0xffff);   // usigned
  }
 
  void ConvertSliceFromByte(int myslice, int bufslice, byte [] Ibuffer, int mstep, int moff) {
    int val;
    int SliceSize = NumBytes*SizeXY;
    // double fval;
    // System.out.println("Integer Converting "+SliceSize+", "+ SizeXY+"\n");
    
    for (int i=0;i<SizeXY;i+=mstep)
    {
        val=Ibuffer[bufslice*SliceSize+NumBytes*(i+moff)] & 0xff; 
        // if (val < 0) val += 256;
        myData[i+SizeXY*myslice] = val; 

        for (int b=1;b<NumBytes;b++)
            {
            val=Ibuffer[bufslice*SliceSize+NumBytes*(i+moff)+b] & 0xff; 
            // if (val < 0) val += 256;
            myData[i+SizeXY*myslice] |= val << (8*b); 
            // System.out.print(" "+val);
            }
        // fval = myData[i+SizeXY*myslice];
    }
    // System.out.print(" "+Ielementdata[elem][i+SizeX*SizeY*slice]+ ", ");
    // if ((i%SizeX)==SizeX-1) System.out.println();
  }
  void ConvertSliceFromRGB(int myslice, int bufslice, int [] mbuffer, int mstep, int moff, int soff)
    {
      for (int i=0;i<SizeXY;i+=mstep)
        myData[i+Sizes[0]*Sizes[1]*myslice] = mbuffer[bufslice*SizeXY+i+moff]; 
    }

  void CopySliceToSimilar(int myslice, Object buffer)  
    {
      short [] mbuffer = (short[]) buffer;
      for (int i=0;i<SizeXY;i++)
          mbuffer[i]=(short) myData[i+Sizes[0]*Sizes[1]*myslice]; 
    }

  void CopyIntVal(AnElement other, double Min, double Max) {
    for (int z = 0; z < Sizes[2]; z++) 
            for (int y = 0; y < Sizes[1]; y++) 
                for (int x = 0; x < Sizes[0]; x++) 
                {
                    double val = other.GetValueAt(x,y,z);
                    val = (val-Min) /(Max-Min);
                    if (val < 0) val=0;
                    if (val > 1.0) val = 1.0;
                    SetValueAt(x,y,z,(Bundle.MaxCTable-1)*val);  // will convert the type and include all thresholds , ...
                }
  }

}
