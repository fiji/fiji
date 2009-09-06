package fiji.updater.logic;

import fiji.updater.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PluginObject {
	public class Version {
		public String checksum;
		public long timestamp;

		Version(String checksum, long timestamp) {
			this.checksum = checksum;
			this.timestamp = timestamp;
		}
	}

	public static enum Action {
		NOT_FIJI ("Not in Fiji"),
		NOT_INSTALLED ("Not installed"),
		INSTALLED ("Up-to-date"),
		UPDATEABLE ("Update available"),
		MODIFIED ("Locally modified"),
		REMOVE ("Remove it"),
		INSTALL ("Install it"),
		UPDATE ("Update it"),
		UPLOAD ("Upload it");

		private String label;
		Action(String label) {
			this.label = label;
		}

		public String toString() {
			return label;
		}

		public static Action forLabel(String label) {
			for (Action action : Action.values())
				if (action.label.equals(label))
					return action;
			return null;
		}
	};

	public static enum Status {
		NOT_INSTALLED (new Action[] { Action.NOT_INSTALLED, Action.INSTALL }, Util.isDeveloper),
		INSTALLED (new Action[] { Action.INSTALLED, Action.REMOVE }, false),
		UPDATEABLE (new Action[] { Action.UPDATEABLE, Action.REMOVE, Action.UPDATE }, Util.isDeveloper),
		MODIFIED (new Action[] { Action.MODIFIED, Action.REMOVE, Action.UPDATE }, Util.isDeveloper),
		NOT_FIJI (new Action[] { Action.NOT_FIJI, Action.REMOVE }, Util.isDeveloper);

		private Action[] actions;
		Status(Action[] actions, boolean allowUpload) {
			if (allowUpload) {
				this.actions = new Action[actions.length + 1];
				System.arraycopy(actions, 0, this.actions, 0,
						actions.length);
				this.actions[actions.length] = Action.UPLOAD;
			}
			else
				this.actions = actions;
		}

		public Action[] getActions() {
			return actions;
		}

		public boolean isValid(Action action) {
			for (Action a : actions)
				if (a.equals(action))
					return true;
			return false;
		}

		public Action getNoAction() {
			return actions[0];
		}
	};

	private Status status;
	private Action action;
	public String filename, description, newChecksum;
	public Version current;
	public Map<Version, Object> previous;
	public long filesize, newTimestamp;

	// TODO: finally add platform

	// These are LinkedHashMaps to retain the order of the entries
	public Map<Dependency, Object> dependencies;
	public Map<String, Object> links, authors;

	public PluginObject(String filename, String checksum, long timestamp,
			Status status) {
		this.filename = filename;
		current = new Version(checksum, timestamp);
		previous = new LinkedHashMap<Version, Object>();
		this.status = status;
		dependencies = new LinkedHashMap<Dependency, Object>();
		authors = new LinkedHashMap<String, Object>();
		links = new LinkedHashMap<String, Object>();
		if (status == Status.NOT_FIJI)
			filesize = Util.getFilesize(filename);
		setNoAction();
	}

	public boolean hasPreviousVersion(String checksum) {
		if (current.checksum.equals(checksum))
			return true;
		for (Version version : previous.keySet())
			if (version.checksum.equals(checksum))
				return true;
		return false;
	}

	public void setLocalVersion(String checksum, long timestamp) {
		if (checksum.equals(current.checksum)) {
			status = Status.INSTALLED;
			setNoAction();
			return;
		}
		status = hasPreviousVersion(checksum) ?
			Status.UPDATEABLE : Status.MODIFIED;
		setNoAction();
		newChecksum = checksum;
		newTimestamp = timestamp;
	}

	public String getDescription() {
		return description;
	}

	// TODO: allow editing those via GUI
	public void addDependency(String filename, long timestamp,
			String relation) {
		addDependency(new Dependency(filename, timestamp, relation));
	}

	public void addDependency(Dependency dependency) {
		dependencies.put(dependency, (Object)null);
	}

	public void addLink(String link) {
		links.put(link, (Object)null);
	}

	public Iterable<String> getLinks() {
		return links.keySet();
	}

	public void addAuthor(String author) {
		authors.put(author, (Object)null);
	}

	public Iterable<String> getAuthors() {
		return authors.keySet();
	}

	public Iterable<Version> getPrevious() {
		return previous.keySet();
	}

	public void addPreviousVersion(String checksum, long timestamp) {
		previous.put(new Version(checksum, timestamp), (Object)null);
	}

	public void setNoAction() {
		action = status.getNoAction();
	}

	public void setAction(String action) {
		setAction(Action.forLabel(action));
	}

	public void setAction(Action action) {
		if (!status.isValid(action))
			throw new Error("Invalid action requested for plugin "
					+ filename + "(" + action
					+ ", " + status + ")");
		if (action == Action.UPLOAD)
			markForUpload();
		this.action = action;
	}

	public void setStatus(Status status) {
		this.status = status;
		setNoAction();
	}

	private void markForUpload() {
		if (!isFiji()) {
			status = Status.INSTALLED;
			newChecksum = current.checksum;
			newTimestamp = current.timestamp;
		}
		else {
			if (status == Status.NOT_INSTALLED) {
				// an "upload" means "remove from the updater" here
				try {
					newChecksum = Util.getDigest(filename, null);
				} catch (Exception e) { e.printStackTrace(); }
				newTimestamp = 0;
				filesize = 0;
			}
			else if (newChecksum == null ||
					newChecksum.equals(current.checksum))
				throw new Error("Plugin " + filename
						+ " is already uploaded");
			addPreviousVersion(current.checksum, current.timestamp);
			current.checksum = newChecksum;
			current.timestamp = newTimestamp;
		}

		PluginCollection plugins = PluginCollection.getInstance();
		for (Dependency dependency : plugins.analyzeDependencies(this))
				addDependency(dependency);
	}

	public String getFilename() {
		return filename;
	}

	public String getChecksum() {
		return action == Action.UPLOAD ? newChecksum : current.checksum;
	}

	public long getTimestamp() {
		return action == Action.UPLOAD ?
			newTimestamp : current.timestamp;
	}

	public Iterable<Dependency> getDependencies() {
		return dependencies.keySet();
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

	// TODO: why that redundancy?  We set Action.UPDATE only if it is updateable anyway!  Besides, use getAction(). DRY, DRY, DRY!
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

	public boolean isFiji() {
		return status != Status.NOT_FIJI;
	}

	/**
	 * For displaying purposes, it is nice to have a plugin object whose
	 * toString() method shows either the filename or the action.
	 */
	public class LabeledPlugin {
		String label;

		LabeledPlugin(String label) {
			this.label = label;
		}

		public PluginObject getPlugin() {
			return PluginObject.this;
		}

		public String toString() {
			return label;
		}
	}

	public LabeledPlugin getLabeledPlugin(int column) {
		switch (column) {
		case 0: return new LabeledPlugin(getFilename());
		case 1: return new LabeledPlugin(getAction().toString());
		}
		return null;
	}
}
