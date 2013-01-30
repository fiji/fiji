package fiji.plugin.trackmate.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

/**
 * A view for the TrackMate_ plugin, strongly inspired from the spots detection GUI of the Imaris® software 
 * from Bitplane ({@link http://www.bitplane.com/}).
 * 
 * @author Jean-Yves Tinevez <tinevez@pasteur.fr> - September 2010 - 2011
 */
public class TrackMateWizard extends javax.swing.JFrame implements ActionListener {

	JButton jButtonSave;
	JButton jButtonLoad;
	JButton jButtonPrevious;
	JButton jButtonNext;

	/*
	 * DEFAULT VISIBILITY & PUBLIC CONSTANTS
	 */

	public static final Font FONT = new Font("Arial", Font.PLAIN, 10);
	public static final Font BIG_FONT = new Font("Arial", Font.PLAIN, 14);
	public static final Font SMALL_FONT = FONT.deriveFont(8);
	static final Dimension TEXTFIELD_DIMENSION = new Dimension(40,18);

	/*
	 * PRIVATE CONSTANTS
	 */

	private static final long serialVersionUID = -4092131926852771798L;
	private static final Icon NEXT_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/arrow_right.png"));
	private static final Icon PREVIOUS_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/arrow_left.png"));
	private static final Icon LOAD_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/page_go.png"));
	private static final Icon SAVE_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/page_save.png"));
	private static final ImageIcon TRACKMATE_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/TrackIcon_small.png"));
	private static final Icon LOG_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/information.png"));;

	/*
	 * DEFAULT VISIBILITY FIELDS
	 */

	/** This {@link ActionEvent} is fired when the 'next' button is pressed. */
	final ActionEvent NEXT_BUTTON_PRESSED = new ActionEvent(this, 0, "NextButtonPressed");
	/** This {@link ActionEvent} is fired when the 'previous' button is pressed. */
	final ActionEvent PREVIOUS_BUTTON_PRESSED = new ActionEvent(this, 1, "PreviousButtonPressed");
	/** This {@link ActionEvent} is fired when the 'load' button is pressed. */
	final ActionEvent LOAD_BUTTON_PRESSED = new ActionEvent(this, 2, "LoadButtonPressed");
	/** This {@link ActionEvent} is fired when the 'save' button is pressed. */
	final ActionEvent SAVE_BUTTON_PRESSED = new ActionEvent(this, 3, "SaveButtonPressed");
	/** This {@link ActionEvent} is fired when the 'log' button is pressed. */
	final ActionEvent LOG_BUTTON_PRESSED = new ActionEvent(this, 4, "LogButtonPressed");

	/*
	 * FIELDS
	 */

	private WizardPanelDescriptor currentDescriptor;
	private HashMap<String, WizardPanelDescriptor> descriptorHashmap = new HashMap<String, WizardPanelDescriptor>();
	private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();

	private JPanel jPanelButtons;
	private JPanel jPanelMain;
	private LogPanel logPanel;
	private CardLayout cardLayout;
	private TrackMateModelView displayer;
	private Component component;
	private final WizardController controller;
	/** The stored 4 button states. It has default visibility so that states can be tweaked by the controller. */ 
	boolean[] storedButtonState;
	private JButton jButtonLog;


	/*
	 * CONSTRUCTOR
	 */

	public TrackMateWizard(Component component, WizardController controller) {
		this.component = component;
		this.controller = controller;
		initGUI();
		positionWindow();
	}

	/*
	 * PUBLIC METHODS
	 */

	/** Expose the controller managing this GUI. */
	public WizardController getController() {
		return controller;
	}

	/** 
	 * Add an {@link ActionListener} to the list of listeners of this GUI, that will be notified 
	 * when one the of push buttons is pressed.
	 */
	public void addActionListener(ActionListener listener) {
		listeners.add(listener);
	}

	/** 
	 * Remove an {@link ActionListener} from the list of listeners of this GUI.
	 * @return  true if the listener was present in the list for this GUI and was successfully removed from it.
	 */
	public boolean removeActionListener(ActionListener listener) {
		return listeners.remove(listener);
	}

	/** 
	 * Return a {@link Logger} suitable for use with this view.
	 */
	public Logger getLogger() {
		return logPanel.getLogger();
	}


	public LogPanel getLogPanel() {
		return logPanel;
	}

	/**
	 * @return a reference to the {@link TrackMateModelView} linked to this wizard.
	 */
	public TrackMateModelView getDisplayer() {
		return displayer;
	}

	/**
	 * Set the {@link TrackMateModelView} to be linked to this wizard.
	 */
	public void setDisplayer(TrackMateModelView displayer) {
		this.displayer = displayer;
	}

	/** 
	 * Simply forward the caught event to listeners of this main frame.
	 */
	@Override
	public void actionPerformed(final ActionEvent event) {
		fireAction(event);
	}

	/*
	 * 
	 * WIZARD MODEL METHODS
	 */

	/**
	 * @return the collection of {@link WizardPanelDescriptor} currently
	 * registered in this wizard.
	 */
	public Collection<WizardPanelDescriptor> getWizardPanelDescriptors() {
		return descriptorHashmap.values();
	}

	/**
	 * Registers the WizardPanelDescriptor in the model using the String-identifier specified.
	 * @param id String-based identifier
	 * @param descriptor WizardPanelDescriptor that describes the panel
	 */    
	public void registerWizardDescriptor(String id, WizardPanelDescriptor descriptor) {
		descriptorHashmap.put(id, descriptor);
	}

	/**
	 * Sets the current panel to that identified by the String passed in.
	 * It must be the {@link WizardPanelDescriptor} string ID, not the component string ID.
	 * @param descriptorID String-based panel identifier
	 */    
	public void showDescriptorPanelFor(final String descriptorID) {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				currentDescriptor = descriptorHashmap.get(descriptorID);

				// Register component instance with the layout on the fly
				String componentID = currentDescriptor.getComponentID();
				cardLayout.addLayoutComponent(currentDescriptor.getComponent(), componentID);
				jPanelMain.add(currentDescriptor.getComponent(), componentID);

				// Display it
				cardLayout.show(jPanelMain, componentID);
			}
		});
	}

	/**
	 * @return The currently displayed WizardPanelDescriptor
	 */    
	public WizardPanelDescriptor getCurrentPanelDescriptor() {
		return currentDescriptor;
	}

	public WizardPanelDescriptor getPanelDescriptorFor(Object id) {
		return descriptorHashmap.get(id);
	}

	/**
	 * Disable the 4 bottom buttons and memorize their state to that they
	 * can be restored when calling {@link #restoreButtonsState()}. 
	 */
	public void disableButtonsAndStoreState() {
		storedButtonState = new boolean[4];
		storedButtonState[0] = jButtonLoad.isEnabled();
		storedButtonState[1] = jButtonSave.isEnabled();
		storedButtonState[2] = jButtonPrevious.isEnabled();
		storedButtonState[3] = jButtonNext.isEnabled();
		setLoadButtonEnabled(false);
		setSaveButtonEnabled(false);
		setPreviousButtonEnabled(false);
		setNextButtonEnabled(false);
	}

	/**
	 * Restore the button state saved when calling {@link #disableButtonsAndStoreState()}.
	 * Do nothing if {@link #disableButtonsAndStoreState()} was not called before. 
	 */
	public void restoreButtonsState() {
		if (storedButtonState == null) {
			return;
		}
		setLoadButtonEnabled(storedButtonState[0]);
		setSaveButtonEnabled(storedButtonState[1]);
		setPreviousButtonEnabled(storedButtonState[2]);
		setNextButtonEnabled(storedButtonState[3]);
	}

	public void setNextButtonEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() { 
				jButtonNext.setEnabled(b);
				if (b) jButtonNext.requestFocusInWindow();
			}
		});
	}

	public void setPreviousButtonEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() { jButtonPrevious.setEnabled(b); }
		});
	}

	public void setSaveButtonEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() { jButtonSave.setEnabled(b); }
		});
	}

	public void setLoadButtonEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() { jButtonLoad.setEnabled(b); }
		});
	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * If the {@link Component} object given at construction is not <code>null</code>,
	 * try to position the GUI cleverly with respect to it.
	 */
	private void positionWindow() {


		if (null != component) {

			// Get total size of all screens
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] gs = ge.getScreenDevices();
			int screenWidth = 0;
			for (int i=0; i<gs.length; i++) {
				DisplayMode dm = gs[i].getDisplayMode();
				screenWidth += dm.getWidth();
			}

			Point windowLoc = component.getLocation();
			Dimension windowSize = component.getSize();
			Dimension guiSize = this.getSize();
			if (guiSize.width > windowLoc.x) {
				if (guiSize.width > screenWidth - (windowLoc.x + windowSize.width)) {
					setLocationRelativeTo(null); // give up
				} else {
					setLocation(windowLoc.x+windowSize.width, windowLoc.y); // put it to the right
				}
			} else {
				setLocation(windowLoc.x-guiSize.width, windowLoc.y); // put it to the left
			}

		} else {
			setLocationRelativeTo(null);
		}
	}

	/**
	 * Forward the given {@link ActionEvent} to the listeners of this GUI.
	 */
	private void fireAction(ActionEvent event) {
		synchronized (event) {
			for (ActionListener listener : listeners)
				listener.actionPerformed(event);
		}
	}

	/**
	 * Layout this GUI.
	 */
	private void initGUI() {
		try {
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			setIconImage(TRACKMATE_ICON.getImage());
			this.setTitle(fiji.plugin.trackmate.TrackMate_.PLUGIN_NAME_STR + " v"+fiji.plugin.trackmate.TrackMate_.PLUGIN_NAME_VERSION);
			this.setResizable(false);
			{
				jPanelMain = new JPanel();
				cardLayout = new CardLayout();
				getContentPane().add(jPanelMain, BorderLayout.CENTER);
				jPanelMain.setLayout(cardLayout);
				jPanelMain.setPreferredSize(new java.awt.Dimension(300, 461));
			}
			{
				jPanelButtons = new JPanel();
				getContentPane().add(jPanelButtons, BorderLayout.SOUTH);
				jPanelButtons.setLayout(null);
				jPanelButtons.setSize(300, 30);
				jPanelButtons.setPreferredSize(new java.awt.Dimension(300, 30));
				{
					jButtonNext = new JButton();
					jPanelButtons.add(jButtonNext);
					jButtonNext.setText("Next");
					jButtonNext.setIcon(NEXT_ICON);
					jButtonNext.setFont(FONT);
					jButtonNext.setBounds(220, 2, 73, 25);
					jButtonNext.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							fireAction(NEXT_BUTTON_PRESSED);
						}
					});
				}
				{
					jButtonPrevious = new JButton();
					jPanelButtons.add(jButtonPrevious);
					jButtonPrevious.setIcon(PREVIOUS_ICON);
					jButtonPrevious.setFont(FONT);
					jButtonPrevious.setBounds(190, 2, 30, 25);
					jButtonPrevious.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							fireAction(PREVIOUS_BUTTON_PRESSED);
						}
					});
				}
				{
					jButtonLog = new JButton();
					jPanelButtons.add(jButtonLog);
					jButtonLog.setIcon(LOG_ICON);
					jButtonLog.setFont(FONT);
					jButtonLog.setBounds(157, 2, 30, 25);
					jButtonLog.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							fireAction(LOG_BUTTON_PRESSED);
						}
					});
				}
				{
					jButtonLoad = new JButton();
					jPanelButtons.add(jButtonLoad);
					jButtonLoad.setText("Load");
					jButtonLoad.setIcon(LOAD_ICON);
					jButtonLoad.setFont(FONT);
					jButtonLoad.setBounds(2, 2, 76, 25);
					jButtonLoad.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							fireAction(LOAD_BUTTON_PRESSED);
						}
					});
				}
				{
					jButtonSave = new JButton();
					jPanelButtons.add(jButtonSave);
					jButtonSave.setText("Save");
					jButtonSave.setIcon(SAVE_ICON);
					jButtonSave.setFont(FONT);
					jButtonSave.setBounds(78, 2, 76, 25);
					jButtonSave.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							fireAction(SAVE_BUTTON_PRESSED);
						}
					});
				}
			}
			pack();
			this.setSize(300, 520);
			// Only instantiate the logger panel, the rest will be done by the controller
			{
				logPanel = new LogPanel();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		repaint();
		validate();
	}

}