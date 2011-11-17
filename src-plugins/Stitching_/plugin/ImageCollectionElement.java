package plugin;

import ij.IJ;
import ij.ImagePlus;

import java.io.File;

import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;

public class ImageCollectionElement 
{
	final File file;
	ImagePlus imp = null;
	
	
	public ImageCollectionElement( final File file )
	{
		this.file = file;
	}
	
	public ImagePlus open()
	{
		if ( imp != null )
		{
			return imp;
		}
		else
		{
			try 
			{
				ImporterOptions options = new ImporterOptions();
				options.setId( file.getAbsolutePath() );
				options.setSplitChannels( false );
				options.setSplitTimepoints( false );
				options.setSplitFocalPlanes( false );
				options.setAutoscale( false );
				
				final ImagePlus[] imp = BF.openImagePlus( file.getAbsolutePath() );
				
				if ( imp.length > 1 )
				{
					IJ.log( "LOCI does not open the file '" + file + "'correctly, it opens the image and splits it - maybe you should convert all input files first to TIFF?" );
					return null;
				}
				else
				{
					return imp[ 0 ];
				}
				
			} catch ( Exception e ) 
			{
				IJ.log( "Cannot open file '" + file + "': " + e );
				e.printStackTrace();
				return null;
			} 
			
		}
	}
}
