package fiji.pluginManager.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PluginCollection extends ArrayList<PluginObject> {
	interface Filter {
		boolean matches(PluginObject plugin);
	}

	public Iterable<PluginObject> getMatchingText(String searchText) {
		return getList(new TextFilter(searchText));
	}

	public static PluginCollection clone(Iterable<PluginObject> iterable) {
		PluginCollection result = new PluginCollection();
		for (PluginObject plugin : iterable)
			result.add(plugin);
		return result;
	}

	private static class TextFilter implements Filter {
		String text;

		public TextFilter(String text) {
			this.text = text.trim().toLowerCase();
		}

		//determining whether search text fits description/title
		public boolean matches(PluginObject plugin) {
			String lcFilename = plugin.getFilename().trim().toLowerCase();
			String description = plugin.description;
			Iterable<String> links = plugin.getLinks();
			Iterable<String> authors = plugin.getAuthors();
			if (lcFilename.indexOf(text) >= 0)
				return true;
			if (description != null && description.indexOf(text) >= 0)
				return true;
			if (links != null)
				for (String link : links)
					if (link.toLowerCase().indexOf(text) >= 0) return true;
			if (authors != null)
				for (String author : authors)
					if (author.toLowerCase().indexOf(text) >= 0) return true;
			return false;
		}
	}

	public Iterable<PluginObject> toUpload() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.toUpload();
			}
		});
	}

	public Iterable<PluginObject> toUninstall() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.toRemove();
			}
		});
	}

	public Iterable<PluginObject> toUpdate() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.toUpdate();
			}
		});
	}

	public Iterable<PluginObject> toInstall() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.toInstall();
			}
		});
	}

	public Iterable<PluginObject> toInstallOrUpdate() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return (plugin.toInstall() || plugin.toUpdate());
			}
		});
	}

	public Iterable<PluginObject> fijiPlugins() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.isFiji();
			}
		});
	}

	public Iterable<PluginObject> nonFiji() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return !plugin.isFiji();
			}
		});
	}

	public class FilteredIterator implements Iterator<PluginObject> {
		Filter filter;
		Iterator<PluginObject> iterator;
		PluginObject next;

		FilteredIterator(Filter filter) {
			this.filter = filter;
			iterator = PluginCollection.this.iterator();
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

	protected Iterable<PluginObject> getList(final Filter filter) {
		return new Iterable<PluginObject>() {
			public Iterator<PluginObject> iterator() {
				return new FilteredIterator(filter);
			}
		};
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

	// TODO: remove.  ChangeStatus should never be set or read.
	public void resetChangeStatuses() {
		for (PluginObject plugin : this)
			plugin.setChangeStatus(PluginObject.ChangeStatus.NONE);
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
			if (plugin.getAction() == PluginObject.Action.UPLOAD)
				return true;
		return false;
	}

	//forces action for every plugin in the list to "install"
	public void setToInstall() {
		for (PluginObject plugin : this)
			if (plugin.isRemovableOnly() || plugin.isUpdateable())
				plugin.setNoAction();
			else
				plugin.setAction(PluginObject.Action.INSTALL);
	}

	//forces action for every update-able plugin in the list to be "update"
	public void setToUpdate() {
		for (PluginObject plugin : this)
			if (plugin.isUpdateable())
				plugin.setAction(PluginObject.Action.UPDATE);
	}

	//forces action for every plugin in the list to be "uninstall"
	public void setToRemove() {
		for (PluginObject plugin : this)
			if (plugin.isInstallable())
				plugin.setNoAction();
			else if (plugin.isRemovableOnly() || plugin.isUpdateable())
				plugin.setAction(PluginObject.Action.REMOVE);
	}
}
