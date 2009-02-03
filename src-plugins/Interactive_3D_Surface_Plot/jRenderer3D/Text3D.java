package jRenderer3D;

import java.awt.Color;

/**
 * This class represents a text element in a 3D coordinate system.
 * It is described by the text string, its position (x, y, z), its color and its size.
 * 
 * @author Kai Uwe Barthel
 *
 */
public class Text3D {
	
	/**
	 * Creates a new Text3D object.
	 * 
	 * @param text - the text string
	 * @param x - x position of the text element
	 * @param y - y position of the text element
	 * @param z - z position of the text element
	 * @param color - color of the text element
	 * @param size - size the text element
	 */
	public Text3D(String text, double x, double y, double z, Color color, double size) {
		this.text = text;
		this.x = x;
		this.y = y;
		this.z = z;
			
		this.color = color;

		this.size = size;
		
		this.number = 1;
	}
	
	public Text3D(String text, double x, double y, double z, Color color, double size, int number) {
		this.text = text;
		this.x = x;
		this.y = y;
		this.z = z;
			
		this.color = color;

		this.size = size;
		
		this.number = number;
	}

	/**
	 * the text string
	 */
	public String text;
	
	/**
	 * the x position of the text element
	 */
	public double x;
	
	
	/**
	 * the y position of the text element
	 */
	public double y;
	
	
	/**
	 * the z position of the text element
	 */
	public double z;
		
	
	/**
	 * the color of the text element
	 */
	public Color color;

	/**
	 * the size of the text element
	 */
	public double size;
	
	public int number;
}
