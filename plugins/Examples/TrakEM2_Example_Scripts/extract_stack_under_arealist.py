# Albert Cardona 2009-11-16 for Nitai Steinberg
#
# Select an AreaList in a TrakEM2 project and then run this script.
#

from ij import IJ, ImageStack, ImagePlus
from ij.gui import ShapeRoi
from ij.process import ByteProcessor, ShortProcessor
from ini.trakem2.display import Display, AreaList, Patch
from java.awt import Color

def extract_stack_under_arealist():
	# Check that a Display is open
	display = Display.getFront()
	if display is None:
		IJ.log("Open a TrakEM2 Display first!")
		return
	# Check that an AreaList is selected and active:
	ali = display.getActive()
	if ali is None or not isinstance(ali, AreaList):
		IJ.log("Please select an AreaList first!")
		return

	# Get the range of layers to which ali paints:
	ls = display.getLayerSet()
	ifirst = ls.indexOf(ali.getFirstLayer())
	ilast = ls.indexOf(ali.getLastLayer())
	layers = display.getLayerSet().getLayers().subList(ifirst, ilast +1)

	# Create a stack with the dimensions of ali
	bounds = ali.getBoundingBox()
	stack = ImageStack(bounds.width, bounds.height)

	# Using 16-bit. To change to 8-bit, use GRAY8 and ByteProcessor in the two lines below:
	type = ImagePlus.GRAY16
	ref_ip = ShortProcessor(bounds.width, bounds.height)

	for layer in layers:
		area = ali.getArea(layer)
		z = layer.getZ()
		ip = ref_ip.createProcessor(bounds.width, bounds.height)
		if area is None:
			stack.addSlice(str(z), bp)
			continue

		# Create a ROI from the area of ali at layer:
		aff = ali.getAffineTransformCopy()
		aff.translate(-bounds.x, -bounds.y)
		roi = ShapeRoi(area.createTransformedArea(aff))

		# Create a cropped snapshot of the images at layer under ali:
		flat = Patch.makeFlatImage(type, layer, bounds, 1.0, layer.getDisplayables(Patch), Color.black)
		b = roi.getBounds()
		flat.setRoi(roi)
		ip.insert(flat.crop(), b.x, b.y)

		# Clear the outside of ROI (ShapeRoi is a non-rectangular ROI type)
		bimp = ImagePlus("", ip)
		bimp.setRoi(roi)
		ip.setValue(0)
		ip.setBackgroundValue(0)
		IJ.run(bimp, "Clear Outside", "")

		# Accumulate slices
		stack.addSlice(str(z), ip)

	imp = ImagePlus("AreaList stack", stack)
	imp.setCalibration(ls.getCalibrationCopy())
	imp.show()


# Execute:
extract_stack_under_arealist()
