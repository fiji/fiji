#!/bin/sh

LIBS="avcodec avformat swscale avcore avdevice avfilter avutil"
PARALLEL=-j5

EXTRA_CONFIGURE=
EXTRA_LDFLAGS=
case "$(uname -s)" in
Linux)
	LIBEXT=.so
	LIBPREFIX=lib
	EXTRA_LDFLAGS="-Wl,-soname,libffmpeg.so -Wl,--warn-common -Wl,--as-needed -Wl,-Bsymbolic"
	;;
Darwin)
	LIBEXT=.dylib
	LIBPREFIX=lib
	EXTRA_CONFIGURE="--disable-yasm --arch=x86_64 --target-os=darwin --enable-cross-compile"
	EXTRA_LDFLAGS="-dynamiclib -Wl,-single_module -Wl,-install_name,/usr/local/lib/libffmpeg.dylib,,-compatibility_version,1 -Wl,-read_only_relocs,suppress"
	# TODO: make fat binary
	export CFLAGS="-arch x86_64 -m64"
	export LDFLAGS="-arch x86_64 -m64"
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

default_excludes="*.[oad] *.pc *$LIBEXT *$LIBEXT.[0-9] *$LIBEXT.[0-9][0-9] .config .version config.* *.ver /*_g /ffmpeg /ffplay /ffserver /ffprobe /version.h /libswscale/ /libavutil/avconfig.h"

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
	 required_excludes="$(echo "$default_excludes" | tr ' ' '\n')" &&
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

echo "Checking whether FFMPEG needs to be built" &&
(cd ffmpeg &&
 uptodate=true &&
 TARGET=${LIBPREFIX}ffmpeg$LIBEXT &&
 if test ! -f $TARGET
 then
	uptodate=false
	break
 fi &&
 if test true = "$uptodate" &&
	test ! -z "$(eval find -name '\\*.[ch]' -a -newer $TARGET)"
 then
	uptodate=false
 fi &&
 if test false = "$uptodate"
 then
	echo "Building FFMPEG" &&
	# make sure that everything is built from scratch
	if test -f config.mak
	then
		make distclean || :
	fi &&
	./configure --enable-gpl --enable-shared $EXTRA_CONFIGURE &&
	: SYMVER breaks our one-single-library approach
	sed 's/\( HAVE_SYMVER.*\) 1$/\1 0/' < config.h > config.h.new &&
	mv -f config.h.new config.h &&
	make $PARALLEL &&
	rm */*$LIBEXT* &&
	out="$(make V=1 | grep -ve '-o libavfilter' |
		sed -n 's/^gcc .* -o lib[^ ]* //p' | tr ' ' '\n')" &&
	gcc -shared $EXTRA_LDFLAGS -o $TARGET \
		$(echo "$out" | grep -ve '^-' -e 'libavcodec/inverse\.o') \
		$(echo "$out" | grep '^-' | grep -ve '^-lav' -e '^-lsw' |
			sort | uniq)
 fi)
