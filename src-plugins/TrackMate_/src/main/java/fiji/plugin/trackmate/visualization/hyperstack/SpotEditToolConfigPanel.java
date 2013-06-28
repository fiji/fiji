package fiji.plugin.trackmate.visualization.hyperstack;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
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
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.panels.components.JNumericTextField;
import fiji.plugin.trackmate.visualization.hyperstack.SpotEditTool.SpotEditToolParams;
import javax.swing.border.LineBorder;

public class SpotEditToolConfigPanel extends JFrame {
	
	private static final long serialVersionUID = 1L;
	private final static ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/TrackIcon_small.png"));
	private final Logger logger;
	private JNumericTextField jNFNeighborhoodSize;
	private JNumericTextField jNFQualityThreshold;
	private JNumericTextField jNFDistanceTolerance;
	private final SpotEditToolParams params;

	public SpotEditToolConfigPanel(final SpotEditToolParams params, Point point) {
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

		
		setTitle("TrackMate edit parameters");
		setIconImage(ICON.getImage());
		setResizable(false);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
		
		JPanel mainPanel = new JPanel();
		getContentPane().add(mainPanel);
		mainPanel.setLayout(null);
		
		JLabel lblTitle = new JLabel("TrackMate edit parameters");
		lblTitle.setBounds(6, 6, 395, 33);
		lblTitle.setFont(BIG_FONT);
		lblTitle.setIcon(ICON);
		mainPanel.add(lblTitle);
		
		JPanel panelSemiAutoParams = new JPanel();
		panelSemiAutoParams.setBorder(new LineBorder(new Color(252, 117, 0), 1, true));
		panelSemiAutoParams.setBounds(6, 51, 192, 184);
		mainPanel.add(panelSemiAutoParams);
		panelSemiAutoParams.setLayout(null);
		
		JLabel lblSemiAutoTracking = new JLabel("Semi-automatic tracking");
		lblSemiAutoTracking.setBounds(6, 6, 180, 16);
		lblSemiAutoTracking.setFont(FONT.deriveFont(Font.BOLD));
		panelSemiAutoParams.add(lblSemiAutoTracking);

		JLabel lblNeighborhoodFactor = new JLabel("Neighborhood size");
		lblNeighborhoodFactor.setToolTipText("<html>" +
				"The size of the neighborhood to inspect <br>" +
				"with the semi automatic tracker. Specified <br>" +
				"in units of the initial spot diameter.</html>");
		lblNeighborhoodFactor.setBounds(6, 28, 119, 16);
		lblNeighborhoodFactor.setFont(SMALL_FONT);
		panelSemiAutoParams.add(lblNeighborhoodFactor);
		
		jNFNeighborhoodSize = new JNumericTextField(params.neighborhoodFactor);
		jNFNeighborhoodSize.setFont(SMALL_FONT);
		jNFNeighborhoodSize.setBounds(137, 26, 49, 18);
		jNFNeighborhoodSize.addActionListener(al);
		jNFNeighborhoodSize.addFocusListener(fl);
		panelSemiAutoParams.add(jNFNeighborhoodSize);
		
		JLabel lblQualityThreshold = new JLabel("Quality threshold");
		lblQualityThreshold.setToolTipText("<html>" +
				"The fraction of the initial spot quality <br>" +
				"found spots must have to be considered for linking. <br>" +
				"The higher, the more stringent.</html>");
		lblQualityThreshold.setBounds(6, 48, 119, 16);
		lblQualityThreshold.setFont(SMALL_FONT);
		panelSemiAutoParams.add(lblQualityThreshold);
		
		jNFQualityThreshold = new JNumericTextField(params.qualityThreshold);
		jNFQualityThreshold.setFont(SMALL_FONT);
		jNFQualityThreshold.setBounds(137, 46, 49, 18);
		jNFQualityThreshold.addActionListener(al);
		jNFQualityThreshold.addFocusListener(fl);

		panelSemiAutoParams.add(jNFQualityThreshold);
		
		JLabel lblDistanceTolerance = new JLabel("Distance tolerance");
		lblDistanceTolerance.setToolTipText("<html>" +
				"The maximal distance above which found spots are rejected, <br>" +
				"expressed in units of the initial spot radius.</html>");
		lblDistanceTolerance.setBounds(6, 68, 119, 16);
		lblDistanceTolerance.setFont(SMALL_FONT);
		panelSemiAutoParams.add(lblDistanceTolerance);
		
		jNFDistanceTolerance = new JNumericTextField(params.distanceTolerance);
		jNFDistanceTolerance.setFont(SMALL_FONT);
		jNFDistanceTolerance.setBounds(137, 66, 49, 18);
		jNFDistanceTolerance.addActionListener(al);
		jNFDistanceTolerance.addFocusListener(fl);
		panelSemiAutoParams.add(jNFDistanceTolerance);
		
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBounds(210, 51, 191, 184);
		mainPanel.add(scrollPane);
		
		final JTextPane textPane = new JTextPane();
		textPane.setFont(SMALL_FONT);
		textPane.setEditable(false);
		textPane.setBackground(this.getBackground());
		scrollPane.setViewportView(textPane);
		
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
		
		setSize(408, 262);
		setLocation(point);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}
	
	public Logger getLogger() {
		return logger;
	}
	
	private void updateParamsFromTextFields() {
		params.distanceTolerance = jNFDistanceTolerance.getValue();
		params.neighborhoodFactor = jNFNeighborhoodSize.getValue();
		params.qualityThreshold = jNFQualityThreshold.getValue();
	}
}
