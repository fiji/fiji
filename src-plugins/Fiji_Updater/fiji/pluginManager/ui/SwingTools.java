package fiji.pluginManager.ui;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentListener;

//Specialized functions to ease repetitive creation of Swing components
public class SwingTools {
	//Created a tabbed pane with a single tab that contains a single component
	public static JTabbedPane getSingleTabbedPane(JTextPane textpane, String title, String tooltip,
			int width, int height, JPanel addTo) {
		JPanel tabPane = new JPanel();
		tabPane.setLayout(new BorderLayout());
		tabPane.add(getTextScrollPane(textpane, width, height, null), BorderLayout.CENTER);

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab(title, null, tabPane, tooltip);
		tabbedPane.setPreferredSize(new Dimension(width,height));
		if (addTo != null)
			addTo.add(tabbedPane);
		return tabbedPane;
	}

	//Creates a JScrollPane for the given textpane of specified width and height
	public static JScrollPane getTextScrollPane(JTextPane textPane, int width, int height, JPanel addTo) {
		textPane.setPreferredSize(new Dimension(width, height));
		JScrollPane scrollpane = new JScrollPane(textPane);
		scrollpane.getViewport().setBackground(textPane.getBackground());
		scrollpane.setPreferredSize(new Dimension(width, height));
		if (addTo != null)
			addTo.add(scrollpane);

		return scrollpane;
	}

	//Creates a JPanel with a label that sticks to the left
	public static JPanel createLabelPanel(String text, JPanel addTo) {
		JLabel label = new JLabel(text, SwingConstants.LEFT);
		JPanel lblPanel = createBoxLayoutPanel(BoxLayout.X_AXIS);
		lblPanel.add(label);
		lblPanel.add(Box.createHorizontalGlue());
		if (addTo != null)
			addTo.add(lblPanel);

		return lblPanel;
	}

	public static JPanel createBoxLayoutPanel(int alignment) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, alignment));
		return panel;
	}

	public static JButton createButton(String buttonTitle, String toolTipText,
			ActionListener listener, JPanel addTo) {
		JButton btn = new JButton(buttonTitle);
		btn.setToolTipText(toolTipText);
		btn.addActionListener(listener);
		if (addTo != null)
			addTo.add(btn);
		return btn;
	}

	public static JPanel createLabelledComponent(String text, JComponent component, JPanel addTo) {
		JPanel panel = SwingTools.createBoxLayoutPanel(BoxLayout.X_AXIS);
		JLabel label = new JLabel(text, SwingConstants.LEFT);
		panel.add(label);
		panel.add(Box.createRigidArea(new Dimension(10,0)));
		panel.add(component);
		if (addTo != null)
			addTo.add(panel);
		return panel;
	}

	public static JScrollPane getTextScrollPane(JTextPane textPane, int width, int height, String contents,
			DocumentListener listener, JPanel addTo) {
		textPane.getDocument().addDocumentListener(listener);
		JScrollPane scrollpane = SwingTools.getTextScrollPane(textPane, width, height, addTo);
		if (contents != null)
			textPane.setText(contents);
		textPane.setSelectionStart(0);
		textPane.setSelectionEnd(0);
		return scrollpane;
	}

	public static JScrollPane getTextScrollPane(JTextPane textPane, int width, int height,
			List<String> contentList, DocumentListener listener, JPanel addTo) {
		textPane.getDocument().addDocumentListener(listener);
		JScrollPane scrollpane = SwingTools.getTextScrollPane(textPane, width, height, addTo);
		if (contentList != null && contentList.size() > 0) {
			String contents = "";
			for (String value : contentList)
				contents += value + "\n";
			textPane.setText(contents);
			textPane.setSelectionStart(0);
			textPane.setSelectionEnd(0);
		}
		return scrollpane;
	}

}