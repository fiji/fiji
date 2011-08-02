package fiji.plugin.timelapsedisplay;

import ij.gui.GUI;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

public class GraphFrame extends JFrame implements ActionListener
{
	private static final long serialVersionUID = 1L;

	JFreeChart chart = null;

	ChartPanel chartPanel = null;
	MouseListener mouseListener;
	JPanel mainPanel;
	
	private int referenceTimePoint;
	final boolean enableReferenceTimePoint;

	public GraphFrame( final JFreeChart chart, final int referenceTimePoint, final boolean enableReferenceTimePoint, final List<FileOpenMenuEntry> extraMenuItems, final ArrayList< RegistrationStatistics > data )
	{
		super();
		
		this.referenceTimePoint = referenceTimePoint;
		this.enableReferenceTimePoint = enableReferenceTimePoint;

		mainPanel = new JPanel();
		mainPanel.setLayout( new BorderLayout() );

		updateWithNewChart( chart, true, extraMenuItems, data );

		JPanel buttonsPanel = new JPanel();
		mainPanel.add( buttonsPanel, BorderLayout.SOUTH );

		setContentPane( mainPanel );
		validate();
		setSize( new java.awt.Dimension( 500, 270 ) );
		GUI.center( this );
	}
	
	public int getReferenceTimePoint() { return mouseListener.getReferenceTimePoint(); }

	synchronized public void updateWithNewChart( JFreeChart c, boolean setSize, final List<FileOpenMenuEntry> extraMenuItems, final ArrayList< RegistrationStatistics > data )
	{
		if ( chartPanel != null )
			remove( chartPanel );
		chartPanel = null;
		this.chart = c;
		chartPanel = new ChartPanel( c );
		chartPanel.setMouseWheelEnabled( true );
		if ( setSize )
			chartPanel.setPreferredSize( new java.awt.Dimension( 800, 600 ) );
		
		mouseListener = new MouseListener( chartPanel, referenceTimePoint, enableReferenceTimePoint, data );
		chartPanel.addChartMouseListener( mouseListener );
		chartPanel.setHorizontalAxisTrace( true );
		mainPanel.add( chartPanel, BorderLayout.CENTER );
		
		
		// add extra items
		final JPopupMenu menu = chartPanel.getPopupMenu();
		
		if ( extraMenuItems != null )
			for ( final FileOpenMenuEntry m : extraMenuItems )
				menu.add( new JMenuItem( m ) );
		
		// link it to the chartPanel and the coordinates of the 
		// popup by using a mouse listener for the right click
		mouseListener.setFileOpenMenuEntryList( extraMenuItems );
		
		//menu.get
		validate();
	}
	
	@Override
	public void actionPerformed( ActionEvent e )
	{
		Object source = e.getSource();
	}
}
