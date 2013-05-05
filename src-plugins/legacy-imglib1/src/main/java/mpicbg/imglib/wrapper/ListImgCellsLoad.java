package mpicbg.imglib.wrapper;

import java.util.ArrayList;

import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.AbstractCells;
import net.imglib2.img.list.ListImg;
import net.imglib2.img.list.ListImgFactory;
import net.imglib2.img.list.ListLocalizingCursor;

public class ListImgCellsLoad< A extends ArrayDataAccess< A > > extends AbstractCells< A, LoadCell< A >, ListImg< LoadCell< A > > >
{
	private final ListImg< LoadCell< A > > cells;

	public ListImgCellsLoad( final ArrayList<A> arrays, final int entitiesPerPixel, final long[] dimensions, final int[] cellDimensions )
	{
		super( entitiesPerPixel, dimensions, cellDimensions );
		cells = new ListImgFactory< LoadCell< A > >().create( numCells, new LoadCell< A >( arrays.get( 0 ), new int[ 1 ], new long[ 1 ], entitiesPerPixel ) );

		final long[] cellGridPosition = new long[ n ];
		final long[] cellMin = new long[ n ];
		final int[] cellDims = new int[ n ];
		final ListLocalizingCursor< LoadCell< A > > cellCursor = cells.localizingCursor();
		int i = 0;
		while ( cellCursor.hasNext() )
		{
			cellCursor.fwd();
			cellCursor.localize( cellGridPosition );
			getCellDimensions( cellGridPosition, cellMin, cellDims );
			cellCursor.set( new LoadCell< A >( arrays.get( i++ ), cellDims, cellMin, entitiesPerPixel ) );
		}
	}

	@Override
	protected ListImg< LoadCell< A >> cells()
	{
		return cells;
	}
}
