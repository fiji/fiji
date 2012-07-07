#!/bin/sh

cd "$(dirname "$0")/.." &&
(cd modules/Retrotranslator &&
 ../../bin/ImageJ.sh --ant build) &&
git rm retro/retrotranslator-\*.jar &&
cp modules/Retrotranslator/build/retrotranslator-*.jar retro/ &&
git add retro/retrotranslator-*.jar &&
git rm -f retro/retrotranslator-runtime13*.jar
