// $Revision$, $Date$, $Author$

package levelsets.algorithm;

/**
 * Tiled array data structure for objects
 */
public class DeferredObjectArray3D<T> extends DeferredArray3D
{
   final T defaultval;
   
   /** Creates a new instance of DeferredDoubleArray3D */
   public DeferredObjectArray3D(int xdim, int ydim, int zdim, int tilesize, T defaultval)
   {
      super(xdim, ydim, zdim, tilesize);
      this.defaultval = defaultval;
   }
   
   public final void set(final int x, final int y, final int z, final T value)
   {
      final Object[][][] tile = (Object[][][]) getTile(x, y, z, true);
      tile[x % tilesize][y % tilesize][z % tilesize] = value;
   }
   
   public final T get(final int x, final int y, final int z)
   {
      final Object[][][] tile = (Object[][][])getTile(x, y, z, false);
      
      if (tile == null)
      {
         return defaultval;
      }
      else
      {
         return (T)(tile[x % tilesize][y % tilesize][z % tilesize]);
      }
      
   }
   
   public final String getAsString(final int x, final int y, final int z)
   {
       return this.get(x, y, z).toString();
   }
   
   protected Object createTile(final int tilesize)
   {
      final Object[][][] tile = new Object [tilesize][tilesize][tilesize];
      
      if (defaultval != null)
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
