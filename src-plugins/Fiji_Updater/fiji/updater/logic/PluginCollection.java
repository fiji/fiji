package fiji.updater.logic;

import fiji.updater.logic.PluginObject.Action;
import fiji.updater.logic.PluginObject.Status;

import fiji.updater.util.DependencyAnalyzer;

import fiji.updater.util.Util;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PluginCollection extends ArrayList<PluginObject> {
	protected PluginCollection() { }
	protected static PluginCollection instance;
	public static PluginCollection getInstance() {
		if (instance == null)
			instance = new PluginCollection();
		return instance;
	}

	static DependencyAnalyzer dependencyAnalyzer;

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
		return filter(is(Action.UPLOAD));
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
				return plugin.getStatus()
				.isValid(Action.UPLOAD);
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
		if (Util.isDeveloper)
			return yes();
		return new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.isForThisPlatform();
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
					plugin.isForThisPlatform();
			}
		});
	}

	public void markForUpdate(boolean evenForcedUpdates) {
		PluginObject updater = getPlugin("plugins/Fiji_Updater.jar");
		if (updater != null &&
				updater.getStatus() == Status.UPDATEABLE) {
			updater.setAction(Action.UPDATE);
			return;
		}
		for (PluginObject plugin : updateable(evenForcedUpdates))
			plugin.setFirstValidAction(new Action[] {
				Action.UPDATE, Action.UNINSTALL, Action.INSTALL
			});
		for (String name : Util.getLaunchers()) {
			PluginObject launcher = getPlugin(name);
			if (launcher == null)
				continue; // the regression test triggers this
			if (launcher.getStatus() == Status.NOT_INSTALLED)
				launcher.setAction(Action.INSTALL);
		}
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
					|| !other.isForThisPlatform())
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
		// first letters in this order: 'f', 'i', 'p', 's', 'm', 'j'
		Collections.sort(this, new Comparator<PluginObject>() {
			public int compare(PluginObject a, PluginObject b) {
				int result = firstChar(a) - firstChar(b);
				return result != 0 ? result :
					a.filename.compareTo(b.filename);
			}

			int firstChar(PluginObject plugin) {
				char c = plugin.filename.charAt(0);
				return "fips".indexOf(c) < 0 ? 0x200 - c : c;
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
