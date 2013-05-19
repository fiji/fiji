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
