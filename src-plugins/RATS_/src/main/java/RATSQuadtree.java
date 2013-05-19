/**
  * RATSQuadtree
  * This class implements a simple quadtree hierarchy suitable for use navigating 
  * ImageJ's ImagePlus instances. The constructor is designed to be called
  * from "outside" just once, the class will take care of subdividing to the 
  * smallest leaf specified by the MinLeafSize input parameter.
  * 
  * Quadtrees are used in image processing to recursively divide an image into
  * 4-sibling leaflets until a minimum leaflet size is achieved.  That is,
  * the entire image is divided into 4 equal quadrants (aka "leaflets" or 
  * "children").  Each of these are in turn divided into 4 equal quadrants, and 
  * so on until the leafs reach a minimum user-specified size.
  *
  * There are many implementations of quadtrees in Java, but these are mostly
  * tied to mapping projects. This is the only one of we know of for working 
  * intimately with ImageJ.   A good starting point for learning more about 
  * quadtrees is Wikipedia - search "quadtree".
  *
  * @author btupper@bigelow.org for 
  * Michael Sieracki,Bigelow Laboratory for Ocean Sciences
  * http://www.bigelow.org
  * @version 2008-12-11
  *            
*/

import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.*;
import java.util.Vector;

public class RATSQuadtree {

  /** produce informative messages flag*/
  public boolean bverbose = false;
  /** the width and height of the images */
  public int[] dim;
  /** the level of this family */
  public int level=0;   
  /** the minimum allowed size in pixels*/
  public double[] minSzPx = {4.0, 4.0};
  /** the children quadtrees and the parent */
  private RATSQuadtree parent;
  private RATSQuadtree[][] qt = new RATSQuadtree[2][2];
  /** the coords of this level */
  public double x0,y0,x1,y1;
  /** the sum of pixels at this level */
  public float sum = 0.0f;
  
/** This is the entry constructor for the top level leaf
  * @param dim the size of the original image
  * @param minSzPx the minimum allowed leaf size in pixels (used for both x and y)
  */
  public RATSQuadtree(int[] dim, double minSzPx){
  
    this.dim = dim;
    this.minSzPx[0] = minSzPx;
    this.minSzPx[1] = minSzPx;
    this.level = 0;
  //  RATSQuadtree[][] qt = new RATSQuadtree[2][2];

    this.x0 = 0.0;
    this.y0 = 0.0;
    this.x1 = (double) dim[0] - 1.0;
    this.y1 = (double) dim[1] - 1.0;
    this.parent = null;
    
    subdivide(qt);
  }
/** This is the entry constructor for the top level leaf
  * @param dim the size of the original image
  * @param minSzPx the minimum allowed leaf size in pixels [minx, miny]
  */
  public RATSQuadtree(int[] dim, double[] minSzPx){
  
    this.dim = dim;
    this.minSzPx = minSzPx;
    this.level = 0;
  //  RATSQuadtree[][] qt = new RATSQuadtree[2][2];

    this.x0 = 0.0;
    this.y0 = 0.0;
    this.x1 = (double) dim[0] - 1.0;
    this.y1 = (double) dim[1] - 1.0;
    this.parent = null;
    
    subdivide(qt);
  }

/** 
  * This constructor is used by a Quadtree as it creates its children
  *
  * @param dim the size of the original image
  * @param minSzPx the minumum leaf size in pixels
  * @param level the assigned tree level\
  * @param x0 the left coord
  * @param y0 the top coord
  * @param x1 the right coord
  * @param y1 the bottom coord
  * @param theParent reference to the parent quadtree.
  */
  public RATSQuadtree(int[] dim, double[] minSzPx, int level, double x0, double y0, double x1, double y1, RATSQuadtree theParent){
  
    this.parent = theParent;
    this.dim = dim;
    this.minSzPx = minSzPx;
    this.level = level;
    this.x0 = x0;
    this.y0 = y0;
    this.x1 = x1;
    this.y1 = y1;
    
    subdivide(qt);
  
  }

  /**
  * Use this method to determine if the quadtree can be subdivided and to 
  * do it if possible
  * @param qt A 4-element (as 2x2) array of quadtrees
  * @return Returns true if this level can be subdivided
  */ 
  private boolean subdivide(RATSQuadtree[][] qt){
  
    boolean ok = true;
      // width and  height
    double w = (this.x1 - this.x0 + 1);
    double h = (this.y1 - this.y0 + 1);
    
    if ((w/2.0 >= minSzPx[0]) && (h/2.0 >= minSzPx[1])) {
     //IJ.log("qt=" + qt);
     qt[0][0] =  new RATSQuadtree(dim, this.minSzPx, this.level + 1,
        this.x0, this.y0, this.x0 + w/2.0 -1, this.y0 + h/2.0 - 1.0, this);
        // IJ.log("qt[0][0]=" + qt[0][0]);
     qt[0][1] =  new RATSQuadtree(this.dim, this.minSzPx, this.level + 1,
        this.x0 + w/2.0, this.y0, this.x0 + w -1.0, this.y0 + h/2.0 - 1.0, this);
     qt[1][0] =  new RATSQuadtree(this.dim, this.minSzPx, this.level + 1,
        this.x0, this.y0 + h/2.0, this.x0 + w/2.0-1.0, this.y0 + h -1.0, this);
     qt[1][1] =  new RATSQuadtree(this.dim, this.minSzPx, this.level + 1,
        this.x0 + w/2.0, this.y0 + h/2.0, this.x0 + w -1.0,this.y0 + h -1.0, this);
    } else {    
      ok = false;
    }
  
    return ok;
  }//subdivide
  
/**
  * Fills the sum values.  This method is for the top level.
  * It manually fills the lowest level.  The recursively fills
  * each level above the lowest.
  */  
  public void fillWithSums(ImageProcessor ip){
   // this will only run for the top leaf
   // step 1. fill the lowest levels
   // step 2. recursively fill the levels above the bottom 
    if (this.level == 0){
      RATSQuadtree[][] q; 
      q = getLevel(countLevels()-1);      
      for (int x = 0; x < q.length; x++){
        for (int y = 0; y < q[0].length; y++){
            q[x][y].sum = q[x][y].getSumFloat(ip);
        }//y
      }//x
      
      // now fill each level above
      for (int iL = (countLevels()-2); iL > (-1); iL--){
        q = getLevel(iL);
        for (int x = 0; x < q.length; x++){
          for (int y = 0; y < q[0].length; y++){
           q[x][y].sumChildren();
          }//y
        }//x
      }//iL
    } //level ==0
  }//fillWithSums
  
/**
  * Sums from the children for all but the lowest level
  */
  private void sumChildren(){
    if (qt[0] != null){
      this.sum = 
        qt[0][0].sum + 
        qt[0][1].sum +
        qt[1][0].sum +
        qt[1][1].sum;
    }
  }
/**
  * Returns the dimensions of the leaflet as integer
  *
  * @return a vector of [width, height]
  */
  public int[] getDimInteger(){
    int[] c = getCoordsInteger();
    int[] dim = {c[2]-c[0] + 1, c[3]-c[1] + 1};
    return dim; 
  }

/**
  * Returns the dimensions of the leaflet as double
  *
  * @return a vector of [width, height]
  */
  public double[] getDimDouble(){
    double[] c = getCoordsDouble();
    double[] dim = {c[2]-c[0] + 1.0, c[3]-c[1] + 1.0};
    return dim; 
  }
    
    
/**
  * Returns the center coordinates as [x,y]
  *
  */
  public double[] getCenter(){
    double[] c = getCoordsDouble();
    double[] d = getDimDouble();
    double[] r = {c[0] + d[0]/2.0, c[1] + d[1]/2.0};
    return r;
  }
  
/** 
  * Returns the coordinates rounded to integer [x0, y0, x1, y1]
  * 
  * @return a four element array of the coords rounded to the nearest integer
  */
  public int[] getCoordsInteger(){
    int[] c = {(int) Math.round(x0), (int) Math.round(y0), 
      (int) Math.round(x1), (int) Math.round(y1)};
    return c;
  }
  
/** 
  * Returns the coordinates rounded to integer [x0, y0, x1, y1]
  * 
  * @return a four element array of the coords rounded to the nearest integer
  */
  public double[] getCoordsDouble(){
    double[] c = {x0, y0, x1, y1};
    return c;
  }

/** Returns the two letter quadrant code for the provided child as (NW, NE, SW, SE, or NA)
  * @param the RATSQuadtree child
  * @return a two letter code indicating the child's quadtrant or NA 
  *  if this child doesn't belong.
  */
  public String getChildName(RATSQuadtree qc){
    if (qt[0][0] == qc) return "NW";
    if (qt[0][1] == qc) return "NE";
    if (qt[1][0] == qc) return "SW";
    if (qt[1][1] == qc) return "SE";
    return "NA"; 
  }
  
  
/** Returns the two letter quadrant code for the quadtree
  * @return the two letter code indicating the child's position (NW,NE, SW or SE)
  *   if this is the top then "NA" is returned. 
  */
  public String getName(){
    return (parent == null) ? "NA" : parent.getChildName(this); 
  }   
/** Returns the level of this quadtree
  *
  * @return the level of this leaf
  */
  public int getLevel(){
    return level;
  }

/**
  * Returns true if this leaf has children
  */ 
  public boolean hasChildren(){
    return (qt[0][0] != null); 
  }
/**
  * Returns the children of this leaf
  */
  public RATSQuadtree[][] getChildren(){
    return qt;
  }

/**
  * Returns the parent of this leaf
  */
  public RATSQuadtree getParent(){
    return parent;
  }

/**
  * Returns the siblings of this leaf
  * @return A 2x2 array of quadtrees or null if no siblings
  */ 
  public RATSQuadtree[][] getSiblings(){
    if (parent == null) {
      return (null);
    } else {
      return parent.getChildren();
    }
  }
  
/**
  * Returns an array of leaflets in [nw,ne,sw,se] order
  * @param theLevel the zero-based index of the level to retrieve
  * @return A nxn array of quadtrees at the specified level
  */
  public RATSQuadtree[][] getLevel(int theLevel){
  
    //this level is requested
    if (theLevel == this.level){
      RATSQuadtree[][] self = new RATSQuadtree[1][1];
      self[0][0] = this; 
      return self;
    } 
    //the child level is requested
    if (theLevel == (this.level + 1)){ 
      return getChildren();
    }
    
    if (theLevel < this.level) {
      IJ.showMessage("HBB_QUATREE",
       "Unable to retrieve a level higher than my own: " + this.level);
    return new RATSQuadtree[0][0];
    }
      
    RATSQuadtree[][] qnw = qt[0][0].getLevel(theLevel);
    RATSQuadtree[][] qne = qt[0][1].getLevel(theLevel);
    RATSQuadtree[][] qsw = qt[1][0].getLevel(theLevel);
    RATSQuadtree[][] qse = qt[1][1].getLevel(theLevel);
        
    int ny = qnw.length;
    int nx = qnw[0].length;
    //IJ.log("Level= " + level + ", NX = "+nx + ", NY = " + ny);
    RATSQuadtree[][] q = new RATSQuadtree[ny*2][nx*2];
    for (int v = 0; v < ny; v++){
      for (int u = 0; u < nx; u++){
        //for each of the qnw, qne, ... populate q
        //IJ.log("v = " + v + ", u = " + u);
        q[v][u] = qnw[v][u];
        q[v][u+nx] = qne[v][u];
        q[v+ny][u] = qsw[v][u];
        q[v+ny][u+nx] = qse[v][u];
      }//u loop
    }//v loop
    
    return q;    
  }
  
/**
  * Returns the sum of the pixels enclosed in this leaflet
  * @param imp The ImagePlus image of pixels
  * @return the sum of the pixels
  */
  public float getSumFloat(ImagePlus imp){
    return getSumFloat(imp.getProcessor());
  }

/**
  * Returns the sum of the pixels enclosed in this leaflet
  * @param imp The imageProcessor of pixels
  * @return the sum of the pixels
  */
  public float getSumFloat(ImageProcessor ip){
//    float[] r = getPixelsFloat(ip);
//    sum = 0.0f;
//    for (int i = 0; i < r.length; i++){sum += r[i];}
//    return sum;
    int[] xy = getCoordsInteger();
    int[] wh = getDimInteger();
    //float[][] pixels = ip.getFloatArray();
    sum = 0.0f;
    for (int j = 0; j < wh[1] ; j++){
      for (int i = 0; i < wh[0]; i++){   
        //r[j*wh[0] + i] = pixels[i + xy[0]][j + xy[1]];
        sum += ip.getPixelValue(i + xy[0], j + xy[1]);    
      }//i loop
    }//jloop
  
    return sum;
  }

/**
  * Returns the sum (which may not have been assigned, so use this after 
  * filling the leaves with the sums.  See fillWithSums()
  */
    public float getSumFloat(){
    return sum;
  }
/**
  * Returns a 2d array of the sums for the level specified
  * The is really for the top level leaf 
  * @param theLevel the zero-based index level
  * @return a 2d float array of the float values
  */
  public float[][] getSumArrayFloat(int theLevel, ImagePlus imp){
    return getSumArrayFloat( theLevel, imp.getProcessor());
  } 
  
/**
  * Returns a 2d array of the sums for the level specified
  * The is really for the top level leaf 
  * @param theLevel the zero-based index level
  * @return a 2d float array of the float values
  */
  public float[][] getSumArrayFloat(int theLevel, ImageProcessor ip){
  
    float[][] arr = makeArrayFloat(theLevel);
    int x, y;
    RATSQuadtree[][] q = getLevel(theLevel);
    int[] wh = q[0][0].getDimInteger();
    double[] c;
    
    for (int v = 0; v<q.length; v++){
      for (int u = 0; u < q[v].length; u++){
        c = q[v][u].getCenter();
        x = ((int) c[0]) / wh[0];
        y = ((int) c[1]) / wh[1];
      
        arr[v][u] = q[v][u].getSumFloat(ip);
      } //u loop
    }//v loop
  
    return arr;
  } 

/**
  * This method returns the pixels enclosed in this leaflet.
  * @param ip the ImagePlus with the pixels
  * @return an 1d array of single precision values
  */
  public float[] getPixelsFloat(ImagePlus imp){
    return getPixelsFloat(imp.getProcessor());
  }//getPixelsFloat
  
/**
  * This method returns the pixels enclosed in this leaflet.
  * @param ip the ImageProcessor with the pixels
  * @return an 1d array of single precision values
  */
  public float[] getPixelsFloat(ImageProcessor ip){
  
    int[] xy = getCoordsInteger();
    int[] wh = getDimInteger();
    //float[][] pixels = ip.getFloatArray();
    float[] r = new float[wh[0] * wh[1]];
    for (int j = 0; j < wh[1] ; j++){
      for (int i = 0; i < wh[0]; i++){   
        //r[j*wh[0] + i] = pixels[i + xy[0]][j + xy[1]];
        r[j*wh[0] + i] = ip.getPixelValue(i + xy[0], j + xy[1]);    
      }//i loop
    }//jloop
  
    return r;
  }//getPixelsFloat


/**
  * This methods returns the number of leaflets for a given level
  * @param level The zero based level index
  * @return The number of leaflets at that level
  */
  public int countLeaflets( int level){
    return (int) Math.pow(4, level);
  }
/**
  * This method returns the number of subsequent generations plus itself.
  * 
  * @return the "depth" of the qaudtree which is 1-based
  */
  public int countLevels(){
  
    int lvl = 1;
    //IJ.log("QNW=" + qt);
    if (qt[0][0] != null) {
      lvl = lvl + qt[0][0].countLevels();
    } 
    return lvl;
  }
  
/**
  * This method draws the rectangle
  * @param imp the ImagePlus on to which to draw the bounds
  */
  public void drawBounds(ImagePlus imp){
      double w = x1-x0+1;
      double h = y1-y0+1;
      boolean isLocked = imp.isLocked();
      
      if (isLocked == true) {imp.unlock();}
      
      IJ.run("Specify...", 
        "width="+ Math.round(w)+
        " height="+ Math.round(h)+
        " x="+ Math.round(x0)+ 
        " y="+ Math.round(y0));   
      IJ.run("Draw");
      
      if (isLocked == true) {imp.lock();}
      
      if (bverbose){
        IJ.log("w="+w+" h="+h+" x="+ x0+" y="+y0);
      }
  }
  
/**
  * This method will draw the center of the box
  * @param imp the ImagePlus onto which to draw the center
  */
  public void drawCenter(ImagePlus imp){
  
    double c[] = getCenter();

    boolean isLocked = imp.isLocked();
    if (isLocked == true) {imp.unlock();} 
  
    IJ.setTool(Toolbar.POINT);
    IJ.makePoint( (int) Math.round(c[0]), (int) Math.round(c[1]));
    IJ.run("Draw");
    
    if (isLocked == true) {imp.lock();}

  }
  
/**
  * This method will draw the box for the leaf
  * 
  * @param drawLevel the depth of leaves to draw
  * @param imp the ImagePlus onto which to draw
  */
  public void drawBounds(int drawLevel, ImagePlus imp){
  
    if (this.level == drawLevel) {
      drawBounds(imp);
    } else {
      if (qt[0][0] != null) {
        qt[0][0].drawBounds(drawLevel, imp);
        qt[0][1].drawBounds(drawLevel, imp);
        qt[1][0].drawBounds(drawLevel, imp);
        qt[1][1].drawBounds(drawLevel, imp);
      }
    }//next level down
  }

/**
  * This method creates a 2d array for the level specified.
  * @param the level to create an array for.
  */
  public float[][] makeArrayFloat(int theLevel){
    int n = (int) Math.pow(2.0, (double) theLevel);
    float[][] arr = new float[n][n];
    return arr;
  } 
  
/**
  * Returns a two element array of [width,height]
  * @param arr The nxn array of quadtrees
  * @return a two element array of [width,height]
  */
  public int[] getArrayDim(RATSQuadtree[][] arr){
    int[] dim = new int[2];
    dim[1] = arr.length;
    dim[0] = arr[0].length;
    return dim;
  }
  
/**
  * Returns a two element array of [width,height]
  * @arr the nxm element array of floats
  * @return a two element array of [width,height]
  */
  public int[] getArrayDim(float[][] arr){
    int[] dim = new int[2];
    dim[1] = arr.length;
    dim[0] = arr[0].length;
    return dim;
  }
  
/**
  * Returns a two element array of [width,height] for the given input array.
  * @param a nxm array
  * @return a two element array of [width,height] for the given input array.
  */
  public int[] getDim(Object[][] arr){
    int[] dim = new int[2];
    dim[1] = arr.length;
    dim[0] = arr[0].length;
    return dim;
  }    
  
}
