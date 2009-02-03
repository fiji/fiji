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
   protected short [][] proc_pixels;
   protected int width = 0;
   protected int stack_size = 0;
   protected boolean isStack = false;
   private int pixel;
   
   protected ShortProcessor lproc = null;
   
   /**
    * Constructs an empty ImageContainer
    */
   public ImageContainer()
   {};
   
   /**
    * Constructs a new ImageContainer
    * @param images An BufferdImages array
    */
   public ImageContainer(ImagePlus ip)
   {
       setImages(ip);      
   }
   
   public ImageContainer(ImageProcessor [] iproc)
   {
	   setImages(iproc);
   }
   
   
      
   /**
    * Replaces this ImageContainers content with the passed ImagePlus.
    * @param images ImagePlus
    */
   public void setImages(ImagePlus ip)
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
   
   
   public void setImages(ImageProcessor [] iproc) {
	   
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

   
   public void cloneImages(ImageProcessor [] iproc) {
   
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
      
   
   public ImagePlus createImagePlus(String title) {
	   
	   if ( title != null ) {
		   this.title = title;
	   }
	   
	   if ( ip != null ) {
		   ip.setTitle(this.title);
		   return ip;
	   }
	   
	   if ( isStack ) {
		   ImageStack is = new ImageStack(this.getWidth(), this.getHeight());
		   for ( int i = 0; i < stack_size; i++ ) {
			   is.addSlice("", sproc[i]);
		   }
		   
		   ip = new ImagePlus(this.title, is);
//		   ip.setStack("", is);
	   } else {
		   ip = new ImagePlus(this.title, sproc[0]);
	   }
	   
	   return ip;
   }
   
   public ImagePlus updateImagePlus(String title) {
	   
	   if ( title != null ) {
		   this.title = title;
	   }
	   	   
	   if ( isStack ) {
		   ImageStack is = new ImageStack(this.getWidth(), this.getHeight());
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
   public int getImageCount() {
	   return stack_size;
   }
   
   
   /**
    * Returns an paased array filled with the pixel values at the selected
    * coordinates
    * @param x The X coordinate
    * @param y The Y coordinate
    * @param z The Z coordinate
    * @param pixel An array suitable to be filled with pixel values - usually RGB(A).
    * @return The array filled with the pixel values
    */
   public int getPixel(int x, int y, int z)
   {
	   int pos = y * width + x;
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
   public int getWidth()
   {
      return width;
   }
   
   /**
    * Returns the height of the contained images
    * @return The height of the contained images
    */
   public int getHeight()
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
   public void setPixel(int x, int y, int z, int pixel)
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
   public ImageContainer deepCopy()
   {
      ImageContainer cpy = new ImageContainer();
      cpy.cloneImages(sproc);
      
      return cpy;
   }
   
   public ImageProgressContainer progressCopy()
   {
	   return new ImageProgressContainer(this);   
   }
   
   
   
   /**
    * Sets the image stack to the contents of the passed three-dimensional double
    * value array. The values are scaled to grey values between 0 and 255
    * @param data The data array
    */
   public void setData(double[][][] data)
   {
      int min = Integer.MAX_VALUE;
      int max = Integer.MIN_VALUE;
      int add = 0;
      
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
      add = -1 * min;
      // Scale value for normation to 0-255 range 
      double scaler = 255d / (max + add - (min + add));
      
      // Transfer data to the images, scale to values between 0 and 255
      int pixel;
      for (int z = 0; z < this.getImageCount(); z++)
      {
         
         for (int x = 0; x < this.getWidth(); x++)
         {
            for (int y = 0; y < this.getHeight(); y++)
            {
               if (data[x][y][z] == Double.MAX_VALUE) continue;
               int current = (int)Math.round(((int)(Math.abs(data[x][y][z])) + add) * scaler);
               pixel = current;
               this.setPixel(x, y, z, pixel);
            }
         }
      }
   }
   
   /**
    * Calculates grey value gradients of the image container an returns a result
    * array.
    * @return The result array
    */
   public double[][][] calculateGradients()
   {
      IJ.log("Calculating gradients");
      double zScale = getzScale();
      double[][][] gradients = new double[this.getWidth()][this.getHeight()][this.getImageCount()];

      for (int z = 0; z < gradients[0][0].length; z++)
      {
         for (int x = 1; x < gradients.length - 1; x++)
         {
            for (int y = 1; y < gradients[0].length - 1; y++)
            {
               
               double xGradient =
                       (this.getPixel(x + 1, y, z) - this.getPixel(x - 1, y, z)) / 2;
               double yGradient =
                       (this.getPixel(x, y + 1, z) - this.getPixel(x, y - 1, z)) / 2;
               double zGradient = 0;
               if ((z > 0) && (z < gradients[0][0].length - 1))
               {
                  zGradient =
                       (this.getPixel(x, y, z + 1) - this.getPixel(x, y, z - 1)) / (2 * zScale);
               }
               gradients[x][y][z] = Math.sqrt(xGradient * xGradient + yGradient * yGradient);
            }
         }
      }
      
      return gradients;
   }
   
   /**
    * Applies the passed Filter on every image in the container.
    * @param filter The filter
    */
   public void applyFilter(Filter filter)
   {
      applyFilter(filter, 0, stack_size - 1);
   }
   
   /**
    * Applies the passed filter on the selected range of images
    * @param filter The Filter
    * @param start The start index (zero based indices)
    * @param end The end index (zero based indices)
    */
   public void applyFilter(Filter filter, int start, int end)
   {
      IJ.log("Applying filter : " + filter.getClass().getCanonicalName() + " (on images " + start + " to " + end + ")");
      if (this.sproc == null) return;
      
      for (int i = start; i <= end; i++)
      {
    	 //images[i] = filter.filter(images[i]);
    	 BufferedImage image = ij2bufferedImage(sproc[i].createImage());
    	 BufferedImage filtered_image = filter.filter(image);
         sproc[i] = new ShortProcessor(filtered_image);
      }
   }
       
   public BufferedImage ij2bufferedImage(Image im)
   {
      BufferedImage bi = new BufferedImage(im.getWidth(null),im.getHeight(null), BufferedImage.TYPE_USHORT_GRAY);
      Graphics bg = bi.getGraphics();
      bg.drawImage(im, 0, 0, null);
      bg.dispose();
      return bi;
   }
   
   public double getzScale()
   {
	   return 1.0;
   }
   
}
