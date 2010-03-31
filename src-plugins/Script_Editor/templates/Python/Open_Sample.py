# Open the sample named 'sample_name' and store it in the variable 'image'.
# 'sample_name' must refer to an iage actually present on Wayne
# Rasband site; they are listed in the Fie >> Open Sample menu.
# For instance:
sample_name = 'blobs.gif';
image = IJ.openImage('http://rsb.info.nih.gov/ij/images/'+sample_name)
# then display it.
image.show()
