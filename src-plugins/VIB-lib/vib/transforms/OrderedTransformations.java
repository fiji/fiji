/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* 
 * This class is to chain together transformations which can't
 * easily be collapsed together.
 * 
 */

package vib.transforms;

import ij.IJ;
import ij.ImageJ;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ByteProcessor;
import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.PlugInFilter;
import ij.WindowManager;

import math3d.Point3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import vib.FastMatrix;

/* This class contains a sequence of transformations... */

public class OrderedTransformations {
	
	private ArrayList<Transform> listOfTransforms;
	
	public Object clone() {
		OrderedTransformations result=new OrderedTransformations();
		
		ArrayList<Transform> clonedList=(ArrayList<Transform>)this.listOfTransforms.clone();
		
		result.listOfTransforms=clonedList;
		return result;
	}
	
	public OrderedTransformations() {
		listOfTransforms = new ArrayList<Transform>();
	}

    /*
    public void add( FastMatrix f ) {
        listOfTransforms.add(f);
    }
    */

    /*
    public void add( Bookstein b ) {
        listOfTransforms.add(b);
    }
    */
	
	public void addFirst( Transform t ) {
		listOfTransforms.add(0,t);
	}
	
	public void addLast( Transform t ) {
		listOfTransforms.add(t);
	}
	
	public void addFirst( OrderedTransformations o ) {
		
		int j=0;
		
		for( Iterator i = o.listOfTransforms.iterator(); i.hasNext(); ) {
			Transform f = (Transform)i.next();
			listOfTransforms.add(j,f);
			++j;
		}
		
	}
	
	public void addLast( OrderedTransformations o ) {
		
		for( Iterator i = o.listOfTransforms.iterator(); i.hasNext(); ) {
			Transform f = (Transform)i.next();
			addLast(f);
		}
		
	}
	
	public int number( ) {
		return listOfTransforms.size();
	}
	
	public Transform getComponentTransform( int i ) {
		return (Transform)listOfTransforms.get(i);
	}
	
	public String toString( ) {
		
		String result = "";
		
		int j = 0;
		for( Iterator i = listOfTransforms.iterator(); i.hasNext(); ++j ) {
			Transform f = (Transform)i.next();
			result += "Transformation " + j + " is:\n";
			result += f.toStringIndented( "   " );
		}
		
		return result;
	}
	
	// FIXME: buggy in the case when you have Bookstein followed by FastMatrix
	
	public void reduce( ) {
		
		Iterator i;
		
		// Remove any identities...
		
		for( i = listOfTransforms.iterator(); i.hasNext(); ) {
			Transform f = (Transform)i.next();
			if( f.isIdentity() )
				i.remove();
		}
		
		// Try to compose each transformation in the list:
		
		ArrayList<Transform> newList = new ArrayList<Transform> ();
		
		Transform last = null;
		
		for( i = listOfTransforms.iterator(); i.hasNext(); ) {
			
            /* Each time we enter this loop:

                - If last is null, then we should get the next matrix
                  and start with that.  (This happens the first time
                  through only.)

                - If last is not null, then we should try to compose
                  the succeeding matrices with it.  (This happens if
                  we've tried a composition and it failed.)
            */

			if( last == null )
				last = (Transform)i.next();
			
			while( i.hasNext() ) {
				
				Transform next = (Transform)i.next();
				
				Transform compositionResult = last.composeWith( next );
				
				if( compositionResult == null ) {
					
					// Then fix up and break...
					
					newList.add( last );
					last = next;
					break;
					
				} else {
					
					// The move the result to last, and try composing
					// the next one.
					
					last = compositionResult;
					
				}
				
			}
			
			// We can get here by running out of elements, or a
			// composition failing.  (In the latter case, we've
			// already added the newly composed bit.)
			
			// If we get to here because there's nothing left in the
			// list, then we should just add last.
			
			if( ! i.hasNext() ) {
				if( last != null )
					newList.add( last );
			}
			
		}
		
		listOfTransforms = newList;
		
	}
	
	public OrderedTransformations inverse( ) {
		
		ArrayList newList = new ArrayList<Transform>();
		
		ListIterator i;
		
		// Move to the end of the list...
		
		for( i = listOfTransforms.listIterator(); i.hasNext(); i.next() )
			;
		
		// Step back through the list, inverting them and adding to
		// the new list.
		
		while( i.hasPrevious() ) {
			
			Transform f = (Transform)i.previous();
			
			Transform f_inverted = f.inverse();
			
			newList.add( f_inverted );
			
		}
		
		OrderedTransformations result = new OrderedTransformations();
		
		result.listOfTransforms = newList;
		
		// System.out.println("inverted transform is:\n"+result.toString());
		
		return result;
	}
	
	public void apply( double x, double y, double z, double [] result ) {
		
		for( Transform f : listOfTransforms ) {
			f.apply( x, y, z, result );
			x = result[0];
			y = result[1];
			z = result[2];
		}
	}
	
	public double [] apply( double x, double y, double z ) {
		
		double [] result = new double[3];
		
		for( Transform f : listOfTransforms ) {
			f.apply( x, y, z, result );
			x = result[0];
			y = result[1];
			z = result[2];
		}
		
		return result;
	}
	
	public Point3d apply( Point3d p ) {
		
		double [] result = apply( p.x, p.y, p.z );
		return new Point3d( result[0], result[1], result[2] );
	}
	
/*
	public double scoreTransformationSmoothed( ImagePlus image0,
						   ImagePlus image1,
						   int skipPixelsInTemplate ) {
		
		Blur_CurrentImage filter=new Blur_CurrentImage();
		filter.blur(8,image0);
		filter.blur(8,image1);
		
		return scoreTransformation(image0,image1,skipPixelsInTemplate);
	}
	
	public double scoreTransformationEqualizedAndSmoothed( ImagePlus image0,
							       ImagePlus image1,
							       int skipPixelsInTemplate ) {
		
		Equalize_CurrentImage equalizer=new Equalize_CurrentImage();
		equalizer.equalize(image0);
		equalizer.equalize(image1);
		
		Blur_CurrentImage filter=new Blur_CurrentImage();
		filter.blur(8,image0);
		filter.blur(8,image1);
		
		return scoreTransformation(image0,image1,skipPixelsInTemplate);
	}
*/
	
	public double scoreTransformation( ImagePlus image0,
					   ImagePlus image1,
					   int skipPixelsInTemplate ) {
		return scoreTransformationReal( image0, image1, skipPixelsInTemplate, null );
	}
	
	public double scoreTransformation( ImagePlus image0,
					   ImagePlus image1,
					   BoundsInclusive boundsInclusive ) {
		return scoreTransformationReal( image0, image1, 0, boundsInclusive );
	}
	
	public double scoreTransformation( ImagePlus image0,
					   ImagePlus image1,
					   int skipPixelsInTemplate,
					   BoundsInclusive boundsInclusive ) {
		
		return scoreTransformationReal(image0,image1,skipPixelsInTemplate,boundsInclusive);
	}
	
	public double scoreTransformationReal( ImagePlus image0,
					       ImagePlus image1,
					       int skipPixelsInTemplate,
					       BoundsInclusive boundsInclusive ) {
		
		OrderedTransformations invertedTransform = inverse();
		
		ImageStack stack0 = image0.getStack();
		ImageStack stack1 = image1.getStack();
		
		int d0 = stack0.getSize();
		int h0 = stack0.getHeight();
		int w0 = stack0.getWidth();
		
		int d1 = stack1.getSize();
		int h1 = stack1.getHeight();
		int w1 = stack1.getWidth();
		
		int xmin = 0;
		int ymin = 0;
		int zmin = 0;
		
		int xmax = w0 - 1;
		int ymax = h0 - 1;
		int zmax = d0 - 1;
		
		if( boundsInclusive != null ) {
			xmin = boundsInclusive.xmin;
			ymin = boundsInclusive.ymin;
			zmin = boundsInclusive.zmin;
			xmax = boundsInclusive.xmax;
			ymax = boundsInclusive.ymax;
			zmax = boundsInclusive.zmax;
		}
		
		double [] transformedPoint = new double[3];
		
		long numberOfPixelsConsidered = 0;
		long sumSquaredDifferences = 0;
		
		int x_in_domain;
		int y_in_domain;
		int z_in_domain;
		
		for( int z = zmin;
		     z <= zmax;
		     z += (1 + skipPixelsInTemplate) ) {
			
			byte [] templatePixels = (byte []) stack0.getPixels( z + 1 );
			
			for( int y = ymin;
			     y <= ymax;
			     y += (1 + skipPixelsInTemplate) ) {
				
				for( int x = xmin;
				     x <= xmax;
				     x += (1 + skipPixelsInTemplate) ) {
					
					invertedTransform.apply( x, y, z, transformedPoint );
					
					x_in_domain = ((int)transformedPoint[0]);
					y_in_domain = ((int)transformedPoint[1]);
					z_in_domain = ((int)transformedPoint[2]);
					
					int value_in_template = (int)( 0xFF & templatePixels[ x + y * w0 ] );
					int value_in_domain;
					
					if( ( x_in_domain >= 0 ) && ( x_in_domain < w1 ) &&
					    ( y_in_domain >= 0 ) && ( y_in_domain < h1 ) &&
					    ( z_in_domain >= 0 ) && ( z_in_domain < d1 ) ) {
						
						byte [] domainPixels = (byte[])stack1.getPixels( z_in_domain + 1 );
						
						value_in_domain = (int)( 0xFF & domainPixels[ x_in_domain + y_in_domain * w1 ] );
						
						int difference = value_in_domain - value_in_template;
						sumSquaredDifferences += (long)( difference * difference );
						numberOfPixelsConsidered += 1;
						
					}
				}
			}
		}
		
		// System.out.println( "Number of pixels considered was: " + numberOfPixelsConsidered );
		// System.out.println( "Sum of squared differences was: " + sumSquaredDifferences );
		
		return Math.sqrt( sumSquaredDifferences / (double) numberOfPixelsConsidered );
		
	}
	
	public double scoreTransformationThresholdedReal( ImagePlus image0,
							  ImagePlus image1,
							  Threshold threshold0,
							  Threshold threshold1,
							  int skipPixelsInTemplate,
							  BoundsInclusive boundsInclusive ) {
		
		OrderedTransformations invertedTransform = inverse();
		
		ImageStack stack0 = image0.getStack();
		ImageStack stack1 = image1.getStack();
		
		int d0 = stack0.getSize();
		int h0 = stack0.getHeight();
		int w0 = stack0.getWidth();
		
		int d1 = stack1.getSize();
		int h1 = stack1.getHeight();
		int w1 = stack1.getWidth();
		
		int xmin = 0;
		int ymin = 0;
		int zmin = 0;
		
		int xmax = w0 - 1;
		int ymax = h0 - 1;
		int zmax = d0 - 1;
		
		if( boundsInclusive != null ) {
			xmin = boundsInclusive.xmin;
			ymin = boundsInclusive.ymin;
			zmin = boundsInclusive.zmin;
			xmax = boundsInclusive.xmax;
			ymax = boundsInclusive.ymax;
			zmax = boundsInclusive.zmax;
		}
		
		double [] transformedPoint = new double[3];
		
		long numberOfPixelsConsidered = 0;
		long sumSquaredDifferences = 0;
		
		int x_in_domain;
		int y_in_domain;
		int z_in_domain;
		
		for( int z = zmin;
		     z <= zmax;
		     z += (1 + skipPixelsInTemplate) ) {
			
			byte [] templatePixels = (byte []) stack0.getPixels( z + 1 );
			
			for( int y = ymin;
			     y <= ymax;
			     y += (1 + skipPixelsInTemplate) ) {
				
				for( int x = xmin;
				     x <= xmax;
				     x += (1 + skipPixelsInTemplate) ) {
					
					invertedTransform.apply( x, y, z, transformedPoint );
					
					x_in_domain = ((int)transformedPoint[0]);
					y_in_domain = ((int)transformedPoint[1]);
					z_in_domain = ((int)transformedPoint[2]);
					
					int value_in_template = (int)( 0xFF & templatePixels[ x + y * w0 ] );
					if( value_in_template < threshold0.value )
						value_in_template = 0;
					
					int value_in_domain;
					
					if( ( x_in_domain >= 0 ) && ( x_in_domain < w1 ) &&
					    ( y_in_domain >= 0 ) && ( y_in_domain < h1 ) &&
					    ( z_in_domain >= 0 ) && ( z_in_domain < d1 ) ) {
						
						byte [] domainPixels = (byte[])stack1.getPixels( z_in_domain + 1 );
						
						value_in_domain = (int)( 0xFF & domainPixels[ x_in_domain + y_in_domain * w1 ] );
						
						if( value_in_domain < threshold1.value )
							value_in_domain = 0;
						
					} else {
						
						value_in_domain = 0;
						
					}
					
					int difference = value_in_domain - value_in_template;
					sumSquaredDifferences += (long)( difference * difference );
					numberOfPixelsConsidered += 1;
					
				}
			}
		}
		
		// System.out.println( "Number of pixels considered was: " + numberOfPixelsConsidered );
		
		return Math.sqrt( sumSquaredDifferences / (double) numberOfPixelsConsidered );
	}
	
	public double scoreTransformationThresholded( ImagePlus image0,
						      ImagePlus image1,
						      Threshold threshold0,
						      Threshold threshold1,
						      BoundsInclusive boundsInclusive ) {
		return scoreTransformationThresholdedReal( image0, image1, threshold0, threshold1, 0, boundsInclusive );
	}
	
	public double scoreTransformationThresholded( ImagePlus image0,
						      ImagePlus image1,
						      Threshold threshold0,
						      Threshold threshold1 ) {
		
		return scoreTransformationThresholdedReal( image0, image1, threshold0, threshold1, 0, null );
	}
	
	public double scoreTransformationThresholded( ImagePlus image0,
						      ImagePlus image1,
						      Threshold threshold0,
						      Threshold threshold1,
						      int skipPixelsInTemplate ) {
		return scoreTransformationThresholdedReal( image0, image1, threshold0, threshold1, skipPixelsInTemplate, null );
	}
	
	// When we want to create a new image, this transformation maps the
	
	public ImagePlus createNewImageReal( ImagePlus image0, // the "template", may be null
					     ImagePlus image1, // the "domain", must be supplied
					     // The parameters below refer
					     // to ranges in the template...
					     int xmin,
					     int xmax, // inclusive (the last value)
					     int ymin,
					     int ymax, // inclusive (the last value)
					     int zmin,
					     int zmax,
					     boolean overlay ) {
		
		System.out.println("      createNewImageReal called with overlay: "+overlay);
		System.out.println("                                      image0: "+image0);
		System.out.println("                                      image1: "+image1);
		System.out.println("                                        xmin: "+xmin);
		System.out.println("                                        xmax: "+xmax);
		System.out.println("                                        ymin: "+ymin);
		System.out.println("                                        ymax: "+ymax);
		System.out.println("                                        zmin: "+zmin);
		System.out.println("                                        zmax: "+zmax);
		
		int widthNew = (xmax - xmin) + 1;
		int heightNew = (ymax - ymin) + 1;
		int depthNew = (zmax - zmin) + 1;
		
		assert( xmax >= xmin );
		assert( ymax >= ymin );
		assert( zmax >= zmin );
		
		assert( image1 != null );
		
		int width0 = -1;
		int height0 = -1;
		int depth0 = -1;
		
		ImageStack stack0=null;
		if( image0 != null ) {
			width0 = image0.getWidth();
			height0 = image0.getHeight();
			stack0=image0.getStack();
			depth0 = stack0.getSize();
		}
		ImageStack stack1=image1.getStack();
		
		int width1=stack1.getWidth();
		int height1=stack1.getHeight();
		int depth1=stack1.getSize();
		
		assert( (image0 != null) || (!overlay) );
		
		if( (image0 != null) && (image0.getType() != ImagePlus.GRAY8) ) {
			IJ.error("OrderedTransformations.createNewImageReal() can only"+
				 "be used on 8 bit image, at the moment.");
			return null;
		}
		
		if( image1.getType() != ImagePlus.GRAY8 ) {
			IJ.error("OrderedTransformations.createNewImageReal() can only"+
				 "be used on 8 bit image, at the moment.");
			return null;
		}
		
		ImageStack newStack=new ImageStack(widthNew,heightNew);
		
		int x, y, z;
		
		int x_in_domain;
		int y_in_domain;
		int z_in_domain;
		
		int x_in_template;
		int y_in_template;
		int z_in_template;
		
		boolean cacheImage=true;
		
		byte [][] image1_data=null;
		if (cacheImage) {
			image1_data=new byte[depth1][];
			for( z = 0; z < depth1; ++z )
				image1_data[z] = (byte[])stack1.getPixels( z + 1 );
			// System.out.println("      Finished caching source data...");
		}
		
		OrderedTransformations invertedTransform = inverse();
		
		for( z = 0; z < depthNew; ++z ) {
			
			// System.out.println("      Creating slice: "+z);
			
			z_in_template = z + zmin;
			
			byte [] greenPixels = new byte[ widthNew * heightNew ];
			byte [] magentaPixels = null;
			byte [] magentaPixelsExpanded = null;
			if( overlay ) {
				magentaPixelsExpanded = new byte[ widthNew * heightNew ];
				if( (z_in_template >= 0) && (z_in_template < stack0.getSize()) )
					magentaPixels=(byte [])stack0.getPixels( z_in_template + 1 );
				else
					magentaPixels=new byte[image0.getWidth()*image0.getHeight()];
			}
			
			double [] transformedPoint = new double[3];
			
			for( y = 0; y < heightNew; ++y )
				for( x = 0; x < widthNew; ++x ) {
					
					y_in_template = y + ymin;
					x_in_template = x + xmin;
					
					invertedTransform.apply( x_in_template,
								 y_in_template,
								 z_in_template,
								 transformedPoint );
					
					x_in_domain = ((int)transformedPoint[0]);
					y_in_domain = ((int)transformedPoint[1]);
					z_in_domain = ((int)transformedPoint[2]);
					
					if( ( x_in_domain >= 0 ) && ( x_in_domain < width1 ) &&
					    ( y_in_domain >= 0 ) && ( y_in_domain < height1 ) &&
					    ( z_in_domain >= 0 ) && ( z_in_domain < depth1 ) ) {
						
						
						if (cacheImage) {
							greenPixels[ x + y * widthNew ]=
								image1_data[z_in_domain][ x_in_domain + y_in_domain * width1 ];
						} else {
							byte[] pixels=(byte[])stack1.getPixels( z_in_domain + 1 );
							greenPixels[ x + y * widthNew ] =
								pixels[ x_in_domain + y_in_domain * width1 ];
						}
						
					}
					
					if( ( z_in_template >= 0 ) && ( z_in_template < depth0 ) &&
					    ( x_in_template >= 0 ) && ( x_in_template < width0 ) &&
					    ( y_in_template >= 0 ) && ( y_in_template < height0 ) ) {
						
						magentaPixelsExpanded[ x + y * widthNew ] =
							magentaPixels[ x_in_template + y_in_template * width0 ];
						
					}
					
					
					IJ.showProgress( (double) (z + 1) / depthNew );
				}
			
			if( overlay ) {
				ColorProcessor cp = new ColorProcessor( widthNew, heightNew );
				cp.setRGB( magentaPixelsExpanded, greenPixels, magentaPixelsExpanded );
				newStack.addSlice( null, cp );
			} else {
				ByteProcessor bp = new ByteProcessor( widthNew, heightNew );
				bp.setPixels( greenPixels );
				newStack.addSlice( null, bp );
			}
			
		}
		
		IJ.showProgress( 1.0 );
		
		ImagePlus impNew;
		
		if( overlay ) {
			impNew=new ImagePlus("overlay of "+image0.getShortTitle()+
					     " and transformed "+image1.getShortTitle(),newStack);
		} else {
			impNew=new ImagePlus("transformation of "+image1.getShortTitle(),newStack);
		}
		
		// FIXME: more generally, should transform the calibration from image1
		if( image0 != null ) {
			impNew.setCalibration(image0.getCalibration());
		}
		
		return impNew;
		
	}
	
	public ImagePlus createNewImage( ImagePlus image0, ImagePlus image1, boolean cropToTemplate ) {
		
		int width0 = image0.getWidth();
		int width1 = image1.getWidth();
		int height0 = image0.getHeight();
		int height1 = image1.getHeight();
		int depth0 = image0.getStack().getSize();
		int depth1 = image1.getStack().getSize();
		
		if( cropToTemplate ) {
			
			return createNewImageReal( image0, image1,
						   0,
						   width0 - 1,
						   0,
						   height0 - 1,
						   0,
						   depth0 - 1,
						   true );
			
		} else {
			
			// ------ Transform the corners of the domain image -----------------------
			
			// FIXME: Obviously with some transformations this won't
			// give us a good bounding box for the transformed image,
			// but for the moment it's good enough.
			
			int new_min_x_1, new_min_y_1, new_min_z_1;
			int new_max_x_1, new_max_y_1, new_max_z_1;
			
			{
				
				Point3d corner0 = new Point3d( 0,           0,            0 );
				Point3d corner1 = new Point3d( 0,           0,            depth1 - 1);
				Point3d corner2 = new Point3d( 0,           height1 - 1,  0 );
				Point3d corner3 = new Point3d( 0,           height1 - 1,  depth1 - 1);
				Point3d corner4 = new Point3d( width1 - 1,  0,            0 );
				Point3d corner5 = new Point3d( width1 - 1,  0,            depth1 - 1);
				Point3d corner6 = new Point3d( width1 - 1,  height1 - 1,  0 );
				Point3d corner7 = new Point3d( width1 - 1,  height1 - 1,  depth1 - 1);
				
				Point3d corner0_transformed = apply( corner0 );
				Point3d corner1_transformed = apply( corner1 );
				Point3d corner2_transformed = apply( corner2 );
				Point3d corner3_transformed = apply( corner3 );
				Point3d corner4_transformed = apply( corner4 );
				Point3d corner5_transformed = apply( corner5 );
				Point3d corner6_transformed = apply( corner6 );
				Point3d corner7_transformed = apply( corner7 );
				
				/*
				  System.out.println( "corner0 now at: " + corner0_transformed );
				  System.out.println( "corner1 now at: " + corner1_transformed );
				  System.out.println( "corner2 now at: " + corner2_transformed );
				  System.out.println( "corner3 now at: " + corner3_transformed );
				  System.out.println( "corner4 now at: " + corner4_transformed );
				  System.out.println( "corner5 now at: " + corner5_transformed );
				  System.out.println( "corner6 now at: " + corner6_transformed );
				  System.out.println( "corner7 now at: " + corner7_transformed );
				*/
				
				double [] corner_xs = { corner0_transformed.x,
							corner1_transformed.x,
							corner2_transformed.x,
							corner3_transformed.x,
							corner4_transformed.x,
							corner5_transformed.x,
							corner6_transformed.x,
							corner7_transformed.x };
				
				double [] corner_ys = { corner0_transformed.y,
							corner1_transformed.y,
							corner2_transformed.y,
							corner3_transformed.y,
							corner4_transformed.y,
							corner5_transformed.y,
							corner6_transformed.y,
							corner7_transformed.y };
				
				double [] corner_zs = { corner0_transformed.z,
							corner1_transformed.z,
							corner2_transformed.z,
							corner3_transformed.z,
							corner4_transformed.z,
							corner5_transformed.z,
							corner6_transformed.z,
							corner7_transformed.z };
				
				java.util.Arrays.sort( corner_xs );
				java.util.Arrays.sort( corner_ys );
				java.util.Arrays.sort( corner_zs );
				
				double min_trans_corner_x = corner_xs[0];
				double min_trans_corner_y = corner_ys[0];
				double min_trans_corner_z = corner_zs[0];
				
				double max_trans_corner_x = corner_xs[7];
				double max_trans_corner_y = corner_ys[7];
				double max_trans_corner_z = corner_zs[7];
				
				new_min_x_1 = (int)Math.floor(min_trans_corner_x);
				new_min_y_1 = (int)Math.floor(min_trans_corner_y);
				new_min_z_1 = (int)Math.floor(min_trans_corner_z);
				
				new_max_x_1 = (int)Math.ceil(max_trans_corner_x);
				new_max_y_1 = (int)Math.ceil(max_trans_corner_y);
				new_max_z_1 = (int)Math.ceil(max_trans_corner_z);
				
				// System.out.println( "min corner: " + new_min_x_1 + ", " + new_min_y_1 + ", " + new_min_z_1 );
				// System.out.println( "max corner: " + new_max_x_1 + ", " + new_max_y_1 + ", " + new_max_z_1 );
				
			}
			// ---- done with the corner stuff ----------------------------------------
			
			// These are the dimensions of the domain stack when mapped...
			
			int width_mapped2 =  (new_max_x_1 - new_min_x_1) + 1;
			int height_mapped2 = (new_max_y_1 - new_min_y_1) + 1;
			int depth_mapped2 =  (new_max_z_1 - new_min_z_1) + 1;
			
			// Offsets for the transformed and template images within
			// the new larger image...
			
			int target_offset_x = 0;
			int target_offset_y = 0;
			int target_offset_z = 0;
			
			if( new_min_x_1 < 0 )
				target_offset_x = - new_min_x_1;
			
			if( new_min_y_1 < 0 )
				target_offset_y = - new_min_y_1;
			
			if( new_min_z_1 < 0 )
				target_offset_z = - new_min_z_1;
			
			/*
			  System.out.println( "target offsets: " + target_offset_x +
			  ", " + target_offset_y + ", " +
			  target_offset_z );
			*/
			
			// ------------------------------------------------------------------------
			
			int widthNew = Math.max( width0, new_max_x_1 + 1 ) - Math.min( 0, new_min_x_1 );
			int heightNew = Math.max( height0, new_max_y_1 + 1 ) - Math.min( 0, new_min_y_1 );
			int depthNew = Math.max( depth0, new_max_z_1 + 1 ) - Math.min( 0, new_min_z_1 );
			
			return createNewImageReal( image0, // the "template", may be null
						   image1, // the "domain", must be supplied
						   // The parameters below refer
						   // to ranges in the template...
						   -target_offset_x,
						   widthNew-target_offset_x, // inclusive (the last value)
						   -target_offset_y,
						   heightNew-target_offset_y, // inclusive (the last value)
						   -target_offset_z,
						   depthNew-target_offset_z,
						   true );
		}
	}
	
	// Transform image1 into a volume the size of image0, but don't
	// overlay, just keep the transformed image1.
	
	public ImagePlus createNewImageSingle( ImagePlus image,
					       int xmin, int xmax,
					       int ymin, int ymax,
					       int zmin, int zmax ) {
		
		return createNewImageReal( null,
					   image,
					   xmin,
					   xmax,
					   ymin,
					   ymax,
					   zmin,
					   zmax,
					   false );
		
	}
	
	// Transform image1 into a volume the size of image0, but don't
	// overlay, just keep the transformed image1.
	
	public ImagePlus createNewImageSingle( ImagePlus image0, ImagePlus image1, boolean cropToTemplate ) {
		
		int width0 = image0.getWidth();
		int width1 = image1.getWidth();
		int height0 = image0.getHeight();
		int height1 = image1.getHeight();
		int depth0 = image0.getStack().getSize();
		int depth1 = image1.getStack().getSize();
		
		int xmin = 0;
		int xmax = 0;
		int ymin = 0;
		int ymax = 0;
		int zmin = 0;
		int zmax = 0;
		
		if (cropToTemplate) {
			
			return createNewImageReal( null,
						   image1,
						   0,
						   width0 - 1,
						   0,
						   height0 - 1,
						   0,
						   depth0 - 1,
						   false );
			
		} else {
			
			// ------ Transform the corners of the domain image -----------------------
			
			/* FIXME: Obviously with some transformations this won't
			   give us a good bounding box for the transformed image,
			   but for the moment it's good enough. */
			
			int new_min_x_1, new_min_y_1, new_min_z_1;
			int new_max_x_1, new_max_y_1, new_max_z_1;
			
			{
				
				Point3d corner0 = new Point3d( 0,           0,            0 );
				Point3d corner1 = new Point3d( 0,           0,            depth1 - 1);
				Point3d corner2 = new Point3d( 0,           height1 - 1,  0 );
				Point3d corner3 = new Point3d( 0,           height1 - 1,  depth1 - 1);
				Point3d corner4 = new Point3d( width1 - 1,  0,            0 );
				Point3d corner5 = new Point3d( width1 - 1,  0,            depth1 - 1);
				Point3d corner6 = new Point3d( width1 - 1,  height1 - 1,  0 );
				Point3d corner7 = new Point3d( width1 - 1,  height1 - 1,  depth1 - 1);
				
				Point3d corner0_transformed = apply( corner0 );
				Point3d corner1_transformed = apply( corner1 );
				Point3d corner2_transformed = apply( corner2 );
				Point3d corner3_transformed = apply( corner3 );
				Point3d corner4_transformed = apply( corner4 );
				Point3d corner5_transformed = apply( corner5 );
				Point3d corner6_transformed = apply( corner6 );
				Point3d corner7_transformed = apply( corner7 );
								
				double [] corner_xs = { corner0_transformed.x,
							corner1_transformed.x,
							corner2_transformed.x,
							corner3_transformed.x,
							corner4_transformed.x,
							corner5_transformed.x,
							corner6_transformed.x,
							corner7_transformed.x };
				
				double [] corner_ys = { corner0_transformed.y,
							corner1_transformed.y,
							corner2_transformed.y,
							corner3_transformed.y,
							corner4_transformed.y,
							corner5_transformed.y,
							corner6_transformed.y,
							corner7_transformed.y };
				
				double [] corner_zs = { corner0_transformed.z,
							corner1_transformed.z,
							corner2_transformed.z,
							corner3_transformed.z,
							corner4_transformed.z,
							corner5_transformed.z,
							corner6_transformed.z,
							corner7_transformed.z };
				
				java.util.Arrays.sort( corner_xs );
				java.util.Arrays.sort( corner_ys );
				java.util.Arrays.sort( corner_zs );
				
				double min_trans_corner_x = corner_xs[0];
				double min_trans_corner_y = corner_ys[0];
				double min_trans_corner_z = corner_zs[0];
				
				double max_trans_corner_x = corner_xs[7];
				double max_trans_corner_y = corner_ys[7];
				double max_trans_corner_z = corner_zs[7];
				
				new_min_x_1 = (int)Math.floor(min_trans_corner_x);
				new_min_y_1 = (int)Math.floor(min_trans_corner_y);
				new_min_z_1 = (int)Math.floor(min_trans_corner_z);
				
				new_max_x_1 = (int)Math.ceil(max_trans_corner_x);
				new_max_y_1 = (int)Math.ceil(max_trans_corner_y);
				new_max_z_1 = (int)Math.ceil(max_trans_corner_z);
				
				// System.out.println( "min corner: " + new_min_x_1 + ", " + new_min_y_1 + ", " + new_min_z_1 );
				// System.out.println( "max corner: " + new_max_x_1 + ", " + new_max_y_1 + ", " + new_max_z_1 );
				
			}
			// ---- done with the corner stuff ----------------------------------------
			
			// These are the dimensions of the domain stack when mapped...
			
			int width_mapped2 =  (new_max_x_1 - new_min_x_1) + 1;
			int height_mapped2 = (new_max_y_1 - new_min_y_1) + 1;
			int depth_mapped2 =  (new_max_z_1 - new_min_z_1) + 1;
			
			// Offsets for the transformed and template images within
			// the new larger image...
			
			int target_offset_x = 0;
			int target_offset_y = 0;
			int target_offset_z = 0;
			
			if( new_min_x_1 < 0 )
				target_offset_x = - new_min_x_1;
			
			if( new_min_y_1 < 0 )
				target_offset_y = - new_min_y_1;
			
			if( new_min_z_1 < 0 )
				target_offset_z = - new_min_z_1;
			
			/*
			  System.out.println( "target offsets: " + target_offset_x +
			  ", " + target_offset_y + ", " +
			  target_offset_z );
			*/
			
			// ------------------------------------------------------------------------
			
			int widthNew = new_max_x_1 -  new_min_x_1;
			int heightNew = new_max_y_1 - new_min_y_1;
			int depthNew = new_max_z_1 - new_min_z_1;
			
			return createNewImageReal( null, // the "template", may be null
						   image1, // the "domain", must be supplied
						   // The parameters below refer
						   // to ranges in the template...
						   -target_offset_x,
						   widthNew-target_offset_x, // inclusive (the last value)
						   -target_offset_y,
						   heightNew-target_offset_y, // inclusive (the last value)
						   -target_offset_z,
						   depthNew-target_offset_z,
						   false );
			
		}
		
	}
	
	
	
	
}
