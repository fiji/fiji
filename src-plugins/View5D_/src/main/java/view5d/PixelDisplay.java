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
import java.awt.event.*;
import java.awt.Graphics;
import java.awt.*;
import java.util.*;
import java.text.*;

public class PixelDisplay extends Panel implements MouseListener,ImageObserver,KeyListener {  // A panel displaying the multi-element contens of a single pixel
    static final long serialVersionUID = 1;
    ImageCanvas c1,c2,c3;
    My3DData data3d;
    int	imgw = -1;
    int	imgh = -1;
    private int lxold=0,lyold=0;  // stores the old end-position of a plotting line
    private int PointNr=0;   // One bigger than datapoint just plotted
    private double pval=0;
    boolean didinit=false;
    int PlotMode = 0;  // 0 : no plot but element-color display, 1: Spectrum Plot, 2: Marker Plot
    PlotInfo myInfo = new PlotInfo();
    boolean normalize=true;  // if true, all spectra will be normalized to the maximum pixel
    boolean logmode=false;  // if true, all elements will be set to log-mode
    double maxdispl=1.0,mindispl=0.0;
    String TextToDisplay="";  // Tags will be added to it.
    PopupMenu MyPopupMenu;
    Menu   ColorMenu;

    void AddColorMenu(String Name,int i)
    {
        MenuItem tmp;
        tmp = new MenuItem(Name);
        tmp.addActionListener(new MyMenuProcessor(this,c1,false,i)); ColorMenu.add(tmp);
        tmp = new MenuItem(Name+" (inverted)");
        tmp.addActionListener(new MyMenuProcessor(this,c1,true,i)); ColorMenu.add(tmp);
    }
    
    PixelDisplay(My3DData data, ImageCanvas C1,ImageCanvas C2, ImageCanvas C3)
    {
	c1 = C1;
	c2 = C2;
	c3 = C3;
	data3d = data;
	// Rectangle r=getBounds();
	// System.out.println("Rectangle "+r);
	setBounds(0,0,200,50);
	// r=getBounds();
	// System.out.println("Rectangle "+r);
	// initialize();
	if (data.Elements > 5)
	    PlotMode = 1;    // Spectrum Plot

	addMouseListener(this); // register this class for handling the events in it
	addKeyListener(this); // register this class for handling the events in it
        
        MyPopupMenu =new PopupMenu("Element Menu");  // tear off menu
	add(MyPopupMenu);

        Menu SubMenu = new Menu("General",false);  // can eventually be dragged to the side
        MyPopupMenu.add(SubMenu);

        MenuItem tmp;
	tmp = new MenuItem("initialise scaling [i]");
	tmp.addActionListener(new MyMenuProcessor(this,'i')); SubMenu.add(tmp);
	tmp = new MenuItem("Set Value Units and Scalings [N]");
	tmp.addActionListener(new MyMenuProcessor(this,'N')); SubMenu.add(tmp);
	tmp = new MenuItem("eXport to ImageJ (ImageJ only) [X]");
	tmp.addActionListener(new MyMenuProcessor(this,'X')); SubMenu.add(tmp);

        SubMenu = new Menu("Markers",false);  // can eventually be dragged to the side
        MyPopupMenu.add(SubMenu);
	tmp = new MenuItem("Marker Menu [M]");
	tmp.addActionListener(new MyMenuProcessor(this,'M')); SubMenu.add(tmp);
	tmp = new MenuItem("print/save marker list [m]");
	tmp.addActionListener(new MyMenuProcessor(this,'m')); SubMenu.add(tmp);

        SubMenu = new Menu("Plotting",false);  // can eventually be dragged to the side
        MyPopupMenu.add(SubMenu);
        tmp = new MenuItem("spawn plot display [s]");
	tmp.addActionListener(new MyMenuProcessor(this,'s')); SubMenu.add(tmp);
        tmp = new MenuItem("toggle plot display [q]");
	tmp.addActionListener(new MyMenuProcessor(this,'q')); SubMenu.add(tmp);
        tmp = new MenuItem("normalize plot display [n]");
	tmp.addActionListener(new MyMenuProcessor(this,'n')); SubMenu.add(tmp);
        tmp = new MenuItem("logarithmic mode [O]");
	tmp.addActionListener(new MyMenuProcessor(this,'O')); SubMenu.add(tmp);
        SubMenu = new Menu("ColorMaps",false);  // can eventually be dragged to the side
        MyPopupMenu.add(SubMenu);
        ColorMenu=SubMenu;   // is used in AddColorMenu
        
        for (int i=0;i<Bundle.ElementModels;i++)
        {
            AddColorMenu(Bundle.ElementModelName[i],i);
        }

    }

    Color GetBWColor(int e) {
      int px = (int) (c2.PositionValue);
      int py = (int) (c3.PositionValue);
      int pz = (int) (c1.PositionValue);
      int val=(int) (data3d.NormedValueAt(px,py,pz,e)*255.0);
      // if (data3d.NumBits > 8) val = val >> (data3d.NumBits-8); 
      if (val < 0) val=0;
      if (val > 255) val = 255;
      return new Color(val,val,val);
   }

    Color GetBWSatColor(int e) {
      int px = (int) (c2.PositionValue);
      int py = (int) (c3.PositionValue);
      int pz = (int) (c1.PositionValue);
      double valsat=data3d.NormedValueAt(px,py,pz,e)*255;
            
      if (valsat >= 255)
	  return Color.blue;	
      else if (valsat <= 0)
         return new Color(0,100,0);
      else
         return new Color((int) valsat,(int) valsat,(int) valsat);
   }

    Color GetColColor(int e) {  // returns the color shown on sceen (without overlay)
        return data3d.GetColColor(e,(int) (c2.PositionValue),(int) (c3.PositionValue),(int) (c1.PositionValue));
   }

    Color GetMarkerColor(int e) {
      return data3d.GetMarkerColor(e);
   }
    
    Color GetCMapColor(int e, int pos, int range) {
      return data3d.GetCMapColor(e,pos,range);
   }

    void CoordinatesChanged()
    {
     repaint();
    }

    public void paint(Graphics g) {
	if (PlotMode > 0)  // one of the plot modes
		{
		    plot(g);
		    return;
		}
	else
	  drawCMaps(g);
    }

    public void drawCMaps(Graphics g)
    {
    	Rectangle r = getBounds();
       	int elements = data3d.GetNumElements();
	int elem = data3d.GetActiveElement();
	double sizex = (r.width-1)/(double) elements;
        int sizey = (r.height-1)/ 4;

        for (int e=0;e < elements;e++)
            {
           	for (int y=0;y < r.height;y++)  // iterates through the y pixels
                {
			g.setColor(GetCMapColor(e,r.height-y-1,r.height));
                	g.drawLine((int) (e*sizex), y, (int) ((e+1)*sizex), y);
		}
                g.setColor(Color.white);
                if (e == data3d.HistoX)
                    g.drawString("HX",(int)(e*sizex+5),(int)(1.0*sizey)); // +(1*sizex/3-10)
                if (e == data3d.HistoY)
                    g.drawString("HY",(int)(e*sizex+5),(int)(1.33*sizey));
                if (e == data3d.HistoZ)
                    g.drawString("HZ",(int)(e*sizex+5),(int)(1.66*sizey));
                if (e == data3d.GateElem)
                    if (data3d.GateActive)
                        g.drawString("GA",(int)(e*sizex+5),(int) 3.0*sizey);
                    else
                        g.drawString("G",(int)(e*sizex+5),(int) 3.0*sizey);
                if (data3d.InOverlayDispl(e))
                    g.drawString("Ov",(int)(e*sizex+0) ,(int) 4.0*sizey);
            // g.drawString(mytitle,10,10);
            }
        g.setColor(Color.red);
        g.drawRect(elem*r.width/elements,0,(int)sizex,r.height);
        super.paint(g);
    }
    
    public void drawElements(Graphics g)
    {
	Rectangle r = getBounds();
       	int elements = data3d.GetNumElements();
	int elem = data3d.GetActiveElement();
	double sizex = (r.width-1)/(double) elements;
        int sizey = (r.height-1)/ 4;

        for (int e=0;e < elements;e++)
            {
                g.setColor(GetBWColor(e));
                g.fillRect((int) (e*sizex), 0, (int)((e+1)*sizex), sizey);
                g.setColor(GetBWSatColor(e));
                g.fillRect((int) (e*sizex), sizey, (int)((e+1)*sizex), 2*sizey);
                g.setColor(GetColColor(e));
                g.fillRect((int) (e*sizex), 2*sizey,(int)((e+1)*sizex), 3*sizey);
                g.setColor(GetMarkerColor(e));
                g.fillRect((int) (e*sizex), 3*sizey,(int)((e+1)*sizex), 4*sizey);
                g.setColor(Color.white);
                if (e == data3d.HistoX)
                    g.drawString("HX",(int)(e*sizex+5),(int)(4.2*sizey));
                if (e == data3d.HistoY)
                    g.drawString("HY",(int)(e*sizex+5+(sizex/3-10)),(int)4.2*sizey);
                if (e == data3d.HistoZ)
                    g.drawString("HZ",(int)(e*sizex+5+(2*sizex/3-10)),(int)4.2*sizey);
                if (e == data3d.GateElem)
                    if (data3d.GateActive)
                        g.drawString("GA",(int)(e*sizex+5),(int) 3.0*sizey);
                    else
                        g.drawString("G",(int)(e*sizex+5),(int) 3.0*sizey);
                if (data3d.InOverlayDispl(e))
                    g.drawString("Ov",(int)(e*sizex+0) ,(int) 4.2*sizey);
            // g.drawString(mytitle,10,10);
            }
        g.setColor(Color.red);
        g.drawRect(elem*r.width/elements,0,(int)sizex,r.height);
        super.paint(g);
    }
    public double ApplyLog(double val)
    {
     if (! logmode)
         return val;
     if (val > 0)
         return (java.lang.Math.log(val)-java.lang.Math.log(0.001))/ (-java.lang.Math.log(0.001));
     else
        return -10;
    }

  int InitDataPoints(int mlist)  // return numer of data-points to plot
  {
      PointNr=0;
      switch (PlotMode)
      {
          case 0: return 0;
          case 1:  // Plot spectrum and Actually displayed marker spectra
            return data3d.Elements;
          case 2:  // Plot marker intensity or maximum traces
          case 3:  
          case 4:  
          case 5:  
            return data3d.NumMarkers(mlist);
      }
      return 0;
  }

  double GetNextDataPoint(int mlist)  // mlist < 0 means no list
  {
      double val=0;
      int e;

      switch (PlotMode)
      {
          case 0: break;
          case 1:  // Plot spectrum and Actually displayed marker spectra
            if (mlist < 0)
            {
                if (data3d.GetProjectionMode(2))
                    val=data3d.GetROIVal(PointNr);
                else 
                    // if (normalize)
                    val=data3d.NormedValueAt((int) (c2.PositionValue),(int) (c3.PositionValue),(int) (c1.PositionValue),PointNr);
                    // else
                    // val=data3d.ValueAt((int) (c2.PositionValue),(int) (c3.PositionValue),(int) (c1.PositionValue),PointNr);
            }
            else  // displayed markers have to be spectrally displayed
            {
                val=data3d.NormedValueAt((int) (data3d.GetPoint(-1,mlist).coord[0]+0.5),
				      (int) (data3d.GetPoint(-1,mlist).coord[1]+0.5),
                                      (int) (data3d.GetPoint(-1,mlist).coord[2]+0.5),PointNr);
            }
            break;
          case 2:  // Plot marker intensity or maximum traces
              pval=data3d.GetPoint(PointNr,mlist).integral / ((data3d.COMX*2.0+1.0)*(data3d.COMY*2.0+1.0)*(data3d.COMZ*2.0+1.0));
              e = (int) data3d.GetPoint(PointNr,mlist).coord[3];
              val = data3d.Normalize(pval,e);
            break;
          case 3:  // Plot marker intensity or maximum traces
              pval=data3d.GetPoint(PointNr,mlist).max;
              e = (int) data3d.GetPoint(PointNr,mlist).coord[3];
              val = data3d.Normalize(pval,e);
            break;
          case 4:  // Plot marker intensity or maximum traces
              pval=data3d.GetPoint(PointNr,mlist).min;
              e = (int) data3d.GetPoint(PointNr,mlist).coord[3];
              val = data3d.Normalize(pval,e);
            break;
          case 5:  // Plot marker intensity or maximum traces
              pval=data3d.GetPoint(PointNr,mlist).integralAboveMin / ((data3d.COMX*2.0+1.0)*(data3d.COMY*2.0+1.0)*(data3d.COMZ*2.0+1.0));
              e = (int) data3d.GetPoint(PointNr,mlist).coord[3];
              val = data3d.Normalize(pval,e);
            break;
      }
      PointNr++;   // Advance to the next datapoint
      return val;
  }

  double GetNextNormDataPoint(int mlist) // mlist==-1 means not plotting markers but cursor position
  {
     double val=GetNextDataPoint(mlist);
     val=ApplyLog(val);  // Only if in logmaode
     if (PointNr== 1)  // first data point
     {
        myInfo.newMaxVal = val;
        myInfo.newMinVal = val;
     }
     if (val > myInfo.newMaxVal) myInfo.newMaxVal=val;
     if (val < myInfo.newMinVal) myInfo.newMinVal=val;
     return val;
  }

  void drawDataPoint(Graphics g,Rectangle r, double val,double posx,double ActPos,double SizeX, boolean DrawMarker)
  {
     if (normalize)
     {
        val=(myInfo.ValScale*(val-myInfo.MinVal)-mindispl)/(maxdispl-mindispl);
     }
     else
        val=(myInfo.ValScale*val-mindispl)/(maxdispl-mindispl);
     int lxnew = (int) (10.0+ posx * (r.width-20.0) / (SizeX - 1.0));
     int lynew = r.height - 10 - (int) ((r.height - 20)*(val));
     if (PointNr > 1)  // Omitt the first point
        g.drawLine(lxold,lyold,lxnew,lynew);
     if (posx == ActPos)
        {
	g.fillOval(lxnew-4,lynew-4,9,9);
        }
     if (DrawMarker)
	g.drawOval(lxnew-5,lynew-5,11,11);
     lxold=lxnew;lyold=lynew;
  }
  
  public void plot(Graphics g) {  // Plots one or multiple spectral graphs
    Rectangle r = getBounds();
    // Color bg = getBackground();
    g.setColor(Color.black);
    g.fillRect(0,0,r.width-1,r.height-1);
    g.setColor(Color.white);
    
    int ActPos=0;
    int NumPts=0;
    int Size = data3d.Elements;
    if (Size == 1) Size = 2;
    g.drawLine(10,r.height-10,r.width-10,r.height-10);
    ActPos = (int) data3d.GetActiveElement();
    int pos =10 + (int)(ActPos*(r.width-20)/(Size-1));
    g.drawLine(pos,0,pos,r.height-1);
    double val;
    // double MaxVal=1.0,MinVal=0.0,ValScale=1.0;

    switch (PlotMode)
    {
        case 1:
        TextToDisplay = "Intensity(Elements)"; break;
        case 2:
        TextToDisplay = "Mean(Marker Trace)";break;
        case 3:
        TextToDisplay = "Max(Marker Trace)";break;
        case 4:
        TextToDisplay = "Min(Marker Trace)";break;
        case 5:
        TextToDisplay = "[Mean-Min](Marker Trace)";break;
    }

    if (data3d.GetProjectionMode(2))
    {
         if (data3d.GetMIPMode(2))
            TextToDisplay+=", Max. Proj.";
         else
            TextToDisplay+=", Avg. Proj.";
    }

    if (normalize && ! data3d.GetProjectionMode(2))
        TextToDisplay += ", normalized";
    if (logmode)
        TextToDisplay += ", logmode";

    if (! normalize)
    {
    myInfo.MinVal = 0; myInfo.MaxVal = 1.0;
    myInfo.ValScale = 1.0;
    }
    
    double printval=0.0;
    String XTitle="";
    if (PlotMode == 1)         // multispectral plot
    {
        XTitle = "Elements";
        if (normalize)
        {
        NumPts=InitDataPoints(-1);
        for (int e=0;e<NumPts;e++)
            val = GetNextNormDataPoint(-1); 
        myInfo.MinVal = myInfo.newMinVal; myInfo.MaxVal = myInfo.newMaxVal;
        myInfo.ValScale = 1.0/(myInfo.MaxVal-myInfo.MinVal);
        }
        g.setColor(Color.white);
        NumPts=InitDataPoints(-1);
        for (int e=0;e<NumPts;e++)
        {
            val = GetNextNormDataPoint(-1);
            drawDataPoint(g,r,val,e,ActPos,Size,false);
            if (ActPos == e)
                printval = data3d.ValueAt((int) (c2.PositionValue),(int) (c3.PositionValue),(int) (c1.PositionValue),e);
        }

        for (int l=0;l<data3d.NumMarkerLists();l++)
         if (data3d.NumMarkers(l) > 0)
            {
            if (normalize)
            {
            NumPts=InitDataPoints(-1);  // return number of elements
            for (int e=0;e<NumPts;e++)
              val = GetNextNormDataPoint(l);  // get the displayed marker in the list
            myInfo.MinVal = myInfo.newMinVal; myInfo.MaxVal = myInfo.newMaxVal;
            myInfo.ValScale = 1.0/(myInfo.MaxVal-myInfo.MinVal);   
            }

            Color MarkerColor=new Color(data3d.GetPoint(0,l).mycolor);
            g.setColor(MarkerColor);
            NumPts=InitDataPoints(-1);  // return number of elements
            for (int e=0;e<NumPts;e++)
                {
                val = GetNextNormDataPoint(l);
                drawDataPoint(g,r,val,e,ActPos,Size,false);
                }
            }
      }  // PlotMOde == 1
    else  // Marker Plot modes
    {
        XTitle=data3d.TrackDirections[data3d.TrackDirection];
        int px = (int) (c2.PositionValue); // Get the absolute z-position
        int py = (int) (c3.PositionValue); // Get the absolute z-position
        int pz = (int) (c1.PositionValue); // Get the absolute z-position
        int element = (int) data3d.ActiveElement; // Get the active time
        int time = (int) data3d.ActiveTime; // Get the active time
        int ActPosition[]= {px,py,pz,element,time};
        
        //double xpos;
        for (int l=0;l<data3d.NumMarkerLists();l++)
         if (data3d.NumMarkers(l) > 0) //  && (data3d.ShowAllLists || l == data3d.ActiveMarkerListPos()))
            {
            NumPts=InitDataPoints(l);  // return number list entries
            for (int e=0;e<NumPts;e++)
            {
              val = GetNextNormDataPoint(l);  // get the displayed marker in the list
              data3d.CheckActiveMarker(l,e,px,py,pz,element,time);
            }
            if (normalize)
            {
                myInfo.MinVal = myInfo.newMinVal; myInfo.MaxVal = myInfo.newMaxVal;
                myInfo.ValScale = 1.0/(myInfo.MaxVal-myInfo.MinVal);   
            }

            Color MarkerColor=new Color(data3d.GetPoint(0,l).mycolor);
            g.setColor(MarkerColor);
            NumPts=InitDataPoints(l);  // return number of elements
            for (int e=0;e<NumPts;e++)
                {
                val = GetNextNormDataPoint(l);

                boolean MDispl=data3d.MarkerDisplayed(data3d.GetPoint(e,l),ActPosition);
                double posx=data3d.GetPoint(e,l).coord[data3d.TrackDirection];
                double APos=data3d.GetPoint(data3d.ActiveMarkerPos(),l).coord[data3d.TrackDirection];
                if (l == data3d.ActiveMarkerListPos() && e == data3d.ActiveMarkerPos())
                {
                    g.setColor(Color.white);
                    printval = pval;  // save for later use to print
                }
                else
                {
                    g.setColor(MarkerColor);
                    APos=-1e10;
                }

                drawDataPoint(g,r,val,
                    posx,  // posx
                    APos,  // ActPos
                    data3d.GetSize(data3d.TrackDirection), 
                    MDispl);  // show the marker, if displayed normally
                }
            }
    }
    g.setColor(Color.white);
    g.drawString(TextToDisplay,12,12); //  r.width - TextToDisplay.length() * 9 , 12); 
    g.drawString(XTitle,r.width - XTitle.length() * 9,r.height - 15);

    NumberFormat nf2 = java.text.NumberFormat.getNumberInstance(Locale.US);
    nf2.setMaximumFractionDigits(4);
    nf2.setGroupingUsed(false);

    g.drawString(nf2.format(printval),12,r.height - 20); 
  }

    public void SetTrackDirVal(int posx)
    {
        if (posx >= data3d.GetSize(data3d.TrackDirection)) posx=data3d.GetSize(data3d.TrackDirection)-1;
        if (posx < 0) posx=0;
        switch (data3d.TrackDirection) 
                {
                case 0:
                    c3.PositionValue=posx; break;
                case 1:
                    c2.PositionValue=posx; break;
                case 2:
                    c1.PositionValue=posx; break;
                case 3:
                    data3d.setElement(posx);
                case 4:
                    data3d.setTime(posx);
                }
    }
    public int GetTrackDirVal()
    {
        switch (data3d.TrackDirection) 
                {
                case 0:
                    return (int) c3.PositionValue; 
                case 1:
                    return (int) c2.PositionValue; 
                case 2:
                    return (int) c1.PositionValue; 
                case 3:
                    return data3d.ActiveElement;
                case 4:
                    return data3d.ActiveTime;
                }
        return 0;
    }

    public void update(Graphics g) { // eliminate flickering
	paint(g);
    }

    public void mouseEntered(MouseEvent e) {
	requestFocus();
    }
    public void mouseClicked(MouseEvent e) { }
    public void mouseExited(MouseEvent e) {
    }
    public void mousePressed(MouseEvent e) {
        //if (c1.applet instanceof View5D_)  // ImageJ
        //	((View5D_) c1.applet).setMenuBar(c1.myPanel.MyMenu);

        if (e.isPopupTrigger())
        {
            MyPopupMenu.show(this,e.getX(),e.getY());
            return;
        }

	Rectangle r = getBounds();
	int xprev = e.getX();
	// int yprev = e.getY();
        if (PlotMode <= 1)         // multispectral plot
            {
            int elements = data3d.GetNumElements();
            int element = xprev*elements/r.width;
        	c1.myPanel.RememberOffset();
            data3d.setElement(element);
        	c1.myPanel.AdjustOffset();
            }
        else 
            if (PlotMode > 1)
            {
            int posx = (int) (xprev*data3d.GetSize(data3d.TrackDirection)/r.width+0.5);
        	c1.myPanel.RememberOffset();
            SetTrackDirVal(posx); // updates the display to the appropriate position along the track direction
        	c1.myPanel.AdjustOffset();
            }
	// System.out.println("Switching to "+element);
	//if (false) // (yprev > r.height/2)  // lower half  -> change also the RGB assignment
	//    {
	//	int MyMask=e.getModifiers();
	//	if (MyMask == InputEvent.BUTTON1_MASK)
	//	    data3d.MarkChannel(0);
	//	if (MyMask == InputEvent.BUTTON2_MASK)
	//	    data3d.MarkChannel(1);
	//	if (MyMask == InputEvent.BUTTON3_MASK)
	//	    data3d.MarkChannel(2);
	//    }
	c1.UpdateAll();
    }
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger())
        {
            MyPopupMenu.show(this,e.getX(),e.getY());
            return;
        }
    }
    public void keyPressed(KeyEvent e) {
	char myChar = e.getKeyChar();
    if (PlotMode <= 1)
        {
        if(KeyEvent.VK_RIGHT == e.getKeyCode())
            {
        	c1.myPanel.RememberOffset();
            c1.my3ddata.advanceElement(1);
        	c1.myPanel.AdjustOffset();
            c1.UpdateAll();
            return;
            }
        if(KeyEvent.VK_LEFT == e.getKeyCode())
            {
        	c1.myPanel.RememberOffset();
            c1.my3ddata.advanceElement(-1);
        	c1.myPanel.AdjustOffset();
            c1.UpdateAll();
            return;
            }
        }
        else //           if (PlotMode > 1)
            if ((KeyEvent.VK_RIGHT == e.getKeyCode()) || (KeyEvent.VK_LEFT == e.getKeyCode()))
            {
            int posx = GetTrackDirVal();
            if(KeyEvent.VK_RIGHT == e.getKeyCode())
                posx++;
            if(KeyEvent.VK_LEFT == e.getKeyCode())
                posx--;
            SetTrackDirVal(posx);
            
            c1.UpdateAll();
            return;
            }

        ProcessKey(myChar);
    }
    
public void ProcessKey(char myChar) {
	switch (myChar)
	    {
            case 's': 
                c1.myPanel.label.remove(this); // if still visible
                GridLayout myLayout=new GridLayout(1,2);
                c1.myPanel.label.setLayout(myLayout);
                c1.myPanel.label.doLayout(); // if still visible

                AlternateViewer xx=new AlternateViewer(c1.applet);  // clone the data and open a new viewer
                xx.AssignPixelDisplay(this);
                // c1.myPanel.label.MyText.doLayout(); // if still visible
                return;
	    case 'q':  // Toggle Plot display
		PlotMode = (PlotMode+1) % 6;
		repaint();
		break;
	    case 'n':  // Toggle Spectral Plot display
		normalize = ! normalize;
		repaint();
		break;
    	    case 'N':  // Pops up a "Units" dialog
		data3d.ValueUnitsDialog();
        	data3d.InvalidateProjs(-1);  // all projections are invalid
		c1.label.CoordsChanged();
		c1.UpdateAll();
		break;
	    case 'm':  // Prints the distance list
		if (data3d.markerOutfilename != null)
	   		data3d.SaveMarkers();  
                c1.label.PrintPointList();
		// repaint();
		break;
	    case 'M':  // Prints the distance list
                data3d.MarkerDialog();  // Togles whether markers are succed to maximum of not
                // data3d.ToggleMarkerToMax(-1);  // Togles whether markers are succed to maximum of not
		// repaint();
		break;
            case 'O':
                logmode = ! logmode;
                /*for (int el=0;el<data3d.Elements;el++)
                    if (logmode)
                        data3d.ToggleLog(el,1);
                    else
                        data3d.ToggleLog(el,0);*/
		repaint();
                break;
	    case '1':  
                if (mindispl < maxdispl - 0.01)
                    mindispl += 0.01;
		repaint();
		break;
	    case '2':  
                mindispl -= 0.01;
		repaint();
		break;
	    case '3':  
                maxdispl += 0.02;
		repaint();
		break;
	    case '4':  
                if (mindispl < maxdispl - 0.02)
                    maxdispl -= 0.02;
		repaint();
		break;
	    case '5':  
                if (mindispl < maxdispl - 0.001)
                    mindispl += 0.001;
		repaint();
		break;
	    case '6':  
                mindispl -= 0.001;
		repaint();
		break;
	    case '7':  
                maxdispl += 0.002;
		repaint();
		break;
	    case '8':  
                if (mindispl < maxdispl - 0.002)
                    maxdispl -= 0.002;
		repaint();
		break;
            case 'i':  
                maxdispl = 1.0;
		mindispl = 0.0;
		repaint();
		break;
            case 'X':  // export to ImageJ
        	if (c1.applet instanceof View5D_)
                data3d.Export(-1,-1);   // a new stack in ImageJ will be generated
                break;
	    default:
		c1.ProcessKey(myChar);  // All keys are processed by XY view
	    }
    }

    public void keyTyped(KeyEvent e) {
    }
    public void keyReleased(KeyEvent e) {
    }
}
