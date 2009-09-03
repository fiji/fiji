package fiji.pluginManager.logic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

// TODO: this class has to be simplified dramatically.
// TODO: this class is misnamed: it is a conflict resolver, not a dependency builder

/*
 * Determine the dependencies of the plugin through ADD and REMOVE scenarios.
 * The dependencies are determined based on the assumption that the user has
 * already selected the plugins he/she wanted to add or remove and indicated to
 * take action.
 */
public class DependencyBuilder {
	private PluginCollection pluginList;

	public DependencyBuilder(PluginCollection pluginList) {
		this.pluginList = pluginList;
	}

	public boolean conflicts() {
		return true; // TODO
	}

}
