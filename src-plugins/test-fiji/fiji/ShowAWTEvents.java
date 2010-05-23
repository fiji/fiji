package fiji;

import java.awt.AWTEvent;
import java.awt.Toolkit;

import java.awt.event.AWTEventListener;

public class ShowAWTEvents implements AWTEventListener {
	long eventMask = -1l;
	String classFilter = null;
	long countFilter = -1; // negative: switch off

	public void eventDispatched(AWTEvent event) {
		if (classFilter != null && !event.getSource().getClass()
				.getName().equals(classFilter))
			return;
		if (countFilter == 0)
			return;
		else if (countFilter > 0)
			countFilter--;
		report("got event: " + event
				+ " (source: " + event.getSource() + ")");
	}

	public static void report(String message) {
		if (!message.endsWith("\n"))
			message += "\n";
		// cannot IJ.log(), as that would give an infinite feedback loop
		System.err.println(message);
	}

	public static void start(long eventMask, String classFilter,
			int maxCount) {
		ShowAWTEvents show = new ShowAWTEvents();
		show.eventMask = eventMask;
		show.classFilter = classFilter;
		Toolkit.getDefaultToolkit()
			.addAWTEventListener(new ShowAWTEvents(), eventMask);
	}

	public static void main(String[] args) {
		start(-1l, null, -1);
		report("AWT Event logger started");
		report("========================");
	}
}
