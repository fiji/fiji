# This script serves two purposes:
#
# - to demonstrate that an AWT Listener can be written in Jython, and
#
# - to find the width of an image you know is uncompressed, but do not know
#   the dimensions.
#
# To use it, open the raw image with File>Import>Raw... choosing a width and
# height that should roughly be the correct one.  Then start this script,
# which will open a dialog box with a slider, with which you can interactively
# test new widths -- the pixels in the image window will be updated accordingly.

from ij.gui import GenericDialog

from java.awt.event import AdjustmentListener

from java.lang import Math, System

image = WindowManager.getCurrentImage()
ip = image.getProcessor()
pixelsCopy = ip.getPixelsCopy()
pixels = ip.getPixels()
width = ip.getWidth()
height = ip.getHeight()

minWidth = int(Math.sqrt(len(pixels) / 16))
maxWidth = minWidth * 16

class Listener(AdjustmentListener):
	def adjustmentValueChanged(self, event):
		value = event.getSource().getValue()
		rowstride = min(width, value)
		for j in range(0, min(height, int(width * height / value))):
			System.arraycopy(pixelsCopy, j * value,
				pixels, j * width, rowstride)
		image.updateAndDraw()

gd = GenericDialog("Width")
gd.addSlider("width", minWidth, maxWidth, ip.getHeight())
gd.getSliders().get(0).addAdjustmentListener(Listener())
gd.showDialog()
if gd.wasCanceled():
	pixels[0:width * height] = pixelsCopy
	image.updateAndDraw()
