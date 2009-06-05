// $Revision$, $Date$, $Author$

package levelsets.algorithm;

/**
 * Encapsulates voxel data in an object - this is important for managing voxels
 * in collections.
 */
public class BandElement extends Coordinate implements Comparable<BandElement>
{
   // The data element
   private double value;
   
   /**
    * Constructs a new BandElement
    * @param x The X coordinate to be set
    * @param y The Y coordinate to be set
    * @param z The Z coordinate to be set
    * @param value Arbitrary data to be set
    */
   public BandElement(final int x, final int y, final int z, final double value)
   {
      super(x, y, z);
      setValue(value);
   }
   
   /**
    * Sets arbitrary data this voxel element should carry
    * @param value The data to be set
    */
   final public void setValue(final double value)
   {
      this.value = value;
   }
   
   /**
    * Returns arbitrary data this voxel element was assigned
    * @return The data
    */
   final public double getValue()
   {
      return this.value;
   }
   
   /**
    * Compares this BandElement to another one
    * @param other The other BandElement
    * @return Returns 0 if the elements are identical by reference, -1 if this element`s value
    * is smaller than the other one`s, +1 otherwise
    */
   final public int compareTo(final BandElement other)
   {
      if (this == other) return 0;
      else if (this.value < other.value) return -1;
      else return +1;
   }
}
