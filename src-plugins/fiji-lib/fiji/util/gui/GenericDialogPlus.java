package fiji.util.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.GenericDialog;

import ij.io.OpenDialog;

import java.awt.Button;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.Toolkit;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import java.io.File;
import java.io.IOException;

import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The GenericDialogPlus class enhances the GenericDialog by
 * a few additional methods.
 *
 * It adds a method to add a file chooser, a dialog chooser,
 * an image chooser, a button, and makes string (and file) fields
 * drop targets.
 */
public class GenericDialogPlus extends GenericDialog implements KeyListener {
	private static final long serialVersionUID = 1L;

	protected int[] windowIDs;
	protected String[] windowTitles;

	public GenericDialogPlus(String title) {
		super(title);
	}

	public GenericDialogPlus(String title, Frame parent) {
		super(title, parent);
	}

	public void addImageChoice(String label, String defaultImage) {
		if (windowTitles == null) {
			windowIDs = WindowManager.getIDList();
			windowTitles = new String[windowIDs.length];
			for (int i = 0; i < windowIDs.length; i++) {
				ImagePlus image = WindowManager.getImage(windowIDs[i]);
				windowTitles[i] = image == null ? "" : image.getTitle();
			}
		}
		addChoice(label, windowTitles, defaultImage);
	}

	public ImagePlus getNextImage() {
		return WindowManager.getImage(windowIDs[getNextChoiceIndex()]);
	}

	public void addStringField(String label, String defaultString, int columns) {
		super.addStringField(label, defaultString, columns);
		TextField text = (TextField)stringField.lastElement();
		text.setDropTarget(null);
		new DropTarget(text, new TextDropTarget(text));
	}

	public void addDirectoryField(String label, String defaultPath) {
		addDirectoryField(label, defaultPath, 20);
	}

	public void addDirectoryField(String label, String defaultPath, int columns) {
		addStringField(label, defaultPath, columns);

		TextField text = (TextField)stringField.lastElement();
		GridBagLayout layout = (GridBagLayout)getLayout();
		GridBagConstraints constraints = layout.getConstraints(text);

		Button button = new Button("Browse...");
		DirectoryListener listener = new DirectoryListener("Browse for " + label, text);
		button.addActionListener(listener);
		button.addKeyListener(this);

		Panel panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		panel.add(text);
		panel.add(button);

		layout.setConstraints(panel, constraints);
		add(panel);
	}

	public void addFileField(String label, String defaultPath) {
		addFileField(label, defaultPath, 20);
	}

	public void addFileField(String label, String defaultPath, int columns) {
		addStringField(label, defaultPath, columns);

		TextField text = (TextField)stringField.lastElement();
		GridBagLayout layout = (GridBagLayout)getLayout();
		GridBagConstraints constraints = layout.getConstraints(text);

		Button button = new Button("Browse...");
		FileListener listener = new FileListener("Browse for " + label, text);
		button.addActionListener(listener);
		button.addKeyListener(this);

		Panel panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		panel.add(text);
		panel.add(button);

		layout.setConstraints(panel, constraints);
		add(panel);
	}

	/**
	 * Add button to the dialog
	 * @param label button label
	 * @param listener listener to handle the action when pressing the button
	 */
	public void addButton(String label, ActionListener listener)
	{
		Button button = new Button(label);

		button.addActionListener(listener);
		button.addKeyListener(this);

		addComponent(button);
	}
	
	public void addComponent(Component component) {
		GridBagLayout layout = (GridBagLayout)getLayout();
		layout.setConstraints(component, getConstraints());
		add(component);
	}

	public void addComponent(Component component, int fill, double weightx) {
		GridBagLayout layout = (GridBagLayout)getLayout();
		GridBagConstraints constraints = getConstraints();
		constraints.fill = fill;
		constraints.weightx = weightx;
		layout.setConstraints(component, constraints);
		add(component);
	}
	
	// Work around too many private restrictions (add a new panel and remove it right away)
	protected GridBagConstraints getConstraints() {
		GridBagLayout layout = (GridBagLayout)getLayout();
		Panel panel = new Panel();
		addPanel(panel);
		GridBagConstraints constraints = layout.getConstraints(panel);
		remove(panel);
		return constraints;
	}

	static class FileListener implements ActionListener {
		String title;
		TextField text;

		public FileListener(String title, TextField text) {
			this.title = title;
			this.text = text;
		}

		public void actionPerformed(ActionEvent e) {
			String fileName = null;
			File dir = new File(text.getText());
			if (!dir.isDirectory()) {
				if (dir.exists())
					fileName = dir.getName();
				dir = dir.getParentFile();
			}
			while (dir != null && !dir.exists())
				dir = dir.getParentFile();

			OpenDialog dialog;
			if (dir == null)
				dialog = new OpenDialog(title, fileName);
			else
				dialog = new OpenDialog(title, dir.getAbsolutePath(), fileName);
			String directory = dialog.getDirectory();
			if (directory == null)
				return;
			fileName = dialog.getFileName();
			text.setText(directory + File.separator + fileName);
		}
	}

	static class DirectoryListener implements ActionListener {
		String title;
		TextField text;

		public DirectoryListener(String title, TextField text) {
			this.title = title;
			this.text = text;
		}

		public void actionPerformed(ActionEvent e) {
			File directory = new File(text.getText());
			while (directory != null && !directory.exists())
				directory = directory.getParentFile();

			JFileChooser fc = new JFileChooser(directory);
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			fc.showOpenDialog(null);
			File selFile = fc.getSelectedFile();
			if (selFile != null)
				text.setText( selFile.getAbsolutePath() );
		}
	}

	static String stripSuffix(String s, String suffix) {
		return !s.endsWith(suffix) ? s :
			s.substring(0, s.length() - suffix.length());
	}

	@SuppressWarnings("unchecked")
	static String getString(DropTargetDropEvent event)
			throws IOException, UnsupportedFlavorException {
		String text = null;
		DataFlavor fileList = DataFlavor.javaFileListFlavor;

		if (event.isDataFlavorSupported(fileList)) {
			event.acceptDrop(DnDConstants.ACTION_COPY);
			List<File> list = (List<File>)event.getTransferable().getTransferData(fileList);
			text = list.get(0).getAbsolutePath();
		}
		else if (event.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			event.acceptDrop(DnDConstants.ACTION_COPY);
			text = (String)event.getTransferable()
				.getTransferData(DataFlavor.stringFlavor);
			if (text.startsWith("file://"))
				text = text.substring(7);
			text = stripSuffix(stripSuffix(text, "\n"),
					"\r").replaceAll("%20", " ");
		}
		else {
			event.rejectDrop();
			return null;
		}

		event.dropComplete(text != null);
		return text;
	}

	static class TextDropTarget extends DropTargetAdapter {
		TextField text;
		DataFlavor flavor = DataFlavor.stringFlavor;

		public TextDropTarget(TextField text) {
			this.text = text;
		}

		public void drop(DropTargetDropEvent event) {
			try {
				text.setText(getString(event));
			} catch (Exception e) { e.printStackTrace(); }
		}
	}

	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		if (keyCode == KeyEvent.VK_ESCAPE || (keyCode == KeyEvent.VK_W &&
				(e.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0))
			// wasCanceled is private; workaround
			windowClosing(null);
	}

	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}

	public static void main(String[] args) {
		GenericDialogPlus gd = new GenericDialogPlus("GenericDialogPlus Test");
		gd.addFileField("A_file", System.getProperty("fiji.dir") + "/jars/ij.jar");
		gd.addDirectoryField("A_directory", System.getProperty("fiji.dir") + "/plugins");
		gd.addButton("Click me!", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				IJ.showMessage("You clicked me!");
			}
		});
		JLabel label = new JLabel("Hello, Ignacio! You're the BEST!");
		JPanel jp = new JPanel();
		jp.add(label);
		gd.addComponent(jp);
		gd.addMessage("(blush)");
		gd.showDialog();
		if (!gd.wasCanceled())
			IJ.showMessage("You chose the file " + gd.getNextString()
				+ "\nand the directory " + gd.getNextString());
	}
}