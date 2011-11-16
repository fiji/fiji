package plugin;

import javax.swing.ImageIcon;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class Stitching_Grid implements PlugIn
{
	String[] choose = new String[]{ "erstens", "zweitens" };
	@Override
	public void run(String arg0) 
	{
		
		IJ.log( "" + getClass().getResource( "images/test.png" ) );
		IJ.log( "" + getClass().getResource( "images/test.png" ).getPath() );
		
		ImageIcon icon = createImageIcon( getClass().getResource( "images/test.png" ).getPath(), "gfgdf" );
		
		
		//GenericDialog gd = new GenericDialog( "test" );
		
		//gd.addChoice( "choose", choose, choose[ 0 ] );
	}
	
	/** Returns an ImageIcon, or null if the path was invalid. */
	protected ImageIcon createImageIcon(String path,
	                                           String description) {
	    java.net.URL imgURL = getClass().getResource(path);
	    if (imgURL != null) {
	        return new ImageIcon(imgURL, description);
	    } else {
	        System.err.println("Couldn't find file: " + path);
	        return null;
	    }
	}
}
