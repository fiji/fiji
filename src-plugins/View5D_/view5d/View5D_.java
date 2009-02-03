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
//import java.lang.*;
// import java.lang.Number.*;
import java.net.*;
import java.awt.image.*;
// import java.awt.image.ColorModel.*;
import java.awt.event.*;
import java.applet.Applet;
import java.awt.Graphics;
// import java.awt.color.*;
import java.awt.*;
import java.util.*;
import java.text.*;
import ij.plugin.frame.PlugInFrame;
import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;

// Tagged component classes, taken from my JFlow project and simplified
class TaggedComponent extends Panel { // a general superclass if called with component==null it can be used as text 
    static final long serialVersionUID = 1;
    GridBagLayout gridbag;
    GridBagConstraints c;
    String name;
    String mytag;
    Label mylabel;
    Component mycomp;
    public TaggedComponent(String tag, String label, Component acomp)
    {
	super();
	name="tcomp";
	mytag = tag;
	mylabel = new Label(label);
	mycomp = acomp;

        gridbag = new GridBagLayout();
	c = new GridBagConstraints();
	c.fill = GridBagConstraints.HORIZONTAL;
	c.gridwidth = GridBagConstraints.REMAINDER;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.weightx = 0.0;  // each component in a row !
	// setFont(new Font("Helvetica", Font.PLAIN, 14));
	setLayout(gridbag);

	if (! label.equals("none"))
	    add(mylabel);
	if (mycomp != null)
	    add(mycomp);
    }
    public void AddComponent(Component acomp)
    {
	mycomp = acomp;
	if (mycomp != null)
	    add(mycomp);
    }
    public void setValue(String val)  // converts value to component attributes
    {
	if (val.equals("none"))
	    return;
	System.out.println("Error Tagged Component setValue called\n"); 
    }
    public Object getValue() {        // converts component attributes to value
	// System.out.println("Error Tagged Component getValue called\n"); 
	return null;
    }
    public String getDescription() {
	String ret=name+" ";
	if(mytag.equals(""))
	    ret+="none ";
	else
	    ret+="\""+mytag+"\" ";
	if(mylabel.equals(""))
	   ret+="none ";
	else
	    ret+="\""+mylabel.getText()+"\" ";
	/*String tmp=getValue();
	if(tmp.equals(""))
	   ret+="none";
	else
	    ret+="\""+tmp+"\"";
         */
	return ret;
    }
}

class TaggedText extends TaggedComponent {  // this is a component with a tag, capable of generating a part of a call
	static final long serialVersionUID = 1;
	public TaggedText(String tag, String label, String value) {
	    super(tag,label,null); 
	    name="text";
	    if ((value != null) && !value.equals("none") && !value.equals(""))
		{
		    AddComponent(new TextField(20));
		    ((TextField) mycomp).setText(value);
		}
	}
    public void setValue(String val)  // converts value to component attributes
    {
	if (val.equals("none"))
	    val="";
	if (mycomp != null)
	    ((TextField) mycomp).setText(val);
    }
    public String getTextValue() {        // converts component attributes to value
	String ret="";
	if (mycomp != null)
	    ret= ((TextField) mycomp).getText();
	return ret;
    }    
}

class TaggedMessage extends TaggedText {  // this is a component with a tag, capable of generating a part of a call
    static final long serialVersionUID = 1;
	public TaggedMessage(String tag, String value) {
	    super(tag,value,""); 
	    name="message";
	}
    public String getTextValue() {        // converts component attributes to value
	return super.getTextValue();
    }    
}


class TaggedDouble extends TaggedText {  // this is a component with a tag, capable of generating a part of a call
    static final long serialVersionUID = 1;
    double prevValue;
    public TaggedDouble(String tag, String label, double value) {
	    super(tag,label,Double.toString(value));
            prevValue = value;
	    name = "float";
	}
   public void setValue(double val)  // converts value to component attributes
    {
        prevValue = val;
        String valtext=Double.toString(val);
	super.setValue(valtext);
    }
   
    public double getDoubleValue() {        // converts component attributes to value
        String valtext=super.getTextValue();
        double val=0;
        try{ 
        val=Double.valueOf(valtext).doubleValue();
        }
	catch(Exception e)
	      {
                  System.out.println("Floating point number is not parsable reverting to old value\n");
		  e.printStackTrace();
                  val = prevValue;
	      }
        return val;
    }
}


class TaggedCheck extends TaggedComponent {  // this is a component with a tag, capable of generating a part of a call
    static final long serialVersionUID = 1;
    public TaggedCheck(String tag, String label, boolean value) {
	super(tag,"",new Checkbox(label,value));
	name = "check";
	setValue(value);
    }
   public void setValue(boolean bval)  // converts value to component attributes
    {
	((Checkbox) mycomp).setState(bval);
    }
    public boolean getBoolValue() {        // converts component attributes to value
	return (((Checkbox) mycomp).getState());
    }
  }

class TaggedChoice extends TaggedComponent {  // this is a component with a tag, capable of generating a part of a call
    static final long serialVersionUID = 1;
    public TaggedChoice (String tag, String label, String[] items, String value) {
   	super(tag,label,new Choice());
        if (items == null) return;
        Choice thisChoice = ((Choice) mycomp);
        //thisChoice.addKeyListener(this);
        //thisChoice.addItemListener(this);
        for (int i=0; i<items.length; i++)
                        thisChoice.addItem(items[i]);
	name = "check";
	setValue(value);
    }
  
   public void setValue(String val)  // converts value to component attributes
    {
	((Choice) mycomp).select(val);
    }
    public int getChoiceValue() {        // converts component attributes to value
	return (((Choice) mycomp).getSelectedIndex());
    }
  }

class ANGenericDialog extends GenericDialog implements ActionListener, FocusListener, ItemListener {             // derived from the ImageJ class or from the emulation AGernericDialog
    static final long serialVersionUID = 1;

    private Button in,out;
    private TextField infile,outfile;
    //GridBagLayout gridbag;
    
    public ANGenericDialog(String title) {super(title);}
    public ANGenericDialog(String title, Frame parent) {super(title,parent);}
      
    // Window Listener Functions
    public void windowClosing (WindowEvent e) {
	setVisible(false);
	dispose();
    }
    public void windowClosed (WindowEvent e) { setVisible(false);}
    public void windowOpened (WindowEvent e) { }
    
    public void addNumericFields(String label, double value, int precision, int howmany) {
        //GridBagLayout gridbag = new GridBagLayout();
        //GridBagConstraints c = new GridBagConstraints();
        //TextField anotherNumber=new TextField(20);
        //anotherNumber.setText(value);
        //add(anotherNumber);

        //Component compold=getComponent(getComponentCount()-1);
        addNumericField(label,value,precision);
        //Component compnew=getComponent(getComponentCount()-1);
        //compnew.invalidate();
        //compnew.setLocation(compold.getX()+compold.getBounds().width+10,compold.getY()+compold.getBounds().height+10);
        //compnew.doLayout();        
    }
    public void addInFile(String label, String value) {
        //addStringField(label,value);
        Panel inpan = new Panel();
    	inpan.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
	in = new Button(label);
	in.addActionListener(this);
	inpan.add(in);
	//GridBagConstraints c = new GridBagConstraints();
	//gridbag.setConstraints(inpan, c);
	add(inpan);
        infile=new TextField(20);
        infile.setText(value);
        add(infile);
    }
    public void addOutFile(String label, String value) {
        //addStringField(label,value);
        Panel outpan = new Panel();
    	outpan.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
	out = new Button(label);
	out.addActionListener(this);
	outpan.add(out);
	//GridBagConstraints c = new GridBagConstraints();
	//gridbag.setConstraints(outpan, c);
	add(outpan);
        outfile=new TextField(20);
        outfile.setText(value);
        add(outfile);
    }

    public String getInFile() {
        //String tmp=getNextString();
        return infile.getText();
    }
    public String getOutFile() {
        //String tmp=getNextString();
        return outfile.getText();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource()==in)
        {
            Frame aframe=WindowManager.getCurrentImage()!=null? (Frame)WindowManager.getCurrentImage().getWindow():IJ.getInstance()!=null?IJ.getInstance():new Frame();
            FileDialog filediag = new FileDialog(aframe,"Input File",FileDialog.LOAD);
            filediag.setFile(infile.getText());
            //filediag.show();
            filediag.setVisible(true);
            if (filediag.getFile() != null)
                infile.setText(filediag.getDirectory()+filediag.getFile());
            return;
        }
        if (e.getSource()==out)
        {
            Frame aframe=WindowManager.getCurrentImage()!=null? (Frame)WindowManager.getCurrentImage().getWindow():IJ.getInstance()!=null?IJ.getInstance():new Frame();
            FileDialog filediag = new FileDialog(aframe,"Output File",FileDialog.SAVE);
            filediag.setFile(outfile.getText());
            //filediag.show();
            filediag.setVisible(true);
            if (filediag.getFile() != null)
                outfile.setText(filediag.getDirectory()+filediag.getFile());
            return;
        }
        super.actionPerformed(e);
	}
}

class AGenericDialog extends Dialog implements WindowListener,ActionListener, FocusListener, ItemListener {  // an easy class for generating user interaction
    static final long serialVersionUID = 1;
    GridBagLayout gridbag;
    private GridBagConstraints c;
    int Components,posS=0,posC=0,posN=0,posCh=0;
    private int y;
    private Button cancel, okay;

    boolean canceled=false;
    boolean firstNumericField=true;
 
    public AGenericDialog(String title) {
		this(title, WindowManager.getCurrentImage()!=null?
			(Frame)WindowManager.getCurrentImage().getWindow():IJ.getInstance()!=null?IJ.getInstance():new Frame());
	}

    /** Creates a new GenericDialog using the specified title and parent frame. */
    public AGenericDialog(String title, Frame parent) {
		super(parent==null?new Frame():parent, title, true);
		gridbag = new GridBagLayout();
		c = new GridBagConstraints();
		setLayout(gridbag);
                addWindowListener(this);
    }
    
    
    public void showDialog() {
        Panel buttons = new Panel();
    	buttons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
	cancel = new Button("Cancel");
	cancel.addActionListener(this);
	okay = new Button("  OK  ");
	okay.addActionListener(this);
	buttons.add(okay);
	buttons.add(cancel);
	c.gridx = 0; c.gridy = y;
	c.anchor = GridBagConstraints.EAST;
	c.gridwidth = 2;
	c.insets = new Insets(15, 0, 0, 0);
	gridbag.setConstraints(buttons, c);
	add(buttons);
        // setVisible(true);
       	// setResizable(false);  // really ?
	pack();
	GUI.center(this);
	//show();
    setVisible(true);
        // toFront();
        // dispose();            // really ?
    }

    public boolean wasCanceled() {
	return canceled;
    }
    
    public void addStringField(String label, String value) {
        TaggedComponent Comp=new TaggedText(label,label,value);
	c.gridx = 0; c.gridy = y;
	c.gridwidth = 2;
	c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(Comp,c);
	add(Comp);
        Components +=1;y++;
    }
    public void addCheckbox(String label, boolean value) {
	TaggedComponent Comp=new TaggedCheck(label,label,value);
	c.gridx = 0; c.gridy = y;
	c.gridwidth = 2;
	c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(Comp,c);
	add(Comp);
        Components +=1;y++;
    }
    
    public void addChoice(String label, String[] items, String defaultItem) {
	TaggedComponent Comp=new TaggedChoice(label,label,items, defaultItem);
        c.gridx = 0; c.gridy = y;
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = 1;
        gridbag.setConstraints(Comp,c);
	add(Comp);
        Components +=1;y++;
    }

    
    public void addMessage(String text) {
        Component theLabel;
    	if (text.indexOf('\n')>=0)
		theLabel = new MultiLineLabel(text);
	else
		theLabel = new Label(text);
	//theLabel.addKeyListener(this);
	c.gridx = 0; c.gridy = y;
	c.gridwidth = 2;
	c.anchor = GridBagConstraints.WEST;
	c.insets = new Insets(text.equals("")?0:10, 20, 0, 0);
	gridbag.setConstraints(theLabel, c);
	add(theLabel);
        Components++;
	y++;
    }    
    
    public void addNumericField(String label, double value, int precision) {
        TaggedDouble Comp=new TaggedDouble(label,label,value);
        c.gridy = y;
	c.anchor = GridBagConstraints.EAST;
	c.gridwidth = 3;
        // int veccomponents=1;
        
        c.gridx=0; // will start with 0 then
        y++;

        if (firstNumericField)
                c.insets = new Insets(3, 0, 3, 0);  // top, left, bottom, right distance
        else
                c.insets = new Insets(0, 0, 3, 0);
        if (firstNumericField) ((TextField) Comp.mycomp).selectAll();

        firstNumericField = false;
        
        gridbag.setConstraints(Comp,c);
        add(Comp);
        Components +=1;
    }

    public String getNextString() {
	int max= getComponentCount();
	while(posS<max)
	    {
                TaggedText dummy = new TaggedText("","","");
                Component comp=getComponent(posS);
		if (dummy.getClass().isInstance(comp) && comp.getClass().isInstance(dummy))
                {
                    //System.out.println("Retrieved String "+posS+": "+((TaggedComponent) comp).getDescription()+"\n");
                    posS++;
                    return ((TaggedText) comp).getTextValue();
                }
             posS++;
	    }
        System.out.println("String component not found error!\n");
	return "";
    }
    public double getNextNumber() {
	int max= getComponentCount();
	while(posN<max)
	    {
                TaggedDouble dummy = new TaggedDouble("","",0);
                Component comp=getComponent(posN);
		if (dummy.getClass().isInstance(comp) && comp.getClass().isInstance(dummy))
                {
                    //System.out.println("Retrieved Number "+posN+": "+((TaggedComponent) comp).getDescription()+"\n");
                    posN++;
                    return ((TaggedDouble) comp).getDoubleValue();
                }
                posN++;
	    }
        System.out.println("Number component not found error!\n");
	return 0;
    }
    public boolean getNextBoolean() {
	int max= getComponentCount();
	while(posC<max)
	    {
                TaggedCheck dummy = new TaggedCheck("","",true);
                Component comp=getComponent(posC);
		if (dummy.getClass().isInstance(comp) && comp.getClass().isInstance(dummy))
                {
                    //System.out.println("Retrieved Checkbox "+posC+": "+((TaggedComponent) comp).getDescription()+"\n");
                    posC++;
                    return ((TaggedCheck) comp).getBoolValue();
                }
                posC++;
	    }
        System.out.println("Check component not found error!\n");
	return false;
    }

    /* Returns the index of the selected item in the next popup menu. */
    public int getNextChoiceIndex() {
	int max= getComponentCount();
	while(posCh<max)
	    {
                TaggedChoice dummy = new TaggedChoice("","",null,"");
                Component comp=getComponent(posCh);
		if (dummy.getClass().isInstance(comp) && comp.getClass().isInstance(dummy))
                {
                    //System.out.println("Retrieved Choice "+posCh+": "+((TaggedComponent) comp).getDescription()+"\n");
                    posCh++;
                    return ((TaggedChoice) comp).getChoiceValue();
                }
                posCh++;
	    }
        System.out.println("Check component not found error!\n");
	return 0;
    }

    // Window Listener Functions
    public void windowClosing (WindowEvent e) {
	setVisible(false);
	dispose();
    System.out.println("Generic Dialog closing!\n");
    }
    
    public void windowClosed (WindowEvent e) { }
    public void windowOpened (WindowEvent e) {canceled=true;}
    public void windowIconified (WindowEvent e) { }
    public void windowDeiconified (WindowEvent e) { }
    public void windowActivated (WindowEvent e) { }
    public void windowDeactivated (WindowEvent e) { }
        
    public void itemStateChanged(ItemEvent e) { }

    public void focusGained(FocusEvent e) {
                Component c = e.getComponent();
                if (c instanceof TextField)
                        ((TextField)c).selectAll();
        }

    public void focusLost(FocusEvent e) {
                Component c = e.getComponent();
                if (c instanceof TextField)
                        ((TextField)c).select(0,0);
    }


    public void actionPerformed(ActionEvent e) {
		canceled = (e.getSource()==cancel);
		setVisible(false);
		dispose();
	}
}


/// ROI abstract base class
abstract class ROI extends Object {
    ROI ()   {}
    abstract boolean InROIRange(int x,int y,int z);
    abstract double GetROISize(int dim);
}

class RectROI extends ROI {
    int ProjMin[], ProjMax[];
     RectROI() {
         super();
     }
     
    double GetROISize(int dim) {
        return (ProjMax[dim] - ProjMin[dim])+1;  // returns number of pixels including both borders
     }

    public Rectangle GetSqrROI(int dim) {
        if (ProjMin == null)
            return null;
        if (dim == 0)
            return new Rectangle(ProjMin[2],ProjMin[1],
            (int) GetROISize(2)-1,(int) GetROISize(1)-1);
        if (dim == 1)
            return new Rectangle(ProjMin[0],ProjMin[2],
            (int) GetROISize(0)-1,(int) GetROISize(2)-1);
        if (dim == 2)
            return new Rectangle(ProjMin[0],ProjMin[1],
            (int) GetROISize(0)-1,(int) GetROISize(1)-1);
        return null;
    }
    

    void TakeSqrROIs(int Pmin[], int Pmax[]) {
        if (ProjMin == null)
        {
            ProjMin = new int[3];
            ProjMax = new int[3];
        }
        for (int d=0;d<3;d++)
        {
            ProjMin[d] = Pmin[d];ProjMax[d] = Pmax[d];
        }
    }

    public void UpdateSqrROI(int ROIX,int ROIY, int ROIXe, int ROIYe,int dir)
    {
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
    }

    boolean InROIRange(int x,int y,int z) {
         if  (ProjMin == null)
             return true;
         if (x < ProjMin[0])
             return false;
         if (y < ProjMin[1])
             return false;
         if (z < ProjMin[2])
             return false;
         if (x > ProjMax[0])
             return false;
         if (y > ProjMax[1])
             return false;
         if (z > ProjMax[2])
             return false;
         return true;
     }
}

// A ROI class based on three Polygons whos orthogonal overlap is the 3D ROI
class PolyROI extends ROI {
    Polygon ROIPolygons[];         // 3 polygons for sectioning

     PolyROI() {
        super();
        ROIPolygons = new Polygon[3];
        ROIPolygons[0] = null;ROIPolygons[1] = null;ROIPolygons[2] = null;
        }

     double GetROISize(int dim) {
        return 0;
     }
    
    void TakePolyROIs(Polygon PS[]) {
        ROIPolygons = PS;
    }
        
    boolean InROIRange(int x,int y, int z) {
        // The problem with the "contains" method is that the right and lower pixels are not inside, if equal to boundary of Polygon
            if (ROIPolygons[2] != null)
                if (! ROIPolygons[2].intersects(x-0.05,y-0.05,0.1,0.1)) // contains((double)x,(double)y))
                    return false;
            if (ROIPolygons[1] != null)
                if (! ROIPolygons[1].intersects(x-0.05,z-0.05,0.1,0.1)) // contains(x,z))
                    return false;
            if (ROIPolygons[0] != null)
                if (! ROIPolygons[0].intersects(z-0.05,y-0.05,0.1,0.1)) // contains(z,y))
                    return false;
            return true;
        }
}

// A RIO class based on separating planes (in 3D, any orientation)
class PlaneROI extends ROI {
    Vector PlanesS[], PlanesD[];  // Starting vectors and direction vectors of planes
    
    PlaneROI() {
        PlanesS = new Vector[3];
        PlanesD = new Vector[3];
    }

    double GetROISize(int dim) {
        return 0;
     }

    void TakePlaneROIs(Vector PS[],Vector PD[]) {
        PlanesS = PS;
        PlanesD = PD;
    }

    boolean InROIRange(int x,int y, int z) {
        int Sx,Sy,Sz;
        double Dx,Dy,Dz;
        if (PlanesS == null || PlanesD == null )
            return false;
        for (int i=0;i<PlanesS[0].size();i++)
            {
            Sx= x-((Integer) PlanesS[0].elementAt(i)).intValue();
            Sy= y-((Integer) PlanesS[1].elementAt(i)).intValue();
            Sz= z-((Integer) PlanesS[2].elementAt(i)).intValue();
            Dx=((Double) PlanesD[0].elementAt(i)).doubleValue();
            Dy=((Double) PlanesD[1].elementAt(i)).doubleValue();
            Dz=((Double) PlanesD[2].elementAt(i)).doubleValue();
            if (Dx*Sx+Dy*Sy+Dz*Sz < 0.0)
                return false;
            }
        return true;
        } 
}

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


abstract class AnElement extends Object {
    // Different Data Types, tags listed below
    static int InvalidType=-10;
    static int ByteType = 0; static int IntegerType = 1; static int FloatType = 2;static int DoubleType = 3;static int ComplexType = 4;static int ShortType=5;static int LongType=6;
    static int NumTypes=7;
    static String TypeNames[]={"Byte","Integer","Float","Double","Complex","Short","Long"};  // The last types are converted to integer
    static String UTypeNames[]={"Unsigned Byte","Unsigned Integer","Float","Double","Complex","Unsigned Short","Unsigned Long"};  // The last types are converted to integer
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

class ByteElement extends AnElement {
  byte [] myData;        // holds the 3D byte data
  int SizeXY;
  
  ByteElement(int SX, int SY, int SZ) {
      super(SX,SY,SZ,256.0);
      myData = new byte[Sizes[0]*Sizes[1]*Sizes[2]];
      SizeXY = Sizes[0]*Sizes[1];
      DataType = ByteType; 
  }
  
  void Clear() {
    for (int i = 0; i < Sizes[0]*Sizes[1]*Sizes[2];i++) 
            myData[i] = 0;
    } 

  void DeleteData() {
            myData = null;
  }
  
  int GetStdByteNum() {return 1;}

  void SetValueAt(int x, int y, int z, double val)
  {
      if (val < 0.0) val = 0.0;
      if (val > 255) val = 255;
      myData[x+Sizes[0]*y+SizeXY*z]= (byte) val;
  }
  
  int GetIntValueAt(int x, int y, int z)  // scaled to 16 bit integer (for pseudocolor display)
  {
      int val = myData[x+Sizes[0]*y+SizeXY*z] & 0xff;
      // if (val < 0) val += 256;
      return (int)((val - shift) * scaleI);
  }
  int GetByteValueAt(int x, int y, int z)
  {
      int val = myData[x+Sizes[0]*y+SizeXY*z] & 0xff;
      // if (val < 0) val += 256;
      return (int) ((val-shift) * scaleB);
  }

  double GetRawValueAt(int x, int y, int z)
  {
      double val = myData[x+Sizes[0]*y+SizeXY*z] & 0xff;
      // if (val < 0 ) val += 256;
      return val;
  }

  double GetValueAt(int x, int y, int z)
  {
      int val = myData[x+Sizes[0]*y+SizeXY*z] & 0xff;
      // if (val < 0) val += 256;
      return val*ScaleV+OffsetV;
  }
        
  void ConvertSliceFromSimilar(int myslice, int bufslice, Object Ibuffer, int mstep, int moff) {
    // System.out.println("Byte Converting "+SizeXY+"\n");
    byte [] mbuffer = (byte []) Ibuffer;
    for (int i=0;i<SizeXY;i+=mstep)
        myData[i+Sizes[0]*Sizes[1]*myslice] = mbuffer[bufslice*SizeXY+i+moff]; 
    }
  
  void ConvertSliceFromByte(int myslice, int bufslice, byte [] Ibuffer, int mstep, int moff)
    {
      for (int i=0;i<SizeXY;i+=mstep)
        myData[i+Sizes[0]*Sizes[1]*myslice] = Ibuffer[bufslice*SizeXY+i+moff]; 
    }

  void ConvertSliceFromRGB(int myslice, int bufslice, int [] Ibuffer, int mstep, int moff, int suboff)  // suboff defines which byte to use
    {
      int bitshift=suboff*8;
      for (int i=0;i<SizeXY;i+=mstep)
          myData[i+Sizes[0]*Sizes[1]*myslice] = ((byte) ((Ibuffer[bufslice*SizeXY+i+moff] >> bitshift) & 0xff)) ; 
    }
  
    void CopySliceToSimilar(int myslice, Object buffer)  
    {
      byte [] mbuffer = (byte[]) buffer;
      for (int i=0;i<SizeXY;i++)
          mbuffer[i]=myData[i+Sizes[0]*Sizes[1]*myslice]; 
    }
  }

class ShortElement extends AnElement {
  short [] myData;        // holds the 3D 16 bit data
  int NumBytes=2;
  int SizeXY;
  
  ShortElement(int SX, int SY, int SZ) {
      super(SX,SY,SZ,256.0);
      myData = new short[Sizes[0]*Sizes[1]*Sizes[2]];
      SizeXY = Sizes[0]*Sizes[1];
      DataType = ShortType; 
      NumBytes = 2; 
  }
  
  void Clear() {
    for (int i = 0; i < Sizes[0]*Sizes[1]*Sizes[2];i++) 
            myData[i] = 0;
    } 

  void DeleteData() {
            myData = null;
  }
  
  int GetStdByteNum() {return 1;}

  void SetValueAt(int x, int y, int z, double val)
  {
      if (val < 0.0) val = 0.0;
      if (val > 255) val = 255;
      myData[x+Sizes[0]*y+SizeXY*z]= (short) val;
  }
  
  int GetIntValueAt(int x, int y, int z)  // scaled to 16 bit integer (for pseudocolor display)
  {
      int val = myData[x+Sizes[0]*y+SizeXY*z] & 0xff;
      // if (val < 0) val += 256;
      return (int)((val - shift) * scaleI);
  }
  int GetByteValueAt(int x, int y, int z)
  {
      int val = myData[x+Sizes[0]*y+SizeXY*z] & 0xff;
      // if (val < 0) val += 256;
      return (int) ((val-shift) * scaleB);
  }

  double GetRawValueAt(int x, int y, int z)
  {
      double val = myData[x+Sizes[0]*y+SizeXY*z] & 0xff;
      // if (val < 0 ) val += 256;
      return val;
  }

  double GetValueAt(int x, int y, int z)
  {
      int val = myData[x+Sizes[0]*y+SizeXY*z] & 0xff;
      // if (val < 0) val += 256;
      return val*ScaleV+OffsetV;
  }
        
  void ConvertSliceFromSimilar(int myslice, int bufslice, Object Ibuffer, int mstep, int moff) {
    // System.out.println("Byte Converting "+SizeXY+"\n");
    short [] mbuffer = (short []) Ibuffer;
    for (int i=0;i<SizeXY;i+=mstep)
        myData[i+Sizes[0]*Sizes[1]*myslice] = mbuffer[bufslice*SizeXY+i+moff]; 
    }
  
  void ConvertSliceFromByte(int myslice, int bufslice, byte [] Ibuffer, int mstep, int moff)
    {
      for (int i=0;i<SizeXY;i+=mstep)
        myData[i+Sizes[0]*Sizes[1]*myslice] = (short) Ibuffer[bufslice*SizeXY+i+moff]; 
    }

  void ConvertSliceFromRGB(int myslice, int bufslice, int [] Ibuffer, int mstep, int moff, int suboff)  // suboff defines which short to use
    {
      int bitshift=suboff*8;
      for (int i=0;i<SizeXY;i+=mstep)
          myData[i+Sizes[0]*Sizes[1]*myslice] = ((short) ((Ibuffer[bufslice*SizeXY+i+moff] >> bitshift) & 0xff)) ; 
    }
  
    void CopySliceToSimilar(int myslice, Object buffer)  
    {
      short [] mbuffer = (short[]) buffer;
      for (int i=0;i<SizeXY;i++)
          mbuffer[i]=myData[i+Sizes[0]*Sizes[1]*myslice]; 
    }
  }

class ZeroElement extends AnElement {
    ZeroElement(int SX, int SY, int SZ) {super(SX,SY,SZ,0.0);DataType=FloatType;}
    void Clear() {return;}
    void DeleteData() {return;}
    int GetStdByteNum() {return 0;}
    void ConvertSliceFromSimilar(int param, int bufslice, Object values, int mstep, int moff) {return;}
    void ConvertSliceFromByte(int myslice, int bufslice, byte [] Ibuffer, int mstep, int moff) {return;}
    void ConvertSliceFromRGB(int myslice, int bufslice, int [] Ibuffer, int mstep, int moff, int soff)    {throw new IllegalArgumentException("Int: Inapplicable conversion\n");}
    int GetByteValueAt(int param, int param1, int param2) {return 0;}
    int GetIntValueAt(int param, int param1, int param2) {return 0;}
    double GetRawValueAt(int param, int param1, int param2) {return 0.0;}
    double GetValueAt(int param, int param1, int param2) {return 0.0;}
    void SetValueAt(int param, int param1, int param2, double param3) {return;}
    void CopySliceToSimilar(int myslice, Object buffer)  {return;}
}

class OneElement extends AnElement {
    OneElement(int SX, int SY, int SZ) {super(SX,SY,SZ,0.0); DataType=FloatType;}
    void Clear() {return;}
    void DeleteData() {return;}
    int GetStdByteNum() {return 0;}
    void ConvertSliceFromSimilar(int param, int bufslice, Object values, int mstep, int moff) {return;}
    void ConvertSliceFromByte(int myslice, int bufslice, byte [] Ibuffer, int mstep, int moff) {return;}
    void ConvertSliceFromRGB(int myslice, int bufslice, int [] Ibuffer, int mstep, int moff, int soff)    {throw new IllegalArgumentException("Int: Inapplicable conversion\n");}
    int GetByteValueAt(int param, int param1, int param2) {return 1;}
    int GetIntValueAt(int param, int param1, int param2) {return 1;}
    double GetRawValueAt(int param, int param1, int param2) {return 1.0;}
    double GetValueAt(int param, int param1, int param2) {return 1.0;}
    void SetValueAt(int param, int param1, int param2, double param3) {return;}
    void CopySliceToSimilar(int myslice, Object buffer)  {return;}
}

class IntegerElement extends AnElement {
  int [] myData;        // holds the 3D integer data
  int SizeXY;
  int NumBytes;
  IntegerElement(int SX, int SY, int SZ, int Bts, double MaxVal) {
      super(SX,SY,SZ, MaxVal);
      NumBytes = Bts;
      myData = new int[Sizes[0]*Sizes[1]*Sizes[2]];
      SizeXY = Sizes[0]*Sizes[1];
      DataType = IntegerType; 
  }
  
  void Clear() {
    for (int i = 0; i < Sizes[0]*Sizes[1]*Sizes[2];i++) 
            myData[i] = 0;
    } 

  void DeleteData() {
            myData = null;
  }

  int GetStdByteNum() {return 4;}
  
  void SetValueAt(int x, int y, int z, double val)
  {
      myData[x+Sizes[0]*y+SizeXY*z]= (int) val;
  }
  
  int GetIntValueAt(int x, int y, int z)  // scaled to 16 bit
  {
      return (int) ((myData[x+Sizes[0]*y+SizeXY*z]-shift) * scaleI);
  }
  
  int GetByteValueAt(int x, int y, int z)
  {
      return (int) ((myData[x+Sizes[0]*y+SizeXY*z]-shift) * scaleB);
  }
  
  double GetRawValueAt(int x, int y, int z)
  {
      return (double) myData[x+Sizes[0]*y+SizeXY*z];
  }
  
  double GetValueAt(int x, int y, int z)
  {
      return myData[x+Sizes[0]*y+SizeXY*z]*ScaleV+OffsetV;
  }
  
 void ConvertSliceFromSimilar(int myslice, int bufslice, Object Ibuffer, int mstep, int moff) {
    // System.out.println("Byte Converting "+SizeXY+"\n");
    short [] mbuffer = (short []) Ibuffer;
    for (int i=0;i<SizeXY;i+=mstep)
        myData[i+Sizes[0]*Sizes[1]*myslice] = (int) (mbuffer[bufslice*SizeXY+i+moff] & 0xffff);   // usigned
  }
 
  void ConvertSliceFromByte(int myslice, int bufslice, byte [] Ibuffer, int mstep, int moff) {
    int val;
    int SliceSize = NumBytes*SizeXY;
    // double fval;
    // System.out.println("Integer Converting "+SliceSize+", "+ SizeXY+"\n");
    
    for (int i=0;i<SizeXY;i+=mstep)
    {
        val=Ibuffer[bufslice*SliceSize+NumBytes*(i+moff)] & 0xff; 
        // if (val < 0) val += 256;
        myData[i+SizeXY*myslice] = val; 

        for (int b=1;b<NumBytes;b++)
            {
            val=Ibuffer[bufslice*SliceSize+NumBytes*(i+moff)+b] & 0xff; 
            // if (val < 0) val += 256;
            myData[i+SizeXY*myslice] |= val << (8*b); 
            // System.out.print(" "+val);
            }
        // fval = myData[i+SizeXY*myslice];
    }
    // System.out.print(" "+Ielementdata[elem][i+SizeX*SizeY*slice]+ ", ");
    // if ((i%SizeX)==SizeX-1) System.out.println();
  }
  void ConvertSliceFromRGB(int myslice, int bufslice, int [] mbuffer, int mstep, int moff, int soff)
    {
      for (int i=0;i<SizeXY;i+=mstep)
        myData[i+Sizes[0]*Sizes[1]*myslice] = mbuffer[bufslice*SizeXY+i+moff]; 
    }

  void CopySliceToSimilar(int myslice, Object buffer)  
    {
      short [] mbuffer = (short[]) buffer;
      for (int i=0;i<SizeXY;i++)
          mbuffer[i]=(short) myData[i+Sizes[0]*Sizes[1]*myslice]; 
    }

  void CopyIntVal(AnElement other, double Min, double Max) {
    for (int z = 0; z < Sizes[2]; z++) 
            for (int y = 0; y < Sizes[1]; y++) 
                for (int x = 0; x < Sizes[0]; x++) 
                {
                    double val = other.GetValueAt(x,y,z);
                    val = (val-Min) /(Max-Min);
                    if (val < 0) val=0;
                    if (val > 1.0) val = 1.0;
                    SetValueAt(x,y,z,(Bundle.MaxCTable-1)*val);  // will convert the type and include all thresholds , ...
                }
  }

}

class FloatElement extends AnElement {
  float [] myData;        // holds the 3D byte data
  int SizeXY;
  
  FloatElement(int SX, int SY, int SZ, float MaxVal) {
      super(SX,SY,SZ, MaxVal);
      myData = new float[Sizes[0]*Sizes[1]*Sizes[2]];
      SizeXY = Sizes[0]*Sizes[1];
      DataType = FloatType; 
  }
  
  void Clear() {
    for (int i = 0; i < Sizes[0]*Sizes[1]*Sizes[2];i++) 
            myData[i] = 0;
    } 

  void DeleteData() {
            myData = null;
  }

  int GetStdByteNum() {return 4;}
  
  void SetValueAt(int x, int y, int z, double val)
  {
      myData[x+Sizes[0]*y+SizeXY*z]= (float) val;
  }
  
  int GetIntValueAt(int x, int y, int z)  // scaled to 16 bit
  {
      return (int) ((myData[x+Sizes[0]*y+SizeXY*z]-shift) * scaleI);
  }
  
  int GetByteValueAt(int x, int y, int z)
  {
      return (int) ((myData[x+Sizes[0]*y+SizeXY*z]-shift)*scaleB);
  }

  double GetRawValueAt(int x, int y, int z)
  {
      return (double) myData[x+Sizes[0]*y+SizeXY*z];
  }

  double GetValueAt(int x, int y, int z)
  {
      return myData[x+Sizes[0]*y+SizeXY*z]*ScaleV+OffsetV;
  }
  
 void ConvertSliceFromSimilar(int myslice, int bufslice, Object Ibuffer, int mstep, int moff) {
    // System.out.println("Byte Converting "+SizeXY+"\n");
    float [] mbuffer = (float []) Ibuffer;
    for (int i=0;i<SizeXY;i+=mstep)
        myData[i+Sizes[0]*Sizes[1]*myslice] = mbuffer[bufslice*SizeXY+i+moff]; 
  }
 
void ConvertSliceFromByte(int myslice, int bufslice, byte [] Ibuffer, int mstep, int moff) {
    int ival,val;
    float fval;
    int SliceSize = 4*SizeXY;
    // System.out.println("Float Converting "+SliceSize+", "+ SizeXY +"\n");
    for (int i=0;i<SizeXY;i+=mstep)
        {
        ival = 0;
        for (int b=1;b<4;b++)
        {
	val=Ibuffer[bufslice*SliceSize+4*(i+moff)+b] & 0xff;   // 4 Bytes !
        // if (val < 0) val += 256;
        ival |= val << (8*b); 
        }
        fval = Float.intBitsToFloat(ival);
        myData[i+SizeXY*myslice] = fval;
        }
  }
  void ConvertSliceFromRGB(int myslice, int bufslice, int [] Ibuffer, int mstep, int moff, int soff)    {throw new IllegalArgumentException("Int: Inapplicable conversion\n");}
  void CopySliceToSimilar(int myslice, Object buffer)  
    {
      float [] mbuffer = (float[]) buffer;
      for (int i=0;i<SizeXY;i++)
          mbuffer[i]=myData[i+Sizes[0]*Sizes[1]*myslice]; 
    }
}


class DoubleElement extends AnElement {
  double [] myData;        // holds the 3D byte data
  int SizeXY;
  DoubleElement(int SX, int SY, int SZ, double MaxVal) {
      super(SX,SY,SZ, MaxVal);
      myData = new double[Sizes[0]*Sizes[1]*Sizes[2]];
      SizeXY = Sizes[0]*Sizes[1];
      DataType = DoubleType; 
  }
  
  void Clear() {
    for (int i = 0; i < Sizes[0]*Sizes[1]*Sizes[2];i++) 
            myData[i] = 0;
    } 

  void DeleteData() {
            myData = null;
  }

  int GetStdByteNum() {return 8;}
  
  void SetValueAt(int x, int y, int z, double val)
  {
      myData[x+Sizes[0]*y+SizeXY*z]= (float) val;
  }
  
  int GetIntValueAt(int x, int y, int z)  // scaled to 16 bit
  {
      return (int) ((myData[x+Sizes[0]*y+SizeXY*z]-shift) * scaleI);
  }
  
  int GetByteValueAt(int x, int y, int z)
  {
      return (int) ((myData[x+Sizes[0]*y+SizeXY*z]-shift)*scaleB);
  }

  double GetRawValueAt(int x, int y, int z)
  {
      return (double) myData[x+Sizes[0]*y+SizeXY*z];
  }

  double GetValueAt(int x, int y, int z)
  {
      return myData[x+Sizes[0]*y+SizeXY*z]*ScaleV+OffsetV;
  }
  
 void ConvertSliceFromSimilar(int myslice, int bufslice, Object Ibuffer, int mstep, int moff) {
    // System.out.println("Byte Converting "+SizeXY+"\n");
    float [] mbuffer = (float []) Ibuffer;
    for (int i=0;i<SizeXY;i+=mstep)
        myData[i+Sizes[0]*Sizes[1]*myslice] = mbuffer[bufslice*SizeXY+i+moff]; 
  }
 
void ConvertSliceFromByte(int myslice, int bufslice, byte [] Ibuffer, int mstep, int moff) {
    long ival,val;
    double fval;
    int SliceSize = 8*SizeXY;
    //System.out.println("Double Converting "+myslice+", "+bufslice+", "+SliceSize+", "+ SizeXY +", "+mstep+"\n");
    for (int i=0;i<SizeXY;i+=mstep)
        {
        ival = 0;
        for (int b=1;b<8;b++)
        {
	val=Ibuffer[bufslice*SliceSize+8*(i+moff)+b] & 0xff;   // 4 Bytes !
        // if (val < 0) val += 256;
        ival |= val << (8*b); 
        }
        fval = Double.longBitsToDouble(ival);
        myData[i+SizeXY*myslice] = fval;
        }
  }
  void ConvertSliceFromRGB(int myslice, int bufslice, int [] Ibuffer, int mstep, int moff, int soff)    {throw new IllegalArgumentException("Int: Inapplicable conversion\n");}
  void CopySliceToSimilar(int myslice, Object buffer)  // ImageJ cannot handle proper double images, thus conversion to float
    {
      float [] mbuffer = (float[]) buffer;
      for (int i=0;i<SizeXY;i++)
          mbuffer[i]=(float) myData[i+Sizes[0]*Sizes[1]*myslice]; 
    }
}

class ComplexElement extends AnElement {
  float [] myData;        // holds the 3D data in pairs of real numbers
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

class Bundle extends Object implements Cloneable {  // this class bundles some user-defined values seperately for each element
    private double mincs=0.0,maxcs=1.0;
    private double ProjMincs[]={0.0,0.0},ProjMaxcs[]={1.0,1.0}; // For the different projection modes (Max, Avg)
    public int ElementModelNr=0;    // just a number for the current model 
    final static int ElementModels=13;  // Nr of element models
    final static String ElementModelName[]={"GrayScale","Red","Green","Blue","Purple","Glow Red","Glow Green-Blue","Glow Green-Yellow","Glow Blue","Glow Purple","Rainbow","Random","Cyclic"};  // Nr of element models
    byte  cmapRed[],cmapGreen[],cmapBlue[];
    static Vector MapSizes = new Vector(),RedMaps = new Vector(),GreenMaps = new Vector(),BlueMaps = new Vector(); // For user-supplied colormaps
    boolean cmapIsInverse=false;
    IndexColorModel ElementModel;
    boolean ShowOvUn=false;
    boolean LogScale=false;
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

    void ToggleOverlayDispl(int val) {
        if (val < 0)
            DispOverlay = ! DispOverlay;
        else if (val == 0)
            DispOverlay = false;
        else if (val == 1)
            DispOverlay = true;            
    }

    void ToggleMulDispl(int val) {
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
    public void TakePlaneROIs(Vector [] PlanesS, Vector [] PlanesD) {return;}  // ignored for now
    
    public Object clone()
    {
        Bundle nb = new Bundle(ElementModelNr,mincs,maxcs);
        nb.MIPMode = MIPMode;
        nb.ShowOvUn= ShowOvUn;
        nb.LogScale=LogScale;
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
                            int MapSize=((Integer) MapSizes.elementAt(elem)).intValue();
                            //int MapSize=((byte []) RedMaps.elementAt(elem)).length;
                            int index=(int) ((MapSize-1)*tmp);
                            cmapRed[i]=((byte []) RedMaps.elementAt(elem))[index];
                            cmapGreen[i]=((byte []) GreenMaps.elementAt(elem))[index];
                            cmapBlue[i]=((byte []) BlueMaps.elementAt(elem))[index];
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
	
public void ToggleLog(int newVal) {
	if (newVal < 0)
	    LogScale = ! LogScale;
	else
	    LogScale = (newVal == 1);
	CompCMap();
   }
	
}

class APoint extends Object implements Cloneable {  // contains the data stored in every marker
    double coord[];   // saves the coordinates of markers in pixel coordinates
    double integral;  
    double max,min,integralAboveMin;  
    int mycolor;
    boolean tagged; // Can be used by users to classify markes
    boolean isDisplayed=false; // This keeps track whether a Marker is currently displayed. This also makes it clickable

    public APoint(double x, double y, double z, double e, double t) {
	coord = new double[5];
	coord[0] = x;
	coord[1] = y;
	coord[2] = z;
	coord[3] = e;
	coord[4] = t;
        integral= 0.0;  // includes minimum pixel
        max=0.0;
        min=0.0;
        integralAboveMin=0.0;
	mycolor = NewColor();
	tagged = false;
    }
    
    public Object clone()
    {
        APoint pt=new APoint(coord[0],coord[1],coord[2],coord[3],coord[4]);
	pt.copy(this);
        return pt;
    }
    
    public void copy(APoint pt)
    {
        mycolor=pt.mycolor;
        integral=pt.integral;
        max=pt.max;
        min=pt.min;
        integralAboveMin=pt.integralAboveMin;
	coord[0] = pt.coord[0];
	coord[1] = pt.coord[1];
	coord[2] = pt.coord[2];
	coord[3] = pt.coord[3];
	coord[4] = pt.coord[4];
	tagged = pt.tagged;
    }
    
    
    static int NewColor() {
	int Red=(int) (255.0 * Math.random()); 
	if (Red < 128) Red = 64;
	int Green=(int) (255.0 * Math.random());
	if (Green < 64) Green = 64;
	int Blue=(int) (255.0 * Math.random());
	if (Blue < 128) Blue = 64;
        return (255 << 24) | (Red << 16) | (Green << 8 ) | Blue;
    }
    
    public void Tag(int value) { // -1 : toggle, 0,1 false, true
    	if (value == 0)
		tagged=false;
	else if (value > 0)
		tagged=true;
	else
	    tagged = !tagged;
    }
    
    public double SqrDistTo(APoint other)  // does not include time and elements
    {
        return (coord[0]-other.coord[0])*(coord[0]-other.coord[0])+
               (coord[1]-other.coord[1])*(coord[1]-other.coord[1])+
               (coord[2]-other.coord[2])*(coord[2]-other.coord[2]);
    }

    public double SqrDistTo(APoint other,double SX,double SY, double SZ)  // does not include time and elements
    {
        return (coord[0]-other.coord[0])*(coord[0]-other.coord[0])*SX*SX+
               (coord[1]-other.coord[1])*(coord[1]-other.coord[1])*SY*SY+
               (coord[2]-other.coord[2])*(coord[2]-other.coord[2])*SZ*SZ;
    }

    public double SqrXYDistTo(APoint other,double SX,double SY)  // does not include time and elements
    {
        return (coord[0]-other.coord[0])*(coord[0]-other.coord[0])*SX*SX+
               (coord[1]-other.coord[1])*(coord[1]-other.coord[1])*SY*SY;
    }
    
    public boolean InRange(double px, double py, int dir, double dx, double dy) {
        // System.out.println("InRange"+px+", "+py+" dir "+dir+"  dx "+dx+" dy"+dy);
        if (dir == 2)
        {
            if (Math.abs(coord[0] - px) < dx && Math.abs(coord[1] - py) < dy)
                return true;
        }
        else if (dir == 1)
        {
            if (Math.abs(coord[0] - px) < dx && Math.abs(coord[2] - py) < dy)
                return true;
        }
        else // dir == 0
        {
            if (Math.abs(coord[2] - px) < dx && Math.abs(coord[1] - py) < dy)
                return true;
        }
        return false;
    }

    public void UpdatePosition(double px, double py, int dir)
    {
        if (dir == 2)
        { coord[0] = px;coord[1]=py;}
        else if (dir == 1)
        { coord[0] = px;coord[2]=py;}
        else 
        { coord[2] = px;coord[1]=py;}
    }
}

class MarkerList extends Object {
    int NumPoints = 0;  // Number of Points in the active list
    int ActivePoint = -1;
    public Vector MyPoints=null;    // currently active list of positions
    MarkerList ParentList=null;    // the list from which this list was inherited
    MarkerList Child1List=null;    // the list from which this list was inherited
    MarkerList Child2List=null;    // the list from which this list was inherited
    int PreferredChild=1;          // to remember the preferred way the user wants to navigate
    boolean toggling=false;
    static final int Attributes=19;   // number of attributes in marker lists
    public String MyName="Unknown";

    public MarkerList() {
        NewList();
      }

    void LinkTo(MarkerList Parent)
    {
    	if (Parent == null) return;
    	ParentList=Parent;   // stores the link, not the number
    	if (ParentList.Child1List == null)
    		ParentList.Child1List = this;
    	else if (ParentList.Child2List == null)
    		ParentList.Child2List = this;
    	if (NumPoints > 0)
    		{
    		SetColor(Parent.GetPoint(0).mycolor);   // use the color of the parent
    		System.out.println("LinkTo Chose color " + Parent.GetPoint(0).mycolor + "\n");
    		}
    	}
    
    void NewList() {
        if (MyPoints != null && NumPoints == 0)
            return; // No New list here, since an empty list would be left behind
      MyPoints = new Vector();    // stores a number of points
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
        if (ParentList != null)
        	ParentList.SetColor(color);
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

    String GetMarkerListName() {
    	return MyName;
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
        // System.out.println("Get Point ("+p+","+list+")");
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
            markers[i][18]=point.mycolor;
        }
        return markers;
    }
 
}

class MarkerLists extends Object { // This class manages multiple lists of point markers
	MarkerList MyActiveList = null;
    int NumLists = 0;
    int ActiveList = -1;
    int CurrentNameNr=1;
    Vector ListOfLists;   // marker coordinates are stored as a list of APoint objects in pixel coordinates here
    //public Vector MyPoints=null;    // currently active list of positions
    static int dx=3,dy=3;  // size of the marker in pixels
    
    public MarkerLists() {
      ListOfLists = new Vector();
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

    boolean AdvancePoint(int howmany) {
    	if (NumLists < 1) return false; 
    	if (MyActiveList.ActivePoint + howmany >= MyActiveList.NumPoints)
    		{
    		if (MyActiveList.Child1List != null && (MyActiveList.Child2List == null || MyActiveList.PreferredChild==1))
    		{SetActiveList(GetChild1Index(ActiveList));MyActiveList.ActivePoint=0;
    		return true;
    		}
    		else if (MyActiveList.Child2List != null && (MyActiveList.Child1List == null || MyActiveList.PreferredChild==2))
    		{
    			SetActiveList(GetChild2Index(ActiveList));
    			MyActiveList.ActivePoint=0;    			
    			return true;
    			}
    		return false;
    		}
        else if (MyActiveList.ActivePoint + howmany < 0)
        {
    		if (MyActiveList.ParentList != null)
    		{
    			if (MyActiveList == MyActiveList.ParentList.Child1List) // remember which way the user came
    				MyActiveList.ParentList.PreferredChild = 1;
    			if (MyActiveList == MyActiveList.ParentList.Child2List)
    				MyActiveList.ParentList.PreferredChild = 2;
    		   	//System.out.println("Devancing from preferred Child" + MyActiveList.ParentList.PreferredChild);
    			SetActiveList(GetParentIndex(ActiveList));
    			MyActiveList.ActivePoint=MyActiveList.NumPoints-1; /// last point in list
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
    	while (Root1.ParentList != null)
    		Root1=Root1.ParentList;        // cycles will kill this here!
    	while (Root2.ParentList != null)
    		Root2=Root2.ParentList;        // cycles will kill this here!
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
    
    void ImportPositions(float [][] positions)   // this works only for one list
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
		while (GetParentIndex(alist) >= 0)
		{
			int parent=GetParentIndex(alist);
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
        md.addNumericField("Parent: ",GetParentIndex(MarkerListNr),5);
        md.addNumericField("Child1: ",GetChild1Index(MarkerListNr),5);
        md.addNumericField("Child2: ",GetChild2Index(MarkerListNr),5);
        md.addNumericField("Red: ",Red,5);
        md.addNumericField("Green: ",Green,5);
        md.addNumericField("Blue: ",Blue,5);
        md.showDialog();
        if (! md.wasCanceled())
        {
            int Parent,Ch1,Ch2;
            mlist.MyName=md.getNextString();
            Parent=(int) md.getNextNumber();
            if (Parent != GetParentIndex(MarkerListNr))
            {
                if (Parent < 0) // uncupple list
                {
            	if (mlist.ParentList != null && mlist.ParentList.Child1List == mlist)
            		mlist.ParentList.Child1List =null;
            	if (mlist.ParentList != null && mlist.ParentList.Child2List == mlist)
            		mlist.ParentList.Child2List =null;
                	mlist.ParentList = null;  // deletes the parents
                }
                else   // the new parent
                {
                	MarkerList nparent=GetMarkerList(Parent);
            		if (nparent.Child1List == null)
            		{
            			nparent.Child1List = mlist;
            			mlist.ParentList = nparent; 
            		}
            		else if (nparent.Child2List == null)
            		{
            			nparent.Child2List = mlist;
            			mlist.ParentList = nparent; 
            		}
            		else
            			IJ.showMessage("Warning: Cannot reconntect parent. New Parent has no free children.");
                }
            }
            Ch1=(int) md.getNextNumber();
            if (Ch1 != GetChild1Index(MarkerListNr))
            {
            	if (Ch1 < 0)
            	{
            	if (mlist.Child1List != null && mlist.Child1List.ParentList == mlist)
            		{
            		mlist.Child1List.ParentList =null;
            		mlist.Child1List=null;
            		}
            	}
            	else  // the new child
                {
                	MarkerList nch =GetMarkerList(Ch1);
                	if (nch != null)
                	{
            		if (nch.ParentList != null)
            		{
            			if (nch.ParentList.Child1List == nch)
            				nch.ParentList.Child1List = null;
            			if (nch.ParentList.Child2List == nch)
            				nch.ParentList.Child2List = null;
            		}
        			nch.ParentList = mlist;
        			mlist.Child1List = nch;
                	}
                }
            }
            
            Ch2=(int) md.getNextNumber();
            if (Ch2 != GetChild1Index(MarkerListNr))
            {
            	if (Ch2 < 0)
            	{
            	if (mlist.Child2List != null && mlist.Child2List.ParentList == mlist)
            		{
            		mlist.Child2List.ParentList =null;
            		mlist.Child2List=null;
            		}
            	}
            	else  // the new child
                {
                	MarkerList nch =GetMarkerList(Ch2);
                	if (nch != null)
                	{
            		if (nch.ParentList != null)
            		{
            			if (nch.ParentList.Child1List == nch)
            				nch.ParentList.Child1List = null;
            			if (nch.ParentList.Child2List == nch)
            				nch.ParentList.Child2List = null;
            		}
        			nch.ParentList = mlist;
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

    
    boolean InsertInformationVector(float [] info, String MarkerName)    // inserts one line of information into the viewer, this can create a new list if necessary
    {
    	int listnr = (int) info[0];
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
    	int ParentNr = (int) info[10+7];
    	int Child1Nr = (int) info[11+7];
    	int Child2Nr = (int) info[12+7];
        int MyColor =(int) info[13+7];
 
    	if (listnr>NumLists)  // This new list is more than one bigger than existing lists
    		return false;

    	if (listnr>=NumLists)  // This new list is one bigger than existing lists -> make new one
    		for (int ll=NumLists;ll <= listnr; ll++)
    			AppendList();
    	if (ParentNr >=0)
    		if (ParentNr >= NumLists)
    		for (int ll=NumLists;ll <= ParentNr; ll++)
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
    	
    	if (ParentNr >=0)
    		MyActiveList.ParentList = GetMarkerList(ParentNr);  		
    
    	if (Child1Nr >=0)
    		MyActiveList.Child1List = GetMarkerList(Child1Nr);
    	if (Child2Nr >=0)
    		MyActiveList.Child2List = GetMarkerList(Child2Nr);

    	// Vector MyPoints = MyActiveList.MyPoints;
        int NumPoints = NumMarkers(ActiveList);
        APoint pt=new APoint(posx,posy,posz,pose,post);
        if (NumPoints > 0)
            pt.mycolor = GetPoint(0,-1).mycolor;  // all have the same color as the first one
        if (NumPoints == 1)  // probably jsut the first point was added
        	MyActiveList.SetColor(pt.mycolor);  // ensure the rest of the connected regions are updated as well

        pt.tagged=(tagged!=0);
        pt.integral=integral;
        pt.max=max;
        pt.mycolor=MyColor;
		MyActiveList.MyName = MarkerName; // use the marker name as a list name for now
		if (MarkerName == null || MarkerName.equals(""))
    		if (ParentNr >= 0)
    		{
    			if (MyActiveList.ParentList.Child1List == MyActiveList)
    				MyActiveList.MyName = MyActiveList.ParentList.MyName + "a";
    			if (MyActiveList.ParentList.Child2List == MyActiveList)
    				MyActiveList.MyName = MyActiveList.ParentList.MyName + "b";
    			if (MyActiveList.Child1List != null)
    				if (MyActiveList.Child1List.ParentList == MyActiveList)
    					MyActiveList.Child1List.MyName = MyActiveList.MyName + "a";
    			if (MyActiveList.Child2List != null)
        			if (MyActiveList.Child2List.ParentList == MyActiveList)
        				MyActiveList.Child2List.MyName = MyActiveList.MyName + "a";
    		}
    		else
        		{
				if (MyActiveList == null || MyActiveList.MyName == null || ! MyActiveList.MyName.equals("" + CurrentNameNr))
					CurrentNameNr=listnr+1;
    			MyActiveList.MyName = "" + CurrentNameNr;
    			// System.out.println("New Name "+MyActiveList.MyName+"\n");
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
    		
            //data3d.ConstrainPoint(pt);
            //data3d.ClippedCOI(pt,data3d.COMX,data3d.COMY,data3d.COMZ,false);  // use the current settings and recompute center of intensity, but do not change positions
    return true;
    }

   public void DeleteAllMarkerLists() // deletes all existing marker lists
   {
	while (NumMarkerLists() > 1) { RemoveList(); }
	RemoveList();
   }

    void ImportMarkerLists(float [][] lists)    // inserts all lists
    {
    	float [] alist;
    	for (int l=0; l<lists.length; l++)
    	{
    		alist = lists[l];
    		InsertInformationVector(alist,"");
    	}
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

    int GetParentIndex(int listnr) {
        if (NumLists <= 0) return -1;
        return ListOfLists.indexOf(GetMarkerList(listnr).ParentList);
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
    	return (GetParentIndex(list) >=0);
    }

    APoint GetParentEndOfTrack(int list) {
    	if (! HasParent(list)) return null;
    	
    	MarkerList myparent=GetMarkerList(GetParentIndex(list));
    	return myparent.GetPoint(myparent.NumPoints-1);
    	}

    double [][] ExportMarkers(int list,My3DData data3d)
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
        markers[i][15] = GetParentIndex(list);
        markers[i][16] = GetChild1Index(list);
        markers[i][17] = GetChild2Index(list);
        }
        return markers;
    }

    double [][] ExportMarkerLists(My3DData data3d)
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

    String PrintList(My3DData data3d) {
        String newtext="#List Nr.,\tMarker Nr,\tPosX [pixels],\tY [pixels],\tZ [pixels],\tElements [element],\tTime [time],\tIntegral (no BG sub) [Units],\tMax (no BG sub) [Units],"+
        "\t"+data3d.GetAxisNames()[0]+" ["+data3d.GetAxisUnits()[0]+"],\t"+data3d.GetAxisNames()[1]+" ["+data3d.GetAxisUnits()[1]+"],\t"+data3d.GetAxisNames()[2]+" ["+data3d.GetAxisUnits()[2]+"],\t"+data3d.GetAxisNames()[3]+" ["+data3d.GetAxisUnits()[3]+"],\t"+data3d.GetAxisNames()[4]+" ["+data3d.GetAxisUnits()[4]+"],\tIntegral "+data3d.GetValueName(data3d.ActiveElement)+" (no BG sub)["+data3d.GetValueUnit(data3d.ActiveElement)+"],\tMax "+data3d.GetValueName(data3d.ActiveElement)+" (no BG sub)["+data3d.GetValueUnit(data3d.ActiveElement)+"]\tTagText \tTagInteger \tParentNr \tChild1 \t Child2 \tListColor \tListName\n";
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
            newtext = newtext + GetParentIndex(l) + "\t";
            newtext = newtext + GetChild1Index(l) + "\t";
            newtext = newtext + GetChild2Index(l) + "\t";
            newtext = newtext + GetMarkerList(l).GetColor() + "\t";
            newtext = newtext + GetMarkerList(l).GetMarkerListName() + "\n";
          }
        }
        return newtext;
    }

 String PrintSummary(My3DData data3d) {
    String newtext = "# Statistics Summary\n";
    APoint point,oldpoint;
    NumberFormat nf = java.text.NumberFormat.getNumberInstance(Locale.US);
    nf.setMaximumFractionDigits(3);
    // nf.setMinimumIntegerDigits(7);
    nf.setGroupingUsed(false);
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
    	MyActiveList.LinkTo(GetMarkerList(linkTo));
    	MyActiveList.MyName = MyActiveList.ParentList.MyName +  NameExtension;
    }

    void RemoveList() {
        if (NumLists < 1) // Also the last list can be removed
            return;
        if (MyActiveList == null) return;
        
        MyActiveList.MyPoints=null;
        if (MyActiveList.ParentList != null)
          if (MyActiveList.ParentList.Child1List == MyActiveList)
        	MyActiveList.ParentList.Child1List = null;  // delete the child information
        if (MyActiveList.ParentList != null)
          if (MyActiveList.ParentList.Child2List == MyActiveList)
        	MyActiveList.ParentList.Child2List = null;  // delete the child information
        if (MyActiveList.Child1List != null)
          if (MyActiveList.Child1List.ParentList == MyActiveList)
        	MyActiveList.Child1List.ParentList = null;  // delete the childs parent information
        if (MyActiveList.Child2List != null)
          if (MyActiveList.Child2List.ParentList == MyActiveList)
        	MyActiveList.Child2List.ParentList = null;  // delete the childs parent information
        
        ListOfLists.removeElementAt(ActiveList);
        NumLists --;
        if (NumLists <= 0)
            {ActiveList=-1;MyActiveList=null;NewList();NumLists=1;}  // Active List will then be 0
        if (ActiveList >= NumLists)
			{ ActiveList = NumLists-1; }
        MyActiveList  = GetMarkerList(ActiveList);   // just use the current position and advance to the next list
    }

    void AdvanceList(int howmany) {
        if (NumLists <= 1) return;
        if (NumMarkers(ActiveList)<=0 && NumLists > 1) 
        {
            RemoveList();howmany-=1;
        }
        ActiveList += howmany;
        if (ActiveList < 0) ActiveList += NumLists;
        if (ActiveList >= NumLists) ActiveList = ActiveList % NumLists;
        MyActiveList = GetMarkerList(ActiveList);
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

	for (int list=0;list < NumLists;list++)
        // if (alllists || list == ActiveList)   // to save some time
            for (int p=0;p<NumMarkers(list);p++)
            {
                APoint Pt=GetPoint(p,list);
                if (Pt.isDisplayed)
                {
                	// double xOff = my3ddata.ElementAt((int) Pt.coord[3],(int) Pt.coord[4]).DisplayOffset[XDim];
                	// double yOff = my3ddata.ElementAt((int) Pt.coord[3],(int) Pt.coord[4]).DisplayOffset[YDim];
                	if (Pt.InRange(px,py,dir,DistX,DistY)) 
                        return Pt;
                }
            }
    return null;
}
}

class My3DData extends Object {
    public String markerInfilename=null;
    public String markerOutfilename=null;
    public Vector MyElements;    // this stores all the data
    
    public Vector MyProjections[];    // these manage progections
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
    int SearchX=3,SearchY=3,SearchZ=1;
    int COMX=3,COMY=3,COMZ=0;
    String [] TrackDirections={"X","Y","Z","Elements","Time"};
    int TrackDirection=2;  // by default track along Z
    String [] TrackModes={"Max","Min"};
    int TrackMode=0;  // by default track Maxima
    boolean Repulsion=false;
    boolean FocusDispToMarker=true;
    boolean AppendVersionNumber=false;
    boolean didSwitchAppendVersionNumber = false;
    boolean ActioReactio=false;
    double FWHM=4.0;

    public int SizeX=0,SizeY=0,SizeZ=0,Elements=0,ActiveElement=0;
    public int DimensionOrder=0;  // defines the order of the dimensions to read in
    public int AppendTo=0;  // defines the dimension to wich to append the extra loaded data

    public Vector MyTimes,MyTimeProj;    // this stores all the different multi element data as a vector of vectors
    public int Times=0,ActiveTime=0;
    public Vector MyTimeColorProj; // a vector of an array of ASlice
    public Vector TimeValues;    // keeps track of the exact time points
    
    My3DData MyHistogram=null;
    My3DData DataToHistogram=null;
    
    int  sizes[]= {0,0,0,0,0};  // x,y,z,elements,time

    int PrevType,PrevBytes, PrevBits; // Just stores the previously used Bytes and Bits values for reload to function
        
    int HistoX=0, HistoY=-1, HistoZ=-1;  // -1 means no histgram is computed along this dimension
    
    int ProjMin[],ProjMax[];       // Square ROIs
    Vector PlanesS[], PlanesD[];
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

    Vector MyBundle;    // containes maxcs, mincs, ElementModelNr;

    void AddPoint(APoint p) {MyMarkers.AddPoint(p);}
    void AddPoint(double x, double y, double z, double e, double t) {AddPoint(new APoint(x,y,z,e,t));}
    void RemovePoint() {MyMarkers.RemovePoint();}
    void RemoveTrailingPoints() {MyMarkers.RemoveTrailingPoints();}
    boolean AdvancePoint(int howmany) {return MyMarkers.AdvancePoint(howmany);} // cave: can change the lists
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
    boolean HasParent(int list) {return MyMarkers.HasParent(list);}
    APoint GetParentEndOfTrack(int list) {return MyMarkers.GetParentEndOfTrack(list);}

    
    APoint GetActiveMarker() {return GetPoint(-1);}

    boolean CheckActiveMarker(int list, int numMarker, int px, int py, int pz, int element, int time) 
    {
    	int mycoord[] = {px,py,pz,element,time};
    	
      if (list == ActiveMarkerListPos() || 
    	  list == MyMarkers.GetParentIndex(MyMarkers.ActiveList) || 
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
    void NewMarkerList() {MyMarkers.NewList();}
    void NewMarkerList(int linkTo, String NameExtension) {MyMarkers.NewList(linkTo,NameExtension);}

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
    					oldChild1.ParentList = MyMarkers.GetMarkerList(newListNr);
    					MyMarkers.GetMarkerList(newListNr).Child1List = oldChild1;
    				}
    			if (oldChild2 != null)
    				{
    					oldChild2.ParentList = MyMarkers.GetMarkerList(newListNr);
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
    void AdvanceMarkerList(int howmany) {MyMarkers.AdvanceList(howmany);}
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
    
    void AutoTrack()  // will start at the current marker and track the spot through the stack
    {
        APoint Pt=GetPoint(-1);  // retrieves the active point in the active List
        if (Pt == null) return; // no idea where to start the tracking
        int PNr = ActiveMarkerPos();
        int NumM = NumMarkers(-1);
        APoint prevPt = Pt;
        double stepcoord=Pt.coord[TrackDirection];
        double preve=Pt.coord[3], prevt=Pt.coord[4],pe,pt;
        
        for (PNr+=1;PNr<NumM;PNr++)  // This updates the allready present markers
            {
              SetActiveMarker(PNr);
              Pt=GetPoint(PNr);
              pe=Pt.coord[3]; pt=Pt.coord[4];
              stepcoord=Pt.coord[TrackDirection];
              // System.out.println("... updating marker nr. "+PNr+"\n");
              Pt.copy(prevPt); // use old point's coordinates
              Pt.coord[TrackDirection]=stepcoord; // restor track position to original

              Pt.coord[0] = Pt.coord[0] - ElementAt((int) pe,(int) pt).DisplayOffset[0] + ElementAt((int) preve,(int) prevt).DisplayOffset[0];
              Pt.coord[1] = Pt.coord[1] - ElementAt((int) pe,(int) pt).DisplayOffset[1] + ElementAt((int) preve,(int) prevt).DisplayOffset[1];
              Pt.coord[2] = Pt.coord[2] - ElementAt((int) pe,(int) pt).DisplayOffset[2] + ElementAt((int) preve,(int) prevt).DisplayOffset[2];
              //Pt.copy(prevPt);
              //Pt.coord[TrackDirection] = backup.coord[TrackDirection];  // preserve the coordinae along track direction
              UpdateMarker(Pt);  // Track with prevPt starting positions
              preve=pe; prevt=pt;
              prevPt=Pt;
              //px=Pt.coord[0];py=Pt.coord[1];pz=Pt.coord[2];pe=Pt.coord[3];pt=Pt.coord[4];
            }
        int TrackSize=0,ix=0,iy=0,iz=0,ie=0,it=0,tpos=0;  // increments during tracking
        double px=Pt.coord[0],py=Pt.coord[1],pz=Pt.coord[2];
        pe=Pt.coord[3];pt=Pt.coord[4];
        
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
        for (;tpos < TrackSize;tpos++)  // continue until the end in z is reached
            {
              //System.out.println("AutoTrack at Pt:"+PNr+", coords:"+px+", "+py+", "+pz);
              //System.out.println("... creating marker\n");
        	  px = px - ElementAt((int) pe,(int) pt).DisplayOffset[0] + ElementAt((int) preve,(int) prevt).DisplayOffset[0];
        	  py = py - ElementAt((int) pe,(int) pt).DisplayOffset[1] + ElementAt((int) preve,(int) prevt).DisplayOffset[1];
        	  pz = pz - ElementAt((int) pe,(int) pt).DisplayOffset[2] + ElementAt((int) preve,(int) prevt).DisplayOffset[2];
              SetMarker(px,py,pz,pe,pt);
              Pt=GetPoint(PNr);
              preve=pe; prevt=pt;
              px=Pt.coord[0];py=Pt.coord[1];pz=Pt.coord[2];pe=Pt.coord[3];
              px+=ix;py+=iy;pz+=iz;pe+=ie;pt+=it;  // advance one
	      PNr++;
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
        md.addCheckbox("Spectral Display for Track",ShowSpectralTrack);
        md.addMessage("For automatic marker positioning an iterative search for the nearest\n"+
                    "maximum signal is performed. The region to be searched is defined by the\n"+
                    "number of voxels to include in each direction, beyond the current plane.\n"+
                    "To restrict the update to any plane or line choose zero in the\n"+
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
        	if (IJ.isWindows())
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
            case 0: SearchX=0;break;
            case 1: SearchY=0;break;
            case 2: SearchZ=0;break;
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
 
    public Vector ElementsAtTime(int atime)
    {
        return (Vector) MyTimes.elementAt(atime);
    }

    public Vector [] ProjsAtTime(int atime)
    {
        return (Vector []) MyTimeProj.elementAt(atime);
    }

    public ASlice [] ColorProjsAtTime(int atime)
    {
        return (ASlice []) MyTimeColorProj.elementAt(atime);
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
       for (int d=0;d<3;d++)
           if (BundleAt(elem).ProjValid[d] && ProjAt(d,elem).isValid)
                return ProjAt(d,elem).ROISum;

       // DoProject(elem,2);  // Also checks if really necessary
       return ProjAt(2,elem).ROISum;
   }

   public double GetROIVoxels(int elem) {
       for (int d=0;d<3;d++)
           if (BundleAt(elem).ProjValid[d] && ProjAt(d,elem).isValid)
                return ProjAt(d,elem).ROIVoxels;

       // DoProject(elem,2);  // Also checks if really necessary
       return ProjAt(2,elem).ROIVoxels;
   }
   
   public double GetROIAvg(int elem) {
       for (int d=0;d<3;d++)
           if (BundleAt(elem).ProjValid[d] && ProjAt(d,elem).isValid)
                return ProjAt(d,elem).ROIAvg;

       // DoProject(elem,2);  // Also checks if really necessary
       return ProjAt(2,elem).ROIAvg;
   }

   public double GetROIMax(int elem) {
       for (int d=0;d<3;d++)
           if (BundleAt(elem).ProjValid[d] && ProjAt(d,elem).isValid)
                return ProjAt(d,elem).ROIMax;

       // DoProject(elem,2);  // Also checks if really necessary
       return ProjAt(2,elem).ROIMax;
   }

   public double GetROIMin(int elem) {
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
        GenericDialog md= new GenericDialog("Axes Units and Scalings");
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

  void ValueUnitsDialog() {  // allows the user to define the units and scales
        GenericDialog md= new GenericDialog("Value Unit and Scaling");
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
        md.showDialog();
        if (! md.wasCanceled())
        {
            String NV,UV;
            double SV,OV,Min,Max;
            NV=md.getNextString();UV=md.getNextString();SV=md.getNextNumber();OV=md.getNextNumber();Min=md.getNextNumber();Max=md.getNextNumber();
            DispOffset[0]=md.getNextNumber();DispOffset[1]=md.getNextNumber();DispOffset[2]=md.getNextNumber();
            ElementAt(e).SetScales(SV,OV,NV,UV);  // The value scales are element specific
           SetScaledMinMaxcs(e,Min,Max);
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

   void InvalidateSlices() {
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
        return (Bundle) MyBundle.elementAt(num);
    }
    
    
    private AnElement GNE(AnElement oldelem, Vector ElementsList, Vector ProjList[]) // will generate a new element in the list
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
        MyBundle.addElement(BundleAt(Elements-2).clone());

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
    
    private AnElement GNE(int DataType, int NumBytes, int NumBits, Vector ElementList, Vector ProjList[])  // just generate the element, not the bundle
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
    
    public int GenerateNewElement(int DataType, int NumBytes, int NumBits, double[] Scales, double[] Offsets,
                                   double ScaleV, double OffsetV, 
                                   String [] Names, String [] Units,
                                   Vector ElementList, Vector ProjList[])
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
        return ne;
    }

    public int GenerateNewTime(int DataType, int NumBytes, int NumBits, double[] Scales,
            double[] Offsets, double ScaleV, double OffsetV, 
           String [] Names, String [] Units)
		{
		int ne=0;

		MyElements = new Vector();   // A list of elements is generated for each timepoint
        MyTimes.addElement(MyElements);  // However, all times use the same list of elements.
        MyProjections = new Vector[3];    // these manage progections
        MyProjections[0] = new Vector();
        MyProjections[1] = new Vector();
        MyProjections[2] = new Vector();
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
    
    public void DeleteActElement(Vector ElementList)  // what, if another dublicate exists?
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
        
        if (! GetBundleAt(e).LogScale)
            return (val-min) /(max-min);
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

    AnElement ElementAt(int num, int time) {
        if (num >= 0 && time >= 0)
            return (AnElement) ElementsAtTime(time).elementAt(num);
        else
            return null;
    }

    AnElement ElementAt(int num) {
        if (num >= 0)
            return (AnElement) MyElements.elementAt(num);
        else
            return null;
    }
    
    AnElement ActElement() {
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
            BundleAt(e).TakePolyROIs(ROIPolygons);
        InvalidateProjs(-1);  // all projections are invalid
    }

    public void MoveSqrROI(int DX,int DY, int dir)  // adds a point to the Polygon ROI
    {
        Rectangle r2 = GetSqrROI(dir);
        for (int e=0;e<Elements;e++)
            BundleAt(e).UpdateSqrROI(r2.x + DX,r2.y + DY, r2.x+r2.width + DX, r2.y+r2.height + DY,dir);
        InvalidateProjs(-1);  // all projections are invalid
    }
    
    public void MoveROI(int DX,int DY, int dir)  // adds a point to the Polygon ROI
    {
        if (SquareROIs())
            MoveSqrROI(DX,DY,dir);
        else
            MovePolyROI(DX,DY,dir);
    }
    
    public void TakePolyROI(int ROIX,int ROIY, int dir)  // adds a point to the Polygon ROI
    {
        if (ROIPolygons[dir] == null)
            ROIPolygons[dir] = new Polygon();
            
        ROIPolygons[dir].addPoint(ROIX,ROIY);
        for (int e=0;e<Elements;e++)
            BundleAt(e).TakePolyROIs(ROIPolygons);
        InvalidateProjs(-1);  // all projections are invalid
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
        // double max = ActElement().ComputeHistogram(DataToHistogram.GetGateElem(), DataToHistogram.ActROI());
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
	if (SizeX*SizeY*SizeZ == 0)
	    {
		myv3d.add("South",new Label ("Error ! Image has zero sizes !\n"));
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

                // System.out.println("Optained Slice");
	    	// System.out.println("Loaded Slice");
			   //if (ImageType == ImagePlus.COLOR_RGB)
			   // elem+=2;  // skip the next three
	     }
	 }
     catch(Exception e)
       {
	myv3d.add("South",new Label ("Memoryexception appeared during data setup ! ("+SizeX+", "+SizeY+", "+SizeZ+") \n"));
	System.out.println("Exception appeared during data setup ! ("+SizeX+", "+SizeY+", "+SizeZ+", z: "+z+", ijslice: "+ijslice+") \n");
	System.out.println("Element ("+Elements+", "+elements+", " + elem+", T "+Times+", "+times+", " + t +") \n");
	System.out.println("Exception was : "+e);
        e.printStackTrace();
	myv3d.setVisible(true);
	}
     sizes[3]=Elements;
     sizes[4]=Times;
     
     // initThresh();

     // AdjustThresh(false);
    }

    public void SaveMarkers() {  // loads a marker-file and generates a new set of marker list from it
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
            if (applet instanceof View5D)
	        myurl = new URL(((View5D) applet).getDocumentBase(), markerInfilename);
            else
	     {
		if (IJ.isWindows())
	        myurl = new URL("file:////"+markerInfilename);
		else
	        myurl = new URL("file://"+markerInfilename);

	    }
            System.out.println("Opening Markerfile "+myurl+"\n");
	    InputStream ifs=myurl.openStream();
            StreamTokenizer is = new StreamTokenizer(new BufferedReader(new InputStreamReader(ifs)));
	    is.commentChar('#');
	    is.slashSlashComments(true);
	    is.slashStarComments(true);
	    is.parseNumbers();
	    while (MyMarkers.readline(is,this))   // reads a line and inserts/updates the appropriate marker
		;
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
		applet.add("South",new Label ("IOException:"+e +"\n"));
		applet.add("South",new Label ("Error: Unable to load file  "+markerInfilename+"\n"));
                e.printStackTrace();
		applet.setVisible(true);
		// System.exit(1);
	    }
	catch(Exception e)
	    {
            	System.out.println("In Markerfile "+myurl+"\n");
		applet.add("South",new Label ("Exception appeared during load!\n"+e+"\n"));
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
	    	System.out.println("bread :"+bread);
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
                else
                System.out.println(".. ignored");
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

  public My3DData(My3DData other) {   // operates on the same data but allows different views and projections
    DataToHistogram=other.DataToHistogram;
    applet=other.applet;
    SizeX=other.SizeX; sizes[0]=other.sizes[0];
    SizeY=other.SizeY; sizes[1]=other.sizes[1];
    SizeZ=other.SizeZ; sizes[2]=other.sizes[2];
 	
	if (IJ.isWindows())
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
    PlanesS= new Vector[3];
    PlanesD= new Vector[3];
    PlanesS[0] = new Vector();PlanesS[1] = new Vector();PlanesS[2] = new Vector();
    PlanesD[0] = new Vector();PlanesD[1] = new Vector();PlanesD[2] = new Vector();
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
        MyBundle = new Vector();
        MyTimeProj = new Vector();  // stores arrays[3] of vectors
        MyTimeColorProj = new Vector();
        // something is wrong with the line below
        //MyBundle = (Vector) other.MyBundle.clone();  // clones the full vector with all its elements
        for (int e=0;e < other.Elements;e++)
            MyBundle.addElement(other.BundleAt(e).clone());

        for (int t=0;t < other.Times;t++)
            {
            MyProjections = new Vector[3];    // these manage progections
            MyProjections[0] = new Vector();
            MyProjections[1] = new Vector();
            MyProjections[2] = new Vector();
            for (int e=0;e < other.Elements;e++)
                {
                MyProjections[0].addElement(new ASlice(0,other.ElementAt(e)));
                MyProjections[1].addElement(new ASlice(1,other.ElementAt(e)));
                MyProjections[2].addElement(new ASlice(2,other.ElementAt(e)));
                }
            MyTimeProj.addElement(MyProjections);
            }
    MyElements = (Vector) MyTimes.elementAt(0);
    MyProjections = (Vector []) MyTimeProj.elementAt(0);

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
	if (IJ.isWindows())
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
   
    PlanesS= new Vector[3];
    PlanesD= new Vector[3];
    PlanesS[0] = new Vector();PlanesS[1] = new Vector();PlanesS[2] = new Vector();
    PlanesD[0] = new Vector();PlanesD[1] = new Vector();PlanesD[2] = new Vector();
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
        MyTimes = new Vector();
        MyTimeProj = new Vector();  // stores arrays[3] of vectors
        MyTimeColorProj = new Vector();
        MyBundle = new Vector();
        Elements = 0;
        Times = times;
	for (int t=0; t < times; t++)
        {
            MyElements = new Vector();   // A list of elements is generated for each timepoint
            MyTimes.addElement(MyElements);  // However, all times use the same list of elements.
            MyProjections = new Vector[3];    // these manage progections
            MyProjections[0] = new Vector();
            MyProjections[1] = new Vector();
            MyProjections[2] = new Vector();
            MyTimeProj.addElement(MyProjections);
        }

        MyElements = (Vector) MyTimes.elementAt(0);
        MyProjections = (Vector []) MyTimeProj.elementAt(0);
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

void ToggleOverlayDispl(int val) {
    GetBundleAt(ActiveElement).ToggleOverlayDispl(val);
}

void ToggleMulDispl(int val) {
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

class ImageErr extends TextArea {
    static final long serialVersionUID = 1;
    public ImageErr() {
	// setLayout(new GridLayout(5, 0));
	super("ERROR ! This version of image viewer runs only under Java1.1\n"+
	      "and later versions.\n"+
	      "You are using a browser with Java version "+System.getProperty("java.version")+".\n"+
	      "Plaese update your system to the proper java version and try again."); // ,7,45);
    }
}



class AlternateViewer extends Frame implements WindowListener {
 static final long serialVersionUID = 1;
 My3DData cloned=null;
 Component mycomponent=null;
 Container applet;
    public AlternateViewer(Container myapplet) {
    super("Alternate Viewer");
    applet = myapplet;
    if (myapplet != null)
        setSize(myapplet.getBounds().width,myapplet.getBounds().height);
    else
        setSize(200,200);
    setVisible(true);
    addWindowListener(this); // register this class for handling the events in it
    }

    public AlternateViewer(Container myapplet, int width, int height) {
    super("Alternate Viewer");
    applet = myapplet;
    setSize(width,height);
    setVisible(true);
    addWindowListener(this); // register this class for handling the events in it
    }
      
    public void Assign3DData(Container myapplet, ImgPanel ownerPanel, My3DData cloneddata) {
    cloned = cloneddata; // new My3DData(datatoclone);
    ImgPanel np=new ImgPanel(myapplet,cloneddata);
    if (applet instanceof View5D_)
	   ((View5D_) applet).panels.addElement(np);  // enter this view into the list
    else
	   ((View5D) applet).panels.addElement(np);  // enter this view into the list
    np.OwnerPanel(ownerPanel);
    mycomponent=np;
    np.CheckScrollBar();
    add("Center", np);	
    setVisible(true);
    }

    public void AssignPixelDisplay(PixelDisplay pd) {
    mycomponent=pd;
    pd.setBounds(getBounds());
    add("Center", pd);
    // ((PixelDisplay) mycomponent).c1.myPanel.label.doLayout();
    }

    public void windowActivated(java.awt.event.WindowEvent windowEvent) {
    }
    
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
        cloned=null;
    if (applet instanceof View5D_)
	   ((View5D_) applet).panels.removeElement(mycomponent);  // remove this view from the list
    else
	   ((View5D) applet).panels.removeElement(mycomponent);  // remove this view from the list
    }
    
    public void windowClosing(java.awt.event.WindowEvent windowEvent) {  // Put the window back into place
    if (mycomponent instanceof PixelDisplay)
    {
        ((PixelDisplay) mycomponent).c1.myPanel.label.add(mycomponent);
        // ((PixelDisplay) mycomponent).c1.myPanel.label.preferredSize();
        GridLayout myLayout=new GridLayout(2,2);   // back to the old layout
        ((PixelDisplay) mycomponent).c1.myPanel.label.setLayout(myLayout);
        ((PixelDisplay) mycomponent).c1.myPanel.label.doLayout();
        ((PixelDisplay) mycomponent).c1.myPanel.label.repaint();
    }
                
    if (cloned != null)
        if (cloned.DataToHistogram != null)
        {
            if (cloned.DataToHistogram.MyHistogram == cloned)
                    cloned.DataToHistogram.MyHistogram = null;
        }
    setVisible(false);
    }
    
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {
    }
    
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {
    // setVisible(true);
    }
    
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {
    // setVisible(false);
    }
    
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {
    setVisible(true);
    }
}



/* The code below is necessary to include the software as a plugin into ImageJ */
public class View5D_ extends PlugInFrame implements PlugIn, WindowListener {
    static final long serialVersionUID = 1;
	// Panel panel;
	int previousId;
	ImagePlus imp;
	ImageProcessor ip;
  	My3DData data3d;  // Takes care of all datasets
  	TextArea myLabel;
	MenuBar IJMenu; // to save it
    Vector panels=new Vector();  // Keeps track of all the views. Sometimes this information is needed to send the updates.
        
    String [] DimensionChoices={"ZCT","CZT","ZTC","TZC","CTZ","TCZ"};
    String [] AppendChoices={"Color","Time"};
    boolean AspectFromData=false;
    public int SizeX=0,SizeY=0,SizeZ=0,Elements=1,Times=1,DimensionOrder=0,AppendTo=0;

    public void windowActivated(java.awt.event.WindowEvent windowEvent) {
    }
    
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
        setVisible(false);
    }
    
    public void windowClosing(java.awt.event.WindowEvent windowEvent) {  
    	// System.out.println("Plugin closing\n");
    	data3d.cleanup();
        removeAll();
        System.gc();    	
        setVisible(false);
    }
    
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {
    }
    
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {
    // setVisible(true);
    }
    
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {
    // setVisible(false);
    }
    
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {
    setVisible(true);
    }
    
    
    public void UpdatePanels()  // update all panels
        {
            for (int i=0;i<panels.size();i++)
                ((ImgPanel) panels.elementAt(i)).c1.UpdateAllNoCoord();
        }
        
        
   void ImportDialog() {  // allows the user to define the import parameters
            ANGenericDialog md= new ANGenericDialog("Import Data");
            md.addChoice("Dimension Order",DimensionChoices,DimensionChoices[DimensionOrder]);
            md.addMessage("Size X: "+SizeX+", Size Y: "+SizeY);
            md.addNumericFields("SliceCount, Size Z: ",SizeZ,3,3);
            md.addNumericFields("TimeCount, Numer of Timepoints: ",Times,3,3);
            md.addNumericFields("Colors: ",Elements,3,3);
            if (data3d != null)
            	md.addChoice("append along",AppendChoices,AppendChoices[AppendTo]);
            md.addCheckbox("Lock Aspects with data scaling",AspectFromData);
            md.showDialog();
            if (! md.wasCanceled())
            {
                DimensionOrder=md.getNextChoiceIndex();
                SizeZ=(int) md.getNextNumber();
                Times=(int) md.getNextNumber();
                Elements=(int) md.getNextNumber();
                if (data3d != null)
                	AppendTo=md.getNextChoiceIndex();
                AspectFromData=md.getNextBoolean();
            }
        }
        

   public boolean LoadImg(int NumZ) {
  		int   redEl=-1,greenEl=-1,blueEl=-1;
                
		imp = WindowManager.getCurrentImage();  // remember image
                if (imp == null)
                {
                  return false;
                }
		ip = imp.getProcessor();  // and its processor

		// ImageStack ims=imp.getStack();  // get the stack associated to this image
		ImageStack ims=imp.getImageStack();  // get the stack associated to this image

		//if imp instanceof Image5D
		//	ImageStack ims=imp.getImageStack();  // get the stack associated to this image
		//end
		
		SizeX = ims.getWidth();
		SizeY = ims.getHeight();
              
		if (NumZ > 0) 
                {
                    SizeZ=1; 
                    Elements=ims.getSize();
                }
        else
                {
                    SizeZ = ims.getSize();
                    if (SizeZ > 1)
                    {
                    	int SizeZ2=1;
                    	try {
                    		SizeZ2=imp.getNSlices();
                            Times= imp.getNFrames();
                            Elements = SizeZ / (SizeZ2 * Times);
                            SizeZ=SizeZ2;
                    	}
                    		catch(Exception e) {  // in case the version of ImageJ is too old to support it.
                    	
                    		SizeZ2= (int) IJ.getNumber("SliceCount unknown while importing stack. Number of slices: ",SizeZ);
                            Times= SizeZ / SizeZ2;
                            if (Times > 1)
                            Times= (int) IJ.getNumber("TimeCount unknown while importing stack. Number of times: ",Times);
                            Elements = SizeZ / (SizeZ2 * Times);
                            SizeZ = SizeZ2;
                    		}
                    }
                }
		if (SizeZ > 1 || Elements > 1 || Times > 1 || data3d != null)
			ImportDialog();
		
		int HistoX = 0;
        int HistoY = -1;
        int HistoZ = -1;
		int DataType=AnElement.ByteType;
                int NumBytes=1, NumBits=8;
		int ImageType=imp.getType();
		switch (ImageType) {
			case ImagePlus.COLOR_256: DataType=AnElement.ByteType;NumBytes=1;NumBits=8;break;    // 8-bit RGB with lookup
			case ImagePlus.COLOR_RGB: DataType=AnElement.ByteType;NumBytes=1;NumBits=8;Elements=3; break;    // 24-bit RGB 
			case ImagePlus.GRAY8: DataType=AnElement.ByteType;NumBytes=1;NumBits=8;break;    // 8-bit grayscale
			case ImagePlus.GRAY16: DataType=AnElement.ShortType;NumBytes=2;NumBits=16;break;    // 16-bit grayscale
			case ImagePlus.GRAY32: DataType=AnElement.FloatType;NumBytes=4;NumBits=32;break;    // float image
		}
                double[] Scales = new double[5];
                for (int j=0;j<5;j++) Scales[j]=1.0;                
                double[] Offsets = new double[5];
                for (int j=0;j<5;j++) Offsets[j]=0.0;
                double ScaleV=1.0,OffsetV=0.0;
                String [] Names = new String[5];
                String [] Units = new String[5];
                Names[0] = "X";Names[1] = "Y";Names[2] = "Z";Names[3] = "Elements";Names[4] = "Time";
                Units[0] = "pixels";Units[1] = "ypixels";Units[2] = "zpixels";Units[3] = "elements";Units[4] = "time";
                String NameV = "intensity";
                String UnitV = "a.u.";

                ij.measure.Calibration myCal=imp.getCalibration();  // retrieve the spatial information
         if (myCal.scaled())
                {
                    Scales[0]=myCal.pixelWidth;
                    Scales[1]=myCal.pixelHeight;
                    Scales[2]=myCal.pixelDepth;
                    Offsets[0]=myCal.xOrigin;
                    Offsets[1]=myCal.yOrigin;
                    Offsets[2]=myCal.zOrigin;
                                
                    if (myCal.getUnits() != "")
                    {
                        Units[0] = myCal.getUnits();
                        Units[1] = myCal.getUnits();
                        Units[2] = myCal.getUnits();
                    }
                                
                    if (myCal.frameInterval != 0 && Scales[2] == 0) 
                    {
                        Scales[2]=myCal.frameInterval;
                        Units[2] = "seconds";
                    }
                }

         if (myCal.calibrated())
                {
                    NameV="calib. int.";
                    UnitV=myCal.getValueUnit();
                    OffsetV=myCal.getCValue(0);
                    ScaleV=myCal.getCValue(1.0)-OffsetV;
                }

        if (data3d == null)  // ist there no existing data?
			{
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
                        data3d = new My3DData(this,SizeX,SizeY,SizeZ,
                                    Elements,Times,
                                    redEl,greenEl,blueEl,
                                    HistoX,HistoY,HistoZ,DataType,NumBytes,NumBits,
                                    Scales,Offsets,
                                    ScaleV, OffsetV, Names, Units);
                        data3d.DimensionOrder=DimensionOrder;
                        data3d.AppendTo=AppendTo;
                        
                	for (int e=0;e<Elements;e++)
                      	 data3d.SetValueScale(e,ScaleV,OffsetV,NameV,UnitV);
			}
		else
			{
			if (SizeX != data3d.SizeX || SizeY != data3d.SizeY || SizeZ != data3d.SizeZ)
				{
                                    IJ.error("New image to load does not correspond in size! ...Not loading it.");
                                    System.out.println("Error! Size of new image to load does not correspond to old image\n");
                                    return false;
				}
			if (AppendTo == 0)  // append to elements
			  for (int i = 0;i< Elements;i++)
				{
				   int nr=data3d.GenerateNewElement(DataType,NumBytes,NumBits,Scales,Offsets,ScaleV, OffsetV, Names, Units);
				   data3d.SetValueScale(nr,ScaleV,OffsetV,NameV,UnitV);
				}
			else
			{
				  for (int i = 0;i< Times;i++)  // for all new time points do
					{
					   // int nr=data3d.GenerateNewTime(DataType,NumBytes,NumBits,Scales,Offsets,ScaleV, OffsetV, Names, Units);
					   //data3d.SetValueScale(nr,ScaleV,OffsetV,NameV,UnitV);
					}
			   // if (data3d.Times - Times <= 1)
	               ((ImgPanel) panels.elementAt(0)).CheckScrollBar();
			}
			}
                
		data3d.SetUp(DataType,this,Elements,Times,ImageType);   // here the data is really loaded
                //System.out.println("Check1\n");
                if (Elements == 1 && data3d.Elements <= 3 && data3d.Elements > 1)
                {
                    data3d.ActiveElement=0;
                    data3d.MarkChannel(0);
                    data3d.MarkAsHistoDim(0);
                    data3d.ActiveElement=1;
                    data3d.MarkChannel(1);
                    data3d.MarkAsHistoDim(1);
                    if (data3d.Elements == 3)
                    {
                        data3d.ActiveElement=2;
                        data3d.MarkChannel(2);
                    }
                }
                if ((data3d.Elements > 1 && data3d.Elements < 5) || redEl >= 0)
		{
                    data3d.ToggleColor(true);  // switch to Color
		}
                else
                {
                    data3d.ToggleColor(false);  // switch to BW
                }
                data3d.AdjustThresh(true); // .initThresh();
                
                //System.out.println("Check2\n");
		if (data3d.HistoY<0 && data3d.Elements > 1)
                    {//System.out.println("Check2b\n");
                    data3d.HistoY = 1;}

                if (! panels.isEmpty())
                    {
                	CheckforLUT();
                    VerifyAspect();
                    }

                // System.out.println("Should lock Aspects\n");
                return true;
	}
        
        public void CheckforLUT()
        {
		imp = WindowManager.getCurrentImage();  // remember image
                if (imp == null)
                {
                  return;
                }
		int ImageType=imp.getType();
                //IJ.showMessage("Checking for LUT!");
                //IJ.showMessage("ImageType is: "+ImageType);
                LookUpTable lt = imp.createLut();
                if (ImageType == ij.ImagePlus.COLOR_256 || 
		    (ImageType != ij.ImagePlus.COLOR_RGB && ! lt.isGrayscale()) ||
		     imp.isInvertedLut())
                // if (! lt.isGrayscale())
                {
                    // IJ.showMessage("Found LUT, importing ...");
                    int lastLUT=data3d.AddLookUpTable(lt.getMapSize(),lt.getReds(),lt.getGreens(),lt.getBlues());
                    NumberFormat  nf = java.text.NumberFormat.getNumberInstance(Locale.US);

                    String LUTName="User defined "+nf.format(lastLUT-Bundle.ElementModels+1);
                    ((ImgPanel) panels.firstElement()).c1.label.PixDisplay.AddColorMenu(LUTName,lastLUT);
                    data3d.SetColorModelNr(data3d.ActiveElement,lastLUT);
		    if (! data3d.SetThresh(0,lt.getMapSize()-1))  // Set to the correct initial thresholds
		    	 data3d.InvalidateSlices();

                }
        }

        public void VerifyAspect(){
        if (AspectFromData)
        {
            System.out.println("Locking Aspects\n");
            for (int i=0;i<panels.size();i++) 
            {   // c2.scale == X is the master
                // System.out.println("Locking Aspect on panel " + i + "\n");
            	((ImgPanel) panels.elementAt(i)).c1.AspectFromView=true;
            	((ImgPanel) panels.elementAt(i)).c2.AspectFromView=true;
            	((ImgPanel) panels.elementAt(i)).c3.AspectFromView=true;
                ((ImgPanel) panels.elementAt(i)).c1.AspectLocked.setState(true);
                ((ImgPanel) panels.elementAt(i)).c2.AspectLocked.setState(true);
                ((ImgPanel) panels.elementAt(i)).c3.AspectLocked.setState(true);
            }
        }
        }

	
	public View5D_() {
		super("5D Viewer");
		data3d=null;
		if (! LoadImg(0))
                {
                  IJ.showMessage("Error: View5D needs a loaded image in ImageJ, when starting!");
                  return;
                }
		ImgPanel mypan=new ImgPanel(this,data3d);
                panels.addElement(mypan);  // enter this view into the list
        CheckforLUT();
        VerifyAspect();

		setLayout(new BorderLayout());
		add("Center", mypan);
                myLabel=new TextArea("5D-viewer Java Applet by Rainer Heintzmann, [press '?' for help]",1,76,TextArea.SCROLLBARS_NONE);
                add("North", myLabel);
		mypan.CheckScrollBar();
                setMenuBar(mypan.MyMenu);
                setVisible(true);
                
		pack();
		GUI.center(this);
		//show();
        setVisible(true);

		// data3d.initThresh();
		mypan.InitScaling();
		addWindowListener(this);
    }

    public ImagePlus GetImp() { return imp;}
    public ImageProcessor GetIp() {return ip;}

    /*  // Why does the below not work??
    public void focusGained(java.awt.event.FocusEvent e)
      {
          if (! panels.isEmpty())
	  {
		ImgPanel mypan=(ImgPanel) panels.firstElement();
		IJMenu=IJ.getInstance().getMenuBar();
		IJ.getInstance().setMenuBar(mypan.MyMenu);
	  }
	}
    
    public void focusLost(java.awt.event.FocusEvent e)
      {
          if (IJMenu != null)
	  {
		IJ.getInstance().setMenuBar(IJMenu);
	  }
	}
	*/
	
    void showAbout() {
              IJ.showMessage("About View5D, Version V1.2.04",
	      "5D-Viewer by Rainer Heintzmann\nKing's College London, London, U.K.\n"+
              "rainer.heintzmann@kcl.ac.uk\n"+
              "http://www.nanoimaging.de/View5D/\n"+
	      "use mouse click for changing slices, \ndrag images, zoom by typing 'A' and 'a'\n"+
	      "'next page' and 'prev. page'  for changing perpendicular slice ,\n'<' and '>' for perpendicular magnificaltion\n"+
	      "type 'i' for init, 'c' for change ColorMap, '1,2' and '3,4' for Threshold" );
        }                                                                                                                 
}

// insert : "public" before this to make it an applet
class View5D extends Applet {
  static final long serialVersionUID = 1;
  public int SizeX=0,SizeY=0,SizeZ=0;
  int Elements=1,Times=1,defaultColor0=-1,
      redEl=-1,greenEl=-1,blueEl=-1;
  My3DData data3d;
  TextArea myLabel;
  ImgPanel mypan=null;
  Vector panels=new Vector();  // Keeps track of all the views. Sometimes this information is needed to send the updates.
  
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
	 if (System.getProperty("java.version").compareTo("1.1") < 0)  // this viewer should work from version 1.1 on
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

/*
class ImageHelp extends TextArea {
    public ImageHelp() {
	// setLayout(new GridLayout(5, 0));
	super("5D-viewer Java Applet by Rainer Heintzmann\nMPI for Biophys. Chemistry (rheintz@gwdg.de)\n"+
	      "Use mouse click for changing slices, \ndrag images, zoom by typing 'A' and 'a'\n"+
	      "Arrow keys for fine control of slicing\n"+
	      "'nexp page' and 'prev. page'  for changing perpendicular slice ,\n'<' and '>' for perpendicular magnificaltion\n"+
	      "'i' for init view, 'c' for change ColorMap, \n"+
	      "'m' for add and 'M' for remove marker\n"+
	      "'d' for mathematically adding to last element\n"+
	      "1,2' and '3,4' for Threshold, 't' and 'T' for automatic contrast adjust\n"+
	      "'e' : toggle elements (if present), 'C' : toggle multicolor display, 'r','g','b' : select element for respective display\n"+
	      "'R','G','B' : clear respective channel, 'p','P': Toggle Projections (MIP, Avg)"); // ,7,45);
    }
}*/

class PlotInfo {
    public double MaxVal=1.0,MinVal=0.0,ValScale=1.0,newMinVal=0.0,newMaxVal=1.0;
}

class PixelDisplay extends Panel implements MouseListener,ImageObserver,KeyListener {  // A panel displaying the multi-element contens of a single pixel
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
	if (false) // (yprev > r.height/2)  // lower half  -> change also the RGB assignment
	    {
		int MyMask=e.getModifiers();
		if (MyMask == InputEvent.BUTTON1_MASK)
		    data3d.MarkChannel(0);
		if (MyMask == InputEvent.BUTTON2_MASK)
		    data3d.MarkChannel(1);
		if (MyMask == InputEvent.BUTTON3_MASK)
		    data3d.MarkChannel(2);
	    }
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

class PositionLabel extends Panel implements MouseListener{
    static final long serialVersionUID = 1;
    ImageCanvas c1,c2,c3;
    My3DData data3d;
    Label l1,l2,l3,l4,l5,l6;
    PixelDisplay PixDisplay;
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
      String newtext="Java 5D image viewer, Version V1.2.04 by Rainer Heintzmann,\nKCL, London (rainer.heintzmann@kcl.ac.uk)\n\n"+
              "Right-click for menu\nUse mouse click for changing slices, \n"+
              "Shift and mouse-drag for square ROIs, Ctrl and mouse-drag for multiple line ROIs\ndrag images, zoom by typing 'A' and 'a' or into ROI by 'Z'\n"+
	      "Arrow keys for fine control of slicing\n"+
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


// This class manages constructing the application
class ImgPanel extends Panel {
    static final long serialVersionUID = 1;

    Container applet;
    ImgPanel    DataPanel=null; // just for the case, that this (histogram) window was generated by another owner
    My3DData  data3d;
    ImageCanvas c1,c2,c3;
    PositionLabel label;
    boolean     ROIstarted = false;   // this is stored for the sqare ROIs in the Canvasses
    APoint DraggedMarker = null;   // a ROI currently being dragged
    APoint SavedMarker = null;   // a ROI currently being dragged
    MenuBar  MyMenu;
    boolean ScrollbarPresent=false;
    double [] OldOffset;  // to remember the active offsets, when changing elements or time
   
    public void InitScaling() {
	c2.InitScaling();
	c3.InitScaling();
	c1.InitScaling();
	c3.CalcPrev();
	c3.UpdateAllNoCoord();
    }
    public void OwnerPanel(ImgPanel owner) {
       DataPanel=owner;
        }
  
   public void CheckScrollBar() {
                if (data3d.Times > 1)
                {
                	Scrollbar Slider=label.TimeScrollbar;
                	if (! ScrollbarPresent ) {
                    Slider=new Scrollbar(Scrollbar.VERTICAL, 0, 1, 0, data3d.Times);
                    ScrollbarPresent=true;
                    label.TimeScrollbar=Slider;  // that it will be moved when browsing though the data
                    add("East",Slider);
                    //data3d.ShowAllSlices=true;
                    data3d.TrackDirection=4;
                    if (data3d.SizeZ > 1)
                    {
                        data3d.SearchZ=1;
                        data3d.COMZ=1;
                    }
                    Slider.setBlockIncrement(data3d.Times / 10 + 1);
                    Slider.addAdjustmentListener(c1);
                	}
                	else
                		Slider.setMaximum(data3d.Times);
                    this.doLayout();
                }
                else
                	if (data3d.Elements > 5)
                        data3d.TrackDirection=3;
 }

    public void setPositions(APoint ap) {
    if (ap != null)
    	{
       	APoint p=data3d.LimitPoint(ap);
    	//System.out.println("Setting Position: (" + p.coord[0] +", " + p.coord[1] +", "+ p.coord[2] +", "+ p.coord[3] +", "+ p.coord[4] +")\n");
       	c1.PositionValue= (int) (p.coord[2]+0.5);
       	c2.PositionValue= (int) (p.coord[0]+0.5);
       	c3.PositionValue= (int) (p.coord[1]+0.5);
       	data3d.setElement((int) (p.coord[3]+0.5));
       	data3d.setTime((int) (p.coord[4]+0.5));
       }
    }

    public float[] getPositions() {
           	float [] pos = new float[5];
           	pos[0]=(float) c2.PositionValue;
           	pos[1]=(float) c3.PositionValue;
           	pos[2]=(float) c1.PositionValue;
           	pos[3]=data3d.ActiveTime;
           	pos[4]=data3d.ActiveElement;
           return pos;
        }

    public void RememberOffset() {
    	OldOffset = data3d.ElementAt(data3d.ActiveElement).DisplayOffset;
    }
    
    public void LimitPositions() {
    	if (c1.PositionValue < 0) c1.PositionValue=0;
    	if (c2.PositionValue < 0) c2.PositionValue=0;
    	if (c3.PositionValue < 0) c3.PositionValue=0;
    	if (c1.PositionValue >= c1.getMaxPos()) c1.PositionValue=c1.getMaxPos()-1;
    	if (c2.PositionValue >= c2.getMaxPos()) c2.PositionValue=c2.getMaxPos()-1;
    	if (c3.PositionValue >= c3.getMaxPos()) c3.PositionValue=c3.getMaxPos()-1;
    }

    public void AdjustOffset() {
    	//return;
    	double [] NewOffset = data3d.ElementAt(data3d.ActiveElement).DisplayOffset;
    	if (OldOffset != null)
    	{
       	c2.PositionValue += (int) ((OldOffset[0]-NewOffset[0]));
       	c3.PositionValue += (int) ((OldOffset[1]-NewOffset[1]));
       	c1.PositionValue += (int) ((OldOffset[2]-NewOffset[2]));
       	LimitPositions();
    	}
    }

    public void AdvancePos() {
    	// System.out.println("Advancing direction " + data3d.TrackDirection);
    	switch (data3d.TrackDirection){
    	case 0:
           	c2.PositionValue += 1;
           	c2.PositionValue = (c2.PositionValue % c2.getMaxPos());
           	break;
    	case 1:
           	c3.PositionValue += 1;
           	c3.PositionValue = (c3.PositionValue % c3.getMaxPos());
           	break;
    	case 2:
           	c1.PositionValue += 1;
           	c1.PositionValue = (c1.PositionValue % c1.getMaxPos());
           	break;
    	case 3:
           	data3d.setElement((data3d.ActiveElement + 1) % data3d.Elements);
    	case 4:
           	data3d.setTime((data3d.ActiveTime + 1) % data3d.Times);
    	}
        }
    
    void AddPopupMenu(Menu aMenu, boolean doSubMenu) {
	if (doSubMenu)  // a real applet -> context menus are required
	{
            c1.MyPopupMenu.add(aMenu);
            //c2.MyPopupMenu.add(aMenu);  // can be debated whether all caveses should have these general menues
            //c3.MyPopupMenu.add(aMenu);  // can be debated whether all caveses should have these general menues
	}
	else
	{    
        MyMenu.add(aMenu);
	}
    }
    
    public ImgPanel(Container app,My3DData mydata) {
	applet = app;
	setLayout(new BorderLayout());
	Panel grid = new Panel();
	grid.setLayout(new GridLayout(2, 2));
	add("Center", grid);

	// Only one 3D Data class:
	data3d = mydata;
	// Image joe = Toolkit.getDefaultToolkit().getImage("./iap.gif");
	// Image joe = applet.getImage(applet.getDocumentBase(), "iap.gif");
	try {
	c1 = new ImageCanvas(applet,this,data3d,2,"XY");
        c1.AspectLocked.setState(true);
	c2 = new ImageCanvas(applet,this,data3d,0,"ZY");
	c3 = new ImageCanvas(applet,this,data3d,1,"XZ");
	} catch(Exception e)
	    {
		applet.add("South",new Label ("Caught Exception:"+e.getMessage()));
		applet.setVisible(true);
		e.printStackTrace();
	    }
	c2.TakeOtherCanvas1(c1);
	c2.TakeOtherCanvas2(c3);
	c3.TakeOtherCanvas1(c2);
	c3.TakeOtherCanvas2(c1);
	c1.TakeOtherCanvas1(c2);
	c1.TakeOtherCanvas2(c3);

	grid.add(c1);
	grid.add(c2);
	grid.add(c3);

        MyMenu =new MenuBar();  
        
        // MenuComponent AMenu = MyMenu;
        // All the events are actually processed by the c1 canvas
        boolean doSubMenu=(applet instanceof View5D);

	if (applet instanceof View5D_)
		if (IJ.isMacintosh())
			doSubMenu=true;
		
        Menu SubMenu = new Menu("General",false);  // can eventually be dragged to the side
        AddPopupMenu(SubMenu,doSubMenu);

        MenuItem tmp;
	tmp = new MenuItem("Help [?]");
	tmp.addActionListener(new MyMenuProcessor(c1,'?')); SubMenu.add(tmp);
	tmp = new MenuItem("Exit [$]");
	tmp.addActionListener(new MyMenuProcessor(c1,'$')); SubMenu.add(tmp);
	tmp = new MenuItem("Readmode (Complex only) [^]");
	tmp.addActionListener(new MyMenuProcessor(c1,'^')); SubMenu.add(tmp);

        SubMenu = new Menu("Import/Export",false);  // can eventually be dragged to the side
        AddPopupMenu(SubMenu,doSubMenu);
        
	tmp = new MenuItem("Reload data [l]");
	tmp.addActionListener(new MyMenuProcessor(c1,'l')); SubMenu.add(tmp);
        tmp = new MenuItem("Reload markers [L]");
	tmp.addActionListener(new MyMenuProcessor(c1,'L')); SubMenu.add(tmp);
        tmp = new MenuItem("Export this element [X] (only in ImageJ)");
        tmp.addActionListener(new MyMenuProcessor(c1,'X')); SubMenu.add(tmp);
        tmp = new MenuItem("Spawn Viewer [s]");
	tmp.addActionListener(new MyMenuProcessor(c1,'s')); SubMenu.add(tmp);

        SubMenu = new Menu("Measuring & Markers",false);  // can eventually be dragged to the side
        AddPopupMenu(SubMenu,doSubMenu);
        
	tmp = new MenuItem("Set Axes Units and Scalings [N]");
	tmp.addActionListener(new MyMenuProcessor(c1,'N')); SubMenu.add(tmp);
        tmp = new MenuItem("set marker [m]");
	tmp.addActionListener(new MyMenuProcessor(c1,'m')); SubMenu.add(tmp);
    tmp = new MenuItem("set split marker [\\]");
	tmp.addActionListener(new MyMenuProcessor(c1,'\\')); SubMenu.add(tmp);
        tmp = new MenuItem("delete active marker [M]");
	tmp.addActionListener(new MyMenuProcessor(c1,'M')); SubMenu.add(tmp);
        tmp = new MenuItem("delete trailing markers [Q]");
	tmp.addActionListener(new MyMenuProcessor(c1,'Q')); SubMenu.add(tmp);
        tmp = new MenuItem("tag/untag marker [&]");
	tmp.addActionListener(new MyMenuProcessor(c1,'&')); SubMenu.add(tmp);
        tmp = new MenuItem("new marker list [k]");
	tmp.addActionListener(new MyMenuProcessor(c1,'k')); SubMenu.add(tmp);
        tmp = new MenuItem("delete marker list [K]");
	tmp.addActionListener(new MyMenuProcessor(c1,'K')); SubMenu.add(tmp);
        tmp = new MenuItem("activate next marker [0]");
	tmp.addActionListener(new MyMenuProcessor(c1,'0')); SubMenu.add(tmp);
        tmp = new MenuItem("activate previous marker [9]");
	tmp.addActionListener(new MyMenuProcessor(c1,'9')); SubMenu.add(tmp);
        tmp = new MenuItem("activate next marker list [j]");
	tmp.addActionListener(new MyMenuProcessor(c1,'j')); SubMenu.add(tmp);
        tmp = new MenuItem("activate previous marker list [J]");
	tmp.addActionListener(new MyMenuProcessor(c1,'J')); SubMenu.add(tmp);
        tmp = new MenuItem("toggles the marker color of current list [w]");
	tmp.addActionListener(new MyMenuProcessor(c1,'w')); SubMenu.add(tmp);
        tmp = new MenuItem("auto-track marker [W]");
	tmp.addActionListener(new MyMenuProcessor(c1,'W')); SubMenu.add(tmp);
    	tmp = new MenuItem("align display to marker track [|]");
	tmp.addActionListener(new MyMenuProcessor(c1,'|')); SubMenu.add(tmp);
		tmp = new MenuItem("reset track alignments  [{]");
	tmp.addActionListener(new MyMenuProcessor(c1,'{')); SubMenu.add(tmp);
        tmp = new MenuItem("subtract track from data [#]");
	tmp.addActionListener(new MyMenuProcessor(c1,'#')); SubMenu.add(tmp);
        tmp = new MenuItem("Show detailed marker menu [n]");
	tmp.addActionListener(new MyMenuProcessor(c1,'n')); SubMenu.add(tmp);
		tmp = new MenuItem("Marker list property menu [}]");
	tmp.addActionListener(new MyMenuProcessor(c1,'}')); SubMenu.add(tmp);
        tmp = new MenuItem("Reload markers [L]");
	tmp.addActionListener(new MyMenuProcessor(c1,'L')); SubMenu.add(tmp);

        SubMenu = new Menu("Color",false);  // can eventually be dragged to the side
        AddPopupMenu(SubMenu,doSubMenu);
        
	tmp = new MenuItem("Toggle multicolor overlay [C]");
	tmp.addActionListener(new MyMenuProcessor(c1,'C')); SubMenu.add(tmp);
        tmp = new MenuItem("Next color map for element [c]");
	tmp.addActionListener(new MyMenuProcessor(c1,'c')); SubMenu.add(tmp);
        tmp = new MenuItem("Next inverse color map [d]");
	tmp.addActionListener(new MyMenuProcessor(c1,'d')); SubMenu.add(tmp);
        tmp = new MenuItem("Toggle over/underflow [o]");
	tmp.addActionListener(new MyMenuProcessor(c1,'o')); SubMenu.add(tmp);
        tmp = new MenuItem("Toggle logarithmic scale [O]");
	tmp.addActionListener(new MyMenuProcessor(c1,'O')); SubMenu.add(tmp);
        tmp = new MenuItem("Adjust threshold element [t]");
	tmp.addActionListener(new MyMenuProcessor(c1,'t')); SubMenu.add(tmp);
        tmp = new MenuItem("Adjust all thresholds [T]");
	tmp.addActionListener(new MyMenuProcessor(c1,'T')); SubMenu.add(tmp);
        tmp = new MenuItem("Coarse raise lower display threshold [1]");
	tmp.addActionListener(new MyMenuProcessor(c1,'1')); SubMenu.add(tmp);
        tmp = new MenuItem("Coarse decrease lower display threshold [2]");
	tmp.addActionListener(new MyMenuProcessor(c1,'2')); SubMenu.add(tmp);
        tmp = new MenuItem("Coarse raise upper display threshold [3]");
	tmp.addActionListener(new MyMenuProcessor(c1,'3')); SubMenu.add(tmp);
        tmp = new MenuItem("Coarse decrease upper display threshold [4], use 5-8 for fine tuning");
	tmp.addActionListener(new MyMenuProcessor(c1,'4')); SubMenu.add(tmp);
        tmp = new MenuItem("Transfer display thresholds to data thresholds [!]");
	tmp.addActionListener(new MyMenuProcessor(c1,'!')); SubMenu.add(tmp);
        tmp = new MenuItem("Advance element [e]");
	tmp.addActionListener(new MyMenuProcessor(c1,'e')); SubMenu.add(tmp);
        tmp = new MenuItem("Devance element [E]");
	tmp.addActionListener(new MyMenuProcessor(c1,'E')); SubMenu.add(tmp);
        tmp = new MenuItem("Define underflow gate [u]");
	tmp.addActionListener(new MyMenuProcessor(c1,'u')); SubMenu.add(tmp);
        tmp = new MenuItem("Toggle defined underflow gate [U]");
	tmp.addActionListener(new MyMenuProcessor(c1,'U')); SubMenu.add(tmp);
        tmp = new MenuItem("Red glow colormap [R]");
	tmp.addActionListener(new MyMenuProcessor(c1,'R')); SubMenu.add(tmp);
        tmp = new MenuItem("Gray colormap [G]");
	tmp.addActionListener(new MyMenuProcessor(c1,'G')); SubMenu.add(tmp);
        tmp = new MenuItem("Rainbow colormap [B]");
	tmp.addActionListener(new MyMenuProcessor(c1,'B')); SubMenu.add(tmp);
        tmp = new MenuItem("Red colomap [r]");
	tmp.addActionListener(new MyMenuProcessor(c1,'r')); SubMenu.add(tmp);
        tmp = new MenuItem("Green colormap [g]");
	tmp.addActionListener(new MyMenuProcessor(c1,'g')); SubMenu.add(tmp);
        tmp = new MenuItem("Blue colormap [b]");
	tmp.addActionListener(new MyMenuProcessor(c1,'b')); SubMenu.add(tmp);
        tmp = new MenuItem("Show in overlay [v]");
	tmp.addActionListener(new MyMenuProcessor(c1,'v')); SubMenu.add(tmp);
        tmp = new MenuItem("Show in multiplicative Overlay [V]");
	tmp.addActionListener(new MyMenuProcessor(c1,'V')); SubMenu.add(tmp);

	SubMenu = new Menu("ROIs",false);  // can eventually be dragged to the side
        AddPopupMenu(SubMenu,doSubMenu);
        tmp = new MenuItem("Toggle rectangular ROIs [S]");
	tmp.addActionListener(new MyMenuProcessor(c1,'S')); SubMenu.add(tmp);
        tmp = new MenuItem("Extract with ROI [Y]");
	tmp.addActionListener(new MyMenuProcessor(c1,'Y')); SubMenu.add(tmp);

        SubMenu = new Menu("Processing",false);  // can eventually be dragged to the side
        AddPopupMenu(SubMenu,doSubMenu);
	
        tmp = new MenuItem("Clone element with upcast to float [f]");
	tmp.addActionListener(new MyMenuProcessor(c1,'f')); SubMenu.add(tmp);
        tmp = new MenuItem("Clone displayed element as short [F]");
	tmp.addActionListener(new MyMenuProcessor(c1,'f')); SubMenu.add(tmp);
        tmp = new MenuItem("Mathematically add gate element [+]");
	tmp.addActionListener(new MyMenuProcessor(c1,'+')); SubMenu.add(tmp);
        tmp = new MenuItem("Mathematically subtract from gate element [-]");
	tmp.addActionListener(new MyMenuProcessor(c1,'-')); SubMenu.add(tmp);
        tmp = new MenuItem("Mathematically multiply with gate element [*]");
	tmp.addActionListener(new MyMenuProcessor(c1,'*')); SubMenu.add(tmp);
        tmp = new MenuItem("Ratio with gate element [/]");
	tmp.addActionListener(new MyMenuProcessor(c1,'/')); SubMenu.add(tmp);
        tmp = new MenuItem("Define this element offset from ROI mean [_]");
	tmp.addActionListener(new MyMenuProcessor(c1,'_')); SubMenu.add(tmp);
        tmp = new MenuItem("Delete active element [D]");
	tmp.addActionListener(new MyMenuProcessor(c1,'D')); SubMenu.add(tmp);

	SubMenu = new Menu("Histograms",false);  // can eventually be dragged to the side
        AddPopupMenu(SubMenu,doSubMenu);
        
	tmp = new MenuItem("Select for histogram X-axis [x]");
        tmp.addActionListener(new MyMenuProcessor(c1,'x')); SubMenu.add(tmp);
        tmp = new MenuItem("Select for histogram Y-axis [y]");
	tmp.addActionListener(new MyMenuProcessor(c1,'y')); SubMenu.add(tmp);
        tmp = new MenuItem("Select for histogram Z-axis [z]");
	tmp.addActionListener(new MyMenuProcessor(c1,'z')); SubMenu.add(tmp);
        tmp = new MenuItem("Histogram (<= 3 dimensional) / apply histogram ROI to data [h]");
	tmp.addActionListener(new MyMenuProcessor(c1,'h')); SubMenu.add(tmp);
        tmp = new MenuItem("Force new histogram window [H]");
	tmp.addActionListener(new MyMenuProcessor(c1,'H')); SubMenu.add(tmp);

        label = new PositionLabel("View5D initialization.",c1,c2,c3,data3d);
        grid.add(label);

	c1.ConnectLabel(label);
	c2.ConnectLabel(label);
	c3.ConnectLabel(label);
	setBounds(0, 0, 20, 20);
        //c1.InitScaling();
	c1.UpdateAllNoCoord();
    }
}

// Now the Action Listener processing all menu event by being associated to a characted
class MyMenuProcessor implements ActionListener {
    ImageCanvas mycanvas;
    PixelDisplay mypix;
    My3DData data3d;
    boolean ColorMapSelector=false;
    char mykey;
    int mycolor=0;
    boolean inverse=false;
    public MyMenuProcessor(ImageCanvas myp,char key)
    {
	mycanvas=myp;
	mypix=null;
	data3d=null;
	mykey=key;
    }
    public MyMenuProcessor(PixelDisplay myp,char key)
    {
	mypix=myp;
	mycanvas=null;
	data3d=null;
	mykey=key;
    }
    public MyMenuProcessor(PixelDisplay myp,ImageCanvas mypc, boolean inv, int colormap)
    {
	data3d=myp.data3d;
	mycanvas=mypc;
	mypix=myp;
	mycolor=colormap;
        inverse=inv;
    }
    
    public void  actionPerformed(ActionEvent e) 
    {
	// System.out.println("Action: :"+e);
        if (data3d != null)
        {
            if (inverse)
                data3d.InvertCMap();
            data3d.ToggleModel(mycolor);
            if (inverse)
                data3d.InvertCMap();
            mycanvas.UpdateAll();
            mypix.CoordinatesChanged();
        }
        else if (mycanvas != null)
            mycanvas.ProcessKey(mykey);
        else
        if (mypix != null)
            mypix.ProcessKey(mykey);
    }
}

// a canvas represents one view of the data
class ImageCanvas extends Canvas implements ImageObserver,MouseListener,MouseMotionListener,KeyListener,FocusListener,AdjustmentListener {
    static final long serialVersionUID = 1;
    //int		xadd = 0;
    //int		yadd = 0;
    int		dadd = 0;  // addition parameter in depth for other two canvases; this offset is measured in display pixel coordinates
    int		xprev = 0;
    int		yprev = 0;
    
    int         ROIs;
    int         ROIe;
    boolean     LineROIStarted = false;
    int         LROIs = -1;
    int         LROIe = -1;
    boolean     ROIMoveStarted=false;
    int         ROIMoveStartX;
    int         ROIMoveStartY;
    int         ROIMoveDX=0;
    int         ROIMoveDY=0;
    boolean     ImgDragStarted=false; // for now allways drag
    int		ximgdrag = 0;
    int		yimgdrag = 0;
    
    int		imgw = -1;
    int		imgh = -1;
    //int		xoff = 0;
    //int		yoff = 0;
    double	scale = 1.0;  // will be changed in init
    double      LineData[];   // will be filled during "plot"
    double      AxisData[];   // will be filled during "plot"
    String      XAxisTitle="X";
    String      YAxisTitle="Y";
    // boolean     AspectLocked=false; // will prevent some scaling operations
    boolean AspectFromView=false;  // if true the aspect ratio of this view is fixed to the first (local x display direction) axis of the data and the scale is defined by the data scaling information.
    CheckboxMenuItem  AspectLocked;
    boolean	focus = false;
    boolean     DispPlot = false;
    // Image	origimage;
    Image	curimage;
    Container	applet;
    ImgPanel    myPanel; // just for the case, that this (histogram) window was generated by another owner
    ImageCanvas   otherCanvas1;  // other dimension object (x dimension in this canvas)
    ImageCanvas   otherCanvas2;  // other dimension object (y dimension in this canvas)
    My3DData    my3ddata;
    Vector PlanesXs, PlanesYs, PlanesXe, PlanesYe;  // this is a dublicate used for displaying the lines

    boolean hastoinit = true;
    int     DimNr=0;
    String  mytitle = "A Slice";
    PositionLabel label;
    PopupMenu MyPopupMenu;

public double PositionValue=1.0;   // value of dimension, that is controlled by this canvas
public double DataOffset=0.0;   // this is used for display
private int  MaxPos=1; 

   public ImageCanvas(Container app, ImgPanel mp, My3DData data3d, int mydimnr, String myt) {

        PlanesXs= new Vector();PlanesYs= new Vector();
    	PlanesXe= new Vector();PlanesYe= new Vector();
    	MyPopupMenu =new PopupMenu(myt);  // tear off menu
	add(MyPopupMenu);
        MenuItem tmp;
        Menu SubMenu = new Menu("Navigation",false);  // can eventually be dragged to the side
        MyPopupMenu.add(SubMenu);
	tmp = new MenuItem("Next slice [page up, ) ]");
	tmp.addActionListener(new MyMenuProcessor(this,')')); SubMenu.add(tmp);
	tmp = new MenuItem("Previous slice [page down, ( ]");
	tmp.addActionListener(new MyMenuProcessor(this,'(')); SubMenu.add(tmp);
	tmp = new MenuItem("Next element (color) [e]");
	tmp.addActionListener(new MyMenuProcessor(this,'e')); SubMenu.add(tmp);
	tmp = new MenuItem("Previous element (color) [E]");
	tmp.addActionListener(new MyMenuProcessor(this,'E')); SubMenu.add(tmp);
	tmp = new MenuItem("Next time [shift-down, ',']");
	tmp.addActionListener(new MyMenuProcessor(this,',')); SubMenu.add(tmp);
	tmp = new MenuItem("Previous time [shift-up, '.']");
	tmp.addActionListener(new MyMenuProcessor(this,'.')); SubMenu.add(tmp);

        SubMenu = new Menu("Display",false);  // can eventually be dragged to the side
        MyPopupMenu.add(SubMenu);
        tmp = new MenuItem("Initialize [i]");
	tmp.addActionListener(new MyMenuProcessor(this,'i')); SubMenu.add(tmp);
        tmp = new MenuItem("Initi Global Threshold [I]");
	tmp.addActionListener(new MyMenuProcessor(this,'I')); SubMenu.add(tmp);
        tmp = new MenuItem("Toggle Plot Display [q]");
	tmp.addActionListener(new MyMenuProcessor(this,'q')); SubMenu.add(tmp);
        tmp = new MenuItem("Zoom into square ROI [Z]");
	tmp.addActionListener(new MyMenuProcessor(this,'Z')); SubMenu.add(tmp);
        
        AspectLocked = new CheckboxMenuItem("Aspect Locked [@]"); //  , false
        SubMenu.add(AspectLocked);  // also responds to short cuts
        AspectLocked.addActionListener(new MyMenuProcessor(this,'@')); 
        
        tmp = new MenuItem("Zoom in [A]");
	tmp.addActionListener(new MyMenuProcessor(this,'A')); SubMenu.add(tmp);
        tmp = new MenuItem("Zoom out [a]");
	tmp.addActionListener(new MyMenuProcessor(this,'a')); SubMenu.add(tmp);
        tmp = new MenuItem("Zoom in perpend. [>]");
	tmp.addActionListener(new MyMenuProcessor(this,'>')); SubMenu.add(tmp);
        tmp = new MenuItem("Zoom in perpend. [<]");
	tmp.addActionListener(new MyMenuProcessor(this,'<')); SubMenu.add(tmp);
    	
        SubMenu = new Menu("Projections",false);  // can eventually be dragged to the side
        MyPopupMenu.add(SubMenu);
        tmp = new MenuItem("Toggle max intensity projection [p]");
	tmp.addActionListener(new MyMenuProcessor(this,'p')); SubMenu.add(tmp);
        tmp = new MenuItem("Toggle sum projection [P]");
	tmp.addActionListener(new MyMenuProcessor(this,'P')); SubMenu.add(tmp);
	applet = app;
        myPanel = mp;
        my3ddata=data3d;
        DimNr=mydimnr;
	mytitle=myt;
        MaxPos = data3d.sizes[mydimnr];
	PositionValue= MaxPos/2;  // init to middle
        ROIs=0; ROIe=MaxPos-1;
	
	// origimage = my3ddata.GiveSection(DimNr,(int) (PositionValue));
	// pickImage();
	setBounds(0, 0, 100, 100);
	addMouseListener(this); // register this class for handling the events in it
	addKeyListener(this); // register this class for handling the events in it
	addMouseMotionListener(this); // register this class for handling the events in it
	addFocusListener(this); // register this class for handling the events in it
        }

   public int getMaxPos() {
	   return MaxPos;
   }

    public void TakeOtherCanvas1(ImageCanvas thecan) {
      otherCanvas1=thecan;
    }

    public void TakeOtherCanvas2(ImageCanvas thecan) {
      otherCanvas2=thecan;
    }


  public void ConnectLabel(PositionLabel alabel) {
      label=alabel;
  }

  public boolean LimitPosition(int space) {  // limits the image positions to within the canvas (if not zoomed)
      if (space > scale*MaxPos)  // More space available than image
      {
	if (dadd < 0)
        {dadd = 0;return false;}
        else
            if (space - dadd - scale*MaxPos < 0) // distance of image to the right border is smaller than zero
            { dadd = space - ((int) (scale*MaxPos));return false;}
      }
      else  // Image is bigger than space
          if (dadd > 0)
          {dadd = 0; return false;}
          else
            if (space - dadd - scale*MaxPos > 0) // distance of image to the right border is smaller than zero
            { dadd = space - ((int) (scale*MaxPos));return false;}
        return true;
  }

  public void InitScaling() {
	Rectangle r = getBounds();
            
        double myaspect = my3ddata.GetScale(0,otherCanvas1.DimNr) / my3ddata.GetScale(0,otherCanvas2.DimNr);
        double myZaspect = my3ddata.GetScale(0,otherCanvas1.DimNr) / my3ddata.GetScale(0,DimNr);
    	double scalex=r.width/((double) otherCanvas1.MaxPos);
        double scaley=r.height/((double) otherCanvas2.MaxPos);
	// if ((scale1 > otherCanvas1.scale) ||
          //   (otherCanvas1.scale == 0.0))    
        // if ((scale1 > otherCanvas2.scale) ||
        //     (otherCanvas2.scale == 0.0)) 
        //System.out.println("My Aspect are " + myaspect +", "+myZaspect+"\n");
        
        if (AspectLocked.getState() || AspectFromView)
        {
            if (scalex > scaley * myaspect)   // fit it into the bigger dimension
                scalex = scaley * myaspect;
            else
                scaley = scalex / myaspect;
           //scalex = scalex * myZaspect;
           scaley = scalex /myaspect;
           otherCanvas1.scale = scalex;   // beeing locked forces to overwrite
           otherCanvas1.dadd = 0;
           otherCanvas2.scale = scaley;
           otherCanvas2.dadd = 0;
           if (otherCanvas1.AspectLocked.getState() || otherCanvas1.AspectFromView ||
        		   otherCanvas2.AspectLocked.getState() || otherCanvas2.AspectFromView)
           {
        	   scale=scalex/myZaspect;
           }
        }        
        	
        if (! otherCanvas2.AspectLocked.getState())
        {
            otherCanvas1.scale = scalex;
            otherCanvas1.dadd = 0;
        }
        if (! otherCanvas1.AspectLocked.getState())
        {
            otherCanvas2.scale = scaley;
            otherCanvas2.dadd = 0;
        }
        DispPlot = false;
        if (DimNr == 0)
            if (my3ddata.GetSize(2) <= 1)
                DispPlot = true;
        if (DimNr == 1)
            if (my3ddata.GetSize(2) <= 1)
                DispPlot = true;
        if (DimNr == 2)
            if (my3ddata.GetSize(0) <= 1)
                DispPlot = true;    
  }

  Color GetMarkerColor(int e) {
      return my3ddata.GetMarkerColor(e);
   }   
   
   public void plot(Graphics g) {  // Plots 1D-data
    Rectangle r = getBounds();
    // Color bg = getBackground();
    g.setColor(Color.black);
    g.fillRect(0,0,r.width-1,r.height-1);
    g.setColor(Color.white);
    
    int px,py,pz;
    int Size;
    int ActPos=0;
    int XAxisDim=0;
    double xDOff = my3ddata.ElementAt(my3ddata.GetActiveElement()).DisplayOffset[otherCanvas1.DimNr];
    double yDOff = my3ddata.ElementAt(my3ddata.GetActiveElement()).DisplayOffset[otherCanvas2.DimNr];
    double Off = my3ddata.ElementAt(my3ddata.GetActiveElement()).DisplayOffset[DimNr];
    if (DimNr==0) { // Plot along Y, but directed along X
        Size = my3ddata.SizeY;
        px = (int) (PositionValue);
        py = (int) (otherCanvas2.PositionValue);
        pz = (int) (otherCanvas1.PositionValue);
        g.drawLine(r.width-10,10,r.width-10,r.height-10);
        // g.drawLine(10,r.height-10,r.width-10,r.height-10);
        g.setColor(Color.green);
        ActPos = (int) otherCanvas2.PositionValue;
        int pos = MouseFromY(ActPos+(int) yDOff) +(int)(0.5*otherCanvas2.scale);
        // MouseFromY(otherCanvas2.PositionValue+yDOff)+(int)(0.5*otherCanvas2.scale);
        g.drawLine(0,pos,r.width-1,pos);        
        XAxisDim=1;
        }
    else if (DimNr==1) {  // Plot along X
        Size = my3ddata.SizeX;
        px = (int) (otherCanvas1.PositionValue);
        py = (int) (PositionValue);
        pz = (int) (otherCanvas2.PositionValue);
        // g.drawLine(10,10,10,r.height-10);
        g.drawLine(10,r.height-10,r.width-10,r.height-10);
        g.setColor(Color.green);
        ActPos = (int) otherCanvas1.PositionValue;
        int pos = MouseFromX(ActPos+(int) xDOff)+(int)(0.5*otherCanvas1.scale);
        g.drawLine(pos,0,pos,r.height-1);
        XAxisDim=0;
        }
   else {   // plot along Z, but directed along X
        Size = my3ddata.SizeZ;
        px = (int) (otherCanvas1.PositionValue);
        py = (int) (otherCanvas2.PositionValue);
        pz = (int) (PositionValue);
        // g.drawLine(10,10,10,r.height-10);
        g.drawLine(10,r.height-10,r.width-10,r.height-10);
        g.setColor(Color.green);
        ActPos = (int) PositionValue;
        int pos =(int)((ActPos+ (int) Off)*r.width/MaxPos) +(int)(0.5*scale);
        g.drawLine(pos ,0,pos,r.height-1);
        XAxisDim=2;
        }

    String XAxisName = my3ddata.GetAxisNames()[XAxisDim];
    String XAxisUnit = my3ddata.GetAxisUnits()[XAxisDim];
    XAxisTitle= XAxisName+" ["+XAxisUnit+"]";

    String YAxisName = my3ddata.GetValueName(my3ddata.GetActiveElement());
    String YAxisUnit = my3ddata.GetValueUnit(my3ddata.GetActiveElement());

    if (my3ddata.GetProjectionMode(DimNr))
    {
        String test;
        if (! my3ddata.GetMIPMode(DimNr))
            test=", Avg. Proj.";
        else
            test=", Max. Proj.";
        g.drawString(test, r.width - test.length() * 9 , 12); // my3ddata.GetUnitsX(-1));       
        YAxisTitle=YAxisName+" ["+YAxisUnit+"]"+test;
    }
    else
        YAxisTitle=YAxisName+" ["+YAxisUnit+"]";
        
    int lxnew=0, lxold=0,lynew=0, lyold=0;
    for (int e=0;e<my3ddata.Elements;e++)
    {
    xDOff = my3ddata.ElementAt(my3ddata.GetActiveElement()).DisplayOffset[otherCanvas1.DimNr];
    yDOff = my3ddata.ElementAt(my3ddata.GetActiveElement()).DisplayOffset[otherCanvas2.DimNr];
    Off = my3ddata.ElementAt(my3ddata.GetActiveElement()).DisplayOffset[DimNr];
    if (my3ddata.InOverlayDispl(e) || (my3ddata.ActiveElement == e))
    {
        g.setColor(GetMarkerColor(e));
                
        for (int pos=0;pos < Size;pos++)   // the loop iterating over the x-plot-axis
            {
                double val=0,valtrue=0;
                if (! my3ddata.ProjMode[DimNr])  // this is not a projection
                {
                if (DimNr==0)
                    {val = my3ddata.NormedValueAt(px,pos,pz,e);
                     valtrue = my3ddata.ValueAt(px,pos,pz,e);
                     lynew = MouseFromY(pos + (int) yDOff)+(int)(0.5*otherCanvas2.scale);
                     lxnew = r.width-10 - (int) ((r.width - 20)*(val));
                    }
                else if (DimNr==1)
                    {val = my3ddata.NormedValueAt(pos,py,pz,e);
                     valtrue = my3ddata.ValueAt(pos,py,pz,e);
                     lxnew = MouseFromX(pos+ (int) xDOff) +(int)(0.5*otherCanvas1.scale);
                     lynew = r.height - 10 - (int) ((r.height - 20 )*(val));
                    }
                else if (DimNr==2)
                    {val = my3ddata.NormedValueAt(px,py,pos,e);
                     valtrue = my3ddata.ValueAt(px,py,pos,e);
                     lxnew = (pos  + (int) Off) * r.width / Size + (int)(0.5*scale);
                     lynew = r.height - 10 - (int) ((r.height - 20)*(val));
                    }
                }
                else  // this is a projection
                {
                val = my3ddata.NormedProjValueAt(DimNr,pos,e);
                valtrue = my3ddata.ProjValueAt(DimNr,pos,e);
                if (DimNr==0)
                    {
                     lynew = MouseFromY(pos+ (int) yDOff)+(int)(0.5*otherCanvas2.scale);
                     lxnew = r.width-10 - (int) ((r.width - 20)*(val));
                    }
                else if (DimNr==1)
                    {
                     lxnew = MouseFromX(pos +  (int) xDOff) +(int)(0.5*otherCanvas1.scale);
                     lynew = r.height - 10 - (int) ((r.height - 20 )*(val));
                    }
                else if (DimNr==2)
                    {
                     lxnew = (pos  + (int) Off) * r.width / Size + (int)(0.5*scale);
                     lynew = r.height - 10 - (int) ((r.height - 20)*(val));
                    }
                }
                
                if (pos != 0)
                    g.drawLine(lxold,lyold,lxnew,lynew);
                if (e == my3ddata.GetActiveElement())
                {
                    if (LineData == null)
                        LineData = new double[Size];
                    LineData[pos] = valtrue;

                    if (AxisData == null)
                        AxisData = new double[Size];
                    AxisData[pos] = pos*my3ddata.GetScale(0,XAxisDim)+my3ddata.GetOffset(0,XAxisDim); 
                    
                    if (pos == ActPos)
                    {
                        g.fillOval(lxnew-5,lynew-5,10,10);
                        if (my3ddata.ProjMode[DimNr])  // this is not a projection
                        {
                            double ProjVal = my3ddata.ProjValueAt(DimNr,pos,e);
                            g.drawString(label.CreateValueString(ProjVal), 5 , 12); 
                        }
                        else
                        {
                            g.drawString(label.GetValueString(), 5 , 12); 
                            g.drawString(label.GetPositionString(), 5 , 24); 
                        }
                        if (my3ddata.GetLogMode())
                        {
                        String test="Log. Mode";
                        g.drawString(test, r.width - test.length() * 9 , 24); // my3ddata.GetUnitsX(-1));
                        }
                    }
                }
                
                lxold=lxnew;lyold=lynew;
            }
    }
    } // end of element loop
    
    g.setColor(Color.white);
    if (my3ddata.SquareROIs())
      {
      if (otherCanvas1.ROIs >= 0 || otherCanvas2.ROIs >= 0)
      if (DimNr ==1)
        {
        int xrs = MouseFromX(otherCanvas1.ROIs);
        int xre = MouseFromX(otherCanvas1.ROIe);
        int tmp;
        if (xrs > xre) {tmp=xrs;xrs=xre;xre=tmp;}
        g.drawRect(xrs, 0, xre-xrs, r.height-9);
        }
       else if (DimNr == 0)
         {
         int yrs = MouseFromY(otherCanvas2.ROIs);
         int yre = MouseFromY(otherCanvas2.ROIe);
         int tmp;
         if (yrs > yre) {tmp=yrs;yrs=yre;yre=tmp;}
         g.drawRect(0, yrs, r.width-9, yre-yrs);
         }
       else 
         {
         g.drawRect(ROIs*r.width/MaxPos, 0, (ROIe-ROIs)*r.width/MaxPos, r.height-9);
         }
    }            
}
 
public void paint(Graphics g) {
	Rectangle r = getBounds();

  	if (hastoinit)
	  {
	    hastoinit = false;
	        InitScaling();
	        CalcPrev();
	        otherCanvas1.repaint();
	        otherCanvas2.repaint();
	  }

        if (DispPlot)
            {
                plot(g);
                return;
            }
        
	int xm = (int) (otherCanvas1.MaxPos*otherCanvas1.scale);
	int ym = (int) (otherCanvas2.MaxPos*otherCanvas2.scale);

	int xOff = otherCanvas1.PixelFromDataPos(my3ddata.ElementAt(my3ddata.GetActiveElement()).DisplayOffset[otherCanvas1.DimNr]);
	int yOff = otherCanvas2.PixelFromDataPos(my3ddata.ElementAt(my3ddata.GetActiveElement()).DisplayOffset[otherCanvas2.DimNr]);
	//int xOff = otherCanvas1.dadd+xDOff;
	//int yOff = otherCanvas2.dadd+yDOff;
	// System.out.println("paint xyOff"+xOff+", "+yOff+"\n");
	
	g.drawImage(curimage, xOff, yOff , xm, ym, this);

	// Color bg = getBackground();
	//g.setColor(bg);
	g.setColor(Color.black);
	g.fillRect(0,0,r.width-1,yOff);
	g.fillRect(0,0,xOff,r.height-1);

	g.fillRect(xm+1+xOff,0,r.width-1,r.height-1);
	g.fillRect(0,ym+1+yOff,r.width-1,r.height-1);

	if (focus) {
	    g.setColor(Color.red);
	} else {
	    g.setColor(Color.darkGray);
	}
	g.drawRect(0, 0, r.width-1, r.height-1);   // draw the frame

        g.setColor(Color.white);
        if (ROIMoveStarted)
           g.setColor(Color.gray);
        
        if (my3ddata.SquareROIs()) // ROIs should not incorporate the data offset
        {
            // if (otherCanvas1.ROIs >= 0 && otherCanvas2.ROIs >= 0)
            if   (true) //! my3ddata.GetProjectionMode(DimNr) || myPanel.ROIstarted)
                {
                    int xrs,yrs,xre,yre;
                    if (myPanel.ROIstarted)  // ROI is not finished but started
                    {
                    xrs = DMouseFromX(otherCanvas1.ROIs);
                    yrs = DMouseFromY(otherCanvas2.ROIs);
                    xre = DMouseFromX(otherCanvas1.ROIe);
                    yre = DMouseFromY(otherCanvas2.ROIe);
                    }
                    else
                    {
                    Rectangle r2 = my3ddata.GetSqrROI(DimNr);
                    xrs = DMouseFromX(r2.x + ROIMoveDX);
                    yrs = DMouseFromY(r2.y + ROIMoveDY);
                    xre = DMouseFromX(r2.x+r2.width + ROIMoveDX);
                    yre = DMouseFromY(r2.y+r2.height + ROIMoveDY);
                    }
                int tmp;
                if (xrs > xre) {tmp=xrs;xrs=xre;xre=tmp;}
                if (yrs > yre) {tmp=yrs;yrs=yre;yre=tmp;}
                g.drawRect(xrs, yrs, xre-xrs, yre-yrs);
                }
        }
        else if   (true) // ! my3ddata.GetProjectionMode(DimNr) || LineROIStarted)
            {
                if (LineROIStarted)
                {
                    int xrs = DMouseFromX(otherCanvas1.LROIs);
                    int yrs = DMouseFromY(otherCanvas2.LROIs);
                    int xre = DMouseFromX(otherCanvas1.LROIe);
                    int yre = DMouseFromY(otherCanvas2.LROIe);
                    g.drawLine(xrs, yrs, xre, yre);
                }
                float coords[] = new float[2];
                
                int Rsize = my3ddata.GetPolyROISize(DimNr);
                if (Rsize>0)
                {
                    my3ddata.GetPolyROICoords(DimNr,0,coords);
                    int xold = DMouseFromX(coords[0] + ROIMoveDX); 
                    int yold = DMouseFromY(coords[1] + ROIMoveDY);
                    int xnew,ynew;
                for (int s=1;s<Rsize;s++)
                    {
                        my3ddata.GetPolyROICoords(DimNr,s,coords);
                        xnew = DMouseFromX(coords[0]+ ROIMoveDX);
                        ynew = DMouseFromY(coords[1]+ ROIMoveDY);
                        g.drawLine(xold, yold, xnew, ynew);
                        xold = xnew; yold = ynew;
                    }
                }
            }
        
        // Below the markers are drawn into the image 
        
        int xp,yp,xprev=0,yprev=0;
        int px = (int) (myPanel.c2.PositionValue); // Get the absolute x-position of the cursor
        int py = (int) (myPanel.c3.PositionValue); // Get the absolute y-position
        int pz = (int) (myPanel.c1.PositionValue); // Get the absolute z-position
        int element = (int) my3ddata.ActiveElement; // Get the active time
        int time = (int) my3ddata.ActiveTime; // Get the active time
        int ActPosition[] = {px,py,pz,element,time};
        NumberFormat nf2 = java.text.NumberFormat.getNumberInstance(Locale.US);
    	nf2.setMaximumFractionDigits(4);
    	nf2.setGroupingUsed(false);

    	boolean foundactive=false;

        // ColorSpace myHLS= new ICC_ColorSpace(ICC_Profile.getInstance(ColorSpace.TYPE_HLS));
        for (int l=0;l<my3ddata.NumMarkerLists();l++) // draw markers for marked points
        {
            int actlistpos= my3ddata.ActiveMarkerListPos();
        	boolean didannotate=false;
            if (my3ddata.ShowAllLists || l == actlistpos)
            for (int p=0;p<my3ddata.NumMarkers(l);p++) // draw markers for marked points
	    {
                APoint Pt=my3ddata.GetPoint(p,l);
                if (my3ddata.ShowSpectralTrack)
                {
                    // float cols[]={((float)Pt.coord[my3ddata.TrackDirection])/((float) my3ddata.sizes[my3ddata.TrackDirection]),0.9f,0.9f};
                    g.setColor(Bundle.ColFromHue(((float)Pt.coord[my3ddata.TrackDirection])/((float) my3ddata.sizes[my3ddata.TrackDirection])));
                }
                else
                    g.setColor(new Color(Pt.mycolor));
                double coords[] =Pt.coord;
		if (DimNr == 0)
		    {
			xp = DMouseFromX(coords[2]);
			yp = DMouseFromY(coords[1]);
		    }
		else if (DimNr == 1)
		    {
			xp = DMouseFromX(coords[0]);
			yp = DMouseFromY(coords[2]);
		    }
		else 
		    {
			xp = DMouseFromX(coords[0]);
			yp = DMouseFromY(coords[1]);
		    }
                 // System.out.println("Draw Rect nr "+l+", "+p+" xy : "+xp+" "+yp);
        	if (my3ddata.MarkerDisplayed(Pt,ActPosition))
        		Pt.isDisplayed = true;
        	else
        		Pt.isDisplayed = false;

        	int xDOff = otherCanvas1.PixelFromDataPos(my3ddata.ElementAt((int) (Pt.coord[3]+0.5),(int) (Pt.coord[4]+0.5)).DisplayOffset[otherCanvas1.DimNr]) - otherCanvas1.dadd;
        	int yDOff = otherCanvas2.PixelFromDataPos(my3ddata.ElementAt((int) (Pt.coord[3]+0.5),(int) (Pt.coord[4]+0.5)).DisplayOffset[otherCanvas2.DimNr]) - otherCanvas2.dadd;

                 if (Pt.isDisplayed)
                 {
                	 if (Pt.tagged)
                        g.fillOval(xp-MarkerLists.dx + xDOff,yp-MarkerLists.dy + yDOff,MarkerLists.dx*2+1,MarkerLists.dx*2+1);
                	 else
                    	g.drawRect(xp-MarkerLists.dx + xDOff,yp-MarkerLists.dy + yDOff,MarkerLists.dx*2+1,MarkerLists.dx*2+1);
                    if (myPanel.DraggedMarker == Pt)  // If currectly being dragged, fill it
                    {
                    	if (Pt.tagged)
                        	g.drawOval(xp-MarkerLists.dx-1 + xDOff,yp-MarkerLists.dy-1 + yDOff,MarkerLists.dx*2+3,MarkerLists.dx*2+3);
                    	else
                        	g.fillRect(xp-MarkerLists.dx + xDOff,yp-MarkerLists.dy + yDOff,MarkerLists.dx*2+1,MarkerLists.dx*2+1);
                    }

                    if (! foundactive && my3ddata.CheckActiveMarker(l,p,px,py,pz,element,time)) // coordinates of display,   p : Numer of marker
                    //if (my3ddata.ActiveMarkerPos() == p && (l == actlistpos))
                    {
                    	g.setColor(Color.white);
                    	if (Pt.tagged)
                        	g.fillOval(xp-MarkerLists.dx-1 + xDOff,yp-MarkerLists.dy-1 + yDOff,MarkerLists.dx*2+3,MarkerLists.dx*2+3);
                    	else
                        	g.drawRect(xp-MarkerLists.dx-1 + xDOff,yp-MarkerLists.dy-1 + yDOff,MarkerLists.dx*2+3,MarkerLists.dx*2+3);
                    	foundactive=true;
                    }

                    if (! didannotate && my3ddata.Annotate)  // show number for first track in list only
                    {
                    	didannotate=true;
                   	 	String ListName=my3ddata.GetMarkerListName(l);
                   	 	if (ListName != null && ! ListName.equals(""))
                   	 		g.drawString(ListName,xp+10 + xDOff,yp+10 + yDOff); 
                   	 // g.drawString(nf2.format(l+1),xp+10,yp+10); 
                    }
                 }
                 //if (my3ddata.ActiveMarkerPos() == p && (l == actlistpos))
                 //	 g.setColor(Color.white);
              
          		boolean shouldDraw=true;

          		if (p == 0)
                	 if (my3ddata.HasParent(l))
                	 {
                		 APoint oPt=my3ddata.GetParentEndOfTrack(l);
                		 int xpOff = otherCanvas1.PixelFromDataPos(my3ddata.ElementAt((int) (oPt.coord[3]+0.5),(int) (oPt.coord[4]+0.5)).DisplayOffset[otherCanvas1.DimNr]) - otherCanvas1.dadd;
                		 int ypOff = otherCanvas2.PixelFromDataPos(my3ddata.ElementAt((int) (oPt.coord[3]+0.5),(int) (oPt.coord[4]+0.5)).DisplayOffset[otherCanvas2.DimNr]) - otherCanvas2.dadd;
                    coords = oPt.coord;
             		if (DimNr == 0)
             		    {xprev = DMouseFromX(coords[2]) +xpOff;yprev = DMouseFromY(coords[1])+ypOff;}
             		else if (DimNr == 1)
             		    {xprev = DMouseFromX(coords[0]) +xpOff;yprev = DMouseFromY(coords[2])+ypOff;}
             		else 
             		    {xprev = DMouseFromX(coords[0]) +xpOff;yprev = DMouseFromY(coords[1])+ypOff;}
             		g.setColor(Color.gray);
                	 }
                	 else shouldDraw=false;

                 if (shouldDraw && my3ddata.ConnectionShown)
                    {
                        if ((my3ddata.ActiveMarkerPos() == p && (l == actlistpos)) || (my3ddata.ShowFullTrace && (my3ddata.ShowAllTrees || my3ddata.CommonRoot(l,actlistpos)))) 
                            {
                        	g.drawLine(xprev,yprev,xp + xDOff,yp + yDOff);
                            }
                    }
                 
                xprev=xp + xDOff;yprev=yp + yDOff;
	    }
        }
        

    xp = getCrossHairX();
    yp = getCrossHairY();
    g.setColor(Color.green);
        
	if (otherCanvas1 != null)   // draw coordinate lines
	 {
	   g.drawLine(xp,0,xp,yp-10);
	   g.drawLine(xp,yp+10,xp,r.height-1);
	 }
	if (otherCanvas2 != null)
	 {
	   g.drawLine(0,yp,xp-10,yp);
	   g.drawLine(xp+10,yp,r.width-1,yp);
	 }
	g.setColor(Color.white);
        
        if (my3ddata.GetProjectionMode(DimNr))
        {
            String test;
            if (! my3ddata.GetMIPMode(DimNr))
                test="Avg. Proj.";
            else
                test="Max. Proj.";
            g.drawString(test, r.width - test.length() * 9 , 12); // my3ddata.GetUnitsX(-1));
        }

        g.drawString(mytitle,10,15);
    }

  public int getCrossHairX() {
	    double xDOff = my3ddata.ElementAt(my3ddata.GetActiveElement()).DisplayOffset[otherCanvas1.DimNr];
	    return MouseFromX(otherCanvas1.PositionValue+xDOff)+(int)(0.5*otherCanvas1.scale);
  }
  
  public int getCrossHairY() {
	    double yDOff = my3ddata.ElementAt(my3ddata.GetActiveElement()).DisplayOffset[otherCanvas2.DimNr];
	    return MouseFromY(otherCanvas2.PositionValue+yDOff)+(int)(0.5*otherCanvas2.scale);	  	  
  }

  public int getCrossHairZ() {
	    double zDOff = my3ddata.ElementAt(my3ddata.GetActiveElement()).DisplayOffset[DimNr];
	    return PixelFromDataPos(PositionValue+zDOff)+(int)(0.5*scale);	  	  
  }


  public void update(Graphics g) { // eliminate flickering
      paint(g);
      // label.CoordsChanged();  // due to roundoff errors?  Why is this necessary to display the elements?
  }

    static final long updateRate = 100;
    public synchronized boolean imageUpdate(Image img, int infoflags,
					    int x, int y, int w, int h) {
	if (img != curimage) {
	    return false;
	}
	boolean ret = true;
	boolean dopaint = false;
	long updatetime = 0;
	if ((infoflags & WIDTH) != 0) {
	    imgw = w;
	    dopaint = true;
	}
	if ((infoflags & HEIGHT) != 0) {
	    imgh = h;
	    dopaint = true;
	}
	if ((infoflags & (FRAMEBITS | ALLBITS)) != 0) {
	    dopaint = true;
	    ret = false;
	} else if ((infoflags & SOMEBITS) != 0) {
	    dopaint = true;
	    updatetime = updateRate;
	}
	if ((infoflags & ERROR) != 0) {
	    ret = false;
	}
	if (dopaint) {
	    repaint(updatetime);
	}
	return ret;
    } 
    
    public synchronized Image pickImage() {  // selects the image
        if (! DispPlot)  // otherwise no new image is needed for this pane
            curimage = my3ddata.GiveSection(DimNr,(int) (PositionValue));
	return curimage;
    }

    public int PixelFromDataPos(double pos) { // rounds at Data level
	// return dadd+(int) ((((int) pos)+0.5)*scale+0.5);
	return dadd+(int) (((int) pos)*scale+0.5);
    }
    
    public int MouseFromX(double pos) {  // converts Data voxel coordinates into display coordinates
    	return otherCanvas1.PixelFromDataPos(pos);
    }
    
    public int MouseFromY(double pos) {
    	return otherCanvas2.PixelFromDataPos(pos);
    }

    public int DPixelFromDataPos(double pos) { // rounds at Data level
    	return dadd+(int) ((pos+0.5)*scale);
        }

    public int DMouseFromX(double pos) {  // rounds at pixel level
    	return otherCanvas1.DPixelFromDataPos(pos);
    }
    
    public int DMouseFromY(double pos) {
    	return otherCanvas2.DPixelFromDataPos(pos);
    }

    public double DataPosFromPixel(int pixelpos) {
    	double result= ((pixelpos-dadd)/scale );
    	if (result < 0) result=0;
    	if (result > MaxPos-1) result=MaxPos-1;
    	return result;    	
    }

    public double DataPosFromPixelNoLimit(int pixelpos) {
    	return ((pixelpos-dadd)/scale );
    }
    
    public double XFromMouse(int xnew) { // converts display pixel coordinates into Data voxel coordinates
	return otherCanvas1.DataPosFromPixel(xnew);
    }

    public double YFromMouse(int ynew) { // converts display pixel coordinates into Data voxel coordinates
    	return otherCanvas2.DataPosFromPixel(ynew);
    }

    public double XFromMouseNoLimit(int xnew) { // converts display pixel coordinates into Data voxel coordinates
    	return otherCanvas1.DataPosFromPixelNoLimit(xnew);
        }

    public double YFromMouseNoLimit(int ynew) { // converts display pixel coordinates into Data voxel coordinates
        	return otherCanvas2.DataPosFromPixelNoLimit(ynew);
        }

  void updateothers(int xnew, int ynew) {  // important, when a position has changed
	  double xOff = my3ddata.ElementAt(my3ddata.ActiveElement).DisplayOffset[otherCanvas1.DimNr];
      double yOff = my3ddata.ElementAt(my3ddata.ActiveElement).DisplayOffset[otherCanvas2.DimNr];

      // Rectangle r = getBounds();
	    double newPos1 = XFromMouse((int)(xnew-otherCanvas1.scale*xOff));

  	    if (otherCanvas1 != null)
	      {
		if (otherCanvas1.PositionValue != newPos1)
		  {
		    otherCanvas1.PositionValue = newPos1;
		    otherCanvas1.pickImage();
		  }
		 otherCanvas1.repaint();
	      }
	    double newPos2 = YFromMouse((int)(ynew-otherCanvas2.scale*yOff));

	    if (otherCanvas2 != null)
	      {
		if (otherCanvas2.PositionValue != newPos2)
		  {
		    otherCanvas2.PositionValue = newPos2;
		    otherCanvas2.pickImage();
		  }
		 otherCanvas2.repaint();
	      }
        //System.out.println("Pos: " + newPos1 + ", "+ newPos2 +"\n");
	}
 
  public void CalcPrev() {
    xprev = otherCanvas1.DPixelFromDataPos(otherCanvas1.PositionValue);// Position into mouse-coordinates
    yprev = otherCanvas2.DPixelFromDataPos(otherCanvas2.PositionValue);
  }

// Process adjustment events of attached scrollbars
  public void adjustmentValueChanged(AdjustmentEvent e) 
   {
       // if (! e.getValueIsAdjusting())   // The user finished adjusting
       {
           int newtime=e.getValue();
           my3ddata.setTime(newtime);
           UpdateAll();
       }       
   }
  
  
// code for event handling :
// Keybord-Focus events
public void focusGained(FocusEvent e) {
    focus = true;
    repaint();
}

public void focusLost(FocusEvent e) {
    focus = false;
    repaint();
}
// mouse events :
public void mouseEntered(MouseEvent e) {
    CalcPrev();
    requestFocus();
    // pickImage();
}

public void mouseClicked(MouseEvent e) {
	double xOff = my3ddata.ElementAt(my3ddata.ActiveElement).DisplayOffset[otherCanvas1.DimNr];
	double yOff = my3ddata.ElementAt(my3ddata.ActiveElement).DisplayOffset[otherCanvas2.DimNr];
    APoint Pt=my3ddata.MarkerFromPosition(XFromMouseNoLimit(e.getX())-0.5-xOff,YFromMouseNoLimit(e.getY())-0.5-yOff,DimNr,
                                                (2*MarkerLists.dx+1)/otherCanvas1.scale,(2*MarkerLists.dy+1)/otherCanvas2.scale);  // , myPanel.getPositions()
    if (Pt != null)
    {
    	if (Pt == my3ddata.GetActiveMarker())
    	{
    	Pt.Tag(-1);
        repaint();
        updateothers(xprev,yprev);
        label.CoordsChanged();
    	}
    	else
    	{
    		if (my3ddata.FocusDispToMarker)
        	    myPanel.RememberOffset();
    		my3ddata.SetActiveMarker(Pt);
    		if (my3ddata.FocusDispToMarker)
    		{
    			myPanel.setPositions(my3ddata.GetActiveMarker());
    			myPanel.AdjustOffset();
    			CalcPrev();
    			UpdateAll();
    		}
    	}
    }
}

public void mousePressed(MouseEvent e) {
    if (e.isPopupTrigger())
    {
	MyPopupMenu.show(this,e.getX(),e.getY());
        return;
    }

    double xOff = my3ddata.ElementAt(my3ddata.ActiveElement).DisplayOffset[otherCanvas1.DimNr];
	double yOff = my3ddata.ElementAt(my3ddata.ActiveElement).DisplayOffset[otherCanvas2.DimNr];

    if (myPanel.DraggedMarker != null)  // A drag was allready started. The user wants to cancel it.
    {
        myPanel.DraggedMarker.coord[0] = myPanel.SavedMarker.coord[0];
        myPanel.DraggedMarker.coord[1] = myPanel.SavedMarker.coord[1];
        myPanel.DraggedMarker.coord[2] = myPanel.SavedMarker.coord[2];
        myPanel.DraggedMarker = null; // cancell the drag
        repaint();
        updateothers(xprev,yprev);
        label.CoordsChanged();  
        return;
    }

    if (LineROIStarted)
    {
        if (otherCanvas1.LROIs == (int) XFromMouse(e.getX()) && otherCanvas2.LROIs == (int) YFromMouse(e.getY()))
        {   // User clicked twice :  Finish the ROI
            int Rsize = my3ddata.GetPolyROISize(DimNr);
            if (Rsize <= 2)   // ROI too small -> clear ROI
                my3ddata.ClearPolyROIs(DimNr);
            else
            {
                float coords[] = new float[2];
                my3ddata.GetPolyROICoords(DimNr,0,coords);
                my3ddata.TakePolyROI((int) coords[0], (int) coords[1], DimNr);  // close the ROI
                my3ddata.InvalidateProjs(-1);  // All projectiopns invalid
            }
            LineROIStarted = false;
            // repaint();
            UpdateAll();
            return;
        }
        else    // just add another point to the PolyROI
            {
            my3ddata.TakePolyROI( (int) XFromMouse(e.getX()), (int) YFromMouse(e.getY()),DimNr);
            }
        otherCanvas1.LROIs = (int) XFromMouse(e.getX());
        otherCanvas2.LROIs = (int) YFromMouse(e.getY());
        return;
     }

    if (e.isControlDown())  // Start a ROI moving event
    {
        ROIMoveStarted=true;
        ROIMoveStartX=(int) (XFromMouse(e.getX())-0.5);
        ROIMoveStartY=(int) (YFromMouse(e.getY())-0.5);
        return;
    }

    if (e.isShiftDown())  // Start a ROI marking event
        if (my3ddata.SquareROIs())
            {
            myPanel.ROIstarted=true;
            otherCanvas1.ROIs = (int) XFromMouse(e.getX());
            otherCanvas2.ROIs = (int) YFromMouse(e.getY());
            return;
            }
        else
            {
            if (! LineROIStarted)  // start a new LineROI
            {
                LineROIStarted=true;
                my3ddata.ClearPolyROIs(DimNr);
                LROIs=-1;
                otherCanvas1.LROIs = (int) XFromMouse(e.getX());
                otherCanvas2.LROIs = (int) YFromMouse(e.getY());
                my3ddata.TakePolyROI( (int) XFromMouse(e.getX()), (int) YFromMouse(e.getY()),DimNr);
                return;
            }
       }
    else
      {
          
      if (!DispPlot)
        {
          // Check wether a marker was hit
        APoint Pt=my3ddata.MarkerFromPosition(XFromMouseNoLimit(e.getX())-0.5-xOff,YFromMouseNoLimit(e.getY())-0.5-yOff,DimNr,
                                                (2*MarkerLists.dx+1)/otherCanvas1.scale,(2*MarkerLists.dy+1)/otherCanvas2.scale); // , myPanel.getPositions()
          if (Pt != null)
          {
        	  if (Pt != my3ddata.GetActiveMarker()) // (! my3ddata.ShowAllLists) && 
        		  ; // my3ddata.SetActiveMarker(Pt); // will be activated in the release function
        	  else
        	  {
        		  myPanel.DraggedMarker = Pt;
        		  myPanel.SavedMarker = (APoint) Pt.clone();
        	  }
          }
          else  // just a mouse pressed in the empty image area
          {
              // e.isAltGraphDown() ||  // This does not work with matlab
            if ( e.isMetaDown() || e.getClickCount()>1 || (e.getModifiers() & Event.ALT_MASK) != 0)
                ImgDragStarted = true;
            if (ImgDragStarted)  // Only drag the image but do not change coordinates
            {
                ximgdrag = e.getX();
                yimgdrag = e.getY();
                ImgDragStarted=true;
                return;
            }
            else
            {
                xprev = e.getX();   // just saves the pixel coordinate at which the mouse was pressed
                yprev = e.getY();
            }
          }
        }
      else   // This is in plotting mode
        {
        if (DimNr == 0)
            yprev=e.getY();
        else if (DimNr == 1)
            xprev=e.getX();
       else if (DimNr == 2)
            PositionValue =  MaxPos * e.getX() / getBounds().width;
        }
      repaint();
      updateothers(xprev,yprev);
      // updateothers(xprev-MouseFromX(xOff),yprev-MouseFromY(yOff));
      label.CoordsChanged();  
      }  // from else (if shiftdown)
}

public void mouseReleased(MouseEvent e) {
    if (e.isPopupTrigger())
    {
	MyPopupMenu.show(this,e.getX(),e.getY());
        return;
    }
    if (ImgDragStarted)
        ImgDragStarted=false;
    
    if (myPanel.DraggedMarker != null)  // successfully finished dragging
    {
            my3ddata.UpdateMarker(myPanel.DraggedMarker);  // Just in case the COI shall be used
            myPanel.DraggedMarker = null;
            repaint();
            updateothers(xprev,yprev);
            label.CoordsChanged();
            return;
    }

    if (! e.isControlDown())
        { ROIMoveStarted=false;ROIMoveDX=0;ROIMoveDY=0;repaint();}
    
    if (ROIMoveStarted & e.isControlDown())  // finalize
    {
            my3ddata.MoveROI(ROIMoveDX,ROIMoveDY,DimNr);
            ROIMoveStarted=false;
            ROIMoveDX=0;
            ROIMoveDY=0;
            LROIs=-1;
            //repaint();
            UpdateAll();
            return;
    }
    
    int SizeX = otherCanvas1.MaxPos-1;
    int SizeY = otherCanvas2.MaxPos-1;
    if (myPanel.ROIstarted)
    {
        otherCanvas1.ROIe = (int) XFromMouse(e.getX());
        otherCanvas2.ROIe = (int) YFromMouse(e.getY());
        if ((otherCanvas1.ROIe == otherCanvas1.ROIs) && (otherCanvas2.ROIe == otherCanvas2.ROIs))  // delete the ROI
        {
            my3ddata.TakeROI(0,0,SizeX,SizeY,DimNr);
            otherCanvas1.ROIs=0;otherCanvas1.ROIe=otherCanvas1.MaxPos-1;
            otherCanvas2.ROIs=0;otherCanvas2.ROIe=otherCanvas2.MaxPos-1;
            ROIs=0;ROIe=SizeX;
            UpdateAll();
        }
        else if (true) // ((otherCanvas1.ROIe >= otherCanvas1.ROIs) && (otherCanvas2.ROIe >= otherCanvas2.ROIs))
            {
                my3ddata.TakeROI(otherCanvas1.ROIs,otherCanvas2.ROIs,otherCanvas1.ROIe,otherCanvas2.ROIe,DimNr);
                UpdateAll();
            }
    }
    /*if (LineROIStarted)
    {
        otherCanvas1.LROIe = (int) XFromMouse(e.getX());
        otherCanvas2.LROIe = (int) YFromMouse(e.getY());
        if ((otherCanvas1.LROIe == otherCanvas1.LROIs) && (otherCanvas2.LROIe == otherCanvas2.LROIs))  // delete the ROI
        {
            my3ddata.ClearLineROIs();
            PlanesXs.removeAllElements();PlanesYs.removeAllElements();PlanesXe.removeAllElements();PlanesYe.removeAllElements();
            otherCanvas1.PlanesXs.removeAllElements();otherCanvas1.PlanesYs.removeAllElements();otherCanvas1.PlanesXe.removeAllElements();otherCanvas1.PlanesYe.removeAllElements();
            otherCanvas2.PlanesXs.removeAllElements();otherCanvas2.PlanesYs.removeAllElements();otherCanvas2.PlanesXe.removeAllElements();otherCanvas2.PlanesYe.removeAllElements();
        }
        else
        {
            my3ddata.TakeLineROI(otherCanvas1.LROIs,otherCanvas2.LROIs,otherCanvas1.LROIe,otherCanvas2.LROIe,DimNr);
            PlanesXs.addElement(new Integer(otherCanvas1.LROIs));
            PlanesYs.addElement(new Integer(otherCanvas2.LROIs));
            PlanesXe.addElement(new Integer(otherCanvas1.LROIe));
            PlanesYe.addElement(new Integer(otherCanvas2.LROIe));
        }
        LineROIStarted=false;
    }*/
    myPanel.ROIstarted=false;
}

public void mouseExited(MouseEvent e) {
}
// mouse motion events :
public void mouseDragged(MouseEvent e) {
   	double xOff = my3ddata.ElementAt(my3ddata.ActiveElement).DisplayOffset[otherCanvas1.DimNr];
	double yOff = my3ddata.ElementAt(my3ddata.ActiveElement).DisplayOffset[otherCanvas2.DimNr];

    if (ROIMoveStarted)
    {
        if (! e.isControlDown())
            { ROIMoveStarted=false;ROIMoveDX=0;ROIMoveDY=0;repaint();return;}

        ROIMoveDX=(int) (XFromMouse(e.getX())-0.5)-ROIMoveStartX;
        ROIMoveDY=(int) (YFromMouse(e.getY())-0.5)-ROIMoveStartY;
        repaint();
        return;
    }

    if (myPanel.DraggedMarker != null)
    {
        myPanel.DraggedMarker.UpdatePosition(XFromMouse(e.getX())-0.5-xOff,YFromMouse(e.getY())-0.5-yOff,DimNr);
    }
    else if (! myPanel.ROIstarted && ! LineROIStarted)   // Drags the image around
    {
        //boolean wasmoved=false;
        //otherCanvas1.dadd += e.getX() - xprev;
        //wasmoved |= otherCanvas1.LimitPosition(getBounds().width);
        //otherCanvas2.dadd += e.getY() - yprev;
        //wasmoved |= otherCanvas2.LimitPosition(getBounds().height);

        if (ImgDragStarted)
        {
            boolean wasmoved=false;
            otherCanvas1.dadd += e.getX() - ximgdrag;
            wasmoved |= otherCanvas1.LimitPosition(getBounds().width);
            otherCanvas2.dadd += e.getY() - yimgdrag;
            wasmoved |= otherCanvas2.LimitPosition(getBounds().height);
            ximgdrag = e.getX();yimgdrag = e.getY();
            repaint();
            return;
        }
        else
        {
            xprev = e.getX();yprev = e.getY();
        }

    }
    else
    {
        if (myPanel.ROIstarted)
        {
            otherCanvas1.ROIe = (int) XFromMouse(e.getX());
            otherCanvas2.ROIe = (int) YFromMouse(e.getY());      
        }
        if (LineROIStarted)
        {
            otherCanvas1.LROIe = (int) XFromMouse(e.getX());
            otherCanvas2.LROIe = (int) YFromMouse(e.getY());      
        }
    }
    repaint();
    updateothers(xprev,yprev);
    //updateothers(xprev-MouseFromX(xOff),yprev-MouseFromY(yOff));
    label.CoordsChanged();  
}

public void mouseMoved(MouseEvent e) {
        if (LineROIStarted)
        {
            otherCanvas1.LROIe = (int) XFromMouse(e.getX());
            otherCanvas2.LROIe = (int) YFromMouse(e.getY());
            repaint();
        }
}
// key press events
public void keyTyped(KeyEvent e) {
}

public void RedrawAll() { // A soft version of UpdateAll. No images have to be generated, just reposition
    repaint();
    otherCanvas1.repaint();
    otherCanvas2.repaint();
    return ;
}

public void UpdateAllNoCoord() {
    pickImage();
    repaint();
    otherCanvas1.pickImage();
    otherCanvas1.repaint();
    otherCanvas2.pickImage();
    otherCanvas2.repaint(); 
    return ;
}

public void UpdateAll() {
    UpdateAllNoCoord();
    label.CoordsChanged();
}

public void UpdateAllPanels() {
    if (applet instanceof View5D_)
       ((View5D_) applet).UpdatePanels();   // commented out for now, since the document base is unknown for non-applets
    else
       ((View5D) applet).UpdatePanels();   // commented out for now, since the document base is unknown for non-applets
    label.CoordsChanged();
}

public void keyPressed(KeyEvent e) {
    char myChar = e.getKeyChar();

    if(KeyEvent.VK_PAGE_UP == e.getKeyCode())  
        {if (e.isShiftDown())
            myChar = ',';           // move along time direction
        else
            myChar = '(';
        }
    
    if(KeyEvent.VK_PAGE_DOWN == e.getKeyCode())
    {
        if (e.isShiftDown())
            myChar = '.';           // move along time direction
        else
            myChar = ')';
    }

    
    int elem=my3ddata.ActiveElement,t=my3ddata.ActiveTime;

    ImageCanvas PreferredCanvas=null;
    int advance=1;
    
    if(KeyEvent.VK_DOWN == e.getKeyCode())
	{
    	advance=1;
    	PreferredCanvas=otherCanvas2;
	}
    if(KeyEvent.VK_UP == e.getKeyCode())
	{
    	advance=-1;
    	PreferredCanvas=otherCanvas2;
	}
    if(KeyEvent.VK_RIGHT == e.getKeyCode())
	{
    	advance=1;
    	PreferredCanvas=otherCanvas1;
	}
    if(KeyEvent.VK_LEFT == e.getKeyCode())
	{
    	advance=-1;
    	PreferredCanvas=otherCanvas1;
	}

    if (PreferredCanvas != null)
    {
    if (e.isShiftDown())
	{
    	int emin=0,emax=my3ddata.Elements-1,pe=my3ddata.ActiveElement;
    	if (my3ddata.TrackDirection != 4)
  	  		{emin=pe;emax=pe;}
    	else
  	  		{emin=0;emax=my3ddata.Elements-1;}
        for (pe=emin;pe<=emax;pe++)
        {
    	my3ddata.ElementAt(pe).DisplayOffset[PreferredCanvas.DimNr]+=advance; 
        my3ddata.InvalidateProjs(pe,t); // only invalidate this element and this time
		}
    }
    else if (e.isControlDown())
    	  if (my3ddata.TrackDirection == 4)  // this is time
    		for (;t<my3ddata.Times;t++)
    		{my3ddata.ElementAt(elem,t).DisplayOffset[PreferredCanvas.DimNr]+=advance; 
    		my3ddata.InvalidateProjs(elem,t);} // only invalidate this element and this time
    	  else
    		for (;elem<my3ddata.Elements;elem++)
    		{my3ddata.ElementAt(elem,t).DisplayOffset[PreferredCanvas.DimNr]+=advance; 
    		my3ddata.InvalidateProjs(elem,t);} // only invalidate this element and this time
    	else 
    		{
    		PreferredCanvas.PositionValue+=advance;
    		if (PreferredCanvas.PositionValue >= PreferredCanvas.MaxPos-1)
    			PreferredCanvas.PositionValue = PreferredCanvas.MaxPos-1;
    		if (PreferredCanvas.PositionValue <= 0)
    			PreferredCanvas.PositionValue = 0;
    		}
    }
    else
    	ProcessKey(myChar);
    UpdateAll();
    return;
}

void SpawnHistogram(boolean forceHistogram) 
{
       // First estimate useful image sizes
        if (my3ddata.DataToHistogram != null)  // This seems to be a histogram, so it can only be applied
        {
            // System.out.println("Here am I\n");
            my3ddata.ApplyHistSelection();
            if (myPanel.DataPanel != null)
                myPanel.DataPanel.c1.UpdateAll();
        }
        else // this means a histogram has to be generated, not applied
        {
           if (my3ddata.MyHistogram == null || forceHistogram)
            {
            int MaxSize = 128;
            double HRangeX=my3ddata.GetScaledRange(my3ddata.HistoX);
            if (HRangeX <= 0) 
            {
                System.out.println("No coordinate selected as X histogram coordinate");
                return;
            }
            int HBinsX = MaxSize;
            double[] Scales = new double[5];
            for (int j=0;j<5;j++) Scales[j]=1.0;                
            double[] Offsets = new double[5];
            for (int j=0;j<5;j++) Offsets[j]=0.0;
            // double ScaleV=1.0,OffsetV=0.0;
            Scales[0] = HRangeX/(HBinsX-1.0);
            if (HRangeX > 50)
                {
                    Scales[0] = ((int) ((HRangeX-1)/MaxSize) + 1);
                    HBinsX = (int) (HRangeX / Scales[0]);
                    System.out.println("X Histscale : "+ HBinsX+ ", " + Scales[0]+"\n");
                }
            double HRangeY=1,HRangeZ=1;
                
            int HBinsY = 1, HBinsZ = 1;
            Offsets[0] = my3ddata.GetScaledMincs(my3ddata.HistoX);
            if (my3ddata.HistoY >=0)
            {
                HRangeY=my3ddata.GetScaledRange(my3ddata.HistoY);
                if (HRangeY <= 0) 
                {
                    System.out.println("No coordinate selected as Y histogram coordinate");
                    return;
                }
                if (HRangeY > 50)
                {
                    Scales[1] = ((int) ((HRangeY-1)/MaxSize) + 1);
                    HBinsY = (int) (HRangeY / Scales[1]);
                    System.out.println("Y Histscale : "+ HBinsY+ ", " + Scales[1]+"\n");
                }
                else
                {
                    HBinsY = MaxSize; Scales[1] = HRangeY / (HBinsY-1.0);
                    System.out.println("::: Y Histscale : "+ HBinsY+ ", "+ HRangeY + ", " + Scales[1]+"\n");
                }
                Offsets[1] = my3ddata.GetScaledMincs(my3ddata.HistoY);
            }
            if (my3ddata.HistoZ >=0)
            {
                HRangeZ=my3ddata.GetScaledRange(my3ddata.HistoZ);
                if (HRangeZ <= 0) 
                {
                    System.out.println("No coordinate selected as Z histogram coordinate");
                    return;
                }
                if (HRangeZ > 50)
                {
                    Scales[2] = ((int) ((HRangeZ-1)/MaxSize) + 1);
                    HBinsZ = (int) (HRangeZ / Scales[2]);
                }
                else
                {
                    // ScaleZ = ((int) ((HSizeZ-1)/MaxSize) + 1);HSizeZ = (int) (HSizeZ / ScaleZ);
                    HBinsZ = MaxSize;Scales[2] = HRangeZ / (HBinsZ-1.0);
                }
                Offsets[2] = my3ddata.GetScaledMincs(my3ddata.HistoZ);
            }
        
            My3DData Histogram = new My3DData(applet,HBinsX,HBinsY,HBinsZ,
                                      1,1, // One element, one timestep
                                      -1,-1,-1,  // RGB
                                      0,-1,-1, // histogram of histogram ?
                                      AnElement.IntegerType,4,31,
                                      Scales,Offsets,
                                      1.0,0.0,my3ddata.GetAxisNames(),my3ddata.GetAxisUnits());  // Four Bytes, 31 bits
        
            Histogram.TakeDataToHistogram(my3ddata);
            Histogram.ComputeHistogram();
            Histogram.ToggleModel(5);  // mark as red glow
            Histogram.ToggleOverlayDispl(1); // Make element visible in multicolor mode
            AlternateViewer xx2=new AlternateViewer(applet);
            xx2.Assign3DData(applet,myPanel,Histogram);
            if (my3ddata.MyHistogram == null)
                my3ddata.MyHistogram = Histogram;  // otherwise forget it as a single window
            my3ddata.DataToHistogram = null; // were was this set to some value??
            // xx2.invalidate();
            // xx2.repaint();
            // xx2.setEnabled(true);
            // What to do to make this window visible without a resize by the user???
            }
            else  // 'h' was pressed in data (not in histogram)
            {
                if (my3ddata.MyHistogram.Elements==1)
                    my3ddata.MyHistogram.colormode=true;  // set to multicolor at the second histogram
                my3ddata.MyHistogram.CloneLastElements();
                my3ddata.MyHistogram.TakeDataToHistogram(my3ddata);  // Necessary to connect this element to HistogramX ...
                my3ddata.MyHistogram.ComputeHistogram();
            }
        }
        // my3ddata.MyHistogram.UpdateAll();
        return ;
 }

 public PlotWindow ExportValues() {
     PlotWindow myplot = new PlotWindow("View5D Plot",XAxisTitle,YAxisTitle,AxisData,LineData);
     myplot.draw();
     return myplot;
   }


public void ProcessKey(char myChar) {
    switch (myChar) {
    case ' ':
        ImgDragStarted = true;
	return;
    case 'D':
        my3ddata.DeleteActElement();
	UpdateAllPanels();
	return;
    case 'f':  // upcast datatype to float
        my3ddata.CloneFloat();
	UpdateAllPanels();
        return;
    case 'F':  // upcast datatype to short, in this case thresholds will be applied
        my3ddata.CloneShort();
	UpdateAllPanels();
        return;
    case '+':
        my3ddata.AddMarkedElement(); // Adds the gate element to the active element
	UpdateAllPanels();
	return;
    case '_':
        my3ddata.AdjustOffsetToROIMean(); // Subtracts the gate element from the active element
	UpdateAllPanels();
	return;
    case '-':
        my3ddata.SubMarkedElement(); // Subtracts the gate element from the active element
	UpdateAllPanels();
	return;
    case '*':
        my3ddata.MulMarkedElement(); // Multiplies the gate element with the active element
	UpdateAllPanels();
	return;
    case '/':
        my3ddata.DivMarkedElement(); // Multiplies the gate element with the active element
	UpdateAllPanels();
	return;
    case 'e':  // advance one element cyclicly
    	myPanel.RememberOffset();
    	my3ddata.advanceElement(1);
    	myPanel.AdjustOffset();
    	UpdateAll();
	return;
    case 'E':  // advance one element cyclicly
    	myPanel.RememberOffset();
    	my3ddata.advanceElement(-1);
    	myPanel.AdjustOffset();
    	UpdateAll();
	return;
    case 't':  // automatic threshold adjustment
	my3ddata.AdjustThresh(false);
	UpdateAll();
	return ;
    case 'T':  // automatic threshold adjustment
	my3ddata.AdjustThresh(true);
	UpdateAll();
	return ;
    case 'n':  // 
        my3ddata.MarkerDialog();
	// my3ddata.ToggleConnection(-1);
	UpdateAll();
	return;
    case 'N':  // Pops up a "Units" dialog
	my3ddata.AxesUnitsDialog();
        my3ddata.InvalidateProjs(-1);  // all projections are invalid
	label.CoordsChanged();
	break;
    case 'r':  // Choose Red colormap
	my3ddata.ToggleModel(1);
	my3ddata.ToggleOverlayDispl(1);
	//my3ddata.MarkChannel(0);
	UpdateAll();
	return ;
    case 'R':  // mark red channel
	my3ddata.ToggleModel(5);
	my3ddata.ToggleOverlayDispl(1);
	//my3ddata.ClearChannel(0);
	UpdateAll();
	return ;
    case 'g':  // mark green channel
	my3ddata.ToggleModel(2);
	my3ddata.ToggleOverlayDispl(1);
	//my3ddata.MarkChannel(1);
	UpdateAll();
	return ;
    case 'G':  // mark green channel
	my3ddata.ToggleModel(0);
	my3ddata.ToggleOverlayDispl(1);
	//my3ddata.ClearChannel(1);
	UpdateAll();
	return;
    case 'b':  // mark blue channel
	my3ddata.ToggleModel(3);
	my3ddata.ToggleOverlayDispl(1);
	//my3ddata.MarkChannel(2);
	UpdateAll();
	return;
    case 'B':  // mark blue channel
	my3ddata.ToggleModel(10);
	my3ddata.ToggleOverlayDispl(1);
	//my3ddata.ClearChannel(2);
	UpdateAll();
	return;
    case 'v':  // in/exclude channel from color display
	my3ddata.ToggleOverlayDispl(-1);
	UpdateAll();
	return;
    case 'V':  // in/exclude channel from color display
	my3ddata.ToggleMulDispl(-1);
	UpdateAll();
	return;
    case 'k':  // Creates a new Marker List and inserts a point
        my3ddata.NewMarkerList();
        my3ddata.SetMarker(label.px,label.py,label.pz);
	// my3ddata.AddPoint(label.px,label.py,label.pz);
	UpdateAll();
	return;
    case '\\':  // '\' Creates two new marker lists which are linked to the current list (e.g. for Cell Division)
    	my3ddata.DevideMarkerList(label.px,label.py,label.pz);
	// my3ddata.AddPoint(label.px,label.py,label.pz);
	UpdateAll();
	return;
    case 'K':  // Set marker for Multiple spectral plots
        my3ddata.RemoveMarkerList();
	UpdateAll();
	return;
    case '&':  // remove this active point
	my3ddata.TagMarker();  // Toggles Marker Tag
	System.out.println("Error Tagged Component setValue called\n"); 
	UpdateAll();
	return;
    case 'm':  // Set marker for Multiple spectral plots
        my3ddata.SetMarker(label.px,label.py,label.pz);
	if (my3ddata.FocusDispToMarker)
		{myPanel.setPositions(my3ddata.GetActiveMarker());CalcPrev();}
	if (my3ddata.Advance)
		myPanel.AdvancePos();
	// System.out.println("should have advanced" + my3ddata.Advance); 
	// my3ddata.AddPoint(label.px,label.py,label.pz);
	UpdateAll();
	return;
    case 'M':  // remove this active point
	my3ddata.RemovePoint();  // If possible
	if (my3ddata.FocusDispToMarker)
		{myPanel.setPositions(my3ddata.GetActiveMarker());CalcPrev();}
	UpdateAll();
	return;
    case '$':  // time to say goodbye
        // java.lang.System.exit(0);   // exit the application by ending the java virtual mashine
        // close the main window and free as much memory as possible
        my3ddata.cleanup();
        applet.removeAll();
        otherCanvas1.removeNotify();
        otherCanvas2.removeNotify();
        this.removeNotify();
        System.gc();
	if (applet instanceof View5D_)  // This is ImageJ specific
            ((View5D_) applet).dispose();
        else
        {
            ((View5D) applet).stop();            
            ((View5D) applet).destroy();
        }
        return;
   case 'Q':  // remove all point to the end of the list
	my3ddata.RemoveTrailingPoints();  // If possible
	UpdateAll();
	return;
    case '0':  // Advance marker
    myPanel.RememberOffset();
	my3ddata.AdvancePoint(1);
    myPanel.AdjustOffset();
	if (my3ddata.FocusDispToMarker)
		{myPanel.setPositions(my3ddata.GetActiveMarker());CalcPrev();} // Update the crosshair
	UpdateAll();
	return;
    case '9':  // Devance Marker
    myPanel.RememberOffset();
	my3ddata.AdvancePoint(-1);
    myPanel.AdjustOffset();
	if (my3ddata.FocusDispToMarker)
		{myPanel.setPositions(my3ddata.GetActiveMarker());CalcPrev();}
	UpdateAll();
	return;
    case 'j':  // Set marker for Multiple spectral plots
	my3ddata.AdvanceMarkerList(1);
	if (my3ddata.FocusDispToMarker)
		{myPanel.setPositions(my3ddata.GetActiveMarker());CalcPrev();}
	UpdateAll();
		
	return;
    case 'J':  
	my3ddata.AdvanceMarkerList(-1);
	if (my3ddata.FocusDispToMarker)
		{myPanel.setPositions(my3ddata.GetActiveMarker());CalcPrev();}
	UpdateAll();
	return;
    case 'w':  
	my3ddata.ToggleMarkerListColor(1);
	UpdateAll();
	return;
    case 'W':  
	my3ddata.AutoTrack();
	UpdateAll();
	return;
    case '|': 
    	my3ddata.AlignOffsetsToTrack();
    	UpdateAll();
    	return;
    case '{': 
    	my3ddata.ResetOffsets();
    	UpdateAll();
    	return;
    case '}': 
    	my3ddata.MarkerListDialog();
    	UpdateAll();
    	return;
    case '@':  
    	AspectLocked.setState(! AspectLocked.getState());
    	return;
    case '#':  
	my3ddata.SubtractTrackedSpot();
        my3ddata.InvalidateProjs(-1);  // all projections are invalid
        my3ddata.InvalidateSlices();
	UpdateAll();
	return;
    case 's': 
        AlternateViewer xx=new AlternateViewer(applet);  // clone the data and open a new viewer
        My3DData nd=new My3DData(my3ddata);
        xx.Assign3DData(applet, null, nd);
	UpdateAll();
        return;
    case 'P':
	my3ddata.ToggleProj(DimNr,false);
	label.CoordsChanged();  
	UpdateAll();
        return ;
    case 'p':
	my3ddata.ToggleProj(DimNr,true);
	UpdateAll();
        return ;
    case '^':
	my3ddata.ActElement().AdvanceReadMode();
	my3ddata.InvalidateSlices();
	my3ddata.InvalidateProjs(-1);
	UpdateAll();
        return ;
    case 'S':
        if (! myPanel.ROIstarted && ! LineROIStarted)   // otherwise the user has to finish the ROI first
        {
            my3ddata.ToggleSquareROIs();
            my3ddata.InvalidateProjs(-1);  // all projections are invalid
            UpdateAll();  // To display current ROIs
        }
        return;
    case 'x':  // mark active element for x-coordinate of histogram
        my3ddata.MarkAsHistoDim(0);
	label.CoordsChanged();
        return;
    case 'y':  // mark active element for x-coordinate of histogram
        my3ddata.MarkAsHistoDim(1);
	label.CoordsChanged();
        return;
    case 'z':  // mark active element for x-coordinate of histogram
        my3ddata.MarkAsHistoDim(2);
	label.CoordsChanged();
        return;
    case 'Z':  // Zoom in to match square ROI
        double scalex= getBounds().width/(1.5+otherCanvas1.ROIe-otherCanvas1.ROIs);
        double scaley= getBounds().height/(1.5+otherCanvas2.ROIe-otherCanvas2.ROIs);
        if (AspectLocked.getState())
            if (scalex > scaley)
                scalex = scaley;
            else
                scaley = scalex;
        otherCanvas1.scale = scalex;
        otherCanvas2.scale = scaley;
        otherCanvas1.dadd -= MouseFromX(otherCanvas1.ROIs-0.5);
        otherCanvas2.dadd -= MouseFromY(otherCanvas2.ROIs-0.5);
	UpdateAll();
        return;
    case 'U':  // set Gating-element to this
        my3ddata.toggleGate(-1);   // will force the thresholds to be copied if necessary
	UpdateAll();
        return;
    case 'u':  // set Gating-element to this
        my3ddata.setGate();  // toggles. Will force the thresholds to be copied if necessary
	UpdateAll();
        return;
    case 'H':  // apply Histogram ROI selection to data
    case 'h':  // generate a Histogram
        SpawnHistogram(myChar == 'H');   // if 'H' the histogram will be forced, even if allready present
        UpdateAllPanels();
        //my3ddata.MyHistogram.UpdateAll();
        return;
    case 'C':
	my3ddata.ToggleColor();
	UpdateAll();
	return ;
    case 'c':
	my3ddata.ToggleModel(-1);
	UpdateAll();
	return ;
    case 'd':
	my3ddata.InvertCMap();
	my3ddata.ToggleModel(-1);
	my3ddata.InvertCMap();
	UpdateAll();
	return ;
    case 'o':   // in color mode, all will be set to the current state
	my3ddata.ToggleOvUn(-1);
	UpdateAll();
	return ;
    case 'O':
	my3ddata.ToggleLog(-1);
	UpdateAll();
	return ;
    case ',':  // moves along time direction
       	myPanel.RememberOffset();
        my3ddata.nextTime(1);
       	myPanel.AdjustOffset();
       	UpdateAll();
	return ;
    case '.':  // moves along time direction
       	myPanel.RememberOffset();
        my3ddata.nextTime(-1);
       	myPanel.AdjustOffset();
       	UpdateAll();
	return ;        
    case '(':   // moves into Z-direction
	if (PositionValue < MaxPos-1)
	    PositionValue++;
        UpdateAll();
	//pickImage();
	//re'paint();
	//updateothers(xprev,yprev);
	return ;
    case ')':
	if (PositionValue > 0)
	    PositionValue--;
	UpdateAll();
	// pickImage();
	// repaint();
	// updateothers(xprev,yprev);
	return ;
    case 'A':
    	
        int xp = getCrossHairX();   // pixel position of the crosshair
        int yp = getCrossHairY();
    	
        otherCanvas1.scale *= 1.25;
        otherCanvas2.scale *= 1.25;
        otherCanvas1.dadd += xp-getCrossHairX();
        otherCanvas2.dadd += yp-getCrossHairY();
        otherCanvas1.LimitPosition(getBounds().width);
        otherCanvas2.LimitPosition(getBounds().height);
        if (otherCanvas2.AspectLocked.getState() || otherCanvas1.AspectLocked.getState())
        {
            double zp = getCrossHairZ();
            scale *= 1.25;
            dadd += zp-getCrossHairZ();
            LimitPosition(getBounds().height);
        }

        RedrawAll();
        label.CoordsChanged(); 
        return ;
    case 'a':
        xp = getCrossHairX();   // pixel position of the crosshair
        yp = getCrossHairY();
    	
        otherCanvas1.scale /= 1.25;
        otherCanvas2.scale /= 1.25;
        otherCanvas1.dadd += xp-getCrossHairX();
        otherCanvas2.dadd += yp-getCrossHairY();
        otherCanvas1.LimitPosition(getBounds().width);
        otherCanvas2.LimitPosition(getBounds().height);
        if (otherCanvas2.AspectLocked.getState() || otherCanvas1.AspectLocked.getState())
        {
            double zp = getCrossHairZ();
            scale /= 1.25;
            dadd += zp-getCrossHairZ();
            LimitPosition(getBounds().height);
        }

        RedrawAll();
        label.CoordsChanged(); 
        return ;
	
    case '>':
        if (! otherCanvas1.AspectLocked.getState() && ! otherCanvas2.AspectLocked.getState())
        {
	dadd -= 0.125*scale*PositionValue;
	scale *= 1.25;
	RedrawAll();
        }
	return ;
    case '<':
        if (! otherCanvas1.AspectLocked.getState() && ! otherCanvas2.AspectLocked.getState())
        {
	if (scale > 1.0)
	    {
		scale /= 1.125;
		dadd += 0.125*scale*PositionValue;
                RedrawAll();
	    }
        }
	return ;
    case 'q':
	DispPlot = ! DispPlot;  // Toggle plot display
	repaint();
	return ;
    
    //case ':':
	//xadd += 5;
	//repaint();
	//return ;
    //case ';':
    //	xadd -= 5;
	//repaint();
	//return ;
    case '!':   // transfers the colormap threshold to data threshold
	// my3ddata.addLThresh(0.02);
	my3ddata.CThreshToValThresh(-1,0.0,1.0);
	UpdateAll();
	return ;
    case '1':
        if (my3ddata.GateActive && (my3ddata.GateElem == my3ddata.GateElem))
            {
            my3ddata.addLThresh(0.02);
            }
        else
            my3ddata.adjustColorMapLThresh(0.02);
	UpdateAll();
	return ;
    case '2':
        if (my3ddata.GateActive && (my3ddata.GateElem == my3ddata.GateElem))
            {
            my3ddata.addLThresh(-0.02);
            }
        else
            my3ddata.adjustColorMapLThresh(-0.02);
 	UpdateAll();
	return ;
    case '3':
        if (my3ddata.GateActive && (my3ddata.GateElem == my3ddata.GateElem))
            {
            my3ddata.addUThresh(0.02);
            }
        else
            my3ddata.adjustColorMapUThresh(0.02);
	UpdateAll();
	return ;
    case '4':
        if (my3ddata.GateActive && (my3ddata.GateElem == my3ddata.GateElem))
            {
            my3ddata.addUThresh(-0.02);
            }
        else
            my3ddata.adjustColorMapUThresh(-0.02);
        UpdateAll();
	return ;
    case '5':
        if (my3ddata.GateActive && (my3ddata.GateElem == my3ddata.GateElem))
            {
            my3ddata.addLThresh(0.002);
            }
        else
            my3ddata.adjustColorMapLThresh(0.002);
	UpdateAll();
	return ;
    case '6':
        if (my3ddata.GateActive && (my3ddata.GateElem == my3ddata.GateElem))
            {
            my3ddata.addLThresh(-0.002);
            }
        else
            my3ddata.adjustColorMapLThresh(-0.002);
	UpdateAll();
	return ;
    case '7':
        if (my3ddata.GateActive && (my3ddata.GateElem == my3ddata.GateElem))
            {
            my3ddata.addUThresh(0.002);
            }
        else
            my3ddata.adjustColorMapUThresh(0.002);
	UpdateAll();
	return ;
    case '8':
        if (my3ddata.GateActive && (my3ddata.GateElem == my3ddata.GateElem))
            {
            my3ddata.addUThresh(-0.002);
            }
        else
            my3ddata.adjustColorMapUThresh(-0.002);
	UpdateAll();
	return ;
    case 'i':
	//my3ddata.initThresh();
	InitScaling();
	CalcPrev();
	UpdateAll();
	return ;
    case 'I':
	my3ddata.initGlobalThresh();
	InitScaling();
	CalcPrev();
	UpdateAll();
	return ;
    case 'X':  // export to ImageJ
	if (applet instanceof View5D_)
            if (DispPlot)
                ExportValues();   // a new stack in ImageJ will be generated
            else
                my3ddata.Export(DimNr, (int) PositionValue);   // a new stack in ImageJ will be generated
	return ;
    case 'Y':  // generate ROI Mask
        my3ddata.GenerateMask(DimNr);   // a new element will be generated
	UpdateAll();  // To display mask
	return ;
    case 'l':
	if (applet instanceof View5D_)
	   ((View5D_) applet).LoadImg(0);  
	else
	   my3ddata.Load(my3ddata.PrevType,my3ddata.PrevBytes,my3ddata.PrevBits,((View5D) applet).filename,((View5D) applet));   // commented out for now, since the document base is unknown for non-applets
        UpdateAllPanels();
	UpdateAll();  // To display mask
	return;
    case 'L':
	my3ddata.LoadMarkers();  
        UpdateAllPanels();
	return;
    
    case '?':
        label.Help();
        return ;
        }
}

public void keyReleased(KeyEvent e) {
}

}
