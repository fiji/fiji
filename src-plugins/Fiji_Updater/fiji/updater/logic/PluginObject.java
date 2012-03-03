package fiji.updater.logic;

import fiji.updater.util.Util;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PluginObject {
	public static class Version implements Comparable<Version> {
		public String checksum;
		// This timestamp is not a Unix epoch!
		// Instead, it is Long.parseLong(Util.timestamp(epoch))
		public long timestamp;

		Version(String checksum, long timestamp) {
			this.checksum = checksum;
			this.timestamp = timestamp;
		}

		@Override
		public int compareTo(Version other) {
			long diff = timestamp - other.timestamp;
			if (diff != 0)
				return diff < 0 ? -1 : +1;
			return checksum.compareTo(other.checksum);
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof Version ? equals((Version)other) : false;
		}

		public boolean equals(Version other) {
			return timestamp == other.timestamp && checksum.equals(other.checksum);
		}

		@Override
		public int hashCode() {
			return (checksum == null ? 0 : checksum.hashCode())
				^ new Long(timestamp).hashCode();
		}

		@Override
		public String toString() {
			return "Version(" + checksum + ";" + timestamp + ")";
		}
	}

	public static enum Action {
		// no changes
		NOT_FIJI ("Not in Fiji"),
		NOT_INSTALLED ("Not installed"),
		INSTALLED ("Up-to-date"),
		UPDATEABLE ("Update available"),
		MODIFIED ("Locally modified"),
		NEW ("New plugin"),
		OBSOLETE ("Obsolete"),

		// changes
		UNINSTALL ("Uninstall it"),
		INSTALL ("Install it"),
		UPDATE ("Update it"),
		// TODO: FORCE_UPDATE

		// developer-only changes
		UPLOAD ("Upload it"),
		REMOVE ("Remove it");

		private String label;
		Action(String label) {
			this.label = label;
		}

		public String toString() {
			return label;
		}
	};

	public static enum Status {
		NOT_INSTALLED (new Action[] { Action.NOT_INSTALLED, Action.INSTALL }, Action.REMOVE),
		INSTALLED (new Action[] { Action.INSTALLED, Action.UNINSTALL }),
		UPDATEABLE (new Action[] { Action.UPDATEABLE, Action.UNINSTALL, Action.UPDATE }, Action.UPLOAD),
		MODIFIED (new Action[] { Action.MODIFIED, Action.UNINSTALL, Action.UPDATE }, Action.UPLOAD),
		NOT_FIJI (new Action[] { Action.NOT_FIJI, Action.UNINSTALL }, Action.UPLOAD),
		NEW (new Action[] { Action.NEW, Action.INSTALL, Action.REMOVE }),
		OBSOLETE_UNINSTALLED (new Action[] { Action.OBSOLETE }),
		OBSOLETE (new Action[] { Action.OBSOLETE, Action.UNINSTALL }, Action.UPLOAD),
		OBSOLETE_MODIFIED (new Action[] { Action.MODIFIED, Action.UNINSTALL }, Action.UPLOAD);

		private Action[] actions, developerActions;

		Status(Action[] actions) {
			this(actions, null);
		}

		Status(Action[] actions, Action developerAction) {
			if (developerAction != null) {
				developerActions = new Action[actions.length + 1];
				System.arraycopy(actions, 0, developerActions, 0, actions.length);
				developerActions[actions.length] = developerAction;
			}
			else
				developerActions = actions;
			this.actions = actions;
		}

		public Action[] getActions() {
			return actions;
		}

		public Action[] getDeveloperActions() {
			return developerActions;
		}

		public boolean isValid(Action action) {
			for (Action a : developerActions)
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
	public String updateSite, filename, description, newChecksum;
	public boolean executable;
	public Version current;
	public Set<Version> previous;
	public long filesize, newTimestamp;
	public boolean metadataChanged;

	// These are LinkedHashMaps to retain the order of the entries
	protected Map<String, Dependency> dependencies;
	protected Map<String, Object> links, authors, platforms, categories;

	public PluginObject(String updateSite, String filename, String checksum, long timestamp,
			Status status) {
		assert(updateSite != null && !updateSite.equals(""));
		this.updateSite = updateSite;
		this.filename = filename;
		if (checksum != null)
			current = new Version(checksum, timestamp);
		previous = new LinkedHashSet<Version>();
		this.status = status;
		dependencies = new LinkedHashMap<String, Dependency>();
		authors = new LinkedHashMap<String, Object>();
		platforms = new LinkedHashMap<String, Object>();
		categories = new LinkedHashMap<String, Object>();
		links = new LinkedHashMap<String, Object>();
		if (status == Status.NOT_FIJI)
			filesize = Util.getFilesize(filename);
		setNoAction();
	}

	public void merge(PluginObject upstream) {
		for (Version previous : upstream.previous)
			addPreviousVersion(previous.checksum, previous.timestamp);
		if (updateSite == null || updateSite.equals(upstream.updateSite)) {
			updateSite = upstream.updateSite;
			description = upstream.description;
			dependencies = upstream.dependencies;
			authors = upstream.authors;
			platforms = upstream.platforms;
			categories = upstream.categories;
			links = upstream.links;
			filesize = upstream.filesize;
			executable = upstream.executable;
			if (current != null && !upstream.hasPreviousVersion(current.checksum))
				addPreviousVersion(current.checksum, current.timestamp);
			current = upstream.current;
			status = upstream.status;
			action = upstream.action;
		}
		else {
			Version other = upstream.current;
			if (other != null && !hasPreviousVersion(other.checksum))
				addPreviousVersion(other.checksum, other.timestamp);
		}
	}

	public boolean hasPreviousVersion(String checksum) {
		if (current != null && current.checksum.equals(checksum))
			return true;
		for (Version version : previous)
			if (version.checksum.equals(checksum))
				return true;
		return false;
	}

	public boolean isNewerThan(long timestamp) {
		if (current != null && current.timestamp <= timestamp)
			return false;
		for (Version version : previous)
			if (version.timestamp <= timestamp)
				return false;
		return true;
	}

	void setVersion(String checksum, long timestamp) {
		if (current != null)
			previous.add(current);
		current = new Version(checksum, timestamp);
	}

	public void setLocalVersion(String checksum, long timestamp) {
		if (current != null && checksum.equals(current.checksum)) {
			status = Status.INSTALLED;
			setNoAction();
			return;
		}
		status = hasPreviousVersion(checksum) ?
			(current == null ?
			 Status.OBSOLETE : Status.UPDATEABLE) :
			(current == null ?
			 Status.OBSOLETE_MODIFIED : Status.MODIFIED);
		setNoAction();
		newChecksum = checksum;
		newTimestamp = timestamp;
	}

	public String getDescription() {
		return description;
	}

	// TODO: allow editing those via GUI
	public void addDependency(String filename) {
		addDependency(filename, Util.getTimestamp(filename), false);
	}

	public void addDependency(String filename, long timestamp,
			boolean overrides) {
		addDependency(new Dependency(filename, timestamp, overrides));
	}

	public void addDependency(Dependency dependency) {
		// the timestamp should not be changed unnecessarily
		if (dependency.filename == null ||
				"".equals(dependency.filename.trim()) ||
				dependencies.containsKey(dependency.filename))
			return;
		dependencies.put(dependency.filename, dependency);
	}

	public void removeDependency(String other) {
		dependencies.remove(other);
	}

	public boolean hasDependency(String filename) {
		return dependencies.containsKey(filename);
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

	public void addPlatform(String platform) {
		if (platform.equals("linux"))
			platform = "linux32";
		if (platform != null && !platform.trim().equals(""))
			platforms.put(platform.trim(), (Object)null);
	}

	public Iterable<String> getPlatforms() {
		return platforms.keySet();
	}

	public void addCategory(String category) {
		categories.put(category, (Object)null);
	}

	public void replaceList(String tag, String[] list) {
		if (tag.equals("Dependency")) {
			long now = Long.parseLong(Util.timestamp(new Date()
						.getTime()));
			Dependency[] newList = new Dependency[list.length];
			for (int i = 0; i < list.length; i++) {
				boolean obsoleted = false;
				String item = list[i].trim();
				if (item.startsWith("obsoletes ")) {
					item = item.substring(10);
					obsoleted = true;
				}
				Dependency dep = dependencies.get(item);
				if (dep == null)
					dep = new Dependency(item,
							now, obsoleted);
				else if (dep.overrides != obsoleted) {
					dep.timestamp = now;
					dep.overrides = obsoleted;
				}
				newList[i] = dep;
			}
			dependencies.clear();
			for (Dependency dep : newList)
				addDependency(dep);
			return;
		}

		Map<String, Object> map =
			tag.equals("Link") ? links :
			tag.equals("Author") ? authors :
			tag.equals("Platform") ? platforms :
			tag.equals("Category") ? categories :
			null;
		map.clear();
		for (String string : list)
			map.put(string.trim(), (Object)null);
	}

	public Iterable<String> getCategories() {
		return categories.keySet();
	}

	public Iterable<Version> getPrevious() {
		return previous;
	}

	public void addPreviousVersion(String checksum, long timestamp) {
		Version version = new Version(checksum, timestamp);
		if (!previous.contains(version))
			previous.add(version);
	}

	public void setNoAction() {
		action = status.getNoAction();
	}

	public void setAction(PluginCollection plugins, Action action) {
		if (!status.isValid(action))
			throw new Error("Invalid action requested for plugin "
					+ filename + "(" + action
					+ ", " + status + ")");
		if (action == Action.UPLOAD) {
			Iterable<String> dependencies =
				plugins.analyzeDependencies(this);
			if (dependencies != null)
				for (String dependency : dependencies)
					addDependency(dependency);
		}
		this.action = action;
	}

	public boolean setFirstValidAction(PluginCollection plugins, Action[] actions) {
		for (Action action : actions)
			if (status.isValid(action)) {
				setAction(plugins, action);
				return true;
			}
		return false;
	}

	public void setStatus(Status status) {
		this.status = status;
		setNoAction();
	}

	public void markUploaded() {
		if (!isFiji()) {
			status = Status.INSTALLED;
			newChecksum = current.checksum;
			newTimestamp = current.timestamp;
		}
		else if (isObsolete() || status == Status.UPDATEABLE) {
			/* force re-upload */
			status = Status.INSTALLED;
			setVersion(newChecksum, newTimestamp);
		}
		else {
			if (newChecksum == null ||
					newChecksum.equals(current.checksum))
				throw new Error("Plugin " + filename
						+ " is already uploaded");
			setVersion(newChecksum, newTimestamp);
		}
	}

	public void markRemoved() {
		addPreviousVersion(current.checksum, current.timestamp);
		setStatus(Status.OBSOLETE);
		current = null;
	}

	public String getFilename() {
		return filename;
	}

	public String getChecksum() {
		return action == Action.UPLOAD ? newChecksum :
			action == Action.REMOVE || current == null ?
			null : current.checksum;
	}

	public long getTimestamp() {
		return action == Action.UPLOAD ? newTimestamp :
			action == Action.REMOVE || current == null ?
			0 : current.timestamp;
	}

	public Iterable<Dependency> getDependencies() {
		return dependencies.values();
	}

	public Status getStatus() {
		return status;
	}

	public Action getAction() {
		return action;
	}

	public boolean isInstallable() {
		return status.isValid(Action.INSTALL);
	}

	public boolean isUpdateable() {
		return status.isValid(Action.UPDATE);
	}

	public boolean isUninstallable() {
		return status.isValid(Action.UNINSTALL);
	}

	public boolean isLocallyModified() {
		return status.getNoAction() == Action.MODIFIED;
	}

	/**
	 * Tell whether this plugin can be uploaded to its update site
	 *
	 * Note: this does not check whether the plugin is locally modified.
	 */
	public boolean isUploadable(PluginCollection plugins) {
		if (updateSite == null)
			return plugins.hasUploadableSites();
		PluginCollection.UpdateSite updateSite = plugins.getUpdateSite(this.updateSite);
		return updateSite != null && updateSite.isUploadable();
	}

	public boolean actionSpecified() {
		return action != status.getNoAction();
	}

	public boolean toUpdate() {
		return action == Action.UPDATE;
	}

	public boolean toUninstall() {
		return action == Action.UNINSTALL;
	}

	public boolean toInstall() {
		return action == Action.INSTALL;
	}

	public boolean toUpload() {
		return action == Action.UPLOAD;
	}

	public boolean isObsolete() {
		switch (status) {
		case OBSOLETE:
		case OBSOLETE_MODIFIED:
		case OBSOLETE_UNINSTALLED:
			return true;
		}
		return false;
	}

	public boolean isForPlatform(String platform) {
		return platforms.containsKey(platform);
	}

	public boolean isForThisPlatform() {
		return platforms.size() == 0 || isForPlatform(Util.platform);
	}

	public boolean isUpdateablePlatform() {
		if (platforms.size() == 0)
			return true;
		for (String platform : platforms.keySet())
			if (Util.isUpdateablePlatform(platform))
				return true;
		return false;
	}

	public boolean isFiji() {
		return status != Status.NOT_FIJI;
	}

	/* This returns true if the user marked the plugin for uninstall, too */
	public boolean willNotBeInstalled() {
		switch (action) {
		case NOT_INSTALLED: case NEW: case OBSOLETE: case UNINSTALL:
		case REMOVE:
			return true;
		case NOT_FIJI: case INSTALLED: case UPDATEABLE: case MODIFIED:
		case INSTALL: case UPDATE: case UPLOAD:
			return false;
		default:
			throw new RuntimeException("Unhandled action: "
					+ action);
		}
	}

	/* This returns true if the user marked the plugin for uninstall, too */
	public boolean willBeUpToDate() {
		switch (action) {
		case OBSOLETE: case REMOVE: case NOT_INSTALLED: case NEW:
		case UPDATEABLE: case MODIFIED: case UNINSTALL:
			return false;
		case INSTALLED: case INSTALL: case UPDATE: case UPLOAD:
		case NOT_FIJI:
			return true;
		default:
			throw new RuntimeException("Unhandled action: "
					+ action);
		}
	}

	// TODO: this needs a better name; something like wantsAction()
	public boolean isUpdateable(boolean evenForcedUpdates) {
		return action == Action.UPDATE ||
			action == Action.INSTALL ||
			status == Status.UPDATEABLE ||
			status == Status.OBSOLETE ||
			(evenForcedUpdates &&
			 (status.isValid(Action.UPDATE) ||
			  status == Status.OBSOLETE_MODIFIED));
	}

	public void stageForUninstall() throws IOException {
		if (action != Action.UNINSTALL)
			throw new RuntimeException(filename + " was not marked "
				+ "for uninstall");
		if (filename.endsWith(".jar"))
			touch(Util.prefixUpdate(filename));
		else {
			String old = filename + ".old";
			if (old.endsWith(".exe.old"))
				old = old.substring(0, old.length() - 8) + ".old.exe";
			new File(Util.prefix(filename)).renameTo(new File(Util.prefix(old)));
			touch(Util.prefixUpdate(old));
		}
		if (status != Status.NOT_FIJI)
			setStatus(isObsolete() ? Status.OBSOLETE_UNINSTALLED
					: Status.NOT_INSTALLED);
	}

	public static void touch(String target) throws IOException {
		File file = new File(target);
		if (file.exists()) {
			long now = new Date().getTime();
			file.setLastModified(now);
		}
		else {
			File parent = file.getParentFile();
			if (!parent.exists())
				parent.mkdirs();
			file.createNewFile();
		}
        }

	public String toDebug() {
		return filename + "(" + status + ", " + action + ")";
	}

	public String toString() {
		return filename;
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
