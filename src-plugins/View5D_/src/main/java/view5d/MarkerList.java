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


import java.util.*;

class MarkerList extends Object {
    int NumPoints = 0;  // Number of Points in the active list
    int ActivePoint = -1;
    public Vector<APoint> MyPoints=null;    // currently active list of positions
    MarkerList Parent1List=null;    // the list from which this list was inherited
    MarkerList Parent2List=null;    // the list from which this list was inherited
    MarkerList Child1List=null;    // the list from which this list was inherited
    MarkerList Child2List=null;    // the list from which this list was inherited
    int PreferredChild=1;          // to remember the preferred way the user wants to navigate
    boolean toggling=false;
    static final int Attributes=20;   // number of attributes in marker lists
    public String MyName="Unknown";

    public MarkerList() {
        NewList();
      }

    void Link1To(MarkerList Parent)
    {
    	if (Parent == null) return;
    	Parent1List=Parent;   // stores the link, not the number
    	if (Parent1List.Child1List == null)
    		Parent1List.Child1List = this;
    	else if (Parent1List.Child2List == null)
    		Parent1List.Child2List = this;
    	if (NumPoints > 0)
    		{
    		SetColor(Parent.GetPoint(0).mycolor);   // use the color of the parent
    		System.out.println("Link1To Chose color " + Parent.GetPoint(0).mycolor + "\n");
    		}
    	}
    
    void Link2To(MarkerList Parent)
    {
    	if (Parent == null) return;
    	Parent2List=Parent;   // stores the link, not the number
    	if (Parent2List.Child1List == null)
    		Parent2List.Child1List = this;
    	else if (Parent2List.Child2List == null)
    		Parent2List.Child2List = this;
    	if (NumPoints > 0)
    		{
    		SetColor(Parent.GetPoint(0).mycolor);   // use the color of the parent
    		System.out.println("Link2To Chose color " + Parent.GetPoint(0).mycolor + "\n");
    		}
    	}
    
    void NewList() {
        if (MyPoints != null && NumPoints == 0)
            return; // No New list here, since an empty list would be left behind
      MyPoints = new Vector<APoint>();    // stores a number of points
      NumPoints = 0;
      ActivePoint=-1;
    }

    void ToggleColor() {
        int color=APoint.NewColor();
        SetColor(color);
    }
    
    int GetColor() {
    	APoint pt=GetPoint(0);
    	if (pt != null)
    		return pt.mycolor;
    	else
    		return 0;
    }

    void SetColor(int color) {
    	if (toggling == true)
    		{
    		// System.out.println("Warning! Detected Cyle of dependencies in Parents of Marker lists");
    		return;
    		}
    	toggling=true;
        for (int i=0;i<NumPoints;i++)
        {
            GetPoint(i).mycolor=color;
        }
        if (Parent1List != null)
        	Parent1List.SetColor(color);
        if (Parent2List != null)
        	Parent2List.SetColor(color);
        
        if (Child1List != null && Child1List.toggling == false)
        	Child1List.SetColor(color);

        if (Child2List != null && Child2List.toggling == false)
        	Child2List.SetColor(color);
        toggling=false;
    }

    void AddPoint(APoint p) {
        if (NumPoints > 0)
            p.mycolor = GetPoint(0).mycolor;  // all have the same color as the first one
        
	// MyPoints.addElement(p);
        if (ActivePoint < 0) 
        {
            ActivePoint=0;
            MyPoints.addElement(p);
        }
        else
        {
            ActivePoint++;
            MyPoints.insertElementAt(p,ActivePoint);
        }
	NumPoints ++;
    }

    boolean RemovePoint() {
        //System.out.println("Remove Point, NumPoints:"+NumPoints+", size: "+MyPoints.size()+"\n");
        //System.out.println("Active Point:"+ActivePoint+"\n");
        if (NumPoints <= 0) return false;
        MyPoints.removeElementAt(ActivePoint);
        NumPoints --;
        //System.out.println("Points left:"+NumPoints+"\n");
        if (ActivePoint >= NumPoints) 
            {
                ActivePoint = NumPoints-1;
                return false;
            }
        return (NumPoints > 0);
    }

    void RemoveTrailingPoints() {
        while (RemovePoint())
            ;
    }
    
    void AdvancePoint(int howmany) {
        if (NumPoints <= 0) return;
	ActivePoint+=howmany;
	if (ActivePoint < 0) ActivePoint += NumPoints;
	if (ActivePoint >= NumPoints) ActivePoint = ActivePoint % NumPoints;
    }

    int GetMarkerNr(APoint pt) {
    	return MyPoints.indexOf(pt);  // returns -1 if object is not found
    }

    int ClosestTrackMarker(APoint other, int dir, int step)  // returns the marker who is the closest along the track direction
    {
		int MinP=0;
		double mindist=Math.abs(GetPoint(MinP).DistTo(other, dir)) - step; 
    	for (int p=1;p<NumMarkers();p++)
    	{
    		APoint Pt=GetPoint(p);
    		double dist=Math.abs(Pt.DistTo(other, dir) - step); 
    		if (dist==0 && step == 1)  // Take the first marker that fits in forward and the last that fits in backward direction
    			return p;
    		if (dist <= mindist)
    		{	
    			mindist = dist;
    			MinP = p;
    		}
    		
    	}
    return MinP;
    }

    APoint MarkerFromPosition(double px, double py, int dir, double DistX, double DistY)  // returns the first marker who is in the range
    {
    	for (int p=0;p<NumMarkers();p++)
    	{
    		APoint Pt=GetPoint(p);
    		if (Pt.isDisplayed)
    			if (Pt.InRange(px,py,dir,DistX,DistY)) 
    				return Pt;
    	}
    return null;
    }
    
    String GetMarkerListName() {
    	return MyName;
    }

    void SetActiveMarkerClosest(APoint other, int dir, int step) {
        if (NumPoints <= 0) return;
        if (other==null)
        	ActivePoint = 0;
        else
        	ActivePoint = ClosestTrackMarker(other,dir,step);
    }

    void SetActiveMarker(int pos) {
        if (NumPoints <= 0) return;
        ActivePoint = pos;
    }

    APoint GetPoint(int p) {
    	if (MyPoints == null) return null;
        int NPoints = MyPoints.size();
        if (NPoints <= 0) return null;
        
        if (p < 0) p=ActivePoint;
        if (p >= NPoints) p = p % NPoints;
        if (p < 0) p = 0;  // This can happen, if the active list is cleared and Active Point in the current list is searched for
        // System.out.println("Get Point Nr "+p+", coords ("+((APoint) MyPoints.elementAt(p)).coord[3] +", "+((APoint) MyPoints.elementAt(p)).coord[4]+")");
        //(p < 0) return null;
        return (APoint) MyPoints.elementAt(p);
    }

    int NumMarkers() 
    {
        return MyPoints.size();
    }

    int ActiveMarkerPos() {return ActivePoint;}

    double [][] ExportMarkers()
    {
        int listlength=NumMarkers();
        double [][] markers= new double[listlength][Attributes];   // one for parentlist and two for the child lists but empty for now
        APoint point;
        for (int i=0;i<listlength;i++)
        {
            point=GetPoint(i);
            markers[i][0]=point.coord[0];
            markers[i][1]=point.coord[1];
            markers[i][2]=point.coord[2];
            markers[i][3]=point.coord[3];
            markers[i][4]=point.coord[4];
            markers[i][5]=point.integral;
            markers[i][6]=point.max;       
            					// for the moment incorrect real world information, but this is corrected in the MarkerLists class
            markers[i][7]=point.coord[0];
            markers[i][8]=point.coord[1];
            markers[i][9]=point.coord[2];
            markers[i][10]=point.coord[3];
            markers[i][11]=point.coord[4];
            
            markers[i][12]=point.integral;
            markers[i][13]=point.max;       
            
            if (point.tagged)
              markers[i][14]=1;
              else
              markers[i][14]=0;

            markers[i][15]=-1;
            markers[i][16]=-1;
            markers[i][17]=-1;
            markers[i][18]=-1;
            markers[i][19]=point.mycolor;
        }
        return markers;
    }
 
}

