// Please start this script with Ctrl+J
// (on MacOSX with Command+J)

importClass(Packages.java.io.InputStreamReader);
importClass(Packages.java.net.URL);
importClass(Packages.javax.script.ScriptEngineManager);

// call the latest version of the Fix_Updater.js script
var url = "https://raw.github.com/fiji/fiji/master/"
	+ "plugins/Scripts/Plugins/Utilities/Fix_Updater.js";
var stream = new URL(url).openStream();
var reader = new InputStreamReader(stream);
var engine = new ScriptEngineManager()
	.getEngineByName("ECMAScript");
engine.eval(reader);
