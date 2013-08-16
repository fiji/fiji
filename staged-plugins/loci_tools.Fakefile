JAVAVERSION=1.5
all <- loci_tools.jar

ABOUT=components/loci-plugins/build/src/loci/plugins/About.java

MAINCLASS(loci_tools.jar)=loci.formats.gui.ImageViewer
loci_tools.jar <- artifacts/loci_tools.jar

artifacts/loci_tools.jar[../../bin/ImageJ.sh --ant tools] <- $ABOUT

$ABOUT[../../bin/ImageJ.sh --ant clean] <- .git/HEAD
