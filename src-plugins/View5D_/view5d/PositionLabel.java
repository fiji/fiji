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

import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.text.*;

public class PositionLabel extends Panel implements MouseListener{
    static final long serialVersionUID = 1;
    ImageCanvas c1,c2,c3;
    My3DData data3d;
    Label l1,l2,l3,l4,l5,l6;
    public PixelDisplay PixDisplay;
    TextArea MyText;
    NumberFormat nf;
    NumberFormat nf2;
    Scrollbar  TimeScrollbar=null;  // if present it will be adjusted

    int px=0,py=0,pz=0,pt=0,lnr=-1,lpos=-1;

    PopupMenu MyPopupMenu;
    CheckboxMenuItem DispIntScaleOffset;
    CheckboxMenuItem DispPosSizePix;
    CheckboxMenuItem DispPosWorld;
    CheckboxMenuItem DispROIInfo;
    CheckboxMenuItem DispCoordScales;
    CheckboxMenuItem DispThreshColor;
    CheckboxMenuItem DispListNrPos;
    CheckboxMenuItem DispMarkerInfo;
    
    public void mousePressed(MouseEvent e) {

        if (e.isPopupTrigger())
        {
            MyPopupMenu.show(this,e.getX(),e.getY());
	    CoordsChanged();
            return;
        }
    }
    
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger())
        {
            MyPopupMenu.show(this,e.getX(),e.getY());
	    CoordsChanged();
            return;
        }
    }
	
    public void mouseEntered(MouseEvent e) {
	requestFocus();
    }
    public void mouseClicked(MouseEvent e) { 
    }
    public void mouseExited(MouseEvent e) {
    }
  
  public PositionLabel(String text,ImageCanvas C1,ImageCanvas C2, ImageCanvas C3,My3DData data)
  {
    c1 = C1;
    c2 = C2;
    c3 = C3;
    data3d = data;
    MyText= new TextArea(text ,15,30);
    MyText.setEditable(false);
    PixDisplay = new PixelDisplay(data,c1,c2,c3);
    nf = java.text.NumberFormat.getNumberInstance(Locale.US);
    nf.setMaximumFractionDigits(2);
    // nf.setMinimumIntegerDigits(7);
    nf.setGroupingUsed(false);

    nf2 = java.text.NumberFormat.getNumberInstance(Locale.US);
    nf2.setMaximumFractionDigits(4);
    nf2.setGroupingUsed(false);

    // Panel grid = new Panel();
    GridLayout myLayout=new GridLayout(2, 2);
    // GridBagLayout myLayout=new GridBagLayout(2, 2);
    setLayout(myLayout);
    // add("South", this);
    add(MyText);
    add(PixDisplay);
    MyText.addMouseListener(this); // register this class for handling the events in it
    	
    MyPopupMenu =new PopupMenu("Text Menu");  // tear off menu
    Menu MyMenu =new PopupMenu("Text to Display");  // Why is this needed? Otherwise the menu is not updating
    add(MyPopupMenu);
    MyPopupMenu.add(MyMenu);
    //Menu SubMenu = new Menu("Navigation",false);  // can eventually be dragged to the side
    DispIntScaleOffset = new CheckboxMenuItem("Intensity, Scale, Offset, Type : ",true);
    MyMenu.add(DispIntScaleOffset);
    DispPosSizePix = new CheckboxMenuItem("Position, Size (Pixel coordinates): ",true);
    MyMenu.add(DispPosSizePix);
    DispPosWorld = new CheckboxMenuItem("Position, (world coordinates): ",true);
    MyMenu.add(DispPosWorld);
    DispROIInfo = new CheckboxMenuItem("ROI Information: ",true);
    MyMenu.add(DispROIInfo);
    DispCoordScales = new CheckboxMenuItem("Coordinate Scales: ",true);
    MyMenu.add(DispCoordScales);
    DispThreshColor = new CheckboxMenuItem("Thresholds, Color: ",true);
    MyMenu.add(DispThreshColor);
    DispListNrPos = new CheckboxMenuItem("List Nr., Position: ",true);
    MyMenu.add(DispListNrPos);
    DispMarkerInfo = new CheckboxMenuItem("Marker and List Information: ",true);
    MyMenu.add(DispMarkerInfo);  
  }

/*      
  void TextDisplayDialog() {  // allows the user to define the units and scales
        GenericDialog md= new GenericDialog("Text To Display");
        md.addCheckbox("Intensity, Scale, Offset: ",DispIntScaleOffset);
        md.addCheckbox("Position, Size (Pixel coordinates): ",DispPosSizePix);
        md.addCheckbox("Position, (world coordinates): ",DispPosWorld);
        md.addCheckbox("ROI Information: ",DispROIInfo);
        md.addCheckbox("Coordinate Scales: ",DispCoordScales);
        md.addCheckbox("Thresholds, Color: ",DispThreshColor);
        md.addCheckbox("List Nr., Position: ",DispListNrPos);
        md.addCheckbox("Marker and List Information: ",DispMarkerInfo);
        md.showDialog();
        if (! md.wasCanceled())
        {
            String NV,UV;
            double SV,OV,Min,Max;
            DispIntScaleOffset=md.getNextBoolean();
            DispPosSizePix=md.getNextBoolean();
            DispPosWorld=md.getNextBoolean();
            DispROIInfo=md.getNextBoolean();
            DispCoordScales=md.getNextBoolean();
            DispThreshColor=md.getNextBoolean();
            DispListNrPos=md.getNextBoolean();
            DispMarkerInfo=md.getNextBoolean();
        }
    }
  */
  
  void Help() {
       // javax.swing.JOptionPane.showMessageDialog(applet,
      String newtext="Java 5D image viewer, Version V"+View5D_.serialVersionUID+"."+View5D_.serialSubVersionUID+"."+View5D_.serialSubSubVersionUID+"" +
      " by Rainer Heintzmann\nUniversity of Jena, Germany (heintzmann@gmail.com)\n"+
              "Right-click for menu\nUse mouse click for changing slices, \n"+
              "Shift and mouse-drag for square ROIs, Ctrl and mouse-drag for multiple line ROIs\ndrag images, zoom by typing 'A' and 'a' or into ROI by 'Z'\n"+
	      "Arrow keys for fine control of slicing position\n"+
	      "'next page' and 'prev. page'  for changing perpendicular slice ,\n'<' and '>' for perpendicular magnificaltion\n"+
	      "'i' for init view, 'c' for change ColorMap, \n"+
	      "1,2,5,6' for lower and '3,4,7,8' for upper Threshold, 't' and 'T' for automatic contrast adjustment using one or all elements\n"+
	      "'e' : toggle elements (if present), 'C' : toggle multicolor display, 'r','g','b' : select element for respective display\n"+
	      "'R','G','B' : clear respective channel, 'p','P': Toggle Projections (MIP, Avg)\n For Documentation see http://www.nanoimaging.de/View5D/";
    MyText.setText(newtext);
    MyText.setCaretPosition(0);
  }
  
  void PrintPointList() {
    MyText.setText(data3d.GetMarkerPrintout(data3d));
    MyText.setCaretPosition(0);
  }

  String GetPositionString() {
  String pstr = "";
  pstr = pstr + "Pos: (" +
   nf.format(((int) (c2.PositionValue))*data3d.GetScale(0,0)+data3d.GetOffset(0,0)) + ", " +
   nf.format(((int) (c3.PositionValue))*data3d.GetScale(0,1)+data3d.GetOffset(0,1)) + ", " +
   nf.format(((int) (c1.PositionValue))*data3d.GetScale(0,2)+data3d.GetOffset(0,2)) + ", "+
   nf.format(data3d.GetActiveElement()*data3d.GetScale(0,3)+data3d.GetOffset(0,3)) +", "+
   nf.format(data3d.GetActiveTime()*data3d.GetScale(0,4)+data3d.GetOffset(0,4))+
   ") ["+data3d.GetAxisUnits()[0]+", "+data3d.GetAxisUnits()[1]+", "+data3d.GetAxisUnits()[2]+", "+data3d.GetAxisUnits()[3]+", "+data3d.GetAxisUnits()[4]+"]";
  return pstr;
  }
  
  String CreateValueString(String valstring) {
  String str= data3d.GetValueName(data3d.GetActiveElement())+" ["+data3d.GetValueUnit(data3d.GetActiveElement())+"]: "
        + valstring;
     return str;
  }
  
  String CreateValueString(double val) {
  return CreateValueString(nf.format(val));
  }
  
  String GetValueString() {
     return CreateValueString(data3d.ActElement().GetValueStringAt((int) (c2.PositionValue),(int) (c3.PositionValue),(int) (c1.PositionValue)));
  }
  
  void CoordsChanged() {
      px = (int) (c2.PositionValue);
      py = (int) (c3.PositionValue);
      pz = (int) (c1.PositionValue);
      pt = data3d.ActiveTime;
      lnr = data3d.MyMarkers.ActiveList;
      lpos = data3d.MyMarkers.ActiveMarkerPos();

      int elem = data3d.GetActiveElement();
      //int elemR = data3d.GetChannel(0);
      //int elemG = data3d.GetChannel(1);
      //int elemB = data3d.GetChannel(2);
      boolean colormd = data3d.GetColorMode();
      boolean projmode = data3d.GetProjectionMode(2);
      boolean histconnected = (data3d.DataToHistogram != null);
      
      // Rectangle r = PixDisplay.getBounds();      
      
      // System.out.println("Rectangle "+r);
      /*if (r.height > 80)
	  {
	      PixDisplay.setBounds(r.x,r.y+r.height-80,r.width,80);
	      Rectangle rt=MyText.getBounds();
	      MyText.setBounds(rt.x,rt.y,rt.width,rt.height+r.height-80);
	  }*/

      PixDisplay.CoordinatesChanged();
      double PsizeX=data3d.GetROISize(-1,0),
             PsizeY=data3d.GetROISize(-1,1),
             PsizeZ=data3d.GetROISize(-1,2);
      double diagonal = Math.sqrt(PsizeX*PsizeX+PsizeY*PsizeY+PsizeZ*PsizeZ);
      
      if (c2.myPanel.ROIstarted) 
    	  	{PsizeX=(data3d.ActElement().Scales[0] * (1+Math.abs((double) c2.ROIe-c2.ROIs)));}
      if (c3.myPanel.ROIstarted) 
    	    {PsizeY=(data3d.ActElement().Scales[1] * (1+Math.abs((double) c3.ROIe-c3.ROIs)));}
      if (c1.myPanel.ROIstarted) 
    	    {PsizeZ=(data3d.ActElement().Scales[2] * (1+Math.abs((double) c1.ROIe-c1.ROIs)));}
      
      //String NameV = data3d.GetValueName(elem);
      //String UnitV = data3d.GetValueUnit(elem);
      String UnitX = data3d.GetAxisUnits()[0];
      String UnitY = data3d.GetAxisUnits()[1];
      String UnitZ = data3d.GetAxisUnits()[2];
      // String UnitE = data3d.GetAxisUnits()[3];
      String UnitT = data3d.GetAxisUnits()[4];
      String X = data3d.GetAxisNames()[0];
      String Y = data3d.GetAxisNames()[1];
      String Z = data3d.GetAxisNames()[2];

      double sx=data3d.GetScale(0,0),sy=data3d.GetScale(0,1),sz=data3d.GetScale(0,2),st=data3d.GetScale(0,4);
      // double se=data3d.GetScale(0,3);
      double mx=0,my=0,mz=0;
      double mx2=0,my2=0,mz2=0;
      double dxy2=0,Dxy=0;
      double dxyz=0,Dxyz=0,DSum=0,dt=0;
      double slope=0,slope3=0;  // 2D speed and 3D / time speed
      double MIntegral=0,MMax=0;
      for (int i=0; i < data3d.NumMarkers(-1);i++)
      {
          if (i == data3d.ActiveMarkerPos())
          {
            mx=data3d.GetPoint(i).coord[0];
            my=data3d.GetPoint(i).coord[1];
            mz=data3d.GetPoint(i).coord[2];
            int iprev=i-1;
            if (iprev < 0) iprev=0;
            mx2=data3d.GetPoint(iprev).coord[0];
            my2=data3d.GetPoint(iprev).coord[1];
            mz2=data3d.GetPoint(iprev).coord[2];
            dt=(data3d.GetPoint(i).coord[4] - data3d.GetPoint(iprev).coord[4])*st;
            dxy2= (mx-mx2)*(mx-mx2)*sx*sx+(my-my2)*(my-my2)*sy*sy;
            dxyz= Math.sqrt(dxy2+(mz-mz2)*(mz-mz2)*sz*sz);
              Dxy = Math.sqrt(dxy2);
              Dxyz=dxyz;
              if (mz -mz2 != 0.0 && sz != 0.0)
                slope= Dxy / (mz-mz2)/sz;
              else
                  slope = 0.0;
              if (dt != 0.0)
                slope3= Dxyz / dt;
              else
                  slope3 = 0.0;
              MIntegral=data3d.GetPoint(i).integral;
              MMax=data3d.GetPoint(i).max;
          }
          DSum += dxyz;
      }
      String GateString="";
      if (data3d.GateActive)
          GateString="(gated) ";
      
      String newtext="";
  if (DispIntScaleOffset.getState()) {
      newtext = newtext+GetValueString()+", scale :" + nf.format(data3d.GetValueScale(elem))+", offset: "+data3d.GetValueOffset(elem)+", type: "+data3d.GetDataTypeName(elem)+"\n";}
  if (DispPosSizePix.getState()) {
      newtext = newtext+"at (" +nf.format(px) + ", " +nf.format(py) + ", " +nf.format(pz) + ", "+nf.format(elem)+", "+nf.format(pt)+") of ("+
			  data3d.SizeX+", "+data3d.SizeY+", "+data3d.SizeZ+", "+data3d.Elements+", "+data3d.Times+")\n";}
  if (DispPosWorld.getState()) {
    newtext = newtext+GetPositionString()+"\n"+
                          "Coordinate scales: ("+nf.format(sx)+", "+nf.format(sy)+", "+nf.format(sz) + ")  ,";}
  if (DispListNrPos.getState()) {
    newtext = newtext + "ListNr+Pos: ("+nf.format(lnr+1)+", "+nf.format(lpos)+")\n"; }
  if (DispROIInfo.getState()) {
    newtext = newtext + "ROI Sizes ["+UnitX+"]: ("+(PsizeX+"     ").substring(0,6) + ", " + nf.format(PsizeY) + ", " +
                          nf.format(PsizeZ) + ")\n -> 3D-Diagonal["+UnitX+"]: "+nf.format(diagonal)+"\n"+
                          "ROI Volume "+GateString+nf.format(data3d.GetROIVoxels(elem))+" voxels, "+nf.format(data3d.GetROIVoxels(elem)*sx*sy*sz)+" ["+UnitX+"*"+UnitY+"*"+UnitZ+"]\n"+
                          "Sum:"+nf.format(data3d.GetROISum(elem))+", Average:"+nf.format(data3d.GetROIAvg(elem))+", Max:"+nf.format(data3d.GetROIMax(elem))+", Min:"+nf.format(data3d.GetROIMin(elem))+"\n";}			  
  if (DispCoordScales.getState()) {
    newtext = newtext + "Magnifications (x,y,z) = " +nf.format(c2.scale)+", " + nf.format(c3.scale)+"," + nf.format(c1.scale)+"\n";}
  if (DispThreshColor.getState()) {
    newtext = newtext + "Thresholds : min=" +nf.format(data3d.GetScaledMincs(elem))+" max=" +nf.format(data3d.GetScaledMaxcs(elem))+"\n"+
			  "Color: "+colormd+", "+"Projection Mode: "+projmode+", Data connected: " + histconnected+ "\n";}
  if (DispMarkerInfo.getState()) {
        newtext = newtext + "List "+data3d.ActiveMarkerListPos()+", Marker " + data3d.ActiveMarkerPos() +": " + nf.format(mx) + ", " + nf.format(my) + ", "+ nf.format(mz) +", Integral: "+nf.format(MIntegral)+", Max= "+nf.format(MMax)+"\n"+
                          "Last Distance "+X+Y+"=" + nf2.format(Dxy) + ", "+X+Y+Z+"=" + nf2.format(Dxyz) +"\n"+
                          "Slope "+X+"/"+Z+"="+nf2.format(slope)+" ["+UnitX+"/"+UnitZ+"]"+ ", TimeSlope="+nf2.format(slope3)+" ["+UnitX+"/"+UnitT+"]"+"  Sum Distances "+X+Y+Z+"="+nf2.format(DSum);
			  }
      // "Red: "+elemR+" Green: "+elemG+" Blue: "+elemB+

     MyText.setText(newtext);
     MyText.setCaretPosition(0);
     if (TimeScrollbar != null)
         TimeScrollbar.setValue(pt);
     
     // MyText.replaceRange(newtext,0,10000);
    }
}
