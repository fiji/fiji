package VolumeJ;
import java.awt.*;
import volume.*;

/**
 * VJRenderer class implements the context and support methods for Volume Rendering.
 * Subclasses of VJRenderer have to implement the actual run() method.
 * Available for the run() method by default:
 * <pre>
 * shader
 * classifier
 * interpolator
 * m and mi, the transformation matrix and its inverse of the required view.
 * v the Volume that is to be rendered
 * width, height, depth the size of the volume in viewspace.
 * ioffset, joffset, koffset the offsets of the volume in viewspace.
 * pixels the viewport buffer
 * </pre>
 *
 * Copyright (c) 1999-2002, Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff
 *
 * Small print:
 * Permission to use, copy, modify and distribute this version of this software or any parts
 * of it and its documentation or any parts of it ("the software"), for any purpose is
 * hereby granted, provided that the above copyright notice and this permission notice
 * appear intact in all copies of the software and that you do not sell the software,
 * or include the software in a commercial package.
 * The release of this software into the public domain does not imply any obligation
 * on the part of the author to release future versions into the public domain.
 * The author is free to make upgraded or improved versions of the software available
 * for a fee or commercially only.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 */
public abstract class VJRenderer extends Thread
{
        /**
         * Defines type of image the rendering is output to: grayscale.
         */
        protected final static int       GRAYBYTE = 1;
        /**
         * Defines type of image the rendering is output to: color (as int).
         */
        protected final static int       COLORINT = 4;

        protected boolean               doPerspective = false;
        protected boolean               doDepthCueing = false;
        /**
         * Type of image output by this renderer: GRAYBYTE or COLORINT.
         */
        protected int                   outputType;

        /**
         * Amount of ms per pixel spent in rendering loop.
         */
        protected double                pixelms;
        protected int                   sequenceNumber;
        /**
         * Flag indicating whether this renderer is running.
        */
        protected boolean               running;
        /** Transformation matrix and inverse. */
        protected VJMatrix     		m, mi;
        /** Separate transformation matrix and inverse for the light. */
        protected VJMatrix     		mLight, miLight;
        /** Voxel classifier. */
        protected VJClassifier          classifier;
        /** Voxel interpolator. */
        protected VJInterpolator        interpolator;
        /** Surface shader. */
        protected VJShader              shader;
        /** Image buffer, is a flat array of int's or byte's, dependent on outputType. */
        protected Object                pixels;
        protected VJCutout		cutout;
        /** Volume to be rendered (polymorphic). */
        protected Volume                v;
        /** Boundaries of viewspace volume (the viewport) in k,j,i space. */
        protected int                   width, height, depth;
        /** Start of rays in viewspace. */
        protected int                   ioffset, joffset, koffset;
        protected String                message;

        /** Pixel tracing variables. */
        protected boolean	        doPixeltracing;
        protected int		        tracei, tracej;
        protected String	        traceString;
        protected String	        description;

        /**
         * Create a new default renderer with specified interpolator, shader and classifier.
         * This renderer does nothing, since run() is not implemented.
         * @param interpolator the interpolator that will interpolate VJValues from the voluem to be rendered.
         * @param shader the VJShader that will be used for shading.
         * @param classifier the VJClassifier that will be used for classifying.
         * @throws IllegalArgumentException if parameters not properly defined.
         */
        public VJRenderer(VJInterpolator interpolator, VJShader shader, VJClassifier classifier)
        throws IllegalArgumentException
        {
                if (! (interpolator instanceof VJInterpolator && shader instanceof VJShader && classifier instanceof VJClassifier))
                        throw new IllegalArgumentException("not proper parameters");
                this.interpolator = interpolator;
                this.shader = shader;
                this.classifier = classifier;
                this.message = "";
                this.outputType = GRAYBYTE;
        }
        /**
         * Get a default viewport automatically.
         * @return int[2] the width and height of the viewport.
        */
        public int [] defaultViewport()
        {
                if (! (v instanceof Volume && m instanceof VJMatrix))
		        VJUserInterface.write("VJRenderer.defaultViewport error: volume and transformation  matrix need to be initialized.");
                // Create a viewport.
                int [] vp = VJViewspaceUtil.suggestViewport(v, m);
                setViewport(vp[0], vp[1]);
                return vp;
        }
        /**
         * Set the output to color.
        */
        public void setOutputColor()
        {
                outputType = COLORINT;
        }
        /**
         * Set the output to color.
        */
        public void setOutputGrayscale()
        {
                outputType = GRAYBYTE;
        }
        /**
         * Set the viewport to a specific size.
         * @param width the width of the viewport.
         * @param height the height of the viewport.
        */
        public void setViewport(int width, int height)
        {
		// Create a viewport.
                this.width = width;
                this.height = height;
		minmax();
        }
        protected Object newViewportBuffer()
        {
                if (outputType == COLORINT)
                        // Color RGB means int array.
                        pixels = (Object) new int[width * height];
                else
                        // Byte array.
                        pixels = (Object) new byte[width * height];
                return pixels;
        }
        /**
         * Put color into the pixel array at i,j.
         * @param pixel an int, which contains the composited pixel value.
         * @param i the i-position in the pixel array.
         * @param j the j-position in the pixel array.
        */
        protected void setPixel(int pixel, int i, int j)
        {
                if (pixels instanceof byte []) // grayscale
                        ((byte []) pixels)[j * width + i] = (byte) pixel;
                else // rgb
                        ((int []) pixels)[j * width + i] = pixel;
        }
        /**
         * Put the pixel into the pixel array at i,j.
         * @param pixel a VJAlphaColor, which contains the composited pixel value.
         * @param i the i-position in the pixel array.
         * @param j the j-position in the pixel array.
        */
        protected void setPixel(VJAlphaColor pixel, int i, int j)
        {
                if (pixels instanceof byte []) // grayscale
                        ((byte []) pixels)[j * width + i] = (byte) (((int) pixel.r) & 0xff);
                else // rgb
                {
                        int rvalue = pixel.getRed();
                        int gvalue = pixel.getGreen();
                        int bvalue = pixel.getBlue();
                        ((int []) pixels)[j * width + i] = (int) (rvalue<<16 | gvalue<<8 | bvalue);
                }
        }
        /**
         * Set the transformation matrix of the renderer to m.
         * This is used to transform object space coordinates to viewspace coordinates and vice-versa
         * for the desired view.
         * Keep a special transformation matrix and its inverse to compute the light position
         * (which should not be influenced by aspect ratio changes).
         * The volume needs to be set before calling this method.
         * Shader needs to be set before calling this method.
         * @param m the transformation matrix wanted.
        */
        public void setTransformation(VJMatrix m)
        {
                setTransformation(m, m);
        }
        /**
         * Set the transformation matrix of the renderer to m
         * and the special light transformation matrix to correct the light position (should not be influenced by
         * e.g. aspect ratios or other transformations that are in m).
         * This is used to transform object space coordinates to viewspace coordinates and vice-versa
         * for the desired view.
         * The volume needs to be set before calling this method.
         * Shader also needs to be set before calling this method.
         * A relatively expensive method since two inverses are computed.
         * @param m the transformation matrix wanted.
         * @param mLight the transformation matrix wanted for the light.
        */
        public void setTransformation(VJMatrix m, VJMatrix mLight)
        {
                this.m = m;
                this.mLight = mLight;
                // Compute the inverse of the transformation matrix.
                try
                {
                        mi = m.inverse();
                } catch (Exception e) { VJUserInterface.error(e.toString()); }
                // Compute the inverse of the light transformation matrix.
                miLight = mLight.inverse();
                // Correct opacities for the sampling interval.
                if (classifier instanceof VJClassifierLevoy)
				   ((VJClassifierLevoy) classifier).setupOpacities((float) mi.getOversamplingRatio());
                // Set the position of the light to objectspace coordinates.
                setShader(this.shader);
                if (width > 0 && height > 0)
                        minmax();
        }
        /**
         * Set the rendering message.
         * @param message a String used for status updates, error messages etc.
        */
        public void setMessage(String message)
        {
                this.message = message;
        }
        /**
         * The shader has changed. Reset the shader, objectify the light.
         * Overload if you have to recompute a shading table.
         * @param shader the new shader.
         */
        public void setShader(VJShader shader)
        {
                this.shader = shader;
                if (shader.getLight() instanceof VJLight)
                        shader.getLight().objectify(miLight);
                //VJUserInterface.write(shader.getLight().toLongString());
        }
        /**
         * Calculate the extents of the volume to be rendered in view and objectspace.
         * When you call this method, v, m and the viewport need to have been initialized!
        */
        protected void minmax()
        {
                // Determine viewspace volume extents.
                int [][] minmax = VJViewspaceUtil.minmax(v, m);
                // Center the view in viewspace.
                // The i (viewspace x) and j (viewspace y) offsets
                // relative to the center of the volume
                // relative to the center of the viewspace.
                ioffset = (minmax[0][0]+minmax[1][0])/2 - width/2;
                joffset = (minmax[0][1]+minmax[1][1])/2 - height/2;
                // set k offset and depth.
                koffset = minmax[0][2];
                depth = minmax[1][2] - minmax[0][2];
        }
        /**
        * To be overloaded, does nothing.
        * Should set <code>running</code> to true and check it.
        */
        public void run()
        {
        }
        /**
        * The kill method is called when this renderer is to stop.
        * You can add wrapup method calls in this method.
        */
        public void kill()
        {
	        running = false;
        }
        /**
         * The volume has changed. Reset the volume.
         * @param volume the new volume.
         */
        public void setVolume(Volume v) {   this.v = v; }
        /**
         * Get the viewport pixel buffer.
         * @return pixels a flat array of ibt or byte containing the RGB or monochrome pixel array resepctively.
         */
        public Object getPixels()
        {
                if (pixels == null)
                        VJUserInterface.write("VJRenderer subclass error: pixels == null");
                return pixels;
        }
        /**
         * Get the size of the viewport.
         * @return an int[2] containg the width and height of the viewport.
         */
        public int [] getViewport() { int [] vp = new int[2]; vp[0] = width; vp[1] = height; return vp; }
        /**
         * Get the width of the viewport.
         * @return the width of the viewport.
         */
        public int getViewportWidth() { return width; }
        /**
         * Get the height of the viewport.
         * @return the height of the viewport.
         */
        public int getViewportHeight() { return height; }
        /**
         * Get the volume in this renderer.
         * @return v, the Volume to be rendered.
         */
        public Volume getVolume() { return v; }
        public VJMatrix getTransformation() { return m; }
        public void setInterpolator(VJInterpolator interpolator) { this.interpolator = interpolator; }
        public VJInterpolator getInterpolator() { return interpolator; }
        public void setClassifier(VJClassifier classifier) { this.classifier = classifier; }
        public VJClassifier getClassifier() { return classifier; }
        public VJShader getShader() { return shader; }
        public void setCutout(VJCutout cutout) { this.cutout = cutout; }
        /**
         * Start pixeltracing for the i,j pixel in the viewport.
         * @param i the x coordinate of the pixel in the viewport buffer
         * @param j the y coordinate of the pixel in the viewport buffer
         */
        public void trace(int i, int j) { doPixeltracing = true; this.tracei = i; this.tracej = j; traceString = ""; }
        protected boolean onTrace(int i, int j) { return (doPixeltracing && i == tracei && j == tracej); }
        protected void trace(String s) { traceString += s+"\n"; }
        protected void traceWrite() { if (doPixeltracing && traceString.length() > 1) VJUserInterface.write(traceString); traceString = ""; }
        public double getTimePerPixel() { return pixelms; }
        public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }
        public void setDescription(String description) { this.description = description; }
}

