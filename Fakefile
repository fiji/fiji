buildDir=build/
javaVersion=1.5
all <- FFMPEG_IO.jar

CLASSPATH(FFMPEG_IO.jar)=jna-wrapper.jar
FFMPEG_IO.jar <- plugin/**/* jna-wrapper.jar/

jna-wrapper.jar <- classes/**/*

generate-classes[generate.bsh] <- generator.jar

generator.jar <- GenerateFFMPEGClasses.java
