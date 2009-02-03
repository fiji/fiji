package bunwarpj;

/**
 * bUnwarpJ plugin for ImageJ(C).
 * Copyright (C) 2005,2006,2007,2008 Ignacio Arganda-Carreras and Jan Kybic 
 *
 * More information at http://biocomp.cnb.csic.es/%7Eiarganda/bUnwarpJ/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

/*====================================================================
|   bUnwarpJClearAll
\===================================================================*/

/*------------------------------------------------------------------*/
import ij.ImagePlus;

import java.awt.Button;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Class to clear all the processes and actions in bUnwarpJ.
 */
public class bUnwarpJClearAll extends Dialog implements ActionListener
{ /* begin class bUnwarpJClearAll */

    /*....................................................................
       Private variables
    ....................................................................*/

    /** image plus for source image */
    private ImagePlus sourceImp;
    /** image plus for target image */
    private ImagePlus targetImp;
    /** point handler for source image */
    private bUnwarpJPointHandler sourcePh;
    /** point handler for target image */
    private bUnwarpJPointHandler targetPh;

    /*....................................................................
       Public methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Create a new instance of bUnwarpJClearAll.
     *
     * @param parentWindow pointer to the parent window
     * @param sourceImp source image representation
     * @param targetImp target image representation
     * @param sourcePh point handler for the source image
     * @param targetPh point handler for the source image
     */
    bUnwarpJClearAll (
       final Frame parentWindow,
       final ImagePlus sourceImp,
       final ImagePlus targetImp,
       final bUnwarpJPointHandler sourcePh,
       final bUnwarpJPointHandler targetPh)
    {
       super(parentWindow, "Removing Points", true);
       this.sourceImp = sourceImp;
       this.targetImp = targetImp;
       this.sourcePh = sourcePh;
       this.targetPh = targetPh;
       setLayout(new GridLayout(0, 1));
       final Button removeButton = new Button("Clear All");
       removeButton.addActionListener(this);
       final Button cancelButton = new Button("Cancel");
       cancelButton.addActionListener(this);
       final Label separation1 = new Label("");
       final Label separation2 = new Label("");
       add(separation1);
       add(removeButton);
       add(separation2);
       add(cancelButton);
       pack();
    } /* end bUnwarpJClearAll */    
    
    /*------------------------------------------------------------------*/
    /**
     * Actions to take place with the dialog for clearing everything
     */
    public void actionPerformed (final ActionEvent ae)
    {
       if (ae.getActionCommand().equals("Clear All")) {
          sourcePh.removePoints();
          targetPh.removePoints();
          setVisible(false);
       }
       else if (ae.getActionCommand().equals("Cancel")) {
          setVisible(false);
       }
    } /* end actionPerformed */

    /*------------------------------------------------------------------*/
    /**
     * Get the insets
     *
     * @return new insets
     */
    public Insets getInsets ()
    {
       return(new Insets(0, 20, 20, 20));
    } /* end getInsets */

} /* end class bUnwarpJClearAll */

