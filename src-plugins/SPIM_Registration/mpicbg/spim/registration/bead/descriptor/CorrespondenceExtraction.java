package mpicbg.spim.registration.bead.descriptor;

import java.util.ArrayList;

import mpicbg.spim.registration.ViewDataBeads;
import mpicbg.spim.registration.bead.BeadCorrespondence;

public interface CorrespondenceExtraction
{
	public ArrayList<BeadCorrespondence> extractCorrespondenceCandidates( final ViewDataBeads viewA, final ViewDataBeads viewB, final double differenceThreshold, final double ratioOfDistance, final boolean useAssociatedBeads );
}
