package fiji.scripting;

import ij.plugin.PlugIn;
import ij.Macro;

public class Script_Editor implements PlugIn {

	public void run(String path) {
		String a = Macro.getOptions();
		try {
			new TextEditor(Macro.getValue(a, "path", null));
		} catch (Exception e) {
			new TextEditor("");
		}
	}

}

