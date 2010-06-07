package fiji.scripting;

import ij.io.OpenDialog;

import ij.plugin.PlugIn;

public class ShowPluginChanges implements PlugIn {
	public void run(String arg) {
		String fijiDir = System.getProperty("fiji.dir") + "/";
		if (arg == null || "".equals(arg)) {
			OpenDialog dialog = new OpenDialog("Which Fiji component",
				fijiDir + "plugins", "");
			if (dialog.getDirectory() == null)
				return;
			arg = dialog.getDirectory() + dialog.getFileName();
		}
		if (arg.startsWith(fijiDir))
			arg = arg.substring(fijiDir.length());
		new FileFunctions(null).showPluginChangesSinceUpload(arg);
	}
}