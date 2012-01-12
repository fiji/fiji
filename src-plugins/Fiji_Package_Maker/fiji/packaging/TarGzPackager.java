package fiji.packaging;

import java.io.IOException;
import java.io.OutputStream;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TarGzPackager extends TarPackager {
	@Override
	public String getExtension() {
		return ".tar.gz";
	}

	@Override
	public void open(OutputStream out) throws IOException {
		this.out = new GZIPOutputStream(out);
	}
}