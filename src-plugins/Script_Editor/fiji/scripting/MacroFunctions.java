package fiji.scripting;

import ij.Menus;

import ij.macro.Interpreter;

import ij.plugin.MacroInstaller;

public class MacroFunctions {
	public void installMacro(String title, String code) {
		String prefix = Interpreter.getAdditionalFunctions();
		if (prefix != null)
			code = prefix + "\n" + code;
                MacroInstaller installer = new MacroInstaller();
                installer.setFileName(title);
                if (installer.install(code, Menus.getMacrosMenu()) > 0)
                        installer.install(null);
	}
}