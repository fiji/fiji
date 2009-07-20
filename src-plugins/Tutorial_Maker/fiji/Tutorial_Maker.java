/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2009 Mark Longair, Johannes Schindelin */

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

import java.awt.AWTException;
import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.TextArea;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.GenericDialog;

import ij.plugin.PlugIn;

import ij.plugin.frame.Editor;

public class Tutorial_Maker implements PlugIn {
	protected String name;

	public static void main(String[] args) {
		MediaWikiClient client =
			new MediaWikiClient("http://localhost/wiki/index.php");
		client.logIn("Schindelin", "test");
		System.err.println(client.isLoggedIn());
		client.uploadPage("Hello", "= Hello =", "test");
		client.uploadFile("b123.png", "hehe",
				new java.io.File("fiji.png"));
		client.logOut();
	}

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Tutorial Maker");
		gd.addStringField("Tutorial_title", "", 20);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		name = gd.getNextString();
		if (name.isEmpty())
			return;

		showSnapshotFrame();
		addEditor();
	}

	protected Editor editor;

	protected void addEditor() {
		editor = new Editor(25, 120, 14, Editor.MENU_BAR);

		Menu menu = new Menu("Wiki");
		MenuItem upload = new MenuItem("Upload");
		upload.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				upload();
			}
		});
		// TODO: preview (using bliki?)

		menu.add(upload);
		editor.getMenuBar().add(menu);

		editor.addWindowListener(new WindowAdapter() {
			public void windowClosed() {
				// TODO: this seems not to be called.  Why?
				if (snapshotFrame != null)
					snapshotFrame.dispose();
			}
		});

		editor.create("Edit Wiki - " + name, "== " + name + " ==\n\n");
		editor.getTextArea().setCaretPosition(Integer.MAX_VALUE);

		MenuBar menuBar = editor.getMenuBar();
		for (int i = menuBar.getMenuCount() - 1; i >= 0; i--) {
			String label = menuBar.getMenu(i).getLabel();
			if (label.equals("Macros") || label.equals("Debug"))
				menuBar.remove(i);
		}
	}

	protected void upload() {
		IJ.error("TODO!");
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
				imp.getWindow().toBack();

				/* insert into editor */
				editor.append("[Image:" + name + "]\n");
				/* TODO: add listeners to track renames? */
			}
		} catch (AWTException e) { /* ignore */ }
	}
}
