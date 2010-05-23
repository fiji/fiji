# Inverse ROI: replaces the current ROI by its inverse.
# Get current ImagePlus
image = WindowManager.getCurrentImage()
# Get current ROI
roi = image.getRoi()
if roi is not None:
  # Convert current roi to a ShapeRoi object
  shape_1 = ShapeRoi(roi)
  # Create a ShapeRoi that grabs the whole image
  shape_2 = ShapeRoi(Roi(0,0, image.getWidth(), image.getHeight()))
  # Compute inverse by XOR operation
  image.setRoi(shape_1.xor(shape_2))
