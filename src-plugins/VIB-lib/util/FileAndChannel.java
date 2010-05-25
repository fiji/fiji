/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* An occasionally useful class for referring to a particular
   channel of a particular file.  (Typically for LSM files.) */

package util;

import ij.*;
import ij.io.*;

import java.io.*;

public class FileAndChannel {

        private String filePath;
        private File fileObject;
        private int channelZeroIndexed;
        private String channelString;

        public FileAndChannel(String filePath,int channelZeroIndexed) {
                this.filePath=filePath;
                this.channelZeroIndexed=channelZeroIndexed;
                channelString="Channel : Ch"+(channelZeroIndexed+1);
                fileObject=new File(this.filePath);
        }

	public boolean exists() {
		return fileObject.exists();
	}

	public boolean correctFileName(ImagePlus i) {
                FileInfo info=i.getOriginalFileInfo();
                String id=info.directory;
                if(id.lastIndexOf(File.separatorChar)==(id.length()-1)) {
                        id=id.substring(0,id.length()-1);
                }
                boolean matches=(id.equals(fileObject.getParent())) &&
                        (info.fileName.equals(fileObject.getName()));
                return matches;
        }

        public boolean correctChannel(ImagePlus i) {
                boolean matches=(-1 != i.getTitle().indexOf(channelString));
                return matches;
        }

        public int getChannelZeroIndexed() {
                return channelZeroIndexed;
        }

        public String getPath() {
                return filePath;
        }

}
