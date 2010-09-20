#!/bin/sh

LIBS="avcodec avformat swscale avcore avdevice avfilter avutil"
PARALLEL=-j5

case "$(uname -s)" in
Linux)
	LIBEXT=.so
	LIBPREFIX=lib
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
*.so.[0-9]
*.so.[0-9][0-9]
.config
.version
config.*
*.ver
/*_g
/ffmpeg
/ffplay
/ffserver
/ffprobe
/version.h
/libswscale/
/libavutil/avconfig.h
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

echo "Checking whether FFMPEG needs to be built" &&
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
	echo "Building FFMPEG" &&
	# make sure that everything is built from scratch
	if test -f config.mak
	then
		make distclean || :
	fi &&
	./configure --enable-gpl --enable-shared \
		--extra-ldflags=-Wl,-R,"'\\\$\\\$\\\$\\\$ORIGIN/'" &&
	: SYMVER breaks our one-single-library approach
	sed 's/\( SYMVER.*\) 1$/\1 0/' < config.h > config.h.new &&
	mv -f config.h.new config.h &&
	make $PARALLEL &&
	rm */*.so* &&
	out="$(make V=1 | grep -ve '-o libavfilter' |
		sed -n 's/^gcc .* -o lib[^ ]* //p' | tr ' ' '\n')" &&
	gcc -shared -Wl,-soname,libffmpeg.so.0 -Wl,--warn-common \
		-Wl,--as-needed -Wl,-Bsymbolic -o libffmpeg.so \
		$(echo "$out" | grep -ve '^-' -e 'libavcodec/inverse\.o') \
		$(echo "$out" | grep '^-' | grep -ve '^-lav' -e '^-lsw' |
			sort | uniq)
 fi)

# Discover Fiji

# TODO: call make_ffmpeg_jna_classes.bsh

# TODO: compile the plugin & bundle all (Fakefile)

# package everything (the libraries into platform-specific directories)

# jna.library.path should suffice, now that rpath is set correctly

# make a VirtualStack
