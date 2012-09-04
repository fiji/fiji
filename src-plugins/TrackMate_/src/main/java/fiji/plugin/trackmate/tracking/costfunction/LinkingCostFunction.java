package fiji.plugin.trackmate.tracking.costfunction;

import java.util.List;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import Jama.Matrix;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.tracking.LAPUtils;

/**
 * <p>Linking cost function used with {@link LAPTracker}.
 * 
 * <p>The <b>cost function</b> is determined by the default equation in the
 * TrackMate plugin, see below.
 * <p>  
 *  It slightly differs from the Jaqaman article, see equation (3) in the paper.
 *  
 *  @see LAPUtils#computeLinkingCostFor(Spot, Spot, double, double, java.util.Map)
 *  
 * @author Nicholas Perry
 * @author Jean-Yves Tinevez
 *
 */
public class LinkingCostFunction <T extends RealType<T> & NativeType<T>> implements CostFunctions {
	
	protected TrackerKeys<T> settings;
	
	public LinkingCostFunction(TrackerKeys<T> settings) {
		this.settings = settings;
	}
	
	@Override
	public Matrix getCostFunction(final List<Spot> t0, final List<Spot> t1) {
		Spot s0 = null;			// Spot in t0
		Spot s1 = null;			// Spot in t1
		final Matrix m = new Matrix(t0.size(), t1.size());
		
		for (int i = 0; i < t0.size(); i++) {
			
			s0 = t0.get(i);
			
			for (int j = 0; j < t1.size(); j++) {
				
				s1 = t1.get(j);
				double cost = LAPUtils.computeLinkingCostFor(s0, s1, 
						settings.linkingDistanceCutOff, settings.blockingValue, settings.linkingFeaturePenalties);
				m.set(i, j, cost);
			}
		}
		
		return m;
	}

}
