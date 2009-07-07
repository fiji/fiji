/*
 * _3DObject.java
 *
 * Created on 7 novembre 2007, 11:54
 *
 * Copyright (C) 2007 Fabrice P. CordeliÃ¨res
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
 * @author Fabrice P. CordeliÃ¨res, fabrice.cordelieres@gmail.com
 * @version 1.0, 7/11/07
 */
public class Object3D {
    /**Stores coordinates and intensities of the current Object3D: obj_voxels[index][0:x, 1:y, 2:z, 3:intensity]*/
    public int[][] obj_voxels;
    
    /**For each obj_voxels[i][j], stores true if current voxel is on surface, false otherwise*/
    public boolean[] surf_voxels;
    
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
    public float int_dens=0;
    
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
    
     /**Coordinates of the centroÃ¯d (centroid[0:x, 1:y, 2:z]*/
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
        this.currIndex=-1;
        //0:x, 1:y, 2:z, 3:val
        this.obj_voxels=new int[this.size][4];
        this.surf_voxels=new boolean[this.size];
        this.cal=cal;
        this.surf_size=0;
        this.surf_cal=0;
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
        this.currIndex++;
        if (currIndex>this.size-1) throw new IllegalArgumentException("The current Object3D is already full: resize it prior to call addVoxel");
        this.obj_voxels[this.currIndex][0]=x;
        this.obj_voxels[this.currIndex][1]=y;
        this.obj_voxels[this.currIndex][2]=z;
        this.obj_voxels[this.currIndex][3]=val;
        this.surf_voxels[this.currIndex]=isSurf;
        if (isSurf) this.surf_size++;
        this.surf_cal+=surf;
        if (currIndex==this.size-1) this.calcStats();
    }
    
    
    /**Calculates the statistices of the current object, once all its voxels have been collected.
     */
    private void calcStats(){
         //Initialisation of the variables containing the stats
        this.centroid=new float[3];
        this.c_mass=new float[3];
        this.bound_cube_TL=new int[3];
        this.bound_cube_BR=new int[3];
        
        this.min=this.obj_voxels[0][3];
        this.max=this.obj_voxels[0][3];
        this.int_dens=0;
        this.mean_dist2surf=0;
        this.median_dist2surf=0;
        this.SD_dist2surf=0;
        
        for (int i=0; i<3; i++){
            this.centroid[i]=0;
            this.c_mass[i]=0;
            this.bound_cube_TL[i]=this.obj_voxels[0][i];
            this.bound_cube_BR[i]=this.obj_voxels[0][i];
        }
        
        //Generation of the statistics
        for (int i=0; i<this.size; i++){
            int curr_int_val=this.obj_voxels[i][3];
            this.int_dens+=curr_int_val;
            this.min=Math.min(this.min, curr_int_val);
            this.max=Math.max(this.max, curr_int_val);
            for (int j=0; j<3; j++){
                int curr_val=this.obj_voxels[i][j];
                this.centroid[j]+=curr_val;
                this.c_mass[j]+=curr_val*curr_int_val;
                this.bound_cube_TL[j]=Math.min(this.bound_cube_TL[j], curr_val);
                this.bound_cube_BR[j]=Math.max(this.bound_cube_BR[j], curr_val);
            }
        }
        
        
        this.bound_cube_width=this.bound_cube_BR[0]-this.bound_cube_TL[0]+1;
        this.bound_cube_height=this.bound_cube_BR[1]-this.bound_cube_TL[1]+1;
        this.bound_cube_depth=this.bound_cube_BR[2]-this.bound_cube_TL[2]+1;
        
        this.mean_gray=this.int_dens/this.size;
        
        //medianTmp will temporarely store only the intensity values
        float[] medianTmp=new float[this.obj_voxels.length];
        for (int i=0; i<this.obj_voxels.length; i++) medianTmp[i]=this.obj_voxels[i][3];
        this.median=median(medianTmp);
        
        this.SD=0;
        if (this.size!=1){
            for (int i=0; i<this.obj_voxels.length; i++) this.SD+=(this.obj_voxels[i][3]-this.mean_gray)*(this.obj_voxels[i][3]-this.mean_gray); // faster than Math.pow(x, 2)
            this.SD=(float) Math.sqrt(this.SD/(this.size-1));
        }
        
        for (int i=0; i<3; i++){
            this.centroid[i]/=this.size;
            this.c_mass[i]/=this.int_dens;
        }
        
        //mean, SD and median distance to surface
        float[] dist2surfArray=new float[this.surf_size];
        int index=0;
        for (int i=0; i<this.size; i++){
            if (this.surf_voxels[i]){
                dist2surfArray[index]=(float) Math.sqrt(this.cal.pixelWidth*this.cal.pixelWidth*(this.obj_voxels[i][0]-this.centroid[0])*(this.obj_voxels[i][0]-this.centroid[0])+this.cal.pixelHeight*this.cal.pixelHeight*(this.obj_voxels[i][1]-this.centroid[1])*(this.obj_voxels[i][1]-this.centroid[1])+this.cal.pixelDepth*this.cal.pixelDepth*(this.obj_voxels[i][2]-this.centroid[2])*(this.obj_voxels[i][2]-this.centroid[2]));
                this.mean_dist2surf+=dist2surfArray[index];
                index++;
            }
        }
        this.mean_dist2surf/=this.surf_size;
        this.SD_dist2surf=0;
        if (this.surf_size!=1){
            for (int i=0; i<this.surf_size; i++) this.SD_dist2surf+=Math.pow(dist2surfArray[i]-this.mean_dist2surf, 2);
            this.median_dist2surf=median(dist2surfArray);
            this.SD_dist2surf=(float) Math.sqrt(this.SD_dist2surf/(this.surf_size-1));
        }
        
    }
    
    /**Calculates the median value of a float array.
     * @param array input float array.
     * @return the median value of the input float array (float).
     */
    public float median(float[] array){
        float median=0, count=0;
        int index=0;
                
        Arrays.sort(array);
        
        //If the number of values is odd, returns the central value, otherwise computes the mean of the two boarding values.
        if ((float) array.length/2-(int) array.length/2==0){
            index=((int) array.length/2)-1;
            median=(array[index]+array[index+1])/2;
            
        }else{
            index=((int) (array.length+1)/2)-1;
            median=array[index];
        }
        
        return median;
    }
}
