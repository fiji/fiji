#!/bin/sh

set -e

cd "$(dirname "$0")"/../VIB/

exec ../fiji \
    --headless \
    --main-class \
    org.junit.runner.JUnitCore \
    math3d.TestEigenvalueDecompositions \
    distance.TestMutualInformation \
    distance.TestEuclidean \
    distance.TestCorrelation \
    landmarks.TestLoading \
    util.TestPenalty \
    vib.TestFastMatrix \
    tracing.Test2DTracing \
    tracing.Test3DTracing
