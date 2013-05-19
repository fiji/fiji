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
import java.awt.event.*;

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
