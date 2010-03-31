# Straighten: like the command in Edit > Selection menu, 
# this snipet creates a new image by taking some pixels along a 
# line ROI. It is typically used to make a straight image from
# a bent selection.
width = 20 # how many pixels should we fetch from around the ROI?
# Get current ImagePlus
image = WindowManager.getCurrentImage()
if image is not None:
  roi = image.getRoi()
  if roi is not None and roi.isLine(): # we can only do it for line ROIs
    # Instantiate plugin
    straightener = Straightener()
    # Are we dealing with a stack?
    stack_size = image.getStackSize()
    if stack_size > 1:
      new_stack = straightener.straightenStack(image, roi, width)
      new_image = ImagePlus( image.getTitle()+"-straightened", new_stack)
    else:
      new_ip = straightener.straighten(image, roi, width)
      new_image = ImagePlus( image.getTitle()+"-straightened", new_ip)
    # Display result
    new_image.show()

