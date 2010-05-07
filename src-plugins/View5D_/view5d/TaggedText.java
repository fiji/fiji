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

// import java.io.*;
import java.awt.*;

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
