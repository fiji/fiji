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

import java.awt.event.*;
import java.awt.*;
// import ij.gui.GenericDialog;  
// import ij.gui.*;  // only needed when based on GenericDialog from ImageJ instead of AGenergicDialog

class ANGenericDialog extends AGenericDialog implements ActionListener, FocusListener, ItemListener {             // derived from the ImageJ class or from the emulation AGernericDialog
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
            Frame aframe=new Frame();
        	//aframe=WindowManager.getCurrentImage()!=null? (Frame)WindowManager.getCurrentImage().getWindow():IJ.getInstance()!=null?IJ.getInstance():new Frame();

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
            Frame aframe=new Frame();
            	// aframe=WindowManager.getCurrentImage()!=null? (Frame)WindowManager.getCurrentImage().getWindow():IJ.getInstance()!=null?IJ.getInstance():new Frame();
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
