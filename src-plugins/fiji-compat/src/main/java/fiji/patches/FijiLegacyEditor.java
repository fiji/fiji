package fiji.patches;

import fiji.FijiTools;

import imagej.legacy.plugin.LegacyEditor;

import java.io.File;

import org.scijava.plugin.Plugin;

@Plugin(type = LegacyEditor.class)
public class FijiLegacyEditor implements LegacyEditor {
	public boolean open(final File file) {
		return FijiTools.openFijiEditor(file);
	}

	public boolean create(final String title, final String content) {
		return FijiTools.openEditor(title, content);
	}
}
