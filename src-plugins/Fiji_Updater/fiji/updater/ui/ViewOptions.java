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

	protected final int customOptionStart;

	public ViewOptions() {
		super(Option.values());

		customOptionStart = getItemCount();

		setMaximumRowCount(15);
	}

	public void clearCustomOptions() {
		while (getItemCount() > customOptionStart)
			removeItemAt(customOptionStart);
	}

	protected interface CustomOption {
		Iterable<PluginObject> getIterable();
	}

	public void addCustomOption(final String title, final Iterable<PluginObject> iterable) {
		addItem(new CustomOption() {
			public String toString() {
				return title;
			}

			public Iterable<PluginObject> getIterable() {
				return iterable;
			}
		});
	}

	public Iterable<PluginObject> getView(PluginTable table) {
		if (getSelectedIndex() >= customOptionStart)
			return ((CustomOption)getSelectedItem()).getIterable();

		PluginCollection plugins = PluginCollection
			.clone(table.getAllPlugins().notHidden());
		plugins.sort();
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