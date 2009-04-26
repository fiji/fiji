// $Revision$, $Date$, $Author$

package levelsets.algorithm;

/**
 * Tiled array data structure for int data type
 */
public class DeferredIntArray3D extends DeferredArray3D
{
   final int defaultval;
   
   /** Creates a new instance of DeferredDoubleArray3D */
   public DeferredIntArray3D(final int xdim, final int ydim, final int zdim, final int tilesize, final int defaultval)
   {
      super(xdim, ydim, zdim, tilesize);
      this.defaultval = defaultval;
   }
   
   public final void set(final int x, final int y, final int z, final int value)
   {
      final int[][][] tile = (int[][][]) getTile(x, y, z, true);
      tile[x % tilesize][y % tilesize][z % tilesize] = value;
   }
   
   public final int get(final int x, final int y, final int z)
   {
      final int[][][] tile = (int[][][])getTile(x, y, z, false);
      
      if (tile == null)
      {
         return defaultval;
      }
      else
      {
         return tile[x % tilesize][y % tilesize][z % tilesize];
      }
   }
   
   public final String getAsString(final int x, final int y, final int z)
   {
       return Integer.toString(this.get(x, y, z));
   }
   
   protected final Object createTile(final int tilesize)
   {
      final int[][][] tile = new int [tilesize][tilesize][tilesize];
      
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
