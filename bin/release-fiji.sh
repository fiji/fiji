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

test a"$1" = a--copy-files && set "$2" "$1"

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
COMMIT_MESSAGE="Precompile Fiji and Fake for $RELEASE"

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
	 export VERSIONER_PERL_PREFER_32_BIT=yes &&
	 ./bin/nightly-build.sh --stdout &&
	 echo "Work around a Heisenbug" &&
	 unzip plugins/loci_tools.jar META-INF/MANIFEST.MF &&
	 if head -n 2 < META-INF/MANIFEST.MF | grep Created-By:
	 then
		sed -i -ne "2{h;n;G}" -e p META-INF/MANIFEST.MF &&
		zip plugins/loci_tools.jar META-INF/MANIFEST.MF
	 fi &&
	 rm -r META-INF &&
	 case "$(uname -s)" in
	 Darwin)
		./Build.sh precompile-fiji precompile-fake dmg &&
		if ! git diff-files -q --exit-code --ignore-submodules
		then
			git commit -s -a -m "'"$COMMIT_MESSAGE"'"
		fi
		;;
	 esac) >&2 &&
	./bin/calculate-checksums.py
'

build_rest='
	(cd $HOME/'$NIGHTLY_BUILD' &&
	 ./Build.sh all-cross precompile-fiji &&
	 if ! git diff-files -q --exit-code --ignore-submodules
	 then
		git commit -s -a -m "'"$COMMIT_MESSAGE"'"
	 fi &&
	 ./Build.sh all-tars all-zips all-7zs) >&2
'
errorcount=0
verify_archive () {
	platformfiles="$(case $1 in
	*.tar.*) tar tvf $1;;
	*.zip) unzip -v $1;;
	*.7z) 7z l $1;;
	*) echo "Error: unknown archive type $1" >&2
	esac |
	grep -e fiji- -e rt.jar |
	grep -ve Archive -e Listing -e fiji-scripting |
	sed -e 's/^.*Fiji.app\///' -e 's/\(java\/[^\/]*\).*/\1/' |
	sort | uniq | tr '\012' ' ')"
	if test a"$2" != a"$platformfiles"
	then
		echo "Archive $1 has"
		echo "  $platformfiles"
		echo "but was supposed to have"
		echo "  $2"
		errorcount=$(($errorcount+1))
	fi
}

verify_archives () {
	for a in fiji-$1.tar.bz2 fiji-$1.zip fiji-$1.7z \
		fiji-$1-*.tar.bz2 fiji-$1-*.zip fiji-$1-*.7z
	do
		test ! -f $a || verify_archive $a "$2"
	done
}

copy_files () {
	mkdir -p $TARGET_DIRECTORY &&
	date=$(date +%Y%m%d) &&
	for f in fiji-*
	do
		mv $f $TARGET_DIRECTORY/${f%%.*}-$date.${f#*.} || break
	done
}

case "$2" in
--copy-files)
	cd $HOME/$NIGHTLY_BUILD &&
	copy_files
	exit
	;;
esac

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

echo "Building for non-MacOSX" &&
git fetch macosx10.5:$NIGHTLY_BUILD master && # for fiji-macosx.7z
if ! git push $HOME/$NIGHTLY_BUILD +FETCH_HEAD:$TMP_HEAD
then
	(eval $clone) &&
	git push $HOME/$NIGHTLY_BUILD +FETCH_HEAD:$TMP_HEAD
fi &&
thissums="$(sh -c "$checkout_and_build")" &&

if test "$macsums" != "$thissums"
then
	echo "The build results are different!"
	echo "$macsums" > .git/macsums
	echo "$thissums" > .git/thissums
	git diff --no-index .git/macsums .git/thissums
	exit 1
fi &&
sh -c "$build_rest" || exit

echo "Verifying"
cd $HOME/$NIGHTLY_BUILD
for a in linux linux64 win32 win64 macosx
do
	launcher="fiji-$a"
	java="java/$a"
	case $a in
	macosx) launcher="Contents/MacOS/fiji-macosx Contents/MacOS/fiji-tiger";
		java=java/macosx-java3d;;
	linux64)
		java=java/linux-amd64;;
	win*)
		launcher="$launcher.exe";;
	esac
	verify_archives $a "$launcher jars/fiji-lib.jar $java "
done

verify_archives nojre "Contents/MacOS/fiji-macosx Contents/MacOS/fiji-tiger fiji-linux fiji-linux64 fiji-win32.exe fiji-win64.exe jars/fiji-lib.jar "

verify_archives all "Contents/MacOS/fiji-macosx Contents/MacOS/fiji-tiger fiji-linux fiji-linux64 fiji-win32.exe fiji-win64.exe jars/fiji-lib.jar java/linux java/linux-amd64 java/macosx-java3d java/win32 java/win64 "

if test $errorcount -gt 0
then
	echo "There were errors: $errorcount"
	echo "You might want to fix them and then run $0 $1 --copy-files"
	exit 1
fi

echo "Uploading" &&
(cd $HOME/$NIGHTLY_BUILD &&
 scp macosx10.5:$NIGHTLY_BUILD/fiji-macosx.dmg ./ &&
 copy_files) || exit

cat << EOF

All files have been built and uploaded to

	http://pacific.mpi-cbg.de/downloads/$RELEASE/

Please test, and if anything is wrong, hit Ctrl-C.
If everything is okay, hit Enter to tag and upload to the Updater.
[Waiting for Enter or Ctrl-C...]
EOF
read dummy

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
git push /srv/git/fiji.git Fiji-$RELEASE &&
(cd $HOME/$NIGHTLY_BUILD &&
 ./bin/update-fiji.py) || exit

