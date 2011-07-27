package mpicbg.spim;

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
import mpicbg.spim.ChartTest.GraphFrame;

public class ChartPopupMenuOpenFile extends AbstractAction 
{
	final File completePath;
	
	public ChartPopupMenuOpenFile( final String title, final File completePath )
	{
		super( title );
		this.completePath = completePath;
	}
	
	@Override
	public void actionPerformed( final ActionEvent e ) 
	{
		JMenuItem a = (JMenuItem)e.getSource();		
		JPopupMenu b= (JPopupMenu)a.getParent();
		
		//ChartPanel panel = c.get

		Point p = b.getLocation();
		
		
		
		System.out.println( b.getLocation() );
		System.out.println( b.getBounds() );
		
		//final Opener o = new Opener();
		//o.openImage( completePath.getAbsolutePath() ).show();
		
		//final Image<FloatType> image = LOCI.openLOCIFloatType( completePath.getAbsolutePath(), new ArrayContainerFactory() );
		//ImageJFunctions.show( image );
	}

}
