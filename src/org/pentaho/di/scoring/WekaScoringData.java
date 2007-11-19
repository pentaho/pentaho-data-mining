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
 *    WekaScoringData.java
 *    Copyright 2007 Pentaho Corporation.  All rights reserved. 
 *
 */

package org.pentaho.di.scoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.io.*;
import java.util.zip.GZIPInputStream;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.RowDataUtil; 

import weka.core.Instances;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Utils;
import weka.classifiers.Classifier;

/**
 * Holds temporary data and has routines for loading
 * serialized models.
 *
 * @author Mark Hall (mhall{[at]}pentaho.org)
 * @version 1.0
 */
public class WekaScoringData extends BaseStepData 
  implements StepDataInterface {

  // some constants for various input field - attribute match/type
  // problems
  public static final int NO_MATCH = -1;
  public static final int TYPE_MISMATCH = -2;

  // this class contains intermediate results,
  // info about the input format, derived output
  // format etc.
  
  // the output data format
  protected RowMetaInterface m_outputRowMeta;

  // holds values for instances constructed for prediction
  private double[] m_vals = null;
  
  public WekaScoringData() {
    super();
  }

  /**
   * Get the meta data for the output format
   *
   * @return a <code>RowMetaInterface</code> value
   */
  public RowMetaInterface getOutputRowMeta() {
    return m_outputRowMeta;
  }

  /**
   * Set the meta data for the output format
   *
   * @param rmi a <code>RowMetaInterface</code> value
   */
  public void setOutputRowMeta(RowMetaInterface rmi) {
    m_outputRowMeta = rmi;
  }

  /**
   * Loads a serialized model.
   *
   * @param modelFile a <code>File</code> value
   * @return the model
   */
  public static WekaScoringModel loadSerializedModel(File modelFile) 
    throws Exception {
    InputStream is = new FileInputStream(modelFile);
    if (modelFile.getName().endsWith(".gz")) {
      is = new GZIPInputStream(is);      
    }
    ObjectInputStream oi =
      new ObjectInputStream(new BufferedInputStream(is));
    
    Object model = oi.readObject();

    // if the model has been saved using the command line
    // rather than the Explorer then this will fail (as
    // the Evaluation class does not save the header). In
    // this case we can't do much anyway, as we need the
    // header for compatibility checking
    Instances header = (Instances)oi.readObject();
    oi.close();

    WekaScoringModel wsm = WekaScoringModel.createScorer(model);
    wsm.setHeader(header);
    return wsm;
  }

  /**
   * Finds a mapping between the attributes that a Weka
   * model has been trained with and the incoming Kettle
   * row format. Returns an array of indices, where the
   * element at index 0 of the array is the index of the 
   * Kettle field that corresponds to the first attribute
   * in the Instances structure, the element at index 1
   * is the index of the Kettle fields that corresponds to
   * the second attribute, ...
   *
   * @param header the Instances header
   * @param inputRowMeta the meta data for the incoming rows
   * @return the mapping as an array of integer indices
   */
  public static int [] findMappings(Instances header,
                                    RowMetaInterface inputRowMeta) {
    //    Instances header = m_model.getHeader();
    int[] mappingIndexes = new int[header.numAttributes()];
    
    HashMap<String, Integer> inputFieldLookup = 
      new HashMap<String, Integer>();
    for (int i = 0; i < inputRowMeta.size(); i++) {
      ValueMetaInterface inField = inputRowMeta.getValueMeta(i);
      inputFieldLookup.put(inField.getName(), Integer.valueOf(i));
    }

    // check each attribute in the header against what is incoming
    for (int i = 0; i < header.numAttributes(); i++) {
      Attribute temp = header.attribute(i);
      String attName = temp.name();
      
      // look for a matching name
      Integer matchIndex = inputFieldLookup.get(attName);
      boolean ok = false;
      int status = NO_MATCH;
      if (matchIndex != null) {
        // check for type compatibility
        ValueMetaInterface tempField = 
          inputRowMeta.getValueMeta(matchIndex.intValue());
        if (tempField.isNumeric() || tempField.isBoolean()) {
          if (temp.isNumeric()) {
            ok = true;
            status = 0;
          } else {
            status = TYPE_MISMATCH;
          }
        } else if (tempField.isString()) {
          if (temp.isNominal()) {
            ok = true;
            status = 0;
            // All we can assume is that this input field is ok.
            // Since we wont know what the possible values are
            // until the data is pumping throug, we will defer
            // the matching of legal values until then
          } else {
            status = TYPE_MISMATCH;
          }
        } else {
          // any other type is a mismatch (might be able to do
          // something with dates at some stage)
          status = TYPE_MISMATCH;
        }
      }
      if (ok) {
        mappingIndexes[i] = matchIndex.intValue();
      } else {
        // mark this attribute as missing or type mismatch
        mappingIndexes[i] = status;
      }
    }
    return mappingIndexes;
  }

  /**
   * Generates a prediction (more specifically, an output row
   * containing all input Kettle fields plus new fields
   * that hold the prediction(s)) for an incoming Kettle
   * row given a Weka model.
   *
   * @param inputMeta the meta data for the incoming rows
   * @param outputMeta the meta data for the output rows
   * @param inputRow the values of the incoming row
   * @param meta meta data for this step
   * @return a Kettle row containing all incoming fields
   * along with new ones that hold the prediction(s)
   * @exception Exception if an error occurs
   */
  public Object[] generatePrediction(RowMetaInterface inputMeta,
                                      RowMetaInterface outputMeta,
                                      Object[] inputRow,
                                      WekaScoringMeta meta) 
    throws Exception {
    
    int [] mappingIndexes = meta.getMappingIndexes();
    WekaScoringModel model = meta.getModel();
    boolean outputProbs = meta.getOutputProbabilities();
    boolean supervised = model.isSupervisedLearningModel();

    Attribute classAtt = null;
    if (supervised) {
      classAtt = model.getHeader().classAttribute();
    }
    
    // need to construct an Instance to represent this
    // input row
    Instance toScore = constructInstance(inputMeta, inputRow, 
                                         mappingIndexes, model);
    double[] prediction = model.distributionForInstance(toScore);
    // First copy the input data to the new result...                                                                                                 
    Object[] resultRow = 
      RowDataUtil.resizeArray(inputRow, outputMeta.size()); 
    int index = inputMeta.size();

    // output for numeric class or discrete class value
    if (prediction.length == 1 || !outputProbs) {
      if (supervised) {
        if (classAtt.isNumeric()) {
          Double newVal = new Double(prediction[0]);
          resultRow[index++] = newVal;
        } else {
          int maxProb = Utils.maxIndex(prediction);
          if (prediction[maxProb] > 0) {
            String newVal = classAtt.value(maxProb);
            resultRow[index++] = newVal;
          } else {
            String newVal = "Unable to predict";
            resultRow[index++] = newVal;
          }
        }
      } else {
        int maxProb = Utils.maxIndex(prediction);
        if (prediction[maxProb] > 0) {
          Double newVal = new Double(maxProb);
          resultRow[index++] = newVal;
        } else {
          String newVal = "Unable to assign cluster";
          resultRow[index++] = newVal;
        }
      }
    } else {
      // output probability distribution
      for (int i = 0; i < prediction.length;i++) {
        Double newVal = new Double(prediction[i]);
        resultRow[index++] = newVal;
      }
    } 

    //    resultRow[index] = " ";
    
    return resultRow;
  }

  /**
   * Helper method that constructs an Instance to input
   * to the Weka model based on incoming Kettle fields
   * and pre-constructed attribute-to-field mapping data.
   *
   * @param inputMeta a <code>RowMetaInterface</code> value
   * @param inputRow an <code>Object</code> value
   * @param mappingIndexes an <code>int</code> value
   * @param model a <code>WekaScoringModel</code> value
   * @return an <code>Instance</code> value
   */
  private Instance constructInstance(RowMetaInterface inputMeta,
                                     Object[] inputRow,
                                     int[] mappingIndexes,
                                     WekaScoringModel model) {
    
    Instances header = model.getHeader();

    // Re-use this array to avoid an object creation
    if (m_vals == null /*|| m_vals.length != header.numAttributes()*/) {
      m_vals = new double[header.numAttributes()];
    }

    for (int i = 0; i < header.numAttributes(); i++) {

      if (mappingIndexes[i] >= 0) {
        try {
          Object inputVal = inputRow[mappingIndexes[i]];

          Attribute temp = header.attribute(i);
          //        String attName = temp.name();
          ValueMetaInterface tempField = 
            inputMeta.getValueMeta(mappingIndexes[i]);
          int fieldType = tempField.getType();

          //          if (inputVal == null) {

          // Check for missing value (null or empty string)
          if (tempField.isNull(inputVal)) {
            m_vals[i] = Instance.missingValue();
            continue;
          }
          
          switch(temp.type()) {
          case Attribute.NUMERIC:
            {
              if (fieldType == ValueMetaInterface.TYPE_BOOLEAN) {
                Boolean b = tempField.getBoolean(inputVal);
                if (b.booleanValue()) {
                  m_vals[i] = 1.0;
                } else {
                  m_vals[i] = 0.0;
                }                
              } else if (fieldType == ValueMetaInterface.TYPE_INTEGER) {
                Long t = tempField.getInteger(inputVal);
                m_vals[i] = (double)t.longValue();
              } else {
                Double n = tempField.getNumber(inputVal);
                m_vals[i] = n.doubleValue();
              }
            }
            break;
          case Attribute.NOMINAL:
            {
              String s = tempField.getString(inputVal);
              // now need to look for this value in the attribute
              // in order to get the correct index
              int index = temp.indexOfValue(s);
              if (index < 0) {
                // set to missing value
                m_vals[i] = Instance.missingValue();
              } else {
                m_vals[i] = (double)index;
              }
            }
            break;
          default:
            //            System.err.println("Missing - default " + i);
            m_vals[i] = Instance.missingValue();
          }
        } catch (Exception e) {
          //          System.err.println("Exception - missing " + i);
          m_vals[i] = Instance.missingValue();
        }
      } else {
        // set to missing value
        //        System.err.println("Unmapped " + i);
        m_vals[i] = Instance.missingValue();
      }

        //      m_vals[i] = Instance.missingValue();
    }

    /*    for (int i = 0; i < header.numAttributes(); i++) {
      if (mappingIndexes[i] >= 0) {
        Object inputVal = inputRow[mappingIndexes[i]];
        if (inputVal == null) {
          // set missing
          m_vals[i] = Instance.missingValue();
          continue;
        }
        Attribute temp = header.attribute(i);
        //        String attName = temp.name();
        ValueMetaInterface tempField = 
          inputMeta.getValueMeta(mappingIndexes[i]);

        // Quick check for type mismatch 
        // (i.e. string occuring in what was thought to be
        // a numeric incoming field
        if (temp.isNumeric()) {
          if (!tempField.isBoolean() && !tempField.isNumeric()) {
            m_vals[i] = Instance.missingValue();
            continue;
          }
        } else {
          if (!tempField.isString()) {
            m_vals[i] = Instance.missingValue();
            continue;
          }
        }

        int fieldType = tempField.getType();

        try {
          switch(fieldType) {
          case ValueMetaInterface.TYPE_BOOLEAN:
            {
              Boolean b = tempField.getBoolean(inputVal);
              if (b.booleanValue()) {
                m_vals[i] = 1.0;
              } else {
                m_vals[i] = 0.0;
              }
            } 
            break;

          case ValueMetaInterface.TYPE_NUMBER:
          case ValueMetaInterface.TYPE_INTEGER:
            {
              Number n = tempField.getNumber(inputVal);
              m_vals[i] = n.doubleValue();
            }
            break;

          case ValueMetaInterface.TYPE_STRING:
            {
              String s = tempField.getString(inputVal);
              // now need to look for this value in the attribute
              // in order to get the correct index
              int index = temp.indexOfValue(s);
              if (index < 0) {
                // set to missing value
                m_vals[i] = Instance.missingValue();
              } else {
                m_vals[i] = (double)index;
              }
            }
            break;

          default:
            // for unsupported type set to missing value
            m_vals[i] = Instance.missingValue();
            break;
          }
        } catch (Exception ex) {
          // quietly ignore -- set to missing anything that
          // is not parseable as the expected type
          m_vals[i] = Instance.missingValue();
        }
      } else {
        // set to missing value
        m_vals[i] = Instance.missingValue();
      }      
      } */
    Instance newInst = new Instance(1.0, m_vals);
    newInst.setDataset(header);
    return newInst;
  }
}
