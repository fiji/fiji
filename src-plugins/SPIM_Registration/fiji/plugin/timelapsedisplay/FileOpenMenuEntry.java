package fiji.plugin.timelapsedisplay;

import ij.io.Opener;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jfree.chart.ChartPanel;

import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.io.LOCI;
import mpicbg.imglib.type.numeric.real.FloatType;

public class FileOpenMenuEntry extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	final File completePath;
	
	int xLocation = -1;
	File worstView = null;
	
	public FileOpenMenuEntry( final String title, final File completePath )
	{
		super( title );
		this.completePath = completePath;
	}

	/**
	 * This method is called by the MouseListener when a right mouse click was detected
	 * 
	 * @param xLocation
	 * @param file - the {@link File} defining the worst view
	 */
	public void setXLocationRightClick( final int xLocation, final File worstView ) 
	{ 
		this.xLocation = xLocation;
		this.worstView = worstView;
		
		if ( worstView != null )
			this.putValue( "name", worstView.getName() );

		System.out.println( xLocation + " = " + worstView );
	}
	
	@Override
	public void actionPerformed( final ActionEvent e ) 
	{
		//final Image<FloatType> image = LOCI.openLOCIFloatType( completePath.getAbsolutePath(), new ArrayContainerFactory() );
		//ImageJFunctions.show( image );
		
		System.out.println( "open " + worstView );
	}

}
