package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.BorderLayout;
import java.awt.Color;

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

import fiji.plugin.trackmate.Logger;

/**
 * A panel using s {@link JTextPane} to log events.
 * @author Jean-Yves Tinevez <tinevez@pasteur.fr> - September 2010 - January 2011.
 */
public class LogPanel extends ActionListenablePanel {

	private static final long serialVersionUID = 1L;
	public static final String DESCRIPTOR = "LogPanel";
	private JScrollPane jScrollPaneLog;
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
						jTextPaneLog.setCaretPosition(len);
						jTextPaneLog.setCharacterAttributes(aset, false);
						jTextPaneLog.replaceSelection(message);
					}
				});
			}

			@Override
			public void setStatus(final String status) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						jProgressBar.setString(status);
					}
				});
			}
			
			@Override
			public void setProgress(double val) {
				if (val < 0) val =0;
				if (val > 1) val = 1;
				final int intVal = (int) (val*100);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						jProgressBar.setValue(intVal);
					}
				});
			}
		};		
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * @return a {@link Logger} object that will log all events to this log panel.
	 */
	public Logger getLogger() {
		return logger;
	}
	
	/**
	 * @return the text content currently displayed in the log panel.
	 */
	public String getTextContent() {
		return jTextPaneLog.getText();
	}
	
	/**
	 * Set the text content currently displayed in the log panel.
	 * @param log  the text to display.
	 */
	public void setTextContent(String log) {
		jTextPaneLog.setText(log);
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
					jProgressBar.setStringPainted(true);
					jProgressBar.setFont(SMALL_FONT);
				}
			}
			{
				jScrollPaneLog = new JScrollPane();
				this.add(jScrollPaneLog);
				jScrollPaneLog.setPreferredSize(new java.awt.Dimension(262, 136));
				{
					jTextPaneLog = new JTextPane();
					jTextPaneLog.setEditable(true);
					jTextPaneLog.setFont(SMALL_FONT);
					jScrollPaneLog.setViewportView(jTextPaneLog);
					jTextPaneLog.setBackground(this.getBackground());
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
