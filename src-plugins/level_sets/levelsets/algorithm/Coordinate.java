// $Revision$, $Date$, $Author$

package levelsets.algorithm;

/**
 * Encapsulates a three-dimensional Coordinate.
 */
public class Coordinate
{
   // The coordinates
   public int x, y, z;
   
   /**
    * Creates a new instance of Coordinate
    * @param x The X coordinate
    * @param y The Y coordinate
    * @param z The Z coordinate
    */
   public Coordinate(int x, int y, int z)
   {
      this.setX(x);
      this.setY(y);
      this.setZ(z);
   }

   /**
    * Returns the X coordinate
    * @return The X coordinate
    */
   public int getX()
   { return x; }
   
   /**
    * Sets the X coordinate
    * @param x The X coordinate to be set
    */
   public void setX(int x)
   {
      this.x = x;
   }
   
   /**
    * Returns the Y coordinate
    * @return The Y coordinate
    */
   public int getY()
   {
      return y;
   }
   
   /**
    * Sets the Y coordinate
    * @param y The Y coordinate to be set
    */
   public void setY(int y)
   {
      this.y = y;
   }
   
   /**
    * Returns the X coordinate
    * @return The X coordinate
    */
   public int getZ()
   {
      return z;
   }
   
   /**
    * Sets the Z coordinate
    * @param z The Z coordinate to be set
    */
   public void setZ(int z)
   {
      this.z = z;
   } 
}
