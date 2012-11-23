/*
 * Please run this Javascript via Macros>Evaluate Javascript
 */
importClass(Packages.ij.IJ);

importClass(Packages.java.net.URL);
importClass(Packages.java.net.URLClassLoader);

baseURL = "http://update.imagej.net/jars/";
jars = [
	"ij-ui-swing-updater-2.0.0-SNAPSHOT.jar-20121106224844",
	"ij-updater-core-2.0.0-SNAPSHOT.jar-20121108211844",
	"ij-core-2.0.0-SNAPSHOT.jar-20121108211844",
	"eventbus-1.4.jar-20120404210913",
	"sezpoz-1.9.jar-20120404210913"
];

urls = [];
for (i = 0; i < jars.length; i++)
	urls[i] = new URL(baseURL + jars[i]);

importClass(Packages.java.lang.ClassLoader);
parent = ClassLoader.getSystemClassLoader().getParent();
loader = new URLClassLoader(urls, parent);
IJ.showStatus("loading remote updater");
guiClass = loader.loadClass("imagej.updater.gui.ImageJUpdater");
IJ.showStatus("running remote updater");
try {
	i = guiClass.newInstance();
	i.run();
} catch (e) {
	IJ.handleException(e.javaException);
}
