package mpicbg.spim.registration.threshold;

import javax.vecmath.Point3d;

public class ComponentProperties 
{
	// the label this component has
	int label;
	
	// size in pixels
	public int size;	
	
	// dimension in pixels
	public int minX = Integer.MAX_VALUE;
	public int maxX = Integer.MIN_VALUE;
	public int minY = Integer.MAX_VALUE;
	public int maxY = Integer.MIN_VALUE;
	public int minZ = Integer.MAX_VALUE;
	public int maxZ = Integer.MIN_VALUE;		
	public int sizeX, sizeY, sizeZ;
	
	// center of mass
	public Point3d center;
}