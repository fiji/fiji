package ij3d.shortcuts;

import ij3d.UniverseSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

public class ShortCuts {

	private final List<String> commands;
	private final HashMap<String, JMenuItem> items;
	private final HashMap<String, String> shortcuts;

	public ShortCuts(JMenuBar menubar) {
		commands = new ArrayList<String>();
		items = new HashMap<String, JMenuItem>();
		shortcuts = UniverseSettings.shortcuts;

		for(int i = 0; i < menubar.getMenuCount(); i++)
			scan(menubar.getMenu(i), "");

		for(String command : commands) {
			String shortcut = shortcuts.get(command);
			if(shortcut != null)
				setShortCut(command, shortcut);
		}
	}

	public void save() {
		UniverseSettings.save();
	}

	public void reload() {
		UniverseSettings.load();
	}

	public Iterable<String> getCommands() {
		return commands;
	}

	public String getShortCut(String command) {
		return shortcuts.get(command);
	}

	public void setShortCut(String command, String shortcut) {
		if(shortcut.trim().length() == 0) {
			clearShortCut(command);
			return;
		}
		shortcuts.put(command,  shortcut);
		KeyStroke stroke = KeyStroke.getKeyStroke(shortcut);
		items.get(command).setAccelerator(stroke);
	}

	public void clearShortCut(String command) {
		items.get(command).setAccelerator(null);
		shortcuts.remove(command);
	}

	public int getNumberOfCommands() {
		return commands.size();
	}

	public String getCommand(int i) {
		return commands.get(i);
	}

	private void scan(JMenu menu, String prefix) {
		prefix += menu.getText() + " > ";
		for(int i = 0; i < menu.getItemCount(); i++) {
			JMenuItem mi = menu.getItem(i);
			if(mi == null)
				continue;
			if(mi instanceof JMenu) {
				scan((JMenu)mi, prefix);
			}
			else {
				String c = prefix + mi.getText();
				commands.add(c);
				items.put(c, mi);
			}
		}
	}
}
