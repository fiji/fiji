#include "JNI_Example.h"
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

static jclass byteArrayClass, shortArrayClass, intArrayClass, floatArrayClass;

static void getClasses(JNIEnv *env)
{
	byteArrayClass = (*env)->FindClass(env, "[B");
	shortArrayClass = (*env)->FindClass(env, "[S");
	intArrayClass = (*env)->FindClass(env, "[I");
	floatArrayClass = (*env)->FindClass(env, "[F");
}

static float cumulative, cumulative2, count;

static void add(JNIEnv *env, int width, int height, jarray array)
{
	int i;

	if ((*env)->IsInstanceOf(env, array, byteArrayClass)) {
		/* For pinning, the 3rd arg needs to point to an int which equals JNI_TRUE */
		jbyte *values = (*env)->GetByteArrayElements(env, (jbyteArray)array, NULL);
		for (i = 0; i < width * height; i++) {
			float value = (unsigned char)values[i];
			cumulative += value;
			cumulative2 += value * value;
		}
		/* To reuse the values, use 0 instead of JNI_ABORT; that works only when the data are pinned. */
		(*env)->ReleaseByteArrayElements(env, (jbyteArray)array, values, JNI_ABORT);
		fprintf(stderr, "Added %d bytes\n", width * height);
	}
	else if ((*env)->IsInstanceOf(env, array, shortArrayClass)) {
		/* For pinning, the 3rd arg needs to point to an int which equals JNI_TRUE */
		jshort *values = (*env)->GetShortArrayElements(env, (jshortArray)array, NULL);
		for (i = 0; i < width * height; i++) {
			float value = (unsigned short)values[i];
			cumulative += value;
			cumulative2 += value * value;
		}
		/* To reuse the values, use 0 instead of JNI_ABORT; that works only when the data are pinned. */
		(*env)->ReleaseShortArrayElements(env, (jshortArray)array, values, JNI_ABORT);
		fprintf(stderr, "Added %d shorts\n", width * height);
	}
	else if ((*env)->IsInstanceOf(env, array, intArrayClass)) {
		/* For pinning, the 3rd arg needs to point to an int which equals JNI_TRUE */
		jint *values = (*env)->GetIntArrayElements(env, (jintArray)array, NULL);
		for (i = 0; i < width * height; i++) {
			float value = (float)(values[i] & 0xff); /* blue */
			cumulative += value;
			cumulative2 += value * value;
			value = (float)((values[i] >> 8) & 0xff); /* green */
			cumulative += value;
			cumulative2 += value * value;
			value = (float)((values[i] >> 16) & 0xff); /* red */
			cumulative += value;
			cumulative2 += value * value;
		}
		/* To reuse the values, use 0 instead of JNI_ABORT; that works only when the data are pinned. */
		(*env)->ReleaseIntArrayElements(env, (jintArray)array, values, JNI_ABORT);
		fprintf(stderr, "Added 3 * %d int-packed bytes\n", width * height);
		count += (float)(2 * width * height);
	}
	else if ((*env)->IsInstanceOf(env, array, floatArrayClass)) {
		/* For pinning, the 3rd arg needs to point to an int which equals JNI_TRUE */
		jfloat *values = (*env)->GetFloatArrayElements(env, (jfloatArray)array, NULL);
		for (i = 0; i < width * height; i++) {
			float value = (float)values[i];
			cumulative += value;
			cumulative2 += value * value;
		}
		/* To reuse the values, use 0 instead of JNI_ABORT; that works only when the data are pinned. */
		(*env)->ReleaseFloatArrayElements(env, (jfloatArray)array, values, JNI_ABORT);
		fprintf(stderr, "Added %d floats\n", width * height);
	}
	else {
		fprintf(stderr, "Unknown array type\n");
		return;
	}
	count += (float)(width * height);
}

JNIEXPORT jobject JNICALL Java_JNI_1Example_run(JNIEnv *env, jclass clazz,
	jstring arg, jstring title,
	jint width, jint height, jint channels, jint slices, jint frames,
	jobjectArray pixels)
{
	const char *arg_str = (*env)->GetStringUTFChars(env, arg, NULL);
	const char *title_str = (*env)->GetStringUTFChars(env, title, NULL);
	int i, stack_size;
	float avg, stddev;

	getClasses(env);

	fprintf(stderr, "Statistics of %s with arg %s and dimensions %dx%dx%dx%dx%d:\n", title_str, arg_str, width, height, channels, slices, frames);

	stack_size = (*env)->GetArrayLength(env, pixels);
	for (i = 0; i < stack_size; i++)
		add(env, width, height, (jarray)(*env)->GetObjectArrayElement(env, pixels, i));

	avg = cumulative / count;
	stddev = cumulative2 / count;
	stddev = (float)sqrt(stddev - avg * avg);
	fprintf(stderr, "count %f, avg %f, stddev %f\n", count, avg, stddev);

	(*env)->ReleaseStringUTFChars(env, title, title_str);
	(*env)->ReleaseStringUTFChars(env, arg, arg_str);

	return 0;
}
