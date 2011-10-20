/**
 * This class implements a base class for a menu item finder
 * similar to the Plugins>Utilities>Find Commands command in
 * ImageJ, on whose source code it is based.
 *
 *  @author Mark Longair <mark-imagej@longair.net>
 *  @author Johannes Schindelin <johannes.schindelin@gmx.de>
 */
package fiji.util.gui;

import fiji.util.Levenshtein;

import ij.IJ;
import ij.WindowManager;

import ij.text.TextWindow;

import java.awt.BorderLayout;
import java.awt.Container;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class CommandFinderBase extends JFrame implements ActionListener, WindowListener, KeyListener, ItemListener, MouseListener {
	protected JTextField prompt;
	protected JList completions;
	protected JScrollPane scrollPane;
	protected DefaultListModel completionsModel;
	protected JButton runButton, closeButton, exportButton;
	protected JCheckBox fullInfoCheckBox, fuzzyCheckBox, closeCheckBox;
	protected List<Action> actions;

	public CommandFinderBase(String title) {
		super(title);

		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		addWindowListener(this);

		fullInfoCheckBox = new JCheckBox("Show full information", false);
		fullInfoCheckBox.addItemListener(this);
		fullInfoCheckBox.addKeyListener(this);
		fuzzyCheckBox = new JCheckBox("Fuzzy matching", false);
		fuzzyCheckBox.addItemListener(this);
		fuzzyCheckBox.addKeyListener(this);
		closeCheckBox = new JCheckBox("Close when running", true);
		fuzzyCheckBox.addItemListener(this);
		fuzzyCheckBox.addKeyListener(this);

		JPanel northPanel = new JPanel();

		northPanel.add(new JLabel("Type part of a command:"));

		prompt = new JTextField("", 30);
		prompt.getDocument().addDocumentListener(new PromptDocumentListener());
		prompt.addKeyListener(this);

		northPanel.add(prompt);

		contentPane.add(northPanel, BorderLayout.NORTH);

		completionsModel = new DefaultListModel();
		completions = new JList(completionsModel);
		scrollPane = new JScrollPane(completions);

		completions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		completions.setLayoutOrientation(JList.VERTICAL);

		completions.setVisibleRowCount(20);
		completions.addKeyListener(this);

		contentPane.add(scrollPane, BorderLayout.CENTER);
		// Add a mouse listener so we can detect double-clicks
		completions.addMouseListener(this);

		runButton = new JButton("Run");
		exportButton = new JButton("Export");
		closeButton = new JButton("Close");

		runButton.addActionListener(this);
		exportButton.addActionListener(this);
		closeButton.addActionListener(this);
		runButton.addKeyListener(this);
		closeButton.addKeyListener(this);

		JPanel southPanel = new JPanel();
		southPanel.setLayout(new BorderLayout());

		JPanel optionsPanel = new JPanel();
		optionsPanel.add(fullInfoCheckBox);
		optionsPanel.add(fuzzyCheckBox);
		optionsPanel.add(closeCheckBox);

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.add(runButton);
		buttonsPanel.add(exportButton);
		buttonsPanel.add(closeButton);

		southPanel.add(optionsPanel, BorderLayout.CENTER);
		southPanel.add(buttonsPanel, BorderLayout.SOUTH);

		contentPane.add(southPanel, BorderLayout.SOUTH);

		pack();
	}

	/**
	 * This function is required to populate the list of actions
	 */
	public abstract void populateActions();

	/**
	 * Override this function to have your own error box that does not
	 * focus on the ImageJ window.
	 */
	public void error(String message) {
		IJ.error(message);
	}

	protected abstract class Action {
		protected String label;
		protected String menuLocation;

		public Action(String label, String menuLocation) {
			this.label = label;
			this.menuLocation = menuLocation;
		}

		public abstract void run();

		public String getExtraInformation() {
			if (!showFullInformation())
				return "";
			return " (" + menuLocation + ")";
		}

		public String toString() {
			return label + getExtraInformation();
		}
	}

	protected boolean showFullInformation() {
		return fullInfoCheckBox.isSelected();
	}

	protected boolean fuzzyMatching() {
		return fuzzyCheckBox.isSelected();
	}

	protected boolean closeWhenRunning() {
		return closeCheckBox.isSelected();
	}

	protected void populateList(String matchingSubstring) {
		String substring = matchingSubstring.toLowerCase();
		completionsModel.removeAllElements();
		if (fuzzyMatching())
			populateListFuzzily(substring, showFullInformation());
		else
			for (Action action : actions)
				if (action.label.toLowerCase().indexOf(substring) >= 0 )
					completionsModel.addElement(action);
	}

	protected static class LevenshteinPair implements Comparable<LevenshteinPair> {
		int index, cost;

		public LevenshteinPair(int index, int cost) {
			this.index = index;
			this.cost = cost;
		}

		public int compareTo(LevenshteinPair other) {
			return cost - other.cost;
		}
	}

	protected void populateListFuzzily(String substring, boolean fullInfo) {
		Levenshtein levenshtein = new Levenshtein(0, 10, 1, 5, 0, 0);
		LevenshteinPair[] pairs = new LevenshteinPair[actions.size()];
		for (int i = 0; i < actions.size(); i++) {
			int cost = levenshtein.cost(substring,
					actions.get(i).label.toLowerCase());
			pairs[i] = new LevenshteinPair(i, cost);
		}

		Arrays.sort(pairs);

		for (int i = 0; i < pairs.length && i < 50; i++)
			completionsModel.addElement(actions.get(pairs[i].index));
	}
	void export() {
		StringBuffer sb = new StringBuffer(5000);
		for (int i=0; i<completionsModel.size(); i++) {
			sb.append(i);
			sb.append("\t");
			sb.append(completionsModel.elementAt(i).toString());
			sb.append("\n");
		}
		TextWindow tw = new TextWindow("Menu Item Labels", " \tCommand", sb.toString(), 600, 500);
	}

	public void run(Action action) {
		if (action == null)
			return;
		action.run();
		if (closeWhenRunning())
			dispose();
	}

	public void run(int itemIndex) {
		if (itemIndex < 0)
			return;
		run((Action)completionsModel.elementAt(itemIndex));
	}

	public void runSelected() {
		run((Action)completions.getSelectedValue());
	}

	@Override
	public void setVisible(boolean visible) {
		/*
		 * We do have to populate the actions here, otherwise the
		 * caller cannot initialize prerequisites in the constructor.
		 */
		if (actions == null) {
			actions = new ArrayList<Action>();
			populateActions();
			populateList("");
		}

		if (visible)
			WindowManager.addWindow(this);
		super.setVisible(visible);
	}

	@Override
	public void dispose() {
		WindowManager.removeWindow(this);
		super.dispose();
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		Object source = ae.getSource();
		if (source == runButton) {
			int selected = completions.getSelectedIndex();
			if (selected < 0) {
				error("Please select a command to run");
				return;
			}
			run(selected);
		}
		else if (source == exportButton)
			export();
		else if (source == closeButton)
			dispose();
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		Object source = ie.getSource();
		int index = completions.getSelectedIndex();
		populateList(prompt.getText());
		if (index >= 0)
			completions.setSelectedIndex(index);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() > 1)
			runSelected();
	}

	@Override
	public void mousePressed(MouseEvent e) {}
	@Override
	public void mouseReleased(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void keyPressed(KeyEvent ke) {
		int key = ke.getKeyCode();
		int items = completionsModel.getSize();
		Object source = ke.getSource();
		if (key == KeyEvent.VK_ESCAPE)
			dispose();
		else if (source == prompt) {
			/*
			 * If you hit enter in the text field, and there
			 * is only one command that matches, run it.
			 */
			if (key == KeyEvent.VK_ENTER) {
				if (1 == items)
					run(0);
				return;
			}

			/*
			 * If you hit the up or down arrows in the
			 * text field, move the focus to the
			 * completions list and select the item at the
			 * bottom or top of that list.
			 */
			int index = -1;
			if (key == KeyEvent.VK_UP) {
				index = completions.getSelectedIndex() - 1;
				if (index < 0)
					index = items - 1;
			}
			else if (key == KeyEvent.VK_DOWN) {
				index = completions.getSelectedIndex() + 1;
				if (index >= items)
					index = Math.min(items - 1, 0);
			}
			else if (key == KeyEvent.VK_PAGE_DOWN)
				index = completions.getLastVisibleIndex();
			if (index >= 0) {
				completions.requestFocus();
				completions.ensureIndexIsVisible(index);
				completions.setSelectedIndex(index);
			}
		}
		else if (key == KeyEvent.VK_BACK_SPACE)
			/*
			 * If someone presses backspace they probably want to
			 * remove the last letter from the search string, so
			 * switch the focus back to the prompt.
			 */
			prompt.requestFocus();
		else if (source == completions) {
			/*
			 * If you hit Return with the focus in the
			 * completions list, run the selected command.
			 */
			if (key == KeyEvent.VK_ENTER)
				runSelected();
			else if (key == KeyEvent.VK_UP) {
				if (completions.getSelectedIndex() <= 0) {
					completions.clearSelection();
					prompt.requestFocus();
				}
			}
			else if (key == KeyEvent.VK_DOWN) {
				if (completions.getSelectedIndex() == items-1) {
					completions.clearSelection();
					prompt.requestFocus();
				}
			}
		}
		else if (source == runButton) {
			if (key == KeyEvent.VK_ENTER)
				runSelected();
		}
		else if (source == closeButton) {
			if (key == KeyEvent.VK_ENTER)
				dispose();
		}
	}

	@Override
	public void keyReleased(KeyEvent ke) { }
	@Override
	public void keyTyped(KeyEvent ke) { }

	protected class PromptDocumentListener implements DocumentListener {
		@Override
		public void insertUpdate(DocumentEvent e) {
			populateList(prompt.getText());
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			populateList(prompt.getText());
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			populateList(prompt.getText());
		}
	}

	/* Make sure that clicks on the close icon close the window: */

	@Override
	public void windowClosing(WindowEvent e) {
		dispose();
	}

	@Override
	public void windowActivated(WindowEvent e) { }
	@Override
	public void windowDeactivated(WindowEvent e) { }
	@Override
	public void windowClosed(WindowEvent e) { }
	@Override
	public void windowOpened(WindowEvent e) { }
	@Override
	public void windowIconified(WindowEvent e) { }
	@Override
	public void windowDeiconified(WindowEvent e) { }
}