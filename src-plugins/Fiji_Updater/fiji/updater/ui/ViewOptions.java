package fiji.updater.ui;

import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginObject;

import javax.swing.JComboBox;

public class ViewOptions extends JComboBox {
	static enum Option {
		ALL("all plugins"),
		INSTALLED("installed plugins only"),
		UNINSTALLED("uninstalled plugins only"),
		UPTODATE("up-to-date plugins only"),
		UPDATEABLE("update-able plugins only"),
		FIJI("Fiji plugins only"),
		OTHERS("non-Fiji plugins only"),
		CHANGES("changes");

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
	}

	public Iterable<PluginObject> getView() {
		PluginCollection plugins = PluginCollection.getInstance();
		switch ((Option)getSelectedItem()) {
			case INSTALLED: return plugins.installed();
			case UNINSTALLED: return plugins.uninstalled();
			case UPTODATE: return plugins.upToDate();
			case UPDATEABLE: return plugins.updateable();
			case FIJI: return plugins.fijiPlugins();
			case OTHERS: return plugins.nonFiji();
			case CHANGES: return plugins.changes();
			default: return plugins;
		}
	}
}
