package VolumeJ;
import java.awt.*;
import volume.*;

/**
 * VJRender class. This subclass of VJRenderer implements the standard classical renderer,
 * and implements polymorphic object-ordered volume rendering.<br>
 * It implements Phong type gradient interpolation (i.e. the gradients are interpolated at the surface), which is superior
 * over Gouraud shading and does'nt make much difference in speed.
 * Note that the of shading can be anything including Phong shading.
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
public class VJRender extends VJRenderer
{
        /**
         * Create a new classic renderer with specified methods.
         * @param interpolator the interpolator that will interpolate VJValues from the voluem to be rendered.
         * @param shader the VJShader that will be used for shading.
         * @param classifier the VJClassifier that will be used for classifying.
         * @throws Exception if parameters not properly defined.
         */
        public VJRender(VJInterpolator interpolator, VJShader shader, VJClassifier classifier)
        throws Exception
        {
                super(interpolator, shader, classifier);
        }
        /**
         *  Objectspace rendering method.
         *  Renders v as an image. The type of image depends on the capabilities of the classifier
         *  , that is inherited from VJRenderer.
         *  This is a general renderer. It can process interpolated voxels of class
         *  VJValue, and these voxels are passed to a classifier and a composer.
         *  Of course, the VJInterpolator, the VJClassifier, the VJShader and the composer will need to be able
         *  to produce and process the needed type of VJValue.
         *  Voxels can be scalars or indexed scalars (implemented here).
         *  For more sophisticated voxels, such as
         *  vectors, including flow vectors this class can be subclassed as needed.
         *  Class variables are all inherited from VJRenderer
        */
        public synchronized void run()
        {
		VJUserInterface.status("starting VJRender");
                running = true;
                if (classifier.does() == VJClassifier.RGB)
                        setOutputColor();
                newViewportBuffer();
                // Get the third column vector of the inverse matrix.
                // This contains the deltas for the ray stepper.
                float [] osstep = mi.getStepperColumn();
                // prepare a cutout
                if (cutout instanceof VJCutout)
                        cutout.setup(m, mi);
                long start = System.currentTimeMillis();
                VJValue value = null;
                if (outputType == COLORINT && v instanceof VolumeRGB)
                        value = new VJValueHSB();
                else
                        value = new VJValue();
                for (int j = 0; j < height && running; j++) // step in j direction (y on image).
                {
                        VJUserInterface.status("Render "+message+"("+(100*j)/height+"%)...");
                        VJUserInterface.progress((float)j/(float) height);
                        for (int i = 0; i < width; i++) // step in i direction (x on image).
                        {
                                // Viewspace location vector.
                                float [] vsv = VJMatrix.newVector(i+ioffset, j+joffset, koffset); // start of ray.
                                // Get the start of the ray in objectspace.
                                VJVoxelLoc vl = new VJVoxelLoc(vsv, mi);
                                //if (i==0 && j==0) VJUserInterface.write("Viewspace start "+vsv[0]+","+vsv[1]+","+vsv[2]+","+" objectspace start "+vl);
                                // Optimization.
                                boolean involume = false;
                                // Initialize the intermediate composite.
                                VJAlphaColor pixel;
                                if (classifier.does() == VJClassifier.RGB)
                                        pixel = new VJAlphaColor(0, 0, 0, 0);
                                else
                                        pixel = new VJAlphaColor(0, 0);
                                // Step through the ray.
                                for (int k = 0; k < depth; k++)
                                {
                                        if (onTrace(i, j))
                                                trace(""+i+","+j+" k: "+k+"("+pixel+")"+" inspect: "+vl.ix+","+vl.iy+","+vl.iz);
                                        // check if ray within volume and opacity < 1.
                                        if (pixel.notOpaque() && interpolator.isValid(vl, v))
                                        {
                                                 // Interpolate a voxel value.
                                                interpolator.value(value, v, vl);
                                                // If value is RGB vector get RGB info.
                                                if (value instanceof VJValueHSB)
                                                        interpolator.valueHS((VJValueHSB) value, (VolumeRGB) v, classifier.getThreshold(), vl);
                                                // Remember the depth.
                                                value.k = k;
                                                if (onTrace(i, j)) trace(" value "+value);
                                                // Check whether this voxel can be skipped (if indexing on
                                                // and the classifier does not find the index interesting).
                                                if (interpolator.isValidGradient(vl, v) && (! classifier.doesIndex() || classifier.visible(value)))
                                                {
                                                        // Phong: interpolate the gradient.
                                                        VJGradient g = interpolator.gradient(v, vl);
                                                        // Classify voxel and gradient.
                                                        VJAlphaColor color = classifier.alphacolor(value, g);
                                                        if (onTrace(i, j))
                                                                trace(classifier.trace(value, g)
                                                                        +" pixel: "+pixel.toString()+" color: "+color.toString());
                                                        if (color.visible())
                                                        {
                                                                // Normalize the gradient.
                                                                g.normalize();
                                                                // Shade.
                                                                VJShade shade = shader.shade(g);
                                                                // Compose the alphacolor into pixel
                                                                blendCompose(pixel, value, g, color, shade);
                                                                // Early ray termination
                                                                if (pixel.almostOpaque()) pixel.setOpaque();
                                                                // Check for cutouts.
                                                                if (cutout instanceof VJCutout)
                                                                        cutout.cutout(pixel, i+ioffset, j+joffset, k+koffset);
                                                                if (onTrace(i, j))
                                                                        trace(" "+shade.toString()+" pixel: "+pixel.toString()+"\n");
                                                        }
                                                        involume = true;
                                                }
                                        }	// within volume data
                                        else if (involume)
                                        {
                                                if (onTrace(i, j))
                                                {
                                                        trace("break?");
                                                        traceWrite();
                                                }
                                                break;	// simple optimization
                                        }
                                        if (onTrace(i, j))
                                                traceWrite();
                                        // proceed along k-ray in objectspace.
                                        vl.move(osstep);
                                }	// k step
                                // Set the pixel in the viewport buffer.
                                setPixel(pixel, i, j);
                        }	// i step
                        yield();
                }	// j step
                VJUserInterface.progress(1f);
                pixelms = (float) (System.currentTimeMillis() - start) / (float) (width * height);
                running = false;
        }
        /**
         * Compose the classified voxel color into pixel.
         * Link to VJAlphaColor.blendComposeScalar() so that this method can be overloaded.
         * @param pixel contains the current composite
         * @param value contains the current voxel (needed by some subclasses).
         * @param gradient the interpolated gradient for shading
         * @param color contains the alpha value and color (grayscale or RGB) of the classified value.
         * @param shade is the effect of shading.
	  */
        protected void blendCompose(VJAlphaColor pixel, VJValue value, VJGradient g, VJAlphaColor color, VJShade shade)
        {
                pixel.blendComposeScalar(color, shade);
        }
        public static String desc() { return "Raytrace"; }
}

