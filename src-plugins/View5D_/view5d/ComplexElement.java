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

public class ComplexElement extends AnElement {
  public float [] myData;        // holds the 3D data in pairs of real numbers
  int SizeXY;
  int readmode;   // 0 : magnitude, 1: phase angle, 2: real part, 3: imaginary part
  ComplexElement(int SX, int SY, int SZ, double MaxVal) {
      super(SX,SY,SZ, MaxVal);
      myData = new float[2*Sizes[0]*Sizes[1]*Sizes[2]];
      SizeXY = Sizes[0]*Sizes[1];
      DataType = ComplexType; 
      readmode = 0;
  }
  
  void AdvanceReadMode() {
      readmode = (readmode+1)%4;
  }
  
  void SetReadMode(int rmode) {
      readmode = (rmode)%4;
  }
  
  void Clear() {
    for (int i = 0; i < 2*Sizes[0]*Sizes[1]*Sizes[2];i++) 
            myData[i] = 0;
    } 

  void DeleteData() {
            myData = null;
  }

  int GetStdByteNum() {return 4;}
  
  void SetValueAt(int x, int y, int z, double val)   // Sets only the real value
  {
      myData[2*(x+Sizes[0]*y+SizeXY*z)]= (float) val;
  }
  
  void SetValueAt(int x, int y, int z, float rval, float ival)
  {
      myData[2*(x+Sizes[0]*y+SizeXY*z)]= rval;
      myData[2*(x+Sizes[0]*y+SizeXY*z)+1]= ival;
  }

    
  int GetIntValueAt(int x, int y, int z)  // scaled to 16 bit
  {
      return (int) ((GetRawValueAt(x,y,z)-shift) * scaleI);
  }
  
  int GetByteValueAt(int x, int y, int z)
  {
      return (int) ((GetRawValueAt(x,y,z)-shift)*scaleB);
  }

  double GetRawValueAt(int x, int y, int z)
  {
  float re=myData[2*(x+Sizes[0]*y+SizeXY*z)];
  float im=myData[2*(x+Sizes[0]*y+SizeXY*z)+1];
  switch(readmode)
  {
  case 0:
      return (double) Math.sqrt(re*re+im*im);
  case 1:
      return (double) Math.atan2(re,im)*180.0/Math.PI;
  case 2:
      return (double) re;
  default:
      return (double) im;
   }
  }

  double GetValueAt(int x, int y, int z)
  {
      return GetRawValueAt(x,y,z)*ScaleV+OffsetV;
  }
    
  String GetValueStringAt(int x, int y, int z)   // Prints the value onto the screen. Can be overwritten
     {
  	float re=myData[2*(x+Sizes[0]*y+SizeXY*z)];
  	float im=myData[2*(x+Sizes[0]*y+SizeXY*z)+1];
      return nf.format(GetValueAt(x,y,z))+" ("+nf.format(re)+", "+nf.format(im)+"i)";
     }
  
 void ConvertSliceFromSimilar(int myslice, int bufslice, Object Ibuffer, int mstep, int moff) {   // set only the real value
    // System.out.println("Byte Converting "+SizeXY+"\n");
    float [] mbuffer = (float []) Ibuffer;
    for (int i=0;i<SizeXY;i+=mstep)
        myData[2*(i+Sizes[0]*Sizes[1]*myslice)] = mbuffer[bufslice*SizeXY+i+moff]; 
  }
 
void ConvertSliceFromByte(int myslice, int bufslice, byte [] Ibuffer, int mstep, int moff) {   // set only the real value
    int ival,val;
    float frval=0,fival=0;
    int SliceSize = 4*SizeXY;
    //System.out.println("Complex Converting "+myslice+", "+bufslice+", "+SliceSize+", "+ SizeXY +", "+mstep+"\n");
    
    for (int i=0;i<SizeXY;i+=mstep)
        {
        ival = 0;
        for (int b=1;b<4;b++)
        {
	val=Ibuffer[2*(bufslice*SliceSize+4*(i+moff))+b] & 0xff;   // 4 Bytes !
        ival |= val << (8*b); 
        }
        frval = Float.intBitsToFloat(ival);
        myData[2*(i+SizeXY*myslice)] = frval;
        ival=0;
        for (int b=1;b<4;b++)
        {
	val=Ibuffer[2*(bufslice*SliceSize+4*(i+moff))+4+b] & 0xff;   // 4 Bytes !
        ival |= val << (8*b); 
        }
	fival = Float.intBitsToFloat(ival);
        myData[2*(i+SizeXY*myslice)+1] = fival;
        }
    //System.out.println("Complex Converted "+nf.format(frval)+", "+nf.format(fival) +"\n");
  }
  
  void ConvertSliceFromRGB(int myslice, int bufslice, int [] Ibuffer, int mstep, int moff, int soff)    {throw new IllegalArgumentException("Int: Inapplicable conversion\n");}
  void CopySliceToSimilar(int myslice, Object buffer)  // ImageJ cannot handle proper complex images, thus conversion to float
    {
      float [] mbuffer = (float[]) buffer;
      int i=0;
      for (int z=0;z<Sizes[2];z++)
      for (int y=0;y<Sizes[1];y++)
	for (int x=0;x<Sizes[0];x++)
	{
          mbuffer[i]=(float) GetRawValueAt(x,y,z); 
	  i=i+1;
	}
    }
}
