package fiji.plugin.trackmate.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JScrollPane;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.tracking.TrackerSettings;


public class LAPTrackerSettingsPanel <T extends RealType<T> & NativeType<T>> extends TrackerConfigurationPanel<T> {

	private static final long serialVersionUID = 1L;
	private JPanelTrackerSettingsMain<T> jPanelMain;

	/*
	 * CONSTRUCTOR
	 */

	public LAPTrackerSettingsPanel() {
		initGUI();
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public TrackerSettings<T> getTrackerSettings() {
		return jPanelMain.getSettings();
	}

	@Override
	public void setTrackerSettings(TrackMateModel<T> model) {
		jPanelMain.echoSettings(model);
	}

	/*
	 * PRIVATE METHODS
	 */

	private void initGUI() {
		try {
			BorderLayout thisLayout = new BorderLayout();
			setPreferredSize(new Dimension(300, 500));
			this.setLayout(thisLayout);
			{
				JScrollPane jScrollPaneMain = new JScrollPane();
				this.add(jScrollPaneMain, BorderLayout.CENTER);
				jScrollPaneMain.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				jScrollPaneMain.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				jScrollPaneMain.getVerticalScrollBar().setUnitIncrement(24);
				{
					jPanelMain = new JPanelTrackerSettingsMain<T>();
					jScrollPaneMain.setViewportView(jPanelMain);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
