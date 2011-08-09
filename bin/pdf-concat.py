#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

from com.itextpdf.text import Document
from com.itextpdf.text.pdf import PdfReader, PdfCopy, SimpleBookmark
from java.io import FileOutputStream
from java.util import ArrayList
import sys

# This Python file is a straight translation of the Concatenate example

if len(sys.argv) < 3:
	print 'Usage:', sys.argv[0], 'source(s).pdf...', 'target.pdf'
	sys.exit(1)

copy = None
all_bookmarks = ArrayList()
page_offset = 0

for file in sys.argv[1:len(sys.argv) - 1]:
	reader = PdfReader(file)
	reader.consolidateNamedDestinations()
	bookmarks = SimpleBookmark.getBookmark(reader)
	if bookmarks != None:
		if page_offset != 0:
			SimpleBookmark.shiftPageNumbers(bookmarks, \
				page_offset, None)
		all_bookmarks.add(bookmarks)

	page_count = reader.getNumberOfPages()
	page_offset += page_offset

	if copy == None:
		document = Document(reader.getPageSizeWithRotation(1))
		output = FileOutputStream(sys.argv[len(sys.argv) - 1])
		copy = PdfCopy(document, output)
		document.open()

	print "Adding", page_count, "pages from", file

	for k in range(0, page_count):
		copy.addPage(copy.getImportedPage(reader, k + 1))

	if reader.getAcroForm() != None:
		copy.copyAcroForm(reader)

if not all_bookmarks.isEmpty():
	copy.setOutlines(all_bookmarks)

if document != None:
	document.close()
