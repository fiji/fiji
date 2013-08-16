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
