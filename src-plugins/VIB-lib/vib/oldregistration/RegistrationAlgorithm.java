/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib.oldregistration;

import ij.ImagePlus;
import ij.IJ;
import ij.WindowManager;

import util.BatchOpener;
import util.FileAndChannel;
import math3d.Point3d;

import landmarks.NamedPointSet;

public abstract class RegistrationAlgorithm {

	public boolean keepSourceImages;
	public ImagePlus[] sourceImages;

	public ImagePlus getTemplate() {
		return sourceImages[0];
	}

	public ImagePlus getDomain() {
		return sourceImages[1];
	}

	// The same functionality according to different terminology:

	public ImagePlus getModel() {
		return sourceImages[0];
	}

	public ImagePlus getFloating() {
		return sourceImages[1];
	}

	public void loadImages( FileAndChannel f0, FileAndChannel f1 ) {

		ImagePlus[] f0imps=BatchOpener.open(f0.getPath());
		ImagePlus[] f1imps=BatchOpener.open(f1.getPath());

		sourceImages=new ImagePlus[2];

		sourceImages[0]=f0imps[f0.getChannelZeroIndexed()];
		sourceImages[1]=f1imps[f1.getChannelZeroIndexed()];
		invalidateTransformation();
	}

	boolean validTransformation = false;

	public void invalidateTransformation( ) {
		validTransformation = false;
	}

	public void validateTransformation( ) {
		validTransformation = true;
	}

	public boolean isTransformationValid( ) {
		return validTransformation;
	}

	public void setImages( ImagePlus template, ImagePlus domain ) {
		if( sourceImages == null )
			sourceImages = new ImagePlus[2];
		sourceImages[0] = template;
		sourceImages[1] = domain;
		invalidateTransformation();
	}

	public void generateTransformation( ) {
		throw new RuntimeException( "generateTransformation() not implemented for this objects of this class ("+this.getClass()+")" );
	}

	public static class ImagePoint {
		public int x, y, z;
	}

	public void transformDomainToTemplate( int x, int y, int z, ImagePoint result ) {
		throw new RuntimeException( "transformDomainToTemplate() not implemented for this objects of this class ("+this.getClass()+")" );
	}

	public void transformDomainToTemplateWorld( double x, double y, double z, Point3d result ) {
		throw new RuntimeException( "transformDomainToTemplateWorld() not implemented for this objects of this class ("+this.getClass()+")" );
	}

	public void transformTemplateToDomain( int x, int y, int z, ImagePoint result ) {
		throw new RuntimeException( "transformTemplateToDomain() not implemented for this objects of this class ("+this.getClass()+")" );
	}

	public void transformTemplateToDomainWorld( double x, double y, double z, Point3d result ) {
		throw new RuntimeException( "transformTemplateToDomainWorld() not implemented for this objects of this class ("+this.getClass()+")" );
	}

	public ImagePlus register() {
		throw new RuntimeException( "register() not implemented for this objects of this class ("+this.getClass()+")" );
	}

	public ImagePlus register( NamedPointSet templatePointSet, NamedPointSet floatingPointSet ) {
		throw new RuntimeException( "register(NamedPointSet,NamedPointSet) not implemented for this objects of this class ("+this.getClass()+")" );
	}

}
