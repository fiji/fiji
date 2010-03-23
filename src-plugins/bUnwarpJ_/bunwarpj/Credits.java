
package bunwarpj;

/**
 * bUnwarpJ plugin for ImageJ and Fiji.
 * Copyright (C) 2005-2010 Ignacio Arganda-Carreras and Jan Kybic 
 *
 * More information at http://biocomp.cnb.csic.es/%7Eiarganda/bUnwarpJ/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

import ij.IJ;
/**
 * Class to show the bUnwarpJ credits
 */
public class Credits implements ij.plugin.PlugIn
{ /* begin class Credits */

	static public String version = "2.6 03-22-2010";

	/*....................................................................
       Public methods
    ....................................................................*/

	/*------------------------------------------------------------------*/
	/**
	 * Launch the credits plugin
	 *
	 * @param arg thread arguments
	 */
	public void run(String arg) 
	{
		String text = new String("");
		text += "\n";
		text += " bUnwarpJ " + Credits.version + "\n";
		text += " Consistent and elastic pairwise image registration (B-spline image and deformation representation).\n";
		text += " \n";
		text += " The work is based on the paper:\n";
		text += " \n";
		text += "\n Ignacio Arganda-Carreras, Carlos O. S. Sorzano, Roberto Marabini, Jose M. Carazo,\n" +
		" Carlos Ortiz de Solorzano, and Jan Kybic. 'Consistent and Elastic Registration of Histological\n" +
		" Sections using Vector-Spline Regularization'. Lecture Notes in Computer Science, Springer\n" +
		" Berlin / Heidelberg, Volume 4241/2006, CVAMIA: Computer Vision Approaches to Medical\n" +
		" Image Analysis, Pages 85-95, 2006.\n";
		text += "\n";

		IJ.showMessage("bUnwarpJ", text);
	}


} /* end class Credits */