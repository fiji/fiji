package fiji.updater.logic;

import fiji.updater.logic.FileUploader.SourceFile;

import fiji.updater.util.Util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/*
 * Implementation of class containing information for FileUploader to use
 */
public class UpdateSource implements SourceFile {
	private String permissions;
	private String sourceFilename, filename;
	private long filesize;

	public UpdateSource(String target) {
		this(Util.prefix(target), target);
	}

	public UpdateSource(PluginObject plugin) {
		this(Util.prefix(plugin.getFilename()),
			plugin.getFilename() + "-" + plugin.getTimestamp());
	}


	public UpdateSource(String source, String target) {
		this(source, target, "C0644");
	}

	public UpdateSource(String source, String target, String permissions) {
		// TODO: fix naming
		this.sourceFilename = source;
		this.filename = target;
		this.permissions = permissions;
		filesize = new File(source).length();
	}
	//********** Implemented methods for SourceFile **********
	public long getFilesize() {
		return filesize;
	}

	public String getFilename() {
		return filename;
	}

	public String getPermissions() {
		return permissions;
	}

	public InputStream getInputStream() throws IOException {
		try {
			return new FileInputStream(sourceFilename);
		} catch (FileNotFoundException e) {
			return new ByteArrayInputStream(new byte[0]);
		}
	}
}
