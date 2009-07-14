// $Revision$, $Date$, $Author$

package levelsets.algorithm;

/**
 * Tiled array data structure for byte data type
 */
public class DeferredByteArray3D extends DeferredArray3D
{
   private final byte defaultval;
   
   /** Creates a new instance of DeferredByteArray3D */
   public DeferredByteArray3D(final int xdim, final int ydim, final int zdim, final int tilesize, final byte defaultval)
   {
      super(xdim, ydim, zdim, tilesize);
      this.defaultval = defaultval;
   }
   
   public final void set(final int x, final int y, final int z, final byte value)
   {
      final byte[][][] tile = (byte[][][]) getTile(x, y, z, true);
      tile[x % tilesize][y % tilesize][z % tilesize] = value;
   }
   
   public final byte get(final int x, final int y, final int z)
   {
      final byte[][][] tile = (byte[][][])getTile(x, y, z, false);
      
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
       return Byte.toString(this.get(x, y, z));
   }
   
   protected final Object createTile(final int tilesize)
   {
      final byte[][][] tile = new byte [tilesize][tilesize][tilesize];
      
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
