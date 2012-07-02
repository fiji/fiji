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

# Whether to show use of deprecated entities
showDeprecation=false

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

FIJI_JAVA_HOME(linux32)=java/linux/jdk1.6.0_24/jre
FIJI_JAVA_HOME(linux64)=java/linux-amd64/jdk1.6.0_24/jre
FIJI_JAVA_HOME(win32)=java/win32/jdk1.6.0_24/jre
FIJI_JAVA_HOME(win64)=java/win64/jdk1.6.0_24/jre
FIJI_JAVA_HOME(macosx)=java/macosx-java3d
FIJI_JAVA_HOME(freebsd)=/usr/local/jdk1.6.0/jre
JAVA_HOME=$FIJI_JAVA_HOME
ENVOVERRIDES(JAVA_HOME)=true

# Java 3D
FIJI_JAVA_EXT=$FIJI_JAVA_HOME/lib/ext
FIJI_JAVA_EXT(macosx)=$FIJI_JAVA_HOME/Home/lib/ext
JAVA3D_JARS=$FIJI_JAVA_EXT/j3dcore.jar:$FIJI_JAVA_EXT/j3dutils.jar:$FIJI_JAVA_EXT/vecmath.jar

# tools.jar
TOOLS_JAR=$JAVA_HOME/../lib/tools.jar
TOOLS_JAR(macosx)=/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar
ENVOVERRIDES(TOOLS_JAR)=true

# the main target

SUBMODULE_TARGETS=\
	jars/ij.jar \
	plugins/loci_tools.jar \
	jars/VectorString.jar \
	plugins/TrakEM2_.jar \
	plugins/mpicbg_.jar \
	jars/clojure.jar \
	plugins/ij-ImageIO_.jar \
	jars/jacl.jar \
	jars/batik.jar \
	jars/junit.jar \
	jars/rsyntaxtextarea.jar \
	jars/autocomplete.jar \
	jars/weka.jar \
	jars/jython.jar \
	jars/imglib.jar \
	jars/imglib-algorithms.jar \
	jars/imglib-ij.jar \
	jars/imglib-io.jar \
	jars/imglib2.jar \
	jars/imglib2-algorithms.jar \
	jars/imglib2-algorithms-gpl.jar \
	jars/imglib2-ij.jar \
	jars/imglib2-io.jar \
	jars/imglib2-ui.jar \
	jars/mpicbg.jar \
	jars/commons-math.jar \
	jars/javassist.jar \
	jars/jsch.jar \
	jars/imglib-scripting.jar \
	plugins/Image_5D.jar \
	jars/ij-app.jar \

PLUGIN_TARGETS=plugins/Jython_Interpreter.jar \
	plugins/Clojure_Interpreter.jar \
	plugins/JRuby_Interpreter.jar \
	plugins/BeanShell_Interpreter.jar \
	plugins/blockmatching_.jar \
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
	plugins/M_I_P.jar \
	plugins/Interactive_3D_Surface_Plot.jar \
	plugins/View5D_.jar \
	plugins/Volume_Viewer.jar \
	plugins/IJ_Robot.jar \
	plugins/Fiji_Updater.jar \
	plugins/Stitching_.jar \
	plugins/LSM_Reader.jar \
	plugins/AnalyzeSkeleton_.jar \
	plugins/Skeletonize3D_.jar \
	plugins/TurboReg_.jar \
	plugins/Feature_Detection.jar \
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
	plugins/Image_Expression_Parser.jar \
	plugins/Algorithm_Launcher.jar \
	plugins/VIB_.jar \
	plugins/Anisotropic_Diffusion_2D.jar \
	plugins/Simple_Neurite_Tracer.jar \
	plugins/SPIM_Registration.jar \
	plugins/QuickPALM_.jar \
	plugins/3D_Viewer.jar \
	plugins/CPU_Meter.jar \
	plugins/Graph_Cut.jar \
	plugins/TopoJ_.jar \
	plugins/Differentials_.jar \
	plugins/MosaicJ_.jar \
	plugins/PointPicker_.jar \
	plugins/SheppLogan_.jar \
	plugins/StackReg_.jar \
	plugins/UnwarpJ_.jar \
	plugins/Snakuscule_.jar \
	jars/imagescience.jar \
	plugins/TransformJ_.jar \
	plugins/FeatureJ_.jar \
	plugins/RandomJ_.jar \
	plugins/Linear_Kuwahara.jar \
	plugins/Thread_Killer.jar \
	plugins/Samples_.jar \
	plugins/Lasso_and_Blow_Tool.jar \
	jars/mij.jar \
	jars/wavelets.jar \
	jars/imageware.jar \
	plugins/Extended_Depth_Field.jar \
	plugins/panorama_.jar \
	jars/weave_jy2java.jar \
	plugins/3D_Blob_Segmentation.jar \
	plugins/Kuwahara_Filter.jar \
	plugins/Action_Bar.jar \
	plugins/Multi_Kymograph.jar \
	plugins/TrackMate_.jar \
	plugins/Colour_Deconvolution.jar \
	plugins/Dichromacy_.jar \
	plugins/Threshold_Colour.jar \
	plugins/Helmholtz_Analysis.jar \
	plugins/Descriptor_based_registration.jar \
	plugins/SPIM_Opener.jar \
	plugins/Reconstruct_Reader.jar \
	plugins/FS_Align_TrakEM2.jar \
	jars/fiji-compat.jar \
	plugins/Fiji_Package_Maker.jar \
	plugins/BalloonSegmentation_.jar \
	plugins/CorrectBleach_.jar

LEGACYLAUNCHER=fiji

all <- ImageJ $LEGACYLAUNCHER $SUBMODULE_TARGETS $PLUGIN_TARGETS

# The "run" rule just executes ./ImageJ (as long as the file "run" does not exist...)
# It has items on the right side, because these would be passed to the executable.

run[] <- all run-fiji
run-fiji[./ImageJ] <-
DEBUG_ARGS=-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n
dev[./ImageJ $DEBUG_ARGS] <-


# JDK

JDK=java/$PLATFORM
JDK(linux64)=java/linux-amd64
JDK(macosx)=java/macosx-java3d

# Call the Jython script to ensure that the JDK is checked out (from Git)
jdk[bin/checkout-jdk.py $JDK] <-

# Prebuilt (needed to unconfuse Fiji Build and analyze-dependencies)
jars/edu_mines_jtk.jar[] <-
jars/jcommon.jar[] <-
jars/jfreechart.jar[] <-
jars/jna.jar[] <-
jars/postgresql.jar[] <-
jars/jai_core.jar[] <-
jars/jai_codec.jar[] <-
jars/batik.jar[] <-
jars/jzlib.jar[] <-

# From submodules
jars/ij.jar <- jars/javac.jar modules/ImageJA/
CLASSPATH(plugins/mpicbg_.jar)=jars/ij.jar:jars/mpicbg.jar
plugins/mpicbg_.jar <- modules/mpicbg/
CLASSPATH(jars/mpicbg.jar)=jars/ij.jar:jars/Jama.jar
jars/mpicbg.jar <- modules/mpicbg/
CLASSPATH(jars/imglib.jar)=jars/mpicbg.jar
jars/imglib.jar <- modules/imglib/
CLASSPATH(jars/imglib-ij.jar)=jars/ij.jar:jars/imglib.jar:jars/mpicbg.jar
jars/imglib-ij.jar <- modules/imglib/
CLASSPATH(jars/imglib-io.jar)=plugins/loci_tools.jar:jars/imglib.jar:jars/imglib-ij.jar:jars/ij.jar
jars/imglib-io.jar <- modules/imglib/
CLASSPATH(jars/imglib-algorithms.jar)=jars/Jama.jar:jars/imglib.jar:jars/edu_mines_jtk.jar:jars/mpicbg.jar
jars/imglib-algorithms.jar <- modules/imglib/
CLASSPATH(jars/imglib-scripting.jar)=jars/ij.jar:jars/imglib.jar:jars/imglib-io.jars:jars/imglib-algorithms.jar:jars/imglib-ij.jar:plugins/loci_tools.jar:jars/mpicbg.jar:jars/jfreechart.jar:jars/jcommon.jar:$JAVA3D_JARS
jars/imglib-scripting.jar <- modules/imglib/
CLASSPATH(jars/imglib-ops.jar)=jars/imglib.jar
jars/imglib-ops.jar <- modules/imglib/
#CLASSPATH(jars/imglib2.jar)=jars/mpicbg.jar
jars/imglib2.jar <- modules/imglib/
#CLASSPATH(jars/imglib2-ij.jar)=jars/ij.jar:jars/imglib2.jar:jars/mpicbg.jar
CLASSPATH(jars/imglib2-ij.jar)=jars/ij.jar:jars/imglib2.jar:jars/mpicbg.jar
jars/imglib2-ij.jar <- modules/imglib/
CLASSPATH(jars/imglib2-io.jar)=plugins/loci_tools.jar:jars/imglib2.jar
jars/imglib2-io.jar <- modules/imglib/
CLASSPATH(jars/imglib2-algorithms.jar)=jars/imglib2.jar
jars/imglib2-algorithms.jar <- modules/imglib/
CLASSPATH(jars/imglib2-algorithms-gpl.jar)=jars/imglib2.jar:jars/imglib2-algorithms.jar:jars/edu_mines_jtk.jar:jars/mpicbg.jar
jars/imglib2-algorithms-gpl.jar <- modules/imglib/
CLASSPATH(jars/imglib2-scripting.jar)=jars/ij.jar:jars/imglib2.jar:jars/imglib2-io.jars:jars/imglib2-algorithms.jar:jars/imglib2-algorithms-gpl.jar:jars/imglib2-ij.jar:plugins/loci_tools.jar:jars/mpicbg.jar:jars/jfreechart.jar:jars/jcommon.jar:$JAVA3D_JARS
jars/imglib2-scripting.jar <- modules/imglib/
CLASSPATH(jars/imglib2-ops.jar)=jars/imglib2.jar
jars/imglib2-ops.jar <- modules/imglib/
CLASSPATH(jars/imglib2-ui.jar)=jars/imglib2.jar:jars/imglib2-io.jar:plugins/loci_tools.jar
jars/imglib2-ui.jar <- jars/imglib2-io.jar modules/imglib/
jars/clojure.jar <- modules/clojure/
plugins/loci_tools.jar <- modules/bio-formats/
CLASSPATH(plugins/loci_tools.jar)=jars/imglib2.jar
CLASSPATH(jars/VectorString.jar)=jars/ij.jar:jars/Jama.jar:$JAVA3D_JARS
jars/VectorString.jar <- modules/TrakEM2/
CLASSPATH(plugins/TrakEM2_.jar)=jars/ij.jar:jars/jai_core.jar:jars/jai_codec.jar:jars/VectorString.jar:jars/postgresql.jar:jars/jcommon.jar:jars/jfreechart.jar:jars/edu_mines_jtk.jar:jars/VIB-lib.jar:plugins/VIB_.jar:jars/mpicbg.jar:plugins/loci_tools.jar:plugins/bUnwarpJ_.jar:plugins/level_sets.jar:plugins/Fiji_Plugins.jar:jars/Jama.jar:jars/imglib.jar:jars/imglib-algorithms.jar:jars/imglib-ij.jar:plugins/Simple_Neurite_Tracer.jar:plugins/3D_Viewer.jar:plugins/Lasso_and_Blow_Tool.jar:plugins/mpicbg_.jar:$JAVA3D_JARS
plugins/TrakEM2_.jar <- modules/TrakEM2/
plugins/ij-ImageIO_.jar <- modules/ij-plugins/
jars/jacl.jar <- modules/tcljava/
jars/batik.jar <- modules/batik/
jars/junit.jar <- modules/junit/
jars/rsyntaxtextarea.jar <- modules/RSyntaxTextArea/
jars/autocomplete.jar <- modules/AutoComplete/
jars/weka.jar <- jars/fiji-compat.jar modules/weka/
jars/jython.jar <- modules/jython/
jars/commons-math.jar <- modules/commons-math/
jars/javassist.jar <- modules/javassist/
jars/jsch.jar <- modules/jsch/
COPYDEPENDENCIES(jars/ij-app.jar)=true
jars/ij-app.jar <- jars/ij.jar jars/imglib2.jar modules/imagej2/
CLASSPATH(plugins/Image_5D.jar)=jars/ij.jar
plugins/Image_5D.jar <- modules/image5d/

# From source
libs[] <- jars/test-fiji.jar jars/zs.jar jars/VIB-lib.jar jars/Jama.jar \
	jars/fiji-scripting.jar jars/fiji-lib.jar jars/jep.jar \
	jars/pal-optimization.jar jars/Updater_Fix.jar plugins/JNI_Example.jar \
	plugins/FFMPEG_IO.jar \


plugins/Trainable_Segmentation.jar <- src-plugins/Trainable_Segmentation/**/*java src-plugins/Trainable_Segmentation/trainableSegmentation/images/*png src-plugins/Trainable_Segmentation/*

mainClass(jars/fiji-compat.jar)=fiji.Main


CLASSPATH(plugins/Refresh_Javas.jar)=jars/ij.jar:jars/fiji-scripting.jar:jars/fake.jar:jars/fiji-compat.jar
CLASSPATH(plugins/Javascript_.jar)=jars/ij.jar:jars/fiji-scripting.jar:jars/js.jar
CLASSPATH(plugins/CLI_.jar)=jars/ij.jar:jars/fiji-scripting.jar
MAINCLASS(plugins/Script_Editor.jar)=fiji.scripting.Script_Editor
CLASSPATH(plugins/Script_Editor.jar)=jars/ij.jar:jars/rsyntaxtextarea.jar:jars/autocomplete.jar:plugins/Clojure_Interpreter.jar:plugins/JRuby_Interpreter.jar:plugins/Javascript_.jar:plugins/Jython_Interpreter.jar:plugins/Refresh_Javas.jar:plugins/BeanShell_Interpreter.jar:plugins/CLI_.jar:jars/fiji-scripting.jar:jars/fiji-compat.jar:jars/imglib2.jar:jars/fiji-lib.jar:jars/fake.jar:$TOOLS_JAR:jars/jfreechart.jar:jars/imglib2-ij.jar:jars/commons-math.jar
NO_COMPILE(plugins/Script_Editor.jar)=src-plugins/Script_Editor/templates/**/*
src-plugins/Script_Editor/icon.png[bin/copy-file.bsh $PRE $TARGET] <- images/icon.png
src-plugins/Script_Editor/var.png[bin/copy-file.bsh $PRE $TARGET] <- images/var.png
src-plugins/Script_Editor/function.png[bin/copy-file.bsh $PRE $TARGET] <- images/function.png

CLASSPATH(jars/zs.jar)=jars/Jama.jar
CLASSPATH(plugins/Siox_Segmentation.jar)=jars/ij.jar:jars/fiji-lib.jar
CLASSPATH(plugins/Image_Expression_Parser.jar)=jars/ij.jar:jars/jep.jar:jars/imglib.jar:jars/junit.jar:jars/imglib-algorithms.jar:jars/imglib-ij.jar


CLASSPATH(plugins/Stitching_.jar)=jars/ij.jar:plugins/loci_tools.jar:jars/fiji-lib.jar:jars/imglib.jar:jars/imglib-algorithms.jar:jars/imglib-ij.jar:jars/edu_mines_jtk.jar:plugins/Fiji_Plugins.jar:jars/mpicbg.jar
CLASSPATH(plugins/TrackMate_.jar)=jars/itextpdf.jar:jars/batik.jar:jars/ij.jar:plugins/3D_Viewer.jar:plugins/loci_tools.jar:jars/fiji-lib.jar:jars/imglib.jar:jars/imglib-ij.jar:jars/imglib-io.jar:jars/imglib-algorithms.jar:jars/jdom.jar:jars/jdom-contrib.jar:jars/jgraphx.jar:jars/jgrapht-jdk1.6.jar:jars/jfreechart.jar:jars/jcommon.jar:jars/Jama.jar:$JAVA3D_JARS
CLASSPATH(plugins/Trainable_Segmentation.jar)=jars/ij.jar:jars/weka.jar:plugins/Stitching_.jar:jars/fiji-lib.jar:plugins/Anisotropic_Diffusion_2D.jar:jars/Jama.jar:jars/VIB-lib.jar:jars/commons-math.jar:jars/imglib.jar:jars/imglib-ij.jar:jars/imglib-algorithms.jar:jars/imagescience.jar:jars/imglib2.jar:jars/imglib2-ij.jar:jars/imglib2-algorithms.jar:$JAVA3D_JARS
CLASSPATH(jars/VIB-lib.jar)=jars/ij.jar:jars/Jama.jar:jars/junit.jar:jars/pal-optimization.jar:jars/jzlib.jar:jars/fiji-lib.jar
CLASSPATH(plugins/SPIM_Opener.jar)=jars/ij.jar:jars/fiji-lib.jar
CLASSPATH(jars/jep.jar)=jars/ij.jar:jars/Jama.jar:jars/junit.jar
CLASSPATH(plugins/SPIM_Registration.jar)=jars/ij.jar:$JAVA3D_JARS:jars/imglib.jar:jars/mpicbg.jar:plugins/3D_Viewer.jar:jars/weka.jar:jars/fiji-lib.jar:plugins/loci_tools.jar:plugins/Fiji_Plugins.jar:jars/VIB-lib.jar:jars/Jama.jar:jars/imglib-algorithms.jar:jars/imglib-ij.jar:jars/imglib-io.jar:jars/jfreechart.jar:jars/jcommon.jar:plugins/SPIM_Opener.jar
CLASSPATH(plugins/Descriptor_based_registration.jar)=jars/ij.jar:jars/imglib.jar:jars/mpicbg.jar:jars/fiji-lib.jar:plugins/Fiji_Plugins.jar:jars/VIB-lib.jar:jars/Jama.jar:jars/imglib-algorithms.jar:jars/imglib-ij.jar:jars/imglib-io.jar:plugins/SPIM_Registration.jar:plugins/Stitching_.jar:$JAVA3D_JARS
CLASSPATH(plugins/Arrow_.jar)=jars/ij.jar:jars/fiji-lib.jar
CLASSPATH(plugins/Colocalisation_Analysis.jar)=jars/ij.jar:jars/imglib2.jar:jars/imglib2-ij.jar:jars/imglib2-algorithms.jar:jars/junit.jar:jars/itextpdf.jar:jars/fiji-lib.jar
CLASSPATH(jars/autocomplete.jar)=jars/rsyntaxtextarea.jar
CLASSPATH(jars/jython.jar)=jars/junit.jar:jars/jna.jar
CLASSPATH(plugins/loci_tools.jar)=jars/ij.jar
CLASSPATH(jars/batik.jar)=jars/jacl.jar:plugins/loci_tools.jar:jars/jython.jar
CLASSPATH(jars/ij.jar)=jars/javac.jar
CLASSPATH(plugins/ij-ImageIO_.jar)=jars/ij.jar:jars/jai_core.jar:jars/jai_codec.jar
CLASSPATH(plugins/Graph_Cut.jar)=jars/ij.jar:jars/imglib.jar:jars/imglib-ij.jar:jars/fiji-lib.jar
CLASSPATH(jars/weave_jy2java.jar)=plugins/Refresh_Javas.jar:jars/fiji-scripting.jar:jars/fiji-compat.jar:jars/ij.jar:plugins/Script_Editor.jar
LIBS(plugins/JNI_Example.jar)=-lm
CLASSPATH(plugins/JNI_Example.jar)=jars/ij.jar:jars/fiji-lib.jar
CLASSPATH(plugins/Fiji_Package_Maker.jar)=jars/ij.jar:plugins/Fiji_Updater.jar:jars/fiji-lib.jar

# pom.xml sub-projects

jars/VIB-lib.jar <- src-plugins/VIB-lib/pom.xml
jars/pal-optimization.jar <- src-plugins/pal-optimization/pom.xml
jars/Jama.jar <- src-plugins/Jama/pom.xml
plugins/blockmatching_.jar <- src-plugins/blockmatching_/pom.xml
plugins/bUnwarpJ_.jar <- src-plugins/bUnwarpJ_/pom.xml
plugins/registration_3d.jar <- src-plugins/registration_3d/pom.xml
plugins/IO_.jar <- src-plugins/IO_/pom.xml
plugins/LSM_Toolbox.jar <- src-plugins/LSM_Toolbox/pom.xml
plugins/SplineDeformationGenerator_.jar <- src-plugins/SplineDeformationGenerator_/pom.xml
plugins/level_sets.jar <- src-plugins/level_sets/pom.xml
plugins/Analyze_Reader_Writer.jar <- src-plugins/Analyze_Reader_Writer/pom.xml
plugins/Color_Histogram.jar <- src-plugins/Color_Histogram/pom.xml
plugins/Color_Inspector_3D.jar <- src-plugins/Color_Inspector_3D/pom.xml
plugins/M_I_P.jar <- src-plugins/M_I_P/pom.xml
plugins/Interactive_3D_Surface_Plot.jar <- src-plugins/Interactive_3D_Surface_Plot/pom.xml
plugins/View5D_.jar <- src-plugins/View5D_/pom.xml
plugins/Volume_Viewer.jar <- src-plugins/Volume_Viewer/pom.xml
plugins/IJ_Robot.jar <- src-plugins/IJ_Robot/pom.xml
plugins/Fiji_Updater.jar <- src-plugins/Fiji_Updater/pom.xml
plugins/LSM_Reader.jar <- src-plugins/LSM_Reader/pom.xml
plugins/AnalyzeSkeleton_.jar <- src-plugins/AnalyzeSkeleton_/pom.xml
plugins/Skeletonize3D_.jar <- src-plugins/Skeletonize3D_/pom.xml
plugins/TurboReg_.jar <- src-plugins/TurboReg_/pom.xml
plugins/Feature_Detection.jar <- src-plugins/Feature_Detection/pom.xml
plugins/Bug_Submitter.jar <- src-plugins/Bug_Submitter/pom.xml
plugins/ToAST_.jar <- src-plugins/ToAST_/pom.xml
plugins/MTrack2_.jar <- src-plugins/MTrack2_/pom.xml
plugins/Time_Stamper.jar <- src-plugins/Time_Stamper/pom.xml
plugins/Series_Labeler.jar <- src-plugins/Series_Labeler/pom.xml
plugins/Statistical_Region_Merging.jar <- src-plugins/Statistical_Region_Merging/pom.xml
plugins/Auto_Threshold.jar <- src-plugins/Auto_Threshold/pom.xml
plugins/Stack_Manipulation.jar <- src-plugins/Stack_Manipulation/pom.xml
plugins/FlowJ_.jar <- src-plugins/FlowJ_/pom.xml
plugins/PIV_analyser.jar <- src-plugins/PIV_analyser/pom.xml
plugins/Video_Editing.jar <- src-plugins/Video_Editing/pom.xml
plugins/Sync_Win.jar <- src-plugins/Sync_Win/pom.xml
plugins/Gray_Morphology.jar <- src-plugins/Gray_Morphology/pom.xml
plugins/LocalThickness_.jar <- src-plugins/LocalThickness_/pom.xml
plugins/Fiji_Developer.jar <- src-plugins/Fiji_Developer/pom.xml
plugins/Manual_Tracking.jar <- src-plugins/Manual_Tracking/pom.xml
plugins/Calculator_Plus.jar <- src-plugins/Calculator_Plus/pom.xml
plugins/3D_Objects_Counter.jar <- src-plugins/3D_Objects_Counter/pom.xml
plugins/IsoData_Classifier.jar <- src-plugins/IsoData_Classifier/pom.xml
plugins/RATS_.jar <- src-plugins/RATS_/pom.xml
plugins/Directionality_.jar <- src-plugins/Directionality_/pom.xml
plugins/Algorithm_Launcher.jar <- src-plugins/Algorithm_Launcher/pom.xml
plugins/Anisotropic_Diffusion_2D.jar <- src-plugins/Anisotropic_Diffusion_2D/pom.xml
plugins/Simple_Neurite_Tracer.jar <- src-plugins/Simple_Neurite_Tracer/pom.xml
plugins/QuickPALM_.jar <- src-plugins/QuickPALM_/pom.xml
plugins/3D_Viewer.jar <- src-plugins/3D_Viewer/pom.xml
plugins/CPU_Meter.jar <- src-plugins/CPU_Meter/pom.xml
plugins/TopoJ_.jar <- src-plugins/TopoJ_/pom.xml
plugins/Differentials_.jar <- src-plugins/Differentials_/pom.xml
plugins/MosaicJ_.jar <- src-plugins/MosaicJ_/pom.xml
plugins/PointPicker_.jar <- src-plugins/PointPicker_/pom.xml
plugins/SheppLogan_.jar <- src-plugins/SheppLogan_/pom.xml
plugins/StackReg_.jar <- src-plugins/StackReg_/pom.xml
plugins/UnwarpJ_.jar <- src-plugins/UnwarpJ_/pom.xml
plugins/Snakuscule_.jar <- src-plugins/Snakuscule_/pom.xml
jars/imagescience.jar <- src-plugins/imagescience/pom.xml
plugins/TransformJ_.jar <- src-plugins/TransformJ_/pom.xml
plugins/FeatureJ_.jar <- src-plugins/FeatureJ_/pom.xml
plugins/RandomJ_.jar <- src-plugins/RandomJ_/pom.xml
plugins/Linear_Kuwahara.jar <- src-plugins/Linear_Kuwahara/pom.xml
plugins/Thread_Killer.jar <- src-plugins/Thread_Killer/pom.xml
plugins/Samples_.jar <- src-plugins/Samples_/pom.xml
jars/mij.jar <- src-plugins/mij/pom.xml
jars/wavelets.jar <- src-plugins/wavelets/pom.xml
jars/imageware.jar <- src-plugins/imageware/pom.xml
plugins/Extended_Depth_Field.jar <- src-plugins/Extended_Depth_Field/pom.xml
plugins/panorama_.jar <- src-plugins/panorama_/pom.xml
plugins/3D_Blob_Segmentation.jar <- src-plugins/3D_Blob_Segmentation/pom.xml
plugins/Kuwahara_Filter.jar <- src-plugins/Kuwahara_Filter/pom.xml
plugins/Action_Bar.jar <- src-plugins/Action_Bar/pom.xml
plugins/Multi_Kymograph.jar <- src-plugins/Multi_Kymograph/pom.xml
plugins/Colour_Deconvolution.jar <- src-plugins/Colour_Deconvolution/pom.xml
plugins/Dichromacy_.jar <- src-plugins/Dichromacy_/pom.xml
plugins/Threshold_Colour.jar <- src-plugins/Threshold_Colour/pom.xml
plugins/Helmholtz_Analysis.jar <- src-plugins/Helmholtz_Analysis/pom.xml
plugins/Reconstruct_Reader.jar <- src-plugins/Reconstruct_Reader/pom.xml
plugins/FS_Align_TrakEM2.jar <- src-plugins/FS_Align_TrakEM2/pom.xml
jars/fiji-compat.jar <- src-plugins/fiji-compat/pom.xml
plugins/BalloonSegmentation_.jar <- src-plugins/BalloonSegmentation_/pom.xml
plugins/CorrectBleach_.jar <- src-plugins/CorrectBleach_/pom.xml
jars/javac.jar <- src-plugins/javac/pom.xml
jars/fiji-scripting.jar <- src-plugins/fiji-scripting/pom.xml
plugins/Jython_Interpreter.jar <- src-plugins/Jython_Interpreter/pom.xml
plugins/Clojure_Interpreter.jar <- src-plugins/Clojure_Interpreter/pom.xml
plugins/JRuby_Interpreter.jar <- src-plugins/JRuby_Interpreter/pom.xml
plugins/BeanShell_Interpreter.jar <- src-plugins/BeanShell_Interpreter/pom.xml
jars/fiji-lib.jar <- src-plugins/fiji-lib/pom.xml
plugins/Lasso_and_Blow_Tool.jar <- src-plugins/Lasso_and_Blow_Tool/pom.xml
plugins/Fiji_Plugins.jar <- src-plugins/Fiji_Plugins/pom.xml
plugins/VIB_.jar <- src-plugins/VIB_/pom.xml
plugins/register_virtual_stack_slices.jar <- src-plugins/register_virtual_stack_slices/pom.xml

# pre-Java5 generics ;-)

src-plugins/VIB-lib/vib/FloatMatrix.java[src-plugins/VIB-lib/sed.py $PRE $TARGET] <- src-plugins/VIB-lib/vib/FastMatrix.java
src-plugins/VIB-lib/math3d/FloatMatrixN.java[src-plugins/VIB-lib/sed.py $PRE $TARGET] <- src-plugins/VIB-lib/math3d/FastMatrixN.java
src-plugins/VIB-lib/math3d/JacobiFloat.java[src-plugins/VIB-lib/sed.py $PRE $TARGET] <- src-plugins/VIB-lib/math3d/JacobiDouble.java
src-plugins/VIB-lib/math3d/Eigensystem3x3Float.java[src-plugins/VIB-lib/sed.py $PRE $TARGET] <- \
	src-plugins/VIB-lib/math3d/Eigensystem3x3Double.java
src-plugins/VIB-lib/math3d/Eigensystem2x2Float.java[src-plugins/VIB-lib/sed.py $PRE $TARGET] <- \
	src-plugins/VIB-lib/math3d/Eigensystem2x2Double.java

MAINCLASS(jars/test-fiji.jar)=fiji.Tests
CLASSPATH(jars/test-fiji.jar)=jars/junit.jar

MAINCLASS(jars/Updater_Fix.jar)=fiji.updater.Fix

# This also compiles lib/<platform>/<ffmpeg-library>
CLASSPATH(plugins/FFMPEG_IO.jar)=jars/ij.jar
plugins/FFMPEG_IO.jar[src-plugins/FFMPEG_IO/generate.bsh] <- src-plugins/FFMPEG_IO/**/*

# This compiles and cross-compiles lib/<platform>/<ffmpeg-library>
CLASSPATH(plugins/FFMPEG_IO.jar-cross)=jars/ij.jar
plugins/FFMPEG_IO.jar-cross[src-plugins/FFMPEG_IO/generate.bsh all] <- src-plugins/FFMPEG_IO/**/*

# the default rules

plugins/*.jar <- src-plugins/*/**/*
jars/*.jar <- src-plugins/*/**/*

# headless.jar

misc/headless.jar[bin/make-headless-jar.bsh] <- jars/fiji-compat.jar jars/javassist.jar jars/ij.jar

# ImageJ launcher

# We re-use ImageJ2's launcher now, so let's use the updater to perform
# the job.

ImageJ[sh bin/download-launchers.sh snapshot $PLATFORM] <- plugins/Fiji_Updater.jar

# legacy launcher

fiji[bin/copy-file.bsh $PRE $TARGET] <- ImageJ

# Precompiled stuff

precompile-fake[] <- precompiled/fake.jar
precompiled/fake.jar <- jars/fake.jar
precompiled/javac.jar <- jars/javac.jar
precompiled/ij.jar <- jars/ij.jar
precompiled/mpicbg.jar <- jars/mpicbg.jar
precompiled/Image_5D.jar <- plugins/Image_5D.jar
precompiled/*[bin/copy-file.bsh $PRE $TARGET] <- *

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
	precompiled/junit.jar \
	precompiled/rsyntaxtextarea.jar \
	precompiled/autocomplete.jar \
	precompiled/weka.jar \
	precompiled/jython.jar \
	precompiled/imglib.jar \
	precompiled/commons-math.jar \
	precompiled/imglib-algorithms.jar \
	precompiled/javassist.jar \
	precompiled/jsch.jar \

precompiled/ij.jar <- jars/ij.jar
precompiled/clojure.jar <- jars/clojure.jar
precompiled/jacl.jar <- jars/jacl.jar
precompiled/batik.jar <- jars/batik.jar
precompiled/junit.jar <- jars/junit.jar
precompiled/rsyntaxtextarea.jar <- jars/rsyntaxtextarea.jar
precompiled/autocomplete.jar <- jars/autocomplete.jar
precompiled/weka.jar <- jars/weka.jar
precompiled/jython.jar <- jars/jython.jar
precompiled/imglib.jar <- jars/imglib.jar
precompiled/imglib-algorithms.jar <- jars/imglib-algorithms.jar
precompiled/imglib-ij.jar <- jars/imglib-ij.jar
precompiled/imglib-io.jar <- jars/imglib-io.jar
precompiled/imglib-scripting.jar <- jars/imglib-scripting.jar
precompiled/commons-math.jar <- jars/commons-math.jar
precompiled/javassist.jar <- jars/javassist.jar
precompiled/jsch.jar <- jars/jsch.jar
precompiled/* <- plugins/*

precompile[] <- precompile-ImageJ precompile-fake precompile-submodules

# precompiled fall back

missingPrecompiledFallBack[sh ./bin/ImageJ.sh --update update $TARGET] <- \
	plugins/Fiji_Updater.jar

# Portable application/.app

all-apps[] <- app-macosx app-linux32 app-linux64 app-win32 app-win64
MACOSX_TIGER_LAUNCHER(macosx)=ImageJ-tiger
app-*[bin/make-app.py * $PLATFORM] <- all $MACOSX_TIGER_LAUNCHER

app-all[bin/make-app.py all $PLATFORM] <- all
app-nojre[bin/make-app.py nojre $PLATFORM] <- all

all-dmgs[] <- fiji-macosx.dmg
fiji-*.dmg[bin/make-dmg.py] <- app-* Fiji.app \
	resources/install-fiji.jpg
dmg[] <- fiji-macosx.dmg

resources/install-fiji.jpg[./fiji bin/generate-finder-background.py] <- \
	bin/generate-finder-background.py

all-tars[] <- fiji-linux32.tar.bz2 fiji-linux64.tar.bz2 \
	fiji-all.tar.bz2 fiji-nojre.tar.bz2
fiji-*.tar.bz2[bin/make-tar.py $TARGET Fiji.app] <- app-* Fiji.app
tar[] <- fiji-$PLATFORM.tar.bz2

all-zips[] <- fiji-linux32.zip fiji-linux64.zip fiji-win32.zip fiji-win64.zip \
	fiji-all.zip fiji-nojre.zip
fiji-*.zip[bin/make-zip.py $TARGET Fiji.app] <- app-* Fiji.app
zip[] <- fiji-$PLATFORM.zip

all-isos[] <- fiji-linux32.iso fiji-linux64.iso fiji-win32.iso fiji-win64.iso \
	fiji-macosx.iso fiji-all.iso fiji-nojre.iso
fiji-*.iso[genisoimage -J -V Fiji -o $TARGET Fiji.app] <- app-*

all-7zs[] <- fiji-linux32.7z fiji-linux64.7z fiji-win32.7z fiji-win64.7z \
	fiji-macosx.7z fiji-all.7z fiji-nojre.7z
fiji-*.7z[bin/make-7z.py $TARGET Fiji.app] <- app-*

# Checks

check[] <- check-launchers check-submodules

LAUNCHERS=$LAUNCHER(linux32) $LAUNCHER(linux64) \
	$LAUNCHER(win32) $LAUNCHER(win64) $LAUNCHER(macosx)
check-launchers[bin/up-to-date-check.py ImageJ.c $LAUNCHERS] <-

check-submodules[] <- check-ij check-TrakEM2 check-mpicbg

check-ij[bin/up-to-date-check.py ImageJA precompiled/ij.jar] <-
check-*[bin/up-to-date-check.py * precompiled/*_.jar] <-

# Fake itself

MAINCLASS(jars/fake.jar)=fiji.build.Fake
jars/fake.jar <- src-plugins/fake/**/*.java
