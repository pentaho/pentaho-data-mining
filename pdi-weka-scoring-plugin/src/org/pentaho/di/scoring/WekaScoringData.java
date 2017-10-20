/*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU General Public License, version 2 as published by the Free Software
* Foundation.
*
* You should have received a copy of the GNU General Public License along with this
* program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html
* or from the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*
* This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
*
* Copyright 2006 - 2017 Hitachi Vantara.  All rights reserved.
*/

package org.pentaho.di.scoring;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.vfs2.FileObject;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.pmml.PMMLFactory;
import weka.core.pmml.PMMLModel;
import weka.core.xml.XStream;

/**
 * Holds temporary data and has routines for loading serialized models.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}org)
 */
public class WekaScoringData extends BaseStepData implements StepDataInterface {

  /**
   * some constants for various input field - attribute match/type problems
   */
  public static final int NO_MATCH = -1;
  public static final int TYPE_MISMATCH = -2;

  /**
   * the output data format
   */
  protected RowMetaInterface m_outputRowMeta;

  /**
   * holds values for instances constructed for prediction
   */
  private double[] m_vals = null;

  /**
   * Holds the actual Weka model (classifier, clusterer or PMML) used by this
   * copy of the step
   */
  protected WekaScoringModel m_model;

  /**
   * Holds a default model - only used when model files are sourced from a field
   * in the incoming data rows. In this case, it is the fallback model if there
   * is no model file specified in the incoming row.
   */
  protected WekaScoringModel m_defaultModel;

  /**
   * used to map attribute indices to incoming field indices
   */
  private int[] m_mappingIndexes;

  /**
   * whether to update the model (if incremental)
   */
  protected boolean m_updateIncrementalModel = false;

  public WekaScoringData() {
    super();
  }

  /**
   * Set the model for this copy of the step to use
   *
   * @param model the model to use
   */
  public void setModel( WekaScoringModel model ) {
    m_model = model;
  }

  /**
   * Get the model that this copy of the step is using
   *
   * @return the model that this copy of the step is using
   */
  public WekaScoringModel getModel() {
    return m_model;
  }

  /**
   * Set the default model for this copy of the step to use. This gets used if
   * we are getting model file paths from a field in the incoming row structure
   * and a given row has null for the model path.
   *
   * @param model the model to use as fallback
   */
  public void setDefaultModel( WekaScoringModel model ) {
    m_defaultModel = model;
  }

  /**
   * Get the default model for this copy of the step to use. This gets used if
   * we are getting model file paths from a field in the incoming row structure
   * and a given row has null for the model path.
   *
   * @return the model to use as fallback
   */
  public WekaScoringModel getDefaultModel() {
    return m_defaultModel;
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
  public void setOutputRowMeta( RowMetaInterface rmi ) {
    m_outputRowMeta = rmi;
  }

  /**
   * Finds a mapping between the attributes that a Weka model has been trained
   * with and the incoming Kettle row format. Returns an array of indices, where
   * the element at index 0 of the array is the index of the Kettle field that
   * corresponds to the first attribute in the Instances structure, the element
   * at index 1 is the index of the Kettle fields that corresponds to the second
   * attribute, ...
   *
   * @param header                 the Instances header
   * @param inputRowMeta           the meta data for the incoming rows
   * @param updateIncrementalModel true if the model is incremental and should
   *                               be updated on the incoming instances
   * @param log                    the log to use
   */
  public void mapIncomingRowMetaData( Instances header, RowMetaInterface inputRowMeta, boolean updateIncrementalModel,
      LogChannelInterface log ) {
    m_mappingIndexes = WekaScoringData.findMappings( header, inputRowMeta );
    m_updateIncrementalModel = updateIncrementalModel;

    // If updating of incremental models has been selected, then
    // check on the ability to do this
    if ( m_updateIncrementalModel && m_model.isSupervisedLearningModel() ) {
      if ( m_model.isUpdateableModel() ) {
        // Do we have the class mapped successfully to an incoming
        // Kettle field
        if ( m_mappingIndexes[header.classIndex()] == WekaScoringData.NO_MATCH
            || m_mappingIndexes[header.classIndex()] == WekaScoringData.TYPE_MISMATCH ) {
          m_updateIncrementalModel = false;
          log.logError(
              BaseMessages.getString( WekaScoringMeta.PKG, "WekaScoringMeta.Log.NoMatchForClass" ) ); //$NON-NLS-1$
        }
      } else {
        m_updateIncrementalModel = false;
        log.logError(
            BaseMessages.getString( WekaScoringMeta.PKG, "WekaScoringMeta.Log.ModelNotUpdateable" ) ); //$NON-NLS-1$
      }
    }
  }

  public static boolean modelFileExists( String modelFile, VariableSpace space ) throws Exception {

    modelFile = space.environmentSubstitute( modelFile );
    FileObject modelF = KettleVFS.getFileObject( modelFile );

    return modelF.exists();
  }

  /**
   * Loads a serialized model. Models can either be binary serialized Java
   * objects, objects deep-serialized to xml, or PMML.
   *
   * @param modelFile a <code>File</code> value
   * @return the model
   * @throws Exception if there is a problem laoding the model.
   */
  public static WekaScoringModel loadSerializedModel( String modelFile, LogChannelInterface log, VariableSpace space )
      throws Exception {

    Object model = null;
    Instances header = null;
    int[] ignoredAttsForClustering = null;

    modelFile = space.environmentSubstitute( modelFile );
    FileObject modelF = KettleVFS.getFileObject( modelFile );
    if ( !modelF.exists() ) {
      throw new Exception( BaseMessages.getString( WekaScoringMeta.PKG, "WekaScoring.Error.NonExistentModelFile",
          space.environmentSubstitute( modelFile ) ) ); //$NON-NLS-1$
    }

    InputStream is = KettleVFS.getInputStream( modelF );
    BufferedInputStream buff = new BufferedInputStream( is );

    if ( modelFile.toLowerCase().endsWith( ".xml" ) ) { //$NON-NLS-1$
      // assume it is PMML
      model = PMMLFactory.getPMMLModel( buff, null );

      // we will use the mining schema as the instance structure
      header = ( (PMMLModel) model ).getMiningSchema().getMiningSchemaAsInstances();

      buff.close();
    } else if ( modelFile.toLowerCase().endsWith( ".xstreammodel" ) ) { //$NON-NLS-1$
      log.logBasic( BaseMessages.getString( WekaScoringMeta.PKG, "WekaScoringData.Log.LoadXMLModel" ) ); //$NON-NLS-1$

      if ( XStream.isPresent() ) {
        Vector v = (Vector) XStream.read( buff );

        model = v.elementAt( 0 );
        if ( v.size() == 2 ) {
          // try and grab the header
          header = (Instances) v.elementAt( 1 );
        }
        buff.close();
      } else {
        buff.close();
        throw new Exception(
            BaseMessages.getString( WekaScoringMeta.PKG, "WekaScoringData.Error.CantLoadXMLModel" ) ); //$NON-NLS-1$
      }
    } else {
      InputStream stream = buff;
      if ( modelFile.toLowerCase().endsWith( ".gz" ) ) { //$NON-NLS-1$
        stream = new GZIPInputStream( buff );
      }
      ObjectInputStream oi = new ObjectInputStream( stream );

      model = oi.readObject();

      // try and grab the header
      header = (Instances) oi.readObject();

      if ( model instanceof weka.clusterers.Clusterer ) {
        // try and grab any attributes to be ignored during clustering
        try {
          ignoredAttsForClustering = (int[]) oi.readObject();
        } catch ( Exception ex ) {
          // Don't moan if there aren't any :-)
        }
      }
      oi.close();
    }

    WekaScoringModel wsm = WekaScoringModel.createScorer( model );
    wsm.setHeader( header );
    if ( wsm instanceof WekaScoringClusterer && ignoredAttsForClustering != null ) {
      ( (WekaScoringClusterer) wsm ).setAttributesToIgnore( ignoredAttsForClustering );
    }

    wsm.setLog( log );
    return wsm;
  }

  public static void saveSerializedModel( WekaScoringModel wsm, File saveTo ) throws Exception {

    Object model = wsm.getModel();
    Instances header = wsm.getHeader();
    OutputStream os = new FileOutputStream( saveTo );

    if ( saveTo.getName().toLowerCase().endsWith( ".gz" ) ) { //$NON-NLS-1$
      os = new GZIPOutputStream( os );
    }
    ObjectOutputStream oos = new ObjectOutputStream( new BufferedOutputStream( os ) );

    oos.writeObject( model );
    oos.writeObject( header );
    oos.close();
  }

  /**
   * Finds a mapping between the attributes that a Weka model has been trained
   * with and the incoming Kettle row format. Returns an array of indices, where
   * the element at index 0 of the array is the index of the Kettle field that
   * corresponds to the first attribute in the Instances structure, the element
   * at index 1 is the index of the Kettle fields that corresponds to the second
   * attribute, ...
   *
   * @param header       the Instances header
   * @param inputRowMeta the meta data for the incoming rows
   * @return the mapping as an array of integer indices
   */
  public static int[] findMappings( Instances header, RowMetaInterface inputRowMeta ) {
    // Instances header = m_model.getHeader();
    int[] mappingIndexes = new int[header.numAttributes()];

    HashMap<String, Integer> inputFieldLookup = new HashMap<String, Integer>();
    for ( int i = 0; i < inputRowMeta.size(); i++ ) {
      ValueMetaInterface inField = inputRowMeta.getValueMeta( i );
      inputFieldLookup.put( inField.getName(), Integer.valueOf( i ) );
    }

    // check each attribute in the header against what is incoming
    for ( int i = 0; i < header.numAttributes(); i++ ) {
      Attribute temp = header.attribute( i );
      String attName = temp.name();

      // look for a matching name
      Integer matchIndex = inputFieldLookup.get( attName );
      boolean ok = false;
      int status = NO_MATCH;
      if ( matchIndex != null ) {
        // check for type compatibility
        ValueMetaInterface tempField = inputRowMeta.getValueMeta( matchIndex.intValue() );
        if ( tempField.isNumeric() || tempField.isBoolean() ) {
          if ( temp.isNumeric() ) {
            ok = true;
            status = 0;
          } else {
            status = TYPE_MISMATCH;
          }
        } else if ( tempField.isString() ) {
          if ( temp.isNominal() || temp.isString() ) {
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
      if ( ok ) {
        mappingIndexes[i] = matchIndex.intValue();
      } else {
        // mark this attribute as missing or type mismatch
        mappingIndexes[i] = status;
      }
    }
    return mappingIndexes;
  }

  /**
   * Generates a batch of predictions (more specifically, an array of output
   * rows containing all input Kettle fields plus new fields that hold the
   * prediction(s)) for each incoming Kettle row given a Weka model.
   *
   * @param inputMeta  the meta data for the incoming rows
   * @param outputMeta the meta data for the output rows
   * @param inputRows  the values of the incoming row
   * @param meta       meta data for this step
   * @return a Kettle row containing all incoming fields along with new ones
   * that hold the prediction(s)
   * @throws Exception if an error occurs
   */
  public Object[][] generatePredictions( RowMetaInterface inputMeta, RowMetaInterface outputMeta,
      List<Object[]> inputRows, WekaScoringMeta meta ) throws Exception {

    int[] mappingIndexes = m_mappingIndexes;
    WekaScoringModel model = getModel(); // copy of the model for this copy of
    // the step
    boolean outputProbs = meta.getOutputProbabilities();
    boolean supervised = model.isSupervisedLearningModel();

    Attribute classAtt = null;
    if ( supervised ) {
      classAtt = model.getHeader().classAttribute();
    }

    Instances batch = new Instances( model.getHeader(), inputRows.size() );
    for ( Object[] r : inputRows ) {
      Instance inst = constructInstance( inputMeta, r, mappingIndexes, model, true );
      batch.add( inst );
    }

    double[][] preds = model.distributionsForInstances( batch );

    Object[][] result = new Object[preds.length][];
    for ( int i = 0; i < preds.length; i++ ) {
      // First copy the input data to the new result...
      Object[] resultRow = RowDataUtil.resizeArray( inputRows.get( i ), outputMeta.size() );
      int index = inputMeta.size();

      double[] prediction = preds[i];

      if ( prediction.length == 1 || !outputProbs ) {
        if ( supervised ) {
          if ( classAtt.isNumeric() ) {
            Double newVal = new Double( prediction[0] );
            resultRow[index++] = newVal;
          } else {
            int maxProb = Utils.maxIndex( prediction );
            if ( prediction[maxProb] > 0 ) {
              String newVal = classAtt.value( maxProb );
              resultRow[index++] = newVal;
            } else {
              String
                  newVal =
                  BaseMessages
                      .getString( WekaScoringMeta.PKG, "WekaScoringData.Message.UnableToPredict" ); //$NON-NLS-1$
              resultRow[index++] = newVal;
            }
          }
        } else {
          int maxProb = Utils.maxIndex( prediction );
          if ( prediction[maxProb] > 0 ) {
            Double newVal = new Double( maxProb );
            resultRow[index++] = newVal;
          } else {
            String
                newVal =
                BaseMessages
                    .getString( WekaScoringMeta.PKG, "WekaScoringData.Message.UnableToPredictCluster" ); //$NON-NLS-1$
            resultRow[index++] = newVal;
          }
        }
      } else {
        // output probability distribution
        for ( int j = 0; j < prediction.length; j++ ) {
          Double newVal = new Double( prediction[j] );
          resultRow[index++] = newVal;
        }
      }

      result[i] = resultRow;
    }

    return result;
  }

  /**
   * Generates a prediction (more specifically, an output row containing all
   * input Kettle fields plus new fields that hold the prediction(s)) for an
   * incoming Kettle row given a Weka model.
   *
   * @param inputMeta  the meta data for the incoming rows
   * @param outputMeta the meta data for the output rows
   * @param inputRow   the values of the incoming row
   * @param meta       meta data for this step
   * @return a Kettle row containing all incoming fields along with new ones
   * that hold the prediction(s)
   * @throws Exception if an error occurs
   */
  public Object[] generatePrediction( RowMetaInterface inputMeta, RowMetaInterface outputMeta, Object[] inputRow,
      WekaScoringMeta meta ) throws Exception {

    int[] mappingIndexes = m_mappingIndexes;
    WekaScoringModel model = getModel();
    boolean outputProbs = meta.getOutputProbabilities();
    boolean supervised = model.isSupervisedLearningModel();

    Attribute classAtt = null;
    if ( supervised ) {
      classAtt = model.getHeader().classAttribute();
    }

    // need to construct an Instance to represent this
    // input row
    Instance toScore = constructInstance( inputMeta, inputRow, mappingIndexes, model, false );
    double[] prediction = model.distributionForInstance( toScore );

    // Update the model??
    if ( meta.getUpdateIncrementalModel() && model.isUpdateableModel() && !toScore.isMissing( toScore.classIndex() ) ) {
      model.update( toScore );
    }
    // First copy the input data to the new result...
    Object[] resultRow = RowDataUtil.resizeArray( inputRow, outputMeta.size() );
    int index = inputMeta.size();

    // output for numeric class or discrete class value
    if ( prediction.length == 1 || !outputProbs ) {
      if ( supervised ) {
        if ( classAtt.isNumeric() ) {
          Double newVal = new Double( prediction[0] );
          resultRow[index++] = newVal;
        } else {
          int maxProb = Utils.maxIndex( prediction );
          if ( prediction[maxProb] > 0 ) {
            String newVal = classAtt.value( maxProb );
            resultRow[index++] = newVal;
          } else {
            String
                newVal =
                BaseMessages.getString( WekaScoringMeta.PKG, "WekaScoringData.Message.UnableToPredict" ); //$NON-NLS-1$
            resultRow[index++] = newVal;
          }
        }
      } else {
        int maxProb = Utils.maxIndex( prediction );
        if ( prediction[maxProb] > 0 ) {
          Double newVal = new Double( maxProb );
          resultRow[index++] = newVal;
        } else {
          String
              newVal =
              BaseMessages
                  .getString( WekaScoringMeta.PKG, "WekaScoringData.Message.UnableToPredictCluster" ); //$NON-NLS-1$
          resultRow[index++] = newVal;
        }
      }
    } else {
      // output probability distribution
      for ( int i = 0; i < prediction.length; i++ ) {
        Double newVal = new Double( prediction[i] );
        resultRow[index++] = newVal;
      }
    }

    return resultRow;
  }

  /**
   * Helper method that constructs an Instance to input to the Weka model based
   * on incoming Kettle fields and pre-constructed attribute-to-field mapping
   * data.
   *
   * @param inputMeta      a <code>RowMetaInterface</code> value
   * @param inputRow       an <code>Object</code> value
   * @param mappingIndexes an <code>int</code> value
   * @param model          a <code>WekaScoringModel</code> value
   * @return an <code>Instance</code> value
   */
  private Instance constructInstance( RowMetaInterface inputMeta, Object[] inputRow, int[] mappingIndexes,
      WekaScoringModel model, boolean freshVector ) {

    Instances header = model.getHeader();

    // Re-use this array (unless told otherwise) to avoid an object creation
    if ( m_vals == null || freshVector ) {
      m_vals = new double[header.numAttributes()];
    }

    for ( int i = 0; i < header.numAttributes(); i++ ) {

      if ( mappingIndexes[i] >= 0 ) {
        try {
          Object inputVal = inputRow[mappingIndexes[i]];

          Attribute temp = header.attribute( i );
          ValueMetaInterface tempField = inputMeta.getValueMeta( mappingIndexes[i] );
          int fieldType = tempField.getType();

          // Check for missing value (null or empty string)
          if ( tempField.isNull( inputVal ) ) {
            m_vals[i] = Utils.missingValue();
            continue;
          }

          switch ( temp.type() ) {
            case Attribute.NUMERIC:
              if ( fieldType == ValueMetaInterface.TYPE_BOOLEAN ) {
                Boolean b = tempField.getBoolean( inputVal );
                if ( b.booleanValue() ) {
                  m_vals[i] = 1.0;
                } else {
                  m_vals[i] = 0.0;
                }
              } else if ( fieldType == ValueMetaInterface.TYPE_INTEGER ) {
                Long t = tempField.getInteger( inputVal );
                m_vals[i] = t.longValue();
              } else {
                Double n = tempField.getNumber( inputVal );
                m_vals[i] = n.doubleValue();
              }
              break;
            case Attribute.NOMINAL:
              String s = tempField.getString( inputVal );
              // now need to look for this value in the attribute
              // in order to get the correct index
              int index = temp.indexOfValue( s );
              if ( index < 0 ) {
                // set to missing value
                m_vals[i] = Utils.missingValue();
              } else {
                m_vals[i] = index;
              }
              break;
            case Attribute.STRING: {
              String s2 = tempField.getString( inputVal );
              // Set the attribute in the header to contain just this string value
              temp.setStringValue( s2 );
              m_vals[i] = 0.0;
              break;
            }
            default:
              m_vals[i] = Utils.missingValue();
          }
        } catch ( Exception e ) {
          m_vals[i] = Utils.missingValue();
        }
      } else {
        // set to missing value
        m_vals[i] = Utils.missingValue();
      }
    }

    Instance newInst = new DenseInstance( 1.0, m_vals );
    newInst.setDataset( header );
    return newInst;
  }
}
