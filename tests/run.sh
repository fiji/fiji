#!/bin/sh

# Tests for fiji

cd "$(dirname "$0")"

# Run all jython tests contained in this current folder

for script in *.py
do
	../fiji --jython $script || {
		echo "Failed test: $script"
		exit 1
	}
done
