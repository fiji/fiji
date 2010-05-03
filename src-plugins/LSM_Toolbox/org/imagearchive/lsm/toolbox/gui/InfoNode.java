package org.imagearchive.lsm.toolbox.gui;

import javax.swing.tree.DefaultMutableTreeNode;

public class InfoNode extends DefaultMutableTreeNode{

	public Object data;

	public String title;

	public InfoNode(String title, Object data) {
		super(data);
        this.title = title;
		this.data = data;
	}

	public String toString() {
		return title;
	}
}
