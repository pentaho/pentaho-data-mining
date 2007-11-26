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
 *    ArffOutput.java
 *    Copyright 2007 Pentaho Corporation.  All rights reserved. 
 *
 */
 
package org.pentaho.di.arff;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

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

public class ArffOutput extends BaseStep 
  implements StepInterface {

  private ArffOutputMeta m_meta;
  private ArffOutputData m_data;

  /**
   * Creates a new <code>ArffOutput</code> instance.
   *
   * @param stepMeta holds the step's meta data
   * @param stepDataInterface holds the step's temporary data
   * @param copyNr the number assigned to the step
   * @param transMeta meta data for the transformation
   * @param trans a <code>Trans</code> value
   */
  public ArffOutput(StepMeta stepMeta, 
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
  public synchronized boolean processRow(StepMetaInterface smi, 
                                         StepDataInterface sdi) 
    throws KettleException {

    m_meta = (ArffOutputMeta)smi;
    m_data = (ArffOutputData)sdi;

    Object[] r = getRow();

    if (r == null) {
      // no more input

      // need to read csv file and append to header
      m_data.finishOutput(m_meta.getRelationName(), m_meta.getEncoding());

      setOutputDone();
      return false;
    }

    if (first) {
      first = false;
      m_data.setOutputRowMeta(getInputRowMeta().clone());
      
      // If we have no arff meta data, then assume all fields
      // are to be written
      if (m_meta.getOutputFields() == null || 
          m_meta.getOutputFields().length == 0) {
        m_meta.setupArffMeta(getInputRowMeta());
      }

      // Determine the output format
      m_meta.getFields(m_data.getOutputRowMeta(), getStepname(), 
                       null, null, this);
        
      // now check to see if the incoming rows have these
      // fields and set up indexes. (pre-configured step might now have a new
      // incoming stream that is not compatible with its
      // configuration
      int[] outputFieldIndexes = new int[m_meta.getOutputFields().length];
      ArffMeta[] arffMeta = m_meta.getOutputFields();
      for (int i = 0; i < arffMeta.length; i++) {
        if (arffMeta[i] != null) {
          outputFieldIndexes[i] = 
            getInputRowMeta().indexOfValue(arffMeta[i].getFieldName());
          // Do we still have this field coming in??
          if (outputFieldIndexes[i] < 0) {
            throw new KettleException("Field [" 
                                      + arffMeta[i].getFieldName()
                                      + "] couldn't be found in the "
                                      +"input stream!");
          }
        } else {
          // OK, this particular entry had no corresponding arff
          // type
          outputFieldIndexes[i] = -1;
        }
      }
      m_data.setOutputFieldIndexes(outputFieldIndexes, arffMeta);
      
      if (Const.isEmpty(m_meta.getFileName())) {
        throw new KettleException("No file name given to write to!");
      }

      if (Const.isEmpty(m_meta.getRelationName())) {
        throw new KettleException("No relation name given!");
      }

      try {
        m_data.openFiles(m_meta.getFileName());
      } catch (IOException ex) {
        throw new KettleException("Unable to open file(s)...", ex);
      }
    }

    try {
      m_data.writeRow(r, m_meta.getEncoding());
    } catch (IOException ex) {
      throw new KettleException("Problem writing a row to the file...",
                                ex);
    }

    // in case the data is to go further
    putRow(m_data.getOutputRowMeta(), r);
    
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
    m_meta = (ArffOutputMeta)smi;
    m_data = (ArffOutputData)sdi;
		
    if (super.init(smi, sdi)) {
      try {
        m_data.setHasEncoding(!Const.isEmpty(m_meta.getEncoding()));
        if (m_data.getHasEncoding()) {
          if (!Const.isEmpty(m_meta.getNewLine())){
            m_data.setBinaryNewLine(m_meta.getNewLine().
                                    getBytes(m_meta.getEncoding()));
          }
          m_data.setBinarySeparator(",".getBytes(m_meta.getEncoding()));
          m_data.setBinaryMissing("?".getBytes(m_meta.getEncoding()));
          //            m_data.setBinaryEnclosure("'".getBytes(m_meta.getEncoding()));
        } else {
          if (!Const.isEmpty(m_meta.getNewLine())) {
            m_data.setBinaryNewLine(m_meta.getNewLine().getBytes());
          }
          m_data.setBinarySeparator(",".getBytes());
          m_data.setBinaryMissing("?".getBytes());
          //            m_data.setBinaryEnclosure("'".getBytes());
        }        
      } catch (UnsupportedEncodingException ex) {
        logError("Encoding problem: " + ex.toString());
        logError(Const.getStackTracker(ex));
        return false;
      }
      return true;
    }
    return false;
  }

  /**
   * Clean up.
   *
   * @param smi a <code>StepMetaInterface</code> value
   * @param sdi a <code>StepDataInterface</code> value
   */
  public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
    m_meta=(ArffOutputMeta)smi;
    m_data=(ArffOutputData)sdi;
    
    try {
      m_data.closeFiles();
    } catch (IOException ex) {
      logError("Exception trying to close file: " + ex.toString());
      setErrors(1);
    }

    super.dispose(smi, sdi);
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