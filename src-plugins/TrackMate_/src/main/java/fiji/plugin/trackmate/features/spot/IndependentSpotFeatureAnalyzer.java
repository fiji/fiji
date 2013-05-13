package fiji.plugin.trackmate.features.spot;

import java.util.Collection;

import net.imglib2.img.ImgPlus;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;

public abstract class IndependentSpotFeatureAnalyzer<T extends RealType<T>> implements SpotAnalyzer<T> {

	protected final ImgPlus<T> img;
	protected final Collection<Spot> spots;
	protected String errorMessage;
	private long processingTime;

	public IndependentSpotFeatureAnalyzer(final ImgPlus<T> img, final Collection<Spot> spots) {
		this.img = img;
		this.spots = spots;
	}
	
	
	public abstract void process(final Spot spot);

	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public boolean process() {
		final long start = System.currentTimeMillis();
		for(Spot spot: spots) {
			process(spot);
		}
		processingTime = System.currentTimeMillis() - start;
		return true;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public long getProcessingTime() {
		return processingTime;
	}

}