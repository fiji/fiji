from ij import IJ, ImagePlus, ImageStack
from ij.io import FileSaver
from ij.process import ImageConverter, StackConverter
from mpicbg.ij.integral import Scale
import os
import sys
from os import path, walk
from loci.formats import ImageReader
from loci.formats import ImageWriter
from fiji.util.gui import GenericDialogPlus
from java.awt.event import TextListener
import zipfile
import zlib

def ScaleImageToSize(ip, width, height):
	"""Scale image to a specific size using Stephans scaler"""
	smaller = ip.scale( width, height )
	return smaller

def SaveToZip(zf, ip, baseDir, counter):
	fs = FileSaver(ip)
	fs.setJpegQuality(75)
	fs.saveAsJpeg(baseDir + "/tmp.jpeg")
	zipName = str(counter) + ".jpeg"
	zf.write(baseDir + "/tmp.jpeg", arcname=zipName)
	os.remove(baseDir + "/tmp.jpeg")

def DirList(baseDir):
	r = ImageReader()
	imgStats = {}
	for root, dirs, files in os.walk(str(baseDir)):
		for f1 in files:
			if f1.endswith(".jpg") or f1.endswith(".jpe") or f1.endswith(".jpeg"):
				id = root + "/" +  f1
				r.setId(id)
				if r is None:
					print "Couldn\'t open image from file:", id
					continue
				w = r.getSizeX()
				h = r.getSizeY()
				imgStats[str(w) + "_" + str(h)] = imgStats.get(str(w) + "_" + str(h), 0)+1
				IJ.log("Found image: " + str(id))
				#counter += 1
	r.close()
	#print summary
	summary = ''
	for k, v in imgStats.iteritems():
		dim = k.split("_")
		ratio = float(dim[0])/float(dim[1])
		IJ.log("Found " + str(v) + " images of dimension " + str(dim[0]) + "x" + str(dim[1]) + " apect ratio " + str(round(ratio, 2)))
		summary = summary + "\nFound " + str(v) + " images of dimension " + str(dim[0]) + "x" + str(dim[1]) + " apect ratio " + str(round(ratio, 2))
	return summary

def PrepareDatabase(minw, maxw, baseDir, aspectRatio, majorWidth, majorHeight):
	outputpath = baseDir + "/" + str(majorWidth) + "_" + str(majorHeight) + "_orig.tif"
	#initialize stacks and labels
	stackScaled = []
	stackOrig = ImageStack(majorWidth, majorHeight)
	imageNames = []
	for i in range(minw, maxw+1):
		stackScaled.append(ImageStack(i, int(round(i/aspectRatio, 0))))
		imageNames.append('')

	counter = 0

	# initialize zip file for originals
	zf = zipfile.ZipFile(baseDir + "/originals.zip", mode='w', compression=zipfile.ZIP_DEFLATED, allowZip64=1)
	zf.writestr('from_string.txt', 'hello')
	zf.close()
	zf = zipfile.ZipFile(baseDir + "/originals.zip", mode='a', compression=zipfile.ZIP_DEFLATED, allowZip64=1)

	for root, dirs, files in os.walk(str(baseDir)):
		for f1 in files:
			if f1.endswith(".jpg") or f1.endswith(".jpe") or f1.endswith(".jpeg"):
				id = root + "/" +  f1
				IJ.redirectErrorMessages()
				IJ.redirectErrorMessages(1)
				imp = IJ.openImage(id)
				if imp is None:
					print "Couldn\'t open image from file:", id
					continue
				# skip non RGBimages
				if imp.getProcessor().getNChannels() != 3:
					print "Converting non RGB image:", id
					if imp.getStackSize() > 1:
						StackConverter(imp).convertToRGB()
					else:
						ImageConverter(imp).convertToRGB()
				#skip images with different aspect ratio
				width = imp.getWidth()
				height = imp.getHeight()
				ratio = round(float(width)/float(height), 2) # this makes the ratio filering approximate, minor variations in image dimensions will be ignored
				if ratio != aspectRatio:
					IJ.log("Skipping image of size: " + str(width) + "," + str(height))
					continue
				# now scale the image within a given range
				scale = Scale(imp.getProcessor())
				IJ.log("Scaling image " + str(counter) + " " + str(id))
				for i in range(minw, maxw+1):
					stackScaled[i-minw].addSlice(None, ScaleImageToSize(scale, i, int(round(i/aspectRatio, 0))))
					imageNames[i-minw] += str(id) + ";"
				# save the originals to a temp directory
				scaledOrig = ImagePlus(None, ScaleImageToSize(scale, majorWidth, majorHeight))
				SaveToZip(zf, scaledOrig, baseDir, counter)
				counter += 1
	zf.close()
	# save the stacks
	for i in range(minw, maxw+1):
		impScaled = ImagePlus(str(minw) + "_" + str(int(round(i/aspectRatio, 0))), stackScaled[i-minw])
		impScaled.show()
		#print imageNames
		impScaled.setProperty('Info', imageNames[i-minw][:-1])
		fs = FileSaver(impScaled)
		filepath = baseDir + "/" + str(i) + "_" + str(int(round(i/aspectRatio, 0))) + ".tif"
		IJ.log("Saving output stack" + str(filepath))
		fs.saveAsTiffStack(filepath)
		#IJ.save(impScaled, filepath);
		IJ.log("Done")



def DialogAnalyze():
	dpi = 300
	defaultAspectRatio = 1.41

	gd = GenericDialogPlus("Cover Maker")
	gd.addMessage("Prepare Image database")
	gd.addDirectoryField("Select base directory containing images", "", 20)

	gd.showDialog()

	if gd.wasCanceled():
		print "User canceled dialog!"
		return
	imageBaseDir = gd.getNextString()

	return imageBaseDir

class RatioToDim(TextListener):
	def __init__(self, aspRatio, minw, maxw, minh, maxh):
		self.aspRatio = aspRatio
		self.minw = minw
		self.maxw = maxw
		self.minh = minh
		self.maxh = maxh
	def textValueChanged(self, e):
		source = e.getSource()
		if source == self.aspRatio:
			#print "bla " + str(self.minw.getText)# + " " + str(float(source.getText()))
			self.minh.setText(str(int(round(float(self.minw.getText())/float(source.getText())))))
			self.maxh.setText(str(int(round(float(self.maxw.getText())/float(source.getText())))))
		elif source == self.minw:
			self.minh.setText(str(int(round(float(source.getText())/float(self.aspRatio.getText()), 0))))
		elif source == self.maxw:
			self.maxh.setText(str(int(round(float(source.getText())/float(self.aspRatio.getText()), 0))))

def DialogGenerate(imageBaseDir, summary):
	dpi = 300
	defaultAspectRatio = 1.33
	defaultTileWidth = 15
	defaultOriginalWidth = 150
	defaultOriginalHeight = 113
	defaultTileHeight = round(defaultTileWidth/defaultAspectRatio)

	gd = GenericDialogPlus("Cover Maker")
	gd.addMessage("Prepare Image database")
	gd.addDirectoryField("Select base directory containing images", imageBaseDir, 20)
	gd.addMessage(summary)
	gd.addNumericField("Aspect ratio", defaultAspectRatio, 2)
	gd.addNumericField("Original width", defaultOriginalWidth, 0)
	gd.addNumericField("Original height", defaultOriginalHeight, 0)
	gd.addNumericField("minimal tile width", defaultTileWidth, 0)
	gd.addNumericField("maximal tile width", defaultTileWidth, 0)
	gd.addNumericField("minimal tile height", defaultTileHeight, 0)
	gd.addNumericField("maximal tile height", defaultTileHeight, 0)

	fields = gd.getNumericFields()

	aspRatio = fields.get(0)
	minw = fields.get(3)
	maxw = fields.get(4)
	minh = fields.get(5)
	maxh = fields.get(6)

	# resolution and size listener
	textListener = RatioToDim(aspRatio, minw, maxw, minh, maxh)
	aspRatio.addTextListener(textListener)
	minw.addTextListener(textListener)
	maxw.addTextListener(textListener)

	gd.showDialog()

	if gd.wasCanceled():
		print "User canceled dialog!"
		return
	imageBaseDir = gd.getNextString()
	aspectRatio = gd.getNextNumber()
	majorWidth = gd.getNextNumber()
	majorHeight = gd.getNextNumber()
	mintilewidth = gd.getNextNumber()
	maxtilewidth = gd.getNextNumber()

	return int(mintilewidth), int(maxtilewidth), imageBaseDir, float(aspectRatio), int(majorWidth), int(majorHeight)

imageBaseDir = ''
summary = ''

#imageBaseDir = DialogAnalyze()
#summary = DirList(imageBaseDir)
(minw, maxw, imageBaseDir, aspectRatio, majorWidth, majorHeight) = DialogGenerate(imageBaseDir, summary)
PrepareDatabase(minw, maxw, imageBaseDir, aspectRatio, majorWidth, majorHeight)
