/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    WekaScoringModel.java
 *    Copyright 2007 Pentaho Corporation.  All rights reserved. 
 *
 */

package org.pentaho.di.scoring;

import java.io.Serializable;

import weka.core.Instances;
import weka.core.Instance;
import weka.classifiers.Classifier;
import weka.clusterers.Clusterer;
import weka.core.pmml.PMMLModel;

/**
 * Abstract wrapper class for a Weka model. Provides a
 * unified interface to obtaining predictions. Subclasses (
 * WekaScoringClassifer and WekaScoringClusterer) encapsulate
 * the actual weka models.
 *
 * @author  Mark Hall (mhall{[at]}pentaho.org)
 * @version 1.0
 */
public abstract class WekaScoringModel implements Serializable {

  // The header of the Instances used to build the model
  private Instances m_header;

  /**
   * Creates a new <code>WekaScoringModel</code> instance.
   *
   * @param model the actual Weka model to enacpsulate
   */
  public WekaScoringModel(Object model) {
    if (model instanceof PMMLModel) {
      LogAdapter logger = new LogAdapter();
      ((PMMLModel)model).setLog(logger);
    }
    setModel(model);
  }

  /**
   * Set the Instances header
   *
   * @param header an <code>Instances</code> value
   */
  public void setHeader(Instances header) {
    m_header = header;
  }

  /**
   * Get the header of the Instances that was used
   * build this Weka model
   *
   * @return an <code>Instances</code> value
   */
  public Instances getHeader() {
    return m_header;
  }

  /**
   * Tell the model that this scoring run is finished.
   */
  public void done() {
    // subclasses override if they need to do
    // something here.
  }

  /**
   * Set the weka model
   *
   * @param model the Weka model
   */
  public abstract void setModel(Object model);

  /**
   * Get the weka model
   *
   * @return the Weka model as an object
   */
  public abstract Object getModel();
  
  /**
   * Return a classification. What this represents
   * depends on the implementing sub-class. It could
   * be the index of a class-value, a numeric value or
   * a cluster number for example.
   *
   * @param inst the Instance to be classified (predicted)
   * @return the prediction
   * @exception Exception if an error occurs
   */
  public abstract double classifyInstance(Instance inst) 
    throws Exception;

  /**
   * Return a probability distribution (over classes or clusters).
   *
   * @param inst the Instance to be predicted
   * @return a probability distribution
   * @exception Exception if an error occurs
   */
  public abstract double[] distributionForInstance(Instance inst)
    throws Exception;

  /**
   * Returns true if the encapsulated Weka model is a supervised
   * model (i.e. has been built to predict a single target in the
   * data).
   *
   * @return true if the encapsulated Weka model is a supervised
   * model
   */
  public abstract boolean isSupervisedLearningModel();

  /**
   * Returns true if the encapsulated Weka model can be updated
   * incrementally in an instance by instance fashion.
   *
   * @return true if the encapsulated Weka model is incremental
   * model
   */
  public abstract boolean isUpdateableModel();

  /**
   * Update (if possible) a model with the supplied Instance
   *
   * @param inst the Instance to update the model with
   * @return true if the model was updated
   * @exception Exception if an error occurs
   */
  public abstract boolean update(Instance inst) throws Exception;

  /**
   * Static factory method to create an instance of an
   * appropriate subclass of WekaScoringModel given a
   * Weka model.
   *
   * @param model a Weka model
   * @return an appropriate WekaScoringModel for this type of
   * Weka model
   * @exception Exception if an error occurs
   */
  public static WekaScoringModel createScorer(Object model) 
    throws Exception {
    if (model instanceof Classifier) {
      return new WekaScoringClassifier(model);
    } else if (model instanceof Clusterer) {
      return new WekaScoringClusterer(model);
    }
    return null;
  }
}
