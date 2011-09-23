/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007, 2008, 2009, 2010, 2011 Mark Longair */

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

import java.awt.Graphics;
import java.io.File;
import java.util.ArrayList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import ij.IJ;

public class TubularGeodesicsTracer extends Thread implements SearchInterface {

	public TubularGeodesicsTracer( File oofFile,
				float start_x_image,
				float start_y_image,
				float start_z_image,
				float end_x_image,
				float end_y_image,
				float end_z_image,
				double x_spacing,
				double y_spacing,
				double z_spacing,
				String spacing_units
		) {

		this.oofFile = oofFile;
		this.start_x_image = start_x_image;
		this.start_y_image = start_y_image;
		this.start_z_image = start_z_image;
		this.end_x_image = end_x_image;
		this.end_y_image = end_y_image;
		this.end_z_image = end_z_image;
		this.x_spacing = x_spacing;
		this.y_spacing = y_spacing;
		this.z_spacing = z_spacing;
		this.spacing_units = spacing_units;
	}

	protected double x_spacing;
	protected double y_spacing;
	protected double z_spacing;
	protected String spacing_units;

	protected File oofFile;

	protected float start_x_image;
	protected float start_y_image;
	protected float start_z_image;

	protected float end_x_image;
	protected float end_y_image;
	protected float end_z_image;

	protected PathResult lastPathResult;

	@Override
	public Path getResult() {
		if (! lastPathResult.getSuccess())
			return null;

		float [] points = lastPathResult.getPath();
		int numberOfPoints = lastPathResult.getNumberOfPoints();

		Path realResult = new Path(x_spacing,
					   y_spacing,
					   z_spacing,
					   spacing_units);
		realResult.createCircles();
		for( int i = 0; i < numberOfPoints; ++i ) {
			int start = i * 4;
			realResult.addPointDouble( points[start],
						   points[start+1],
						   points[start+2] );
			realResult.radiuses[i] = points[start+3];
			System.out.println("point "+i+" is "+points[start]+", "+points[start+1]+", "+points[start+2]+", "+points[start+3]);
		}
		realResult.setGuessedTangents(2);
		return realResult;
	}

	@Override
	public void drawProgressOnSlice( int plane,
					 int currentSliceInPlane,
					 TracerCanvas canvas,
					 Graphics g ) {
	}

	protected ArrayList< SearchProgressCallback > progressListeners =
		new ArrayList< SearchProgressCallback >();

	public void addProgressListener( SearchProgressCallback callback ) {
		progressListeners.add( callback );
	}

	@Override
	public void requestStop() {
		// FIXME: should probably add a "stoppable" query method to SearchInterface
		throw new RuntimeException("TubularGeodesicsTracer:requestStop: Not implemented yet...");
	}

	@Override
	public void run( ) {

		try {

			float [] p1 = new float[3];
			float [] p2 = new float[3];

			p1[0] = start_x_image;
			p1[1] = start_y_image;
			p1[2] = start_z_image;

			p2[0] = end_x_image;
			p2[1] = end_y_image;
			p2[2] = end_z_image;

			PathResult result = new PathResult();

			// Call the JNI here:

			ClassLoader loader = IJ.getClassLoader();
			if (loader == null)
				throw new RuntimeException("IJ.getClassLoader() failed (!)");

			try {

				/* Unfortunately, we can't be sure that the tubularity plugin
				   will be available at compile- or run-time, so we have to
				   try to load it via reflection. */

				Class<?> c = loader.loadClass("FijiITKInterface.TubularGeodesics");
				Object newInstance = c.newInstance();

				Class [] parameterTypes = { String.class,
							    float[].class,
							    float[].class,
							    PathResult.class };

				Method m = c.getMethod( "getPathResult", parameterTypes );
				Object [] parameters = new Object[4];
				parameters[0] = oofFile.getAbsolutePath();
				parameters[1] = p1;
				parameters[2] = p2;
				parameters[3] = result;

				m.invoke(newInstance,parameters);

				System.out.println("finished running the ITK code");

			} catch (IllegalArgumentException e) {
				reportFinished(false);
				throw new RuntimeException("There was an illegal argument when trying to invoke getPathResult: " + e);
			} catch (InvocationTargetException e) {
				reportFinished(false);
				Throwable realException = e.getTargetException();
				throw new RuntimeException("There was an exception thrown by getPathResult: " + realException);
			} catch (ClassNotFoundException e) {
				reportFinished(false);
				throw new RuntimeException("The FijiITKInterface.TubularGeodesics class was not found: " + e);
			} catch (InstantiationException e) {
				reportFinished(false);
				throw new RuntimeException("Failed to instantiate the FijiITKInterface.TubularGeodesics object: " + e);
			} catch ( IllegalAccessException e ) {
				reportFinished(false);
				throw new RuntimeException("IllegalAccessException when trying to create an instance of FijiITKInterface.TubularGeodesics: "+e);
			} catch (NoSuchMethodException e) {
				reportFinished(false);
				throw new RuntimeException("There was a NoSuchMethodException when trying to invoke getPathResult: " + e);
			} catch (SecurityException e) {
				reportFinished(false);
				throw new RuntimeException("There was a SecurityException when trying to invoke getPathResult: " + e);
			}

			System.out.println("before checking for success");

			if(!result.getSuccess()) {
				reportFinished( false );
				throw new RuntimeException("getPathResult failed, reporting the error: "+result.getErrorMessage());
			}

			lastPathResult = result;

			System.out.println("about to call reportFinished, with "+progressListeners.size()+" listeners");

			reportFinished( true );

		} catch( Throwable t ) {
			System.out.println("Got an exception from call to ITK code: "+t);
			t.printStackTrace();
			IJ.error("There was an error in calling to ITK code: "+t);
		}
	}

	public void reportFinished( boolean success ) {
		for( SearchProgressCallback progress : progressListeners )
			progress.finished( this, success );
	}

}
