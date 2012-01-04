#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''

import os
import sys
from compat import execute

if len(sys.argv) < 3:
	print 'Usage: ', sys.argv[0], ' <zipfile> <folder>'
	exit(1)

zipfile = sys.argv[1]
folder = sys.argv[2]

verbose = False

print 'Making', zipfile, 'from', folder

if os.name == 'java':
	from java.io import FileOutputStream
	from java.util.zip import ZipOutputStream, ZipEntry

	def add_folder(zip, folder):
		for file in os.listdir(folder):
			file = folder + '/' + file
			if os.path.isdir(file):
				add_folder(zip, file)
			elif os.path.isfile(file):
				if verbose:
					print file
				entry = ZipEntry(file)
				zip.putNextEntry(entry)
				f = open(file, "rb")
				zip.write(f.read())
				f.close()
				zip.closeEntry()

	output = FileOutputStream(zipfile)
	zip = ZipOutputStream(output)
	add_folder(zip, folder)
	zip.close()
else:
	execute('zip -9r ' + zipfile + ' ' + folder)
