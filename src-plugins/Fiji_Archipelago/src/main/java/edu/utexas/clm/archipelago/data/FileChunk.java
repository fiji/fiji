/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * 
 * @author Larry Lindsey llindsey@clm.utexas.edu
 */

package edu.utexas.clm.archipelago.data;


import edu.utexas.clm.archipelago.FijiArchipelago;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
/**
 *
 * @author Larry Lindsey
 */
public class FileChunk extends DataChunk<String>
{
    private static final long serialVersionUID = -8641288090205214129L;

    private final String fileName;

    public FileChunk(String path)
    {
        super();
        File f = new File(path);
        fileName = FijiArchipelago.truncateFileRoot(f.getAbsolutePath());
    }
    
    public FileChunk(String path, DataChunk oldChunk)
    {
        super(oldChunk);
        File f = new File(path);
        fileName = FijiArchipelago.truncateFileRoot(f.getAbsolutePath());
    }
    
    public String getData()
    {
        return FijiArchipelago.getFileRoot() + fileName;
    }

    public Iterator<DataChunk<String>> iterator()
    {
        // Return a correctly-typed iterator with nothing in it.
        return new ArrayList<DataChunk<String>>().iterator();
    }
    
    public String toString()
    {
        return getData();
    }
}
