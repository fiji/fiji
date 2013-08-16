#!/bin/sh
''''exec "$(dirname "$0")"/ImageJ.sh --jython "$0" "$@" # (call again with fiji)'''

from org.jpedal import PdfDecoder
from ij import ImageJ, ImagePlus
import sys

if len(sys.argv) != 2:
	print 'Usage:', sys.argv[0], 'source.pdf'
	sys.exit(1)

ij = None

decoder = PdfDecoder(False)
decoder.setExtractionMode(PdfDecoder.RAWIMAGES | PdfDecoder.FINALIMAGES)
decoder.openPdfFile(sys.argv[1])

for page in range(0, decoder.getPageCount()):
	decoder.decodePage(page + 1)
	images = decoder.getPdfImageData()
	image_count = images.getImageCount()
	for i in range(0, image_count):
		name = images.getImageName(i)
		image = decoder.getObjectStore().loadStoredImage('R' + name)
		if ij == None:
			ij = ImageJ()
			ij.exitWhenQuitting(True)
		ImagePlus(name, image).show()
	decoder.flushObjectValues(True)
decoder.closePdfFile()
