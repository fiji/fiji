/****************************************************************************
 *   Copyright (C) 1996-2009 by Rainer Heintzmann                          *
 *   heintzmann@gmail.com                                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; Version 2 of the License.               *
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

import java.applet.Applet;
import java.awt.*;
import java.util.*;
// import view5d.*;

// insert : "public" before this to make it an applet
public class View5D extends Applet {
  static final long serialVersionUID = 1;  // Just to fulfil the requirement. Is not used. Look at View5D_.java
  public int SizeX=0,SizeY=0,SizeZ=0;
  int Elements=1,Times=1,defaultColor0=-1,
      redEl=-1,greenEl=-1,blueEl=-1;
  My3DData data3d;
  TextArea myLabel;
  ImgPanel mypan=null;
  Vector<ImgPanel> panels=new Vector<ImgPanel>();  // Keeps track of all the views. Sometimes this information is needed to send the updates.
  
  public String filename=null;
 
  public void UpdatePanels()  // update all panels
        {
            for (int i=0;i<panels.size();i++)
                ((ImgPanel) panels.elementAt(i)).c1.UpdateAllNoCoord();
        }
 
   /* The code below is necessary to include the software as a plugin into Matlab and DipImage (Univ. Delft) */
   public View5D AddElement(byte [] myarray, int SizeX, int SizeY, int SizeZ,int Elements,int Times) {
        int DataType=AnElement.ByteType, NumBytes=1, NumBits=8;
        int ne=data3d.GenerateNewElement(DataType,NumBytes,NumBits,data3d.GetScale(data3d.ActiveElement),
                            data3d.GetOffset(data3d.ActiveElement),1.0,0.0,data3d.GetAxisNames(),data3d.GetAxisUnits());
        for (int t=0;t<Times;t++) {
                System.arraycopy( myarray, t*SizeX*SizeY*SizeZ,
                                 ((ByteElement) data3d.ElementAt(ne,t)).myData, 0 , SizeX*SizeY*SizeZ);
		}
	data3d.GetBundleAt(ne).ToggleOverlayDispl(1);
        return this;
   }
   /* The code below is necessary to include the software as a plugin into Matlab and DipImage (Univ. Delft) */
   public View5D AddElement(short [] myarray, int SizeX, int SizeY, int SizeZ,int Elements,int Times) {
        int DataType=AnElement.ShortType, NumBytes=1, NumBits=8;
        int ne=data3d.GenerateNewElement(DataType,NumBytes,NumBits,data3d.GetScale(data3d.ActiveElement),
                            data3d.GetOffset(data3d.ActiveElement),1.0,0.0,data3d.GetAxisNames(),data3d.GetAxisUnits());
        for (int t=0;t<Times;t++) {
                System.arraycopy( myarray, t*SizeX*SizeY*SizeZ,
                                 ((ShortElement) data3d.ElementAt(ne,t)).myData, 0 , SizeX*SizeY*SizeZ);
		}
	data3d.GetBundleAt(ne).ToggleOverlayDispl(1);
        return this;
   }
   /* The code below is necessary to include the software as a plugin into Matlab and DipImage (Univ. Delft) */
   public View5D AddElement(float [] myarray, int SizeX, int SizeY, int SizeZ,int Elements,int Times) {
        int DataType=AnElement.FloatType, NumBytes=1, NumBits=8;
        int ne=data3d.GenerateNewElement(DataType,NumBytes,NumBits,data3d.GetScale(data3d.ActiveElement),
                            data3d.GetOffset(data3d.ActiveElement),1.0,0.0,data3d.GetAxisNames(),data3d.GetAxisUnits());
        for (int t=0;t<Times;t++) {
                System.arraycopy( myarray, t*SizeX*SizeY*SizeZ,
                                 ((FloatElement) data3d.ElementAt(ne,t)).myData, 0 , SizeX*SizeY*SizeZ);
		}
	data3d.GetBundleAt(ne).ToggleOverlayDispl(1);
        return this;
   }
   /* The code below is necessary to include the software as a plugin into Matlab and DipImage (Univ. Delft) */
   public View5D AddElement(double [] myarray, int SizeX, int SizeY, int SizeZ,int Elements,int Times) {
        int DataType=AnElement.DoubleType, NumBytes=1, NumBits=8;
        int ne=data3d.GenerateNewElement(DataType,NumBytes,NumBits,data3d.GetScale(data3d.ActiveElement),
                            data3d.GetOffset(data3d.ActiveElement),1.0,0.0,data3d.GetAxisNames(),data3d.GetAxisUnits());
        for (int t=0;t<Times;t++) {
                System.arraycopy( myarray, t*SizeX*SizeY*SizeZ,
                                 ((DoubleElement) data3d.ElementAt(ne,t)).myData, 0 , SizeX*SizeY*SizeZ);
		}
	data3d.GetBundleAt(ne).ToggleOverlayDispl(1);
        return this;
   } 

   /* The code below is necessary to include the software as a plugin into Matlab and DipImage (Univ. Delft) */
   public View5D AddElementC(float [] myarray, int SizeX, int SizeY, int SizeZ,int Elements,int Times) {
        int DataType=AnElement.ComplexType, NumBytes=8, NumBits=64;
        int ne=data3d.GenerateNewElement(DataType,NumBytes,NumBits,data3d.GetScale(data3d.ActiveElement),
                            data3d.GetOffset(data3d.ActiveElement),1.0,0.0,data3d.GetAxisNames(),data3d.GetAxisUnits());
        for (int t=0;t<Times;t++) {
                System.arraycopy( myarray, 2*t*SizeX*SizeY*SizeZ,
                                 ((ComplexElement) data3d.ElementAt(ne,t)).myData, 0 , 2*SizeX*SizeY*SizeZ);
		}
	data3d.GetBundleAt(ne).ToggleOverlayDispl(1);
        return this;
   } 

  /* The code below is necessary to include the software as a plugin into Matlab and DipImage (Univ. Delft) */
   public static View5D Start5DViewer(byte [] barray, int SizeX, int SizeY, int SizeZ,int Elements,int Times) {
        int DataType=AnElement.ByteType, NumBytes=1, NumBits=8;
        View5D anApplet=Prepare5DViewer(SizeX,SizeY,SizeZ,Elements,Times,DataType,NumBytes,NumBits);
        ((ByteElement) anApplet.data3d.ActElement()).myData= barray; // new byte[SizeX*SizeY*SizeZ];
	anApplet.data3d.ToggleOverlayDispl(1);
        AlternateViewer aviewer=new AlternateViewer(anApplet,600,500);
        for (int t=0;t<Times;t++)
            for (int e=0;e<Elements;e++)
            {
                if (e==0 && t ==0)
                    aviewer.Assign3DData(anApplet,anApplet.mypan,anApplet.data3d);
                else
                    System.arraycopy( barray, (e+Elements*t)*SizeX*SizeY*SizeZ,
                                 ((ByteElement) anApplet.data3d.ElementAt(e,t)).myData, 0 , SizeX*SizeY*SizeZ);
            }
        anApplet.start();
        return anApplet;
   }

   /* The code below is necessary to include the software as a plugin into Matlab and DipImage (Univ. Delft) */
   public static View5D Start5DViewer(short [] sarray, int SizeX, int SizeY, int SizeZ,int Elements,int Times) {
        int DataType=AnElement.ShortType, NumBytes=2, NumBits=8;
        System.out.println("viewer invoked (short datatype)");
        View5D anApplet=Prepare5DViewer(SizeX,SizeY,SizeZ,Elements,Times,DataType,NumBytes,NumBits);
        ((ShortElement) anApplet.data3d.ActElement()).myData= sarray; // new byte[SizeX*SizeY*SizeZ];
	anApplet.data3d.ToggleOverlayDispl(1);
        AlternateViewer aviewer=new AlternateViewer(anApplet,600,500);
        for (int t=0;t<Times;t++)
            for (int e=0;e<Elements;e++)
            {
                if (e==0 && t ==0)
                    aviewer.Assign3DData(anApplet,anApplet.mypan,anApplet.data3d);
                else
                    System.arraycopy(sarray, (e+Elements*t)*SizeX*SizeY*SizeZ,
                                 ((ShortElement) anApplet.data3d.ElementAt(e,t)).myData, 0 , SizeX*SizeY*SizeZ);
            }
        anApplet.start();
        return anApplet;
   }

   /* The code below is necessary to include the software as a plugin into Mathlab and DipImage (Univ. Delft) */
   public static View5D Start5DViewer(float [] farray, int SizeX, int SizeY, int SizeZ,int Elements,int Times) {
        int DataType=AnElement.FloatType, NumBytes=4, NumBits=32;
        View5D anApplet=Prepare5DViewer(SizeX,SizeY,SizeZ,Elements,Times,DataType,NumBytes,NumBits);
        ((FloatElement) anApplet.data3d.ActElement()).myData= farray; // new byte[SizeX*SizeY*SizeZ];
	anApplet.data3d.ToggleOverlayDispl(1);
        AlternateViewer aviewer=new AlternateViewer(anApplet,600,500);
        for (int t=0;t<Times;t++)
            for (int e=0;e<Elements;e++)
                if (e==0 && t ==0)
                    aviewer.Assign3DData(anApplet,anApplet.mypan,anApplet.data3d);
                else     
                    System.arraycopy( farray, (e+Elements*t)*SizeX*SizeY*SizeZ,
                                 ((FloatElement) anApplet.data3d.ElementAt(e,t)).myData, 0 , SizeX*SizeY*SizeZ);
        anApplet.start();
        return anApplet;
   }
   /* Complex is not a known class in java, therefore a separate */
   public static View5D Start5DViewerC(float [] carray, int SizeX, int SizeY, int SizeZ,int Elements,int Times) {
        int DataType=AnElement.ComplexType, NumBytes=8, NumBits=64;
	// SizeX=SizeX/2;
        View5D anApplet=Prepare5DViewer(SizeX,SizeY,SizeZ,Elements,Times,DataType,NumBytes,NumBits);
        ((ComplexElement) anApplet.data3d.ActElement()).myData= carray; // new byte[SizeX*SizeY*SizeZ];
	anApplet.data3d.ToggleOverlayDispl(1);
        AlternateViewer aviewer=new AlternateViewer(anApplet,600,500);
        for (int t=0;t<Times;t++)
            for (int e=0;e<Elements;e++)
                if (e==0 && t ==0)
                    aviewer.Assign3DData(anApplet,anApplet.mypan,anApplet.data3d);
                else
                    System.arraycopy(carray, 2*(e+Elements*t)*SizeX*SizeY*SizeZ,
                                 ((ComplexElement) anApplet.data3d.ElementAt(e,t)).myData, 0 , 2*SizeX*SizeY*SizeZ);
        anApplet.start();
        return anApplet;
   }

   /* The code below is necessary to include the software as a plugin into Mathlab and DipImage (Univ. Delft) */
   public static View5D Start5DViewer(double [] farray, int SizeX, int SizeY, int SizeZ,int Elements,int Times) {
        int DataType=AnElement.DoubleType, NumBytes=8, NumBits=64;
        View5D anApplet=Prepare5DViewer(SizeX,SizeY,SizeZ,Elements,Times,DataType,NumBytes,NumBits);
        ((DoubleElement) anApplet.data3d.ActElement()).myData= farray; // new byte[SizeX*SizeY*SizeZ];
	anApplet.data3d.ToggleOverlayDispl(1);
        AlternateViewer aviewer=new AlternateViewer(anApplet,600,500);
        for (int t=0;t<Times;t++)
            for (int e=0;e<Elements;e++)
                if (e==0 && t ==0)
                    aviewer.Assign3DData(anApplet,anApplet.mypan,anApplet.data3d);
                else     
                    System.arraycopy( farray, (e+Elements*t)*SizeX*SizeY*SizeZ,
                                 ((DoubleElement) anApplet.data3d.ElementAt(e,t)).myData, 0 , SizeX*SizeY*SizeZ);
        anApplet.start();
        return anApplet;
   }
   
   /* The code below is necessary to include the software as a plugin into Mathlab and DipImage (Univ. Delft) */
   public static View5D Start5DViewer(int [] iarray, int SizeX, int SizeY, int SizeZ,int Elements,int Times) {
        int DataType=AnElement.IntegerType, NumBytes=4, NumBits=32;
        System.out.println("viewer invoked (int datatype)");
        View5D anApplet=Prepare5DViewer(SizeX,SizeY,SizeZ,Elements,Times,DataType,NumBytes,NumBits);
        ((IntegerElement) anApplet.data3d.ActElement()).myData= iarray; // new byte[SizeX*SizeY*SizeZ];
	anApplet.data3d.ToggleOverlayDispl(1);
        AlternateViewer aviewer=new AlternateViewer(anApplet,600,500);
        for (int t=0;t<Times;t++)
            for (int e=0;e<Elements;e++)
                if (e==0 && t ==0)
                    aviewer.Assign3DData(anApplet,anApplet.mypan,anApplet.data3d);
                else                        
                    System.arraycopy( iarray, (e+Elements*t)*SizeX*SizeY*SizeZ,
                                 ((IntegerElement) anApplet.data3d.ElementAt(e,t)).myData, 0 , SizeX*SizeY*SizeZ);
        anApplet.start();
        return anApplet;
   }

   /* The code below is necessary to include the software as a plugin into Mathlab and DipImage (Univ. Delft) */
   //public static View5D Start5DViewer(short [] sarray, int SizeX, int SizeY, int SizeZ,int Elements,int Times) {
   //     int [] iarray= new int[SizeX*SizeY*SizeZ*Times*Elements];
   //     for (int i=0;i<SizeX*SizeY*SizeZ*Times*Elements;i++)
   //          iarray[i]=sarray[i];
   //     return Start5DViewer(iarray, SizeX, SizeY, SizeZ, Elements, Times);
   //}   
   
   public void ProcessKeyMainWindow(char myChar)  // for simple scripting from outside
   {
	   mypan.c1.ProcessKey(myChar);
	   mypan.c1.UpdateAll();
   }
   
   public void ProcessKeyElementWindow(char myChar)  // for simple scripting from outside
   {
	   mypan.label.PixDisplay.ProcessKey(myChar);      
	   mypan.c1.UpdateAll();
   }

   public String ExportMarkers()
   {
       return data3d.MyMarkers.PrintList(data3d);
   }

   public double[][] ExportMarkerLists()
   {
       return data3d.MyMarkers.ExportMarkerLists(data3d);
   }

   public double[][] ExportMarkers(int list)
   {
       return data3d.MyMarkers.ExportMarkers(list,data3d);
   }
   
   // one of the function accesable from Matlab and Mathematica
   // The whole state of all lists and their connections is imported
   public void ImportMarkerLists(float [][] lists)  // all markers are just stored in one big matrix  
   {
       data3d.MyMarkers.ImportMarkerLists(lists); // insert the markers into the list
       data3d.InvalidateSlices();
       mypan.c1.UpdateAllNoCoord();
   }
   public void DeleteAllMarkerLists() // deletes all existing marker lists
   {
       data3d.MyMarkers.DeleteAllMarkerLists(); // insert the markers into the list	
   }


   public void ImportMarkers(float [][] positions, int NumPos)   // for backward compatibility reasons
   {
     ImportMarkers(positions);
   }

   // one of the function accesable from Matlab and Mathematica.
   // This function imports only one list!
   public void ImportMarkers(float [][] positions)  
   {
       data3d.NewMarkerList();  // open a new list
       data3d.MyMarkers.ImportPositions(positions); // insert the markers into the list
       data3d.InvalidateSlices();
       mypan.c1.UpdateAllNoCoord();
   }
   
   public static View5D Prepare5DViewer(int SizeX, int SizeY, int SizeZ, int Elements, int Times, int DataType, int NumBytes, int NumBits) {
	// SizeX=100,SizeY=100,SizeZ=100,
        int   redEl=-1,greenEl=-1,blueEl=-1;
        double[] Scales = new double[5];
        for (int j=0;j<5;j++) Scales[j]=1.0;                
        double[] Offsets = new double[5];
        for (int j=0;j<5;j++) Offsets[j]=0.0;
        double ScaleV=1.0,OffsetV=0.0;
        int HistoX = 0;
        int HistoY = -1;
        int HistoZ = -1;
        if (Elements > 1)
            HistoY = 1;

        if (Elements > 1)
	{
            redEl=0;
            greenEl=1;
	}
	if (Elements > 2)
            blueEl=2;
                
        if (Elements >= 5)
        {
            redEl=-1;greenEl=-1;blueEl=-1;
        }
        
        String [] Names = new String[5];
        String [] Units = new String[5];
        Names[0] = "X";Names[1] = "Y";Names[2] = "Z";Names[3] = "Elements";Names[4] = "Time";
        Units[0] = "pixels";Units[1] = "ypixels";Units[2] = "zpixels";Units[3] = "elements";Units[4] = "time";
        String NameV = "intensity";
        String UnitV = "a.u.";

        View5D anApplet=new View5D();

        My3DData data3d = new My3DData(anApplet,SizeX,SizeY,SizeZ,
                                    Elements,Times,
                                    redEl,greenEl,blueEl,
                                    HistoX,HistoY,HistoZ,
				    DataType,NumBytes,NumBits,
                                    Scales,Offsets,
                                    ScaleV, OffsetV, Names, Units);
        System.out.println("created data " + Elements);
        for (int e=0;e<Elements;e++)
            data3d.SetValueScale(e,ScaleV,OffsetV,NameV,UnitV);
        
        anApplet.data3d = data3d;
        anApplet.Elements = Elements;
        anApplet.Times = Times;
        anApplet.SizeX = SizeX;
        anApplet.SizeY = SizeY;
        anApplet.SizeZ = SizeZ;
        anApplet.redEl = redEl;
        anApplet.greenEl = greenEl;
        anApplet.blueEl = blueEl;
        anApplet.initLayout(Times);
        if ((Elements > 1 && Elements < 5) || redEl >= 0)
		{
                    data3d.ToggleColor(true);  // switch to Color
                    // data3d.AdjustThresh(true); // initThresh();
		}
                else
                {
                    data3d.ToggleColor(false);  // switch to BW
                    // data3d.AdjustThresh(true); // initThresh();
                }                
        // System.out.println("Layout ready");
        // anApplet.mypan.setVisible(true);  // does not work ???
        // System.out.println("started");
        return anApplet;
    }                
  
  // Container ExtraWindows;
  int ParseInt(String s, boolean dowarn, int adefault) {  // a version of ParseInt, which performs a check only if required
    int result;
      try 
        {result = Integer.parseInt(getParameter(s));}
      catch(Exception e)
        {
            if (dowarn)
            {
            System.out.println("ParseInt: Caught Exceptionlooking for Parameter " + s +":"+e.getMessage());
            e.printStackTrace();
            }
            result = adefault;
	}
    return result;
  }
  
  double ParseDouble(String s, boolean dowarn, double adefault) {  // a version of ParseInt, which performs a check only if required
    double result;
      try 
        {result = Double.valueOf(getParameter(s)).doubleValue();}
      catch(Exception e)
        {
            if (dowarn)
            {
            System.out.println("ParseDouble: Caught Exception looking for Parameter " + s +":"+e.getMessage());
            e.printStackTrace();
            }
            result = adefault;
	}
    return result;
  }
  
  String ParseString(String s, boolean dowarn, String adefault) {  
    String result=null;
      try 
        {result = getParameter(s);}
      catch(Exception e)
        {
            if (dowarn)
            {
            System.out.println("ParseString: Caught Exception looking for Parameter " + s +":"+e.getMessage());
            e.printStackTrace();
            }
            result = adefault;
	}
    if (result == null)
            result = adefault;
        
    return result;
  }

  public String StringFromType(int TypeNr)
  {
  	if (TypeNr >= 0 && TypeNr < AnElement.NumTypes)
	{
	   return AnElement.TypeNames[TypeNr];
	}
	else
	{
	 System.out.println("Error in StringFromType: Unknown datatype: "+TypeNr+"\n");
	 return "";
	}
  }
  
  public int TypeFromString(String AType)
  {
  	for (int i=0;i<AnElement.NumTypes;i++)
	   if (AType.equals(AnElement.TypeNames[i]) || AType.equals(AnElement.UTypeNames[i]))  // unsigned types are converted to signed
	   	return i;
	System.out.println("Error: Unknown datatype: "+AType+"\n");
	return -1;
  }
      
  public void initLayout(int Times) {
                    // data3d.initGlobalThresh();
                    
		setLayout(new BorderLayout());
		mypan = new ImgPanel(this,data3d);
                panels.addElement(mypan);  // enter this view into the list
		add("Center", mypan);
                myLabel=new TextArea("5D-viewer Java Applet by Rainer Heintzmann, [press '?' for help]",1,76,TextArea.SCROLLBARS_NONE);
                add("North", myLabel);
 		mypan.CheckScrollBar(); 
                //Frame myfr=new Frame("View5D menu");
                //myfr.setMenuBar(mypan.MyMenu);
                //myfr.setSize(this.getSize().width,20);
                //myfr.show();
                
        setVisible(true);
  }
  
  public void init() {
	 if (System.getProperty("java.version").compareTo("1.4") < 0)  // this viewer should work from version 1.1 on
	     {
	 	setLayout(new BorderLayout());
	 	add("North", new ImageErr());
	 	setVisible(true);
	     }
	 else
	    {
		filename = getParameter("file");
		if (filename == null) filename="xxx.raw";
		
		SizeX = Integer.parseInt(getParameter("sizex"));
		SizeY = Integer.parseInt(getParameter("sizey"));
		SizeZ = ParseInt("sizez",true,1);
                Times = ParseInt("times",false,1);
                defaultColor0 = ParseInt("defcol0",false,-1);
                Elements = ParseInt("elements",true,1);
		if (Elements > 1)
		{
                    redEl=0;
                    greenEl=1;
		}
		if (Elements > 2)
                    blueEl=2;
                
                if (Elements >= 5)
                {
                    redEl=-1;greenEl=-1;blueEl=-1;
                }
		redEl = ParseInt("red",false,redEl);
		greenEl = ParseInt("green",false,greenEl);
		blueEl = ParseInt("blue",false,blueEl); 
		    
		int DataType=AnElement.ByteType;
		int DataTypeN;
                int NumBytes=1;
		int NumBits=8;
		NumBytes = ParseInt("bytes",false,NumBytes);  // 0,1 means byte, 2= short, -1 = float
		NumBits = ParseInt("bits",false,NumBits);  
                if (NumBytes > 1) {
                	if (NumBytes > 1) {
				DataType=AnElement.IntegerType; }
			else {
				DataType=AnElement.ShortType; }
			}
                else if (NumBytes == -1) {DataType=AnElement.FloatType;NumBytes=4;NumBits=32;}
                if (NumBytes*8 < NumBits) NumBits = NumBytes*8;
		
		String DatType = ParseString("dtype",false,StringFromType(DataType));  // 0: byte, 1: int, 2: float, 3: double, 4: complex(float)
		
		DataTypeN = TypeFromString(DatType);
		if (DataTypeN != DataType) 
		{
		   DataType = DataTypeN;
		   if (DataType == AnElement.IntegerType && NumBytes <1)
		   	NumBytes=2;
		   if (DataType == AnElement.ByteType)
		   	NumBytes = 1;
		   if (DataType == AnElement.FloatType)
		   	NumBytes = 4;
		   if (DataType == AnElement.DoubleType)
		   	NumBytes = 8;
		   if (DataType == AnElement.ComplexType)
		   	NumBytes = 8;
		   if (DataType == AnElement.ShortType)
		   	NumBytes = 2;
		   if (DataType == AnElement.LongType)
		   {
		   	DataType=AnElement.IntegerType;
		   	NumBytes = 4;
		   }
		}
		
                double[] Scales = new double[5];
                for (int j=0;j<5;j++) Scales[j]=1.0;                
                double[] Offsets = new double[5];
                for (int j=0;j<5;j++) Offsets[j]=0.0;
                double ScaleV=1.0,OffsetV=0.0;
                
                Scales[0] = ParseDouble("scalex",false,1.0);  // scaling
		Scales[1] = ParseDouble("scaley",false,1.0);  
		Scales[2] = ParseDouble("scalez",false,1.0);
		Scales[3] = ParseDouble("scalee",false,1.0);
		Scales[4] = ParseDouble("scalet",false,1.0);
                Offsets[0] = ParseDouble("offsetx",false,0.0);  // scaling
		Offsets[1] = ParseDouble("offsety",false,0.0);  
		Offsets[2] = ParseDouble("offsetz",false,0.0);
		Offsets[3] = ParseDouble("offsete",false,0.0);
		Offsets[4] = ParseDouble("offsett",false,0.0);
                String [] Names = new String[5];
                String [] Units = new String[5];
                String NameV = "intensity";
                String UnitV = "a.u.";
                
                Names[0] = ParseString("namex",false,"X");
                Names[1] = ParseString("namey",false,"Y");
                Names[2] = ParseString("namez",false,"Z");
                Names[3] = ParseString("namee",false,"Elements");
                Names[4] = ParseString("namet",false,"Time");
                Units[0] = ParseString("unitsx",false,"pixels");
                Units[1] = ParseString("unitsy",false,"ypixels");
                Units[2] = ParseString("unitsz",false,"zpixels");
                Units[3] = ParseString("unitse",false,"elements");
                Units[4] = ParseString("unitst",false,"time");
                
		if (SizeX <= 0) SizeX = 256;
		if (SizeY <= 0) SizeY = 256;
		if (SizeZ <= 0) SizeZ = 10;

                int HistoX = 0;
                int HistoY = -1;
                int HistoZ = -1;
                if (Elements > 1)
                    HistoY = 1;
                // if (Elements > 2)    HistoZ = 2;  // the user will have to choose
                
                HistoX = ParseInt("histox",false,HistoX);  
		HistoY = ParseInt("histoy",false,HistoY);  
		HistoZ = ParseInt("histoz",false,HistoZ);  
		
                data3d = new My3DData(this,SizeX,SizeY,SizeZ,
                                    Elements,Times,
                                    redEl,greenEl,blueEl,
                                    HistoX,HistoY,HistoZ,DataType,NumBytes,NumBits, 
                                    Scales,Offsets,
                                    ScaleV,OffsetV,Names,Units);

                if(defaultColor0 >0) 
			data3d.ToggleModel(0,defaultColor0);  
                
                for (int e=0;e<Elements;e++)
                {
                    ScaleV = ParseDouble("scalev"+(e+1),false,1.0); 
                    OffsetV = ParseDouble("offsetv"+(e+1),false,0.0); 
                    NameV = ParseString("namev"+(e+1),false,"intensity"); 
                    UnitV = ParseString("unitsv"+(e+1),false,"a.u."); 
                    data3d.SetValueScale(e,ScaleV,OffsetV,NameV,UnitV);
                }
                
		data3d.Load(DataType,NumBytes,NumBits,filename,this);

		String MyMarkerIn = getParameter("markerInFile");
		if (MyMarkerIn != null) 
			{
			data3d.markerInfilename=MyMarkerIn;
			System.out.println("... loading marker file "+data3d.markerInfilename);
				data3d.LoadMarkers();
			}

		String MyMarkerOut = getParameter("markerOutFile");
		if (MyMarkerOut != null) 
			data3d.markerOutfilename = MyMarkerOut;

        initLayout(Times);
                if ((Elements > 1 && Elements < 5) || redEl >= 0)
		{
                    data3d.ToggleColor(true);  // switch to Color
                    // data3d.AdjustThresh(true); // initThresh();
		}
                else
                {
                    data3d.ToggleColor(false);  // switch to BW
                    // data3d.AdjustThresh(true); // initThresh();
                }                
	    }
    }

    public void start() {
		data3d.AdjustThresh(true); // initThresh();
		mypan.InitScaling();
    }

    public void stop() {
    }

    public void destroy() {
    }


    public String getAppletInfo() {
        return "A 5Dimage viewing tool.";
    }

}
