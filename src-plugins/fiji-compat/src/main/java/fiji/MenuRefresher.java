package fiji;

import ij.IJ;
import ij.Menus;
import ij.plugin.PlugIn;
import imagej.legacy.SwitchToModernMode;

import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.event.KeyEvent;
import java.util.Hashtable;

public class MenuRefresher implements PlugIn, Runnable {

	/**
	 * The entry point of the {@link PlugIn}.
	 * 
	 * @param arg "update" if called from ij-legacy-patched ImageJ 1.x when the
	 *          user called <i>Help>Refresh Menus</i>.
	 */
	@Override
	public void run(final String arg) {
		run();
	}

	@Override
	public void run() {
		if (IJ.getInstance() != null) {
			Menu help = Menus.getMenuBar().getHelpMenu();
			for (int i = help.getItemCount() - 1; i >= 0; i--) {
				MenuItem item = help.getItem(i);
				String name = item.getLabel();
				if (name.equals("Update Menus"))
					item.setLabel("Refresh Menus");
			}
		}

		installScripts();
		overrideCommands();
		SwitchToModernMode.registerMenuItem();
		SampleImageLoader.install();
		Main.installRecentCommands();

		// install '{' as short cut for the Script Editor
		@SuppressWarnings("unchecked")
		final Hashtable<Integer, String> shortcuts = (Hashtable<Integer, String>)Menus.getShortcuts();
		shortcuts.put(KeyEvent.VK_OPEN_BRACKET, "Script Editor");
		shortcuts.put(200 + KeyEvent.VK_OPEN_BRACKET, "Script Editor");
	}

	/**
	 * Install the scripts in Fiji.app/plugins/
	 */
	public static void installScripts() {
		if (System.getProperty("jnlp") != null)
			return;
		Main.runGently("Refresh Javas");
		String[] languages = { "Jython", "JRuby", "Clojure", "BSH",
				"Javascript", "Scala" };
		for (int i = 0; i < languages.length; i++)
			Main.runGently("Refresh " + languages[i] + " Scripts");
		Main.runGently("Refresh Macros");
	}

	@SuppressWarnings("unchecked")
	static void overrideCommands() {
		final Hashtable<String, String> commands = Menus.getCommands();
		if (!commands.containsKey("Install PlugIn...")) {
			commands.put("Install PlugIn...", "fiji.PlugInInstaller");
			if (IJ.getInstance() != null) {
				final Menu plugins = FijiTools.getMenu("Plugins");
				if (plugins != null)
					for (int i = 0; i < plugins.getItemCount(); i++)
						if (plugins.getItem(i).getLabel().equals("-")) {
							plugins.insert("Install PlugIn...", i);
							plugins.getItem(i).addActionListener(
									IJ.getInstance());
							break;
						}
			}
		}
		commands.put("Compile and Run...", "fiji.Compile_and_Run");
		// make sure "Edit>Options>Memory & Threads runs Fiji's plugin
		commands.put("Memory & Threads...", "fiji.Memory");
	}

}