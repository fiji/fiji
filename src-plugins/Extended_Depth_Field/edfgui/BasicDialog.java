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
////==============================================================================

package edfgui;

import ij.ImagePlus;
import ij.WindowManager;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;

import edf.Tools;


public class BasicDialog extends AbstractDialog
	implements ActionListener, ItemListener, WindowListener {

	private static final long serialVersionUID = 1L;
	private JPanel jContentPane = null;
	private JSlider jSliderQuality = null;
	private JLabel jLabelHeightMapQuality = null;
	private JSlider jSliderRegularization = null;
	private JPanel jPanelControls = null;
	private JPanel jPanelButtons = null;
	private JButton jButtonRun = null;
	private JButton jButtonClose = null;
	private JPanel jPanelBotRight = null;
	private JLabel jLabelTradeOff = null;
	private boolean isColorStack = false;
	private JPanel jPanelBottom;
	private JPanel jPanelBotCenter;
	private JCheckBox jCheckBoxShowHeightMap = null;
	private JCheckBox jCheckBoxShow3D = null;
	private JButton jButtonCredits = null;
	private int[] nScalesAndSize;
	private int nScales;
	private boolean applet = false;

	/**
	 * This is the default constructor.
	 */
	public BasicDialog( int[] imageSize, boolean isColorStack, boolean applet) {
		super();
		//super.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.isColorStack = isColorStack;
		parameters.color = isColorStack;
		parameters.showTopology = true;
		this.applet = applet;
		nScalesAndSize = Tools.computeScaleAndPowerTwoSize(imageSize[0],imageSize[1]);
		nScales = nScalesAndSize[0];
		parameters.maxScales = nScales;
		addWindowListener(this);
	}

	/**
	 * This method initializes this.
	 */
	public void initialize() {
		this.setContentPane(getJContentPane());
		this.setTitle("Extended Depth of Field" + (applet ? " [Applet]" : ""));
	}

	/**
	 * This method initializes jContentPane.
	 */
	private JPanel getJContentPane() {
		if ( jContentPane == null ) {
			jContentPane = new JPanel(gbLayout);

			this.addComponent(jContentPane,1,0,1,1,10,getJPanelControls());
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbConstraints.weightx = 1.0;
			gbConstraints.weighty = 1.0;
			gbLayout.setConstraints(getJPanelControls(), gbConstraints);

			this.addComponent(jContentPane,2,0,1,1,0,getJPanelButtons());
			gbConstraints.fill = GridBagConstraints.HORIZONTAL;
			gbLayout.setConstraints(getJPanelButtons(),gbConstraints);

			this.addComponent(jContentPane,3,0,1,1,0,getJPanelBottom());
			gbConstraints.fill = GridBagConstraints.HORIZONTAL;
			gbLayout.setConstraints(getJPanelBottom(),gbConstraints);

		}
		return jContentPane;
	}

	/**
	 * This method initializes jSliderQuality.
	 */
	private JSlider getJSliderQuality() {
		if (jSliderQuality == null) {
			jSliderQuality = new JSlider();
			jSliderQuality.setMajorTickSpacing(2);
			jSliderQuality.setMaximum(4);
			jSliderQuality.setPaintLabels(true);
			jSliderQuality.setPaintTicks(true);
			jSliderQuality.setSnapToTicks(true);
			jSliderQuality.setMinimumSize(new java.awt.Dimension(200,50));
			jSliderQuality.setPreferredSize(new java.awt.Dimension(200,50));
			jSliderQuality.setMinorTickSpacing(1);

			Hashtable labelTable = new Hashtable();
			labelTable.put( new Integer( 0 ), new JLabel("Fast") );
			labelTable.put( new Integer( 2 ), new JLabel("Medium") );
			labelTable.put( new Integer( 4 ), new JLabel("High") );
			jSliderQuality.setLabelTable( labelTable );
			jSliderQuality.setValue(0);
		}
		return jSliderQuality;
	}

	/**
	 * This method sets jSliderQuality.
	 */
	public void setJSliderQuality(int value) {
		if (jSliderQuality != null) {
			jSliderQuality.setValue(value);
		}
	}

	/**
	 * This method initializes getJSliderRegularization.
	 */
	private JSlider getJSliderRegularization() {
		if (jSliderRegularization == null) {
			jSliderRegularization = new MySlider();
			jSliderRegularization.setMaximum(4);
			jSliderRegularization.setMinorTickSpacing(1);
			jSliderRegularization.setPaintTicks(true);
			jSliderRegularization.setPaintLabels(true);
			jSliderRegularization.setSnapToTicks(true);
			jSliderRegularization.setMinimumSize(new java.awt.Dimension(200,50));
			jSliderRegularization.setPreferredSize(new java.awt.Dimension(200,50));
			jSliderRegularization.setMajorTickSpacing(2);

			Hashtable labelTable = new Hashtable();
			labelTable.put( new Integer( 0 ), new JLabel("None") );
			labelTable.put( new Integer( 2 ), new JLabel("Medium") );
			labelTable.put( new Integer( 4 ), new JLabel("Smooth") );
			jSliderRegularization.setLabelTable( labelTable );
			jSliderRegularization.setValue(0);
		}
		return jSliderRegularization;
	}

	/**
	 * This method sets jSliderRegularization
	 */
	public void setJSliderRegularization(int value) {
		if (jSliderRegularization != null) {
			jSliderRegularization.setValue(value);
		}
	}

	/**
	 * This method initializes jPanelControls.
	 */
	private JPanel getJPanelControls() {
		if (jPanelControls == null) {
			jPanelControls = new JPanel(gbLayout);
			jPanelControls.setBorder(BorderFactory.createEtchedBorder());

			jLabelTradeOff = new JLabel("Speed/Quality");
			jLabelHeightMapQuality = new JLabel("Height-map reg.");

			addComponent(jPanelControls, 0,0,1,1,3, jLabelTradeOff);
			addComponent(jPanelControls, 0,1,1,1,3, getJSliderQuality());
			addComponent(jPanelControls, 1,0,1,1,3, jLabelHeightMapQuality);
			addComponent(jPanelControls, 1,1,1,1,3, getJSliderRegularization());
			addComponent(jPanelControls, 2,0,2,1,3, getJCheckBoxShowTopology());
			addComponent(jPanelControls, 3,0,2,1,3, getJCheckBoxShow3D());
		}
		return jPanelControls;
	}

	/**
	 * This method initializes jPanelButtons.
	 */
	private JPanel getJPanelButtons() {
		if (jPanelButtons == null) {
			jPanelButtons = new JPanel(gbLayout);
			addComponent(jPanelButtons, 0, 0, 1, 1, 5, getJButtonCredits());
			gbConstraints.anchor = GridBagConstraints.WEST;
			gbConstraints.weightx = 1.0;
			gbConstraints.insets = new Insets(5,10,5,5);
			gbLayout.setConstraints(getJButtonCredits(),gbConstraints);
			addComponent(jPanelButtons, 0, 1, 1, 1, 5, getJButtonClose());
			gbConstraints.anchor = GridBagConstraints.EAST;
			gbLayout.setConstraints(getJButtonClose(),gbConstraints);
			addComponent(jPanelButtons, 0, 2, 1, 1, 5, getJButtonRun());
			gbConstraints.anchor = GridBagConstraints.EAST;
			gbConstraints.insets = new Insets(5,5,5,10);
			gbLayout.setConstraints(getJButtonRun(),gbConstraints);
		}
		return jPanelButtons;
	}

	/**
	 * This method initializes jButtonRun.
	 */
	private JButton getJButtonRun() {
		if (jButtonRun == null) {
			jButtonRun = new JButton("Run");
			jButtonRun.addActionListener(this);
		}
		return jButtonRun;
	}

	/**
	 * This method initializes jButtonClose.
	 */
	private JButton getJButtonClose() {
		if (jButtonClose == null) {
			jButtonClose = new JButton("Close");
			jButtonClose.addActionListener(this);
		}
		return jButtonClose;
	}


	/**
	 * This method initializes jPanelBottom.
	 */
	private JPanel getJPanelBottom() {
		if (jPanelBottom == null) {
			jPanelBottom = new JPanel(gbLayout);
			addComponent(jPanelBottom,1,0,1,1,0,getJPanelBotCenter());
			gbConstraints.weightx = 1.0;
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbLayout.setConstraints(getJPanelBotCenter(),gbConstraints);
			addComponent(jPanelBottom,1,1,1,1,0,getJPanelBotRight());
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbLayout.setConstraints(getJPanelBotRight(),gbConstraints);
		}
		return jPanelBottom;
	}

	/**
	 * This method initializes jPanelBotLeft.
	 */
	private JPanel getJPanelBotCenter() {
		if (jPanelBotCenter == null) {
			jLabelMemMessage = new JLabel(STR_COPYRIGHT);
			GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
			gridBagConstraints11.gridx = 0;
			gridBagConstraints11.gridy = 0;
			gridBagConstraints11.anchor = java.awt.GridBagConstraints.WEST;
			gridBagConstraints11.fill = java.awt.GridBagConstraints.BOTH;
			jPanelBotCenter = new JPanel(new GridBagLayout());
			jPanelBotCenter.setBorder(BorderFactory.createEtchedBorder());
			jPanelBotCenter.setPreferredSize(new java.awt.Dimension(200,28));
			jPanelBotCenter.setMinimumSize(new java.awt.Dimension(200,28));
			jPanelBotCenter.add(jLabelMemMessage, gridBagConstraints11);
		}
		return jPanelBotCenter;
	}

	/**
	 * This method initializes jPanelBoRight.
	 */
	private JPanel getJPanelBotRight() {
		if (jPanelBotRight == null) {
			jPanelBotRight = new JPanel();
			jPanelBotRight.setBorder(BorderFactory.createEtchedBorder());
			jPanelBotRight.setMinimumSize(new java.awt.Dimension(150,28));
			jPanelBotRight.setPreferredSize(new java.awt.Dimension(150,28));
			jPanelBotRight.add(getJProgressBar(), null);
		}
		return jPanelBotRight;
	}

	/**
	 * This method initializes jProgressBar.
	 */
	private JProgressBar getJProgressBar() {
		if (jProgressBar == null) {
			jProgressBar = new JProgressBar(0,100);
			jProgressBar.setStringPainted(true);
			jProgressBar.setPreferredSize(new java.awt.Dimension(130,16));
		}
		return jProgressBar;
	}

	/**
	 * This method initializes jCheckBoxShowTopology.
	 */
	private JCheckBox getJCheckBoxShowTopology() {
		if (jCheckBoxShowHeightMap == null) {
			jCheckBoxShowHeightMap = new JCheckBox();
			jCheckBoxShowHeightMap.setText("Compute and show the Height-map");
			jCheckBoxShowHeightMap.setSelected(true);
			jCheckBoxShowHeightMap.addItemListener(this);
		}
		return jCheckBoxShowHeightMap;
	}

	/**
	 * This method initializes jCheckBoxShowTopology.
	 */
	public void setJCheckBoxShowTopology(boolean value) {
		if (jCheckBoxShowHeightMap != null) {
			jCheckBoxShowHeightMap.setSelected(value);
		}
	}

	/**
	 * This method initializes jCheckBoxShow3D.
	 */
	private JCheckBox getJCheckBoxShow3D() {
		if (jCheckBoxShow3D == null) {
			jCheckBoxShow3D = new JCheckBox();
			jCheckBoxShow3D.setText("Show 3-D view");
		}
		return jCheckBoxShow3D;
	}

	/**
	 * This method initializes jCheckBoxShow3D.
	 */
	public void setJCheckBoxShow3D(boolean value) {
		if (jCheckBoxShow3D != null) {
			jCheckBoxShow3D.setSelected(value);
		}
	}

	/**
	 * This method initializes jButtonCredits.
	 */
	private JButton getJButtonCredits() {
		if (jButtonCredits == null) {
			jButtonCredits = new JButton("About...");
			jButtonCredits.addActionListener(this);
		}
		return jButtonCredits;
	}

	/**
	 * Implements the actionPerformed of the ActionEvent.
	 */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if ( source == this.jButtonCredits) {
			AboutDialog about = new AboutDialog(this);
			about.setVisible(true);
		}
		else if ( source == this.jButtonClose) {
			cleanup();
			dispose();
			System.gc();
			if (applet) {
				int[] id = WindowManager.getIDList();
				for(int i=0; i<id.length; i++) {
					ImagePlus imp = WindowManager.getImage(id[i]);
					imp.close();
				}
			}
		}
		else if ( source == this.jButtonRun) {
			getParameters();
			process();
		}
	}

	/**
	 * Start the processing in a thread.
	 */
	public void process() {
		if (super.threadEdf != null) {
			if(threadEdf.isAlive()) {
				return;
			}
			else {
				super.threadEdf = new ThreadEdf(parameters);
				super.threadEdf.setPriority(Thread.MIN_PRIORITY);
				super.threadEdf.start();
				super.timer.start();
			}
		}else {
			super.threadEdf = new ThreadEdf(parameters);
			super.threadEdf.setPriority(Thread.MIN_PRIORITY);
			super.threadEdf.start();
			super.timer.start();
		}
	}

	/**
	 * Implements itemStateChanged method.
	 */
	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub
		Object source = e.getItemSelectable();

		if (source == this.jCheckBoxShowHeightMap){
			if (e.getStateChange() == ItemEvent.DESELECTED) {
				this.jCheckBoxShow3D.setEnabled(false);
				this.jCheckBoxShow3D.setSelected(false);
				this.jSliderRegularization.setEnabled(false);
				this.jLabelHeightMapQuality.setEnabled(false);
			}else{
				this.jCheckBoxShow3D.setEnabled(true);
				this.jSliderRegularization.setEnabled(true);
				this.jLabelHeightMapQuality.setEnabled(true);
			}
		}
	}

	/**
	 * Retrieve the input user parameters.
	 */
	private void getParameters(){
		parameters.show3dView = this.jCheckBoxShow3D.isSelected();
		parameters.outputColorMap = isColorStack ? Parameters.COLOR_RGB : Parameters.GRAYSCALE;
		parameters.colorConversionMethod = 0;
		int index = (int)jSliderQuality.getValue();
		parameters.setQualitySettings(index);
		index = (int)jSliderRegularization.getValue();
		parameters.setTopologySettings(index);
	}

	/**
	 * Implements the methods for the WindowListener.
	 */
	public void windowClosing(WindowEvent e) {
		cleanup();
		dispose();
	}
	public void windowActivated(WindowEvent e) 		{}
	public void windowClosed(WindowEvent e) 		{}
	public void windowDeiconified(WindowEvent e)	{}
	public void windowIconified(WindowEvent e)		{}
	public void windowOpened(WindowEvent e)			{}
	public void windowDeactivated(WindowEvent e) 	{}


}
