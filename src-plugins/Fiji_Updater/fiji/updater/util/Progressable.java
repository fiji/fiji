package fiji.updater.util;

import java.util.ArrayList;
import java.util.List;

/*
 * This class is the base class for serving Progress instances.  For this
 * reason, it implements the same interface.
 */
public class Progressable implements Progress {
	protected List<Progress> progress;

	public Progressable() {
		progress = new ArrayList<Progress>();
	}

	public void addProgress(Progress progress) {
		this.progress.add(progress);
	}

	public void removeProgress(Progress progress) {
		this.progress.remove(progress);
	}

	public void setTitle(String title) {
		for (Progress progress : this.progress)
			progress.setTitle(title);
	}

	public void setCount(int count, int total) {
		for (Progress progress : this.progress)
			progress.setCount(count, total);
	}

	public void addItem(Object item) {
		for (Progress progress : this.progress)
			progress.addItem(item);
	}

	public void setItemCount(int count, int total) {
		for (Progress progress : this.progress)
			progress.setItemCount(count, total);
	}

	public void itemDone(Object item) {
		for (Progress progress : this.progress)
			progress.itemDone(item);
	}

	public void done() {
		for (Progress progress : this.progress)
			progress.done();
	}
}
