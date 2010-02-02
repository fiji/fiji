/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2009, 2010 Mark Longair, Johannes Schindelin */

/*
  This file is part of the ImageJ plugin "Tutorial Maker".

  The ImageJ plugin "Tutorial Maker" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Tutorial Maker" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package fiji;

import fiji.scripting.TextEditor;

import ij.IJ;
import ij.ImagePlus;
import ij.Menus;
import ij.WindowManager;

import ij.gui.GenericDialog;

import ij.io.FileInfo;

import ij.plugin.BrowserLauncher;
import ij.plugin.JpegWriter;
import ij.plugin.PlugIn;

import java.awt.AWTException;
import java.awt.Button;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.TextField;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import java.util.regex.Pattern;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class Wiki_Editor implements PlugIn, ActionListener {
	protected String name;

	protected final static String URL = "http://pacific.mpi-cbg.de/wiki/";

	protected enum Mode { TUTORIAL_MAKER, NEWS };
	protected Mode mode;

	public void run(String arg) {
		String dialogTitle = "Tutorial Maker";
		String defaultTitle = "";
		mode = Mode.TUTORIAL_MAKER;

		if (arg.equals("rename")) {
			rename();
			return;
		}
		else if (arg.equals("news")) {
			mode = Mode.NEWS;
			dialogTitle = "Fiji News";
			defaultTitle = new SimpleDateFormat("yyyy-MM-dd - ")
				.format(Calendar.getInstance().getTime());
		}
		else
			interceptRenames();

		GenericDialog gd = new GenericDialog(dialogTitle);
		gd.addStringField("Tutorial_title", defaultTitle, 20);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		name = gd.getNextString();
		if (name.length() == 0)
			return;
		name = capitalize(name).replace(' ', '_');

		addEditor();
	}

	protected static String capitalize(String string) {
		return string.substring(0, 1).toUpperCase()
			+ string.substring(1);
	}

	protected TextEditor editor;
	protected JMenuItem upload, preview, toBackToggle, renameImage;

	protected void addEditor() {
		editor = new TextEditor(null);

		int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		JMenu menu = new JMenu("Wiki");
		menu.setMnemonic(KeyEvent.VK_W);
		upload = editor.addToMenu(menu, "Upload", KeyEvent.VK_U, ctrl);
		preview = editor.addToMenu(menu, "Preview", KeyEvent.VK_R, ctrl);
		if (mode == Mode.TUTORIAL_MAKER) {
			toBackToggle = editor.addToMenu(menu, "", 0, 0);
			renameImage = editor.addToMenu(menu, "Rename Image", KeyEvent.VK_I, ctrl);
			toBackToggleSetLabel();
		}

		for (int i = 0; i < menu.getItemCount(); i++)
			menu.getItem(i).addActionListener(this);

		editor.getJMenuBar().add(menu);

		editors.add(editor);

		editor.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				if (snapshotFrame != null)
					snapshotFrame.dispose();
				editors.remove(this);
			}
		});

		String text = "== " + name.replace('_', ' ') + " ==\n\n";
		String category = "\n[[Category:"
			+ (mode == Mode.TUTORIAL_MAKER ? "Tutorials" : "News")
			+ "]]";
		editor.setTitle("Edit Wiki - " + name);
		editor.getTextArea().setText(text + category);
		editor.getTextArea().setCaretPosition(text.length());

		JMenuBar menuBar = editor.getJMenuBar();
		for (int i = menuBar.getMenuCount() - 1; i >= 0; i--) {
			String label = menuBar.getMenu(i).getLabel();
			if (!label.equals("File") && !label.equals("Edit") &&
					!label.equals("Wiki"))
				menuBar.remove(i);
		}

		if (mode == Mode.TUTORIAL_MAKER)
			showSnapshotFrame();

		editor.setVisible(true);
	}

	public String getText() {
		return editor.getTextArea().getText();
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == upload)
			upload();
		else if (source == preview)
			preview();
		else if (source == renameImage)
			renameImage();
		else if (source == toBackToggle) {
			putSnapshotsToBack = !putSnapshotsToBack;
			toBackToggleSetLabel();
		}
	}

	protected boolean putSnapshotsToBack = true;

	protected void toBackToggleSetLabel() {
		toBackToggle.setLabel(putSnapshotsToBack ?
			"Leave snapshots in the foreground" :
			"Put snapshots into the background");
	}

	protected void upload() {
		IJ.showStatus("Uploading " + name + "...");
		IJ.showProgress(0, 1);
		List<String> images = getImages();
		if (!saveOrUploadImages(null, images))
			return;

		GraphicalMediaWikiClient client =
			new GraphicalMediaWikiClient(URL + "index.php");

		if (!client.login("Wiki Login"))
			return;

		if (!saveOrUploadImages(client, images))
			return;

		client.uploadPage(name, getText(), "Add " + name);

		client.logOut();

		IJ.showStatus("Uploading " + name + " finished.");
		IJ.showProgress(1, 1);

		new BrowserLauncher().run(URL + "index.php?title= " + name);
		editor.dispose();
	}

	protected void preview() {
		IJ.showStatus("Previewing " + name + "...");
		IJ.showProgress(0, 2);

		List<String> images = getImages();
		if (!saveOrUploadImages(null, images))
			return;

		GraphicalMediaWikiClient client =
			new GraphicalMediaWikiClient(URL + "index.php");

		if (!client.login("Wiki Login (Preview)"))
			return;
		String html = client.uploadOrPreviewPage(name, getText(),
				"Add " + name, true);
		client.logOut();

		IJ.showStatus("Preparing " + name + " for preview...");
		IJ.showProgress(1, 2);

		html = html.replaceAll("<img[^>]*src=\"(?=/wiki/)",
				"$0" + URL.substring(0, URL.length() - 6));

		int start = html.indexOf("<div class='previewnote'>");
		start = html.indexOf("</div>", start) + 6;
		int end = html.indexOf("<div id='toolbar'>");
		html = "<html>\n<head>\n<title>Preview of " + name
			+ "</title>\n</head>\n<body>\n"
			+ html.substring(start, end)
			+ "</body>\n</html>\n";
		Pattern imagePattern = Pattern.compile("<a href=[^>]*DestFile=",
				Pattern.DOTALL);
		String[] parts = imagePattern.split(html);

		html = parts[0];
		for (int i = 1; i < parts.length; i++) {
			int quote = parts[i].indexOf('"', 1);
			int endTag = parts[i].indexOf("</a>", quote + 1);

			String image = parts[i].substring(0, quote);
			ImagePlus imp = WindowManager.getImage(image);
			if (imp == null && Character
					.isUpperCase(image.charAt(0))) {
				image = capitalize(image);
				imp = WindowManager.getImage(image);
			}

			if (imp == null)
				html += "&lt;img src=" + image + "&gt;";
			else {
				FileInfo info = imp.getOriginalFileInfo();
				File file = new File(info.directory,
						info.fileName);
				try {
					html += "<img src=\""
						+ file.toURL() + "\">";
				} catch (Exception e) { e.printStackTrace(); }
			}

			html += parts[i].substring(endTag + 4);
		}

		try {
			File file = File.createTempFile("preview", ".html");
			FileOutputStream out = new FileOutputStream(file);
			out.write(html.getBytes());
			out.close();

			new BrowserLauncher().run(file.toURL().toString());

			IJ.showStatus("Browsing " + name);
			IJ.showProgress(2, 2);
		} catch (IOException e) {
			e.printStackTrace();
			error(e.getMessage());
		}
	}

	protected List<String> getImages() {
		List<String> result = new ArrayList<String>();
		String text = getText();
		int image = 0;
		for (;;) {
			image = text.indexOf("[[Image:", image);
			if (image < 0)
				return result;
			image = image + 8;
			int bracket = text.indexOf("]]", image);
			int pipe = text.indexOf('|', image);
			if (bracket < 0 || (pipe >= 0 && pipe < bracket))
				bracket = pipe;
			if (bracket < 0)
				return result;
			result.add(text.substring(image, bracket));
			image = bracket + 1;
		}
	}

	protected boolean error(String message) {
		IJ.showProgress(1, 1);
		IJ.error(message);
		return false;
	}

	protected boolean saveOrUploadImages(MediaWikiClient client,
			List<String> images) {
		int i = 0, total = images.size() * 2 + 1;
		for (String image : images) {
			ImagePlus imp = WindowManager.getImage(image);
			if (imp == null)
				return error("There is no image " + image);
			if (image.indexOf(' ') >= 0) {
				String newTitle = image.replace(' ', '_');
				if (!IJ.showMessageWithCancel("Rename Image",
						"Image title '" + image
						+ "' contains spaces; fix?"))
					return error("Aborted");
				imp.setTitle(newTitle);
				rename(image, newTitle);
				images.set(i, newTitle);
			}
			FileInfo info = imp.getOriginalFileInfo();
			if (info == null) {
				info = new FileInfo();
				info.width = imp.getWidth();
				info.height = imp.getHeight();
				info.directory = IJ.getDirectory("temp");
				info.fileName = image;
				imp.changes = true;
				imp.setFileInfo(info);
			}
			if (info.directory == null) {
				info.directory = IJ.getDirectory("temp");
				imp.changes = true;
			}
			if (info.fileName == null) {
				info.fileName = image;
				imp.changes = true;
			}
			if (imp.changes) {
				JpegWriter.save(imp,
					info.directory + "/" + info.fileName,
					JpegWriter.DEFAULT_QUALITY);
				imp.changes = false;
			}
			if (client != null) {
				if (wikiHasImage(image))
					switch (imageExistsDialog(image)) {
					case 1: return error("Aborted");
					case 2: continue;
					}
				if (!client.uploadFile(image, "Upload " + image
							+ " for " + name,
							new File(info.directory,
								info.fileName)))
					return error("Uploading "
							+ image + " failed");
				IJ.showStatus("Uploading " + image + "...");
				IJ.showProgress(++i + total / 2, total);
			}
			else
				// TODO check if it is already there
				IJ.showProgress(++i, total);
		}
		return true;
	}

	protected boolean wikiHasImage(String image) {
		try {
			URL url = new URL(URL
					+ "index.php?title=Image:" + image);
			InputStream input = url.openStream();
			byte[] buffer = new byte[65536];
			int offset = 0;
			while (offset < buffer.length) {
				int count = input.read(buffer, offset,
					buffer.length - offset);
				if (count < 0)
					break;
				offset += count;
			}
			input.close();
			boolean hasFile = new String(buffer).indexOf("No file "
					+ "by this name exists") < 0;
			if (hasFile)
				System.err.println("has image: "
						+ new String(buffer));
			return hasFile;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	protected int imageExistsDialog(String image) {
		GenericDialog gd = new GenericDialog("Image exists");
		gd.addMessage("The image '" + image + "' exists already on "
			+ "the Wiki");
		String[] choice = {
			"Upload '" + image + "' anyway",
			"Abort",
			"Skip uploading '" + image + "'"
		};
		gd.addChoice("action", choice, choice[0]);
		gd.showDialog();
		if (gd.wasCanceled())
			return 1;
		return gd.getNextChoiceIndex();
	}

	protected static List<TextEditor> editors = new ArrayList<TextEditor>();

	protected static String originalRename, originalRenameArg;

	protected void interceptRenames() {
		if (originalRename != null)
			return;

		originalRename = (String)Menus.getCommands().get("Rename...");
		if (originalRename.endsWith("\")")) {
			int paren = originalRename.lastIndexOf("(\"");
			originalRenameArg = originalRename.substring(paren + 2,
				originalRename.length() - 2);
			originalRename = originalRename.substring(0, paren);
		}
		else
			originalRenameArg = "";

		Menus.getCommands().put("Rename...", getClass().getName()
			+ "(\"rename\")");
	}

	protected void rename() {
		String oldTitle = WindowManager.getCurrentImage().getTitle();
		IJ.runPlugIn(originalRename, originalRenameArg);
		String newTitle = WindowManager.getCurrentImage().getTitle();
		rename(oldTitle, newTitle);
	}

	protected void rename(String oldTitle, String newTitle) {
		if (oldTitle.equals(newTitle))
			return;
		for (TextEditor editor : editors) {
			String text = getText();
			String transformed = text.replaceAll("\\[\\[Image:"
					+ oldTitle.replaceAll("\\.", "\\\\.")
					+ "(?=[]|])",
				"[[Image:" + newTitle);
			if (!text.equals(transformed))
				editor.getTextArea().setText(transformed);
		}
	}

	protected void renameImage() {
		List<String> images = getImages();
		if (images.size() == 0) {
			IJ.error("The text refers to no image");
			return;
		}

		String[] list = images.toArray(new String[0]);
		GenericDialog gd = new GenericDialog("Rename Image");
		gd.addChoice("image", list, list[0]);
		gd.addStringField("new_title", list[0], 20);

		final TextField textField =
			(TextField)gd.getStringFields().lastElement();
		final Choice choice = (Choice)gd.getChoices().lastElement();
		choice.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				textField.setText(choice.getSelectedItem());
			}
		});

		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String oldTitle = gd.getNextChoice();
		String newTitle = gd.getNextString();
		if (newTitle.length() == 0)
			return;

		ImagePlus image = WindowManager.getImage(oldTitle);
		if (image == null) {
			IJ.error("No such image: " + oldTitle);
			return;
		}
		image.setTitle(newTitle);
		rename(oldTitle, newTitle);
	}

	protected Frame snapshotFrame;

	public void showSnapshotFrame() {
		snapshotFrame = new Frame("Snapshot");
		snapshotFrame.setLayout(new FlowLayout());
		snapshotFrame.add(createButton("Snap", 0));
		snapshotFrame.add(createButton("Snap (3sec delay)", 3000));
		snapshotFrame.pack();
		snapshotFrame.setAlwaysOnTop(true);
		snapshotFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				// TODO: ask first
				snapshotFrame.dispose();
			}
		});
		snapshotFrame.show();
	}

	protected Button createButton(String title, long delay) {
		Button button = new Button(title);
		if (delay > 0) {
			AutoSnap auto = new AutoSnap(button, delay);
			button.addActionListener(auto);
			button.addMouseListener(auto);
			snapshotFrame.addMouseListener(auto);
		}
		else {
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					snapshot();
				}
			});
			button.addMouseListener(new AutoSnap(button, 1000));
		}
		return button;
	}

	class AutoSnap extends MouseAdapter implements ActionListener {
		String originalLabel;
		Button button;
		int delay = 3;

		AutoSnap(Button button, long millis) {
			this.button = button;
			originalLabel = button.getLabel();
			delay = (int)(millis / 1000);
		}

		Thread thread;
		synchronized void startThread() {
			stopThread();
			thread = new Thread() {
				public void run() {
					delayedSnap();
					button.setLabel(originalLabel);
				}
			};
			thread.start();
		}

		protected void delayedSnap() {
			for (int i = delay; i >= 0; i--) {
					if (!sleep(1000))
						return; /* stopped */
					button.setLabel(delay == 1 ? "..." :
						"Snap in " + i + " secs");
			}
			snapshot();
		}

		synchronized void stopThread() {
			if (thread == null)
				return;
			button.setLabel(originalLabel);
			thread.interrupt();
			thread = null;
		}

		public void mouseEntered(MouseEvent e) {
			startThread();
		}

		public void mouseExited(MouseEvent e) {
			stopThread();
		}

		public void actionPerformed(ActionEvent event) {
			stopThread();
			delayedSnap();
		}
	}

	protected int snapshotCounter;

	protected String getSnapshotName() {
		for (;;) {
			String title = name
				+ "-" + (++snapshotCounter) + ".jpg";
			if (WindowManager.getImage(title) == null)
				return title;
		}
	}

	protected boolean sleep(long millis) {
		try {
			Thread.sleep(millis);
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}

	protected void snapshot() {
		try {
			Robot robot = new Robot();
			Rectangle rectangle = new Rectangle(IJ.getScreenSize());
			snapshotFrame.hide();
			Image image = robot.createScreenCapture(rectangle);
			snapshotFrame.show();
			if (image != null) {
				String name = getSnapshotName();
				ImagePlus imp = new ImagePlus(name, image);
				imp.show();
				if (putSnapshotsToBack)
					imp.getWindow().toBack();

				/* insert into editor */
				editor.getTextArea()
					.insert("[[Image:" + name + "]]\n",
					editor.getTextArea().getCaretPosition());
			}
		} catch (AWTException e) { /* ignore */ }
	}
}
