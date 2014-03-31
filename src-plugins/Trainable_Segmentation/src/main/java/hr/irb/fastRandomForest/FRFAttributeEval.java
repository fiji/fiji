package hr.irb.fastRandomForest;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.AttributeEvaluator;
import weka.classifiers.AbstractClassifier;
import weka.core.Capabilities;
import weka.core.Instances;
import weka.core.RevisionUtils;

/**
 * Evaluate the merit of each attribute using a random forest.
 *
 * @author Santi Villalba
 * @version $Id: FRFAttributeEval.java 49 2010-10-05 14:05:11Z vinaysethmohta $
 */
public class FRFAttributeEval extends ASEvaluation implements AttributeEvaluator {

  private static final long serialVersionUID = -4504270948574160991L;

  /** The feature importances. */
  private double[] m_Importances;

  /** The prototype for the rf. */
  private FastRandomForest m_frfProto = new FastRandomForest();

  /** Constructor */
  public FRFAttributeEval() {
  }

  /**
   * Constructor.
   *
   * @param frfProto the prototype for the random forest.
   */
  public FRFAttributeEval(FastRandomForest frfProto) {
    m_frfProto = frfProto;
  }

  /** {@inheritDoc} */
  public void buildEvaluator(Instances data) throws Exception {
    FastRandomForest forest = (FastRandomForest) AbstractClassifier.makeCopy(m_frfProto);
    forest.buildClassifier(data);
    m_Importances = forest.getFeatureImportances();
  }

  /** {@inheritDoc} */
  public double evaluateAttribute(int attribute) throws Exception {
    return m_Importances[attribute];
  }

  /** @return the prototype for the random forest */
  public FastRandomForest getFrfProto() {
    return m_frfProto;
  }

  /** @param frfProto the prototype for the random forest */
  public void setFrfProto(FastRandomForest frfProto) {
    m_frfProto = frfProto;
  }

  @Override
  public Capabilities getCapabilities() {
    return m_frfProto.getCapabilities();
  }

  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Id: FRFAttributeEval.java 49 2010-10-05 14:05:11Z vinaysethmohta $");
  }

//TODO: uncomment after implementing all the optionhandler machinery
//  public static void main(String[] args) {
//    runEvaluator(new FRFAttributeEval(), args);
//  }
}
