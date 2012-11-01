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

import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edf.Tools;

public class AdvancedDialog extends AbstractDialog
		implements 	ListSelectionListener, ActionListener, ItemListener, WindowListener {

	private NumberFormat percentFormat;
	private DecimalFormat decimalFormat;
	private MyVerifier verifier = new MyVerifier();

	private	String filename = System.getProperty("user.dir") + "/plugins/ExtendedDepthField.txt";

	private String STR_VARIANCE = "Variance";
	private String STR_SOBEL = "Sobel";
	private String STR_REAL_WV = "Real Wavelets";
	private String STR_COMPLEX_WV = "Complex Wavelets";

	private String STR_PRINCIPAL_COMPONENTS = "Principal components";
	private String STR_FIXED_WEIGHTS = "Fixed weights";
	private String STR_MEAN_WEIGHTS = "Mean weights";

	private String STR_OUTPUT_COLOR = "Color RGB";
	private String STR_OUTPUT_GRAYSCALE = "Grayscale";

	private boolean isColor = false;

	private JPanel jContentPane = null;
	private JSplitPane jSplitPane = null;

	private JToolBar jPanelSettings = null;
	private JPanel jPanelButtons = null;
	private JPanel jPanelLeft = null;
	private JPanel jPanelRight = null;
	private JPanel jPanelBottom = null;
	private JPanel jPanelBotRight = null;
	private JPanel jPanelEdf = null;
	private JPanel jPanelColorTreatment = null;
	private JPanel jPanelColorTreatmentOptions = null;
	private JPanel jPanelHeightMap = null;
	private JPanel jPanelPostProcessing = null;
	private JPanel jPanelLog = null;
	private JPanel jPanelEdfParams = null;
	private JPanel jPanelVarianceParams = null;
	private JPanel jPanelDenoising = null;
	private JPanel jPanelDenoisingWv = null;
	private JPanel jPanelDenoisingGeneral = null;
	private JPanel jPanelEdfWv = null;
	private JPanel jPanelWT = null;
	private JPanel jPanelComplexWT = null;
	private JPanel jPanelRealWT = null;
	private JPanel jPanelHeightMapProcessing = null;

	private JButton jButtonRun = null;
	private JButton jButtonClose = null;
	private JButton jButtonCredits = null;
	private JButton jButtonSaveSettings = null;
	private JButton jButtonLoadSettings = null;
	private JButton jButtonResetSettings = null;

	private JScrollPane jScrollPane = null;
	private JTable jTable = null;

	private JCheckBox jCheckBoxLog = null;
	private JCheckBox jCheckBoxHeightMap = null;
	private JCheckBox jCheckBoxTopology3dView = null;
	private JCheckBox jCheckBoxTopoMedian = null;
	private JCheckBox jCheckBoxTopoMorphOpen = null;
	private JCheckBox jCheckBoxTopoMorphClose = null;
	private JCheckBox jCheckBoxTopoGauss = null;
	private JCheckBox jCheckBoxSubBandCC = null;
	private JCheckBox jCheckBoxMajCC = null;
	private JCheckBox jCheckBoxDenoising = null;

	private LogPane logPane = null;

	private JComboBox jComboBoxEdf = null;
	private JComboBox jComboBoxMedianWindowSize = null;
	private JComboBox jComboBoxVarWindowSize = null;
	private JComboBox jComboBoxColorTreatment = null;
	private JComboBox jComboBoxOutputColor = null;
	private JComboBox jComboBoxComplexWT = null;
	private JComboBox jComboBoxComplexFilterLen = null;
	private JComboBox jComboBoxWTScales = null;
	private JComboBox jComboBoxWT = null;
	private JComboBox jComboBoxSplineOrder = null;

	private JLabel jLabelEdfVar1 = null;
	private JLabel jLabelEdf = null;
	private JLabel jLabelCT1 = null;
	private JLabel jLabelCT2 = null;

	private JPanel jPanelSobel = null;
	private JLabel jLabelTopo0 = null;
	private JLabel jLabelTopo2 = null;
	private JTextField jTextFieldTopoSigma = null;
	private JCheckBox jCheckBoxReassignment = null;
	private JButton jButtonTopoApply = null;

	private JLabel jLabelComplexWT1 = null;
	private JLabel jLabelComplexWT2 = null;
	private JLabel jLabelEdfWv1 = null;
	private JLabel jLabelEdfWv2 = null;
	private JLabel jLabelRealWT1 = null;
	private JLabel jLabelRealWT2 = null;
	private JLabel jLabelDenWv2 = null;

	private JLabel jLabelDenWv1 = null;
	private JTextField jTextFieldWvDenoisingThreshold = null;
	private JLabel jLabelDenGen1 = null;
	private JTextField jTextFieldDenoisingSigma = null;

	private JPanel jPanelEdfWvCommon;
	private JPanel jPanelPostProcessingOptions;

	private JPanel jPanelBotCenter;

	private int[] nScalesAndSize;
	private int nScales;

	private boolean waveletMethod;

	/**
	 * This is the default constructor
	 */
	public AdvancedDialog(int[] imageSize, boolean isColor) {
		super();
		this.isColor = isColor;

		nScalesAndSize = Tools.computeScaleAndPowerTwoSize(imageSize[0],imageSize[1]);
		nScales = nScalesAndSize[0];
		parameters.maxScales = nScales;

		setUpFormats();
		initialize();
	}

	/**
	 * This method initializes this
	 *
	 * @return void
	 */
	private void initialize() {
		this.setContentPane(getJContentPane());
		this.setTitle("Extended Depth of Field");
		getPreferences();
		repaint();
	}

	/**
	 * This method initializes jContentPane
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {

			jContentPane = new JPanel();
			jContentPane.setLayout(gbLayout);
			addComponent(jContentPane,0,0,1,1,0,getJPanelSettings());
			gbConstraints.fill = GridBagConstraints.HORIZONTAL;
			gbLayout.setConstraints(getJPanelSettings(), gbConstraints);

			addComponent(jContentPane,1,0,1,1,5,getJSplitPane());
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbConstraints.weightx = 1.0;
			gbConstraints.weighty = 1.0;
			gbLayout.setConstraints(getJSplitPane(), gbConstraints);

			addComponent(jContentPane,2,0,1,1,0,getJPanelButtons());
			gbConstraints.fill = GridBagConstraints.HORIZONTAL;
			gbLayout.setConstraints(getJPanelButtons(),gbConstraints);

			addComponent(jContentPane,3,0,1,1,0,getJPanelBottom());
			gbConstraints.fill = GridBagConstraints.HORIZONTAL;
			gbLayout.setConstraints(getJPanelBottom(),gbConstraints);

		}
		return jContentPane;
	}

	/**
	 * This method initializes jPanelSettings
	 *
	 * @return javax.swing.JToolBar
	 */
	private JToolBar getJPanelSettings() {
		if (jPanelSettings == null) {
			jPanelSettings = new JToolBar("Settings");
			jPanelSettings.setBorder(BorderFactory.createEtchedBorder());
			//jPanelSettings.setFloatable(false);
			addComponent(jPanelSettings, 0, 0, 1, 1, 3, getJButtonSaveSettings());
			gbConstraints.anchor = GridBagConstraints.WEST;
			gbConstraints.insets = new Insets(10,1,1,1);
			gbLayout.setConstraints(getJButtonSaveSettings(),gbConstraints);

			addComponent(jPanelSettings, 0, 1, 1, 1, 3, getJButtonLoadSettings());
			gbConstraints.insets = new Insets(10,1,1,1);
			gbConstraints.anchor = GridBagConstraints.WEST;
			gbLayout.setConstraints(getJButtonLoadSettings(),gbConstraints);

			addComponent(jPanelSettings, 0, 2, 1, 1, 3, getJButtonResetSettings());
			gbConstraints.insets = new Insets(10,1,1,1);
			gbConstraints.anchor = GridBagConstraints.WEST;
			gbConstraints.weightx = 1.0;
			gbLayout.setConstraints(getJButtonResetSettings(),gbConstraints);
		}
		return jPanelSettings;
	}

	/**
	 * This method initializes jButtonCredits
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonCredits() {
		if (jButtonCredits == null) {
			jButtonCredits = new JButton();
			jButtonCredits.setText("About...");
			jButtonCredits.addActionListener(this);
		}
		return jButtonCredits;
	}

	/**
	 * This method initializes jSplitPane
	 *
	 * @return javax.swing.JSplitPane
	 */
	private JSplitPane getJSplitPane() {
		if (jSplitPane == null) {
			jSplitPane = new JSplitPane();
			jSplitPane.setLeftComponent(getJPanelLeft());
			jSplitPane.setRightComponent(getJPanelRight());
		}
		return jSplitPane;
	}

	/**
	 * This method initializes jPanelButtons
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelButtons() {
		if (jPanelButtons == null) {
			jPanelButtons = new JPanel();
			jPanelButtons.setLayout(gbLayout);

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
	 * This method initializes jButtonRun
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonRun() {
		if (jButtonRun == null) {
			jButtonRun = new JButton();
			jButtonRun.setText("Run");
			jButtonRun.addActionListener(this);
		}
		return jButtonRun;
	}

	/**
	 * This method initializes jButtonClose
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonClose() {
		if (jButtonClose == null) {
			jButtonClose = new JButton();
			jButtonClose.setText("Close");
			jButtonClose.addActionListener(this);
		}
		return jButtonClose;
	}

	/**
	 * This method initializes jPanelLeft
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelLeft() {
		if (jPanelLeft == null) {
			jPanelLeft = new JPanel();
			jPanelLeft.setLayout(new BoxLayout(getJPanelLeft(), BoxLayout.X_AXIS));
			//jPanelLeft.setPreferredSize(new java.awt.Dimension(240,200));
			jPanelLeft.add(getJScrollPane(), null);
		}
		return jPanelLeft;
	}

	/**
	 * This method initializes jPanelRight
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelRight() {
		if (jPanelRight == null) {
			jPanelRight = new JPanel();
			jPanelRight.setLayout(new CardLayout());
			jPanelRight.add(getJPanelEdf(), getJPanelEdf().getName());
			jPanelRight.add(getJPanelColorTreatment(), getJPanelColorTreatment().getName());
			jPanelRight.add(getJPanelHeightMap(), getJPanelHeightMap().getName());
			jPanelRight.add(getJPanelPostProcessing(), getJPanelPostProcessing().getName());
			jPanelRight.add(getJPanelLog(), getJPanelLog().getName());
			((CardLayout)jPanelRight.getLayout()).show(jPanelRight,getJPanelEdf().getName());
		}
		return jPanelRight;
	}

	/**
	 * This method initializes jPanelBottom
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelBottom() {
		if (jPanelBottom == null) {
			jPanelBottom = new JPanel();
			jPanelBottom.setLayout(gbLayout);

			super.addComponent(jPanelBottom,0,1,1,1,0,getJPanelSettings());
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbLayout.setConstraints(getJPanelSettings(),gbConstraints);

			super.addComponent(jPanelBottom,0,2,1,1,0,getJPanelBotCenter());
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbConstraints.weightx = 1.0;
			gbLayout.setConstraints(getJPanelBotCenter(),gbConstraints);

			super.addComponent(jPanelBottom,0,3,1,1,0,getJPanelBotRight());
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbLayout.setConstraints(getJPanelBotRight(),gbConstraints);
		}
		return jPanelBottom;
	}

	/**
	 * This method initializes jPanelBotLeft
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelBotCenter() {
		if (jPanelBotCenter == null) {
			jLabelMemMessage = new JLabel();
			jLabelMemMessage.setText(STR_COPYRIGHT);
			//jLabelMemMessage.setPreferredSize(new java.awt.Dimension(190,14));

			GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
			gridBagConstraints11.gridx = 0;
			gridBagConstraints11.gridy = 0;
			gridBagConstraints11.anchor = java.awt.GridBagConstraints.WEST;
			gridBagConstraints11.fill = java.awt.GridBagConstraints.BOTH;

			jPanelBotCenter = new JPanel();
			jPanelBotCenter.setLayout(new GridBagLayout());
			jPanelBotCenter.setBorder(javax.swing.BorderFactory.createEtchedBorder());
			jPanelBotCenter.setPreferredSize(new java.awt.Dimension(200,28));
			jPanelBotCenter.setMinimumSize(new java.awt.Dimension(200,28));
			jPanelBotCenter.add(jLabelMemMessage, gridBagConstraints11);
		}
		return jPanelBotCenter;
	}

	/**
	 * This method initializes jPanelBoRight
	 *
	 * @return javax.swing.JPanel
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
	 * This method initializes jProgressBar
	 *
	 * @return javax.swing.JProgressBar
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
	 * This method initializes jScrollPane
	 *
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setBackground(java.awt.Color.white);
			jScrollPane.setViewportView(getJTable());
			jScrollPane.getViewport().setBackground(java.awt.Color.white);
			jScrollPane.setPreferredSize(new java.awt.Dimension(260,100));
		}
		return jScrollPane;
	}

	/**
	 * This method initializes jTable
	 *
	 * @return javax.swing.JTable
	 */
	private JTable getJTable() {

		if (jTable == null) {

			if(isColor){
				String[] columnNames = {"Modules","Values"};
				Object[][] data = {
						{"EDF","Sobel"},
						{"Color Treatement","Principal Components"},
						{"Post-Processing","Yes"},
						{"Height-Map","Yes"},
						{"Log","Yes"}
				};
				jTable = new JTable(new SimpleTableModel(columnNames,data));
			}
			else {
				String[] columnNames = {"Modules","Values"};
				Object[][] data = {
						{"EDF","Sobel"},
						{"Post-Processing","Yes"},
						{"Height-Map","Yes"},
						{"Log","Yes"}
				};
				jTable = new JTable(new SimpleTableModel(columnNames, data));
			}

			MyTableRenderer tr = new MyTableRenderer();
			jTable.setDefaultRenderer(String.class,tr);

			ListSelectionModel rowSM = jTable.getSelectionModel();
			jTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
			rowSM.addListSelectionListener(this);
		}
		return jTable;
	}

	/**
	 * This method initializes jPanelEdf
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelEdf() {
		if (jPanelEdf == null) {

			jLabelEdf = new JLabel();
			jLabelEdf.setText("Sharpness Estimator: ");

			jPanelEdf = new JPanel();
			jPanelEdf.setLayout( gbLayout );
			jPanelEdf.setName("jPanelEdf");

			addComponent(jPanelEdf, 0, 0, 1, 1, 10, jLabelEdf );
			addComponent(jPanelEdf, 0, 1, 1, 1, 10, getJComboBoxEdf());
			gbConstraints.weightx = 1.0;
			gbLayout.setConstraints(getJComboBoxEdf(),gbConstraints);

			addComponent(jPanelEdf, 1, 0, 2, 1, 5, getJPanelEdfParams());
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbConstraints.weightx = 1.0;
			gbConstraints.weighty = 1.0;
			gbLayout.setConstraints(getJPanelEdfParams(),gbConstraints);

		}
		return jPanelEdf;
	}

	/**
	 * This method initializes jPanelColorTreatment
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelColorTreatment() {

		if (jPanelColorTreatment == null) {

			jPanelColorTreatment = new JPanel();
			jPanelColorTreatment.setLayout(gbLayout);
			jPanelColorTreatment.setName("jPanelColorTreatment");
			addComponent(jPanelColorTreatment,0,0,1,1,5,getJPanelColorTreatmentOptions());
			gbConstraints.weightx = 1.0;
			gbConstraints.weighty = 1.0;
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbLayout.setConstraints(getJPanelColorTreatmentOptions(),gbConstraints);
		}

		return jPanelColorTreatment;
	}


	private JPanel getJPanelColorTreatmentOptions(){
		if (jPanelColorTreatmentOptions == null) {

			jLabelCT2 = new JLabel();
			jLabelCT2.setText("Output");
			jLabelCT1 = new JLabel();
			jLabelCT1.setText("Color to grayscale conversion:");

			jPanelColorTreatmentOptions = new JPanel();
			jPanelColorTreatmentOptions.setLayout(gbLayout);
			jPanelColorTreatmentOptions.setName("jPanelColorTreatmentOptions");
			jPanelColorTreatmentOptions.setBorder(BorderFactory.createTitledBorder("Color Processing"));

			addComponent(jPanelColorTreatmentOptions, 0, 0, 1, 1, 10, jLabelCT1);
			addComponent(jPanelColorTreatmentOptions, 0, 1, 1, 1, 5, getJComboBoxColorTreatment());
			addComponent(jPanelColorTreatmentOptions, 1, 0, 1, 1, 10, jLabelCT2);
			addComponent(jPanelColorTreatmentOptions, 1, 1, 1, 1, 5, getJComboBoxOutputColor());
			gbConstraints.weightx = 1.0;
			gbConstraints.weighty = 1.0;
			gbLayout.setConstraints(getJComboBoxOutputColor(),gbConstraints);
		}

		return jPanelColorTreatmentOptions;
	}

	/**
	 * This method initializes jPanelHeightMap
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelHeightMap() {
		if (jPanelHeightMap == null) {
			jPanelHeightMap = new JPanel(gbLayout);
			jPanelHeightMap.setName("jPanelHeightMap");
			addComponent(jPanelHeightMap, 0,0,1,1,5,getJCheckBoxTopology());
			addComponent(jPanelHeightMap, 1,0,1,1,5,getJCheckBoxTopology3dView());
			addComponent(jPanelHeightMap, 2,0,1,1,5,getJPanelHeightMapProcessing());
			gbConstraints.weightx = 1.0;
			gbConstraints.weighty = 1.0;
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbLayout.setConstraints(getJPanelHeightMapProcessing(),gbConstraints);

		}
		return jPanelHeightMap;
	}


	/**
	 * This method initializes jPanelPostProcessing
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelPostProcessing(){
		if(jPanelPostProcessing==null){
			jPanelPostProcessing = new JPanel(gbLayout);
			jPanelPostProcessing.setName("jPanelPostProcessing");
			addComponent(jPanelPostProcessing, 0,0,1,1,5,getJPanelPostProcessingOptions());
			gbConstraints.weightx = 1.0;
			gbConstraints.weighty = 1.0;
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbLayout.setConstraints(getJPanelPostProcessingOptions(),gbConstraints);

		}
		return jPanelPostProcessing;
	}


	/**
	 * This method initializes jPanelPostProcessing
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelPostProcessingOptions() {
		if (jPanelPostProcessingOptions == null) {
			jPanelPostProcessingOptions = new JPanel(gbLayout);
			jPanelPostProcessingOptions.setName("jPanelPostProcessing");
			jPanelPostProcessingOptions.setBorder(BorderFactory.createTitledBorder("Post Processing"));
			addComponent(jPanelPostProcessingOptions, 0,0,1,1,5,getJCheckBoxReassignment());
			addComponent(jPanelPostProcessingOptions, 1,0,1,1,5,getJCheckBoxDenoising());
			addComponent(jPanelPostProcessingOptions, 2,0,1,1,5,getJPanelDenoising());
			gbConstraints.weightx = 1.0;
			gbConstraints.weighty = 1.0;
			gbLayout.setConstraints(getJPanelDenoising(),gbConstraints);
		}
		return jPanelPostProcessingOptions;
	}

	/**
	 * This method initializes jPanelLog
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelLog() {
		if (jPanelLog == null) {
			jPanelLog = new JPanel();
			jPanelLog.setLayout(gbLayout);
			jPanelLog.setName("jPanelLog");
			addComponent(jPanelLog, 0, 0, 1, 1, 5, getJCheckBoxLog());
			addComponent(jPanelLog, 1, 0, 1, 1, 5, getJScrollPaneLog());
			gbConstraints.weightx = 1.0;
			gbConstraints.weighty = 1.0;
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbLayout.setConstraints(getJScrollPaneLog(),gbConstraints);
		}
		return jPanelLog;
	}

	/**
	 * This method initializes jCheckBoxLog
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCheckBoxLog() {
		if (jCheckBoxLog == null) {
			jCheckBoxLog = new JCheckBox();
			jCheckBoxLog.setText("Show log messages");
			jCheckBoxLog.setSelected(true);
			jCheckBoxLog.addItemListener(this);
		}
		return jCheckBoxLog;
	}

	private LogPane getJScrollPaneLog() {
		if (logPane == null) {
			logPane = new LogPane();
		}
		return logPane;
	}

	/**
	 * This method initializes jComboBoxEdf
	 *
	 * @return javax.swing.JComboBox
	 */
	private JComboBox getJComboBoxEdf() {
		if (jComboBoxEdf == null) {
			jComboBoxEdf = new JComboBox();
			jComboBoxEdf.addItem(STR_SOBEL);
			jComboBoxEdf.addItem(STR_VARIANCE);
			jComboBoxEdf.addItem(STR_REAL_WV);
			jComboBoxEdf.addItem(STR_COMPLEX_WV);
			jComboBoxEdf.addActionListener(this);
		}
		return jComboBoxEdf;
	}

	/**
	 * This method initializes jPanelEdfParams
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelEdfParams() {
		if (jPanelEdfParams == null) {
			jPanelEdfParams = new JPanel(new CardLayout());
			jPanelEdfParams.setBorder(BorderFactory.createTitledBorder("Parameters"));
			jPanelEdfParams.add(getJPanelSobel(), getJPanelSobel().getName());
			jPanelEdfParams.add(getJPanelVarianceParams(), getJPanelVarianceParams().getName());
			jPanelEdfParams.add(getJPanelEdfWv(), getJPanelEdfWv().getName());
		}
		return jPanelEdfParams;
	}

	/**
	 * This method initializes jPanelVarianceParams
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelVarianceParams() {
		if (jPanelVarianceParams == null) {
			jLabelEdfVar1 = new JLabel("Window Size");
			jPanelVarianceParams = new JPanel(gbLayout);
			jPanelVarianceParams.setName("jPanelVarianceParams");
			addComponent(jPanelVarianceParams, 0, 0, 1, 1, 10, jLabelEdfVar1);
			gbConstraints.weighty =1.0;
			gbLayout.setConstraints(jLabelEdfVar1,gbConstraints);
			addComponent(jPanelVarianceParams,0,1,1,1,10,getJComboBoxVarWindowSize());
			gbConstraints.weightx =1.0;
			gbLayout.setConstraints(getJComboBoxVarWindowSize(),gbConstraints);
		}
		return jPanelVarianceParams;
	}


	private JComboBox getJComboBoxMedianWindowSize() {
		if (jComboBoxMedianWindowSize == null) {
			jComboBoxMedianWindowSize = new JComboBox();
			jComboBoxMedianWindowSize.addItem(new Integer(3));
			jComboBoxMedianWindowSize.addItem(new Integer(5));
			jComboBoxMedianWindowSize.addItem(new Integer(7));
			jComboBoxMedianWindowSize.addItem(new Integer(9));
		}
		return jComboBoxMedianWindowSize;
	}

	/**
	 * This method initializes jComboBoxVarWindowSize
	 *
	 * @return javax.swing.JComboBox
	 */
	private JComboBox getJComboBoxVarWindowSize() {
		if (jComboBoxVarWindowSize == null) {
			jComboBoxVarWindowSize = new JComboBox();
			jComboBoxVarWindowSize.addItem(new Integer(3));
			jComboBoxVarWindowSize.addItem(new Integer(5));
			jComboBoxVarWindowSize.addItem(new Integer(7));
			jComboBoxVarWindowSize.addItem(new Integer(9));
		}
		return jComboBoxVarWindowSize;
	}

	/**
	 * This method initializes jComboBoxColorTreatment
	 *
	 * @return javax.swing.JComboBox
	 */
	private JComboBox getJComboBoxColorTreatment() {
		if (jComboBoxColorTreatment == null) {
			jComboBoxColorTreatment = new JComboBox();
			jComboBoxColorTreatment.addItem(STR_PRINCIPAL_COMPONENTS);
			jComboBoxColorTreatment.addItem(STR_FIXED_WEIGHTS);
//			jComboBoxColorTreatment.addItem(STR_MEAN_WEIGHTS);
			jComboBoxColorTreatment.addActionListener(this);
		}
		return jComboBoxColorTreatment;
	}

	/**
	 * This method initializes jCheckBoxTopology
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCheckBoxTopology() {
		if (jCheckBoxHeightMap == null) {
			jCheckBoxHeightMap = new JCheckBox("Process and show the height-map", true);
			jCheckBoxHeightMap.setSelected(true);
			jCheckBoxHeightMap.addItemListener(this);
		}
		return jCheckBoxHeightMap;
	}

	/**
	 * This method initializes jCheckBoxTopology3dView
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCheckBoxTopology3dView() {
		if (jCheckBoxTopology3dView == null) {
			jCheckBoxTopology3dView = new JCheckBox("Show 3-D View", false);
		}
		return jCheckBoxTopology3dView;
	}

	/**
	 * This method initializes jCheckBoxTopoMedian
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCheckBoxTopoMedian() {
		if (jCheckBoxTopoMedian == null) {
			jCheckBoxTopoMedian = new JCheckBox("Median Filter");
		}
		return jCheckBoxTopoMedian;
	}

	/**
	 * This method initializes jCheckBoxTopoMorphOpen
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCheckBoxTopoMorphOpen() {
		if (jCheckBoxTopoMorphOpen == null) {
			jCheckBoxTopoMorphOpen = new JCheckBox("Morphological open");
		}
		return jCheckBoxTopoMorphOpen;
	}

	/**
	 * This method initializes jCheckBoxTopoMorphClose
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCheckBoxTopoMorphClose() {
		if (jCheckBoxTopoMorphClose == null) {
			jCheckBoxTopoMorphClose = new JCheckBox("Morphological closing");
		}
		return jCheckBoxTopoMorphClose;
	}

	/**
	 * This method initializes jCheckBoxTopoGauss
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCheckBoxTopoGauss() {
		if (jCheckBoxTopoGauss == null) {
			jCheckBoxTopoGauss = new JCheckBox("Gaussian smoothing");
		}
		return jCheckBoxTopoGauss;
	}



	/**
	 * This method initializes jComboBoxOutputColor
	 *
	 * @return javax.swing.JComboBox
	 */
	private JComboBox getJComboBoxOutputColor() {
		if (jComboBoxOutputColor == null) {
			jComboBoxOutputColor = new JComboBox();
			jComboBoxOutputColor.addItem(STR_OUTPUT_COLOR);
			jComboBoxOutputColor.addItem(STR_OUTPUT_GRAYSCALE);
			jComboBoxOutputColor.addActionListener(this);
		}
		return jComboBoxOutputColor;
	}

	/**
	 * This method initializes jPanelSobel
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelSobel() {
		if (jPanelSobel == null) {
			jPanelSobel = new JPanel();
			jPanelSobel.setName("jPanelSobel");
		}
		return jPanelSobel;
	}

	/**
	 * This method initializes jTextFieldTopoSigma
	 *
	 * @return javax.swing.JTextField
	 */
	private JTextField getJTextFieldTopoSigma() {
		if (jTextFieldTopoSigma == null) {
			jTextFieldTopoSigma = new JTextField("1.0");
			jTextFieldTopoSigma.setPreferredSize(new java.awt.Dimension(50,20));
			jTextFieldTopoSigma.setInputVerifier(verifier);
			jTextFieldTopoSigma.addActionListener(verifier);
		}
		return jTextFieldTopoSigma;
	}

	/**
	 * This method initializes jCheckBoxReassignment
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCheckBoxReassignment() {
		if (jCheckBoxReassignment == null) {
			jCheckBoxReassignment = new JCheckBox();
			jCheckBoxReassignment.setText("Reassignment to stack pixels.");
			jCheckBoxReassignment.setSelected(false);
			jCheckBoxReassignment.setEnabled(false);
			jCheckBoxReassignment.addItemListener(this);
		}
		return jCheckBoxReassignment;
	}

	/**
	 * This method initializes jButtonApplyTopologyProc
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonApplyTopologyProc() {
		if (jButtonTopoApply == null) {
			jButtonTopoApply = new JButton();
			jButtonTopoApply.setText("Apply");
			jButtonTopoApply.setEnabled(false);
			jButtonTopoApply.addActionListener(this);
		}
		return jButtonTopoApply;
	}

	/**
	 * This method initializes jPanelEdfWv
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelEdfWv() {
		if (jPanelEdfWv == null) {
			jPanelEdfWv = new JPanel(gbLayout);
			jPanelEdfWv.setName("jPanelEdfWv");
			addComponent(jPanelEdfWv, 0, 0, 1, 1, 0, getJPanelWT());
			addComponent(jPanelEdfWv, 1, 0, 1, 1, 0, getJPanelEdfWvCommon());
			gbConstraints.weighty = 1.0;
			gbConstraints.weightx = 1.0;
			gbConstraints.fill = GridBagConstraints.NONE;
			gbLayout.setConstraints(getJPanelEdfWvCommon(),gbConstraints);
		}
		return jPanelEdfWv;
	}

	/**
	 * This method initializes jPanelWT
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelWT() {
		if (jPanelWT == null) {
			jPanelWT = new JPanel(new CardLayout());
			jPanelWT.add(getJPanelComplexWT(), getJPanelComplexWT().getName());
			jPanelWT.add(getJPanelRealWT(), getJPanelRealWT().getName());
		}
		return jPanelWT;
	}

	/**
	 * This method initializes jPanelComplexWT
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelComplexWT() {
		if (jPanelComplexWT == null) {
			jLabelComplexWT1 = new JLabel("Complex Wavelet Basis");
			jLabelComplexWT2 = new JLabel("Filter Length");
			jPanelComplexWT = new JPanel(gbLayout);
			jPanelComplexWT.setName("jPanelComplexWT");
			addComponent(jPanelComplexWT,0,0,1,1,5,jLabelComplexWT1);
			gbConstraints.anchor = GridBagConstraints.EAST;
			gbLayout.setConstraints(jLabelComplexWT1, gbConstraints);
			addComponent(jPanelComplexWT,0,1,1,1,5,getJComboBoxComplexWT());

			addComponent(jPanelComplexWT,1,0,1,1,5,jLabelComplexWT2);
			gbConstraints.anchor = GridBagConstraints.EAST;
			gbLayout.setConstraints(jLabelComplexWT2, gbConstraints);

			addComponent(jPanelComplexWT,1,1,1,1,5,getJComboBoxComplexFilterLen());

		}
		return jPanelComplexWT;
	}

	/**
	 * This method initializes jPanelRealWT
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelRealWT() {
		if (jPanelRealWT == null) {
			jLabelRealWT1 = new JLabel("Wavelet basis");
			jLabelRealWT2 = new JLabel("Spline order");
			jPanelRealWT = new JPanel(gbLayout);
			jPanelRealWT.setName("jPanelRealWT");
			addComponent(jPanelRealWT,0,0,1,1,5,jLabelRealWT1);
			gbConstraints.anchor = GridBagConstraints.EAST;
			gbLayout.setConstraints(jLabelComplexWT1, gbConstraints);
			addComponent(jPanelRealWT,0,1,1,1,5,getJComboBoxWT());
			addComponent(jPanelRealWT,1,0,1,1,5,jLabelRealWT2);
			gbConstraints.anchor = GridBagConstraints.EAST;
			gbLayout.setConstraints(jLabelRealWT2, gbConstraints);
			addComponent(jPanelRealWT,1,1,1,1,5,getJComboBoxSplineOrder());
		}
		return jPanelRealWT;
	}

	/**
	 * This method initializes jPanelEdfWvCommon
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelEdfWvCommon(){
		if(jPanelEdfWvCommon==null){
			jLabelEdfWv1 = new JLabel("Number of scales");
			jLabelEdfWv2 = new JLabel("Consistency");
			jPanelEdfWvCommon = new JPanel(gbLayout);
			jPanelEdfWvCommon.setName("jPanelEdfWvCommon");
			addComponent(jPanelEdfWvCommon, 0,0,1,1,5, jLabelEdfWv1);
			gbConstraints.anchor = GridBagConstraints.EAST;
			gbLayout.setConstraints(jLabelEdfWv1, gbConstraints);
			addComponent(jPanelEdfWvCommon, 0,1,1,1,5, getJComboBoxWTScales());
			addComponent(jPanelEdfWvCommon, 1,0,1,1,5, jLabelEdfWv2);
			gbConstraints.anchor = GridBagConstraints.EAST;
			gbLayout.setConstraints(jLabelEdfWv2, gbConstraints);
			addComponent(jPanelEdfWvCommon, 1,1,1,1,5, getJCheckBoxSubBandCC());
			addComponent(jPanelEdfWvCommon, 2,1,1,1,5, getJCheckBoxMajCC());
		}
		return jPanelEdfWvCommon;
	}

	/**
	 * This method initializes jComboBoxComplexWT
	 *
	 * @return javax.swing.JComboBox
	 */
	private JComboBox getJComboBoxComplexWT() {
		if (jComboBoxComplexWT == null) {
			jComboBoxComplexWT = new JComboBox();
			jComboBoxComplexWT.addItem("Complex Daubechies");
		}
		return jComboBoxComplexWT;
	}

	/**
	 * This method initializes jComboBoxComplexFilterLen
	 *
	 * @return javax.swing.JComboBox
	 */
	private JComboBox getJComboBoxComplexFilterLen() {
		if (jComboBoxComplexFilterLen == null) {
			jComboBoxComplexFilterLen = new JComboBox();
			jComboBoxComplexFilterLen.addItem(new Integer(6));
			jComboBoxComplexFilterLen.addItem(new Integer(14));
			jComboBoxComplexFilterLen.addItem(new Integer(22));
		}
		return jComboBoxComplexFilterLen;
	}

	/**
	 * This method initializes jComboBoxWTScales
	 *
	 * @return javax.swing.JComboBox
	 */
	private JComboBox getJComboBoxWTScales() {
		if (jComboBoxWTScales == null) {
			jComboBoxWTScales = new JComboBox();
			for(int i = 1; i <= nScales; i++){
				jComboBoxWTScales.addItem(new Integer(i));
			}
		}
		return jComboBoxWTScales;
	}

	/**
	 * This method initializes jComboBoxWT
	 *
	 * @return javax.swing.JComboBox
	 */
	private JComboBox getJComboBoxWT() {
		if (jComboBoxWT == null) {
			jComboBoxWT = new JComboBox();
			jComboBoxWT.addItem("B-spline Wavelets");
		}
		return jComboBoxWT;
	}

	/**
	 * This method initializes jPanelDenoising
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelDenoising() {
		if (jPanelDenoising == null) {
			jPanelDenoising = new JPanel(new CardLayout());
			jPanelDenoising.add(getJPanelDenoisingGeneral(), getJPanelDenoisingGeneral().getName());
			jPanelDenoising.add(getJPanelDenoisingWv(), getJPanelDenoisingWv().getName());
		}
		return jPanelDenoising;
	}

	/**
	 * This method initializes jPanelDenoisingWv
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelDenoisingWv() {
		if (jPanelDenoisingWv == null) {
			FlowLayout flowLayout = new FlowLayout();
			flowLayout.setAlignment(java.awt.FlowLayout.LEFT);
			jLabelDenWv2 = new JLabel();
			jLabelDenWv2.setText("%");
			jLabelDenWv1 = new JLabel();
			jLabelDenWv1.setText("Wavelet Coefficient Threshold =");
			jPanelDenoisingWv = new JPanel();
			jPanelDenoisingWv.setLayout(flowLayout);
			jPanelDenoisingWv.setName("jPanelDenoisingWv");
			jPanelDenoisingWv.add(jLabelDenWv1, null);
			jPanelDenoisingWv.add(getJTextFieldWvDenoisingThreshold(), null);
			jPanelDenoisingWv.add(jLabelDenWv2, null);
		}
		return jPanelDenoisingWv;
	}

	/**
	 * This method initializes jPanelDenoisingGeneral
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelDenoisingGeneral() {
		if ( jPanelDenoisingGeneral == null ) {
			FlowLayout flowLayout = new FlowLayout();
			flowLayout.setAlignment(FlowLayout.LEFT );
			jLabelDenGen1 = new JLabel("Gaussian filter of sigma");
			jPanelDenoisingGeneral = new JPanel(flowLayout);
			jPanelDenoisingGeneral.setName( "jPanelDenoisingGeneral" );
			jPanelDenoisingGeneral.add(jLabelDenGen1, null);
			jPanelDenoisingGeneral.add(getJTextFieldDenoisingSigma(), null);
		}
		return jPanelDenoisingGeneral;
	}

	/**
	 * This method initializes jTextFieldWvDenoisingThreshold
	 *
	 * @return javax.swing.JTextField
	 */
	private JTextField getJTextFieldWvDenoisingThreshold() {
		if (jTextFieldWvDenoisingThreshold == null) {
			jTextFieldWvDenoisingThreshold = new JTextField();
			jTextFieldWvDenoisingThreshold.setPreferredSize(new java.awt.Dimension(50,20));
			jTextFieldWvDenoisingThreshold.setText("10.0");
			jTextFieldWvDenoisingThreshold.setInputVerifier(verifier);
			jTextFieldWvDenoisingThreshold.addActionListener(verifier);
		}
		return jTextFieldWvDenoisingThreshold;
	}

	/**
	 * This method initializes jTextFieldDenoisingSigma
	 *
	 * @return javax.swing.JTextField
	 */
	private JTextField getJTextFieldDenoisingSigma() {
		if (jTextFieldDenoisingSigma == null) {
			jTextFieldDenoisingSigma = new JTextField();
			jTextFieldDenoisingSigma.setPreferredSize(new java.awt.Dimension(50,20));
			jTextFieldDenoisingSigma.setText("1.0");
			jTextFieldDenoisingSigma.setInputVerifier(verifier);
			jTextFieldDenoisingSigma.addActionListener(verifier);
		}
		return jTextFieldDenoisingSigma;
	}

	/**
	 * This method initializes jCheckBoxDenoising
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCheckBoxDenoising() {
		if (jCheckBoxDenoising == null) {
			jCheckBoxDenoising = new JCheckBox("Denoising", false);
			jCheckBoxDenoising.addItemListener(this);
		}
		return jCheckBoxDenoising;
	}

	/**
	 * This method initializes jButtonMode
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonSaveSettings() {
		if (jButtonSaveSettings == null) {
			jButtonSaveSettings = new JButton("Save settings");
			jButtonSaveSettings.addActionListener(this);
		}
		return jButtonSaveSettings;
	}


	/**
	 * This method initializes jButtonLoadSettings
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonLoadSettings() {
		if (jButtonLoadSettings == null) {
			jButtonLoadSettings = new JButton("Load settings");
			jButtonLoadSettings.addActionListener(this);
		}
		return jButtonLoadSettings;
	}

	/**
	 * This method initializes jButtonSettings
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonResetSettings() {
		if (jButtonResetSettings == null) {
			jButtonResetSettings = new JButton("Reset settings");
			jButtonResetSettings.addActionListener(this);
		}
		return jButtonResetSettings;
	}

	/**
	 * This method initializes jCheckBoxSubBandCC
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCheckBoxSubBandCC() {
		if (jCheckBoxSubBandCC == null) {
			jCheckBoxSubBandCC = new JCheckBox("Sub-band consistency check");
		}
		return jCheckBoxSubBandCC;
	}

	/**
	 * This method initializes jCheckBoxMajCC
	 *
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getJCheckBoxMajCC() {
		if (jCheckBoxMajCC == null) {
			jCheckBoxMajCC = new JCheckBox("Majority consistency check");
		}
		return jCheckBoxMajCC;
	}

	/**
	 * This method initializes jComboBoxSplineOrder
	 *
	 * @return javax.swing.JComboBox
	 */
	private JComboBox getJComboBoxSplineOrder() {
		if (jComboBoxSplineOrder == null) {
			jComboBoxSplineOrder = new JComboBox();
			jComboBoxSplineOrder.addItem(new Integer(0));
			jComboBoxSplineOrder.addItem(new Integer(1));
			jComboBoxSplineOrder.addItem(new Integer(3));
			jComboBoxSplineOrder.addItem(new Integer(5));
		}
		return jComboBoxSplineOrder;
	}

	/**
	 * This method initializes getJPanelHeightMapProcessing
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelHeightMapProcessing() {
		if (jPanelHeightMapProcessing == null) {

			jLabelTopo2 = new JLabel("sigma");
			jLabelTopo0 = new JLabel("window size");

			jPanelHeightMapProcessing = new JPanel(gbLayout);
			jPanelHeightMapProcessing.setName("jPanelHeightMapProcessing");
			jPanelHeightMapProcessing.setBorder(BorderFactory.createTitledBorder("Processing"));

			addComponent(jPanelHeightMapProcessing,0,0,1,1,5,getJCheckBoxTopoMedian());
			addComponent(jPanelHeightMapProcessing,0,1,1,1,5,jLabelTopo0);
			gbConstraints.anchor = GridBagConstraints.EAST;
			gbLayout.setConstraints(jLabelTopo0,gbConstraints);
			addComponent(jPanelHeightMapProcessing,0,2,1,1,5,getJComboBoxMedianWindowSize());
			gbConstraints.weightx = 1.0;
			gbLayout.setConstraints(getJComboBoxMedianWindowSize(), gbConstraints);

			addComponent(jPanelHeightMapProcessing,1,0,1,1,5,getJCheckBoxTopoMorphClose());
			addComponent(jPanelHeightMapProcessing,2,0,1,1,5,getJCheckBoxTopoMorphOpen());
			addComponent(jPanelHeightMapProcessing,3,0,1,1,5,getJCheckBoxTopoGauss());
			addComponent(jPanelHeightMapProcessing,3,1,1,1,5,jLabelTopo2);
			gbConstraints.anchor = GridBagConstraints.EAST;
			gbLayout.setConstraints(jLabelTopo2,gbConstraints);
			addComponent(jPanelHeightMapProcessing,3,2,1,1,5,getJTextFieldTopoSigma());
			addComponent(jPanelHeightMapProcessing,4,2,1,1,5,getJButtonApplyTopologyProc());
			gbConstraints.weightx = 1.0;
			gbConstraints.weighty = 1.0;
			gbLayout.setConstraints(getJButtonApplyTopologyProc(), gbConstraints);

		}
		return jPanelHeightMapProcessing;
	}

	/**
	 *
	 */
	public void actionPerformed(ActionEvent e) {

		Object source = e.getSource();

		if ( source == this.jButtonCredits) {
			AboutDialog about = new AboutDialog(this);
			about.setVisible(true);
		}
		else if(source == jButtonClose) {
			setPreferences();
			cleanup();
			dispose();
			System.gc();
		}
		else if (source == jButtonRun) {
			getParameters();
			if(threadEdf!=null) {
				if(threadEdf.isAlive()) {
					return;
				}
			}

			super.threadEdf = new ThreadEdf(super.parameters);
			super.threadEdf.setPriority(Thread.MIN_PRIORITY);
			super.threadEdf.start();
			super.timer.start();
			((CardLayout)jPanelRight.getLayout()).show(getJPanelRight(),getJPanelLog().getName());
			if( this.isColor ){
				jTable.setRowSelectionInterval(4,4);
			}else{
				jTable.setRowSelectionInterval(3,3);
			}
		}
		else if (source == jButtonTopoApply) {
			getParameters();
			if(threadTopoProc !=null) {
				if(threadTopoProc.isAlive()) {
					return;
				}
			}
			if(threadEdf !=null) {
				if(threadEdf.isAlive()) {
					return;
				}
			}
			super.threadTopoProc = new ThreadTopoProc(parameters);
			threadTopoProc.start();
		}
		else if (source == jButtonSaveSettings){
			setPreferences();
		}
		else if (source == jButtonLoadSettings){
			getPreferences();
			this.repaint();
		}
		else if( source == jButtonResetSettings){
			parameters.reset();
			updateGUIFromParameters();
		}
		else if (source == jComboBoxEdf) {
			String edfName = (String)getJComboBoxEdf().getSelectedItem();
			if(edfName.equalsIgnoreCase(STR_VARIANCE)){
				this.jTable.setValueAt(STR_VARIANCE,0,1);
				((CardLayout)jPanelEdfParams.getLayout()).show(jPanelEdfParams,jPanelVarianceParams.getName());
				((CardLayout)jPanelDenoising.getLayout()).show(jPanelDenoising,jPanelDenoisingGeneral.getName());
				this.jCheckBoxReassignment.setEnabled(false);
				this.jCheckBoxReassignment.setSelected(false);
				this.jPanelEdfParams.setVisible(true);
				waveletMethod = false;
			}else if(edfName.equalsIgnoreCase(STR_SOBEL)){
				this.jTable.setValueAt(STR_SOBEL,0,1);
				((CardLayout)jPanelEdfParams.getLayout()).show(jPanelEdfParams,jPanelSobel.getName());
				((CardLayout)jPanelDenoising.getLayout()).show(jPanelDenoising,jPanelDenoisingGeneral.getName());
				this.jCheckBoxReassignment.setEnabled(false);
				this.jCheckBoxReassignment.setSelected(false);
				this.jPanelEdfParams.setVisible(true);
				waveletMethod = false;
			}else if(edfName.equalsIgnoreCase(STR_REAL_WV)){
				this.jTable.setValueAt(STR_REAL_WV,0,1);
				((CardLayout)jPanelEdfParams.getLayout()).show(jPanelEdfParams,jPanelEdfWv.getName());
				((CardLayout)jPanelWT.getLayout()).show(jPanelWT,getJPanelRealWT().getName());
				((CardLayout)jPanelDenoising.getLayout()).show(jPanelDenoising,jPanelDenoisingWv.getName());
				this.jPanelEdfParams.setVisible(true);
				this.jCheckBoxReassignment.setEnabled(true);
				this.jCheckBoxReassignment.setSelected(false);
				this.jCheckBoxHeightMap.setSelected(false);
				waveletMethod = true;
			}else if(edfName.equalsIgnoreCase(STR_COMPLEX_WV)){
				this.jTable.setValueAt(STR_COMPLEX_WV,0,1);
				((CardLayout)jPanelEdfParams.getLayout()).show(jPanelEdfParams,jPanelEdfWv.getName());
				((CardLayout)jPanelWT.getLayout()).show(jPanelWT,getJPanelComplexWT().getName());
				((CardLayout)jPanelDenoising.getLayout()).show(jPanelDenoising,jPanelDenoisingWv.getName());
				this.jPanelEdfParams.setVisible(true);
				this.jCheckBoxReassignment.setEnabled(true);
				this.jCheckBoxReassignment.setSelected(false);
				this.jCheckBoxHeightMap.setSelected(false);
				waveletMethod =  true;
			}else{
				throw new RuntimeException("Unknown error: unknown source.");
			}
		}else if (source == jComboBoxColorTreatment){
			String s = (String)jComboBoxColorTreatment.getSelectedItem();
			if (s.equalsIgnoreCase(this.STR_PRINCIPAL_COMPONENTS)){
				this.jTable.setValueAt(STR_PRINCIPAL_COMPONENTS,1,1);
			}else if (s.equalsIgnoreCase(this.STR_FIXED_WEIGHTS)){
				this.jTable.setValueAt(STR_FIXED_WEIGHTS,1,1);
			}else if (s.equalsIgnoreCase(this.STR_MEAN_WEIGHTS)){
				this.jTable.setValueAt(STR_MEAN_WEIGHTS,1,1);
			}
		}else if (source == this.jComboBoxOutputColor){
			if(waveletMethod){
				if (jComboBoxOutputColor.getSelectedIndex() == 0){
					this.jCheckBoxReassignment.setSelected(true);
				}
			}
		}else{
			throw new RuntimeException("Unknown error: unknown source.");
		}
	}

	/**
	 *
	 */
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		int index = 0;

		if (source == this.jCheckBoxHeightMap) {
			index = 0;
		}
		else if (source == this.jCheckBoxLog) {
			index = 1;
		}
		else if (source == this.jCheckBoxReassignment) {
			index = 2;
		}
		else if (source == this.jCheckBoxDenoising) {
			index = 3;
		}
		else {
			throw new RuntimeException("Error in checkbox event.");
		}

		if (e.getStateChange() == ItemEvent.DESELECTED) {
			switch(index){
			case 0: //topology
				this.jCheckBoxTopoMedian.setEnabled(false);
				this.jCheckBoxTopoMedian.setSelected(false);
				this.jLabelTopo0.setEnabled(false);
				this.jComboBoxMedianWindowSize.setEnabled(false);
				this.jCheckBoxTopoMorphClose.setEnabled(false);
				this.jCheckBoxTopoMorphClose.setSelected(false);
				this.jCheckBoxTopoMorphOpen.setEnabled(false);
				this.jCheckBoxTopoMorphOpen.setSelected(false);
				this.jCheckBoxTopoGauss.setEnabled(false);
				this.jCheckBoxTopoGauss.setSelected(false);
				this.jLabelTopo2.setEnabled(false);
				this.jTextFieldTopoSigma.setEnabled(false);
				this.jButtonTopoApply.setEnabled(false);
				this.jCheckBoxTopology3dView.setEnabled(false);
				this.jCheckBoxTopology3dView.setSelected(false);
				if(isColor) {
					this.jTable.setValueAt("No",3,1);
				}else{
					this.jTable.setValueAt("No",2,1);
				}
				break;

			case 1: //Log
				this.logPane.setEnabled(false);
				if(isColor) {
					this.jTable.setValueAt("No",4,1);
				}else {
					this.jTable.setValueAt("No",3,1);
				}
				break;

			case 2: // reasssignment

				if (waveletMethod){
					this.jCheckBoxHeightMap.setSelected(false);
					this.jComboBoxOutputColor.setSelectedIndex(1);
				}

				break;

			default:

			}
			if (!this.jCheckBoxDenoising.isSelected() &&
					!this.jCheckBoxReassignment.isSelected()){
				if(isColor) {
					this.jTable.setValueAt("No",2,1);
				}else{
					this.jTable.setValueAt("No",1,1);
				}
			}
		}
		else {
			switch(index) {
			case 0: //topology
				this.jCheckBoxTopoMedian.setEnabled(true);
				this.jLabelTopo0.setEnabled(true);
				this.jComboBoxMedianWindowSize.setEnabled(true);
				this.jCheckBoxTopoMorphClose.setEnabled(true);
				this.jCheckBoxTopoMorphOpen.setEnabled(true);
				this.jCheckBoxTopoGauss.setEnabled(true);
				this.jLabelTopo2.setEnabled(true);
				this.jTextFieldTopoSigma.setEnabled(true);
				this.jButtonTopoApply.setEnabled(true);
				this.jCheckBoxTopology3dView.setEnabled(true);

				if(isColor) {
					this.jTable.setValueAt("Yes",3,1);
				}else{
					this.jTable.setValueAt("Yes",2,1);
				}

				if(waveletMethod){
					this.jCheckBoxReassignment.setSelected(true);
				}
				break;

			case 1: //log
				this.logPane.setEnabled(true);

				if(isColor) {
					this.jTable.setValueAt("Yes",4,1);
				}else{
					this.jTable.setValueAt("Yes",3,1);
				}
				break;
			case 2:

				break;
			default:

			}

			if (this.jCheckBoxDenoising.isSelected() ||
					this.jCheckBoxReassignment.isSelected()){
				if(isColor) {
					this.jTable.setValueAt("Yes",2,1);
				}else{
					this.jTable.setValueAt("Yes",1,1);
				}
			}
		}
	}

	/**
	 *
	 */
	public void valueChanged(ListSelectionEvent e) {

		if (e.getValueIsAdjusting()) return;

		ListSelectionModel lsm = (ListSelectionModel)e.getSource();
		if (lsm.isSelectionEmpty()) {
			System.out.println("No rows are selected.");
		}
		else {
			int selectedRow = lsm.getMinSelectionIndex();
			if(isColor){
				switch (selectedRow){
				case 0:
					((CardLayout)jPanelRight.getLayout()).show(getJPanelRight(),getJPanelEdf().getName());
					break;
				case 1:
					((CardLayout)jPanelRight.getLayout()).show(getJPanelRight(),getJPanelColorTreatment().getName());
					break;
				case 2:
					((CardLayout)jPanelRight.getLayout()).show(getJPanelRight(),getJPanelPostProcessing().getName());
					break;
				case 3:
					((CardLayout)jPanelRight.getLayout()).show(getJPanelRight(),getJPanelHeightMap().getName());
					break;
				case 4:
					((CardLayout)jPanelRight.getLayout()).show(getJPanelRight(),getJPanelLog().getName());
					break;
				default:
					throw new RuntimeException("Invalid option.");
				}
			}else {
				switch (selectedRow){
				case 0:
					((CardLayout)jPanelRight.getLayout()).show(getJPanelRight(),getJPanelEdf().getName());
					break;
				case 1:
					((CardLayout)jPanelRight.getLayout()).show(getJPanelRight(),getJPanelPostProcessing().getName());
					break;
				case 2:
					((CardLayout)jPanelRight.getLayout()).show(getJPanelRight(),getJPanelHeightMap().getName());
					break;
				case 3:
					((CardLayout)jPanelRight.getLayout()).show(getJPanelRight(),getJPanelLog().getName());
					break;
				default:
					throw new RuntimeException("Invalid option.");
				}
			}
		}
	}

	/**
	 *
	 */
	private void getParameters(){

		parameters.color = this.isColor;

		String strTemp = (String)this.jComboBoxEdf.getSelectedItem();

		if (strTemp.equalsIgnoreCase(STR_VARIANCE)){
			parameters.edfMethod = ExtendedDepthOfField.VARIANCE;
		}
		else if (strTemp.equalsIgnoreCase(STR_SOBEL)){
			parameters.edfMethod = ExtendedDepthOfField.SOBEL;
		}
		else if (strTemp.equalsIgnoreCase(STR_REAL_WV)){
			parameters.edfMethod = ExtendedDepthOfField.REAL_WAVELETS;
		}
		else if (strTemp.equalsIgnoreCase(STR_COMPLEX_WV)){
			parameters.edfMethod = ExtendedDepthOfField.COMPLEX_WAVELETS;
		}
		else{
			throw new RuntimeException("Error in string.");
		}

		parameters.colorConversionMethod = this.jComboBoxColorTreatment.getSelectedIndex();

		strTemp = (String)this.jComboBoxOutputColor.getSelectedItem();
		if (strTemp.equalsIgnoreCase(STR_OUTPUT_COLOR)){
			parameters.outputColorMap = 0;
		}
		else if (strTemp.equalsIgnoreCase(STR_OUTPUT_GRAYSCALE)){
			parameters.outputColorMap = 1;
		}
		else{
			throw new RuntimeException("Error in string.");
		}

		parameters.subBandCC = this.jCheckBoxSubBandCC.isSelected();
		parameters.majCC = this.jCheckBoxMajCC.isSelected();
		parameters.doDenoising = this.jCheckBoxDenoising.isSelected();
		parameters.doMedian = this.jCheckBoxTopoMedian.isSelected();
		parameters.doMorphoOpen = this.jCheckBoxTopoMorphOpen.isSelected();
		parameters.doMorphoClose = this.jCheckBoxTopoMorphClose.isSelected();
		parameters.reassignment = this.jCheckBoxReassignment.isSelected();
		parameters.doGaussian = this.jCheckBoxTopoGauss.isSelected();
		parameters.showTopology = this.jCheckBoxHeightMap.isSelected();
		parameters.show3dView = this.jCheckBoxTopology3dView.isSelected();
		parameters.log = this.jCheckBoxLog.isSelected();

		parameters.medianWindowSize = ((Integer)this.jComboBoxMedianWindowSize.getSelectedItem()).intValue();
		parameters.nScales = ((Integer)this.jComboBoxWTScales.getSelectedItem()).intValue();
		parameters.daubechielength = ((Integer)this.jComboBoxComplexFilterLen.getSelectedItem()).intValue();
		parameters.splineOrder = ((Integer)this.jComboBoxSplineOrder.getSelectedItem()).intValue();
		parameters.varWindowSize = ((Integer)this.jComboBoxVarWindowSize.getSelectedItem()).intValue();

		parameters.sigmaDenoising = Double.parseDouble(this.jTextFieldDenoisingSigma.getText());
		parameters.sigma = Double.parseDouble(this.jTextFieldTopoSigma.getText());
		parameters.rateDenoising = Double.parseDouble(this.jTextFieldWvDenoisingThreshold.getText());

	}


	private void updateGUIFromParameters(){

		jComboBoxEdf.setSelectedIndex(parameters.edfMethod);
		jComboBoxColorTreatment.setSelectedIndex(parameters.colorConversionMethod);
		jComboBoxOutputColor.setSelectedIndex(parameters.outputColorMap);

		jCheckBoxReassignment.setSelected(parameters.reassignment);

		jCheckBoxTopoMedian.setSelected(parameters.doMedian);
		jCheckBoxTopoMorphOpen.setSelected(parameters.doMorphoOpen);
		jCheckBoxTopoMorphClose.setSelected(parameters.doMorphoClose);
		jCheckBoxLog.setSelected(parameters.log);
		jCheckBoxDenoising.setSelected(parameters.doDenoising);
		jCheckBoxTopoGauss.setSelected(parameters.doGaussian);
		jCheckBoxSubBandCC.setSelected(parameters.subBandCC);
		jCheckBoxMajCC.setSelected(parameters.majCC);
		jCheckBoxHeightMap.setSelected(parameters.showTopology);
		jCheckBoxTopology3dView.setSelected(parameters.show3dView);

		jComboBoxWTScales.setSelectedItem( new Integer(parameters.nScales));
		jComboBoxVarWindowSize.setSelectedItem( new Integer(parameters.varWindowSize));
		jComboBoxMedianWindowSize.setSelectedItem( new Integer(parameters.medianWindowSize));
		jComboBoxSplineOrder.setSelectedItem( new Integer(parameters.splineOrder));
		jComboBoxComplexFilterLen.setSelectedItem(new Integer(parameters.daubechielength));

		jTextFieldDenoisingSigma.setText(""+parameters.sigmaDenoising);
		jTextFieldTopoSigma.setText(""+ parameters.sigma);
		jTextFieldWvDenoisingThreshold.setText(""+ parameters.rateDenoising);
	}

	/**
	 *
	 *
	 */
	private void setPreferences(){

		try {

			FileOutputStream fos = new FileOutputStream(filename);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			Properties properties = new Properties();

			getParameters();

			properties.setProperty("edfmethod", "" + parameters.edfMethod);
			properties.setProperty("colorconversion", ""+ parameters.colorConversionMethod);
			properties.setProperty("outputcolormap", "" + parameters.outputColorMap);

			properties.setProperty("median",""+parameters.doMedian);
			properties.setProperty("morphopen",""+parameters.doMorphoOpen);
			properties.setProperty("morphclose",""+parameters.doMorphoClose);
			properties.setProperty("log", ""+parameters.log);
			properties.setProperty("denoising", ""+parameters.doDenoising);
			properties.setProperty("topogauss", ""+parameters.doGaussian);
			properties.setProperty("subbandcc", ""+parameters.subBandCC);
			properties.setProperty("majcc", ""+parameters.majCC);
			properties.setProperty("showtopo", ""+parameters.showTopology);
			properties.setProperty("show3d", ""+parameters.show3dView);
			properties.setProperty("reassignment", ""+parameters.reassignment);

			properties.setProperty("nscales", ""+parameters.nScales);
			properties.setProperty("varwindow", ""+parameters.varWindowSize);
			properties.setProperty("medianwindow", ""+parameters.medianWindowSize);
			properties.setProperty("splineorder", ""+parameters.splineOrder);
			properties.setProperty("filterlen", ""+parameters.daubechielength);

			properties.setProperty("densigma", "" + parameters.sigmaDenoising);
			properties.setProperty("toposigma", "" + parameters.sigma);
			properties.setProperty("wvthreshold", "" + parameters.rateDenoising);

			properties.store(bos,"Extended Depth of Field");

			bos.flush();
			bos.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 *
	 */
	private void getPreferences(){
		try {

			String s;

			FileInputStream fis = new FileInputStream(filename);

			BufferedInputStream bis = new BufferedInputStream(fis);

			Properties properties = new Properties();

			parameters.reset();
			properties.load(bis);

			s = properties.getProperty("edfmethod", "" + parameters.edfMethod);
			parameters.edfMethod = Integer.parseInt(s);
			s = properties.getProperty("colorconversion", ""+ parameters.colorConversionMethod);
			parameters.colorConversionMethod = Integer.parseInt(s);
			s = properties.getProperty("outputcolormap", "" + parameters.outputColorMap);
			parameters.outputColorMap = Integer.parseInt(s);

			s = properties.getProperty("median",""+parameters.doMedian);
			parameters.doMedian = Boolean.valueOf(s).booleanValue();
			s = properties.getProperty("morphopen",""+parameters.doMorphoOpen);
			parameters.doMorphoOpen = Boolean.valueOf(s).booleanValue();
			s = properties.getProperty("morphclose",""+parameters.doMorphoClose);
			parameters.doMorphoClose = Boolean.valueOf(s).booleanValue();
			s = properties.getProperty("log", ""+parameters.log);
			parameters.log = Boolean.valueOf(s).booleanValue();
			s = properties.getProperty("denoising", ""+parameters.doDenoising);
			parameters.doDenoising = Boolean.valueOf(s).booleanValue();
			s = properties.getProperty("topogauss", ""+parameters.doGaussian);
			parameters.doGaussian = Boolean.valueOf(s).booleanValue();
			s = properties.getProperty("subbandcc", ""+parameters.subBandCC);
			parameters.subBandCC = Boolean.valueOf(s).booleanValue();
			s = properties.getProperty("majcc", ""+parameters.majCC);
			parameters.majCC = Boolean.valueOf(s).booleanValue();
			s = properties.getProperty("showtopo", ""+parameters.showTopology);
			parameters.showTopology = Boolean.valueOf(s).booleanValue();
			s = properties.getProperty("show3d", ""+parameters.show3dView);
			parameters.show3dView = Boolean.valueOf(s).booleanValue();
			s = properties.getProperty("reassignment", ""+parameters.reassignment);
			parameters.reassignment = Boolean.valueOf(s).booleanValue();

			s = properties.getProperty("nscales", ""+parameters.nScales);
			parameters.nScales = Integer.parseInt(s);
			s = properties.getProperty("varwindow", ""+parameters.varWindowSize);
			parameters.varWindowSize = Integer.parseInt(s);
			s = properties.getProperty("medianwindow", ""+parameters.medianWindowSize);
			parameters.medianWindowSize = Integer.parseInt(s);
			s = properties.getProperty("splineorder", ""+parameters.splineOrder);
			parameters.splineOrder = Integer.parseInt(s);
			s = properties.getProperty("filterlen", ""+parameters.daubechielength);
			parameters.daubechielength = Integer.parseInt(s);

			s = properties.getProperty("densigma", "" + parameters.sigmaDenoising);
			parameters.sigmaDenoising = Double.parseDouble(s);
			s = properties.getProperty("toposigma", "" + parameters.sigma);
			parameters.sigma = Double.parseDouble(s);
			s = properties.getProperty("wvthreshold", "" + parameters.rateDenoising);
			parameters.rateDenoising = Double.parseDouble(s);

			bis.close();

			updateGUIFromParameters();

		} catch (Exception e) {
			parameters.reset();
			updateGUIFromParameters();
		}
	}

	/**
	 */
	private void setUpFormats() {

		percentFormat = NumberFormat.getNumberInstance();
		percentFormat.setMinimumFractionDigits(1);

		decimalFormat = (DecimalFormat)NumberFormat.getNumberInstance();
		decimalFormat.setParseIntegerOnly(false);
		decimalFormat.setMaximumFractionDigits(2);

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

	/**
	 * This class checks the user input.
	 */
	class MyVerifier extends InputVerifier implements ActionListener {

		double MIN_SIGMA = 0.0;
		double MAX_SIGMA = 100.0;
		double MIN_RATE = 0.0;
		double MAX_RATE = 100.0;

		public boolean shouldYieldFocus(JComponent input) {
			boolean inputOK = verify(input);
			makeItPretty(input);

			if (inputOK) {
				return true;
			} else {
				Toolkit.getDefaultToolkit().beep();
				return false;
			}
		}

		/**
		 *This method checks input, but should cause no side effects.
		 */
		public boolean verify(JComponent input) {
			return checkField(input, false);
		}

		/**
		 */
		protected void makeItPretty(JComponent input) {
			checkField(input, true);
		}

		/**
		 */
		protected boolean checkField(JComponent input, boolean changeIt) {
			if (input == jTextFieldWvDenoisingThreshold) {
				return checkRateField(changeIt);
			} else if (input == jTextFieldTopoSigma) {
				return checkTopoSigmaField(changeIt);
			} else if (input == jTextFieldDenoisingSigma) {
				return checkSigmaField(changeIt);
			} else {
				return true; //shouldn't happen
			}
		}

		/**
		 */
		protected boolean checkRateField(boolean change) {
			boolean wasValid = true;
			double rate = 10.0;

//			Parse the value.
			try {
				rate = percentFormat.parse(jTextFieldWvDenoisingThreshold.
						getText()).doubleValue();
			} catch (ParseException pe) {
				wasValid = false;
			}

//			Value was invalid.
			if (rate < MIN_RATE || rate > MAX_RATE) {
				wasValid = false;
				if (change) {
					if(rate < MIN_RATE)
						rate = MIN_RATE;
					else
						rate = MAX_RATE;
				}
			}

//			Whether value was valid or not, format it nicely.
			if (change) {
				jTextFieldWvDenoisingThreshold.setText(percentFormat.format(rate));
				jTextFieldWvDenoisingThreshold.selectAll();
			}

			return wasValid;
		}

		/**
		 */
		protected boolean checkTopoSigmaField(boolean change) {
			boolean wasValid = true;
			double sigma = parameters.sigma;

//			Parse the value.
			try {
				sigma = decimalFormat.parse(jTextFieldTopoSigma.getText()).
				doubleValue();
			} catch (ParseException pe) {
				wasValid = false;
			}

//			Value was invalid.
			if ((sigma < MIN_SIGMA)) {
				wasValid = false;
				if (change) {
					if (sigma < MIN_SIGMA) {
						sigma = MIN_SIGMA;
					} else { // numPeriods is greater than MAX_PERIOD
						sigma = MAX_SIGMA;
					}
				}
			}

//			Whether value was valid or not, format it nicely.
			if (change) {
				jTextFieldTopoSigma.setText(decimalFormat.format(sigma));
				jTextFieldTopoSigma.selectAll();
			}

			return wasValid;
		}

		/**
		 */
		protected boolean checkSigmaField(boolean change) {
			boolean wasValid = true;
			double sigma = parameters.sigma;

//			Parse the value.
			try {
				sigma = decimalFormat.parse(jTextFieldDenoisingSigma.getText()).
				doubleValue();
			} catch (ParseException pe) {
				wasValid = false;
			}

//			Value was invalid.
			if ((sigma < MIN_SIGMA)) {
				wasValid = false;
				if (change) {
					if (sigma < MIN_SIGMA) {
						sigma = MIN_SIGMA;
					} else {
						sigma = MAX_SIGMA;
					}
				}
			}

//			Whether value was valid or not, format it nicely.
			if (change) {
				jTextFieldDenoisingSigma.setText(decimalFormat.format(sigma));
				jTextFieldDenoisingSigma.selectAll();
			}

			return wasValid;
		}

		/**
		 *
		 */
		public void actionPerformed(ActionEvent e) {
			JTextField source = (JTextField)e.getSource();
			shouldYieldFocus(source); //ignore return value
			source.selectAll();
		}
	}


}
