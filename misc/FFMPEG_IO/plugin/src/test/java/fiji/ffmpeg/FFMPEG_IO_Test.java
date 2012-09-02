package fiji.ffmpeg;

import static org.junit.Assert.*;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.StackConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.junit.Test;

public class FFMPEG_IO_Test {
	protected final int width = 512, height = 512, frameRate = 25, bitRate = 400000;

	@Test
	public void testWriteAndRead() throws Exception {
		unpackNar();

		File tmp = File.createTempFile("ffmpeg-", ".avi");
		tmp.deleteOnExit();
		ImagePlus image = generateStack(30);

		IO io = new IO();
		io.writeMovie(image, tmp.getPath(), frameRate, bitRate);

		ImagePlus read = io.readMovie(tmp.getPath(), false, 0, -1);
		new StackConverter(read).convertToGray8();
		int maxDiff = getMaxDiff(image, read);
		assertTrue(maxDiff < 5);
	}

	protected void unpackNar() throws IOException {
		String suffix = "/" + getClass().getName().replace('.', '/') + ".class";
		String url = getClass().getResource(suffix).toString();
		if (url.startsWith("file:"))
			url = url.substring(5);
		if (url.endsWith(suffix))
			url = url.substring(0, url.length() - suffix.length());
		if (url.endsWith("/test-classes"))
			url = url.substring(0, url.length() - "/test-classes".length());
		File target = new File(url);
		File ijDir = new File(target, "ij");
		File lib = new File(ijDir, "lib/" + JNALibraryLoader.getPlatform());
		if (!lib.exists())
			assertTrue(lib.mkdirs());

		String osName = System.getProperty("os.name");
		boolean isWindows = osName.startsWith("Win");
		boolean isMacOSX = osName.indexOf("OS X") > 0;
		String arch = System.getProperty("os.arch");
		boolean is64Bit = arch != null && arch.indexOf("64") >= 0;
		String narSuffix = "-"
				+ (is64Bit ? (isMacOSX ? "x86_64" : "amd64") : (isWindows ? "x86" : "i386"))
				+ "-"
				+ (isWindows ? "Windows" : isMacOSX ? "MacOSX" : "Linux")
				+ "-gcc-shared";

		File nar = new File(target, "../../ffmpeg/target/ffmpeg-native-2.0.0-SNAPSHOT" + narSuffix + ".nar");
		JarInputStream in = new JarInputStream(new FileInputStream(nar));
		byte[] buffer = new byte[65536];
		for (;;) {
			JarEntry entry = in.getNextJarEntry();
			if (entry == null)
				break;
			FileOutputStream out = new FileOutputStream(new File(lib, entry.getName()));
			for (;;) {
				int count = in.read(buffer);
				if (count < 0)
					break;
				out.write(buffer, 0, count);
			}
			out.close();
		}
		in.close();
		System.setProperty("ij.dir", ijDir.getPath());
	}

	protected ImagePlus generateStack(int sliceCount) {
		ImagePlus result = IJ.createImage("Test Movie", "8-bit", width, height, sliceCount);
		ImageStack stack = result.getStack();
		for (int slice = 1; slice <= sliceCount; slice++)
			generateGradient(stack.getProcessor(slice), slice * Math.PI / 12);
		return result;
	}

	protected void generateGradient(ImageProcessor ip, double angle) {
		double c = Math.cos(angle), s = Math.sin(angle), factor = 255 / Math.sqrt(width * width + height * height);
		byte[] pixels = (byte[])ip.getPixels();
		for (int j = 0; j < height; j++)
			for (int i = 0; i < width; i++)
				pixels[i + width * j] = (byte)(127 + factor * ((i - width / 2.0) * s - (j - height / 2.0) * c));
	}

	protected int getMaxDiff(ImagePlus a, ImagePlus b) {
		return getMaxDiff(a.getStack(), b.getStack());
	}

	protected int getMaxDiff(ImageStack a, ImageStack b) {
		int diff = 0;
		int m = a.getSize(), n = b.getSize();
		for (int i = 1; i <= m || i <= n; i++) {
			diff = Math.max(diff, getMaxDiff(i <= m ? a.getProcessor(i) : null, i <= n ? b.getProcessor(i) : null));
		}
		return diff;
	}

	protected int getMaxDiff(ImageProcessor a, ImageProcessor b) {
		if (a == null)
			return (int)b.maxValue();
		else if (b == null)
			return (int)a.maxValue();

		int diff = 0;
		int w1 = a.getWidth(), h1 = a.getHeight();
		int w2 = b.getWidth(), h2 = b.getHeight();
		for (int j = 0; j < h1 || j < h2; j++)
			for (int i = 0; i < w1 || i < w2; i++) {
				if (i >= w1 || j >= h1)
					diff = Math.max(diff,  (int)b.getf(i, j));
				else if (i >= w2 || j >= h2)
					diff = Math.max(diff,  (int)a.getf(i, j));
				else
					diff = Math.max(diff, (int)Math.abs(a.getf(i, j) - b.getf(i, j)));
			}
		return diff;
	}
}
