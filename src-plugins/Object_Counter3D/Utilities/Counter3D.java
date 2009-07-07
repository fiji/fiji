/*
 * Counter3D.java
 *
 * Created on 7 novembre 2007, 11:54
 *
 * Copyright (C) 2007 Fabrice P. Cordelières
 *  
 * License:
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 
 */

package Utilities;

import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.process.*;

import java.awt.*;
import java.util.*;

/**
 *
 * @author Fabrice P. Cordelières, fabrice.cordelieres@gmail.com
 * @version 1.0, 7/11/07
 */
public class Counter3D {
    int thr=0;
    boolean[] isSurf;
    int width=1, height=1, nbSlices=1, length=1, depth=8;
    Calibration cal;
    String title="img";
    int minSize, maxSize, nbObj=0, nbSurfPix=0;
    int[] imgArray, objID, IDcount, surfList;
    boolean[] IDisAtEdge;
    int[][] surfCoord;
    float[][] centreOfMass, centroid;
    boolean sizeFilter=true, exclude=false, redirect=false, closeImg=Prefs.get("3D-OC-Options_closeImg.boolean", false), showMaskedImg=Prefs.get("3D-OC-Options_showMaskedImg.boolean", true);
    Object3D[] obj;
    
    boolean foundObjects=false, getObjects=false, getCentreOfMass=false, getCentroid=false, getSurfList=false, getSurfCoord=false;
    
    
    /**
     * Creates a new instance of Counter3D.
     *
     * @param img specifies the image to convert into an Counter3D.
     * @param thr specifies the threshold value (should be an Integer).
     * @param min specifies the min size threshold to be used (should be an Integer).
     * @param max specifies the max size threshold to be used (should be an Integer).
     * @param exclude specifies if the objects on the edges should be excluded (should be a boolean).
     * @param redirect specifies if intensities measurements should be redirected to another image defined within the options window (should be a boolean).
     */
     public Counter3D(ImagePlus img, int thr, int min, int max, boolean exclude, boolean redirect) {
        this.width=img.getWidth();
        this.height=img.getHeight();
        this.nbSlices=img.getNSlices();
        this.length=this.width*this.height*this.nbSlices;
        this.depth=img.getBitDepth();
        this.title=img.getTitle();
        this.cal=img.getCalibration();
        this.thr=thr;
        this.minSize=min;
        this.maxSize=max;
        this.sizeFilter=true;
        this.exclude=exclude;
        this.redirect=redirect;
        
        if (depth!=8 && depth!=16) throw new IllegalArgumentException("Counter3D class expects 8- or 16-bits images only");
         
        this.nbObj=this.length;
        
        this.imgArray=new int[this.length];
        
        this.imgArrayModifier(img);
        
    }
    
    /**
     * Creates a new instance of Counter3D.
     *
     * @param img specifies the image to convert into an Counter3D.
     * @param thr specifies the threshold value (should be an Integer).
     */
    public Counter3D(ImagePlus img, int thr) {
        this(img, thr, 1, img.getWidth()*img.getHeight()*img.getNSlices(), false, false);
        this.sizeFilter=false;
    }
    
    /**
     * Creates a new instance of Counter3D
     *
     * @param img specifies the image to convert into an Counter3D.
     */
    public Counter3D(ImagePlus img) {
        this(img, 0, 1, img.getWidth()*img.getHeight()*img.getNSlices(), false, false);
        this.sizeFilter=false;
    }
    
    /**
     * Creates a new instance of Counter3D.
     *
     * @param img specifies the array containing the image data.
     * @param title specifies the image title.
     * @param width specifies the image width.
     * @param height specifies the image height.
     * @param nbSlices specifies the image numbre of slices.
     * @param thr specifies the threshold value (should be an Integer).
     * @param min specifies the min size threshold to be used (should be an Integer).
     * @param max specifies the max size threshold to be used (should be an Integer).
     * @param exclude specifies if the objects on the edges should be excluded (should be a boolean).
     * @param redirect specifies if intensities measurements should be redirected to another image defined within the options window (should be a boolean).
     * @param cal specifies the image calibration to be used.
     */
    public Counter3D(int[] img, String title, int width, int height, int nbSlices, int thr, int min, int max, boolean exclude, boolean redirect, Calibration cal) {
        this.title=title;
        this.width=width;
        this.height=height;
        this.nbSlices=nbSlices;
        this.length=this.width*this.height*this.nbSlices;
        if (this.length!=img.length) throw new IllegalArgumentException("The image array length differs from the given image dimensions");
        this.title=title;
        this.cal=cal;
        
        this.thr=thr;
        this.minSize=min;
        this.maxSize=max;
        this.sizeFilter=true;
        this.exclude=exclude;
        this.redirect=redirect;
        
        this.nbObj=this.length;
        
        this.imgArray=img;
                
        this.imgArrayModifier();
    }
    
    /**
     * Creates a new instance of Counter3D.
     *
     * @param img specifies the array containing the image data.
     * @param title specifies the image title.
     * @param width specifies the image width.
     * @param height specifies the image height.
     * @param nbSlices specifies the image number of slices.
     * @param thr specifies the threshold value (should be an Integer).
     * @param cal specifies the image calibration to be used.
     */
    public Counter3D(int[] img, String title, int width, int height, int nbSlices, int thr, Calibration cal) {
        this(img, title, width, height, nbSlices, thr, 1, width*height*nbSlices, false, false, cal);
        this.sizeFilter=false;
    }
    
    
    /**
     * Creates a new instance of Counter3D.
     *
     * @param img specifies the array containing the image data.
     * @param title specifies the image title.
     * @param width specifies the image width.
     * @param height specifies the image height.
     * @param nbSlices specifies the image number of slices.
     * @param thr specifies the threshold value (should be an Integer).
     */
    public Counter3D(int[] img, String title, int width, int height, int nbSlices, int thr) {
        this(img, title, width, height, nbSlices, thr, 1, width*height*nbSlices, false, false, new Calibration());
        this.sizeFilter=false;
    }
    
    /**
     * Creates a new instance of Counter3D.
     *
     * @param img specifies the array containing the image data.
     * @param title specifies the image title.
     * @param width specifies the image width.
     * @param height specifies the image height.
     * @param nbSlices specifies the image number of slices.
     * @param cal specifies the image calibration to be used.
     */
    public Counter3D(int[] img, String title, int width, int height, int nbSlices, Calibration cal) {
        this(img, title, width, height, nbSlices, 0, 1, width*height*nbSlices, false, false, cal);
        this.sizeFilter=false;
    }
    
    /**
     * Creates a new instance of Counter3D.
     *
     * @param img specifies the array containing the image data.
     * @param title specifies the image title.
     * @param width specifies the image width.
     * @param height specifies the image height.
     * @param nbSlices specifies the image number of slices.
     */
    public Counter3D(int[] img, String title, int width, int height, int nbSlices) {
        this(img, title, width, height, nbSlices, 0, 1, width*height*nbSlices, false, false, new Calibration());
        this.sizeFilter=false;
    }
    
    /** Generates the connexity analysis.
     */
    private void findObjects() {
        //First ID attribution
        int currID=0;
        int currPos=0;
        int minID=0;
        int surfPix=0;
        int neigbNb=0;
        int pos, currPixID;
        int neigbX, neigbY, neigbZ;
        
        long start=System.currentTimeMillis();
        /*
         Finding the structures:
         *The minID tag is initialized with the current value of tag (currID).If thresholded,
         *the neighborhood of the current pixel is collected. For each of those 13 pixels,
         *the value is retrieved and tested against minID: only the minimum of the two is kept.
         *As anterior pixels have already been tagged, only two possibilities may exists:
         *1-The minimum is currID: we start a new structure and currID should be incremented
         *2-The minimum is not currID: we continue an already existing structure
         *Each time a new pixel is tagged, a counter of pixels in the current tag is incremented.
         */
                
        this.objID=new int[this.length];
        
        for (int z=1; z<=this.nbSlices; z++){
            for (int y=0; y<this.height; y++){
                for (int x=0; x<this.width; x++){
                    if (minID==currID) currID++;
                    if (this.imgArray[currPos]!=0){
                        minID=currID;
                        minID=minAntTag(minID, x, y, z);
                        this.objID[currPos]=minID;
                    }
                    currPos++;
                }
            }
            //IJ.showStatus("Finding structures "+z*100/this.nbSlices+"%");
            IJ.showStatus("Step 1/3: Finding structures");
            IJ.showProgress(z, this.nbSlices);
        }
        IJ.showStatus("");
        
        this.IDcount=new int[currID];
        for (int i=0; i<this.length; i++) this.IDcount[this.objID[i]]++;
        
        this.IDisAtEdge=new boolean[currID];
        Arrays.fill(this.IDisAtEdge, false);
        /*
         *Connecting structures:
         *The first tagging of structure may have led to shearing apart pieces of a same structure
         *This part will connect them back by attributing the minimal retrieved tag among the 13 neighboring
         *pixels located prior to the current pixel + the centre pixel and will replace all the values of those pixels
         *by the minimum value.
         */
        this.isSurf=new boolean[this.length];
        currPos=0;
        minID=1;
                
        for (int z=1; z<=this.nbSlices; z++){
            for (int y=0; y<this.height; y++){
                for (int x=0; x<this.width; x++){
                    if (this.imgArray[currPos]!=0){
                        minID=this.objID[currPos];
                        surfPix=0;
                        neigbNb=0;
                        //Find the minimum tag in the neighbours pixels
                        for (neigbZ=z-1; neigbZ<=z+1; neigbZ++){
                            for (neigbY=y-1; neigbY<=y+1; neigbY++){
                                for (neigbX=x-1; neigbX<=x+1; neigbX++){
                                    //Following line is important otherwise objects might be linked from one side of the stack to the other !!!
                                    if (neigbX>=0 && neigbX<this.width && neigbY>=0 && neigbY<this.height && neigbZ>=1 && neigbZ<=this.nbSlices){
                                        pos=offset(neigbX, neigbY, neigbZ);
                                        if (this.imgArray[pos]!=0){
                                            if ((this.nbSlices>1 && ((neigbX==x && neigbY==y && neigbZ==z-1) ||(neigbX==x && neigbY==y && neigbZ==z+1))) ||(neigbX==x && neigbY==y-1 && neigbZ==z) ||(neigbX==x && neigbY==y+1 && neigbZ==z) ||(neigbX==x-1 && neigbY==y && neigbZ==z) ||(neigbX==x+1 && neigbY==y && neigbZ==z)) surfPix++;
                                            minID=Math.min(minID, this.objID[pos]);
                                        }
                                        neigbNb++;
                                    }
                                }
                            }
                        }
                        if ((surfPix!=6 && this.nbSlices>1) || (surfPix!=4 && this.nbSlices==1)){
                            this.isSurf[currPos]=true;
                            this.nbSurfPix++;
                        }else{
                            this.isSurf[currPos]=false;
                        }
                        //Replacing tag by the minimum tag found
                        for (neigbZ=z-1; neigbZ<=z+1; neigbZ++){
                            for (neigbY=y-1; neigbY<=y+1; neigbY++){
                                for (neigbX=x-1; neigbX<=x+1; neigbX++){
                                    //Following line is important otherwise objects might be linked from one side of the stack to the other !!!
                                    if (neigbX>=0 && neigbX<this.width && neigbY>=0 && neigbY<this.height && neigbZ>=1 && neigbZ<=this.nbSlices){
                                        pos=offset(neigbX, neigbY, neigbZ);
                                        if (this.imgArray[pos]!=0){
                                            currPixID=this.objID[pos];
                                            if (currPixID>minID) replaceID(currPixID, minID);
                                        }
                                    }
                                }
                            }
                        }
                        
                        //Check if the current particle is touching an edge
                        if(x==0 || y==0 || x==this.width-1 || y==this.height-1 || (this.nbSlices!=1 && (z==1 || z==this.nbSlices))) this.IDisAtEdge[minID]=true;
                    }
                    currPos++;
                }
            }
            IJ.showStatus("Step 2/3: Connecting structures");
            IJ.showProgress(z, this.nbSlices);
        }
        IJ.showStatus("");
        
        int newCurrID=0;
        
        //Renumbering of all the found objects and update of their respective number of pixels while filtering based on the number of pixels
        for (int i=1; i<this.IDcount.length; i++){
            if ((this.IDcount[i]!=0 && this.IDcount[i]>=this.minSize && this.IDcount[i]<=this.maxSize)&& (!this.exclude || !(this.exclude && this.IDisAtEdge[i]))){
                newCurrID++;
                int nbPix=this.IDcount[i];
                replaceID(i, newCurrID);
                this.IDcount[newCurrID]=nbPix;
            }else{
                replaceID(i,0);
            }
            IJ.showStatus("Step 3/3: Renumbering structures");
            IJ.showProgress(i, currID);
        }
        IJ.showStatus("");
        
        if (this.redirect) this.prepareImgArrayForRedirect();
        if (this.showMaskedImg) this.buildImg(this.imgArray, null, "Masked image for "+this.title, false, false, false, 0, 0).show();
        
        this.nbObj=newCurrID;
        this.foundObjects=true;
            
        this.getObjects();
    }
    
    /** Generates the objects list.
     */
    private void getObjects(){
        if (!this.foundObjects) this.findObjects();
        
        if (!this.getObjects){
            this.obj=new Object3D[this.nbObj];

            for (int i=0; i<this.nbObj; i++) this.obj[i]=new Object3D(this.IDcount[i+1], this.cal);
            this.IDcount=null;
            
            int currPos=0;
            for (int z=1; z<=this.nbSlices; z++){
                for (int y=0; y<this.height; y++){
                    for (int x=0; x<this.width; x++){
                        int currID=this.objID[currPos];
                        if (currID!=0){
                            float surf=0;
                            if (this.nbSlices==1) surf=(float) (this.cal.pixelWidth*this.cal.pixelHeight);
                            if (this.isSurf[currPos] && this.nbSlices>1){
                                surf=(float) (2*(this.cal.pixelHeight*this.cal.pixelDepth+this.cal.pixelWidth*this.cal.pixelDepth+this.cal.pixelWidth*this.cal.pixelHeight));
                                //Look at the 6 exposed surfaces
                                if (x>0 && this.objID[offset (x-1, y, z)]==currID) surf-=this.cal.pixelHeight*this.cal.pixelDepth;
                                if (x<this.width-1 && this.objID[offset (x+1, y, z)]==currID) surf-=this.cal.pixelHeight*this.cal.pixelDepth;
                                if (y>0 && this.objID[offset (x, y-1, z)]==currID) surf-=this.cal.pixelWidth*this.cal.pixelDepth;
                                if (y<this.height-1 && this.objID[offset (x, y+1, z)]==currID) surf-=this.cal.pixelWidth*this.cal.pixelDepth;
                                if (z>1 && this.objID[offset (x, y, z-1)]==currID) surf-=this.cal.pixelWidth*this.cal.pixelHeight;
                                if (z<=this.nbSlices-1 && this.objID[offset (x, y, z+1)]==currID) surf-=this.cal.pixelWidth*this.cal.pixelHeight;
                            }
                            this.obj[currID-1].addVoxel(x, y, z, this.imgArray[currPos], this.isSurf[currPos], surf);
                        }
                        currPos++;
                    }
                }
            }
            this.imgArray=null;
            System.gc();
        }
        this.getObjects=true;
    }
    
    /**
     * Returns the list of all found objects.
     *
     * @return the list of all found objects as a Object3D array.
     */
    public Object3D[] getObjectsList(){
        if (!this.getObjects) this.getObjects();
        return this.obj;
    }
    
    /**
     * Returns the objects map.
     * @param drawNb should be true if numbers have to be drawn at each coordinate stored in cenArray (boolean).
     * @param fontSize font size of the numbers to be shown (integer).* @return an ImagePlus containing all found objects, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getObjMap(boolean drawNb, int fontSize){
        if (!this.getObjects) this.getObjects();
        if (!this.getCentroid) this.populateCentroid();
        return buildImg(this.objID, coord2imgArray(this.centroid), "Objects map of "+this.title, false, drawNb, true, 0, fontSize);
    }
    
    /**
     * Returns the objects map.
     *
     * @return an ImagePlus containing all found objects, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getObjMap(){
        if (!this.getObjects) this.getObjects();
        return buildImg(this.objID, null, "Objects map of "+this.title, false, false, true, 0, 0);
    }
    
    /** Generates and fills the "centreOfMass" array.
     */
    private void populateCentreOfMass(){
        if (!this.getObjects) this.getObjects();
        this.centreOfMass=new float[this.obj.length][3];
        
        for (int i=0; i<this.obj.length; i++){
            Object3D currObj=this.obj[i];
            float [] tmp=currObj.c_mass;
            for (int j=0; j<3; j++) this.centreOfMass[i][j]=tmp[j];
        }
        this.getCentreOfMass=true;
    }
    
    /**
     * Returns the centres of masses' list.
     *
     * @return the coordinates of all found centres of masses as a dual float array ([ID][0:x, 1:y, 2:z]).
     */
    public float[][] getCentreOfMassList(){
        if (!this.getCentreOfMass) this.populateCentreOfMass();
        return this.centreOfMass;
    }
    
     /**
     * Returns the centres of masses' map.
     * @param drawNb should be true if numbers have to be drawn at each coordinate stored in cenArray (boolean).
     * @param whiteNb should be true if numbers have to appear white  (boolean).
     * @param dotSize size of the dots to be drawn (integer).
     * @param fontSize font size of the numbers to be shown (integer).* @return an ImagePlus containing all centres of masses, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getCentreOfMassMap(boolean drawNb, boolean whiteNb, int dotSize, int fontSize){
        if (!this.getCentreOfMass) this.populateCentreOfMass();
        int[] array=coord2imgArray(this.centreOfMass);
        return buildImg(array, array, "Centres of mass map of "+this.title, true, drawNb, whiteNb, dotSize, fontSize);
    }
    
    /**
     * Returns the centres of masses' map.
     *
     * @return an ImagePlus containing all centres of masses, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getCentreOfMassMap(){
        if (!this.getCentreOfMass) this.populateCentreOfMass();
        int[] array=coord2imgArray(this.centreOfMass);
        return buildImg(array, null, "Centres of mass map of "+this.title, true, false, false, 5, 0);
    }
     
    /** Generates and fills the "centroid" array.
     */
    private void populateCentroid(){
        if (!this.getObjects) this.getObjects();
        this.centroid=new float[this.obj.length][3];
        
        for (int i=0; i<this.obj.length; i++){
            Object3D currObj=this.obj[i];
            float [] tmp=currObj.centroid;
            for (int j=0; j<3; j++) this.centroid[i][j]=tmp[j];
        }
        this.getCentroid=true;
    }
    
    /**
     * Returns the centroïds' list.
     *
     * @return the coordinates of all found centroïds as a dual float array ([ID][0:x, 1:y, 2:z]).
     */
    public float[][] getCentroidList(){
        if (!this.getCentroid) this.populateCentroid();
        return this.centroid;
    }
    
    /**
     * Returns the centroïds' map.
     * @param drawNb should be true if numbers have to be drawn at each coordinate stored in cenArray (boolean).
     * @param whiteNb should be true if numbers have to appear white  (boolean).
     * @param dotSize size of the dots to be drawn (integer).
     * @param fontSize font size of the numbers to be shown (integer).* @return an ImagePlus containing all centroïds, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getCentroidMap(boolean drawNb, boolean whiteNb, int dotSize, int fontSize){
        if (!this.getCentroid) this.populateCentroid();
        int[] array=coord2imgArray(this.centroid);
        return buildImg(array, array, "Centroids map of "+this.title, true, drawNb, whiteNb, dotSize, fontSize);
    }
    
    /**
     * Returns the centroïds' map.
     *
     * @return an ImagePlus containing all centroïds, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getCentroidMap(){
        if (!this.getCentroid) this.populateCentroid();
        int[] array=coord2imgArray(this.centroid);
        return buildImg(array, null, "Centroids map of "+this.title, true, false, false, 5, 0);
    }
    
    /** Generates and fills the "surface" array.
     */
    private void populateSurfList(){
        if (!this.getObjects) this.getObjects();
        
        this.surfList=new int[this.length];
        for (int i=0; i<this.length; i++) this.surfList[i]=this.isSurf[i]?this.objID[i]:0;
        this.getSurfList=true;
    }
    
    /**
     * Returns the surface pixels' list.
     *
     * @return the coordinates of all pixels found at the surface of objects as a mono-dimensional integer array.
     */
    public int[] getSurfPixList(){
        if (!this.getSurfList) this.populateSurfList();
        return this.surfList;
    }
    
    /** Generates and fills the "surfArray" array.
     */
    private void populateSurfPixCoord(){
        int index=0;
        
        this.surfCoord=new int[this.nbSurfPix][4];
        
        for (int i=0; i<this.nbObj; i++){
            Object3D currObj=this.obj[i];
            for (int j=0; j<currObj.surf_size; j++){
                this.surfCoord[index][0]=i+1;
                for (int k=1; k<4; k++) this.surfCoord[index][k]=currObj.obj_voxels[j][k-1];
                index++;
            }
        }
    }
    
    /**
     * Returns the surface pixels coordinates' list.
     *
     * @return the coordinates of all pixels found at the surface of objects as a dual integer array([index][0:x, 1:y, 2:z, 3:ID]).
     */
    public int[][] getSurfPixCoord(){
        if (!this.getSurfCoord) this.populateSurfPixCoord();
        return this.surfCoord;
    }
    
    /**
     * Returns the surface pixels' map.
     * @param drawNb should be true if numbers have to be drawn at each coordinate stored in cenArray (boolean).
     * @param whiteNb should be true if numbers have to appear white  (boolean).
     * @param fontSize font size of the numbers to be shown (integer).* @return an ImagePlus containing all pixels found at the surface of objects, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getSurfPixMap(boolean drawNb, boolean whiteNb, int fontSize){
        if (!this.getSurfList) this.populateSurfList();
        if (!this.getCentroid) this.populateCentroid();
        return buildImg(this.surfList, coord2imgArray(this.centroid), "Surface map of "+this.title, false, drawNb, whiteNb, 0, fontSize);
    }
    
    /**
     * Returns the surface pixels' map.
     *
     * @return an ImagePlus containing all pixels found at the surface of objects, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getSurfPixMap(){
        if (!this.getSurfList) this.populateSurfList();
        return buildImg(this.surfList, null, "Surface map of "+this.title, false, false, false, 0, 0);
    }
           
    /** Transforms a coordinates array ([ID][0:x, 1:y, 3:z]) to a linear array containing all pixels one next to the other.
     *
     *@return the linear array as an integer array.
     */
    private int[] coord2imgArray(float[][] coord){
        int[] array=new int[this.length];
        for (int i=0; i<coord.length; i++)array[offset((int) coord[i][0], (int) coord[i][1], (int) coord[i][2])]=i+1;
        return array;
    }
    
    /** Set to zero pixels below the threshold in the "imgArray" arrays.
     */
    private void imgArrayModifier(ImagePlus img){
        int index=0;
        for (int i=1; i<=this.nbSlices; i++){
            img.setSlice(i);
            for (int j=0; j<this.height; j++){
                for (int k=0; k<this.width; k++){
                    this.imgArray[index]=img.getProcessor().getPixel(k, j);
                    if (this.imgArray[index]<this.thr){
                        this.imgArray[index]=0;
                        this.nbObj--;
                    }
                    index++;
                }
            }
        }
        if (this.closeImg) img.close();
        if (this.nbObj<=0){
            IJ.error("No object found");
            return;
        }
    }
    
    /** Set to zero pixels below the threshold in the "imgArray" arrays.
     */
    private void imgArrayModifier(){
        int index=0;
        for (int i=1; i<=this.nbSlices; i++){
            for (int j=0; j<this.height; j++){
                for (int k=0; k<this.width; k++){
                    if (this.imgArray[index]<this.thr){
                        this.imgArray[index]=0;
                        this.nbObj--;
                    }
                    index++;
                }
            }
        }
        if (this.nbObj<=0){
            IJ.error("No object found");
            return;
        }
    }
    
    private void prepareImgArrayForRedirect(){
        int index=0;
        ImagePlus imgRedir=WindowManager.getImage(Prefs.get("3D-OC-Options_redirectTo.string", "none"));
        this.title=this.title+" redirect to "+imgRedir.getTitle();
        for (int i=1; i<=this.nbSlices; i++){
            imgRedir.setSlice(i);
            for (int j=0; j<this.height; j++){
                for (int k=0; k<this.width; k++){
                    if (this.objID[index]!=0){
                        this.imgArray[index]=imgRedir.getProcessor().getPixel(k, j);
                    }else{
                        this.imgArray[index]=0;
                    }
                    index++;
                }
            }
        }
        if (this.closeImg) imgRedir.close();
    }
    
    /** Returns an ResultsTable containing statistics on objects:</P>
     * Volume and Surface: number of pixel forming the structures and at its surface respectively.</P>
     * StdDev, Median, IntDen, Min and Max: standard deviation, median, sum, minimum and maximum of all intensities for the current object.</P>
     * X, Y and Z: coordinates of the current object's centroïd.</P>
     * XM, YM and ZM: coordinates of the current object's centre of mass.</P>
     * BX, BY and BZ: coordinates of the top-left corner of the current object's bounding box.</P>
     * B-width, B-height and B-depth: current object's bounding box dimensions.</P>
     * @param newRT should be false if the result window is to be named "Results", allowing use of "Analyze/Distribution" and "Analyze/Summarize". If true, the window will be named "Statistics for "+image title.
     */
    public void showStatistics(boolean newRT){
        if (!this.getObjects) this.getObjects();
        float calXYZ=(float) (this.cal.pixelWidth*this.cal.pixelHeight*this.cal.pixelDepth);
        String unit=this.cal.getUnit();
        
        String[] header={"Volume ("+unit+"^3)", "Surface ("+unit+"^2)", "Nb of obj. voxels", "Nb of surf. voxels", "IntDen", "Mean", "StdDev", "Median", "Min", "Max", "X", "Y", "Z", "Mean dist. to surf. ("+unit+")", "SD dist. to surf. ("+unit+")", "Median dist. to surf. ("+unit+")", "XM", "YM", "ZM", "BX", "BY", "BZ", "B-width", "B-height", "B-depth"};
        ResultsTable rt;
        
        if (newRT){
            rt=new ResultsTable();
        }else{
            rt=ResultsTable.getResultsTable();
            rt.reset();
        }
        
        for (int i=0; i<header.length; i++) rt.setHeading(i, header[i]);
        for (int i=0; i<this.nbObj; i++){
            rt.incrementCounter();
            Object3D currObj=this.obj[i];
            
            if (Prefs.get("3D-OC-Options_volume.boolean", true)) rt.setValue("Volume ("+unit+"^3)", i, currObj.size*calXYZ);
            if (Prefs.get("3D-OC-Options_surface.boolean", true)) rt.setValue("Surface ("+unit+"^2)", i, currObj.surf_cal);
            if (Prefs.get("3D-OC-Options_objVox.boolean", true)) rt.setValue("Nb of obj. voxels", i, currObj.size);
            if (Prefs.get("3D-OC-Options_surfVox.boolean", true)) rt.setValue("Nb of surf. voxels", i, currObj.surf_size);
            if (Prefs.get("3D-OC-Options_IntDens.boolean", true)) rt.setValue("IntDen", i, currObj.int_dens);
            if (Prefs.get("3D-OC-Options_mean.boolean", true)) rt.setValue("Mean", i, currObj.mean_gray);
            if (Prefs.get("3D-OC-Options_stdDev.boolean", true)) rt.setValue("StdDev", i, currObj.SD);
            if (Prefs.get("3D-OC-Options_median.boolean", true)) rt.setValue("Median", i, currObj.median);
            if (Prefs.get("3D-OC-Options_min.boolean", true)) rt.setValue("Min", i, currObj.min);
            if (Prefs.get("3D-OC-Options_max.boolean", true)) rt.setValue("Max", i, currObj.max);
            
            
            if (Prefs.get("3D-OC-Options_centroid.boolean", true)){
                float[] tmpArray=currObj.centroid;
                rt.setValue("X", i, tmpArray[0]);
                rt.setValue("Y", i, tmpArray[1]);
                if (this.nbSlices!=1) rt.setValue("Z", i, tmpArray[2]);
            }
            
            if (Prefs.get("3D-OC-Options_meanDist2Surf.boolean", true)) rt.setValue("Mean dist. to surf. ("+unit+")", i, currObj.mean_dist2surf);
            if (Prefs.get("3D-OC-Options_SDDist2Surf.boolean", true)) rt.setValue("SD dist. to surf. ("+unit+")", i, currObj.SD_dist2surf);
            if (Prefs.get("3D-OC-Options_medDist2Surf.boolean", true)) rt.setValue("Median dist. to surf. ("+unit+")", i, currObj.median_dist2surf);
            
            if (Prefs.get("3D-OC-Options_COM.boolean", true)){
                float[] tmpArray=currObj.c_mass;
                rt.setValue("XM", i, tmpArray[0]);
                rt.setValue("YM", i, tmpArray[1]);
                if (this.nbSlices!=1) rt.setValue("ZM", i, tmpArray[2]);
            }
            
            if (Prefs.get("3D-OC-Options_BB.boolean", true)){
            int[] tmpArrayInt=currObj.bound_cube_TL;
                rt.setValue("BX", i, tmpArrayInt[0]);
                rt.setValue("BY", i, tmpArrayInt[1]);
                if (this.nbSlices!=1) rt.setValue("BZ", i, tmpArrayInt[2]);

                rt.setValue("B-width", i, currObj.bound_cube_width);
                rt.setValue("B-height", i, currObj.bound_cube_height);
                if (this.nbSlices!=1) rt.setValue("B-depth", i, currObj.bound_cube_depth);
            }
            
        }
        
        if (newRT){
            rt.show("Statistics for "+this.title);
        }else{
            rt.show("Results");
        }
    }
    
    /** Returns a summary containing the image name and the number of retrieved objects including the set filter size and threshold.
     */
    public void showSummary(){
        IJ.log(this.title+": "+this.nbObj+" objects detected (Size filter set to "+this.minSize+"-"+this.maxSize+" voxels, threshold set to: "+this.thr+").");
    }
    
    /** Returns an ResultsTable containing coordinates of the surface pixels for all objects:</P>
     * Object ID: current object number.</P>
     * X, Y and Z: coordinates of the current object's surface pixel.</P>
     */
    public void showSurfPix(){
        if (!this.getSurfCoord) this.populateSurfPixCoord();
        
        String[] header={"Object ID", "X", "Y", "Z"};
        ResultsTable rt=new ResultsTable();
        for (int i=0; i<header.length; i++) rt.setHeading(i, header[i]);
        for (int i=0; i<this.surfCoord.length; i++){
            rt.incrementCounter();
            for (int j=0; j<4; j++) rt.setValue(j, i, this.surfCoord[i][j]);
        }
        
        rt.show("Surface pixel coordinates for "+this.title);
    }
    
    /** Returns the index where to find the informations corresponding to pixel (x, y, z).
     * @param x coordinate of the pixel.
     * @param y coordinate of the pixel.
     * @param z coordinate of the pixel.
     * @return the index where to find the informations corresponding to pixel (x, y, z).
     */
    private int offset(int m,int n,int o){
        if (m+n*this.width+(o-1)*this.width*this.height>=this.width*this.height*this.nbSlices){
            return this.width*this.height*this.nbSlices-1;
        }else{
            if (m+n*this.width+(o-1)*this.width*this.height<0){
                return 0;
            }else{
                return m+n*this.width+(o-1)*this.width*this.height;
            }
        }
    }
    
    /** Returns the minimum anterior tag among the 13 previous pixels (4 pixels in 2D).
     * @param initialValue: value to which the 13 (or 4) retrieved values should be compared to
     * @param x coordinate of the current pixel.
     * @param y coordinate of the current pixel.
     * @param z coordinate of the current pixel.
     * @return the minimum found anterior tag as an integer.
     */
    private int minAntTag(int initialValue, int x, int y, int z){
        int min=initialValue;
        int currPos;

        for (int neigbY=y-1; neigbY<=y+1; neigbY++){
            for (int neigbX=x-1; neigbX<=x+1; neigbX++){
                //Following line is important otherwise objects might be linked from one side of the stack to the other !!!
                if (neigbX>=0 && neigbX<this.width && neigbY>=0 && neigbY<this.height && z-1>=1 && z-1<=this.nbSlices){
                    currPos=offset(neigbX, neigbY, z-1);
                    if (this.imgArray[currPos]!=0) min=Math.min(min, this.objID[currPos]);
                }
            }
        }

        for (int neigbX=x-1; neigbX<=x+1; neigbX++){
            //Following line is important otherwise objects might be linked from one side of the stack to the other !!!
            if (neigbX>=0 && neigbX<this.width && y-1>=0 && y-1<this.height && z>=1 && z<=this.nbSlices){
                currPos=offset(neigbX, y-1, z);
                if (this.imgArray[currPos]!=0) min=Math.min(min, this.objID[currPos]);
            }
        }

        //Following line is important otherwise objects might be linked from one side of the stack to the other !!!
        if (x-1>=0 && x-1<this.width && y>=0 && y<this.height && z>=1 && z<=this.nbSlices ){
            currPos=offset(x-1, y, z);
            if (this.imgArray[currPos]!=0 && x>=1 && y>=0 && z>=1) min=Math.min(min, this.objID[currPos]);
        }

        return min;
    }
    
    /** Replaces one object ID by another within the objID array.
     * @param old value to be replaced.
     * @param new value to be replaced by. </P>
     * NB: the arrays carrying the number of pixels/surface pixels carrying those IDs will also be updated.
     */
    private void replaceID(int oldVal, int newVal){
        if (oldVal!=newVal){
            int nbFoundPix=0;
            for (int i=0; i<this.objID.length; i++){
                if (this.objID[i]==oldVal){
                    this.objID[i]=newVal;
                    nbFoundPix++;
                }
                if (nbFoundPix==this.IDcount[oldVal]) i=this.objID.length;
            }
            this.IDcount[oldVal]=0;
            this.IDcount[newVal]+=nbFoundPix;
         }
    }
    
    /** Generates the ImagePlus based on Counter3D object width, height and number of slices, the input array and title.
     * @param imgArray containing the pixels intensities (integer array).
     * @param cenArray containing the coordinates of pixels where the labels should be put (integer array).
     * @param title to attribute to the ImagePlus (string).
     * @param drawDots should be true if dots should be drawn instead of a single pixel for each coordinate of imgArray (boolean).
     * @param drawNb should be true if numbers have to be drawn at each coordinate stored in cenArray (boolean).
     * @param whiteNb should be true if numbers have to appear white  (boolean).
     * @param dotSize size of the dots to be drawn (integer).
     * @param fontSize font size of the numbers to be shown (integer).
     */
    private ImagePlus buildImg(int[] imgArray, int[] cenArray, String title, boolean drawDots, boolean drawNb, boolean whiteNb, int dotSize, int fontSize){
        int index=0;
        int imgDepth=16;
        float min=imgArray[0];
        float max=imgArray[0];
        
        for (int i=0; i<imgArray.length; i++){
            int currVal=imgArray[i];
            min=Math.min(min, currVal);
            max=Math.max(max, currVal);
        }
        
        if (max<256) imgDepth=8;
        ImagePlus img=NewImage.createImage(title, this.width, this.height, this.nbSlices, imgDepth, 1);
        
        for (int z=1; z<=this.nbSlices; z++){
            IJ.showStatus("Creating the image...");
            img.setSlice(z);
            ImageProcessor ip=img.getProcessor();
            for (int y=0; y<this.height; y++){
                for (int x=0; x<this.width; x++){
                    int currVal=imgArray[index];
                    if (currVal!=0){
                        ip.setValue(currVal);
                        if (drawDots){
                            ip.setLineWidth(dotSize);
                            ip.drawDot(x, y);
                        }else{
                            ip.putPixel(x, y, currVal);
                        }
                    }
                    index++;
                }
            }
        }
        IJ.showStatus("");
        
        index=0;
        if (drawNb && cenArray!=null){
            for (int z=1; z<=this.nbSlices; z++){
                IJ.showStatus("Numbering objects...");
                img.setSlice(z);
                ImageProcessor ip=img.getProcessor();
                ip.setValue(Math.pow(2, imgDepth));
                ip.setFont(new Font("Arial", Font.PLAIN, fontSize));
                for (int y=0; y<this.height; y++){
                    for (int x=0; x<this.width; x++){
                        int currVal=cenArray[index];
                        if (currVal!=0){
                            if (!whiteNb) ip.setValue(currVal);
                            ip.drawString(""+currVal, x, y);
                        }
                        index++;
                    }
                }
            }
        }
        IJ.showStatus("");
        
        img.setCalibration(this.cal);
        img.setDisplayRange(min, max);
        return img;
    }
}
