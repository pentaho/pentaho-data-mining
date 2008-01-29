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

import java.util.ArrayList;
import java.util.List;
import java.io.File;

import org.pentaho.di.compatibility.*;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.ValueDataUtil;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
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
      if (m_meta.getUpdateIncrementalModel()) {
        if (!Const.isEmpty(m_meta.getSavedModelFileName())) {
          // try and save that sucker...
          try {
            WekaScoringData.saveSerializedModel(m_meta.getModel(),
                                                new File(m_meta.getSavedModelFileName()));
          } catch (Exception ex) {
            throw new KettleException("Problem saving updated model to "
                                      + "file!"); 
          }
        }
      }

      setOutputDone();
      return false;
    }

    // Handle the first row
    if (first) {
      first = false;

      m_data.setOutputRowMeta(getInputRowMeta().clone());
      
      // If we don't have a model, or a file name is set, then load from file
      if (m_meta.getModel() == null || 
          !Const.isEmpty(m_meta.getSerializedModelFileName())) {

        // Check that we have a file to try and load a classifier from
        if (Const.isEmpty(m_meta.getSerializedModelFileName())) {
          throw new KettleException("No filename to load  "
                                    + "model from!!");
        }

        // Check that the specified file exists (on this file system)
        File modelFile = 
          new File(m_meta.getSerializedModelFileName());
        if (!modelFile.exists()) {
          throw new KettleException("Serialized model file does "
                                    + "not exist on disk!");
        }
        
        // Load the classifier and set up the expected input format
        // according to what the classifier has been trained on
        try {
          WekaScoringModel model = WekaScoringData.loadSerializedModel(modelFile);
          m_meta.setModel(model);
        } catch (Exception ex) {
          throw new KettleException("Problem de-serializing model "
                                    + "file!"); 
        }
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
      Object [] outputRow = 
        m_data.generatePrediction(getInputRowMeta(), 
                                  m_data.getOutputRowMeta(),
                                  r, 
                                  m_meta);
      putRow(m_data.getOutputRowMeta(), outputRow);
    } catch (Exception ex) {
      throw new KettleException("Unable to make prediction for "
                                + "row #" + linesRead
                                + " : " + r); 
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