#!/bin/bash

set -e
set -x

export GIT_SSH=/home/mark/bin/ssh-pacific

cd ~/fiji-for-debian/

rm -rf fiji_* fiji*.deb

if [ ! -e fiji ]
then

    # If the fiji symlink isn't there, assume we have to start
    # from scratch:

    rm -rf fiji-*
    git clone contrib@fiji.sc:/srv/git/fiji.git
    ( cd fiji &&
	git submodule update --init \
	    modules/AutoComplete \
	    modules/ImageJA \
	    modules/RSyntaxTextArea \
	    modules/TrakEM2 \
	    modules/bio-formats \
	    modules/commons-math \
	    modules/ij-plugins \
	    modules/imglib \
	    modules/jython \
	    modules/mpicbg \
	    modules/tcljava \
	    modules/weka \
	    modules/imagej2 \
	    modules/image5d
    )

    VERSION=$( cd fiji && dpkg-parsechangelog | egrep '^Version' | sed 's/^Version: //' )
    D=fiji-$VERSION

    mv fiji $D
    ln -s $D fiji

elif [ -L fiji ]
then

  VERSION=$( cd fiji && dpkg-parsechangelog | egrep '^Version' | sed 's/^Version: //' )
  OLD_D=$(readlink -n fiji)
  OLD_D=${OLD_D%/}
  D=fiji-$VERSION
  if [ "$D" != "$OLD_D" ]
  then
    mv $OLD_D $D
    rm fiji
    ln -s $D fiji
  fi

else

  echo "fiji existed, but was not a symlink"
  exit 1

fi

# This cleaning logic is taken from bin/nightly-build.sh in Fiji:

( cd $D &&
    bin/gitignore-in-submodules.sh submodule
    git fetch origin master &&
    git reset --hard FETCH_HEAD &&
    git submodule update &&
    git clean -q -x -d -f &&
    for d in $(git ls-files --others --directory)
    do
	rm -rf $d || break
    done &&
    for submodule in $(git ls-files --stage |
	sed -n 's/^160000 .\{40\} 0.//p')
    do
	(cd "$submodule" &&
                         git clean -q -x -d -f &&
                         # remove empty directories
                         for d in $(git ls-files --others --directory)
                         do
                                rm -rf $d || break
                         done)
    done
)

OLD_REVISION=$(cd $D && git rev-parse HEAD)

( cd $D &&
    debian/update-debian.py --add-changelog-template "This is the automatic weekly build" &&
    git commit -m "Add the changelog, updated with the weekly build version" debian/changelog
)

NEW_VERSION=$( cd fiji && dpkg-parsechangelog | egrep '^Version' | sed 's/^Version: //' )
if [ $VERSION != $NEW_VERSION ]
then
    rm fiji
    mv fiji-$VERSION fiji-$NEW_VERSION
    D=fiji-$NEW_VERSION
    ln -s $D fiji
fi

( cd $D &&
    debian/complete-build &&
    debian/build-in-i386-chroot.py &&
    debian/upload-to-pacific.py &&
    NEW_REVISION=$(git rev-parse HEAD) &&
    git reset --hard $OLD_REVISION &&
    git merge --squash $NEW_REVISION &&
    git commit -m "debian: Changes produced by the weekly build" &&
    git fetch origin master &&
    git rebase FETCH_HEAD &&
    git config remote.origin.url longair@fiji.sc:/srv/git/fiji.git &&
    git push origin master
)
