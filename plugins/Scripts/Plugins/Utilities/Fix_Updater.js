/*
 * Please run this Javascript via
 *
 *    Macros>Evaluate Javascript
 *
 * or by hitting Ctrl+J (on MacOSX, Apple+J).
 *
 * If this fails, please call Edit>Select All,
 * Edit>Copy, switch to the main window call
 * File>New>Script..., Edit>Paste, select
 * Language>JavaScript and then hit the Run
 * button.
 */

importClass(Packages.java.io.File);
importClass(Packages.java.lang.System);
importClass(Packages.java.net.URL);
importClass(Packages.java.net.URLClassLoader);

baseURL = 'http://update.imagej.net/jars/';
jars = [
	'imagej-ui-swing-0.4.6.jar-20140609213633',
	'imagej-updater-0.3.6.jar-20140607183739',
	'scijava-common-2.22.5.jar-20140610214546',
	'imagej-common-0.7.2.jar-20140603151316',
	'mapdb-1.0.3.jar-20140610190158',
	'udunits-4.3.18.jar-20131018164809',
	'eventbus-1.4.jar-20120404210913',
	'gentyref-1.1.0.jar-20140516211031'
];

urls = [];
for (i = 0; i < jars.length; i++)
	urls[i] = new URL(baseURL + jars[i]);

importClass(Packages.java.lang.ClassLoader);
parent = ClassLoader.getSystemClassLoader().getParent();
loader = new URLClassLoader(urls, parent);

isCommandLine = typeof arguments != 'undefined';

if (isCommandLine) {
	importClass(Packages.java.lang.System);

	var IJ = {
		getDirectory: function(label) {
			// command-line: default to current directory
			return new File("").getAbsolutePath();
		},

		showStatus: function(message) {
			print(message + "\n");
		},

		error: function(message) {
			print(message + "\n");
		},

		handleException: function(exception) {
			exception.printStackTrace();
		}
	}

	var updaterClassName = "net.imagej.updater.CommandLine";
} else {
	try {
		importClass(Packages.ij.IJ);
	} catch (e) {
		// ignore; this is a funny PluginClassLoader problem
	}

	if (typeof IJ == 'undefined') {
		importClass(Packages.java.lang.Thread);
		var IJ = Thread.currentThread().getContextClassLoader().loadClass('ij.IJ').newInstance();
	}

	var updaterClassName = "net.imagej.ui.swing.updater.ImageJUpdater";
}


// make sure that the system property 'imagej.dir' is set correctly
if (System.getProperty("imagej.dir") == null) {
	imagejDir = System.getProperty("ij.dir");
	if (imagejDir == null) {
		imagejDir = IJ.getDirectory("imagej");
	}
	if (imagejDir == null) {
		url = IJ.getClassLoader().loadClass("ij.IJ").getResource("/ij/IJ.class").toString();
		bang = url.indexOf(".jar!/");
		if (url.startsWith("jar:file:") && bang > 0) {
			imagejDir = new File(url.substring(9, bang)).getParent();
			if (imagejDir.endsWith("/target") || imagejDir.endsWith("\\target"))
				imagejDir = imagejDir.substring(0, imagejDir.length() - 7);
		}
		else if (url.startsWith("file:") && bang < 0 && url.endsWith("/ij/IJ.class")) {
			imagejDir = url.substring(5, url.length() - 12);
			if (imagejDir.endsWith("/classes"))
				imagejDir = imagejDir.substring(0, imagejDir.length() - 8);
			if (imagejDir.endsWith("/target"))
				imagejDir = imagejDir.substring(0, imagejDir.length() - 7);
		}
		else {
			IJ.error("Cannot set imagej.dir for " + url);
		}
	}
	System.setProperty("imagej.dir", imagejDir);
}

// for backwards-compatibility, make sure that the system property 'ij.dir'
// is set correctly, too, just in case
if (System.getProperty("ij.dir") == null) {
	System.setProperty("ij.dir", System.getProperty("imagej.dir"));
}

imagejDir = new File(System.getProperty("imagej.dir"));
if (!new File(imagejDir, "db.xml.gz").exists()) {
	IJ.showStatus("adding the Fiji update site");
	filesClass = loader.loadClass("net.imagej.updater.FilesCollection");
	files = filesClass.getConstructor([ loader.loadClass("java.io.File") ]).newInstance([ imagejDir ]);
	files.getUpdateSite("ImageJ").timestamp = -1;
	files.addUpdateSite("Fiji", "http://fiji.sc/update/", null, null, -1);
	files.write();
}

IJ.showStatus("loading remote updater");
updaterClass = loader.loadClass(updaterClassName);
IJ.showStatus("running remote updater");
try {
	i = updaterClass.newInstance();
	if (isCommandLine) {
		i.main(arguments);
	} else {
		i.run();
	}
} catch (e) {
	IJ.handleException(e.javaException);
}
