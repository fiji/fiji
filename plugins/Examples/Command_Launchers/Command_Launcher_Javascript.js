// Crude java-like version by Albert Cardona
importClass(Packages.ij.IJ);
importClass(Packages.ij.Menus);
importClass(Packages.ij.gui.GenericDialog);
importClass(Packages.java.util.ArrayList);
commands = Menus.getCommands();
keys = new ArrayList(commands.keySet());

gd = new GenericDialog("Command Launcher");
gd.addStringField("Command: ", "");
prom = gd.getStringFields().get(0);
importClass(Packages.java.awt.Color)
prom.setForeground(Color.red);

// Create a body for the interface TextListener,
// since 'new' cannot be called directly on an interface
body = { textValueChanged: function(evt) {
		text = prom.getText();
		for (i=0;i<keys.size();i++) {
			command = keys.get(i);
			if (command.equals(text)) {
				prom.setForeground(Color.black);
				return;
			}
		}
		// no command found
		prom.setForeground(Color.red);
	}
}
importClass(Packages.java.awt.event.TextListener);
prom.addTextListener(new TextListener(body));

gd.showDialog();
if (!gd.wasCanceled()) {
	IJ.doCommand(gd.getNextString());
}
