package fiji;

import ij.IJ;
import ij.Menus;

import ij.plugin.PlugIn;
import ij.plugin.URLOpener;

import java.awt.Menu;

import java.io.File;

import java.net.URL;

import java.util.Hashtable;

public class SampleImageLoader implements PlugIn {
	protected final static String plugin = "ij.plugin.URLOpener";

	public void run(String arg) {
		String fijiDir = System.getProperty("fiji.dir");
		if (fijiDir != null) try {
			int slash = arg.lastIndexOf('/');
			File file = new File(fijiDir + "/samples",
					arg.substring(slash + 1));
			if (file.exists()) {
				IJ.open(file.getPath());
				return;
			}
		} catch(Exception e) { e.printStackTrace(); }

		new URLOpener().run(arg);
	}

	public static void install() {
		Menu menu = (Menu)User_Plugins.getMenuItem("File>Open Samples");
		if (menu == null)
			return;

		Hashtable commands = Menus.getCommands();

		for (int i = 0; i < menu.getItemCount(); i++) {
			String label = menu.getItem(i).getLabel();
			String command = (String)commands.get(label);
			if (command == null || !command.endsWith("\")") ||
					!command.startsWith(plugin + "(\""))
				continue;
			String newCommand = "fiji.SampleImageLoader(\""
				+ command.substring(plugin.length() + 2,
					command.length() - 2) + "\")";
			commands.put(label, newCommand);
		}
	}
}
