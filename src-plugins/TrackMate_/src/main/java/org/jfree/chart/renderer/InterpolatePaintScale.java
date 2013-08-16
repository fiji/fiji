package org.jfree.chart.renderer;

import java.awt.Color;
import java.io.Serializable;
import java.util.Set;
import java.util.TreeMap;

import org.jfree.util.PublicCloneable;

/**
 * This class implements a {@link PaintScale} that generate colors interpolated 
 * within a list of given color, using a linear scale.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Sept 2010
 */
public class InterpolatePaintScale implements PaintScale, PublicCloneable, Serializable {

	private static final long serialVersionUID = 2977884191627862512L;
	private static final Color DEFAULT_COLOR = Color.BLACK;
	private double lowerBound;
	private double upperBound;
	private TreeMap<Double, Color> colors = new TreeMap<Double, Color>();
	private Color defaultColor;

	/*
	 * INNER CLASSES
	 */
	
	/**
	 * A {@link InterpolatePaintScale} that map a typical "Jet" colormap going from
	 * blue to red to the range [0, 1].
	 */
	public static final InterpolatePaintScale Jet;
	static {
		Jet = new InterpolatePaintScale(0, 1);
		Jet.add(0.00, new Color(0.0f, 0.0f, 1.0f));
		Jet.add(0.16, new Color(0.0f, 0.5f, 1.0f));
		Jet.add(0.33, new Color(0.0f, 1.0f, 1.0f));
		Jet.add(0.50, new Color(0.5f, 1.0f, 0.5f));
		Jet.add(0.66, new Color(1.0f, 1.0f, 0.0f));
		Jet.add(0.83, new Color(1.0f, 0.5f, 0.0f));
		Jet.add(1.00, new Color(1.0f, 0.0f, 0.0f));
	}
	
	
	/*
	 * CONSTRUCTORS
	 */
	
	/**
	 * Create a paint scale with given lower and upper bound, and a specified default color.
	 */
	public InterpolatePaintScale(double lowerBound, double upperBound, Color defaultColor) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.defaultColor = defaultColor;
	}
	
	/**
	 * Create a paint scale with a given lower and upper bound and a default black color.
	 */
	public InterpolatePaintScale(double lowerBound, double upperBound) {
		this(lowerBound, upperBound, DEFAULT_COLOR);
	}

	/**
	 * Create a paint scale with a lower bound of 0, an upper bound of 1 and a default
	 * black color. 
	 */
	public InterpolatePaintScale() {
		this(0, 1);
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Add a color to the color list of this paint scale, at the position given by
	 * <code>value</code>. If <code>value</code> is greater than the upper bound or lower
	 * than the lower bound set at construction, this call will be ignored. 
	 */
	public void add(double value, Color color) {
		if (value > upperBound) return;
		if (value < lowerBound) return;
		colors.put(value, color);
	}
	
	
	@Override
	public double getLowerBound() {
		return lowerBound;
	}

	/**
	 * Return a color interpolated within the color list of this paint scale. 
	 * The interpolation is a linear one between the two colors in the list 
	 * whose associated values frame the one given.
	 */
	@Override
	public Color getPaint(double value) {
		if (colors.isEmpty()) return defaultColor;
		if (colors.size() == 1) return colors.get(colors.firstKey());
		
		if (value > upperBound)
			value = upperBound;
		if (value < lowerBound)
			value = lowerBound;
		Set<Double> keys = colors.keySet();
		double bottom = colors.firstKey();
		double top = colors.lastKey();
		for (double key : keys) { 
			top = key;
			if (value < key) 
				break;
			 else 
				 bottom = top;
		}
		
		double alpha;
		if (top == bottom)
			alpha = 0; // we reached the end of the list
		else
			alpha = (value-bottom) / (top - bottom);
		
		Color colorBottom = colors.get(bottom);
		Color colorTop = colors.get(top);
		int red 	= (int) ((1-alpha) * colorBottom.getRed() + alpha * colorTop.getRed());
		int green 	= (int) ((1-alpha) * colorBottom.getGreen() + alpha * colorTop.getGreen()); 
		int blue	= (int) ((1-alpha) * colorBottom.getBlue() + alpha *  colorTop.getBlue()); 
		return new Color(red, green, blue);
	}

	@Override
	public double getUpperBound() {
		return upperBound;
	}
	
	public InterpolatePaintScale clone() {
		InterpolatePaintScale ips = new InterpolatePaintScale(lowerBound, upperBound);
		for(double key : colors.keySet())
			ips.add(key, colors.get(key));
		return ips;
	}

}
