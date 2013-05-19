package mpicbg.spim.registration.bead;

import java.util.ArrayList;

import mpicbg.spim.registration.detection.DetectionStructure;

public class BeadStructure extends DetectionStructure<Bead>
{
	public ArrayList<Bead> getBeadList() { return getDetectionList(); }
}
