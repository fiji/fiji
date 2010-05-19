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

//import java.io.*;
//import java.lang.*;
// import java.lang.Number.*;
// import java.awt.image.ColorModel.*;
// import java.awt.color.*;

import java.awt.*;

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
