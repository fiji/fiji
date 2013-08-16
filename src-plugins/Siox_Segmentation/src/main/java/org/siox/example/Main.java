/*
   Copyright 2005, 2006 by Gerald Friedland, Kristian Jantz and Lars Knipping

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.siox.example;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.beans.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.StringTokenizer;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Class to start the SIOX demo as GUI application or as Applet.
 * <P>
 * As Applets, this allows the user to load predefined image resources
 * from the classpath, specified as Applet parameter. Run as application
 * or as signed Applet (with certificate accepted by the user) this
 * allows to load images from the local file system or arbitray URLs
 * and save any result to an PNG in the local file system.
 * <P>
 * The application version displays the SIOX control panel in a frame
 * and opened images in dialogs. The Applet version uses a desktop,
 * where the control panel and the images are displayed in internal frames.
 *
 * @author Lars Knipping
 * @version 1.09
 */
public class Main extends JApplet
{
	// CHANGELOG
	// 2005-12-09 1.09 added set ruler menu, sview mem menu and filechooser preview
	// 2005-12-06 1.08 minor changes on memory output
	// 2005-12-05 1.07 added out of memory checks
	// 2005-12-05 1.06 added support for internal frames, used internal frames
	//                 for Applet version to avoid to-back-switchinng of
	//                 browser-window when an image is opened
	// 2005-12-02 1.05 made class an Applet, added open rsrc action
	// 2005-11-29 1.04 support for open files and URLs
	// 2005-11-24 1.03 added set bg support
	// 2005-11-22 1.01 added zoom support
	// 2005-11-10 1.00 initial release

	// KNOWN PROBLEMS/BUGS
	// - Segmentator need huge amount of memory on image load
	//     (together with GUI about 8 Byte/pixel)

	/** Demo frames title. */
	public static final String FRAME_TITLE = "SIOX Api Demo";
	/** Timle of chequer pattern as icon. */
	private final static Icon CHEQUER_ICON = new ChequerIcon(Color.gray, Color.darkGray, 8);
	/** File filter for png images. */
	private static final ExtensionFileFilter PNG_FILEFILTER =
		new ExtensionFileFilter("PNG images (*.png)", new String[]{"png"});
	/** File filter for supported images. */
	private static final ExtensionFileFilter IMAGE_FILEFILTER =
		new ExtensionFileFilter("Images (*.png, *.jpg, *.gif)",
				new String[]{"png","jpg","jpeg","jpe","gif"});

	/** File chooser for open and save as actions, lazily initialied. */
	private static JFileChooser jFileChooser = null;

	/** The user input managing control panel. */
	private final ControlJPanel ctrlJPanel = new ControlJPanel();
	/** Permission flag for write files. */
	private final boolean canWriteFiles = canWriteFiles();
	/** Flag set when inited as applet. */
	private boolean isInitedAsApplet = false;
	/** Currently save target, <CODE>null</CODE> for none. */
	private File targetFile = null;

	// menu actions
	private final Action openAction = canReadFiles()
	  ? (Action) new OpenAction() : (Action) new EmptyAction("Open File");
	private final Action openUrlAction = canReadUrls()
	  ? (Action) new OpenUrlAction() : (Action) new EmptyAction("Open URL");
	private final Action saveAction = canWriteFiles ? (Action) new SaveAction() : (Action) new EmptyAction("Save");
	private final Action saveAsAction = canWriteFiles ? (Action) new SaveAsAction((SaveAction) saveAction) : (Action) new EmptyAction("Save As");
	private final Action resetAction = new ResetAction();
	private final Action[] zoomActions = new Action[4+1+4];
	private final Action[] bgActions = {
		ctrlJPanel.createSetBgAction(null, "Default", null),
		ctrlJPanel.createSetBgAction(Color.white, "White"),
		ctrlJPanel.createSetBgAction(Color.lightGray, "Light Gray"),
		ctrlJPanel.createSetBgAction(Color.darkGray, "Dark Gray"),
		ctrlJPanel.createSetBgAction(Color.black, "Black"),
		ctrlJPanel.createSetBgAction(Color.red, "Red"),
		ctrlJPanel.createSetBgAction(Color.blue, "Blue"),
		ctrlJPanel.createSetBgAction(Color.green, "Green"),
		ctrlJPanel.createSetBgAction(CHEQUER_ICON, "Chequer", CHEQUER_ICON),
	};
	private final Action[] rulerActions = {
		ctrlJPanel.createSetRulerAction(ScrollDisplay.NO_RULER),
		//ctrlJPanel.createSetRulerAction(ScrollDisplay.EMPTY_RULER),
		ctrlJPanel.createSetRulerAction(ScrollDisplay.METRIC_RULER),
		ctrlJPanel.createSetRulerAction(ScrollDisplay.INCH_RULER),
		ctrlJPanel.createSetRulerAction(ScrollDisplay.PIXEL_RULER),
	};
	// button group of set zoom menus
	private final ButtonGroup zoomGroup = new ButtonGroup();
	// button group of set bg menus
	private final ButtonGroup bgGroup = new ButtonGroup();
	// button group of set ruler menus
	private final ButtonGroup rulerGroup = new ButtonGroup();
	// all zoom actions by command string
	private final HashMap hashMapOfZooms = new HashMap();
	// all bg actions by command string
	private final HashMap hashMapOfBgs = new HashMap();
	// all ruler actions by command string
	private final HashMap hashMapOfRulers = new HashMap();

	/** Contructor used for both, Applet and application. */
	public Main()
	{
		ctrlJPanel.enableCheckForUnsavedChanges(canWriteFiles);
		for (int i=0,j=4; j>=-4; --j,++i)
		{
			final int zoom = 1<<Math.abs(j);
			zoomActions[i] = ctrlJPanel.createZoomAction(zoom, j>=0);
		}
	}

	///////////////////////////////////////////////////////////////
	// APPLET RELATED METHODS
	///////////////////////////////////////////////////////////////

	/** Handles Applet-param dependend setup. */
	public void init()
	{
		isInitedAsApplet = true;
		System.err.println(getAppletInfo());
		final String imagelist = getParameter("imagelist");
		final String[] imgs;
		if (imagelist == null) {
			imgs = null;
		} else {
			final StringTokenizer tok = new StringTokenizer(imagelist, ",");
			imgs = new String[tok.countTokens()];
			for (int i=0; i<imgs.length; ++i) {
				imgs[i] = tok.nextToken();
			}
		}
		setJMenuBar(createJMenuBar(null, imgs, null));

		final JDesktopPane jDesktopPane = new JDesktopPane();
		 setContentPane(jDesktopPane);
		ctrlJPanel.setDesktopPane(jDesktopPane);
		final JInternalFrame jif = new JInternalFrame("SIOX Demo");
		jif.getContentPane().add(ctrlJPanel);
		jif.pack();
		jDesktopPane.add(jif);
		jif.show();
	}

	// public void start() {}
	// public void stop() {}
	// public void destroy() {}

	/**  Returns copyright information about this applet. */
	public String getAppletInfo() {
		return "Demo Applet for the SIOX Java API.\n"
		  + "Copyright 2005, 2006 by Gerald Friedland, Kristian Jantz and Lars Knipping.\n"
		  +"All rights reserved.";
	}

	/** Returns information about the parameters that are understood by this applet. */
	public String[][] getParameterInfo() {
	   return new String[][] {
		 { "imagelist", "String",
		   "Comma separated list of image resources made available to the user.." }
	   };
	}

	///////////////////////////////////////////////////////////////
	// MISC METHODS
	///////////////////////////////////////////////////////////////

	/** Displays a user warning message. */
	private static void showWarning(ControlJPanel ctrlJPanel, String msg)
	{
		showMessage(ctrlJPanel, FRAME_TITLE+": Warning", msg, JOptionPane.WARNING_MESSAGE);
	}

	/** Displays a user message dialog. */
	private static void showMessage(ControlJPanel ctrlJPanel, String title, String msg, int type)
	{
		final Container c = ctrlJPanel.getParent();
		if (c instanceof JInternalFrame)
			JOptionPane.showInternalMessageDialog(c, msg, title, type);
		else
			JOptionPane.showMessageDialog(ctrlJPanel, msg, title, type);
	}

	/** Returns info string on JVM memory usage. */
	private static String getMemStat()
	{
		final Runtime rt = Runtime.getRuntime();
		final long max = rt.maxMemory();
		return "used: "+ getFormattedMemStat(rt.totalMemory()-rt.freeMemory())
		  + ", max available="+(max==Long.MAX_VALUE ? "unlimited" : getFormattedMemStat(max));
	}

	 /** Chooses unit to make it human readable. */
	private static String getFormattedMemStat(long bytes)
	{
		if (bytes < 2 * 1<<10) {
			return bytes + "B";
		}
		double val  = ((double) bytes) / (1<<10);
		final String[] units = { "kB", "MB", "GB", "TB", "PB", "EB" };
		int i = 0;
		while (i<units.length && val>1<<10) {
			++i;
			val /= 1<<10;
		}
		return ((int) val) + "." + (((int) (val*10))%10) + units[i];
	}

	/** Returns short info string how to set JVM max memory. */
	private static String getMemSettingsInfo(boolean forApplet)
	{
	  return forApplet
		? "Maximum JVM memory for Applets can be set with -Xmx<size> (e.g. \"-Xmx512M\")\n"
		+ "in the Plugin Control Panel, Advanced Tab, as Java Runtime Parameter entry."
		: "Maximum JVM memory for Applets can be set with -Xmx<size> as parameter,\n"
		+ "e.g. \"java -Xmx512M org.siox.example.Main\"";
	}

	/**
	 * Handles lazy initialization of static file chooser instance.
	 *
	 * @exception SecurityException if file access permission is not granted.
	 */
	private static JFileChooser getJFileChooser() throws SecurityException
	{
		JFileChooser fChooser = jFileChooser;
		if (fChooser != null) {
			return fChooser;
		}
		// set up file chooser
		fChooser = new JFileChooser();
		fChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fChooser.addChoosableFileFilter(PNG_FILEFILTER);
		fChooser.addChoosableFileFilter(IMAGE_FILEFILTER);
		fChooser.setMultiSelectionEnabled(false);
		final ImagePreview imagePreview = new ImagePreview(fChooser);
		final JPanel jPanel = new JPanel(new BorderLayout());
		jPanel.add(imagePreview.jCheckBox, BorderLayout.NORTH);
		jPanel.add(imagePreview, BorderLayout.CENTER);
		fChooser.setAccessory(jPanel);
		return (jFileChooser = fChooser);
	}

	/** Makes a check for a good guess about file read permissions. */
	private static boolean canReadFiles()
	{
		final SecurityManager securityManager = System.getSecurityManager();
		if (securityManager!=null) {
			try {
				final File file = getJFileChooser().getCurrentDirectory();
				securityManager.checkRead(file.getPath());
			} catch (SecurityException e) {
				return false;
			}
		}
		return true;
	}

	 /** Makes a check for a good guess about URL read permissions. */
	private static boolean canReadUrls()
	{
		final SecurityManager securityManager = System.getSecurityManager();
		if (securityManager!=null) {
			try {
				securityManager.checkConnect("java.sun.com", 80);
			} catch (SecurityException e) {
				return false;
			}
		}
		return true;
	}

	/** Makes a check for a good guess about file erite permissions. */
	private static boolean canWriteFiles()
	{
		try {
			File.createTempFile("siox", null).delete();
		  } catch (SecurityException e) {
			  return false;
			} catch (IOException e) {
				 // ignore
			}
		return true;
	}

	/**
	 * Loads the given file to the control panel, displays warnings
	 * on failures and updates the <CODE>targetFile</CODE> on success.
	 */
	private void open(File file)
	{
		final BufferedImage image;
		try {
			try {
			  image=ImageIO.read(file);
			} catch (SecurityException e) {
			  showWarning(ctrlJPanel, "Cannot access file "+file.getPath());
			  return;
			} catch (IOException e) {
			  showWarning(ctrlJPanel, "Failed to load image "+file.getPath());
			  return;
			}
			if (image!=null) {
				final String title =  FRAME_TITLE+":  "+file.getName()+", "
				  +image.getWidth()+"x"+image.getHeight();
				targetFile = file;
				ctrlJPanel.openImage(title, image);
			} else {
				showWarning(ctrlJPanel, "Unsupported image format: "+file.getPath());
			}
		} catch (OutOfMemoryError e) {
			  System.gc();
			showWarning(ctrlJPanel, "Not enough memory to load image\n"
						+file.getPath()+".\n\n"
						+"JVM memory\n"+getMemStat()+"\n\n"
						+getMemSettingsInfo(isInitedAsApplet));
		}
	}

	/** Updates menu actions enabling on open and close of image display. */
	private void imageIsOpenedChanged(boolean open)
	{
		saveAction.setEnabled(open && canWriteFiles && targetFile!=null);
		saveAsAction.setEnabled(open && canWriteFiles);
		resetAction.setEnabled(open);
		for (int i=0; i<zoomActions.length; ++i)
			zoomActions[i].setEnabled(open);
		for (int i=0; i<bgActions.length; ++i)
			bgActions[i].setEnabled(open);
		for (int i=0; i<rulerActions.length; ++i)
			rulerActions[i].setEnabled(open);
		if (open) {
			// restore zoom
			final String zoomCmd = zoomGroup.getSelection().getActionCommand();
			final Action zoomAction = (Action) hashMapOfZooms.get(zoomCmd);
			zoomAction.actionPerformed(null);
			// restore bg setting
			final String bgCmd = bgGroup.getSelection().getActionCommand();
			final Action bgAction = (Action) hashMapOfBgs.get(bgCmd);
			bgAction.actionPerformed(null);
			// restore ruler setting
			final String rulerCmd = rulerGroup.getSelection().getActionCommand();
			final Action rulerAction = (Action) hashMapOfRulers.get(rulerCmd);
			rulerAction.actionPerformed(null);
		}
	}

	/**
	 * Creates a menu bar for the control panel and adds listeners to the
	 * control panels image display to handle the enabling of the
	 * menu items and to re-apply menu settings to newly opened images.
	 *
	 * @param file file to open or <CODE>null</CODE> for none.
	 * @param rsrcList image resource list avialable from the classpath
	 *        for opening, to be added in a "Open Resource" menu.
	 *        This array may be <CODE>null</CODE>.
	 *        Empty string entries (length null) are ignored.
	 * @param exitAction exit action to be added to the file menu or
	 *         <CODE>null</CODE> for no exit file menu.
	 */
	private JMenuBar createJMenuBar(File file, String[] rsrcList, Action exitAction) {
		final JMenuBar jMenuBar = new JMenuBar();
		final JMenu fileJMenu = new JMenu("File");
		final JMenu editJMenu = new JMenu("Edit");
		final JMenu viewJMenu = new JMenu("View");
		jMenuBar.add(fileJMenu);
		jMenuBar.add(editJMenu);
		jMenuBar.add(viewJMenu);
		fileJMenu.add(openAction);
		fileJMenu.add(openUrlAction);
		if (rsrcList!=null && rsrcList.length>0) {
			final JMenu openRsrcJMenu = new JMenu("Open Resource");
			for (int i=0; i<rsrcList.length; ++i) {
			  if (rsrcList[i].length() > 0)
				  openRsrcJMenu.add(new OpenRsrcAction(rsrcList[i]));
			}
			if (openRsrcJMenu.getMenuComponentCount()>0) {
				fileJMenu.add(openRsrcJMenu);
			}
		}
		fileJMenu.addSeparator();
		fileJMenu.add(saveAction);
		fileJMenu.add(saveAsAction);
		if (exitAction != null) {
			fileJMenu.addSeparator();
			fileJMenu.add(exitAction);
		}
		editJMenu.add(resetAction);
		final JMenu viewZoomJMenu = new JMenu("Set Zoom");
		viewJMenu.add(viewZoomJMenu);
		for (int i=0,j=4; j>=-4; --j,++i)
		{
			final Action action = zoomActions[i];
			final JRadioButtonMenuItem jMenuItem = new JRadioButtonMenuItem(action);
			jMenuItem.setSelected(j==0);
			zoomGroup.add(jMenuItem);
			jMenuItem.setActionCommand((String) action.getValue(Action.NAME));
			hashMapOfZooms.put(jMenuItem.getActionCommand(), action);
			viewZoomJMenu.add(jMenuItem);
			action.setEnabled(false);
		}
		final JMenu viewBgJMenu = new JMenu("Set Background");
		viewJMenu.add(viewBgJMenu);
		for (int i=0; i<bgActions.length; ++i) {
			final Action action = bgActions[i];
			final JRadioButtonMenuItem jMenuItem = new JRadioButtonMenuItem(action);
			jMenuItem.setSelected(i==0);
			bgGroup.add(jMenuItem);
			jMenuItem.setActionCommand((String) action.getValue(Action.NAME));
			hashMapOfBgs.put(jMenuItem.getActionCommand(), action);
			viewBgJMenu.add(jMenuItem);
			action.setEnabled(false);
		}
		final JMenu viewRulerJMenu = new JMenu("Set Ruler");
		viewJMenu.add(viewRulerJMenu);
		for (int i=0; i<rulerActions.length; ++i) {
			final Action action = rulerActions[i];
			final JRadioButtonMenuItem jMenuItem = new JRadioButtonMenuItem(action);
			jMenuItem.setSelected(i==0);
			rulerGroup.add(jMenuItem);
			jMenuItem.setActionCommand((String) action.getValue(Action.NAME));
			hashMapOfRulers.put(jMenuItem.getActionCommand(), action);
			viewRulerJMenu.add(jMenuItem);
			action.setEnabled(false);
		}
		viewJMenu.add(new ShowMemStatAction());
		ctrlJPanel.addImageWindowListener(new WindowAdapter() {
			public void	windowOpened(WindowEvent e)
			{
				imageIsOpenedChanged(true);
			}
			public void	windowClosed(WindowEvent e)
			{
				targetFile = null;
				imageIsOpenedChanged(false);
			}
		  });
		ctrlJPanel.addImageWindowListener(new InternalFrameAdapter() {
			public void	internalFrameOpened(InternalFrameEvent e)
			{
				imageIsOpenedChanged(true);
			}
			public void	internalFrameClosed(InternalFrameEvent e)
			{
				targetFile = null;
				imageIsOpenedChanged(false);
			}
		  });
		  if (file != null) {
			  open(file);
		  }
		  return jMenuBar;
	}


	/////////////////////////////////////////////////////////////////////
	// INNER ACTION CLASSES
	/////////////////////////////////////////////////////////////////////


	/** Opens a file for editing. */
	private class OpenAction extends AbstractAction
	{
		OpenAction()
		{
		   super("Open File");
		   putValue(Action.SHORT_DESCRIPTION, "Opens an Image.");
		}

		public void actionPerformed(ActionEvent actionEvent)
		{
			if (!ctrlJPanel.closeImage()) {
			  return; // user cancel due to unsaved changes
			}
			final JFileChooser fChooser;
			try {
			  fChooser = getJFileChooser();
			} catch (SecurityException e) { // "can't happen"
				showWarning(ctrlJPanel, "Failed to accesss file system.");
				setEnabled(false); // do not try again
				return;
			}
			fChooser.setDialogTitle(FRAME_TITLE+": Open Image");
			fChooser.setFileFilter(IMAGE_FILEFILTER);
			if (JFileChooser.APPROVE_OPTION != fChooser.showOpenDialog(ctrlJPanel)) {
				return; // user cancel
			}
			final File file = fChooser.getSelectedFile();
			if (file != null) {
				open(file);
			}
		}
	}

	/** Opens an image URL. */
	private class OpenUrlAction extends AbstractAction
	{
		private final JTextField jTextField = new JTextField("http://", 30);

		OpenUrlAction()
		{
		   super("Open URL");
		   putValue(Action.SHORT_DESCRIPTION, "Reads an Image from a URL.");
		}

		OpenUrlAction(String name, String tooltip)
		{
		   super(name);
		   putValue(Action.SHORT_DESCRIPTION, tooltip);
		}

		void open(URL url)
		{
			final BufferedImage image;
			try {
				image=ImageIO.read(url);
				if (image!=null) {
					final String title =  FRAME_TITLE+":  "
					  +(new File(url.getPath())).getName()
					  +", "+image.getWidth()+"x"+image.getHeight();
					targetFile = null;
					ctrlJPanel.openImage(title, image);
				} else {
					showWarning(ctrlJPanel, "Unsupported image format: "+url);
				}
			} catch (SecurityException e) {
				showWarning(ctrlJPanel, "Not allowed to load image "+url);
			} catch (IOException e) {
				showWarning(ctrlJPanel, "Failed to load image "+url);
			}
		}

		private URL toUrl(String urlString)
		{
			try {
				return new URL(urlString);
			} catch (MalformedURLException e) {
				try {
					return new URL("http://"+urlString);
				} catch (MalformedURLException mue) {
					return null;
				}
			}
		}

		public void actionPerformed(ActionEvent actionEvent)
		{
			if (!ctrlJPanel.closeImage()) {
				return; // user cancel due to unsaved changes
			}
			final Object[] msg = new Object[] {"Image URL:", jTextField };
			if (JOptionPane.OK_OPTION !=
				JOptionPane.showConfirmDialog(ctrlJPanel, msg,
											  FRAME_TITLE+":  Open URL",
											  JOptionPane.OK_CANCEL_OPTION,
											  JOptionPane.QUESTION_MESSAGE)) {
				return; // user cancel
			}
			final URL url = toUrl(jTextField.getText());
			if (url == null) {
				showWarning(ctrlJPanel, "Not a valid URL: \""+jTextField.getText()+"\".");
			} else {
				try {
					open(url);
				} catch (OutOfMemoryError e) {
					System.gc();
					showWarning(ctrlJPanel,
								"Not enough memory to load image\n"
								+url+".\n\n"
								+"JVM memory\n"+getMemStat()+"\n\n"
								+getMemSettingsInfo(isInitedAsApplet));

				}
			}
		}
	}

	/** Opens an image for a predefined resource name. */
	private class OpenRsrcAction extends OpenUrlAction
	{
		private final String name; // rc name

		OpenRsrcAction(String name)
		{
		   super(name, "Opens the Image Resource "+name+".");
		   this.name = name;
		}

		public void actionPerformed(ActionEvent actionEvent)
		{
			if (!ctrlJPanel.closeImage()) {
				return;  // user cancel due to unsaved changes
			}
			final URL url;
			try {
				url = getClass().getClassLoader().getResource(name);
			} catch (SecurityException e) { //  strange combination of permissions
				showWarning(ctrlJPanel, "Failed to access resource "+name+".");
				setEnabled(false); // next try won't be any better ...
				return;
			}
			if (url != null) {
				try {
					open(url);
				} catch (OutOfMemoryError e) {
					System.gc();
					showWarning(ctrlJPanel,
								"Not enough memory to load image\n"
								+url+".\n\n"
								+"JVM memory\n"+getMemStat()+"\n\n"
								+getMemSettingsInfo(isInitedAsApplet));

				}
			} else {
				showWarning(ctrlJPanel, "Failed to locate resource "+name+".");
			}
		}
	}

	/** Saves image to current file. */
	private class SaveAction extends AbstractAction
	{
		SaveAction()
		{
		   super("Save");
		   putValue(Action.SHORT_DESCRIPTION, "Saves Image as PNG.");
		   setEnabled(false);
		}

		public void actionPerformed(ActionEvent actionEvent)
		{
			final File file = targetFile;
			if (file == null) // "can't happen"
				throw new IllegalStateException("no target file for save");
			ctrlJPanel.storeCurrentImageTo(file, false); // unconfirmed overwrite
		}
	}

	/** Queries use to a target file, saves image to given file. */
	private class SaveAsAction extends AbstractAction
	{

		// to ensuring save is enabled when target name has been
		// determined by save as
		private final SaveAction saveAction;

		SaveAsAction(SaveAction saveAction)
		{
			super("Save As");
			putValue(Action.SHORT_DESCRIPTION, "Saves Image as PNG to User Selected File.");
			setEnabled(false);
			this.saveAction = saveAction;
		}

		public void actionPerformed(ActionEvent actionEvent)
		{
			// open file chooser for getting a file target
			final JFileChooser fChooser;
			try {
				fChooser = getJFileChooser();
			} catch (SecurityException e) { // "can't happen"
				showWarning(ctrlJPanel, "Failed to accesss file system.");
				// unfortunately setEnabled(false); makes no sense here, as it will be enabled gain ...
				return;
			}
			fChooser.setDialogTitle( FRAME_TITLE+": Save as PNG");
			fChooser.setFileFilter(PNG_FILEFILTER);
			if (JFileChooser.APPROVE_OPTION != fChooser.showSaveDialog(ctrlJPanel)) {
				return; // user cancel
			}
			final File file = fChooser.getSelectedFile();
			if (file != null) {
				if (ctrlJPanel.storeCurrentImageTo(file, true)) {
					targetFile = file;
					saveAction.setEnabled(true);
				}
			}
		}
	}

	/** Action to show mem stats. */
	private class ShowMemStatAction extends AbstractAction
	{
		ShowMemStatAction()
		{
		   super("Show Memory Usage");
		}

		public void actionPerformed(ActionEvent actionEvent)
		{
			System.gc();
			showMessage(ctrlJPanel, FRAME_TITLE+": Memory Usage",
						"JVM memory\n"+getMemStat(),
						JOptionPane.INFORMATION_MESSAGE);

		}
	}

   /** Reset image, its selections, and the other GUI components. */
	private class ResetAction extends AbstractAction
	{

		public ResetAction()
		{
			super("Reset");
			putValue(Action.SHORT_DESCRIPTION,
					 "Resets Image and Control Panel Components.");
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e)
		{
			ctrlJPanel.reset();
		}
	}

	/** Action for exiting application. */
	private static class ExitAction extends AbstractAction
	{
		final ControlJPanel ctrlJPanel;
		final JFrame jFrame;

		 ExitAction(ControlJPanel ctrlJPanel, JFrame jFrame)
		{
			super("Exit");
			putValue(Action.SHORT_DESCRIPTION, "Exits Application.");
			this.ctrlJPanel = ctrlJPanel;
			this.jFrame = jFrame;
		}

		public void actionPerformed(ActionEvent e)
		{
			if (ctrlJPanel.closeImage()) {
				jFrame.dispose();
				System.exit(0);
			}
		}
	}

	/** Named and disabled dummy action. */
	private static class EmptyAction extends AbstractAction
	{
		 EmptyAction(String name)
		{
			super(name);
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e)
		{
		}
	}

	/////////////////////////////////////////////////////////////////////
	// OTHER INNER CLASSES
	/////////////////////////////////////////////////////////////////////

	/** Square checker pattern icon for marking transparent bg. */
	private static class ChequerIcon implements Icon
	{
		private final Color c0, c1;
		private final int size;

		ChequerIcon(Color c0, Color c1, int squareSize)
		{
			this.c0 = c0;
			this.c1 = c1;
			this.size = 2*squareSize;
		}

		public int getIconHeight()
		{
		  return size;
		}

		public int getIconWidth()
		{
			return size;
		}

		public void paintIcon(Component comp, Graphics graphics, int x, int y) {
			final Graphics g = graphics.create();
			final int d = size/2;
			g.translate(x, y);
			g.setColor(c0);
			g.fillRect(0, 0, d, d);
			g.fillRect(d, d, d, d);
			g.setColor(c1);
			g.fillRect(d, 0, d, d);
			g.fillRect(0, d, d, d);
			g.dispose();
		}
	}

	/** Image previewer for file chooser. */
	private static class ImagePreview extends JComponent
	implements ChangeListener, PropertyChangeListener {

		/** Toggles prewies on/off. */
		   public final JCheckBox jCheckBox = new JCheckBox("Preview Images");
		/** Filechooser to preview the selected images for. */
		private final JFileChooser jFileChooser;
		/** Currently shown image. */
		private BufferedImage image = null;

		ImagePreview(JFileChooser jFileChooser)
		{
			setPreferredSize(new Dimension(150, 150));
			this.jFileChooser = jFileChooser;
			jFileChooser.addPropertyChangeListener(this);
			jCheckBox.addChangeListener(this);
			final String tooltipString = "Show Preview of Any Selected Image.";
			this.setToolTipText(tooltipString);
			jCheckBox.setToolTipText(tooltipString);
		}

		public void propertyChange(PropertyChangeEvent e)
		{
			updateImage();
		}

		public void stateChanged(ChangeEvent e)
		{
			updateImage();
		}

		protected void paintComponent(Graphics g)
		{
			final BufferedImage img = image;
			if (img == null)
				return;
			// get scale factor (only scale down, not up)
			final int w = getWidth();
			final int h = getHeight();
			final int iw = img.getWidth();
			final int ih = img.getHeight();
			final double scale =  Math.min(1, Math.min(w/(double) iw, h/(double) ih));
			final AffineTransform af = AffineTransform.getScaleInstance(scale, scale);
			((Graphics2D) g).drawImage(img, af, null);
			// image info
			final String info = iw+"x"+ih;
			g.setColor(getBackground());
			for (int dx=-1; dx<=+1; ++dx)
				for (int dy=-1; dy<=+1; ++dy)
					g.drawString(info, 6+dx, h-6+dy);
			g.setColor(getForeground());
			g.drawString(info, 6, h-6);
		}

		private void updateImage()
		{
			try {
				final File file = jCheckBox.isSelected()
				  ? getJFileChooser().getSelectedFile() : null;
				image = (file==null || !file.isFile()) ? null : ImageIO.read(file);
			} catch (SecurityException e) {
				//e.printStackTrace();
				image = null;
			} catch (IOException e) {
				//e.printStackTrace();
				image = null;
			} finally {
				repaint();
			}
		}
	}

	/////////////////////////////////////////////////////////////////////
	// MAIN METHOD & RELATED
	/////////////////////////////////////////////////////////////////////

	/**
	 * Prints out usage.
	 */
	private static void printUsage()
	{
		System.out.println("A Demo program for the SIOX Java API.\n\n"
						   +"usage> java org.siox.example.Main [-h | <infile>]\n\n"
						   +"options:\n"
						   +"-h         Displays this message.\n"
						   +"parameters:\n"
						   +"<infile>   Image file in format supported by Java (at least PNG, JPG, GIF)\n");
	}

	/**
	 * This runs the demo application.
	 */
	public static void main(String[] argv)
	{
		if (argv.length>1 || (argv.length==1 && argv[0].startsWith("-h"))) {
			printUsage();
			System.exit((argv.length>1) ? -1 : 0);
		}
		final Main main = new Main();
		final File file = (argv.length>0) ? new File(argv[0]) : null;
		final JFrame jFrame = new JFrame(FRAME_TITLE);
		jFrame.setContentPane(main.ctrlJPanel);
		final Action exitAction = new ExitAction(main.ctrlJPanel, jFrame);
		jFrame.setJMenuBar(main.createJMenuBar(file, null, exitAction));
		jFrame.pack();
		jFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		jFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e)
			{
				exitAction.actionPerformed(null);
			}
		});
		jFrame.setVisible(true);
	}
}
