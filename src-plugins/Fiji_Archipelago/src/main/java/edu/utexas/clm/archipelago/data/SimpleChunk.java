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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
/**
 *
 * @author Larry Lindsey
 */
public class SimpleChunk<T> extends DataChunk<T> implements Serializable 
{
    private final T t;
    
    public SimpleChunk(final T inT)
    {
        t = inT;
    }
    
    public SimpleChunk(final T inT, DataChunk oldChunk)
    {
        super(oldChunk);
        t = inT;
    }

    @Override
    public T getData() {
        return t;
    }

    public Iterator<DataChunk<T>> iterator() {
        return new ArrayList<DataChunk<T>>().iterator();
    }
    
    public String toString()
    {
        return t.toString();
    }
}
