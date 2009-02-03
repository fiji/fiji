package jRenderer3D;

import java.awt.Color;

/**
 * <p>This class represents a point in a 3D coordinate system. 
 * 		It is defined by its position (x,y,z), its size and its color.
 * </p>
 * 
 * <p>A point can be drawn in the folling modes:
 * 	<ul>
 * 		<li>DOTS: Draws point as a dot. Size information has no effect (fastest).</li>
 * 		<li>CIRCLES: Draws a point as a (2D) circle.</li>
 * 		<li>SPHERES: Draws a point as a sphere (slowest).</li>
 * 	</ul>
 * </p>
 * 
 * @author Kai Uwe Barthel
 *
 */
public class Point3D {

////////////////////////////////////////////////////////
	//
	//	 Point drawing modes
	
	/**
	 * Draws a point as a dot. Size information has no effect.
	 */
	public static final int DOT = 0;
	/**
	 * Draws a point as a (2D) circle.
	 */
	public static final int CIRCLE = 1;
	/**
	 * Draws a point as a sphere (slowest).
	 */
	public static final int SPHERE = 2;
	
	
	
	
	/**
	 * x-coordinate
	 */
	protected double x;
	
	/**
	 * y-coordinate
	 */
	protected double y;
	
	/**
	 * z-coordinate
	 */
	protected double z;
		
	/**
	 * color of point
	 */
	protected int rgb;

	/**
	 * size of point
	 */
	protected double size = 1;
	
	protected int drawMode = DOT;
	
	protected Point3D(){
		
	}
	
	/**
	 * Creates a new Point3D object. No parameters except of size (set to 1) are set.
	 *
	 */
	public Point3D(double x, double y, double z, int c){
		this.x = x;
		this.y = y;
		this.z = z;
		this.rgb = c;
	}
	
	public Point3D(double x, double y, double z, Color c){
		this.x = x;
		this.y = y;
		this.z = z;
		this.rgb = c.getRGB();
	}
	
	
	/**
	 * Creates a new Point3D object with given x, y, and z coordinates, size and color.
	 * 
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @param z the z-coordinate
	 * @param size the size of the point
	 * @param rgb the rgb-value of the point (like 0xFF00FFFF)
	 */
	public Point3D(double x, double y, double z, double size, int rgb) {
		this.x = x;
		this.y = y;
		this.z = z;
			
		this.rgb = rgb;

		this.size = size;
	}
	
	/**
	 * Creates a new Point3D object with given x, y, and z coordinates, size and color.
	 * 
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @param z the z-coordinate
	 * @param size the size of the point
	 * @param rgb the rgb-value of the point (like 0xFF00FFFF)
	 * @param drawMode mode of drawing the point
	 */
	public Point3D(double x, double y, double z, double size, int rgb, int drawMode) {
		this.x = x;
		this.y = y;
		this.z = z;
			
		this.rgb = rgb;

		this.size = size;
		this.drawMode = drawMode;
	}
	
	/**
	 * Creates a new Point3D object with given x, y, and z coordinates, size and color.
	 * 
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @param z the z-coordinate
	 * @param size the size of the point
	 * @param color the color of the point (like Color.RED)
	 */
	public Point3D(double x, double y, double z, double size, Color color) {
		this.x = x;
		this.y = y;
		this.z = z;
			
		this.rgb = color.getRGB();

		this.size = size;
	}
	
	/**
	 * Creates a new Point3D object with given x, y, and z coordinates, size and color.
	 * 
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @param z the z-coordinate
	 * @param size the size of the point
	 * @param color the color of the point (like Color.RED)
	 * @param drawMode mode of drawing the point
	 */
	public Point3D(double x, double y, double z, double size, Color color, int drawMode) {
		this.x = x;
		this.y = y;
		this.z = z;
			
		this.rgb = color.getRGB();

		this.size = size;
		
		this.drawMode = drawMode;
	}


}

