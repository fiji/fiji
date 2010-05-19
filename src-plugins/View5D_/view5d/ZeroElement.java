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

// import java.io.*;
package view5d;

class ZeroElement extends AnElement {
    ZeroElement(int SX, int SY, int SZ) {super(SX,SY,SZ,0.0);DataType=FloatType;}
    void Clear() {return;}
    void DeleteData() {return;}
    int GetStdByteNum() {return 0;}
    void ConvertSliceFromSimilar(int param, int bufslice, Object values, int mstep, int moff) {return;}
    void ConvertSliceFromByte(int myslice, int bufslice, byte [] Ibuffer, int mstep, int moff) {return;}
    void ConvertSliceFromRGB(int myslice, int bufslice, int [] Ibuffer, int mstep, int moff, int soff)    {throw new IllegalArgumentException("Int: Inapplicable conversion\n");}
    int GetByteValueAt(int param, int param1, int param2) {return 0;}
    int GetIntValueAt(int param, int param1, int param2) {return 0;}
    double GetRawValueAt(int param, int param1, int param2) {return 0.0;}
    double GetValueAt(int param, int param1, int param2) {return 0.0;}
    void SetValueAt(int param, int param1, int param2, double param3) {return;}
    void CopySliceToSimilar(int myslice, Object buffer)  {return;}
}
