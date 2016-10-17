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
 *    WekaScoring.java
 *    Copyright 2007 Pentaho Corporation.  All rights reserved. 
 *
 */
 
package org.pentaho.di.scoring;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import weka.core.Instances;

/**
 * Applies a pre-built weka model (classifier or clusterer)
 * to incoming rows and appends predictions. Predictions can
 * be a label (classification/clustering), a number (regression),
 * or a probability distribution over classes/clusters. <p>
 *
 * Attributes that the Weka model was constructed from are
 * automatically mapped to incoming Kettle fields on the basis
 * of name and type. Any attributes that cannot be mapped due
 * to type mismatch or not being present in the incoming fields
 * receive missing values when incoming Kettle rows are converted
 * to Weka's Instance format. Similarly, any values for string
 * fields that have not been seen during the training of the
 * Weka model are converted to missing values.
 *
 * @author Mark Hall (mhall{[at]}pentaho.org)
 * @version 1.0
 */
public class WekaScoring extends BaseStep 
  implements StepInterface {

  private WekaScoringMeta m_meta;
  private WekaScoringData m_data;

  private TransMeta m_transMeta;
  
  // only used when grabbing model file names from the incoming stream
  private int m_indexOfFieldToLoadFrom = -1;
  
  // cache for models that are loaded from files specified in
  // incoming rows
  private Map<String, WekaScoringModel> m_modelCache;  
  
  // model filename from the last row processed (if reading
  // model filenames from a row field
  private String m_lastRowModelFile = "";

  /**
   * Creates a new <code>WekaScoring</code> instance.
   *
   * @param stepMeta holds the step's meta data
   * @param stepDataInterface holds the step's temporary data
   * @param copyNr the number assigned to the step
   * @param transMeta meta data for the transformation
   * @param trans a <code>Trans</code> value
   */
  public WekaScoring(StepMeta stepMeta, 
                         StepDataInterface stepDataInterface, 
                         int copyNr, TransMeta transMeta, Trans trans) {
    super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    m_transMeta = transMeta;
  }
  
  /**
   * Sets the model to use from the path supplied in a
   * user chosen field of the incoming data stream. User
   * may opt to have loaded models cached in memory. User
   * may also opt to supply a default model to be used when
   * there is none specified in the field value.
   * 
   * @param row
   * @throws KettleException
   */
  private void setModelFromField(Object[] row) 
    throws KettleException {
    
    RowMetaInterface inputRowMeta = getInputRowMeta();
    String modelFileName = inputRowMeta.getString(row, m_indexOfFieldToLoadFrom);
    
    if (Const.isEmpty(modelFileName)) {
      // see if there is a default model to use
      WekaScoringModel defaultM = m_meta.getDefaultModel();
      if (defaultM == null) {
        throw new KettleException("No model file name specified in field and no " +
        		"default model to use!");
      }
      logDebug("Using default model.");
      m_meta.setModel(defaultM);
      return;
    }
    
    String resolvedName =  m_transMeta.environmentSubstitute(modelFileName);
    
    if (resolvedName.equals(m_lastRowModelFile)) {
      // nothing to do, just return
      return;
    }
    
    if (m_meta.getCacheLoadedModels()) {
      WekaScoringModel modelToUse = m_modelCache.get(resolvedName);
      if (modelToUse != null) {
        logDebug("Found model in cache. " + 
            modelToUse.getModel().getClass());
        m_meta.setModel(modelToUse);
        m_lastRowModelFile = resolvedName;
        return;
      }
    }
    
    // load the model
    logDebug("Loading model using field value" + 
        m_transMeta.environmentSubstitute(modelFileName));
    WekaScoringModel modelToUse = setModel(modelFileName);

    if (m_meta.getCacheLoadedModels()) {
      m_modelCache.put(resolvedName, modelToUse);
    }
  }
  
  private WekaScoringModel setModel(String modelFileName) throws KettleException {
    String modName = m_transMeta.environmentSubstitute(modelFileName);
    File modelFile = null;
    if (modName.startsWith("file:")) {
      try {
        modName = modName.replace(" ", "%20");
        modelFile = 
          new File(new java.net.URI(modName));
      } catch (Exception ex) {
        throw new KettleException("Malformed URI for model file");
      }
    } else {
      modelFile = new File(modName);
    }
    if (!modelFile.exists()) {
      throw new KettleException("Serialized model file does "
                                + "not exist on disk!");
    }
    
    // Load the model
    WekaScoringModel model = null;
    try {
      model = WekaScoringData.loadSerializedModel(modelFile, getLogChannel());
      m_meta.setModel(model);
      
      if (m_meta.getFileNameFromField()) {
        m_lastRowModelFile = m_transMeta.environmentSubstitute(modelFileName);
      }
    } catch (Exception ex) {
      throw new KettleException("Problem de-serializing model "
                                + "file!"); 
    } 
    return model;
  }

  /**
   * Process an incoming row of data.
   *
   * @param smi a <code>StepMetaInterface</code> value
   * @param sdi a <code>StepDataInterface</code> value
   * @return a <code>boolean</code> value
   * @exception KettleException if an error occurs
   */
  public boolean processRow(StepMetaInterface smi, 
                            StepDataInterface sdi) 
    throws KettleException {

    m_meta = (WekaScoringMeta)smi;
    m_data = (WekaScoringData)sdi;    

    Object[] r = getRow();

    if (r == null) {
      
      // see if we have an incremental model that is to be saved somewhere.
      if (!m_meta.getFileNameFromField() && m_meta.getUpdateIncrementalModel()) {
        if (!Const.isEmpty(m_meta.getSavedModelFileName())) {
          // try and save that sucker...
          try {
            String modName = m_transMeta.environmentSubstitute(m_meta.getSavedModelFileName());
            File updatedModelFile = null;
            if (modName.startsWith("file:")) {
              try {
                modName = modName.replace(" ", "%20");
                updatedModelFile = 
                  new File(new java.net.URI(modName));
              } catch (Exception ex) {
                throw new KettleException("Malformed URI for updated model file");
              }
            } else {
              updatedModelFile = new File(modName);
            }
            WekaScoringData.saveSerializedModel(m_meta.getModel(), updatedModelFile);
          } catch (Exception ex) {
            throw new KettleException("Problem saving updated model to "
                                      + "file!"); 
          }
        }
      }

      if (m_meta.getFileNameFromField()) {
        // clear the main model
        m_meta.setModel(null);
      } else {
        m_meta.getModel().done();
      }

      setOutputDone();
      return false;
    }

    // Handle the first row
    if (first) {
      first = false;

      m_data.setOutputRowMeta(getInputRowMeta().clone());
      if (m_meta.getFileNameFromField()) {
        RowMetaInterface inputRowMeta = getInputRowMeta();
        
        m_indexOfFieldToLoadFrom = 
          inputRowMeta.indexOfValue(m_meta.getFieldNameToLoadModelFrom());
        
        if (m_indexOfFieldToLoadFrom < 0) {
          throw new KettleException("Unable to locate model file field "
              + m_meta.getFieldNameToLoadModelFrom() + " in the incoming stream!");
        }
        
        if (!inputRowMeta.getValueMeta(m_indexOfFieldToLoadFrom).isString()) {
          throw new KettleException("Model file field in incoming stream " +
          		"is not a String field!");
        }
        
        if (m_meta.getCacheLoadedModels()) {
          m_modelCache = new HashMap<String, WekaScoringModel>();
        }
        
        setModelFromField(r);
        logBasic("Sourcing model file names from input field " + 
            m_meta.getFieldNameToLoadModelFrom());
      } else if (m_meta.getModel() == null || 
          !Const.isEmpty(m_meta.getSerializedModelFileName())) {
        // If we don't have a model, or a file name is set, then load from file

        // Check that we have a file to try and load a classifier from
        if (Const.isEmpty(m_meta.getSerializedModelFileName())) {
          throw new KettleException("No filename to load  "
                                    + "model from!!");
        }
        
        setModel(m_meta.getSerializedModelFileName());        
      }

      // Check the input row meta data against the instances
      // header that the classifier was trained with
      try {
        Instances header = m_meta.getModel().getHeader();
        m_meta.mapIncomingRowMetaData(header, getInputRowMeta());
      } catch (Exception ex) {
        throw new KettleException("Incoming data format does not seem to "
                                  + "match what the model was trained with");
      }
      
      // Determine the output format
      m_meta.getFields(m_data.getOutputRowMeta(), getStepname(),
                       null, null, this);
      //      System.err.println("Output Format: \n"
      //                 + m_data.getOutputRowMeta().toStringMeta());
      
    } // end (if first)

    // Make prediction for row using model
    try {
      if (m_meta.getFileNameFromField()) {
        setModelFromField(r);
      }
      Object [] outputRow = 
        m_data.generatePrediction(getInputRowMeta(), 
                                  m_data.getOutputRowMeta(),
                                  r, 
                                  m_meta);
      putRow(m_data.getOutputRowMeta(), outputRow);
    } catch (Exception ex) {
      throw new KettleException("Unable to make prediction for "
                                + "row #" + linesRead
                                + " ( " + ex.getMessage() + ")"); 
    }
    
    if (log.isRowLevel()) { 
      log.logRowlevel(toString(), 
                      "Read row #"+linesRead+" : "+r);
    }
    
    if (checkFeedback(linesRead)) {
      logBasic("Linenr "+linesRead);
    }
    return true;
  }

  /**
   * Initialize the step.
   *
   * @param smi a <code>StepMetaInterface</code> value
   * @param sdi a <code>StepDataInterface</code> value
   * @return a <code>boolean</code> value
   */
  public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
    m_meta=(WekaScoringMeta)smi;
    m_data=(WekaScoringData)sdi;
		
    if (super.init(smi, sdi)) {
      return true;
    }
    return false;
  }
	
  /**
   * Run is where the action happens!
   */
  public void run() {
    logBasic("Starting to run...");
    try {
        while (processRow(m_meta, m_data) && !isStopped());
    } catch(Exception e) {
      logError("Unexpected error : "+e.toString());
      logError(Const.getStackTracker(e));
      setErrors(1);
      stopAll();
    } finally {
      dispose(m_meta, m_data);
      logBasic("Finished, processing "+linesRead+" rows");
      markStop();
    }
  }
}