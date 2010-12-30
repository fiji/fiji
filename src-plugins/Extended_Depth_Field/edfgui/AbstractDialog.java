//==============================================================================
//
// Project: EDF - Extended Depth of Focus
//
// Author: Alex Prudencio
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

package edfgui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;
import javax.swing.Timer;

import edf.LogSingleton;

public abstract class AbstractDialog extends JFrame {

	public final static int ONE_SECOND = 1000;
	public Parameters parameters;
	public Thread threadEdf;
	public Thread threadTopoProc;
	public JProgressBar jProgressBar = null;
	public Timer timer;
	public JLabel jLabelMemMessage = null;
	protected GridBagLayout gbLayout = new GridBagLayout();
	protected GridBagConstraints gbConstraints = new GridBagConstraints();
	protected String STR_COPYRIGHT = "(c) 2010, BIG, EPFL, Switzerland";
	protected LogSingleton log = LogSingleton.getInstance();

	/**
	 * Constructor.
	 * Start the timer
	 */
	public AbstractDialog() {
		super();
		parameters = new Parameters();
		timer = new Timer(ONE_SECOND/2, new TimerListener());
		jLabelMemMessage = new JLabel("");
	}

	/**
	 * Add a component in a panel in the northeast of the cell.
	 */
	protected void addComponent(JPanel pn, int row, int col, int width, int height, int space, Component comp) {
		gbConstraints.gridx = col;
		gbConstraints.gridy = row;
		gbConstraints.gridwidth = width;
		gbConstraints.gridheight = height;
		gbConstraints.weightx = 0.0;
		gbConstraints.weighty = 0.0;
		gbConstraints.anchor = GridBagConstraints.NORTHWEST;
		gbConstraints.fill = GridBagConstraints.NONE;
		gbConstraints.insets = new Insets(space, space, space, space);
		gbLayout.setConstraints(comp, gbConstraints);
		pn.add(comp);
	}

	/**
	 * Add a component in a panel in the northeast of the cell.
	 */
	protected void addComponent(JToolBar pn, int row, int col, int width, int height, int space, Component comp) {
		gbConstraints.gridx = col;
		gbConstraints.gridy = row;
		gbConstraints.gridwidth = width;
		gbConstraints.gridheight = height;
		gbConstraints.weightx = 0.0;
		gbConstraints.weighty = 0.0;
		gbConstraints.anchor = GridBagConstraints.NORTHWEST;
		gbConstraints.fill = GridBagConstraints.NONE;
		gbConstraints.insets = new Insets(space, space, space, space);
		gbLayout.setConstraints(comp, gbConstraints);
		pn.add(comp);
	}

	/**
	 * Cleanup properly and stop the threads.
	 */
	public void cleanup(){
		threadEdf = null;
		threadTopoProc = null;
		if( timer != null){
			timer.stop();
			timer = null;
		}
		log.clear();
		log = null;
		System.gc();
	}

	/**
	 * The actionPerformed method in this class
	 * is called each time the Timer "goes off".
	 */
	class TimerListener implements ActionListener {
		private DecimalFormat formatMem = new DecimalFormat("000.0");
		public void actionPerformed(ActionEvent arg0) {
			int length = log.getProgessLength();
			jProgressBar.setValue(length);
			double mem = Runtime.getRuntime().freeMemory()/1024.0/1024.0;
			double total = Runtime.getRuntime().totalMemory()/1024.0/1024.0;
			jLabelMemMessage.setText( "Memory "+ formatMem.format(mem) + "/" + formatMem.format(total) + "MB");
			jLabelMemMessage.repaint();
			jProgressBar.setString(log.getElapsedTime());
			if (length==100)
				timer.stop();
		}
	}

}
