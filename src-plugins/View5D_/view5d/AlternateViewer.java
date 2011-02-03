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
// import java.awt.image.ColorModel.*;
package view5d;

import java.awt.event.*;
// import java.awt.color.*;
import java.awt.*;

public class AlternateViewer extends Frame implements WindowListener {
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
    if (!(applet instanceof View5D))
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
    if (!(applet instanceof View5D))
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
