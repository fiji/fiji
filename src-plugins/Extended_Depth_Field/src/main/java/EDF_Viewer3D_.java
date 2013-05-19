//==============================================================================
//
// Project: EDF - Extended Depth of Focus
//
// Author: Daniel Sage
//
// Organization: Biomedical Imaging Group (BIG)
// Ecole Polytechnique Federale de Lausanne (EPFL), Lausanne, Switzerland
//
// Information: http://bigwww.epfl.ch/demo/edf/
//
// Reference: B. Forster, D. Van De Ville, J. Berent, D. Sage, M. Unser
// Complex Wavelets for Extended Depth-of-Field: A New Method for the Fusion
// of Multichannel Microscopy Images, Microscopy Research and Techniques,
// 65(1-2), pp. 33-42, September 2004.
//
// Conditions of use: You'll be free to use this software for research purposes,
// but you should not redistribute it without our consent. In addition, we
// expect you to include a citation or acknowledgment whenever you present or
// publish results that are based on it.
//
// History:
// - Updated (Daniel Sage, 21 December 2010)
//
//==============================================================================

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import surfacemap.SurfaceMap3D;

public class EDF_Viewer3D_ extends JDialog implements ActionListener {

	private Vector indexMap;
	private Vector indexTexture;

	private JPanel jContentPane = null;
	private GridBagLayout layout;
	private GridBagConstraints constraint;
	private JButton bnHeightMap;
	private JButton bnClose;
	private JLabel lblMap;
	private JLabel lblTexture;
	private JComboBox choiceMap;
	private JComboBox choiceTexture;
	private String STR_CHOICE = "<Select an image>";
	private boolean isMapSelected = false;
	private boolean isTextureSelected = false;

	/**
	 * Constructor extends JDialog.
	 */
	public EDF_Viewer3D_() {
		super();
		super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setContentPane(getJContentPane());
		setTitle("EDF Viewer 3D");

		pack();
		setVisible(true);
		GUI.center(this);
	}

	/**
	 * This method initializes jContentPane.
	 */
	private JPanel getJContentPane() {

		if (jContentPane == null) {
			// Layout
			layout = new GridBagLayout();
			constraint = new GridBagConstraints();

			// List of images
			lblMap 		= new JLabel("Height-Map");
			lblTexture  = new JLabel("Texture");

			// Buttons
			bnClose 		= new JButton("Close");
			bnHeightMap 	= new JButton("Show Height-Map");
			bnHeightMap.setEnabled(false);

			// Panel buttons
			JPanel pnButtons = new JPanel();
			pnButtons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
			pnButtons.add(bnClose);
			pnButtons.add(bnHeightMap);

			jContentPane = new JPanel(layout);
			choiceMap	= new JComboBox();
			choiceMap.addActionListener(this);
			choiceTexture = new JComboBox();
			choiceTexture.addActionListener(this);
			choiceMap.addItem(STR_CHOICE);
			choiceTexture.addItem(STR_CHOICE);
			indexMap  = setMapList(choiceMap);
			indexTexture = setTextureList(choiceTexture);

			JPanel pnControls = new JPanel(layout);
			pnControls.setBorder(BorderFactory.createEtchedBorder());
			addComponent(pnControls, 1, 0, 1, 1, 5, lblMap);
			addComponent(pnControls, 1, 1, 1, 1, 5, choiceMap);
			addComponent(pnControls, 2, 0, 1, 1, 5, lblTexture);
			addComponent(pnControls, 2, 1, 1, 1, 5, choiceTexture);

			addComponent(jContentPane, 1, 0, 3, 1, 10, pnControls);
			addComponent(jContentPane, 3, 0, 3, 1, 10, pnButtons);

			bnClose.addActionListener(this);
			bnHeightMap.addActionListener(this);

			if (indexMap.size() == 0 || indexTexture.size() == 0) {
				IJ.error("You have to open a Topology image and a Texture image");
			}

		}
		return jContentPane;
	}

	/**
	 * Add a component in a panel in the northeast of the cell.
	 */
	private void addComponent(JPanel pn, int row, final int col, int width, final int height, int space, JComponent comp) {
		constraint.gridx = col;
		constraint.gridy = row;
		constraint.gridwidth = width;
		constraint.gridheight = height;
		constraint.anchor = GridBagConstraints.NORTHWEST;
		constraint.insets = new Insets(space, space, space, space);
		constraint.weightx = IJ.isMacintosh()?90:100;
		constraint.fill = GridBagConstraints.HORIZONTAL;
		layout.setConstraints(comp, constraint);
		pn.add(comp);
	}

	/**
	 * Implements the actionPerformed method of the ActionListener.
	 */
	public synchronized void actionPerformed(ActionEvent e) {

		Object source = e.getSource();
		if (source == bnClose) {
			dispose();
		}
		else if (source == bnHeightMap) {

			int index = choiceMap.getSelectedIndex() - 1;
			Integer map = (Integer)indexMap.elementAt(index);
			ImagePlus imp = WindowManager.getImage(map.intValue());

			index = choiceTexture.getSelectedIndex() - 1;
			Integer Texture = (Integer)indexTexture.elementAt(index);
			ImagePlus impTexture = WindowManager.getImage(Texture.intValue());

			SurfaceMap3D viewer = new SurfaceMap3D(imp, impTexture);
			Thread thread = new Thread(viewer);
			thread.start();
			this.dispose();
		}
		else if (source == choiceMap){
			if (choiceMap.getSelectedIndex() !=0) {
				isMapSelected = true;
			}
			else{
				isMapSelected = false;
			}
			if (isMapSelected && isTextureSelected)
				this.bnHeightMap.setEnabled(true);
			else
				this.bnHeightMap.setEnabled(false);
		}
		else if (source == choiceTexture){
			if (choiceTexture.getSelectedIndex() !=0) {
				isTextureSelected = true;
			}
			else{
				isTextureSelected = false;
			}
			if ( isMapSelected && isTextureSelected)
				this.bnHeightMap.setEnabled(true);
			else
				this.bnHeightMap.setEnabled(false);
		}
	}

	/**
	 * Set the list of images.
	 */
	private Vector setTextureList(JComboBox choice) {
		Vector index = new Vector(1,1);
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			return index;
		}
		index.removeAllElements();
		Vector list = new Vector(1,1);
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if (imp != null)
				if (imp.getStackSize() == 1)
					if ((imp.getType() == ImagePlus.COLOR_RGB) ||
							(imp.getType() == ImagePlus.GRAY8) ||
							(imp.getType() == ImagePlus.GRAY16) ||
							(imp.getType() == ImagePlus.GRAY32) ||
							(imp.getType() == ImagePlus.COLOR_256)) {
						list.addElement(imp.getTitle());
						index.addElement(new Integer(wList[i]));
					}
		}

		if (list.size() <= 0 && choice == choiceMap) {
			IJ.error("No suitable texture images are open.");
			return index;
		}

		for (int i=0; i<list.size(); i++)
			choice.addItem((String)list.elementAt(i));

		return index;
	}

	/**
	 * Set the list of images.
	 */
	private Vector setMapList(JComboBox choice)	{

		Vector index = new Vector(1,1);
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			if (choice == choiceMap) // To display the message only one time
				IJ.error("No images are open.");
			return index;
		}
		Vector list = new Vector(1,1);
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if (imp != null)
				if (imp.getStackSize() == 1)
					if ((imp.getType() == ImagePlus.GRAY32) ||
							(imp.getType() == ImagePlus.GRAY8) ||
							(imp.getType() == ImagePlus.GRAY16)) {
						list.addElement(imp.getTitle());
						index.addElement(new Integer(wList[i]));
					}
		}

		if (list.size() <= 0 && choice == choiceMap) {
			IJ.error("No suitable map images are open. Try Grayscale Images.");
			return index;
		}

		for (int i=0; i<list.size(); i++)
			choice.addItem((String)list.elementAt(i));

		return index;
	}

}
