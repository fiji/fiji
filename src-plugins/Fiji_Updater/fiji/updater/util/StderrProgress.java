package fiji.updater.util;

public class StderrProgress implements Progress {
	final static String end = "\033[K\r";
	protected String label;
	protected Object item;
	protected long lastShown, minShowDelay = 500;
	protected int lineWidth = -1;

	public StderrProgress() {}

	public StderrProgress(int lineWidth) {
		this.lineWidth = lineWidth;
	}

	protected void print(String label, String rest) {
		if (lineWidth < 0)
			System.err.print(label + " " + rest + end);
		else {
			if (label.length() >= lineWidth - 3)
				label = label.substring(0, lineWidth - 3) + "...";
			else {
				int diff = label.length() + 1 + rest.length() - lineWidth;
				if (diff < 0)
					label += " " + rest;
				else
					label += (" " + rest).substring(0, rest.length() - diff - 3) + "...";
			}
			System.err.print(label + end);
		}
	}

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
		print(label, "" + count + "/" + total);
	}

	public void addItem(Object item) {
		this.item = item;
		print(label, "(" + item + ") ");
	}

	public void setItemCount(int count, int total) {
		if (skipShow())
			return;
		print(label, "(" + item + ") [" + count + "/" + total + "]");
	}

	public void itemDone(Object item) {
		print(item.toString(), "done");
	}

	public void done() {
		print("Done:", label);
		System.err.println("");
	}
}
