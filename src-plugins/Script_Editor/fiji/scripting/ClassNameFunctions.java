package fiji.scripting;

import fiji.scripting.completion.ClassCompletionProvider;
import fiji.scripting.completion.ClassNames;

import ij.gui.GenericDialog;

import ij.plugin.BrowserLauncher;

import java.util.List;

import javax.swing.JOptionPane;

public class ClassNameFunctions {
	ClassNames names;

	public ClassNameFunctions(ClassNames names) {
		this.names = names;
	}

	public ClassNameFunctions(ClassCompletionProvider provider) {
		this(provider.names);
	}

	/**
	 * Return the full name (including the package) for a given class name.
	 *
	 * If there are multiple classes of the specified name, ask the user.
	 * Returns null if the user canceled, or if there was no class of that
	 * name.
	 */
	public String getFullName(String className) {
		List<String> list = names.getFullPackageNames(className);
		if (list.size() == 0)
			return null;
		if (list.size() == 1)
			return list.get(0);
		String[] names = list.toArray(new String[list.size()]);
		GenericDialog gd = new GenericDialog("Choose class");
		gd.addChoice("class", names, names[0]);
		gd.showDialog();
		if (gd.wasCanceled())
			return null;
		return gd.getNextChoice();
	}

	public void openHelpForClass(String className, boolean withFrames) {
		String fullName = getFullName(className);
		if (fullName == null) {
			JOptionPane.showMessageDialog(null, "Class '"
					+ className + "' was not found!");
			return;
		}
		String urlPrefix;
		if (fullName.startsWith("java.") ||
				fullName.startsWith("javax."))
			urlPrefix = "http://java.sun.com/j2se/1.5.0/docs/api/";
		else
			urlPrefix = "http://pacific.mpi-cbg.de/javadoc/";
		new BrowserLauncher().run(urlPrefix
				+ (withFrames ? "index.html?" : "")
				+ fullName.replace('.', '/') + ".html");
	}
}
