package mpicbg.spim.fusion;

import java.util.ArrayList;

import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;

public class BlendingSimpleFactory implements CombinedPixelWeightenerFactory<BlendingSimple>
{
	@Override
	public BlendingSimple createInstance( ArrayList<ViewDataBeads> views ) { return new BlendingSimple( views ); }
	
	@Override
	public void printProperties()
	{
		IOFunctions.println("BlendingSimpleFactory(): no special properties.");		
	}
	@Override
	public String getDescriptiveName() { return "Blending"; }
	
	@Override
	public String getErrorMessage() { return ""; }

	@Override
	public void setParameters(String configuration) { }
}
