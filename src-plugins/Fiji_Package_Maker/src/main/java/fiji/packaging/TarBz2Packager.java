package fiji.packaging;

import java.io.IOException;
import java.io.OutputStream;

import java.lang.reflect.Constructor;

public class TarBz2Packager extends TarPackager {
	protected Constructor<OutputStream> ctor;

	public TarBz2Packager() throws ClassNotFoundException {
		Class<OutputStream> clazz = (Class<OutputStream>)getClass().getClassLoader().loadClass("org.apache.tools.bzip2.CBZip2OutputStream");
		try {
			ctor = clazz.getConstructor(OutputStream.class);
		} catch (NoSuchMethodException e) {
			throw new ClassNotFoundException("Incompatible CBZip2OutputStream");
		}
	}

	@Override
	public String getExtension() {
		return ".tar.bz2";
	}

	@Override
	public void open(OutputStream out) throws IOException {
		try {
			out.write("BZ".getBytes());
			this.out = ctor.newInstance(out);
		} catch(Exception e) {
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}
	}
}