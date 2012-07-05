/**
 * <p>Title: ImageContainer</p>
 *
 * <p>Description: Adapter of the ImageJ image structures to the code from Toersel.</p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: The Hackathon 2008 @ Janelia Farm and previous work by Arne-Michael Toersel</p>
 *
 * <p>License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * @author Arne-Michael Toersel and Erwin Frise
 * @version 1.0
 */

package levelsets.ij;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import levelsets.filter.Filter;


/**
 * Container for three-dimensional images
 */
public class ImageContainer
{
   protected ImagePlus ip = null;
   protected String title = "Level set workset"; // generic title
   protected ImageProcessor [] sproc;
   final protected short [][] proc_pixels;
   protected int width = 0;
   protected int stack_size = 0;
   protected boolean isStack = false;
   private int pixel;
   
   protected ShortProcessor lproc = null;

   public ImageContainer() {
	   // ImageProgressContainer doesn't even use it ...
	   proc_pixels = null;
   }
   
   /**
    * Constructs a new ImageContainer
    * @param images An BufferdImages array
    */
   public ImageContainer(final ImagePlus ip)
   {
      this.ip = ip; 
	   
      stack_size = ip.getStackSize();
            
      isStack = stack_size > 1 ? true : false;
      
      sproc = new ShortProcessor[stack_size];
      proc_pixels = new short[stack_size][];
      
      if ( isStack ) {
    	  for ( int i = 0; i < stack_size; i++ ) {   		  
    		  sproc[i] = ( ShortProcessor ) ip.getStack().getProcessor(i+1).convertToShort(false);
    		  proc_pixels[i] = (short []) sproc[i].getPixels();
    	  }
      } else {
    	  sproc[0] = (ShortProcessor) ip.getProcessor().convertToShort(false);
    	  proc_pixels[0] = (short []) sproc[0].getPixels();
      }
      
      width = sproc[0].getWidth();
   }
   
   public ImageContainer(final ImageProcessor [] iproc)
   {
	   stack_size = iproc.length;
	   
	   isStack = stack_size > 1 ? true : false;
	   
	   sproc = new ShortProcessor[stack_size];
	   proc_pixels = new short[stack_size][];
	   
	   for ( int i = 0; i < stack_size; i++ ) {
		   sproc[i] = ( ShortProcessor ) iproc[i].convertToShort(false);
		   proc_pixels[i] = (short []) sproc[i].getPixels();
	   }
	   
	   width = sproc[0].getWidth();
   }
   
   // for deepcopy
   private ImageContainer(final ShortProcessor [] iproc) {
   
	   stack_size = iproc.length;
	   
	   isStack = stack_size > 1 ? true : false;
	   
	   sproc = new ShortProcessor[stack_size];
	   proc_pixels = new short[stack_size][];
	   
	   for ( int i = 0; i < stack_size; i++ ) {
		   sproc[i] = ( ShortProcessor ) iproc[i].duplicate().convertToShort(false);
		   proc_pixels[i] = (short []) sproc[i].getPixels();
	   }
	   
	   width = sproc[0].getWidth();
   }
      
   
   public final ImagePlus createImagePlus(final String title) {
	   
	   if ( title != null ) {
		   this.title = title;
	   }
	   
	   if ( ip != null ) {
		   ip.setTitle(this.title);
		   return ip;
	   }
	   
	   if ( isStack ) {
		   final ImageStack is = new ImageStack(this.getWidth(), this.getHeight());
		   for ( int i = 0; i < stack_size; i++ ) {
			   is.addSlice("", sproc[i]);
		   }
		   
		   int slice = null != ip ? ip.getCurrentSlice() : 1;
		   ip = new ImagePlus(this.title, is);
//		   ip.setStack("", is);
	   	   ip.setSlice(slice);
	   } else {
		   ip = new ImagePlus(this.title, sproc[0]);
	   }

	   return ip;
   }
   
   public final ImagePlus updateImagePlus(final String title) {
	   
	   if ( title != null ) {
		   this.title = title;
	   }
	   	   
	   if ( isStack ) {
		   final ImageStack is = new ImageStack(this.getWidth(), this.getHeight());
		   for ( int i = 0; i < stack_size; i++ ) {
			   is.addSlice("", sproc[i]);
		   }
		   
		   if ( ip == null ) {
			   ip = new ImagePlus(this.title, is);
		   } else {
			   ip.setStack(this.title, is);
		   }
	   } else {
		   if ( ip == null ) {
			   ip = new ImagePlus(this.title, sproc[0]);
		   } else {
			   ip.setProcessor(this.title, sproc[0]);
		   }
	   }
	   
	   return ip;
   }

   /**
    * Returns the number of contained images
    * @return The array filled with pixel values - usually RGB(A).
    */
   public final int getImageCount() {
	   return stack_size;
   }
   
   
   /**
    * Returns an passed array filled with the pixel values at the selected
    * coordinates
    * @param x The X coordinate
    * @param y The Y coordinate
    * @param z The Z coordinate
    * @param pixel An array suitable to be filled with pixel values - usually RGB(A).
    * @return The array filled with the pixel values
    */
   public final int getPixel(final int x, final int y, final int z)
   {
	   final int pos = y * width + x;
	   if ( pos >= proc_pixels[0].length ) {
		   System.out.println("Exception: x=" + x + ",y=" + y);
		   IJ.log("Exception: x=" + x + ",y=" + y);
	   }
	   
	   if ( isStack ) {
		   return proc_pixels[z][pos];
	   } else {
		   return proc_pixels[0][pos];
	   }
   }
   
   /**
    * Returns the width of the contained images
    * @return The height of the contained images
    */
   public final int getWidth()
   {
      return width;
   }
   
   /**
    * Returns the height of the contained images
    * @return The height of the contained images
    */
   public final int getHeight()
   {
      return sproc[0].getHeight();
   }
   
   /**
    * Sets the selected pixel to value provided by a array
    * @param x The X coordinate
    * @param y The Y coordinate
    * @param z The Z coordinate
    * @param pixel An array containing values for the pixel - usually RGB(A).
    */
   public final void setPixel(final int x, final int y, final int z, final int pixel)
   {
	   if ( isStack ) {
		   proc_pixels[z][x * width + y] = (short) pixel;
	   } else {
		   proc_pixels[0][x * width + y] = (short)pixel;
	   }
   }
   
   /**
    * Returns a copy of a contained image.
    * @param index The index (zero based indices) of the image to copy
    * @return A deep copy of the image
    */
//   public BufferedImage deepCopySingle(int index)
//   {
//   }
   
   /**
    * Returns a deep copy of the whole ImageContainer
    * @return The deep copy of the ImageContainer
    */
   public final ImageContainer deepCopy()
   {
      return new ImageContainer(sproc);
   }
   
   public final ImageProgressContainer progressCopy()
   {
	   return new ImageProgressContainer(this);   
   }
   
   
   
   /**
    * Sets the image stack to the contents of the passed three-dimensional double
    * value array. The values are scaled to grey values between 0 and 255
    * @param data The data array
    */
   public final void setData(final double[][][] data)
   {
      int min = Integer.MAX_VALUE;
      int max = Integer.MIN_VALUE;
      
      // aquire minimum and maximum data values
      for (int i = 0; i < data.length; i++)
      {
         for (int j = 0; j < data[0].length; j++)
         {
            for (int k = 0; k < data[0][0].length; k++)
            {
               if (data[i][j][k] == Double.MAX_VALUE) continue;
               int current = (int)(Math.abs(data[i][j][k]));
               if (current < min) min = current;
               if (current > max) max = current;
            }
         }
      }
      
      // Offset for the minimum value (normate to zero)
      final int add = -1 * min;
      // Scale value for normation to 0-255 range 
      final double scaler = 255d / (max + add - (min + add));
      
      // Transfer data to the images, scale to values between 0 and 255
      for (int z = 0; z < this.getImageCount(); z++)
      {
         
         for (int x = 0; x < this.getWidth(); x++)
         {
            for (int y = 0; y < this.getHeight(); y++)
            {
               if (data[x][y][z] == Double.MAX_VALUE) continue;
               //int current = (int)Math.round(((int)(Math.abs(data[x][y][z])) + add) * scaler);
               this.setPixel(x, y, z, (int)Math.round(((int)(Math.abs(data[x][y][z])) + add) * scaler));
            }
         }
      }
   }
   
   /**
    * Calculates grey value gradients of the image container an returns a result
    * array.
    * @return The result array
    */
   public final double[][][] calculateGradients()
   {
      // IJ.log("Calculating gradients");
      final double zScale = getzScale();
      final double[][][] gradients = new double[this.getWidth()][this.getHeight()][this.getImageCount()];

      for (int z = 0; z < gradients[0][0].length; z++)
      {
         for (int x = 1; x < gradients.length - 1; x++)
         {
            for (int y = 1; y < gradients[0].length - 1; y++)
            {
               
               final double xGradient =
                       (this.getPixel(x + 1, y, z) - this.getPixel(x - 1, y, z)) / 2;
               final double yGradient =
                       (this.getPixel(x, y + 1, z) - this.getPixel(x, y - 1, z)) / 2;
               final double zGradient;
               if ((z > 0) && (z < gradients[0][0].length - 1))
               {
                  zGradient =
                       (this.getPixel(x, y, z + 1) - this.getPixel(x, y, z - 1)) / (2 * zScale);
               } else {
		       zGradient = 0;
	       }
               gradients[x][y][z] = Math.sqrt(xGradient * xGradient + yGradient * yGradient + zGradient * zGradient);
            }
         }
      }
      
      return gradients;
   }
   
   /**
    * Applies the passed Filter on every image in the container.
    * @param filter The filter
    */
   public final void applyFilter(final Filter filter)
   {
      applyFilter(filter, 0, stack_size - 1);
   }
   
   /**
    * Applies the passed filter on the selected range of images
    * @param filter The Filter
    * @param start The start index (zero based indices)
    * @param end The end index (zero based indices)
    */
   public final void applyFilter(final Filter filter, final int start, final int end)
   {
      IJ.log("Applying filter : " + filter.getClass().getCanonicalName() + " (on images " + start + " to " + end + ")");
      if (this.sproc == null) return;
      
      for (int i = start; i <= end; i++)
      {
    	 //images[i] = filter.filter(images[i]);
    	 //final BufferedImage image = ij2bufferedImage(sproc[i].createImage());
    	 //final BufferedImage filtered_image = filter.filter(image);
         //sproc[i] = new ShortProcessor(filtered_image);

	 // THIS HAS TO CHANGE: it creates a new ShortProcessor from a BufferedImage that is create from the createImage() of another ShortProcessor !
	 // AND in the filter subclasses, all sorts of unholy java imaging code goes on.
	 // sproc[i] = new ShortProcessor(filter.filter(ij2bufferedImage(sproc[i].createImage())));
	 final short[] src_pix = (short[]) sproc[i].getPixels();
	 final short[] pix = new short[src_pix.length];
	 final int width = sproc[i].getWidth();
	 final int height = sproc[i].getHeight();
	 filter.filter(width, height, src_pix, pix);
	 sproc[i] = new ShortProcessor(width, height, pix, null); 
      }
   }
       
   public final BufferedImage ij2bufferedImage(final Image im)
   {
      final BufferedImage bi = new BufferedImage(im.getWidth(null),im.getHeight(null), BufferedImage.TYPE_USHORT_GRAY);
      final Graphics bg = bi.getGraphics();
      bg.drawImage(im, 0, 0, null);
      bg.dispose();
      return bi;
   }
   
   public final double getzScale()
   {
	   return 1.0;
   }
   
}
