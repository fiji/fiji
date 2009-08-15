package fiji.pluginManager.logic;
import java.util.ArrayList;
import java.util.List;

public class PluginCollection extends ArrayList<PluginObject> {
	private interface Filter {
		boolean matches(PluginObject plugin);
	}

	public PluginCollection getMatchingText(String searchText) {
		return getList(new TextFilter(searchText));
	}

	private static class TextFilter implements Filter {
		String text;

		public TextFilter(String text) {
			this.text = text.trim().toLowerCase();
		}

		//determining whether search text fits description/title
		public boolean matches(PluginObject plugin) {
			String lcFilename = plugin.getFilename().trim().toLowerCase();
			String description = plugin.getPluginDetails().getDescription();
			List<String> links = plugin.getPluginDetails().getLinks();
			List<String> authors = plugin.getPluginDetails().getAuthors();
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

	//take in only plugins that are neither installed nor told to do so
	public PluginCollection getUnlistedForInstall() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				boolean actionNone = (plugin.isInstallable() && !plugin.actionSpecified());
				return plugin.toRemove() || actionNone || plugin.toUpload();
			}
		});
	}

	//take in only update-able plugins not instructed to update
	public PluginCollection getUnlistedForUpdate() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				boolean actionNoUpdate = plugin.isUpdateable() && !plugin.toUpdate();
				return actionNoUpdate || plugin.toUpload();
			}
		});
	}

	//take in only plugins that are not instructed to uninstall
	public PluginCollection getUnlistedForUninstall() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				boolean actionNotRemove = plugin.isRemovable() && !plugin.toRemove();
				return actionNotRemove || plugin.toInstall() || plugin.toUpload();
			}
		});
	}

	public PluginCollection getActionsSpecified() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.actionSpecified();
			}
		});
	}

	public PluginCollection getNonUploadActions() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.actionSpecified() && !plugin.toUpload();
			}
		});
	}

	public PluginCollection getToUpload() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.toUpload();
			}
		});
	}

	public PluginCollection getToUninstall() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.toRemove();
			}
		});
	}

	public PluginCollection getToUpdate() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.toUpdate();
			}
		});
	}

	public PluginCollection getToInstall() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.toInstall();
			}
		});
	}

	public PluginCollection getToAddOrUpdate() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return (plugin.toInstall() || plugin.toUpdate());
			}
		});
	}

	public PluginCollection getStatusesInstalled() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.isRemovable();
			}
		});
	}

	public PluginCollection getStatusesUninstalled() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.isInstallable();
			}
		});
	}

	public PluginCollection getStatusesFullyUpdated() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.isRemovableOnly();
			}
		});
	}

	public PluginCollection getStatusesUpdateable() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.isUpdateable();
			}
		});
	}

	public PluginCollection getFijiPlugins() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.isFijiPlugin();
			}
		});
	}

	public PluginCollection getNonFiji() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return !plugin.isFijiPlugin();
			}
		});
	}

	public PluginCollection getChangeSucceeded() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.changeSucceeded();
			}
		});
	}

	public PluginCollection getChangeFailed() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.changeFailed();
			}
		});
	}

	public PluginCollection getNoChangeYet() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.changeNotDone();
			}
		});
	}

	public PluginCollection getNoSuccessfulChanges() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return !plugin.changeSucceeded();
			}
		});
	}

	public PluginCollection getSuccessfulDownloads() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return (plugin.toInstall() || plugin.toUpdate()) && plugin.changeSucceeded();
			}
		});
	}

	public PluginCollection getFailedDownloads() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return (plugin.toInstall() || plugin.toUpdate()) && plugin.changeFailed();
			}
		});
	}

	public PluginCollection getSuccessfulRemoves() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.toRemove() && plugin.changeSucceeded();
			}
		});
	}

	public PluginCollection getFailedRemovals() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.toRemove() && plugin.changeFailed();
			}
		});
	}

	public PluginCollection getReadOnly() {
		return getList(new Filter() {
			public boolean matches(PluginObject plugin) {
				return plugin.isReadOnly();
			}
		});
	}

	private PluginCollection getList(Filter filter) {
		PluginCollection list = new PluginCollection();
		for (PluginObject plugin : this)
			if (filter.matches(plugin))
				list.add(plugin);
		return list;
	}

	public PluginObject getPlugin(String filename) { //filename is unique identifier
		for (PluginObject plugin : this) {
			if (plugin.getFilename().equals(filename))
				return plugin;
		}
		return null;
	}

	public PluginObject getPluginFromTimestamp(String filename, String timestamp) {
		for (PluginObject plugin : this)
			if (plugin.getFilename().equals(filename) && plugin.getTimestamp().equals(timestamp))
				return plugin;
		return null;
	}

	public PluginObject getPluginFromDigest(String filename, String digest) {
		for (PluginObject plugin : this)
			if (plugin.getFilename().equals(filename) &&
					plugin.getmd5Sum().equals(digest))
				return plugin;
		return null;
	}

	//this method assumes list of plugins are of the same filename (i.e.: different versions)
	public PluginObject getLatestPlugin() {
		PluginObject latest = null;
		for (PluginObject plugin : this)
			if (latest == null || plugin.getTimestamp().compareTo(latest.getTimestamp()) > 0)
				latest = plugin;
		return latest;
	}

	public void resetChangeStatuses() {
		for (PluginObject plugin : this)
			plugin.setChangeStatus(PluginObject.ChangeStatus.NONE);
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
