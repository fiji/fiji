# Cover Maker was written by PAvel Tomancak with minor help of
# Albert Cardona & Johannes Schindelin (Pop & Mom)

from ij import IJ, ImagePlus, ImageStack
from ij.gui import Roi
from ij.io import FileSaver
from ij.process import ColorProcessor
from math import sqrt, pow
import os
from os import path
import sys
import re
import random
from fiji.util import CoverMakerUtils
from fiji.util.gui import GenericDialogPlus
from mpicbg.ij.integral import Scale
from java.awt.event import ActionListener, TextListener
from java.lang import Float
from loci.formats.gui import BufferedImageReader
import zipfile
import zlib

def CropInputImage(ip, width, height):
	temp = int(ip.width/width)
	newwidth = temp * width
	temp = int(ip.height/height)
	newheight = temp * height

	roi = Roi(0,0,newwidth,newheight)
	ip.setRoi(roi)
	ip = ip.crop()
	return ip.crop()

#split template image into tiles
def SplitImage(ip, width, height):
	stack = ImageStack(width, height)

	for x in range(0,ip.width,width):
		for y in range(0,ip.height,height):
			roi = Roi(x,y,width,height)
			ip.setRoi(roi)
			ip2 = ip.crop()
			stack.addSlice(None, ip2)
	return stack

#compare all tiles to all downsampled database images of the same size
#iterate through template slices
def CreateCover(ip, width, height, dbpath):
	# split input image into appropriate tiles
	stackt = SplitImage(ip, width, height)
	impt = ImagePlus("template", stackt)
	nSlicestmp = impt.getNSlices()

	# open the preprocessed database
	print dbpath
	impd = IJ.openImage(dbpath)
	stackd = impd.getImageStack()
	nSlicesdb = impd.getNSlices()

	#associate index with image names
	imageNames = impd.getProperty('Info')
	imageList = imageNames.split(';')

	# set up preview output
	outputip = ColorProcessor(ip.width, ip.height)
	outputimp = ImagePlus("output", outputip)
	outputimp.show()

	cols = ip.width/width
	rows = ip.height/height

	print str(cols) + "," + str(rows)

	x = 0
	y = 0

	arrays = [None, None] # a list of two elements
	tileNames = {}
	tileIndex = {}
	placed = {}
	used = {}

	while len(placed) < nSlicestmp:
		randomTileIndex = random.randint(1, nSlicestmp)
		if randomTileIndex in placed:
			continue
		# transform to row adn column coordinate
		if randomTileIndex%rows == 0:
			y = rows-1
			x = (randomTileIndex/rows)-1
		else:
			y = (randomTileIndex%rows)-1
			x = int(randomTileIndex/rows)

		pixelst = stackt.getPixels(randomTileIndex)
		minimum = Float.MAX_VALUE
		#iterate through database images
		j = 1
		indexOfBestMatch = 0
		arrays[0] = pixelst
		while j < nSlicesdb:
			if j in used:
				j +=1
				continue
			arrays[1] = stackd.getPixels(j)
			diff = CoverMakerUtils.tileTemplateDifference(arrays)
			if diff < minimum:
				minimum = diff
				indexOfBestMatch = j
			j += 1
		ip = stackd.getProcessor(indexOfBestMatch)
		outputip.copyBits(ip, x*width, y*height, 0)
		used[indexOfBestMatch] = 1
		tileNames[randomTileIndex] = imageList[indexOfBestMatch-1]
		tileIndex[randomTileIndex] = indexOfBestMatch-1
		outputimp.draw()
		placed[randomTileIndex] = 1

	return tileNames, tileIndex, cols, rows

def ScaleImageToSize(ip, width, height):
	"""Scale image to a specific size using Stephans scaler"""
	smaller = ip.scale( width, height );
	return smaller

def SaveCoverFromFs(tiles, newwidth, newheight, cols, rows):

	tilewidth = int(newwidth/cols)
	tileheight = int(newheight/rows)

	newwidth = int(newwidth/tilewidth) * tilewidth
	newheight = int(newheight/tileheight) * tileheight

	hiresoutip = ColorProcessor(newwidth, newheight)
	hiresout = ImagePlus("hi res output", hiresoutip)
	hiresout.show()

	x = 0
	y = -1

	plane = []

	# scale the images
	for i in sorted(tiles.iterkeys()):
		if y < rows-1:
			y += 1
		else:
			y = 0
			x += 1
		imp = IJ.openImage(str(tiles[i]))
		scale = Scale(imp.getProcessor())
		ipscaled = ScaleImageToSize(scale, tilewidth, tileheight)
		hiresoutip.copyBits(ipscaled, x*tilewidth, y*tileheight, 0)
		hiresout.draw()


def SaveCoverFromZip(tileIndex, newwidth, newheight, cols, rows, originalspath):
	baseDir = re.sub(r'\/originals.zip', "", originalspath)

	#print baseDir

	zf = zipfile.ZipFile(originalspath, mode='r')

	tilewidth = int(newwidth/cols)
	tileheight = int(newheight/rows)

	newwidth = int(newwidth/tilewidth) * tilewidth
	newheight = int(newheight/tileheight) * tileheight

	hiresoutip = ColorProcessor(newwidth, newheight)
	hiresout = ImagePlus("hi res output", hiresoutip)
	hiresout.show()

	x = 0
	y = -1

	plane = []

	# scale the images
	for i in sorted(tileIndex.iterkeys()):
		if y < rows-1:
			y += 1
		else:
			y = 0
			x += 1
		#bi = bir.openImage(tileIndex[i]);
		#ip = ColorProcessor(bi)
		image = zf.read(str(tileIndex[i]) + ".jpeg")
		#IJ.log("Placing image :" + str(tileIndex[i]) + ".jpeg")
		my_file = open(baseDir + 'temporary.jpeg','w')
		my_file.write(image)
		my_file.close()
		imp = IJ.openImage(baseDir + "/temporary.jpeg")
		ip = imp.getProcessor()
		scale = Scale(ip)
		ipscaled = ScaleImageToSize(scale, tilewidth, tileheight)
		hiresoutip.copyBits(ipscaled, x*tilewidth, y*tileheight, 0)
		hiresout.draw()

class ResolutionListener(TextListener):
	def __init__(self, resField, widthPixels, heightPixels, widthInches, heightInches):
		self.resField = resField
		self.widthPixels = widthPixels
		self.heightPixels = heightPixels
		self.widthInches = widthInches
		self.heightInches = heightInches
	def textValueChanged(self, e):
		source = e.getSource()
		if source == self.resField:
			dpi = float(source.getText())
			width = float(self.widthInches.getText())
			height = float(self.heightInches.getText())
			self.widthPixels.setText(str(int(width * dpi)))
			self.heightPixels.setText(str(int(height * dpi)))
		elif source == self.widthInches:
			dpi = float(self.resField.getText())
			widthInches = float(source.getText())
			heightInches = widthInches/ratio
			self.heightInches.setText(str(heightInches))
			self.widthPixels.setText(str(int(float(self.widthInches.getText()) * dpi)))
			self.heightPixels.setText(str(int(float(self.heightInches.getText()) * dpi)))

def Dialog(imp):
	dpi = 300
	# a4 width in inches
	defaultWidth = 11.69
	defaultHeight = defaultWidth/ratio
	defaultAspectRatio = 1.41

	if imp:
		gd = GenericDialogPlus("Cover Maker")
		gd.addMessage("Input Options")
		gd.addFileField("Select image database", "", 20)
		gd.addMessage("Cover Maker Options")
		gd.addNumericField("tile width", 12, 0)
		gd.addNumericField("tile height", 9, 0)

		gd.showDialog()

		if gd.wasCanceled():
			print "User canceled dialog!"
			return
		databasepath = gd.getNextString()
		tilewidth = gd.getNextNumber()
		tileheight = gd.getNextNumber()

		return databasepath, imp.getWidth(), imp.getHeight(), int(tilewidth), int(tileheight)
	else:
		IJ.showMessage( "You should have at least one image open." )

def SaveDialog(imp):
	dpi = 300
	# a4 width in inches
	defaultWidth = 11.69
	defaultHeight = defaultWidth/ratio
	defaultAspectRatio = 1.41

	if imp:
		gd = GenericDialogPlus("Cover Maker")
		gd.addMessage("Saving options")
		gd.addNumericField("resolution (dpi)", dpi, 0)
		gd.addNumericField("width (pixels)", defaultWidth*dpi, 0)
		gd.addNumericField("height (pixels)", defaultHeight*dpi, 0)
		gd.addNumericField("width (inches)", defaultWidth, 2)
		gd.addNumericField("height (inches)", defaultHeight, 2)
		gd.addFileField("Select Originals database", "", 20)

		fields = gd.getNumericFields()

		resField = fields.get(0)
		widthPixels = fields.get(1)
		heightPixels = fields.get(2)
		widthInches = fields.get(3)
		heightInches = fields.get(4)

		# resolution and size listener
		textListener = ResolutionListener(resField, widthPixels, heightPixels, widthInches, heightInches)
		resField.addTextListener(textListener)
		widthInches.addTextListener(textListener)
		heightInches.addTextListener(textListener)

		gd.showDialog()

		if gd.wasCanceled():
			print "User canceled dialog!"
			return

		newres = gd.getNextNumber()
		newwidth = gd.getNextNumber()
		newheight = gd.getNextNumber()
		originalspath = gd.getNextString()

		return int(newwidth), int(newheight), newres, originalspath
	else:
		IJ.showMessage( "You should have at least one image open." )

# main body of the program
imp = IJ.getImage()
ratio = float(imp.getWidth()) / float(imp.getHeight())

#get options
(dbpath, width, height, tilewidth, tileheight) = Dialog(imp)

# run program
ip = CropInputImage(imp.getProcessor(), tilewidth, tileheight)
tileName, tileIndex, cols, rows = CreateCover(ip, tilewidth, tileheight, dbpath)

# save output
newwidth, newheight, res, originalspath = SaveDialog(imp)
if originalspath:
	SaveCoverFromZip(tileIndex, newwidth, newheight, cols, rows, originalspath)
else:
	SaveCoverFromFs(tileName, newwidth, newheight, cols, rows)
