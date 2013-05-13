package trainableSegmentation.metrics;

/**
*
* License: GPL
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License 2
* as published by the Free Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*
* Authors: Ignacio Arganda-Carreras (iarganda@mit.edu), Verena Kaynig (verena.kaynig@inf.ethz.ch),
*          Albert Cardona (acardona@ini.phys.ethz.ch)
*/

/**
 * This class stores statistics from a classification
 */
public class ClassificationStatistics 
{
	/** number of true positives */
	public double truePositives = 0;
	/** number of true negatives */
	public double trueNegatives = 0;
	/** number of false positives */
	public double falsePositives = 0;
	/** number of false negatives */
	public double falseNegatives = 0;
	
	/** value of the classification metric */
	public double metricValue = 0;
	
	/** precision: true positives / ( true positives + false positives ) */
	public double precision = 0;
	/** recall (also called sensitivity of hit rate): true positives / ( true positives + false negatives ) */
	public double recall = 0;
	/** F-score, harmonic mean of precision and recall */
	public double fScore = 0;
	/** specificity, also called true negative rate (TNR): true negatives / (true negatives + false negatives) */
	public double specificity = 0;
	
	
	/**
	 * Create classification statistics
	 * 
	 * @param truePositives number of true positives
	 * @param trueNegatives number of true negatives
	 * @param falsePositives number of false positives 
	 * @param falseNegatives number of false negatives 
	 * @param metricValue value of the classification metric
	 */
	public ClassificationStatistics(
			double truePositives,
			double trueNegatives,
			double falsePositives,
			double falseNegatives,
			double metricValue)
	{
		this.truePositives = truePositives;
		this.trueNegatives = trueNegatives;
		this.falsePositives = falsePositives;
		this.falseNegatives = falseNegatives;
		this.metricValue = metricValue;

		
		final double totalNegatives = trueNegatives + falsePositives;
		
		this.specificity = (totalNegatives > 0) ? trueNegatives / totalNegatives : 0;
		
		// no false positives involves maximum precision
		if( falsePositives == 0 )
			this.precision = 1;
		else
			this.precision = truePositives / (truePositives + falsePositives);
		
		// no false negatives involves maximum recall
		if( falseNegatives == 0)
			this.recall = 1;
		else
			this.recall = truePositives / (truePositives + falseNegatives);
		
		if( (precision + recall) > 0)
			this.fScore = 2.0 * precision * recall / ( precision + recall );				
	}
}
