package fiji.plugin.timelapsedisplay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

public class MouseListenerTimelapse implements ChartMouseListener
{
	final ChartPanel panel;
	ValueMarker valueMarker;
	boolean markerShown = false;
	int referenceTimePoint;
	final boolean enableReferenceTimePoint;
	
	// update the location of the last right click and the filename to open
	final ArrayList<FileOpenMenuEntry> updateList = new ArrayList<FileOpenMenuEntry>();
	
	MouseListenerTimelapse( final ChartPanel panel )
	{
		this( panel, -1, false );
	}

	MouseListenerTimelapse( final ChartPanel panel, final boolean enableReferenceTimePoint )
	{
		this( panel, -1, enableReferenceTimePoint );
	}

	MouseListenerTimelapse( final ChartPanel panel, final int referenceTimePoint, final boolean enableReferenceTimePoint )
	{
		this.panel = panel;
		this.referenceTimePoint = referenceTimePoint;
		this.enableReferenceTimePoint = enableReferenceTimePoint;
		
		if ( enableReferenceTimePoint || referenceTimePoint != -1 ) // at least show it if it is not -1
		{
			valueMarker = makeMarker( referenceTimePoint );
			
			if ( referenceTimePoint >= 0 )
			{
				((XYPlot)panel.getChart().getPlot()).addDomainMarker( valueMarker );
				markerShown = true;
			}
		}
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
			referenceTimePoint = getChartXLocation( e.getTrigger().getPoint(), panel );
			
			valueMarker.setValue( referenceTimePoint );
			valueMarker.setLabel( " Reference\n Timepoint " + referenceTimePoint );
			
			if ( !markerShown )
			{
				((XYPlot) e.getChart().getPlot()).addDomainMarker( valueMarker );
				markerShown = true;
			}
		}
	}
	
	public static int getChartXLocation( final Point point, final ChartPanel panel )
	{
		final Point2D p = panel.translateScreenToJava2D( point );
		final Rectangle2D plotArea = panel.getScreenDataArea();
		final XYPlot plot = (XYPlot) panel.getChart().getPlot();
		final double chartX = plot.getDomainAxis().java2DToValue( p.getX(), plotArea, plot.getDomainAxisEdge() );
		//final double chartY = plot.getRangeAxis().java2DToValue( p.getY(), plotArea, plot.getRangeAxisEdge() );
		
		return (int)Math.round( chartX );			
	}

	@Override
	public void chartMouseMoved( ChartMouseEvent e )
	{
	}
}
