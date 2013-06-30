package fiji.plugin.trackmate.visualization.hyperstack;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.panels.components.JNumericTextField;
import fiji.plugin.trackmate.visualization.hyperstack.SpotEditTool.SpotEditToolParams;
import javax.swing.JButton;

public class SpotEditToolConfigPanel extends JFrame {
	
	private static final long serialVersionUID = 1L;
	private final static ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/TrackIcon_small.png"));
	private final static ImageIcon SELECT_TRACK_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/arrow_updown.png"));
	private final static ImageIcon SELECT_TRACK_ICON_UPWARDS = new ImageIcon(TrackMateWizard.class.getResource("images/arrow_up.png"));
	private final static ImageIcon SELECT_TRACK_ICON_DOWNWARDS = new ImageIcon(TrackMateWizard.class.getResource("images/arrow_down.png"));
	private final static ImageIcon LINK_SPOTS_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/EdgeIcon_supersmall.png"));
	private final Logger logger;
	private JNumericTextField jNFQualityThreshold;
	private JNumericTextField jNFDistanceTolerance;
	private final SpotEditToolParams params;
	private Model model;

	public SpotEditToolConfigPanel(final SpotEditToolParams params) {
		this.params = params;
		
		/*
		 * Listeners
		 */
		
		ActionListener al = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateParamsFromTextFields();
			}
		};
		FocusListener fl = new FocusListener() {
			@Override
			public void focusLost(FocusEvent arg0) {
				updateParamsFromTextFields();
			}
			@Override public void focusGained(FocusEvent arg0) {}
		};
		
		
		/*
		 * GUI
		 */

		
		setTitle("TrackMate tools");
		setIconImage(ICON.getImage());
		setResizable(false);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
		
		JPanel mainPanel = new JPanel();
		getContentPane().add(mainPanel);
		mainPanel.setLayout(null);
		
		JLabel lblTitle = new JLabel("TrackMate tools");
		lblTitle.setBounds(6, 6, 395, 33);
		lblTitle.setFont(BIG_FONT);
		lblTitle.setIcon(ICON);
		mainPanel.add(lblTitle);
		
		JPanel panelSemiAutoParams = new JPanel();
		panelSemiAutoParams.setBorder(new LineBorder(new Color(252, 117, 0), 1, false));
		panelSemiAutoParams.setBounds(6, 51, 192, 85);
		mainPanel.add(panelSemiAutoParams);
		panelSemiAutoParams.setLayout(null);
		
		JLabel lblSemiAutoTracking = new JLabel("Semi-automatic tracking");
		lblSemiAutoTracking.setBounds(6, 6, 180, 16);
		lblSemiAutoTracking.setFont(FONT.deriveFont(Font.BOLD));
		panelSemiAutoParams.add(lblSemiAutoTracking);

		JLabel lblQualityThreshold = new JLabel("Quality threshold");
		lblQualityThreshold.setToolTipText("<html>" +
				"The fraction of the initial spot quality <br>" +
				"found spots must have to be considered for linking. <br>" +
				"The higher, the more stringent.</html>");
		lblQualityThreshold.setBounds(6, 35, 119, 16);
		lblQualityThreshold.setFont(SMALL_FONT);
		panelSemiAutoParams.add(lblQualityThreshold);
		
		jNFQualityThreshold = new JNumericTextField(params.qualityThreshold);
		jNFQualityThreshold.setHorizontalAlignment(SwingConstants.CENTER);
		jNFQualityThreshold.setFont(SMALL_FONT);
		jNFQualityThreshold.setBounds(137, 33, 49, 18);
		jNFQualityThreshold.addActionListener(al);
		jNFQualityThreshold.addFocusListener(fl);

		panelSemiAutoParams.add(jNFQualityThreshold);
		
		JLabel lblDistanceTolerance = new JLabel("Distance tolerance");
		lblDistanceTolerance.setToolTipText("<html>" +
				"The maximal distance above which found spots are rejected, <br>" +
				"expressed in units of the initial spot radius.</html>");
		lblDistanceTolerance.setBounds(6, 55, 119, 16);
		lblDistanceTolerance.setFont(SMALL_FONT);
		panelSemiAutoParams.add(lblDistanceTolerance);
		
		jNFDistanceTolerance = new JNumericTextField(params.distanceTolerance);
		jNFDistanceTolerance.setHorizontalAlignment(SwingConstants.CENTER);
		jNFDistanceTolerance.setFont(SMALL_FONT);
		jNFDistanceTolerance.setBounds(137, 53, 49, 18);
		jNFDistanceTolerance.addActionListener(al);
		jNFDistanceTolerance.addFocusListener(fl);
		panelSemiAutoParams.add(jNFDistanceTolerance);
		
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBounds(210, 51, 254, 246);
		mainPanel.add(scrollPane);
		
		final JTextPane textPane = new JTextPane();
		textPane.setFont(SMALL_FONT);
		textPane.setEditable(false);
		textPane.setBackground(this.getBackground());
		scrollPane.setViewportView(textPane);
		
		JPanel panelButtons = new JPanel();
		panelButtons.setBounds(6, 147, 192, 150);
		panelButtons.setBorder(new LineBorder(new Color(252, 117, 0), 1, false));
		mainPanel.add(panelButtons);
		panelButtons.setLayout(null);
		
		JLabel lblSelectionTools = new JLabel("Selection tools");
		lblSelectionTools.setFont(FONT.deriveFont(Font.BOLD));
		lblSelectionTools.setBounds(10, 11, 172, 14);
		panelButtons.add(lblSelectionTools);
		
		JButton buttonSelectTrack = new JButton(SELECT_TRACK_ICON);
		buttonSelectTrack.setBounds(10, 36, 33, 23);
		panelButtons.add(buttonSelectTrack);

		JLabel lblSelectTrack = new JLabel("Select track");
		lblSelectTrack.setBounds(53, 36, 129, 23);
		lblSelectTrack.setFont(SMALL_FONT);
		lblSelectTrack.setToolTipText("Select the whole tracks selected spots belong to.");
		panelButtons.add(lblSelectTrack);

		JButton buttonSelectTrackUp = new JButton(SELECT_TRACK_ICON_UPWARDS);
		buttonSelectTrackUp.setBounds(10, 61, 33, 23);
		panelButtons.add(buttonSelectTrackUp);

		JLabel lblSelectTrackUpward = new JLabel("Select track upward");
		lblSelectTrackUpward.setBounds(53, 61, 129, 23);
		lblSelectTrackUpward.setFont(SMALL_FONT);
		lblSelectTrackUpward.setToolTipText("<html>" +
				"Select the whole tracks selected spots <br>" +
				"belong to, backward in time.</html>");
		panelButtons.add(lblSelectTrackUpward);

		JButton buttonSelectTrackDown = new JButton(SELECT_TRACK_ICON_DOWNWARDS);
		buttonSelectTrackDown.setBounds(10, 86, 33, 23);
		panelButtons.add(buttonSelectTrackDown);

		JLabel lblSelectTrackDown = new JLabel("Select track downward");
		lblSelectTrackDown.setBounds(53, 86, 129, 23);
		lblSelectTrackDown.setFont(SMALL_FONT);
		lblSelectTrackDown.setToolTipText("<html>" +
				"Select the whole tracks selected spots <br>" +
				"belong to, forward in time.</html>");
		panelButtons.add(lblSelectTrackDown);

		JButton buttonLinkSpots = new JButton(LINK_SPOTS_ICON);
		buttonLinkSpots.setBounds(10, 111, 33, 23);
		panelButtons.add(buttonLinkSpots);
		
		JLabel lblLinkSpots = new JLabel("Link spots");
		lblLinkSpots.setFont(SMALL_FONT);
		lblLinkSpots.setToolTipText("Link selected spots, sorted in time.");
		lblLinkSpots.setBounds(53, 111, 129, 23);
		panelButtons.add(lblLinkSpots);
		
		
		
		logger = new Logger() {

			@Override
			public void error(String message) {
				log(message, Logger.ERROR_COLOR);				
			}

			@Override
			public void log(final String message, final Color color) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						textPane.setEditable(true);
						StyleContext sc = StyleContext.getDefaultStyleContext();
						AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);
						int len = textPane.getDocument().getLength();
						textPane.setCaretPosition(len);
						textPane.setCharacterAttributes(aset, false);
						textPane.replaceSelection(message);
						textPane.setEditable(false);
					}
				});
			}

			@Override
			public void setStatus(final String status) {
				log(status, Logger.GREEN_COLOR);
			}
			
			@Override
			public void setProgress(double val) {}
		};	
		
		setSize(480, 336);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setVisible(true);
	}
	
	/**
	 * Returns the {@link Logger} that outputs on this config panel.
	 * @return  the {@link Logger} instance of this panel.
	 */
	public Logger getLogger() {
		return logger;
	}
	
	/**
	 * Sets the model the tools of this panel will affect.
	 * @param model  the target model.
	 */
	public void setTargetModel(Model model) {
		this.model = model;
	}
	
	private void updateParamsFromTextFields() {
		params.distanceTolerance = jNFDistanceTolerance.getValue();
		params.qualityThreshold = jNFQualityThreshold.getValue();
	}
}
