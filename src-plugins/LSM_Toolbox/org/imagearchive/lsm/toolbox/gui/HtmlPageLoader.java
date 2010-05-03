package org.imagearchive.lsm.toolbox.gui;

import java.awt.Cursor;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;

public class HtmlPageLoader extends Thread {

	private Object[][] pages;

	private JDialog c;

	public HtmlPageLoader(JDialog c, Object[][] pages) {
		this.pages = pages;
		this.c = c;
	}

	public void run() {
		c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		for (int i = 0; i < pages.length; i++) {
			try {
				URL source = getClass().getResource((String)pages[i][1]);
				((JEditorPane)pages[i][0]).setPage(source);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(c, "Could not load page. Jar must be corrupted",
						"Warning", JOptionPane.WARNING_MESSAGE);
				System.err.println(e.getMessage());
			}
		}
		((JEditorPane)pages[0][0]).validate();
		c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
}
