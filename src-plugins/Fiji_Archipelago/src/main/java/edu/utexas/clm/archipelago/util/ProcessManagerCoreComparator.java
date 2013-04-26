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

package edu.utexas.clm.archipelago.util;

import edu.utexas.clm.archipelago.compute.ProcessManager;

import java.util.Comparator;


public class ProcessManagerCoreComparator implements Comparator<ProcessManager>
{
    int threadCount = 128;
    
    public void setThreadCount(final int count)
    {
        threadCount = count;
    }

    public int compare(ProcessManager pm1, ProcessManager pm2) {
        final int t1 = pm1.requestedCores(threadCount);
        final int t2 = pm2.requestedCores(threadCount);
        if (t1 == t2)
        {
            final long id1 = pm1.getID();
            final long id2 = pm2.getID();
            return id1 > id2 ? 1 : id1 == id2 ? 0 : -1;
        }
        else 
        {
            return t1 > t2 ? -1 : 1;
        }
    }
}
