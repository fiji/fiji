#!/bin/sh
curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/master/travis-build.sh
sh travis-build.sh $encrypted_ae5c55655408_key $encrypted_ae5c55655408_iv

# -- Generate component sidebars and summary tables --

if [ "$TRAVIS_SECURE_ENV_VARS" = true \
  -a "$TRAVIS_PULL_REQUEST" = false \
  -a "$TRAVIS_BRANCH" = master ]
then
  echo
  echo '== Regenerating wiki content =='

  # Discern needed component versions.
  imagej_version=$(mvn help:evaluate -Dexpression=imagej.version -q -DforceStdout)
  echo "- ImageJ version is $imagej_version"
  fiji_version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
  echo "- Fiji version is $fiji_version"

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
else
  echo 'Skipping wiki content regeneration.'
fi
