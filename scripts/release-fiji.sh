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

TARGET_DIRECTORY=/var/www/downloads/$RELEASE

cd $HOME

clone_and_check='(test -d release-fiji ||
	git clone git://pacific.mpi-cbg.de/fiji.git release-fiji) &&
	cd release-fiji &&
	git diff-files &&
	git diff-index HEAD &&
	git checkout -f master &&
	git fetch origin &&
	git reset --hard origin/master &&
	git clean -d -f &&
	for d in $(git ls-files --stage | sed -n "s/^160.*	//p");
	do
		case "$d" in
		java/*)
			git submodule update --init $d || break
			continue;;
		esac
		test -z "$(ls $d/)" || break;
	done'

COMMIT_MESSAGE="Precompile Fiji and Fake for $RELEASE"

ssh macosx10.5 "$clone_and_check &&
	(test ! -z \"\$(git rev-parse --verify mactmp-$RELEASE 2>/dev/null)\" ||
	 (./Build.sh precompile-fiji precompile-fake dmg &&
	  git checkout -b mactmp-$RELEASE &&
	  (git commit -a -s -m \"$COMMIT_MESSAGE\" ||
	   true)))" &&

eval "$clone_and_check" &&
! git rev-parse --verify refs/tags/Fiji-$RELEASE &&
git pull macosx10.5:release-fiji mactmp-$RELEASE &&
./Build.sh all-cross precompile-fiji all-tars all-zips &&
if test "$COMMIT_MESSAGE" = "$(git log -1 --pretty=format:%s HEAD)"
then
	GIT_EDITOR=: git commit --amend -a
else
	git commit -a -s -m "$COMMIT_MESSAGE" || true
fi &&
git tag Fiji-$RELEASE &&
git push /srv/git/fiji.git master Fiji-$RELEASE &&
mkdir -p $TARGET_DIRECTORY &&
mv *.tar.bz2 *.zip $TARGET_DIRECTORY &&
cd $TARGET_DIRECTORY &&
scp macosx10.5:release-fiji/fiji-macosx.dmg ./ &&
for i in *
do
	date=$(date +%Y%m%d)
	case $i in
	*-$date.*)
	;;
	*)
		mv $i $(echo $i | sed "s/\\./-$date&/")
	;;
	esac
done
