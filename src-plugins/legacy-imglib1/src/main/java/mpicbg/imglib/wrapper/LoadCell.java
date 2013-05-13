package mpicbg.imglib.wrapper;

import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.AbstractCell;

public class LoadCell< A extends ArrayDataAccess< A > > extends AbstractCell< A >
{
	private final A data;

	public LoadCell( final A data, final int[] dimensions, final long[] min, final int entitiesPerPixel )
	{
		super( dimensions, min );
		this.data = data;
	}

	@Override
	public A getData()
	{
		return data;
	}
}