package fiji.updater.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import javax.swing.event.DocumentListener;

// Helper functions to instantiated Swing components
public class SwingTools {
	public static JTabbedPane tab(Component component,
			String title, String tooltip,
			int width, int height, Container addTo) {
		JPanel tab = new JPanel();
		tab.setLayout(new BorderLayout());
		tab.add(scrollPane(component, width, height, null),
				BorderLayout.CENTER);

		JTabbedPane tabbed = new JTabbedPane();
		tabbed.addTab(title, null, tab, tooltip);
		tabbed.setPreferredSize(new Dimension(width,height));
		if (addTo != null)
			addTo.add(tabbed);
		return tabbed;
	}

	public static JScrollPane scrollPane(Component component,
			int width, int height, Container addTo) {
		JScrollPane scroll = new JScrollPane(component);
		scroll.getViewport().setBackground(component.getBackground());
		scroll.setPreferredSize(new Dimension(width, height));
		if (addTo != null)
			addTo.add(scroll);
		return scroll;
	}

	public static JPanel label(String text, Container addTo) {
		JLabel label = new JLabel(text, SwingConstants.LEFT);
		JPanel panel = horizontalPanel();
		panel.add(label);
		panel.add(Box.createHorizontalGlue());
		if (addTo != null)
			addTo.add(panel);
		return panel;
	}

	public static JPanel boxLayoutPanel(int alignment) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, alignment));
		return panel;
	}

	public static JPanel horizontalPanel() {
		return boxLayoutPanel(BoxLayout.X_AXIS);
	}

	public static JPanel verticalPanel() {
		return boxLayoutPanel(BoxLayout.Y_AXIS);
	}

	public static JButton button(String title, String toolTip,
			ActionListener listener, Container addTo) {
		JButton button = new JButton(title);
		button.setToolTipText(toolTip);
		button.addActionListener(listener);
		if (addTo != null)
			addTo.add(button);
		return button;
	}

	public static JPanel labelComponent(String text, JComponent component,
			Container addTo) {
		JPanel panel = horizontalPanel();
		JLabel label = new JLabel(text, SwingConstants.LEFT);
		panel.add(label);
		panel.add(Box.createRigidArea(new Dimension(10,0)));
		panel.add(component);
		if (addTo != null)
			addTo.add(panel);
		return panel;
	}

	/** Uses a GridBagLayout to prevent odd resizing behaviours. */
	public static JPanel labelComponentRigid(String text, JComponent component) {
		JPanel panel = new JPanel();
		GridBagLayout gb = new GridBagLayout();
		panel.setLayout(gb);
		GridBagConstraints c = new GridBagConstraints(0, 0,  // x, y
				                              1, 3,  // rows, cols
							      0, 0,  // weightx, weighty
							      GridBagConstraints.WEST, // anchor
							      GridBagConstraints.NONE, // fill
							      new Insets(0,0,0,0),
							      0, 0); // ipadx, ipady
		JLabel label = new JLabel(text, SwingConstants.LEFT);
		gb.setConstraints(label, c);
		panel.add(label);

		Component box = Box.createRigidArea(new Dimension(10,0));
		c.gridx = 1;
		gb.setConstraints(box, c);
		panel.add(box);

		c.gridx = 2;
		c.weightx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		gb.setConstraints(component, c);
		panel.add(component);

		return panel;
	}

	public static JTextPane scrolledText(int width, int height,
			String text, DocumentListener listener,
			Container addTo) {
		JTextPane component = new JTextPane();
		component.getDocument().addDocumentListener(listener);
		if (text != null)
			component.setText(text);
		component.setSelectionStart(0);
		component.setSelectionEnd(0);
		scrollPane(component, width, height, addTo);
		return component;
	}

	public static JTextPane scrolledText(int width, int height,
			Iterable<String> list, DocumentListener listener,
			Container addTo) {
		StringBuilder builder = new StringBuilder();
		for (String text : list)
			builder.append(text + "\n");
		return scrolledText(width, height, builder.toString(),
				listener, addTo);
	}

	/**
	 * Add a keyboard accelerator to a container.
	 *
	 * This method adds a keystroke to the input map of a container that
	 * sends an action event with the given source to the given listener.
	 */
        public static void addAccelerator(final Component source,
			final JComponent container,
			final ActionListener listener, int key, int modifiers) {
                container.getInputMap(container.WHEN_IN_FOCUSED_WINDOW)
			.put(KeyStroke.getKeyStroke(key, modifiers), source);
                if (container.getActionMap().get(source) != null)
                        return;
                container.getActionMap().put(source,
                                new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                                if (!source.isEnabled())
                                        return;
                                ActionEvent event = new ActionEvent(source,
                                        0, "Accelerator");
                                listener.actionPerformed(event);
                        }
                });
        }
}
