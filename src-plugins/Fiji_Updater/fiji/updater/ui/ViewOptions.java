package fiji.updater.ui;

import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginObject;

import fiji.updater.logic.PluginObject.Status;

import javax.swing.JComboBox;

public class ViewOptions extends JComboBox {
	public static enum Option {
		ALL("all plugins"),
		INSTALLED("installed plugins only"),
		UNINSTALLED("uninstalled plugins only"),
		UPTODATE("only up-to-date plugins"),
		UPDATEABLE("updateable plugins only"),
		LOCALLY_MODIFIED("locally modified plugins only"),
		FIJI("Fiji plugins only"),
		OTHERS("non-Fiji plugins only"),
		CHANGES("changes"),
		SELECTED("selected");

		String label;
		Option(String label) {
			this.label = "View " + label;
		}

		public String toString() {
			return label;
		}
	}

	public ViewOptions() {
		super(Option.values());
		setMaximumRowCount(15);
	}

	public Iterable<PluginObject> getView(PluginTable table) {
		PluginCollection plugins = PluginCollection
			.clone(PluginCollection.getInstance().notHidden());
		switch ((Option)getSelectedItem()) {
			case INSTALLED: return plugins.installed();
			case UNINSTALLED: return plugins.uninstalled();
			case UPTODATE: return plugins.upToDate();
			case UPDATEABLE: return plugins.shownByDefault();
			case LOCALLY_MODIFIED: return plugins.locallyModified();
			case FIJI: return plugins.fijiPlugins();
			case OTHERS: return plugins.nonFiji();
			case CHANGES: return plugins.changes();
			case SELECTED: return table.getSelectedPlugins();
			default: return plugins;
		}
	}
}
