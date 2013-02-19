# Rotate current ROI
import math
angle = 0.1 # must be in radian
# Get current ImagePlus
image = WindowManager.getCurrentImage()
# Get current ROI
roi = image.getRoi()
if roi is not None:
  # Get ROI points
  polygon = roi.getPolygon()
  n_points = polygon.npoints
  x = polygon.xpoints
  y = polygon.ypoints
  # Compute center of mass
  xc = 0
  yc = 0
  for i in range(n_points):
    xc = xc + x[i]
    yc = yc + y[i]
  xc = xc / n_points
  yc = yc / n_points
  # Compute new rotated points
  new_x = []
  new_y = []
  for i in range(n_points):
    new_x.append( int ( xc + (x[i]-xc)*math.cos(angle) - (y[i]-yc)*math.sin(angle) ) )
    new_y.append( int ( yc + (x[i]-xc)*math.sin(angle) + (y[i]-yc)*math.cos(angle) ) )
  # Create new ROI
  new_roi = PolygonRoi(new_x, new_y, n_points,  Roi.POLYGON )
  image.setRoi(new_roi)
