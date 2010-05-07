/****************************************************************************
 *   Copyright (C) 1996-2007 by Rainer Heintzmann                          *
 *   heintzmann@gmail.com                                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************
*/
// By making the appropriate class "View5D" or "View5D_" public and renaming the file, this code can be toggled between Applet and ImageJ respectively
package view5d;


// import java.io.*;
import java.awt.*;

class RectROI extends ROI {
    int ProjMin[], ProjMax[];
     RectROI() {
         super();
     }
     
    double GetROISize(int dim) {
        return (ProjMax[dim] - ProjMin[dim])+1;  // returns number of pixels including both borders
     }

    public Rectangle GetSqrROI(int dim) {
        if (ProjMin == null)
            return null;
        if (dim == 0)
            return new Rectangle(ProjMin[2],ProjMin[1],
            (int) GetROISize(2)-1,(int) GetROISize(1)-1);
        if (dim == 1)
            return new Rectangle(ProjMin[0],ProjMin[2],
            (int) GetROISize(0)-1,(int) GetROISize(2)-1);
        if (dim == 2)
            return new Rectangle(ProjMin[0],ProjMin[1],
            (int) GetROISize(0)-1,(int) GetROISize(1)-1);
        return null;
    }
    

    void TakeSqrROIs(int Pmin[], int Pmax[]) {
        if (ProjMin == null)
        {
            ProjMin = new int[3];
            ProjMax = new int[3];
        }
        for (int d=0;d<3;d++)
        {
            ProjMin[d] = Pmin[d];ProjMax[d] = Pmax[d];
        }
    }

    public void UpdateSqrROI(int ROIX,int ROIY, int ROIXe, int ROIYe,int dir)
    {
        if (dir ==0)
        {
            ProjMin[2] = ROIX;ProjMax[2] = ROIXe;
            ProjMin[1] = ROIY;ProjMax[1] = ROIYe;
        }
        else if (dir==1)
        {
            ProjMin[0] = ROIX;ProjMax[0] = ROIXe;
            ProjMin[2] = ROIY;ProjMax[2] = ROIYe;
        }
        else
        {
            ProjMin[0] = ROIX;ProjMax[0] = ROIXe;
            ProjMin[1] = ROIY;ProjMax[1] = ROIYe;
        }
    }

    boolean InROIRange(int x,int y,int z) {
         if  (ProjMin == null)
             return true;
         if (x < ProjMin[0])
             return false;
         if (y < ProjMin[1])
             return false;
         if (z < ProjMin[2])
             return false;
         if (x > ProjMax[0])
             return false;
         if (y > ProjMax[1])
             return false;
         if (z > ProjMax[2])
             return false;
         return true;
     }
}
