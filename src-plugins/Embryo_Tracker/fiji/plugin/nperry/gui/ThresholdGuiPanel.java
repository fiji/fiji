package fiji.plugin.nperry.gui;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.EnumMap;
import java.util.Random;
import java.util.Stack;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.nperry.Feature;
import fiji.plugin.nperry.Spot;

import static fiji.plugin.nperry.gui.SpotTrackerFrame.FONT;
import static fiji.plugin.nperry.gui.SpotTrackerFrame.SMALL_FONT;



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
public class ThresholdGuiPanel extends javax.swing.JPanel implements ChangeListener {
	/**
	 * This action is fired when the "Next >>" button is pressed.
	 */
	public final ActionEvent NEXT_BUTTON_PRESSED = new ActionEvent(this, 0, "NextButtonPressed");
	/**
	 * This action is fired when the feature to color in the "Set color by feature"
	 * JComboBox is changed.
	 */
	public final ActionEvent COLOR_FEATURE_CHANGED = new ActionEvent(this, 1, "ColorFeatureChanged");
	private final ChangeEvent CHANGE_EVENT = new ChangeEvent(this);
	
	private static final long serialVersionUID = 1L;

	
	private JLabel jLabelSetColorBy;
	private JComboBox jComboBoxSetColorBy;
	private JPanel jPanelByFeature;
	private JPanel jPanelBottom;

	private JScrollPane jScrollPaneThresholds;
	private JButton jButtonRemoveThreshold;
	private JButton jButtonNext;
	private JPanel jPanelAllThresholds;
	private JButton jButtonAddThreshold;
	private JPanel jPanelButtons;

	private Stack<ThresholdPanel<Feature>> thresholdPanels = new Stack<ThresholdPanel<Feature>>();
	private Stack<Component> struts = new Stack<Component>();
	private Collection<Spot> spots;
	private EnumMap<Feature, double[]> featureValues = new EnumMap<Feature, double[]>(Feature.class);
	private int newFeatureIndex;
	
	private Feature[] features = new Feature[0];
	private double[] thresholds = new double[0];
	private boolean[] isAbove = new boolean[0];
	private ArrayList<ActionListener> actionListeners = new ArrayList<ActionListener>();
	private ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
	
	private String[] featureStringList;
	private Feature setColorByFeature;
	
	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * CONSTRUCTOR
	 */
	
	public ThresholdGuiPanel(Collection<Spot> spots, Feature selectedFeature) {
		super();
		newFeatureIndex = selectedFeature.ordinal();
		setSpots(spots);
		initGUI();
		if (null != spots)
			addThresholdPanel();
	}

	public ThresholdGuiPanel(Collection<Spot> spots) {
		this(spots, Feature.values()[0]);
	}
	
	public ThresholdGuiPanel() {
		this(null);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Set the Spot collection displayed in this GUI.
	 * <p>
	 * Calling this method causes the individual threshold panel to be all 
	 * removed.
	 */
	public void setSpots(Collection<Spot> spots) {
		for(ThresholdPanel<Feature> tp : thresholdPanels)
			jPanelAllThresholds.remove(tp);
		for(Component strut : struts)
			jPanelAllThresholds.remove(strut);
		if (null != jPanelAllThresholds)
			jPanelAllThresholds.repaint();
		this.spots = spots;
		prepareDataArrays();
	}
	

	/**
	 * Called when one of the {@link ThresholdPanel} is changed by the user.
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		thresholds = new double[thresholdPanels.size()];
		isAbove = new boolean[thresholdPanels.size()];
		features = new Feature[thresholdPanels.size()];
		int index = 0;
		for (ThresholdPanel<Feature> tp : thresholdPanels) {
			features[index] = tp.getKey();
			thresholds[index] = tp.getThreshold();
			isAbove[index] = tp.isAboveThreshold();
			index++;
		}
		fireThresholdChanged(e);
	}

	/**
	 * Return the threshold values set under this panel.
	 * @see {@link #getIsAbove()}, {@link #getFeatures()}
	 */
	public double[] getThresholds() {
		return thresholds;
	}
	
	/**
	 * Return the feature selected in the "Set color by feature" comb-box. 
	 * Return <code>null</code> if the item "Default" is selected.
	 */
	public Feature getColorByFeature() {
		return setColorByFeature;
	}
	
	/**
	 * Return whether each threshold shown under this panel is meant to
	 * be a threshold <i>above<i>. 
	 * @see {@link #getThresholds()}, {@link #getFeatures()}
	 */
	public boolean[] getIsAbove() {
		return isAbove;
	}
	
	/**
	 * Return the selected {@link Feature} thresholded under this panel.
	 * @see {@link #getThresholds()}, {@link #getIsAbove()}
	 */
	public Feature[] getFeatures() {
		return features;
	}
	
	/**
	 * Add an {@link ChangeListener} to this panel. The {@link ChangeListener} will
	 * be notified when a change happens to the thresholds displayed by this panel, whether
	 * due to the slider being move, the auto-threshold button being pressed, or
	 * the combo-box selection being changed.
	 */
	public void addChangeListener(ChangeListener listener) {
		changeListeners.add(listener);
	}
	
	/**
	 * Remove a ChangeListener from this panel. 
	 * @return true if the listener was in listener collection of this instance.
	 */
	public boolean removeChangeListener(ChangeListener listener) {
		return changeListeners.remove(listener);
	}
	
	public Collection<ChangeListener> getChangeListeners() {
		return changeListeners;
	}
	
	/**
	 * Add an {@link ActionListener} to this panel. These listeners will be notified when
	 * a button is pushed or when the feature to color is changed.
	 */
	public void addActionListener(ActionListener listener) {
		actionListeners.add(listener);
	}
	
	/**
	 * Remove an ActionListener from this panel. 
	 * @return true if the listener was in the ActionListener collection of this instance.
	 */
	public boolean removeActionListener(ActionListener listener) {
		return actionListeners.remove(listener);
	}
	
	public Collection<ActionListener> getActionListeners() {
		return actionListeners;
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void setColorByFeature() {
		int selection = jComboBoxSetColorBy.getSelectedIndex();
		if (selection == 0) 
			setColorByFeature = null;
		else
			setColorByFeature = Feature.values()[selection-1];
		fireAction(COLOR_FEATURE_CHANGED);
	}
	
	private void fireAction(ActionEvent e) {
		for (ActionListener l : actionListeners)
			l.actionPerformed(e);
	}
	
	private void fireThresholdChanged(ChangeEvent e) {
		for (ChangeListener cl : changeListeners) 
			cl.stateChanged(e);
	}
	
	private void prepareDataArrays() {
		if (null == spots)
			return;
		int index;
		Float val;
		boolean noDataFlag = true;
		for(Feature feature : Feature.values()) {
			// Make a double array to comply to JFreeChart histograms
			double[] values = new double[spots.size()];
			index = 0;
			for (Spot spot : spots) {
				val = spot.getFeature(feature);
				if (null == val)
					continue;
				values[index] = val; 
				index++;
				noDataFlag = false;
			}
			if (noDataFlag)
				featureValues.put(feature, null);
			else 
				featureValues.put(feature, values);
		}
	}
	
	
	public void addThresholdPanel() {
		addThresholdPanel(Feature.values()[newFeatureIndex]);		
	}
	
	public void addThresholdPanel(Feature feature) {
		if (null == featureValues)
			return;
		ThresholdPanel<Feature> tp = new ThresholdPanel<Feature>(featureValues, feature);
		tp.addChangeListener(this);
		newFeatureIndex++;
		if (newFeatureIndex >= Feature.values().length) 
			newFeatureIndex = 0;
		Component strut = Box.createVerticalStrut(5);
		struts.push(strut);
		thresholdPanels.push(tp);
		jPanelAllThresholds.add(tp);
		jPanelAllThresholds.add(strut);
		jPanelAllThresholds.revalidate();
		fireThresholdChanged(CHANGE_EVENT);
	}
	
	private void removeThresholdPanel() {
		try {
			ThresholdPanel<Feature> tp = thresholdPanels.pop();
			tp.addChangeListener(this);
			Component strut = struts.pop();
			jPanelAllThresholds.remove(strut);
			jPanelAllThresholds.remove(tp);
			jPanelAllThresholds.repaint();
			fireThresholdChanged(CHANGE_EVENT);
		} catch (EmptyStackException ese) {	}
	}
	
	private void initGUI() {
		try {
			BorderLayout thisLayout = new BorderLayout();
			this.setLayout(thisLayout);
			setPreferredSize(new Dimension(270, 500));
			{
				jScrollPaneThresholds = new JScrollPane();
				this.add(jScrollPaneThresholds, BorderLayout.CENTER);
				jScrollPaneThresholds.setPreferredSize(new java.awt.Dimension(250, 389));
				jScrollPaneThresholds.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				jScrollPaneThresholds.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				{
					jPanelAllThresholds = new JPanel();
					BoxLayout jPanelAllThresholdsLayout = new BoxLayout(jPanelAllThresholds, BoxLayout.Y_AXIS);
					jPanelAllThresholds.setLayout(jPanelAllThresholdsLayout);
					jScrollPaneThresholds.setViewportView(jPanelAllThresholds);
				}
			}
			{
				jPanelBottom = new JPanel();
				BorderLayout jPanelBottomLayout = new BorderLayout();
				jPanelBottom.setLayout(jPanelBottomLayout);
				this.add(jPanelBottom, BorderLayout.SOUTH);
				jPanelBottom.setPreferredSize(new java.awt.Dimension(270, 80));
				{
					jPanelButtons = new JPanel();
					jPanelBottom.add(jPanelButtons, BorderLayout.SOUTH);
					BoxLayout jPanelButtonsLayout = new BoxLayout(jPanelButtons, javax.swing.BoxLayout.X_AXIS);
					jPanelButtons.setLayout(jPanelButtonsLayout);
					jPanelButtons.setPreferredSize(new java.awt.Dimension(270, 36));
					{
						jPanelButtons.add(Box.createHorizontalStrut(5));
						jButtonAddThreshold = new JButton();
						jPanelButtons.add(jButtonAddThreshold);
						jButtonAddThreshold.setText("+");
						jButtonAddThreshold.setFont(SMALL_FONT);
						jButtonAddThreshold.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								addThresholdPanel();
							}
						});
					}
					{
						jPanelButtons.add(Box.createHorizontalStrut(5));
						jButtonRemoveThreshold = new JButton();
						jPanelButtons.add(jButtonRemoveThreshold);
						jButtonRemoveThreshold.setText("-");
						jButtonRemoveThreshold.setFont(SMALL_FONT);
						jButtonRemoveThreshold.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								removeThresholdPanel();
							}
						});
					}
					{
						jPanelButtons.add(Box.createHorizontalGlue());
						jButtonNext = new JButton();
						jPanelButtons.add(jButtonNext);
						jButtonNext.setText("Next >>");
						jButtonNext.setFont(FONT);
						jButtonNext.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								fireAction(NEXT_BUTTON_PRESSED);
							}
						});
						jPanelButtons.add(Box.createHorizontalStrut(5));
					}
				}
				{
					jPanelByFeature = new JPanel();
					BoxLayout jPanelByFeatureLayout = new BoxLayout(jPanelByFeature, javax.swing.BoxLayout.X_AXIS);
					jPanelByFeature.setLayout(jPanelByFeatureLayout);
					jPanelBottom.add(jPanelByFeature, BorderLayout.CENTER);
					jPanelByFeature.setPreferredSize(new java.awt.Dimension(270, 43));
					{
						jPanelByFeature.add(Box.createHorizontalStrut(5));
						jLabelSetColorBy = new JLabel();
						jPanelByFeature.add(jLabelSetColorBy);
						jLabelSetColorBy.setText("Set color by");
						jLabelSetColorBy.setFont(SMALL_FONT);
					}
					{
						Feature[] allFeatures = Feature.values();
						featureStringList = new String[allFeatures.length+1];
						featureStringList[0] = "Default";
						for (int i = 0; i < allFeatures.length; i++) 
							featureStringList[i+1] = allFeatures[i].toString();
						ComboBoxModel jComboBoxSetColorByModel = new DefaultComboBoxModel(featureStringList);
						jComboBoxSetColorBy = new JComboBox();
						jPanelByFeature.add(Box.createHorizontalStrut(5));
						jPanelByFeature.add(jComboBoxSetColorBy);
						jPanelByFeature.add(Box.createHorizontalStrut(5));
						jComboBoxSetColorBy.setModel(jComboBoxSetColorByModel);
						jComboBoxSetColorBy.setFont(SMALL_FONT);
						jComboBoxSetColorBy.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								setColorByFeature();
							}
						});
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * MAIN METHOD
	 */
	
	/**
	* Auto-generated main method to display this 
	* JPanel inside a new JFrame.
	 * @throws IOException 
	*/
	public static void main(String[] args) throws IOException {
		// Generate fake Spot data
		final int NSPOT = 100;
		final Random rn = new Random();
		ArrayList<Spot> spots = new ArrayList<Spot>(NSPOT);
		Spot spot;
		for (int i = 0; i < NSPOT; i++) {
			spot = new Spot(new float[] {0, 0, 0});
			for (Feature feature : Feature.values())
				spot.putFeature(feature, (float) (rn.nextGaussian()+5));
			spots.add(spot);
		}
		
		// Generate GUI
		ThresholdGuiPanel gui = new ThresholdGuiPanel();
		JFrame frame = new JFrame();
		frame.getContentPane().add(gui);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		
		System.out.println("Type <Enter> to ad spots to this");
		System.in.read();
		gui.setSpots(spots);
		
	}

	


}
