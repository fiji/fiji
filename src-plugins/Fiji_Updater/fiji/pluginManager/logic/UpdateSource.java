package fiji.pluginManager.logic;

import fiji.pluginManager.logic.FileUploader.SourceFile;

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

	public UpdateSource(String source, String target, String permissions) {
		this.permissions = permissions;
		this.sourceFilename = source;
		this.filename = target;
		filesize = new File(source).length();
	}

	public UpdateSource(String source, PluginObject plugin, String permissions) {
		this(source, plugin.getFilename() + "-" + plugin.getTimestamp(), permissions);
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
