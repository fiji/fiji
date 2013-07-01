package fiji;

/**
 * Modify some IJ1 quirks at runtime, thanks to Javassist
 */

import imagej.legacy.LegacyExtensions;
import imagej.legacy.LegacyExtensions.LegacyEditorPlugin;
import imagej.util.AppUtils;

import java.io.File;

import javassist.CannotCompileException;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;

public class IJHacker extends JavassistHelper {

	@Override
	public void instrumentClasses() throws BadBytecode, CannotCompileException, NotFoundException {
		LegacyExtensions.setAppName("(Fiji Is Just) ImageJ");
		LegacyExtensions.setIcon(new File(AppUtils.getBaseDirectory(), "images/icon.png"));
		LegacyExtensions.setLegacyEditor(new LegacyEditorPlugin() {

			@Override
			public boolean open(File path) {
				return FijiTools.openFijiEditor(path);
			}

			@Override
			public boolean create(String title, String body) {
				return FijiTools.openFijiEditor(title, body);
			}
		});
	}

}
