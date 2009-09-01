package fiji.pluginManager.logic;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

/*
 * Determine the dependencies of the plugin through ADD and REMOVE scenarios.
 * The dependencies are determined based on the assumption that the user has already
 * selected the plugins he/she wanted to add or remove and indicated to take action.
 */
public class DependencyBuilder {
	private PluginCollection pluginList; //current states of all plugins

	//The different structures of the same information that the user can retrieve
	public PluginCollection changeList;
	public Map<PluginObject,PluginCollection> installDependenciesMap;
	public Map<PluginObject,PluginCollection> updateDependenciesMap;
	public Map<PluginObject,PluginCollection> uninstallDependentsMap;
	public PluginCollection toInstallList;
	public PluginCollection toUpdateList;
	public PluginCollection toRemoveList;

	public DependencyBuilder(PluginCollection pluginList) {
		this.pluginList = pluginList;
		changeList = pluginList.getNonUploadActions();
		PluginCollection change_addOrUpdateList = changeList.getToAddOrUpdate();
		PluginCollection change_removeList = changeList.getToUninstall();

		//Generates a map of plugins and their individual dependencies/dependents
		installDependenciesMap = new HashMap<PluginObject,PluginCollection>();
		updateDependenciesMap = new HashMap<PluginObject,PluginCollection>();
		uninstallDependentsMap = new HashMap<PluginObject,PluginCollection>();

		//Going through list requesting for ADD or UPDATE
		for (PluginObject myPlugin : change_addOrUpdateList) {
			//Generate lists of dependencies for each plugin
			PluginCollection toInstallList = new PluginCollection();
			PluginCollection toUpdateList = new PluginCollection();
			addDependsOn(toInstallList, toUpdateList, myPlugin);
			installDependenciesMap.put(myPlugin, toInstallList);
			updateDependenciesMap.put(myPlugin, toUpdateList);
		}

		//Going through list requesting for REMOVE
		for (PluginObject myPlugin : change_removeList) {
			//Generate lists of dependents for each plugin
			PluginCollection toRemoveList = new PluginCollection();
			addRequiredBy(toRemoveList, myPlugin);
			uninstallDependentsMap.put(myPlugin, toRemoveList);
		}

		//Combines all the dependencies for individual plugins into one list
		toInstallList = new PluginCollection();
		toUpdateList = new PluginCollection();
		toRemoveList = new PluginCollection();
		unifyInstallAndUpdateList(installDependenciesMap,
				updateDependenciesMap,
				toInstallList,
				toUpdateList);
		unifyUninstallList(uninstallDependentsMap, toRemoveList);
	}

	//comes up with a list of plugins needed for download to go with this selected plugin
	private void addDependsOn(PluginCollection changeToInstallList,
			PluginCollection changeToUpdateList, PluginObject selectedPlugin) {
		//First retrieve the dependency list
		List<Dependency> dependencyList = selectedPlugin.getDependencies();

		//Does not belong in any lists yet
		if (!changeToInstallList.contains(selectedPlugin) &&
				!changeToUpdateList.contains(selectedPlugin)) {
			if (selectedPlugin.isUpdateable()) //can only mean "update the plugin"
				changeToUpdateList.add(selectedPlugin);
			else //in context of "addDependency", can only mean "install the plugin"
				changeToInstallList.add(selectedPlugin);
		}
		//else... (Plugin already added earlier)

		//if there are no dependencies for this selected plugin
		if (dependencyList == null || dependencyList.size() == 0) {
			return;
		} else {
			//if there are dependencies, check for prerequisites
			for (Dependency dependency : dependencyList) {
				PluginObject plugin = pluginList.getPlugin(dependency.getFilename());

				//When prerequisite is found
				if (plugin != null) {
					boolean inInstallList = changeToInstallList.contains(plugin);
					boolean inUpdateList = changeToUpdateList.contains(plugin);
					//Which does not exist in any of the "change" lists yet
					if (!inInstallList && !inUpdateList) {
						//if prerequisite installed/uninstalled
						if (plugin.isRemovableOnly() || plugin.isInstallable()) {
							//add to list
							changeToInstallList.add(plugin);
							addDependsOn(changeToInstallList, changeToUpdateList, plugin);
						}
						//if prerequisite is update-able
						else if (plugin.isUpdateable()) {
							//if current dependency's plugin is outdated
							if (plugin.getTimestamp().compareTo(dependency.getTimestamp()) < 0) {
								changeToUpdateList.add(plugin); //add to update list ("special" case)
								addDependsOn(changeToInstallList, changeToUpdateList, plugin);
							} else { //if not, just remain as it is should be fine
								changeToInstallList.add(plugin);
							}
						}
					}
					//if previous "update-able" prerequisite only requires an install
					else if (inInstallList && !inUpdateList && plugin.isUpdateable()) {
						//Then check again if this current dependency's plugin is outdated
						if (plugin.getTimestamp().compareTo(dependency.getTimestamp()) < 0) {
							changeToInstallList.remove(plugin);
							changeToUpdateList.add(plugin); //add to update list ("special" case)
							addDependsOn(changeToInstallList, changeToUpdateList, plugin);
						}
					}
				}

			}
		}
	}

	//comes up with a list of plugins needed to be removed if selected is removed
	private void addRequiredBy(PluginCollection changeToUninstallList, PluginObject selectedPlugin) {
		if (!changeToUninstallList.contains(selectedPlugin)) {
			changeToUninstallList.add(selectedPlugin);
		}
		//Search through entire list
		for (PluginObject plugin : pluginList) {

			boolean inUninstallList = changeToUninstallList.contains(plugin);
			if (inUninstallList) {
				continue; //already in UninstallList ==> Its dependents are assumed to be too
			} else {
				List<Dependency> dependencyList = plugin.getDependencies();
				if (dependencyList == null || dependencyList.size() == 0) {
					//do nothing
				} else {
					for (Dependency dependency : dependencyList) {
						String dependencyFilename = dependency.getFilename();
						if (dependencyFilename.equals(selectedPlugin.getFilename())) {
							changeToUninstallList.add(plugin);
							addRequiredBy(changeToUninstallList, plugin);
							break;
						}
					}
				}
			}

		} //end of search through pluginList
	}

	private void addToListWithNoDuplicates(PluginCollection existingList, PluginCollection additional) {
		//For every plugin in this list
		for (PluginObject plugin : additional) {
			//if existing list does not contain the plugin yet, add it
			if (!existingList.contains(plugin)) {
				existingList.add(plugin);
			}
		}
	}

	//combine the mapping of installs into one single list of "to install" plugins
	//the same goes for the mapping of updates ==> "to update" list
	private void unifyInstallAndUpdateList(Map<PluginObject,PluginCollection> installDependenciesMap,
			Map<PluginObject,PluginCollection> updateDependenciesMap,
			PluginCollection installList,
			PluginCollection updateList) {

		Iterator<PluginCollection> iterInstallLists = installDependenciesMap.values().iterator();
		while (iterInstallLists.hasNext())
			addToListWithNoDuplicates(installList, iterInstallLists.next());

		Iterator<PluginCollection> iterUpdateLists = updateDependenciesMap.values().iterator();
		while (iterUpdateLists.hasNext())
			addToListWithNoDuplicates(updateList, iterUpdateLists.next());

		for (PluginObject plugin : updateList)
			installList.remove(plugin); //remove any plugin already in update list
	}

	//combine the mapping of uninstalls into one single list of "to uninstall" plugins
	private void unifyUninstallList(Map<PluginObject,PluginCollection> uninstallDependentsMap,
			PluginCollection uninstallList) {

		Iterator<PluginCollection> iterUninstallLists = uninstallDependentsMap.values().iterator();
		while (iterUninstallLists.hasNext())
			addToListWithNoDuplicates(uninstallList, iterUninstallLists.next());
	}

	private boolean conflicts(PluginCollection list1, PluginCollection list2) {
		Iterator<PluginObject> iter = list1.iterator();
		while (iter.hasNext()) {
			PluginObject thisPlugin = iter.next();
			if (list2.contains(thisPlugin)) return true;
		}
		return false;
	}

	public boolean conflicts(PluginCollection installList, PluginCollection updateList,
			PluginCollection uninstallList) {
		if (!conflicts(installList, uninstallList) && !conflicts(updateList, uninstallList))
			return false;
		else
			return true;
	}

}