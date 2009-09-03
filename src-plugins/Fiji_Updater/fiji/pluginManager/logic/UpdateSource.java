package fiji.pluginManager.logic;

import fiji.pluginManager.logic.FileUploader.SourceFile;

import fiji.pluginManager.util.Util;

import java.io.File;
import java.io.FileInputStream;
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
		return new FileInputStream(sourceFilename);
	}
}
