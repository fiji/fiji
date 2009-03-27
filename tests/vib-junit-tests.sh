#!/bin/sh

set -e

cd ../VIB/

exec ../fiji \
    --headless \
    --class-path \
    junit-4.4.jar \
    --main-class \
    org.junit.runner.JUnitCore \
    math3d.TestEigenvalueDecompositions \
    distance.TestMutualInformation \
    distance.TestEuclidean \
    distance.TestCorrelation \
    landmarks.TestLoading \
    util.TestPenalty \
    vib.TestFastMatrix
