package archipelago.ijsupport;

import archipelago.data.DataChunk;
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
