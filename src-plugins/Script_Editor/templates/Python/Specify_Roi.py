# Create new ROI programmatically
# Get current ImagePlus
image = WindowManager.getCurrentImage()
# Enter ROI coordinates
width = 50
height = 50
x_ROI = 100 # ROI center
y_ROI = 100 # ROI center
oval = False # Set to True to get an oval ROI
# Make it
x1 = int(x_ROI - (width/2))
y1 = int(y_ROI - (height/2))
if oval:
  roi = OvalRoi(x1, y1, width, height);
else:
  roi = Roi(x1, y1, width, height);
image.setRoi(roi);

