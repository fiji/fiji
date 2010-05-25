#!/bin/bash

#
# check-class-version.sh
#

# Checks class version for the given arguments (typically JAR files).
#
# Author: Curtis Rueden (ctrueden at wisc.edu)

FIJIROOT="$(dirname "$0")/.."
FIJI="$FIJIROOT"/fiji

function canonpath() {
  echo $(cd $(dirname $1); pwd -P)/$(basename $1)
}

# get canonical path name for each argument
count=0
for f in $*
do
  file[$count]="$(canonpath $f)"
  let count=count+1
done

# run CheckClassVersions on each argument
cd $FIJIROOT
idx=0
while [ $idx -lt $count ]
do
  f=${file[$idx]}
  let idx=idx+1
  echo "=== $f ==="
  ./fiji fiji.CheckClassVersions.class "$f"
done
