package mpicbg.spim.fusion;

import mpicbg.imglib.container.ContainerFactory;
import mpicbg.spim.registration.ViewDataBeads;

public class AverageContentFactory implements IsolatedPixelWeightenerFactory<AverageContent>
{
	ContainerFactory avgContentContainer;
	
	public AverageContentFactory( final ContainerFactory avgContentContainer ) { this.avgContentContainer = avgContentContainer; }
	
	@Override
	public AverageContent createInstance( final ViewDataBeads view ) 
	{ 
		return new AverageContent( view, avgContentContainer ); 
	}
	
	public String getDescriptiveName() { return "Average approximated Entropy using Integral images"; }

	public void printProperties()
	{
		System.out.print("AverageContentFactory(): Owns Factory for Image<FloatType>");
		avgContentContainer.printProperties();
	}
	
	public String getErrorMessage() { return ""; }

	@Override
	public void setParameters(String configuration) { }
	
}
