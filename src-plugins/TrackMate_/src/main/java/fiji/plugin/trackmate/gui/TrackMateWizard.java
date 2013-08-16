package fiji.plugin.trackmate.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.gui.descriptors.WizardPanelDescriptor;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

/**
 * A view for the TrackMate_ trackmate, strongly inspired from the spots detection GUI of the Imaris® software 
 * from <a href="http://www.bitplane.com/">Bitplane</a>.
 * 
 * @author Jean-Yves Tinevez <tinevez@pasteur.fr> - September 2010 - 2011
 */
public class TrackMateWizard extends JFrame implements ActionListener {

	JButton jButtonSave;
	JButton jButtonLoad;
	JButton jButtonPrevious;
	JButton jButtonNext;
	JButton jButtonLog;

	/*
	 * DEFAULT VISIBILITY & PUBLIC CONSTANTS
	 */

	public static final Font FONT = new Font("Arial", Font.PLAIN, 10);
	public static final Font BIG_FONT = new Font("Arial", Font.PLAIN, 14);
	public static final Font SMALL_FONT = FONT.deriveFont(8);
	public static final Dimension TEXTFIELD_DIMENSION = new Dimension(40,18);
	public static final Icon NEXT_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/arrow_right.png"));
	public static final String NEXT_TEXT = "Next";
	public static final String RESUME_TEXT = "Resume";
	public static final ImageIcon TRACKMATE_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/TrackIcon_small.png"));

	/*
	 * PRIVATE CONSTANTS
	 */

	private static final long serialVersionUID = -4092131926852771798L;
	private static final Icon PREVIOUS_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/arrow_left.png"));
	private static final Icon LOAD_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/page_go.png"));
	private static final Icon SAVE_ICON = new ImageIcon(TrackMateWizard.class.getResource("images/page_save.png"));
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

	private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();

	private JPanel jPanelButtons;
	private JPanel jPanelMain;
	private LogPanel logPanel;
	private TrackMateModelView displayer;
	private final TrackMateGUIController controller;

	/*
	 * CONSTRUCTOR
	 */

	public TrackMateWizard(TrackMateGUIController controller) {
		this.controller = controller;
		initGUI();
	}

	/*
	 * PUBLIC METHODS
	 */

	/** Expose the controller managing this GUI. */
	public TrackMateGUIController getController() {
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


	/**
	 * Sets the current panel to that identified by the String passed in.
	 * It must be the {@link WizardPanelDescriptor} string ID, not the component string ID.
	 * @param descriptorID String-based panel identifier
	 */    
	public void show(final WizardPanelDescriptor descriptor) {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				// Register component instance with the layout on the fly
				jPanelMain.removeAll();
				jPanelMain.add(descriptor.getComponent(), BorderLayout.CENTER);
				jPanelMain.validate();
				jPanelMain.repaint();
			}
		});
	}

	
	public void setNextButtonEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() { 
				jButtonNext.setEnabled(b);
				if (b) {
					jButtonNext.requestFocusInWindow();
				} else {
					jButtonPrevious.requestFocusInWindow();
				}
			}
		});
	}
	
	public void setLogButtonEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() { jButtonLog.setEnabled(b); }
		});
	}


	public void setPreviousButtonEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() { jButtonPrevious.setEnabled(b); }
		});
	}

	public void setSaveButtonEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() { jButtonSave.setEnabled(b); }
		});
	}

	public void setLoadButtonEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() { jButtonLoad.setEnabled(b); }
		});
	}

	/*
	 * PRIVATE METHODS
	 */

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
			setTitle(fiji.plugin.trackmate.TrackMate.PLUGIN_NAME_STR + " v"+fiji.plugin.trackmate.TrackMate.PLUGIN_NAME_VERSION);
			{
				jPanelMain = new JPanel();
				getContentPane().add(jPanelMain, BorderLayout.CENTER);
				jPanelMain.setLayout(new BorderLayout());
				jPanelMain.setPreferredSize(new java.awt.Dimension(300, 461));
			}
			jPanelButtons = new JPanel();
			jPanelButtons.setLayout(new BoxLayout(jPanelButtons, BoxLayout.X_AXIS));
			getContentPane().add(jPanelButtons, BorderLayout.SOUTH);
			jPanelButtons.setLayout(new BoxLayout(jPanelButtons, BoxLayout.LINE_AXIS));
			
			/*  We don't show the load button anymore. Load action can be accessed from the menu.
			 *  Out of simplicity, we leave the load button logic in place, but do not show it. */
			jButtonLoad = addButton("Load", LOAD_ICON, 2, 2, 76, 25, LOAD_BUTTON_PRESSED);
			jPanelButtons.remove(jButtonLoad);
			
			jButtonSave = addButton("Save", SAVE_ICON, 78, 2, 76, 25, SAVE_BUTTON_PRESSED);
			jPanelButtons.add(Box.createHorizontalGlue());
			jButtonLog = addButton(null, LOG_ICON, 157, 2, 30, 25, LOG_BUTTON_PRESSED);
			jPanelButtons.add(Box.createHorizontalGlue());
			jButtonPrevious = addButton(null, PREVIOUS_ICON, 190, 2, 30, 25, PREVIOUS_BUTTON_PRESSED);
			jButtonNext = addButton(NEXT_TEXT, NEXT_ICON, 220, 2, 73, 25, NEXT_BUTTON_PRESSED);
			pack();
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

	private JButton addButton(final String label, final Icon icon, int x, int y, int width, int height, final ActionEvent action) {
		JButton button = new JButton();
		jPanelButtons.add(button);
		button.setText(label);
		button.setIcon(icon);
		button.setFont(FONT);
		button.setBounds(x, y, width, height);
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fireAction(action);
			}
		});
		return button;
	}

	public JButton getNextButton() {
		return jButtonNext;
	}

}