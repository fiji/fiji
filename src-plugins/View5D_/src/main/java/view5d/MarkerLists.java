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

import java.io.*;
import java.util.*;
import java.text.*;
import ij.*;
import ij.gui.*;

public class MarkerLists extends Object { // This class manages multiple lists of point markers
	MarkerList MyActiveList = null;
    int NumLists = 0;
    int ActiveList = -1;
    int ListOffset = 0;
    int CurrentNameNr=1;
    Vector<MarkerList> ListOfLists;   // marker coordinates are stored as a list of APoint objects in pixel coordinates here
    //public Vector MyPoints=null;    // currently active list of positions
    static int dx=3,dy=3;  // size of the marker in pixels
    
    public MarkerLists() {
      ListOfLists = new Vector<MarkerList>();
      NewList();
    }

    int NumMarkers(int lpos) 
    {
    	if (NumLists < 1) return 0;  // no lists -> no markers
        if (lpos < 0) lpos = ActiveList; 
        //System.out.println("Num Markers ("+lpos+"):"+((Vector) ListOfLists.elementAt(lpos)).size());
    	if (ListOfLists != null && lpos >=0)
    		{
    		MarkerList MyMarkerList=GetMarkerList(lpos);
    		if (MyMarkerList != null)
    			return MyMarkerList.NumMarkers();
    		else return 0;
    		}
    	else return 0;
    }
    
    int NumMarkerLists() {return NumLists;}
    int ActiveMarkerListPos() {return ActiveList;}
    MarkerList ActiveMarkerList() {return MyActiveList;}    

    MarkerList GetMarkerList(int list) {
    	if (list < 0) return null;
    	if (ListOfLists != null)
    		return (MarkerList) ListOfLists.elementAt(list);
    	else
    		return null;
    }
    
    APoint GetPoint(int p, int list) {
        MarkerList alist;
        if (list < 0) alist=MyActiveList;
        else alist=GetMarkerList(list);
        if (alist==null) return null;
        return alist.GetPoint(p);
    }

    void AddPoint(APoint p) {
    	if (NumLists < 1) NewList();
    	MyActiveList.AddPoint(p);
    }
    
    void RemovePoint() {
    	if (NumLists < 1) return;  // nothing to remove
    	MyActiveList.RemovePoint();
    }

    void RemoveTrailingPoints() {
    	if (NumLists < 1) return;  // nothing to remove
    	MyActiveList.RemoveTrailingPoints();
    }

    boolean AdvancePoint(int howmany, int TrackDirection) {  // TrackDirection is needed to get the right position in the childs
    	if (NumLists < 1) return false;
    	int CurPos=MyActiveList.ActivePoint;
    	// APoint CurPt=MyActiveList.GetPoint(CurPos);
    	if (CurPos + howmany >= MyActiveList.NumPoints)
    		{
    		if (MyActiveList.Child1List != null && (MyActiveList.Child2List == null || MyActiveList.PreferredChild==1))
    		{
    			SetActiveList(GetChild1Index(ActiveList)); MyActiveList.SetActiveMarker(0);
    			// MyActiveList.SetActiveMarkerClosest(CurPt, TrackDirection, howmany);
    			return true;
    		}
    		else if (MyActiveList.Child2List != null && (MyActiveList.Child1List == null || MyActiveList.PreferredChild==2))
    		{
    			SetActiveList(GetChild2Index(ActiveList)); MyActiveList.SetActiveMarker(0);
    			// MyActiveList.SetActiveMarkerClosest(CurPt, TrackDirection, howmany);
    			return true;
    		}
    		return false;
    		}
        else if (CurPos + howmany < 0)
        {
    		if (MyActiveList.Parent1List != null)
    		{
    			if (MyActiveList == MyActiveList.Parent1List.Child1List) // remember which way the user came
    				MyActiveList.Parent1List.PreferredChild = 1;
    			if (MyActiveList == MyActiveList.Parent1List.Child2List)
    				MyActiveList.Parent1List.PreferredChild = 2;
    		   	//System.out.println("Devancing from preferred Child" + MyActiveList.ParentList.PreferredChild);
    			SetActiveList(GetParent1Index(ActiveList));  MyActiveList.SetActiveMarker(MyActiveList.NumMarkers()-1);
    			// MyActiveList.SetActiveMarkerClosest(CurPt, TrackDirection, howmany);
    			return true;
    			}
    		if (MyActiveList.Parent2List != null)
    		{
    			if (MyActiveList == MyActiveList.Parent2List.Child1List) // remember which way the user came
    				MyActiveList.Parent2List.PreferredChild = 1;
    			if (MyActiveList == MyActiveList.Parent2List.Child2List)
    				MyActiveList.Parent2List.PreferredChild = 2;
    		   	//System.out.println("Devancing from preferred Child" + MyActiveList.ParentList.PreferredChild);
    			SetActiveList(GetParent2Index(ActiveList));  MyActiveList.SetActiveMarker(MyActiveList.NumMarkers()-1);
    			// MyActiveList.SetActiveMarkerClosest(CurPt, TrackDirection, howmany);
    			return true;
    			}
        return false;
        }
        MyActiveList.AdvancePoint(howmany);
        return true;
    }
    
    int ActiveMarkerPos() 
    {
    	if (NumLists < 1) return 0;  
    	return MyActiveList.ActiveMarkerPos();
    }

    boolean CommonRoot(int list1,int list2) 
    {
    	MarkerList Root1=GetMarkerList(list1);
    	MarkerList Root2=GetMarkerList(list2);
    	while (Root1.Parent1List != null)
    		Root1=Root1.Parent1List;        // cycles will kill this here!
    	while (Root2.Parent1List != null)
    		Root2=Root2.Parent1List;        // cycles will kill this here!
    	return Root1==Root2;
    }

    void SetActiveMarker(int pos) {
    	if (NumLists < 1) return; 
        MyActiveList.SetActiveMarker(pos);
    }

    void SetActiveMarker(APoint pt) {
    	if (NumLists < 1) return;  
    	for (int nr=0;nr<NumLists;nr++)  // find the list this marker belongs to
    	{
    		int mymarker=GetMarkerList(nr).GetMarkerNr(pt);
    		if (mymarker >= 0)
    		{
    		  SetActiveList(nr);
    		  SetActiveMarker(mymarker);
    		  return;
    		}
    	}
    }

    void ToggleColor() {
    	if (NumLists < 1) return;  // nothing to remove
        MyActiveList.ToggleColor();
    }
    
    public void ImportPositions(float [][] positions)   // this works only for one list
    {
       int numpos = positions.length;
       for (int n=0;n<numpos;n++)
          {
    	   	MyActiveList.AddPoint(new APoint(positions[n][0],positions[n][1],positions[n][2],positions[n][3],positions[n][4]));
          }
    }
    
    void SetActiveList(int listnr) {
       	ActiveList = listnr;	
    	MyActiveList = GetMarkerList(ActiveList);
    }

    int GetActiveList() {
       	return ActiveList;	
    }

    int GetPrefChildListNr(int alist) {
    	if (alist < 0) alist=ActiveList;
		if (GetMarkerList(alist).Child1List != null && (GetMarkerList(alist).Child2List == null || GetMarkerList(alist).PreferredChild==1))
			return GetChild1Index(alist);
		else if (GetMarkerList(alist).Child2List != null && (GetMarkerList(alist).Child1List == null || GetMarkerList(alist).PreferredChild==2))
			return GetChild2Index(alist);
       	return -1;	
    }

    int GetFirstAncestorNr(int alist) {
    	if (alist < 0) alist=ActiveList;
		while (GetParent1Index(alist) >= 0)
		{
			int parent=GetParent1Index(alist);
			if (GetMarkerList(parent).Child1List == GetMarkerList(alist)) // I am child1
				GetMarkerList(parent).PreferredChild=1;  // make this the preferred child
			else
				GetMarkerList(parent).PreferredChild=2;  // make this the preferred child
			alist=parent;
		}
		return alist;
    }

    void MarkerListDialog(int MarkerListNr) {  // allows the user to define the units and scales
    	if (MarkerListNr < 0)
    		MarkerListNr = ActiveList;
    
    	MarkerList mlist = GetMarkerList(MarkerListNr);
    	
    	int mycolor = mlist.GetColor();
    	int Red=((mycolor/256/256) & 0xff);
    	int Green=((mycolor/256) & 0xff);
    	int Blue=(mycolor & 0xff);
    	
    	
        GenericDialog md= new GenericDialog("Marker List " + MarkerListNr);
        md.addStringField("MarkerListName: ",mlist.MyName);    // Only here the element is important, since value is element specific
        md.addNumericField("Parent1: ",GetParent1Index(MarkerListNr),5);
        md.addNumericField("Parent2: ",GetParent2Index(MarkerListNr),5);
        md.addNumericField("Child1: ",GetChild1Index(MarkerListNr),5);
        md.addNumericField("Child2: ",GetChild2Index(MarkerListNr),5);
        md.addNumericField("Red: ",Red,5);
        md.addNumericField("Green: ",Green,5);
        md.addNumericField("Blue: ",Blue,5);
        md.showDialog();
        if (! md.wasCanceled())
        {
            int Parent1,Parent2,Ch1,Ch2;
            mlist.MyName=md.getNextString();
            Parent1=(int) md.getNextNumber();
            Parent2=(int) md.getNextNumber();
            
            if (Parent1 != GetParent1Index(MarkerListNr))  // was changed
            {
                if (Parent1 < 0) // uncouple list
                {
            	if (mlist.Parent1List != null && mlist.Parent1List.Child1List == mlist)
            		mlist.Parent1List.Child1List =null;
            	if (mlist.Parent1List != null && mlist.Parent1List.Child2List == mlist)
            		mlist.Parent1List.Child2List =null;
                	mlist.Parent1List = null;  // deletes the parents
                }
                else   // the new parent
                {
                	MarkerList nparent=GetMarkerList(Parent1);
            		if (nparent.Child1List == null)
            		{
            			nparent.Child1List = mlist;
            			mlist.Parent1List = nparent; 
            		}
            		else if (nparent.Child2List == null)
            		{
            			nparent.Child2List = mlist;
            			mlist.Parent1List = nparent; 
            		}
            		else
            			IJ.showMessage("Warning: Cannot reconnect parent 1. New Parent 1 has no free children.");
                }
            }
            if (Parent2 != GetParent2Index(MarkerListNr))  // was changed
            {
                if (Parent2 < 0) // uncouple list
                {
            	if (mlist.Parent2List != null && mlist.Parent2List.Child1List == mlist)
            		mlist.Parent2List.Child1List =null;
            	if (mlist.Parent2List != null && mlist.Parent2List.Child2List == mlist)
            		mlist.Parent2List.Child2List =null;
                	mlist.Parent2List = null;  // deletes the parents
                }
                else   // the new parent
                {
                	MarkerList nparent=GetMarkerList(Parent2);
            		if (nparent.Child1List == null)
            		{
            			nparent.Child1List = mlist;
            			mlist.Parent2List = nparent; 
            		}
            		else if (nparent.Child2List == null)
            		{
            			nparent.Child2List = mlist;
            			mlist.Parent2List = nparent; 
            		}
            		else
            			IJ.showMessage("Warning: Cannot reconnect parent 2. New Parent 2 has no free children.");
                }
            }

            Ch1=(int) md.getNextNumber();
            if (Ch1 != GetChild1Index(MarkerListNr))
            {
            	if (Ch1 < 0) // uncoupple list
            	{
            	if (mlist.Child1List != null && mlist.Child1List.Parent1List == mlist)  // was previously connected via Child 1 - parent 1
            		{
            		mlist.Child1List.Parent1List =null;
            		mlist.Child1List=null;
            		}
            	else if (mlist.Child1List != null && mlist.Child1List.Parent2List == mlist)  // was previously connected via Child 1 - parent 2
            		{
            		mlist.Child1List.Parent2List =null;
            		mlist.Child1List=null;
            		}
            	}
            	else  // the new child 1 without connection
                {
                	MarkerList nch =GetMarkerList(Ch1);
                	if (nch != null)
                	{
            		if (nch.Parent1List != null)  // child has another parent 1
            			if (nch.Parent2List != null)  // also the other parent 2 is not free
            			{
            			if (nch.Parent2List.Child1List == nch)  // disconnect parent 2
            				nch.Parent2List.Child1List = null;
            			if (nch.Parent2List.Child2List == nch)
            				nch.Parent2List.Child2List = null;
            			nch.Parent2List = mlist;
            			}
            			else
            				nch.Parent2List = mlist;  // as parent 2 is free 
            		else
        				nch.Parent1List = mlist;  // as parent 1 is free
        			mlist.Child1List = nch;
                	}
                }
            }
            
            Ch2=(int) md.getNextNumber();
            if (Ch2 != GetChild2Index(MarkerListNr))
            {
            	if (Ch2 < 0)
            	{
            	if (mlist.Child2List != null && mlist.Child2List.Parent1List == mlist)  // was previously connected via Child 1 - parent 1
            		{
            		mlist.Child2List.Parent1List =null;
            		mlist.Child2List=null;
            		}
            	else if (mlist.Child2List != null && mlist.Child2List.Parent2List == mlist)  // was previously connected via Child 1 - parent 2
            		{
            		mlist.Child2List.Parent2List =null;
            		mlist.Child2List=null;
            		}
            	}
            	else  // the new child 1 without connection
                {
                	MarkerList nch =GetMarkerList(Ch2);
                	if (nch != null)
                	{
            		if (nch.Parent1List != null)  // child has another parent 1
            			if (nch.Parent2List != null)  // also the other parent 2 is not free
            			{
            			if (nch.Parent2List.Child1List == nch)  // disconnect parent 2
            				nch.Parent2List.Child1List = null;
            			if (nch.Parent2List.Child2List == nch)
            				nch.Parent2List.Child2List = null;
            			nch.Parent2List = mlist;
            			}
            			else
            				nch.Parent2List = mlist;  // as parent 2 is free 
            		else
        				nch.Parent1List = mlist;  // as parent 1 is free
        			mlist.Child2List = nch;
                	}
                }
            }
            
            Red=(int) md.getNextNumber();
            Green=(int) md.getNextNumber();
            Blue=(int) md.getNextNumber();
            mlist.SetColor(Blue+Green*256+Red*256*256);

        }
    }

    int FindMarkerListByName(String MarkerName)
    {	int n;
        for (n=0;n<NumLists;n++) { // Find the corresponding list by using the marker names
        	if (GetMarkerList(n).MyName.equals(MarkerName))
        		return n;  // found a suitable list that should be replaced
        }
        return NumLists;  // for a new list
    }
    
    void DeleteDublicateMarkerLists() {
    for (int n=ListOffset;n<NumLists;n++) { // Find the corresponding list by using the marker names
       for (int m=0;m<ListOffset;m++) { // Find the corresponding list by using the marker names
    	   MarkerList nlist=GetMarkerList(n);
    	   if (nlist.MyName != null && !nlist.MyName.equals("") && nlist.MyName.equals(GetMarkerList(m).MyName)) { // previous list will be deleted
    		   int mynum;
    			try {
    				mynum=Integer.parseInt(nlist.MyName);
    	    		if (mynum >= CurrentNameNr)
    	    			CurrentNameNr=mynum+1;
    			}
    			catch(NumberFormatException e) { }  // just ignore this as no new CurrentNameNr needs to be generated
     		   SetActiveList(m);
    		   RemoveList();
    		   ListOffset = ListOffset-1;
    		   m=m-1;
    		   n=n-1;
    	    }
    	   }
       }
	ListOffset=0;  // Reset the ListOffset
	SetActiveList(NumLists-1);
	MyActiveList.ActivePoint=0; 
    }
    
    boolean InsertInformationVector(float [] info, String MarkerName)    // inserts one line of information into the viewer, this can create a new list if necessary
    {
    	int listnr = (int) info[0] + ListOffset; // ListOffset is used to first insert totally new markers and then delete dublicated list names
    	int markernr = (int) info[1];
    	
    	double posx = info[2];
    	double posy = info[3];
    	double posz = info[4];
    	double pose = info[5];
    	double post = info[6];
    	double integral = info[7];
    	double max = info[8];
        // 9 .. 9+7=16  is the same in real world coordinates
    	int tagged = (int) info[9+7];      // ignore all the real world information
    	int Parent1Nr = (int) info[10+7];
    	int Parent2Nr = (int) info[11+7];
    	int Child1Nr = (int) info[12+7];
    	int Child2Nr = (int) info[13+7];
        int MyColor =(int) info[14+7];
                
        if (Parent1Nr >= 0) Parent1Nr += ListOffset;
        if (Parent2Nr >= 0) Parent2Nr += ListOffset;
        if (Child1Nr >= 0) Child1Nr += ListOffset;
        if (Child2Nr >= 0) Child2Nr += ListOffset;

        if (listnr>NumLists)  // This new list is more than one bigger than existing lists
    		listnr = NumLists;
    		// return false;

    	if (listnr>=NumLists)  // This new list is one bigger than existing lists -> make new one
    		for (int ll=NumLists;ll <= listnr; ll++)
    			AppendList();
    	
    	if (Parent1Nr >=0)
    		if (Parent1Nr >= NumLists)
    		for (int ll=NumLists;ll <= Parent1Nr; ll++)
    			AppendList();

    	if (Parent2Nr >=0)
    		if (Parent2Nr >= NumLists)
    		for (int ll=NumLists;ll <= Parent2Nr; ll++)
    			AppendList();

    	if (Child1Nr >=0)
    	if (Child1Nr >= NumLists)
    		for (int ll=NumLists;ll <= Child1Nr; ll++)
    			AppendList();
    	if (Child2Nr >=0)
    	if (Child2Nr >= NumLists)
    		for (int ll=NumLists;ll <= Child2Nr; ll++)
    			AppendList();
    	
    	SetActiveList(listnr);
    	//System.out.println("NumLists "+NumLists+", " + ParentNr + ", "+ Child1Nr + ", "+ Child2Nr + "\n");
    	
    	if (Parent1Nr >=0)
    		MyActiveList.Parent1List = GetMarkerList(Parent1Nr);  		
    
    	if (Parent2Nr >=0)
    		MyActiveList.Parent2List = GetMarkerList(Parent2Nr);  		
    
    	if (Child1Nr >=0)
    		MyActiveList.Child1List = GetMarkerList(Child1Nr);
    	if (Child2Nr >=0)
    		MyActiveList.Child2List = GetMarkerList(Child2Nr);

    	// Vector MyPoints = MyActiveList.MyPoints;
        int NumPoints = NumMarkers(ActiveList);
        APoint pt=new APoint(posx,posy,posz,pose,post);
        if (NumPoints > 0)
            pt.mycolor = GetPoint(0,-1).mycolor;  // all have the same color as the first one
        if (NumPoints == 1)  // probably just the first point was added
        	MyActiveList.SetColor(pt.mycolor);  // ensure the rest of the connected regions are updated as well

        pt.tagged=(tagged!=0);
        pt.integral=integral;
        pt.max=max;
        pt.mycolor=MyColor;
		MyActiveList.MyName = MarkerName; // use the marker name as a list name for now
		if (MarkerName == null || MarkerName.equals(""))
		{
    		if (Parent1Nr >= 0 && Parent2Nr >= 0)  // automatic name generation: Two parent == join their names
    			MyActiveList.MyName = "(" + MyActiveList.Parent1List.MyName + "_" + MyActiveList.Parent2List.MyName +")";
    		else if (Parent1Nr >= 0)            // automatic name generation
    		{
    			if (MyActiveList.Parent1List.Child1List == MyActiveList)
    				MyActiveList.MyName = MyActiveList.Parent1List.MyName + "a";
    			if (MyActiveList.Parent1List.Child2List == MyActiveList)
    				MyActiveList.MyName = MyActiveList.Parent1List.MyName + "b";
    			if (MyActiveList.Child1List != null)
    				if (MyActiveList.Child1List.Parent1List == MyActiveList)
    					MyActiveList.Child1List.MyName = MyActiveList.MyName + "a";
    			if (MyActiveList.Child2List != null)
        			if (MyActiveList.Child2List.Parent1List == MyActiveList)
        				MyActiveList.Child2List.MyName = MyActiveList.MyName + "b";
    		}
    		else if (Parent2Nr >= 0)            // automatic name generation
    		{
    			if (MyActiveList.Parent2List.Child1List == MyActiveList)
    				MyActiveList.MyName = MyActiveList.Parent2List.MyName + "a";
    			if (MyActiveList.Parent2List.Child2List == MyActiveList)
    				MyActiveList.MyName = MyActiveList.Parent2List.MyName + "b";
    			if (MyActiveList.Child1List != null)
    				if (MyActiveList.Child1List.Parent2List == MyActiveList)
    					MyActiveList.Child1List.MyName = MyActiveList.MyName + "a";
    			if (MyActiveList.Child2List != null)
        			if (MyActiveList.Child2List.Parent2List == MyActiveList)
        				MyActiveList.Child2List.MyName = MyActiveList.MyName + "b";
    		}
    		else
        		{
				if (MyActiveList == null || MyActiveList.MyName == null || ! MyActiveList.MyName.equals("" + CurrentNameNr))
					CurrentNameNr=listnr+1;
    			MyActiveList.MyName = "" + CurrentNameNr;
    			// System.out.println("New Name "+MyActiveList.MyName+"\n");
        		}
		}
		// else System.out.println("Assinged existing name "+MyActiveList.MyName+"\n");
    				
		if (markernr>NumPoints)  // This new list is more than one bigger than existing lists
    		return false;
    	
    	if (markernr==NumPoints)  // This new list is one bigger than existing lists -> make new one
    		MyActiveList.AddPoint(pt);
    	else // markernr < NumPoints
    	   {
    		pt=GetPoint(markernr,listnr);
    		pt.coord[0]=posx;              // update the values
    		pt.coord[1]=posy;
    		pt.coord[2]=posz;
    		pt.coord[3]=pose;
    		pt.coord[4]=post;
    	   }
    		pt.integral=integral;
    		pt.max=max;
    		pt.tagged=(tagged!=0);
    		
            //System.out.println("Added Marker "+markernr+" in List "+listnr);
            //data3d.ConstrainPoint(pt);
            //data3d.ClippedCOI(pt,data3d.COMX,data3d.COMY,data3d.COMZ,false);  // use the current settings and recompute center of intensity, but do not change positions
    return true;
    }

   public void DeleteAllMarkerLists() // deletes all existing marker lists
   {
	while (NumMarkerLists() > 1) { RemoveList(); }
	RemoveList();
   }

    public void ImportMarkerLists(float [][] lists)    // inserts all lists
    {
    	float [] alist;
    	ListOffset=NumLists;
    	for (int l=0; l<lists.length; l++)
    	{
    		alist = lists[l];
    		InsertInformationVector(alist,"");
    	}
    	DeleteDublicateMarkerLists();
    }

    
    boolean readline(StreamTokenizer is, My3DData data3d) throws java.io.IOException {  // reads a line from the stream and inserts/updates the marker

    float [] myinfo = new float[MarkerList.Attributes+2];
    is.eolIsSignificant(true); // treats EOL as token
	for (int pos=0;pos<MarkerList.Attributes+2;)
	{
		if (is.ttype == StreamTokenizer.TT_EOF)  return false;
		is.nextToken();
		if (is.ttype == StreamTokenizer.TT_EOL)  
			{
			if (pos > 7)
				return InsertInformationVector(myinfo,"");;
			}
		if (is.ttype == StreamTokenizer.TT_NUMBER) // else ignore the token
			{
			myinfo[pos] = (float) is.nval;
			pos++;
			}		
		 //else System.out.println("StringToken "+is.sval+" at " + pos + " ... ignored\n");
		 //if (pos > 0) System.out.println("pos " + (pos-1) + ", value "+myinfo[pos-1]+"\n");
	}
	is.nextToken();  // this should be the name of the marker
	String myVal="";
	while (is.ttype != StreamTokenizer.TT_EOL && is.ttype != StreamTokenizer.TT_EOF)
	{
	if (is.ttype == StreamTokenizer.TT_NUMBER) // else ignore the token
	{
		if ((double) (int) is.nval == is.nval)
			myVal += (int) is.nval;
		else myVal += is.nval;
		if (is.nval > CurrentNameNr)
			CurrentNameNr = (int) is.nval+1; // future actions by the user should choose bigger names
	}
	if (is.ttype == StreamTokenizer.TT_WORD)
		myVal += is.sval;
		is.nextToken();  // this should be the name of the marker
	}
	//System.out.println("StringToken1 "+myVal+" at " + (MarkerList.Attributes+2) + " ... Marker List Name\n");
	//System.out.println("Line Finished\n");

	return InsertInformationVector(myinfo,myVal);
   }

    int GetParent1Index(int listnr) {
        if (NumLists <= 0) return -1;
        return ListOfLists.indexOf(GetMarkerList(listnr).Parent1List);
    }
    int GetParent2Index(int listnr) {
        if (NumLists <= 0) return -1;
        return ListOfLists.indexOf(GetMarkerList(listnr).Parent2List);
    }
    int GetChild1Index(int listnr) {
        if (NumLists <= 0) return -1;
        return ListOfLists.indexOf(GetMarkerList(listnr).Child1List);
    }
    int GetChild2Index(int listnr) {
        if (NumLists <= 0) return -1;
        return ListOfLists.indexOf(GetMarkerList(listnr).Child2List);
    }
    boolean HasParent(int list) {
    	return (GetParent1Index(list) >=0 || GetParent2Index(list) >=0);
    }
    boolean HasParent1(int list) {
    	return (GetParent1Index(list) >=0 );
    }
    boolean HasParent2(int list) {
    	return (GetParent2Index(list) >=0);
    }

    APoint GetParent1EndOfTrack(int list,int dir) {
    	if (! HasParent(list)) return null;
    	//MarkerList mylist=GetMarkerList(list);
    	MarkerList myparent=GetMarkerList(GetParent1Index(list));
    	return myparent.GetPoint(myparent.NumPoints-1);
    	// return myparent.GetPoint(myparent.ClosestTrackMarker(mylist.GetPoint(0), dir, -1));  // return the closest marker when stepping backwards
    	}
    APoint GetParent2EndOfTrack(int list,int dir) {
    	if (! HasParent(list)) return null;
    	//MarkerList mylist=GetMarkerList(list);
    	MarkerList myparent=GetMarkerList(GetParent2Index(list));
    	return myparent.GetPoint(myparent.NumPoints-1);
    	// return myparent.GetPoint(myparent.ClosestTrackMarker(mylist.GetPoint(0), dir, -1));  // return the closest marker when stepping backwards
    	}

    public double [][] ExportMarkers(int list,My3DData data3d)
    {
        if (list >= NumLists)
            return new double[0][0];
        double [][] markers = GetMarkerList(list).ExportMarkers();   // leaves ParentList an empty field

        for (int i=0;i<markers.length;i++)
        {
        	int elem=(int) (markers[i][3]+0.5);
        	markers[i][7] *= data3d.GetScale(elem,0);
        	markers[i][7] += data3d.GetOffset(elem,0);
        	markers[i][8] *= data3d.GetScale(elem,1);
        	markers[i][8] += data3d.GetOffset(elem,1);
        	markers[i][9] *= data3d.GetScale(elem,2);
        	markers[i][9] += data3d.GetOffset(elem,2);
        	markers[i][10] *= data3d.GetScale(elem,3);
        	markers[i][10] += data3d.GetOffset(elem,3);
        	markers[i][11] *= data3d.GetScale(elem,4);
        	markers[i][11] += data3d.GetOffset(elem,4);

        	markers[i][12] *= data3d.GetValueScale(elem);
        	markers[i][12] += data3d.GetValueScale(elem);
        	markers[i][13] *= data3d.GetValueScale(elem);
        	markers[i][13] += data3d.GetValueScale(elem);

        	// markers[i][14]   is tagged, untouched
        	markers[i][15] = GetParent1Index(list);
        	markers[i][16] = GetParent2Index(list);
        	markers[i][17] = GetChild1Index(list);
        	markers[i][18] = GetChild2Index(list);
        }
        return markers;
    }

    public double [][] ExportMarkerLists(My3DData data3d)
    {
    	int totallength=0;
        for (int list=0;list<NumLists;list++)  // Test for all lists
        	totallength += GetMarkerList(list).NumPoints;
        
    	double [][] alllists = new double[totallength][];
    	double [][] alist;
    	int pos=0;
        for (int list=0;list<NumLists;list++)  // Test for all lists
        {
        	alist = ExportMarkers(list,data3d);
        	for (int m=0;m<alist.length;m++)
        	{
        		alllists[pos] = new double[MarkerList.Attributes+2];
        		alllists[pos][0] = list; 
        		alllists[pos][1] = m; 
            	for (int n=0;n<alist[m].length;n++)
            	{
            	   //	System.out.println("pos " + pos + ", n "+n+", m "+m+" totallength "+totallength+"\n");
            		alllists[pos][n+2] = alist[m][n]; 
            	}
        		pos ++;
        	}
        }
        return alllists;
    }

    double [] MSD(int list, My3DData data3d)  // returns a list of Mean Square Deviations
    {
        int asize=data3d.sizes[data3d.TrackDirection];
        double msds[]= new double[asize];
        double events[]= new double[asize];
        MSDSum(list,data3d,msds,events);
        MSDFinal(msds,events,asize);
        return msds;
    }

    double [] AllMSD(My3DData data3d)
    {
        int asize=data3d.sizes[data3d.TrackDirection];
        double msds[]= new double[asize];
        double events[]= new double[asize];
        for (int list=0;list<NumLists;list++)  // Test for all lists
            MSDSum(list,data3d,msds,events);
        MSDFinal(msds,events,asize);
        return msds;
    }

    final static double ClipAt(double data,double low, double high)
    {
        return (data > high) ? high : (data < low) ? low : data;
    }
    
    boolean MSDSum(int list, My3DData data3d, double msds[], double events[])  // returns a list of Mean Square Deviations in msds[]
    {

        int asize=data3d.sizes[data3d.TrackDirection];
        int listlength=NumMarkers(list);
        APoint pointT,pointTN;
        for (int n=0;n<listlength;n++)   // iterate of delta times : n* dt
          for (int t=0;t<listlength-n;t++)   // iterate over all possible times at this delta-time
          {
             pointT=GetPoint(t,list);
             pointTN=GetPoint(t+n,list);
             int dT = (int) (pointTN.coord[data3d.TrackDirection]-pointT.coord[data3d.TrackDirection]);
             dT=(int) ClipAt(dT,0,asize-1);
             if (data3d.TrackDirection == 2)
                msds[dT] += pointT.SqrXYDistTo(pointTN,1,1);
             else
                msds[dT] += pointT.SqrDistTo(pointTN,1,1,1);
             events[dT] += 1;
          }
        return true;
    }

    boolean MSDFinal(double msds[],double events[],int listlength)
    {
        for (int n=0;n<listlength;n++)
            msds[n] /= events[n];
        return true;
    }
    
        
    void Penalize(APoint aPt,double fwhm, int direction) // computes the repulsion penalty for a specified pixel an penalizes the pixel
    {
        double sigma2=(fwhm/2.0)*(fwhm/2.0)/java.lang.Math.log(2.0);
        for (int list=0;list<NumLists;list++)  // Test for all lists
        if (list != ActiveList)  // this the current marker is from this list
        {
        int listlength=NumMarkers(list);
        APoint point;
        for (int i=0;i<listlength;i++) // Test for all points in the list being at equal hyperplane
            {
                point=GetPoint(i,list);
                if (aPt != point && point.coord[direction] == aPt.coord[direction])  // Points are in the same hyperplane
                {
                    double ssqr=0.0,weight;
                    for (int d=0;d<5;d++)  // compute penalty distance
                        if (direction != d)
                        {
                            ssqr+=(point.coord[d]-aPt.coord[d])*(point.coord[d]-aPt.coord[d]);
                        }
                    System.out.println("penalizing point "+aPt.toString()+" by " + ssqr);
                    if (ssqr <= 0.0) return;  // no chance to penalize, direction is unknown
                    weight = fwhm*Math.exp(-ssqr/sigma2);  // Penality weight
                    System.out.println(" weight"+weight);
                    for (int d=0;d<5;d++)   // apply penalty
                        if (direction != d)
                            aPt.coord[d] -= (point.coord[d] -aPt.coord[d]) / Math.sqrt(ssqr) * weight;
                    System.out.println("penalized point"+aPt.toString()+" by " + ssqr);
                }
            }
        }
    }

    double Penalty(APoint aPt,double fwhm, int direction, double IntMaxScale) // computes the repulsion penalty for a specified pixel an penalizes the pixel
    {
        double penalty=0.0;
        double sigma2=(fwhm/2.0)*(fwhm/2.0)/java.lang.Math.log(2.0);
        for (int list=0;list<NumLists;list++)  // Test for all lists
        if (list != ActiveList)  // this the current marker is from this list
        {
        int listlength=NumMarkers(list);
        APoint point;
        for (int i=0;i<listlength;i++) // Test for all points in the list being at equal hyperplane
            {
                double ssqr=0.0;
                point=GetPoint(i,list);
                if (aPt != point && point.coord[direction] == aPt.coord[direction])  // Points are in the same hyperplane
                    for (int d=0;d<5;d++)  // compute penalty distance
                        if (direction != d)
                            ssqr+=(point.coord[d]-aPt.coord[d])*(point.coord[d]-aPt.coord[d]);
                penalty += IntMaxScale*point.integralAboveMin*Math.exp(-ssqr/sigma2);  // Penality weight
            }
        }
        return penalty;
    }

    public String PrintList(My3DData data3d) {
        String newtext="#List Nr.,\tMarker Nr,\tPosX [pixels],\tY [pixels],\tZ [pixels],\tElements [element],\tTime [time],\tIntegral (no BG sub) [Units],\tMax (no BG sub) [Units],"+
        "\t"+data3d.GetAxisNames()[0]+" ["+data3d.GetAxisUnits()[0]+"],\t"+data3d.GetAxisNames()[1]+" ["+data3d.GetAxisUnits()[1]+"],\t"+data3d.GetAxisNames()[2]+" ["+data3d.GetAxisUnits()[2]+"],\t"+data3d.GetAxisNames()[3]+" ["+data3d.GetAxisUnits()[3]+"],\t"+data3d.GetAxisNames()[4]+" ["+data3d.GetAxisUnits()[4]+"],\tIntegral "+data3d.GetValueName(data3d.ActiveElement)+" (no BG sub)["+data3d.GetValueUnit(data3d.ActiveElement)+"],\tMax "+data3d.GetValueName(data3d.ActiveElement)+" (no BG sub)["+data3d.GetValueUnit(data3d.ActiveElement)+"]\tTagText \tTagInteger \tParent1 \tParent2 \tChild1 \t Child2 \tListColor \tListName\n";
        APoint point;
        NumberFormat nf = java.text.NumberFormat.getNumberInstance(Locale.US);
        nf.setMaximumFractionDigits(2);
        // nf.setMinimumIntegerDigits(7);
        nf.setGroupingUsed(false);
        for (int l=0;l<NumLists;l++)
        {
          newtext+= "\n";
          for (int i=0;i<NumMarkers(l);i++)
          {
            point=GetPoint(i,l);
            int elem = (int) (point.coord[3] + 0.5);
            newtext+= l + "\t"+i + "\t" + nf.format(point.coord[0]) + "\t" + nf.format(point.coord[1]) + "\t"+nf.format(point.coord[2])+"\t"+nf.format(point.coord[3])+"\t"+nf.format(point.coord[4])+"\t"+
                  nf.format(point.integral)+"\t"+nf.format(point.max)+"\t"+
                  nf.format(point.coord[0]*data3d.GetScale(elem,0)+data3d.GetOffset(elem,0))+"\t"+
                  nf.format(point.coord[1]*data3d.GetScale(elem,1)+data3d.GetOffset(elem,1))+"\t"+
                  nf.format(point.coord[2]*data3d.GetScale(elem,2)+data3d.GetOffset(elem,2))+"\t"+
                  nf.format(point.coord[3]*data3d.GetScale(elem,3)+data3d.GetOffset(elem,3))+"\t"+
                  nf.format(point.coord[4]*data3d.GetScale(elem,4)+data3d.GetOffset(elem,4))+"\t"+
                  nf.format(point.integral*data3d.GetValueScale(elem)+data3d.GetValueOffset(elem))+"\t"+
                  nf.format(point.max*data3d.GetValueScale(elem)+data3d.GetValueOffset(elem))+'\t'+point.tagged+"\t";
            if (point.tagged)
            	newtext = newtext+"1\t";
            else
            	newtext = newtext+"0\t";
            newtext = newtext + GetParent1Index(l) + "\t";
            newtext = newtext + GetParent2Index(l) + "\t";
            newtext = newtext + GetChild1Index(l) + "\t";
            newtext = newtext + GetChild2Index(l) + "\t";
            newtext = newtext + GetMarkerList(l).GetColor() + "\t";
            newtext = newtext + GetMarkerList(l).GetMarkerListName() + "\n";
          }
        }
        return newtext;
    }

 double[] MSDFromList(My3DData data3d, int l, boolean do3D)
 {
  double MSDs[] =new double[(int) Math.sqrt(NumMarkers(l)+1)];
  double MSDNums[] =new double[(int) Math.sqrt(NumMarkers(l)+1)];
  APoint point,point2;
  double SumDist2;
  int adT;   // deltaTime but in unit steps
  for (int i=0;i<NumMarkers(l);i++) {
  for (int j=i+1;j<NumMarkers(l);j++)  	{
      point=GetPoint(i,l);
      point2=GetPoint(j,l);
      int elem = (int) point.coord[3];
      adT = (int) (point2.coord[data3d.TrackDirection]-point.coord[data3d.TrackDirection]);
      if (do3D)
          SumDist2 = point.SqrDistTo(point2,data3d.GetScale(elem,0),data3d.GetScale(elem,1),data3d.GetScale(elem,2));
      else
          SumDist2 = point.SqrXYDistTo(point2,data3d.GetScale(elem,0),data3d.GetScale(elem,1));
      if (adT < 0) adT = -adT;
      if (adT < MSDs.length+1) 
      {
    	  if (adT > 0) {
    	  MSDs[adT-1] += SumDist2;
    	  MSDNums[adT-1]++;
    	  }
      }
      else
    	  j=NumMarkers(l);  // break this for loop and go on
  	}
  }

  for (int i=0;i<MSDs.length;i++) {
	  if (MSDNums[i] > 0)
		  MSDs[i] /= MSDNums[i];
  	}
  return MSDs;
 }
    
 String PrintSummary(My3DData data3d) {
    String newtext = "# Statistics Summary\n#\t";
    APoint point,oldpoint;
    NumberFormat nf = java.text.NumberFormat.getNumberInstance(Locale.US);
    nf.setMaximumFractionDigits(3);
    // nf.setMinimumIntegerDigits(7);
    nf.setGroupingUsed(false);
    int doMSD=1;
    for (int l=0;l<NumLists;l++)
        {
          double adT,XYDist,XYZDist, SumXYSpeed = 0.0, SumXYZSpeed = 0.0, N = 0.0;
          oldpoint=GetPoint(0,l);
          for (int i=1;i<NumMarkers(l);i++)
            {
            point=GetPoint(i,l);
            int elem = (int) point.coord[3];
            adT = (point.coord[data3d.TrackDirection]-oldpoint.coord[data3d.TrackDirection])*data3d.GetScale(elem,data3d.TrackDirection);
            XYDist = Math.sqrt(point.SqrXYDistTo(oldpoint,data3d.GetScale(elem,0),data3d.GetScale(elem,1)));
            XYZDist = Math.sqrt(point.SqrDistTo(oldpoint,data3d.GetScale(elem,0),data3d.GetScale(elem,1),data3d.GetScale(elem,2)));
            SumXYSpeed += XYDist/adT;
            SumXYZSpeed += XYZDist/adT;
            oldpoint=point;
            N += 1.0;
            }

         
          if (NumMarkers(l) > 0)
          {
          newtext += "\n# Summary for List \t"+l+"\n";
          int elem = (int) GetPoint(0,l).coord[3];
          double totalXY = Math.sqrt(GetPoint(0,l).SqrXYDistTo(GetPoint(NumMarkers(l)-1,l),data3d.GetScale(elem,0),data3d.GetScale(elem,1)));
          double totalXYZ = Math.sqrt(GetPoint(0,l).SqrDistTo(GetPoint(NumMarkers(l)-1,l),data3d.GetScale(elem,0),data3d.GetScale(elem,1),data3d.GetScale(elem,2)));
          double dT = (GetPoint(NumMarkers(l)-1,l).coord[data3d.TrackDirection]-GetPoint(elem,l).coord[data3d.TrackDirection])*data3d.GetScale(elem,data3d.TrackDirection);
          String XY=data3d.GetAxisNames()[0]+data3d.GetAxisNames()[1];
          String XYZ=data3d.GetAxisNames()[0]+data3d.GetAxisNames()[1]+data3d.GetAxisNames()[2];
          
          if (data3d.TrackDirection == 2)
          {
            newtext += "\n# Total "+XY+" Distance traveled: \t"+nf.format(totalXY)+" ["+data3d.GetAxisUnits()[0]+"]\n";
            newtext += "# Length of Trace: \t"+nf.format(dT)+" ["+data3d.GetAxisUnits()[data3d.TrackDirection]+"]\n";
            newtext += "# Segments in Trace: \t"+nf.format(N)+"\n";
            newtext += "# Total "+XY+" speed: \t"+nf.format(totalXY/dT)+" ["+data3d.GetAxisUnits()[0]+"/"+data3d.GetAxisUnits()[data3d.TrackDirection]+"]\n";
            newtext += "# Average "+XY+" speed: \t"+nf.format(SumXYSpeed/N)+" ["+data3d.GetAxisUnits()[0]+"/"+data3d.GetAxisUnits()[data3d.TrackDirection]+"]\n";
            newtext += "# Directionality Index: \t"+nf.format((totalXY/dT)/(SumXYSpeed/N))+"\n";
          }
          else if (data3d.TrackDirection == 4) // time
          {
            newtext += "\n# Total XYZ Distance traveled: \t"+nf.format(totalXYZ)+" ["+data3d.GetAxisUnits()[0]+"]\n";
            newtext += "# Length of Trace: \t"+nf.format(dT)+" ["+data3d.GetAxisUnits()[data3d.TrackDirection]+"]\n";
            newtext += "# Segments in Trace: \t"+nf.format(N)+"\n";
            newtext += "# Total "+XY+" speed: \t"+nf.format(totalXY/dT)+" ["+data3d.GetAxisUnits()[0]+"/"+data3d.GetAxisUnits()[data3d.TrackDirection]+"]\n";
            newtext += "# Total "+XYZ+" speed: \t"+nf.format(totalXYZ/dT)+" ["+data3d.GetAxisUnits()[0]+"/"+data3d.GetAxisUnits()[data3d.TrackDirection]+"]\n";
            newtext += "# Average "+XY+" speed: \t"+nf.format(SumXYSpeed/N)+" ["+data3d.GetAxisUnits()[0]+"/"+data3d.GetAxisUnits()[data3d.TrackDirection]+"]\n";
            newtext += "# Average "+XYZ+" speed: \t"+nf.format(SumXYZSpeed/N)+" ["+data3d.GetAxisUnits()[0]+"/"+data3d.GetAxisUnits()[data3d.TrackDirection]+"]\n";
            newtext += "# Directionality "+XY+" Index: \t"+nf.format((totalXY/dT)/(SumXYSpeed/N))+"\n";
            newtext += "# Directionality "+XYZ+" Index: \t"+nf.format((totalXYZ/dT)/(SumXYZSpeed/N))+"\n";
          }
          }
    // MSD Plots
    // compute MSD plots
    if (doMSD > 0 && NumMarkers(l) > 1)
    	{
        double MSDs[];
        if (data3d.TrackDirection == 2 || data3d.SizeZ == 1)
        {
            newtext += "\n# XY-MSD summary list : \t"+l+"]\n";
            MSDs=MSDFromList(data3d,l,false);
        }
        else
        {
            newtext += "\n# XYZ-MSD summary list : \t"+l+"]\n";
            MSDs=MSDFromList(data3d,l,true);  // in 3D
        }
        int elem = (int) GetPoint(0,l).coord[3];
  		for (int dd=0;dd<MSDs.length;dd++) {
            newtext+="#\t"+ l + "\t"+(dd+1)*data3d.GetScale(elem,data3d.TrackDirection) + "\t" + MSDs[dd] + "\n";  	      		  
  		  }        		
    	}
    }

    return newtext;
    }


 	void AppendList() {  // will not influence the active list
    	MyActiveList = new MarkerList();
        ListOfLists.addElement(MyActiveList); 	
        NumLists++;
 	}

 	void NewList() {
    	MyActiveList = new MarkerList();
        if (ActiveList < 0) 
        {
            ActiveList=0;
            ListOfLists.addElement(MyActiveList);
        }
        else
        {
            ActiveList++;
            ListOfLists.insertElementAt(MyActiveList,ActiveList);
        }
      NumLists++;
  	MyActiveList.MyName = "" + (CurrentNameNr++); // Just use the number as a name
    }

    void NewList(int linkTo, String NameExtension) {
    	NewList();
    	CurrentNameNr--;
    	MyActiveList.Link1To(GetMarkerList(linkTo));
    	MyActiveList.MyName = MyActiveList.Parent1List.MyName +  NameExtension;
    }

    void RemoveList() {
        if (NumLists < 1) // Also the last list can be removed
            return;
        if (MyActiveList == null) return;
        
        MyActiveList.MyPoints=null;
        if (MyActiveList.Parent1List != null)
          {
        	if (MyActiveList.Parent1List.Child1List == MyActiveList)
        		MyActiveList.Parent1List.Child1List = null;  // delete the child information
            if (MyActiveList.Parent1List.Child2List == MyActiveList)
            	MyActiveList.Parent1List.Child2List = null;  // delete the child information
          }
        if (MyActiveList.Parent2List != null)
        {
      	if (MyActiveList.Parent2List.Child1List == MyActiveList)
      		MyActiveList.Parent2List.Child1List = null;  // delete the child information
          if (MyActiveList.Parent2List.Child2List == MyActiveList)
          	MyActiveList.Parent2List.Child2List = null;  // delete the child information
        }
        if (MyActiveList.Child1List != null)
          {
        	if (MyActiveList.Child1List.Parent1List == MyActiveList)
        		MyActiveList.Child1List.Parent1List = null;  // delete the childs parent information
        	if (MyActiveList.Child1List.Parent2List == MyActiveList)
        		MyActiveList.Child1List.Parent2List = null;  // delete the childs parent information
          }
        if (MyActiveList.Child2List != null)
        {
      	if (MyActiveList.Child2List.Parent1List == MyActiveList)
      		MyActiveList.Child2List.Parent1List = null;  // delete the childs parent information
      	if (MyActiveList.Child2List.Parent2List == MyActiveList)
      		MyActiveList.Child2List.Parent2List = null;  // delete the childs parent information
        }
        
        ListOfLists.removeElementAt(ActiveList);
        NumLists --;
        if (NumLists <= 0)
            {ActiveList=-1;MyActiveList=null;NewList();NumLists=1;}  // Active List will then be 0
        if (ActiveList >= NumLists)
			{ ActiveList = NumLists-1; }
        MyActiveList  = GetMarkerList(ActiveList);   // just use the current position and advance to the next list
    }

    void AdvanceList(int howmany, int TrackDirection) {
        if (NumLists <= 1) return;
        int curpos= MyActiveList.ActiveMarkerPos();
        APoint CurPt=MyActiveList.GetPoint(curpos);
        if (NumMarkers(ActiveList)<=0 && NumLists > 1) 
        {
            RemoveList();howmany-=1;
        }
        ActiveList += howmany;
        if (ActiveList < 0) ActiveList += NumLists;
        if (ActiveList >= NumLists) ActiveList = ActiveList % NumLists;
        MyActiveList = GetMarkerList(ActiveList);
		MyActiveList.SetActiveMarkerClosest(CurPt, TrackDirection, 0);
	    // MyActiveList.SetActiveMarker(curpos); // At least roughly right
    }
    
APoint MarkerFromPosition(double px, double py, int dir, double DistX, double DistY, boolean alllists, boolean allslices, boolean alltrack, int trackdir, int trackpos)  // returns the first marker who is in the range
    {
        for (int list=0;list < NumLists;list++)
            if (alllists || list == ActiveList)
                for (int p=0;p<NumMarkers(list);p++)
                {
                    APoint Pt=GetPoint(p,list);
                    if (Pt.InRange(px,py,dir,DistX,DistY)) 
                        // if (allslices || Pt == GetPoint(-1,-1)) // alslices was activated or the point is the current point
                        if (alltrack || Pt == GetPoint(-1,-1)) 
                            return Pt;
                        else  // alltrack is not active but maybe the point corresponds to a current position
                        if (((int) Pt.coord[trackdir] +0.5) == trackpos)
                            return Pt;    	
                }
        return null;
    }


APoint MarkerFromPosition(double px, double py, int dir, double DistX, double DistY, My3DData my3ddata)  // returns the first marker who is in the range
{
	// int XDim=0,YDim=1;
	// switch (dir) {
	//case 0:
	//	XDim=1;YDim=2;  // YZ
	//	break;
	//case 1:
	//	XDim=0;YDim=2;  // XZ
	//	break;
	//case 2:
	//	XDim=0;YDim=1; // XY
	//	break;
	//}
	APoint Pt=null;
	for (int list=0;list < NumLists;list++)
	{
		Pt=GetMarkerList(list).MarkerFromPosition(px, py, dir, DistX, DistY);
		if (Pt != null)
			return Pt;
	}
    return null;
}
}
