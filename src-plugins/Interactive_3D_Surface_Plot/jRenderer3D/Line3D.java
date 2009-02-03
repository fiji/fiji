package jRenderer3D;

import java.awt.Color;

/**
 * This class represents a line in 3D coordinate system. 
 * It is defined by its start point (x1, y1, z1) and its end point (x2, y2, z2), and its color.
 * 
 * <p>At the moment it is not possible to set line width.</p>
 * 
 * @author Kai Uwe Barthel
 *
 */
public class Line3D {

	/**
	 * Creates a new lines3D object. No parameters are set.
	 *
	 */
	public Line3D() {};
	
	
	/**
	 * Creates a new lines3D object with start point, end point and color of the line
	 * 
	 * @param x1 - x coordinate of start point
	 * @param y1 - y coordinate of start point
	 * @param z1 - z coordinate of start point
	 * @param x2 - x coordinate of end point
	 * @param y2 - y coordinate of end point
	 * @param z2 - z coordinate of end point
	 * @param rgb - color of line (hex)
	 */
	public Line3D(int x1, int y1, int z1, int x2, int y2, int z2, int rgb) {

		this.x1 = x1;
		this.y1 = y1;
		this.z1 = z1;
		
		// end coordinates
		this.x2 = x2;
		this.y2 = y2;
		this.z2 = z2;
			
		this.color = rgb;
	}
	
	/**
	 * Creates a new lines3D object with start point, end point and color of the line
	 * 
	 * @param x1 - x coordinate of start point
	 * @param y1 - y coordinate of start point
	 * @param z1 - z coordinate of start point
	 * @param x2 - x coordinate of end point
	 * @param y2 - y coordinate of end point
	 * @param z2 - z coordinate of end point
	 * @param color - color of line 
	 */
	public Line3D(int x1, int y1, int z1, int x2, int y2, int z2, Color color) {

		this.x1 = x1;
		this.y1 = y1;
		this.z1 = z1;
		
		// end coordinates
		this.x2 = x2;
		this.y2 = y2;
		this.z2 = z2;
			
		this.color = color.getRGB();
	}
	
	public Line3D(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {

		this.x1 = x1;
		this.y1 = y1;
		this.z1 = z1;
		
		// end coordinates
		this.x2 = x2;
		this.y2 = y2;
		this.z2 = z2;
			
		this.color = color.getRGB();
	}

	public Line3D(double x1, double y1, double z1, double x2, double y2, double z2, Color color, boolean isPair) {

		this.x1 = x1;
		this.y1 = y1;
		this.z1 = z1;
		
		// end coordinates
		this.x2 = x2;
		this.y2 = y2;
		this.z2 = z2;
			
		this.color = color.getRGB();
		
		this.isPair = isPair;
	}

	
	/**
	 *  Creates a new lines3D object with start point, end point and color of the line
	 *  
	 * @param p1   		start point int[3] 
	 * @param p2		start point int[3]
	 * @param rgb		color of the line (int)
	 */
	public Line3D(int[] p1, int[] p2, int rgb) {

		this.x1 = p1[0];
		this.y1 = p1[1];
		this.z1 = p1[2];
		
		// end coordinates
		this.x2 = p2[0];
		this.y2 = p2[1];
		this.z2 = p2[2];
			
		this.color = rgb;
	}
	
	/**
	 *  Creates a new lines3D object with start point, end point and color of the line
	 *  
	 * @param p1   		start point int[3] 
	 * @param p2		start point int[3]
	 * @param color		color of the line
	 */
	public Line3D(int[] p1, int[] p2, Color color) {

		this.x1 = p1[0];
		this.y1 = p1[1];
		this.z1 = p1[2];
		
		// end coordinates
		this.x2 = p2[0];
		this.y2 = p2[1];
		this.z2 = p2[2];
			
		this.color = color.getRGB();
	}
	
	
	/**
	 * x coordinate of start point
	 */
	public double x1;
	
	/**
	 * y coordinate of start point
	 */
	public double y1;
	
	/**
	 * z coordinate of start point
	 */
	public double z1;
	
	// end coordinates
	
	/**
	 * x coordinate of end point
	 */
	public double x2;
	
	/**
	 * y coordinate of end point
	 */
	public double y2;
	
	
	/**
	 * z coordinate of end point
	 */
	public double z2;
	
	
	/**
	 * color of line (hex)
	 */
	public int color;
	
	public boolean isPair;
}

