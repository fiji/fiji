package VolumeJ;
import java.awt.*;
import volume.*;

/**
 * VJViewspaceRender class. Implements viewspace rendering, where the volume is rendered
 * in the order of the pixels on the viewport. So for the whole viewport,
 * rays are cast into the volume for one k-step, and in the next step, the rays are advanced by one k-step.
 * This class implements polymorphic viewspace-ordered volume rendering.
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
public class VJViewspaceRender extends VJRenderer
{
        // Most variables are inherited from VJRenderer.
        /** The viewspace pixel buffer contains the rgb composites and the alpha for all pixels. */
        protected VJAlphaColor []     viewspacePixel;
        /**
        * The viewspace buffer contains the x, y, z, and the interpolation
        * coefficients steps for the next.
        */
        protected VJVoxelLoc []       viewspace;

        /** size of entry viewspacePixel. */
        protected int                 pixelSize = 2;

        // Java cannot stand too many variables on the stack.
        protected static float []    vsv;
        protected static float []    vsvstep;
        protected static float []    osv;

        /**
         * Create a new viewspace renderer with specified methods.
         * @param interpolator the interpolator that will interpolate VJValues from the voluem to be rendered.
         * @param shader the VJShader that will be used for shading.
         * @param classifier the VJClassifier that will be used for classifying.
         * @throws Exception if parameters not properly defined.
         */
        public VJViewspaceRender(VJInterpolator interpolator, VJShader shader, VJClassifier classifier)
        throws Exception
        {
                super(interpolator, shader, classifier);
                // default is grayscale rendering (depends on classifier).
                pixelSize = 2;
        }
        /**
         * Viewspace rendering.
         * Render v into a pixel array. The type of pixel array depends on the value of
         * pixelSize  (grayscale or RGB).
         * This is a general renderer. It can process interpolated voxels of type
         * Object, and these voxels are passed to a classifier and a composer.
         * Of course, interpolator, classifier and composer will need to be able
         * to produce and process the needed type of Object.
         * Thus, voxels can be scalars, indexed scalars (implemented here), or
         * vectors, such as RGB (implemented here) or flow vectors.
        */
        public synchronized void run()
        {
                running = true;

                // Correct opacities for the sampling interval.
                //classifier.setupOpacities((float) mi.getOversamplingRatio());
                // The viewspace voxel locator.
                viewspace = new VJVoxelLoc[height*width];
                // Create a new VJAlphaColor buffer for the viewport.
                viewspacePixel = new VJAlphaColor[height * width];
                // Initialize the viewspace matrix.
                for (int j = 0; j < height; j++)
                for (int i = 0; i < width; i++)
                {
                        // Determine what the classifier returns in terms of color and alpha.
                        if (classifier.does() == VJClassifier.RGB)
                                viewspacePixel[j*width+i] = new VJAlphaColor();
                        else
                                viewspacePixel[j*width+i] = new VJAlphaColor(0, 0);
                        // calculate the index in viewspace.
                        int index = j*width + i;
                        {
                                // the viewspace location.
                                float [] vsv = VJMatrix.newVector(i+ioffset, j+joffset, koffset); // start of ray.
                                // the objectspace location.
                                float [] osv = mi.mul(vsv); // calculate the start of this ray in objectspace.
                                viewspace[index] = new VJVoxelLoc();
                                // Set the viewspace entries to the object locations.
                                viewspace[index].x = (float) osv[0];  // object x
                                viewspace[index].y = (float) osv[1];  // object y
                                viewspace[index].z = (float) osv[2];  // object z
                        }
                }
                float [] dummy = new float[4];
                // Advance all rays to start of k-space.
                advanceAllRays(dummy);
                /*
                        Now get the third column vector of the inverse matrix.
                        This contains the deltas for the ray stepper.
                */
                vsvstep = mi.getStepperColumn();

                // prepare a cutout
                if (cutout instanceof VJCutout)
                        cutout.setup(m, mi);
                VJValue value = new VJValue();

                // Run through the rays.
                for (int k = 0; k < depth && running; k++) // step through volume along ray.
                {
                        VJUserInterface.status("Render "+message+"("+(100*k)/depth+"%)...");
                        VJUserInterface.progress((float)k/(float)depth);
                        // Traverse viewspace in viewspace order for these rays.
                        for (int index = 0; index < height * width; index++)
                        {
                                //if (doPixeltracing && i == tracei && j == tracej)
                                //		  traceString = ""+i+","+j+","+k;
                                // check if ray within volume and opacity < 1.
                                if (interpolator.isValid(viewspace[index], v)
                                        && viewspacePixel[index].notOpaque())
				{
					// Get the interpolated voxel value or values (vector).
                                        interpolator.value(value, v, viewspace[index]);
                                        /*
                                          Check whether this voxel can be skipped (if indexing on
                                          and the classifier does not find the index interesting).
                                        */
                                        if (interpolator.isValidGradient(viewspace[index], v)
                                                && (! classifier.doesIndex() || classifier.visible(value)))
                                        {
                                                // Do not skip.
                                                // Get the interpolated normalized gradient for this voxel.
                                                VJGradient g = interpolator.gradient(v, viewspace[index]);
                                                // Classify on the interpolated value(s), index and gradient.
                                                VJAlphaColor color = classifier.alphacolor(value, g);
                                                if (color.visible())
                                                {
                                                        // shade.
                                                        g.normalize();
                                                        VJShade shade = shader.shade(g);
                                                        // compose into pac.
                                                        blendCompose(index, value, g, color, shade);
                                                        // early ray termination
                                                        if (viewspacePixel[index].almostOpaque())
                                        			viewspacePixel[index].setOpaque();
                                                        // check for cutouts.
                                                        //if (cutout instanceof VJCutout)
                                                        //		cutout.cutout(pixel, i+ioffset, j+joffset, k+koffset);
                                                        //if (doPixeltracing && i == tracei && j == tracej)
                                                        //		traceString += ": shading: "+IJ.d2s(attenuation,3);
                                                }
                                        } // if do not skip.
                                }	// if within image.
                        } // for index
                        // What do you think it does?
                        advanceAllRays(vsvstep);
                        yield();
                } // for k
                // Convert the viewport VJAlphaColor buffer to pixels, the pixel buffer.
                makePixels();
                running = false;
        }
        /**
         * Compose the classified voxel color into pixel.
         * Link to VJAlphaColor.blendComposeScalar() so that this method can be overloaded.
         * @param index the index of the current composited pixel (so far).
         * @param value the voxel VJValue for the current voxel (needed by some subclasses).
         * @param g the interpolated gradient for shading
         * @param color contains the alpha value and the color (grayscale or RGB) of the classified values.
         * @param shade is the effect of shading.
	  */
        protected void blendCompose(int index, VJValue value, VJGradient g, VJAlphaColor color, VJShade shade)
        {
                viewspacePixel[index].blendComposeScalar(color, shade);
        }
        /**
         * Conversion of the viewspace VJAlphaColor buffer into an RGB or byte pixels array (an array of ints or bytes).
         * @return pixels the pixel array as an Object
        */
        private Object makePixels()
        {
                // Create new pixels array or correct type.
                newViewportBuffer();
                if (pixels instanceof int [])
                {
                        for (int index = 0; index < height * width; index++)
                        {
                                /*----- Set viewspace pixel value -----*/
                                int rvalue = viewspacePixel[index].getRed();
                                int gvalue = viewspacePixel[index].getGreen();
                                int bvalue = viewspacePixel[index].getBlue();
                                if (rvalue > 255 || gvalue > 255 || bvalue > 255)
                                        VJUserInterface.write("Had composite overflow");
                                ((int []) pixels)[index] = (int) (rvalue<<16 | gvalue<<8 | bvalue);
                        }
                }
                else
                {
                        for (int index = 0; index < height * width; index++)
                        {
                                /*----- Set viewspace pixel value -----*/
                                int value = viewspacePixel[index].getValue();
                                if (value > 255)
                                VJUserInterface.write("Composite overflow");
                                ((byte []) pixels)[index] = (byte) value;
                        }
                }
                return (Object) pixels;
        }
        /**
         * Advance all rays of the viewspace. vectorstep contains the amount in x,y,z.
         * I wouldnt overload this method.
        */
        private void advanceAllRays(float [] vsvstep)
        {
                for (int index = 0; index < height * width; index ++)
                {
                        // advance along k in direction of ray.
                        viewspace[index].move(vsvstep);
                }
        }
        public static String desc() { return "viewspace"; }
}

