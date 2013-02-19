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
import java.util.*;

// A RIO class based on separating planes (in 3D, any orientation)
class PlaneROI extends ROI {
    Vector<Integer> PlanesS[];
    Vector<Double>PlanesD[];  // Starting vectors and direction vectors of planes
    
    @SuppressWarnings("unchecked")
	PlaneROI() {
        PlanesS = (Vector<Integer>[]) new Vector[3];
        PlanesD = (Vector<Double>[]) new Vector[3];
    }

    double GetROISize(int dim) {
        return 0;
     }

    void TakePlaneROIs(Vector<Integer> PS[],Vector<Double> PD[]) {
        PlanesS = PS;
        PlanesD = PD;
    }

    boolean InROIRange(int x,int y, int z) {
        int Sx,Sy,Sz;
        double Dx,Dy,Dz;
        if (PlanesS == null || PlanesD == null )
            return false;
        for (int i=0;i<PlanesS[0].size();i++)
            {
            Sx= x-((Integer) PlanesS[0].elementAt(i)).intValue();
            Sy= y-((Integer) PlanesS[1].elementAt(i)).intValue();
            Sz= z-((Integer) PlanesS[2].elementAt(i)).intValue();
            Dx=((Double) PlanesD[0].elementAt(i)).doubleValue();
            Dy=((Double) PlanesD[1].elementAt(i)).doubleValue();
            Dz=((Double) PlanesD[2].elementAt(i)).doubleValue();
            if (Dx*Sx+Dy*Sy+Dz*Sz < 0.0)
                return false;
            }
        return true;
        } 
}
