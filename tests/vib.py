#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

from java.io import File

from java.lang import System

import lib, sys, os, errno, urllib
from os.path import realpath

fiji_dir = System.getProperty('fiji.dir')
images_dir = os.path.join(fiji_dir, 'tests', 'sample-data')

# Ensure that the sample-data directory exists:
try:
	os.mkdir(images_dir)
except OSError, e:
	if e.errno != errno.EEXIST:
		raise

# Download any required test images that are missing:
for filename in (
		'CantonF41c-reduced.tif.points.xml',
		'CantonF41c-reduced.tif',
		'tidied-mhl-62yxUAS-lacZ0-reduced.tif.points.R',
		'tidied-mhl-62yxUAS-lacZ0-reduced.tif',
		'tidied-mhl-62yxUAS-lacZ0-reduced.tif',
		'71yAAeastmost.labels.points',
		'71yAAeastmost.labels',
		'c005BA.labels',
		'181y-12bit-aaarrg-dark-detail-reduced.tif',
		'181y-12bit-aaarrg-mid-detail-reduced.tif',
		'181y-12bit-aaarrg-bright-reduced.tif',
		'tidied-mhl-62yxUAS-lacZ0-reduced.tif.points.R',
		'c061AG-small-section-z-max.tif',
		'c061AG-small-section.tif'):
	destination = os.path.join(images_dir, filename)
	url = 'http://fiji.sc/test-data/' + urllib.quote(filename)
	if not os.path.exists(destination):
		print 'Downloading', filename
		urllib.urlretrieve(url, destination)

if realpath(os.getcwd()) != realpath(fiji_dir):
    print >> sys.stderr, "The tests must be run from", realpath(fiji_dir)
    sys.exit(1)

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
