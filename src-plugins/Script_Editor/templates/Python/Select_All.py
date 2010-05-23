# Create a ROI around the whole image
# Get current ImagePlus
image = WindowManager.getCurrentImage()
# Create ROI
roi = Roi(0, 0, image.getWidth(), image.getHeight())
# Assign it to the image and display it 
image.setRoi(roi, True)
