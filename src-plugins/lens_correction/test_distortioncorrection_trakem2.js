importClass( Packages.lenscorrection.DistortionCorrectionTask );
importClass( Packages.ini.trakem2.display.Display );
importClass( Packages.ini.trakem2.display.Patch );

/** Remove all coordinate transforms from all patches in the layer set */
var layers = Display.getFront().getSelection().getLayer().getParent().getLayers();
for ( var i = 0; i < layers.size(); ++i )
{
	var patches = layers.get( i ).getDisplayables( Patch );
	for ( var j = 0; j < patches.size(); ++j )
	{
		var patch = patches.get( j );
		patch.setCoordinateTransform( null );
		patch.updateMipmaps();
	}
}

/** Estimate and apply distortion correction model */
DistortionCorrectionTask.correctDistortionFromSelection( Display.getFront().getSelection() );

