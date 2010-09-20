buildDir=build/
javaVersion=1.5
all <- FFMPEG_IO.jar

FFMPEG_IO.jar <- plugin.jar/ jna-wrapper.jar/ \
	linux/libffmpeg.so[ffmpeg/libffmpeg.so]

CLASSPATH(plugin.jar)=jna-wrapper.jar
plugin.jar <- plugin/**/* 

jna-wrapper.jar <- classes/**/*

generate-classes[generate.bsh] <- generator.jar

generator.jar <- GenerateFFMPEGClasses.java
