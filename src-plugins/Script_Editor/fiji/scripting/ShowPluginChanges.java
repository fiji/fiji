package fiji.scripting;

import ij.io.OpenDialog;

import ij.plugin.PlugIn;

public class ShowPluginChanges implements PlugIn {
	public void run(String arg) {
		FileFunctions fileFunctions = new FileFunctions(null);
		if (arg == null || "".equals(arg)) {
			OpenDialog dialog = new OpenDialog("Which Fiji component",
				fileFunctions.fijiDir + "plugins", "");
			if (dialog.getDirectory() == null)
				return;
			arg = dialog.getDirectory() + dialog.getFileName();
		}
		if (arg.startsWith(fileFunctions.fijiDir))
			arg = arg.substring(fileFunctions.fijiDir.length());
		fileFunctions.showPluginChangesSinceUpload(arg);
	}
}