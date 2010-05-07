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

// import java.awt.image.ColorModel.*;
// import java.awt.color.*;
package view5d;

import java.util.*;
import java.text.*;

public abstract class AnElement extends Object {
    // Different Data Types, tags listed below
    public static int InvalidType=-10;
    public static int ByteType = 0, IntegerType = 1, FloatType = 2, DoubleType = 3, ComplexType = 4, ShortType=5, LongType=6;
    public static int NumTypes=7;
    public static String TypeNames[]={"Byte","Integer","Float","Double","Complex","Short","Long"};  // The last types are converted to integer
    public static String UTypeNames[]={"Unsigned Byte","Unsigned Integer","Float","Double","Complex","Unsigned Short","Unsigned Long"};  // The last types are converted to integer
    public int Sizes[];
    public double Scales[], ScaleV;
    public double OffsetV;  // marks the minimum of the dataset
    public double Offsets[];
    public double[] DisplayOffset={0,0,0};   // display offsets of canvasses, stored here to allow independence of times
    public String [] Units,Names;
    public String UnitV;
    public String NameV;
    public int DataType; // a tag for the type of element  // was static
    
    double MaxValue;       // marks the maximum of the dataset (or the max possible value)
    double scaleB=1.0,scaleI=1.0,shift=0.0,Min=0.0,Max=0.0;
    AnElement DataToHistogramX=null, DataToHistogramY=null, DataToHistogramZ=null;
    public NumberFormat nf;
        
    AnElement(int SX, int SY, int SZ, double MV) {
        MaxValue = MV;
        Sizes = new int[3];
        Units = new String[5];
        Names = new String[5];
        Sizes[0]=SX;Sizes[1]=SY;Sizes[2]=SZ;
        // System.out.println("Allocated Sizes: "+Sizes[0]+", "+Sizes[1]+", "+Sizes[2]+"\n");
                        
        Scales = new double[5];
        Offsets = new double[5];
        Scales[0]=1.0;Scales[1]=1.0;Scales[2]=1.0;Scales[3]=1.0;Scales[4]=1.0;
        ScaleV=1.0; OffsetV=0.0;
        Offsets[0]=0.0;Offsets[1]=0.0;Offsets[2]=0.0;Offsets[3]=0.0;Offsets[4]=0.0;
        Names[0] ="X";Names[1] ="Y";Names[2] ="Z";Names[3] ="Element";Names[4] ="Time";
        Units[0] ="pixels";Units[1] ="pixels";Units[2] ="pixels";Units[3] ="elements";Units[4] ="steps";
        NameV ="intensity";
        UnitV ="a.u.";
        DataType = InvalidType; // (invalid)
    	nf = java.text.NumberFormat.getNumberInstance(Locale.US);
    	nf.setMaximumFractionDigits(2);
    	// nf.setMinimumIntegerDigits(7);
    	nf.setGroupingUsed(false);
    }
    
    abstract void Clear();
    abstract void DeleteData();
    abstract void SetValueAt(int x, int y, int z, double val);
    abstract int GetStdByteNum();   // returns the standard number of bytes
    int GetStdBitNum() {return 8*GetStdByteNum();}
    
    abstract double GetRawValueAt(int x, int y, int z);
    abstract double GetValueAt(int x, int y, int z);

    String GetDataTypeName() {return TypeNames[DataType];}

    double GetRawValueWithBounds(int x, int y, int z)
    {
        if (x >= Sizes[0]) x=Sizes[0]-1;
        if (y >= Sizes[1]) y=Sizes[1]-1;
        if (z >= Sizes[2]) z=Sizes[2]-1;
        if (x < 0) x=0;
        if (y < 0) y=0;
        if (z < 0) z=0;
        return GetRawValueAt(x,y,z);
    }

    double GetRawValueAtOffset(int x, int y, int z, AnElement Reference) {
    	return GetRawValueWithBounds(x - (int) DisplayOffset[0] + (int) Reference.DisplayOffset[0],
    			y - (int) DisplayOffset[1] + (int) Reference.DisplayOffset[1],
    			z - (int) DisplayOffset[2] + (int) Reference.DisplayOffset[2]);
    }

    double GetValueAtOffset(int x, int y, int z, AnElement Reference) {
    	return GetValueWithBounds(x - (int) DisplayOffset[0] + (int) Reference.DisplayOffset[0],
    			y - (int) DisplayOffset[1] + (int) Reference.DisplayOffset[1],
    			z - (int) DisplayOffset[2] + (int) Reference.DisplayOffset[2]);
    }

//    double GetValueAtOffset(int x, int y, int z) {
//    	return GetValueAt(x - (int) DisplayOffset[0],y - (int) DisplayOffset[1],z - (int) DisplayOffset[2]);
//    }

    String GetValueStringAt(int x, int y, int z)   // Prints the value onto the screen. Can be overwritten
     {
      return nf.format(GetValueAt(x,y,z));
     }
    
    boolean GateAboveZero(int x, int y, int z, AnElement Reference){
        return (GetIntValueAt(x - (int) DisplayOffset[0] + (int) Reference.DisplayOffset[0],
        		y-(int) DisplayOffset[1] + (int) Reference.DisplayOffset[1],
        		z-(int) DisplayOffset[2] + (int) Reference.DisplayOffset[2]) > 0);
    }
    
    boolean InROIRange(int x, int y, int z, ROI myroi){
        return myroi.InROIRange(x+(int) DisplayOffset[0],y+(int) DisplayOffset[1],z+(int) DisplayOffset[2]);
    }

    boolean InsideBounds(int x, int y, int z)
    {
        if (x >= Sizes[0]) return false;
        if (y >= Sizes[1]) return false;
        if (z >= Sizes[2]) return false;
        if (x < 0) return false;
        if (y < 0) return false;
        if (z < 0) return false;
	return true;   
    }
    
    double GetValueWithBounds(int x, int y, int z)
    {
        if (x >= Sizes[0]) x=Sizes[0]-1;
        if (y >= Sizes[1]) y=Sizes[1]-1;
        if (z >= Sizes[2]) z=Sizes[2]-1;
        if (x < 0) x=0;
        if (y < 0) y=0;
        if (z < 0) z=0;
        return GetValueAt(x,y,z);
    }

    abstract int GetIntValueAt(int x, int y, int z);
    
    boolean WithinBounds(int x, int y, int z) {
        if (x >= Sizes[0]) return false;
        if (y >= Sizes[1]) return false;
        if (z >= Sizes[2]) return false;
        if (x < 0) return false;
        if (y < 0) return false;
        if (z < 0) return false;
        return true;
    }
    
    int GetIntValueWithBounds(int x, int y, int z)
    {
        if (x >= Sizes[0]) x=Sizes[0]-1;
        if (y >= Sizes[1]) y=Sizes[1]-1;
        if (z >= Sizes[2]) z=Sizes[2]-1;
        if (x < 0) x=0;
        if (y < 0) y=0;
        if (z < 0) z=0;
        return GetIntValueAt(x,y,z);
    }

    abstract int GetByteValueAt(int x, int y, int z);  // even thought returning a value scaled to 255 it returns integer
    // The methods below are for ImageJ, which returns the arrays with specific types (uncastable?)
    abstract void ConvertSliceFromSimilar(int myslice, int bufslice, Object Ibuffer, int mstep, int moff);
    abstract void ConvertSliceFromByte(int myslice, int bufslice, byte [] Ibuffer, int mstep, int moff);
    abstract void ConvertSliceFromRGB(int myslice, int bufslice, int [] Ibuffer, int mstep, int moff, int soff);
    // copies a slice to a buffer of the same datatyp
    abstract void CopySliceToSimilar(int myslice, Object Ibuffer);
    
    void AdvanceReadMode() {
    return;  // just ignore. Only useful for complex dataclass
    }
  
    void SetReadMode(int rmode) {
    return;  // just ignore. Only useful for complex dataclass
    }

    void SetScales(AnElement e) {
        // scaleB= e.scaleB;scaleI= e.scaleI;
        Scales[0]=e.Scales[0];Scales[1]=e.Scales[1];Scales[2]=e.Scales[2];Scales[3]=e.Scales[3];Scales[4]=e.Scales[4];
        Offsets[0]=e.Offsets[0];Offsets[1]=e.Offsets[1];Offsets[2]=e.Offsets[2];Offsets[3]=e.Offsets[3];Offsets[4]=e.Offsets[4];
        ScaleV=e.ScaleV;
        OffsetV=e.OffsetV;
        NameV = e.NameV;  // can be changed !?
        UnitV = e.UnitV;  // can be changed !?
        Units = e.Units;  // are allways coupled together!
        Names = e.Names;  // are allways coupled together!
    }
    
    void SetScales(double ValScale,double ValOffset, String VName, String VUnit)
    {
        ScaleV = ValScale;
        OffsetV = ValOffset;
        NameV = VName;  // can be changed !?
        UnitV = VUnit;
    }
    void SetScales(double[] NScales, double[] NOffsets, double SV, double OV) {
        Scales=NScales; // (double []) NScales.clone();
        Offsets=NOffsets; // (double []) Offsets.clone();
        ScaleV=SV;OffsetV=OV;
    }
    
  void Add(AnElement other) {
    for (int z = 0; z < Sizes[2]; z++) 
            for (int y = 0; y < Sizes[1]; y++) 
                for (int x = 0; x < Sizes[0]; x++) 
                        SetValueAt(x,y,z,GetValueAt(x,y,z)+other.GetValueAt(x,y,z));
  }
  void Sub(AnElement other) {
    for (int z = 0; z < Sizes[2]; z++) 
            for (int y = 0; y < Sizes[1]; y++) 
                for (int x = 0; x < Sizes[0]; x++) 
                        SetValueAt(x,y,z,GetValueAt(x,y,z)-other.GetValueAt(x,y,z));
  }
  void Mul(AnElement other) {
    for (int z = 0; z < Sizes[2]; z++) 
            for (int y = 0; y < Sizes[1]; y++) 
                for (int x = 0; x < Sizes[0]; x++) 
                        SetValueAt(x,y,z,GetValueAt(x,y,z)*other.GetValueAt(x,y,z));
  }

  void Div(AnElement other) {
    for (int z = 0; z < Sizes[2]; z++) 
            for (int y = 0; y < Sizes[1]; y++) 
                for (int x = 0; x < Sizes[0]; x++)
                    if (other.GetValueAt(x,y,z) != 0)
                        SetValueAt(x,y,z,GetValueAt(x,y,z)/other.GetValueAt(x,y,z));
                    else if (GetValueAt(x,y,z) == 0)
                        SetValueAt(x,y,z,0);
                    else
                        SetValueAt(x,y,z,1e32);
  }
  
  static double ComputeIntMaxScale(int dx,int dy,int dz, double fwhm)
  {
      double r2,myintegral=0,sigma2=(fwhm/2.0)*(fwhm/2.0)/java.lang.Math.log(2.0);
      for (int zz=-dz; zz<=dz;zz++)    // first compute the integral of this gaussian (without background)
        for (int yy=-dy; yy<=+dy;yy++)
          for (int xx=-dx; xx<=+dx;xx++)
          {
              r2 = xx*xx+yy*yy+zz*zz;
              myintegral += java.lang.Math.exp(-r2/sigma2);
          }
      if (myintegral > 0.0)
        return 1.0/myintegral;
      else
          return 0.0;
  }
  
  void SubtractGauss(double px,double py,double pz,double integral,int dx,int dy,int dz, double fwhm)
  {  // subtracts a Gaussian shaped intensity from the data
      // The background (as given by the minimum is left in
      int x=(int)(px+0.5);int y=(int)(py+0.5);int z=(int)(pz+0.5);
      double r2,myintegral=0,valintegral=0,sigma2=(fwhm/2.0)*(fwhm/2.0)/java.lang.Math.log(2.0);
      double mval=GetValueAt(x,y,z),val;
      for (int zz=z-dz; zz<=z+dz;zz++)    // first compute the integral of this gaussian (without background)
        for (int yy=y-dy; yy<=y+dy;yy++)
          for (int xx=x-dx; xx<=x+dx;xx++)
          {
              r2 = (xx-px)*(xx-px)+(yy-py)*(yy-py)+(zz-pz)*(zz-pz);
              myintegral += java.lang.Math.exp(-r2/sigma2);
              val=GetValueAt(xx,yy,zz);
              valintegral+=val;
              if (val < mval) mval=val;
          }
      double intensity=(valintegral-mval*(dx*2+1)*(dy*2+1)*(dz*2+1))/myintegral;
      
      for (int zz=z-dz; zz<=z+dz;zz++)
        for (int yy=y-dy; yy<=y+dy;yy++)
          for (int xx=x-dx; xx<=x+dx;xx++)
          {
              r2 = (xx-px)*(xx-px)+(yy-py)*(yy-py)+(zz-pz)*(zz-pz);
              val=GetValueAt(xx,yy,zz)-intensity*java.lang.Math.exp(-r2/sigma2);
              SetValueAt(xx,yy,zz,val);
          }
  }


  void CopyVal(AnElement other) {
    for (int z = 0; z < Sizes[2]; z++) 
            for (int y = 0; y < Sizes[1]; y++) 
                for (int x = 0; x < Sizes[0]; x++) 
                        SetValueAt(x,y,z,other.GetValueAt(x,y,z));  // will convert the type and include all scaling factors
  }
      
   void SetScaleShift(double mincs,double maxcs)
    {
        scaleB = 256/(maxcs-mincs);
        //scaleI = 65536/(maxcs-mincs);
        scaleI = (Bundle.MaxCTable+1)/(maxcs-mincs);
        shift = mincs;
    }
    
    void SetMinMax() {
	Max = GetRawValueAt(0,0,0);
        Min = GetRawValueAt(0,0,0);
        double val;
        for (int z = 0; z < Sizes[2]; z++) 
            for (int y = 0; y < Sizes[1]; y++) 
                for (int x = 0; x < Sizes[0]; x++) 
                {
                    val = GetRawValueAt(x,y,z);
                    if (val > Max) Max = val;
                    if (val < Min) Min = val;
                }
    }

    
    double ROIMaximum(ROI roi) {
	double max = -1e30;
        for (int z = 0; z < Sizes[2]; z++) 
            for (int y = 0; y < Sizes[1]; y++) 
                for (int x = 0; x < Sizes[0]; x++) 
                    if (roi.InROIRange(x,y,z))
                        if (GetRawValueAt(x,y,z) > max)
                            max=GetRawValueAt(x,y,z);
	return max;
    }
    
    double ROIMinimum(ROI roi) {
	double min = 1e30;
        for (int z = 0; z < Sizes[2]; z++) 
            for (int y = 0; y < Sizes[1]; y++) 
                for (int x = 0; x < Sizes[0]; x++) 
                    if (roi.InROIRange(x,y,z))
                        if (GetRawValueAt(x,y,z) < min)
                            min=GetRawValueAt(x,y,z);
	return min;
    }
    
    void GenerateMask(ROI roi, AnElement gate, AnElement from, boolean cpData)
    {
        if (! cpData)
        {
        for (int z = 0; z < Sizes[2]; z++) 
            for (int y = 0; y < Sizes[1]; y++) 
                for (int x = 0; x < Sizes[0]; x++) 
                    if (roi.InROIRange(x,y,z))
                        if (gate.GetIntValueAt(x,y,z) > 0) // Values below gate are not considered
                            SetValueAt(x,y,z,1);
                        else
                            SetValueAt(x,y,z,0);
        }
        else
        {
        for (int z = 0; z < Sizes[2]; z++) 
            for (int y = 0; y < Sizes[1]; y++) 
                for (int x = 0; x < Sizes[0]; x++) 
                    if (roi.InROIRange(x,y,z))
                        if (gate.GetIntValueAt(x,y,z) > 0) // Values below gate are not considered
                            SetValueAt(x,y,z,from.GetRawValueAt(x,y,z));
                        else
                            SetValueAt(x,y,z,0);
        }
    }
    
    void ComputeHistMask(AnElement mask, ROI roi)
    {
        double ex=0,ey=0,ez=0;
        if (DataToHistogramX == null)
        {
            System.out.println("Error : No X-direction present for Histogram !\n");
            return;
        }
          for (int z = 0; z < DataToHistogramX.Sizes[2]; z++) 
            for (int y = 0; y < DataToHistogramX.Sizes[1]; y++) 
                for (int x = 0; x < DataToHistogramX.Sizes[0]; x++) 
                {
                    if (DataToHistogramX != null)
                        ex = DataToHistogramX.GetValueAtOffset(x,y,z,DataToHistogramX);  // always reference to HistoX element in original data
                    if (DataToHistogramY != null)
                        ey = DataToHistogramY.GetValueAtOffset(x,y,z,DataToHistogramX);
                    if (DataToHistogramZ != null)
                        ez = DataToHistogramZ.GetValueAtOffset(x,y,z,DataToHistogramX);
                    if (roi.InROIRange((int)((ex-Offsets[0])/Scales[0] + 0.5),
                                       (int)((ey-Offsets[1])/Scales[1] + 0.5),(int)((ez-Offsets[2])/Scales[2] + 0.5)))
                        mask.SetValueAt(x,y,z,1); // will always be forced to correspond to HistoX data location
                    else
                        mask.SetValueAt(x,y,z,0);
                }
        mask.Max = 1.0;
        mask.Min = 0.0;
        mask.MaxValue = 5.0;
        mask.AlignDisplayTo(DataToHistogramX);
    }
    
    public void AlignDisplayTo(AnElement other) {
        DisplayOffset[0] = other.DisplayOffset[0]; 
        DisplayOffset[1] = other.DisplayOffset[1]; 
        DisplayOffset[2] = other.DisplayOffset[2]; 
    }
   public int ComputeHistogram(AnElement gate, ROI roi) {  // runs through the given dataset inside the ROI and computes a 3D Histogram
        NameV = "frequency";
        UnitV = "cnts";
        System.out.println("Computing Histogram with Hscale (X,Y,Z): "+Scales[0]+", "+Scales[1]+", "+Scales[2]+"\n");
        System.out.println("Offsets: "+Offsets[0]+", "+Offsets[1]+", "+Offsets[2]+"\n");
            
        if (DataToHistogramX == null) {
            System.out.println("Error: No data connected to this histogram");
            return 0;  // cannot compute a histogramm
            }
        Units[0] = DataToHistogramX.UnitV;
        Names[0] = DataToHistogramX.NameV;
        if (DataToHistogramY != null)
            {Units[1] = DataToHistogramY.UnitV;
            Names[1] = DataToHistogramY.NameV;}
        if (DataToHistogramZ != null)
            {Units[2] = DataToHistogramZ.UnitV;
            Names[2] = DataToHistogramZ.NameV;}
        
        int px,py,pz;
        double val;
        int vali,max=0;
        for (int z=0;z< DataToHistogramX.Sizes[2];z++)      // this is always the coordinate system of the HistoX element
            for (int y=0;y< DataToHistogramX.Sizes[1];y++)
                for (int x=0;x< DataToHistogramX.Sizes[0];x++)
                    if (gate.GateAboveZero(x,y,z,DataToHistogramX) && DataToHistogramX.InROIRange(x,y,z,roi)) // Values below gate are not considered
                    {
                        px=0;py=0;pz=0;
                        // System.out.println("datapos : "+x+", "+y+", "+z+":     ");
                        
                        if (DataToHistogramX != null )
                        {
                            val = DataToHistogramX.GetValueAtOffset(x,y,z,DataToHistogramX);
                        //System.out.println("valx: "+val+":     ");
                            px = (int) ((val-Offsets[0])/Scales[0]);   // find the correct bin
                        //System.out.println("px: "+px+":     ");
                            if (px < 0) px=0;
                            if (px >= Sizes[0]) px = Sizes[0]-1;
                        }
                        if (DataToHistogramY != null)
                            {
                            val = DataToHistogramY.GetValueAtOffset(x,y,z,DataToHistogramX);
                        //System.out.println("valy: "+val+":     ");
                            py = (int) ((val-Offsets[1])/Scales[1]);
                        //System.out.println("py: "+py+":     ");
                            if (py < 0) py=0;
                            if (py >= Sizes[1]) py = Sizes[1]-1;
                            }
                        if (DataToHistogramZ != null)
                            {
                            val = DataToHistogramZ.GetValueAtOffset(x,y,z,DataToHistogramX);
                            pz = (int) ((val-Offsets[2])/Scales[2]);
                            if (pz < 0) pz=0;
                            if (pz >= Sizes[2]) pz = Sizes[2]-1;
                            }
        
                        // System.out.println("pos : "+px+", "+py+", "+pz+":     ");
                        // System.out.println("Sizes: "+Sizes[0]+", "+Sizes[1]+", "+Sizes[2]+"\n");
                        	vali = ((int) GetRawValueAt(px,py,pz))+1;
                        	SetValueAt(px,py,pz,vali);
                        	if (vali > max) max=vali;
                    }
        System.out.println("max : "+max+"\n");
        Max = max; Min = 0.0;
        MaxValue = Max; 
        SetScaleShift(0.0,Max);
        return max;
     }
}
