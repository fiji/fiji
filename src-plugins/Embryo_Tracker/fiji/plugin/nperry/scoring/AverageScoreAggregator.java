
package fiji.plugin.nperry.scoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import fiji.plugin.nperry.Spot;

public class AverageScoreAggregator implements ScoreAggregator {

	private ArrayList<Scorer> scorers;

	@Override
	public void aggregate(Collection<Spot> spots) {
		for (Scorer scorer : scorers) {
			scorer.score(spots);
			
			// If needed, scale scores to the range 0-1
			if (!scorer.isNormalized()) {
				// Find min - max over the colletion of spots, for this scorer
				double min = Double.POSITIVE_INFINITY;
				double max = Double.NEGATIVE_INFINITY;
				double currentScore;
				for (Spot spot : spots) {
					currentScore = spot.getScores().get(scorer.getName());
					if (min < currentScore)
						min = currentScore;
					if (max > currentScore)
						max = currentScore;
				}
				// Update scores for this scorer
				double scaledScore;
				for (Spot spot : spots) {
					currentScore = spot.getScores().get(scorer.getName());
					scaledScore = (currentScore - min) / (max - min);
					spot.getScores().put(scorer.getName(), scaledScore);
				}
			}
			
		}
		// Average score
		for (Spot spot : spots) {
			double mean = 0.0;
			int counter = 0;
			Iterator<Double> it = spot.getScores().values().iterator();
			while (it.hasNext()) {
				mean += it.next();
				counter++;
			}
			spot.setAggregatedScore(mean / counter);
		}
	}
	
	
	
	/*
	 * COLLECTION METHODS
	 */

	@Override
	public boolean add(Scorer o) {
		return scorers.add(o);
	}

	@Override
	public boolean addAll(Collection<? extends Scorer> c) {
		return scorers.addAll(c);
	}

	@Override
	public void clear() {
		scorers.clear();
	}

	@Override
	public boolean contains(Object o) {
		return scorers.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return scorers.containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return scorers.isEmpty();
	}

	@Override
	public Iterator<Scorer> iterator() {
		return scorers.iterator();
	}

	@Override
	public boolean remove(Object o) {
		return scorers.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return scorers.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return scorers.retainAll(c);
	}

	@Override
	public int size() {
		return scorers.size();
	}

	@Override
	public Object[] toArray() {
		return scorers.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return scorers.toArray(a);
	}

}
