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

package edu.utexas.clm.archipelago.ijsupport;

import edu.utexas.clm.archipelago.data.DataChunk;
import ij.process.FloatProcessor;

import java.io.IOException;
import java.io.ObjectInputStream;

public class FloatProcessorChunk extends DataChunk<FloatProcessor>
{

    private transient FloatProcessor processor;
    private final float[] pixels;
    private final int width, height;


    public FloatProcessorChunk(FloatProcessor fp)
    {
        processor = fp;
        width = processor.getWidth();
        height = processor.getHeight();
        pixels = (float[])processor.getPixels();
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException
    {
        ois.defaultReadObject();
        processor = new FloatProcessor(width, height, pixels, null);
    }

    @Override
    public FloatProcessor getData()
    {
        return processor;
    }

}
