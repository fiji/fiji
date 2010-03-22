/*
 * Counter3D.java
 *
 * Created on 7 novembre 2007, 11:54
 *
 * Copyright (C) 2007 Fabrice P. Cordelieres
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

import Utilities.Counter3D.*;
import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.process.*;

import java.awt.*;
import java.util.*;

/**
 *
 * @author Fabrice P. Cordelieres, fabrice.cordelieres@gmail.com
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
    Vector<Object3D> obj;
    
    boolean foundObjects=false, getObjects=false, getCentreOfMass=false, getCentroid=false, getSurfList=false, getSurfCoord=false;
    
    
    /**
     * Creates a new instance of Counter3D.
     *
     * @param img specifies the image to convert into an Counter3D.
     * @param thr specifies the threshold value (should be an Integer).
     * @param MIN specifies the MIN size threshold to be used (should be an Integer).
     * @param MAX specifies the MAX size threshold to be used (should be an Integer).
     * @param exclude specifies if the objects on the edges should be excluded (should be a boolean).
     * @param redirect specifies if intensities measurements should be redirected to another image defined within the options window (should be a boolean).
     */
     public Counter3D(ImagePlus img, int thr, int min, int max, boolean exclude, boolean redirect) {
        width=img.getWidth();
        height=img.getHeight();
        nbSlices=img.getNSlices();
        length=width*height*nbSlices;
        depth=img.getBitDepth();
        title=img.getTitle();
        cal=img.getCalibration();
        this.thr=thr;
        minSize=min;
        maxSize=max;
        sizeFilter=true;
        this.exclude=exclude;
        this.redirect=redirect;
        
        if (depth!=8 && depth!=16) throw new IllegalArgumentException("Counter3D class expects 8- or 16-bits images only");
         
        nbObj=length;
        
        imgArray=new int[length];
        
        imgArrayModifier(img);
        
    }
    
    /**
     * Creates a new instance of Counter3D.
     *
     * @param img specifies the image to convert into an Counter3D.
     * @param thr specifies the threshold value (should be an Integer).
     */
    public Counter3D(ImagePlus img, int thr) {
        this(img, thr, 1, img.getWidth()*img.getHeight()*img.getNSlices(), false, false);
        sizeFilter=false;
    }
    
    /**
     * Creates a new instance of Counter3D
     *
     * @param img specifies the image to convert into an Counter3D.
     */
    public Counter3D(ImagePlus img) {
        this(img, 0, 1, img.getWidth()*img.getHeight()*img.getNSlices(), false, false);
        sizeFilter=false;
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
     * @param MIN specifies the MIN size threshold to be used (should be an Integer).
     * @param MAX specifies the MAX size threshold to be used (should be an Integer).
     * @param exclude specifies if the objects on the edges should be excluded (should be a boolean).
     * @param redirect specifies if intensities measurements should be redirected to another image defined within the options window (should be a boolean).
     * @param cal specifies the image calibration to be used.
     */
    public Counter3D(int[] img, String title, int width, int height, int nbSlices, int thr, int min, int max, boolean exclude, boolean redirect, Calibration cal) {
        this.title=title;
        this.width=width;
        this.height=height;
        this.nbSlices=nbSlices;
        length=width*height*nbSlices;
        if (length!=img.length) throw new IllegalArgumentException("The image array length differs from the given image dimensions");
        this.title=title;
        this.cal=cal;
        
        this.thr=thr;
        minSize=min;
        maxSize=max;
        sizeFilter=true;
        this.exclude=exclude;
        this.redirect=redirect;
        
        nbObj=length;
        
        imgArray=img;
                
        imgArrayModifier();
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
        sizeFilter=false;
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
        sizeFilter=false;
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
        sizeFilter=false;
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
        sizeFilter=false;
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
                
        objID=new int[length];
        
        for (int z=1; z<=nbSlices; z++){
            for (int y=0; y<height; y++){
                for (int x=0; x<width; x++){
                    if (minID==currID) currID++;
                    if (imgArray[currPos]!=0){
                        minID=currID;
                        minID=minAntTag(minID, x, y, z);
                        objID[currPos]=minID;
                    }
                    currPos++;
                }
            }
            //IJ.showStatus("Finding structures "+z*100/nbSlices+"%");
            IJ.showStatus("Step 1/3: Finding structures");
            IJ.showProgress(z, nbSlices);
        }
        IJ.showStatus("");
        
        IDcount=new int[currID];
        for (int i=0; i<length; i++) IDcount[objID[i]]++;
        
        IDisAtEdge=new boolean[currID];
        Arrays.fill(IDisAtEdge, false);
        /*
         *Connecting structures:
         *The first tagging of structure may have led to shearing apart pieces of a same structure
         *This part will connect them back by attributing the minimal retrieved tag among the 13 neighboring
         *pixels located prior to the current pixel + the centre pixel and will replace all the values of those pixels
         *by the minimum value.
         */
        isSurf=new boolean[length];
        currPos=0;
        minID=1;
                
        for (int z=1; z<=nbSlices; z++){
            for (int y=0; y<height; y++){
                for (int x=0; x<width; x++){
                    if (imgArray[currPos]!=0){
                        minID=objID[currPos];
                        surfPix=0;
                        neigbNb=0;
                        //Find the minimum tag in the neighbours pixels
                        for (neigbZ=z-1; neigbZ<=z+1; neigbZ++){
                            for (neigbY=y-1; neigbY<=y+1; neigbY++){
                                for (neigbX=x-1; neigbX<=x+1; neigbX++){
                                    //Following line is important otherwise objects might be linked from one side of the stack to the other !!!
                                    if (neigbX>=0 && neigbX<width && neigbY>=0 && neigbY<height && neigbZ>=1 && neigbZ<=nbSlices){
                                        pos=offset(neigbX, neigbY, neigbZ);
                                        if (imgArray[pos]!=0){
                                            if ((nbSlices>1 && ((neigbX==x && neigbY==y && neigbZ==z-1) ||(neigbX==x && neigbY==y && neigbZ==z+1))) ||(neigbX==x && neigbY==y-1 && neigbZ==z) ||(neigbX==x && neigbY==y+1 && neigbZ==z) ||(neigbX==x-1 && neigbY==y && neigbZ==z) ||(neigbX==x+1 && neigbY==y && neigbZ==z)) surfPix++;
                                            minID=Math.min(minID, objID[pos]);
                                        }
                                        neigbNb++;
                                    }
                                }
                            }
                        }
                        if ((surfPix!=6 && nbSlices>1) || (surfPix!=4 && nbSlices==1)){
                            isSurf[currPos]=true;
                            nbSurfPix++;
                        }else{
                            isSurf[currPos]=false;
                        }
                        //Replacing tag by the minimum tag found
                        for (neigbZ=z-1; neigbZ<=z+1; neigbZ++){
                            for (neigbY=y-1; neigbY<=y+1; neigbY++){
                                for (neigbX=x-1; neigbX<=x+1; neigbX++){
                                    //Following line is important otherwise objects might be linked from one side of the stack to the other !!!
                                    if (neigbX>=0 && neigbX<width && neigbY>=0 && neigbY<height && neigbZ>=1 && neigbZ<=nbSlices){
                                        pos=offset(neigbX, neigbY, neigbZ);
                                        if (imgArray[pos]!=0){
                                            currPixID=objID[pos];
                                            if (currPixID>minID) replaceID(currPixID, minID);
                                        }
                                    }
                                }
                            }
                        }
                        
                        //Check if the current particle is touching an edge
                        if(x==0 || y==0 || x==width-1 || y==height-1 || (nbSlices!=1 && (z==1 || z==nbSlices))) IDisAtEdge[minID]=true;
                    }
                    currPos++;
                }
            }
            IJ.showStatus("Step 2/3: Connecting structures");
            IJ.showProgress(z, nbSlices);
        }
        IJ.showStatus("");
        
        int newCurrID=0;
        
        //Renumbering of all the found objects and update of their respective number of pixels while filtering based on the number of pixels
        for (int i=1; i<IDcount.length; i++){
            if ((IDcount[i]!=0 && IDcount[i]>=minSize && IDcount[i]<=maxSize)&& (!exclude || !(exclude && IDisAtEdge[i]))){
                newCurrID++;
                int nbPix=IDcount[i];
                replaceID(i, newCurrID);
                IDcount[newCurrID]=nbPix;
            }else{
                replaceID(i,0);
            }
            IJ.showStatus("Step 3/3: Renumbering structures");
            IJ.showProgress(i, currID);
        }
        IJ.showStatus("");
        
        if (redirect) prepareImgArrayForRedirect();
        if (showMaskedImg) buildImg(imgArray, null, "Masked image for "+title, false, false, false, 0, 0).show();
        
        nbObj=newCurrID;
        foundObjects=true;
    }
    
    /** Generates the objects list.
     */
    public void getObjects(){
        if (!foundObjects) findObjects();
        
        if (!getObjects){
            obj=new Vector<Object3D>();

            for (int i=0; i<nbObj; i++) obj.add(new Object3D(IDcount[i+1], cal));
            IDcount=null;
            
            int currPos=0;
            for (int z=1; z<=nbSlices; z++){
                for (int y=0; y<height; y++){
                    for (int x=0; x<width; x++){
                        int currID=objID[currPos];
                        if (currID!=0){
                            float surf=0;
                            if (nbSlices==1) surf=(float) (cal.pixelWidth*cal.pixelHeight);
                            if (isSurf[currPos] && nbSlices>1){
                                surf=(float) (2*(cal.pixelHeight*cal.pixelDepth+cal.pixelWidth*cal.pixelDepth+cal.pixelWidth*cal.pixelHeight));
                                //Look at the 6 exposed surfaces
                                if (x>0 && objID[offset (x-1, y, z)]==currID) surf-=cal.pixelHeight*cal.pixelDepth;
                                if (x<width-1 && objID[offset (x+1, y, z)]==currID) surf-=cal.pixelHeight*cal.pixelDepth;
                                if (y>0 && objID[offset (x, y-1, z)]==currID) surf-=cal.pixelWidth*cal.pixelDepth;
                                if (y<height-1 && objID[offset (x, y+1, z)]==currID) surf-=cal.pixelWidth*cal.pixelDepth;
                                if (z>1 && objID[offset (x, y, z-1)]==currID) surf-=cal.pixelWidth*cal.pixelHeight;
                                if (z<=nbSlices-1 && objID[offset (x, y, z+1)]==currID) surf-=cal.pixelWidth*cal.pixelHeight;
                            }
                            ((Object3D) (obj.get(currID-1))).addVoxel(x, y, z, imgArray[currPos], isSurf[currPos], surf);
                        }
                        currPos++;
                    }
                }
            }
            imgArray=null;
            System.gc();
        }
        getObjects=true;
    }
    
    /**
     * Returns the object at the provided index, as an Object3D
     * @param index the index of the object to return
     * @return an Object3D or null idf the index is out of bounds
     */
    public Object3D getObject(int index){
        if (!getObjects) getObjects();
        if (index<0 || index>=nbObj) return null;
        return (Object3D) obj.get(index);
    }
    
    /**
     * Add the provided Object3D to the list 
     * @param object Object3D to add
     */
    public void addObject(Object3D object){
        if (!getObjects) getObjects();
        obj.add(object);
        nbObj++;
    }
    
    /**
     * Removes the Object3D stored at the provided index. Does nothing if index is out of bounds.
     * @param index index of the Object3D to be removed
     */
    public void removeObject(int index){
        if (!getObjects) getObjects();
        if (!(index<0 || index>=nbObj)){
            obj.remove(index);
            nbObj--;
        }
    }


    /**
     * Returns the list of all found objects.
     *
     * @return the list of all found objects as a Object3D array.
     */
    public Vector getObjectsList(){
        if (!getObjects) getObjects();
        return obj;
    }
    
    /**
     * Returns the objects map.
     * @param drawNb should be true if numbers have to be drawn at each coordinate stored in cenArray (boolean).
     * @param fontSize font size of the numbers to be shown (integer).* @return an ImagePlus containing all found objects, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getObjMap(boolean drawNb, int fontSize){
        if (!getObjects) getObjects();
        if (!getCentroid) populateCentroid();
        return buildImg(objID, coord2imgArray(centroid), "Objects map of "+title, false, drawNb, true, 0, fontSize);
    }
    
    /**
     * Returns the objects map.
     *
     * @return an ImagePlus containing all found objects, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getObjMap(){
        if (!getObjects) getObjects();
        return buildImg(objID, null, "Objects map of "+title, false, false, true, 0, 0);
    }

    /**
     * Returns the objects map as a 1D integer array.
     *
     * @return an ImagePlus containing all found objects, each one carrying pixel value equal to its ID.
     */
    public int[] getObjMapAsArray(){
        if (!getObjects) getObjects();
        return objID;
    }
    
    /** Generates and fills the "centreOfMass" array.
     */
    private void populateCentreOfMass(){
        if (!getObjects) getObjects();
        centreOfMass=new float[obj.size()][3];
        
        for (int i=0; i<obj.size(); i++){
            Object3D currObj=(Object3D) obj.get(i);
            float [] tmp=currObj.c_mass;
            for (int j=0; j<3; j++) centreOfMass[i][j]=tmp[j];
        }
        getCentreOfMass=true;
    }
    
    /**
     * Returns the centres of masses' list.
     *
     * @return the coordinates of all found centres of masses as a dual float array ([ID][0:x, 1:y, 2:z]).
     */
    public float[][] getCentreOfMassList(){
        if (!getCentreOfMass) populateCentreOfMass();
        return centreOfMass;
    }
    
     /**
     * Returns the centres of masses' map.
     * @param drawNb should be true if numbers have to be drawn at each coordinate stored in cenArray (boolean).
     * @param whiteNb should be true if numbers have to appear white  (boolean).
     * @param dotSize size of the dots to be drawn (integer).
     * @param fontSize font size of the numbers to be shown (integer).* @return an ImagePlus containing all centres of masses, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getCentreOfMassMap(boolean drawNb, boolean whiteNb, int dotSize, int fontSize){
        if (!getCentreOfMass) populateCentreOfMass();
        int[] array=coord2imgArray(centreOfMass);
        return buildImg(array, array, "Centres of mass map of "+title, true, drawNb, whiteNb, dotSize, fontSize);
    }
    
    /**
     * Returns the centres of masses' map.
     *
     * @return an ImagePlus containing all centres of masses, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getCentreOfMassMap(){
        if (!getCentreOfMass) populateCentreOfMass();
        int[] array=coord2imgArray(centreOfMass);
        return buildImg(array, null, "Centres of mass map of "+title, true, false, false, 5, 0);
    }
     
    /** Generates and fills the "centroid" array.
     */
    private void populateCentroid(){
        if (!getObjects) getObjects();
        centroid=new float[obj.size()][3];
        
        for (int i=0; i<obj.size(); i++){
            Object3D currObj=(Object3D) obj.get(i);
            float [] tmp=currObj.centroid;
            for (int j=0; j<3; j++) centroid[i][j]=tmp[j];
        }
        getCentroid=true;
    }
    
    /**
     * Returns the centroïds' list.
     *
     * @return the coordinates of all found centroïds as a dual float array ([ID][0:x, 1:y, 2:z]).
     */
    public float[][] getCentroidList(){
        if (!getCentroid) populateCentroid();
        return centroid;
    }
    
    /**
     * Returns the centroïds' map.
     * @param drawNb should be true if numbers have to be drawn at each coordinate stored in cenArray (boolean).
     * @param whiteNb should be true if numbers have to appear white  (boolean).
     * @param dotSize size of the dots to be drawn (integer).
     * @param fontSize font size of the numbers to be shown (integer).* @return an ImagePlus containing all centroïds, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getCentroidMap(boolean drawNb, boolean whiteNb, int dotSize, int fontSize){
        if (!getCentroid) populateCentroid();
        int[] array=coord2imgArray(centroid);
        return buildImg(array, array, "Centroids map of "+title, true, drawNb, whiteNb, dotSize, fontSize);
    }
    
    /**
     * Returns the centroïds' map.
     *
     * @return an ImagePlus containing all centroïds, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getCentroidMap(){
        if (!getCentroid) populateCentroid();
        int[] array=coord2imgArray(centroid);
        return buildImg(array, null, "Centroids map of "+title, true, false, false, 5, 0);
    }
    
    /** Generates and fills the "surface" array.
     */
    private void populateSurfList(){
        if (!getObjects) getObjects();
        
        surfList=new int[length];
        for (int i=0; i<length; i++) surfList[i]=isSurf[i]?objID[i]:0;
        getSurfList=true;
    }
    
    /**
     * Returns the surface pixels' list.
     *
     * @return the coordinates of all pixels found at the surface of objects as a mono-dimensional integer array.
     */
    public int[] getSurfPixList(){
        if (!getSurfList) populateSurfList();
        return surfList;
    }
    
    /** Generates and fills the "surfArray" array.
     */
    private void populateSurfPixCoord(){
        int index=0;
        
        surfCoord=new int[nbSurfPix][4];
        
        for (int i=0; i<nbObj; i++){
            Object3D currObj=(Object3D) obj.get(i);
            for (int j=0; j<currObj.surf_size; j++){
                surfCoord[index][0]=i+1;
                for (int k=1; k<4; k++) surfCoord[index][k]=currObj.obj_voxels[j][k-1];
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
        if (!getSurfCoord) populateSurfPixCoord();
        return surfCoord;
    }
    
    /**
     * Returns the surface pixels' map.
     * @param drawNb should be true if numbers have to be drawn at each coordinate stored in cenArray (boolean).
     * @param whiteNb should be true if numbers have to appear white  (boolean).
     * @param fontSize font size of the numbers to be shown (integer).* @return an ImagePlus containing all pixels found at the surface of objects, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getSurfPixMap(boolean drawNb, boolean whiteNb, int fontSize){
        if (!getSurfList) populateSurfList();
        if (!getCentroid) populateCentroid();
        return buildImg(surfList, coord2imgArray(centroid), "Surface map of "+title, false, drawNb, whiteNb, 0, fontSize);
    }
    
    /**
     * Returns the surface pixels' map.
     *
     * @return an ImagePlus containing all pixels found at the surface of objects, each one carrying pixel value equal to its ID.
     */
    public ImagePlus getSurfPixMap(){
        if (!getSurfList) populateSurfList();
        return buildImg(surfList, null, "Surface map of "+title, false, false, false, 0, 0);
    }
           
    /** Transforms a coordinates array ([ID][0:x, 1:y, 3:z]) to a linear array containing all pixels one next to the other.
     *
     *@return the linear array as an integer array.
     */
    private int[] coord2imgArray(float[][] coord){
        int[] array=new int[length];
        for (int i=0; i<coord.length; i++)array[offset((int) coord[i][0], (int) coord[i][1], (int) coord[i][2])]=i+1;
        return array;
    }
    
    /** Set to zero pixels below the threshold in the "imgArray" arrays.
     */
    private void imgArrayModifier(ImagePlus img){
        int index=0;
        for (int i=1; i<=nbSlices; i++){
            img.setSlice(i);
            for (int j=0; j<height; j++){
                for (int k=0; k<width; k++){
                    imgArray[index]=img.getProcessor().getPixel(k, j);
                    if (imgArray[index]<thr){
                        imgArray[index]=0;
                        nbObj--;
                    }
                    index++;
                }
            }
        }
        if (closeImg) img.close();
        if (nbObj<=0){
            IJ.error("No object found");
            return;
        }
    }
    
    /** Set to zero pixels below the threshold in the "imgArray" arrays.
     */
    private void imgArrayModifier(){
        int index=0;
        for (int i=1; i<=nbSlices; i++){
            for (int j=0; j<height; j++){
                for (int k=0; k<width; k++){
                    if (imgArray[index]<thr){
                        imgArray[index]=0;
                        nbObj--;
                    }
                    index++;
                }
            }
        }
        if (nbObj<=0){
            IJ.error("No object found");
            return;
        }
    }
    
    private void prepareImgArrayForRedirect(){
        int index=0;
        ImagePlus imgRedir=WindowManager.getImage(Prefs.get("3D-OC-Options_redirectTo.string", "none"));
        title=title+" redirect to "+imgRedir.getTitle();
        for (int i=1; i<=nbSlices; i++){
            imgRedir.setSlice(i);
            for (int j=0; j<height; j++){
                for (int k=0; k<width; k++){
                    if (objID[index]!=0){
                        imgArray[index]=imgRedir.getProcessor().getPixel(k, j);
                    }else{
                        imgArray[index]=0;
                    }
                    index++;
                }
            }
        }
        if (closeImg) imgRedir.close();
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
        if (!getObjects) getObjects();
        float calXYZ=(float) (cal.pixelWidth*cal.pixelHeight*cal.pixelDepth);
        String unit=cal.getUnit();
        
        String[] header={"Volume ("+unit+"^3)", "Surface ("+unit+"^2)", "Nb of obj. voxels", "Nb of surf. voxels", "IntDen", "Mean", "StdDev", "Median", "Min", "Max", "X", "Y", "Z", "Mean dist. to surf. ("+unit+")", "SD dist. to surf. ("+unit+")", "Median dist. to surf. ("+unit+")", "XM", "YM", "ZM", "BX", "BY", "BZ", "B-width", "B-height", "B-depth"};
        ResultsTable rt;
        
        if (newRT){
            rt=new ResultsTable();
        }else{
            rt=ResultsTable.getResultsTable();
            rt.reset();
        }
        
        for (int i=0; i<header.length; i++) rt.setHeading(i, header[i]);
        for (int i=0; i<nbObj; i++){
            rt.incrementCounter();
            Object3D currObj=(Object3D) obj.get(i);
            
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
                if (nbSlices!=1) rt.setValue("Z", i, tmpArray[2]);
            }
            
            if (Prefs.get("3D-OC-Options_meanDist2Surf.boolean", true)) rt.setValue("Mean dist. to surf. ("+unit+")", i, currObj.mean_dist2surf);
            if (Prefs.get("3D-OC-Options_SDDist2Surf.boolean", true)) rt.setValue("SD dist. to surf. ("+unit+")", i, currObj.SD_dist2surf);
            if (Prefs.get("3D-OC-Options_medDist2Surf.boolean", true)) rt.setValue("Median dist. to surf. ("+unit+")", i, currObj.median_dist2surf);
            
            if (Prefs.get("3D-OC-Options_COM.boolean", true)){
                float[] tmpArray=currObj.c_mass;
                rt.setValue("XM", i, tmpArray[0]);
                rt.setValue("YM", i, tmpArray[1]);
                if (nbSlices!=1) rt.setValue("ZM", i, tmpArray[2]);
            }
            
            if (Prefs.get("3D-OC-Options_BB.boolean", true)){
            int[] tmpArrayInt=currObj.bound_cube_TL;
                rt.setValue("BX", i, tmpArrayInt[0]);
                rt.setValue("BY", i, tmpArrayInt[1]);
                if (nbSlices!=1) rt.setValue("BZ", i, tmpArrayInt[2]);

                rt.setValue("B-width", i, currObj.bound_cube_width);
                rt.setValue("B-height", i, currObj.bound_cube_height);
                if (nbSlices!=1) rt.setValue("B-depth", i, currObj.bound_cube_depth);
            }
            
        }
        
        if (newRT){
            rt.show("Statistics for "+title);
        }else{
            rt.show("Results");
        }
    }
    
    /** Returns a summary containing the image name and the number of retrieved objects including the set filter size and threshold.
     */
    public void showSummary(){
        IJ.log(title+": "+nbObj+" objects detected (Size filter set to "+minSize+"-"+maxSize+" voxels, threshold set to: "+thr+").");
    }
    
    /** Returns an ResultsTable containing coordinates of the surface pixels for all objects:</P>
     * Object ID: current object number.</P>
     * X, Y and Z: coordinates of the current object's surface pixel.</P>
     */
    public void showSurfPix(){
        if (!getSurfCoord) populateSurfPixCoord();
        
        String[] header={"Object ID", "X", "Y", "Z"};
        ResultsTable rt=new ResultsTable();
        for (int i=0; i<header.length; i++) rt.setHeading(i, header[i]);
        for (int i=0; i<surfCoord.length; i++){
            rt.incrementCounter();
            for (int j=0; j<4; j++) rt.setValue(j, i, surfCoord[i][j]);
        }
        
        rt.show("Surface pixel coordinates for "+title);
    }
    
    /** Returns the index where to find the informations corresponding to pixel (x, y, z).
     * @param x coordinate of the pixel.
     * @param y coordinate of the pixel.
     * @param z coordinate of the pixel.
     * @return the index where to find the informations corresponding to pixel (x, y, z).
     */
    private int offset(int m,int n,int o){
        if (m+n*width+(o-1)*width*height>=width*height*nbSlices){
            return width*height*nbSlices-1;
        }else{
            if (m+n*width+(o-1)*width*height<0){
                return 0;
            }else{
                return m+n*width+(o-1)*width*height;
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
                if (neigbX>=0 && neigbX<width && neigbY>=0 && neigbY<height && z-1>=1 && z-1<=nbSlices){
                    currPos=offset(neigbX, neigbY, z-1);
                    if (imgArray[currPos]!=0) min=Math.min(min, objID[currPos]);
                }
            }
        }

        for (int neigbX=x-1; neigbX<=x+1; neigbX++){
            //Following line is important otherwise objects might be linked from one side of the stack to the other !!!
            if (neigbX>=0 && neigbX<width && y-1>=0 && y-1<height && z>=1 && z<=nbSlices){
                currPos=offset(neigbX, y-1, z);
                if (imgArray[currPos]!=0) min=Math.min(min, objID[currPos]);
            }
        }

        //Following line is important otherwise objects might be linked from one side of the stack to the other !!!
        if (x-1>=0 && x-1<width && y>=0 && y<height && z>=1 && z<=nbSlices ){
            currPos=offset(x-1, y, z);
            if (imgArray[currPos]!=0 && x>=1 && y>=0 && z>=1) min=Math.min(min, objID[currPos]);
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
            for (int i=0; i<objID.length; i++){
                if (objID[i]==oldVal){
                    objID[i]=newVal;
                    nbFoundPix++;
                }
                if (nbFoundPix==IDcount[oldVal]) i=objID.length;
            }
            IDcount[oldVal]=0;
            IDcount[newVal]+=nbFoundPix;
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
        ImagePlus img=NewImage.createImage(title, width, height, nbSlices, imgDepth, 1);
        
        for (int z=1; z<=nbSlices; z++){
            IJ.showStatus("Creating the image...");
            img.setSlice(z);
            ImageProcessor ip=img.getProcessor();
            for (int y=0; y<height; y++){
                for (int x=0; x<width; x++){
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
            for (int z=1; z<=nbSlices; z++){
                IJ.showStatus("Numbering objects...");
                img.setSlice(z);
                ImageProcessor ip=img.getProcessor();
                ip.setValue(Math.pow(2, imgDepth));
                ip.setFont(new Font("Arial", Font.PLAIN, fontSize));
                for (int y=0; y<height; y++){
                    for (int x=0; x<width; x++){
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
        
        img.setCalibration(cal);
        img.setDisplayRange(min, max);
        return img;
    }
}
