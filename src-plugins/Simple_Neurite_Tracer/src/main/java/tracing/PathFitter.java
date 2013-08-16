/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2011 Mark Longair */

/*
  This file is part of the ImageJ plugin "Simple Neurite Tracer".

  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  In addition, as a special exception, the copyright holders give
  you permission to combine this program with free software programs or
  libraries that are released under the Apache Public License.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import java.awt.Color;
import java.awt.Graphics;

import ij.gui.*;
import ij.*;
import ij.process.*;
import ij.measure.Calibration;

import pal.math.*;

import stacks.ThreePanes;

import ij3d.Image3DUniverse;
import ij3d.Content;
import ij3d.Pipe;
import ij3d.MeshMaker;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;

import java.util.concurrent.Callable;

/* This class represents a list of points, and has methods for drawing
 * them onto ThreePanes-style image canvases. */

public class PathFitter implements Callable<Path> {

	protected SimpleNeuriteTracer plugin;
	protected int fitterIndex;
	protected Path path;
	protected MultiTaskProgress progress;
	protected boolean showDetailedFittingResults;
	protected boolean succeeded;

	public boolean getSucceeded() {
		return succeeded;
	}

	public PathFitter( SimpleNeuriteTracer plugin, Path path, boolean showDetailedFittingResults ) {
		this.plugin = plugin;
		this.path = path;
		this.fitterIndex = -1;
		this.progress = null;
		this.showDetailedFittingResults = showDetailedFittingResults;
		if( path.isFittedVersionOfAnotherPath() )
			throw new RuntimeException("BUG: trying to fit a fitted path");
	}

	public void setProgressCallback( int fitterIndex, MultiTaskProgress progress ) {
		this.fitterIndex = fitterIndex;
		this.progress = progress;
	}

	public Path call() throws Exception {
		Path fitted = path.fitCircles( 40, plugin.getImagePlus(), showDetailedFittingResults, plugin, fitterIndex, progress );
		if( fitted == null ) {
			succeeded = false;
			return null;
		} else {
			succeeded = true;
			path.setFitted(fitted);
			path.setUseFitted(true, plugin);
			return fitted;
		}
	}

}
