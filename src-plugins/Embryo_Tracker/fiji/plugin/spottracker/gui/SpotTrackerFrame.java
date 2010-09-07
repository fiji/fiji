package fiji.plugin.spottracker.gui;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import fiji.plugin.spottracker.Logger;
import fiji.plugin.spottracker.Settings;
import fiji.plugin.spottracker.Spot_Tracker;


/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class SpotTrackerFrame extends javax.swing.JFrame {

	static final Font FONT = new Font("Arial", Font.PLAIN, 11);
	static final Font SMALL_FONT = FONT.deriveFont(10f);
	static enum GuiState {
		START,
		SEGMENTING,
		THRESHOLD_BLOBS;
	};
	
	private static final long serialVersionUID = 1L;
	private static final String START_DIALOG_KEY = "Start";
	private static final String THRESHOLD_GUI_KEY = "Threshold";
	private static final String LOG_PANEL_KEY = "Log";

	private StartDialogPanel startDialogPanel;
	private ThresholdGuiPanel thresholdGuiPanel;
	private CardLayout cardLayout;
	private GuiState state;
	private Settings settings;
	private Spot_Tracker spotTracker;
	private LogPanel logPanel;
	private Logger logger;
	
	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * CONSTRUCTORS
	 */
	
	public SpotTrackerFrame(Spot_Tracker plugin) {
		if (null == plugin)
			plugin = new Spot_Tracker();
		this.spotTracker = plugin;
		initGUI();
	}
	
	public SpotTrackerFrame() {
		this(new Spot_Tracker());
	}
	
	private void next() {
		switch(state) {
			case START:
				state = GuiState.SEGMENTING;
				cardLayout.show(getContentPane(), LOG_PANEL_KEY);
				settings = startDialogPanel.updateSettings(settings);
				logger = logPanel.getLogger();
				logger.log("Starting segmentation...\n", Logger.BLUE_COLOR);
				new Thread() {					
					public void run() {
						long start = System.currentTimeMillis();
						try {
							spotTracker.setLogger(logger);
							logPanel.jButtonNext.setEnabled(false);
							spotTracker.execSegmentation(settings);
						} catch (Exception e) {
							logger.error("An error occured:\n"+e.getMessage());
						} finally {
							logPanel.jButtonNext.setEnabled(true);
							long end = System.currentTimeMillis();
							logger.log(String.format("Segmentation done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
						}
					}
				}.start();
				break;
				
			case SEGMENTING:
				cardLayout.show(getContentPane(), THRESHOLD_GUI_KEY);
				break;
				
			case THRESHOLD_BLOBS:
				break;
				
		}
	}
	
	private void recolorBlobs() {
		// TODO
	}
	
	private void initGUI() {
		try {
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			this.setTitle("Spot Tracker");
			cardLayout = new CardLayout();
			getContentPane().setLayout(cardLayout);
			this.setResizable(false);
			pack();
			this.setSize(300, 520);
			{
				startDialogPanel = new StartDialogPanel();
				startDialogPanel.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						next();
					}
				});
				getContentPane().add(startDialogPanel, START_DIALOG_KEY);
			}
			{
				logPanel = new LogPanel();
				logPanel.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						next();
					}
				});
				getContentPane().add(logPanel, LOG_PANEL_KEY);
			}
			{
				thresholdGuiPanel = new ThresholdGuiPanel();
				thresholdGuiPanel.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (e == thresholdGuiPanel.NEXT_BUTTON_PRESSED)
							next();
						else
							recolorBlobs();
					}
				});
				getContentPane().add(thresholdGuiPanel, THRESHOLD_GUI_KEY);
			}
			cardLayout.show(getContentPane(), START_DIALOG_KEY);
			state = GuiState.START;
		} catch (Exception e) {
			e.printStackTrace();
		}
		repaint();
		validate();
	}


	/**
	 * Auto-generated main method to display this JFrame
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				SpotTrackerFrame inst = new SpotTrackerFrame();
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
			}
		});
	}
	
}
