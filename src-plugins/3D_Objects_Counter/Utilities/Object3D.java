/*
 * _3DObject.java
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

import ij.measure.*;

import java.util.*;

/**
 *
 * @author Fabrice P. Cordelieres, fabrice.cordelieres@gmail.com
 * @version 1.0, 7/11/07
 */
public class Object3D {
    /**Stores coordinates and intensities of the current Object3D: obj_voxels[index][0:x, 1:y, 2:z, 3:intensity]*/
    public int[][] obj_voxels;

    /**For each obj_voxels[i][j], stores true if current voxel is on surface, false otherwise*/
    public boolean[] isSurfVoxels;

    /**Vector of length 3 array containing the 3D coordinates of all surface pixels**/
    public Vector<int[]> surf_voxelsCoord;
    
    /**Mean intensity*/
    public float mean_gray;
    
    /**Median intensity*/
    public float median;
    
    /**SD of the intensity*/
    public float SD;
    
    /**Minimum intensity*/
    public int min;
    
    /**Maximum intensity*/
    public int max;
    
    /**Integrated density / summed intensity*/
    public  float int_dens=0;
    
    /**Mean distance to the surface (distance calibrated measure)*/
    public float mean_dist2surf;
    
    /**Median distance to the surface (distance calibrated measure)*/
    public float median_dist2surf;
    
    /**SD of the distance to the surface (distance calibrated measure)*/
    public float SD_dist2surf;
        
    /**Object3D's size (in pixels or voxels)*/
    public int size;
    
    /**Number of pixels/voxels on the surface*/
    public int surf_size;
    
    /**True calibrated surface measurement*/
    public float surf_cal;
    
     /**Coordinates of the centroïd (centroid[0:x, 1:y, 2:z]*/
    public float[] centroid;
    
    /**Coordinates of the centre of mass (c_mass[0:x, 1:y, 2:z]*/
    public float[] c_mass;
    
    /**Bounding cube top-left corner coordinates (bound_cube_TL[0:x, 1:y, 2:z])*/
    public int[] bound_cube_TL;
    
    /**Bounding cube bottom-right corner coordinates (bound_cube_BR[0:x, 1:y, 2:z])*/
    public int[] bound_cube_BR;
    
    
    /**Width of the bounding cube*/
    public int bound_cube_width;
    
    /**Height of the bounding cube*/
    public int bound_cube_height;
    
    /**Depth of the bounding cube*/
    public int bound_cube_depth;
    
    /**Calibration of the Object3D*/
    public Calibration cal;
    
    int currIndex;
    
    /** Creates a new instance of Object3D (distances will be calibrated according to the input calibration)
     *  @param size specifies the number of voxels for the new 3DObject, distances being calibrated.
     *  @param cal calibration to be used for distance/volume measurements
     */
    public Object3D(int size, Calibration cal) {
        this.size=size;
        currIndex=-1;
        //0:x, 1:y, 2:z, 3:val
        obj_voxels=new int[size][4];
        isSurfVoxels=new boolean[size];
        surf_voxelsCoord=new Vector<int[]>();
        this.cal=cal;
        surf_size=0;
        surf_cal=0;
    }
    
    /** Creates a new instance of Object3D
     *  @param size specifies the number of voxels for the new 3DObject.
     */
    public Object3D(int size) {
        this(size, new Calibration()); // use of "new Calibration()" gives 1x1x1 calibration in pixels instead of null which gives nothing...
    }
    
    /** Adds a new voxel to the specified Object3D, at the next available slot in the object.
     *  @param x specifies the x coordinate of the voxel to be added.
     *  @param y specifies the y coordinate of the voxel to be added.
     *  @param z specifies the z coordinate of the voxel to be added.
     *  @param val specifies the intensity of the voxel to be added.
     *  @param isSurf specifies if the voxel to be added is on the surface of the object.
     */
    public void addVoxel(int x, int y, int z, int val, boolean isSurf, float surf){
        currIndex++;
        if (currIndex>size-1) throw new IllegalArgumentException("The current Object3D is already full: resize it prior to call addVoxel");
        obj_voxels[currIndex][0]=x;
        obj_voxels[currIndex][1]=y;
        obj_voxels[currIndex][2]=z;
        obj_voxels[currIndex][3]=val;
        isSurfVoxels[currIndex]=isSurf;
        if (isSurf){
            int[]coord={x, y, z};
            surf_voxelsCoord.add(coord);
            surf_size++;
        }
        surf_cal+=surf;
        if (currIndex==size-1) calcStats();
    }
    
    
    /**Calculates the statistices of the current object, once all its voxels have been collected.
     */
    private void calcStats(){
         //Initialisation of the variables containing the stats
        centroid=new float[3];
        c_mass=new float[3];
        bound_cube_TL=new int[3];
        bound_cube_BR=new int[3];
        
        min=obj_voxels[0][3];
        max=obj_voxels[0][3];
        int_dens=0;
        mean_dist2surf=0;
        median_dist2surf=0;
        SD_dist2surf=0;
        
        for (int i=0; i<3; i++){
            centroid[i]=0;
            c_mass[i]=0;
            bound_cube_TL[i]=obj_voxels[0][i];
            bound_cube_BR[i]=obj_voxels[0][i];
        }
        
        //Generation of the statistics
        for (int i=0; i<size; i++){
            int curr_int_val=obj_voxels[i][3];
            int_dens+=curr_int_val;
            min=Math.min(min, curr_int_val);
            max=Math.max(max, curr_int_val);
            for (int j=0; j<3; j++){
                int curr_val=obj_voxels[i][j];
                centroid[j]+=curr_val;
                c_mass[j]+=curr_val*curr_int_val;
                bound_cube_TL[j]=Math.min(bound_cube_TL[j], curr_val);
                bound_cube_BR[j]=Math.max(bound_cube_BR[j], curr_val);
            }
        }
        
        
        bound_cube_width=bound_cube_BR[0]-bound_cube_TL[0]+1;
        bound_cube_height=bound_cube_BR[1]-bound_cube_TL[1]+1;
        bound_cube_depth=bound_cube_BR[2]-bound_cube_TL[2]+1;
        
        mean_gray=int_dens/size;
        
        //medianTmp will temporarely store only the intensity values
        float[] medianTmp=new float[obj_voxels.length];
        for (int i=0; i<obj_voxels.length; i++) medianTmp[i]=obj_voxels[i][3];
        median=median(medianTmp);
        
        SD=0;
        if (size!=1){
            for (int i=0; i<obj_voxels.length; i++) SD+=(obj_voxels[i][3]-mean_gray)*(obj_voxels[i][3]-mean_gray); // faster than Math.pow(x, 2)
            SD=(float) Math.sqrt(SD/(size-1));
        }
        
        for (int i=0; i<3; i++){
            centroid[i]/=size;
            c_mass[i]/=int_dens;
        }
        
        //mean, SD and med distance to surface
        float[] dist2surfArray=new float[surf_size];
        int index=0;
        for (int i=0; i<size; i++){
            if (isSurfVoxels[i]){
                dist2surfArray[index]=(float) Math.sqrt(cal.pixelWidth*cal.pixelWidth*(obj_voxels[i][0]-centroid[0])*(obj_voxels[i][0]-centroid[0])+cal.pixelHeight*cal.pixelHeight*(obj_voxels[i][1]-centroid[1])*(obj_voxels[i][1]-centroid[1])+cal.pixelDepth*cal.pixelDepth*(obj_voxels[i][2]-centroid[2])*(obj_voxels[i][2]-centroid[2]));
                mean_dist2surf+=dist2surfArray[index];
                index++;
            }
        }
        mean_dist2surf/=surf_size;
        SD_dist2surf=0;
        if (surf_size!=1){
            for (int i=0; i<surf_size; i++) SD_dist2surf+=Math.pow(dist2surfArray[i]-mean_dist2surf, 2);
            median_dist2surf=median(dist2surfArray);
            SD_dist2surf=(float) Math.sqrt(SD_dist2surf/(surf_size-1));
        }
        
    }
    
    /**Calculates the med value of a float array.
     * @param array input float array.
     * @return the med value of the input float array (float).
     */
    public float median(float[] array){
        float med=0;
        int index=0;
                
        Arrays.sort(array);
        
        //If the number of values is odd, returns the central value, otherwise computes the mean of the two boarding values.
        if ((float) array.length/2-(int) array.length/2==0){
            index=((int) array.length/2)-1;
            med=(array[index]+array[index+1])/2;
            
        }else{
            index=((int) (array.length+1)/2)-1;
            med=array[index];
        }
        
        return med;
    }
}
