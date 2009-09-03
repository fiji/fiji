package fiji.pluginManager.logic;
import java.util.Observable;

/*
 * Class functionality:
 * A specialized version of PluginData.
 *
 * Allows a user interface to observe it. Class is designed for performing multiple
 * tasks, allowing the user to decide when to inform the interface of its status.
 */
public class PluginDataObservable extends Observable {
	protected String taskname; //Generic title of the current task, namely a filename
	protected int counter, total;

	public String getTaskname() {
		return taskname;
	}

	public int getCounter() {
		return counter;
	}

	public int getTotal() {
		return total;
	}

	public boolean isDone() {
		return counter >= total;
	}

	protected void progress(String taskname) {
		progress(taskname, counter + 1, total);
	}

	protected void progress(String taskname, int counter, int total) {
		this.taskname = taskname;
		this.counter = counter;
		this.total = total;
		setChanged();
		notifyObservers();
	}

	protected void done() {
		counter = total;
		setChanged();
		notifyObservers();
	}
}
