JAVAVERSION=1.5
DISTDIR=junit4.5
ANTTARGET=build jars
all <- junit.jar

MAINCLASS(junit.jar)=org.junit.runner.JUnitCore
junit.jar <- $DISTDIR/junit-4.5.jar

$DISTDIR/junit-4.5.jar[../../bin/ImageJ.sh --ant $ANTTARGET] <-
