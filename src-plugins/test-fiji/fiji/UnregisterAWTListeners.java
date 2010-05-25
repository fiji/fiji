package fiji;

import ij.IJ;

import java.awt.Toolkit;

import java.awt.event.AWTEventListener;
import java.awt.event.AWTEventListenerProxy;


public class UnregisterAWTListeners {
	public static void listAWTListeners() {
		report("List of AWT event listeners");
		report("===========================");
		for (AWTEventListener listener : Toolkit.getDefaultToolkit()
				.getAWTEventListeners()) {
			report("listener: " + listener);
			if (listener instanceof AWTEventListenerProxy)
				report("        proxies "
					+ ((AWTEventListenerProxy)listener)
					.getListener());
		}
		report("end of listeners");
	}

	public static void unregisterAWTListeners() {
		boolean headerShown = false;
		for (AWTEventListener listener : Toolkit.getDefaultToolkit()
				.getAWTEventListeners()) {
			AWTEventListener save = listener;
			if (listener instanceof AWTEventListenerProxy)
				listener = (AWTEventListener)
					((AWTEventListenerProxy)listener)
					.getListener();
			Class clazz = listener.getClass();
			String name = clazz.getName();
			if (name.startsWith("javax.") ||
					name.startsWith("java.") ||
					name.equals("fiji.Main"))
				continue;
			if (!headerShown) {
				report("Removing listener(s):");
				report("=====================");
				headerShown = true;
			}
			report("removing " + listener + (listener == save ?
						"" : " (proxied)"));
			Toolkit.getDefaultToolkit()
				.removeAWTEventListener(save);
		}
	}

	public static void report(String message) {
		if (!message.endsWith("\n"))
			message += "\n";
		IJ.log(message);
	}

	public static void main(String[] args) {
		listAWTListeners();
		unregisterAWTListeners();
	}
}
