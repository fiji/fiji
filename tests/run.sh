#!/bin/sh

# Tests for fiji

cd "$(dirname "$0")"/.. &&
./Build.sh jars/test-fiji.jar &&
./fiji --main-class=fiji.Tests
