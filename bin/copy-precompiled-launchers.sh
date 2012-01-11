#!/bin/sh

cd $(dirname "$0")/..

for launcher in precompiled/ImageJ-*
do
	file=${launcher#*/}
	prefix=.
	case $launcher in *-macosx|*-tiger)
		prefix=Contents/MacOS;;
	esac
	cp $launcher $prefix/$file
	fiji=fiji-${file#ImageJ-}
	test $fiji = fiji-linux32 && fiji=fiji-linux
	cp $launcher $prefix/$fiji
done
