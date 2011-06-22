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

RELEASE="${1#Fiji-}"
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
HOST=fiji.sc
COMMIT_MESSAGE="Precompile Fiji and Fake for $RELEASE"

clone_nightly_build () {
	(cd $HOME &&
	 test -d $NIGHTLY_BUILD ||
	 git clone git://fiji.sc/fiji.git $NIGHTLY_BUILD)
}

make_dmg () {
	if ! git push macosx10.5:$NIGHTLY_BUILD +$HEAD:$TMP_HEAD
	then
		ssh macosx10.5 "git clone git://fiji.sc/fiji.git $NIGHTLY_BUILD" &&
		git push macosx10.5:$NIGHTLY_BUILD +$HEAD:$TMP_HEAD
	fi &&
	./Build.sh app-macosx &&
	tar cvf - Fiji.app |
	ssh macosx10.5 "cd $NIGHTLY_BUILD && rm -rf Fiji.app && tar xvf - && VERSIONER_PERL_PREFER_32_BIT=yes ./bin/make-dmg.py" &&
	scp macosx10.5:$NIGHTLY_BUILD/fiji-macosx.dmg ./
}

checkout_and_build () {
	git checkout -f $TMP_HEAD^0 &&
	git reset --hard &&
	for d in $(git ls-files --stage | sed -n "s/^160.*	//p");
	do
		case "$d" in
		java/*)
			git submodule update --init $d &&
			continue ||
			break;;
		esac &&
		test -z "$(ls $d/)" || {
			echo "Submodule $d checked out" >&2
			exit 1
		}
	done &&
	./bin/nightly-build.sh --stdout &&
	echo "Work around a Heisenbug" &&
	unzip plugins/loci_tools.jar META-INF/MANIFEST.MF &&
	if head -n 2 < META-INF/MANIFEST.MF | grep Created-By:
	then
		sed -i -ne "2{h;n;G}" -e p META-INF/MANIFEST.MF &&
		zip plugins/loci_tools.jar META-INF/MANIFEST.MF
	fi &&
	rm -r META-INF &&
	for d in win-sysroot mac-sysroot
	do
		test -e bin/$d || ln -s ../../bin/$d bin/
	done &&
	./Build.sh all-cross precompile-fiji &&
	if ! git diff-files -q --exit-code --ignore-submodules
	then
		git commit -s -a -m "$COMMIT_MESSAGE"
	fi &&
	./Build.sh all-tars all-zips all-7zs
}

errorcount=0
verify_archive () {
	platformfiles="$(case $1 in
	*.tar.*) tar tvf $1;;
	*.zip) unzip -v $1;;
	*.7z) 7z l $1;;
	*) echo "Error: unknown archive type $1" >&2
	esac |
	grep -e fiji- -e rt.jar |
	grep -ve Archive -e Listing -e fiji-scripting -e = |
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
	ssh $HOST "mkdir -p $TARGET_DIRECTORY" &&
	date=$(date +%Y%m%d) &&
	for f in fiji-*
	do
		scp $f $HOST:$TARGET_DIRECTORY/${f%%.*}-$date.${f#*.} || break
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

echo Checking for tag Fiji-$RELEASE
git rev-parse --verify refs/tags/Fiji-$RELEASE 2>/dev/null && {
	echo "Tag Fiji-$RELEASE already exists!" >&2
	exit 1
}

echo "Making sure that $NIGHTLY_BUILD exists" &&
clone_nightly_build &&

echo "Pushing current revision into $NIGHTLY_BUILD" &&
git push $HOME/$NIGHTLY_BUILD +$HEAD:$TMP_HEAD &&
cd $HOME/$NIGHTLY_BUILD &&

echo "Building" &&
checkout_and_build &&

echo "Making .dmg" &&
make_dmg &&

echo "Verifying" &&
for a in linux linux64 win32 win64 macosx
do
	launcher="fiji-$a" &&
	java="java/$a" &&
	case $a in
	macosx) launcher="Contents/MacOS/fiji-macosx Contents/MacOS/fiji-tiger" &&
		java=java/macosx-java3d;;
	linux64)
		java=java/linux-amd64;;
	win*)
		launcher="$launcher.exe";;
	esac
	verify_archives $a "$launcher jars/fiji-lib.jar $java "
done &&

verify_archives nojre "Contents/MacOS/fiji-macosx Contents/MacOS/fiji-tiger fiji-linux fiji-linux64 fiji-win32.exe fiji-win64.exe jars/fiji-lib.jar " &&

verify_archives all "Contents/MacOS/fiji-macosx Contents/MacOS/fiji-tiger fiji-linux fiji-linux64 fiji-win32.exe fiji-win64.exe jars/fiji-lib.jar java/linux java/linux-amd64 java/macosx-java3d java/win32 java/win64 " ||
errorcount=$(($errorcount+1))

if test $errorcount -gt 0
then
	echo "There were errors: $errorcount"
	echo "You might want to fix them and then run $0 $1 --copy-files"
	exit 1
fi

echo " Note: Asked for the files which are not up-to-date, the Updater says:"
./fiji --update list-not-uptodate

echo "Uploading" &&
copy_files || exit

cat << EOF &&

All files have been built and uploaded to

	http://fiji.sc/downloads/$RELEASE/

Please test, and if anything is wrong, hit Ctrl-C.
If everything is okay, hit Enter to tag and upload to the Updater.
[Waiting for Enter or Ctrl-C...]
EOF
read dummy

echo "Tagging" &&
git tag -m "Fiji $RELEASE" Fiji-$RELEASE &&
git push fiji.git Fiji-$RELEASE &&
echo "Please start the updater and make sure that all files are uploaded." || exit
