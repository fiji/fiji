display = Display.getFront()
if display is None:
	IJ.showMessage("No TrakEM displays are open.")
else:
	layer_set = display.getLayer().getParent()
	for la in layer_set.getLayers():
		for d in la.getDisplayables():
			d.getAffineTransform().setToIdentity()
