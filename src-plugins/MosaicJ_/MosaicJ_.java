/*====================================================================
| Version: March 13, 2008
\===================================================================*/

/*====================================================================
| EPFL/STI/IOA/LIB
| Philippe Thevenaz
| Bldg. BM-Ecublens 4.137
| CH-1015 Lausanne VD
| Switzerland
|
| phone (CET): +41(21)693.51.61
| fax: +41(21)693.37.01
| RFC-822: philippe.thevenaz@epfl.ch
| X-400: /C=ch/A=400net/P=switch/O=epfl/S=thevenaz/G=philippe/
| URL: http://bigwww.epfl.ch/
\===================================================================*/

/*====================================================================
| This work is based on the following paper:
|
| P. Thevenaz, M. Unser
| User-Friendly Semiautomated Assembly of Accurate Image Mosaics in Microscopy
| Microscopy Research and Technique
| vol. 70, no. 2, pp. 135-146, February 2007
|
| This paper is available on-line at
| http://bigwww.epfl.ch/preprints/thevenaz0604p.html
|
| Other relevant on-line publications are available at
| http://bigwww.epfl.ch/publications/
\===================================================================*/

/*====================================================================
| Additional help available at http://bigwww.epfl.ch/thevenaz/mosaicj/
|
| You'll be free to use this software for research purposes, but you
| should not redistribute it without our consent. In addition, we expect
| you to include a citation or acknowledgment whenever you present or
| publish results that are based on it.
\===================================================================*/

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Menus;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.gui.Toolbar;
import ij.io.FileSaver;
import ij.io.PluginClassLoader;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.TypeConverter;
import java.awt.BorderLayout;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Stack;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/*====================================================================
|	MosaicJ_
\===================================================================*/
public class MosaicJ_
	extends
		JFrame
	implements
		ActionListener,
		ChangeListener,
		PlugIn

{ /* begin class MosaicJ_ */

/*....................................................................
	private variables
....................................................................*/

private MosaicJPlayField playField = null;
private final JPanel thumbnailArea = new JPanel(true);
private final JPanel workArea = new JPanel(true);
private final JProgressBar progressBar = new JProgressBar(0, 0);
private final JScrollPane thumbnailScrollPane = new JScrollPane(
	JScrollPane.VERTICAL_SCROLLBAR_NEVER,
	JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
private final JScrollPane workScrollPane = new JScrollPane(workArea,
	JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
	JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
private final MenuItem abort = new MenuItem("Abort",
	new MenuShortcut(KeyEvent.VK_W));
private final MenuItem magnification = new MenuItem("");
private final MenuItem magnify = new MenuItem("Magnify",
	new MenuShortcut(KeyEvent.VK_ADD));
private final MenuItem minify = new MenuItem("Minify",
	new MenuShortcut(KeyEvent.VK_SUBTRACT));
private final MenuItem openFile = new MenuItem("Open Image...",
	new MenuShortcut(KeyEvent.VK_O));
private final MenuItem openImageSequence = new MenuItem(
	"Open Image Sequence...", new MenuShortcut(KeyEvent.VK_O, true));
private int id = 0;
private static MenuItem blending = null;
private static MenuItem createMosaic = null;
private static MenuItem credits = null;
private static MenuItem decolorization = null;
private static MenuItem deselect = null;
private static MenuItem forget = null;
private static MenuItem freeze = null;
private static MenuItem group = null;
private static MenuItem help = null;
private static MenuItem hide = null;
private static MenuItem loadPreMosaic = null;
private static MenuItem maximizeContrast = null;
private static MenuItem nudgeDown = null;
private static MenuItem nudgeLeft = null;
private static MenuItem nudgeRight = null;
private static MenuItem nudgeUp = null;
private static MenuItem outputLog = null;
private static MenuItem quickAndDirty = null;
private static MenuItem replay = null;
private static MenuItem resetContrast = null;
private static MenuItem resetMosaicJ = null;
private static MenuItem revertPreMosaic = null;
private static MenuItem rotation = null;
private static MenuItem savePreMosaic = null;
private static MenuItem selectAll = null;
private static MenuItem sendToBack = null;
private static MenuItem setContrast = null;
private static MenuItem showAll = null;
private static MenuItem status = null;
private static MenuItem stow = null;
private static MenuItem unfreeze = null;
private static MenuItem ungroup = null;
private static boolean blend = true;
private static boolean createOutputLog = false;
private static boolean hidden = false;
private static boolean quickAndDirtyScaling = false;
private static boolean rotate = true;
private static boolean smartDecolorization = true;
private static final Stack preMosaic = new Stack();
private static final int LOAD_ABORT = -1;
private static final int LOAD_ALL = 3;
private static final int LOAD_NO_MORE = 0;
private static final int LOAD_ONE = 1;
private static final int LOAD_SOME = 2;
private static final int SCREEN_HEIGHT = Toolkit.getDefaultToolkit(
	).getScreenSize().height;
private static final int SCREEN_WIDTH = Toolkit.getDefaultToolkit(
	).getScreenSize().width;
private static int reductionFactor = 4;

/*....................................................................
	ActionListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void actionPerformed (
	final ActionEvent e
) {
	if (e.getSource() == abort) {
		System.gc();
		dispose();
	}
	if (e.getSource() == blending) {
		setBlend(!blend);
	}
	if (e.getSource() == createMosaic) {
		playField.recordThumbs(preMosaic);
		if (MosaicJTree.createMosaic(progressBar, this,
			playField.getStackingOrder(), blend, rotate, createOutputLog)) {
			releaseResources();
		}
		savePreMosaic(IJ.getDirectory("temp") + "MosaicJPreMosaic");
	}
	if (e.getSource() == credits) {
		final MosaicJCredits dialog = new MosaicJCredits(this);
	}
	if (e.getSource() == decolorization) {
		setSmartDecolorization(!smartDecolorization);
	}
	if (e.getSource() == deselect) {
		playField.deselectThumbs();
	}
	if (e.getSource() == forget) {
		playField.forgetSelectedThumbs();
		System.gc();
	}
	if (e.getSource() == freeze) {
		playField.freezeSelectedThumbs();
	}
	if (e.getSource() == group) {
		playField.groupSelectedThumbs();
	}
	if (e.getSource() == help) {
//@
	}
	if (e.getSource() == hide) {
		playField.hideSelectedThumbs();
		hidden = true;
	}
	if (e.getSource() == loadPreMosaic) {
		final Frame f = new Frame();
		final FileDialog fd = new FileDialog(f, "Load Pre-Mosaic",
			FileDialog.LOAD);
		fd.setVisible(true);
		final String path = fd.getDirectory();
		final String filename = fd.getFile();
		if ((path != null) && (filename != null)) {
			loadPreMosaic(path + filename, true);
		}
	}
	if (e.getSource() == magnify) {
		updateScale(reductionFactor / 2);
		magnify.setEnabled(1 < reductionFactor);
		magnification.setLabel(getMagnificationAsString());
	}
	if (e.getSource() == maximizeContrast) {
		playField.maximizeContrastSelectedThumbs();
	}
	if (e.getSource() == minify) {
		updateScale(2 * reductionFactor);
		magnify.setEnabled(true);
		magnification.setLabel(getMagnificationAsString());
	}
	if (e.getSource() == nudgeDown) {
		playField.translateSelectedThumbs(0, 1);
	}
	if (e.getSource() == nudgeLeft) {
		playField.translateSelectedThumbs(-1, 0);
	}
	if (e.getSource() == nudgeRight) {
		playField.translateSelectedThumbs(1, 0);
	}
	if (e.getSource() == nudgeUp) {
		playField.translateSelectedThumbs(0, -1);
	}
	if (e.getSource() == openFile) {
		openOneFile();
	}
	if (e.getSource() == openImageSequence) {
		openManyFiles();
	}
	if (e.getSource() == outputLog) {
		setCreateOutputLog(!createOutputLog);
	}
	if (e.getSource() == quickAndDirty) {
		setQuickAndDirtyScaling(!quickAndDirtyScaling);
		updateScale(reductionFactor);
	}
	if (e.getSource() == replay) {
		final Frame f = new Frame();
		final FileDialog fd = new FileDialog(f, "Replay from Log",
			FileDialog.LOAD);
		fd.setVisible(true);
		final String path = fd.getDirectory();
		final String filename = fd.getFile();
		if ((path != null) && (filename != null)) {
			replay(path + filename);
			releaseResources();
		}
	}
	if (e.getSource() == resetMosaicJ) {
		final String path = IJ.getDirectory("temp");
		if (0 != JOptionPane.showConfirmDialog(null,
			"Are you ready to reset MosaicJ and its cache?\n"
			+ "Deleting: " + path + "MosaicJ*",
			"Reset MosaicJ Cache",
			JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
			return;
		}
		playField.showAll();
		playField.selectAll();
		playField.forgetSelectedThumbs();
		final Component[] thumbs = thumbnailArea.getComponents();
		for (int k = 0, K = thumbs.length; (k < K); k++) {
			thumbnailArea.remove(thumbs[k]);
		}
		preMosaic.removeAllElements();
		getContentPane().validate();
		final File directory = new File(path);
		if ((directory == null) || (!directory.isDirectory())) {
			return;
		}
		final String[] file = directory.list();
		for (int k = 0, K = file.length; (k < K); k++) {
			if (file[k].startsWith("MosaicJ")) {
				if (!(new File(path + file[k])).delete()) {
					JOptionPane.showMessageDialog(null,
						"Could not delete: " + path + file[k], "File Error",
						JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}
	if (e.getSource() == resetContrast) {
		playField.resetContrastSelectedThumbs();
	}
	if (e.getSource() == revertPreMosaic) {
		loadPreMosaic(IJ.getDirectory("temp") + "MosaicJPreMosaic", true);
	}
	if (e.getSource() == rotation) {
		setRotate(!rotate);
	}
	if (e.getSource() == savePreMosaic) {
		final Frame f = new Frame();
		final FileDialog fd = new FileDialog(f, "Save Pre-Mosaic",
			FileDialog.SAVE);
		fd.setFile("PreMosaic.txt");
		fd.setVisible(true);
		final String path = fd.getDirectory();
		final String filename = fd.getFile();
		if ((path != null) && (filename != null)) {
			playField.recordThumbs(preMosaic);
			savePreMosaic(path + filename);
		}
	}
	if (e.getSource() == selectAll) {
		playField.selectAll();
	}
	if (e.getSource() == sendToBack) {
		playField.sendToBackSelectedThumbs();
	}
	if (e.getSource() == setContrast) {
		double min = MosaicJThumb.getMin();
		double max = MosaicJThumb.getMax();
		if (max <= min) {
			min = 0.0;
			max = 255.0;
		}
		GenericDialog gd = new GenericDialog("Range of Grays", this);
		gd.addNumericField("Min: ", min, 4, 12, "");
		gd.addNumericField("Max: ", max, 4, 12, "");
		gd.showDialog();
		if (!gd.wasCanceled()) {
			min = gd.getNextNumber();
			String error = gd.getErrorMessage();
			if (error != null) {
				IJ.error("Invalid Min", error);
				return;
			}
			max = gd.getNextNumber();
			error = gd.getErrorMessage();
			if (error != null) {
				IJ.error("Invalid Max", error);
				return;
			}
			if (max <= min) {
				IJ.error("Invalid Range", "Min should be less than Max");
				return;
			}
			playField.setContrastSelectedThumbs(min, max);
		}
	}
	if (e.getSource() == showAll) {
		playField.showAll();
		hidden = false;
	}
	if (e.getSource() == status) {
		final MosaicJStatus dialog = new MosaicJStatus(this,
			smartDecolorization,
			quickAndDirtyScaling, reductionFactor,
			hidden, !(playField.getStackingOrder().isEmpty() || hidden),
			blend, rotate, createOutputLog);
	}
	if (e.getSource() == stow) {
		playField.stowSelectedThumbs();
	}
	if (e.getSource() == unfreeze) {
		playField.unfreezeSelectedThumbs();
	}
	if (e.getSource() == ungroup) {
		playField.ungroupSelectedThumbs();
	}
} /* end actionPerformed */

/*....................................................................
	ChangeListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void stateChanged (
	final ChangeEvent e
) {
	progressBar.paintImmediately(progressBar.getBounds());
} /* end stateChanged */

/*....................................................................
	PlugIn methods
....................................................................*/

/*------------------------------------------------------------------*/
public void run (
	final String arg
) {
	setTitle("MosaicJ " + getMagnificationAsString());
	hidden = false;
	blending = new MenuItem((blend)
		? ("Deactivate Blending")
		: ("Activate Blending"));
	createMosaic = new MenuItem(
		"Create Mosaic", new MenuShortcut(KeyEvent.VK_S));
	credits = new MenuItem(
		"Credits");
	decolorization = new MenuItem((smartDecolorization)
		? ("Deactivate Smart Color Conversion")
		: ("ActivateSmart Color Conversion"));
	deselect = new MenuItem(
		"Deselect", new MenuShortcut(KeyEvent.VK_D));
	forget = new MenuItem(
		"Forget", new MenuShortcut(KeyEvent.VK_X));
	freeze = new MenuItem(
		"Freeze", new MenuShortcut(KeyEvent.VK_F));
	group = new MenuItem(
		"Group", new MenuShortcut(KeyEvent.VK_G));
	help = new MenuItem(
		"Help");
	hide = new MenuItem(
		"Hide", new MenuShortcut(KeyEvent.VK_H, true));
	loadPreMosaic = new MenuItem(
		"Load Pre-Mosaic...", new MenuShortcut(KeyEvent.VK_R, true));
	maximizeContrast = new MenuItem(
		"Maximize Contrast", new MenuShortcut(KeyEvent.VK_L, true));
	nudgeDown = new MenuItem(
		"Nudge Down", new MenuShortcut(KeyEvent.VK_DOWN));
	nudgeLeft = new MenuItem(
		"Nudge Left", new MenuShortcut(KeyEvent.VK_LEFT));
	nudgeRight = new MenuItem(
		"Nudge Right", new MenuShortcut(KeyEvent.VK_RIGHT));
	nudgeUp = new MenuItem(
		"Nudge Up", new MenuShortcut(KeyEvent.VK_UP));
	outputLog = new MenuItem((createOutputLog)
		? ("Deactivate Log File")
		: ("Activate Log File"));
	quickAndDirty = new MenuItem((quickAndDirtyScaling)
		? ("Deactivate Quick&Dirty Scaling")
		: ("Activate Quick&Dirty Scaling"));
	replay = new MenuItem(
		"Replay from Log...");
	resetMosaicJ = new MenuItem(
		"Reset MosaicJ");
	resetContrast = new MenuItem(
		"Reset Contrast", new MenuShortcut(KeyEvent.VK_R));
	revertPreMosaic = new MenuItem(
		"Previous Pre-Mosaic...", new MenuShortcut(KeyEvent.VK_Z));
	rotation = new MenuItem((rotate)
		? ("Deactivate Rotation")
		: ("Activate Rotation"));
	savePreMosaic = new MenuItem(
		"Save Pre-Mosaic...", new MenuShortcut(KeyEvent.VK_S, true));
	selectAll = new MenuItem(
		"Select All", new MenuShortcut(KeyEvent.VK_A));
	sendToBack = new MenuItem(
		"Send to Back", new MenuShortcut(KeyEvent.VK_B));
	setContrast = new MenuItem(
		"Set Contrast...", new MenuShortcut(KeyEvent.VK_L));
	showAll = new MenuItem(
		"Show All", new MenuShortcut(KeyEvent.VK_A, true));
	status = new MenuItem(
		"Status");
	stow = new MenuItem(
		"Stow", new MenuShortcut(KeyEvent.VK_T));
	unfreeze = new MenuItem(
		"Unfreeze", new MenuShortcut(KeyEvent.VK_F, true));
	ungroup = new MenuItem(
		"Ungroup", new MenuShortcut(KeyEvent.VK_U));
	if ((SCREEN_WIDTH < 640) || (SCREEN_HEIGHT < 480)) {
		IJ.error("Screen too small to proceed.");
		return;
	}
	final PluginClassLoader loader = new PluginClassLoader(
		Menus.getPlugInsPath());
	try {
		final Class ancillaryPlugin = loader.loadClass("TurboReg_");
	} catch (ClassNotFoundException e) {
		IJ.error("Please download TurboReg_ from\n"
			+ "http://bigwww.epfl.ch/thevenaz/turboreg/");
		return;
	}
	setSize(SCREEN_WIDTH, SCREEN_HEIGHT - 22);
	setResizable(false);
	progressBar.addChangeListener(this);
	workScrollPane.setBorder(null);
	thumbnailArea.setSize(
		new Dimension(SCREEN_WIDTH, MosaicJThumb.THUMBNAIL_HEIGHT));
	thumbnailScrollPane.setPreferredSize(
		new Dimension(SCREEN_WIDTH, MosaicJThumb.THUMBNAIL_HEIGHT));
	thumbnailScrollPane.setViewportView(thumbnailArea);
	thumbnailScrollPane.getHorizontalScrollBar().setBlockIncrement(128);
	thumbnailScrollPane.getHorizontalScrollBar().setUnitIncrement(32);
	getContentPane().setLayout(new BorderLayout(0, 0));
	getContentPane().add(workScrollPane, BorderLayout.CENTER);
	getContentPane().add(thumbnailScrollPane, BorderLayout.SOUTH);
	setVisible(true);

	playField = new MosaicJPlayField(workArea.getWidth(), workArea.getHeight(),
		workScrollPane, reductionFactor);
	workArea.setLayout(new BorderLayout(0, 0));
	workArea.add(playField, BorderLayout.CENTER);

	getContentPane().add(progressBar, BorderLayout.NORTH);
	getContentPane().validate();
	getContentPane().remove(progressBar);
	getContentPane().validate();

	final Menu fileMenu = new Menu("File");
	openFile.addActionListener(this);
	fileMenu.add(openFile);
	openImageSequence.addActionListener(this);
	fileMenu.add(openImageSequence);
	decolorization.addActionListener(this);
	fileMenu.add(decolorization);
	fileMenu.addSeparator();
	createMosaic.addActionListener(this);
	fileMenu.add(createMosaic);
	blending.addActionListener(this);

	fileMenu.add(blending);
	rotation.addActionListener(this);
	fileMenu.add(rotation);
	outputLog.addActionListener(this);
	fileMenu.add(outputLog);
	fileMenu.addSeparator();
	savePreMosaic.addActionListener(this);
	fileMenu.add(savePreMosaic);
	loadPreMosaic.addActionListener(this);
	fileMenu.add(loadPreMosaic);
	revertPreMosaic.addActionListener(this);
	fileMenu.add(revertPreMosaic);
	replay.addActionListener(this);
	fileMenu.add(replay);
	fileMenu.addSeparator();
	resetMosaicJ.addActionListener(this);
	fileMenu.add(resetMosaicJ);
	fileMenu.addSeparator();
	abort.addActionListener(this);
	fileMenu.add(abort);

	final Menu editMenu = new Menu("Edit");
	selectAll.addActionListener(this);
	editMenu.add(selectAll);
	deselect.addActionListener(this);
	editMenu.add(deselect);
	editMenu.addSeparator();
	stow.addActionListener(this);
	editMenu.add(stow);
	forget.addActionListener(this);
	editMenu.add(forget);
	editMenu.addSeparator();
	nudgeRight.addActionListener(this);
	editMenu.add(nudgeRight);
	nudgeUp.addActionListener(this);
	editMenu.add(nudgeUp);
	nudgeLeft.addActionListener(this);
	editMenu.add(nudgeLeft);
	nudgeDown.addActionListener(this);
	editMenu.add(nudgeDown);

	final Menu objectMenu = new Menu("Object");
	sendToBack.addActionListener(this);
	objectMenu.add(sendToBack);
	objectMenu.addSeparator();
	group.addActionListener(this);
	objectMenu.add(group);
	ungroup.addActionListener(this);
	objectMenu.add(ungroup);
	objectMenu.addSeparator();
	freeze.addActionListener(this);
	objectMenu.add(freeze);
	unfreeze.addActionListener(this);
	objectMenu.add(unfreeze);
	hide.addActionListener(this);
	objectMenu.add(hide);
	showAll.addActionListener(this);
	objectMenu.add(showAll);
	objectMenu.addSeparator();
	maximizeContrast.addActionListener(this);
	objectMenu.add(maximizeContrast);
	setContrast.addActionListener(this);
	objectMenu.add(setContrast);
	resetContrast.addActionListener(this);
	objectMenu.add(resetContrast);

	final Menu scaleMenu = new Menu("Scale");
	minify.addActionListener(this);
	scaleMenu.add(minify);
	magnify.setEnabled(1 < reductionFactor);
	magnify.addActionListener(this);
	scaleMenu.add(magnify);
	scaleMenu.addSeparator();
	quickAndDirty.addActionListener(this);
	scaleMenu.add(quickAndDirty);
	magnification.setLabel(getMagnificationAsString());
	magnification.setEnabled(false);
	scaleMenu.add(magnification);

	final Menu helpMenu = new Menu("Help");
	help.addActionListener(this);
//@	helpMenu.add(help);
	credits.addActionListener(this);
	helpMenu.add(credits);
	status.addActionListener(this);
	helpMenu.add(status);

	final MenuBar menuBar = new MenuBar();
	menuBar.add(fileMenu);
	menuBar.add(editMenu);
	menuBar.add(objectMenu);
	menuBar.add(scaleMenu);
	menuBar.add(helpMenu);
	setMenuBar(menuBar);

	loadPreMosaic(IJ.getDirectory("temp") + "MosaicJPreMosaic", false);
	setActivatedMenus(true, true, true, false, false);
} /* end run */

/*....................................................................
	public methods
....................................................................*/

/*------------------------------------------------------------------*/
public String getMagnificationAsString (
) {
	return("(1\u2215" + reductionFactor
		+ " = " + (100.0 / (double)reductionFactor) + "%)");
} /* end getMagnificationAsString */

/*------------------------------------------------------------------*/
public boolean getQuickAndDirtyScaling (
) {
	return(quickAndDirtyScaling);
} /* end getQuickAndDirtyScaling */

/*------------------------------------------------------------------*/
public static void setActivatedMenus (
	final boolean isEmptyPlayField,
	final boolean noSelectedThumbs,
	final boolean isBackmostThumb,
	final boolean isMultiSelection,
	final boolean containsGroup
) {
	if (preMosaic.empty()) {
		revertPreMosaic.setEnabled(false);
	}
	else {
		revertPreMosaic.setEnabled(true);
	}
	if (isEmptyPlayField) {
		replay.setEnabled(true);
	}
	else {
		replay.setEnabled(false);
	}
	if (isEmptyPlayField || hidden) {
		savePreMosaic.setEnabled(false);
		createMosaic.setEnabled(false);
		blending.setEnabled(false);
		rotation.setEnabled(false);
		outputLog.setEnabled(false);
		selectAll.setEnabled(false);
	}
	else {
		savePreMosaic.setEnabled(true);
		createMosaic.setEnabled(true);
		blending.setEnabled(true);
		rotation.setEnabled(true);
		outputLog.setEnabled(true);
		selectAll.setEnabled(true);
	}
	if (noSelectedThumbs) {
		stow.setEnabled(false);
		forget.setEnabled(false);
		nudgeRight.setEnabled(false);
		nudgeUp.setEnabled(false);
		nudgeLeft.setEnabled(false);
		nudgeDown.setEnabled(false);
		sendToBack.setEnabled(false);
		group.setEnabled(false);
		ungroup.setEnabled(false);
		freeze.setEnabled(false);
		unfreeze.setEnabled(false);
		hide.setEnabled(false);
		maximizeContrast.setEnabled(false);
		setContrast.setEnabled(false);
		resetContrast.setEnabled(false);
		deselect.setEnabled(false);
	}
	else {
		stow.setEnabled(true);
		forget.setEnabled(true);
		nudgeRight.setEnabled(true);
		nudgeUp.setEnabled(true);
		nudgeLeft.setEnabled(true);
		nudgeDown.setEnabled(true);
		sendToBack.setEnabled(!isBackmostThumb);
		group.setEnabled(isMultiSelection);
		ungroup.setEnabled(containsGroup);
		freeze.setEnabled(true);
		unfreeze.setEnabled(true);
		hide.setEnabled(true);
		maximizeContrast.setEnabled(true);
		setContrast.setEnabled(true);
		resetContrast.setEnabled(true);
		deselect.setEnabled(true);
	}
	showAll.setEnabled(hidden);
} /* end setActivatedMenus */

/*------------------------------------------------------------------*/
public void setBlend (
	final boolean blend
) {
	this.blend = blend;
	if (blend) {
		blending.setLabel("Deactivate Blending");
	}
	else {
		blending.setLabel("Activate Blending");
	}
} /* end setBlend */

/*------------------------------------------------------------------*/
public void setCreateOutputLog (
	final boolean createOutputLog
) {
	this.createOutputLog = createOutputLog;
	if (createOutputLog) {
		outputLog.setLabel("Deactivate Log File");
	}
	else {
		outputLog.setLabel("Activate Log File");
	}
} /* end setCreateOutputLog */

/*------------------------------------------------------------------*/
public void setQuickAndDirtyScaling (
	final boolean quickAndDirtyScaling
) {
	this.quickAndDirtyScaling = quickAndDirtyScaling;
	if (quickAndDirtyScaling) {
		quickAndDirty.setLabel("Deactivate Quick&Dirty Scaling");
	}
	else {
		quickAndDirty.setLabel("Activate Quick&Dirty Scaling");
	}
} /* end setQuickAndDirtyScaling */

/*------------------------------------------------------------------*/
public void setRotate (
	final boolean rotate
) {
	this.rotate = rotate;
	if (rotate) {
		rotation.setLabel("Deactivate Rotation");
	}
	else {
		rotation.setLabel("Activate Rotation");
	}
} /* end setRotate */

/*------------------------------------------------------------------*/
public void setSmartDecolorization (
	final boolean smartDecolorization
) {
	this.smartDecolorization = smartDecolorization;
	if (smartDecolorization) {
		decolorization.setLabel("Deactivate Smart Color Conversion");
	}
	else {
		decolorization.setLabel("Activate Smart Color Conversion");
	}
} /* end setSmartDecolorization */

/*------------------------------------------------------------------*/
public void updateScale (
	final int reductionFactor
) {
	this.reductionFactor = reductionFactor;
	setTitle("MosaicJ " + getMagnificationAsString());
	getContentPane().add(progressBar, BorderLayout.NORTH);
	getContentPane().validate();
	final Component[] tiles = playField.getComponents();
	final Component[] thumbs = thumbnailArea.getComponents();
	progressBar.setMaximum(tiles.length + thumbs.length);
	for (int k = 0, K = tiles.length; (k < K); k++) {
		progressBar.setValue(k + 1);
		((MosaicJThumb)tiles[k]).updateScale(reductionFactor,
			quickAndDirtyScaling);
	}
	playField.setReductionFactor(reductionFactor);
	for (int k = 0, K = thumbs.length; (k < K); k++) {
		progressBar.setValue(tiles.length + k + 1);
		thumbnailArea.remove(thumbs[k]);
		((MosaicJThumb)thumbs[k]).updateScale(reductionFactor,
			quickAndDirtyScaling);
		thumbnailArea.add(thumbs[k]);
	}
	thumbnailScrollPane.setViewportView(thumbnailArea);
	getContentPane().remove(progressBar);
	getContentPane().validate();
} /* end updateScale */

/*....................................................................
	private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void loadPreMosaic (
	final String filename,
	final boolean fullRestore
) {
	final int rescuedScale = reductionFactor;
	if (fullRestore) {
		playField.showAll();
		playField.selectAll();
		playField.forgetSelectedThumbs();
		final Component[] thumbs = thumbnailArea.getComponents();
		for (int k = 0, K = thumbs.length; (k < K); k++) {
			thumbnailArea.remove(thumbs[k]);
		}
		updateScale(1);
	}
	preMosaic.removeAllElements();
	try {
		final BufferedReader br = new BufferedReader(new FileReader(filename));
		String line = br.readLine();
		if (line == null) {
			br.close();
			return;
		}
		String[] data = line.split("\t", -1);
		while (data.length == 10) {
			final int x = new Integer(data[5]).intValue();
			final int y = new Integer(data[6]).intValue();
			preMosaic.push(data[9]);
			preMosaic.push(new Boolean(data[8].equals("frozen")));
			preMosaic.push(new Integer(data[7]));
			preMosaic.push(new Point(x, y));
			preMosaic.push(new Double(data[4]));
			preMosaic.push(new Double(data[3]));
			preMosaic.push(new Double(data[2]));
			preMosaic.push(new Double(data[1]));
			preMosaic.push(new Double(data[0]));
			line = br.readLine();
			if (line == null) {
				break;
			}
			data = line.split("\t", -1);
		}
		br.close();
	} catch (FileNotFoundException e) {
		preMosaic.removeAllElements();
		return;
	} catch (IOException e) {
		preMosaic.removeAllElements();
		return;
	}
	if (fullRestore) {
		restoreThumbs(preMosaic);
		updateScale(rescuedScale);
	}
} /* loadPreMosaic */

/*------------------------------------------------------------------*/
private int loadThumb (
	final String filePath,
	final int loadPolicy,
	final Stack justLoaded,
	final int slice,
	final boolean frozen,
	final double grayContrast,
	final double grayOffset,
	final double[] colorWeight
) {
	final ImagePlus imp = new ImagePlus(filePath);
	if ((imp == null) || (!imp.isProcessor())
		|| (imp.getStackSize() < Math.abs(slice))) {
		if (loadPolicy == LOAD_ALL) {
			return(loadPolicy);
		}
		final String[] options = {
			"Open All Valid",
			"Resume Opening",
			"Stop Opening",
			"Cancel"
		};
		final int option = new JOptionPane().showOptionDialog(this,
			"Unable to open\n" + filePath + "\n",
			"Select an Option", JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE, null, options, options[0]);
		switch (option) {
			case 0: {
				return(LOAD_ALL);
			}
			case 1: {
				return(LOAD_SOME);
			}
			case 2: {
				return(LOAD_NO_MORE);
			}
			case JOptionPane.CLOSED_OPTION:
			case 3: {
				while (!justLoaded.isEmpty()) {
					final MosaicJThumb thumb = (MosaicJThumb)justLoaded.pop();
					if (!thumb.isStowed()) {
						thumb.stow();
					}
					thumbnailArea.remove(thumb);
					thumb.incrementScrollWidth(-thumb.getWidth());
				}
				return(LOAD_ABORT);
			}
			default: {
				IJ.error("Internal error.");
				return(LOAD_NO_MORE);
			}
		}
	}
	boolean isColored = false;
	if (imp.getStack().isHSB()) {
		isColored = true;
		new ImageConverter(imp).convertHSBToRGB();
	}
	if (imp.getStack().isRGB()) {
		isColored = true;
		new ImageConverter(imp).convertRGBStackToRGB();
	}
	if ((loadPolicy == LOAD_ONE) && (1 < imp.getStackSize())) {
		getContentPane().add(progressBar, BorderLayout.NORTH);
		getContentPane().validate();
		progressBar.setMaximum(imp.getStackSize());
	}
	for (int k = 1, K = imp.getStackSize(); (k <= K); k++) {
		if ((loadPolicy == LOAD_ONE) && (1 < K)) {
			progressBar.setValue(k);
		}
		if (slice != 0) {
			k = Math.abs(slice);
		}
		final MosaicJThumb thumb = new MosaicJThumb(id++, imp,
			isColored, smartDecolorization, reductionFactor,
			thumbnailArea, thumbnailScrollPane, playField,
			quickAndDirtyScaling, grayContrast, grayOffset, colorWeight,
			frozen, filePath, k, K);
		justLoaded.push(thumb);
		if (0 < slice) {
			thumb.unstow();
		}
		if (slice != 0) {
			k = K;
		}
	}
	if ((loadPolicy == LOAD_ONE) && (1 < imp.getStackSize())) {
		getContentPane().remove(progressBar);
		getContentPane().validate();
	}
	return(loadPolicy);
} /* end loadThumb */

/*------------------------------------------------------------------*/
private void openManyFiles (
) {
	final String directoryPath = new ij.io.OpenDialog("", "").getDirectory();
	if (directoryPath == null) {
		return;
	}
	final File directory = new File(directoryPath);
	if ((directory == null) || (!directory.isDirectory())) {
		return;
	}
	final String[] file = directory.list();
	int loadPolicy = LOAD_SOME;
	final Stack justLoaded = new Stack();
	getContentPane().add(progressBar, BorderLayout.NORTH);
	getContentPane().validate();
	progressBar.setMaximum(file.length);
	for (int k = 0, K = file.length; (k < K); k++) {
		progressBar.setValue(k + 1);
		if (file[k].startsWith(".")) {
			continue;
		}
		loadPolicy = loadThumb(directoryPath + file[k], loadPolicy,
			justLoaded, 0, false, 0.0, 0.0, null);
		if ((loadPolicy == LOAD_ALL) || (loadPolicy == LOAD_SOME)) {
			continue;
		}
		if ((loadPolicy == LOAD_NO_MORE) || (loadPolicy == LOAD_ABORT)) {
			getContentPane().remove(progressBar);
			getContentPane().validate();
			break;
		}
		IJ.error("Internal error.");
		getContentPane().remove(progressBar);
		getContentPane().validate();
		return;
	}
	getContentPane().remove(progressBar);
	getContentPane().validate();
} /* end openManyFiles */

/*------------------------------------------------------------------*/
private void openOneFile (
) {
	final ij.io.OpenDialog openDialog = new ij.io.OpenDialog("", "");
	if ((openDialog.getDirectory() == null)
		|| (openDialog.getFileName() == null)) {
		return;
	}
	loadThumb(openDialog.getDirectory() + openDialog.getFileName(),
		LOAD_ONE, new Stack(), 0, false, 0.0, 0.0, null);
} /* end openOneFile */

/*------------------------------------------------------------------*/
private void releaseResources (
) {
	blending = null;
	createMosaic = null;
	credits = null;
	decolorization = null;
	deselect = null;
	forget = null;
	freeze = null;
	group = null;
	help = null;
	hide = null;
	loadPreMosaic = null;
	maximizeContrast = null;
	nudgeDown = null;
	nudgeLeft = null;
	nudgeRight = null;
	nudgeUp = null;
	outputLog = null;
	quickAndDirty = null;
	replay = null;
	resetMosaicJ = null;
	resetContrast = null;
	revertPreMosaic = null;
	rotation = null;
	savePreMosaic = null;
	selectAll = null;
	sendToBack = null;
	setContrast = null;
	showAll = null;
	status = null;
	stow = null;
	unfreeze = null;
	ungroup = null;
	System.gc();
	dispose();
} /* end releaseResources */

/*------------------------------------------------------------------*/
private void replay (
	final String filename
) {
	preMosaic.removeAllElements();
	final Stack line = new Stack();
	int coloredMosaic = 0;
	int width = 0;
	int height = 0;
	try {
		final BufferedReader br = new BufferedReader(new FileReader(filename));
		String header = br.readLine();
		if (header == null) {
			IJ.error("Invalid log file header.");
			br.close();
			return;
		}
		String[] data = header.split("\t", -1);
		if (data.length != 3) {
			IJ.error("Invalid log file header.");
			br.close();
			return;
		}
		if (data[0].equals("Gray")) {
			coloredMosaic = 1;
		}
		if (data[0].equals("Color")) {
			coloredMosaic = 3;
		}
		if (coloredMosaic == 0) {
			IJ.error("Invalid log file header.");
			br.close();
			return;
		}
		width = new Integer(data[1]).intValue();
		height = new Integer(data[2]).intValue();
		if ((width <= 0) || (height <= 0)) {
			IJ.error("Invalid log file header.");
			br.close();
			return;
		}
		do {
			line.push(br.readLine());
		} while (line.peek() != null);
		line.pop();
		br.close();
	} catch (FileNotFoundException e) {
		return;
	} catch (IOException e) {
		return;
	}
	getContentPane().add(progressBar, BorderLayout.NORTH);
	getContentPane().validate();
	progressBar.setMaximum(line.size());
	ImagePlus globalImage = null;
	ImagePlus globalImageR = null;
	ImagePlus globalImageG = null;
	ImagePlus globalImageB = null;
	switch (coloredMosaic) {
		case 1: {
			globalImage = ij.gui.NewImage.createFloatImage(
				"MosaicJ", width, height, 1,
				ij.gui.NewImage.CHECK_AVAILABLE_MEMORY);
		}
		case 3: {
			globalImageR = ij.gui.NewImage.createFloatImage(
				"MosaicJ", width, height, 1,
				ij.gui.NewImage.CHECK_AVAILABLE_MEMORY);
			globalImageG = ij.gui.NewImage.createFloatImage(
				"MosaicJ", width, height, 1,
				ij.gui.NewImage.CHECK_AVAILABLE_MEMORY);
			globalImageB = ij.gui.NewImage.createFloatImage(
				"MosaicJ", width, height, 1,
				ij.gui.NewImage.CHECK_AVAILABLE_MEMORY);
			break;
		}
	}
	final float[] globalMask = new float[width * height];
	for (int k = 0, K = globalMask.length; (k < K); k++) {
		globalMask[k] = 0.0F;
	}
	for (int k = 1, K = line.size(); (k <= K); k++) {
		progressBar.setValue(k);
		String tile = (String)line.pop();
		final String[] data = tile.split("\t", -1);
		final double[][] sourcePoints = new double[3][2];
		final double[][] targetPoints = new double[3][2];
		final double[] bottomRight = new double[2];
		sourcePoints[0][0] = new Double(data[0]).doubleValue();
		sourcePoints[0][1] = new Double(data[1]).doubleValue();
		sourcePoints[1][0] = new Double(data[2]).doubleValue();
		sourcePoints[1][1] = new Double(data[3]).doubleValue();
		sourcePoints[2][0] = new Double(data[4]).doubleValue();
		sourcePoints[2][1] = new Double(data[5]).doubleValue();
		targetPoints[0][0] = new Double(data[6]).doubleValue();
		targetPoints[0][1] = new Double(data[7]).doubleValue();
		targetPoints[1][0] = new Double(data[8]).doubleValue();
		targetPoints[1][1] = new Double(data[9]).doubleValue();
		targetPoints[2][0] = new Double(data[10]).doubleValue();
		targetPoints[2][1] = new Double(data[11]).doubleValue();
		bottomRight[0] = new Double(data[12]).doubleValue();
		bottomRight[1] = new Double(data[13]).doubleValue();
		final int slice = new Integer(data[14]).intValue();
		final String filePath = data[15];
		final MosaicJThumb thumb = new MosaicJThumb(id++, filePath, slice);
		switch (coloredMosaic) {
			case 1: {
				MosaicJTree.drawGlobalImage(thumb,
					sourcePoints, targetPoints,
					bottomRight, globalImage, globalMask, blend);
				break;
			}
			case 3: {
				MosaicJTree.drawGlobalImage(thumb,
					sourcePoints, targetPoints,
					bottomRight, globalImageR, globalImageG, globalImageB,
					globalMask, blend);
				break;
			}
		}
	}
	switch (coloredMosaic) {
		case 1: {
			MosaicJTree.normalizeGlobalImage(globalImage, globalMask);
			break;
		}
		case 3: {
			globalImage = MosaicJTree.normalizeGlobalImage(
				globalImageR, globalImageG, globalImageB, globalMask,
				width, height);
			break;
		}
	}
	getContentPane().remove(progressBar);
	getContentPane().validate();
	globalImage.show();
	globalImage.updateAndDraw();
} /* replay */

/*------------------------------------------------------------------*/
private void restoreThumbs (
	final Stack preMosaic
) {
	int loadPolicy = LOAD_SOME;
	getContentPane().add(progressBar, BorderLayout.NORTH);
	getContentPane().validate();
	progressBar.setMaximum(preMosaic.size() / 8);
	final Stack justLoaded = new Stack();
	final Iterator i = preMosaic.iterator();
	int k = 0;
	MosaicJThumb firstThumb = null;
	Point firstTrueLocation = null;
	boolean localized = false;
	while (i.hasNext()) {
		final String filePath = (String)i.next();
		final boolean frozen = ((Boolean)i.next()).booleanValue();
		final int slice = ((Integer)i.next()).intValue();
		final Point trueLocation = (Point)i.next();
		final double blueWeight = ((Double)i.next()).doubleValue();
		final double greenWeight = ((Double)i.next()).doubleValue();
		final double redWeight = ((Double)i.next()).doubleValue();
		final double grayOffset = ((Double)i.next()).doubleValue();
		final double grayContrast = ((Double)i.next()).doubleValue();
		final double[] colorWeight = {redWeight, greenWeight, blueWeight};
		progressBar.setValue(++k);
		loadPolicy = loadThumb(filePath, loadPolicy, justLoaded, slice,
			frozen, grayContrast, grayOffset, colorWeight);
		if ((loadPolicy == LOAD_NO_MORE) || (loadPolicy == LOAD_ABORT)) {
			getContentPane().remove(progressBar);
			getContentPane().validate();
			break;
		}
		MosaicJThumb thumb = (MosaicJThumb)justLoaded.peek();
		if ((0 < slice) && !localized) {
			firstThumb = thumb;
			firstTrueLocation = trueLocation;
			localized = true;
		}
		if (0 < slice) {
			final int dx = trueLocation.x - thumb.getLocation().x
				+ firstThumb.getLocation().x - firstTrueLocation.x;
			final int dy = trueLocation.y - thumb.getLocation().y
				+ firstThumb.getLocation().y - firstTrueLocation.y;
			playField.translateSelectedThumbs(dx, dy);
		}
		if ((loadPolicy == LOAD_ALL) || (loadPolicy == LOAD_SOME)) {
			continue;
		}
		IJ.error("Internal error.");
		getContentPane().remove(progressBar);
		getContentPane().validate();
		return;
	}
	getContentPane().remove(progressBar);
	getContentPane().validate();
} /* end restoreThumbs */

/*------------------------------------------------------------------*/
private void savePreMosaic (
	final String filename
) {
	try {
		final FileWriter fw = new FileWriter(filename);
		final Iterator i = preMosaic.iterator();
		while (i.hasNext()) {
			final String filePath = (String)i.next();
			final boolean frozen = ((Boolean)i.next()).booleanValue();
			final int slice = ((Integer)i.next()).intValue();
			final Point trueLocation = (Point)i.next();
			final double blueWeight = ((Double)i.next()).doubleValue();
			final double greenWeight = ((Double)i.next()).doubleValue();
			final double redWeight = ((Double)i.next()).doubleValue();
			final double grayOffset = ((Double)i.next()).doubleValue();
			final double grayContrast = ((Double)i.next()).doubleValue();
			fw.write("" + grayContrast + "\t" + grayOffset);
			fw.write("\t" + redWeight + "\t" + greenWeight + "\t" + blueWeight);
			fw.write("\t" + trueLocation.x + "\t" + trueLocation.y);
			fw.write("\t" + slice);
			fw.write("\t" + (frozen ? ("frozen") : ("unfrozen")));
			fw.write("\t" + filePath + "\n");
		}
		final Component[] thumbs = thumbnailArea.getComponents();
		for (int k = 0, K = thumbs.length; (k < K); k++) {
			final MosaicJThumb thumb = (MosaicJThumb)thumbs[k];
			final Point trueLocation = thumb.getTrueLocation();
			final double[] colorWeight = thumb.getColorWeights();
			fw.write("" + thumb.getGrayContrast()
				+ "\t" + thumb.getGrayOffset());
			fw.write("\t" + colorWeight[0]
				+ "\t" + colorWeight[1] + "\t" + colorWeight[2]);
			fw.write("\t" + trueLocation.x + "\t" + trueLocation.y);
			fw.write("\t" + -thumb.getSlice());
			fw.write("\t" + (thumb.isFrozen() ? ("frozen") : ("unfrozen")));
			fw.write("\t" + thumb.getFilePath() + "\n");
		}
		fw.close();
	} catch (IOException e) {
		IJ.log("IOException " + e.getMessage());
		preMosaic.removeAllElements();
		return;
	}
} /* savePreMosaic */

} /* end class MosaicJ_ */

/*====================================================================
|	MosaicJCredits
\===================================================================*/
class MosaicJCredits
	extends
		JDialog
	implements
		ActionListener

{ /* begin class MosaicJCredits */

/*....................................................................
	ActionListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void actionPerformed (
	final ActionEvent e
) {
	if (e.getActionCommand().equals("Done")) {
		dispose();
	}
} /* end actionPerformed */

/*....................................................................
	Constructors
....................................................................*/

/*------------------------------------------------------------------*/
public MosaicJCredits (
	final JFrame parentWindow
) {
	super(parentWindow, "MosaicJ", true);
	getContentPane().setLayout(new BorderLayout(0, 20));
	final JPanel buttonPanel = new JPanel();
	buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	final JButton doneButton = new JButton("Done");
	doneButton.addActionListener(this);
	buttonPanel.add(doneButton);
	final TextArea text = new TextArea(23, 56);
	text.setEditable(false);
	text.append("\n");
	text.append(" This work is based on the following paper:\n");
	text.append("\n");
	text.append(" P. Th\u00E9venaz, M. Unser\n");
	text.append(" User-Friendly Semiautomated Assembly of Accurate Image"
		+ " Mosaics in Microscopy\n");
	text.append(" Microscopy Research and Technique\n");
	text.append(" vol. 70, no. 2, pp. 135-146, February 2007\n");
	text.append("\n");
	text.append(" This paper is available on-line at\n");
	text.append(" http://bigwww.epfl.ch/publications/thevenaz0701.html\n");
	text.append("\n");
	text.append(" Other relevant on-line publications are available at\n");
	text.append(" http://bigwww.epfl.ch/publications/\n");
	text.append("\n");
	text.append(" Additional help available at\n");
	text.append(" http://bigwww.epfl.ch/thevenaz/mosaicj/\n");
	text.append("\n");
	text.append(" You'll be free to use this software for research purposes,\n");
	text.append(" but you should not redistribute it without our consent. In\n");
	text.append(" addition, we expect you to include a citation or an\n");
	text.append(" acknowledgment whenever you present or publish results\n");
	text.append(" that are based on it.\n");
	getContentPane().add("Center", text);
	getContentPane().add("South", buttonPanel);
	pack();
	GUI.center(this);
	setVisible(true);
} /* end MosaicJCredits */

} /* end class MosaicJCredits */

/*====================================================================
|	MosaicJEdge
\===================================================================*/
class MosaicJEdge
	implements
		Comparable

{ /* begin class MosaicJEdge */

/*....................................................................
	private variables
....................................................................*/

private MosaicJThumb source = null;
private MosaicJThumb target = null;
private double edgeWeight = 0.0;
private double[][] sourcePoints = new double[3][2];
private double[][] targetPoints = new double[3][2];

/*....................................................................
	Constructors
....................................................................*/

/*------------------------------------------------------------------*/
public MosaicJEdge (
	final MosaicJThumb source,
	final MosaicJThumb target,
	final double edgeWeight
) {
	this.source = source;
	this.target = target;
	this.edgeWeight = edgeWeight;
} /* end MosaicJEdge */

/*....................................................................
	Comparable methods
....................................................................*/

/*------------------------------------------------------------------*/
public int compareTo (
	final Object obj
) {
	if (edgeWeight < ((MosaicJEdge)obj).getEdgeWeight()) {
		return(-1);
	}
	if (((MosaicJEdge)obj).getEdgeWeight() < edgeWeight) {
		return(1);
	}
	return(0);
} /* end compareTo */

/*....................................................................
	public methods
....................................................................*/

/*------------------------------------------------------------------*/
public double getEdgeWeight (
) {
	return(edgeWeight);
} /* end getEdgeWeight */

/*------------------------------------------------------------------*/
public double[][] getMatrixFromSourceToTarget (
) {
	final double[][] matrix = new double[3][3];
	final double angle = Math.atan2(targetPoints[2][0] - targetPoints[1][0],
		targetPoints[2][1] - targetPoints[1][1])
		- Math.atan2(sourcePoints[2][0] - sourcePoints[1][0],
		sourcePoints[2][1] - sourcePoints[1][1]);
	final double c = Math.cos(angle);
	final double s = -Math.sin(angle);
	matrix[0][0] = c;
	matrix[0][1] = -s;
	matrix[0][2] = targetPoints[0][0]
		- c * sourcePoints[0][0] + s * sourcePoints[0][1];
	matrix[1][0] = s;
	matrix[1][1] = c;
	matrix[1][2] = targetPoints[0][1]
		- s * sourcePoints[0][0] - c * sourcePoints[0][1];
	matrix[2][0] = 0.0;
	matrix[2][1] = 0.0;
	matrix[2][2] = 1.0;
	return(matrix);
} /* end getMatrixFromSourceToTarget */

/*------------------------------------------------------------------*/
public MosaicJThumb getNeighbor (
	final MosaicJThumb vertex
) {
	if (vertex == source) {
		return(target);
	}
	if (vertex == target) {
		return(source);
	}
	return(null);
} /* end getNeighbor */

/*------------------------------------------------------------------*/
public MosaicJThumb getSource (
) {
	return(source);
} /* end getSource */

/*------------------------------------------------------------------*/
public MosaicJThumb getTarget (
) {
	return(target);
} /* end getTarget */

/*------------------------------------------------------------------*/
public void setSourcePoints (
	final double[][] s
) {
	sourcePoints[0][0] = s[0][0];
	sourcePoints[0][1] = s[0][1];
	sourcePoints[1][0] = s[1][0];
	sourcePoints[1][1] = s[1][1];
	sourcePoints[2][0] = s[2][0];
	sourcePoints[2][1] = s[2][1];
} /* end setSourcePoints */

/*------------------------------------------------------------------*/
public void setTargetPoints (
	final double[][] t
) {
	targetPoints[0][0] = t[0][0];
	targetPoints[0][1] = t[0][1];
	targetPoints[1][0] = t[1][0];
	targetPoints[1][1] = t[1][1];
	targetPoints[2][0] = t[2][0];
	targetPoints[2][1] = t[2][1];
} /* end setTargetPoints */

/*------------------------------------------------------------------*/
public void swap (
) {
	final MosaicJThumb s = source;
	source = target;
	target = s;
	final double[][] p = sourcePoints;
	sourcePoints = targetPoints;
	targetPoints = p;
} /* end swap */

} /* end class MosaicJEdge */

/*====================================================================
|	MosaicJHierarchy
\===================================================================*/
class MosaicJHierarchy
	extends
		Component

{ /* begin class MosaicJHierarchy */

/*....................................................................
	private variables
....................................................................*/

private MosaicJThumb thumb = null;
private Point trueLocation = null;
private boolean frozen = false;
private boolean selected = false;
private final Stack children = new Stack();
private final Stack outline = new Stack();
private final Stack overlap = new Stack();
private int reductionFactor = 1;

/*....................................................................
	Constructors
....................................................................*/

/*------------------------------------------------------------------*/
public MosaicJHierarchy (
	final int reductionFactor
) {
	this.reductionFactor = reductionFactor;
} /* end MosaicJHierarchy */

/*------------------------------------------------------------------*/
public MosaicJHierarchy (
	final MosaicJThumb thumb,
	final int reductionFactor
) {
	this.thumb = thumb;
	this.frozen = thumb.isFrozen();
	this.reductionFactor = reductionFactor;
	setBounds(thumb.getBounds());
	trueLocation = new Point(
		thumb.getLocation().x * reductionFactor,
		thumb.getLocation().y * reductionFactor);
	thumb.setTrueLocation(trueLocation);
	createOutline();
} /* end MosaicJHierarchy */

/*....................................................................
	Component methods
....................................................................*/

/*------------------------------------------------------------------*/
public void setLocation (
	final Point location
) {
	final int dx = location.x - getLocation().x;
	final int dy = location.y - getLocation().y;
	trueLocation.x += dx * reductionFactor;
	trueLocation.y += dy * reductionFactor;
	final Rectangle bounds = new Rectangle(getBounds());
	bounds.translate(dx, dy);
	setBounds(bounds);
	final Iterator i = children.iterator();
	while (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		final Point p = new Point(thumbs.getLocation());
		p.translate(dx, dy);
		thumbs.setLocation(p);
	}
	if (thumb != null) {
		final Point p = new Point(thumb.getLocation());
		p.translate(dx, dy);
		thumb.setLocation(p);
		thumb.translateTrueLocation(dx * reductionFactor, dy * reductionFactor);
	}
} /* end setLocation */

/*....................................................................
	public methods
....................................................................*/

/*------------------------------------------------------------------*/
public void add (
	final MosaicJHierarchy thumbs
) {
	if (trueLocation == null) {
		trueLocation = new Point(thumbs.getTrueLocation());
		setBounds(thumbs.getBounds());
	}
	else {
		trueLocation.x = Math.min(trueLocation.x, thumbs.getTrueLocation().x);
		trueLocation.y = Math.min(trueLocation.y, thumbs.getTrueLocation().y);
	}
	final Rectangle bounds = getBounds();
	bounds.add(thumbs.getBounds());
	setBounds(bounds);
	thumbs.setSelected(false);
	children.push(thumbs);
	createOverlap();
	createOutline();
} /* end add */

/*------------------------------------------------------------------*/
public boolean contains (
	final int x,
	final int y
) {
	final Iterator i = children.iterator();
	while (i.hasNext()) {
		if (((MosaicJHierarchy)i.next()).contains(x, y)) {
			return(true);
		}
	}
	if ((thumb != null) && (thumb.contains(x, y))) {
		return(true);
	}
	return(false);
} /* end contains */

/*------------------------------------------------------------------*/
public Stack getChildren (
) {
	return(children);
} /* end getChildren */

/*------------------------------------------------------------------*/
public MosaicJThumb getThumb (
) {
	return(thumb);
} /* end getThumb */

/*------------------------------------------------------------------*/
public Stack getThumbs (
) {
	final Stack thumbs = new Stack();
	final Iterator i = children.iterator();
	while (i.hasNext()) {
		thumbs.addAll(((MosaicJHierarchy)i.next()).getThumbs());
	}
	if (thumb != null) {
		thumbs.push(thumb);
	}
	return(thumbs);
} /* end getThumbs */

/*------------------------------------------------------------------*/
public boolean hasChildren (
) {
	return(!children.isEmpty());
} /* end hasChildren */

/*------------------------------------------------------------------*/
public boolean isFrozen (
) {
	return(frozen);
} /* end isFrozen */

/*------------------------------------------------------------------*/
public boolean isSelected (
) {
	return(selected);
} /* end isSelected */

/*------------------------------------------------------------------*/
public void paintOutline (
	final Graphics g
) {
	if (hasChildren()) {
		if (frozen) {
			g.setColor(new Color(255, 128, 32));
		}
		else {
			g.setColor(new Color(64, 255, 96));
		}
	}
	else {
		if (frozen) {
			g.setColor(new Color(255, 64, 64));
		}
		else {
			g.setColor(new Color(64, 96, 255));
		}
	}
	final int dx = getLocation().x;
	final int dy = getLocation().y;
	final Iterator i = outline.iterator();
	while (i.hasNext()) {
		final Point p = (Point)i.next();
		g.fillRect(p.x + dx, p.y + dy, 1, 1);
	}
} /* end paintOutline */

/*------------------------------------------------------------------*/
public void setFrozen (
	final boolean frozen
) {
	final Iterator i = children.iterator();
	while (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		thumbs.setFrozen(frozen);
	}
	if (thumb != null) {
		thumb.setFrozen(frozen);
	}
	this.frozen = frozen;
} /* end setFrozen */

/*------------------------------------------------------------------*/
public void setSelected (
	final boolean isSelected
) {
	selected = isSelected;
} /* end setSelected */

/*------------------------------------------------------------------*/
public void updateReductionfactor (
	final int reductionFactor
) {
	this.reductionFactor = reductionFactor;
	final Rectangle bounds = new Rectangle(new Point(
		trueLocation.x / reductionFactor, trueLocation.y / reductionFactor),
		new Dimension(0, 0));
	final Iterator i = children.iterator();
	while (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		thumbs.updateReductionfactor(reductionFactor);
		bounds.add(thumbs.getBounds());
	}
	if (thumb != null) {
		final Rectangle thumbBounds = new Rectangle(
			trueLocation.x / reductionFactor, trueLocation.y / reductionFactor,
			thumb.getPreferredSize().width, thumb.getPreferredSize().height);
		thumb.setBounds(thumbBounds);
		bounds.add(thumbBounds);
	}
	setBounds(bounds);
	createOverlap();
	createOutline();
} /* end updateReductionfactor */

/*....................................................................
	private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void createOutline (
) {
	outline.removeAllElements();
	final Iterator i = children.iterator();
	while (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		final int dx = thumbs.getBounds().x - getBounds().x;
		final int dy = thumbs.getBounds().y - getBounds().y;
		final Stack partialOutline = thumbs.getOutline();
		while (!partialOutline.isEmpty()) {
			final Point p = new Point((Point)partialOutline.pop());
			p.translate(dx, dy);
			if (!overlapContains(p)) {
				outline.push(p);
			}
		}
	}
	if (thumb != null) {
		for (int x = 1, X = getBounds().width - 1, Y = getBounds().height - 1;
			(x < X); x++) {
			outline.push(new Point(x, 0));
			outline.push(new Point(x, Y));
		}
		for (int y = 1, X = getBounds().width - 1, Y = getBounds().height - 1;
			(y < Y); y++) {
			outline.push(new Point(0, y));
			outline.push(new Point(X, y));
		}
	}
} /* end createOutline */

/*------------------------------------------------------------------*/
private void createOverlap (
) {
	overlap.removeAllElements();
	final int dx = -getBounds().x;
	final int dy = -getBounds().y;
	final Stack thumbs = getThumbs();
	while (!thumbs.isEmpty()) {
		final Rectangle currentArea = ((MosaicJThumb)thumbs.pop()).getBounds();
		final Iterator i = thumbs.iterator();
		while (i.hasNext()) {
			final Rectangle commonArea = currentArea.intersection(
				((MosaicJThumb)i.next()).getBounds());
			if (!commonArea.isEmpty()) {
				commonArea.translate(dx, dy);
				overlap.push(commonArea);
			}
		}
	}
} /* end createOverlap */

/*------------------------------------------------------------------*/
private Stack getOutline (
) {
	return((Stack)outline.clone());
} /* end getOutline */

/*------------------------------------------------------------------*/
private Point getTrueLocation (
) {
	return(trueLocation);
} /* end getTrueLocation */

/*------------------------------------------------------------------*/
private boolean overlapContains (
	final Point p
) {
	boolean inside = false;
	final Iterator i = overlap.iterator();
	while (i.hasNext() && (!inside)) {
		inside |= ((Rectangle)i.next()).contains(p);
	}
	return(inside);
} /* end overlapContains */

} /* end class MosaicJHierarchy */

/*====================================================================
|	MosaicJPlayField
\===================================================================*/
class MosaicJPlayField
	extends
		Container
	implements
		LayoutManager,
		MouseListener,
		MouseMotionListener

{ /* begin class MosaicJPlayField */

/*....................................................................
	private variables
....................................................................*/

private Dimension playFieldSize = null;
private JScrollPane workScrollPane = null;
private Point mouse = null;
private final Stack selectedThumbs = new Stack();
private final Stack stackingOrder = new Stack();
private int minHeight = 0;
private int minWidth = 0;
private int reductionFactor = 1;

/*....................................................................
	Constructors
....................................................................*/

/*------------------------------------------------------------------*/
public MosaicJPlayField (
	final int minWidth,
	final int minHeight,
	final JScrollPane workScrollPane,
	final int reductionFactor
) {
	this.minWidth = minWidth;
	this.minHeight = minHeight;
	this.playFieldSize = new Dimension(minWidth, minHeight);
	this.workScrollPane = workScrollPane;
	this.reductionFactor = reductionFactor;
	setLayout(this);
	addMouseListener(this);
	addMouseMotionListener(this);
} /* end MosaicJPlayField */

/*....................................................................
	Component methods
....................................................................*/

/*------------------------------------------------------------------*/
public void paint (
	final Graphics g
) {
	final Dimension dimension = getSize();
	final int width = dimension.width;
	final int height = dimension.height;
	final Rectangle viewRect = workScrollPane.getViewport().getViewRect();
	g.setColor(new Color(236, 236, 236));
	g.fillRect(viewRect.x, viewRect.y, viewRect.width - 1, viewRect.height - 1);
	g.setColor(new Color(228, 228, 228));
	for (int x = 6; (x < width); x += 12) {
		g.drawLine(x, 0, x, height - 1);
	}
	for (int y = 6; (y < height); y += 12) {
		g.drawLine(0, y, width - 1, y);
	}
	boolean backmost = true;
	boolean containsGroup = false;
	final Iterator i = stackingOrder.iterator();
	while (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		if (thumbs.isVisible()) {
			final Stack j = thumbs.getThumbs();
			while (!j.isEmpty()) {
				final MosaicJThumb thumb = (MosaicJThumb)j.pop();
				thumb.setOpacity(thumb.getBounds(), 0xFF);
				final Iterator k = getOverlap(thumb).iterator();
				while (k.hasNext()) {
					thumb.setOpacity((Rectangle)k.next(), 0x7F);
				}
				thumb.paint(g);
			}
			if (thumbs.isSelected()) {
				backmost &=
					(stackingOrder.indexOf(thumbs) < selectedThumbs.size());
				containsGroup |= thumbs.hasChildren();
				thumbs.paintOutline(g);
			}
		}
	}
	MosaicJ_.setActivatedMenus(stackingOrder.isEmpty(),
		selectedThumbs.isEmpty(), backmost, 1 < selectedThumbs.size(),
		containsGroup);
} /* end paint */

/*....................................................................
	Container methods
....................................................................*/

/*------------------------------------------------------------------*/
public Component getComponentAt (
	final Point p
) {
	final Stack i = (Stack)stackingOrder.clone();
	while (!i.isEmpty()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.pop();
		if ((thumbs.isVisible()) && (thumbs.contains(p))) {
			return(thumbs);
		}
	}
	return(null);
} /* end getComponentAt */

/*....................................................................
	LayoutManager methods
....................................................................*/

/*------------------------------------------------------------------*/
public void addLayoutComponent (
	final String name,
	final Component comp
) {
	final MosaicJThumb thumb = (MosaicJThumb)comp;
	final Rectangle viewRect = workScrollPane.getViewport().getViewRect();
	final Dimension thumbSize = thumb.getPreferredSize();
	thumb.setBounds(viewRect.x + (viewRect.width - thumbSize.width) / 2,
		viewRect.y + (viewRect.height - thumbSize.height) / 2,
		thumbSize.width, thumbSize.height);
	final MosaicJHierarchy thumbs = new MosaicJHierarchy(
		thumb, reductionFactor);
	stackingOrder.push(thumbs);
	validate();
	deselectThumbs();
	select(thumbs);
} /* end addLayoutComponent */

/*------------------------------------------------------------------*/
public void layoutContainer (
	final Container parent
) {
	if (stackingOrder.isEmpty()) {
		playFieldSize = new Dimension(minWidth, minHeight);
		return;
	}
	final Rectangle playFieldRectangle =
		getCircumbscribedRectangle(stackingOrder);
	int dx = 0;
	int dy = 0;
	final JViewport viewport = workScrollPane.getViewport();
	final Point o = viewport.getViewPosition();
	if (playFieldRectangle.width < minWidth) {
		if (playFieldRectangle.x < 0) {
			dx = -playFieldRectangle.x;
		}
		if (minWidth < (playFieldRectangle.x + playFieldRectangle.width)) {
			dx = minWidth - (playFieldRectangle.x + playFieldRectangle.width);
		}
		o.translate(-o.x, 0);
	}
	else {
		dx = -playFieldRectangle.x;
		if ((playFieldRectangle.x + playFieldRectangle.width) < minWidth) {
			o.translate(playFieldRectangle.width - minWidth - o.x, 0);
		}
		else if (0 < playFieldRectangle.x) {
			o.translate(-o.x, 0);
		}
		else {
			o.translate(dx, 0);
		}
	}
	if (playFieldRectangle.height < minHeight) {
		if (playFieldRectangle.y < 0) {
			dy = -playFieldRectangle.y;
		}
		if (minHeight < (playFieldRectangle.y + playFieldRectangle.height)) {
			dy = minHeight - (playFieldRectangle.y + playFieldRectangle.height);
		}
		o.translate(0, -o.y);
	}
	else {
		dy = -playFieldRectangle.y;
		if ((playFieldRectangle.y + playFieldRectangle.height) < minHeight) {
			o.translate(0, playFieldRectangle.height - minHeight - o.y);
		}
		else if (0 < playFieldRectangle.y) {
			o.translate(0, -o.y);
		}
		else {
			o.translate(0, dy);
		}
	}
	viewport.setViewPosition(o);
	if ((dx != 0) || (dy != 0)) {
		final Iterator i = stackingOrder.iterator();
		while (i.hasNext()) {
			final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
			final Point p = new Point(thumbs.getLocation());
			p.translate(dx, dy);
			thumbs.setLocation(p);
		}
		playFieldRectangle.translate(dx, dy);
	}
	playFieldRectangle.add(new Rectangle(
		playFieldRectangle.getLocation(), minimumLayoutSize(null)));
	playFieldSize = playFieldRectangle.getSize();
	workScrollPane.setViewport(viewport);
} /* end layoutContainer */

/*------------------------------------------------------------------*/
public Dimension minimumLayoutSize (
	final Container parent
) {
	return(new Dimension(minWidth, minHeight));
} /* end minimumLayoutSize */

/*------------------------------------------------------------------*/
public Dimension preferredLayoutSize (
	final Container parent
) {
	return(playFieldSize);
} /* end preferredLayoutSize */

/*------------------------------------------------------------------*/
public void removeLayoutComponent (
	final Component comp
) {
} /* end removeLayoutComponent */

/*....................................................................
	MouseListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void mouseClicked (
	final MouseEvent e
) {
	mouse = e.getPoint();
	final MosaicJHierarchy thumbs = (MosaicJHierarchy)getComponentAt(mouse);
	if ((thumbs != null) && (thumbs.isSelected()) && (!e.isShiftDown())) {
		deselectThumbs();
		stackingOrder.remove(thumbs);
		stackingOrder.push(thumbs);
		select(thumbs);
	}
} /* end mouseClicked */

/*------------------------------------------------------------------*/
public void mouseEntered (
	final MouseEvent e
) {
} /* end mouseEntered */

/*------------------------------------------------------------------*/
public void mouseExited (
	final MouseEvent e
) {
} /* end mouseExited */

/*------------------------------------------------------------------*/
public void mousePressed (
	final MouseEvent e
) {
	mouse = e.getPoint();
	final MosaicJHierarchy thumbs = (MosaicJHierarchy)getComponentAt(mouse);
	if (thumbs == null) {
		if (!e.isShiftDown()) {
			deselectThumbs();
		}
		mouse = null;
		return;
	}
	if (thumbs.isSelected()) {
		if (e.isShiftDown()) {
			mouse = null;
			deselect(thumbs);
		}
		else {
			stackingOrder.remove(thumbs);
			stackingOrder.push(thumbs);
		}
	}
	else {
		if (!e.isShiftDown()) {
			deselectThumbs();
		}
		stackingOrder.remove(thumbs);
		stackingOrder.push(thumbs);
		select(thumbs);
	}
} /* end mousePressed */

/*------------------------------------------------------------------*/
public void mouseReleased (
	final MouseEvent e
) {
	if (mouse == null) {
		return;
	}
	mouse = null;
	validate();
	repaint();
} /* end mouseReleased */

/*....................................................................
	MouseMotionListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void mouseDragged (
	final MouseEvent e
) {
	if (mouse == null) {
		return;
	}
	final Point p = e.getPoint();
	translateSelectedThumbs(p.x - mouse.x, p.y - mouse.y);
	mouse = p;
} /* end mouseDragged */

/*------------------------------------------------------------------*/
public void mouseMoved (
	final MouseEvent e
) {
} /* end mouseMoved */

/*....................................................................
	public methods
....................................................................*/

/*------------------------------------------------------------------*/
public void deselectThumbs (
) {
	while (!selectedThumbs.isEmpty()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)selectedThumbs.pop();
		thumbs.setSelected(false);
		final Rectangle bounds = thumbs.getBounds();
		repaint(bounds.x, bounds.y, bounds.width, bounds.height);
	}
} /* end deselectThumbs */

/*------------------------------------------------------------------*/
public void forgetSelectedThumbs (
) {
	while (!selectedThumbs.isEmpty()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)selectedThumbs.pop();
		if (thumbs.hasChildren()) {
			selectedThumbs.push(thumbs);
			ungroupSelectedThumbs();
		}
		else {
			stackingOrder.remove(thumbs);
			remove(thumbs.getThumb());
		}
	}
	validate();
	repaint();
} /* end forgetSelectedThumbs */

/*------------------------------------------------------------------*/
public void freezeSelectedThumbs (
) {
	Iterator i = selectedThumbs.iterator();
	while (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		thumbs.setFrozen(true);
	}
	repaint();
} /* end freezeSelectedThumbs */

/*------------------------------------------------------------------*/
public Stack getStackingOrder (
) {
	return(stackingOrder);
} /* end getStackingOrder */

/*------------------------------------------------------------------*/
public void groupSelectedThumbs (
) {
	boolean frozen = false;
	boolean homogenous = true;
	Iterator i = selectedThumbs.iterator();
	if (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		frozen = thumbs.isFrozen();
	}
	while (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		homogenous = (frozen == thumbs.isFrozen());
	}
	if (!homogenous) {
		final String[] options = {
			"Freeze All",
			"Unfreeze All",
			"Cancel"
		};
		final int option = new JOptionPane().showOptionDialog(this,
			"Grouping a mixture of frozen and unfrozen tiles\n",
			"Select an Option", JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE, null, options, options[2]);
		switch (option) {
			case 0: {
				frozen = true;
				homogenous = true;
				break;
			}
			case 1: {
				frozen = false;
				homogenous = true;
				break;
			}
			case JOptionPane.CLOSED_OPTION:
			case 2: {
				return;
			}
			default: {
				IJ.error("Internal error.");
				return;
			}
		}
	}
	final MosaicJHierarchy group = new MosaicJHierarchy(reductionFactor);
	while (!selectedThumbs.isEmpty()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)selectedThumbs.pop();
		stackingOrder.remove(thumbs);
		group.add(thumbs);
	}
	group.setFrozen(frozen);
	stackingOrder.add(group);
	select(group);
} /* end groupSelectedThumbs */

/*------------------------------------------------------------------*/
public void hideSelectedThumbs (
) {
	while (!selectedThumbs.isEmpty()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)selectedThumbs.pop();
		thumbs.setSelected(false);
		thumbs.setVisible(false);
	}
	repaint();
} /* end hideSelectedThumbs */

/*------------------------------------------------------------------*/
public void maximizeContrastSelectedThumbs (
) {
	MosaicJThumb.setMinAndMaxGrays(1.0, -1.0);
	Iterator i = selectedThumbs.iterator();
	while (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		final Stack j = thumbs.getThumbs();
		while (!j.isEmpty()) {
			final MosaicJThumb thumb = (MosaicJThumb)j.pop();
			thumb.updateMinAndMax();
		}
	}
	if (MosaicJThumb.getMax() <= MosaicJThumb.getMin()) {
		return;
	}
	final double grayContrast = 255.0
		/ (MosaicJThumb.getMax() - MosaicJThumb.getMin());
	final double grayOffset = -MosaicJThumb.getMin();
	i = selectedThumbs.iterator();
	while (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		final Stack j = thumbs.getThumbs();
		while (!j.isEmpty()) {
			final MosaicJThumb thumb = (MosaicJThumb)j.pop();
			thumb.setContrastAndOffset(grayContrast, grayOffset);
		}
	}
	repaint();
} /* end maximizeContrastSelectedThumbs */

/*------------------------------------------------------------------*/
public void recordThumbs (
	final Stack preMosaic
) {
	preMosaic.removeAllElements();
	final Iterator i = stackingOrder.iterator();
	while (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		final Stack j = thumbs.getThumbs();
		while (!j.isEmpty()) {
			final MosaicJThumb thumb = (MosaicJThumb)j.pop();
			preMosaic.push(thumb.getFilePath());
			preMosaic.push(new Boolean(thumb.isFrozen()));
			preMosaic.push(new Integer(thumb.getSlice()));
			preMosaic.push(thumb.getTrueLocation());
			preMosaic.push(new Double(thumb.getColorWeights()[2]));
			preMosaic.push(new Double(thumb.getColorWeights()[1]));
			preMosaic.push(new Double(thumb.getColorWeights()[0]));
			preMosaic.push(new Double(thumb.getGrayOffset()));
			preMosaic.push(new Double(thumb.getGrayContrast()));
		}
	}
} /* end recordThumbs */

/*------------------------------------------------------------------*/
public void resetContrastSelectedThumbs (
) {
	final Iterator i = selectedThumbs.iterator();
	while (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		final Stack j = thumbs.getThumbs();
		while (!j.isEmpty()) {
			final MosaicJThumb thumb = (MosaicJThumb)j.pop();
			thumb.setContrastAndOffset(1.0, 0.0);
		}
	}
	repaint();
} /* end resetContrastSelectedThumbs */

/*------------------------------------------------------------------*/
public void selectAll (
) {
	final Iterator i = stackingOrder.iterator();
	while (i.hasNext()) {
		select((MosaicJHierarchy)i.next());
	}
} /* end selectAll */

/*------------------------------------------------------------------*/
public void sendToBackSelectedThumbs (
) {
	while (!selectedThumbs.isEmpty()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)selectedThumbs.pop();
		stackingOrder.remove(thumbs);
		stackingOrder.add(0, thumbs);
		thumbs.setSelected(false);
		final Rectangle bounds = thumbs.getBounds();
		repaint(bounds.x, bounds.y, bounds.width, bounds.height);
	}
} /* end sendToBackSelectedThumbs */

/*------------------------------------------------------------------*/
public void setContrastSelectedThumbs (
	final double min,
	final double max
) {
	MosaicJThumb.setMinAndMaxGrays(min, max);
	final double grayContrast = 255.0 / (max - min);
	final double grayOffset = -min;
	final Iterator i = selectedThumbs.iterator();
	while (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		final Stack j = thumbs.getThumbs();
		while (!j.isEmpty()) {
			final MosaicJThumb thumb = (MosaicJThumb)j.pop();
			thumb.setContrastAndOffset(grayContrast, grayOffset);
		}
	}
	repaint();
} /* end setContrastSelectedThumbs */

/*------------------------------------------------------------------*/
public void setReductionFactor (
	final int reductionFactor
) {
	if (stackingOrder.isEmpty()) {
		this.reductionFactor = reductionFactor;
		return;
	}
	Iterator i = stackingOrder.iterator();
	while (i.hasNext()) {
		((MosaicJHierarchy)i.next()).updateReductionfactor(reductionFactor);
	}
	Point translation = null;
	if (reductionFactor < this.reductionFactor) {
		translation = magnifyViewport();
	}
	else {
		translation = minifyViewport();
	}
	if ((translation.x != 0) || (translation.y != 0)) {
		i = stackingOrder.iterator();
		while (i.hasNext()) {
			final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
			final Point p = new Point(thumbs.getLocation());
			p.translate(translation.x, translation.y);
			thumbs.setLocation(p);
		}
	}
	this.reductionFactor = reductionFactor;
	validate();
	repaint();
} /* end setReductionFactor */

/*------------------------------------------------------------------*/
public void showAll (
) {
	deselectThumbs();
	final Iterator i = stackingOrder.iterator();
	while (i.hasNext()) {
		((MosaicJHierarchy)i.next()).setVisible(true);
	}
	repaint();
} /* end showAll */

/*------------------------------------------------------------------*/
public void stowSelectedThumbs (
) {
	while (!selectedThumbs.isEmpty()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)selectedThumbs.pop();
		if (thumbs.hasChildren()) {
			selectedThumbs.push(thumbs);
			ungroupSelectedThumbs();
		}
		else {
			stackingOrder.remove(thumbs);
			final MosaicJThumb thumb = thumbs.getThumb();
			remove(thumb);
			thumb.setOpacity(thumb.getBounds(), 0xFF);
			thumb.stow();
		}
	}
	validate();
	repaint();
} /* end stowSelectedThumbs */

/*------------------------------------------------------------------*/
public void translateSelectedThumbs (
	final int dx,
	final int dy
) {
	final Iterator i = selectedThumbs.iterator();
	while (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		final Point p = new Point(thumbs.getLocation());
		p.translate(dx, dy);
		thumbs.setLocation(p);
		final Rectangle bounds = thumbs.getBounds();
		repaint(bounds.x, bounds.y, bounds.width, bounds.height);
	}
} /* end translateSelectedThumbs */

/*------------------------------------------------------------------*/
public void unfreezeSelectedThumbs (
) {
	Iterator i = selectedThumbs.iterator();
	while (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		thumbs.setFrozen(false);
	}
	repaint();
} /* end unfreezeSelectedThumbs */

/*------------------------------------------------------------------*/
public void ungroupSelectedThumbs (
) {
	final Stack ungrouped = new Stack();
	while (!selectedThumbs.isEmpty()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)selectedThumbs.pop();
		if (thumbs.hasChildren()) {
			stackingOrder.remove(thumbs);
			final Stack children = thumbs.getChildren();
			while (!children.isEmpty()) {
				final MosaicJHierarchy child = (MosaicJHierarchy)children.pop();
				stackingOrder.push(child);
				ungrouped.push(child);
			}
		}
		else {
			ungrouped.push(thumbs);
		}
	}
	while (!ungrouped.isEmpty()) {
		select((MosaicJHierarchy)ungrouped.pop());
	}
} /* end ungroupSelectedThumbs */

/*....................................................................
	private methods
....................................................................*/

/*------------------------------------------------------------------*/
public void deselect (
	final MosaicJHierarchy thumbs
) {
	selectedThumbs.remove(thumbs);
	thumbs.setSelected(false);
	final Rectangle bounds = thumbs.getBounds();
	repaint(bounds.x, bounds.y, bounds.width, bounds.height);
} /* end deselect */

/*------------------------------------------------------------------*/
private Rectangle getCircumbscribedRectangle (
	final Stack thumbs
) {
	if (thumbs.isEmpty()) {
		return(null);
	}
	final Iterator i = thumbs.iterator();
	final Rectangle playFieldRectangle =
		((MosaicJHierarchy)i.next()).getBounds();
	while (i.hasNext()) {
		playFieldRectangle.add(((MosaicJHierarchy)i.next()).getBounds());
	}
	return(playFieldRectangle);
} /* end getCircumbscribedRectangle */

/*------------------------------------------------------------------*/
private Stack getOverlap (
	final MosaicJThumb currentThumb
) {
	final Stack overlap = new Stack();
	final Iterator i = stackingOrder.iterator();
	while (i.hasNext()) {
		final MosaicJHierarchy thumbs = (MosaicJHierarchy)i.next();
		if (thumbs.isVisible()) {
			final Stack j = thumbs.getThumbs();
			while (!j.isEmpty()) {
				final MosaicJThumb thumb = (MosaicJThumb)j.pop();
				if (thumb == currentThumb) {
					return(overlap);
				}
				final Rectangle commonArea =
					thumb.getBounds().intersection(currentThumb.getBounds());
				if (!commonArea.isEmpty()) {
					overlap.push(commonArea);
				}
			}
		}
	}
	return(overlap);
} /* end getOverlap */

/*------------------------------------------------------------------*/
private Point magnifyViewport (
) {
	if (stackingOrder.isEmpty()) {
		return(null);
	}
	final Rectangle playFieldRectangle =
		getCircumbscribedRectangle(stackingOrder);
	final JViewport viewport = workScrollPane.getViewport();
	final Point o = viewport.getViewPosition();
	int dx = 0;
	int dy = 0;
	if (playFieldRectangle.width < minWidth) {
		o.x = 0;
		dx = (minWidth - playFieldRectangle.width) / 2 - playFieldRectangle.x;
		playFieldSize.width = minWidth;
	}
	else {
		o.x *= 2;
		playFieldSize.width = playFieldRectangle.width;
		dx = -playFieldRectangle.x;
	}
	if (playFieldRectangle.height < minHeight) {
		o.y = 0;
		dy = (minHeight - playFieldRectangle.height) / 2 - playFieldRectangle.y;
		playFieldSize.height = minHeight;
	}
	else {
		o.y *= 2;
		playFieldSize.height = playFieldRectangle.height;
		dy = -playFieldRectangle.y;
	}
	viewport.setViewPosition(o);
	return(new Point(dx, dy));
} /* end magnifyViewport */

/*------------------------------------------------------------------*/
private Point minifyViewport (
) {
	if (stackingOrder.isEmpty()) {
		return(null);
	}
	final Rectangle playFieldRectangle =
		getCircumbscribedRectangle(stackingOrder);
	final JViewport viewport = workScrollPane.getViewport();
	final Point o = viewport.getViewPosition();
	int dx = 0;
	int dy = 0;
	if (playFieldRectangle.width < minWidth) {
		o.x = 0;
		dx = (minWidth - playFieldRectangle.width) / 2 - playFieldRectangle.x;
		playFieldSize.width = minWidth;
	}
	else {
		o.x /= 2;
		playFieldSize.width = playFieldRectangle.width;
	}
	if (playFieldRectangle.height < minHeight) {
		o.y = 0;
		dy = (minHeight - playFieldRectangle.height) / 2 - playFieldRectangle.y;
		playFieldSize.height = minHeight;
	}
	else {
		o.y /= 2;
		playFieldSize.height = playFieldRectangle.height;
	}
	viewport.setViewPosition(o);
	return(new Point(dx, dy));
} /* end minifyViewport */

/*------------------------------------------------------------------*/
private void select (
	final MosaicJHierarchy thumbs
) {
	if (selectedThumbs.contains(thumbs)) {
		return;
	}
	selectedThumbs.push(thumbs);
	thumbs.setSelected(true);
	final Rectangle bounds = thumbs.getBounds();
	repaint(bounds.x, bounds.y, bounds.width, bounds.height);
} /* end select */

} /* end class MosaicJPlayField */

/*====================================================================
|	MosaicJStatus
\===================================================================*/
class MosaicJStatus
	extends
		JDialog
	implements
		ActionListener

{ /* begin class MosaicJStatus */

/*....................................................................
	private variables
....................................................................*/

private MosaicJ_ mosaicJ = null;
private boolean rescuedQuickAndDirtyScaling = false;
private int reductionFactor = 1;

/*....................................................................
	ActionListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void actionPerformed (
	final ActionEvent e
) {
	if (e.getActionCommand().equals("Done")) {
		if (mosaicJ.getQuickAndDirtyScaling() != rescuedQuickAndDirtyScaling) {
			mosaicJ.updateScale(reductionFactor);
		}
		dispose();
	}
} /* end actionPerformed */

/*....................................................................
	Constructors
....................................................................*/

/*------------------------------------------------------------------*/
public MosaicJStatus (
	final MosaicJ_ mosaicJ,
	final boolean smartDecolorization,
	final boolean quickAndDirtyScaling,
	final int reductionFactor,
	final boolean hidden,
	final boolean exportEnabled,
	final boolean blend,
	final boolean rotate,
	final boolean createOutputLog
) {
	super(mosaicJ, "MosaicJ Status", true);
	this.mosaicJ = mosaicJ;
	this.rescuedQuickAndDirtyScaling = quickAndDirtyScaling;
	this.reductionFactor = reductionFactor;
	getContentPane().setLayout(new BorderLayout(0, 20));
	final JTabbedPane tabbedPane = new JTabbedPane();
	tabbedPane.addTab("Import", new ImportPanel(mosaicJ,
		smartDecolorization));
	tabbedPane.addTab("Display", new DisplayPanel(mosaicJ,
		quickAndDirtyScaling, reductionFactor, hidden));
	tabbedPane.addTab("Export", new ExportPanel(mosaicJ,
		blend, rotate, createOutputLog));
	tabbedPane.setEnabledAt(2, exportEnabled);
	final JPanel buttonPanel = new JPanel();
	buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	final JButton doneButton = new JButton("Done");
	doneButton.addActionListener(this);
	buttonPanel.add(doneButton);
	getContentPane().add("Center", tabbedPane);
	getContentPane().add("South", buttonPanel);
	pack();
	GUI.center(this);
	setResizable(false);
	setVisible(true);
} /* end MosaicJStatus */

/*....................................................................
	Subclasses
....................................................................*/

/*==================================================================*/
class DisplayPanel
	extends
		JPanel
	implements
		ItemListener

{ /* begin subclass DisplayPanel */

/*....................................................................
	private variables
....................................................................*/

private MosaicJ_ mosaicJ = null;
private final JCheckBox quickAndDirty = new JCheckBox(
	"Quick&Dirty Rescaling Method");

/*....................................................................
	ItemListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void itemStateChanged (
	ItemEvent e
) {
	if (e.getItem() == quickAndDirty) {
		mosaicJ.setQuickAndDirtyScaling(quickAndDirty.isSelected());
	}
} /* end itemStateChanged */

/*....................................................................
	Constructors
....................................................................*/

/*------------------------------------------------------------------*/
private DisplayPanel (
	final MosaicJ_ mosaicJ,
	final boolean quickAndDirtyScaling,
	final int reductionFactor,
	final boolean hidden
) {
	this.mosaicJ = mosaicJ;
	setLayout(new GridLayout(0, 1));
	quickAndDirty.setSelected(quickAndDirtyScaling);
	quickAndDirty.addItemListener(this);
	add(quickAndDirty);
	add(new JLabel());
	final JLabel magnification = new JLabel(
		"Current Reduction Factor: " + mosaicJ.getMagnificationAsString());
	add(magnification);
	final JLabel hiddenTiles = new JLabel((hidden)
		? ("Some Tiles Are Hidden (Reveal with [Object\u2192Show All])")
		: ("There Are No Hidden Tiles"));
	add(hiddenTiles);
} /* end DisplayPanel */

} /* end subclass DisplayPanel */

/*==================================================================*/
class ExportPanel
	extends
		JPanel
	implements
		ItemListener

{ /* begin subclass ExportPanel */

/*....................................................................
	private variables
....................................................................*/

private MosaicJ_ mosaicJ = null;
private final JCheckBox blending = new JCheckBox(
	"Seamless Blending of Tiles");
private final JCheckBox outputLog = new JCheckBox(
	"Create a Log File (Can Rebuild the Mosaic)");
private final JCheckBox rotation = new JCheckBox(
	"Compensate for Rotation of Tiles");

/*....................................................................
	ItemListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void itemStateChanged (
	ItemEvent e
) {
	if (e.getItem() == blending) {
		mosaicJ.setBlend(blending.isSelected());
	}
	if (e.getItem() == outputLog) {
		mosaicJ.setCreateOutputLog(outputLog.isSelected());
	}
	if (e.getItem() == rotation) {
		mosaicJ.setRotate(rotation.isSelected());
	}
} /* end itemStateChanged */

/*....................................................................
	Constructors
....................................................................*/

/*------------------------------------------------------------------*/
private ExportPanel (
	final MosaicJ_ mosaicJ,
	final boolean blend,
	final boolean rotate,
	final boolean createOutputLog
) {
	this.mosaicJ = mosaicJ;
	setLayout(new GridLayout(0, 1));
	blending.setSelected(blend);
	blending.addItemListener(this);
	add(blending);
	rotation.setSelected(rotate);
	rotation.addItemListener(this);
	add(rotation);
	outputLog.setSelected(createOutputLog);
	outputLog.addItemListener(this);
	add(outputLog);
} /* end ExportPanel */

} /* end subclass ExportPanel */

/*==================================================================*/
class ImportPanel
	extends
		JPanel
	implements
		ItemListener

{ /* begin subclass ImportPanel */

/*....................................................................
	private variables
....................................................................*/

private MosaicJ_ mosaicJ = null;
private final JCheckBox decolorization = new JCheckBox(
	"Smart Color Conversion");

/*....................................................................
	ItemListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void itemStateChanged (
	ItemEvent e
) {
	if (e.getItem() == decolorization) {
		mosaicJ.setSmartDecolorization(decolorization.isSelected());
	}
} /* end itemStateChanged */

/*....................................................................
	Constructors
....................................................................*/

/*------------------------------------------------------------------*/
private ImportPanel (
	final MosaicJ_ mosaicJ,
	final boolean smartDecolorization
) {
	this.mosaicJ = mosaicJ;
	setLayout(new GridLayout(0, 1));
	decolorization.setSelected(smartDecolorization);
	decolorization.addItemListener(this);
	add(decolorization);
} /* end ImportPanel */

} /* end subclass ImportPanel */

} /* end class MosaicJStatus */

/*====================================================================
|	MosaicJThumb
\===================================================================*/
class MosaicJThumb
	extends
		JComponent
	implements
		MouseListener

{ /* begin class MosaicJThumb */

/*....................................................................
	public variables
....................................................................*/

public static final double TINY = (double)Float.intBitsToFloat((int)0x33FFFFFF);
public static final int THUMBNAIL_HEIGHT = 158;
public static final int INTERSECTION_MARGIN = 5;

/*....................................................................
	private variables
....................................................................*/

private BufferedImage bufferedImage = null;
private String filePath = "";
private boolean frozen = false;
private boolean hasGlobalTransform = false;
private boolean isColored = false;
private boolean isThumb = true;
private double[] colorWeight = null;
private double grayContrast = 1.0;
private double grayOffset = 0.0;
private final Point trueLocation = new Point(0, 0);
private final double[][] globalTransform = new double[3][3];
private float[] pixels = null;
private int height = 0;
private int id = 0;
private int slice = 1;
private int slices = 1;
private int trueHeight = 0;
private int trueWidth = 0;
private int width = 0;
private static JPanel thumbnailArea = null;
private static JScrollPane thumbnailScrollPane = null;
private static MosaicJPlayField playField = null;
private static double min = 1.0;
private static double max = -1.0;
private static final int SCREEN_WIDTH = Toolkit.getDefaultToolkit(
	).getScreenSize().width;
private static int thumbWidth = 0;

/*....................................................................
	Constructors
....................................................................*/

/*------------------------------------------------------------------*/
public MosaicJThumb (
	final int id,
	final ImagePlus imp,
	final boolean isColored,
	final boolean smartDecolorization,
	final int reductionFactor,
	final JPanel thumbnailArea,
	final JScrollPane thumbnailScrollPane,
	final MosaicJPlayField playField,
	final boolean quickAndDirtyScaling,
	final double grayContrast,
	final double grayOffset,
	final double[] colorWeight,
	final boolean frozen,
	final String filePath,
	final int slice,
	final int slices
) {
	this.id = id;
	this.isColored = isColored;
	this.thumbnailArea = thumbnailArea;
	this.thumbnailScrollPane = thumbnailScrollPane;
	this.playField = playField;
	if (colorWeight != null) {
		this.grayContrast = grayContrast;
		this.grayOffset = grayOffset;
		this.colorWeight = colorWeight;
	}
	this.frozen = frozen;
	this.filePath = filePath;
	this.slice = slice;
	this.slices = slices;
	final FloatProcessor fp = getGray32Processor(
		imp, slice, smartDecolorization);
	if (fp == null) {
		return;
	}
	if (1 < slices) {
		setToolTipText("Slice " + slice + " of " + slices + ": " + filePath);
	}
	else {
		setToolTipText(filePath);
	}
	if (quickAndDirtyScaling) {
		downsampleThumb(fp, reductionFactor);
	}
	else {
		reduceThumb(fp, reductionFactor);
	}
	setPreferredSize(new Dimension(width, THUMBNAIL_HEIGHT));
	createBufferedImage();
	setBorder(null);
	addMouseListener(this);
	thumbnailArea.add(this);
	incrementScrollWidth(width);
	thumbnailArea.repaint();
} /* end MosaicJThumb */

/*------------------------------------------------------------------*/
public MosaicJThumb (
	final int id,
	final BufferedImage bufferedImage,
	final boolean isColored,
	final float[] pixels,
	final int height,
	final int width,
	final double grayContrast,
	final double grayOffset,
	final double[] colorWeight,
	final boolean frozen,
	final String filePath,
	final int slice,
	final int slices
) {
	this.id = id;
	this.bufferedImage = bufferedImage;
	this.isColored = isColored;
	this.pixels = pixels;
	this.height = height;
	this.width = width;
	this.grayContrast = grayContrast;
	this.grayOffset = grayOffset;
	this.colorWeight = colorWeight;
	this.frozen = frozen;
	this.filePath = filePath;
	this.slice = slice;
	this.slices = slices;
	if (1 < slices) {
		setToolTipText("Slice " + slice + " of " + slices + ": " + filePath);
	}
	else {
		setToolTipText(filePath);
	}
	setPreferredSize(new Dimension(width, THUMBNAIL_HEIGHT));
	setBorder(null);
	addMouseListener(this);
	thumbnailArea.add(this);
	incrementScrollWidth(width);
	thumbnailArea.repaint();
} /* end MosaicJThumb */

/*------------------------------------------------------------------*/
public MosaicJThumb (
	final int id,
	final String filePath,
	final int slice
) {
	this.id = id;
	this.filePath = filePath;
	this.slice = slice;
	final ImagePlus imp = new ImagePlus(filePath);
	if ((imp == null) || (!imp.isProcessor())
		|| (imp.getStackSize() < Math.abs(slice))) {
		return;
	}
	isColored = false;
	if (imp.getStack().isHSB()) {
		isColored = true;
		new ImageConverter(imp).convertHSBToRGB();
	}
	if (imp.getStack().isRGB()) {
		isColored = true;
		new ImageConverter(imp).convertRGBStackToRGB();
	}
	final FloatProcessor fp = getGray32Processor(imp, slice, false);
} /* end MosaicJThumb */

/*....................................................................
	JComponent methods
....................................................................*/

/*------------------------------------------------------------------*/
public boolean contains (
	final int x,
	final int y
) {
	if (isThumb) {
		return((new Rectangle(getPreferredSize())).contains(x, y));
	}
	else {
		return(getBounds().contains(x, y));
	}
} /* end contains */

/*------------------------------------------------------------------*/
public double getGrayContrast (
) {
	return(grayContrast);
} /* end getGrayContrast */

/*------------------------------------------------------------------*/
public double getGrayOffset (
) {
	return(grayOffset);
} /* end getGrayOffset */

/*------------------------------------------------------------------*/
public int getHeight (
) {
	return(height);
} /* end getHeight */

/*------------------------------------------------------------------*/
public int getWidth (
) {
	return(width);
} /* end getWidth */

/*------------------------------------------------------------------*/
public boolean isColored (
) {
	return(isColored);
} /* end isColored */

/*------------------------------------------------------------------*/
public boolean isFrozen (
) {
	return(frozen);
} /* end isFrozen */

/*------------------------------------------------------------------*/
public void paint (
	final Graphics g
) {
	final Graphics2D g2D = (Graphics2D)g;
	if (isThumb) {
		g2D.drawImage(bufferedImage, null, 0, 0);
	}
	else {
		g2D.drawImage(bufferedImage, null, getLocation().x, getLocation().y);
	}
} /* end paint */

/*....................................................................
	MouseListener methods
....................................................................*/

/*------------------------------------------------------------------*/
public void mouseClicked (
	final MouseEvent e
) {
	unstow();
} /* end mouseClicked */

/*------------------------------------------------------------------*/
public void mouseEntered (
	final MouseEvent e
) {
	setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
} /* end mouseEntered */

/*------------------------------------------------------------------*/
public void mouseExited (
	final MouseEvent e
) {
	setCursor(Cursor.getDefaultCursor());
} /* end mouseExited */

/*------------------------------------------------------------------*/
public void mousePressed (
	final MouseEvent e
) {
} /* end mousePressed */

/*------------------------------------------------------------------*/
public void mouseReleased (
	final MouseEvent e
) {
} /* end mouseReleased */

/*....................................................................
	Object methods
....................................................................*/

/*------------------------------------------------------------------*/
public String toString (
) {
	return("MosaicJ" + id);
} /* end toString */

/*....................................................................
	public methods
....................................................................*/

/*------------------------------------------------------------------*/
public double[] getColorWeights (
) {
	if (colorWeight == null) {
		final double[] colorWeight = {1.0 / 3.0, 1.0 / 3.0, 1.0 / 3.0};
		return(colorWeight);
	}
	else {
		return(colorWeight);
	}
} /* end getColorWeights */

/*------------------------------------------------------------------*/
public String getFilePath (
) {
	return(filePath);
} /* end getFilePath */

/*------------------------------------------------------------------*/
public double[][] getGlobalTransform (
) {
	return(globalTransform);
} /* end getGlobalTransform */

/*------------------------------------------------------------------*/
public static double getMax (
) {
	return(max);
} /* end getMax */

/*------------------------------------------------------------------*/
public static double getMin (
) {
	return(min);
} /* end getMin */

/*------------------------------------------------------------------*/
public int getSlice (
) {
	return(slice);
} /* end getSlice */

/*------------------------------------------------------------------*/
public Rectangle getTrueBounds (
) {
	return(new Rectangle(trueLocation, new Dimension(trueWidth, trueHeight)));
} /* end getTrueBounds */

/*------------------------------------------------------------------*/
public Point getTrueLocation (
) {
	return(trueLocation);
} /* end getTrueLocation */

/*------------------------------------------------------------------*/
public boolean hasGlobalTransform (
) {
	return(hasGlobalTransform);
} /* end hasGlobalTransform */

/*------------------------------------------------------------------*/
public void incrementScrollWidth (
	final int width
) {
	thumbnailArea.validate();
	final Insets insets = getInsets();
	thumbWidth += insets.left + width + insets.right;
	thumbnailArea.setSize(new Dimension(Math.max(SCREEN_WIDTH, thumbWidth),
		THUMBNAIL_HEIGHT));
	thumbnailScrollPane.setViewportView(thumbnailArea);
} /* end incrementScrollWidth */

/*------------------------------------------------------------------*/
public boolean isStowed (
) {
	return(isThumb);
} /* end isStowed */

/*------------------------------------------------------------------*/
public void setContrastAndOffset (
	final double grayContrast,
	final double grayOffset
) {
	this.grayContrast = grayContrast;
	this.grayOffset = grayOffset;
	createBufferedImage();
} /* end setContrastAndOffset */

/*------------------------------------------------------------------*/
public void setFrozen (
	final boolean frozen
) {
	this.frozen = frozen;
} /* end setFrozen */

/*------------------------------------------------------------------*/
public void setGlobalTransform (
	final double[][] globalTransform
) {
	for (int i = 0; (i < 3); i++) {
		for (int j = 0; (j < 3); j++) {
			this.globalTransform[i][j] = globalTransform[i][j];
		}
	}
	hasGlobalTransform = true;
} /* end setGlobalTransform */

/*------------------------------------------------------------------*/
public static void setMinAndMaxGrays (
	final double minGray,
	final double maxGray
) {
	min = minGray;
	max = maxGray;
} /* end setMinAndMaxGrays */

/*------------------------------------------------------------------*/
public void setOpacity (
	final Rectangle seeThrough,
	final int opacity
) {
	final int[] submask = new int[seeThrough.width * seeThrough.height];
	for (int k = 0, K = submask.length; (k < K); k++) {
		submask[k] = opacity;
	}
	bufferedImage.getAlphaRaster().setPixels(
		seeThrough.x - getLocation().x, seeThrough.y - getLocation().y,
		seeThrough.width, seeThrough.height, submask);
} /* end setOpacity */

/*------------------------------------------------------------------*/
public void setTrueLocation (
	final Point trueLocation
) {
	this.trueLocation.x = trueLocation.x;
	this.trueLocation.y = trueLocation.y;
} /* end setTrueLocation */

/*------------------------------------------------------------------*/
public void stow (
) {
	thumbnailArea.add(new MosaicJThumb(id, bufferedImage, isColored,
		pixels, height, width, grayContrast, grayOffset, colorWeight,
		frozen, filePath, slice, slices));
} /* end stow */

/*------------------------------------------------------------------*/
public void translateTrueLocation (
	final int dx,
	final int dy
) {
	trueLocation.x += dx;
	trueLocation.y += dy;
} /* end translateTrueLocation */

/*------------------------------------------------------------------*/
public void unstow (
) {
	isThumb = false;
	thumbnailArea.remove(this);
	incrementScrollWidth(-width);
	setPreferredSize(new Dimension(width, height));
	createBufferedImage();
	playField.add("" + id, this);
	removeMouseListener(this);
//@setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	setEnabled(false);
} /* end unstow */

/*------------------------------------------------------------------*/
public void updateMinAndMax (
) {
	if (max <= min) {
		min = (double)pixels[0];
		max = (double)pixels[0];
	}
	for (int k = 0, K = pixels.length; (k < K); k++) {
		min = (min < (double)pixels[k]) ? (min) : ((double)pixels[k]);
		max = ((double)pixels[k] < max) ? (max) : ((double)pixels[k]);
	}
} /* end updateMinAndMax */

/*------------------------------------------------------------------*/
public void updateScale (
	final int reductionFactor,
	final boolean quickAndDirtyScaling
) {
	final ImagePlus imp = new ImagePlus(
		IJ.getDirectory("temp") + "MosaicJ" + id);
	if (isThumb) {
		incrementScrollWidth(-width);
		if (quickAndDirtyScaling) {
			downsampleThumb(
				(FloatProcessor)imp.getProcessor(), reductionFactor);
		}
		else {
			reduceThumb((FloatProcessor)imp.getProcessor(), reductionFactor);
		}
		setPreferredSize(new Dimension(width, THUMBNAIL_HEIGHT));
		createBufferedImage();
		incrementScrollWidth(width);
	}
	else {
		if (quickAndDirtyScaling) {
			downsampleThumb(
				(FloatProcessor)imp.getProcessor(), reductionFactor);
		}
		else {
			reduceThumb((FloatProcessor)imp.getProcessor(), reductionFactor);
		}
		setPreferredSize(new Dimension(width, height));
		createBufferedImage();
	}
} /* end updateScale */

/*....................................................................
	private methods
....................................................................*/

/*------------------------------------------------------------------*/
private void QRdecomposition (
	final double[][] Q,
	final double[][] R
) {
	final int lines = Q.length;
	final int columns = Q[0].length;
	final double[][] A = new double[lines][columns];
	double s;
	for (int j = 0; (j < columns); j++) {
		for (int i = 0; (i < lines); i++) {
			A[i][j] = Q[i][j];
		}
		for (int k = 0; (k < j); k++) {
			s = 0.0;
			for (int i = 0; (i < lines); i++) {
				s += A[i][j] * Q[i][k];
			}
			for (int i = 0; (i < lines); i++) {
				Q[i][j] -= s * Q[i][k];
			}
		}
		s = 0.0;
		for (int i = 0; (i < lines); i++) {
			s += Q[i][j] * Q[i][j];
		}
		if ((s * s) == 0.0) {
			s = 0.0;
		}
		else {
			s = 1.0 / Math.sqrt(s);
		}
		for (int i = 0; (i < lines); i++) {
			Q[i][j] *= s;
		}
	}
	for (int i = 0; (i < columns); i++) {
		for (int j = 0; (j < i); j++) {
			R[i][j] = 0.0;
		}
		for (int j = i; (j < columns); j++) {
			R[i][j] = 0.0;
			for (int k = 0; (k < lines); k++) {
				R[i][j] += Q[k][i] * A[k][j];
			}
		}
	}
} /* end QRdecomposition */

/*------------------------------------------------------------------*/
private void basicToCardinal2D (
	final float[] basic,
	final float[] cardinal,
	final int width,
	final int height,
	final int degree
) {
	final double[] hLine = new double[width];
	final double[] vLine = new double[height];
	final double[] hData = new double[width];
	final double[] vData = new double[height];
	double[] h = null;
	switch (degree) {
		case 3: {
			h = new double[2];
			h[0] = 2.0 / 3.0;
			h[1] = 1.0 / 6.0;
			break;
		}
		case 7: {
			h = new double[4];
			h[0] = 151.0 / 315.0;
			h[1] = 397.0 / 1680.0;
			h[2] = 1.0 / 42.0;
			h[3] = 1.0 / 5040.0;
			break;
		}
		default: {
			h = new double[1];
			h[0] = 1.0;
		}
	}
	for (int y = 0; (y < height); y++) {
		extractRow(basic, y, hLine);
		symmetricFirMirrorOffBounds1D(h, hLine, hData);
		putRow(cardinal, y, hData);
	}
	for (int x = 0; (x < width); x++) {
		extractColumn(cardinal, width, x, vLine);
		symmetricFirMirrorOffBounds1D(h, vLine, vData);
		putColumn(cardinal, width, x, vData);
	}
} /* end basicToCardinal2D */

/*------------------------------------------------------------------*/
private void cardinalToDual2D (
	final float[] cardinal,
	final float[] dual,
	final int width,
	final int height,
	final int degree
) {
	basicToCardinal2D(getBasicFromCardinal2D(cardinal, width, height, degree),
		dual, width, height, 2 * degree + 1);
} /* end cardinalToDual2D */

/*------------------------------------------------------------------*/
private void computeStatistics (
	final ImageProcessor ip,
	final double[] average,
	final double[][] scatterMatrix
) {
	final int length = ip.getWidth() * ip.getHeight();
	double r;
	double g;
	double b;
	if (ip.getPixels() instanceof byte[]) {
		final byte[] pixels = (byte[])ip.getPixels();
		final IndexColorModel icm = (IndexColorModel)ip.getColorModel();
		final int mapSize = icm.getMapSize();
		final byte[] reds = new byte[mapSize];
		final byte[] greens = new byte[mapSize];
		final byte[] blues = new byte[mapSize];	
		icm.getReds(reds);
		icm.getGreens(greens);
		icm.getBlues(blues);
		final double[] histogram = new double[mapSize];
		for (int k = 0; (k < mapSize); k++) {
			histogram[k] = 0.0;
		}
		for (int k = 0; (k < length); k++) {
			histogram[pixels[k] & 0xFF]++;
		}
		for (int k = 0; (k < mapSize); k++) {
			r = (double)(reds[k] & 0xFF);
			g = (double)(greens[k] & 0xFF);
			b = (double)(blues[k] & 0xFF);
			average[0] += histogram[k] * r;
			average[1] += histogram[k] * g;
			average[2] += histogram[k] * b;
			scatterMatrix[0][0] += histogram[k] * r * r;
			scatterMatrix[0][1] += histogram[k] * r * g;
			scatterMatrix[0][2] += histogram[k] * r * b;
			scatterMatrix[1][1] += histogram[k] * g * g;
			scatterMatrix[1][2] += histogram[k] * g * b;
			scatterMatrix[2][2] += histogram[k] * b * b;
		}
	}
	else if (ip.getPixels() instanceof int[]) {
		final int[] pixels = (int[])ip.getPixels();
		for (int k = 0; (k < length); k++) {
			r = (double)((pixels[k] & 0x00FF0000) >>> 16);
			g = (double)((pixels[k] & 0x0000FF00) >>> 8);
			b = (double)(pixels[k] & 0x000000FF);
			average[0] += r;
			average[1] += g;
			average[2] += b;
			scatterMatrix[0][0] += r * r;
			scatterMatrix[0][1] += r * g;
			scatterMatrix[0][2] += r * b;
			scatterMatrix[1][1] += g * g;
			scatterMatrix[1][2] += g * b;
			scatterMatrix[2][2] += b * b;
		}
	}
	average[0] /= (double)length;
	average[1] /= (double)length;
	average[2] /= (double)length;
	scatterMatrix[0][0] /= (double)length;
	scatterMatrix[0][1] /= (double)length;
	scatterMatrix[0][2] /= (double)length;
	scatterMatrix[1][1] /= (double)length;
	scatterMatrix[1][2] /= (double)length;
	scatterMatrix[2][2] /= (double)length;
	scatterMatrix[0][0] -= average[0] * average[0];
	scatterMatrix[0][1] -= average[0] * average[1];
	scatterMatrix[0][2] -= average[0] * average[2];
	scatterMatrix[1][1] -= average[1] * average[1];
	scatterMatrix[1][2] -= average[1] * average[2];
	scatterMatrix[2][2] -= average[2] * average[2];
	scatterMatrix[2][1] = scatterMatrix[1][2];
	scatterMatrix[2][0] = scatterMatrix[0][2];
	scatterMatrix[1][0] = scatterMatrix[0][1];
} /* computeStatistics */

/*------------------------------------------------------------------*/
private void createBufferedImage (
) {
	final float[] grays = new float[pixels.length];
	if (isThumb) {
		bufferedImage = new BufferedImage(width, height,
			BufferedImage.TYPE_BYTE_GRAY);
		for (int k = 0, K = pixels.length; (k < K); k++) {
			grays[k] = (float)(grayContrast * ((double)pixels[k] + grayOffset));
			grays[k] = (grays[k] < 0.0F) ? (0.0F)
				: ((255.0F < grays[k]) ? (255.0F) : (grays[k]));
		}
		bufferedImage.getRaster().setPixels(0, 0, width, height, grays);
	}
	else {
		bufferedImage = new BufferedImage(width, height,
			BufferedImage.TYPE_INT_ARGB);
		int[] mask = new int[width * height];
		for (int k = 0, K = mask.length; (k < K); k++) {
			mask[k] = 0xFF;
			grays[k] = (float)(grayContrast * ((double)pixels[k] + grayOffset));
			grays[k] = (grays[k] < 0.0F) ? (0.0F)
				: ((255.0F < grays[k]) ? (255.0F) : (grays[k]));
		}
		bufferedImage.getRaster().setSamples(0, 0, width, height, 0, grays);
		bufferedImage.getRaster().setSamples(0, 0, width, height, 1, grays);
		bufferedImage.getRaster().setSamples(0, 0, width, height, 2, grays);
		bufferedImage.getRaster().setSamples(0, 0, width, height, 3, mask);
	}
} /* end createBufferedImage */

/*------------------------------------------------------------------*/
private void downsampleThumb (
	final FloatProcessor fp,
	final int reductionFactor
) {
	final int fullWidth = fp.getWidth();
	final int fullHeight = fp.getHeight();
	width = fullWidth / reductionFactor;
	height = fullHeight / reductionFactor;
	pixels = new float[width * height];
	final float[] allPixels = (float[])fp.getPixels();
	int n = 0;
	int j = 0;
	for (int y = 0, Y = height; (y < Y); y++) {
		int i = j;
		for (int x = 0, X = width; (x < X); x++) {
			pixels[n++] = allPixels[i];
			i += reductionFactor;
		}
		j += reductionFactor * fullWidth;
	}
} /* end downsampleThumb */

/*------------------------------------------------------------------*/
private void dualToCardinal2D (
	final float[] dual,
	final float[] cardinal,
	final int width,
	final int height,
	final int degree
) {
	basicToCardinal2D(getBasicFromCardinal2D(dual, width, height,
		2 * degree + 1), cardinal, width, height, degree);
} /* end dualToCardinal2D */

/*------------------------------------------------------------------*/
private void extractColumn (
	final float[] array,
	final int width,
	int x,
	final double[] column
) {
	for (int j = 0, J = column.length; (j < J); j++) {
		column[j] = (double)array[x];
		x += width;
	}
} /* end extractColumn */

/*------------------------------------------------------------------*/
private void extractRow (
	final float[] array,
	int y,
	final double[] row
) {
	y *= row.length;
	for (int i = 0, I = row.length; (i < I); i++) {
		row[i] = (double)array[y++];
	}
} /* end extractRow */

/*------------------------------------------------------------------*/
private float[] getBasicFromCardinal2D (
	final float[] cardinal,
	final int width,
	final int height,
	final int degree
) {
	final float[] basic = new float[width * height];
	final double[] hLine = new double[width];
	final double[] vLine = new double[height];
	for (int y = 0; (y < height); y++) {
		extractRow(cardinal, y, hLine);
		samplesToInterpolationCoefficient1D(hLine, degree, 0.0);
		putRow(basic, y, hLine);
	}
	for (int x = 0; (x < width); x++) {
		extractColumn(basic, width, x, vLine);
		samplesToInterpolationCoefficient1D(vLine, degree, 0.0);
		putColumn(basic, width, x, vLine);
	}
	return(basic);
} /* end getBasicFromCardinal2D */

/*------------------------------------------------------------------*/
private double[] getColorWeightsFromCCIR709 (
) {
	final double[] colorWeight = {0.212671, 0.71516, 0.072169};
	return(colorWeight);
} /* getColorWeightsFromCCIR709 */

/*------------------------------------------------------------------*/
private double[] getColorWeightsFromPrincipalComponents (
	final ImageProcessor ip
) {
	final double[] average = {0.0, 0.0, 0.0};
	final double[][] scatterMatrix = {
		{0.0, 0.0, 0.0},
		{0.0, 0.0, 0.0},
		{0.0, 0.0, 0.0}};
	computeStatistics(ip, average, scatterMatrix);
	double[] eigenvalue = getEigenvalues(scatterMatrix);
	if ((eigenvalue[0] * eigenvalue[0] + eigenvalue[1] * eigenvalue[1]
		+ eigenvalue[2] * eigenvalue[2]) <= TINY) {
		IJ.error("Warning: eigenvalues too small.");
		return(getColorWeightsFromCCIR709());
	}
	double bestEigenvalue = getLargestAbsoluteEigenvalue(eigenvalue);
	final double colorWeight[] = getEigenvector(scatterMatrix, bestEigenvalue);
	final double normalization =
		colorWeight[0] + colorWeight[1] + colorWeight[2];
	if (TINY < Math.abs(normalization)) {
		colorWeight[0] /= normalization;
		colorWeight[1] /= normalization;
		colorWeight[2] /= normalization;
	}
	return(colorWeight);
} /* getColorWeightsFromPrincipalComponents */

/*------------------------------------------------------------------*/
private double[] getEigenvalues (
	final double[][] scatterMatrix
) {
	final double[] a = {
		scatterMatrix[0][0] * scatterMatrix[1][1] * scatterMatrix[2][2]
			+ 2.0 * scatterMatrix[0][1] * scatterMatrix[1][2]
			* scatterMatrix[2][0]
			- scatterMatrix[0][1] * scatterMatrix[0][1] * scatterMatrix[2][2]
			- scatterMatrix[1][2] * scatterMatrix[1][2] * scatterMatrix[0][0]
			- scatterMatrix[2][0] * scatterMatrix[2][0] * scatterMatrix[1][1],
		scatterMatrix[0][1] * scatterMatrix[0][1]
			+ scatterMatrix[1][2] * scatterMatrix[1][2]
			+ scatterMatrix[2][0] * scatterMatrix[2][0]
			- scatterMatrix[0][0] * scatterMatrix[1][1]
			- scatterMatrix[1][1] * scatterMatrix[2][2]
			- scatterMatrix[2][2] * scatterMatrix[0][0],
		scatterMatrix[0][0] + scatterMatrix[1][1] + scatterMatrix[2][2],
		-1.0
	};
	double[] RealRoot = new double[3];
	double Q = (3.0 * a[1] - a[2] * a[2] / a[3]) / (9.0 * a[3]);
	double R = (a[1] * a[2] - 3.0 * a[0] * a[3]
		- (2.0 / 9.0) * a[2] * a[2] * a[2] / a[3]) / (6.0 * a[3] * a[3]);
	double Det = Q * Q * Q + R * R;
	if (Det < 0.0) {
		Det = 2.0 * Math.sqrt(-Q);
		R /= Math.sqrt(-Q * Q * Q);
		R = (1.0 / 3.0) * Math.acos(R);
		Q = (1.0 / 3.0) * a[2] / a[3];
		RealRoot[0] = Det * Math.cos(R) - Q;
		RealRoot[1] = Det * Math.cos(R + (2.0 / 3.0) * Math.PI) - Q;
		RealRoot[2] = Det * Math.cos(R + (4.0 / 3.0) * Math.PI) - Q;
		if (RealRoot[0] < RealRoot[1]) {
			if (RealRoot[2] < RealRoot[1]) {
				double Swap = RealRoot[1];
				RealRoot[1] = RealRoot[2];
				RealRoot[2] = Swap;
				if (RealRoot[1] < RealRoot[0]) {
					Swap = RealRoot[0];
					RealRoot[0] = RealRoot[1];
					RealRoot[1] = Swap;
				}
			}
		}
		else {
			double Swap = RealRoot[0];
			RealRoot[0] = RealRoot[1];
			RealRoot[1] = Swap;
			if (RealRoot[2] < RealRoot[1]) {
				Swap = RealRoot[1];
				RealRoot[1] = RealRoot[2];
				RealRoot[2] = Swap;
				if (RealRoot[1] < RealRoot[0]) {
					Swap = RealRoot[0];
					RealRoot[0] = RealRoot[1];
					RealRoot[1] = Swap;
				}
			}
		}
	}
	else if (Det == 0.0) {
		final double P = 2.0 * ((R < 0.0) ? (Math.pow(-R, 1.0 / 3.0))
			: (Math.pow(R, 1.0 / 3.0)));
		Q = (1.0 / 3.0) * a[2] / a[3];
		if (P < 0) {
			RealRoot[0] = P - Q;
			RealRoot[1] = -0.5 * P - Q;
			RealRoot[2] = RealRoot[1];
		}
		else {
			RealRoot[0] = -0.5 * P - Q;
			RealRoot[1] = RealRoot[0];
			RealRoot[2] = P - Q;
		}
	}
	else {
		IJ.error("Warning: complex eigenvalue found.",
			"Ignoring imaginary part.");
		Det = Math.sqrt(Det);
		Q = ((R + Det) < 0.0) ? (-Math.exp((1.0 / 3.0) * Math.log(-R - Det)))
			: (Math.exp((1.0 / 3.0) * Math.log(R + Det)));
		R = Q + ((R < Det) ? (-Math.exp((1.0 / 3.0) * Math.log(Det - R)))
			: (Math.exp((1.0 / 3.0) * Math.log(R - Det))));
		Q = (-1.0 / 3.0) * a[2] / a[3];
		Det = Q + R;
		RealRoot[0] = Q - R / 2.0;
		RealRoot[1] = RealRoot[0];
		RealRoot[2] = RealRoot[1];
		if (Det < RealRoot[0]) {
			RealRoot[0] = Det;
		}
		else {
			RealRoot[2] = Det;
		}
	}
	return(RealRoot);
} /* end getEigenvalues */

/*------------------------------------------------------------------*/
private double[] getEigenvector (
	final double[][] scatterMatrix,
	final double eigenvalue
) {
	final int n = scatterMatrix.length;
	final double[][] matrix = new double[n][n];
	for (int i = 0; (i < n); i++) {
		System.arraycopy(scatterMatrix[i], 0, matrix[i], 0, n);
		matrix[i][i] -= eigenvalue;
	}
	final double[] eigenvector = new double[n];
	double absMax;
	double max;
	double norm;
	for (int i = 0; (i < n); i++) {
		norm = 0.0;
		for (int j = 0; (j < n); j++) {
			norm += matrix[i][j] * matrix[i][j];
		}
		norm = Math.sqrt(norm);
		if (TINY < norm) {
			for (int j = 0; (j < n); j++) {
				matrix[i][j] /= norm;
			}
		}
	}
	for (int j = 0; (j < n); j++) {
		max = matrix[j][j];
		absMax = Math.abs(max);
		int k = j;
		for (int i = j + 1; (i < n); i++) {
			if (absMax < Math.abs(matrix[i][j])) {
				max = matrix[i][j];
				absMax = Math.abs(max);
				k = i;
			}
		}
		if (k != j) {
			final double[] partialLine = new double[n - j];
			System.arraycopy(matrix[j], j, partialLine, 0, n - j);
			System.arraycopy(matrix[k], j, matrix[j], j, n - j);
			System.arraycopy(partialLine, 0, matrix[k], j, n - j);
		}
		if (TINY < absMax) {
			for (k = 0; (k < n); k++) {
				matrix[j][k] /= max;
			}
		}
		for (int i = j + 1; (i < n); i++) {
			max = matrix[i][j];
			for (k = 0; (k < n); k++) {
				matrix[i][k] -= max * matrix[j][k];
			}
		}
	}
	final boolean[] ignore = new boolean[n];
	int valid = n;
	for (int i = 0; (i < n); i++) {
		ignore[i] = false;
		if (Math.abs(matrix[i][i]) < TINY) {
			ignore[i] = true;
			valid--;
			eigenvector[i] = 1.0;
			continue;
		}
		if (TINY < Math.abs(matrix[i][i] - 1.0)) {
			IJ.error("Insufficient accuracy.");
			eigenvector[0] = 0.212671;
			eigenvector[1] = 0.71516;
			eigenvector[2] = 0.072169;
			return(eigenvector);
		}
		norm = 0.0;
		for (int j = 0; (j < i); j++) {
			norm += matrix[i][j] * matrix[i][j];
		}
		for (int j = i + 1; (j < n); j++) {
			norm += matrix[i][j] * matrix[i][j];
		}
		if (Math.sqrt(norm) < TINY) {
			ignore[i] = true;
			valid--;
			eigenvector[i] = 0.0;
			continue;
		}
	}
	if (0 < valid) {
		double[][] reducedMatrix = new double[valid][valid];
		for (int i = 0, u = 0; (i < n); i++) {
			if (!ignore[i]) {
				for (int j = 0, v = 0; (j < n); j++) {
					if (!ignore[j]) {
						reducedMatrix[u][v] = matrix[i][j];
						v++;
					}
				}
				u++;
			}
		}
		double[] reducedEigenvector = new double[valid];
		for (int i = 0, u = 0; (i < n); i++) {
			if (!ignore[i]) {
				for (int j = 0; (j < n); j++) {
					if (ignore[j]) {
						reducedEigenvector[u] -= matrix[i][j] * eigenvector[j];
					}
				}
				u++;
			}
		}
		reducedEigenvector = linearLeastSquares(reducedMatrix,
			reducedEigenvector);
		for (int i = 0, u = 0; (i < n); i++) {
			if (!ignore[i]) {
				eigenvector[i] = reducedEigenvector[u];
				u++;
			}
		}
	}
	norm = 0.0;
	for (int i = 0; (i < n); i++) {
		norm += eigenvector[i] * eigenvector[i];
	}
	norm = Math.sqrt(norm);
	if (Math.sqrt(norm) < TINY) {
		IJ.error("Insufficient accuracy.");
		eigenvector[0] = 0.212671;
		eigenvector[1] = 0.71516;
		eigenvector[2] = 0.072169;
		return(eigenvector);
	}
	absMax = Math.abs(eigenvector[0]);
	valid = 0;
	for (int i = 1; (i < n); i++) {
		max = Math.abs(eigenvector[i]);
		if (absMax < max) {
			absMax = max;
			valid = i;
		}
	}
	norm = (eigenvector[valid] < 0.0) ? (-norm) : (norm);
	for (int i = 0; (i < n); i++) {
		eigenvector[i] /= norm;
	}
	return(eigenvector);
} /* getEigenvector */

/*------------------------------------------------------------------*/
private FloatProcessor getGray32Processor (
	final ImagePlus imp,
	final int slice,
	final boolean smartDecolorization
) {
	imp.setSlice(slice);
	FloatProcessor fp = null;
	final ImageProcessor ip = imp.getProcessor();
	switch (imp.getType()) {
		case ImagePlus.GRAY8: {
			if (ip.isColorLut()) {
				isColored = true;
				saveRGB(ip);
				if (colorWeight == null) {
					if (smartDecolorization) {
						colorWeight = getColorWeightsFromPrincipalComponents(ip);
					}
					else {
						colorWeight = getColorWeightsFromCCIR709();
					}
				}
				fp = new FloatProcessor(imp.getWidth(), imp.getHeight(),
					getGrays(ip), null);
			}
			else {
				final TypeConverter tc = new TypeConverter(ip, false);
				fp = (FloatProcessor)tc.convertToFloat(null);
			}
			break;
		}
		case ImagePlus.GRAY16: {
			final TypeConverter tc = new TypeConverter(ip, false);
			fp = (FloatProcessor)tc.convertToFloat(null);
			final double bits = Math.ceil(Math.log(fp.getMax())
				/ Math.log(2.0));
			fp.multiply(Math.pow(2.0, 8.0 - bits));
			break;
		}
		case ImagePlus.GRAY32: {
			fp = (FloatProcessor)ip;
			break;
		}
		case ImagePlus.COLOR_256:
		case ImagePlus.COLOR_RGB: {
			isColored = true;
			saveRGB(ip);
			if (colorWeight == null) {
				if (smartDecolorization) {
					colorWeight = getColorWeightsFromPrincipalComponents(ip);
				}
				else {
					colorWeight = getColorWeightsFromCCIR709();
				}
			}
			fp = new FloatProcessor(imp.getWidth(), imp.getHeight(),
				getGrays(ip), null);
			break;
		}
		default: {
			IJ.error("Unknown type.");
			return(null);
		}
	}
	trueWidth = fp.getWidth();
	trueHeight = fp.getHeight();
	final FloatProcessor mask = new FloatProcessor(trueWidth, trueHeight);
	final float[] distanceTransform = (float[])mask.getPixels();
	float distance = 0.5F;
	for (int d = 0, D = (Math.min(trueWidth, trueHeight) + 1) / 2;
		(d < D); d++) {
		for (int x = d, X = trueWidth - d; (x < X); x++) {
			distanceTransform[d * trueWidth + x] = distance;
			distanceTransform[(trueHeight - d - 1) * trueWidth + x] = distance;
		}
		for (int y = d + 1, Y = trueHeight - d - 1; (y < Y); y++) {
			distanceTransform[y * trueWidth + d] = distance;
			distanceTransform[y * trueWidth + (trueWidth - d - 1)] = distance;
		}
		distance += 1.0F;
	}
	final ImagePlus cache = new ImagePlus("MosaicJ" + id, fp);
	final ImageStack is = cache.getStack();
	is.addSlice("", mask);
	cache.setStack("", is);
	new FileSaver(cache).saveAsTiffStack(
		IJ.getDirectory("temp") + "MosaicJ" + id);
	return(fp);
} /* end getGray32Processor */

/*------------------------------------------------------------------*/
private float[] getGrays (
	final ImageProcessor ip
) {
	final int length = ip.getWidth() * ip.getHeight();
	final float[] gray = new float[length];
	double r;
	double g;
	double b;
	if (ip.getPixels() instanceof byte[]) {
		final byte[] pixels = (byte[])ip.getPixels();
		final IndexColorModel icm = (IndexColorModel)ip.getColorModel();
		final int mapSize = icm.getMapSize();
		final byte[] reds = new byte[mapSize];
		final byte[] greens = new byte[mapSize];
		final byte[] blues = new byte[mapSize];	
		icm.getReds(reds);
		icm.getGreens(greens);
		icm.getBlues(blues);
		int index;
		for (int k = 0; (k < length); k++) {
			index = (int)(pixels[k] & 0xFF);
			r = (double)(reds[index] & 0xFF);
			g = (double)(greens[index] & 0xFF);
			b = (double)(blues[index] & 0xFF);
			gray[k] = (float)(
				colorWeight[0] * r + colorWeight[1] * g + colorWeight[2] * b);
		}
	}
	else if (ip.getPixels() instanceof int[]) {
		final int[] pixels = (int[])ip.getPixels();
		for (int k = 0; (k < length); k++) {
			r = (double)((pixels[k] & 0x00FF0000) >>> 16);
			g = (double)((pixels[k] & 0x0000FF00) >>> 8);
			b = (double)(pixels[k] & 0x000000FF);
			gray[k] = (float)(
				colorWeight[0] * r + colorWeight[1] * g + colorWeight[2] * b);
		}
	}
	return(gray);
} /* getGrays */

/*------------------------------------------------------------------*/
private float[] getHalfDual2D (
	final float[] fullDual,
	final int fullWidth,
	final int fullHeight
) {
	final int halfWidth = fullWidth / 2;
	final int halfHeight = fullHeight / 2;
	final double[] hLine = new double[fullWidth];
	final double[] hData = new double[halfWidth];
	final double[] vLine = new double[fullHeight];
	final double[] vData = new double[halfHeight];
	final float[] demiDual = new float[halfWidth * fullHeight];
	final float[] halfDual = new float[halfWidth * halfHeight];
	for (int y = 0; (y < fullHeight); y++) {
		extractRow(fullDual, y, hLine);
		reduceDual1D(hLine, hData);
		putRow(demiDual, y, hData);
	}
	for (int x = 0; (x < halfWidth); x++) {
		extractColumn(demiDual, halfWidth, x, vLine);
		reduceDual1D(vLine, vData);
		putColumn(halfDual, halfWidth, x, vData);
	}
	return(halfDual);
} /* end getHalfDual2D */

/*------------------------------------------------------------------*/
private double getInitialAntiCausalCoefficientMirrorOffBounds (
	final double[] c,
	final double z,
	final double tolerance
) {
	return(z * c[c.length - 1] / (z - 1.0));
} /* end getInitialAntiCausalCoefficientMirrorOffBounds */

/*------------------------------------------------------------------*/
private double getInitialCausalCoefficientMirrorOffBounds (
	final double[] c,
	final double z,
	final double tolerance
) {
	double z1 = z;
	double zn = Math.pow(z, c.length);
	double sum = (1.0 + z) * (c[0] + zn * c[c.length - 1]);
	int horizon = c.length;
	if (0.0 < tolerance) {
		horizon = 2 + (int)(Math.log(tolerance) / Math.log(Math.abs(z)));
		horizon = (horizon < c.length) ? (horizon) : (c.length);
	}
	zn = zn * zn;
	for (int n = 1, N = horizon - 1; (n < N); n++) {
		z1 = z1 * z;
		zn = zn / z;
		sum = sum + (z1 + zn) * c[n];
	}
	return(sum / (1.0 - Math.pow(z, 2 * c.length)));
} /* end getInitialCausalCoefficientMirrorOffBounds */

/*------------------------------------------------------------------*/
private double getLargestAbsoluteEigenvalue (
	final double[] eigenvalue
) {
	double best = eigenvalue[0];
	for (int k = 1, K = eigenvalue.length; (k < K); k++) {
		if (Math.abs(best) < Math.abs(eigenvalue[k])) {
			best = eigenvalue[k];
		}
		if (Math.abs(best) == Math.abs(eigenvalue[k])) {
			if (best < eigenvalue[k]) {
				best = eigenvalue[k];
			}
		}
	}
	return(best);
} /* getLargestAbsoluteEigenvalue */

/*------------------------------------------------------------------*/
private double[] linearLeastSquares (
	final double[][] A,
	final double[] b
) {
	final int lines = A.length;
	final int columns = A[0].length;
	final double[][] Q = new double[lines][columns];
	final double[][] R = new double[columns][columns];
	final double[] x = new double[columns];
	double s;
	for (int i = 0; (i < lines); i++) {
		for (int j = 0; (j < columns); j++) {
			Q[i][j] = A[i][j];
		}
	}
	QRdecomposition(Q, R);
	for (int i = 0; (i < columns); i++) {
		s = 0.0;
		for (int j = 0; (j < lines); j++) {
			s += Q[j][i] * b[j];
		}
		x[i] = s;
	}
	for (int i = columns - 1; (0 <= i); i--) {
		s = R[i][i];
		if ((s * s) == 0.0) {
			x[i] = 0.0;
		}
		else {
			x[i] /= s;
		}
		for (int j = i - 1; (0 <= j); j--) {
			x[j] -= R[j][i] * x[i];
		}
	}
	return(x);
} /* end linearLeastSquares */

/*------------------------------------------------------------------*/
private void putColumn (
	final float[] array,
	final int width,
	int x,
	final double[] column
) {
	for (int j = 0, J = column.length; (j < J); j++) {
		array[x] = (float)column[j];
		x += width;
	}
} /* end putColumn */

/*------------------------------------------------------------------*/
private void putRow (
	final float[] array,
	int y,
	final double[] row
) {
	y *= row.length;
	for (int i = 0, I = row.length; (i < I); i++) {
		array[y++] = (float)row[i];
	}
} /* end putRow */

/*------------------------------------------------------------------*/
private void reduceThumb (
	final FloatProcessor fp,
	final int reductionFactor
) {
	width = fp.getWidth();
	height = fp.getHeight();
	pixels = (float[])fp.getPixels();
	if (1 < reductionFactor) {
		float[] fullDual = new float[width * height];
		cardinalToDual2D(pixels, fullDual, width, height, 3);
		float[] dual = fullDual;
		for (int depth = 1; (depth < reductionFactor); depth *= 2) {
			final int fullWidth = width;
			final int fullHeight = height;
			width /= 2;
			height /= 2;
			if (width * height == 0) {
				width = fullWidth;
				height = fullHeight;
				break;
			}
			dual = getHalfDual2D(fullDual, fullWidth, fullHeight);
			fullDual = dual;
		}
		pixels = new float[width * height];
		dualToCardinal2D(dual, pixels, width, height, 3);
	}
} /* end reduceThumb */

/*------------------------------------------------------------------*/
private void reduceDual1D (
	final double[] c,
	final double[] s
) {
	final double h[] = {6.0 / 16.0, 4.0 / 16.0, 1.0 / 16.0};
	if (2 <= s.length) {
		s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]) + h[2] * (c[1] + c[2]);
		for (int i = 2, j = 1, J = s.length - 1; (j < J); i += 2, j++) {
			s[j] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1])
				+ h[2] * (c[i - 2] + c[i + 2]);
		}
		if (c.length == (2 * s.length)) {
			s[s.length - 1] = h[0] * c[c.length - 2]
				+ h[1] * (c[c.length - 3] + c[c.length - 1])
				+ h[2] * (c[c.length - 4] + c[c.length - 1]);
		}
		else {
			s[s.length - 1] = h[0] * c[c.length - 3]
				+ h[1] * (c[c.length - 4] + c[c.length - 2])
				+ h[2] * (c[c.length - 5] + c[c.length - 1]);
		}
	}
	else {
		switch (c.length) {
			case 3: {
				s[0] = h[0] * c[0] + h[1] * (c[0] + c[1])
					+ h[2] * (c[1] + c[2]);
				break;
			}
			case 2: {
				s[0] = h[0] * c[0] + h[1] * (c[0] + c[1])
					+ 2.0 * h[2] * c[1];
				break;
			}
		}
	}
} /* end reduceDual1D */

/*------------------------------------------------------------------*/
private void samplesToInterpolationCoefficient1D (
	final double[] c,
	final int degree,
	final double tolerance
) {
	double[] z = new double[0];
	double lambda = 1.0;
	switch (degree) {
		case 3: {
			z = new double[1];
			z[0] = Math.sqrt(3.0) - 2.0;
			break;
		}
		case 7: {
			z = new double[3];
			z[0] =
				-0.5352804307964381655424037816816460718339231523426924148812;
			z[1] =
				-0.122554615192326690515272264359357343605486549427295558490763;
			z[2] =
				-0.0091486948096082769285930216516478534156925639545994482648003;
			break;
		}
	}
	if (c.length == 1) {
		return;
	}
	for (int k = 0, K = z.length; (k < K); k++) {
		lambda *= (1.0 - z[k]) * (1.0 - 1.0 / z[k]);
	}
	for (int n = 0, N = c.length; (n < N); n++) {
		c[n] = c[n] * lambda;
	}
	for (int k = 0, K = z.length; (k < K); k++) {
		c[0] = getInitialCausalCoefficientMirrorOffBounds(c, z[k], tolerance);
		for (int n = 1, N = c.length; (n < N); n++) {
			c[n] = c[n] + z[k] * c[n - 1];
		}
		c[c.length - 1] = getInitialAntiCausalCoefficientMirrorOffBounds(
			c, z[k], tolerance);
		for (int n = c.length - 2; (0 <= n); n--) {
			c[n] = z[k] * (c[n+1] - c[n]);
		}
	}
} /* end samplesToInterpolationCoefficient1D */

/*------------------------------------------------------------------*/
private void saveRGB (
	final ImageProcessor ip
) {
	final int width = ip.getWidth();
	final int height = ip.getHeight();
	final int length = width * height;
	final byte R[] = new byte[length];
	final byte G[] = new byte[length];
	final byte B[] = new byte[length];
	if (ip.getPixels() instanceof byte[]) {
		final byte[] pixels = (byte[])ip.getPixels();
		final IndexColorModel icm = (IndexColorModel)ip.getColorModel();
		final int mapSize = icm.getMapSize();
		final byte[] reds = new byte[mapSize];
		final byte[] greens = new byte[mapSize];
		final byte[] blues = new byte[mapSize];
		icm.getReds(reds);
		icm.getGreens(greens);
		icm.getBlues(blues);
		int index;
		for (int k = 0; (k < length); k++) {
			index = (int)(pixels[k] & 0xFF);
			R[k] = reds[index];
			G[k] = greens[index];
			B[k] = blues[index];
		}
	}
	else if (ip.getPixels() instanceof int[]) {
		((ColorProcessor)ip).getRGB(R, G, B);
	}
	final FloatProcessor fpR = (FloatProcessor)(new ByteProcessor(
		width, height, R, null).convertToFloat());
	final FloatProcessor fpG = (FloatProcessor)(new ByteProcessor(
		width, height, G, null).convertToFloat());
	final FloatProcessor fpB = (FloatProcessor)(new ByteProcessor(
		width, height, B, null).convertToFloat());
	final float[] distanceTransform = new float[length];
	float distance = 0.5F;
	for (int d = 0, D = (Math.min(width, height) + 1) / 2; (d < D); d++) {
		for (int x = d, X = width - d; (x < X); x++) {
			distanceTransform[d * width + x] = distance;
			distanceTransform[(height - d - 1) * width + x] = distance;
		}
		for (int y = d + 1, Y = height - d - 1; (y < Y); y++) {
			distanceTransform[y * width + d] = distance;
			distanceTransform[y * width + (width - d - 1)] = distance;
		}
		distance += 1.0F;
	}
	final ImagePlus impR = new ImagePlus("MosaicJ" + id + "R", fpR);
	final ImagePlus impG = new ImagePlus("MosaicJ" + id + "G", fpG);
	final ImagePlus impB = new ImagePlus("MosaicJ" + id + "B", fpB);
	final ImageStack isR = impR.getStack();
	final ImageStack isG = impG.getStack();
	final ImageStack isB = impB.getStack();
	isR.addSlice("",
		new FloatProcessor(width, height, distanceTransform, null));
	isG.addSlice("",
		new FloatProcessor(width, height, distanceTransform, null));
	isB.addSlice("",
		new FloatProcessor(width, height, distanceTransform, null));
	impR.setStack("", isR);
	impG.setStack("", isG);
	impB.setStack("", isB);
	new FileSaver(impR).saveAsTiffStack(
		IJ.getDirectory("temp") + "MosaicJ" + id + "R");
	new FileSaver(impG).saveAsTiffStack(
		IJ.getDirectory("temp") + "MosaicJ" + id + "G");
	new FileSaver(impB).saveAsTiffStack(
		IJ.getDirectory("temp") + "MosaicJ" + id + "B");
} /* end saveRGB */

/*------------------------------------------------------------------*/
private void symmetricFirMirrorOffBounds1D (
	final double[] h,
	final double[] c,
	final double[] s
) {
	switch (h.length) {
		case 2: {
			if (2 <= c.length) {
				s[0] = h[0] * c[0] + h[1] * (c[0] + c[1]);
				for (int i = 1, I = s.length - 1; (i < I); i++) {
					s[i] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1]);
				}
				s[s.length - 1] = h[0] * c[c.length - 1]
					+ h[1] * (c[c.length - 2] + c[c.length - 1]);
			}
			else {
				s[0] = (h[0] + 2.0 * h[1]) * c[0];
			}
			break;
		}
		case 4: {
			if (6 <= c.length) {
				s[0] = h[0] * c[0] + h[1] * (c[0] + c[1])
					+ h[2] * (c[1] + c[2]) + h[3] * (c[2] + c[3]);
				s[1] = h[0] * c[1] + h[1] * (c[0] + c[2])
					+ h[2] * (c[0] + c[3]) + h[3] * (c[1] + c[4]);
				s[2] = h[0] * c[2] + h[1] * (c[1] + c[3])
					+ h[2] * (c[0] + c[4]) + h[3] * (c[0] + c[5]);
				for (int i = 3, I = s.length - 3; (i < I); i++) {
					s[i] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1])
						+ h[2] * (c[i - 2] + c[i + 2])
						+ h[3] * (c[i - 3] + c[i + 3]);
				}
				s[s.length - 3] = h[0] * c[c.length - 3]
					+ h[1] * (c[c.length - 4] + c[c.length - 2])
					+ h[2] * (c[c.length - 5] + c[c.length - 1])
					+ h[3] * (c[c.length - 6] + c[c.length - 1]);
				s[s.length - 2] = h[0] * c[c.length - 2]
					+ h[1] * (c[c.length - 3] + c[c.length - 1])
					+ h[2] * (c[c.length - 4] + c[c.length - 1])
					+ h[3] * (c[c.length - 5] + c[c.length - 2]);
				s[s.length - 1] = h[0] * c[c.length - 1]
					+ h[1] * (c[c.length - 2] + c[c.length - 1])
					+ h[2] * (c[c.length - 3] + c[c.length - 2])
					+ h[3] * (c[c.length - 4] + c[c.length - 3]);
			}
			else {
				switch (c.length) {
					case 5: {
						s[0] = h[0] * c[0] + h[1] * (c[0] + c[1])
							+ h[2] * (c[1] + c[2]) + h[3] * (c[2] + c[3]);
						s[1] = h[0] * c[1] + h[1] * (c[0] + c[2])
							+ h[2] * (c[0] + c[3]) + h[3] * (c[1] + c[4]);
						s[2] = h[0] * c[2] + h[1] * (c[1] + c[3])
							+ (h[2] + h[3]) * (c[0] + c[4]);
						s[3] = h[0] * c[3] + h[1] * (c[2] + c[4])
							+ h[2] * (c[1] + c[4]) + h[3] * (c[0] + c[3]);
						s[4] = h[0] * c[4] + h[1] * (c[3] + c[4])
							+ h[2] * (c[2] + c[3]) + h[3] * (c[1] + c[2]);
						break;
					}
					case 4: {
						s[0] = h[0] * c[0] + h[1] * (c[0] + c[1])
							+ h[2] * (c[1] + c[2]) + h[3] * (c[2] + c[3]);
						s[1] = h[0] * c[1] + h[1] * (c[0] + c[2])
							+ h[2] * (c[0] + c[3]) + h[3] * (c[1] + c[3]);
						s[2] = h[0] * c[2] + h[1] * (c[1] + c[3])
							+ h[2] * (c[0] + c[3]) + h[3] * (c[0] + c[2]);
						s[3] = h[0] * c[3] + h[1] * (c[2] + c[3])
							+ h[2] * (c[1] + c[2]) + h[3] * (c[0] + c[1]);
						break;
					}
					case 3: {
						s[0] = h[0] * c[0] + h[1] * (c[0] + c[1])
							+ h[2] * (c[1] + c[2]) + 2.0 * h[3] * c[2];
						s[1] = h[0] * c[1] + (h[1] + h[2]) * (c[0] + c[2])
							+ 2.0 * h[3] * c[1];
						s[2] = h[0] * c[2] + h[1] * (c[1] + c[2])
							+ h[2] * (c[0] + c[1]) + 2.0 * h[3] * c[0];
						break;
					}
					case 2: {
						s[0] = (h[0] + h[1] + h[3]) * c[0]
							+ (h[1] + 2.0 * h[2] + h[3]) * c[1];
						s[1] = (h[0] + h[1] + h[3]) * c[1]
							+ (h[1] + 2.0 * h[2] + h[3]) * c[0];
						break;
					}
					case 1: {
						s[0] = (h[0] + 2.0 * (h[1] + h[2] + h[3])) * c[0];
						break;
					}
				}
			}
			break;
		}
	}
} /* end symmetricFirMirrorOffBounds1D */

} /* end class MosaicJThumb */

/*====================================================================
|	MosaicJTree
\===================================================================*/
class MosaicJTree

{ /*begin class MosaicJTree */

/*....................................................................
	static public methods
....................................................................*/

/*------------------------------------------------------------------*/
static public boolean createMosaic (
	final JProgressBar progressBar,
	final JFrame parent,
	final Stack stackingOrder,
	final boolean blend,
	final boolean rotate,
	final boolean createOutputLog
) {
	final double[][] edgeWeight = getEdgeWeight(stackingOrder);
	final Stack connectedComponent =
		MosaicJTree.countConnectedComponents(edgeWeight);
	if (2 < connectedComponent.size()) {
		final String[] options = {
			"Create Largest",
			"Abort Creation",
			"Cancel"
		};
		final int option = new JOptionPane().showOptionDialog(parent,
			"There are " + (connectedComponent.size() / 2) + " mosaics\n",
			"Select an Option", JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE, null, options, options[0]);
		switch (option) {
			case 0: {
				break;
			}
			case 1: {
				return(false);
			}
			case JOptionPane.CLOSED_OPTION:
			case 2: {
				return(true);
			}
			default: {
				IJ.error("Internal error.");
				return(true);
			}
		}
	}
	FileWriter fw = null;
	if (createOutputLog) {
		final Frame f = new Frame();
		final FileDialog fd = new FileDialog(f, "Output Log", FileDialog.SAVE);
		fd.setFile("Mosaic.log");
		fd.setVisible(true);
		final String path = fd.getDirectory();
		final String filename = fd.getFile();
		if ((path != null) && (filename != null)) {
			try {
				fw = new FileWriter(path + filename);
			} catch (IOException e) {
				IJ.log("IOException " + e.getMessage());
				return(false);
			}
		}
	}
	final Stack vertices = new Stack();
	final Stack tree = getMaximumSpanningTree(stackingOrder, edgeWeight,
		getRoot(connectedComponent), vertices);
	ImagePlus globalImage = null;
	if (tree.isEmpty()) {
		tree.push(new MosaicJEdge((MosaicJThumb)vertices.peek(),
			(MosaicJThumb)vertices.peek(), 1.0));
		globalImage = getGlobalImage(tree, vertices, progressBar, blend, fw);
		globalImage.show();
		globalImage.updateAndDraw();
	}
	else {
		parent.getContentPane().add(progressBar, BorderLayout.NORTH);
		parent.getContentPane().validate();
		progressBar.setMaximum(tree.size() + vertices.size());
		final Iterator i = tree.iterator();
		for (int k = 1, K = tree.size(); (k <= K); k++) {
			progressBar.setValue(k);
			localAlign((MosaicJEdge)i.next(), rotate);
		}
		globalImage = getGlobalImage(tree, vertices, progressBar, blend, fw);
		globalImage.show();
		globalImage.updateAndDraw();
		parent.getContentPane().remove(progressBar);
		parent.getContentPane().validate();
	}
	if (createOutputLog) {
		try {
			fw.close();
		} catch (IOException e) {
			IJ.log("IOException " + e.getMessage());
			return(false);
		}
	}
	return(true);
} /* end createMosaic */

/*------------------------------------------------------------------*/
static public void drawGlobalImage (
	final MosaicJThumb source,
	final double[][] sourcePoints,
	final double[][] targetPoints,
	final double[] bottomRight,
	final ImagePlus globalImage,
	final float[] globalMask,
	final boolean blend
) {
	final double minX = Math.min(
		Math.min(targetPoints[0][0], targetPoints[1][0]),
		Math.min(targetPoints[2][0], bottomRight[0]));
	final double maxX = Math.max(
		Math.max(targetPoints[0][0], targetPoints[1][0]),
		Math.max(targetPoints[2][0], bottomRight[0]));
	final double minY = Math.min(
		Math.min(targetPoints[0][1], targetPoints[1][1]),
		Math.min(targetPoints[2][1], bottomRight[1]));
	final double maxY = Math.max(
		Math.max(targetPoints[0][1], targetPoints[1][1]),
		Math.max(targetPoints[2][1], bottomRight[1]));
	final int dx = (int)Math.round(minX);
	final int dy = (int)Math.round(minY);
	targetPoints[0][0] -= (double)dx;
	targetPoints[0][1] -= (double)dy;
	final int width = (int)Math.round(maxX - minX + 1);
	final int height = (int)Math.round(maxY - minY + 1);
	final Object turboReg = IJ.runPlugIn("TurboReg_", "-transform"
		+ " -file \"" + (IJ.getDirectory("temp") + source + "\"")
		+ " " + width
		+ " " + height
		+ " " + "-rigidBody"
		+ " " + sourcePoints[0][0]
		+ " " + sourcePoints[0][1]
		+ " " + targetPoints[0][0]
		+ " " + targetPoints[0][1]
		+ " " + sourcePoints[1][0]
		+ " " + sourcePoints[1][1]
		+ " " + targetPoints[1][0]
		+ " " + targetPoints[1][1]
		+ " " + sourcePoints[2][0]
		+ " " + sourcePoints[2][1]
		+ " " + targetPoints[2][0]
		+ " " + targetPoints[2][1]
		+ " " + "-hideOutput"
	);
	ImagePlus imp = null;
	try {
		Method method = turboReg.getClass().getMethod("getTransformedImage",
			(Class[])null);
		imp = (ImagePlus)method.invoke(turboReg, (Object[])null);
	} catch (IllegalAccessException e) {
		IJ.log("IllegalAccessException " + e.getMessage());
	} catch (InvocationTargetException e) {
		IJ.log("InvocationTargetException " + e.getMessage());
	} catch (NoSuchMethodException e) {
		IJ.log("NoSuchMethodException " + e.getMessage());
	}
	imp.setSlice(2);
	final float[] imageMask =
		(float[])((FloatProcessor)imp.getProcessor()).getPixels();
	imp.setSlice(1);
	final float[] imageBuffer =
		(float[])((FloatProcessor)imp.getProcessor()).getPixels();
	final float[] finalBuffer =
		(float[])((FloatProcessor)globalImage.getProcessor()).getPixels();
	final int globalWidth = globalImage.getWidth();
	for (int j = 0, k = 0, v = dy; (j < height); v++, j++) {
		for (int i = 0, u = dx; (i < width); k++, u++, i++) {
			imageMask[k] = (blend) ? (imageMask[k])
				: ((imageMask[k] == 0.0F) ? (0.0F) : (1.0F));
			final int w = globalWidth * v + u;
			finalBuffer[w] += imageMask[k] * imageBuffer[k];
			globalMask[w] += imageMask[k];
		}
	}
} /* end drawGlobalImage */

/*------------------------------------------------------------------*/
static public void drawGlobalImage (
	final MosaicJThumb source,
	final double[][] sourcePoints,
	final double[][] targetPoints,
	final double[] bottomRight,
	final ImagePlus globalImageR,
	final ImagePlus globalImageG,
	final ImagePlus globalImageB,
	final float[] globalMask,
	final boolean blend
) {
	final double minX = Math.min(
		Math.min(targetPoints[0][0], targetPoints[1][0]),
		Math.min(targetPoints[2][0], bottomRight[0]));
	final double maxX = Math.max(
		Math.max(targetPoints[0][0], targetPoints[1][0]),
		Math.max(targetPoints[2][0], bottomRight[0]));
	final double minY = Math.min(
		Math.min(targetPoints[0][1], targetPoints[1][1]),
		Math.min(targetPoints[2][1], bottomRight[1]));
	final double maxY = Math.max(
		Math.max(targetPoints[0][1], targetPoints[1][1]),
		Math.max(targetPoints[2][1], bottomRight[1]));
	final int dx = (int)Math.round(minX);
	final int dy = (int)Math.round(minY);
	targetPoints[0][0] -= (double)dx;
	targetPoints[0][1] -= (double)dy;
	final int width = (int)Math.round(maxX - minX + 1);
	final int height = (int)Math.round(maxY - minY + 1);
	ImagePlus impR = null;
	ImagePlus impG = null;
	ImagePlus impB = null;
	ImagePlus imp = null;
	if (source.isColored()) {
		Object turboReg;
		turboReg = IJ.runPlugIn("TurboReg_", "-transform"
			+ " -file \"" + (IJ.getDirectory("temp") + source + "R\"")
			+ " " + width
			+ " " + height
			+ " " + "-rigidBody"
			+ " " + sourcePoints[0][0]
			+ " " + sourcePoints[0][1]
			+ " " + targetPoints[0][0]
			+ " " + targetPoints[0][1]
			+ " " + sourcePoints[1][0]
			+ " " + sourcePoints[1][1]
			+ " " + targetPoints[1][0]
			+ " " + targetPoints[1][1]
			+ " " + sourcePoints[2][0]
			+ " " + sourcePoints[2][1]
			+ " " + targetPoints[2][0]
			+ " " + targetPoints[2][1]
			+ " " + "-hideOutput"
		);
		try {
			Method method = turboReg.getClass().getMethod("getTransformedImage",
				(Class[])null);
			impR = (ImagePlus)method.invoke(turboReg, (Object[])null);
		} catch (IllegalAccessException e) {
			IJ.log("IllegalAccessException " + e.getMessage());
		} catch (InvocationTargetException e) {
			IJ.log("InvocationTargetException " + e.getMessage());
		} catch (NoSuchMethodException e) {
			IJ.log("NoSuchMethodException " + e.getMessage());
		}
		turboReg = IJ.runPlugIn("TurboReg_", "-transform"
			+ " -file \"" + (IJ.getDirectory("temp") + source + "G\"")
			+ " " + width
			+ " " + height
			+ " " + "-rigidBody"
			+ " " + sourcePoints[0][0]
			+ " " + sourcePoints[0][1]
			+ " " + targetPoints[0][0]
			+ " " + targetPoints[0][1]
			+ " " + sourcePoints[1][0]
			+ " " + sourcePoints[1][1]
			+ " " + targetPoints[1][0]
			+ " " + targetPoints[1][1]
			+ " " + sourcePoints[2][0]
			+ " " + sourcePoints[2][1]
			+ " " + targetPoints[2][0]
			+ " " + targetPoints[2][1]
			+ " " + "-hideOutput"
		);
		try {
			Method method = turboReg.getClass().getMethod("getTransformedImage",
				(Class[])null);
			impG = (ImagePlus)method.invoke(turboReg, (Object[])null);
		} catch (IllegalAccessException e) {
			IJ.log("IllegalAccessException " + e.getMessage());
		} catch (InvocationTargetException e) {
			IJ.log("InvocationTargetException " + e.getMessage());
		} catch (NoSuchMethodException e) {
			IJ.log("NoSuchMethodException " + e.getMessage());
		}
		turboReg = IJ.runPlugIn("TurboReg_", "-transform"
			+ " -file \"" + (IJ.getDirectory("temp") + source + "B\"")
			+ " " + width
			+ " " + height
			+ " " + "-rigidBody"
			+ " " + sourcePoints[0][0]
			+ " " + sourcePoints[0][1]
			+ " " + targetPoints[0][0]
			+ " " + targetPoints[0][1]
			+ " " + sourcePoints[1][0]
			+ " " + sourcePoints[1][1]
			+ " " + targetPoints[1][0]
			+ " " + targetPoints[1][1]
			+ " " + sourcePoints[2][0]
			+ " " + sourcePoints[2][1]
			+ " " + targetPoints[2][0]
			+ " " + targetPoints[2][1]
			+ " " + "-hideOutput"
		);
		try {
			Method method = turboReg.getClass().getMethod("getTransformedImage",
				(Class[])null);
			impB = (ImagePlus)method.invoke(turboReg, (Object[])null);
		} catch (IllegalAccessException e) {
			IJ.log("IllegalAccessException " + e.getMessage());
		} catch (InvocationTargetException e) {
			IJ.log("InvocationTargetException " + e.getMessage());
		} catch (NoSuchMethodException e) {
			IJ.log("NoSuchMethodException " + e.getMessage());
		}
		imp = impB;
	}
	else {
		final Object turboReg = IJ.runPlugIn("TurboReg_", "-transform"
			+ " -file \"" + (IJ.getDirectory("temp") + source + "\"")
			+ " " + width
			+ " " + height
			+ " " + "-rigidBody"
			+ " " + sourcePoints[0][0]
			+ " " + sourcePoints[0][1]
			+ " " + targetPoints[0][0]
			+ " " + targetPoints[0][1]
			+ " " + sourcePoints[1][0]
			+ " " + sourcePoints[1][1]
			+ " " + targetPoints[1][0]
			+ " " + targetPoints[1][1]
			+ " " + sourcePoints[2][0]
			+ " " + sourcePoints[2][1]
			+ " " + targetPoints[2][0]
			+ " " + targetPoints[2][1]
			+ " " + "-hideOutput"
		);
		try {
			Method method = turboReg.getClass().getMethod("getTransformedImage",
				(Class[])null);
			imp = (ImagePlus)method.invoke(turboReg, (Object[])null);
		} catch (IllegalAccessException e) {
			IJ.log("IllegalAccessException " + e.getMessage());
		} catch (InvocationTargetException e) {
			IJ.log("InvocationTargetException " + e.getMessage());
		} catch (NoSuchMethodException e) {
			IJ.log("NoSuchMethodException " + e.getMessage());
		}
		impR = imp;
		impG = imp;
		impB = imp;
	}
	impR.setSlice(1);
	final float[] imageBufferR =
		(float[])((FloatProcessor)impR.getProcessor()).getPixels();
	impG.setSlice(1);
	final float[] imageBufferG =
		(float[])((FloatProcessor)impG.getProcessor()).getPixels();
	impB.setSlice(1);
	final float[] imageBufferB =
		(float[])((FloatProcessor)impB.getProcessor()).getPixels();
	imp.setSlice(2);
	final float[] imageMask =
		(float[])((FloatProcessor)imp.getProcessor()).getPixels();
	final float[] finalBufferR =
		(float[])((FloatProcessor)globalImageR.getProcessor()).getPixels();
	final float[] finalBufferG =
		(float[])((FloatProcessor)globalImageG.getProcessor()).getPixels();
	final float[] finalBufferB =
		(float[])((FloatProcessor)globalImageB.getProcessor()).getPixels();
	final int globalWidth = globalImageR.getWidth();
	for (int j = 0, k = 0, v = dy; (j < height); v++, j++) {
		for (int i = 0, u = dx; (i < width); k++, u++, i++) {
			imageMask[k] = (blend) ? (imageMask[k])
				: ((imageMask[k] == 0.0F) ? (0.0F) : (1.0F));
			final int w = globalWidth * v + u;
			finalBufferR[w] += imageMask[k] * imageBufferR[k];
			finalBufferG[w] += imageMask[k] * imageBufferG[k];
			finalBufferB[w] += imageMask[k] * imageBufferB[k];
			globalMask[w] += imageMask[k];
		}
	}
} /* end drawGlobalImage */

/*------------------------------------------------------------------*/
static public void normalizeGlobalImage (
	final ImagePlus globalImage,
	final float[] globalMask
) {
	final double[] colorWeight = ColorProcessor.getWeightingFactors();
	final float backgroundIntensity =
		(float)(colorWeight[0] * (double)Toolbar.getBackgroundColor().getRed()
		+ colorWeight[1] * (double)Toolbar.getBackgroundColor().getGreen()
		+ colorWeight[2] * (double)Toolbar.getBackgroundColor().getBlue());
	final float[] pixels = (float[])globalImage.getProcessor().getPixels();
	for (int k = 0, K = pixels.length; (k < K); k++) {
		if (globalMask[k] != 0.0F) {
			pixels[k] /= globalMask[k];
		}
		else {
			pixels[k] = backgroundIntensity;
		}
	}
	if (globalImage.isInvertedLut()) {
		globalImage.getProcessor().invertLut();
	}
	globalImage.getProcessor().resetMinAndMax();
} /* end normalizeGlobalImage */

/*------------------------------------------------------------------*/
static public ImagePlus normalizeGlobalImage (
	final ImagePlus globalImageR,
	final ImagePlus globalImageG,
	final ImagePlus globalImageB,
	final float[] globalMask,
	final int width,
	final int height
) {
	ImagePlus globalImage = ij.gui.NewImage.createRGBImage("MosaicJ",
		width, height, 1, ij.gui.NewImage.CHECK_AVAILABLE_MEMORY);
	final int[] pixels = (int[])globalImage.getProcessor().getPixels();
	final float[] R = (float[])globalImageR.getProcessor().getPixels();
	final float[] G = (float[])globalImageG.getProcessor().getPixels();
	final float[] B = (float[])globalImageB.getProcessor().getPixels();
	for (int k = 0, K = pixels.length; (k < K); k++) {
		if (globalMask[k] != 0.0F) {
			R[k] /= globalMask[k];
			G[k] /= globalMask[k];
			B[k] /= globalMask[k];
			R[k] = (R[k] < 0.0F) ? (0.0F) : ((R[k] < 255.0F)
				? (Math.round(R[k])) : (255.0F));
			G[k] = (G[k] < 0.0F) ? (0.0F) : ((G[k] < 255.0F)
				? (Math.round(G[k])) : (255.0F));
			B[k] = (B[k] < 0.0F) ? (0.0F) : ((B[k] < 255.0F)
				? (Math.round(B[k])) : (255.0F));
			pixels[k] = ((int)R[k] << 16) | ((int)G[k] << 8) | ((int)B[k]);
		}
		else {
			pixels[k] = Toolbar.getBackgroundColor().getRGB();
		}
	}
	return(globalImage);
} /* end normalizeGlobalImage */

/*....................................................................
	static private methods
....................................................................*/

/*------------------------------------------------------------------*/
static private Stack countConnectedComponents (
	final double[][] edgeWeight
) {
	final Stack connectedComponents = new Stack();
	final boolean[][] adjacency =
		new boolean[edgeWeight.length][edgeWeight.length];
	for (int i = 0, I = adjacency.length; (i < I); i++) {
		for (int j = 0, J = adjacency.length; (j < J); j++) {
			adjacency[i][j] = (edgeWeight[i][j] != 0.0);
		}
	}
	final Stack neighbor = new Stack();
	for (int i = 0, I = adjacency.length; (i < I); i++) {
		for (int j = 0, J = adjacency.length; (j < J); j++) {
			if (adjacency[i][j]) {
				neighbor.push(new Integer(j));
				adjacency[i][j] = false;
				adjacency[j][i] = false;
			}
		}
		if (neighbor.isEmpty()) {
			continue;
		}
		connectedComponents.push(neighbor.peek());
		int tiles = 0;
		while (!neighbor.isEmpty()) {
			final int j = ((Integer)neighbor.pop()).intValue();
			tiles++;
			for (int k = 0, K = adjacency.length; (k < K); k++) {
				if (adjacency[j][k]) {
					if (j != k) {
						neighbor.push(new Integer(k));
					}
					adjacency[j][k] = false;
					adjacency[k][j] = false;
				}
			}
		}
		connectedComponents.push(new Integer(tiles));
	}
	return(connectedComponents);
} /* end countConnectedComponents */

/*------------------------------------------------------------------*/
static private double[][] getEdgeWeight (
	final Stack stackingOrder
) {
	final Stack thumbs = new Stack();
	final Iterator k = stackingOrder.iterator();
	while (k.hasNext()) {
		thumbs.addAll(((MosaicJHierarchy)k.next()).getThumbs());
	}
	final double[][] edgeWeight = new double[thumbs.size()][thumbs.size()];
	for (int i = 0, I = thumbs.size(); (i < I); i++) {
		final Rectangle uTrueBounds =
			((MosaicJThumb)thumbs.elementAt(i)).getTrueBounds();
		edgeWeight[i][i] = (double)(uTrueBounds.width * uTrueBounds.height);
		for (int j = 0, J = i; (j < J); j++) {
			final Rectangle vTrueBounds =
				((MosaicJThumb)thumbs.elementAt(j)).getTrueBounds();
			if (uTrueBounds.intersects(vTrueBounds)) {
				final Rectangle area = uTrueBounds.intersection(vTrueBounds);
				edgeWeight[i][j] = (double)(area.width * area.height);
			}
			else {
				edgeWeight[i][j] = 0.0;
			}
			edgeWeight[j][i] = edgeWeight[i][j];
		}
	}
	return(edgeWeight);
} /* end getEdgeWeight */

/*------------------------------------------------------------------*/
static private ImagePlus getGlobalImage (
	Stack tree,
	final Stack vertices,
	final JProgressBar progressBar,
	final boolean blend,
	final FileWriter fw
) {
	tree = (Stack)tree.clone();
	final Stack neighbors = new Stack();
	for (int k = 0, K = vertices.size(); (k < K); k++) {
		final MosaicJThumb thumb = (MosaicJThumb)vertices.elementAt(k);
		if (thumb.isFrozen()) {
			final double[][] targetTransform = new double[3][3];
			for (int i = 0; (i < 3); i++) {
				for (int j = 0; (j < 3); j++) {
					targetTransform[i][j] = (i == j) ? (1.0) : (0.0);
				}
			}
			targetTransform[0][2] = (double)thumb.getTrueLocation().x;
			targetTransform[1][2] = (double)thumb.getTrueLocation().y;
			thumb.setGlobalTransform(targetTransform);
			final Stack neighbor = new Stack();
			neighbor.push(thumb);
			neighbors.push(neighbor);
		}
	}
	if (neighbors.isEmpty()) {
		final MosaicJThumb thumb = ((MosaicJEdge)tree.peek()).getTarget();
		thumb.setFrozen(true);
		final double[][] targetTransform = new double[3][3];
		for (int i = 0; (i < 3); i++) {
			for (int j = 0; (j < 3); j++) {
				targetTransform[i][j] = (i == j) ? (1.0) : (0.0);
			}
		}
		targetTransform[0][2] = (double)thumb.getTrueLocation().x;
		targetTransform[1][2] = (double)thumb.getTrueLocation().y;
		thumb.setGlobalTransform(targetTransform);
		final Stack neighbor = new Stack();
		neighbor.push(thumb);
		neighbors.push(neighbor);
	}
	while (!neighbors.isEmpty()) {
		final Iterator n = neighbors.iterator();
		while (n.hasNext()) {
			final Stack neighbor = (Stack)n.next();
			if (neighbor.isEmpty()) {
				n.remove();
				continue;
			}
			final MosaicJThumb target = (MosaicJThumb)neighbor.pop();
			final double[][] targetTransform = target.getGlobalTransform();
			final Iterator t = tree.iterator();
			while (t.hasNext()) {
				final MosaicJEdge edge = (MosaicJEdge)t.next();
				final MosaicJThumb source = edge.getNeighbor(target);
				if (source != null) {
					if (source.hasGlobalTransform()) {
						t.remove();
						continue;
					}
					if (edge.getSource() == target) {
						edge.swap();
					}
					final double[][] sourceToTarget =
						edge.getMatrixFromSourceToTarget();
					final double[][] sourceTransform = new double[3][3];
					for (int i = 0; (i < 3); i++) {
						for (int j = 0; (j < 3); j++) {
							sourceTransform[i][j] = 0.0;
							for (int k = 0; (k < 3); k++) {
								sourceTransform[i][j] +=
									targetTransform[i][k] * sourceToTarget[k][j];
							}
						}
					}
					source.setGlobalTransform(sourceTransform);
					neighbor.push(source);
					t.remove();
				}
			}
		}
	}
	final double[][] topRight = new double[vertices.size()][2];
	final double[][] topLeft = new double[vertices.size()][2];
	final double[][] bottomLeft = new double[vertices.size()][2];
	final double[][] bottomRight = new double[vertices.size()][2];
	final int[] width = new int[vertices.size()];
	final int[] height = new int[vertices.size()];
	double lowX = Double.POSITIVE_INFINITY;
	double lowY = Double.POSITIVE_INFINITY;
	double highX = Double.NEGATIVE_INFINITY;
	double highY = Double.NEGATIVE_INFINITY;
	boolean coloredMosaic = false;
	for (int k = 0, K = vertices.size(); (k < K); k++) {
		final MosaicJThumb thumb = (MosaicJThumb)vertices.elementAt(k);
		coloredMosaic |= thumb.isColored();
		width[k] = thumb.getTrueBounds().width;
		height[k] = thumb.getTrueBounds().height;
		final double[] tr = {
			(double)(width[k] - 1),
			0.0,
			1.0
		};
		final double[] tl = {
			0.0,
			0.0,
			1.0
		};
		final double[] bl = {
			0.0,
			(double)(height[k] - 1),
			1.0
		};
		final double[] br = {
			(double)(width[k] - 1),
			(double)(height[k] - 1),
			1.0
		};
		topRight[k][0] = 0.0;
		topRight[k][1] = 0.0;
		topLeft[k][0] = 0.0;
		topLeft[k][1] = 0.0;
		bottomLeft[k][0] = 0.0;
		bottomLeft[k][1] = 0.0;
		bottomRight[k][0] = 0.0;
		bottomRight[k][1] = 0.0;
		final double[][] globalTransform = thumb.getGlobalTransform();
		for (int j = 0; (j < 3); j++) {
			topRight[k][0] += globalTransform[0][j] * tr[j];
			topRight[k][1] += globalTransform[1][j] * tr[j];
			topLeft[k][0] += globalTransform[0][j] * tl[j];
			topLeft[k][1] += globalTransform[1][j] * tl[j];
			bottomLeft[k][0] += globalTransform[0][j] * bl[j];
			bottomLeft[k][1] += globalTransform[1][j] * bl[j];
			bottomRight[k][0] += globalTransform[0][j] * br[j];
			bottomRight[k][1] += globalTransform[1][j] * br[j];
		}
		lowX = Math.min(lowX, Math.min(
			Math.min(topRight[k][0], topLeft[k][0]),
			Math.min(bottomLeft[k][0], bottomRight[k][0])));
		lowY = Math.min(lowY, Math.min(
			Math.min(topRight[k][1], topLeft[k][1]),
			Math.min(bottomLeft[k][1], bottomRight[k][1])));
		highX = Math.max(highX, Math.max(
			Math.max(topRight[k][0], topLeft[k][0]),
			Math.max(bottomLeft[k][0], bottomRight[k][0])));
		highY = Math.max(highY, Math.max(
			Math.max(topRight[k][1], topLeft[k][1]),
			Math.max(bottomLeft[k][1], bottomRight[k][1])));
	}
	lowX = Math.round(lowX);
	lowY = Math.round(lowY);
	highX = Math.round(highX);
	highY = Math.round(highY);
	for (int k = 0; (k < vertices.size()); k++) {
		topRight[k][0] -= lowX;
		topRight[k][1] -= lowY;
		topLeft[k][0] -= lowX;
		topLeft[k][1] -= lowY;
		bottomLeft[k][0] -= lowX;
		bottomLeft[k][1] -= lowY;
		bottomRight[k][0] -= lowX;
		bottomRight[k][1] -= lowY;
	}
	final Dimension globalSize = new Dimension(
		(int)Math.ceil(highX - lowX) + 2,
		(int)Math.ceil(highY - lowY) + 2);
	ImagePlus globalImage = null;
	ImagePlus globalImageR = null;
	ImagePlus globalImageG = null;
	ImagePlus globalImageB = null;
	if (coloredMosaic) {
		globalImageR = ij.gui.NewImage.createFloatImage(
			"MosaicJ", globalSize.width, globalSize.height, 1,
			ij.gui.NewImage.CHECK_AVAILABLE_MEMORY);
		globalImageG = ij.gui.NewImage.createFloatImage(
			"MosaicJ", globalSize.width, globalSize.height, 1,
			ij.gui.NewImage.CHECK_AVAILABLE_MEMORY);
		globalImageB = ij.gui.NewImage.createFloatImage(
			"MosaicJ", globalSize.width, globalSize.height, 1,
			ij.gui.NewImage.CHECK_AVAILABLE_MEMORY);
	}
	else {
		globalImage = ij.gui.NewImage.createFloatImage(
			"MosaicJ", globalSize.width, globalSize.height, 1,
			ij.gui.NewImage.CHECK_AVAILABLE_MEMORY);
	}
	final float[] globalMask =
		new float[globalSize.width * globalSize.height];
	for (int k = 0, K = globalMask.length; (k < K); k++) {
		globalMask[k] = 0.0F;
	}
	if (fw != null) {
		try {
			fw.write("" + ((coloredMosaic) ? ("Color") : ("Gray"))
				+ "\t" + globalSize.width + "\t" + globalSize.height + "\n");
		} catch (IOException e) {
			IJ.log("IOException " + e.getMessage());
			return(globalImage);
		}
	}
	for (int k = 0, K = vertices.size(); (k < K); k++) {
		final MosaicJThumb thumb = (MosaicJThumb)vertices.elementAt(k);
		progressBar.setValue(vertices.size() + k);
		final double[][] sourcePoints = {
			{(double)(width[k] - 1), 0.0},
			{0.0, 0.0},
			{0.0, (double)(height[k] - 1)}
		};
		final double[][] targetPoints = {
			{topRight[k][0], topRight[k][1]},
			{topLeft[k][0], topLeft[k][1]},
			{bottomLeft[k][0], bottomLeft[k][1]}
		};
		if (fw != null) {
			try {
				fw.write("" + sourcePoints[0][0]);
				fw.write("\t" + sourcePoints[0][1]);
				fw.write("\t" + sourcePoints[1][0]);
				fw.write("\t" + sourcePoints[1][1]);
				fw.write("\t" + sourcePoints[2][0]);
				fw.write("\t" + sourcePoints[2][1]);
				fw.write("\t" + targetPoints[0][0]);
				fw.write("\t" + targetPoints[0][1]);
				fw.write("\t" + targetPoints[1][0]);
				fw.write("\t" + targetPoints[1][1]);
				fw.write("\t" + targetPoints[2][0]);
				fw.write("\t" + targetPoints[2][1]);
				fw.write("\t" + bottomRight[k][0]);
				fw.write("\t" + bottomRight[k][1]);
				fw.write("\t" + -((MosaicJThumb)thumb).getSlice());
				fw.write("\t" + ((MosaicJThumb)thumb).getFilePath() + "\n");
			} catch (IOException e) {
				IJ.log("IOException " + e.getMessage());
				return(globalImage);
			}
		}
		if (coloredMosaic) {
			drawGlobalImage(thumb, sourcePoints, targetPoints, bottomRight[k],
				globalImageR, globalImageG, globalImageB, globalMask, blend);
		}
		else {
			drawGlobalImage(thumb, sourcePoints, targetPoints, bottomRight[k],
				globalImage, globalMask, blend);
		}
	}
	progressBar.setIndeterminate(true);
	if (coloredMosaic) {
		globalImage = normalizeGlobalImage(globalImageR,
			globalImageG, globalImageB, globalMask,
			globalSize.width, globalSize.height);
	}
	else {
		normalizeGlobalImage(globalImage, globalMask);
	}
	return(globalImage);
} /* end getGlobalImage */

/*------------------------------------------------------------------*/
static private Stack getMaximumSpanningTree (
	final Stack stackingOrder,
	final double[][] edgeWeight,
	final int root,
	final Stack vertices
) {
	final Stack thumbs = new Stack();
	final Iterator k = stackingOrder.iterator();
	while (k.hasNext()) {
		thumbs.addAll(((MosaicJHierarchy)k.next()).getThumbs());
	}
	final boolean[][] disconnectedEdge =
		new boolean[edgeWeight.length][edgeWeight.length];
	for (int i = 0, I = disconnectedEdge.length; (i < I); i++) {
		for (int j = 0, J = disconnectedEdge.length; (j < J); j++) {
			disconnectedEdge[i][j] = (edgeWeight[i][j] != 0.0);
		}
	}
	final Stack neighbor = new Stack();
	neighbor.push(new Integer(root));
	vertices.push((MosaicJThumb)thumbs.elementAt(root));
	while (!neighbor.isEmpty()) {
		final int i = ((Integer)neighbor.pop()).intValue();
		for (int j = 0, J = disconnectedEdge.length; (j < J); j++) {
			if (disconnectedEdge[i][j]) {
				if (i != j) {
					neighbor.push(new Integer(j));
					if (!vertices.contains((MosaicJThumb)thumbs.elementAt(j))) {
						vertices.push((MosaicJThumb)thumbs.elementAt(j));
					}
				}
				disconnectedEdge[i][j] = false;
				disconnectedEdge[j][i] = false;
			}
		}
	}
	final Stack edges = new Stack();
	for (int i = 0, I = disconnectedEdge.length; (i < I); i++) {
		for (int j = 0, J = i; (j < J); j++) {
			if ((edgeWeight[i][j] != 0.0) && (!disconnectedEdge[i][j])) {
				edges.push(new MosaicJEdge((MosaicJThumb)thumbs.elementAt(i),
					(MosaicJThumb)thumbs.elementAt(j), edgeWeight[i][j]));
			}
		}
	}
	final MosaicJEdge[] graph =
		(MosaicJEdge[])edges.toArray(new MosaicJEdge[0]);
	Arrays.sort(graph);
	final Stack tree = new Stack();
	for (int n = graph.length - 1; (0 <= n); n--) {
		final MosaicJEdge edge = graph[n];
		if (!pathExists(tree, edge.getSource(), edge.getTarget())) {
			tree.push(edge);
			if (tree.size() == (vertices.size() - 1)) {
				break;
			}
		}
	}
	return(tree);
} /* end getMaximumSpanningTree */

/*------------------------------------------------------------------*/
static private int getRoot (
	final Stack connectedComponent
) {
	int largest = ((Integer)connectedComponent.pop()).intValue();
	int root = ((Integer)connectedComponent.pop()).intValue();
	while (!connectedComponent.isEmpty()) {
		final int large = ((Integer)connectedComponent.pop()).intValue();
		final int vertex = ((Integer)connectedComponent.pop()).intValue();
		if (largest < large) {
			largest = large;
			root = vertex;
		}
	}
	return(root);
} /* end getRoot */

/*------------------------------------------------------------------*/
static private void localAlign (
	final MosaicJEdge edge,
	final boolean rotate
) {
	if (edge.getSource().isFrozen()) {
		edge.swap();
	}
	MosaicJThumb source = edge.getSource();
	MosaicJThumb target = edge.getTarget();
	Rectangle sourceCrop = source.getTrueBounds();
	Rectangle targetCrop = target.getTrueBounds();
	final Rectangle commonArea = sourceCrop.intersection(targetCrop);
	final int horizontalOffset = Math.max(1, commonArea.width / 4);
	final Point sourcePoint = new Point(
		commonArea.x + commonArea.width / 2,
		commonArea.y + commonArea.height / 2);
	final Point targetPoint = new Point(sourcePoint);
	commonArea.grow(
		MosaicJThumb.INTERSECTION_MARGIN, MosaicJThumb.INTERSECTION_MARGIN);
	sourceCrop = sourceCrop.intersection(commonArea);
	targetCrop = targetCrop.intersection(commonArea);
	int dx = -source.getTrueBounds().x;
	int dy = -source.getTrueBounds().y;
	sourceCrop.translate(dx, dy);
	sourcePoint.translate(dx, dy);
	dx = -target.getTrueBounds().x;
	dy = -target.getTrueBounds().y;
	targetCrop.translate(dx, dy);
	targetPoint.translate(dx, dy);
	if (rotate) {
		final Object turboReg = IJ.runPlugIn("TurboReg_", "-align"
			+ " -file \"" + (IJ.getDirectory("temp") + source + "\"")
			+ " " + sourceCrop.x
			+ " " + sourceCrop.y
			+ " " + (sourceCrop.x + sourceCrop.width - 1)
			+ " " + (sourceCrop.y + sourceCrop.height - 1)
			+ " -file \"" + (IJ.getDirectory("temp") + target + "\"")
			+ " " + targetCrop.x
			+ " " + targetCrop.y
			+ " " + (targetCrop.x + targetCrop.width - 1)
			+ " " + (targetCrop.y + targetCrop.height - 1)
			+ " -rigidBody"
			+ " " + sourcePoint.x + " " + sourcePoint.y
			+ " " + targetPoint.x + " " + targetPoint.y
			+ " " + (sourcePoint.x - horizontalOffset)
			+ " " + sourcePoint.y
			+ " " + (targetPoint.x - horizontalOffset)
			+ " " + targetPoint.y
			+ " " + (sourcePoint.x + horizontalOffset)
			+ " " + sourcePoint.y
			+ " " + (targetPoint.x + horizontalOffset)
			+ " " + targetPoint.y
			+ " -hideOutput"
		);
		try {
			Method method = turboReg.getClass().getMethod("getSourcePoints",
				(Class[])null);
			edge.setSourcePoints((double[][])method.invoke(turboReg,
				(Object[])null));
			method = turboReg.getClass().getMethod("getTargetPoints",
				(Class[])null);
			edge.setTargetPoints((double[][])method.invoke(turboReg,
				(Object[])null));
		} catch (ClassCastException e) {
			IJ.log("ClassCastException " + e.getMessage());
		} catch (IllegalAccessException e) {
			IJ.log("IllegalAccessException " + e.getMessage());
		} catch (InvocationTargetException e) {
			IJ.log("InvocationTargetException " + e.getMessage());
		} catch (NoSuchMethodException e) {
			IJ.log("NoSuchMethodException " + e.getMessage());
		}
	}
	else {
		final Object turboReg = IJ.runPlugIn("TurboReg_", "-align"
			+ " -file \"" + (IJ.getDirectory("temp") + source + "\"")
			+ " " + sourceCrop.x
			+ " " + sourceCrop.y
			+ " " + (sourceCrop.x + sourceCrop.width - 1)
			+ " " + (sourceCrop.y + sourceCrop.height - 1)
			+ " -file \"" + (IJ.getDirectory("temp") + target + "\"")
			+ " " + targetCrop.x
			+ " " + targetCrop.y
			+ " " + (targetCrop.x + targetCrop.width - 1)
			+ " " + (targetCrop.y + targetCrop.height - 1)
			+ " -translation"
			+ " " + sourcePoint.x + " " + sourcePoint.y
			+ " " + targetPoint.x + " " + targetPoint.y
			+ " -hideOutput"
		);
		double[][] sourcePoints = null;
		double[][] targetPoints = null;
		try {
			Method method = turboReg.getClass().getMethod("getSourcePoints",
				(Class[])null);
			sourcePoints = (double[][])method.invoke(turboReg, (Object[])null);
			method = turboReg.getClass().getMethod("getTargetPoints",
				(Class[])null);
			targetPoints = (double[][])method.invoke(turboReg, (Object[])null);
		} catch (ClassCastException e) {
			IJ.log("ClassCastException " + e.getMessage());
		} catch (IllegalAccessException e) {
			IJ.log("IllegalAccessException " + e.getMessage());
		} catch (InvocationTargetException e) {
			IJ.log("InvocationTargetException " + e.getMessage());
		} catch (NoSuchMethodException e) {
			IJ.log("NoSuchMethodException " + e.getMessage());
		}
		sourcePoints[1][0] = sourcePoints[0][0] + (double)commonArea.width;
		sourcePoints[1][1] = sourcePoints[0][1];
		sourcePoints[2][0] = sourcePoints[0][0];
		sourcePoints[2][1] = sourcePoints[0][1] + (double)commonArea.height;
		edge.setSourcePoints(sourcePoints);
		targetPoints[1][0] = targetPoints[0][0] + (double)commonArea.width;
		targetPoints[1][1] = targetPoints[0][1];
		targetPoints[2][0] = targetPoints[0][0];
		targetPoints[2][1] = targetPoints[0][1] + (double)commonArea.height;
		edge.setTargetPoints(targetPoints);
	}
} /* end localAlign */

/*------------------------------------------------------------------*/
static private boolean pathExists (
	Stack tree,
	MosaicJThumb u,
	final MosaicJThumb v
) {
	tree = (Stack)tree.clone();
	final Stack neighbor = new Stack();
	neighbor.push(u);
	while (!neighbor.isEmpty()) {
		u = (MosaicJThumb)neighbor.pop();
		final Iterator i = tree.iterator();
		while (i.hasNext()) {
			final MosaicJThumb thumb = ((MosaicJEdge)i.next()).getNeighbor(u);
			if (thumb != null) {
				if (thumb == v) {
					return(true);
				}
				neighbor.push(thumb);
				i.remove();
			}
		}
	}
	return(false);
} /* end pathExists */

} /* end class MosaicJTree */