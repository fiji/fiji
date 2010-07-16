package mpicbg.spim.vis3d;

import java.util.ArrayList;

import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.image.display.imagej.InverseTransformDescription;
import mpicbg.imglib.interpolation.InterpolatorFactory;
import mpicbg.imglib.interpolation.nearestneighbor.NearestNeighborInterpolatorFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.ViewStructure;

public class PreviewRegistration 
{
	public PreviewRegistration( final ViewStructure viewStructure )
	{
		final OutOfBoundsStrategyValueFactory<FloatType> outsideFactory = new OutOfBoundsStrategyValueFactory<FloatType>( new FloatType(0) );
		final InterpolatorFactory<FloatType> interpolatorFactory = new NearestNeighborInterpolatorFactory<FloatType>( outsideFactory );

		final ArrayList<InverseTransformDescription<FloatType>> list = new ArrayList<InverseTransformDescription<FloatType>>();
		
		for ( final ViewDataBeads view : viewStructure.getViews() )
		{
			if ( view.isConnected() )
			{
				InverseTransformDescription<FloatType> i = new InverseTransformDescription<FloatType>( view.getTile().getModel(), interpolatorFactory, view.getImage() );
				list.add( i );
			}
		}

		if ( list.size() > 0 )
			ImageJFunctions.displayAsVirtualStack( list, ImageJFunctions.GRAY32, new int[]{0,1,2}, new int[3] ).show();
		else
			if ( viewStructure.getDebugLevel() <= ViewStructure.DEBUG_ERRORONLY )
				IOFunctions.println("PreviewRegistration(): no view is connected to any other view, cannot display.");
	}
}
