package mpicbg.spim.fusion;

import mpicbg.spim.io.IOFunctions;
import mpicbg.spim.registration.ViewStructure;

public class BlendingFactory implements CombinedPixelWeightenerFactory<Blending>
{
	public Blending createInstance( ViewStructure viewStructure ) { return new Blending( viewStructure ); }

	public void printProperties()
	{
		IOFunctions.println("BlendingFactory(): no special properties.");		
	}
	
	public String getDescriptiveName() { return "Blending"; }
	
	public String getErrorMessage() { return ""; }

	@Override
	public void setParameters(String configuration) { }
}
