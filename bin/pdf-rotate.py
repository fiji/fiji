#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

from com.itextpdf.text.pdf import PdfReader, PdfName, PdfNumber, PdfStamper
from java.io import FileOutputStream
import sys

if len(sys.argv) != 3:
	print 'Usage:', sys.argv[0], 'source.pdf', 'target.pdf'
	sys.exit(1)

reader = PdfReader(sys.argv[1])
for k in range(0, reader.getNumberOfPages()):
	reader.getPageN(k + 1).put(PdfName.ROTATE, PdfNumber(90))
	print "rotated", k

stamper = PdfStamper(reader, FileOutputStream(sys.argv[2]))
stamper.close()
