/*
 *
 */

package features;

import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.measure.Calibration;

/* For testing the hessianEigenvaluesAtPoint() method, essentially,
 * and experimenting with measures based on those eigenvalues. */

public class TestComputeCurvatures_ implements PlugIn, GaussianGenerationCallback {

    ImagePlus imp;

    public void run( String ignored ) {

        imp = WindowManager.getCurrentImage();

        // Just ignore the calibration information in this plugin:
        ComputeCurvatures c = new ComputeCurvatures( imp, 1.0, this, false );

        c.run();

        int width = imp.getWidth();
        int height = imp.getHeight();
        int depth = imp.getStackSize();

        System.out.println("w: "+width+", h: "+height+", d:" +depth);

        ImageStack stack_0 = new ImageStack( width - 2, height - 2 );
        ImageStack stack_1 = new ImageStack( width - 2, height - 2 );
        ImageStack stack_2 = new ImageStack( width - 2, height - 2 );

        ImageStack stack_s = new ImageStack( width - 2, height - 2 );
        ImageStack stack_m = new ImageStack( width - 2, height - 2 );
        ImageStack stack_d = new ImageStack( width - 2, height - 2 );

        double [] evalues = new double[3];

        Calibration calibration = imp.getCalibration();

        float sepX = 1, sepY = 1, sepZ = 1;
        if( calibration != null ) {
            sepX = (float)calibration.pixelWidth;
            sepY = (float)calibration.pixelHeight;
            sepZ = (float)calibration.pixelDepth;
        }

        for( int z = 1; z < (depth - 1); ++z ) {

            System.out.println( "Working on slice: "+z );

            float [] slice_0 = new float[ (width - 2) * (height - 2) ];
            float [] slice_1 = new float[ (width - 2) * (height - 2) ];
            float [] slice_2 = new float[ (width - 2) * (height - 2) ];

            float [] slice_s = new float[ (width - 2) * (height - 2) ];
            float [] slice_m = new float[ (width - 2) * (height - 2) ];
            float [] slice_d = new float[ (width - 2) * (height - 2) ];

            for( int y = 1; y < (height - 1); ++y ) {
                for( int x = 1; x < (width - 1); ++x ) {

                    boolean succeeded = c.hessianEigenvaluesAtPoint3D( x, y, z,
                                                                       true, // order absolute
                                                                       evalues,
                                                                       false,
                                                                       false,
                                                                       sepX,
                                                                       sepY,
                                                                       sepZ );

                    int index = (y - 1) * (width - 2) + (x - 1);

                    /* slice_0[index] = (float)evalues[0]; */ slice_0[index] = (float)Math.abs( evalues[0] );
                    /* slice_1[index] = (float)evalues[1]; */ slice_1[index] = (float)Math.abs( (evalues[1] > 0) ? 0: evalues[1] );
                    /* slice_2[index] = (float)evalues[2]; */ slice_2[index] = (float)Math.abs( (evalues[2] > 0) ? 0: evalues[2] );

                    if( (evalues[1] >= 0) || (evalues[2] >= 0) ) {

                        // If either of the two principle eigenvalues
                        // is positive then the curvature is in the
                        // wrong direction - towards higher
                        // instensities rather than lower.

                        slice_s[index] = 0;
                        slice_m[index] = 0;
                        slice_d[index] = 0;

                    } else {

                        /*
                        double diff = Math.abs(evalues[2]-evalues[1]);
                        slice_s[index] = (float)diff;
                        */

                        slice_s[index] = (float) Math.abs( evalues[2] * evalues[1] );
                        slice_m[index] = (float) Math.sqrt( Math.abs( evalues[2] * evalues[1] ) );
                        slice_d[index] = (float) Math.abs( evalues[2] * evalues[1] * evalues[1] );

                    }
                }
            }

            FloatProcessor fp0 = new FloatProcessor( width - 2, height - 2 );
            FloatProcessor fp1 = new FloatProcessor( width - 2, height - 2 );
            FloatProcessor fp2 = new FloatProcessor( width - 2, height - 2 );

            FloatProcessor fps = new FloatProcessor( width - 2, height - 2 );
            FloatProcessor fpm = new FloatProcessor( width - 2, height - 2 );
            FloatProcessor fpd = new FloatProcessor( width - 2, height - 2 );

            fp0.setPixels( slice_0 ); stack_0.addSlice( null, fp0 );
            fp1.setPixels( slice_1 ); stack_1.addSlice( null, fp1 );
            fp2.setPixels( slice_2 ); stack_2.addSlice( null, fp2 );

            fps.setPixels( slice_s ); stack_s.addSlice( null, fps );
            fpm.setPixels( slice_m ); stack_m.addSlice( null, fpm );
            fpd.setPixels( slice_d ); stack_d.addSlice( null, fpd );

            IJ.showProgress( z / (double)(depth - 2) );

        }

        IJ.showProgress( 1.0 );

        ImagePlus imp0 = new ImagePlus( "evalue 0 "+imp.getTitle(), stack_0 ); imp0.show();
        ImagePlus imp1 = new ImagePlus( "evalue 1 (abs for > 0) "+imp.getTitle(), stack_1 ); imp1.show();
        ImagePlus imp2 = new ImagePlus( "evalue 2 (abs for > 0) "+imp.getTitle(), stack_2 ); imp2.show();

        ImagePlus imps = new ImagePlus( "product of e1 and e2", stack_s ); imps.show();
        ImagePlus impm = new ImagePlus( "sqrt of product of e1 and e2", stack_m ); impm.show();
        ImagePlus impd = new ImagePlus( "product of e1, e1 and e2", stack_d ); impd.show();

    }

    public void proportionDone( double d ) {
        IJ.showProgress( d );
    }

}
