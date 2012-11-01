# Remove any ROI in currently selected image
# Get current ImagePlus
image = WindowManager.getCurrentImage()
# Remove ROI from it
image.killRoi()
