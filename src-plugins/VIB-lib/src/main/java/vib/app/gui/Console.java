package vib.app.gui;

import ij.text.TextWindow;

public class Console {

	private static Console instance = new Console();
	private TextWindow window;

	private static String welcome = "Welcome to the VIB Protocol\n" + 
				"========================================\n";

	public static Console instance() {
		return instance;
	}
	
	private Console() {}

	private void newWindow() {
		if(window != null)
			window.close();
		
		window = new TextWindow("The VIB Protocol",
					"", welcome, 512, 512);
	}

	public void clear() {
		newWindow();
	}

	public void append(String text) {
		if(window == null)
			newWindow();
		window.append(text + '\n');
	}
}

