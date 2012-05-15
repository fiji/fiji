#!/bin/sh
''''exec "$(dirname "$0")"/ImageJ.sh --jython "$0" "$@" # (call again with fiji)'''

# This script replaces all .policy files in a given directory structure
# with files that grant all

import os
import sys

def replace_in(path):
	if os.path.isdir(path):
		for file in os.listdir(path):
			replace_in(path + '/' + file)
	elif path.endswith('.policy'):
		out = open(path, 'w')
		out.write("grant {\n")
		out.write("\tpermission java.security.AllPermission;\n")
		out.write("};\n")
		out.close()

for dir in sys.argv:
	replace_in(dir)
