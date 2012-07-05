package org.imagearchive.lsm.toolbox.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;

public class GUIMaker {

	public static Container addComponentToGrid(Component component,
			Container container, int x, int y, int width, int height, int fill, int anchor,
			double weightx, double weighty) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.gridwidth = width;
		constraints.gridheight = height;
		constraints.weightx = weightx;
		constraints.weighty = weighty;
		constraints.anchor = anchor;
		constraints.fill = fill;
		container.add(component, constraints);
		return container;
	}

}
