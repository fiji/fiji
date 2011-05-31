#!/bin/sh

cd "$(dirname "$0")"

TARGET=x86_64-w64-mingw32
TARGET32=i686-w64-mingw32
SYSROOT="$(pwd)/win-sysroot"
test -d "$SYSROOT" || mkdir -p "$SYSROOT"
PARALLEL=-j5

makeinfo --help > /dev/null 2>&1
if test $? != 0
then
	echo "You need to install makeinfo (texinfo package)." >&2
	exit 1
fi

if test ! -f "$SYSROOT"/bin/$TARGET-objdump
then
	(if test ! -d binutils
	then
		git clone git://repo.or.cz/binutils.git
	fi &&
	mkdir -p build-binutils &&
	cd build-binutils &&
	../binutils/configure --target=$TARGET \
		--enable-targets=$TARGET,$TARGET32 \
		--with-sysroot="$SYSROOT" --prefix="$SYSROOT" &&
	make $PARALLEL &&
	make $PARALLEL install)
fi &&
if test ! -f "$SYSROOT"/$TARGET/include/winnt.h
then
	if test ! -d mingw-w64
	then
		git clone git://fiji.sc/mingw-w64
	fi &&
	(mkdir -p build-headers &&
	 cd build-headers &&
	 ../mingw-w64/mingw-w64-headers/configure --build=x86_64-unknown-linux \
                --host=$TARGET --prefix="$SYSROOT" &&
	 make $PARALLEL install)
fi &&
if test ! -h "$SYSROOT"/mingw
then
	ln -s $TARGET "$SYSROOT"/mingw
fi &&
if test ! -h "$SYSROOT"/$TARGET/lib64
then
	ln -s lib "$SYSROOT"/$TARGET/lib64
fi &&
if test ! -f "$SYSROOT"/bin/$TARGET-gcc
then
	(if test ! -d gcc
	 then
		git clone git://repo.or.cz/gcc.git
	 fi &&
	 for d in gmp-4.3.2 mpfr-2.4.2 mpc-0.8.1
	 do
		 if test ! -d $d
		 then
			case $d in
			mpc*)
				f=$d.tar.gz &&
				taropt=xzvf
				;;
			*)
				f=$d.tar.bz2 &&
				taropt=xjvf
				;;
			esac &&
			if test ! -f $f
			then
				curl -O ftp://gcc.gnu.org/pub/gcc/infrastructure/$f
			fi &&
			tar $taropt $f &&
			(cd $d &&
			 CFLAGS="-I$SYSROOT/include" \
			 LDFLAGS="-L$SYSROOT/lib" \
			./configure --prefix="$SYSROOT" \
				--enable-static --disable-shared &&
			 make $PARALLEL install) || break
		fi || break
	 done &&
	 mkdir -p build-gcc &&
	 cd build-gcc &&
	 ../gcc/configure --target=$TARGET --enable-targets=all \
		--with-gmp="$SYSROOT" --with-mpfr="$SYSROOT" \
		--with-mpc="$SYSROOT" \
		--with-sysroot="$SYSROOT" --prefix="$SYSROOT" &&
	 make $PARALLEL all-gcc &&
	 make $PARALLEL install-gcc)
fi &&
export LD_LIBRARY_PATH="$SYSROOT/lib" &&
export PATH="$SYSROOT/bin:$PATH" &&
PARALLEL= &&
INTRIN_H="$SYSROOT/x86_64-w64-mingw32/include/intrin.h" &&
if grep '__MACHINEX86X_NOX64.__m64 _m_pinsrw.__m64,int,int..' "$INTRIN_H"
then
	sed -e '/__MACHINEX86X_NOX64.__m64 _m_pinsrw.__m64,int,int../d' \
		-e '/__MACHINEX86X_NOX64.__m64 _m_pshufw.__m64,int../d' \
		< "$INTRIN_H" > "$INTRIN_H.new" &&
	mv "$INTRIN_H.new" "$INTRIN_H"
fi &&
if test ! -f "$SYSROOT"/$TARGET/lib/crt2.o
then
	(mkdir -p build-crt &&
	 cd build-crt &&
	 CFLAGS="-I$SYSROOT/$TARGET/include" \
	 LDFLAGS="-L$SYSROOT/$TARGET/lib" \
	 ../mingw-w64/mingw-w64-crt/configure --host=$TARGET \
		--enable-lib32 \
		--with-sysroot="$SYSROOT" --prefix="$SYSROOT" &&
	 make $PARALLEL &&
	 make $PARALLEL install)
fi &&
if test ! -f "$SYSROOT"/lib/gcc/$TARGET/*/libgcc_eh.a
then
	(cd build-gcc &&
	 make $PARALLEL &&
	 make $PARALLEL install)
fi
