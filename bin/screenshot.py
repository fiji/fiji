#!/usr/bin/python

import sys
from ij import IJ, ImageJ

if len(sys.argv) < 2:
	print 'Need an output file'
	sys.exit(1)

window = ImageJ()
window.hide()
IJ.run("Capture Screen ")
IJ.save(sys.argv[1])
sys.exit(0)
