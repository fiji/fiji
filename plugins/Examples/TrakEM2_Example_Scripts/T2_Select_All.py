# Example script to select all 2D objects of the current layer
# in a trakem project whose display window is at the front.

display = Display.getFront()

if display is None:
	IJ.showMessage("No TrakEM2 displays are open.")
else:
	layer = display.getLayer()
	sel = display.getSelection()
	# Add all displayables of the layer to the selection of the front display:
	for d in layer.getDisplayables():
		sel.add(d)
