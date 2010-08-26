package fiji.util;

import ij.CommandListener;
import ij.Executer;
import ij.IJ;
import ij.Menus;
import ij.Prefs;

import ij.gui.GenericDialog;

import ij.plugin.PlugIn;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class Recent_Commands implements ActionListener, CommandListener, KeyListener, ListSelectionListener, PlugIn {
	protected static int recentListSize = 8;
	protected static int frequentListSize = 8;
	protected static int maxLRUSize = 100;
	protected static boolean suppressRepeatedCommands = true;
	protected final static String PREFS_KEY = "recent.command";

	public void run(String arg) {
		readPrefs();
		if ("install".equals(arg))
			install();
		else
			runInteractively();
	}

	public void install() {
		Executer.addCommandListener(this);
		if (IJ.getInstance() != null)
			Menus.installPlugin(getClass().getName(),
				Menus.SHORTCUTS_MENU, "*recent commands", "9",
				IJ.getInstance());
	}

	JDialog dialog;
	JList mostRecent, mostFrequent;
	JButton okay, cancel, options;

	public void runInteractively() {
		Vector recent = getMostRecent(recentListSize);
		if (recent.size() == 0) {
			JOptionPane.showMessageDialog(IJ.getInstance(),
				"No recent commands available!");
			return;
		}

		mostRecent = makeJList(recent);
		mostFrequent = makeJList(getMostFrequent(frequentListSize));
		mostRecent.setSelectedIndex(0);
		mostFrequent.clearSelection();

		dialog = new JDialog(IJ.getInstance(), "Recent Commands", true);
		dialog.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = c.HORIZONTAL;
		c.gridy = c.gridx = 0;
		dialog.add(new JLabel("Recent Commands:"), c);
		c.gridy++; c.gridx = 0;
		dialog.add(mostRecent, c);
		c.gridy++; c.gridx = 0;
		dialog.add(new JLabel("Frequently used Commands:"), c);
		c.gridy++; c.gridx = 0;
		dialog.add(mostFrequent, c);
		okay = new JButton("OK");
		okay.addActionListener(this);
		okay.addKeyListener(this);
		dialog.getRootPane().setDefaultButton(okay);
		cancel = new JButton("Cancel");
		cancel.addActionListener(this);
		cancel.addKeyListener(this);
		options = new JButton("Options");
		options.addActionListener(this);
		options.addKeyListener(this);
		JPanel panel = new JPanel();
		panel.add(okay);
		panel.add(cancel);
		panel.add(options);
		c.gridy++; c.gridx = 0;
		dialog.add(panel, c);
		dialog.pack();
		dialog.setVisible(true);
	}

	JList makeJList(Vector items) {
		JList list = new JList(items);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(this);
		list.addKeyListener(this);
		list.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					runSelectedCommand();
					dialog.dispose();
				}
			}
		});
		return list;
	}

	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource() == mostRecent) {
			if (mostRecent.getSelectedIndex() >= 0)
				mostFrequent.clearSelection();
		}
		else {
			if (mostFrequent.getSelectedIndex() >= 0)
				mostRecent.clearSelection();
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == okay)
			runSelectedCommand();
		else if (source == options) {
			showOptionsDialog();
			return;
		}
		dialog.dispose();
	}

	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		if (key == KeyEvent.VK_ESCAPE)
			dialog.dispose();
		else if (e.getSource() instanceof JButton) {
			if (key == KeyEvent.VK_ENTER)
				actionPerformed(new ActionEvent(e.getSource(), 0, ""));
			else if (key == KeyEvent.VK_UP) {
				mostFrequent.setSelectedIndex(mostFrequent.getModel().getSize() - 1);
				mostFrequent.requestFocus();
			}
			else if (key == KeyEvent.VK_DOWN) {
				mostRecent.setSelectedIndex(0);
				mostRecent.requestFocus();
			}
			else if (key == KeyEvent.VK_RIGHT) {
				((JButton)e.getSource()).transferFocus();
			}
			else if (key == KeyEvent.VK_LEFT) {
				((JButton)e.getSource()).transferFocusBackward();
			}
			return;
		}
		else if (key == KeyEvent.VK_ENTER) {
			runSelectedCommand();
			dialog.dispose();
		}
		else if (e.getSource() == mostRecent) {
			if (key == KeyEvent.VK_DOWN &&
					mostRecent.getSelectedIndex() == mostRecent.getModel().getSize() - 1) {
				mostFrequent.setSelectedIndex(0);
				mostFrequent.requestFocus();
			}
			else if (key == KeyEvent.VK_UP &&
					mostRecent.getSelectedIndex() == 0) {
				mostFrequent.setSelectedIndex(mostFrequent.getModel().getSize() - 1);
				mostFrequent.requestFocus();
			}
		}
		else if (e.getSource() == mostFrequent) {
			if (key == KeyEvent.VK_UP &&
					mostFrequent.getSelectedIndex() == 0) {
				mostRecent.setSelectedIndex(mostRecent.getModel().getSize() - 1);
				mostRecent.requestFocus();
			}
			else if (key == KeyEvent.VK_DOWN &&
					mostFrequent.getSelectedIndex() == mostFrequent.getModel().getSize() - 1) {
				mostRecent.setSelectedIndex(0);
				mostRecent.requestFocus();
			}
		}
	}

	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}

	public String commandExecuting(String command) {
		if (command.equals("*recent commands") ||
				command.equals("Repeat a Recent Command"))
			return command;
		int listIndex = getListIndex();
		Prefs.set(PREFS_KEY + listIndex, command);
		listIndex = ((listIndex + 1) % maxLRUSize);
		Prefs.set(PREFS_KEY + ".lastIndex", "" + listIndex);
		return command;
	}

	protected void runSelectedCommand() {
		String command = (String)mostRecent.getSelectedValue();
		if (command == null)
			command = (String)mostFrequent.getSelectedValue();
		if (command != null)
			new Executer(command, null);
	}

	protected int getListIndex() {
		String value = Prefs.get(PREFS_KEY + ".lastIndex", "0");
		return "".equals(value) ? 0 : Integer.parseInt(value);
	}

	protected Vector getMostRecent(int maxCount) {
		Vector result = new Vector();
		Set<String> all = suppressRepeatedCommands ?
			new HashSet<String>() : null;
		int listIndex = getListIndex();
		for (int i = 0; i < maxLRUSize; i++) {
			listIndex = ((listIndex - 1 + maxLRUSize) % maxLRUSize);
			String command = Prefs.get(PREFS_KEY + listIndex, null);
			if (command == null)
				break;
			if (suppressRepeatedCommands) {
				if (all.contains(command))
					continue;
				all.add(command);
			}
			result.add(command);
			if (result.size() >= maxCount)
				break;
		}
		return result;
	}

	protected Vector getMostFrequent(int maxCount) {
		Vector result = new Vector();
		final Map<String, Integer> map = new HashMap<String, Integer>();
		for (int i = 0; i < maxLRUSize; i++) {
			String command = Prefs.get(PREFS_KEY + i, null);
			if (command == null)
				break;
			Integer value = map.get(command);
			if (value == null) {
				map.put(command, new Integer(1));
				result.add(command);
			}
			else
				map.put(command, new Integer(value.intValue() + 1));
		}
		Collections.sort(result, new Comparator<String>() {
			public int compare(String c1, String c2) {
				return map.get(c2).intValue()
					- map.get(c1).intValue();
			}

			public boolean equals(Object other) {
				return false;
			}
		});
		return new Vector(result.subList(0, Math.min(result.size(), maxCount)));
	}

	void readPrefs() {
		recentListSize = (int)Prefs.get(PREFS_KEY + ".recent-list-size", recentListSize);
		frequentListSize = (int)Prefs.get(PREFS_KEY + ".frequent-list-size", frequentListSize);
		maxLRUSize = (int)Prefs.get(PREFS_KEY + ".max-lru-size", maxLRUSize);
		suppressRepeatedCommands = Prefs.get(PREFS_KEY + ".suppress-repetitions", suppressRepeatedCommands);
	}

	void showOptionsDialog() {
		GenericDialog gd = new GenericDialog("Recent Command Options");
		gd.addNumericField("size_of_recent_list", recentListSize, 0);
		gd.addNumericField("size_of_most-frequent_list", frequentListSize, 0);
		gd.addNumericField("history_size", maxLRUSize, 0);
		gd.addCheckbox("suppress_repeated_commands", suppressRepeatedCommands);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		int value = (int)gd.getNextNumber();
		if (value != recentListSize) {
			recentListSize = value;
			Prefs.set(PREFS_KEY + ".recent-list-size", recentListSize);
		}
		value = (int)gd.getNextNumber();
		if (value != frequentListSize) {
			frequentListSize = value;
			Prefs.set(PREFS_KEY + ".frequent-list-size", frequentListSize);
		}
		value = (int)gd.getNextNumber();
		if (value != maxLRUSize) {
			maxLRUSize = value;
			Prefs.set(PREFS_KEY + ".max-lru-size", maxLRUSize);
		}
		boolean bool = gd.getNextBoolean();
		if (bool != suppressRepeatedCommands) {
			suppressRepeatedCommands = bool;
			Prefs.set(PREFS_KEY + ".suppress-repetitions", suppressRepeatedCommands);
		}
		mostRecent.setListData(getMostRecent(recentListSize));
		mostFrequent.setListData(getMostFrequent(frequentListSize));
		dialog.pack();
	}
}
