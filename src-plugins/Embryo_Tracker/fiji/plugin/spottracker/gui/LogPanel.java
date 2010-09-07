package fiji.plugin.spottracker.gui;

import static fiji.plugin.spottracker.gui.SpotTrackerFrame.FONT;
import static fiji.plugin.spottracker.gui.SpotTrackerFrame.SMALL_FONT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import fiji.plugin.spottracker.Logger;

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
public class LogPanel extends ActionListenablePanel {


	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static final long serialVersionUID = 1L;

	/**
	 * This action is fired when the "Next >>" button is pressed.
	 */
	public final ActionEvent NEXT_BUTTON_PRESSED = new ActionEvent(this, 0, "NextButtonPressed");
	
	JButton jButtonNext;

	private JScrollPane jScrollPaneLog;
	private JPanel jPanelButtons;
	private JTextPane jTextPaneLog;
	private JProgressBar jProgressBar;
	private JPanel jPanelProgressBar;
	private final Logger logger;

	public LogPanel() {
		super();
		initGUI();
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
						StyleContext sc = StyleContext.getDefaultStyleContext();
						AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);
						int len = jTextPaneLog.getDocument().getLength();
						jTextPaneLog.setEditable(true); // Ugly ugly but otherwise does not display anything
						jTextPaneLog.setCaretPosition(len);
						jTextPaneLog.setCharacterAttributes(aset, false);
						jTextPaneLog.replaceSelection(message);
						jTextPaneLog.setEditable(false); // Ugly ugly

					}
				});

			}

			@Override
			public void setProgress(float val) {
				if (val < 0) val =0;
				if (val > 1) val = 1;
				int intVal = (int) (val*100);
				jProgressBar.setValue(intVal);
			}
			
		};		
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public Logger getLogger() {
		return logger;
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void initGUI() {
		try {
			BorderLayout thisLayout = new BorderLayout();
			this.setLayout(thisLayout);
			this.setPreferredSize(new java.awt.Dimension(270, 500));
			{
				jPanelProgressBar = new JPanel();
				BorderLayout jPanelProgressBarLayout = new BorderLayout();
				jPanelProgressBar.setLayout(jPanelProgressBarLayout);
				this.add(jPanelProgressBar, BorderLayout.NORTH);
				jPanelProgressBar.setPreferredSize(new java.awt.Dimension(270, 32));
				{
					jProgressBar = new JProgressBar();
					jPanelProgressBar.add(jProgressBar, BorderLayout.CENTER);
					jProgressBar.setPreferredSize(new java.awt.Dimension(270, 20));
				}
			}
			{
				jScrollPaneLog = new JScrollPane();
				this.add(jScrollPaneLog);
				jScrollPaneLog.setPreferredSize(new java.awt.Dimension(262, 136));
				{
					jTextPaneLog = new JTextPane();
					jTextPaneLog.setEditable(false);
//					jTextPaneLog.setFocusable(false);
					jTextPaneLog.setFont(SMALL_FONT);
					jScrollPaneLog.setViewportView(jTextPaneLog);
					jTextPaneLog.setBackground(this.getBackground());
				}
			}
			{
				jPanelButtons = new JPanel();
				BoxLayout jPanelButtonsLayout = new BoxLayout(jPanelButtons, javax.swing.BoxLayout.X_AXIS);
				jPanelButtons.setLayout(jPanelButtonsLayout);
				this.add(jPanelButtons, BorderLayout.SOUTH);
				jPanelButtons.setPreferredSize(new java.awt.Dimension(270, 30));
				{
					jButtonNext = new JButton();
					jPanelButtons.add(Box.createHorizontalGlue());
					jPanelButtons.add(jButtonNext);
					jPanelButtons.add(Box.createHorizontalStrut(10));
					jButtonNext.setText("Next >>");
					jButtonNext.setFont(FONT);
					jButtonNext.addActionListener(new ActionListener() {						
						@Override
						public void actionPerformed(ActionEvent e) {
							fireAction(NEXT_BUTTON_PRESSED);
						}
					});
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
	 */
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		LogPanel lp = new LogPanel();
		frame.getContentPane().add(lp);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		
		lp.getLogger().log("Hello!\n", Logger.BLUE_COLOR);
		lp.getLogger().log("World!\n");
		lp.getLogger().error("Oh no!!!! More lemmings!!!!\n");
		lp.getLogger().setProgress(0.4f);
	}

}
