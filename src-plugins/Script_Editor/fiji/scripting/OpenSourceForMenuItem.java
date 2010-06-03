package fiji.scripting;

import fiji.util.MenuItemDiverter;

import ij.IJ;
import ij.Menus;

import javax.swing.JOptionPane;

public class OpenSourceForMenuItem extends MenuItemDiverter {
	public void run(String arg) {
		super.run(arg);
	}

	protected String getTitle() {
		return "Menu Item Source";
	}

	protected void action(String arg) {
		String action = (String)Menus.getCommands().get(arg);
		if (action != null) try {
			IJ.showStatus("Opening source for " + arg);
			int paren = action.indexOf('(');
			if (paren > 0)
				action = action.substring(0, paren);
			String path = new FileFunctions(Script_Editor.getInstance()).getSourcePath(action);
			if (path != null) {
				new Script_Editor().run(path);
				return;
			}
		} catch (Exception e) { e.printStackTrace(); /* fallthru */ }
		error("Could not get source for '" + arg + "'");
	}

	protected void error(String message) {
		JOptionPane.showMessageDialog(null, message);
	}
}