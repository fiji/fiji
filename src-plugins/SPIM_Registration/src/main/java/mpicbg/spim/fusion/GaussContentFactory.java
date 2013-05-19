package mpicbg.spim.fusion;

import mpicbg.imglib.container.ContainerFactory;
import mpicbg.spim.registration.ViewDataBeads;

public class GaussContentFactory implements IsolatedPixelWeightenerFactory<GaussContent>
{
	ContainerFactory gaussContentContainer;
	
	public GaussContentFactory( final ContainerFactory gaussContentContainer ) { this.gaussContentContainer = gaussContentContainer; }
	
	@Override
	public GaussContent createInstance( final ViewDataBeads view ) 
	{ 
		return new GaussContent( view, gaussContentContainer ); 
	}
	
	public String getDescriptiveName() { return "Gauss approximated Entropy"; }

	public void printProperties()
	{
		System.out.print("GaussContentFactory(): Owns Factory for Image<FloatType>");
		gaussContentContainer.printProperties();
	}
	
	public String getErrorMessage() { return ""; }

	@Override
	public void setParameters(String configuration) { }
	
}
