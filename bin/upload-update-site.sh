#!/bin/sh
set -x

# Instructions :

# Continous Integration and automatic upload to SciJava Maven repository.

# - Enable Travis for your GitHub repo at https://travis-ci.org/GITHUB_NAME/GITHUB_REPO.
# - Run the [travisify.sh](https://github.com/scijava/scijava-scripts/blob/master/travisify.sh) script on your repo.
# - Starting from here, Travis will build and deploys `SNAPSHOT` build for each new commit pushed to `master` to the [SciJava Maven repo](https://maven.scijava.org/).

# - To release a new version, you have to run the [`release-version.sh`](https://github.com/scijava/scijava-scripts/blob/master/release-version.sh) script.

# Automatic upload to ImageJ update site released artifacts.

# - Visit the Travis setting page (https://travis-ci.org/GITHUB_NAME/GITHUB_REPO/settings).
# - Add the following three environment variables:

#   - `UPDATE_SITE_NAME` : The name of the ImageJ update site.
#   - `UPDATE_SITE_PASSWORD` : The associated password.
#   - `UPLOAD_WITH_DEPENDENCIES` : Wether or not you want to upload only your package with its dependencies. This value should be `true` or `false`.

# - From the root of your repository run:

# ```sh
# cat >> .travis/build.sh <<EOL
# curl -fsLO https://raw.githubusercontent.com/fiji/fiji/master/bin/upload-update-site.sh
# sh upload-update-site.sh
# EOL
# ```

# - Commit and push your changes to GitHub.
# - Release a new version.
# - Your package should be uploaded to your update ImageJ site.

if [ -z "$UPDATE_SITE_NAME" ]; then
    # Silently exit if UPDATE_SITE_NAME is not set.
    exit 0
fi

if [ -n "$TRAVIS_TAG" ]; then

    echo "** Travis tag detected. Start uploading the specified update site **"

    if [ -z "$UPDATE_SITE_PASSWORD" ]; then
        echo "The variable UPDATE_SITE_PASSWORD is not set. You need to set it in the Travis configuration."
        exit -1
    fi

    if [ -z "$UPLOAD_WITH_DEPENDENCIES" ]; then
        echo "The variable UPLOAD_WITH_DEPENDENCIES is not set. You need to set it in the Travis configuration."
        echo "It can be either 'true' or 'false'."
        exit -1
    fi

    if [ -z "$UPDATE_SITE_USER" ]; then
        UPDATE_SITE_USER=$UPDATE_SITE_NAME
    fi

    echo "* Setup variables."
    URL="http://sites.imagej.net/$UPDATE_SITE_NAME/"
    IJ_PATH="$HOME/Fiji.app"
    IJ_LAUNCHER="$IJ_PATH/ImageJ-linux64"

    echo "* Installing Fiji."
    mkdir -p $IJ_PATH/
    cd $HOME/
    wget -q "https://downloads.imagej.net/fiji/latest/fiji-linux64.zip"
    unzip -q fiji-linux64.zip

    echo "* Updating Fiji."
    $IJ_LAUNCHER --update update-force-pristine

    cd $TRAVIS_BUILD_DIR/

    echo "* Gather some project informations."
    VERSION=`mvn help:evaluate -Dexpression=project.version | grep -e '^[^\[]'`
    NAME=`mvn help:evaluate -Dexpression=project.name | grep -e '^[^\[]'`

    echo "* Adding $URL as an Update Site."
    $IJ_LAUNCHER --update edit-update-site $UPDATE_SITE_NAME $URL "webdav:$UPDATE_SITE_USER:$UPDATE_SITE_PASSWORD" .

    if [ "$UPLOAD_WITH_DEPENDENCIES" = false ]; then

        echo "* Install project to Fiji directory."
        mvn clean install -Dimagej.app.directory=$IJ_PATH -Ddelete.other.versions=true -Dscijava.ignoreDependencies=true

        echo "* Upload only \"jars/$NAME.jar\"."
        $IJ_LAUNCHER --update upload --update-site "$UPDATE_SITE_NAME" --force-shadow --forget-missing-dependencies "jars/$NAME.jar"

    else

        echo "* Install project to Fiji directory."
        mvn clean install -Dimagej.app.directory=$IJ_PATH -Ddelete.other.versions=true

        echo "* Upload $NAME with its dependencies."
        $IJ_LAUNCHER --update upload-complete-site --force-shadow "$UPDATE_SITE_NAME"

    fi
else
    echo "== Travis tag not detected. Don't perform upload to update site =="
fi
