package fiji.scripting;

import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SpringLayout;
import javax.swing.Spring;
import javax.swing.JLabel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Container;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.BorderFactory;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;


public class FindAndReplaceDialog extends JDialog implements ActionListener {
	JCheckBox regexCB;
	JTextField searchField;
	JCheckBox matchCaseCB;
	JCheckBox markAllCB;
	JCheckBox wholeWordCB;
	JTextField replaceField;
	boolean ifReplace;
	boolean forward = true;
	boolean matchCase, wholeWord, markAll, regex;
	ComponentOrientation orientation;
	RSyntaxTextArea textArea;
	JPanel contentPane, enterTextPane, temp, temp2;
	JLabel findFieldLabel, replaceFieldLabel;
	JButton findNextButton, replaceButton, replaceAllButton, cancelButton;

	public FindAndReplaceDialog(TextEditor editor, RSyntaxTextArea textArea, boolean isReplace) {
		super(editor);
		this.textArea = textArea;
		orientation = ComponentOrientation.getOrientation(getLocale());
		ifReplace = isReplace;
		regexCB = new JCheckBox("Regex");
		matchCaseCB = new JCheckBox("Match Case");
		markAllCB = new JCheckBox("Mark All");
		wholeWordCB = new JCheckBox("Whole Word");
		enterTextPane = new JPanel(new SpringLayout());
		contentPane = new JPanel(new BorderLayout());
		searchField = new JTextField();
		createFieldLabel(searchField, findFieldLabel, "Find Next", temp);
		if (ifReplace) {
			replaceField = new JTextField();
			createFieldLabel(replaceField, replaceFieldLabel, "Replace with", temp2);

		}

		makeSpringCompactGrid(enterTextPane, ifReplace ? 2 : 1, 2,	0, 0, 6, 6);
		JPanel bottomPanel = new JPanel(new BorderLayout());
		temp = new JPanel(new BorderLayout());
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		JPanel searchConditionsPanel = new JPanel();
		searchConditionsPanel.setLayout(new BorderLayout());
		JPanel temp1 = new JPanel();
		temp1.setLayout(new BoxLayout(temp1, BoxLayout.PAGE_AXIS));
		temp1.add(matchCaseCB);
		temp1.add(markAllCB);
		searchConditionsPanel.add(temp1, BorderLayout.LINE_START);
		temp1 = new JPanel();
		temp1.setLayout(new BoxLayout(temp1, BoxLayout.PAGE_AXIS));
		temp1.add(regexCB);
		temp1.add(wholeWordCB);
		searchConditionsPanel.add(temp1, BorderLayout.LINE_END);
		temp.add(searchConditionsPanel, BorderLayout.LINE_START);
		bottomPanel.add(temp, BorderLayout.LINE_START);

		// Now, make a panel containing all the above stuff.
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.add(enterTextPane);
		leftPanel.add(bottomPanel);

		// Make a panel containing the action buttons.
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(ifReplace ? 4 : 2, 1, 5, 5));
		findNextButton = new JButton("Find Next");
		findNextButton.addActionListener(this);
		buttonPanel.add(findNextButton);
		if (ifReplace) {
			createButton(buttonPanel, replaceButton, "Replace");
			createButton(buttonPanel, replaceAllButton, "Replace All");
		}
		createButton(buttonPanel, cancelButton, "Cancel");
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BorderLayout());
		rightPanel.add(buttonPanel, BorderLayout.NORTH);

		// Put everything into a neat little package.


		contentPane.add(leftPanel);
		contentPane.add(rightPanel, BorderLayout.LINE_END);
		temp = new JPanel(new BorderLayout());
		temp.add(contentPane, BorderLayout.NORTH);
		setContentPane(temp);
		getRootPane().setDefaultButton(findNextButton);
		setTitle(ifReplace ? "Replace" : "Find");
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		applyComponentOrientation(orientation);
	}

	private void handleOrientation(JPanel textPane, JPanel panel, JLabel fieldLabel) {
		if (orientation.isLeftToRight()) {
			textPane.add(fieldLabel);
			textPane.add(panel);
			contentPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 5));
		} else {
			textPane.add(panel);
			textPane.add(fieldLabel);
			contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
		}
	}

	private void createFieldLabel(JTextField field, JLabel label, String labelName, JPanel panel) {
		panel = new JPanel(new BorderLayout());
		label = new JLabel(labelName);
		panel.add(field);
		panel.setBorder(BorderFactory.createLineBorder(Color.black));
		handleOrientation(enterTextPane, panel, label);
	}

	private void createButton(JPanel panel, JButton button, String name) {
		button = new JButton(name);
		button.addActionListener(this);
		panel.add(button);
	}

	public void actionPerformed(ActionEvent e) {

		String action = e.getActionCommand();
		if (action.equals("Cancel")) {
			this.dispose();
		}
		if (prepareForFindReplace() == 0) return;
		if (action.equals("Find Next")) {
			boolean found = SearchEngine.find(textArea, searchField.getText(), forward, matchCase, wholeWord, regex);
			messageAtEnd(found);
		}

		if (action.equals("Replace")) {
			boolean replace = SearchEngine.replace(textArea, searchField.getText(), replaceField.getText(), forward, matchCase, wholeWord, regex);
			messageAtEnd(replace);
		}

		if (action.equals("Replace All")) {
			int replace = SearchEngine.replaceAll(textArea, searchField.getText(), replaceField.getText(), matchCase, wholeWord, regex);
			JOptionPane.showMessageDialog(this, replace + " replacements made!");
		}


	}

	private int prepareForFindReplace() {
		if (searchField.getText().length() == 0) {
			return 0;
		}
		setOptions();
		return 1;
	}

	private void setOptions() {
		matchCase = matchCaseCB.isSelected();
		wholeWord = wholeWordCB.isSelected();
		markAll = markAllCB.isSelected();
		regex = regexCB.isSelected();
	}

	private void messageAtEnd(boolean value) {
		if (!value) {
			JOptionPane.showMessageDialog(this, "Fiji has finished searching the document");
			textArea.setCaretPosition(0);
		}
	}

	public boolean isReplace() {
		return ifReplace;
	}
	public final void makeSpringCompactGrid(Container parent, int rows,
	                                        int cols, int initialX, int initialY,
	                                        int xPad, int yPad) {

		SpringLayout layout;
		try {
			layout = (SpringLayout)parent.getLayout();
		} catch (ClassCastException cce) {
			System.err.println("The first argument to makeCompactGrid " +
			                   "must use SpringLayout.");
			return;
		}

		Spring x = Spring.constant(initialX);
		for (int c = 0; c < cols; c++) {
			Spring width = Spring.constant(0);
			for (int r = 0; r < rows; r++) {
				width = Spring.max(width,
				                   getConstraintsForCell(
				                           r, c, parent, cols).getWidth());
			}
			for (int r = 0; r < rows; r++) {
				SpringLayout.Constraints constraints =
				        getConstraintsForCell(r, c, parent, cols);
				constraints.setX(x);
				constraints.setWidth(width);
			}
			x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
		}
		Spring y = Spring.constant(initialY);
		for (int r = 0; r < rows; r++) {
			Spring height = Spring.constant(0);
			for (int c = 0; c < cols; c++) {
				height = Spring.max(height,
				                    getConstraintsForCell(r, c, parent, cols).getHeight());
			}
			for (int c = 0; c < cols; c++) {
				SpringLayout.Constraints constraints =
				        getConstraintsForCell(r, c, parent, cols);
				constraints.setY(y);
				constraints.setHeight(height);
			}
			y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
		}
		SpringLayout.Constraints pCons = layout.getConstraints(parent);
		pCons.setConstraint(SpringLayout.SOUTH, y);
		pCons.setConstraint(SpringLayout.EAST, x);

	}

	private final SpringLayout.Constraints getConstraintsForCell(
	        int row, int col,
	        Container parent, int cols) {
		SpringLayout layout = (SpringLayout) parent.getLayout();
		Component c = parent.getComponent(row * cols + col);
		return layout.getConstraints(c);
	}


}





