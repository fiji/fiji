# Set current image properties. WARNING!! There is no check that what you enter is correct
# Set dimensions
n_channels  = 1
n_slices  = 1    # Z slices
n_frames  = 1    # time frames
# Get current image
image = WindowManager.getCurrentImage()
# Check that we have correct dimensions
stack_size = image.getImageStackSize() # raw number of images in the stack
if n_channels * n_slices * n_frames == stack_size:
  image.setDimensions(n_channels, n_slices, n_frames)
else:
  IJ.log('The product of channels ('+str(n_channels)+'), slices ('+str(n_slices)+')')
  IJ.log('and frames ('+str(n_frames)+') must equal the stack size ('+str(stack_size)+').')
# Set calibration
pixel_width   = 1
pixel_height  = 1
pixel_depth   = 1
space_unit    = 'µm'
frame_interval  = 1
time_unit     = 's'
calibration = Calibration() # new empty calibration
calibration.pixelWidth    = pixel_width
calibration.pixelHeight   = pixel_height
calibration.pixelDepth    = pixel_depth
calibration.frameInterval   = frame_interval
calibration.setUnit(space_unit)
calibration.setTimeUnit(time_unit)
image.setCalibration(calibration)
image.repaintWindow()

