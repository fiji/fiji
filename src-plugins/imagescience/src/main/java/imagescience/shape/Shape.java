package imagescience.shape;

import imagescience.image.ByteImage;
import imagescience.image.Image;

/** Interface for objects representing closed and bounded geometrical figures up to 5D. */
public interface Shape {
	
	/** Computes a bitmapped version of the shape.
		
		@param binary determines whether the returned bitmap image is two-valued. If {@code true}, image elements whose center positions fall inside the shape are set to {@code 255}, and elements whose center positions fall outside the shape are set to {@code 0}. If {@code false}, additional processing is performed, by which each image element falling partly inside and partly outside the shape is set to {@code 255} times its (approximate) inside-fraction.
		
		@return a new {@link ByteImage} object containing a bitmapped version of the shape. The origin of the integer coordinate system of the image corresponds to the position in the shape coordinate system obtained by taking the floor value of the lower bound of the shape in each dimension.
	*/
	public Image bitmap(final boolean binary);
	
	/** Computes the bounding box of the shape.
		
		@return a new {@code Bounds} object containing the bounding box of the shape.
	*/
	public Bounds bounds();
	
	/** Indicates the position of a point relative to the shape.
		
		@param point the point whose position relative to the shape is to be tested.
		
		@return {@code true} if the point is on or inside the boundary of the shape; {@code false} if this is not the case.
		
		@exception NullPointerException if {@code point} is {@code null}.
	*/
	public boolean contains(final Point point);
	
	/** Duplicates the shape.
		
		@return a new {@code Shape} object that is an exact copy of this object. All information is copied and no memory is shared between this and the returned object.
	*/
	public Shape duplicate();
	
	/** Indicates whether the shape is empty.
		
		@return {@code true} if the shape does not enclose any space; {@code false} if it does.
	*/
	public boolean empty();
	
	/** Indicates whether this object has the same shape as the given object.
		
		@param shape the shape to compare this shape with.
		
		@return {@code true} if {@code shape} is not {@code null}, and an instance of the same class as this object, with the exact same shape; {@code false} if this is not the case.
	*/
	public boolean equals(final Shape shape);
	
}
