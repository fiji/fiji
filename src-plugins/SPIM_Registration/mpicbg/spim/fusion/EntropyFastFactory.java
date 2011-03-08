package mpicbg.spim.fusion;

import mpicbg.imglib.container.ContainerFactory;
import mpicbg.spim.registration.ViewDataBeads;

public class EntropyFastFactory implements IsolatedPixelWeightenerFactory<EntropyFast>
{
	ContainerFactory entropyContainer;
	
	public EntropyFastFactory( final ContainerFactory entropyContainer ) { this.entropyContainer = entropyContainer; }
	
	@Override
	public EntropyFast createInstance( ViewDataBeads view ) 
	{ 
		return new EntropyFast( view, entropyContainer ); 
	}
	
	public String getDescriptiveName() { return "Entropy"; }

	public void printProperties()
	{
		System.out.print("EntropyFastFactory(): Owns Factory for Image<FloatType>");
		entropyContainer.printProperties();
	}
	
	public String getErrorMessage() { return ""; }

	@Override
	public void setParameters(String configuration) { }
	
}
