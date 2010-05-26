package ij3d.pointlist;

import java.awt.Component;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.BorderLayout;
import java.awt.ScrollPane;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

/**
 * This class represents a window which can hold a set of PointListPanels. These
 * panels can dynamically be added and removed from the window.
 * @author Benjamin Schmid
 *
 */
public class PointListDialog extends Dialog {

	/** The constraints for the layout. */
	private GridBagConstraints c;

	/** The layout itself */
	private GridBagLayout gridbag;

	/** The parent panel of the PointListPanels */
	private Panel panel;

	/**
	 * An additional panel which is layed out at the bottom of the window
	 * and may be used for showing some buttons.
	 */
	private Panel extraPanel;

	/**
	 * Constructs an empty PointListDialog
	 * @param owner
	 */
	public PointListDialog(Frame owner) {
		super(owner, "Point list");
		panel = new Panel();
		gridbag = new GridBagLayout();
		panel.setLayout(gridbag);

		panel.setBackground(Color.WHITE);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weighty = 0.1f;
		c.fill = GridBagConstraints.NONE;
		ScrollPane scroll = new ScrollPane();
		scroll.add(panel);
		add(scroll);
	}

	/**
	 * Adds the specified PointListPanel. The window is made visible.
	 * @param name
	 * @param plp
	 */
	public void addPointList(String name, PointListPanel plp) {
		if(!containsPointList(plp)) {
			plp.setName(name);
			gridbag.setConstraints(plp, c);
			panel.add(plp);
			c.gridx++;
			// if not displayed yet, do so now.
			if(!isVisible()) {
				setSize(250, 200);
				setVisible(true);
			}
		}
	}

	/**
	 * Removes the specified PointListPanel and hides the window, if it
	 * was the last one.
	 * @param plp
	 */
	public void removePointList(PointListPanel plp) {
		if(containsPointList(plp)) {
			panel.remove(plp);
			c.gridx--;
			// hide if it is empty
			if(panel.getComponentCount() == 0)
				setVisible(false);
		}
	}

	/**
	 * Returns true if the specified PointListPanel is already displayed
	 * @param plp
	 * @return
	 */
	public boolean containsPointList(PointListPanel plp) {
		Component[] c = panel.getComponents();
		for(int i = 0; i < c.length; i++) {
			if(c[i] == plp)
				return true;
		}
		return false;
	}

	/**
	 * Displays an optional panel at the bottom of the window. This may
	 * be used for showing a panel with some buttons, for example.
	 * @param p
	 */
	public void addPanel(Panel p) {
		if(extraPanel != null)
			remove(extraPanel);
		extraPanel = p;
		add(p, BorderLayout.SOUTH);
		update();
	}

	/**
	 * Remove the optional panel at the bottom of the window (if there is
	 * one).
	 */
	public void removeExtraPanel() {
		if(extraPanel != null) {
			remove(extraPanel);
			extraPanel = null;
			update();
		}
	}

	/**
	 * Update the layout.
	 */
	public void update() {
 		validateTree();
	}
}

