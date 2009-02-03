// $Revision$, $Date$, $Author$
package levelsets.algorithm;

/**
 * Tiled array data structure for double data type
 */
public class DeferredDoubleArray3D extends DeferredArray3D
{
   double defaultval = 0;
   
   /** Creates a new instance of DeferredDoubleArray3D */
   public DeferredDoubleArray3D(int xdim, int ydim, int zdim, int tilesize, double defaultval)
   {
      super(xdim, ydim, zdim, tilesize);
      this.defaultval = defaultval;
   }
   
   public void set(int x, int y, int z, double value)
   {
      double[][][] tile = (double[][][]) getTile(x, y, z, true);
      tile[x % tilesize][y % tilesize][z % tilesize] = value;
   }
   
   public double get(int x, int y, int z)
   {
      double[][][] tile = (double[][][])getTile(x, y, z, false);
      
      if (tile == null)
      {
         return defaultval;
      }
      else
      {
         return tile[x % tilesize][y % tilesize][z % tilesize];
      }
      
   }
   
   public String getAsString(int x, int y, int z)
   {
       return Double.toString(this.get(x, y, z));
   }
   
   protected Object createTile(int tilesize)
   {
      double[][][] tile = new double [tilesize][tilesize][tilesize];
      
      if (defaultval != 0)
      {
         for (int x = 0; x < tile.length; x++)
         {
            for (int y = 0; y < tile[0].length; y++)
            {
               for (int z = 0; z < tile[0][0].length; z++)
               {
                  tile[x][y][z] = defaultval;
               }
            }
         }
      }
      
      return tile;
   }
}
