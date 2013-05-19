package mpicbg.icp;


import java.util.List;

import fiji.util.node.Leaf;

import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.pointdescriptor.exception.NoSuitablePointsException;

public interface PointMatchIdentification < P extends Point & Leaf<P> >
{
	public List<PointMatch> assignPointMatches( final List<P> target, final List<P> reference ) throws NoSuitablePointsException;
}
