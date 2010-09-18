#!/bin/sh

LIBS="avcodec avformat swscale avcore avdevice avfilter avutil"
#LIBS="swscale"
JNAERATOR_URL=http://jnaerator.googlecode.com/files/jnaerator-0.9.4.jar
PARALLEL=-j20

case "$(uname -s)" in
Linux)
	LIBEXT=.so
	LIBPREFIX=lib
	# TODO: find out how to ask gcc for these (probably -dumpmachine)
	JNA_INCLUDES="-I/usr/include/linux/ -I/usr/lib/gcc/x86_64-linux-gnu/4.4/include/"
	JNA_INCLUDES=""
	;;
Darwin)
	LIBEXT=.dylib
	LIBPREFIX=lib
	;;
MINGW*)
	LIBEXT=.dll
	LIBPREFIX=
	;;
esac
TARGETS="$(for lib in $LIBS
	do
		echo lib$lib/$LIBPREFIX$lib$LIBEXT
	done)"

die () {
	echo "$*" >&2
	exit 1
}

# Go to working directory

cd "$(dirname "$0")" || die "Could not go to $(dirname "$0")"

# Make sure ffmpeg and libswscale are checked out at the correct revision

require_clean_working_directory () {
        git rev-parse --verify HEAD > /dev/null &&
        git update-index --ignore-submodules --refresh &&
        git diff-files --quiet --ignore-submodules &&
        git diff-index --cached --quiet HEAD --ignore-submodules -- ||
	die "Not clean: $(pwd)"
}

pseudo_submodule_update () {
	path=$1
	url=$2
	revision=$3

	if test ! -d "$path"
	then
		git clone "$url" "$path"
	fi &&
	(cd "$path" &&
	 exclude_file="$(git rev-parse --git-dir)"/info/exclude &&
	 exclude="$(cat "$exclude_file" 2> /dev/null)" &&
	 required_excludes="$(cat << \EOF)" &&
*.[oad]
*.pc
*.so
.config
.version
config.*
EOF
	 (echo "$exclude"; echo "$exclude"; echo "$required_excludes") |
		sort | uniq -u >> "$exclude_file" &&
	 require_clean_working_directory &&
	 if test "$revision" != "$(git rev-parse HEAD)"
	 then
		git checkout "$revision"
	 fi)
}

echo "Making sure that ffmpeg and libswscale are at correct revision" &&
pseudo_submodule_update ffmpeg \
	contrib@pacific.mpi-cbg.de:/srv/git/ffmpeg.git \
	90d23d8677612ee974261eacea53ca95de5c95a4 &&
pseudo_submodule_update ffmpeg/libswscale \
	contrib@pacific.mpi-cbg.de:/srv/git/libswscale.git \
	a88e950fe043e419d0f9e7f851d2d8e35aea2b83 &&

# Build FFMPEG

echo "Building FFMPEG if necessary" &&
(cd ffmpeg &&
 uptodate=true &&
 for target in $TARGETS
 do
	if test ! -f "$target"
	then
		uptodate=false
		break
	fi
 done &&
 if test true = "$uptodate" &&
	test ! -z "$(eval find -name '\\*.[ch]' -a \
		$(for target in $TARGETS
		  do
			echo "-newer $target"
		  done) )"
 then
	uptodate=false
 fi &&
 if test false = "$uptodate"
 then
	# let's make sure that everything is built from scratch
	if test -f Makefile
	then
		make distclean || :
	fi &&
	./configure --enable-gpl --enable-shared &&
	make $PARALLEL
 fi) &&

# Get JNAerator source

pseudo_submodule_update nativelibs4java \
	contrib@pacific.mpi-cbg.de:/srv/git/nativelibs4java \
	cc537ac9a9f3e43723c22e18fcae63e5d178e19c

# Build JNAerator if necessary

JNAERATOR_JAR=nativelibs4java/jnaerator/bin/jnaerator.jar
if test ! -f "$JNAERATOR_JAR"
then
	(cd nativelibs4java &&
	 "$(fiji --print-fiji-dir)/bin/maven.sh" install)
fi &&

## Get JNAerator binary
#JNAERATOR_JAR=${JNAERATOR_URL##*/} &&
#if test ! -f "$JNAERATOR_JAR"
#then
#	echo "Getting JNAerator" &&
#	curl "$JNAERATOR_URL" > "$JNAERATOR_JAR"
#fi &&

# Run JNAerator

echo "Running JNAerator" &&
rm -rf jnaerated &&
mkdir jnaerated &&
(cd ffmpeg &&
 eval fiji --jar ../"$JNAERATOR_JAR" -- -o ../jnaerated -noComp \
	-structsInLibrary -v $JNA_INCLUDES \
	$(for lib in $LIBS
	  do
		echo "-library $lib lib$lib/$lib.h lib$lib/$LIBPREFIX$lib$LIBEXT"
	  done) ) &&
for file in $(find jnaerated -name \*.java)
do
	sed 's/\(static final \)int\( .* [<>]=\? [0-9][0-9]*);\)/\1boolean\2/' \
		< $file > $file.tmp &&
	mv $file.tmp $file || break
done &&
fiji --javac --cp "$JNAERATOR_JAR" \
	-d jnaerated $(find jnaerated -name \*.java)
