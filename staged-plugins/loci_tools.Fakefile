JAVAVERSION=1.5
all <- loci_tools.jar

ABOUT=components/loci-plugins/build/src/loci/plugins/About.java

MAINCLASS(loci_tools.jar)=loci.ome.notes.Notes
loci_tools.jar <- artifacts/loci_tools.jar

artifacts/loci_tools.jar[../../ImageJ --ant tools] <- $ABOUT

$ABOUT[../../ImageJ --ant clean] <- .git/HEAD
