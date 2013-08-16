# Create a new blank image and store in the variable 'image'
title = 'Image title'
width = 512
height = 512
n_slices = 8  # this will create a stack of 8 slices
bit_depth = 8 # this will create a grayscale image. Acceptable values: 8, 16, 24 (RGB) and 32 (float)
options = NewImage.FILL_BLACK # other choices:  FILL_RAMP, FILL_WHITE
image = NewImage.createImage(title, width, height, n_slices, bit_depth, options)
image.show()

