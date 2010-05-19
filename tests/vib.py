#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

from java.io import File

from java.lang import System

import lib, sys

print System.getProperty('fiji.dir')

sys.exit(lib.launchFiji(['--headless', \
		'--main-class=org.junit.runner.JUnitCore', \
		'math3d.TestEigenvalueDecompositions', \
		'util.TestFindConnectedRegions', \
		'distance.TestMutualInformation', \
		'distance.TestEuclidean', \
		'distance.TestCorrelation', \
		'landmarks.TestLoading', \
		'util.TestPenalty', \
		'vib.TestFastMatrix', \
		'tracing.Test2DTracing', \
		'tracing.Test3DTracing'], \
	File(System.getProperty('fiji.dir') + '/VIB')))

'''
This does not work:

from org.junit.runner import JUnitCore

JUnitCore.main(['math3d.TestEigenvalueDecompositions', \
		'distance.TestMutualInformation', \
		'distance.TestEuclidean', \
		'distance.TestCorrelation', \
		'landmarks.TestLoading', \
		'util.TestPenalty', \
		'vib.TestFastMatrix', \
		'tracing.Test2DTracing', \
		'tracing.Test3DTracing'])

because the working directory is all wrong
'''
