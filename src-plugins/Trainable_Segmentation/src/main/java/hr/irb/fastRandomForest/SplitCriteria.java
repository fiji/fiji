/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    SplitCriteria.java
 *    Copyright (C) 1999 University of Waikato, Hamilton, NZ (original
 *      code, ContingencyTables.java )
 *    Copyright (C) 2008 Fran Supek (adapted code)
 */

package hr.irb.fastRandomForest;


/**
 * Functions used for finding best splits in FastRfTree. Based on parts of
 * weka.core.ContingencyTables, revision 1.7, by Eibe Frank
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) - original code
 * @author Fran Supek (fran.supek[AT]irb.hr) - adapted code
 * @version 0.9
 */
public class SplitCriteria {
  
  
  /**
   * Similar to weka.core.ContingencyTables.entropyConditionedOnRows.
   * 
   * Does not output entropy, output is modified to make routine faster:
   * the result is not divided by "total", as the total is a constant
   * in all operations (subtraction, comparison) performed as a part of
   * splitting in FastRfTree. Also, we don't have to divide by Math.log(2)
   * as the logarithms provided by fastLog2() are already base 2.
   * 
   * @param matrix the contingency table
   * @return the conditional entropy of the columns given the rows
   */
  public static double entropyConditionedOnRows(double[][] matrix) {

    double returnValue = 0, sumForBranch;
    //double total = 0;

    for (int branchNum = 0; branchNum < matrix.length; branchNum++) {
      sumForBranch = 0;
      for (int classNum = 0; classNum < matrix[0].length; classNum++) {
        returnValue = returnValue + lnFunc(matrix[branchNum][classNum]);
        sumForBranch += matrix[branchNum][classNum];
      }
      returnValue = returnValue - lnFunc(sumForBranch);
      // total += sumForRow;
    }

    //return -returnValue / (total * log2);
    return -returnValue; 
         
  }

  

  /**
   * Similar to weka.core.ContingencyTables.entropyOverColumns
   * 
   * Does not output entropy, output is modified to make routine faster:
   * the result is not divided by "total", as the total is a constant
   * in all operations (subtraction, comparison) performed as a part of
   * splitting in FastRfTree. Also, we don't have to divide by Math.log(2)
   * as the logarithms provided by fastLog2() are already base 2.
   *   
   * @param matrix the contingency table
   * @return the columns' entropy
   */
  public static double entropyOverColumns(double[][] matrix) {
    
    //return ContingencyTables.entropyOverColumns(matrix);
    
    double returnValue = 0, sumForColumn, total = 0;

    for (int j = 0; j < matrix[0].length; j++) {
      sumForColumn = 0;
      for (int i = 0; i < matrix.length; i++) {
        sumForColumn += matrix[i][j];
      }
      returnValue -= lnFunc(sumForColumn);
      total += sumForColumn;
    }

    //return (returnValue + lnFunc(total)) / (total * log2);
    return (returnValue + lnFunc(total)); 
     
  }

  
  
  /**
   * A fast approximation of log base 2, in single precision. Approximately
   * 4 times faster than Java's Math.log() function.
   * 
   * Inspired by C code by Laurent de Soras:
   * http://www.flipcode.com/archives/Fast_log_Function.shtml
   */
   public static float fastLog2( float val ) {
     
     int bits = Float.floatToIntBits(val);
     
     final int log_2 = ( (bits >> 23) & 255) - 128;
     bits &= ~(255 << 23);
     bits += 127 << 23;
     
     val = Float.intBitsToFloat(bits);
     
     val = ((-1.0f/3) * val + 2) * val - 2.0f/3;
     return (val + log_2);

   }
  
  
  
  /**
   * Help method for computing entropy.
   */
  private static double lnFunc(double num) {

    if (num <= 1e-6) {
      return 0;
    } else {
      return num * fastLog2( (float) num );
      //return num * Math.log( num );
    }
    
  }

  

  
  
}
