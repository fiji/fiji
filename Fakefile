buildDir=build/
javaVersion=1.5
all <- FFMPEG_IO.jar

FFMPEG_IO.jar <- plugin.jar/ jna-wrapper.jar/ \
	linux/libavutil.so[ffmpeg/libavutil/libavutil.so] \
	linux/libavcore.so[ffmpeg/libavcore/libavcore.so] \
	linux/libavdevice.so[ffmpeg/libavdevice/libavdevice.so] \
	linux/libavcodec.so[ffmpeg/libavcodec/libavcodec.so] \
	linux/libavformat.so[ffmpeg/libavformat/libavformat.so]

CLASSPATH(plugin.jar)=jna-wrapper.jar
plugin.jar <- plugin/**/* 

jna-wrapper.jar <- classes/**/*

generate-classes[generate.bsh] <- generator.jar

generator.jar <- GenerateFFMPEGClasses.java
