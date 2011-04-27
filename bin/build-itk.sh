#!/bin/sh

# change to the Fiji root directory
cd "$(dirname "$0")"/.. &&
FIJI_ROOT="$(pwd -P)" &&

mkdir -p 3rdparty &&
cd 3rdparty &&

echo CMake &&
if test ! -d cmake
then
	git clone contrib@pacific.mpi-cbg.de:/srv/git/cmake
fi &&
if test ! -x cmake/bin/cmake
then
	(cd cmake &&
	 ./configure &&
	 make $PARALLEL)
fi &&

echo ITK &&
if test ! -d Insight
then
	git clone contrib@pacific.mpi-cbg.de:/srv/git/Insight
fi &&
CMAKE_COMMAND="../cmake/bin/cmake -D BUILD_TESTING=OFF -D BUILD_EXAMPLES=OFF" &&
if test ! -d ITK-build
then
	mkdir ITK-build &&
	(cd ITK-build &&
	 eval $CMAKE_COMMAND ../Insight &&
	 make $PARALLEL)
fi &&
for target in macosx32 macosx64
do
	case $target in
	macosx32)
		SYSTEMNAME=Darwin &&
		SO=dylib &&
		SYSROOT="$FIJI_ROOT/bin/mac-sysroot" &&
		SYSPREFIX="$SYSROOT/bin/i686-apple-darwin8"
		;;
	macosx64)
		SYSTEMNAME=Darwin &&
		SO=dylib &&
		SYSROOT="$FIJI_ROOT/bin/mac-sysroot" &&
		SYSPREFIX="$SYSROOT/bin/x86_64-apple-darwin8"
		;;
	esac &&
	TRYRUN=TryRunResults-$target.cmake &&
	if test ! -e ITK-build-$target/bin/libITKAlgorithms.$SO
	then
		if test ! -f $TRYRUN
		then
			cat > $TRYRUN << EOF
# DUMMY
#    indicates whether the executable would have been able to run on its
#    target platform. If so, set DUMMY to
#    the exit code (in many cases 0 for success), otherwise enter "FAILED_TO_RUN".
SET(DUMMY "0" CACHE STRING "Result from TRY_RUN" FORCE)
SET(DUMMY__TRYRUN_OUTPUT "hello, world." CACHE STRING "Output from TRY_RUN" FORCE)

# QNANHIBIT_VALUE
#    indicates whether the executable would have been able to run on its
#    target platform. If so, set QNANHIBIT_VALUE to
#    the exit code (in many cases 0 for success), otherwise enter "FAILED_TO_RUN".
SET(QNANHIBIT_VALUE "FAILED_TO_RUN" CACHE STRING "Result from TRY_RUN" FORCE)
SET(QNANHIBIT_VALUE__TRYRUN_OUTPUT "" CACHE STRING "Output from TRY_RUN" FORCE)

# VCL_HAS_SLICED_DESTRUCTOR_BUG
#    indicates whether the executable would have been able to run on its
#    target platform. If so, set VCL_HAS_SLICED_DESTRUCTOR_BUG to
#    the exit code (in many cases 0 for success), otherwise enter "FAILED_TO_RUN".
SET(VCL_HAS_SLICED_DESTRUCTOR_BUG "0" CACHE STRING "Result from TRY_RUN" FORCE)
SET(VCL_HAS_SLICED_DESTRUCTOR_BUG__TRYRUN_OUTPUT "" CACHE STRING "Output from TRY_RUN" FORCE)

# VCL_HAS_WORKING_STRINGSTREAM
#    indicates whether the executable would have been able to run on its
#    target platform. If so, set VCL_HAS_WORKING_STRINGSTREAM to
#    the exit code (in many cases 0 for success), otherwise enter "FAILED_TO_RUN".
SET(VCL_HAS_WORKING_STRINGSTREAM "0" CACHE STRING "Result from TRY_RUN" FORCE)
SET(VCL_HAS_WORKING_STRINGSTREAM__TRYRUN_OUTPUT "" CACHE STRING "Output from TRY_RUN" FORCE)

# VCL_HAS_LFS
#    indicates whether the executable would have been able to run on its
#    target platform. If so, set VCL_HAS_LFS to
#    the exit code (in many cases 0 for success), otherwise enter "FAILED_TO_RUN".
SET(VCL_HAS_LFS "0" CACHE STRING "Result from TRY_RUN" FORCE)
SET(VCL_HAS_LFS__TRYRUN_OUTPUT "" CACHE STRING "Output from TRY_RUN" FORCE)

# VXL_HAS_SSE2_HARDWARE_SUPPORT
#    indicates whether the executable would have been able to run on its
#    target platform. If so, set VXL_HAS_SSE2_HARDWARE_SUPPORT to
#    the exit code (in many cases 0 for success), otherwise enter "FAILED_TO_RUN".
SET(VXL_HAS_SSE2_HARDWARE_SUPPORT "1" CACHE STRING "Result from TRY_RUN" FORCE)
SET(VXL_HAS_SSE2_HARDWARE_SUPPORT__TRYRUN_OUTPUT "" CACHE STRING "Output from TRY_RUN" FORCE)

# VXL_SSE2_HARDWARE_SUPPORT_POSSIBLE
#    indicates whether the executable would have been able to run on its
#    target platform. If so, set VXL_SSE2_HARDWARE_SUPPORT_POSSIBLE to
#    the exit code (in many cases 0 for success), otherwise enter "FAILED_TO_RUN".
SET(VXL_SSE2_HARDWARE_SUPPORT_POSSIBLE "1" CACHE STRING "Result from TRY_RUN" FORCE)
SET(VXL_SSE2_HARDWARE_SUPPORT_POSSIBLE__TRYRUN_OUTPUT "" CACHE STRING "Output from TRY_RUN" FORCE)

# VCL_COMPLEX_POW_WORKS
#    indicates whether the executable would have been able to run on its
#    target platform. If so, set VCL_COMPLEX_POW_WORKS to
#    the exit code (in many cases 0 for success), otherwise enter "FAILED_TO_RUN".
SET(VCL_COMPLEX_POW_WORKS "0" CACHE STRING "Result from TRY_RUN" FORCE)
SET(VCL_COMPLEX_POW_WORKS__TRYRUN_OUTPUT "" CACHE STRING "Output from TRY_RUN" FORCE)

# VCL_CHAR_IS_SIGNED
#    indicates whether the executable would have been able to run on its
#    target platform. If so, set VCL_CHAR_IS_SIGNED to
#    the exit code (in many cases 0 for success), otherwise enter "FAILED_TO_RUN".
SET(VCL_CHAR_IS_SIGNED "FAILED_TO_RUN" CACHE STRING "Result from TRY_RUN" FORCE)
SET(VCL_CHAR_IS_SIGNED__TRYRUN_OUTPUT "" CACHE STRING "Output from TRY_RUN" FORCE)

# RUN_RESULT
#    indicates whether the executable would have been able to run on its
#    target platform. If so, set RUN_RESULT to
#    the exit code (in many cases 0 for success), otherwise enter "FAILED_TO_RUN".
SET(RUN_RESULT "FAILED_TO_RUN" CACHE STRING "Result from TRY_RUN" FORCE)
SET(RUN_RESULT__TRYRUN_OUTPUT "" CACHE STRING "Output from TRY_RUN" FORCE)

# VCL_NUMERIC_LIMITS_HAS_INFINITY
#    indicates whether the executable would have been able to run on its
#    target platform. If so, set VCL_NUMERIC_LIMITS_HAS_INFINITY to
#    the exit code (in many cases 0 for success), otherwise enter "FAILED_TO_RUN".
SET(VCL_NUMERIC_LIMITS_HAS_INFINITY "0" CACHE STRING "Result from TRY_RUN" FORCE)
SET(VCL_NUMERIC_LIMITS_HAS_INFINITY__TRYRUN_OUTPUT "" CACHE STRING "Output from TRY_RUN" FORCE)

# VCL_PROCESSOR_HAS_INFINITY
#    indicates whether the executable would have been able to run on its
#    target platform. If so, set VCL_PROCESSOR_HAS_INFINITY to
#    the exit code (in many cases 0 for success), otherwise enter "FAILED_TO_RUN".
SET(VCL_PROCESSOR_HAS_INFINITY "0" CACHE STRING "Result from TRY_RUN" FORCE)
SET(VCL_PROCESSOR_HAS_INFINITY__TRYRUN_OUTPUT "" CACHE STRING "Output from TRY_RUN" FORCE)

# KWSYS_LFS_WORKS
#    indicates whether the executable would have been able to run on its
#    target platform. If so, set KWSYS_LFS_WORKS to
#    the exit code (in many cases 0 for success), otherwise enter "FAILED_TO_RUN".
SET(KWSYS_LFS_WORKS "0" CACHE STRING "Result from TRY_RUN" FORCE)
SET(KWSYS_LFS_WORKS__TRYRUN_OUTPUT "" CACHE STRING "Output from TRY_RUN" FORCE)

# KWSYS_CHAR_IS_SIGNED
#    indicates whether the executable would have been able to run on its
#    target platform. If so, set KWSYS_CHAR_IS_SIGNED to
#    the exit code (in many cases 0 for success), otherwise enter "FAILED_TO_RUN".
SET(KWSYS_CHAR_IS_SIGNED "1" CACHE STRING "Result from TRY_RUN" FORCE)
SET(KWSYS_CHAR_IS_SIGNED__TRYRUN_OUTPUT "" CACHE STRING "Output from TRY_RUN" FORCE)

SET(COREFOUNDATION_LIBRARY "$SYSROOT/System/Library/Frameworks/CoreFoundation.framework" CACHE STRING "" FORCE)
SET(VXL_HAS_INT_8 "1" CACHE STRING "" FORCE)
SET(VXL_INT_8 "char" CACHE STRING "" FORCE)
SET(VXL_HAS_INT_16 "1" CACHE STRING "" FORCE)
SET(VXL_INT_16 "short" CACHE STRING "" FORCE)
SET(VXL_HAS_INT_32 "1" CACHE STRING "" FORCE)
SET(VXL_INT_32 "int" CACHE STRING "" FORCE)
SET(VXL_HAS_INT_64 "1" CACHE STRING "" FORCE)
SET(VXL_INT_64 "long long" CACHE STRING "" FORCE)
SET(VXL_HAS_BYTE "1" CACHE STRING "" FORCE)
SET(VXL_BYTE "char" CACHE STRING "" FORCE)
EOF
		fi &&
		mkdir -p ITK-build-$target/bin &&
		(cd ITK-build-$target &&
		 eval $CMAKE_COMMAND -D CMAKE_SYSTEM_NAME=$SYSTEMNAME \
			-D CMAKE_C_COMPILER="$SYSPREFIX-gcc" \
			-D CMAKE_CXX_COMPILER="$SYSPREFIX-g++" \
			-C ../$TRYRUN ../Insight/ &&
		 make $PARALLEL || {
			: most likely, itkmkg3states was built for the _target_ &&
			cp ../ITK-build/bin/itkmkg3states bin/ &&
			make $PARALLEL
		} || {
			: make sure that vxl_int_32 is set properly &&
			eval $CMAKE_COMMAND -D CMAKE_SYSTEM_NAME=$SYSTEMNAME \
				-D CMAKE_C_COMPILER="$SYSPREFIX-gcc" \
				-D CMAKE_CXX_COMPILER="$SYSPREFIX-g++" \
				-C ../$TRYRUN ../Insight/ &&
			make $PARALLEL
		})
	fi || exit 1
done
