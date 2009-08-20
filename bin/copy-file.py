#!/usr/bin/python

import os
import shutil
import sys

if len(sys.argv) < 3:
	print 'Usage: ' + sys.argv[0] + ' <source> <target>'
	exit(1)

if not os.path.exists(sys.argv[1]) and os.path.exists(sys.argv[1] + '.exe'):
	sys.argv[1] += '.exe'
	sys.argv[2] += '.exe'

shutil.copyfile(sys.argv[1], sys.argv[2])
