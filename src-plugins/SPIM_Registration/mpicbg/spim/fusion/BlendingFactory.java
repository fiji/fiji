package mpicbg.spim.fusion;

import java.util.ArrayList;

import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewDataBeads;

public class BlendingFactory implements CombinedPixelWeightenerFactory<Blending>
{
	public Blending createInstance( ArrayList<ViewDataBeads> views ) { return new Blending( views ); }

	public void printProperties()
	{
		IOFunctions.println("BlendingFactory(): no special properties.");		
	}
	
	public String getDescriptiveName() { return "Blending"; }
	
	public String getErrorMessage() { return ""; }

	@Override
	public void setParameters(String configuration) { }
}
