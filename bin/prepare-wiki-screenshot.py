#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''

from ij import IJ, ImageJ

label = 'Fiji Is Just ImageJ'

if IJ.getInstance() == None:
	# called from the command line
	from sys import argv

	if len(argv) > 1:
		file = argv[1]
	else:
		file = "Stitching-overview.jpg"
	if len(argv) > 2:
		label = argv[2]
	ImageJ()
	screenshot = IJ.openImage(file)
	print "Opened", file, screenshot
else:
	screenshot = IJ.getImage()
	label = IJ.getString('Label:', label)

from fiji import Prettify_Wiki_Screenshot

plugin = Prettify_Wiki_Screenshot()
plugin.label = label
plugin.run(screenshot.getProcessor())
