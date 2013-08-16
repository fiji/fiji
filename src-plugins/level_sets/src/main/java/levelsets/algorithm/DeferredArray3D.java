// $Revision$, $Date$, $Author$

package levelsets.algorithm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Base class for tiled array data structure
 */
public abstract class DeferredArray3D
{
   private final int xdim, ydim, zdim;
   private final int xtiles, ytiles, ztiles;
   protected final int tilesize;
   protected final Object[] tiles;
   
   /**
    * Creates a new instance of DeferredArray3D
    * @param xdim Size in X direction
    * @param ydim Size in Y direction
    * @param zdim Size in Z direction
    * @param tilesize The tile size - length of an edge tile.
    */
   public DeferredArray3D(final int xdim, final int ydim, final int zdim, final int tilesize)
   {
      this.tilesize = tilesize;
      
      this.xdim = xdim;
      this.ydim = ydim;
      this.zdim = zdim;
      
      int xtiles = xdim / tilesize;
      if (xdim % tilesize > 0) xtiles++;
      this.xtiles = xtiles;

      int ytiles = ydim / tilesize;
      if (ydim % tilesize > 0) ytiles++;
      this.ytiles = ytiles;

      int ztiles = zdim / tilesize;
      if (zdim % tilesize > 0) ztiles++;
      this.ztiles = ztiles;
      
      tiles = new Object[xtiles * ytiles * ztiles];
   }
   
   /**
    * Returns the length of the tile edge. The edges have uniform length in all
    * directions
    * @return The size of the tile edges
    */
   public final int getTileSize()
   {
      return tilesize;
   }
   
   /**
    * Requests the tile for the passed coordinates.
    * @param x The X index
    * @param y The Y index
    * @param z The Z index
    * @param create Determines whether the tile should be created if it has not been allocated yet.
    * @return The tile - a three dimensional array of the proper data type
    */
   protected final Object getTile(final int x, final int y, final int z, final boolean create)
   {
      checkBounds(x, y, z);
      
      final int x_tile = x / tilesize;
      final int y_tile = y / tilesize;
      final int z_tile = z / tilesize;
      
      final int offset = x_tile + y_tile * xtiles + z_tile * xtiles * ytiles;
      
      final Object tile = tiles[offset];
      if (tile == null && create == true)
      {
         tiles[offset] = createTile(tilesize);
         return tiles[offset];
      }
      
      return tile;
   }
   
   /**
    * Returns the size of the whole virtual array in X direction
    * @return The size in X direction
    */
   public final int getXLength()
   {
      return xdim;
   }
   
   /**
    *  Returns the size of the whole virtual array in Y direction
    * @return The size in Y direction
    */
   public final int getYLength()
   {
      return ydim;
   }
   
   /**
    *  Returns the size of the whole virtual array in Z direction
    * @return The size in Z direction
    */
   public final int getZLength()
   {
      return zdim;
   }
   
   /**
    * Dumps the array content into a textfile. A matrix is dumped for every Z layer.
    * @param path Path to the file to be created.
    */
   public final void dumpToFile(final String path)
   {
      try
      {
         final BufferedWriter out = new BufferedWriter(new FileWriter(new File(path)));
         
         out.write(this.getXLength() + " " + this.getYLength() + " " + this.getZLength());
         out.newLine(); out.newLine();
         
         for (int z = 0; z < this.getZLength(); z++)
         {
            for (int y = 0; y < this.getYLength(); y++)
            {
               for (int x = 0; x < this.getXLength(); x++)
               {
                  out.write(this.getAsString(x, y, z) + " ");
               }
               out.newLine();
            }
            out.newLine();
         }
         out.close();
      }
      catch (IOException ioe)
      {
         ioe.printStackTrace();
      }
   }
   
   private final void checkBounds(final int x, final int y, final int z)
   {
      if (x < 0 || x > (xdim - 1) || y < 0 || y > (ydim - 1) || z < 0 || z > (zdim - 1))
      {
         throw new ArrayIndexOutOfBoundsException("At index : (" + x + ", " + y + ", " + z + ")");
      }
   }
   
   /**
    * Creates a tile. This is delegated to concrete subclasses as the tile needs to
    * be of the proper data type.
    * @param tilesize The tile dimension
    * @return The tile - a three dimensional array of the proper data type
    */
   protected abstract Object createTile(final int tilesize);
   
   /**
    * Returns the data at the requested position represented as a String
    * @param x The X index
    * @param y The Y index
    * @param z The Z index
    * @return The data as String
    */
   protected abstract String getAsString(final int x, final int y, final int z);
}
