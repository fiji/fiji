package imglib.mpicbg.imglib.cursor.special;

import imglib.mpicbg.imglib.cursor.special.meta.AlwaysTruePredicate;
import imglib.mpicbg.imglib.cursor.special.meta.Predicate;
import mpicbg.imglib.type.Type;

import java.util.Arrays;

import mpicbg.imglib.cursor.Cursor;

/**
 * A class that emulates cursor behavior and allows constraints
 * to be specified for the value of the current elements.
 */
public class TwinValueRangeCursor< T extends Type<T> & Comparable<T> > extends MetaCursor<T> {
	// the predicates for checking if the channel positions are valid
	Predicate<T> predicate1, predicate2;
	// the cursors to simplify access within class
	Cursor<T> cursor1, cursor2;
	/* Available forwarding modes for the predicates:
	 * And: Both predicates need to be true
	 * Or: One and/or the other has to be true
	 * Xor: One or (exclusive) must be true
	 * None: Both need to be false
	 */
	enum ForwardMode { And, Or, Xor, None }
	// the selected forward mode, defaulting to "Or"
	ForwardMode forwardMode = ForwardMode.Or;
	// indicate if a check for a next element has already be performed
	boolean hasNextChecked = false;
	// the latest types found for valid elements
	T cachedType1 = null, cachedType2 = null;

	/**
	 * Creates a TwinValueRangeCursor without any restrictions in values.
	 * It allows iteration over all values of both cursors.
	 */
	public TwinValueRangeCursor(Cursor<T> cursor1, Cursor<T> cursor2) {
		this( cursor1, cursor2,
				new AlwaysTruePredicate<T>(),
				new AlwaysTruePredicate<T>() );
	}

	/**
	 * Creates a new TwinValueRangeCursor that limits access to the values.
	 * It will only iterate over values that are below the passed threshold.
	 *
	 * @param cursor1 The cursor for channel one
	 * @param cursor2 The cursor for channel two
	 * @param maxThresholdCh1 A maximum value threshold for channel one
	 * @param maxThresholdCh2 A maximum value threshold for channel two
	 */
	@SuppressWarnings("unchecked")
	public TwinValueRangeCursor(Cursor<T> cursor1, Cursor<T> cursor2, Predicate<T> predicate1, Predicate<T> predicate2 ) {
		super( Arrays.asList(cursor1, cursor2) );
		this.cursor1 = cursor1;
		this.cursor2 = cursor2;
		this.predicate1 = predicate1;
		this.predicate2 = predicate2;
	}

	public T getChannel1Type() {
		return cachedType1;
	}

	public T getChannel2Type() {
		return cachedType2;
	}

	/**
	 * Walks to the next valid elements and stores them in member
	 * variables cachedType1 and cachedType2.
	 *
	 * @return true if a next element was found, false otherwise
	 */
	protected boolean walkToNextElement() {
		boolean found = false;
		while( super.hasNext() ) {
			super.fwd();
			boolean ch1Valid = predicate1.evaluate( cursor1.getType() );
			boolean ch2Valid = predicate2.evaluate( cursor2.getType() );

			if (forwardMode == ForwardMode.And) {
				if( ch1Valid && ch2Valid ) {
					found = true;
					break;
				}
			} else if (forwardMode == ForwardMode.Or) {
				if( ch1Valid || ch2Valid ) {
					found = true;
					break;
				}
			} else if (forwardMode == ForwardMode.Xor) {
				if( ch1Valid ^ ch2Valid ) {
					found = true;
					break;
				}
			} else if (forwardMode == ForwardMode.None) {
				if( ! (ch1Valid && ch2Valid) ) {
					found = true;
					break;
				}
			}
		}

		if (found) {
			// save the cursor data
			cachedType1 = cursor1.getType();
			cachedType2 = cursor2.getType();
		} else {
			cachedType1 = null;
			cachedType2 = null;
		}

		return found;
	}

	@Override
	public void fwd() {
		/* if we did not check for a next valid element before
		 * (and thus have no cached types) walk to the next element
		 */
		if ( ! hasNextChecked )
			walkToNextElement();

		/* since we have manually forwarded the cursor, the cached
		 * information is not valid any more
		 */
		hasNextChecked = false;
	}

	@Override
	public boolean hasNext() {
		// did we already check for a next element without doing a fwd()?
		if ( hasNextChecked ) {
			/* It should be enough to check if one type is null as they get
			 * always changed both at once.
			 */
			return ! ( cachedType1 == null );
		}

		// indicate that we will already move the cursors to the next elements
		hasNextChecked = true;

		return walkToNextElement();
	}

	public void reset() {
		super.reset();
		hasNextChecked = false;
	}
}
