display = Display.getFront()
if display is None:
	IJ.showMessage("No TrakEM2 displays are open.")
else:
	sel = display.getLayer().getSelection()
	for d in layer.getDisplayables():
		sel.add(d)
