/*
 * Please run this Javascript via
 *
 *    Macros>Evaluate Javascript
 *
 * or by hitting Ctrl+J (on MacOSX, Apple+J).
 */

importClass(Packages.java.io.File);
importClass(Packages.java.net.URL);
importClass(Packages.java.net.URLClassLoader);

baseURL = 'http://update.imagej.net/jars/';
jars = [
	'ij-ui-swing-updater-2.0.0-beta-7.1.jar-20130716162630',
	'miglayout-3.7.3.1-swing.jar-20120404210913',
	'ij-updater-core-2.0.0-SNAPSHOT.jar-20130716173252',
	'ij-core-2.0.0-beta-7.1.jar-20130716162630',
	'scijava-common-1.5.0.jar-20130716162630',
	'sezpoz-1.9-imagej.jar-20130508011618',
	'eventbus-1.4.jar-20120404210913',
	'tools-1.4.2.jar-20120802184342'
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

	var updaterClassName = "imagej.updater.ui.CommandLine";
} else {
	importClass(Packages.ij.IJ);

	if (typeof IJ == 'undefined') {
		var IJ = Thread.currentThread().getContextClassLoader().loadClass('ij.IJ').newInstance();
	}

	var updaterClassName = "imagej.updater.gui.ImageJUpdater";
}


// make sure that the system property 'ij.dir' is set correctly
if (System.getProperty("ij.dir") == null) {
	ijDir = IJ.getDirectory("imagej");
	if (ijDir == null) {
		url = IJ.getClassLoader().loadClass("ij.IJ").getResource("/ij/IJ.class").toString();
		bang = url.indexOf(".jar!/");
		if (url.startsWith("jar:file:") && bang > 0) {
			ijDir = new File(url.substring(9, bang)).getParent();
			if (ijDir.endsWith("/target") || ijDir.endsWith("\\target"))
				ijDir = ijDir.substring(0, ijDir.length() - 7);
		}
		else if (url.startsWith("file:") && bang < 0 && url.endsWith("/ij/IJ.class")) {
			ijDir = url.substring(5, url.length() - 12);
			if (ijDir.endsWith("/classes"))
				ijDir = ijDir.substring(0, ijDir.length() - 8);
			if (ijDir.endsWith("/target"))
				ijDir = ijDir.substring(0, ijDir.length() - 7);
		}
		else {
			IJ.error("Cannot set ij.dir for " + url);
		}
	}
	System.setProperty("ij.dir", ijDir);
}

ijDir = new File(System.getProperty("ij.dir"));
if (!new File(ijDir, "db.xml.gz").exists()) {
	IJ.showStatus("adding the Fiji update site");
	filesClass = loader.loadClass("imagej.updater.core.FilesCollection");
	files = filesClass.getConstructor([ loader.loadClass("java.io.File") ]).newInstance([ ijDir ]);
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
