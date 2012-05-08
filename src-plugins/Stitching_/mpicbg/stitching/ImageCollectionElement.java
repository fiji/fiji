package mpicbg.stitching;

import ij.IJ;
import ij.ImagePlus;

import java.io.File;

import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import mpicbg.models.Model;

public class ImageCollectionElement 
{
	final File file;
	ImagePlus imp = null;
	final int index;
	Model<?> model;
	int dimensionality;
	boolean virtual = false;
	
	//2d or 3d offset
	float[] offset;	
	
	//2d or 3d size if image
	int[] size;
	
	public ImageCollectionElement( final File file, final int index )
	{
		this.file = file;
		this.index = index;		
	}
	
	public void setOffset( final float[] offset ) { this.offset = offset; }
	public float[] getOffset() { return offset; }
	public float getOffset( final int dim ) { return offset[ dim ]; }
	
	public int[] getDimensions() { return size; }
	public int getDimension( final int dim ) { return size[ dim ]; }
	
	public int getIndex() { return index; }
	
	public void setModel( final Model<?> model ) { this.model = model; }
	public Model<?> getModel() { return model; }
	
	public void setDimensionality( final int dimensionality ) { this.dimensionality = dimensionality; }
	public int getDimensionality() { return dimensionality; }
	
	public File getFile() { return file; }
	public boolean isVirtual() { return virtual; }
	
	/**
	 * Used by the multi-series stitching
	 * 
	 * @param imp - the ImagePlus of this series
	 */
	public void setImagePlus( final ImagePlus imp ) 
	{ 
		this.imp = imp; 
		
		if ( imp.getNSlices() == 1 )
			size = new int[] { imp.getWidth(), imp.getHeight() };
		else
			size = new int[] { imp.getWidth(), imp.getHeight(), imp.getNSlices() };	
	}
	
	public ImagePlus open( final boolean virtual )
	{
		if ( imp != null && this.isVirtual() == virtual )
		{
			return imp;
		}
		else
		{
			// TODO: Unify this image loading mechanism with the one in
			// plugin/Stitching_Grid.java. Otherwise changes to how images
			// are loaded must be made in multiple places in the code.
			if ( imp != null )
				imp.close();
			
			this.virtual = virtual;
			
			try 
			{
				if ( !file.exists() )
				{
					IJ.log( "Cannot find file: '" + file + "' - abort stitching." );
					return null;
				}
				
				ImporterOptions options = new ImporterOptions();
				options.setId( file.getAbsolutePath() );
				options.setSplitChannels( false );
				options.setSplitTimepoints( false );
				options.setSplitFocalPlanes( false );
				options.setAutoscale( false );
				options.setVirtual( virtual );
				
				final ImagePlus[] imp;
				
				if ( virtual )
					imp = BF.openImagePlus( options );
				else
					imp = BF.openImagePlus( file.getAbsolutePath() ); // this worked, so we keep it (altough both should be the same)
				
				if ( imp.length > 1 )
				{
					IJ.log( "LOCI does not open the file '" + file + "'correctly, it opens the image and splits it - maybe you should convert all input files first to TIFF?" );
					
					for ( ImagePlus i : imp )
						i.close();
					
					return null;
				}
				else
				{
					if ( imp[ 0 ].getNSlices() == 1 )
						size = new int[] { imp[ 0 ].getWidth(), imp[ 0 ].getHeight() };
					else
						size = new int[] { imp[ 0 ].getWidth(), imp[ 0 ].getHeight(), imp[ 0 ].getNSlices() };
					
					this.imp = imp[ 0 ];
					return this.imp;
				}
				
			} 
			catch ( Exception e ) 
			{
				IJ.log( "Cannot open file '" + file + "': " + e );
				//e.printStackTrace();
				return null;
			} 
			
		}
	}

	public void close() 
	{
		imp.close();
		imp = null;
	}
}
