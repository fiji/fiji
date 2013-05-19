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
    
    public double DistTo(APoint other,int dir)  // does not include time and elements
    {
        return coord[dir]-other.coord[dir];
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
