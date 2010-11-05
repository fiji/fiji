/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* This is a cut-down version of the TrakEM2 VectorString3D class. */

/**

TrakEM2 plugin for ImageJ(C).
Copyright (C) 2005, 2006 Albert Cardona and Rodney Douglas.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

In addition, as a special exception, the copyright holders give
you permission to combine this program with free software programs or
libraries that are released under the Apache Public License.

You may contact Albert Cardona at acardona at ini.phys.ethz.ch
Institute of Neuroinformatics, University of Zurich / ETH, Switzerland.
**/

package ij3d;

import java.util.List;
import java.util.ArrayList;

import javax.vecmath.Point3f;

import Jama.Matrix;

import ij.IJ;

class Utils {

	/** Will make a new double[] array, then fit in it as many points from the given array as possible according to the desired new length. If the new length is shorter that a.length, it will shrink and crop from the end; if larger, the extra spaces will be set with zeros. */
	static final double[] copy(final double[] a, final int new_length) {
		final double[] b = new double[new_length];
		final int len = a.length > new_length ? new_length : a.length;
		System.arraycopy(a, 0, b, 0, len);
		return b;
	}
}
