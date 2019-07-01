#!/bin/sh
curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/master/travis-build.sh
sh travis-build.sh $encrypted_ae5c55655408_key $encrypted_ae5c55655408_iv

# -- Generate component sidebars and summary tables --

# Discern needed component versions.
imagej_version=$(mvn dependency:list | grep 'net.imagej:imagej:' | sed 's/.*:\([0-9][^:]*\):.*/\1/')
fiji_version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

# Write out the wiki upload credentials.
echo "machine imagej.net" > ~/.netrc
echo "        login TravisCI" >> ~/.netrc
echo "        password $WIKI_PASSWORD" >> ~/.netrc

# Clone the wiki content generator.
git clone git://github.com/scijava/mediawiki-maven-info
cd mediawiki-maven-info

# Generate the content and upload!
mvn -Dinfo.url=https://imagej.net/ \
    -Dmwmi.groupId=net.imagej \
    -Dmwmi.artifactId=imagej \
    -Dmwmi.version="$imagej_version" \
    -Dmwmi.includeBase \
    -Dmwmi.groupId2=sc.fiji \
    -Dmwmi.artifactId2=fiji \
    -Dmwmi.version2="$fiji_version" \
    -Dmwmi.includeBase2
