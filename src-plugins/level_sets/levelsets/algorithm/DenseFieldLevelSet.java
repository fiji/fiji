// $Revision$, $Date$, $Author$

package levelsets.algorithm;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author Arne
 */
public class DenseFieldLevelSet implements StagedAlgorithm
{
   private double[][] phi = null;
   private double [][] phi_upd = null;
   private double [][] gradients = null;
   private double [][] extended_gradients = null;
   private byte[][] status = null;
   private BufferedImage inImg = null;
   private BufferedImage img = null;
   private LinkedList<BandElement> contour = new LinkedList();
   //   private Raster inData = null;
   private FastMarching fm = null;
   private boolean needInit = true;
   // preallocate
   int [] pixel = new int[4];
   
   private static byte FRONT = 1;
   private static byte BAND = 2;
   private static byte FAR = 3;
   
   private static double ADVECTION_FORCE = 0.1;
   private static double CURVATURE_EPSILON = 1;
   private static double DELTA_T = 0.1;
   
   /** Creates a new instance of LevelSet */
   public DenseFieldLevelSet(BufferedImage img, FastMarching fm)
   {
      this.img = img;
      this.fm = fm;
      
      phi = new double[img.getWidth()][img.getHeight()];
      phi_upd = new double[img.getWidth()][img.getHeight()];
      gradients = new double[img.getWidth()][img.getHeight()];
      extended_gradients = new double[img.getWidth()][img.getHeight()];
      
      // phi = fm.getDistanceGrid();
      status = new byte[img.getWidth()][img.getHeight()];
      inImg = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
      img.copyData(inImg.getRaster());
      calculateGradients();
   }
   
   
   public boolean step(int granularity)
   {
      if (needInit)
      {
         initPhi();
         createDistanceTransform();
         visualize();
         needInit = false;
      }
      
      for (int i = 0; i < granularity; i++)
      {
         iterate();
         visualize();
      }
      return true;
   }
   
   private void visualize()
   {
      inImg.copyData(img.getRaster());
      //      createImage(gradients);
      createFrontOverlay();
   }
   
   private void iterate()
   {
      for (int x = 0; x < phi.length; x++)
      {
         for (int y = 0; y < phi[0].length; y++)
         {
            update(x, y);
         }
      }
      // flip phi buffers
      double [][] phi_swap = phi;
      phi = phi_upd;
      phi_upd = phi_swap;
   }
   
   private void update(int x, int y)
   {
      double advection = getAdvectionTerm(x, y);
      double curvature = getCurvature(x, y);
      
      phi_upd[x][y] = phi[x][y] - DELTA_T * (advection * ADVECTION_FORCE + curvature * CURVATURE_EPSILON);
   }
   
   // upwind scheme
   private double getAdvectionTerm(int x, int y)
   {
      double xB = (x > 0) ?
         phi[x - 1][y] : Double.MAX_VALUE;
      double xF = (x + 1 < phi.length) ?
         phi[x + 1][y] : Double.MAX_VALUE;
      double yB = (y > 0) ?
         phi[x][y - 1] : Double.MAX_VALUE;
      double yF = (y + 1 < phi[0].length) ?
         phi[x][y + 1] : Double.MAX_VALUE;
      
      double xBdiff = Math.max(phi[x][y] - xB, 0);
      double xFdiff = Math.min(xF - phi[x][y], 0);
      double yBdiff = Math.max(phi[x][y] - yB, 0);
      double yFdiff = Math.min(yF - phi[x][y], 0);
      
      return Math.sqrt(xBdiff * xBdiff + xFdiff * xFdiff +
              yBdiff * yBdiff + yFdiff * yFdiff);
   }
   
   // central differneces
   private double getCurvature(int x, int y)
   {
      if (x == 0 || x >= (phi.length - 1)) return 0;
      if (y == 0 || y >= (phi[0].length - 1)) return 0;
      
      double phiX = (phi[x + 1][y] - phi[x - 1][y]) / 2;
      double phiY = (phi[x][y + 1] - phi[x][y - 1]) / 2;
      double phiXX = (phi[x + 1][y] + phi[x - 1][y] - (2 * phi[x][y]));
      double phiYY = (phi[x][y + 1] + phi[x][y - 1] - (2 * phi[x][y]));
      double phiXY = (phi[x+1][y+1] - phi[x+1][y-1] - phi[x-1][y+1] + phi[x-1][y-1]) / 4;
      
      double curvature = 0, deltaPhi = 0;
      if (phiX != 0 || phiY != 0)
      {
         deltaPhi = Math.sqrt(phiX * phiX + phiY * phiY);
         curvature = -1 * ((phiXX * phiY * phiY) + (phiYY * phiX * phiX)
         - (2 * phiX * phiY * phiXY)) /
                 Math.pow(phiX * phiX + phiY * phiY, 3/2);
      }
      
      return curvature * deltaPhi;
   }
   
   private double getImageTerm()
   {
      return 0;
   }
   
   private void extractContour()
   {
      contour.clear();
      for (int x = 0; x < phi.length - 1; x++)
      {
         for (int y = 0; y < phi[0].length - 1; y++)
         {
            double max = Math.max(Math.max(phi[x][y], phi[x + 1][y]), Math.max(phi[x][y + 1], phi[x + 1][y + 1]));
            double min = Math.min(Math.min(phi[x][y], phi[x + 1][y]), Math.min(phi[x][y + 1], phi[x + 1][y + 1]));
            if (max < 0 || min > 0)
            {
               //writePixel(x, y, 0);
            }
            else
            {
               contour.add(new BandElement(x, y, 0, 0));
               writeContourPixel(x, y);
            }
         }
      }
   }
   
   private void calculateGradients()
   {
      WritableRaster raster = img.getRaster();
      for (int x = 1; x < gradients.length - 2; x++)
      {
         for (int y = 1; y < phi[0].length - 2; y++)
         {
            double xGradient = (raster.getPixel(x + 1, y, pixel)[0] - raster.getPixel(x - 1, y, pixel)[0]) / 2;
            double yGradient = (raster.getPixel(x, y + 1, pixel)[0] - raster.getPixel(x, y - 1, pixel)[0]) / 2;
            gradients[x][y] = Math.sqrt(xGradient * xGradient + yGradient * yGradient);
         }
      }
      
   }
   
   private void writeContourPixel(int x, int y)
   {
      //writePixel(x, y, 255);
      WritableRaster raster = img.getRaster();
      pixel[0] = 255;
      raster.setPixel(x, y, pixel);
   }
   
   private void createFrontOverlay()
   {
      //      inImg.copyData(img.getRaster());
      extractContour();
   }
   
   private void writePixel(int x, int y, double value)
   {
      WritableRaster raster = img.getRaster();
      pixel[0] = pixel[1] = pixel[2] = (int)value;
      raster.setPixel(x, y, pixel);
   }
   
   public BufferedImage createImage(double[][] data)
   {
      int min = Integer.MAX_VALUE;
      int max = Integer.MIN_VALUE;
      int add = 0;
      
      for (int i = 0; i < data.length; i++)
      {
         for (int j = 0; j < data[0].length; j++)
         {
            int current = (int)(Math.abs(data[i][j]));
            if (current < min) min = current;
            if (current > max) max = current;
         }
      }
      
      add = -1 * min;
      
      BufferedImage image = img;
      WritableRaster out = image.getRaster();
      
      int[] pixel = new int[4];
      double scaler = 255d / (max + add - (min + add));
      for (int i = 0; i < image.getWidth(); i++)
      {
         for (int j = 0; j < image.getHeight(); j++)
         {
            int current = (int)Math.round(((int)(Math.abs(data[i][j])) + add) * scaler);
            pixel[0] = pixel[1] = pixel[2] = current;
            out.setPixel(i, j, pixel);
         }
      }
      
      return image;
   }
   
   private void initPhi()
   {
      DeferredByteArray3D statemap = fm.getStateMap();
      for (int x = 0; x < statemap.getXLength(); x++)
      {
         for (int y = 0; y < statemap.getYLength(); y++)
         {
            if (statemap.get(x, y, 0) == FastMarching.BAND)
            {
               phi[x][y] = 0;
               contour.add(new BandElement(x, y, 0, 0));
            }
            else if (statemap.get(x, y, 0) == FastMarching.ALIVE)
            {
               phi[x][y] = -1;
            }
            else
            {
               phi[x][y] = +1;
            }
         }
      }
   }
   
   private void createDistanceTransform()
   {
      DeferredByteArray3D statemap = fm.getStateMap();
      for (int x = 0; x < statemap.getXLength(); x++)
      {
         for (int y = 0; y < statemap.getYLength(); y++)
         {
            if (!(statemap.get(x, y, 0) == FastMarching.BAND))
            {
               double sign = phi[x][y];
               phi[x][y] = Double.MAX_VALUE;
               Iterator<BandElement> it = contour.iterator();
               while (it.hasNext())
               {
                  BandElement current = it.next();
                  double dist =
                          Math.sqrt((x - current.getX()) * (x - current.getX()) +
                          (y - current.getY()) * (y - current.getY()));
                  if (dist < phi[x][y])
                  {
                     phi[x][y] = dist;
                  }
               }
               phi[x][y] = phi[x][y] * sign;
            }
         }
      }
   }
}
