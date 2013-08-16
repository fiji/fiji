package mpicbg.spim.registration.segmentation;

import java.util.ArrayList;

import mpicbg.spim.registration.detection.DetectionStructure;

public class NucleusStructure extends DetectionStructure<Nucleus>
{
	public ArrayList<Nucleus> getNucleiList() { return getDetectionList(); }
}
