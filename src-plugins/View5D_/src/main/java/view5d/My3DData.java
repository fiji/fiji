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

import java.io.*;
import java.net.*;
import java.awt.image.*;
import java.applet.Applet;
import java.awt.*;
import java.util.*;
import java.text.*;
import ij.*;
import ij.gui.*;

public class My3DData extends Object {
    public String markerInfilename=null;
    public String markerOutfilename=null;
    public Vector<AnElement> MyElements;    // this stores all the data
    
    public Vector<ASlice> MyProjections[];    // these manage projections
    public ASlice MyColorProjection[];    // this manage color projections

    public ASlice MySlice[];    // this manage the ZY, XZ and XY slices
    public ASlice MyColorSlice[];    // this manage the ZY, XZ and XY slices

    public MarkerLists MyMarkers;
    boolean ConnectionShown=true; // Defines, whether a connection-line is drawn between successive markers
    boolean ShowFullTrace=true; // Defines, whether all connection-lines are drawn between successive markers
    boolean ShowAllLists=true; 
    //boolean ShowAllSlices=true; // if false, only a marker in the current slice is shown
    boolean ShowAllTrees=true; // if false, only a marker in the current slice is shown
    boolean ShowAllTrack=false;  // if false, only a marker in the correct time is shown
    boolean Annotate=false;  // if false, only a marker in the correct time is shown
    boolean Advance=false;  // if false, only a marker in the correct time is shown
    boolean ShowSpectralTrack=false;  // if true, the traces are displayed as spectrum (from red to blue).
    boolean MarkerToMax=true;  // determines whether markers are "sucked to the maximum position"
    boolean UseCOI=true;  // determines whether center of intensity is used
    int SearchX=3,SearchY=3,SearchZ=0;
    int COMX=3,COMY=3,COMZ=0;
    String [] TrackDirections={"X","Y","Z","Elements","Time"};
    int TrackDirection=2;  // by default track along Z
    String [] TrackModes={"Max","Min"};
    int TrackMode=0;  // by default track Maxima
    String [] FinishModes={"None","Freeze", "Stop"};
    int    FinishMode=2;  // by default continue until relative threshold is reached
    String [] FinishMeasures={"Max","Integral","Integral Above Min"};
    int    FinishMeasure=1;  // by default continue until relative threshold is reached
    double ThreshValue=0.2;
    double TotalMax,TotalMin,TotalInt,TotalIntDiff;  // Used as variables when running thought the current MarkerList

    boolean Repulsion=false;
    boolean FocusDispToMarker=true;
    boolean AppendVersionNumber=false;
    boolean didSwitchAppendVersionNumber = false;
    boolean ActioReactio=false;
    double FWHM=4.0;

    public int SizeX=0,SizeY=0,SizeZ=0,Elements=0,ActiveElement=0;
    public int DimensionOrder=0;  // defines the order of the dimensions to read in
    public int AppendTo=0;  // defines the dimension to wich to append the extra loaded data

    public Vector<Vector<AnElement>> MyTimes;
    public Vector<Vector<ASlice>[]> MyTimeProj;    // this stores all the different multi element projection data as a vector of vectors
    public int Times=0,ActiveTime=0;
    public Vector<ASlice[]>  MyTimeColorProj; // a vector of an array of ASlice
//    public Vector TimeValues;    // keeps track of the exact time points
    
    My3DData MyHistogram=null;
    My3DData DataToHistogram=null;
    
    int  sizes[]= {0,0,0,0,0};  // x,y,z,elements,time

    int PrevType,PrevBytes, PrevBits; // Just stores the previously used Bytes and Bits values for reload to function
        
    int HistoX=0, HistoY=-1, HistoZ=-1;  // -1 means no histgram is computed along this dimension
    
    int ProjMin[],ProjMax[];       // Square ROIs
    Vector<Integer> PlanesS[];
    Vector<Double> PlanesD[];
    Polygon ROIPolygons[];
    
    boolean ProjMode[];
    
    boolean projinit=false;
    boolean cprojinit=false;
    
    int  elemR=0;  // defines which element is red
    int  elemG=0;  // defines which element is green
    int  elemB=0;  // defines which element is blue
    int  GateElem=0;  // if defined, this element will be used for gating (only color display)
    boolean GateActive=false;  // just saves the position where the gate was placed last, to be able to toggle it
    boolean colormode=false;

    public Container  applet;

    Vector<Bundle> MyBundle;    // containes maxcs, mincs, ElementModelNr;

    void AddPoint(APoint p) {MyMarkers.AddPoint(p);}
    void AddPoint(double x, double y, double z, double e, double t) {AddPoint(new APoint(x,y,z,e,t));}
    void RemovePoint() {MyMarkers.RemovePoint();}
    void RemoveTrailingPoints() {MyMarkers.RemoveTrailingPoints();}
    boolean AdvancePoint(int howmany) {return MyMarkers.AdvancePoint(howmany,TrackDirection);} // cave: can change the lists
    APoint GetPoint(int p,int list) {return MyMarkers.GetPoint(p,list);}
    APoint GetPoint(int p) {return MyMarkers.GetPoint(p,-1);}
    int NumMarkers(int lpos) {return MyMarkers.NumMarkers(lpos);}
    int ActiveMarkerPos() {return MyMarkers.ActiveMarkerPos();}
    int GetActMarkerListNr() {return MyMarkers.GetActiveList();}
    int GetPrefChildListNr(int alist) {return MyMarkers.GetPrefChildListNr(alist);}
    int GetFirstAncestorNr(int alist) {return MyMarkers.GetFirstAncestorNr(alist);}
    boolean CommonRoot(int list1,int list2) {return MyMarkers.CommonRoot(list1,list2);}
    void SetActiveMarker(int p) {MyMarkers.SetActiveMarker(p);}
    void SetActiveMarker(APoint p) {MyMarkers.SetActiveMarker(p);}
    boolean HasParent1(int list) {return MyMarkers.HasParent1(list);}
    boolean HasParent2(int list) {return MyMarkers.HasParent2(list);}
    APoint GetParent1EndOfTrack(int list) {return MyMarkers.GetParent1EndOfTrack(list,TrackDirection);}
    APoint GetParent2EndOfTrack(int list) {return MyMarkers.GetParent2EndOfTrack(list,TrackDirection);}

    
    APoint GetActiveMarker() {return GetPoint(-1);}

    boolean CheckActiveMarker(int list, int numMarker, int px, int py, int pz, int element, int time) 
    {
    	int mycoord[] = {px,py,pz,element,time};
    	
      if (list == ActiveMarkerListPos() || 
    	  list == MyMarkers.GetParent1Index(MyMarkers.ActiveList) || 
    	  list == MyMarkers.GetParent2Index(MyMarkers.ActiveList) || 
    	  ((MyMarkers.MyActiveList.PreferredChild==1) && (list == MyMarkers.GetChild1Index(MyMarkers.ActiveList))) || 
    	  ((MyMarkers.MyActiveList.PreferredChild==2) && (list == MyMarkers.GetChild2Index(MyMarkers.ActiveList))))
      {
    	  if (GetPoint(MyMarkers.ActiveMarkerPos(),MyMarkers.ActiveList).coord[TrackDirection] == mycoord[TrackDirection]) // active marker is in this hyperplane
    		  if (list==MyMarkers.ActiveList && numMarker == MyMarkers.ActiveMarkerPos())
    			  return true;
    		  else
    			  return false;
    	  // ActiveMarker is in a different hyperplane but this marker can be made active
    	  //System.out.println("CheckingActive Pref. "+ MyMarkers.MyActiveList.PreferredChild+"\n");
    	  if (GetPoint(numMarker,list).coord[TrackDirection] == mycoord[TrackDirection]) 
    	  	{MyMarkers.SetActiveList(list);SetActiveMarker(numMarker); return true;}
    	  
      }
      return false;
    }
    
    int NumMarkerLists() {return MyMarkers.NumMarkerLists();}
    int ActiveMarkerListPos() {return MyMarkers.ActiveMarkerListPos();}
    String GetMarkerPrintout (My3DData data3d) {return MyMarkers.PrintList(data3d)+MyMarkers.PrintSummary(data3d);}
    public void NewMarkerList() {MyMarkers.NewList();}
    public void NewMarkerList(int linkTo, String NameExtension) {MyMarkers.NewList(linkTo,NameExtension);}

    void DevideMarkerList(double px, double py, double pz) {
    int currentList = ActiveMarkerListPos();
	APoint sam =GetActiveMarker();
	boolean amIsDisplayed=true; //am.isDisplayed;
	//System.out.println("current List is" + currentList + "\n"); 
    	if (currentList >= 0)   // else ignore this command as user cannot split a non-existing trace
    	{
    		MarkerList oldChild1=MyMarkers.GetMarkerList(currentList).Child1List;
    		MarkerList oldChild2=MyMarkers.GetMarkerList(currentList).Child2List;

    		NewMarkerList(currentList,"a");  // first new list
    		int newListNr = ActiveMarkerListPos();

    		MyMarkers.SetActiveList(currentList);
    		MyMarkers.SetActiveMarker(sam);
    		
    		if (amIsDisplayed)  // split the list
    		{
    			do {
        			APoint am = GetActiveMarker();
        			MyMarkers.SetActiveList(newListNr);
        			AddPoint(am);
        			MyMarkers.SetActiveList(currentList);
            		MyMarkers.SetActiveMarker(am);
        			//RemovePoint();
    			} while (AdvancePoint(1) && ActiveMarkerListPos() == currentList);

    			MyMarkers.SetActiveList(currentList);
        		MyMarkers.SetActiveMarker(sam);
    			RemoveTrailingPoints();
    			
    			if (oldChild1 != null)
    				{
    				    if (oldChild1.Parent1List == oldChild1)
    				    	oldChild1.Parent1List = MyMarkers.GetMarkerList(newListNr);
    				    if (oldChild1.Parent2List == oldChild1)
    				    	oldChild1.Parent2List = MyMarkers.GetMarkerList(newListNr);
    					MyMarkers.GetMarkerList(newListNr).Child1List = oldChild1;
    				}
    			if (oldChild2 != null)
    				{
				    if (oldChild2.Parent1List == oldChild2)
    					oldChild2.Parent1List = MyMarkers.GetMarkerList(newListNr);
				    if (oldChild2.Parent2List == oldChild2)
    					oldChild2.Parent2List = MyMarkers.GetMarkerList(newListNr);
    					MyMarkers.GetMarkerList(newListNr).Child2List = oldChild2;
    				}
    			MyMarkers.SetActiveList(currentList);
    		}
    		else 
    			SetMarker(px,py,pz,MyMarkers.GetMarkerList(currentList).GetColor());
    		NewMarkerList(currentList,"b");  // second new list
    		SetMarker(px,py,pz,MyMarkers.GetMarkerList(currentList).GetColor());
    	}
    }

    void RemoveMarkerList() {MyMarkers.RemoveList();}
    void AdvanceMarkerList(int howmany) {MyMarkers.AdvanceList(howmany,TrackDirection);}
    void ToggleMarkerListColor(int howmany) {MyMarkers.ToggleColor();}
    String GetMarkerListName(int listnr)    {return MyMarkers.GetMarkerList(listnr).GetMarkerListName();}

    APoint MarkerFromPosition(double px, double py, int dir, double dx, double dy)  // , float [] positions
        {
    	  // APoint Pt = MyMarkers.MarkerFromPosition(px,py,dir,dx,dy,ShowAllLists,ShowAllSlices,ShowAllTrack,TrackDirection, (int) (positions[TrackDirection] + 0.5));
    	  APoint Pt = MyMarkers.MarkerFromPosition(px,py,dir,dx,dy,this);
          return Pt;
    	}

    public int AddLookUpTable(int MapSize,byte Reds[],byte Greens[],byte Blues[])
    { 
      int lastLUT=0;
      lastLUT=Bundle.ElementModels+Bundle.AddLookUpTable(MapSize,Reds,Greens,Blues); 
      SetColorModelNr (ActiveElement,lastLUT);
      GetBundleAt(ActiveElement).CompCMap();
      return lastLUT;  // returns the last existing total LUT index
    }

    void cleanup() // tries to free memomory
    {
    	if (MyElements == null) return;
        for (int e=0; e<Elements;e++)
        {
            for (int t=0; t<Times;t++)
            {
            	ElementAt(e,t).DataToHistogramX= null;
            	ElementAt(e,t).DataToHistogramY= null;
            	ElementAt(e,t).DataToHistogramZ= null;
            	ElementAt(e,t).DeleteData();
        	}
            GetBundleAt(e).cmapRed=null;
            GetBundleAt(e).cmapGreen=null;
            GetBundleAt(e).cmapBlue=null;
            // GetBundleAt(e)=null;
            ProjAt(0,e).my1DProjVoxels=null;
            ProjAt(0,e).mySlice=null;
            //ProjAt(0,e)=null;
            ProjAt(1,e).my1DProjVoxels=null;
            ProjAt(1,e).mySlice=null;
            //ProjAt(1,e)=null;
            ProjAt(2,e).my1DProjVoxels=null;
            ProjAt(2,e).mySlice=null;
            //ProjAt(2,e)=null;
        }
       MyElements=null;
       MyTimes=null;
       MyProjections=null;
       MyColorProjection=null;
       MySlice=null;
       MyColorSlice=null;
   	   // System.out.println("cleanup\n");
       System.gc();
    }
    
    void SetMarker(double x, double y, double z, double e, double t)
    {
        APoint p=new APoint(x,y,z,e,t);
        if (MarkerToMax) 
           p=IterativeClosestMax(p, SearchX,SearchY,SearchZ);
        ClippedCOI(p,COMX,COMY,COMZ,UseCOI);
        AddPoint(p);		
    }
    
    void SetMarker(double x, double y, double z)
    {
        SetMarker(x,y,z,ActiveElement,ActiveTime);
    }
    
    void SetMarker(double x, double y, double z, int mycolor)
    {
    	SetMarker(x,y,z);
    	GetActiveMarker().mycolor = mycolor;
    }

    void TagMarker()
    {
    	GetActiveMarker().Tag(-1);
    }

    void ResetOffsets() {
    	for (int pt=0;pt<Times;pt++)
    		for (int pe=0;pe<Elements;pe++)
    		{
    			ElementAt((int) pe,(int) pt).DisplayOffset[0] = 0;
    			ElementAt((int) pe,(int) pt).DisplayOffset[1] = 0;
    			ElementAt((int) pe,(int) pt).DisplayOffset[2] = 0;
    		}    	
    }

    void AlignOffsetsToTrack() {
    	if (TrackDirection < 3) 
    		{
    			System.out.println("WARNING: Attempted to align Data to a marker trace even though the current tracking direction is not elements or time! Aborted.\n");
    			return; // no alignment
    		}
    	
        APoint APt=GetPoint(-1);  // retrieves the active point in the active List
        if (APt == null) return; // no idea where to start the tracking
        int PNr = ActiveMarkerPos();
        double x0=APt.coord[0];
        double y0=APt.coord[1];
        double z0=APt.coord[2];
        int alist = GetActMarkerListNr();
        APoint Pt=GetPoint(PNr);

        // APoint prevPt = APt;
        // double stepcoord=Pt.coord[TrackDirection];
        double preve=Pt.coord[3], prevt=Pt.coord[4],pe,pt,emin,emax;
        double PrevOffsetX = ElementAt((int) preve,(int) prevt).DisplayOffset[0];
        double PrevOffsetY = ElementAt((int) preve,(int) prevt).DisplayOffset[1];
        double PrevOffsetZ = ElementAt((int) preve,(int) prevt).DisplayOffset[2];        

        alist = GetFirstAncestorNr(alist);  // also sets this as the preferred child
		//System.out.println("Ancestor " + alist +"\n");
        
        do {
        int NumM = NumMarkers(alist);
        for (PNr=0;PNr<NumM;PNr++)  // This updates the allready present markers
            {
              Pt=GetPoint(PNr,alist);
              pe=Pt.coord[3]; pt=Pt.coord[4];
              if (TrackDirection != 4)  // Not time so it is elements
            	  {emin=pe;emax=pe;}
              else
            	  {emin=0;emax=Elements-1;}
            		  
              for (pe=emin;pe<=emax;pe++)
              {
              ElementAt((int) pe,(int) pt).DisplayOffset[0] = (int) (PrevOffsetX - Pt.coord[0] + x0);
              ElementAt((int) pe,(int) pt).DisplayOffset[1] = (int) (PrevOffsetY - Pt.coord[1] + y0);
              ElementAt((int) pe,(int) pt).DisplayOffset[2] = (int) (PrevOffsetZ - Pt.coord[2] + z0);
              }
      		//System.out.println("Aligned point " + PNr +" of " + NumM +"\n");
            } 
        alist= GetPrefChildListNr(alist);
		//System.out.println("Moved to child" + alist +"\n");
        } while (alist >= 0);
        //SetActiveList(alist);
        //SetActiveMarker(APt);
    }

    int TestValidPoint(APoint Pt)
    {
    	int TrackState=0;
    	TotalMax=Math.max(TotalMax,Pt.max);
    	TotalMin=Math.min(TotalMin,Pt.min);
    	TotalInt=Math.max(TotalInt,Pt.integral);
    	TotalIntDiff=Math.max(TotalIntDiff,Pt.integralAboveMin);
    	double myMeasure=0;
    	switch (FinishMeasure)  // {"Max","Integral","Integral Above Min"}
    	{
    	case 0:
    		myMeasure = Pt.max/TotalMax;
			break;
    	case 1:
    		myMeasure = Pt.integral/TotalInt;
			break;
    	case 2:            
    		myMeasure = Pt.integralAboveMin/TotalIntDiff;
			break;
    	}

    	if (myMeasure < ThreshValue)
    	{
    		switch (FinishMode)
    		{
    		case 0: // None  
    			TrackState=0;  // Go on tracking
    			break;
    		case 1:
    			TrackState=1;  // freeze
    			break;
    		case 2:
    			TrackState=2;  // stop tracking
    			break;
    		}
    	}
    	return TrackState;
    }

    void AutoTrack()  // will start at the current marker and track the spot through the stack
    {
        APoint Pt=GetPoint(-1);  // retrieves the active point in the active List
        if (Pt == null) return; // no idea where to start the tracking
        int PNr = ActiveMarkerPos();
        //int NumM = NumMarkers(-1);
        //APoint prevPt = Pt;
        //double stepcoord=Pt.coord[TrackDirection];
        double preve=Pt.coord[3], prevt=Pt.coord[4],pe,pt;
        TotalMax=Pt.max;
        TotalMin=Pt.min;
        TotalInt=Pt.integral;
        TotalIntDiff=Pt.integralAboveMin;
        int myP;
        for (myP=1;myP<=PNr;myP++)  // To determine the spot statistics so far
        {
            TestValidPoint(GetPoint(PNr)); // Just to fill the statistics
        }
        
        int TrackState=0;  // 1 means frozen, 2 means track ended
        double px=Pt.coord[0],py=Pt.coord[1],pz=Pt.coord[2];pe=Pt.coord[3];pt=Pt.coord[4];
        for (;(NumMarkers(-1) > 0)&&(ActiveMarkerPos() >= 0) && (ActiveMarkerPos()<NumMarkers(-1)-1) && (TrackState < 2);)  // This updates the already present markers
            {
        	  AdvancePoint(1);
              Pt=GetPoint(-1);  // retrieves the active point in the active List              
              //SetActiveMarker(PNr);
              //Pt=GetPoint(PNr);
              //stepcoord=Pt.coord[TrackDirection];
              px=Pt.coord[0];py=Pt.coord[1];pz=Pt.coord[2];pe=Pt.coord[3];pt=Pt.coord[4];
        	  px = px - ElementAt((int) pe,(int) pt).DisplayOffset[0] + ElementAt((int) preve,(int) prevt).DisplayOffset[0];
        	  py = py - ElementAt((int) pe,(int) pt).DisplayOffset[1] + ElementAt((int) preve,(int) prevt).DisplayOffset[1];
        	  pz = pz - ElementAt((int) pe,(int) pt).DisplayOffset[2] + ElementAt((int) preve,(int) prevt).DisplayOffset[2];
        	  UpdateMarker(Pt);
        	  //RemovePoint();
              //SetMarker(px,py,pz,pe,pt); // This will determine the positions
              Pt=GetPoint(-1);  // retrieves the active point in the active List              

              // System.out.println("... updating marker nr. "+PNr+"\n");
//              Pt.copy(prevPt); // use old point's coordinates
//              px=Pt.coord[0];py=Pt.coord[1];pz=Pt.coord[2];pe=Pt.coord[3];pt=Pt.coord[4];
//              Pt.coord[TrackDirection]=stepcoord; // restore track position to original
//
//              Pt.coord[0] = px - ElementAt((int) pe,(int) pt).DisplayOffset[0] + ElementAt((int) preve,(int) prevt).DisplayOffset[0];
//              Pt.coord[1] = py - ElementAt((int) pe,(int) pt).DisplayOffset[1] + ElementAt((int) preve,(int) prevt).DisplayOffset[1];
//              Pt.coord[2] = pz - ElementAt((int) pe,(int) pt).DisplayOffset[2] + ElementAt((int) preve,(int) prevt).DisplayOffset[2];
              //Pt.copy(prevPt);
              //Pt.coord[TrackDirection] = backup.coord[TrackDirection];  // preserve the coordinae along track direction
//              UpdateMarker(Pt);  // Track with prevPt starting positions
              px=Pt.coord[0];py=Pt.coord[1];pz=Pt.coord[2];pe=Pt.coord[3];pt=Pt.coord[4];
              TrackState=TestValidPoint(Pt);
              if (TrackState > 0)
              {
                  System.out.println("Marker frozen@ "+PNr+ "\n");
            	  RemovePoint();
                  Pt=GetPoint(-1);  // retrieves the active point in the active List              
                  px=Pt.coord[0];py=Pt.coord[1];pz=Pt.coord[2];pe=Pt.coord[3];pt=Pt.coord[4];
              } 
              if (TrackState >= 2)
              {
                  System.out.println("Track ended@ "+PNr+ "\n");                	  
            	  RemoveTrailingPoints();
            	  return;
              }
              preve=pe; prevt=pt;
              //prevPt=Pt;
              PNr = ActiveMarkerPos();
              System.out.println("Active Marker: "+PNr+ ", "+NumMarkers(-1)+"\n");
              System.out.println("pxyz: "+px+ ", "+py+ ", "+pz+ "\n");
              //px=Pt.coord[0];py=Pt.coord[1];pz=Pt.coord[2];pe=Pt.coord[3];pt=Pt.coord[4];
            }
        int TrackSize=0,ix=0,iy=0,iz=0,ie=0,it=0,tpos=0;  // increments during tracking
        //double px=Pt.coord[0],py=Pt.coord[1],pz=Pt.coord[2];pe=Pt.coord[3];pt=Pt.coord[4];
        
        switch(TrackDirection)
        {
            case 0: TrackSize=SizeX;ix=1;tpos=(int) px+1;break;
            case 1: TrackSize=SizeY;iy=1;tpos=(int) py+1;break;
            case 2: TrackSize=SizeZ;iz=1;tpos=(int) pz+1;break;
            case 3: TrackSize=Elements;ie=1;tpos=(int) pe+1;break;
            case 4: TrackSize=Times;it=1;tpos=(int) pt+1;break;
        }

        preve=pe; prevt=pt;
        px+=ix;py+=iy;pz+=iz;pe+=ie;pt+=it;  // advance one
        for (;(tpos < TrackSize) & (TrackState < 2);tpos++)  // continue until the end in trackingDirection is reached
            {
              System.out.println("Extend Active MarkerPos: "+ActiveMarkerPos()+"\n");
              //System.out.println("AutoTrack at Pt:"+PNr+", coords:"+px+", "+py+", "+pz);
              //System.out.println("... creating marker\n");
        	  px = px - ElementAt((int) pe,(int) pt).DisplayOffset[0] + ElementAt((int) preve,(int) prevt).DisplayOffset[0];
        	  py = py - ElementAt((int) pe,(int) pt).DisplayOffset[1] + ElementAt((int) preve,(int) prevt).DisplayOffset[1];
        	  pz = pz - ElementAt((int) pe,(int) pt).DisplayOffset[2] + ElementAt((int) preve,(int) prevt).DisplayOffset[2];
              SetMarker(px,py,pz,pe,pt); // This will determine the positions
              Pt=GetPoint(-1);  // retrieves the active point in the active List
              px=Pt.coord[0];py=Pt.coord[1];pz=Pt.coord[2];pe=Pt.coord[3];
              // Pt=GetPoint(PNr);
              
              TrackState=TestValidPoint(Pt);
              if (TrackState > 0)
              {
                  System.out.println("Marker frozen@ "+px+", "+py+", "+pz+ "\n");
                  if (TrackState > 1)
                  {
                      System.out.println("Track ended@ "+px+", "+py+", "+pz+ "\n");                	  
                  }
            	  RemovePoint();
              }
              preve=pe; prevt=pt;
              px+=ix;py+=iy;pz+=iz;pe+=ie;pt+=it;  // advance one
            }
    }
    
    void SubtractTrackedSpot()   // Subtracts a Gaussian with appropriate intensity from the image
    {
        APoint Pt;
        int PNr = ActiveMarkerPos();
        int NumM = NumMarkers(-1);
        for (;PNr<NumM;PNr++)  // This updates the allready present markers
            {
              SetActiveMarker(PNr);
              Pt=GetPoint(PNr);
              ElementAt((int)Pt.coord[3],(int)Pt.coord[4]).SubtractGauss(Pt.coord[0],Pt.coord[1],Pt.coord[2],Pt.integral,COMX,COMY,COMZ,FWHM);
            }
    }

    void UpdateMarker(APoint Pt)
    {
        if (MarkerToMax)
        {
            APoint p= (APoint) Pt.clone();
            APoint q=IterativeClosestMax(p,SearchX,SearchY,SearchZ);
            Pt.copy(q);
        }
        ClippedCOI(Pt,COMX,COMY,COMZ,UseCOI);
    }
    
    void ToggleMarkerToMax(int val)
    {
        if (val <0)
            MarkerToMax = ! MarkerToMax;
        else
            if (val == 0)
                MarkerToMax = false;
            else
                MarkerToMax = true;
    }

    void Penalize(APoint aPt) // penalize a specified pixel
    {
        if (Repulsion)
            MyMarkers.Penalize(aPt,FWHM,TrackDirection);
    }
    
    double Penalty(APoint aPt) // computes the repulsion penalty for a specified pixel
    {
        if (Repulsion)
            return MyMarkers.Penalty(aPt,FWHM,TrackDirection,AnElement.ComputeIntMaxScale(COMX,COMY,COMZ,FWHM));
        else return 0.0;
    }
    
    double Restrict(double val, int limit)
    {
    	if (val >= limit) return limit-1;
	if (val < 0) return 0;
	return val;
    }
    
    APoint LimitPoint(APoint p)
    {
    	APoint np= (APoint) p.clone();
	for (int i=0; i < 5; i++)
		np.coord[i]=Restrict(p.coord[i],sizes[i]);
	return np;
    }
    
    APoint ClosestMax(APoint pt, int dx, int dy, int dz)  // finds the maximum point in an area
    {
        int x=(int) (pt.coord[0]+0.5), y=(int) (pt.coord[1]+0.5), z=(int) (pt.coord[2]+0.5), 
            e=(int) (pt.coord[3]+0.5), t=(int) (pt.coord[4]+0.5);
	boolean didnegate=false;
	if (TrackMode == 1) // Minimum Mode
	{
		didnegate=true;
		ElementAt(e,t).ScaleV = -ElementAt(e,t).ScaleV;  // Temporarily negate the values during tracking
	}
        double val,maxv=ElementAt(e,t).GetValueWithBounds(x,y,z),px=x,py=y,pz=z;  // if values are equal do not slide, use old value!
        //APoint p= new APoint(x-dx,y-dy,z-dz,e,t);
        APoint p= (APoint) pt.clone();
        maxv -= Penalty(p);
        for (int zz=z-dz; zz<=z+dz;zz++)
        {
            p.coord[2] = zz;
            for (int yy=y-dy; yy<=y+dy;yy++)
            {
                    p.coord[1] = yy;
                for (int xx=x-dx; xx<=x+dx;xx++)
                {
                    p.coord[0] = xx;
                    val=ElementAt(e,t).GetValueWithBounds(xx,yy,zz);
                    if (val > maxv)
                    {
                        val -= Penalty(p);  // subtract Gaussian intensity
                        if (val > maxv)
                        {
                            maxv=val; px=xx;py=yy;pz=zz;
                        }
                    }
                }
            }
        }
        p.coord[0] = px;
        p.coord[1] = py;
        p.coord[2] = pz;
        p.max=maxv;  // save the maximum
        // Penalize(p); // apply the penalty if necessary
	if (didnegate) // was done in Minimum Mode
		ElementAt(e,t).ScaleV = -ElementAt(e,t).ScaleV;  // re-negate the values to be normal again
        return p;
    }
    
    APoint IterativeClosestMax(APoint p, int dx, int dy, int dz)  // keeps searching with updates
    {
        APoint p1 = (APoint) p.clone();
        APoint p2 = ClosestMax(p,dx,dy,dz);
        int iter=0; // ,MaxIter=10;
        
        while (p1.SqrDistTo(p2) > 0.1) // .1 && iter < MaxIter)  //
        {
            //System.out.println("p1 at:"+p1.coord[0]+", "+p1.coord[1]+", "+p1.coord[2]+", "+p1.coord[4]+"\n");
            p1=p2;
            p2 = ClosestMax(p1,dx,dy,dz);
            //System.out.println("p2 at:"+p2.coord[0]+", "+p2.coord[1]+", "+p2.coord[2]+", "+p2.coord[4]+"\n");
            iter++;
        }
        p2.mycolor=p.mycolor;
        return p2;
    }

    void ConstrainPoint(APoint p)  // constrains the point to the data sizes
    {
        if (p.coord[0] > SizeX-1) p.coord[0]=SizeX-1.0;
        if (p.coord[0] < 0) p.coord[0]=0.0;
        if (p.coord[1] > SizeY-1) p.coord[1]=SizeY-1.0;
        if (p.coord[1] < 0) p.coord[1]=0.0;
        if (p.coord[2] > SizeZ-1) p.coord[2]=SizeZ-1.0;
        if (p.coord[2] < 0) p.coord[2]=0.0;
        if (p.coord[3] > Elements-1) p.coord[3]=Elements-1.0;
        if (p.coord[3] < 0) p.coord[3]=0.0;
        if (p.coord[4] > Times-1) p.coord[4]=Times-1.0;
        if (p.coord[4] < 0) p.coord[4]=0.0;      
    }

    void ClippedCOI(APoint p, int dx, int dy, int dz, boolean doCOI)  // computes the clipped center of intensity (with background subtraction
    {
        int x=(int) (p.coord[0]+0.5);
        int y=(int) (p.coord[1]+0.5);
        int z=(int) (p.coord[2]+0.5);
        int e=(int) (p.coord[3]+0.5);
        int t=(int) (p.coord[4]+0.5);

	boolean didnegate=false;
	if (TrackMode == 1) // Minimum Mode
	{
		didnegate=true;
		ElementAt(e,t).ScaleV = -ElementAt(e,t).ScaleV;  // Temporarily negate the values during tracking
	}
        double maxv=ElementAt(e,t).GetValueWithBounds(x,y,z);
        double val,sum=0,sx=0,sy=0,sz=0,minv=maxv;
        double nump=0; 
	AnElement GateElement=GetGateElem(t);
        
        for (int zz=z-dz; zz<=z+dz;zz++)
            for (int yy=y-dy; yy<=y+dy;yy++)
                for (int xx=x-dx; xx<=x+dx;xx++)
                  if (GateElement.GetIntValueWithBounds(xx,yy,zz) > 0)
		    if (ElementAt(e,t).InsideBounds(xx,yy,zz))
                {
                    val=ElementAt(e,t).GetValueAt(xx,yy,zz);  // GetValueWithBounds
                    //val -= Penalty(p);  // subtract Gaussian intensity
                    sx += val*xx;
                    sy += val*yy;
                    sz += val*zz;
                    sum+=val;
                    if (val < minv)
                    {
                        minv=val;
                    }
                    if (val > maxv)
                    {
                        maxv=val;
                    }
                    nump += 1.0;
                }
        if (minv >= maxv) minv = 0.0;  // revert to center of area
        p.integral=sum;  // save the total integral
        p.max=maxv;  // save the integral
        p.min=minv;
        sum -= nump*minv;
        p.integralAboveMin=sum;
        if (sum == 0.0) sum = 1.0;
        if (nump == 0.0) return;  // do not correct anything
        
        double px = (sx-minv*nump*x) / sum,
               py = (sy-minv*nump*y) / sum,
               pz = (sz-minv*nump*z) / sum;
        if (px < 0) px=0;
        if (py < 0) py=0;
        if (pz < 0) pz=0;
        // System.out.println("COI at:"+px+", "+py+", "+pz+" elem " + e +" time "+t+"\n");
        
        if (doCOI && maxv > minv) // otherwise everything is zero: leave it as it is!
        {
            p.coord[0] = px;
            p.coord[1] = py;
            p.coord[2] = pz;
            p.coord[3] = e;
            p.coord[4] = t;
        }
	if (didnegate) // was done in Minimum Mode
		ElementAt(e,t).ScaleV = -ElementAt(e,t).ScaleV;  // re-negate the values to be normal again
    }

    boolean MarkerDisplayed(APoint Pt, int [] ActPosition)
    {
        // int actlistpos= ActiveMarkerListPos();
        if (ShowAllTrack || Pt.coord[TrackDirection] == (int) (ActPosition[TrackDirection]+0.5))
        	return true;
            //if ( Pt.coord[2] == ((int) ActPosition[2] + 0.5)) // ShowAllSlices ||
            //    return true;
            //else
            //    return false;
        else
            return false;
    }
    void MarkerListDialog() {MyMarkers.MarkerListDialog(-1);}
    
    void MarkerDialog() {  // allows the user to define the units and scales
        ANGenericDialog md= new ANGenericDialog("Marker positioning");
        // int e=ActiveElement;
        md.addCheckbox("Display connecting lines",ConnectionShown);
        md.addCheckbox("Show all lines",ShowFullTrace);
        md.addCheckbox("Show all lists",ShowAllLists);
        md.addCheckbox("Show all trees",ShowAllTrees);
        md.addCheckbox("Show markers along track",ShowAllTrack);
        md.addCheckbox("Annotate with List Nr.",Annotate);
        md.addCheckbox("Advance each marker",Advance);
        md.addChoice("Tracking Mode: ",TrackModes,TrackModes[TrackMode]);
        md.addChoice("Tracking direction: ",TrackDirections,TrackDirections[TrackDirection]);
        md.addChoice("Finnish Mode: ",FinishModes,FinishModes[FinishMode]);
        md.addChoice("Finnish Metric: ",FinishMeasures,FinishMeasures[FinishMeasure]);
        md.addNumericFields("Threshold",ThreshValue,3,3);

        md.addCheckbox("Spectral Display for Track",ShowSpectralTrack);
        md.addMessage("For automatic marker positioning an iterative search for the nearest\r\n"+
                    "maximum signal is performed. The region to be searched is defined by the\r\n"+
                    "number of voxels to include in each direction, beyond the current plane.\r\n"+
                    "To restrict the update to any plane or line choose zero in the\r\n"+
                    "appropriate fields below.");
        md.addCheckbox("Use automatic maximum finding",MarkerToMax);
        md.addNumericFields("X Neighbours: ",SearchX,3,3);
        md.addNumericFields("Y:",SearchY,3,3);
        md.addNumericFields("Z:",SearchZ,3,3);
        md.addCheckbox("Subpixel positions by Center of Mass",UseCOI);
        md.addNumericFields("X Center of Intensity: ",COMX,3,3);
        md.addNumericFields("Y:",COMY,3,3);
        md.addNumericFields("Z:",COMZ,3,3);
        md.addNumericField("FWHM of Gaussian for subtaction: ",FWHM,3);
        md.addCheckbox("Repulsion",Repulsion);
        md.addCheckbox("Focus Display to Marker",FocusDispToMarker);
        //md.addCheckbox("Actio=Reactio",ActioReactio);

        md.addInFile("MarkerFileIn: ",markerInfilename);
        md.addOutFile("MarkerFileOut: ",markerOutfilename);
        md.addCheckbox("Append Version Number",AppendVersionNumber);
        boolean newAppendV=AppendVersionNumber;
        md.showDialog();
        if (! md.wasCanceled())
        {
            ConnectionShown=md.getNextBoolean();
            ShowFullTrace=md.getNextBoolean();
            ShowAllLists=md.getNextBoolean();
            ShowAllTrees=md.getNextBoolean();
            ShowAllTrack=md.getNextBoolean();
            Annotate=md.getNextBoolean();
            Advance=md.getNextBoolean();
            TrackMode=md.getNextChoiceIndex();
            TrackDirection=md.getNextChoiceIndex();
            FinishMode=md.getNextChoiceIndex();
            FinishMeasure=md.getNextChoiceIndex();
            ThreshValue= md.getNextNumber();

            ShowSpectralTrack=md.getNextBoolean();
            MarkerToMax=md.getNextBoolean();
            SearchX=(int) md.getNextNumber();
            SearchY=(int) md.getNextNumber();
            SearchZ=(int) md.getNextNumber();
            UseCOI=md.getNextBoolean();
            COMX=(int) md.getNextNumber();
            COMY=(int) md.getNextNumber();
            COMZ=(int) md.getNextNumber();
            FWHM= md.getNextNumber();
            Repulsion=md.getNextBoolean();
            FocusDispToMarker=md.getNextBoolean();
            //ActioReactio=md.getNextBoolean();
            markerInfilename=md.getInFile();
            markerOutfilename=md.getOutFile();
            newAppendV=md.getNextBoolean();
        if (AppendVersionNumber!=newAppendV && newAppendV && ! didSwitchAppendVersionNumber) // was switched on
        {
        	didSwitchAppendVersionNumber = true;
            ANGenericDialog ms= new ANGenericDialog("Marker positioning");
            String newName;
        	if (System.getProperty("os.name").startsWith("Windows"))
                newName="C:\\temp\\markers001.txt";
        	else
                newName="/tmp/markers001.txt";
        	
        	//MyNumber=markerOutfilename.substring(nBeg,nEnd);
        	//if (markerOutfilename)
        	
            ms.addMessage("Change marker filename to " + newName +"?\n");
            ms.showDialog();
            if (! md.wasCanceled())
            	markerOutfilename=newName;
        }
        AppendVersionNumber=newAppendV;
        }

        switch(TrackDirection)
        {
            case 0: SearchX=0;COMX=0;break;
            case 1: SearchY=0;COMY=0;break;
            case 2: SearchZ=0;COMZ=0;break;
            case 3: break;
            case 4: break;
        }
    }

    public void toggleGate(int toggle) {
        if (toggle < 0)
            GateActive = ! GateActive;
        else
            GateActive = (toggle == 1);
        if (GateActive) 
            CThreshToValThresh(GateElem,0.0,1.0);
        InvalidateSlices();
        InvalidateProjs(-1);
    }

    public void setGate() {
        GateElem = ActiveElement;
        if (GateActive)
        {
            InvalidateSlices();
            InvalidateProjs(-1);
            CThreshToValThresh(-1,0.0,1.0);
        }
    }
    
    public void setModelThresh() {
        setModelThresh(ActiveElement);
    }

    public void setModelThresh(int e) {
	GetBundleAt(e).CompCMap();
    }

    public int GetActiveColorModelNr () {
        return GetBundleAt(ActiveElement).ElementModelNr;
    }
    
    public int GetColorModelNr (int e) {
        return GetBundleAt(e).ElementModelNr;
    }
    
    public void SetColorModelNr (int e, int actModel) {
        GetBundleAt(e).ElementModelNr = actModel;
    }

    public Bundle GetBundleAt(int e) {
	return ((Bundle) MyBundle.elementAt(e));
    }

    public void ToggleLog(int newVal) {
        ToggleLog(ActiveElement,newVal);
    }

    public void ToggleConnection(int newVal) {
        if (newVal < 0)
            ConnectionShown = ! ConnectionShown;
        else if (newVal == 0)
            ConnectionShown = false;
        else
            ConnectionShown = true;
    }

    public void ToggleLog(int e,int newVal) {
	GetBundleAt(e).ToggleLog(newVal);
    }
    
    public void SetGamma(int e, double gamma) {
	GetBundleAt(e).SetGamma(gamma);
    }

    public double GetGamma(int e) {
    	return GetBundleAt(e).GetGamma();
        }
    
    public void ToggleOvUn(int newVal) {
	boolean resOvUn=GetBundleAt(ActiveElement).ToggleOvUn(newVal);
        if (resOvUn) newVal=1;
        else newVal=0;
        if (colormode)
            for (int i=0;i<Elements;i++)
                GetBundleAt(i).ToggleOvUn(newVal);
    }
    
    public void ToggleModel(int elem,int newModel ) {
	GetBundleAt(elem).ToggleModel(newModel);
	if (elem == ActiveElement)
	{
	IndexColorModel mymodel = GetBundleAt(elem).ElementModel;
        MySlice[0].TakeModel(mymodel);
        MySlice[1].TakeModel(mymodel);
        MySlice[2].TakeModel(mymodel);
	}
    if (GetBundleAt(elem).DispOverlay) // This element is displayed in the overlay
    	{
    	InvalidateColor();
    	}
    }
    
    public void ToggleModel(int newModel ) {
	ToggleModel(ActiveElement,newModel);
    }
    
    public void InvertCMap()
    {
	GetBundleAt(ActiveElement).cmapIsInverse = ! GetBundleAt(ActiveElement).cmapIsInverse;
    }
    /*private void Setmincs(int elem, double val) {
        BundleAt(elem).SetMincs(val);
      }
    
    private void Setmaxcs(int elem, double val) {
        System.out.println("SetMaxcs "+elem+": " + val+"\n");
        BundleAt(elem).SetMaxcs(val);
      }*/
    
    private double Getmincs(int elem) {
        return BundleAt(elem).GetMincs();
    }
    
    private double Getmaxcs(int elem) {
        // System.out.println("GetMaxcs "+elem+": " + BundleAt(elem).GetMaxcs()+"\n");
	return BundleAt(elem).GetMaxcs();
    }
    
    public double GetScaledMincs(int elem) {
        return GetMinThresh(elem)*ElementAt(elem).ScaleV + ElementAt(elem).OffsetV;
        //return Getmincs(elem)*ElementAt(elem).ScaleV + ElementAt(elem).OffsetV;
    }
    
    public double GetScaledMaxcs(int elem) {
        return GetMaxThresh(elem)*ElementAt(elem).ScaleV + ElementAt(elem).OffsetV;
        //return Getmaxcs(elem)*ElementAt(elem).ScaleV + ElementAt(elem).OffsetV;
    }
    
    public double GetScaledRange(int elem) {
        // System.out.println("GetScaledRange "+elem+"\n");
        if (elem < 0) return 0.0;
        else
            return ElementAt(elem).ScaleV*(GetMaxThresh(elem)-GetMinThresh(elem));
            //return ElementAt(elem).ScaleV*(Getmaxcs(elem)-Getmincs(elem));
    }
    
    public void transferThresh(int elem) {
        for (int t=0;t < Times;t++)
             ElementAt(elem,t).SetScaleShift(Getmincs(elem),Getmaxcs(elem));
    }

    public boolean SetThresh(double min, double max)  // returns whether the display is valid
    {
    	return SetThresh(ActiveElement,min,max);
    }
    
    public boolean SetThresh(int e, double min, double max)  // returns whether the display is valid
    {
       if ((BundleAt(e).GetMincs() != min) ||
       	(BundleAt(e).GetMaxcs() != max))
       	{
       	BundleAt(e).SetMincs(min);
       	BundleAt(e).SetMaxcs(max);
       	return false;
       }
       return true;
    }
    
    public boolean SetScaledMinMaxcs(int elem, double Min, double Max) {
        return SetThresh(elem, (Min - ElementAt(elem).OffsetV) / ElementAt(elem).ScaleV, (Max - ElementAt(elem).OffsetV) / ElementAt(elem).ScaleV);
        //return Getmincs(elem)*ElementAt(elem).ScaleV + ElementAt(elem).OffsetV;
    }
    
        
    public void AdjustThresh(boolean allelements) {
        if (! allelements)
            {   
                BundleAt(ActiveElement).cmapcHigh = Bundle.MaxCTable-1;  // set the color map thresholds back to normal
                BundleAt(ActiveElement).cmapcLow = 0;
                BundleAt(ActiveElement).CompCMap();
                double min = ActElement().ROIMinimum(ActROI());
                double max = ActElement().ROIMaximum(ActROI());
		if (! SetThresh(ActiveElement,min,max))
                    InvalidateSlices();
            	transferThresh(ActiveElement);
	    }
	else
        {
            boolean valid=true;
	    for (int e=0;e<Elements;e++)
		{
                BundleAt(e).cmapcHigh = Bundle.MaxCTable-1;  // set the color map thresholds back to normal
                BundleAt(e).cmapcLow = 0;
                BundleAt(e).CompCMap();
                double min = ElementAt(e).ROIMinimum(ActROI());
                double max = ElementAt(e).ROIMaximum(ActROI());
		if (! SetThresh(e,min,max))
		   valid=false;
                transferThresh(e);
                }
            if (! valid)
                InvalidateSlices();
        }
    }

    public void adjustColorMapLThresh(double howmuch)  // These functions are far quicker for the display especially for large images
    {
	double howmany= (howmuch*Bundle.MaxCTable);
        double max = BundleAt(ActiveElement).cmapcHigh;
        double min = BundleAt(ActiveElement).cmapcLow;
        if (howmany < 0 || max > min + howmany)
            BundleAt(ActiveElement).cmapcLow += (int) howmany;

        if (BundleAt(ActiveElement).cmapcLow > 0)
            BundleAt(ActiveElement).CompCMap();   // This is usually faster than recomputing images
        else
            CThreshToValThresh(ActiveElement,0.25,1.0);  // If underflow a recomputation becomes necessary, but with 25% extra space
    }

    public void adjustColorMapUThresh(double howmuch) 
    {
	double howmany= (howmuch*Bundle.MaxCTable);
        double max = BundleAt(ActiveElement).cmapcHigh;
        double min = BundleAt(ActiveElement).cmapcLow;
        if (howmany > 0 || max + howmany > min)
            BundleAt(ActiveElement).cmapcHigh += (int) howmany;

        if (BundleAt(ActiveElement).cmapcHigh <= Bundle.MaxCTable-1)
            BundleAt(ActiveElement).CompCMap();   // This is usually faster than recomputing images
        else
            CThreshToValThresh(ActiveElement,0.0,0.75);  // If underflow a recomputation becomes necessary, but with 25% extra space
    }

    public double GetMinThresh(int elem)   // returns the Effective threshold independent of which part of it is colormap and which is Mincs
    {
        double max = BundleAt(elem).GetMaxcs();     // These are the datavalues to which the min and max of the colormap point
        double min = BundleAt(elem).GetMincs();
        // double cmax = BundleAt(elem).cmapcHigh;    //  These are the current indices into the colormap
        double cmin = BundleAt(elem).cmapcLow;
        
        double scale = (max-min) / Bundle.MaxCTable;
        double nmin = min + scale*cmin;
        return nmin;
    }

    public double GetMaxThresh(int elem)   // returns the Effective threshold independent of which part of it is colormap and which is Mincs
    {
        double max = BundleAt(elem).GetMaxcs();     // These are the datavalues to which the min and max of the colormap point
        double min = BundleAt(elem).GetMincs();
        double cmax = BundleAt(elem).cmapcHigh;    //  These are the current indices into the colormap
        // double cmin = BundleAt(elem).cmapcLow;
        
        double nmax = min + (max-min)*cmax/Bundle.MaxCTable;
        return nmax;
    }

    
    public void CThreshToValThresh(int elem, double facmin, double facmax)   // copies the color-map threshold to a real value threshold
    {  // The factor determines the percentage at of the colormap at which the new min/max position will point (defaults = 0,1.0)
        if (elem < 0) elem=ActiveElement;
        double max = BundleAt(elem).GetMaxcs();     // These are the datavalues to which the min and max of the colormap point
        double min = BundleAt(elem).GetMincs();
        double cmax = BundleAt(elem).cmapcHigh;    //  These are the current indices into the colormap
        double cmin = BundleAt(elem).cmapcLow;
        // if (cmin == 0 && cmax == Bundle.MaxCTable-1) return;  // No need to change anything!

        double cmaxnew = (int) ((Bundle.MaxCTable-1)*facmax);   // new indices into colormap
        double cminnew = (int) ((Bundle.MaxCTable-1)*facmin);
        
        double scale = (max-min) / Bundle.MaxCTable;
        double scale2 = (cmax-cmin)/(cmaxnew-cminnew);
        double nmax = max - scale*(Bundle.MaxCTable - (cmax+scale2*(Bundle.MaxCTable-1-cmaxnew)));
        double nmin = min + scale*(cmin - scale2*cminnew);
        
        BundleAt(elem).SetMincs(nmin);
        BundleAt(elem).SetMaxcs(nmax);
        BundleAt(elem).cmapcLow = (int) cminnew;
        BundleAt(elem).cmapcHigh = (int) cmaxnew;
        InvalidateSlices();InvalidateProjs(elem);
        transferThresh(elem);
        BundleAt(elem).CompCMap();
    }
    
    public void addLThresh(double howmuch) {
	double howmany= (howmuch*ActElement().MaxValue);
        double max = BundleAt(ActiveElement).GetMaxcs();
        double min = BundleAt(ActiveElement).GetMincs();
        if (howmany < 0 || max > min + howmany)
        {
            BundleAt(ActiveElement).SetMincs(min + howmany);
            InvalidateSlices();InvalidateProjs(ActiveElement);
        }
        transferThresh(ActiveElement);
    }

    public void addUThresh(double howmuch) {
	double howmany=(howmuch*ActElement().MaxValue);
        double max = BundleAt(ActiveElement).GetMaxcs();
        double min = BundleAt(ActiveElement).GetMincs();
        if (howmany > 0 || max + howmany > min)
        {
            BundleAt(ActiveElement).SetMaxcs(max + howmany);
            InvalidateSlices();InvalidateProjs(ActiveElement);
        }
        transferThresh(ActiveElement);
    }
    
    public void initThresh() {
	for (int e=0;e<Elements;e++)
	    {
              BundleAt(e).cmapcLow = 0;
              BundleAt(e).cmapcHigh = Bundle.MaxCTable-1;  // set the color map thresholds back to normal
              BundleAt(e).CompCMap();
              AnElement ne = ElementAt(e);
              if (ne instanceof FloatElement || ne instanceof DoubleElement)
                {
                    BundleAt(e).SetMincs(ne.Min);
                    BundleAt(e).SetMaxcs(ne.Max);
                }
              else
                {
                    BundleAt(e).SetMincs(ne.OffsetV);
                    BundleAt(e).SetMaxcs(ne.MaxValue);
                }
            ToggleLog(e,0); // logarithmic off
            transferThresh(e);
            }
         InvalidateSlices();                    
    }

    public void initGlobalThresh() {
        AnElement ne = ElementAt(0);
        double min=ne.Min,max =ne.Max;
        
	for (int e=1;e<Elements;e++)
        {
            ne = ElementAt(e);
            if (ne.Min < min) min = ne.Min;
            if (ne.Max > max) max = ne.Max;
        }

        for (int e=0;e<Elements;e++)
	    {
                BundleAt(e).SetMincs(min);
                BundleAt(e).SetMaxcs(max);
                ToggleLog(e,0);
                transferThresh(e);
            }
        InvalidateSlices();                    
    }
 
    public Vector<AnElement> ElementsAtTime(int atime)
    {
        return MyTimes.elementAt(atime);
    }

    public Vector<ASlice> [] ProjsAtTime(int atime)
    {
        return MyTimeProj.elementAt(atime);
    }

    public ASlice [] ColorProjsAtTime(int atime)
    {
        return MyTimeColorProj.elementAt(atime);
    }
    
    public void GetElementsFromTime() 
    {
        MyElements = ElementsAtTime(ActiveTime);
        InvalidateSlices();
    }

    public void GetProjsFromTime() 
    {
        MyProjections = ProjsAtTime(ActiveTime);
        MyColorProjection = ColorProjsAtTime(ActiveTime);
        // InvalidateProjections();
    }

    public void setTime(int num) {
	if (num >= Times)
	    num=num%Times;
	if (num < 0)
	    num=(num+Times) % Times;
	ActiveTime=num;
        GetElementsFromTime();
        GetProjsFromTime();
    }

    public void nextTime(int num) // if > 0 advance, else devance
    {
        int newtime=ActiveTime+num;
        setTime(newtime);
    }

    public void advanceTime(int howmany) {
	ActiveTime+= howmany;
	if (ActiveTime < 0)
	    ActiveTime += Times;
	if (ActiveTime < 0)
	    ActiveTime = 0;
	ActiveTime %= Times;
        GetElementsFromTime();
        GetProjsFromTime();
    }

    public void setElement(int num) {
	if (num >= Elements)
	    num=Elements-1;
	if (num < 0)
	    num=0;
	ActiveElement=num;
        InvalidateSlices();
    }

    public void advanceElement(int howmany) {
	ActiveElement+= howmany;
	if (ActiveElement < 0)
	    ActiveElement += Elements;
	if (ActiveElement < 0)
	    ActiveElement = 0;
	ActiveElement %= Elements;
        InvalidateSlices();
    }

    public int GetNumElements() {
	return Elements;
    }

   public int GetSize(int DimNr) {
	return sizes[DimNr];
    }
   
   public double GetROISize(int elem, int dim) {
       if (elem < 0)
           return ActElement().Scales[dim] * ActROI().GetROISize(dim);
       else
           return ElementAt(elem).Scales[dim] * ActROI().GetROISize(dim);
   }

   public double GetROISum(int elem) {
       DoProject(elem,0);  // Also checks if really necessary
       for (int d=0;d<3;d++)
           if (BundleAt(elem).ProjValid[d] && ProjAt(d,elem).isValid)
                return ProjAt(d,elem).ROISum;

       // DoProject(elem,2);  // Also checks if really necessary
       return ProjAt(2,elem).ROISum;
   }

   public double GetROIVoxels(int elem) {
       DoProject(elem,0);  // Also checks if really necessary
       for (int d=0;d<3;d++)
           if (BundleAt(elem).ProjValid[d] && ProjAt(d,elem).isValid)
                return ProjAt(d,elem).ROIVoxels;

       // DoProject(elem,2);  // Also checks if really necessary
       return ProjAt(2,elem).ROIVoxels;
   }
   
   public double GetROIAvg(int elem) {
       DoProject(elem,0);  // Also checks if really necessary
       for (int d=0;d<3;d++)
           if (BundleAt(elem).ProjValid[d] && ProjAt(d,elem).isValid)
                return ProjAt(d,elem).ROIAvg;

       // DoProject(elem,2);  // Also checks if really necessary
       return ProjAt(2,elem).ROIAvg;
   }

   public double GetROIMax(int elem) {
       DoProject(elem,0);  // Also checks if really necessary
       for (int d=0;d<3;d++)
           if (BundleAt(elem).ProjValid[d] && ProjAt(d,elem).isValid)
                return ProjAt(d,elem).ROIMax;

       // DoProject(elem,2);  // Also checks if really necessary
       return ProjAt(2,elem).ROIMax;
   }

   public double GetROIMin(int elem) {
       DoProject(elem,0);  // Also checks if really necessary
       for (int d=0;d<3;d++)
           if (BundleAt(elem).ProjValid[d] && ProjAt(d,elem).isValid)
                return ProjAt(d,elem).ROIMin;

       // DoProject(elem,2);  // Also checks if really necessary
       return ProjAt(2,elem).ROIMin;
   }

   public double GetROIVal(int elem) {  // depending on the mode Avg or Max will be returned
       if (GetMIPMode(0))
           return GetROIMax(elem);
       else
           return GetROIAvg(elem);           
   }
   
   public String GetDataTypeName(int elem) {
       return ElementAt(elem).GetDataTypeName();
   }

   public double GetValueScale(int elem) {
       return ElementAt(elem).ScaleV;
   }

   public double GetScale(int elem,int dim) {
      // System.out.println("GetScale " + elem + " dim " + dim+ " Val "+ElementAt(elem).Scales[dim]);
      return ElementAt(elem).Scales[dim];
   }

   public double[] GetScale(int elem) {
      // System.out.println("GetScale " + elem + " dim " + dim+ " Val "+ElementAt(elem).Scales[dim]);
      return ElementAt(elem).Scales;
   }

   public double GetOffset(int elem,int dim) {
       return ElementAt(elem).Offsets[dim];
   }

   public double[] GetOffset(int elem) {
       return ElementAt(elem).Offsets;
   }

   public void SetValueScale(int elem, double ScaleV, double Off, String NameV, String UnitV) {
       ElementAt(elem).SetScales(ScaleV,Off,NameV,UnitV);
   }
        
   public void SetOffsets(int elem, double OfX,double OfY, double OfZ) {
        ElementAt(elem).Offsets[0] = OfX;
        ElementAt(elem).Offsets[1] = OfY;
        ElementAt(elem).Offsets[2] = OfZ;
   }

   public String GetValueName(int elem) {
       return ElementAt(elem).NameV;
   }

   public String GetValueUnit(int elem) {
       return ElementAt(elem).UnitV;
   }

   public String[] GetAxisUnits() {
       return ActElement().Units;
   }

   public String[] GetAxisNames() {
       return ActElement().Names;
   }

   void AxesUnitsDialog() {  // allows the user to define the units and scales

	   Runnable runnable = new Runnable()  // This is workaround to make dialogues work properly in newer Matlab versions
	   {
	       public void run()
	       {
	    	   AGenericDialog md= new AGenericDialog("Axes Units and Scalings");
	           int e=ActiveElement;
	           md.addStringField("NameX: ",GetAxisNames()[0]);
	           md.addStringField("UnitX: ",GetAxisUnits()[0]);
	           md.addNumericField("ScaleX: ",GetScale(e,0),5);
	           md.addNumericField("OffsetX: ",GetOffset(e,0),5);
	           md.addStringField("NameY: ",GetAxisNames()[1]);
	           md.addStringField("UnitY: ",GetAxisUnits()[1]);
	           md.addNumericField("ScaleY: ",GetScale(e,1),5);
	           md.addNumericField("OffsetY: ",GetOffset(e,1),5);
	           md.addStringField("NameZ: ",GetAxisNames()[2]);
	           md.addStringField("UnitZ: ",GetAxisUnits()[2]);
	           md.addNumericField("ScaleZ: ",GetScale(e,2),5);
	           md.addNumericField("OffsetZ: ",GetOffset(e,2),5);
	           md.addStringField("NameE: ",GetAxisNames()[3]);
	           md.addStringField("UnitE: ",GetAxisUnits()[3]);
	           md.addNumericField("ScaleE: ",GetScale(e,3),5);
	           md.addNumericField("OffsetE: ",GetOffset(e,3),5);
	           md.addStringField("NameT: ",GetAxisNames()[4]);
	           md.addStringField("UnitT: ",GetAxisUnits()[4]);
	           md.addNumericField("ScaleT: ",GetScale(e,4),5);
	           md.addNumericField("OffsetT: ",GetOffset(e,4),5);
	           md.showDialog();
	           if (! md.wasCanceled())
	           {
	               String Units[] = new String[5];
	               String Names[] = new String[5];
	               double SX,SY,SZ,SE,ST,OX,OY,OZ,OE,OT;
	               Names[0]=md.getNextString();Units[0]=md.getNextString();SX=md.getNextNumber();OX=md.getNextNumber();
	               Names[1]=md.getNextString();Units[1]=md.getNextString();SY=md.getNextNumber();OY=md.getNextNumber();
	               Names[2]=md.getNextString();Units[2]=md.getNextString();SZ=md.getNextNumber();OZ=md.getNextNumber();
	               Names[3]=md.getNextString();Units[3]=md.getNextString();SE=md.getNextNumber();OE=md.getNextNumber();
	               Names[4]=md.getNextString();Units[4]=md.getNextString();ST=md.getNextNumber();OT=md.getNextNumber();
	               for (int i=0 ; i< Elements;i++)
	               {
	                   ElementAt(i).Scales[0]=SX;
	                   ElementAt(i).Scales[1]=SY;
	                   ElementAt(i).Scales[2]=SZ;
	                   ElementAt(i).Scales[3]=SE;
	                   ElementAt(i).Scales[4]=ST;
	                   ElementAt(i).Offsets[0]=OX;ElementAt(i).Offsets[1]=OY;ElementAt(i).Offsets[2]=OZ;
	                   ElementAt(i).Offsets[3]=OE;ElementAt(i).Offsets[4]=OT;
	                   ElementAt(i).Names = Names;
	                   ElementAt(i).Units = Units;
	               }
	           }
	       }
	   };
	   try {
	   // EventQueue.invokeAndWait(runnable);
	   EventQueue.invokeLater(runnable);
	   }
	   catch(Exception e)
	   {
		System.out.println("Exception appeared in dialogue \n");
		e.printStackTrace();
	   }

    }

  void ValueUnitsDialog() {  // allows the user to define the units and scales
        AGenericDialog md= new AGenericDialog("Value Unit and Scaling");
        int e=ActiveElement;
        double [] DispOffset=GetDisplayOffset(e);
        md.addStringField("ValueName: ",GetValueName(e));    // Only here the element is important, since value is element specific
        md.addStringField("ValueUnit: ",GetValueUnit(e));    // Only here the element is important, since value is element specific
        md.addNumericField("ValueScale: ",GetValueScale(e),5);
        md.addNumericField("ValueOffset: ",GetValueOffset(e),5);
        md.addNumericField("Threshold Min: ",GetScaledMincs(e),5);
        md.addNumericField("Threshold Max: ",GetScaledMaxcs(e),5);
        md.addNumericField("DisplayOffset X: ",DispOffset[0],5);
        md.addNumericField("DisplayOffset Y: ",DispOffset[1],5);
        md.addNumericField("DisplayOffset Z: ",DispOffset[2],5);
        md.addNumericField("Display Gamma: ",GetGamma(e),5);
        md.showDialog();
        if (! md.wasCanceled())
        {
            String NV,UV;
            double SV,OV,Min,Max,Gamma;
            NV=md.getNextString();UV=md.getNextString();SV=md.getNextNumber();OV=md.getNextNumber();Min=md.getNextNumber();Max=md.getNextNumber();
            DispOffset[0]=md.getNextNumber();DispOffset[1]=md.getNextNumber();DispOffset[2]=md.getNextNumber();Gamma=md.getNextNumber();
            ElementAt(e).SetScales(SV,OV,NV,UV);  // The value scales are element specific
           SetScaledMinMaxcs(e,Min,Max);
           SetGamma(e,Gamma);
        }
    }

   public double GetValueOffset(int elem) {
       return ElementAt(elem).OffsetV;
   }

   public double [] GetDisplayOffset(int elem) {
       return ElementAt(elem).DisplayOffset;
   }

   public void SetValueOffset(int elem, double value) {
       ElementAt(elem).OffsetV=value;
   }

    public int GetActiveElement() {
	return ActiveElement;
    }

    public int GetActiveTime() {
	return ActiveTime;
    }

    public boolean GetColorMode() {
	return colormode;
    }

    public int GetChannel(int col) {
	if (col==0)
	    return elemR;
	if (col==1)
	    return elemG;
	if (col==2)
	    return elemB;
	return -1;
    }

    public void InvalidateColor(int time) {
        MyColorSlice[0].Invalidate();MyColorSlice[1].Invalidate();MyColorSlice[2].Invalidate();
        MyColorProjection[0].Invalidate();MyColorProjection[1].Invalidate();MyColorProjection[2].Invalidate();
    }

    public void InvalidateColor() {
        for (int t=0;t<Times;t++)
            InvalidateColor(t);
    }

   public void InvalidateSlices() {
        MyColorSlice[0].Invalidate();MyColorSlice[1].Invalidate();MyColorSlice[2].Invalidate();
        MySlice[0].Invalidate();MySlice[1].Invalidate();MySlice[2].Invalidate();
   }

    public void InvalidateProjs(int which, int time) {  // Invalidates for a specific time only
        // System.out.println("Invalidating Time : " +time+", element:" + which);
        if (which < 0)
        {
            for (int e=0;e<Elements;e++)
            {
                ((ASlice) ProjsAtTime(time)[0].elementAt(e)).Invalidate();
                ((ASlice) ProjsAtTime(time)[1].elementAt(e)).Invalidate();
                ((ASlice) ProjsAtTime(time)[2].elementAt(e)).Invalidate();
                BundleAt(e).Invalidate();
            }
        ColorProjsAtTime(time)[0].Invalidate();
        ColorProjsAtTime(time)[1].Invalidate();
        ColorProjsAtTime(time)[2].Invalidate();
        }
        else
            {
                ((ASlice) ProjsAtTime(time)[0].elementAt(which)).Invalidate();
                ((ASlice) ProjsAtTime(time)[1].elementAt(which)).Invalidate();
                ((ASlice) ProjsAtTime(time)[2].elementAt(which)).Invalidate();
                BundleAt(which).Invalidate();
                if (InOverlayDispl(which))
                {
                    ColorProjsAtTime(time)[0].Invalidate();
                    ColorProjsAtTime(time)[1].Invalidate();
                    ColorProjsAtTime(time)[2].Invalidate();
                }
            }
    }

    public void InvalidateProjs(int which) {  // Invalidates for all times
        for (int t=0;t<Times;t++)
            InvalidateProjs(which,t);
    }
    
    public void ClearChannel(int col) {  // removes this channel from display
	if (col==0)
	    {
                if (elemR >= 0)
                {
                    GetBundleAt(elemR).ToggleOverlayDispl(0);
                    GetBundleAt(elemR).ToggleMulDispl(0);
                    GetBundleAt(elemR).ToggleModel(0);
                }
		elemR = -1;
	    }
	if (col==1)
	    {
                if (elemG >= 0)
                {
                    GetBundleAt(elemG).ToggleOverlayDispl(0);
                    GetBundleAt(elemR).ToggleMulDispl(0);
                    GetBundleAt(elemG).ToggleModel(0);
                }
		elemG = -1;
	    }
	if (col==2)
	    {
                if (elemB >= 0)
                {
                    GetBundleAt(elemB).ToggleOverlayDispl(0);
                    GetBundleAt(elemR).ToggleMulDispl(0);
                    GetBundleAt(elemB).ToggleModel(0);
                }
		elemB = -1;
	    }
        InvalidateColor();
    }

    public void MarkChannel(int col) {
	MarkChannel(ActiveElement,col);
    }

    public void MarkChannel(int elem,int col) {
        //ClearChannel(col);
        //if (elemR == elem) ClearChannel(0);
        //if (elemG == elem) ClearChannel(1);
        //if (elemB == elem) ClearChannel(2);
	
	if (col==0)
	    {
		elemR = elem;
                GetBundleAt(elemR).ToggleOverlayDispl(1);
                GetBundleAt(elemR).ToggleModel(1);
	    }
	if (col==1)
	    {
		elemG = elem;
                GetBundleAt(elemG).ToggleOverlayDispl(1);
                GetBundleAt(elemG).ToggleModel(2);
	    }
	if (col==2)
	    {
		elemB = elem;
                GetBundleAt(elemB).ToggleOverlayDispl(1);
                GetBundleAt(elemB).ToggleModel(3);
	    }
	InvalidateColor();
        }

    public void MarkAsHistoDim(int dim) { // this will toggle the histo on and of, if at the same element
        System.out.println("Element " + ActiveElement + " marked as HistoDim " + dim);
                
	if (dim==0)
            if (HistoX != ActiveElement) // Histo X can not be deleted
                HistoX = ActiveElement;
	if (dim==1)
	    if (HistoY == ActiveElement && HistoZ == -1)
                HistoY=-1;
            else
                HistoY = ActiveElement;
	if (dim==2)
	    if (HistoZ == ActiveElement)
                HistoZ=-1;
            else
                HistoZ = ActiveElement;
    }

    public void ToggleSquareROIs() {
        for (int e=0;e < Elements;e++)
            BundleAt(e).ToggleROI();
        InvalidateProjs(-1);  // all invalid
    }
    
    public boolean SquareROIs() { return BundleAt(ActiveElement).SquareROIs();};

    public void ToggleColor(boolean set) {
	colormode=set;
    }
    
    public void ToggleColor() {
	if (!colormode)
                colormode=true;
	else
		colormode=false;
    }

//    public void ComputeColorProj(int dim) {   // copies elementdata into the color array
//	int indexR=elemR,indexG=elemG,indexB=elemB;
//	MyColorProjection[dim].ClearColor();
//	if (indexR>=0)
//            MyColorProjection[dim].MergeColor(0,ProjAt(dim,indexR));
//        if (indexG>=0)
//            MyColorProjection[dim].MergeColor(1,ProjAt(dim,indexG));
//        if (indexB>=0) 
//            MyColorProjection[dim].MergeColor(2,ProjAt(dim,indexB));
//        MyColorProjection[dim].isValid = true;
//    }

 public void ComputeColorProj(int dim) {   // copies elementdata into the color array
	MyColorProjection[dim].ClearColor();
    for (int e=0;e<Elements;e++)
    {
        if (GetBundleAt(e).MulOverlayDispl())  // has this element been selected as multiplicative color?
        {
            ElementAt(e).SetScaleShift(Getmincs(e),Getmaxcs(e));
            MyColorProjection[dim].MulToColorSlice(ProjAt(dim,e),GetBundleAt(e).cmapRed,GetBundleAt(e).cmapGreen,GetBundleAt(e).cmapBlue,ProjAt(dim,GateElem),GateActive, ElementAt(ActiveElement));
        }
        if (GetBundleAt(e).InOverlayDispl())  // if an RGB color is selected, these will be active
        {
            ElementAt(e).SetScaleShift(Getmincs(e),Getmaxcs(e));
            MyColorProjection[dim].SumToColorSlice(ProjAt(dim,e),GetBundleAt(e).cmapRed,GetBundleAt(e).cmapGreen,GetBundleAt(e).cmapBlue,ProjAt(dim,GateElem),GateActive, ElementAt(ActiveElement));
        }
    }
	
        MyColorProjection[dim].isValid = true;
    }

    
    Bundle BundleAt (int num) {
        return MyBundle.elementAt(num);
    }
    
    
    private AnElement GNE(AnElement oldelem, Vector<AnElement> ElementsList, Vector<ASlice> ProjList[]) // will generate a new element in the list
    {
        AnElement ne=null;
        if (oldelem.DataType == AnElement.IntegerType)
        {
           IntegerElement AE= (IntegerElement) oldelem;
           ne=new IntegerElement(AE.Sizes[0],AE.Sizes[1],AE.Sizes[2],AE.NumBytes,AE.MaxValue);
        }
        else if (oldelem.DataType == AnElement.FloatType)
           ne=new FloatElement(oldelem.Sizes[0],oldelem.Sizes[1],oldelem.Sizes[2],(float) 1000);
        else if (oldelem.DataType == AnElement.DoubleType)
           ne=new DoubleElement(oldelem.Sizes[0],oldelem.Sizes[1],oldelem.Sizes[2],(double) 1000);
        else if (oldelem.DataType == AnElement.ComplexType)
           ne=new ComplexElement(oldelem.Sizes[0],oldelem.Sizes[1],oldelem.Sizes[2],(float) 1000);
        else if (oldelem.DataType == AnElement.ByteType)
           ne=new ByteElement(oldelem.Sizes[0],oldelem.Sizes[1],oldelem.Sizes[2]);
        else if (oldelem.DataType == AnElement.ShortType)
           ne=new ShortElement(oldelem.Sizes[0],oldelem.Sizes[1],oldelem.Sizes[2]);
        else 
        {
            System.out.println("Fatal Error! Element of unknown type: "+oldelem.DataType+"\n");
            throw new IllegalArgumentException("Unknown Element type\n");
        }
        ElementsList.addElement(ne);
        ProjList[0].addElement(new ASlice(0,ne));
        ProjList[1].addElement(new ASlice(1,ne));
        ProjList[2].addElement(new ASlice(2,ne));
        ne.SetScales(oldelem);

        return ne;
    }

    private void GNE(AnElement oldelem) // will generate new element in all time lists
    {
        // AnElement ne=null;
        for (int t=0;t<Times;t++)
            GNE(oldelem,ElementsAtTime(t),ProjsAtTime(t));
        
        Elements ++;
    }

    public void CloneElement(AnElement oldelem) 
    {
        // System.out.println("CloningLast Element\n");
        if (Elements == 1)
            MarkChannel(0);  // mark as red
        
        // Bundle bd = (Bundle) MyBundle.lastElement();
	GNE(oldelem);
        MyBundle.addElement((Bundle) BundleAt(Elements-2).clone());

        ActiveElement=Elements-1;
        if (Elements == 2)
            MarkChannel(1);  // mark as green
        if (Elements == 3)
            MarkChannel(2);  // mark as blue
        InvalidateSlices();
        InvalidateColor();
        // CompCMap(Elements-1);
    }

    public void CloneLastElements() // Clones the elements for all timesteps
    {
        for (int t=0;t<Times;t++)
            CloneElement((AnElement) ElementsAtTime(t).lastElement());
    }
    
    private AnElement GNE(int DataType, int NumBytes, int NumBits, Vector<AnElement> ElementList, Vector<ASlice> ProjList[])  // just generate the element, not the bundle
    {
        double MaxValue=(2<<(NumBits-1))-1;
        // System.out.println("Sizes: "+SizeX+", "+SizeY+", "+SizeZ+ ", MaxValue : :"+MaxValue);

        AnElement ne=null;
        if (DataType == AnElement.IntegerType)
           ne=new IntegerElement(SizeX,SizeY,SizeZ,NumBytes,MaxValue);
        if (DataType == AnElement.FloatType)
           ne=new FloatElement(SizeX,SizeY,SizeZ,(float) 1000);
        if (DataType == AnElement.DoubleType)
           ne=new DoubleElement(SizeX,SizeY,SizeZ,(double) 1000);
        if (DataType == AnElement.ComplexType)
           ne=new ComplexElement(SizeX,SizeY,SizeZ,(float) 1000);
        if (DataType == AnElement.ByteType)
           ne=new ByteElement(SizeX,SizeY,SizeZ);
        if (DataType == AnElement.ShortType)
           ne=new ShortElement(SizeX,SizeY,SizeZ);
        ElementList.addElement(ne);
        ProjList[0].addElement(new ASlice(0,ne));
        ProjList[1].addElement(new ASlice(1,ne));
        ProjList[2].addElement(new ASlice(2,ne));

        return ne;
    }

//    private void GNE(int DataType, int NumBytes, int NumBits) // will generate new element in all time lists
//    {
//       AnElement ne=null;
//        for (int t=0;t<Times;t++)
//            ne=GNE(DataType,NumBytes,NumBits,ElementsAtTime(t),ProjsAtTime(t));
//
//        Elements ++;
//    }
    
    private int GenerateNewElement(int DataType, int NumBytes, int NumBits, double[] Scales, double[] Offsets,
                                   double ScaleV, double OffsetV, 
                                   String [] Names, String [] Units,
                                   Vector<AnElement> ElementList, Vector<ASlice> ProjList[])
    {
	int mynewnr = Elements; // this will be the number of the new element
        AnElement ne=GNE(DataType,NumBytes,NumBits,ElementList, ProjList);
        ne.SetScales(Scales, Offsets, ScaleV,OffsetV);
        ne.Names = Names;
        ne.Units = Units;
        
        //ne.TakePlaneROIs(PlanesS,PlanesD);
	return mynewnr;
    }

    public int GenerateNewElement(int DataType, int NumBytes, int NumBits, double[] Scales,
                                    double[] Offsets, double ScaleV, double OffsetV, 
                                   String [] Names, String [] Units)
    {
        int ne=0;
        for (int t=0;t<Times;t++)
            ne=GenerateNewElement(DataType, NumBytes, NumBits, Scales, 
                                    Offsets, ScaleV, OffsetV, Names, Units,
                                    ElementsAtTime(t),ProjsAtTime(t));
        Bundle nb; 
        double MaxValue=(2<<(NumBits-1))-1;
        if (DataType == AnElement.FloatType)
            nb=new Bundle(0,0.0,1000.0);
        else if (DataType == AnElement.ByteType)
            nb=new Bundle(0,0.0,MaxValue);
        else         //if (DataType == IntegerType)
            nb=new Bundle(0,0.0,MaxValue);
        MyBundle.addElement(nb);
        nb.TakeSqrROIs(ProjMin,ProjMax);
        nb.TakePolyROIs(ROIPolygons);
        if (ne > 0)  // This means, there was already a first element
            if (BundleAt(0).ActiveROI == BundleAt(0).rectROI)
                nb.ActiveROI = nb.rectROI;
            else
                nb.ActiveROI = nb.polyROI;
        else nb.ActiveROI = nb.rectROI;
        Elements ++;
      	sizes[3]=Elements;
     	sizes[4]=Times;
       return ne;
    }

    @SuppressWarnings("unchecked")
	public int GenerateNewTime(int DataType, int NumBytes, int NumBits, double[] Scales,
            double[] Offsets, double ScaleV, double OffsetV, 
           String [] Names, String [] Units)
		{
		int ne=0;

		MyElements = new Vector<AnElement>();   // A list of elements is generated for each timepoint
        MyTimes.addElement(MyElements);  // However, all times use the same list of elements.
        MyProjections = (Vector<ASlice>[]) new Vector[3];    // these manage RGB projections
        MyProjections[0] = new Vector<ASlice>();
        MyProjections[1] = new Vector<ASlice>();
        MyProjections[2] = new Vector<ASlice>();
        MyTimeProj.addElement(MyProjections);
		
		ActiveTime=Times;
		Times ++;
		for (int e=0;e<Elements;e++)
		ne=GenerateNewElement(DataType, NumBytes, NumBits, Scales, 
		            Offsets, ScaleV, OffsetV, Names, Units,
		            ElementsAtTime(Times-1),ProjsAtTime(Times-1));   // append the required number of elements

		MyColorProjection = new ASlice[3];    // this manages color projections
        MyColorProjection[0]=new ASlice(0,(AnElement) ElementsAtTime(Times-1).firstElement());
        MyColorProjection[1]=new ASlice(1,(AnElement) ElementsAtTime(Times-1).firstElement());
        MyColorProjection[2]=new ASlice(2,(AnElement) ElementsAtTime(Times-1).firstElement());
        MyTimeColorProj.addElement(MyColorProjection);
	    //System.out.println("Generated New Time\n");
      	sizes[3]=Elements;
     	sizes[4]=Times;
		return ne;
		}

    public void AdjustOffsetToROIMean()  // takes the mean of the ROI and adjusts the offset of this element accordingly
    {
        int elem=ActiveElement;
        double val=GetROIAvg(elem);
        SetValueOffset(elem,GetValueOffset(elem)-val);
        InvalidateProjs(elem);
        InvalidateSlices();
    }

    int GetPartnerElem() {  // retrieves a partner for arithmetic operations
        int myelem=GateElem;
        if (myelem < 0)
            myelem=Elements-1;
        return myelem;
    }

    public void AddMarkedElement()  
    {
        int myelem=GetPartnerElem();
        if (myelem >= 0)
        for (int t=0;t<Times;t++) {
            ElementAt(ActiveElement,t).Add(ElementAt(myelem,t));
	    }
        InvalidateSlices();
    }

    public void SubMarkedElement()  
    {
        int myelem=GetPartnerElem();
        if (myelem >= 0)
        for (int t=0;t<Times;t++) {
            ElementAt(ActiveElement,t).Sub(ElementAt(myelem,t));
	    }
        InvalidateSlices();
    }

    public void MulMarkedElement()  
    {
        int myelem=GetPartnerElem();
        if (myelem >= 0)
        for (int t=0;t<Times;t++) {
            ElementAt(ActiveElement,t).Mul(ElementAt(myelem,t));
	    }
        InvalidateSlices();
    }

    public void DivMarkedElement()  
    {
         int myelem=GetPartnerElem();
        if (myelem >= 0)
        for (int t=0;t<Times;t++) {
            ElementAt(ActiveElement,t).Div(ElementAt(myelem,t));
	    }
        InvalidateSlices();
    }
    
    public void DeleteActElement(Vector<AnElement> ElementList)  // what, if another dublicate exists?
    {
        if (Elements <= 1) // The last element must not be deleted
            return;
        if (elemR == ActiveElement)
            elemR = -1;
        else if (elemR > ActiveElement) elemR --;
        if (elemG == ActiveElement)
            elemG = -1;
        else if (elemG > ActiveElement) elemG --;
        if (elemB == ActiveElement)
            elemB = -1;
        else if (elemB > ActiveElement) elemB --;
        if (GateElem == ActiveElement)
            GateElem = 0;
        else if (GateElem > ActiveElement) GateElem --;
        
        ElementList.removeElementAt(ActiveElement);
    }

    public void DeleteActElement()  // what, if another dublicate exists?
    {
        if (Elements > 1) // The last element must not be deleted
        {
        for (int t=0;t<Times;t++)
            DeleteActElement(ElementsAtTime(t));
        MyBundle.removeElementAt(ActiveElement);
        MyProjections[0].removeElementAt(ActiveElement);
        MyProjections[1].removeElementAt(ActiveElement);
        MyProjections[2].removeElementAt(ActiveElement);
        Elements --;
        
        if (ActiveElement >= Elements)
            ActiveElement = Elements-1;
        InvalidateSlices();
        InvalidateColor();
        }
    }
    
    public Color GetMarkerColor(int e) {
      int Red=0,Green=0,Blue=0;
      int MaxC=GetBundleAt(e).cmapcHigh;
      if (MaxC > Bundle.MaxCTable) MaxC = Bundle.MaxCTable;
      Red=GetBundleAt(e).cmapRed[MaxC-2]; // almost highest value
      Green=GetBundleAt(e).cmapGreen[MaxC-2]; // almost highest value
      Blue=GetBundleAt(e).cmapBlue[MaxC-2]; // almost highest value
      if (Red < 0 ) Red += 256;
      if (Green < 0 ) Green += 256;
      if (Blue < 0 ) Blue += 256;
      
      if (Red < 50) Red = 50;
      if (Green < 50) Green = 50;
      if (Blue < 50) Blue = 50;
      return new Color(Red,Green,Blue);
    }
    
    public Color GetCMapColor(int e, int pos,int mysize)
    {
    return GetBundleAt(e).GetCMapColor(pos,mysize);
     }

    public double Normalize(double val, int e)
    {
        return (val-GetMinThresh(e)) /(GetMaxThresh(e)-GetMinThresh(e));
    }

    public double NormedValueAt(int x, int y, int z, int e) {  // will return the log-value if Logscale is on
        double val= ElementAt(e).GetRawValueAtOffset(x,y,z,ElementAt(ActiveElement));
        double min=GetMinThresh(e);
        double max=GetMaxThresh(e);
        double Gamma=GetBundleAt(e).Gamma;
        
        if (! GetBundleAt(e).LogScale)
        	if (Gamma == 1.0)
        		return (val-min) /(max-min);
        	else
        		return java.lang.Math.pow((val-min) /(max-min),Gamma);
        else
            if (val > 0)
            {
                double vl=0.1,vh = 1.0;
                if (min > 0)
                    vl = min;
                if (max > 0)
                    vh = max;
                return (java.lang.Math.log(val)-java.lang.Math.log(vl)) /(java.lang.Math.log(vh)-java.lang.Math.log(vl));
            }
            else
                return -10;
    }
    
    public int GetIntValueAt(int x, int y, int z, int e) {
        return ElementAt(e).GetIntValueAt(x,y,z);
    }
    
    public Color GetColColor(int e,int x,int y,int z) {
      int val= (int) GetIntValueAt(x,y,z,e);
      if (val < 0) val = 0; if (val > Bundle.MaxCTable-1) val = Bundle.MaxCTable-1;
      int red = GetBundleAt(e).cmapRed[val] & 0xff;
      int green = GetBundleAt(e).cmapGreen[val] & 0xff;
      int blue = GetBundleAt(e).cmapBlue[val] & 0xff;
      return new Color(red,green,blue);
    }
    
    public double NormedProjValueAt(int dir, int pos, int e) {  // will return the log-value if Logscale is on
        DoProject(e,dir);  // Also checks if really necessary
        int xdir=0;
        switch (dir) {
        case 0: xdir=1; break;
        case 1: xdir=0; break;
        case 2: xdir=2; break;
        }
        double val= ProjAt(dir,e).GetNormed1DProjValue(pos + (int) ElementAt(ActiveElement).DisplayOffset[xdir]);
        if (! GetBundleAt(e).LogScale)
            return val;
        else
            if (val > 0)
            {
                return (java.lang.Math.log(val)-java.lang.Math.log(0.01))/ (-java.lang.Math.log(0.01));
            }
            else
                return -10;
        }

    public double ProjValueAt(int dir, int pos, int e) {
        DoProject(e,dir);  // Also checks if really necessary
        int xdir=0;
        switch (dir) {
        case 0: xdir=1; break;
        case 1: xdir=0; break;
        case 2: xdir=2; break;
        }
        return ProjAt(dir,e).Get1DProjValue(pos+ (int) ElementAt(ActiveElement).DisplayOffset[xdir]);
    }
    
    
    public double ValueAt(int x, int y, int z, int e) {
        return ElementAt(e).GetValueAtOffset(x,y,z, ElementAt(ActiveElement));
    }
    
    public void ApplyHistSelection() {  // applies ROI selections to the alpha channel of the associated RGB data
        if (DataToHistogram == null)
            System.out.println("Error: No data connected to this histogram");
        else
        {
        DataToHistogram.GenerateNewElement(AnElement.IntegerType,4,32,DataToHistogram.GetScale(0),
                                            DataToHistogram.GetOffset(0), 1.0,0.0,DataToHistogram.GetAxisNames(),DataToHistogram.GetAxisUnits());
        DataToHistogram.setElement(DataToHistogram.Elements-1);
        // System.out.println("Applying Histogram \n");
        AnElement ne = DataToHistogram.ActElement();
        ActElement().ComputeHistMask(ne,ActROI());
        DataToHistogram.BundleAt(DataToHistogram.Elements-1).SetMaxcs(ne.Max);
        DataToHistogram.BundleAt(DataToHistogram.Elements-1).SetMincs(ne.Min);
        }
    }

    public void CloneFloat() {  // will generate a new element in float, NO Thresholds applied
        int prevActElem=ActiveElement;
        int ne=GenerateNewElement(AnElement.FloatType,4,32,GetScale(ActiveElement),
                            GetOffset(ActiveElement),1.0,0.0,GetAxisNames(),GetAxisUnits());
        for (int t=0;t<Times;t++) {
        	ElementAt(ne,t).CopyVal(ElementAt(prevActElem,t));
		}
    }

    public void CloneShort() {  // will generate a new element in short type, thresholds WILL BE appliled
        int prevActElem=ActiveElement;
        int ne=GenerateNewElement(AnElement.ShortType,2,16,GetScale(ActiveElement),
                            GetOffset(ActiveElement),1.0,0.0,GetAxisNames(),GetAxisUnits());
        for (int t=0;t<Times;t++) {
        	((IntegerElement) ElementAt(ne,t)).CopyIntVal(ElementAt(prevActElem,t),Getmincs(prevActElem),Getmaxcs(prevActElem));
		}
    }

    public void GenerateMask(int dim) {
        int prevActElem=ActiveElement;
        int ne=0;
        if (ProjMode[dim])
        {
            ne=GenerateNewElement(ActElement().DataType,ActElement().GetStdByteNum(),ActElement().GetStdBitNum(),GetScale(ActiveElement),
                            GetOffset(ActiveElement),1.0,0.0,GetAxisNames(),GetAxisUnits());
            ElementAt(ne).GenerateMask(ROIAt(prevActElem),GetGateElem(),ElementAt(prevActElem),true);  // will copy the data
        }
        else
        {
            ne=GenerateNewElement(AnElement.ByteType,1,8,GetScale(ActiveElement),
                            GetOffset(ActiveElement),1.0,0.0,GetAxisNames(),GetAxisUnits());
            ElementAt(ne).SetScales(1.0,0.0,"inside ROI","a.u.");
            ElementAt(ne).GenerateMask(ROIAt(prevActElem),GetGateElem(),ElementAt(prevActElem),false); // will only generate threshold
            BundleAt(ne).SetMaxcs(1);
            BundleAt(ne).SetMincs(0);
        }
        setElement(ne);
    }

    public AnElement ElementAt(int num, int time) {
        if (num >= 0 && time >= 0)
            return (AnElement) ElementsAtTime(time).elementAt(num);
        else
            return null;
    }

    public AnElement ElementAt(int num) {
        if (num >= 0)
            return (AnElement) MyElements.elementAt(num);
        else
            return null;
    }
    
    public AnElement ActElement() {
        return ElementAt(ActiveElement);
    }  
    
    ROI ROIAt(int elem) {
        return BundleAt(elem).ActiveROI;
    }
    
    ROI ActROI() {
        return ROIAt(ActiveElement);
    }  
    
    public int GetPolyROISize(int dir)  // returns the number of corners in the polygon
    {
    	if (ROIPolygons == null)
    		return 0;
        if (ROIPolygons[dir] != null)
            return ROIPolygons[dir].npoints;
        else
            return 0;
    }
    
    public void GetPolyROICoords(int dir,int segment, float [] coords)  // adds a point to the Polygon ROI
    {
        if (ROIPolygons[dir] != null)
        {
            coords[0] = ROIPolygons[dir].xpoints[segment];
            coords[1] = ROIPolygons[dir].ypoints[segment];
        }
    }

    public void MovePolyROI(int DX,int DY, int dir)  // adds a point to the Polygon ROI
    {
        ROIPolygons[dir].translate(DX,DY);
        for (int e=0;e<Elements;e++)
        {
            BundleAt(e).TakePolyROIs(ROIPolygons);
            InvalidateProjs(e);  // all projections are invalid
            DoProject(e,dir);
        }
    }

    public void MoveSqrROI(int DX,int DY, int dir)  // adds a point to the Polygon ROI
    {
        Rectangle r2 = GetSqrROI(dir);
        for (int e=0;e<Elements;e++)
        {
            BundleAt(e).UpdateSqrROI(r2.x + DX,r2.y + DY, r2.x+r2.width + DX, r2.y+r2.height + DY,dir);
        	InvalidateProjs(e);  // all projections are invalid
        	DoProject(e,dir);
        }
    }
    
    public void MoveROI(int DX,int DY, int dir)  // adds a point to the Polygon ROI
    {
        if (SquareROIs())
            MoveSqrROI(DX,DY,dir);
        else
            MovePolyROI(DX,DY,dir);
    }
    
//    public void TakePolyROI(int ROIX,int ROIY, int dir)  // adds a point to the Polygon ROI
//    {
//        if (ROIPolygons[dir] == null)
//            ROIPolygons[dir] = new Polygon();
//            
//        ROIPolygons[dir].addPoint(ROIX,ROIY);
//        for (int e=0;e<Elements;e++) {
//            BundleAt(e).TakePolyROIs(ROIPolygons);
//    		InvalidateProjs(e);  // all projections are invalid
//    		DoProject(e,dir);
//    		}
//        return;
//    }

    public void TakePolyROI(Polygon myNewROI, int dir)  // adds a point to the Polygon ROI
    {
        ROIPolygons[dir]=myNewROI;
        for (int e=0;e<Elements;e++) {
            BundleAt(e).TakePolyROIs(ROIPolygons);
    		InvalidateProjs(e);  // all projections are invalid
    		DoProject(e,dir);
    		}
		// System.out.println("Take PolyROI");
		return;
    }
    
    public void ClearPolyROIs(int dir) {
        ROIPolygons[dir] = null;
        for (int e=0;e<Elements;e++)
            BundleAt(e).TakePolyROIs(ROIPolygons);
        InvalidateProjs(-1);  // all projections are invalid
    }
    
    public void ClearPolyROIs() {
        ROIPolygons[0] = null;
        ROIPolygons[1] = null;
        ROIPolygons[2] = null;
        for (int e=0;e<Elements;e++)
            BundleAt(e).TakePolyROIs(ROIPolygons);
        InvalidateProjs(-1);  // all projections are invalid
        }
    
    public void TakeLineROI(int ROIX,int ROIY, int ROIXe, int ROIYe,int dir)  // just defines a single plane
    {
        int ROIXs=0,ROIYs=0,ROIZs=0;
        double vecx=0.0,vecy=0.0,vecz=0.0;
        
        if (dir ==0)
         {
            vecx=0.0;
            ROIZs = ROIX;
            vecy = (ROIXe-ROIX);
            ROIYs = ROIY;
            vecz = -(ROIYe-ROIY);
         }
        else if (dir==1)
        {
            vecy=0.0;
            ROIXs = ROIX;
            vecz = (ROIXe-ROIX);
            ROIZs = ROIY;
            vecx = -(ROIYe-ROIY);
            }
        else
        {
            vecz=0.0;
            ROIXs = ROIX;
            vecy = (ROIXe-ROIX);
            ROIYs = ROIY;
            vecx = -(ROIYe-ROIY);
        }
        
        // vecx = 1.0; vecy = 0.0; vecz = 0.0;
        PlanesS[0].addElement(new Integer(ROIXs));PlanesS[1].addElement(new Integer(ROIYs));PlanesS[2].addElement(new Integer(ROIZs));
        PlanesD[0].addElement(new Double(vecx));PlanesD[1].addElement(new Double(vecy));PlanesD[2].addElement(new Double(vecz));
        // Just add the starting points to the Polygon
        for (int e=0;e<Elements;e++)
            BundleAt(e).TakePlaneROIs(PlanesS,PlanesD);
        InvalidateProjs(-1);  // all projections are invalid
        return;
    }
    
    public void ClearLineROIs() {
        PlanesS[0].removeAllElements();PlanesS[1].removeAllElements();PlanesS[2].removeAllElements();
        PlanesD[0].removeAllElements();PlanesD[1].removeAllElements();PlanesD[2].removeAllElements();
        for (int e=0;e<Elements;e++)
            BundleAt(e).TakePlaneROIs(PlanesS,PlanesD);
        InvalidateProjs(-1);
    }
    
    public Rectangle GetSqrROI(int dim) {
        return BundleAt(ActiveElement).rectROI.GetSqrROI(dim);
    }
    
    public void TakeROI(int ROIX,int ROIY, int ROIXe, int ROIYe,int dir)
    {
        int tmp;
        if (ROIX > ROIXe) {tmp=ROIX;ROIX=ROIXe;ROIXe=tmp;}
        if (ROIY > ROIYe) {tmp=ROIY;ROIY=ROIYe;ROIYe=tmp;}
        
        if (dir ==0)
        {
            ProjMin[2] = ROIX;ProjMax[2] = ROIXe;
            ProjMin[1] = ROIY;ProjMax[1] = ROIYe;
        }
        else if (dir==1)
        {
            ProjMin[0] = ROIX;ProjMax[0] = ROIXe;
            ProjMin[2] = ROIY;ProjMax[2] = ROIYe;
        }
        else
        {
            ProjMin[0] = ROIX;ProjMax[0] = ROIXe;
            ProjMin[1] = ROIY;ProjMax[1] = ROIYe;
        }
        for (int e=0;e<Elements;e++)
            BundleAt(e).UpdateSqrROI(ROIX,ROIY, ROIXe, ROIYe,dir);
        InvalidateProjs(-1);  // all projections are invalid
        return;
    }

    public void TakeDataToHistogram(My3DData data)
    {
        ActElement().DataToHistogramX = data.ElementAt(data.HistoX);
        ActElement().DataToHistogramY = data.ElementAt(data.HistoY);
        ActElement().DataToHistogramZ = data.ElementAt(data.HistoZ);    
        DataToHistogram = data;
    }
    
    AnElement GetGateElem(int t) {
        AnElement gate=null;
        if (GateElem >= 0 && GateActive) gate = ElementAt(GateElem,t);
        else
            gate = new OneElement(SizeX,SizeY,SizeZ);
        return gate;
    }
    
    AnElement GetGateElem() {
        return GetGateElem(ActiveTime);
    }
    
    void ComputeHistogram()
    {
        // double max = 
        ActElement().ComputeHistogram(DataToHistogram.GetGateElem(), DataToHistogram.ActROI());
        BundleAt(ActiveElement).SetMaxcs(ActElement().Max);
        BundleAt(ActiveElement).SetMincs(ActElement().Min);
        // ToggleColor(false);
        // ToggleModel(5);  // Log- colormap
        ToggleLog(1);  // logarithmic model
        ToggleOvUn(0);  // without over/underflow
    }

   void SetMinMaxCalib(ImagePlus myim,int dim)
   {
      double coeff[] = new double[2];
      coeff[0]=GetValueOffset(ActiveElement);
      coeff[1]=GetValueScale(ActiveElement);
      if (! colormode)
       {
       if (dim >= 0)
           // myim.getProcessor().setMinAndMax(0,BundleAt(ActiveElement).MaxCTable);  // Full range for slices and projections
           myim.getProcessor().setMinAndMax(0,Bundle.MaxCTable);  // Full range for slices and projections
       else
          myim.getProcessor().setMinAndMax((GetScaledMincs(ActiveElement)-coeff[0])/coeff[1],(GetScaledMaxcs(ActiveElement)-coeff[0])/coeff[1]);
       }
      ij.measure.Calibration myCal=myim.getCalibration();  // retrieve the spatial information
      double sx= GetScale(ActiveElement,0),sy= GetScale(ActiveElement,1),sz= GetScale(ActiveElement,2);
      double ox= GetOffset(ActiveElement,0),oy= GetOffset(ActiveElement,1),oz= GetOffset(ActiveElement,2);
	    	
      myCal.pixelWidth = sx;
      myCal.pixelHeight = sy;
      myCal.pixelDepth = sz;
      myCal.xOrigin = ox;
      myCal.yOrigin = oy;
      myCal.zOrigin = oz;
      myCal.setUnit(GetAxisUnits()[0]);

      if (dim >=0)
      {
          switch(dim)
            {
            case 0: sx = GetScale(ActiveElement,2); ox=GetOffset(ActiveElement,2); sy = GetScale(ActiveElement,1); oy=GetOffset(ActiveElement,1); break;
            case 1: sx = GetScale(ActiveElement,0); ox=GetOffset(ActiveElement,0); sy = GetScale(ActiveElement,2); oy=GetOffset(ActiveElement,2); break;
            case 2: sx = GetScale(ActiveElement,0); ox=GetOffset(ActiveElement,0); sy = GetScale(ActiveElement,1); oy=GetOffset(ActiveElement,1); break;
            }
          // myCal.setValueUnit("not calibrated");
      }
      else
        myCal.setFunction(ij.measure.Calibration.STRAIGHT_LINE, coeff, GetValueUnit(ActiveElement)); 

      // System.out.println("Calibrating Export: "+dim+", Max: "+ActElement().Max+ ", "+sx+", "+sy+", "+sz );
      // myCal.setValueUnit(GetValueUnit(ActiveElement));
   }
   
   // Export writes the current element back to ImageJ
   void Export(int dim, int pos) {
       ImagePlus myim=null;
       if (dim >= 0)
       {
           if (colormode)
                myim=getDisplayedSlice(dim,pos).ColorExport();
           else
                myim=getDisplayedSlice(dim,pos).Export();
       }
       else
       {
           if (ActElement() instanceof ByteElement)
                myim=NewImage.createByteImage("View5D Volume",SizeX,SizeY,SizeZ,NewImage.FILL_BLACK);
           if (ActElement() instanceof ShortElement)
                myim=NewImage.createShortImage("View5D Volume",SizeX,SizeY,SizeZ,NewImage.FILL_BLACK);
           if (ActElement() instanceof IntegerElement)
                myim=NewImage.createShortImage("View5D Volume",SizeX,SizeY,SizeZ,NewImage.FILL_BLACK);
           if (ActElement() instanceof FloatElement)
                myim=NewImage.createFloatImage("View5D Volume",SizeX,SizeY,SizeZ,NewImage.FILL_BLACK);
           if (ActElement() instanceof DoubleElement)
                myim=NewImage.createFloatImage("View5D Volume",SizeX,SizeY,SizeZ,NewImage.FILL_BLACK);

           for (int i=0;i<SizeZ;i++)
           {
                ActElement().CopySliceToSimilar(i,myim.getImageStack().getPixels(i+1));
           }
       }
       SetMinMaxCalib(myim,dim);
       myim.show();
       myim.updateAndDraw();
   }


    public void SetUp(int DataType, View5D_ myv3d, int elements, int times, int ImageType) {
    	// System.out.println("Setup loading ImageJ image");
    	
    	if (SizeX*SizeY*SizeZ == 0)
    	{
    		myv3d.add("South",new Label ("Error ! Image has zero sizes !\n"));
        	System.out.println("Error: Image has zero size");
    		myv3d.setVisible(true);
    	}
    	// ActiveElement=0;
    	// copy dataset to  arrays
    	int ijslice=0, z=0, elem=0, t=0;
    	try {

    		if (ImageType == ImagePlus.COLOR_RGB) elements/=3;
    		for (t=0;t<times;t++)  
    			for (elem=0;elem<elements;elem++)  
    				for (z=0;z<SizeZ;z++)  
    				{
    					switch(DimensionOrder)
    					{
    					case 0: ijslice=t*elements*SizeZ + elem *SizeZ + z; break;
    					case 1: ijslice=t*elements*SizeZ + z *elements + elem; break;
    					case 2: ijslice=elem*times*SizeZ + t *SizeZ + z; break;
    					case 3: ijslice=elem*times*SizeZ + z *elements + t; break;
    					case 4: ijslice=z*elements*times + t *elements + elem; break;
    					case 5: ijslice=z*elements*times + elem *times + t; break;
    					}

    					// System.out.println("Elem: " + Elements+", elements: "+elements+", time = "+t+", Filling element "+((Elements-elements)+elem)+"\n");
    					if (ImageType == ImagePlus.COLOR_RGB)
    					{int slice[] = (int[]) myv3d.GetImp().getImageStack().getPixels(ijslice+1);
    					ElementAt((Elements-3*elements)+elem,Times-times+t).ConvertSliceFromRGB(z,0,slice,1,0,2);  
    					ElementAt((Elements-3*elements)+elem+1,Times-times+t).ConvertSliceFromRGB(z,0,slice,1,0,1); 
    					ElementAt((Elements-3*elements)+elem+2,Times-times+t).ConvertSliceFromRGB(z,0,slice,1,0,0);
    					}
    					else
    						ElementAt((Elements-elements)+elem,Times-times+t).ConvertSliceFromSimilar(z,0,myv3d.GetImp().getImageStack().getPixels(ijslice+1),1,0);

    					//System.out.println("Optained Slice");
    					//System.out.println("Loaded Slice");
    					//if (ImageType == ImagePlus.COLOR_RGB)
    					// elem+=2;  // skip the next three
    				}
    	}
    	catch(Exception e)
    	{
    		System.out.println("Exception appeared during data setup ! ("+SizeX+", "+SizeY+", "+SizeZ+", z: "+z+", ijslice: "+ijslice+") \n");
    		System.out.println("Element ("+Elements+", "+elements+", " + elem+", T "+Times+", "+times+", " + t +") \n");
    		System.out.println("Exception was : "+e);
        	System.out.println("Setup loading ImageJ image");
    		e.printStackTrace();
    		myv3d.setVisible(true);
    	}
    	sizes[3]=Elements;
    	sizes[4]=Times;
     
     // initThresh();

     // AdjustThresh(false);
    }

    public void SaveMarkers() {  // saves a marker-file and generates a new set of marker list from it
      try {
        System.out.println("Saving marker file: "+markerOutfilename);
	FileOutputStream os=new FileOutputStream(markerOutfilename);

	if (AppendVersionNumber)  // to advance the number
	  {
		markerInfilename = markerOutfilename; // copies the old name
		  int nBeg=markerOutfilename.length()-1,nEnd=markerOutfilename.length(),Num=1;
		  String MyNumber;
		  // ist the last digit a number?
		  try{
		  while(nBeg >= 0) // look for the longest number before the ".txt" ending
			  { 
    		    MyNumber=markerOutfilename.substring(nBeg,nEnd);
                //System.out.println("Substring:"+MyNumber+"\n");
		  	    Num=Integer.parseInt(MyNumber);
		  	    nBeg--;
		  	  }}
		  catch (Exception e)
		      {}
		  if (nBeg == nEnd-1)  // last digit was not a number
		  {
			  nBeg -= 4; // skip the last three digits and try again:
			  nEnd -= 4;
			  try{
				  while(nBeg >= 0) // look for the longest number before the ".txt" ending
					  { MyNumber=markerOutfilename.substring(nBeg,nEnd);
		                //System.out.println("early Substring:"+MyNumber+"\n");
				  	    Num=Integer.parseInt(MyNumber);
				  	    nBeg--;
				  	  }}
				  catch (Exception e)
				      {}
		  }
          NumberFormat  nf = java.text.NumberFormat.getIntegerInstance(Locale.US);
          nf.setMinimumIntegerDigits(nEnd-nBeg-1);
          markerOutfilename=markerOutfilename.substring(0,nBeg+1)+nf.format(Num+1) + markerOutfilename.substring(nEnd,markerOutfilename.length());
	  }

	  Writer out = new BufferedWriter(new OutputStreamWriter(os));
	out.write(GetMarkerPrintout(this));
	out.close();
        os.close();
	}
        catch(IOException e)
            {
                System.out.println("saving markers, IOException:"+e);
                e.printStackTrace();
            }
        catch(Exception e)
            {
                System.out.println("saving markers, Memoryexception! "+e + "\n");
                e.printStackTrace();
            }

    }

    public void LoadMarkers() {  // loads a marker-file and generates a new set of marker list from it
            URL myurl=null;
	try {
			if (markerInfilename == null)
				System.out.println("Markerfile == null  !?\n");
			
            //if (applet instanceof View5D)
	        //myurl = new URL(((View5D) applet).getDocumentBase(), markerInfilename);
            //else
	     {
 	    if (markerInfilename.startsWith("http:") || markerInfilename.startsWith("file:") || markerInfilename.startsWith("ftp:")) 	    	
	        myurl = new URL(markerInfilename);
 	    else
            //if (applet instanceof View5D)
            // 	myurl = new URL(((View5D) applet).getDocumentBase(), markerInfilename);
 	    	// OLD: if (System.getProperty("os.name").startsWith("Windows"))
 	    		// OLD: myurl = new URL(protocol+":////"+markerInfilename);
 	    	//else // This is in ImageJ
 		    myurl = new URL("file:///"+markerInfilename);
 	    	// myurl = new URL(protocol+"://"+markerInfilename);

	    }
            System.out.println("Opening Markerfile "+myurl+"\n");
	    InputStream ifs=myurl.openStream();
            StreamTokenizer is = new StreamTokenizer(new BufferedReader(new InputStreamReader(ifs)));
	    is.commentChar('#');
	    is.slashSlashComments(true);
	    is.slashStarComments(true);
	    is.parseNumbers();
	    MyMarkers.ListOffset=MyMarkers.NumLists;
	    while (MyMarkers.readline(is,this))   // reads a line and inserts/updates the appropriate marker
	    	;
	    MyMarkers.DeleteDublicateMarkerLists();
	    ifs.close();
	} catch(MalformedURLException e)
	    {
            	System.out.println("In Markerfile "+myurl+"\n");
		System.out.println("reading markers, URLException:"+e);
		applet.add("South",new Label ("URLException:"+e +"\n"));
                e.printStackTrace();
		applet.setVisible(true);
		// System.exit(1);
	    }
	catch(IOException e)
	    {
            	System.out.println("In Markerfile "+myurl+"\n");
		System.out.println("reading markers, URLException:"+e);
		applet.add("South",new Label ("IOException: "+e +"\n"));
		//applet.add("South",new Label ("Error: Unable to load file  "+markerInfilename));
                e.printStackTrace();
		applet.setVisible(true);
		// System.exit(1);
	    }
	catch(Exception e)
	    {
            	System.out.println("In Markerfile "+myurl+"\n");
		applet.add("South",new Label ("Exception appeared during load! \n"+e+"\n"));
                System.out.println("reading markers, Memoryexception! "+e + "\n");
                e.printStackTrace();
		applet.setVisible(true);
	    }
}
    
    
    public void Load(int DataType, int NumBytes, int NumBits, String filename, Applet rapplet) { 
	try {
	    URL myurl = new URL(rapplet.getDocumentBase(), filename);
            BufferedInputStream is = new BufferedInputStream(myurl.openStream());

            PrevType = DataType;
            PrevBytes = NumBytes;
            PrevBits = NumBits;
            
	    int tread=0;  // bytes read
	    int bread=0;  // bytes read
	    int slice=0;  
	    ActiveElement=0;
	    int elem=0,time=0;  // counting elements 
	    int SliceSize=SizeX*SizeY;  

	    byte Ibuffer[]=null;
            
	    System.out.println("Loading ... ");
	    System.out.println("Type Nr (from # bytes, -1 = float): [0: byte, 1: int, 2: float, 3:double, 4:complex(float)]"+DataType);
	    System.out.println("Number of Bytes :"+NumBytes);
	    System.out.println("Number of Bits :"+NumBits);
	    
            Ibuffer=new byte[NumBytes*SizeX*SizeY*SizeZ];   // A buffer for storing the integers of one cube
            SliceSize *= NumBytes;
	    System.out.println("SliceSize :"+SliceSize);
	
            do {
		do {
		    // System.out.println("Reading slice "+slice);
			bread=is.read(Ibuffer,tread+slice*SliceSize,SliceSize-tread); // load a single slice into byte buffer
		    tread+=bread;
		} while (tread < SliceSize && bread != -1);
		tread = 0;
                // System.out.println("Reading ... Elem:"+elem+", time: "+time+" out of "+Elements);
                if (bread != -1)
                {
                	// System.out.println("bread :"+bread);
                ElementAt(elem,time).ConvertSliceFromByte(slice,slice,Ibuffer,1,0);
                slice ++;
		if (slice == SizeZ)  // Its time to swich to a different element
		     {
			 if (time < Times-1)
			    time++;
                         else
                         {
                             time=0;
                             elem++;
                         }
			 /*if (elem < Elements-1)
			    elem++;
                         else
                         {
                             elem=0;
                             time++;
                         }*/
		 	slice = 0;
		     }
                }
                // else System.out.println(".. ignored");
	    }
	    while (bread != -1);

            for (int e=0;e<Elements;e++)
            {
                AnElement ne = ElementAt(e);
                ne.SetMinMax();
                if (ne instanceof FloatElement || ne instanceof DoubleElement)
                    { ne.MaxValue = ne.Max; ne.shift = ne.Min;}
            }

	    is.close();
	    // applet.add("East",new Label ("Data read successfully" +"\n"));
	} catch(MalformedURLException e)
	    {
		System.out.println("URLException:"+e);
		applet.add("South",new Label ("URLException:"+e +"\n"));
                e.printStackTrace();
		applet.setVisible(true);
		// System.exit(1);
	    }
	catch(IOException e)
	    {
		System.out.println("URLException:"+e);
		applet.add("South",new Label ("IOException:"+e +"\n"));
		applet.add("South",new Label ("Error: Unable to load file  "+filename+"\n"));
                e.printStackTrace();
		applet.setVisible(true);
		// System.exit(1);
	    }
	catch(Exception e)
	    {
		applet.add("South",new Label ("Exception appeared during load!\n"+e+"\n"));
                System.out.println("Memoryexception appeared during data setup ! Element: "+e + "\n");
                e.printStackTrace();
		applet.setVisible(true);
	    }
}

    public boolean GetProjectionMode(int DimNr) {  // returns whether projection mode is "on"
        return ProjMode[DimNr];
    }
    
    public boolean GetMIPMode(int DimNr) {  // returns whether projection mode is "on"
        return BundleAt(ActiveElement).MIPMode;
    }
    
    public boolean GetLogMode() {  // returns whether projection mode is "on"
        return BundleAt(ActiveElement).LogScale;
    }

    ASlice ActProj(int DimNr) {
        return (ASlice) MyProjections[DimNr].elementAt(ActiveElement);
    }
    
    ASlice ProjAt(int DimNr, int e) {
        return (ASlice) MyProjections[DimNr].elementAt(e);
    }

    public void ToggleProj(int DimNr,boolean mipmode) {
        if (mipmode != BundleAt(ActiveElement).MIPMode)
            InvalidateProjs(-1);
	BundleAt(ActiveElement).MIPMode = mipmode;
        ProjMode[DimNr] = ! ProjMode[DimNr];
    }

  @SuppressWarnings("unchecked")
public My3DData(My3DData other) {   // operates on the same data but allows different views and projections
    DataToHistogram=other.DataToHistogram;
    applet=other.applet;
    SizeX=other.SizeX; sizes[0]=other.sizes[0];
    SizeY=other.SizeY; sizes[1]=other.sizes[1];
    SizeZ=other.SizeZ; sizes[2]=other.sizes[2];
 	
	if (System.getProperty("os.name").startsWith("Windows"))
	{
        if (markerInfilename == null) markerInfilename="C:\\temp\\markers.txt";
        if (markerOutfilename == null) markerOutfilename="C:\\temp\\markers.txt";
	}
	else
	{
        if (markerInfilename == null) markerInfilename="/tmp/markers.txt";
        if (markerOutfilename == null) markerOutfilename="/tmp/markers.txt";
	}
   
    Elements=other.Elements;
    sizes[3]=other.sizes[3];
    Times=other.Times;
    sizes[4]=other.sizes[4];
    PrevBytes=other.PrevBytes;
    PrevBits=other.PrevBits;
    MyMarkers=other.MyMarkers; // Markers always will be identical
    HistoX=other.HistoX;
    HistoY=other.HistoY;
    HistoZ=other.HistoZ;
    
    elemR =other.elemR;
    elemG =other.elemG;
    elemB =other.elemB;
    GateElem =other.GateElem;
    GateActive =other.GateActive;
    PlanesS= (Vector<Integer>[]) new Vector[3];
    PlanesD= (Vector<Double>[]) new Vector[3];
    PlanesS[0] = new Vector<Integer>();PlanesS[1] = new Vector<Integer>();PlanesS[2] = new Vector<Integer>();
    PlanesD[0] = new Vector<Double>();PlanesD[1] = new Vector<Double>();PlanesD[2] = new Vector<Double>();
    ROIPolygons = new Polygon[3];
    
    ProjMin = new int[3];ProjMax = new int[3];
    ProjMin[0]=other.ProjMin[0];ProjMin[1]=other.ProjMin[1];ProjMin[2]=other.ProjMin[2];
    ProjMax[0]=other.ProjMax[0];ProjMax[1]=other.ProjMax[1];ProjMax[2]=other.ProjMax[2];
    ProjMode = new boolean[3];
    ProjMode[0]=other.ProjMode[0]; ProjMode[1]=other.ProjMode[1];ProjMode[2]=other.ProjMode[2];
  
    MySlice = new ASlice[3];
    MyColorSlice = new ASlice[3];    // this manage the ZY, XZ and XY slices
    // MyProjections = new Vector[3];    // these manage progections
    // MyProjections[0] = new Vector();MyProjections[1] = new Vector();MyProjections[2] = new Vector();

    // MyBundle = new Vector();
    
    try {
	MyElements = other.MyElements;
        Elements = other.Elements;       // Contains the data 
        MyTimes = other.MyTimes;         // Contains the data
        Times = other.Times;
        MyBundle = new Vector<Bundle>();
        MyTimeProj = new Vector<Vector<ASlice>[]>();  // stores arrays[3] of vectors
        MyTimeColorProj = new Vector<ASlice[]>();
        // something is wrong with the line below
        //MyBundle = (Vector) other.MyBundle.clone();  // clones the full vector with all its elements
        for (int e=0;e < other.Elements;e++)
            MyBundle.addElement((Bundle) other.BundleAt(e).clone());

        for (int t=0;t < other.Times;t++)
            {
            MyProjections = (Vector<ASlice>[]) new Vector[3];    // these manage progections
            MyProjections[0] = new Vector<ASlice>();
            MyProjections[1] = new Vector<ASlice>();
            MyProjections[2] = new Vector<ASlice>();
            for (int e=0;e < other.Elements;e++)
                {
                MyProjections[0].addElement(new ASlice(0,other.ElementAt(e)));
                MyProjections[1].addElement(new ASlice(1,other.ElementAt(e)));
                MyProjections[2].addElement(new ASlice(2,other.ElementAt(e)));
                }
            MyTimeProj.addElement(MyProjections);
            }
    MyElements = MyTimes.elementAt(0);
    MyProjections = MyTimeProj.elementAt(0);

    MySlice[0] = new ASlice(0,((AnElement) MyElements.firstElement()));
    MySlice[1] = new ASlice(1,((AnElement) MyElements.firstElement()));
    MySlice[2] = new ASlice(2,((AnElement) MyElements.firstElement()));
    MyColorSlice[0] = new ASlice(0,((AnElement) MyElements.firstElement()));
    MyColorSlice[1] = new ASlice(1,((AnElement) MyElements.firstElement()));
    MyColorSlice[2] = new ASlice(2,((AnElement) MyElements.firstElement()));
    for (int t=0; t < other.Times; t++)
      {
        MyColorProjection = new ASlice[3];    // this manages color projections
        MyColorProjection[0]=new ASlice(0,(AnElement) ElementsAtTime(t).firstElement());
        MyColorProjection[1]=new ASlice(1,(AnElement) ElementsAtTime(t).firstElement());
        MyColorProjection[2]=new ASlice(2,(AnElement) ElementsAtTime(t).firstElement());
        MyTimeColorProj.addElement(MyColorProjection);
      }
    MyColorProjection = (ASlice []) MyTimeColorProj.elementAt(0);
    ActiveElement=other.ActiveElement;
    ActiveTime=other.ActiveTime;
    }
    catch(Exception e)
	{
	    System.out.println("Exception appeared during spawning viewer !\n");
	    e.printStackTrace();
	    applet.add("South",new Label ("Exception appeared during spawning viewer!\n"));
	    applet.setVisible(true);
	}
    	
    ClearPolyROIs();  // Generates Empty Polygons
    InvalidateProjs(-1);  // All projections invalid
    InvalidateColor();
    InvalidateSlices();
    colormode=other.colormode;
    ShowAllLists=other.ShowAllLists;
    // ShowAllSlices=other.ShowAllSlices;
    ShowAllTrees=other.ShowAllTrees;
    ShowAllTrack=other.ShowAllTrack;
    ShowFullTrace=other.ShowFullTrace;
    ShowSpectralTrack=other.ShowSpectralTrack;
    TrackDirection=other.TrackDirection;
 }

 @SuppressWarnings("unchecked")
public My3DData(Container myapp,int sizex,int sizey, int sizez, 
                    int elements, int times,
                    int redEl,int greenEl, int blueEl, 
                    int hisx,int hisy,int hisz,
                    int myType, int NumBytes, int NumBits,
                    double[] Scales,
                    double[] Offsets,
                    double ScaleV,double OffsetV, 
                    String [] Names, String[] Units) {
    applet=myapp;
    DataToHistogram=null;
    SizeX=sizex; sizes[0]=sizex;
    SizeY=sizey; sizes[1]=sizey;
    SizeZ=sizez; sizes[2]=sizez;
    sizes[3]=elements;
    sizes[4]=times;
	if (System.getProperty("os.name").startsWith("Windows"))
	{
        if (markerInfilename == null) markerInfilename="C:\\temp\\markers.txt";
        if (markerOutfilename == null) markerOutfilename="C:\\temp\\markers.txt";
	}
	else
	{
        if (markerInfilename == null) markerInfilename="/tmp/markers.txt";
        if (markerOutfilename == null) markerOutfilename="/tmp/markers.txt";
	}
    
    try {
    ProjMin = new int[3];ProjMax = new int[3];
    
    ProjMin[0]=0;ProjMin[1]=0;ProjMin[2]=0;
    ProjMax[0]=SizeX-1;ProjMax[1]=SizeY-1;ProjMax[2]=SizeZ-1;
    ProjMode = new boolean[3]; ProjMode[0] = false; ProjMode[1] = false; ProjMode[2] = false;
   
    PlanesS= (Vector<Integer>[]) new Vector[3];
    PlanesD= (Vector<Double>[]) new Vector[3];
    PlanesS[0] = new Vector<Integer>();PlanesS[1] = new Vector<Integer>();PlanesS[2] = new Vector<Integer>();
    PlanesD[0] = new Vector<Double>();PlanesD[1] = new Vector<Double>();PlanesD[2] = new Vector<Double>();

    ROIPolygons = new Polygon[3];
    
    MySlice = new ASlice[3];
    MyColorSlice = new ASlice[3];    // this manage the ZY, XZ and XY slices

    MyMarkers = new MarkerLists();    // stores a number of points

    HistoX=hisx; HistoY=hisy;HistoZ=hisz;

    } catch(Exception e)
	{
            System.out.println("Initialization exception !\n");
	    e.printStackTrace();
	    applet.add("South",new Label ("Error initializing !\n"));
	    applet.setVisible(true);
	}
    try {
        MyTimes = new Vector<Vector<AnElement>>();
        MyTimeProj = new Vector<Vector<ASlice>[]>();  // stores arrays[3] of vectors
        MyTimeColorProj = new Vector<ASlice[]>();
        MyBundle = new Vector<Bundle>();
        Elements = 0;
        Times = times;
	for (int t=0; t < times; t++)
        {
            MyElements = new Vector<AnElement>();   // A list of elements is generated for each timepoint
            MyTimes.addElement(MyElements);  // However, all times use the same list of elements.
            MyProjections = (Vector<ASlice>[]) new Vector[3];    // these manage progections
            MyProjections[0] = new Vector<ASlice>();
            MyProjections[1] = new Vector<ASlice>();
            MyProjections[2] = new Vector<ASlice>();
            MyTimeProj.addElement(MyProjections);
        }

        MyElements = MyTimes.elementAt(0);
        MyProjections = MyTimeProj.elementAt(0);
        String [] nNames = (String []) Names.clone();
        String [] nUnits = (String []) Units.clone();
        
	for (int e = 0; e < elements; e++) {  // The line below will generate a new element for every time point
            GenerateNewElement(myType,NumBytes,NumBits,Scales,Offsets,ScaleV,OffsetV,nNames,nUnits);
            }
	for (int t=0; t < times; t++)
          {
            MyColorProjection = new ASlice[3];    // this manages color projections
            MyColorProjection[0]=new ASlice(0,(AnElement) ElementsAtTime(t).firstElement());
            MyColorProjection[1]=new ASlice(1,(AnElement) ElementsAtTime(t).firstElement());
            MyColorProjection[2]=new ASlice(2,(AnElement) ElementsAtTime(t).firstElement());
            MyTimeColorProj.addElement(MyColorProjection);
          }
        MyColorProjection = (ASlice []) MyTimeColorProj.elementAt(0);
        }
    catch(Exception e)
	{
            System.out.println("Memoryexception appeared ! Need more space for array !\n");
	    e.printStackTrace();
	    applet.add("South",new Label ("Memoryexception appeared ! Need more space for array !\n"));
	    applet.setVisible(true);
	}
    if (Elements!=elements)
        System.out.println("Error initializing elements: Wrong count: Elements = "+Elements+", elements = "+elements+"\n");
    
    MySlice[0] = new ASlice(0,((AnElement) MyElements.firstElement()));
    MySlice[1] = new ASlice(1,((AnElement) MyElements.firstElement()));
    MySlice[2] = new ASlice(2,((AnElement) MyElements.firstElement()));
    MyColorSlice[0] = new ASlice(0,((AnElement) MyElements.firstElement()));
    MyColorSlice[1] = new ASlice(1,((AnElement) MyElements.firstElement()));
    MyColorSlice[2] = new ASlice(2,((AnElement) MyElements.firstElement()));

    ClearPolyROIs();  // Generates Empty Polygons
    elemR =-1;elemG =-1;elemB =-1;
    if (redEl >= 0)
	MarkChannel(redEl,0);
    if (greenEl >= 0)
	MarkChannel(greenEl,1);
    if (blueEl >= 0)
	MarkChannel(blueEl,2);
 }

 private void DoProject(int elem, int dim)
 {
    // System.out.println("DoProject, Bundle: "+BundleAt(elem).ProjValid[dim]+", Proj: "+ProjAt(dim,elem).isValid);
    if (BundleAt(elem).ProjValid[dim] && ProjAt(dim,elem).isValid)  // No need to project, since all projections are still valid
        return;
    // System.out.println("projecting ...");
    ActProj(dim).setMIPMode(BundleAt(elem).MIPMode);
    ProjAt(dim,elem).DoProject(dim,ElementAt(elem),GetGateElem(), ActROI());
    BundleAt(elem).ProjValid[dim] = true;
    MyColorProjection[dim].Invalidate();
 }

boolean InOverlayDispl(int e) {
    return GetBundleAt(e).InOverlayDispl();
}

public void ToggleOverlayDispl(int val) {
    GetBundleAt(ActiveElement).ToggleOverlayDispl(val);
}

public void ToggleMulDispl(int val) {
    GetBundleAt(ActiveElement).ToggleMulDispl(val);
}
 
Image GiveSection(int dim,int pos) {
    ASlice ms=getDisplayedSlice(dim,pos);

    if (colormode)
      {
          if (! ProjMode[dim])
          {
                ms.ClearColor();
                for (int e=0;e<Elements;e++)
                {
                    if (GetBundleAt(e).MulOverlayDispl())  // has this element been selected as multiplicative color?
                    {
                        ElementAt(e).SetScaleShift(Getmincs(e),Getmaxcs(e));
                        ms.MulToColorSlice(pos,ElementAt(e),GetBundleAt(e).cmapRed,GetBundleAt(e).cmapGreen,GetBundleAt(e).cmapBlue,GetGateElem(),ElementAt(ActiveElement));
                    }
                    if (GetBundleAt(e).InOverlayDispl())  // if an RGB color is selected, these will be active
                    {
                        ElementAt(e).SetScaleShift(Getmincs(e),Getmaxcs(e));
                        ms.SumToColorSlice(pos,ElementAt(e),GetBundleAt(e).cmapRed,GetBundleAt(e).cmapGreen,GetBundleAt(e).cmapBlue,GetGateElem(),ElementAt(ActiveElement));
                    }
                }
          }
          else  // Projmode[dim]
          {
            for (int e=0;e<Elements;e++)
		if (GetBundleAt(e).InOverlayDispl())  // if an RGB color is selected, these will be active
                   DoProject(e,dim);  // Also checks if really necessary
            // System.out.println("Color projection state: "+ms.isValid+"\n");
            
             // if (! ms.isValid) 
                 ComputeColorProj(dim);
          }
          return ms.GenColorImage(applet);
      }
  else
      {
        ms.TakeModel(BundleAt(ActiveElement).ElementModel);
        return ms.GenImage(applet);
       }
 }

ASlice getDisplayedSlice(int dim, int pos)
{
  if (colormode)
      {
       if (ProjMode[dim])
            return MyColorProjection[dim];
       else
            return MyColorSlice[dim];
       }
  else
  {
    AnElement mye=ActElement();
    mye.SetScaleShift(Getmincs(ActiveElement),Getmaxcs(ActiveElement));
    if (ProjMode[dim])
        {
        DoProject(ActiveElement,dim);
        return ActProj(dim);
        }
    else
        {
        MySlice[dim].UpdateSlice(pos,mye,GetGateElem());
        return MySlice[dim];
        }
  }
}

void Clear(int e)
{
    ElementAt(e).Clear();
}

}  // end of class My3DData
