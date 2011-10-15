package mpicbg.imglib.cursor.special.predicate;

import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.type.logic.BitType;

public class MaskPredicate implements Predicate<BitType> {
	
	public boolean test(final LocalizableCursor<BitType> cursor) {
		return cursor.getType().get();
	}
}