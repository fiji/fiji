// $Revision$, $Date$, $Author$

package levelsets.algorithm;

/**
 * Tiled array data structure for int data type
 */
public class DeferredIntArray3D extends DeferredArray3D
{
   int defaultval = 0;
   
   /** Creates a new instance of DeferredDoubleArray3D */
   public DeferredIntArray3D(int xdim, int ydim, int zdim, int tilesize, int defaultval)
   {
      super(xdim, ydim, zdim, tilesize);
      this.defaultval = defaultval;
   }
   
   public void set(int x, int y, int z, int value)
   {
      int[][][] tile = (int[][][]) getTile(x, y, z, true);
      tile[x % tilesize][y % tilesize][z % tilesize] = value;
   }
   
   public int get(int x, int y, int z)
   {
      int[][][] tile = (int[][][])getTile(x, y, z, false);
      
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
       return Integer.toString(this.get(x, y, z));
   }
   
   protected Object createTile(int tilesize)
   {
      int[][][] tile = new int [tilesize][tilesize][tilesize];
      
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
