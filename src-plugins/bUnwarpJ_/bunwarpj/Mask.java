package bunwarpj;

/**
 * bUnwarpJ plugin for ImageJ(C).
 * Copyright (C) 2005-2010 Ignacio Arganda-Carreras and Jan Kybic 
 *
 * More information at http://biocomp.cnb.csic.es/%7Eiarganda/bUnwarpJ/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
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
 */

import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Vector;

/**
 * This class is responsible for the mask preprocessing that takes
 * place concurrently with user-interface events. It contains methods
 * to compute the mask pyramids.
 */
public class Mask
{ /* begin class Mask */

    /*....................................................................
       Private variables
    ....................................................................*/
    // Mask related
    /** mask flags */
    private boolean[]     mask;
    /** mask width */
    private int           width;
    /** mask height */
    private int           height;
    /** polygon composing the flag */
    private Polygon       polygon = null;
    /** flag to check if the mask comes from the stack of images */
    private boolean       mask_from_the_stack;

    /*....................................................................
       Public methods
    ....................................................................*/

    /*------------------------------------------------------------------*/
    /**
     * Constructor, the input image is used only to take the
     * image size if take_mask is false, otherwise, it is used as the 
     * mask information.
     *
     * @param ip image
     * @param take_mask flag to take the mask from the stack of images
     */
    public Mask (final ImageProcessor ip, boolean take_mask)
    {
       width  = ip.getWidth();
       height = ip.getHeight();
       mask = new boolean[width * height];
       
       // Empty mask
       if (!take_mask) 
       {
           mask_from_the_stack = false;
           clearMask();
       } 
       else // Mask from image 
       {
    	   mask_from_the_stack = true;
    	   int k = 0;
    	   
    	   if (ip instanceof ByteProcessor) 
    	   {
    		   final byte[] pixels = (byte[])ip.getPixels();
    		   for (int y = 0; (y < height); y++) 
    			   for (int x = 0; (x < width); x++, k++) 
    				   mask[k] = (pixels[k] != 0);
    	   }
    	   else if (ip instanceof ShortProcessor) 
    	   {
    		   final short[] pixels = (short[])ip.getPixels();
    		   for (int y = 0; (y < height); y++) 
    			   for (int x = 0; (x < width); x++, k++)
    				   mask[k] = (pixels[k] != 0);
    	   }
    	   else if (ip instanceof FloatProcessor) 
    	   {
    		   final float[] pixels = (float[])ip.getPixels();
    		   for (int y = 0; (y < height); y++) 
    			   for (int x = 0; (x < width); x++, k++)
    				   mask[k] = (pixels[k] != 0.0F);    				       			       		  
    	   }
    	   else if (ip instanceof ColorProcessor) 
    	   {
    		   for (int y = 0; (y < height); y++) 
    			   for (int x = 0; (x < width); x++, k++)
    				   mask[k] = ( (ip.get(x, y) & 0x00FFFFFF) != 0);
    	   }
    	   else
    		   IJ.error("Mask slice is an image processor bUnwarpJ cannot process");
       }
    } 
  
    /*------------------------------------------------------------------*/
    /**
     * Create mask of specific size (all pixels set to true) 
     *
     * @param width mask width
     * @param height mask height
     */
    public Mask (final int width, final int height)
    {
       this.width  = width;
       this.height = height;
       mask = new boolean[width * height];
       Arrays.fill(mask, true);
    } 
    
    /**
     * Bounding box for the mask.
     * An array is returned with the convention [x0,y0,xF,yF]. This array
     * is returned in corners. This vector should be already resized.
     *
     * @param corners array of coordinates of the bounding box
     */
    public void BoundingBox(int [] corners)
    {
       if (polygon.npoints!=0) {
          Rectangle boundingbox=polygon.getBounds();
          corners[0]=(int)boundingbox.x;
          corners[1]=(int)boundingbox.y;
          corners[2]=corners[0]+(int)boundingbox.width;
          corners[3]=corners[1]+(int)boundingbox.height;
       } else {
          corners[0]=0;
          corners[1]=0;
          corners[2]=width;
          corners[3]=height;
       }
    }

    /*------------------------------------------------------------------*/
    /**
     * Set to true every pixel of the full-size mask.
     */
    public void clearMask ()
    {
       int k = 0;
       for (int y = 0; (y < height); y++)
          for (int x = 0; (x < width); x++)
             mask[k++] = true;
       polygon=new Polygon();
    } /* end clearMask */

    /*------------------------------------------------------------------*/
    /**
     * Fill the mask associated to the mask points.
     *
     * @param tool option to invert or not the mask
     */
    public void fillMask (int tool)
    {
       int k=0;
       for (int y = 0; (y < height); y++)
          for (int x = 0; (x < width); x++) {
             mask[k] = polygon.contains(x,y);
             if (tool==PointAction.INVERTMASK) mask[k]=!mask[k];
             k++;
          }
    }

    /*------------------------------------------------------------------*/
    /**
     * Get the value of the mask at a certain pixel.
     * If the sample is not integer then the closest point is returned.
     *
     * @param x x- coordinate of the pixel
     * @param y y- coordinate of the pixel
     * @return value of the mask at the pixel in (x,y)
     */
    public boolean getValue(double x, double y)
    {
       int u=(int)Math.round(x);
       int v=(int)Math.round(y);
       if (u<0 || u>=width || v<0 || v>=height) return false;
       else                                     return mask[v*width+u];
    }

    /*------------------------------------------------------------------*/
    /**
     * Get a point from the mask.
     *
     * @param i index of the point in the polygong
     * @return corresponding point
     */
    public Point getPoint(int i)
    {
       return new Point(polygon.xpoints[i],polygon.ypoints[i]);
    }

    /*------------------------------------------------------------------*/
    /**
     * Check if the mask was taken from the stack.
     *
     * @return True if the mask was taken from the stack
     */
    public boolean isFromStack()
    {
       return mask_from_the_stack;
    }

    /*------------------------------------------------------------------*/
    /**
     * Get the number of points in the mask.
     *
     * @return number of point of the polygon that composes the mask
     */
    public int numberOfMaskPoints() {return polygon.npoints;}

    /*------------------------------------------------------------------*/
    /**
     * Read mask from file.
     * An error is shown if the file read is not of the same size as the
     * previous mask.
     *
     * @param filename name of the mask file
     */
    public void readFile(String filename)
    {
       ImagePlus aux = new ImagePlus(filename);
       if (aux.getWidth()!=width || aux.getHeight()!=height)
          IJ.error("Mask in file is not of the expected size");
       ImageProcessor ip = aux.getProcessor();
       int k=0;
       for (int y = 0; (y < height); y++)
          for (int x = 0; (x < width); x++, k++)
             if (ip.getPixelValue(x,y)!=0) mask[k]=true;
             else                          mask[k]=false;
    }

    /*------------------------------------------------------------------*/
    /**
     * Show mask.
     */
    public void showMask ()
    {
        double [][]img=new double[height][width];
       int k = 0;
       for (int y = 0; (y < height); y++)
          for (int x = 0; (x < width); x++)
             if (mask[k++]) img[y][x]=1; else img[y][x]=0;
       MiscTools.showImage("Mask",img);
    }

    /*------------------------------------------------------------------*/
    /**
     * Set the mask points.
     *
     * @param listMaskPoints list of points composing the mask
     */
    public void setMaskPoints (final Vector <Point> listMaskPoints)
    {
       int imax=listMaskPoints.size();
       for (int i=0; i<imax; i++)
       {
          Point p=(Point)listMaskPoints.elementAt(i);
          polygon.addPoint(p.x,p.y);
       }
    }

    /*------------------------------------------------------------------*/
    /**
     * Sets the value of the mask at a certain pixel.
     *
     * @param u x- coordinate of the pixel
     * @param v y- coordinate of the pixel
     * @param value mask value to be set
     */
    public void setValue(int u, int v, boolean value)
    {
       if (u>=0 && u<width && v>=0 && v<height) mask[v*width+u]=value;
    }



} /* end class Mask */

