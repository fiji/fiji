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

//import java.io.*;
//import java.lang.*;
// import java.lang.Number.*;
package view5d;

import java.awt.event.*;
import java.awt.*;
// import ij.*;
// import ij.gui.*;

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
    		this(title, new Frame());
    		// this(title, WindowManager.getCurrentImage()!=null?(Frame)WindowManager.getCurrentImage().getWindow():IJ.getInstance()!=null?IJ.getInstance():new Frame());
	}

    /** Creates a new GenericDialog using the specified title and parent frame. */
    public AGenericDialog(String title, Frame parent) {
		super(parent==null?new Frame():parent, title, true);  // True means "modal"
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
	// GUI.center(this);
	//show();
    //setBackground(Color.black);
    setVisible(true);
    //repaint();
    //validate();
    toFront();
    repaint();
    update(this.getGraphics()); // nasty, but necessary for Matlab single-thread behaviour
    //show();
    // while (isShowing()) {}
    }

    public void repaint() {
    	super.repaint();
        update(this.getGraphics()); // nasty, but necessary for Matlab single-thread behaviour
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
        String mysplit[] = text.split("\n");
        int lines= mysplit.length;
    	if (lines>1)
    	{	int columns=0;
    	for (int i=0;i<lines;i++)
    		if (mysplit[i].length() > columns)
    			columns=mysplit[i].length();
    		// theLabel = new MultiLineLabel(text);
    		TextArea myTextArea=new TextArea(text,lines,columns,TextArea.SCROLLBARS_NONE );
    		myTextArea.setEditable(false);
    		theLabel = myTextArea;
    	}
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
	if (canceled)
        System.out.println("canceled!\n");
	else
        System.out.println("okay!\n");
    }
    
    public void windowClosed (WindowEvent e) { }
    public void windowOpened (WindowEvent e) {canceled=true;
    	update(this.getGraphics()); // nasty, but necessary for Matlab single-thread behaviour
    }

//    public void setSize(Dimension d) { 
//    	super.setSize(d);
//    	update(this.getGraphics()); // nasty, but necessary for Matlab single-thread behaviour
//    }
//    public void setSize(int width, int height) { 
//    	super.setSize(width, height);
//    	update(this.getGraphics()); // nasty, but necessary for Matlab single-thread behaviour
//    }
    
    public void windowIconified (WindowEvent e) { }
    public void windowDeiconified (WindowEvent e) { }
    public void windowActivated (WindowEvent e) { }
    public void windowDeactivated (WindowEvent e) { }
        
    public void itemStateChanged(ItemEvent e) { }

    public void focusGained(FocusEvent e) {
                Component c = e.getComponent();
                if (c instanceof TextField)
                        ((TextField)c).selectAll();
                update(this.getGraphics()); // nasty, but necessary for Matlab single-thread behaviour
        }

    public void focusLost(FocusEvent e) {
                Component c = e.getComponent();
                if (c instanceof TextField)
                        ((TextField)c).select(0,0);
    }


    public void actionPerformed(ActionEvent e) {
        // System.out.println("action performed!\n");
		canceled = (e.getSource()==cancel);
		//if (canceled)
	    //    System.out.println("canceled!\n");
		//else
	    //    System.out.println("okay!\n");
	    //System.out.println("Generic Dialog closing!\n");
		setVisible(false);
		// dispose();    // Not good. This will close the whole application under some systems
	}
}
