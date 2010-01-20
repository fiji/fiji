#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

from java.awt import *
from ij3d import Image3DUniverse
from javax.media.j3d import Transform3D

from ij import IJ

label = 'Fiji Is Just ImageJ'

if IJ.getInstance() == None:
	# called from the command line
	from ij import ImageJ, ImagePlus
	from ij.process import Blitter, ColorProcessor
	from java.lang import Math
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

# make a gradient image

ip = screenshot.getProcessor()
w = ip.getWidth()
h = ip.getHeight()

u = int(0.99 * 255)
l = int(0.5 * 255)

def gray2rgb(value):
	return (value << 16) | (value << 8) | value

# This is a trick: make a 1xh image (because calculation is slow in Python) and resize it to wxh
from jarray import array
maskPixels = array([gray2rgb(int(u + (l + 1 - u) * i / (h))) for i in range(0, h)], 'i')
mask = ColorProcessor(1, h, maskPixels).resize(w, h)

# "blend" mask with original image

mask.copyBits(ip, 0, 0, Blitter.MAX)
mask.flipVertical()

# instantiate the 3D viewer

univ = Image3DUniverse()
univ.show()

# add the images

cImage = univ.addOrthoslice(ImagePlus("screenshot", ip), \
		None, "image", 0, [1, 1, 1], 1)
dy = -int(h/8)
cImage.setTransform(Transform3D([1.0, 0.0, 0.0, 0.0, \
		0.0, 1.0, 0.0, dy, \
		0.0, 0.0, 1.0, 0.0, \
		0.0, 0.0, 0.0, 1.0]))


cMirror = univ.addOrthoslice(ImagePlus("mirror", mask), \
		None, "mirror", 0, [1, 1, 1], 1)

cos = 0.0
sin = 1.0
cMirror.applyTransform(Transform3D([1.0, 0.0, 0.0, 0.0, \
		0.0, cos, sin, 0.0, \
		0.0, -sin, cos, 0.0, \
		0.0, 0.0, 0.0, 1.0]))

cMirror.applyTransform(Transform3D([1.0, 0.0, 0.0, 0.0, \
		0.0, 1.0, 0.0, h, \
		0.0, 0.0, 1.0, dy, \
		0.0, 0.0, 0.0, 1.0]))

# rotate nicely

from time import sleep
sleep(1)
univ.rotateY(Math.PI/12)
univ.fireTransformationUpdated()
sleep(1)
univ.adjustView()
univ.fireTransformationUpdated()

# set background

background = 1.0
univ.getCanvas().getBG().setColor(background, background, background)

# take snapshot

snapshotWidth = 2047
snapshot = univ.takeSnapshot(snapshotWidth, snapshotWidth)
univ.close()

# downsample

w2 = 400
h2 = 400
IJ.run(snapshot, "downsample ", "width=" + str(w2) + " height=" + str(h2) + " source=0.50 target=0.50 keep")

# write label

from java.awt import Color, Font
def drawOutlineText(ip, string, size, x, y):
	#font = Font("Sans-serif", Font.BOLD, size)
	font = Font("Arial", Font.BOLD, size)
	#offsets = [[-2, 0], [-1, -1], [0, -2], [1, -1], [2, 0], [1, 1], [0, 2], [-1, 1]]
	offsets = [[-1, 0], [-1, -1], [0, -1], [1, -1], [1, 0], [1, 1], [0, 1], [-1, 1]]
	shadowOffset = 3

	ip.setFont(font)
	ip.setAntialiasedText(True)

	# cast shadow
	shadowGray = 64
	ip.setColor(Color(shadowGray, shadowGray, shadowGray, 32))
	for dxy in offsets:
		ip.drawString(string, x + shadowOffset + dxy[0], y + shadowOffset + dxy[1])
	ip.setColor(Color(shadowGray, shadowGray, shadowGray, 64))
	ip.drawString(string, x + shadowOffset, y + shadowOffset)

	# simulate outline

	# setColor(int) does not set the drawingColor; go figure!
	ip.setColor(Color(0, 0, 0, 80))
	for dxy in offsets:
		ip.drawString(string, x + dxy[0], y + dxy[1])
	ip.setColor(Color(255, 255, 255, 255))
	ip.drawString(string, x, y)

smallImage = IJ.getImage()
drawOutlineText(smallImage.getProcessor(), label, 24, 30, smallImage.getHeight() - 30)
smallImage.updateAndDraw()
