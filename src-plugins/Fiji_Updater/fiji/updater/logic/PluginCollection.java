package fiji.updater.logic;

import fiji.updater.logic.PluginObject.Action;
import fiji.updater.logic.PluginObject.Status;

import fiji.updater.util.DependencyAnalyzer;
import fiji.updater.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.SAXException;

public class PluginCollection extends ArrayList<PluginObject> {
	public final static String DEFAULT_UPDATE_SITE = "Fiji";

	public static class UpdateSite {
		public String url, sshHost, uploadDirectory;
		public long timestamp;

		public UpdateSite(String url, String sshHost, String uploadDirectory, long timestamp) {
			if (url.equals("http://pacific.mpi-cbg.de/update/")) {
				url = Util.MAIN_URL;
				if (sshHost != null && sshHost.equals("pacific.mpi-cbg.de"))
					sshHost = Util.SSH_HOST;
				else if (sshHost != null && sshHost.endsWith("@pacific.mpi-cbg.de"))
					sshHost = sshHost.substring(0, sshHost.length() - 18) + Util.SSH_HOST;
			}
			if (!url.endsWith("/"))
				url += "/";
			if (uploadDirectory != null && !uploadDirectory.equals("") && !uploadDirectory.endsWith("/"))
				uploadDirectory += "/";
			this.url = url;
			this.sshHost = sshHost;
			this.uploadDirectory = uploadDirectory;
			this.timestamp = timestamp;
		}

		public boolean isLastModified(long lastModified) {
			return timestamp == Long.parseLong(Util.timestamp(lastModified));
		}

		public void setLastModified(long lastModified) {
			timestamp = Long.parseLong(Util.timestamp(lastModified));
		}

		public boolean isUploadable() {
			return uploadDirectory != null && !uploadDirectory.equals("");
		}

		public String toString() {
			return url + (sshHost != null ? ", " + sshHost : "")
				+ (uploadDirectory != null ? ", " + uploadDirectory : "");
		}
	}

	protected Map<String, UpdateSite> updateSites;

	public PluginCollection() {
		updateSites = new LinkedHashMap<String, UpdateSite>();
		addUpdateSite(DEFAULT_UPDATE_SITE, Util.MAIN_URL,
			Util.isDeveloper ? Util.SSH_HOST : null,
			Util.isDeveloper ? Util.UPDATE_DIRECTORY : null,
			Util.getTimestamp(Util.XML_COMPRESSED));
	}

	public void addUpdateSite(String name, String url, String sshHost, String uploadDirectory, long timestamp) {
		updateSites.put(name, new UpdateSite(url, sshHost, uploadDirectory, timestamp));
	}

	public void renameUpdateSite(String oldName, String newName) {
		if (getUpdateSite(newName) != null)
			throw new RuntimeException("Update site " + newName + " exists already!");
		if (getUpdateSite(oldName) == null)
			throw new RuntimeException("Update site " + oldName + " does not exist!");

		// handle all plugins
		for (PluginObject plugin : this)
			if (plugin.updateSite.equals(oldName))
				plugin.updateSite = newName;

		// preserve order
		HashMap<String, UpdateSite> newMap = new LinkedHashMap<String, UpdateSite>();
		for (String name : updateSites.keySet())
			if (name.equals(oldName))
				newMap.put(newName, getUpdateSite(oldName));
			else
				newMap.put(name, getUpdateSite(name));

		updateSites = newMap;
	}

	public void removeUpdateSite(String name) {
		updateSites.remove(name);
	}

	public UpdateSite getUpdateSite(String name) {
		if (name == null)
			return null;
		return updateSites.get(name);
	}

	public Collection<String> getUpdateSiteNames() {
		return updateSites.keySet();
	}

	public Collection<String> getSiteNamesToUpload() {
		Collection<String> set = new HashSet<String>();
		for (PluginObject plugin : toUpload(true))
			set.add(plugin.updateSite);
		// keep the update sites' order
		List<String> result = new ArrayList<String>();
		for (String name : getUpdateSiteNames())
			if (set.contains(name))
				result.add(name);
		if (result.size() != set.size())
			throw new RuntimeException("Unknown update site in "
				+ set.toString() + " (known: "
				+ result.toString() + ")");
		return result;
	}

	public boolean hasUploadableSites() {
		for (String name : updateSites.keySet())
			if (getUpdateSite(name).isUploadable())
				return true;
		return false;
	}

	public Action[] getActions(PluginObject plugin) {
		return plugin.isUploadable(this) ?
			plugin.getStatus().getDeveloperActions() :
			plugin.getStatus().getActions();
	}

	public Action[] getActions(Iterable<PluginObject> plugins) {
		List<Action> result = null;
		int count = 0;
		for (PluginObject plugin : plugins) {
			Action[] actions = getActions(plugin);
			if (result == null) {
				result = new ArrayList<Action>();
				for (Action action : actions)
					result.add(action);
			}
			else {
				Set<Action> set = new TreeSet<Action>();
				for (Action action : actions)
					set.add(action);
				Iterator iter = result.iterator();
				while (iter.hasNext())
					if (!set.contains(iter.next()))
						iter.remove();
			}
		}
		return result.toArray(new Action[result.size()]);
	}

	public void read() throws IOException, ParserConfigurationException, SAXException {
		new XMLFileReader(this).read(new File(Util.prefix(Util.XML_COMPRESSED)));
	}

	public void write() throws IOException, SAXException, TransformerConfigurationException, ParserConfigurationException {
		new XMLFileWriter(this).write(new GZIPOutputStream(new FileOutputStream(Util.prefix(Util.XML_COMPRESSED))), true);
	}

	protected static DependencyAnalyzer dependencyAnalyzer;

	public interface Filter {
		boolean matches(PluginObject plugin);
	}

	public static PluginCollection clone(Iterable<PluginObject> iterable) {
		PluginCollection result = new PluginCollection();
		for (PluginObject plugin : iterable)
			result.add(plugin);
		return result;
	}

	public Iterable<PluginObject> toUploadOrRemove() {
		return filter(or(is(Action.UPLOAD), is(Action.REMOVE)));
	}

	public Iterable<PluginObject> toUpload() {
		return toUpload(false);
	}

	public Iterable<PluginObject> toUpload(boolean includeMetadataChanges) {
		if (!includeMetadataChanges)
			return filter(is(Action.UPLOAD));
		return filter(or(is(Action.UPLOAD), new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.metadataChanged;
			}
		}));
	}

	public Iterable<PluginObject> toUpload(String updateSite) {
		return filter(and(is(Action.UPLOAD), isUpdateSite(updateSite)));
	}

	public Iterable<PluginObject> toUninstall() {
		return filter(is(Action.UNINSTALL));
	}

	public Iterable<PluginObject> toRemove() {
		return filter(is(Action.REMOVE));
	}

	public Iterable<PluginObject> toUpdate() {
		return filter(is(Action.UPDATE));
	}

	public Iterable<PluginObject> upToDate() {
		return filter(is(Action.INSTALLED));
	}

	public Iterable<PluginObject> toInstall() {
		return filter(is(Action.INSTALL));
	}

	public Iterable<PluginObject> toInstallOrUpdate() {
		return filter(oneOf(new Action[] {Action.INSTALL,
					Action.UPDATE}));
	}

	public Iterable<PluginObject> notHidden() {
		return filter(and(not(is(Status.OBSOLETE_UNINSTALLED)),
				 doesPlatformMatch()));
	}

	public Iterable<PluginObject> uninstalled() {
		return filter(is(Status.NOT_INSTALLED));
	}

	public Iterable<PluginObject> installed() {
		return filter(not(oneOf(new Status[] {Status.NOT_FIJI,
						Status.NOT_INSTALLED})));
	}

	public Iterable<PluginObject> locallyModified() {
		return filter(oneOf(new Status[] {Status.MODIFIED,
					Status.OBSOLETE_MODIFIED}));
	}

	public Iterable<PluginObject> forUpdateSite(String name) {
		return filter(isUpdateSite(name));
	}

	public Iterable<PluginObject> fijiPlugins() {
		return filter(not(is(Status.NOT_FIJI)));
	}

	public Iterable<PluginObject> forCurrentTXT() {
		return filter(and(not(oneOf(new Status[] {
				Status.NOT_FIJI, Status.OBSOLETE,
				Status.OBSOLETE_MODIFIED,
				Status.OBSOLETE_UNINSTALLED
			/* the old updater will only checksum these! */
			})), or(startsWith("fiji-"),
				and(startsWith(new String[] {
					"plugins/", "jars/", "retro/", "misc/"
					}), endsWith(".jar")))));
	}

	public Iterable<PluginObject> nonFiji() {
		return filter(is(Status.NOT_FIJI));
	}

	public Iterable<PluginObject> shownByDefault() {
		/*
		 * Let's not show the NOT_INSTALLED ones, as the user chose not
		 * to have them.
		 */
		Status[] oneOf = {
			Status.UPDATEABLE, Status.NEW,
			Status.OBSOLETE, Status.OBSOLETE_MODIFIED
		};
		return filter(or(oneOf(oneOf), is(Action.INSTALL)));
	}


	public Iterable<PluginObject> uploadable() {
		return filter(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.isUploadable(PluginCollection.this);
			}
		});
	}

	public Iterable<PluginObject> changes() {
		return filter(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.getAction() !=
					plugin.getStatus().getActions()[0];
			}
		});
	}

	public static class FilteredIterator implements Iterator<PluginObject> {
		Filter filter;
		boolean opposite;
		Iterator<PluginObject> iterator;
		PluginObject next;

		FilteredIterator(Filter filter,
				Iterable<PluginObject> plugins) {
			this.filter = filter;
			iterator = plugins.iterator();
			findNext();
		}

		public boolean hasNext() {
			return next != null;
		}

		public PluginObject next() {
			PluginObject plugin = next;
			findNext();
			return plugin;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		protected void findNext() {
			while (iterator.hasNext()) {
				next = iterator.next();
				if (filter.matches(next))
					return;
			}
			next = null;
		}
	}

	public static Iterable<PluginObject> filter(final Filter filter,
			final Iterable<PluginObject> plugins) {
		return new Iterable<PluginObject>() {
			public Iterator<PluginObject> iterator() {
				return new FilteredIterator(filter, plugins);
			}
		};
	}

	public static Iterable<PluginObject> filter(final String search,
			final Iterable<PluginObject> plugins) {
		final String keyword = search.trim().toLowerCase();
		return filter(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.getFilename().trim().toLowerCase()
					.indexOf(keyword) >= 0;
			}
		}, plugins);
	}

	public Filter yes() {
		return new Filter() {
			public boolean matches(PluginObject plugin) {
				return true;
			}
		};
	}

	public Filter doesPlatformMatch() {
		// If we're a developer or no platform was specified, return yes
		if (hasUploadableSites())
			return yes();
		return new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.isUpdateablePlatform();
			}
		};
	}

	public Filter is(final Action action) {
		return new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.getAction() == action;
			}
		};
	}

	public Filter isNoAction() {
		return new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.getAction() ==
					plugin.getStatus().getNoAction();
			}
		};
	}

	public Filter oneOf(final Action[] actions) {
		final Set<Action> oneOf = new HashSet<Action>();
		for (Action action : actions)
			oneOf.add(action);
		return new Filter() {
			public boolean matches(PluginObject plugin) {
				return oneOf.contains(plugin.getAction());
			}
		};
	}

	public Filter is(final Status status) {
		return new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.getStatus() == status;
			}
		};
	}

	public Filter isUpdateSite(final String updateSite) {
		return new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.updateSite != null && // is null for non-Fiji files
					plugin.updateSite.equals(updateSite);
			}
		};
	}

	public Filter oneOf(final Status[] states) {
		final Set<Status> oneOf = new HashSet<Status>();
		for (Status status : states)
			oneOf.add(status);
		return new Filter() {
			public boolean matches(PluginObject plugin) {
				return oneOf.contains(plugin.getStatus());
			}
		};
	}

	public Filter startsWith(final String prefix) {
		return new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.filename.startsWith(prefix);
			}
		};
	}

	public Filter startsWith(final String[] prefixes) {
		return new Filter() {
			public boolean matches(PluginObject plugin) {
				for (String prefix : prefixes)
					if (plugin.filename.startsWith(prefix))
						return true;
				return false;
			}
		};
	}

	public Filter endsWith(final String suffix) {
		return new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.filename.endsWith(suffix);
			}
		};
	}

	public Filter not(final Filter filter) {
		return new Filter() {
			public boolean matches(PluginObject plugin) {
				return !filter.matches(plugin);
			}
		};
	}

	public Filter or(final Filter a, final Filter b) {
		return new Filter() {
			public boolean matches(PluginObject plugin) {
				return a.matches(plugin) || b.matches(plugin);
			}
		};
	}

	public Filter and(final Filter a, final Filter b) {
		return new Filter() {
			public boolean matches(PluginObject plugin) {
				return a.matches(plugin) && b.matches(plugin);
			}
		};
	}

	public Iterable<PluginObject> filter(final Filter filter) {
		return filter(filter, this);
	}

	public PluginObject getPlugin(String filename) {
		for (PluginObject plugin : this) {
			if (plugin.getFilename().equals(filename))
				return plugin;
		}
		return null;
	}

	public PluginObject getPlugin(String filename, long timestamp) {
		for (PluginObject plugin : this)
			if (plugin.getFilename().equals(filename) &&
					plugin.getTimestamp() == timestamp)
				return plugin;
		return null;
	}

	public PluginObject getPluginFromDigest(String filename, String digest) {
		for (PluginObject plugin : this)
			if (plugin.getFilename().equals(filename) &&
					plugin.getChecksum().equals(digest))
				return plugin;
		return null;
	}

	public Iterable<String> analyzeDependencies(PluginObject plugin) {
		try {
			if (dependencyAnalyzer == null)
				dependencyAnalyzer = new DependencyAnalyzer();
			String path = Util.prefix(plugin.getFilename());
			return dependencyAnalyzer.getDependencies(path);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void updateDependencies(PluginObject plugin) {
		Iterable<String> dependencies = analyzeDependencies(plugin);
		if (dependencies == null)
			return;
		for (String dependency : dependencies)
			plugin.addDependency(dependency);
	}

	public boolean has(final Filter filter) {
		for (PluginObject plugin : this)
			if (filter.matches(plugin))
				return true;
		return false;
	}

	public boolean hasChanges() {
		return has(not(isNoAction()));
	}

	public boolean hasUploadOrRemove() {
		return has(oneOf(new Action[] {Action.UPLOAD, Action.REMOVE}));
	}

	public boolean hasForcableUpdates() {
		for (PluginObject plugin : updateable(true))
			if (!plugin.isUpdateable(false))
				return true;
		return false;
	}

	public Iterable<PluginObject> updateable(final boolean evenForcedOnes) {
		return filter(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.isUpdateable(evenForcedOnes) &&
					plugin.isUpdateablePlatform();
			}
		});
	}

	public void markForUpdate(boolean evenForcedUpdates) {
		for (PluginObject plugin : updateable(evenForcedUpdates)) {
			if (Util.isDeveloper && Util.isLauncher(plugin.filename))
				continue;
			plugin.setFirstValidAction(this, new Action[] {
				Action.UPDATE, Action.UNINSTALL, Action.INSTALL
			});
		}
		if (!Util.isDeveloper)
			for (String name : Util.launchers) {
				PluginObject launcher = getPlugin(name);
				if (launcher == null)
					continue; // the regression test triggers this
				if (launcher.getStatus() == Status.NOT_INSTALLED && launcher.isForThisPlatform())
					launcher.setAction(this, Action.INSTALL);
			}
	}

	public String getURL(PluginObject plugin) {
		String siteName = plugin.updateSite;
		assert(siteName != null && !siteName.equals(""));
		UpdateSite site = getUpdateSite(siteName);
		return site.url + plugin.filename.replace(" ", "%20") + "-" + plugin.getTimestamp();
	}

	public static class DependencyMap
			extends HashMap<PluginObject, PluginCollection> {
		// returns true when the map did not have the dependency before
		public boolean add(PluginObject dependency,
				PluginObject dependencee) {
			if (containsKey(dependency)) {
				get(dependency).add(dependencee);
				return false;
			}
			PluginCollection list = new PluginCollection();
			list.add(dependencee);
			put(dependency, list);
			return true;
		}
	}

	// TODO: for developers, there should be a consistency check:
	// no dependencies on non-Fiji plugins, no circular dependencies,
	// and no overring circular dependencies.
	void addDependencies(PluginObject plugin, DependencyMap map,
			boolean overriding) {
		for (Dependency dependency : plugin.getDependencies()) {
			PluginObject other = getPlugin(dependency.filename);
			if (other == null || overriding != dependency.overrides
					|| !other.isUpdateablePlatform())
				continue;
			if (dependency.overrides) {
				if (other.willNotBeInstalled())
					continue;
			}
			else if (other.willBeUpToDate())
				continue;
			if (!map.add(other, plugin))
				continue;
			// overriding dependencies are not recursive
			if (!overriding)
				addDependencies(other, map, overriding);
		}
	}

	public DependencyMap getDependencies(boolean overridingOnes) {
		DependencyMap result = new DependencyMap();
		for (PluginObject plugin : toInstallOrUpdate())
			addDependencies(plugin, result, overridingOnes);
		return result;
	}

	public void sort() {
		// first letters in this order: 'f', 'p', 'j', 's', 'i', 'm'
		Collections.sort(this, new Comparator<PluginObject>() {
			public int compare(PluginObject a, PluginObject b) {
				int result = firstChar(a) - firstChar(b);
				return result != 0 ? result :
					a.filename.compareTo(b.filename);
			}

			int firstChar(PluginObject plugin) {
				char c = plugin.filename.charAt(0);
				int index =  "fpjsim".indexOf(c);
				return index < 0 ? 0x200 + c : index;
			}
		});
	}

	String checkForCircularDependency(PluginObject plugin,
			Set<PluginObject> seen) {
		if (seen.contains(plugin))
			return "";
		String result = checkForCircularDependency(plugin, seen,
				new HashSet<PluginObject>());
		if (result == null)
			return "";

		// Display only the circular dependency
		int last = result.lastIndexOf(' ');
		int off = result.lastIndexOf(result.substring(last), last - 1);
		return "Circular dependency detected: "
			+ result.substring(off + 1) + "\n";
	}

	String checkForCircularDependency(PluginObject plugin,
			Set<PluginObject> seen,
			Set<PluginObject> chain) {
		if (seen.contains(plugin))
			return null;
		for (String dependency : plugin.dependencies.keySet()) {
			PluginObject dep = getPlugin(dependency);
			if (dep == null)
				continue;
			if (chain.contains(dep))
				return " " + dependency;
			chain.add(dep);
			String result =
				checkForCircularDependency(dep, seen, chain);
			seen.add(dep);
			if (result != null)
				return " " + dependency + " ->" + result;
			chain.remove(dep);
		}
		return null;
	}

	/* returns null if consistent, error string when not */
	public String checkConsistency() {
		StringBuilder result = new StringBuilder();
		Set<PluginObject> circularChecked = new HashSet<PluginObject>();
		for (PluginObject plugin : this) {
			result.append(checkForCircularDependency(plugin,
					circularChecked));
			// only non-obsolete components can have dependencies
			Set<String> deps = plugin.dependencies.keySet();
			if (deps.size() > 0 && plugin.isObsolete())
				result.append("Obsolete plugin " + plugin
					+ "has dependencies: "
					+ Util.join(", ", deps) + "!\n");
			for (String dependency : deps) {
				PluginObject dep = getPlugin(dependency);
				if (dep == null || dep.current == null)
					result.append("The plugin " + plugin
						+ " has the obsolete/non-Fiji "
						+ "dependency "
						+ dependency + "!\n");
			}
		}
		return result.length() > 0 ? result.toString() : null;
	}

	public String toString() {
		return Util.join(", ", this);
	}
}
