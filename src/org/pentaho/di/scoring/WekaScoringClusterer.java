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
 *    WekaScoringClusterer.java
 *    Copyright 2007 Pentaho Corporation.  All rights reserved. 
 *
 */

package org.pentaho.di.scoring;

import weka.core.Instance;
import weka.clusterers.Clusterer;
import weka.clusterers.UpdateableClusterer;
import weka.clusterers.DensityBasedClusterer;

/**
 * Subclass of WekaScoringModel that encapsulates a Clusterer.
 *
 * @author  Mark Hall (mhall{[at]}pentaho.org)
 * @version 1.0
 */
class WekaScoringClusterer extends WekaScoringModel {
  
  // The encapsulated clusterer
  private Clusterer m_model;
  
  /**
   * Creates a new <code>WekaScoringClusterer</code> instance.
   *
   * @param model the Clusterer
   */
  public WekaScoringClusterer(Object model) {
    super(model);
  }
  
  /**
   * Set the Clusterer model
   *
   * @param model a Clusterer
   */
  public void setModel(Object model) {
      m_model = (Clusterer)model;
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
   * Return a classification (cluster that the test instance
   * belongs to)
   *
   * @param inst the Instance to be clustered (predicted)
   * @return the cluster number
   * @exception Exception if an error occurs
   */
  public double classifyInstance(Instance inst) throws Exception {
    return (double)m_model.clusterInstance(inst);
  }

  /**
   * Update (if possible) the model with the supplied instance
   *
   * @param inst the Instance to update with
   * @return true if the update was updated successfully
   * @exception Exception if an error occurs
   */
  public boolean update(Instance inst) throws Exception {
    // Only cobweb is updateable at present
    if (isUpdateableModel()) {
      //      System.err.println("In update...");
      ((UpdateableClusterer)m_model).updateClusterer(inst);
      //      System.err.println(m_model);
      return true;
    }
    return false;
  }
  
  /**
   * Return a probability distribution (over clusters).
   *
   * @param inst the Instance to be predicted
   * @return a probability distribution
   * @exception Exception if an error occurs
   */  
  public double[] distributionForInstance(Instance inst)
    throws Exception {
    return m_model.distributionForInstance(inst);
  }

  /**
   * Returns false. Clusterers are unsupervised methods.
   *
   * @return false
   */
  public boolean isSupervisedLearningModel() {
    return false;
  }

  /**
   * Returns false. No clusterers in Weka are 
   * incremental... yet
   *
   * @return false
   */
  public boolean isUpdateableModel() {
    if (m_model instanceof UpdateableClusterer) {
      return true;
    }
    
    return false;
  }

  /**
   * Returns true if the wrapped clusterer can produce
   * cluster membership probability estimates
   *
   * @return true if probability estimates can be produced
   */
  public boolean canProduceProbabilities() {
    if (m_model instanceof DensityBasedClusterer) {
      return true;
    }
    return false;
  }

  /**
   * Returns the number of clusters that the encapsulated
   * Clusterer has learned.
   *
   * @return the number of clusters in the model.
   * @exception Exception if an error occurs
   */
  public int numberOfClusters() throws Exception {
    return m_model.numberOfClusters();
  }

  /**
   * Returns the textual description of the Clusterer's model.
   *
   * @return the Clusterer's model as a String
   */
  public String toString() {
    return m_model.toString();
  }
}
