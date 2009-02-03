# Albert Cardona 20081204 14:55
# 
# An example script for TrakEM2 to measure the areas of each AreaList at each
# layer, and also the mean intensity values of the images under such areas.
#
# Works by creating a ShapeRoi ouf of the area of the java.awt.geom.Area that
# an AreaList has for a given Layer.
#
# Reports in an ImageJ Results Table.
# 
# There are two measure functions:
# 1 - measure: uses ImageJ's measurement settings and options
# 2 - measureCustom: directly creates a ResultsTable with each AreaList name,
# id, layer index, layer Z, area in the layer and mean in the layer.
#
# The declaration and invocation of the first "measure" function are commented
# out with triple quotes.
# 
# Built as requested by Jean-Yves Tinevez on 20081204 on fiji-devel mailing list
# 

from java.awt.geom import AffineTransform

"""
def measure(layerset):
  # Obtain a list of all AreaLists:
  alis = layerset.getZDisplayables(AreaList)
  # The loader
  loader = layerset.getProject().getLoader()

  for ali in alis:
    affine = ali.getAffineTransformCopy()
    box = ali.getBoundingBox()
    for layer in layerset.getLayers():
      # The java.awt.geom.Area object for the AreaList 'ali'
      # at the given Layer 'layer':
      area = ali.getArea(layer)
      if area:
        # Bring the area to world coordinates,
        # and then local to its own data:
        tr = AffineTransform()
        tr.translate(-box.x, -box.y)
        tr.concatenate(affine)
        area = area.createTransformedArea(tr)
        # Create a snapshot of the images under the area:
        imp = loader.getFlatImage(layer, box, 1, 0xffffffff,
              ImagePlus.GRAY8, Patch, False)
        # Set the area as a roi
        imp.setRoi(ShapeRoi(area))
        # Perform measurement according to ImageJ's measurement options:
	# (Calibrated)
        IJ.run(imp, "Measure", "")

display = Display.getFront()
if display is not None:
  # Obtain the LayerSet of the current Display canvas:
  layerset = Display.getFront().getLayer().getParent()
  # Measure!
  measure(layerset)
else:
  IJ.showMessage("Open a TrakEM2 display first!")
"""


# As an alternative, create your own ResultsTable:
def measureCustom(layerset):
  # Obtain a list of all AreaLists:
  alis = layerset.getZDisplayables(AreaList)
  # The loader
  loader = layerset.getProject().getLoader()
  # The ResultsTable
  table = Utils.createResultsTable("AreaLists", ["id", "layer", "Z", "area", "mean"])
  # The LayerSet's Calibration (units in microns, etc)
  calibration = layerset.getCalibrationCopy()
  # The measurement options as a bit mask:
  moptions = Measurements.AREA | Measurements.MEAN
  
  for ali in alis:
    affine = ali.getAffineTransformCopy()
    box = ali.getBoundingBox()
    index = 0
    for layer in layerset.getLayers():
      index += 1 # layer index starts at 1, so sum before
      # The java.awt.geom.Area object for the AreaList 'ali'
      # at the given Layer 'layer':
      area = ali.getArea(layer)
      if area:
        # Bring the area to world coordinates,
        # and then local to its own data:
        tr = AffineTransform()
        tr.translate(-box.x, -box.y)
        tr.concatenate(affine)
        area = area.createTransformedArea(tr)
        # Create a snapshot of the images under the area:
        imp = loader.getFlatImage(layer, box, 1, 0xffffffff,
              ImagePlus.GRAY8, Patch, False)
        # Set the area as a roi
        imp.setRoi(ShapeRoi(area))
        # Perform measurements (uncalibrated)
	# (To get the calibration, call layerset.getCalibrationCopy())
        stats = ByteStatistics(imp.getProcessor(), moptions, calibration)
	table.incrementCounter()
	table.addLabel("Name", ali.getTitle())
	table.addValue(0, ali.getId())
	table.addValue(1, index) # the layer index
	table.addValue(2, layer.getZ()) 
	table.addValue(3, stats.area)
	table.addValue(4, stats.mean)
    # Update and show the table
    table.show("AreaLists")


# Get the front display, if any:
display = Display.getFront()

if display is not None:
  # Obtain the LayerSet of the current Display canvas:
  layerset = Display.getFront().getLayer().getParent()
  # Measure!
  measureCustom(display.getFront().getLayer().getParent())
else:
  IJ.showMessage("Open a TrakEM2 display first!")

