#!/bin/sh

case $# in
0) n=10;;
*) n=$1;;
esac

build_ij () {
	version=$(git show |
		sed -n 's/.* \(1\.4[0-9a-z]*\).*/\1/p' |
		head -n 1) &&
	../../Build.sh ij.jar &&
	mkdir -p ../../ij-versions/$version &&
	cp ij.jar ../../ij-versions/$version/
}

cd "$(dirname "$0")"/../modules/ImageJA &&
git checkout origin/imagej^0 &&
cat > Fakefile << \EOF &&
javaVersion=1.5
all <- ij.jar

MAINCLASS(ij.jar)=ij.ImageJ
ij.jar <- **/*.java \
        microscope.gif[images/microscope.gif] about.jpg[images/about.jpg] \
        IJ_Props.txt macros/*.txt
EOF
i=0 &&
while test $i -lt $n
do
	if test $i -gt 0
	then
		git checkout HEAD^
	fi &&
	build_ij &&
	i=$(($i+1)) || {
		echo "Failed at imagej^$i"
		break
	}
done
