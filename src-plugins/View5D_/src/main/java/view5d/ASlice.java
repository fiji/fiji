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

// import java.io.*;
package view5d;

import java.awt.image.*;
import java.awt.*;
import ij.*;
import ij.gui.*;

class ASlice extends Object {
    int [] mySlice;     // int-slices for image display (can also be used for color display
    double [] my1DProj = null;     // if needed, 1D sums values will be stored in a projection
    public double[] DisplayOffset;   // display offsets of canvasses, stored here to allow independence of times
    int myDim;          // marks the dimension this slice is for
    int AllSize;        // in local coordinate system
    int SliceSizeX;     // needed for image generation
    int SliceSizeY;
    boolean isValid;
    int previousSlice;
    double ROISum=Double.NaN;
    double ROIAvg=Double.NaN;
    double ROIMax=Double.NaN;
    double ROIMin=Double.NaN;
    double ROIVoxels=Double.NaN;
    boolean MIPMode=true;
    ColorModel MyColorModel;
    int [] my1DProjVoxels = null;     // if needed, 1D sums values will be stored in a projection
    double Proj1DScale = 1.0;
    double Proj1DOffset = 0.0;
    	
    ASlice(int mDim, AnElement myData)
    {
        myDim = mDim;
        isValid = false;
        previousSlice = -1;
        DisplayOffset = myData.DisplayOffset;
        
        switch (myDim) {
            case 0:
                SliceSizeX = myData.Sizes[2];
                SliceSizeY = myData.Sizes[1];
                AllSize = SliceSizeX*SliceSizeY;
                mySlice = new int [AllSize];
                break;
            case 1:
                SliceSizeX = myData.Sizes[0];
                SliceSizeY = myData.Sizes[2];
                AllSize = SliceSizeX*SliceSizeY;
                mySlice = new int [AllSize];
                break;
            case 2:
                SliceSizeX = myData.Sizes[0];
                SliceSizeY = myData.Sizes[1];
                AllSize = SliceSizeX*SliceSizeY;
                mySlice = new int [AllSize];
                break;
        }
        AllSize = SliceSizeX*SliceSizeY;
    }

    void TakeModel(ColorModel newModel)
    {
        MyColorModel = newModel;
    }
    
    void Invalidate() {
        isValid = false;
        ROISum=Double.NaN;
        ROIAvg=Double.NaN;
        ROIMax=Double.NaN;
        ROIMin=Double.NaN;
        ROIVoxels=Double.NaN;
    }
    
    void setMIPMode(boolean mipmode) {
        if (mipmode != MIPMode)
            Invalidate();
        MIPMode = mipmode;
    }
    
    Image GenImage(Container applet) {
        MemoryImageSource ms;
          try {
	        ms = new MemoryImageSource(SliceSizeX,SliceSizeY,MyColorModel, mySlice, 0, SliceSizeX);
          } catch(Exception e)
	      {
                  System.out.println("Caught Image generation Exception:"+e+"\n");
		  e.printStackTrace();
		  ms = null;
	      }
    return applet.createImage(ms);
    }

    Image GenColorImage(Container applet) {
        MemoryImageSource ms;
          try {
	        ms = new MemoryImageSource(SliceSizeX,SliceSizeY, mySlice, 0, SliceSizeX);
          } catch(Exception e)
	      {
                  System.out.println("Caught Image generation Exception:"+e+"\n");
		  e.printStackTrace();
		  ms = null;
	      }
    return applet.createImage(ms);
    }

    void UpdateSlice(int sliceNr, AnElement myData, AnElement GateElement) {
        if (previousSlice != sliceNr)
            isValid = false;
        if (isValid)
            {// System.out.println("valid\n");
            return;}
        // System.out.println("invalid\n");
        int val;
        try {
        switch (myDim) {
            case 0:
                for (int z = 0; z < myData.Sizes[2]; z++) 
                    for (int y = 0; y < myData.Sizes[1]; y++) 
                    {
                        val = myData.GetIntValueAt(sliceNr,y,z);  // with cast to unsigned
                        if (val < 0) val = 0; if (val > Bundle.MaxCTable-1) val = Bundle.MaxCTable-1;
                        if (GateElement.GetIntValueAt(sliceNr,y,z) <= 0)
                            val=0;
                        mySlice[z+myData.Sizes[2]*y] = val;
                    }
                break;
            case 1:
                for (int z = 0; z < myData.Sizes[2]; z++) 
                    for (int x = 0; x < myData.Sizes[0]; x++) 
                    {
                        val = myData.GetIntValueAt(x,sliceNr,z);
                        if (val < 0) val = 0; if (val > Bundle.MaxCTable-1) val = Bundle.MaxCTable-1;
                        if (GateElement.GetIntValueAt(x,sliceNr,z) <= 0)
                            val=0;
                        mySlice[x+myData.Sizes[0]*z] =  val;
                    }
                break;
            case 2:
            for (int y = 0; y < myData.Sizes[1]; y++) 
                    for (int x = 0; x < myData.Sizes[0]; x++) 
                    {
                        val = myData.GetIntValueAt(x,y,sliceNr);
                        if (val < 0) val = 0; if (val > Bundle.MaxCTable-1) val = Bundle.MaxCTable-1;
                        if (GateElement.GetIntValueAt(x,y,sliceNr) <= 0)
                            val=0;
                        mySlice[x+myData.Sizes[0]*y] =  val;
                    }
                break;
        }
    } catch(Exception e)
	      {
                System.out.println("Caught Slice-Copy Exception:"+e+"\n");
		e.printStackTrace();
	      }
    isValid = true;
    previousSlice = sliceNr;
    return;
  } 

  
    // The function below adds an element to diplay into the color display slice
  void SumToColorSlice(int sliceNr, AnElement myData, byte [] CMapR, byte [] CMapG, byte [] CMapB, AnElement GateElement, AnElement ActiveElement) {
        int val;
	int Red,Green,Blue,col;
	int OffsetX=-(int) (myData.DisplayOffset[0]- ActiveElement.DisplayOffset[0]); // subtract the offsets of the current element here
	int OffsetY=-(int) (myData.DisplayOffset[1]- ActiveElement.DisplayOffset[1]);
	int OffsetZ=-(int) (myData.DisplayOffset[2]- ActiveElement.DisplayOffset[2]);
	int GOffsetX=-(int) (GateElement.DisplayOffset[0]- ActiveElement.DisplayOffset[0]); // subtract the offsets of the current element here
	int GOffsetY=-(int) (GateElement.DisplayOffset[1]- ActiveElement.DisplayOffset[1]);
	int GOffsetZ=-(int) (GateElement.DisplayOffset[2]- ActiveElement.DisplayOffset[2]);
	
        try {
        switch (myDim) {
            case 0:
                for (int z = 0; z < myData.Sizes[2]; z++) 
                    for (int y = 0; y < myData.Sizes[1]; y++) 
                    {
                    	if (myData.WithinBounds(sliceNr+OffsetX,y+OffsetY,z+OffsetZ))
                    		{val = myData.GetIntValueAt(sliceNr+OffsetX,y+OffsetY,z+OffsetZ);
                        	if (val < 0) val = 0; if (val > Bundle.MaxCTable-1) val = Bundle.MaxCTable-1;
                        	if (GateElement.WithinBounds(sliceNr+GOffsetX,y+GOffsetY,z+GOffsetZ))
                            {if (GateElement.GetIntValueAt(sliceNr+GOffsetX,y+GOffsetY,z+GOffsetZ) <= 0)
                                val=0;}
                        	else
                        		val=0;
                    		}
                    	else
                    		val=0;
			col = mySlice[z+myData.Sizes[2]*y];
			Red = CMapR[val] & 0xff; // if (Red < 0) Red += 256;
			Green = CMapG[val] & 0xff; // if (Green < 0) Green += 256;
			Blue = CMapB[val] & 0xff; // if (Blue < 0) Blue += 256;
			Red = (Red << 16) + (col & (255 << 16)); 
			if (Red > (255 << 16)) Red = (255 << 16);
			Green = (Green << 8) + (col & (255 << 8)); 
			if (Green > (255 << 8)) Green = (255 << 8);
			Blue = Blue + (col & 0xff); 
			if (Blue > 255) Blue =  255;
                        mySlice[z+myData.Sizes[2]*y] = (255 << 24) | Red | Green | Blue;
                    }
                break;
            case 1:
                for (int z = 0; z < myData.Sizes[2]; z++) 
                    for (int x = 0; x < myData.Sizes[0]; x++) 
                    {
                    	if (myData.WithinBounds(x+OffsetX,sliceNr+OffsetY,z+OffsetZ))
                    		{val = myData.GetIntValueAt(x+OffsetX,sliceNr+OffsetY,z+OffsetZ);
                    		if (val < 0) val = 0; if (val > Bundle.MaxCTable-1) val = Bundle.MaxCTable-1;
                        	if (GateElement.WithinBounds(x+GOffsetX,sliceNr+GOffsetY,z+GOffsetZ))
                    		{if (GateElement.GetIntValueAt(x+GOffsetX,sliceNr+GOffsetY,z+GOffsetZ) <= 0)
                    			val=0;}
                        	else val=0;
                    		}
                    	else val=0;
                    	
			col = mySlice[x+myData.Sizes[0]*z];
			Red = CMapR[val] & 0xff; // if (Red < 0) Red += 256;
			Green = CMapG[val] & 0xff; // if (Green < 0) Green += 256;
			Blue = CMapB[val] & 0xff; // if (Blue < 0) Blue += 256;
			Red = (Red << 16) + (col & (255 << 16)); 
			if (Red > (255 << 16)) Red = (255 << 16);
			Green = (Green << 8) + (col & (255 << 8)); 
			if (Green > (255 << 8)) Green = (255 << 8);
			Blue = Blue + (col & 255); 
			if (Blue > 255) Blue =  255;
                        mySlice[x+myData.Sizes[0]*z] = (255 << 24) | Red | Green | Blue;
                    }
                break;
            case 2:
            for (int y = 0; y < myData.Sizes[1]; y++) 
                    for (int x = 0; x < myData.Sizes[0]; x++) 
                    {
                    	if (myData.WithinBounds(x+OffsetX,y+OffsetY,sliceNr+OffsetZ))
                		{val = myData.GetIntValueAt(x+OffsetX,y+OffsetY,sliceNr+OffsetZ);
                        if (val < 0) val = 0; if (val > Bundle.MaxCTable-1) val = Bundle.MaxCTable-1;
                    	if (GateElement.WithinBounds(x+GOffsetX,y+GOffsetY,sliceNr+GOffsetZ))
                		{if (GateElement.GetIntValueAt(x+GOffsetX,y+GOffsetY,sliceNr+GOffsetZ) <= 0)
                            val=0;}
                         else val=0;
                		}
                        else val=0;
			col = mySlice[x+myData.Sizes[0]*y];
			Red = CMapR[val] & 0xff;// if (Red < 0) Red += 256;
			Green = CMapG[val] & 0xff; // if (Green < 0) Green += 256;
			Blue = CMapB[val] & 0xff; // if (Blue < 0) Blue += 256;
			Red = (Red << 16) + (col & (255 << 16)); 
			if (Red > (255 << 16)) Red = (255 << 16);
			Green = (Green << 8) + (col & (255 << 8)); 
			if (Green > (255 << 8)) Green = (255 << 8);
			Blue = Blue + (col & 255); 
			if (Blue > 255) Blue =  255;
                        mySlice[x+myData.Sizes[0]*y] = (255 << 24) | Red | Green | Blue;
                    }
                break;
        }
    } catch(Exception e)
	      {
                System.out.println("Caught Slice-Copy Exception:"+e+"\n");
		e.printStackTrace();
	      }
        previousSlice = sliceNr;
    return;
  } 

  
  // The function below multiplies an element to diplay with the current color display slice
  void MulToColorSlice(int sliceNr, AnElement myData, byte [] CMapR, byte [] CMapG, byte [] CMapB, AnElement GateElement,AnElement ActiveElement) {
        int val;
	int Red,Green,Blue,col;
	int OffsetX=-(int) (myData.DisplayOffset[0]- ActiveElement.DisplayOffset[0]); // subtract the offsets of the current element here
	int OffsetY=-(int) (myData.DisplayOffset[1]- ActiveElement.DisplayOffset[1]);
	int OffsetZ=-(int) (myData.DisplayOffset[2]- ActiveElement.DisplayOffset[2]);
	int GOffsetX=-(int) (GateElement.DisplayOffset[0]- ActiveElement.DisplayOffset[0]); // subtract the offsets of the current element here
	int GOffsetY=-(int) (GateElement.DisplayOffset[1]- ActiveElement.DisplayOffset[1]);
	int GOffsetZ=-(int) (GateElement.DisplayOffset[2]- ActiveElement.DisplayOffset[2]);
        try {
        switch (myDim) {
            case 0:
                for (int z = 0; z < myData.Sizes[2]; z++) 
                    for (int y = 0; y < myData.Sizes[1]; y++) 
                    {
                    	if (myData.WithinBounds(sliceNr+OffsetX,y+OffsetY,z+OffsetZ))
                		{val = myData.GetIntValueAt(sliceNr+OffsetX,y+OffsetY,z+OffsetZ);
                    	if (val < 0) val = 0; if (val > Bundle.MaxCTable-1) val = Bundle.MaxCTable-1;
                    	if (GateElement.WithinBounds(sliceNr+GOffsetX,y+GOffsetY,z+GOffsetZ))
                        {if (GateElement.GetIntValueAt(sliceNr+GOffsetX,y+GOffsetY,z+GOffsetZ) <= 0)
                            val=0;}
                    	else
                    		val=0;
                		}
                	else
                		val=0;
			col = mySlice[z+myData.Sizes[2]*y];
			Red = (CMapR[val] & 0xff) * ((col>>16) & 0xff); // if (Red < 0) Red += 256;
			Green = (CMapG[val] & 0xff)* ((col>>8) & 0xff); // if (Green < 0) Green += 256;
			Blue = (CMapB[val] & 0xff)* (col & 0xff); // if (Blue < 0) Blue += 256;
			Red = (((Red >> 8) & 0xff) << 16); 
			Green = (Green & 0xff00); 
			Blue = (Blue >> 8) & 0xff; 
                        mySlice[z+myData.Sizes[2]*y] = (255 << 24) | Red | Green | Blue;
                    }
                break;
            case 1:
                for (int z = 0; z < myData.Sizes[2]; z++) 
                    for (int x = 0; x < myData.Sizes[0]; x++) 
                    {
                    	if (myData.WithinBounds(x+OffsetX,sliceNr+OffsetY,z+OffsetZ))
                		{val = myData.GetIntValueAt(x+OffsetX,sliceNr+OffsetY,z+OffsetZ);
                		if (val < 0) val = 0; if (val > Bundle.MaxCTable-1) val = Bundle.MaxCTable-1;
                    	if (GateElement.WithinBounds(x+GOffsetX,sliceNr+GOffsetY,z+GOffsetZ))
                		{if (GateElement.GetIntValueAt(x+GOffsetX,sliceNr+GOffsetY,z+GOffsetZ) <= 0)
                			val=0;}
                    	else val=0;
                		}
                	else val=0;
			col = mySlice[x+myData.Sizes[0]*z];
			Red = (CMapR[val] & 0xff) * ((col>>16) & 0xff); // if (Red < 0) Red += 256;
			Green = (CMapG[val] & 0xff)* ((col>>8) & 0xff); // if (Green < 0) Green += 256;
			Blue = (CMapB[val] & 0xff)* (col & 0xff); // if (Blue < 0) Blue += 256;
			Red = (((Red >> 8) & 0xff) << 16); 
			Green = (Green & 0xff00); 
			Blue = (Blue >> 8) & 0xff; 
                        mySlice[x+myData.Sizes[0]*z] = (255 << 24) | Red | Green | Blue;
                    }
                break;
            case 2:
            for (int y = 0; y < myData.Sizes[1]; y++) 
                    for (int x = 0; x < myData.Sizes[0]; x++) 
                    {
                    	if (myData.WithinBounds(x+OffsetX,y+OffsetY,sliceNr+OffsetZ))
                		{val = myData.GetIntValueAt(x+OffsetX,y+OffsetY,sliceNr+OffsetZ);
                        if (val < 0) val = 0; if (val > Bundle.MaxCTable-1) val = Bundle.MaxCTable-1;
                    	if (GateElement.WithinBounds(x+GOffsetX,y+GOffsetY,sliceNr+GOffsetZ))
                		{if (GateElement.GetIntValueAt(x+GOffsetX,y+GOffsetY,sliceNr+GOffsetZ) <= 0)
                            val=0;}
                         else val=0;
                		}
                        else val=0;
			col = mySlice[x+myData.Sizes[0]*y];
			Red = (CMapR[val] & 0xff) * ((col>>16) & 0xff); // if (Red < 0) Red += 256;
			Green = (CMapG[val] & 0xff)* ((col>>8) & 0xff); // if (Green < 0) Green += 256;
			Blue = (CMapB[val] & 0xff)* (col & 0xff); // if (Blue < 0) Blue += 256;
			Red = (((Red >> 8) & 0xff) << 16); 
			Green = (Green & 0xff00); 
			Blue = (Blue >> 8) & 0xff; 
                        mySlice[x+myData.Sizes[0]*y] = (255 << 24) | Red | Green | Blue;
                    }
                break;
        }
    } catch(Exception e)
	      {
                System.out.println("Caught Slice-Copy Exception:"+e+"\n");
		e.printStackTrace();
	      }
        previousSlice = sliceNr;
    return;
  } 

  // The function below multiplies an element to diplay with the current color display slice
  void MulToColorSlice(ASlice myData, byte [] CMapR, byte [] CMapG, byte [] CMapB, ASlice GateSlice, boolean GateActive, AnElement ActiveElement) {
        int val;
	int Red,Green,Blue,col;
	int OffsetX,OffsetY,GOffsetX,GOffsetY;
	int XDim=0,YDim=1;
	switch (myDim) {
	case 0:
		XDim=1;YDim=2;  // YZ
		break;
	case 1:
		XDim=0;YDim=2;  // XZ
		break;
	case 2:
		XDim=0;YDim=1; // XY
		break;
	}
	OffsetX =-(int) (myData.DisplayOffset[XDim]- ActiveElement.DisplayOffset[XDim]); // subtract the offsets of the current element here
	OffsetY =-(int) (myData.DisplayOffset[YDim]- ActiveElement.DisplayOffset[YDim]);
	GOffsetX =-(int) (GateSlice.DisplayOffset[XDim]- ActiveElement.DisplayOffset[XDim]); // subtract the offsets of the current element here
	GOffsetY =-(int) (GateSlice.DisplayOffset[YDim]- ActiveElement.DisplayOffset[YDim]);
        try {
            for (int y = 0; y < myData.SliceSizeY; y++) 
                    for (int x = 0; x < myData.SliceSizeX; x++) 
                    {
                    	if (myData.WithinBounds(x+OffsetX,y+OffsetY))
                		{val = myData.GetIntValueAt(x+OffsetX,y+OffsetY);
                    	if (val < 0) val = 0; if (val > Bundle.MaxCTable-1) val = Bundle.MaxCTable-1;
                    	if (GateActive)
                    		if (GateSlice.WithinBounds(x+GOffsetX,y+GOffsetY))
                        {if (GateSlice.GetIntValueAt(x+GOffsetX,y+GOffsetY) <= 0)
                            val=0;}
                    	else
                    		val=0;
                		}
                	else
                		val=0;
                        if (val < 0) val = 0; if (val > Bundle.MaxCTable-1) val = Bundle.MaxCTable-1;
                        if (GateActive && GateSlice.GetIntValueAt(x,y) <= 0)
                            val=0;
                        col = mySlice[x+myData.SliceSizeX*y];
						Red = (CMapR[val] & 0xff) * ((col>>16) & 0xff); // if (Red < 0) Red += 256;
						Green = (CMapG[val] & 0xff)* ((col>>8) & 0xff); // if (Green < 0) Green += 256;
						Blue = (CMapB[val] & 0xff)* (col & 0xff); // if (Blue < 0) Blue += 256;
						Red = (((Red >> 8) & 0xff) << 16); 
						Green = (Green & 0xff00); 
						Blue = (Blue >> 8) & 0xff; 
				        mySlice[x+myData.SliceSizeX*y] = (255 << 24) | Red | Green | Blue;
                    }
    } catch(Exception e)
	      {
                System.out.println("Caught Slice-Copy Exception:"+e+"\n");
		e.printStackTrace();
	      }
    return;
  } 

  // The function below multiplies an element to diplay with the current color display slice
  void SumToColorSlice(ASlice myData, byte [] CMapR, byte [] CMapG, byte [] CMapB, ASlice GateSlice, boolean GateActive, AnElement ActiveElement) {
    int val;
	int Red,Green,Blue,col;
	int OffsetX,OffsetY,GOffsetX,GOffsetY;
	int XDim=0,YDim=1;
	switch (myDim) {
	case 0:
		XDim=1;YDim=2;  // YZ
		break;
	case 1:
		XDim=0;YDim=2;  // XZ
		break;
	case 2:
		XDim=0;YDim=1; // XY
		break;
	}
	OffsetX=-(int) (myData.DisplayOffset[XDim]- ActiveElement.DisplayOffset[XDim]); // subtract the offsets of the current element here
	OffsetY=-(int) (myData.DisplayOffset[YDim]- ActiveElement.DisplayOffset[YDim]);
	GOffsetX=-(int) (GateSlice.DisplayOffset[XDim]- ActiveElement.DisplayOffset[XDim]); // subtract the offsets of the current element here
	GOffsetY=-(int) (GateSlice.DisplayOffset[YDim]- ActiveElement.DisplayOffset[YDim]);
        try {
            for (int y = 0; y < myData.SliceSizeY; y++) 
                    for (int x = 0; x < myData.SliceSizeX; x++) 
                    {
                    	if (myData.WithinBounds(x+OffsetX,y+OffsetY))
                		{val = myData.GetIntValueAt(x+OffsetX,y+OffsetY);
                    	if (val < 0) val = 0; if (val > Bundle.MaxCTable-1) val = Bundle.MaxCTable-1;
                    	if (GateActive)
                    		if (GateSlice.WithinBounds(x+GOffsetX,y+GOffsetY))
                        {
                    		if (GateSlice.GetIntValueAt(x+GOffsetX,y+GOffsetY) <= 0)
                            val=0;}
                    		else
                    		val=0;
                		}
                    		else
                    			val=0;

                        col = mySlice[x+myData.SliceSizeX*y];
            			Red = CMapR[val] & 0xff;// if (Red < 0) Red += 256;
            			Green = CMapG[val] & 0xff; // if (Green < 0) Green += 256;
            			Blue = CMapB[val] & 0xff; // if (Blue < 0) Blue += 256;
            			Red = (Red << 16) + (col & (255 << 16)); 
            			if (Red > (255 << 16)) Red = (255 << 16);
            			Green = (Green << 8) + (col & (255 << 8)); 
            			if (Green > (255 << 8)) Green = (255 << 8);
            			Blue = Blue + (col & 255); 
            			if (Blue > 255) Blue =  255;
				        mySlice[x+myData.SliceSizeX*y] = (255 << 24) | Red | Green | Blue;
                    }
    } catch(Exception e)
	      {
                System.out.println("Caught Slice-Copy Exception:"+e+"\n");
		e.printStackTrace();
	      }
    return;
  } 

  boolean WithinBounds(int x, int y) {
	  //System.out.println("Within Bounds? ");
      if (x >= SliceSizeX) return false;
      if (y >= SliceSizeY) return false;
      if (x < 0) return false;
      if (y < 0) return false;
	  //System.out.println("Yes !\n");
      return true;
  }

  int GetIntValueAt(int x, int y) {
    	return mySlice[x+SliceSizeX*y];
    }
    
 /* void UpdateColorSlice(int sliceNr, AnElement myDataR,AnElement myDataG,AnElement myDataB, AnElement GateElement)  // will simply updata the memory
  {
        if (previousSlice != sliceNr)
            isValid = false;
        if (isValid)
            return;
        int R,G,B;
        try {
        switch (myDim) {
            case 0:
                for (int z = 0; z < myDataR.Sizes[2]; z++) 
                    for (int y = 0; y < myDataR.Sizes[1]; y++)
                    {
                        R = myDataR.GetByteValueAt(sliceNr,y,z); G = myDataG.GetByteValueAt(sliceNr,y,z); B = myDataB.GetByteValueAt(sliceNr,y,z);
                        if (R < 0) R = 0; if (R > 255) R = 255;
                        if (G < 0) G = 0; if (G > 255) G = 255;
                        if (B < 0) B = 0; if (B > 255) B = 255;
                        if (GateElement.GetIntValueAt(sliceNr,y,z) <= 0)
                            {R=0;G=0;B=0;}
                        mySlice[z+SliceSizeX*y] =  (255 << 24) | (R << 16) | (G << 8) | B;
                    }
                break;
            case 1:
                for (int z = 0; z < myDataR.Sizes[2]; z++) 
                    for (int x = 0; x < myDataR.Sizes[0]; x++) 
                    {
                        R = myDataR.GetByteValueAt(x,sliceNr,z); G = myDataG.GetByteValueAt(x,sliceNr,z); B = myDataB.GetByteValueAt(x,sliceNr,z);
                        if (R < 0) R = 0; if (R > 255) R = 255;
                        if (G < 0) G = 0; if (G > 255) G = 255;
                        if (B < 0) B = 0; if (B > 255) B = 255;
                        if (GateElement.GetIntValueAt(x,sliceNr,z) <= 0)
                             {R=0;G=0;B=0;}
                        mySlice[x+SliceSizeX*z] =  (255 << 24) | (R << 16) | (G << 8) | B;
                    }
                break;
            case 2:
            for (int y = 0; y < myDataR.Sizes[1]; y++) 
                    for (int x = 0; x < myDataR.Sizes[0]; x++) 
                    {
                        R = myDataR.GetByteValueAt(x,y,sliceNr); G = myDataG.GetByteValueAt(x,y,sliceNr); B = myDataB.GetByteValueAt(x,y,sliceNr);
                        if (R < 0) R = 0; if (R > 255) R = 255;
                        if (G < 0) G = 0; if (G > 255) G = 255;
                        if (B < 0) B = 0; if (B > 255) B = 255;
                        if (GateElement.GetIntValueAt(x,y,sliceNr) <= 0)
                             {R=0;G=0;B=0;}
                        mySlice[x+SliceSizeX*y] =  (255 << 24) | (R << 16) | (G << 8) | B;
                    }
                break;
        }
    } catch(Exception e)
	      {
                System.out.println("Caught Color-Slice-Copy Exception:"+e+"\n");
		e.printStackTrace();
	      }
     isValid = true;
     previousSlice = sliceNr;
     return;
  }*/

  void MergeColor(int color, ASlice Slice)  // mereges a byte-slice into a color channel
  {
      int shift=0;   // bits to shift
      if (color == 0)
          shift = 16;
      else if (color == 1)
          shift = 8;
      else
          shift = 0;
      
      int val;
      for (int i = 0; i < AllSize; i++)
      {
          val = Slice.mySlice[i] >> 8;
        mySlice[i] |= (val  << shift);
      }
    return;
  }
  
  public void ClearColor() {   // one could use java.lang.Arrays.fill(...) but this is > java 1.2
    for (int i = 0; i < AllSize; i++) 
            mySlice[i] = 255 << 24;
  }
  
  public void Clear() {   // one could use java.lang.Arrays.fill(...) but this is > java 1.2
    for (int i = 0; i < AllSize; i++) 
            mySlice[i] = 0;
  }
  
//  private int ProjectionPos(int dir, int x, int y, int z)
//  {
//      if (dir == 0)
//          return z+SliceSizeX*y;
//      else if (dir == 1)
//          return x+SliceSizeX*z;
//      else if (dir == 2)
//          return x+SliceSizeX*y;
//      else return 0;
//  }
  
  private int ProjSizeDim(int projdim)  // returns the dimension who's size shall be used for 1D-sum data
  {
      if (projdim == 0)
          return 1;
      if (projdim == 2)
          return 2;
      if (projdim == 1)
          return 0;
      return 2;
  }
  
  private int Proj1DPos(int projdim,int x, int y, int z)  // returns the dimension who's size shall be used for 1D-sum data
  {
      if (projdim == 0)
          return y;
      if (projdim == 2)
          return z;
      if (projdim == 1)
          return x;
      return z;
  }
  
  private int XPos3D(int pdir, int px, int py, int pos) // computes the x 3D position from projection position
  {
      if (pdir == 0)
          return pos;
      else if (pdir == 1)
          return px;
      else // dir == 2
          return px;
  }
  private int YPos3D(int pdir, int px, int py, int pos) // computes the x 3D position from projection position
  {
      if (pdir == 0)
          return py;
      else if (pdir == 1)
          return pos;
      else // dir == 2
          return py;
  }
  private int ZPos3D(int pdir, int px, int py, int pos) // computes the x 3D position from projection position
  {
      if (pdir == 0)
          return px;
      else if (pdir == 1)
          return py;
      else // dir == 2
          return pos;
  }
  
  public void DoProject(int direction, AnElement myData, AnElement gate, ROI roi) {
  	int x,y,z,xo,yo,zo,gx,gy,gz;
	int val,voxels=0, maxVal=0, pvoxels=0;
        double rval;
        ROISum=0.0;ROIAvg=0.0;ROIMax=-Double.MAX_VALUE;ROIMin=Double.MAX_VALUE;
        int Proj1DSize = myData.Sizes[ProjSizeDim(direction)];
        if (my1DProj == null)
            {
                my1DProj = new double[Proj1DSize];
                my1DProjVoxels = new int[Proj1DSize];
                // System.out.println("Allocated dim: "+direction+", size: "+Proj1DSize+"\n");
            }
        Clear();
        for (int i = 0; i < Proj1DSize; i++) 
            {
                my1DProj[i] = 0.0;
                my1DProjVoxels[i] = 0;
            }
        
        for (int px = 0; px < SliceSizeX; px++)
            for (int py = 0; py < SliceSizeY; py++)
            {
                pvoxels = 0;
                for (int pos = 0; pos < myData.Sizes[direction]; pos++)
                {
                    x = XPos3D(direction,px,py,pos)+(int) myData.DisplayOffset[0];
                    xo = x-(int) myData.DisplayOffset[0];
                    y = YPos3D(direction,px,py,pos)+(int) myData.DisplayOffset[1];
                    yo = y-(int) myData.DisplayOffset[1];
                    z = ZPos3D(direction,px,py,pos)+(int) myData.DisplayOffset[2];
                    zo = z-(int) myData.DisplayOffset[2];
                    gx = XPos3D(direction,px,py,pos)+(int) gate.DisplayOffset[0];
                    gy = YPos3D(direction,px,py,pos)+(int) gate.DisplayOffset[1];
                    gz = ZPos3D(direction,px,py,pos)+(int) gate.DisplayOffset[2];
                    if (myData.WithinBounds(xo,yo,zo) && gate.WithinBounds(gx,gy,gz))
                     if (roi.InROIRange(x,y,z) && (gate.GetIntValueAt(gx,gy,gz) > 0)) // Values below gate are not considered
                        {
                        voxels++; pvoxels++;
                        rval = myData.GetValueAt(xo,yo,zo);
                        ROISum += rval;
                        if (rval < ROIMin) ROIMin = rval;
                        if (rval > ROIMax) ROIMax = rval;
                        val = myData.GetIntValueAt(xo,yo,zo);
                        if (val < 0) val = 0;
                        my1DProjVoxels[Proj1DPos(direction,x,y,z)] ++;
                        
                        if (MIPMode)  // Maximum Intensity Projection
                            {
                            if (val > mySlice[px+SliceSizeX*py])
                                mySlice[px+SliceSizeX*py] = val;
                            if (rval > my1DProj[Proj1DPos(direction,x,y,z)])
                                my1DProj[Proj1DPos(direction,x,y,z)] = rval;
                            }
                        else  // Compute sum projection
                            {
                            mySlice[px+SliceSizeX*py] += val;
                            my1DProj[Proj1DPos(direction,x,y,z)] += rval;
                            }
                        }  // end of if IN RANGE
                }  // end of for pos
               if (pvoxels > 0)
                        {
                            if (! MIPMode)
                                mySlice[px+SliceSizeX*py] /= pvoxels;  // compute average instead of sum
                            if (mySlice[px+SliceSizeX*py] > maxVal)
                                maxVal = mySlice[px+SliceSizeX*py];
                        }
               } // end of for px,py
                
        double scale = ((double) Bundle.MaxCTable-1) / maxVal;
        for (int i = 0; i < AllSize; i++) 
            {
                mySlice[i] = (int) (mySlice[i] * scale);
                if (mySlice[i] < 0) mySlice[i] = 0;
            }
	
        double maxVal1D=-1e30, minVal1D=1e30;
        for (int i = 0; i < Proj1DSize; i++) 
            {
                if (! MIPMode && my1DProjVoxels[i] > 0)
                    my1DProj[i] /= my1DProjVoxels[i];
                if (my1DProj[i] > maxVal1D)
                    maxVal1D = my1DProj[i];
                if (my1DProj[i] < minVal1D)
                    minVal1D = my1DProj[i];
            }
        Proj1DScale = ((double) 1.0) / (maxVal1D - minVal1D);
        Proj1DOffset = minVal1D;
        
	if (voxels > 0)
            ROIAvg = ROISum / (double) (voxels);
        ROIVoxels = (double) voxels;   // Only the voxels 
        
        isValid = true;
    }
  
  double Get1DProjValue(int pos) {
	  if (pos <0) pos=0;
	  if (pos >= SliceSizeX*SliceSizeY) pos=SliceSizeX*SliceSizeY-1;
      if (my1DProj != null)
         return my1DProj[pos];
      else
      {
          System.out.println("Error: Projection not initialized\n");
          return 0.0;
      }
  }

  double GetNormed1DProjValue(int pos) {
	  if (pos <0) pos=0;
	  if (pos >= SliceSizeX*SliceSizeY) pos=SliceSizeX*SliceSizeY-1;
      if (my1DProj != null)
         return (my1DProj[pos] - Proj1DOffset) * Proj1DScale;
      else
      {
          System.out.println("Error: Projection not initialized\n");
          return 0.0;
      }
  }
  
  // Export writes this slice back to ImageJ
  public ImagePlus Export() {
       ImagePlus myim=NewImage.createShortImage("View5D Gray Slice",SliceSizeX,SliceSizeY,1,NewImage.FILL_BLACK);
       short[] pix= (short []) myim.getImageStack().getPixels(1);
       
       for (int i=0;i<SliceSizeX*SliceSizeY;i++)
         pix[i] = (short) mySlice[i];
       return myim;
   }

  public ImagePlus ColorExport() {
       ImagePlus myim=NewImage.createRGBImage("View5D Color Slice",SliceSizeX,SliceSizeY,1,NewImage.FILL_BLACK);
       int[] pix= (int []) myim.getImageStack().getPixels(1);
       
       for (int i=0;i<SliceSizeX*SliceSizeY;i++)
         pix[i] = mySlice[i];
       return myim;
   }

}
