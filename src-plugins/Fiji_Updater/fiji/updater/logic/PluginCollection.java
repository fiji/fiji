package fiji.updater.logic;

import fiji.updater.logic.PluginObject.Action;
import fiji.updater.logic.PluginObject.Status;

import fiji.updater.util.DependencyAnalyzer;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PluginCollection extends ArrayList<PluginObject> {
	protected PluginCollection() { }
	protected static PluginCollection instance;
	public static PluginCollection getInstance() {
		if (instance == null)
			instance = new PluginCollection();
		return instance;
	}

	static DependencyAnalyzer dependencyAnalyzer;

	interface Filter {
		boolean matches(PluginObject plugin);
	}

	public static PluginCollection clone(Iterable<PluginObject> iterable) {
		PluginCollection result = new PluginCollection();
		for (PluginObject plugin : iterable)
			result.add(plugin);
		return result;
	}

	public Iterable<PluginObject> toUpload() {
		return filter(Action.UPLOAD);
	}

	public Iterable<PluginObject> toUninstall() {
		return filter(Action.REMOVE);
	}

	public Iterable<PluginObject> toUpdate() {
		return filter(Action.UPDATE);
	}

	public Iterable<PluginObject> upToDate() {
		return filter(Action.INSTALLED);
	}

	public Iterable<PluginObject> toInstall() {
		return filter(Action.INSTALL);
	}

	public Iterable<PluginObject> toInstallOrUpdate() {
		return filter(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.getAction() == Action.INSTALL ||
					plugin.getAction() == Action.UPDATE;
			}
		});
	}

	public Iterable<PluginObject> uploadable() {
		return filter(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.getStatus()
				.isValid(Action.UPLOAD);
			}
		});
	}

	public Iterable<PluginObject> uninstalled() {
		return filter(Status.NOT_INSTALLED);
	}

	public Iterable<PluginObject> installed() {
		return filter(Status.INSTALLED);
	}

	public Iterable<PluginObject> updateable() {
		return filter(Status.UPDATEABLE);
	}

	public Iterable<PluginObject> modified() {
		return filter(Status.MODIFIED);
	}

	public Iterable<PluginObject> fijiPlugins() {
		return filterOut(Status.NOT_FIJI);
	}

	public Iterable<PluginObject> nonFiji() {
		return filter(Status.NOT_FIJI);
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

	public Iterable<PluginObject> filter(final Filter filter) {
		return filter(filter, this);
	}

	public Iterable<PluginObject> filter(final Status status) {
		return filter(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.getStatus() == status;
			}
		}, this);
	}

	public Iterable<PluginObject> filterOut(final Status status) {
		return filter(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.getStatus() != status;
			}
		}, this);
	}

	public Iterable<PluginObject> filter(final Action action) {
		return filter(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.getAction() == action;
			}
		}, this);
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

	protected class Dependencies implements Iterator<Dependency> {
		Iterator<String> iterator;
		Dependency current;
		Dependencies(Iterable<String> dependencies) {
			if (dependencies == null)
				return;
			iterator = dependencies.iterator();
			findNext();
		}

		public boolean hasNext() {
			return current != null;
		}

		public Dependency next() {
			Dependency result = current;
			findNext();
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		protected void findNext() {
			while (iterator.hasNext()) {
				PluginObject plugin =
					getPlugin(iterator.next());
				if (plugin == null)
					continue;
				current = new Dependency(plugin.getFilename(),
					plugin.getTimestamp(), "at-least");
				return;
			}
			current = null;
		}
	}

	public Iterable<Dependency> analyzeDependencies(PluginObject plugin) {
		try {
			if (dependencyAnalyzer == null)
				dependencyAnalyzer = new DependencyAnalyzer();
			final Iterable<String> dependencies = dependencyAnalyzer
				.getDependencies(plugin.getFilename());

			return new Iterable<Dependency>() {
				public Iterator<Dependency> iterator() {
					return new Dependencies(dependencies);
				}
			};
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean hasChanges() {
		for (PluginObject plugin : this)
			if (plugin.getAction() !=
					plugin.getStatus().getActions()[0])
				return true;
		return false;
	}

	public boolean hasUpload() {
		for (PluginObject plugin : this)
			if (plugin.getAction() == Action.UPLOAD)
				return true;
		return false;
	}
}
