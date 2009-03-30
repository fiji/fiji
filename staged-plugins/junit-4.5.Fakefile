JAVAVERSION=1.5
JAR=junit-4.5.jar
DISTDIR=junit4.5
TARGET=build jars
all <- $JAR

MAINCLASS(junit-4.5.jar)=org.junit.runner.JUnitCore
$JAR <- $DISTDIR/$JAR

$DISTDIR/$JAR[../fiji --ant $TARGET] <-
