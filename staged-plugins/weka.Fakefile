JAVAVERSION=1.5
all <- weka.jar

weka.jar[sh -c "cp $PRE $TARGET && \
               ../bin/fix-java6-classes.sh $TARGET"]  <- weka/dist/weka.jar

weka/dist/weka.jar[../fiji --ant -f weka/build.xml exejar] <-

