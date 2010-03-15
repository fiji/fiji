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
import javax.swing.JTextArea;

public class Wiki_Editor implements PlugIn, ActionListener {
	protected String name;

	protected static String URL = "http://pacific.mpi-cbg.de/wiki/";

	protected enum Mode { TUTORIAL_MAKER, NEWS, SCREENSHOT };
	protected Mode mode;
	protected ImagePlus screenshot;

	public void run(String arg) {
		String dialogTitle = "Tutorial Maker";
		String defaultTitle = "";
		String label = "Tutorial_title";
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
			label = "News_title";
		}
		else if (arg.equals("screenshot")) {
			screenshot = IJ.getImage();
			if (screenshot == null) {
				IJ.error("Which screenshot do you want to upload?");
				return;
			}
			mode = Mode.SCREENSHOT;
			dialogTitle = "Fiji Wiki Screenshot";
			defaultTitle = screenshot.getTitle().replace('_', ' ');
			int dot = defaultTitle.lastIndexOf('.');
			if (dot > 0)
				defaultTitle = defaultTitle.substring(0, dot);
			label = "Project_title";
		}
		else
			interceptRenames();

		GenericDialog gd = new GenericDialog(dialogTitle);
		gd.addStringField(label, defaultTitle, 20);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		name = gd.getNextString();
		if (name.length() == 0)
			return;
		if (mode != Mode.SCREENSHOT)
			name = capitalize(name).replace(' ', '_');
		else {
			new Prettify_Wiki_Screenshot().run(screenshot.getProcessor());
			screenshot = IJ.getImage();
			String imageTitle = name + "-snapshot.jpg";
			for (int i = 2; wikiHasImage(imageTitle); i++)
				imageTitle = name + "-snapshot-" + i + ".jpg";
			screenshot.setTitle(imageTitle);
		}

		addEditor();
	}

	protected static String capitalize(String string) {
		return string.substring(0, 1).toUpperCase()
			+ string.substring(1);
	}

	protected TextEditor editor;
	protected JMenuItem upload, preview, toBackToggle, renameImage,
		changeURL, insertPluginInfobox;

	protected void addEditor() {
		editor = new TextEditor(null);
		editor.getTextArea().setLineWrap(true);

		int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		JMenu menu = new JMenu("Wiki");
		menu.setMnemonic(KeyEvent.VK_W);
		upload = editor.addToMenu(menu, "Upload", KeyEvent.VK_U, ctrl);
		preview = editor.addToMenu(menu, "Preview", KeyEvent.VK_R, ctrl);
		if (mode == Mode.TUTORIAL_MAKER) {
			toBackToggle = editor.addToMenu(menu, "", 0, 0);
			renameImage = editor.addToMenu(menu, "Rename Image", KeyEvent.VK_I, ctrl);
			toBackToggleSetLabel();
			insertPluginInfobox = editor.addToMenu(menu,
					"Insert Plugin Infobox", 0, 0);
		}

		changeURL = editor.addToMenu(menu, "Change Wiki URL", 0, 0);

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

		String text = "", category = "";
		switch (mode) {
			case TUTORIAL_MAKER:
				text = "== " + name.replace('_', ' ') + " ==\n\n";
				category = "\n[[Category:Tutorials]]";
				break;
			case NEWS:
				category = "\n[[Category:News]]";
				break;
			case SCREENSHOT:
				try {
					text = getPageSource("Fiji:Featured_Projects");
				} catch (IOException e) {
					IJ.error("Could not get page source for '" + name + "'");
					return;
				}
				text += "\n* " + name + "|"
					+ screenshot.getTitle() + "\n"
					+ "The [[" + name + "]] plugin <describe the project here>\n";
				break;
		}
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
		editor.setTitle("Edit Wiki - " + name);
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
		else if (source == changeURL) {
			GenericDialog gd = new GenericDialog("Change URL");
			gd.addStringField("URL", URL, 40);
			gd.showDialog();
			if (!gd.wasCanceled()) {
				URL = gd.getNextString();
				int off = URL.indexOf("/index.php");
				if (off > 0)
					URL = URL.substring(0, off + 1);
				client = null;
			}
		}
		else if (source == insertPluginInfobox) {
			JTextArea textArea = editor.getTextArea();
			textArea.insert("{{Infobox Plugin\n"
				+ "| software               = ImageJ\n"
				+ "| name                   = \n"
				+ "| maintainer             = [mailto:author_at_example_dot_com A U Thor]\n"
				+ "| author                 = A U Thor\n"
				+ "| source                 = \n"
				+ "| released               = 15/06/2005\n"
				+ "| latest version         = 12/08/2009\n"
				+ "| status                 = \n"
				+ "| category               = [[:Category:Plugins]]\n"
				+ "| website                = \n"
				+ "}}\n", 0);
			textArea.insert("\n[[Category:Plugins]]",
				textArea.getDocument().getLength());
		}
	}

	protected boolean putSnapshotsToBack = true;

	protected void toBackToggleSetLabel() {
		toBackToggle.setLabel(putSnapshotsToBack ?
			"Leave snapshots in the foreground" :
			"Put snapshots into the background");
	}

	GraphicalMediaWikiClient client;


	protected void getClient() {
		if (client != null)
			return;
		client = new GraphicalMediaWikiClient(URL + "index.php");
	}

	protected void upload() {
		IJ.showStatus("Uploading " + name + "...");
		IJ.showProgress(0, 1);
		List<String> images = getImages();
		if (!saveOrUploadImages(null, images))
			return;

		getClient();

		if (!client.login("Wiki Login"))
			return;

		if (!saveOrUploadImages(client, images))
			return;

		String name = mode == Mode.SCREENSHOT ?
			"Fiji:Featured_Projects" : this.name;
		boolean result =
			client.uploadPage(name, getText(), "Add " + this.name);

		client.logOut();

		IJ.showStatus("Uploading " + name + " finished.");
		IJ.showProgress(1, 1);

		if (!result) {
			IJ.error("Could not upload!");
			return;
		}

		new BrowserLauncher().run(URL + "index.php?title= " + name);
		editor.dispose();
	}

	protected void preview() {
		IJ.showStatus("Previewing " + name + "...");
		IJ.showProgress(0, 2);

		List<String> images = getImages();
		if (!saveOrUploadImages(null, images))
			return;

		getClient();

		if (!client.login("Wiki Login (Preview)"))
			return;
		String name = mode == Mode.SCREENSHOT ?
			"Fiji:Featured_Projects" : this.name;
		String html = client.uploadOrPreviewPage(name, getText(),
				"Add " + this.name, true);
		client.logOut();

		if (html == null) {
			IJ.error("Could not parse response");
			return;
		}

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

	public String getPageSource(String title) throws IOException {
		String result = getPage(title, "edit");
		client.logOut();
		int offset = result.indexOf("id=\"wpTextbox1\"");
		if (offset < 0)
			return "";
		offset = result.indexOf('>', offset);
		if (offset < 0)
			return "";
		int endOffset = result.indexOf("</textarea>", offset);
		if (endOffset < 0)
			return "";
		return result.substring(offset + 1, endOffset);
	}

	/* This method must not log out */
	public String getPage(String title) throws IOException {
		return getPage(title, null);
	}

	public String getPage(String title, String action) throws IOException {
		getClient();
		String[] getVars = {
			"title", title
		};
		if (action != null)
			getVars = new String[] {
				"title", title,
				"action", action
			};
		String result = client.sendRequest(getVars, null);
		if (result == null || result.indexOf("Login Required") > 0 ||
				result.indexOf("Login required") > 0) {
			// Try after login
			getClient();
			if (!client.login("Login to view " + title))
				return null;
			result = client.sendRequest(getVars, null);
		}
		return result;
	}

	protected List<String> getImages() {
		List<String> result = new ArrayList<String>();
		if (mode == Mode.SCREENSHOT) {
			result.add(screenshot.getTitle());
			return result;
		}
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

	protected boolean saveOrUploadImages(GraphicalMediaWikiClient client,
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
				if (!client.login("Login to upload " + image))
					return false;
				if (!client.uploadFile(image, "Upload " + image
							+ " for " + name,
							new File(info.directory,
								info.fileName))
						&& !wikiHasImage(image))
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
			String html = getPage("Image:" + image);
			boolean hasFile =
				html.indexOf("No file by this name exists") < 0;
			if (hasFile)
				System.err.println("has image: " + html);
			return hasFile;
		} catch (IOException e) {
			IJ.error("Could not retrieve image " + image + ": "
					+ e.getMessage());
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
			String text = editor.getTextArea().getText();
			String transformed = text.replaceAll("\\[\\[Image:"
					+ oldTitle.replaceAll("\\.", "\\\\.")
					+ "(?=[]|])",
				"[[Image:" + newTitle);
			if (!text.equals(transformed)) {
				int pos = editor.getTextArea()
					.getCaretPosition();
				editor.getTextArea().setText(transformed);
				try {
					editor.getTextArea()
						.setCaretPosition(pos);
				} catch (Exception e) { /* ignore */ }
			}
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
				int p = editor.getTextArea().getCaretPosition();
				String insert = "[[Image:" + name + "]]\n";
				editor.getTextArea().insert(insert, p);
				p += insert.length();
				editor.getTextArea().setCaretPosition(p);
			}
		} catch (AWTException e) { /* ignore */ }
	}
}
