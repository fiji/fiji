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
import java.util.*;

public class Bundle extends Object implements Cloneable {  // this class bundles some user-defined values seperately for each element
    private double mincs=0.0,maxcs=1.0;
    private double ProjMincs[]={0.0,0.0},ProjMaxcs[]={1.0,1.0}; // For the different projection modes (Max, Avg)
    public int ElementModelNr=0;    // just a number for the current model 
    final static int ElementModels=13;  // Nr of element models
    final static String ElementModelName[]={"GrayScale","Red","Green","Blue","Purple","Glow Red","Glow Green-Blue","Glow Green-Yellow","Glow Blue","Glow Purple","Rainbow","Random","Cyclic"};  // Nr of element models
    byte  cmapRed[],cmapGreen[],cmapBlue[];
    static Vector<Integer> MapSizes = new Vector<Integer>();
    static Vector<byte[]> RedMaps = new Vector<byte[]>(),GreenMaps = new Vector<byte[]>(),BlueMaps = new Vector<byte[]>(); // For user-supplied colormaps
    boolean cmapIsInverse=false;
    IndexColorModel ElementModel;
    boolean ShowOvUn=false;
    boolean LogScale=false;
    double Gamma=1.0;
    final static int MaxCTableBits=15;
    final static int MaxCTable = (2 << (MaxCTableBits-1));
    int cmapcLow=0, cmapcHigh=(2 << (MaxCTableBits-1)); // cmap indices for threshold
        
    boolean DispOverlay=false;    // will be displayed in Multicolor overlay mode
    boolean MulDisplay=false;     // used as multiplicative display
    boolean MIPMode=true;         // is Projection MipMode
    boolean ProjValid[];          // is Projection for this specific direction valid or does it have to be recomputed?
    ROI     ActiveROI;
    RectROI rectROI;
    PolyROI polyROI;
    
    Bundle(int EN, double min, double max)
    {
        ElementModelNr =EN; mincs=min; maxcs=max;
	ProjMincs[0]=min; ProjMaxcs[0]=max;
	ProjMincs[1]=min; ProjMaxcs[1]=max;
        ProjValid = new boolean[3];
        rectROI = new RectROI();
        polyROI = new PolyROI();
        ActiveROI = rectROI;

        Invalidate();
        try {
            GenCMap();
        } catch(Exception e)
	 {
	     System.out.println("Exception creating Bundle\n");
	     e.printStackTrace();
		// System.exit(1);
	 }
        // System.out.println("Created Bundle : "+mincs+", "+maxcs+", Model "+EN+"\n");
    }
    
    void Invalidate() {ProjValid[0] = false;ProjValid[1] = false;ProjValid[2] = false;};
    
    double GetMincs() { return mincs;};
    double GetMaxcs() { return maxcs;};
    void SetMincs(double val) { mincs=val;};
    void SetMaxcs(double val) { maxcs=val;};

    void ToggleROI() {
        if (ActiveROI == rectROI)
            ActiveROI = polyROI;
        else
            ActiveROI = rectROI;
    }

    boolean InOverlayDispl() {return DispOverlay;}
    boolean MulOverlayDispl() {return MulDisplay;}

    public void ToggleOverlayDispl(int val) {
        if (val < 0)
            DispOverlay = ! DispOverlay;
        else if (val == 0)
            DispOverlay = false;
        else if (val == 1)
            DispOverlay = true;            
    }

    public void ToggleMulDispl(int val) {
        if (val < 0)
            MulDisplay= ! MulDisplay;
        else if (val == 0)
            MulDisplay = false;
        else if (val == 1)
	{
            MulDisplay = true;
	    DispOverlay = false; // do not display additive in overlay
	}
    }
    
    public boolean SquareROIs() { return (ActiveROI == rectROI);}

    public void TakeSqrROIs(int [] Pmin, int [] Pmax) {rectROI.TakeSqrROIs(Pmin,Pmax);}
    public void UpdateSqrROI(int ROIX,int ROIY, int ROIXe, int ROIYe,int dir)
    {rectROI.UpdateSqrROI(ROIX,ROIY,ROIXe,ROIYe,dir);}

    public void TakePolyROIs(Polygon []PS) {polyROI.TakePolyROIs(PS);}
    public void TakePlaneROIs(Vector<Integer> [] PlanesS, Vector<Double> [] PlanesD) {return;}  // ignored for now
    
    public Object clone()
    {
        Bundle nb = new Bundle(ElementModelNr,mincs,maxcs);
        nb.MIPMode = MIPMode;
        nb.ShowOvUn= ShowOvUn;
        nb.LogScale=LogScale;
        nb.Gamma=Gamma;
        nb.DispOverlay=DispOverlay;      // will be displayed in Multicolor overlay mode
        // System.out.println("Cloned Bundle : "+mincs+", "+maxcs+", Model "+ElementModelNr+"\n");
        nb.TakeSqrROIs(rectROI.ProjMin,rectROI.ProjMax);
        // nb.MapSizes=MapSizes;nb.RedMaps=RedMaps;nb.GreenMaps=GreenMaps;nb.BlueMaps=BlueMaps;
        return nb;
    }
    
    public static double CClip(double c) {
        return (c < 0.0) ? 0.0 : (c > 1.0) ? 1.0 : c;
    }

    public static double MClip(double c) {  // mirror clip 1 .. 2 -> 1 .. 0
        return (c <= 0.0) ? 0.0 : (c > 1.0) ? (c >= 2.0) ? 0.0 : 2.0-c : c;
    }
        
    public Color GetCMapColor(int pos,int mysize)
    {
    	int cpos = (pos*(cmapcHigh-cmapcLow))/mysize + cmapcLow;
	if (cpos >= MaxCTable) cpos = MaxCTable-1;
	if (cpos < 0) cpos = 0;
	int red = cmapRed[cpos] & 0xff;
	int green = cmapGreen[cpos]& 0xff;
	int blue = cmapBlue[cpos]& 0xff;

	return new Color(red,green,blue);
    }

    public static Color ColFromHue(float tmp)
    {
        return new Color((float) MClip(tmp*1.5+1.0),(float) MClip(tmp*1.5+0.25),(float) MClip(tmp*1.5-0.5));
    }
        
    public void CompCMap() {   // computes the color map of this element
	double tmp;
        double LogGain = (double) MaxCTable;
        double LogCalib = java.lang.Math.log(LogGain+1.0);
	Random myrnd= new Random(0);
        
        for (int i=0;i<MaxCTable;i++)
	    {
                tmp=((double) i-cmapcLow)/(cmapcHigh-cmapcLow);   // clipping here
                if (tmp <= 0 || i == 0) 
                {
                    tmp=0;
                    if (ShowOvUn)  // mark underflow as green
                    {
                     cmapGreen[i]=(byte) 100;
                     cmapRed[i]=(byte) 0;
                     cmapBlue[i]=(byte) 0;
                     continue;
                    }
                }
                
                if (tmp >= 1.0 || i == MaxCTable-1) 
                {
                    tmp=1.0;
                    if (ShowOvUn)  // mark overflow as blue
                    {
                     cmapGreen[i]=(byte) 0;
                     cmapRed[i]=(byte) 0;
                     cmapBlue[i]=(byte) 255;
                     continue;
                    }
                }

        if (Gamma != 1.0)
        	tmp = java.lang.Math.pow(tmp,Gamma);
        
		if (LogScale)
		    tmp = java.lang.Math.log(LogGain*tmp+1)/LogCalib; 

		switch (ElementModelNr) {
			case 0:   // Gray scale
			    cmapRed[i]=(byte) (tmp * 255);
			    cmapGreen[i]=(byte) (tmp * 255);
			    cmapBlue[i]=(byte) (tmp * 255);
			    break;
                        case 1:   // Red 
			    cmapRed[i]=(byte) (tmp * 255);
			    cmapGreen[i]=0;
			    cmapBlue[i]=0;
			    break;
                        case 2:   // Green
			    cmapRed[i]=0;
			    cmapGreen[i]=(byte) (tmp * 255);
			    cmapBlue[i]=0;
			    break;
                        case 3:   // Blue
			    cmapRed[i]=0;
			    cmapGreen[i]=0;
			    cmapBlue[i]=(byte) (tmp * 255);
			    break;
                        case 4:   // Violet
			    cmapRed[i]=(byte) (tmp * 255);
			    cmapGreen[i]=0;
			    cmapBlue[i]=(byte) (tmp * 255);
			    break;
                        case 5:   // Nonlin glow Red
                            cmapRed[i]=(byte) (255*CClip(tmp*3.0)); 
                            cmapGreen[i]=(byte) (255*CClip(tmp*3.0-1.0));
                            cmapBlue[i]=(byte) (255*CClip(tmp*3.0-2.0));
                            break;
                        case 6:   // Nonlin glow Green
                            cmapRed[i]=(byte) (255.0*CClip(tmp*3.0-2.0)); 
                            cmapGreen[i]=(byte) (255.0*CClip(tmp*3.0));
                            cmapBlue[i]=(byte) (255.0*CClip(tmp*3.0-1.0));	
                            break;
                        case 7:  // Nonlin glow Green-Yellow
                            cmapRed[i]=(byte) (255*CClip(tmp*3.0-1.0)); 
                            cmapGreen[i]=(byte) (255*CClip(tmp*3.0));
                            cmapBlue[i]=(byte) (255*CClip(tmp*3.0-2.0));
                            break;
                        case 8:   // Nonlin glow Blue
                            cmapBlue[i]=(byte) (255*CClip(tmp*3.0)); 
                            cmapGreen[i]=(byte) (255*CClip(tmp*3.0-1.0));
                            cmapRed[i]=(byte) (255*CClip(tmp*3.0-2.0));
                            break;
                        case 9: // Nonlin glow Violet
                            cmapRed[i]=(byte) (255*CClip(tmp*2.0)); 
                            cmapGreen[i]=(byte) (255*CClip(tmp*2.0-1.0));
                            cmapBlue[i]=(byte) (255*CClip(tmp*2.0));
                            break;
                        case 10: // Rainbow
                            cmapRed[i]=(byte) (255*MClip(tmp*1.5+1.0)); 
                            cmapGreen[i]=(byte) (255*MClip(tmp*1.5+0.25));
                            cmapBlue[i]=(byte) (255*MClip(tmp*1.5-0.5));
                            if (i==0)
                            {
                            cmapRed[i]=(byte) (0); 
                            cmapGreen[i]=(byte) (0);
                            cmapBlue[i]=(byte) (0);
                            }
                            break;
                        case 11:   // Random Colors
                            if (i==0)
                            {
                            cmapRed[i]=(byte) (0); 
                            cmapGreen[i]=(byte) (0);
                            cmapBlue[i]=(byte) (0);
                            }
                            else
                            {
                            cmapRed[i]=(byte) (255.0 * myrnd.nextDouble()); 
                            cmapGreen[i]=(byte) (255.0 * myrnd.nextDouble());
                            cmapBlue[i]=(byte) (255.0 * myrnd.nextDouble());
                            }
                            break;
                        case 12:   // Random Colors
                            if (tmp < 1/3.0)
				{
                            cmapRed[i]=(byte) (255*MClip(1-tmp*3)); 
                            cmapGreen[i]=(byte) (255*MClip(tmp*3));
                            cmapBlue[i]=(byte) 0;
				}
                            if (tmp>= 1/3.0 && tmp < 2/3.0)
				{
                            cmapRed[i]=(byte) 0; 
                            cmapGreen[i]=(byte) (255*MClip(1-(tmp-1/3.0)*3));
                            cmapBlue[i]=(byte) (255*MClip((tmp-1/3.0)*3)); 
				}
                            if (tmp>= 2/3.0)
				{
                            cmapRed[i]=(byte) (255*MClip((tmp-2/3.0)*3)); 
                            cmapGreen[i]=(byte) 0;
                            cmapBlue[i]=(byte) (255*MClip(1-(tmp-2/3.0)*3));
				}
                            if (i==0)
                            {
                            cmapRed[i]=(byte) (0); 
                            cmapGreen[i]=(byte) (0);
                            cmapBlue[i]=(byte) (0);
                            }
                            break;
                        default:  // a user supplied colormap
                            int elem=ElementModelNr-ElementModels;
                            int MapSize=MapSizes.elementAt(elem).intValue();
                            //int MapSize=((byte []) RedMaps.elementAt(elem)).length;
                            int index=(int) ((MapSize-1)*tmp);
                            cmapRed[i]=RedMaps.elementAt(elem)[index];
                            cmapGreen[i]=GreenMaps.elementAt(elem)[index];
                            cmapBlue[i]=BlueMaps.elementAt(elem)[index];
		    }
		if (cmapIsInverse)
		{
			cmapRed[i] = (byte) (255-cmapRed[i]);
			cmapGreen[i] = (byte) (255-cmapGreen[i]);
			cmapBlue[i] = (byte) (255-cmapBlue[i]);
		}
	    }
        
        
	ElementModel = new IndexColorModel (MaxCTableBits,MaxCTable,cmapRed,cmapGreen,cmapBlue);  // n bit model

        //Invalidate();
    }

    public void GenCMap() {
	cmapRed = new byte[MaxCTable];
	cmapGreen = new byte[MaxCTable];
	cmapBlue = new byte[MaxCTable];
	CompCMap();
    }

    public static int AddLookUpTable(int TableSize, byte reds[], byte greens[], byte blues[])
    {
        MapSizes.addElement(new Integer(TableSize));
        RedMaps.addElement(reds);
        GreenMaps.addElement(greens);
        BlueMaps.addElement(blues);
        return MapSizes.size()-1;   // returns the index of the last user-model
    }
    
    public void ToggleModel(int actModel) {
	// int oldmodel = ElementModelNr;
        if (actModel < 0)
            ElementModelNr = ElementModelNr+1;
	else ElementModelNr = actModel;
        
        if (ElementModelNr >= ElementModels+MapSizes.size()) ElementModelNr=0;
	// if (oldmodel != ElementModelNr)   // since a colormap can be inv
        CompCMap();
    }

   public boolean ToggleOvUn(int newVal) {
	if (newVal < 0)
	    ShowOvUn = ! ShowOvUn;
	else
	    ShowOvUn = (newVal == 1);
	CompCMap();
        return ShowOvUn;
   }
	
   public void SetGamma(double newVal) {
	   double oldGamma=Gamma;
	   Gamma=newVal;
	   if (Gamma != oldGamma)
		   CompCMap();
   }

   public double GetGamma() {
		return Gamma;
	    }
   
public void ToggleLog(int newVal) {
	if (newVal < 0)
	    LogScale = ! LogScale;
	else
	    LogScale = (newVal == 1);
	CompCMap();
   }
	
}
