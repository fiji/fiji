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
	protected int currentlyLoaded;
	protected int totalToLoad;
	protected boolean allTasksComplete;

	public String getTaskname() {
		return taskname;
	}

	public int getCurrentlyLoaded() {
		return currentlyLoaded;
	}

	public int getTotalToLoad() {
		return totalToLoad;
	}

	public boolean allTasksComplete() {
		return allTasksComplete;
	}

	protected void changeStatus(String taskname, int currentlyLoaded, int totalToLoad) {
		this.taskname = taskname;
		this.currentlyLoaded = currentlyLoaded;
		this.totalToLoad = totalToLoad;
		setChanged();
		notifyObservers();
	}

	protected void setStatusComplete() {
		allTasksComplete = true;
		setChanged();
		notifyObservers();
	}
}
