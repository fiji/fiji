#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''

# This script checks if the ImageJ launcher for a given platform is
# up-to-date.
#
# The idea is to check every line of history (in reverse chronological
# order, from HEAD) stopping when the ImageJ launcher was modified.  If
# somewhere on that line, the source was modified, the launcher needs
# rebuilding.

import sys
from compat import execute

if len(sys.argv) < 3:
	print 'Usage:', sys.argv[0], '<source> <launcher>...'
	sys.exit(1)

source = sys.argv[1]
count = 0
for file in sys.argv[2:]:
	# Find out the edge commits (edges being the commits changing the
	# launcher, but having no offspring with the same property)

	edges = ''
	while True:
		edge = execute('git rev-list -1 HEAD ' + edges + ' -- ' + file)
		if edge == '':
			break
		edges = edges + ' ^' + edge

	if edges == '':
		print file, 'has not been committed yet'
		count += 1
		continue

	# Now verify that that the source has not changed since any of those
	# edges

	if execute('git rev-list HEAD ' + edges + ' -- ' + source) != '':
		print file, 'is not up-to-date.'
		count += 1
