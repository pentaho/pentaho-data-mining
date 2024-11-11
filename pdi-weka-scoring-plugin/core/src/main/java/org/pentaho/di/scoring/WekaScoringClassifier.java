/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/


package org.pentaho.di.scoring;

import weka.core.BatchPredictor;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.classifiers.Classifier;
import weka.classifiers.pmml.consumer.PMMLClassifier;
import weka.classifiers.UpdateableClassifier;

/**
 * Subclass of WekaScoringModel that encapsulates a Classifier.
 *
 * @author Mark Hall (mhall{[at]}pentaho.org)
 * @version 1.0
 */
class WekaScoringClassifier extends WekaScoringModel {

  // The encapsulated classifier
  private Classifier m_model;

  /**
   * Creates a new <code>WekaScoringClassifier</code> instance.
   *
   * @param model the Classifier
   */
  public WekaScoringClassifier( Object model ) {
    super( model );
  }

  /**
   * Set the Classifier model
   *
   * @param model a Classifier
   */
  public void setModel( Object model ) {
    m_model = (Classifier) model;
  }

  /**
   * Get the weka model
   *
   * @return the Weka model as an object
   */
  public Object getModel() {
    return m_model;
  }

  /**
   * Return a classification (number for regression problems
   * or index of a class value for classification problems).
   *
   * @param inst the Instance to be classified (predicted)
   * @return the prediction (either a number for regression or
   * the index of a class-value for classification) as a double
   * @throws Exception if an error occurs
   */
  public double classifyInstance( Instance inst ) throws Exception {
    return m_model.classifyInstance( inst );
  }

  /**
   * Update (if possible) the model with the supplied instance
   *
   * @param inst the Instance to update with
   * @return true if the update was updated successfully
   * @throws Exception if an error occurs
   */
  public boolean update( Instance inst ) throws Exception {
    if ( isUpdateableModel() ) {
      //      System.err.println("In update...");
      ( (UpdateableClassifier) m_model ).updateClassifier( inst );
      //      System.err.println(m_model);
      return true;
    }
    return false;
  }

  /**
   * Return a probability distribution (over classes).
   *
   * @param inst the Instance to be predicted
   * @return a probability distribution
   * @throws Exception if an error occurs
   */
  public double[] distributionForInstance( Instance inst ) throws Exception {
    return m_model.distributionForInstance( inst );
  }

  /**
   * Returns true. Classifiers are supervised methods.
   *
   * @return true
   */
  public boolean isSupervisedLearningModel() {
    return true;
  }

  /**
   * Returns true if the classifier can be updated
   * incrementally
   *
   * @return true if the classifier can be updated incrementally
   */
  public boolean isUpdateableModel() {
    if ( m_model instanceof UpdateableClassifier ) {
      return true;
    }

    return false;
  }

  /**
   * If the model is a PMMLClassifier, tell it that
   * the scoring run has finished.
   */
  public void done() {
    if ( m_model instanceof PMMLClassifier ) {
      ( (PMMLClassifier) m_model ).done();
    }
  }

  /**
   * Returns the textual description of the Classifier's model.
   *
   * @return the Classifier's model as a String
   */
  public String toString() {
    return m_model.toString();
  }

  /**
   * Batch scoring method.
   *
   * @param insts the instances to score
   * @return an array of predictions (index of the predicted class label for
   * each instance)
   * @throws Exception if a problem occurs
   */
  public double[] classifyInstances( Instances insts ) throws Exception {
    double[][] preds = distributionsForInstances( insts );

    double[] result = new double[preds.length];
    for ( int i = 0; i < preds.length; i++ ) {
      double[] p = preds[i];

      if ( Utils.sum( p ) <= 0 ) {
        result[i] = Utils.missingValue();
      } else {
        result[i] = Utils.maxIndex( p );
      }
    }

    return result;
  }

  /**
   * Batch scoring method
   *
   * @param insts the instances to get predictions for
   * @return an array of probability distributions, one for each instance
   * @throws Exception if a problem occurs
   */
  public double[][] distributionsForInstances( Instances insts ) throws Exception {
    if ( !isBatchPredictor() ) {
      throw new Exception( "Weka model cannot produce batch predictions!" );
    }

    return ( (BatchPredictor) m_model ).distributionsForInstances( insts );
  }

  /**
   * Returns true if the encapsulated Weka model can produce
   * predictions in a batch.
   *
   * @return true if the encapsulated Weka model can produce
   * predictions in a batch
   */
  public boolean isBatchPredictor() {
    return ( m_model instanceof BatchPredictor && ( ( ( (BatchPredictor) m_model )
        .implementsMoreEfficientBatchPrediction() ) ) );
  }
}
