#!/bin/sh

# This script verifies that all the services of fiji.sc work as expected

set -e

echo Fiji Wiki
curl http://fiji.sc/Fiji | grep "Fiji Is Just ImageJ"

echo Fiji Wiki legacy
curl http://fiji.sc/wiki/Fiji | grep "Fiji Is Just ImageJ"

echo Fiji Wiki legacy \#2
curl -L http://fiji.sc/mediawiki/phase3/Fiji | grep "Fiji Is Just ImageJ"

echo Fiji Wiki page starting with a digit
curl -L http://fiji.sc/2013-02-25_-_TrackMate_v2.0.0_released | grep "We just released"

echo Smart HTTP
git ls-remote http://fiji.sc/fiji.git master

echo GitWeb
curl -L http://fiji.sc/git/ | grep fiji.git

echo GitWeb, link to repository
curl -L http://fiji.sc/git/fiji.git | grep commitdiff
