package fiji.plugin.trackmate.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import javax.swing.JScrollPane;


public class LAPTrackerSettingsPanel extends ConfigurationPanel {

	private static final long serialVersionUID = 1L;
	private JPanelTrackerSettingsMain jPanelMain;
	private final String trackerName;
	private final String spaceUnits;
	private final List<String> features;
	private final Map<String, String> featureNames;

	/*
	 * CONSTRUCTOR
	 */

	public LAPTrackerSettingsPanel(final String trackerName, final String spaceUnits, final List<String> features, final Map<String, String> featureNames) {
		this.trackerName = trackerName;
		this.spaceUnits = spaceUnits;
		this.features = features;
		this.featureNames = featureNames;
		initGUI();
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public Map<String, Object> getSettings() {
		return jPanelMain.getSettings();
	}

	@Override
	public void setSettings(final Map<String, Object> settings) {
		jPanelMain.echoSettings(settings);
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
					jPanelMain = new JPanelTrackerSettingsMain(trackerName, spaceUnits, features, featureNames);
					jScrollPaneMain.setViewportView(jPanelMain);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
