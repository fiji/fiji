package fiji.pluginManager.logic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginObject {
	private String strFilename; //Main identifier
	private String md5Sum; //Used for comparison: Determine if update needed
	private String timestamp; //Version of plugin file ("Unique within each filename")
	private String newMd5Sum; //if any
	private String newTimestamp; //if any
	private PluginDetails pluginDetails;
	private long filesize;
	private List<Dependency> dependency; //Dependency object: filename and timestamp

	//Status of its record in database
	private boolean fiji; //name in records or not
	private boolean recorded; //its md5 sum in records or not
	private boolean readOnly; //physical file (local side) read-only?

	public static enum Action {
		NOT_INSTALLED ("Not installed"),
		INSTALLED ("Installed"),
		REMOVE ("Remove it"),
		INSTALL ("Install it"),
		UPDATE ("Update it"),
		UPLOAD ("Upload it");

		private String label;
		Action(String label) {
			this.label = label;
		}
		public String getLabel() {
			return label;
		}
	};

	public static enum Status {
		NOT_INSTALLED (new Action[] { Action.NOT_INSTALLED, Action.INSTALL }),
		INSTALLED (new Action[] { Action.INSTALLED, Action.REMOVE }),
		UPDATEABLE (new Action[] { Action.INSTALLED, Action.REMOVE, Action.UPDATE });

		private String[] labels, develLabels;
		Status(Action[] actions) {
			labels = new String[actions.length];
			for (int i = 0; i < labels.length; i++)
				labels[i] = actions[i].getLabel();

			develLabels = new String[actions.length + 1];
			System.arraycopy(labels, 0,
					develLabels, 0, labels.length);
			develLabels[labels.length] = Action.UPLOAD.getLabel();
		}

		public String[] getActionLabels(boolean isDeveloper) {
			return isDeveloper ? develLabels : labels;
		}
	};
	private Status status;

	private Action action;

	private static Map<String, Action> actionMap;
	static {
		actionMap = new HashMap<String, Action>();
		for (Action action : Action.values())
			actionMap.put(action.getLabel(), action);
	}
	public static Action getAction(String label) {
		return actionMap.get(label);
	}

	//State to indicate whether Plugin removed/downloaded successfully
	public static enum ChangeStatus { NONE, SUCCESS, FAIL };
	private ChangeStatus changedStatus = ChangeStatus.NONE;

	public PluginObject(String strFilename, String md5Sum, String timestamp, Status status,
			boolean fiji, boolean recorded) {
		this.strFilename = strFilename;
		this.md5Sum = md5Sum;
		this.timestamp = timestamp;
		this.status = status;
		this.fiji = fiji;
		this.recorded = recorded;
		pluginDetails = new PluginDetails(); //default: no information, empty
		setNoAction();
	}

	public void setUpdateDetails(String newMd5Sum, String newTimestamp) {
		status = Status.UPDATEABLE; //set status, if not done so already
		this.newMd5Sum = newMd5Sum;
		this.newTimestamp = newTimestamp;
	}

	public void setPluginDetails(PluginDetails pluginDetails) {
		this.pluginDetails = pluginDetails;
	}

	public void setDependency(List<Dependency> dependency) {
		this.dependency = dependency;
	}

	public void setDependency(Iterable<String> dependencies, PluginCollection allPlugins) {
		dependency = new ArrayList<Dependency>();
		if (dependencies == null)
			return;
		for (String file : dependencies) {
			//Only add if JAR file is in Fiji records
			PluginObject other = allPlugins.getPlugin(file);
			if (other != null)
				dependency.add(new Dependency(file, other.getTimestamp(), "at-least"));
		}
	}

	public void setFilesize(long filesize) {
		this.filesize = filesize;
	}

	public void setChangeStatus(ChangeStatus status) {
		this.changedStatus = status;
	}

	public void success() {
		setChangeStatus(ChangeStatus.SUCCESS);
	}

	public void fail() {
		setChangeStatus(ChangeStatus.FAIL);
	}

	public void setNoAction() {
		action = (status == Status.NOT_INSTALLED ?
				Action.NOT_INSTALLED : Action.INSTALLED);
	}

	public void setAction(Action action) {
		if ((action == Action.REMOVE && !isRemovable()) ||
				(action == Action.NOT_INSTALLED &&
				 status != Status.NOT_INSTALLED) ||
				(action == Action.INSTALLED &&
				 status == Status.NOT_INSTALLED) ||
				(action == Action.UPDATE && !isUpdateable()) ||
				(action == Action.INSTALL && !isInstallable()))
			throw new Error("Invalid action requested for plugin "
					+ strFilename + "(" + action + ", " + status + ")");
		this.action = action;
	}

	public void setIsReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public String getFilename() {
		return strFilename;
	}

	public String getmd5Sum() {
		return md5Sum;
	}

	public String getNewMd5Sum() {
		return newMd5Sum;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public String getNewTimestamp() {
		return newTimestamp;
	}

	public PluginDetails getPluginDetails() {
		return pluginDetails;
	}

	public long getFilesize() {
		return filesize;
	}

	public List<Dependency> getDependencies() {
		return dependency;
	}

	public Status getStatus() {
		return status;
	}

	public Action getAction() {
		return action;
	}

	public boolean isInstallable() {
		return status == Status.NOT_INSTALLED;
	}

	public boolean isUpdateable() {
		return status == Status.UPDATEABLE;
	}

	public boolean isRemovableOnly() {
		return status == Status.INSTALLED;
	}

	public boolean isRemovable() {
		return status == Status.INSTALLED || status == Status.UPDATEABLE;
	}

	public boolean actionSpecified() {
		return action != Action.NOT_INSTALLED &&
			action != Action.INSTALLED;
	}

	public boolean toUpdate() {
		return isUpdateable() && action == Action.UPDATE;
	}

	public boolean toRemove() {
		return isRemovable() && action == Action.REMOVE;
	}

	public boolean toInstall() {
		return isInstallable() && action == Action.INSTALL;
	}

	public boolean toUpload() {
		return action == Action.UPLOAD;
	}

	public boolean isFijiPlugin() {
		return fiji;
	}

	public boolean isInRecords() {
		return recorded;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean changeSucceeded() {
		return changedStatus == ChangeStatus.SUCCESS;
	}

	public boolean changeFailed() {
		return changedStatus == ChangeStatus.FAIL;
	}

	public boolean changeNotDone() {
		return changedStatus == ChangeStatus.NONE;
	}
}
