#!/bin/sh
''''exec "$(dirname "$0")"/ImageJ.sh --jython "$0" "$@" # (call again with fiji)'''

import os
import sys

if len(sys.argv) < 2:
	print 'Need a path to the JDK'
	sys.exit(1)

jdk = sys.argv[1]

if not os.path.isdir(os.path.join(jdk, '.git')):
        print 'Initializing ', jdk
	if os.system('git submodule init ' + jdk) \
			or os.system('git submodule update ' + jdk):
		print 'Could not check out ', jdk
		sys.exit(1)
else:
	print 'Updating ', jdk
	if os.system('cd ' + jdk  \
			+ ' && git pull origin master'):
		print 'Could not update ', jdk
		sys.exit(1)
