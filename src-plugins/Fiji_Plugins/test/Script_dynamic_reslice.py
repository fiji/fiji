'''
This jython script intends at demonstrating how to script the 
Dynamic_Reslice plugin. It will open the t1-head sample stack
in Fiji, draw a ROI on it, and animate it while updating the 
Reslice image.

Created on Apr 23, 2009

@author: Jean-Yves Tinevez
'''

import fiji
import time

xstart = 40
ystart = 50


# Fetch the t1-head stack from URL
IJ.open('http://rsb.info.nih.gov/ij/images/t1-head.zip')

# Get the current ImagePlus
source_imp = WindowManager.getCurrentImage()

# Select middle slice (does not matter)
source_imp.setSlice(60)

# Instantiate the Dynamic_Reslice plugin
dr = fiji.stacks.Dynamic_Reslice(source_imp)

# Set up the plugin so that it will rotate the resulting image, and will 
# parse slices from bottom to top
dr.setRotate(True)
dr.setFlip(True)

# Get the destination ImagePlus
dest_imp = dr.getImagePlus()

# Now move the roi and update the image
for dx in range(170):
    
    IJ.showStatus('Moving the Roi by '+str(dx))
    
    # Draw a line ROI on the source imp
    roi = Line(xstart+dx, ystart, xstart+dx, ystart+170)
    source_imp.setRoi(roi)
    
    # Update the reslice. We have to call it manually in the script.
    dr.update();
    
    # Wait a bit so that we can see what is happening
    time.sleep(0.03)
    
IJ.showStatus('Done')
