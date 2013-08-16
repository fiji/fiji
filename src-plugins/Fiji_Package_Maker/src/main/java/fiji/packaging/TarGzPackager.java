package fiji.packaging;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

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