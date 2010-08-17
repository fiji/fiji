# This is a configuration file for Fiji mAKE ("fake")
#
# The syntax of a Fakefile is meant to be very simple.
#
# The first rule is the default rule.
#
# All rules are of the form
#
#	target <- prerequisites
#
# before making "target", all prerequisites will be made (i.e. if there
# exists a rule, for an item on the right side, it will be executed before
# the current rule).
#
# Most rules have implicit actions: if the target is a .jar file, the items
# on the right side are packaged into the target, compiling them first, if
# they are .java files.
#
# If the last item on the right side is a .c file, the GNU C++ compiler
# will be invoked to make the target from it.
#
# If an item on the right side is a directory, and a Fakefile or a Makefile
# exists in that directory, "fake" or "make" will be called in that directory.
# The target will be simply copied from that directory after handling all
# dependencies.
#
# There is a special type of rule when "fake" does not know how to produce
# the target from the prerequisites: you can call a program with
#
#	target[program] <- items
#
# This will check if the target is up-to-date, by checking the timestamps of the
# items (if there is no item, the target is deemed _not_ up-to-date). If the target
# is not up-to-date, "fake" will execute the program with all items as parameters.
#
# Variables are defined like this:
#
#	VARIABLE=VALUE
#
# and their values can be accessed with "$VARIABLE" in most places.
#
# You can define variables depending on the platform, the target, and in some
# cases the prerequisite, by adding a tag in parentheses to the name:
#
#	VARIABLE(target)=xyz
#	VARIABLE(platform)=abc

# These variables are special, as they will be interpreted by "fake".

# Do not be verbose
verbose=false

# Usually not necessary
debug=false

# Compile .java files for this Java version
javaVersion=1.5

# If false, skips rebuilds triggered from newer Fakefile or fake.jar
# (see issues 40 & 45)
rebuildIfFakeIsNewer=true

# When building a .jar file, and a .config file of the same name is found in
# this directory, it will be included as "plugins.config".
pluginsConfigDirectory=staged-plugins

# When a prerequisite is a directory, but contains neither Fakefile nor
# Makefile, just ignore it
ignoreMissingFakefiles=true

# When a submodule could not be made, fall back to copying from this directory
precompiledDirectory=precompiled/

buildDir=build/

FIJI_JAVA_HOME(linux)=java/linux/jdk1.6.0_20/jre
FIJI_JAVA_HOME(linux64)=java/linux-amd64/jdk1.6.0_20/jre
FIJI_JAVA_HOME(win32)=java/win32/jdk1.6.0_20/jre
FIJI_JAVA_HOME(win64)=java/win64/jdk1.6.0_20/jre
FIJI_JAVA_HOME(macosx)=java/macosx-java3d
JAVA_HOME=$FIJI_JAVA_HOME
ENVOVERRIDES(JAVA_HOME)=true

# Java 3D
FIJI_JAVA_EXT=$FIJI_JAVA_HOME/lib/ext
FIJI_JAVA_EXT(macosx)=$FIJI_JAVA_HOME/Home/lib/ext
JAVA3D_JARS=$FIJI_JAVA_EXT/j3dcore.jar:$FIJI_JAVA_EXT/j3dutils.jar:$FIJI_JAVA_EXT/vecmath.jar

# tools.jar
TOOLS_JAR=$JAVA_HOME/../lib/tools.jar
TOOLS_JAR(macosx)=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar

# the main target

SUBMODULE_TARGETS=\
	jars/ij.jar \
	misc/headless.jar \
	plugins/loci_tools.jar \
	jars/VectorString.jar \
	plugins/TrakEM2_.jar \
	plugins/mpicbg_.jar \
	jars/clojure.jar \
	plugins/ij-ImageIO_.jar \
	jars/jacl.jar \
	jars/batik.jar \
	jars/junit-4.5.jar \
	jars/rsyntaxtextarea.jar \
	jars/autocomplete.jar \
	jars/weka.jar \
	jars/jython.jar \
	jars/imglib.jar \
	jars/mpicbg.jar \
	jars/commons-math.jar

PLUGIN_TARGETS=plugins/Jython_Interpreter.jar \
	plugins/Clojure_Interpreter.jar \
	plugins/JRuby_Interpreter.jar \
	plugins/BeanShell_Interpreter.jar \
	plugins/bUnwarpJ_.jar \
	plugins/register_virtual_stack_slices.jar \
	plugins/Siox_Segmentation.jar \
	plugins/registration_3d.jar \
	plugins/IO_.jar \
	plugins/CLI_.jar \
	plugins/Javascript_.jar \
	plugins/LSM_Toolbox.jar \
	plugins/SplineDeformationGenerator_.jar \
	plugins/level_sets.jar \
	plugins/Analyze_Reader_Writer.jar \
	plugins/Color_Histogram.jar \
	plugins/Color_Inspector_3D.jar \
	plugins/Image_5D.jar \
	plugins/M_I_P.jar \
	plugins/Interactive_3D_Surface_Plot.jar \
	plugins/View5D_.jar \
	plugins/Volume_Viewer.jar \
	plugins/IJ_Robot.jar \
	plugins/Fiji_Updater.jar \
	plugins/Daltonize_.jar \
	plugins/Stitching_.jar \
	plugins/LSM_Reader.jar \
	plugins/AnalyzeSkeleton_.jar \
	plugins/Skeletonize3D_.jar \
	plugins/TurboReg_.jar \
	plugins/Bug_Submitter.jar \
	plugins/Fiji_Plugins.jar \
	plugins/ToAST_.jar \
	plugins/MTrack2_.jar \
	plugins/Time_Stamper.jar \
        plugins/Series_Labeler.jar \
	plugins/Statistical_Region_Merging.jar \
	plugins/Refresh_Javas.jar \
	plugins/Auto_Threshold.jar \
	plugins/Arrow_.jar \
	plugins/Stack_Manipulation.jar \
	plugins/FlowJ_.jar \
	plugins/PIV_analyser.jar \
	plugins/Record_Screen.jar \
	plugins/Video_Editing.jar \
	plugins/Sync_Win.jar \
	plugins/Gray_Morphology.jar \
	plugins/Colocalisation_Analysis.jar \
	plugins/LocalThickness_.jar \
	plugins/Fiji_Developer.jar \
	plugins/Script_Editor.jar \
	plugins/Manual_Tracking.jar \
	plugins/Calculator_Plus.jar \
	plugins/3D_Objects_Counter.jar \
	plugins/Trainable_Segmentation.jar \
	plugins/IsoData_Classifier.jar \
	plugins/RATS_.jar \
	plugins/Directionality_.jar \
	jars/Fiji.jar \
	plugins/Image_Expression_Parser.jar \
	plugins/Algorithm_Launcher.jar \
	plugins/VIB_.jar \
	plugins/Anisotropic_Diffusion_2D.jar \
	plugins/Simple_Neurite_Tracer.jar \
	plugins/SPIM_Registration.jar \
	plugins/QuickPALM_.jar \
	plugins/3D_Viewer.jar \
	plugins/CPU_Meter.jar \
	plugins/TopoJ_.jar

all <- fiji $SUBMODULE_TARGETS $PLUGIN_TARGETS third-party-plugins

# The "run" rule just executes ./fiji (as long as the file "run" does not exist...)
# It has items on the right side, because these would be passed to the executable.

run[] <- all run-fiji
run-fiji[./fiji] <-
DEBUG_ARGS=-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n
dev[./fiji $DEBUG_ARGS] <-


# JDK

JDK=java/$PLATFORM
JDK(linux64)=java/linux-amd64
JDK(macosx)=java/macosx-java3d

# Call the Jython script to ensure that the JDK is checked out (from Git)
jdk[bin/checkout-jdk.py $JDK] <-

# From submodules
jars/ij.jar <- jars/javac.jar ImageJA/
misc/headless.jar <- jars/javac.jar ImageJA/
CLASSPATH(plugins/mpicbg_.jar)=jars/mpicbg.jar
plugins/mpicbg_.jar <- mpicbg/
jars/mpicbg.jar <- mpicbg/
CLASSPATH(jars/imglib.jar)=jars/mpicbg.jar
jars/imglib.jar <- plugins/loci_tools.jar imglib/
jars/clojure.jar <- clojure/
plugins/loci_tools.jar <- bio-formats/
CLASSPATH(jars/VectorString.jar)=jars/Jama-1.0.2.jar
jars/VectorString.jar <- TrakEM2/
CLASSPATH(plugins/TrakEM2_.jar)=plugins/VIB_.jar:jars/mpicbg.jar:plugins/loci_tools.jar:plugins/bUnwarpJ_.jar:plugins/level_sets.jar:plugins/Fiji_Plugins.jar:jars/Jama-1.0.2.jar:jars/imglib.jar:plugins/Simple_Neurite_Tracer.jar:plugins/3D_Viewer.jar
plugins/TrakEM2_.jar <- jars/ij.jar plugins/VIB_.jar jars/mpicbg.jar plugins/bUnwarpJ_.jar plugins/level_sets.jar plugins/Fiji_Plugins.jar jars/imglib.jar jars/VectorString.jar TrakEM2/
plugins/ij-ImageIO_.jar <- ij-plugins/
jars/jacl.jar <- tcljava/
jars/batik.jar <- batik/
jars/junit-4.5.jar <- junit/
jars/rsyntaxtextarea.jar <- RSyntaxTextArea/
jars/autocomplete.jar <- AutoComplete/
jars/weka.jar <- jars/Fiji.jar weka/
jars/jython.jar <- jython/
jars/commons-math.jar <- commons-math/

# From source
libs[] <- jars/test-fiji.jar jars/zs.jar jars/VIB-lib.jar jars/Jama-1.0.2.jar \
	jars/fiji-scripting.jar jars/fiji-lib.jar jars/jep.jar \
	jars/pal-optimization.jar jars/MacOSX_Updater_Fix.jar

plugins/Record_Screen.jar <- src-plugins/Record_Screen/ src-plugins/Record_Screen/**/*

mainClass(jars/Fiji.jar)=fiji.Main
src-plugins/Fiji/icon.png[cp $PRE $TARGET] <- images/icon.png

MAINCLASS(jars/javac.jar)=com.sun.tools.javac.Main

CLASSPATH(jars/fiji-scripting.jar)=jars/jython.jar:jars/Fiji.jar:jars/bsh-2.0b4.jar:jars/js.jar
CLASSPATH(plugins/Refresh_Javas.jar)=jars/fiji-scripting.jar:jars/fake.jar:jars/Fiji.jar
CLASSPATH(plugins/Jython_Interpreter.jar)=jars/fiji-scripting.jar:jars/jython.jar
CLASSPATH(plugins/Clojure_Interpreter.jar)=jars/fiji-scripting.jar:jars/clojure.jar
CLASSPATH(plugins/JRuby_Interpreter.jar)=jars/fiji-scripting.jar:jars/jruby.jar
CLASSPATH(plugins/BeanShell_Interpreter.jar)=jars/fiji-scripting.jar:jars/bsh-2.0b4.jar
CLASSPATH(plugins/Javascript_.jar)=jars/fiji-scripting.jar:jars/js.jar
CLASSPATH(plugins/CLI_.jar)=jars/fiji-scripting.jar
MAINCLASS(plugins/Script_Editor.jar)=fiji.scripting.Script_Editor
CLASSPATH(plugins/Script_Editor.jar)=jars/rsyntaxtextarea.jar:jars/autocomplete.jar:plugins/Clojure_Interpreter.jar:plugins/JRuby_Interpreter.jar:plugins/Javascript_.jar:plugins/Jython_Interpreter.jar:plugins/Refresh_Javas.jar:plugins/BeanShell_Interpreter.jar:plugins/CLI_.jar:jars/fiji-scripting.jar:jars/Fiji.jar:jars/imglib.jar:jars/fiji-lib.jar:jars/fake.jar:$TOOLS_JAR
NO_COMPILE(plugins/Script_Editor.jar)=src-plugins/Script_Editor/templates/**/*
src-plugins/Script_Editor/icon.png[cp $PRE $TARGET] <- images/icon.png
src-plugins/Script_Editor/var.png[cp $PRE $TARGET] <- images/var.png
src-plugins/Script_Editor/function.png[cp $PRE $TARGET] <- images/function.png

CLASSPATH(jars/zs.jar)=jars/Jama-1.0.2.jar
CLASSPATH(plugins/register_virtual_stack_slices.jar)=plugins/TrakEM2_.jar:jars/mpicbg.jar:plugins/bUnwarpJ_.jar:jars/fiji-lib.jar
CLASSPATH(plugins/registration_3d.jar)=jars/edu_mines_jtk.jar
CLASSPATH(plugins/Siox_Segmentation.jar)=jars/fiji-lib.jar
CLASSPATH(plugins/Image_Expression_Parser.jar)=jars/jep.jar:jars/imglib.jar:jars/junit-4.5.jar

CLASSPATH(plugins/Algorithm_Launcher.jar)=jars/imglib.jar
plugins/Algorithm_Launcher.jar <- \
	src-plugins/Algorithm_Launcher/**/*.java \
	src-plugins/Algorithm_Launcher/**/*.config

CLASSPATH(plugins/Directionality_.jar)=jars/jfreechart-1.0.13.jar:jars/jcommon-1.0.12.jar
CLASSPATH(plugins/LSM_Toolbox.jar)=plugins/LSM_Reader.jar
MAINCLASS(plugins/LSM_Toolbox.jar)=org.imagearchive.lsm.toolbox.gui.AboutDialog
MAINCLASS(plugins/Interactive_3D_Surface_Plot.jar)=Interactive_3D_Surface_Plot
CLASSPATH(plugins/Stitching_.jar)=plugins/loci_tools.jar:jars/fiji-lib.jar:jars/imglib.jar:jars/edu_mines_jtk.jar
CLASSPATH(plugins/Fiji_Plugins.jar)=jars/jsch-0.1.37.jar:jars/fiji-lib.jar
MAINCLASS(plugins/Fiji_Updater.jar)=fiji.updater.Main
CLASSPATH(plugins/Fiji_Updater.jar)=jars/jsch-0.1.37.jar
CLASSPATH(plugins/IO_.jar)=jars/batik.jar:jars/jpedalSTD.jar:jars/itext-1.3.jar:jars/jzlib-1.0.7.jar
CLASSPATH(plugins/Sync_Win.jar)=plugins/Image_5D.jar
CLASSPATH(plugins/Fiji_Developer.jar)=plugins/Script_Editor.jar:plugins/Fiji_Plugins.jar:jars/rsyntaxtextarea.jar:plugins/3D_Viewer.jar:$JAVA3D_JARS
CLASSPATH(plugins/Trainable_Segmentation.jar)=jars/weka.jar:plugins/Stitching_.jar:jars/fiji-lib.jar
CLASSPATH(plugins/VIB_.jar)=$JAVA3D_JARS:jars/VIB-lib.jar:jars/pal-optimization.jar:plugins/3D_Viewer.jar:jars/imglib.jar
CLASSPATH(jars/VIB-lib.jar)=jars/Jama-1.0.2.jar:jars/junit-4.5.jar:jars/pal-optimization.jar:jars/jzlib-1.0.7.jar
CLASSPATH(plugins/Simple_Neurite_Tracer.jar)=$JAVA3D_JARS:jars/VIB-lib.jar:plugins/VIB_.jar:jars/pal-optimization.jar:jars/junit-4.5.jar:plugins/3D_Viewer.jar:jars/commons-math.jar:jars/jfreechart-1.0.13.jar:jars/jcommon-1.0.12.jar:jars/batik.jar
CLASSPATH(plugins/3D_Viewer.jar)=jars/VIB-lib.jar:jars/imglib.jar:jars/Jama-1.0.2.jar:$JAVA3D_JARS
CLASSPATH(jars/jep.jar)=jars/Jama-1.0.2.jar:jars/junit-4.5.jar
CLASSPATH(plugins/SPIM_Registration.jar)=$JAVA3D_JARS:jars/imglib.jar:jars/mpicbg.jar:plugins/3D_Viewer.jar:jars/weka.jar:jars/fiji-lib.jar:plugins/loci_tools.jar:plugins/Fiji_Plugins.jar:jars/VIB-lib.jar:jars/Jama-1.0.2.jar
CLASSPATH(plugins/Bug_Submitter.jar)=plugins/Fiji_Updater.jar
CLASSPATH(plugins/TopoJ_.jar)=jars/Jama-1.0.2.jar

# pre-Java5 generics ;-)

src-plugins/VIB-lib/vib/FloatMatrix.java[src-plugins/VIB-lib/sed.py $PRE $TARGET] <- src-plugins/VIB-lib/vib/FastMatrix.java
src-plugins/VIB-lib/math3d/FloatMatrixN.java[src-plugins/VIB-lib/sed.py $PRE $TARGET] <- src-plugins/VIB-lib/math3d/FastMatrixN.java
src-plugins/VIB-lib/math3d/JacobiFloat.java[src-plugins/VIB-lib/sed.py $PRE $TARGET] <- src-plugins/VIB-lib/math3d/JacobiDouble.java
src-plugins/VIB-lib/math3d/Eigensystem3x3Float.java[src-plugins/VIB-lib/sed.py $PRE $TARGET] <- \
	src-plugins/VIB-lib/math3d/Eigensystem3x3Double.java
src-plugins/VIB-lib/math3d/Eigensystem2x2Float.java[src-plugins/VIB-lib/sed.py $PRE $TARGET] <- \
	src-plugins/VIB-lib/math3d/Eigensystem2x2Double.java

MAINCLASS(jars/test-fiji.jar)=fiji.Tests
CLASSPATH(jars/test-fiji.jar)=jars/junit-4.5.jar

MAINCLASS(jars/MacOSX_Updater_Fix.jar)=fiji.updater.Fix

# the default rules

plugins/*.jar <- src-plugins/*/**/*
jars/*.jar <- src-plugins/*/**/*

# Third party plugins

THIRD_PARTY_PLUGINS= \
	plugins/TransformJ_.jar \

third-party-plugins[] <- $THIRD_PARTY_PLUGINS
plugins/*.jar <- staged-plugins/*.jar

# Fiji launcher

JAVA_LIB_PATH(linux)=lib/i386/client/libjvm.so
JAVA_LIB_PATH(linux64)=lib/amd64/server/libjvm.so
JAVA_LIB_PATH(win32)=bin/client/jvm.dll
JAVA_LIB_PATH(win64)=bin/server/jvm.dll
JAVA_LIB_PATH(macosx)=

# The variables CFLAGS, CXXFLAGS, LDFLAGS and LIBS will be used for compiling
# C and C++ programs.
CFLAGS(*)=-Wall -Iincludes \
	-DJAVA_HOME='"$FIJI_JAVA_HOME"' -DJAVA_LIB_PATH='"$JAVA_LIB_PATH"'
WINOPTS=-mwindows -mno-cygwin -DMINGW32
CFLAGS(win32)=$CFLAGS $WINOPTS
CFLAGS(win64)=$CFLAGS $WINOPTS

# Include 64-bit architectures only in ./fiji (as opposed to ./fiji-tiger),
# and only on MacOSX
MACOPTS(osx10.3)=-I/System/Library/Frameworks/JavaVM.Framework/Headers \
	-DMACOSX
MACOPTS(osx10.4)=$MACOPTS(osx10.3) -mmacosx-version-min=10.3 -arch i386 -arch ppc
MACOPTS(osx10.5)=$MACOPTS(osx10.4) -arch x86_64

CFLAGS(linux)=$CFLAGS -DIPV6_MAYBE_BROKEN
CFLAGS(linux64)=$CFLAGS -DIPV6_MAYBE_BROKEN

LDFLAGS(win32)=$LDFLAGS $WINOPTS

CFLAGS(fiji)=$CFLAGS $MACOPTS
LDFLAGS(fiji)=$LDFLAGS $MACOPTS

LIBS(linux)=-ldl
LIBS(linux64)=-ldl
LIBS(macosx)=-framework CoreFoundation -framework JavaVM

fiji <- fiji.c

CFLAGS(fiji-macosx)=$CFLAGS $MACOPTS(osx10.5)
LDFLAGS(fiji-macosx)=$LDFLAGS $MACOPTS(osx10.5)
fiji-macosx <- fiji.c

CFLAGS(fiji-tiger)=$CFLAGS $MACOPTS(osx10.4)
LDFLAGS(fiji-tiger)=$LDFLAGS $MACOPTS(osx10.4)
fiji-tiger <- fiji.c

CFLAGS(fiji-panther)=$CFLAGS $MACOPTS(osx10.3)
LDFLAGS(fiji-panther)=$LDFLAGS $MACOPTS(osx10.3)
fiji-panther <- fiji.c

# Cross-compiling (works only on Linux64 so far)

all-cross[] <- cross-win32 cross-win64 cross-linux
# cross-tiger does not work yet

cross-win64[bin/cross-compiler.py win64 $CFLAGS(win64)] <- fiji.c
cross-tiger[bin/chrooted-cross-compiler.sh tiger \
	$CFLAGS(macosx) $LIBS(macosx)] <- fiji.c
cross-*[bin/chrooted-cross-compiler.sh * \
	$CFLAGS(*) $LIBS(*)] <- fiji.c

# Precompiled stuff

LAUNCHER(*)=precompiled/fiji-$PLATFORM
LAUNCHER(win32)=precompiled/fiji-win32.exe
LAUNCHER(win64)=precompiled/fiji-win64.exe
LAUNCHER(osx10.4)=precompiled/fiji-macosx
LAUNCHER(osx10.5)=precompiled/fiji-macosx precompiled/fiji-tiger
precompile-fiji[] <- $LAUNCHER

precompiled/fiji-tiger[bin/copy-file.py $PRE $TARGET] <- fiji-tiger
precompiled/fiji-macosx[bin/copy-file.py $PRE $TARGET] <- fiji-macosx
# this rule only matches precompiled/fiji-$PLATFORM
precompiled/fiji-*[bin/copy-file.py $PRE $TARGET] <- fiji

precompile-fake[] <- precompiled/fake.jar
precompiled/fake.jar <- jars/fake.jar
precompiled/javac.jar <- jars/javac.jar
precompiled/ij.jar <- jars/ij.jar
precompiled/mpicbg.jar <- jars/mpicbg.jar
precompiled/*[bin/copy-file.py $PRE $TARGET] <- *

precompile-submodules[] <- \
	precompiled/ij.jar \
	precompiled/loci_tools.jar \
	precompiled/TrakEM2_.jar \
	precompiled/mpicbg_.jar \
	precompiled/mpicbg.jar \
	precompiled/clojure.jar \
	precompiled/ij-ImageIO_.jar \
	precompiled/jacl.jar \
	precompiled/batik.jar \
	precompiled/junit-4.5.jar \
	precompiled/rsyntaxtextarea.jar \
	precompiled/autocomplete.jar \
	precompiled/weka.jar \
	precompiled/jython.jar \
	precompiled/imglib.jar \
	precompiled/commons-math.jar \

precompiled/ij.jar <- jars/ij.jar
precompiled/clojure.jar <- jars/clojure.jar
precompiled/jacl.jar <- jars/jacl.jar
precompiled/batik.jar <- jars/batik.jar
precompiled/junit-4.5.jar <- jars/junit-4.5.jar
precompiled/rsyntaxtextarea.jar <- jars/rsyntaxtextarea.jar
precompiled/autocomplete.jar <- jars/autocomplete.jar
precompiled/weka.jar <- jars/weka.jar
precompiled/jython.jar <- jars/jython.jar
precompiled/imglib.jar <- jars/imglib.jar
precompiled/commons-math.jar <- jars/commons-math.jar
precompiled/* <- plugins/*

precompile[] <- precompile-fiji precompile-fake precompile-submodules

# precompiled fall back

missingPrecompiledFallBack[./fiji --jar plugins/Fiji_Updater.jar --update $TARGET] <- \
	jars/Fiji.jar plugins/Fiji_Updater.jar

# Portable application/.app

all-apps[] <- app-macosx app-linux app-linux64 app-win32 app-win64
MACOSX_TIGER_LAUNCHER(macosx)=fiji-tiger
app-*[bin/make-app.py * $PLATFORM] <- all $MACOSX_TIGER_LAUNCHER

app-all[bin/make-app.py all $PLATFORM] <- all
app-nojre[bin/make-app.py nojre $PLATFORM] <- all

all-dmgs[] <- fiji-macosx.dmg
fiji-*.dmg[bin/make-dmg.py] <- app-* Fiji.app \
	resources/install-fiji.jpg
dmg[] <- fiji-macosx.dmg

resources/install-fiji.jpg[./fiji bin/generate-finder-background.py] <- \
	bin/generate-finder-background.py

all-tars[] <- fiji-linux.tar.bz2 fiji-linux64.tar.bz2 \
	fiji-all.tar.bz2 fiji-nojre.tar.bz2
fiji-*.tar.bz2[bin/make-tar.py $TARGET Fiji.app] <- app-* Fiji.app
tar[] <- fiji-$PLATFORM.tar.bz2

all-zips[] <- fiji-linux.zip fiji-linux64.zip fiji-win32.zip fiji-win64.zip \
	fiji-all.zip fiji-nojre.zip
fiji-*.zip[bin/make-zip.py $TARGET Fiji.app] <- app-* Fiji.app
zip[] <- fiji-$PLATFORM.zip

all-isos[] <- fiji-linux.iso fiji-linux64.iso fiji-win32.iso fiji-win64.iso \
	fiji-macosx.iso fiji-all.iso fiji-nojre.iso
fiji-*.iso[genisoimage -J -V Fiji -o $TARGET Fiji.app] <- app-*

all-7zs[] <- fiji-linux.7z fiji-linux64.7z fiji-win32.7z fiji-win64.7z \
	fiji-macosx.7z fiji-all.7z fiji-nojre.7z
fiji-*.7z[bin/make-7z.py $TARGET Fiji.app] <- app-*

# Checks

check[] <- check-launchers check-submodules

LAUNCHERS=$LAUNCHER(linux) $LAUNCHER(linux64) \
	$LAUNCHER(win32) $LAUNCHER(win64) $LAUNCHER(macosx)
check-launchers[bin/up-to-date-check.py fiji.c $LAUNCHERS] <-

check-submodules[] <- check-ij check-VIB check-TrakEM2 check-mpicbg

check-ij[bin/up-to-date-check.py ImageJA precompiled/ij.jar] <-
check-*[bin/up-to-date-check.py * precompiled/*_.jar] <-

# Fake itself

MAINCLASS(jars/fake.jar)=fiji.build.Fake
JAVAVERSION(jars/fake.jar)=1.3
jars/fake.jar <- src-plugins/fake/**/*.java
