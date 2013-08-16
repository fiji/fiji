############## Ball Size Homogenize
# Set a specific radius to all individual spheres
# of all Ball objects of the displayed TrakEM2 project.

gd = GenericDialog("Ball Radius")
gd.addNumericField( "radius :", 40, 2 )
gd.showDialog()
if not gd.wasCanceled() :
	calibrated_radius = gd.getNextNumber()  # in microns, nm, whatever

	display = Display.getFront()
	layerset = display.getLayerSet()
	cal = layerset.getCalibration()
	# bring radius to pixels
	new_radius = calibrated_radius / cal.pixelWidth

	for ballOb in layerset.getZDisplayables(Ball):
		for i in range(ballOb.getCount()):
			ballOb.setRadius(i, new_radius)
			ballOb.repaint(True, None)
##############