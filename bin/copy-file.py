#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''

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

from java.io import File
if File(sys.argv[1]).canExecute():
	File(sys.argv[2]).setExecutable(True)
