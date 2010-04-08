package fiji.updater.util;

public class StderrProgress implements Progress {
	final static String end = "\033[K\r";
	protected String label;
	protected Object item;
	protected long lastShown, minShowDelay = 500;

	protected boolean skipShow() {
		long now = System.currentTimeMillis();
		if (now - lastShown < minShowDelay)
			return true;
		lastShown = now;
		return false;
	}

	public void setTitle(String title) {
		label = title;
	}

	public void setCount(int count, int total) {
		if (skipShow())
			return;
		System.err.print(label + " "
			+ count + "/" + total + end);
	}

	public void addItem(Object item) {
		this.item = item;
		System.err.print(label + " (" + item + ") " + end);
	}

	public void setItemCount(int count, int total) {
		if (skipShow())
			return;
		System.err.print(label + " (" + item + ") ["
			+ count + "/" + total + "]" + end);
	}

	public void itemDone(Object item) {
		System.err.print(item.toString() + " done" + end);
	}

	public void done() {
		System.err.println("Done: " + label + end);
	}
}
