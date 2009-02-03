// $Revision$, $Date$, $Author$

package levelsets.algorithm;

/**
 * Tiled array data structure for byte data type
 */
public class DeferredByteArray3D extends DeferredArray3D
{
   private byte defaultval = 0;
   
   /** Creates a new instance of DeferredByteArray3D */
   public DeferredByteArray3D(int xdim, int ydim, int zdim, int tilesize, byte defaultval)
   {
      super(xdim, ydim, zdim, tilesize);
      this.defaultval = defaultval;
   }
   
   public void set(int x, int y, int z, byte value)
   {
      byte[][][] tile = (byte[][][]) getTile(x, y, z, true);
      tile[x % tilesize][y % tilesize][z % tilesize] = value;
   }
   
   public byte get(int x, int y, int z)
   {
      byte[][][] tile = (byte[][][])getTile(x, y, z, false);
      
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
       return Byte.toString(this.get(x, y, z));
   }
   
   protected Object createTile(int tilesize)
   {
      byte[][][] tile = new byte [tilesize][tilesize][tilesize];
      
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
