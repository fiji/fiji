package fiji.plugin.timelapsedisplay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

public class MouseListener implements ChartMouseListener
{
	final ChartPanel panel;
	ValueMarker valueMarker;
	boolean markerShown = false;
	int referenceTimePoint;
	final boolean enableReferenceTimePoint;
	final ArrayList< RegistrationStatistics > data;
	
	// update the location of the last right click and the filename to open
	final ArrayList<FileOpenMenuEntry> updateList = new ArrayList<FileOpenMenuEntry>();
	
	MouseListener( final ChartPanel panel )
	{
		this( panel, -1, false, null );
	}

	MouseListener( final ChartPanel panel, final boolean enableReferenceTimePoint )
	{
		this( panel, -1, enableReferenceTimePoint, null );
	}

	MouseListener( final ChartPanel panel, final int referenceTimePoint, final boolean enableReferenceTimePoint, final ArrayList< RegistrationStatistics > data )
	{
		this.panel = panel;
		this.referenceTimePoint = referenceTimePoint;
		this.enableReferenceTimePoint = enableReferenceTimePoint;
		this.data = data;
		
		if ( enableReferenceTimePoint )
		{
			valueMarker = makeMarker( referenceTimePoint );
			
			if ( referenceTimePoint >= 0 )
			{
				((XYPlot)panel.getChart().getPlot()).addDomainMarker( valueMarker );
				markerShown = true;
			}
		}
	}
	
	public void setFileOpenMenuEntryList( final List<FileOpenMenuEntry> updateList ) 
	{ 
		this.updateList.clear();
		this.updateList.addAll( updateList ); 
	} 
	public int getReferenceTimePoint() { return referenceTimePoint; }
	
	protected ValueMarker makeMarker( final int timePoint )
	{
		final ValueMarker valueMarker = new ValueMarker( timePoint );
		valueMarker.setStroke( new BasicStroke ( 1.5f ) );
		valueMarker.setPaint( new Color( 0.0f, 93f/255f, 9f/255f ) );
		valueMarker.setLabel( " Reference\n Timepoint " + timePoint );
		valueMarker.setLabelAnchor(RectangleAnchor.BOTTOM );
		valueMarker.setLabelTextAnchor( TextAnchor.BOTTOM_LEFT );
		
		return valueMarker;
	}

	@Override
	public void chartMouseClicked( final ChartMouseEvent e )
	{
		// left mouse click
		if ( e.getTrigger().getButton() == MouseEvent.BUTTON1 && enableReferenceTimePoint )
		{
			referenceTimePoint = getChartXLocation( e );
			
			valueMarker.setValue( referenceTimePoint );
			valueMarker.setLabel( " Reference\n Timepoint " + referenceTimePoint );
			
			if ( !markerShown )
			{
				((XYPlot) e.getChart().getPlot()).addDomainMarker( valueMarker );
				markerShown = true;
			}
		}
		else if ( e.getTrigger().getButton() == MouseEvent.BUTTON3 )
		{
			System.out.println( "3" );
			if ( updateList.size() > 0 && data != null && data.size() > 0 )
			{
				// right mouse click
				final int tp = getChartXLocation( e );
				File file = null;
				
				
				for ( final RegistrationStatistics stat : data )
					if ( stat.timePoint == tp )
					{
						file = stat.worstView;
						break;
					}
				
				// update item
				for ( final FileOpenMenuEntry m : updateList )
				{
					m.setXLocationRightClick( tp, file );
				}
			}
		}
	}
	
	protected int getChartXLocation( final ChartMouseEvent e )
	{
		final Point2D p = panel.translateScreenToJava2D( e.getTrigger().getPoint() );
		final Rectangle2D plotArea = panel.getScreenDataArea();
		final XYPlot plot = (XYPlot) e.getChart().getPlot();
		final double chartX = plot.getDomainAxis().java2DToValue( p.getX(), plotArea, plot.getDomainAxisEdge() );
		//final double chartY = plot.getRangeAxis().java2DToValue( p.getY(), plotArea, plot.getRangeAxisEdge() );
		
		return (int)Math.round( chartX );			
	}

	@Override
	public void chartMouseMoved( ChartMouseEvent e )
	{
	}
}
