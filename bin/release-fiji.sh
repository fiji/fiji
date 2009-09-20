#!/bin/sh

# This script is used to make a release of Fiji on pacific.  The precompiled
# launchers for MacOSX cannot be cross-compiled yet, so you need a MacOSX10.5
# ssh account to build, and you need to alias it to "macosx10.5" in your
# ~/.ssh/config.

# The MacOSX launchers are made on that machine, a (partial) commit is done,
# which is then pulled into a clean checkout on pacific, and augmented by
# the cross-compiled launchers for Windows and 32-bit Linux, as well as the
# launcher for 64-bit Linux.

# Then, everything is tagged, and put into the download directory.

RELEASE="$1"
test -z "$RELEASE" && {
	echo "Need a release"
	exit 1
}
case "$RELEASE" in
*" "*|*"	"*)
	echo "Release name must not contain spaces"
	exit 1
esac

HEAD="$2"
HEAD=$(git rev-parse ${HEAD:-HEAD})

TARGET_DIRECTORY=/var/www/downloads/$RELEASE
NIGHTLY_BUILD=fiji/nightly-build
TMP_HEAD=refs/heads/tmp

clone='
	cd $HOME &&
	test -d '$NIGHTLY_BUILD' ||
	git clone git://pacific.mpi-cbg.de/fiji.git '$NIGHTLY_BUILD

checkout_and_build='
	cd $HOME/'$NIGHTLY_BUILD' &&
	(git checkout -f master &&
	 git reset --hard '$TMP_HEAD' &&
	 for d in $(git ls-files --stage | sed -n "s/^160.*	//p");
	 do
		case "$d" in
		java/*)
			git submodule update --init $d &&
			continue ||
			break;;
		esac &&
		test -z "$(ls $d/)" || break;
	 done &&
	 ./bin/nightly-build.sh --stdout) >&2 &&
	./bin/calculate-checksums.py
'

COMMIT_MESSAGE="Precompile Fiji and Fake for $RELEASE"

build_dmg='
	(cd $HOME/'$NIGHTLY_BUILD' &&
	 ./Build.sh precompile-fiji precompile-fake dmg &&
	 if ! git diff-files -q --exit-code --ignore-submodules
	 then
		git commit -s -a -m "'"$COMMIT_MESSAGE"'"
	 fi) >&2
'

build_rest='
	(cd $HOME/'$NIGHTLY_BUILD' &&
	 ./Build.sh all-cross precompile-fiji all-tars all-zips all-7zs &&
	 if ! git diff-files -q --exit-code --ignore-submodules
	 then
		git commit -s -a -m "'"$COMMIT_MESSAGE"'"
	 fi) >&2
'

git diff-files --ignore-submodules --quiet &&
git diff-index --cached --quiet HEAD || {
	echo "There are uncommitted changes!" >&2
	exit 1
}

git rev-parse --verify refs/tags/Fiji-$RELEASE 2>/dev/null && {
	echo "Tag Fiji-$RELEASE already exists!" >&2
	exit 1
}

echo "Building for MacOSX" &&
if ! git push macosx10.5:$NIGHTLY_BUILD +$HEAD:$TMP_HEAD
then
	ssh macosx10.5 "$clone" &&
	git push macosx10.5:$NIGHTLY_BUILD +$HEAD:$TMP_HEAD
fi &&
macsums="$(ssh macosx10.5 "$checkout_and_build")" &&
ssh macosx10.5 "$build_dmg" &&

echo "Building for non-MacOSX" &&
git fetch macosx10.5:$NIGHTLY_BUILD master && # for fiji-macosx.7z
if ! git push $HOME/$NIGHTLY_BUILD +FETCH_HEAD:$TMP_HEAD
then
	(eval $clone) &&
	git push $HOME/$NIGHTLY_BUILD +FETCH_HEAD:$TMP_HEAD
fi &&
thissums="$(sh -c "$checkout_and_build")" &&
sh -c "$build_rest" &&

if test "$macsums" != "$thissums"
then
	echo "The build results are different!"
	echo "$macsums" > .git/macsums
	echo "$thissums" > .git/thissums
	git diff --no-index .git/macsums .git/thissums
	exit 1
fi &&

echo "Tagging" &&
git fetch macosx10.5:$NIGHTLY_BUILD master &&
git read-tree -u -m FETCH_HEAD &&
git fetch $HOME/$NIGHTLY_BUILD master &&
git read-tree -u -m FETCH_HEAD &&
if ! git diff-index --cached --quiet HEAD
then
	git commit -s -a -m "$COMMIT_MESSAGE"
fi &&
git tag -m "Fiji $RELEASE" Fiji-$RELEASE &&
git push /srv/git/fiji.git master Fiji-$RELEASE &&

echo "Uploading" &&
(cd $HOME/$NIGHTLY_BUILD &&
 scp macosx10.5:$NIGHTLY_BUILD/fiji-macosx.dmg ./ &&
 mkdir -p $TARGET_DIRECTORY &&
 date=$(date +%Y%m%d) &&
 for f in fiji-*
 do
	mv $f $TARGET_DIRECTORY/${f%%.*}-$date.${f#*.} || break
 done)
