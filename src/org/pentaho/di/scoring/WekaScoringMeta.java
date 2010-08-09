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
 *    WekaScoringMeta.java
 *    Copyright 2007 Pentaho Corporation.  All rights reserved. 
 *
 */

package org.pentaho.di.scoring;

import java.util.List;
import java.util.Map;
import java.io.*;

import org.eclipse.swt.widgets.Shell;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.kdr.KettleDatabaseRepository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.logging.LogWriter;
import org.w3c.dom.Node;

import weka.core.Instances;
import weka.core.SerializedObject;

/**
 * Contains the meta data for the WekaScoring step.
 *
 * @author Mark Hall (mhall{[at]}pentaho.org)
 * @version 1.0
 */
public class WekaScoringMeta 
  extends BaseStepMeta
  implements StepMetaInterface {

  public static final String XML_TAG = "weka_scoring";
  
  // Use a model file specified in an incoming field
  private boolean m_fileNameFromField;
  
  // Whether to cache loaded models in memory (when they are being specified
  // by a field in the incoming rows
  private boolean m_cacheLoadedModels;
  
  // The name of the field that is being used to specify model file name/path
  private String m_fieldNameToLoadModelFrom;
  
  // File name of the serialized Weka model to load/import
  private String m_modelFileName;

  // File name to save incrementally updated model to
  private String m_savedModelFileName;
  
  // True if predicted probabilities are to be output
  // (has no effect if the class (target is numeric)
  private boolean m_outputProbabilities;

  // True if user has selected to update a model on the incoming
  // data stream and the model supports incremental updates and
  // there exists a column in the incoming data stream that has
  // been matched successfully to the class attribute (if one 
  // exists).
  private boolean m_updateIncrementalModel;

  // Holds the actual Weka model (classifier or clusterer)
  private WekaScoringModel m_model;

  // holds a default model - used only when model files are sourced
  // from a field in the incoming data rows. In this case, it is
  // the fallback model if there is no model file specified in the
  // current incoming row. Is also necessary so that getFields()
  // can determine the full output structure.
  private WekaScoringModel m_defaultModel;

  // used to map attribute indices to incoming field indices
  private int[] m_mappingIndexes;

  // logging
  //protected LogChannelInterface m_log;

  /**
   * Creates a new <code>WekaScoringMeta</code> instance.
   */
  public WekaScoringMeta() {
    super(); // allocate BaseStepMeta
  }
    
  public void setFileNameFromField(boolean f) {
    m_fileNameFromField = f;
  }
  
  public boolean getFileNameFromField() {
    return m_fileNameFromField;
  }
  
  public void setCacheLoadedModels(boolean l) {
    m_cacheLoadedModels = l;
  }
  
  public boolean getCacheLoadedModels() {
    return m_cacheLoadedModels;
  }
  
  public void setFieldNameToLoadModelFrom(String fn) {
    m_fieldNameToLoadModelFrom = fn;
  }
  
  public String getFieldNameToLoadModelFrom() {
    return m_fieldNameToLoadModelFrom;
  }

  /**
   * Set the file name of the serialized Weka model to 
   * load/import from
   *
   * @param mfile the file name
   */
  public void setSerializedModelFileName(String mfile) {
    m_modelFileName = mfile;
  }

  /**
   * Get the filename of the serialized Weka model to 
   * load/import from
   *
   * @return the file name
   */
  public String getSerializedModelFileName() {
    return m_modelFileName;
  }

  /**
   * Set the file name that the incrementally updated model
   * will be saved to when the current stream of data terminates
   *
   * @param savedM the file name to save to
   */
  public void setSavedModelFileName(String savedM) {
    m_savedModelFileName = savedM;
  }

  /**
   * Get the file name that the incrementally updated model
   * will be saved to when the current stream of data terminates
   *
   * @return the file name to save to
   */
  public String getSavedModelFileName() {
    return m_savedModelFileName;
  }

  /**
   * Set the Weka model
   *
   * @param model a <code>WekaScoringModel</code> that encapsulates
   * the actual Weka model (Classifier or Clusterer)
   */
  public void setModel(WekaScoringModel model) {
    m_model = model;
  }

  /**
   * Get the Weka model
   *
   * @return a <code>WekaScoringModel</code> that encapsulates
   * the actual Weka model (Classifier or Clusterer)
   */
  public WekaScoringModel getModel() {
    return m_model;
  }
  
  /**
   * Gets the default model (only used when model file names
   * are being sourced from a field in the incoming rows).
   * 
   * @return the default model to use when there is no
   * filename provided in the incoming data row.
   */
  public WekaScoringModel getDefaultModel() {
    return m_defaultModel;
  }
  
  /**
   * Sets the default model (only used when model file names
   * are being sourced from a field in the incoming rows).
   * 
   * @param defaultM the default model to use.
   */
  public void setDefaultModel(WekaScoringModel defaultM) {
    m_defaultModel = defaultM;
  }

  /**
   * Set whether to predict probabilities
   *
   * @param b true if a probability distribution is
   * to be output
   */
  public void setOutputProbabilities(boolean b) {
    m_outputProbabilities = b;
  }

  /**
   * Get whether to predict probabilities
   *
   * @return a true if a probability distribution is
   * to be output
   */
  public boolean getOutputProbabilities() {
    return m_outputProbabilities;
  }

  /**
   * Get whether the model is to be incrementally updated with
   * each incoming row (after making a prediction for it).
   *
   * @return a true if the model is to be updated incrementally
   * with each incoming row
   */
  public boolean getUpdateIncrementalModel() {
    return m_updateIncrementalModel;
  }

  /**
   * Set whether to update the model incrementally
   *
   * @param u true if the model should be updated with
   * each incoming row (after predicting it)
   */
  public void setUpdateIncrementalModel(boolean u) {
    m_updateIncrementalModel = u;
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
   */
  public void mapIncomingRowMetaData(Instances header,
                                     RowMetaInterface inputRowMeta) {
    m_mappingIndexes = WekaScoringData.findMappings(header, inputRowMeta);
    
    // If updating of incremental models has been selected, then
    // check on the ability to do this
    if (m_updateIncrementalModel && 
        m_model.isSupervisedLearningModel()) {
      if (m_model.isUpdateableModel()) {
        // Do we have the class mapped successfully to an incoming
        // Kettle field
        if (m_mappingIndexes[header.classIndex()] ==
            WekaScoringData.NO_MATCH ||
            m_mappingIndexes[header.classIndex()] ==
            WekaScoringData.TYPE_MISMATCH) {
          m_updateIncrementalModel = false;
          logError(Messages.getString("WekaScoringMeta.Log.NoMatchForClass"));
          /*          System.err.println("Can't update model because there is no "
                             +"match for the class attribute in the "
                             +"incoming data stream!!"); */
        }
      } else {
        m_updateIncrementalModel = false;
        logError(Messages.getString("WekaScoringMeta.Log.ModelNotUpdateable"));
        /*        System.err.println("Model is not updateable. Can't learn "
                  + "from incoming data stream!"); */
      }
    }
  }

  /**
   * Get the mapping from attributes to incoming Kettle fields
   *
   * @return the mapping as an array of integer indices
   */
  public int[] getMappingIndexes() {
    return m_mappingIndexes;
  }

  /**
   * Return the XML describing this (configured) step
   *
   * @return a <code>String</code> containing the XML
   */
  public String getXML() {

    StringBuffer retval = new StringBuffer(100);

    retval.append("<" + XML_TAG + ">");
    
    retval.append(XMLHandler.addTagValue("output_probabilities",
                                         m_outputProbabilities));
    retval.append(XMLHandler.addTagValue("update_model",
                                         m_updateIncrementalModel));

    if (m_updateIncrementalModel) {
      // any file name to save the changed model to?
      if (!Const.isEmpty(m_savedModelFileName)) {
        retval.append(XMLHandler.addTagValue("model_export_file_name",
                                             m_savedModelFileName));
      }
    }
    
    retval.append(XMLHandler.addTagValue("file_name_from_field", 
        m_fileNameFromField));
    if (m_fileNameFromField) {
      // any non-null field name?
      if (!Const.isEmpty(m_fieldNameToLoadModelFrom)) {
        retval.append(XMLHandler.addTagValue("field_name_to_load_from", 
            m_fieldNameToLoadModelFrom));
        System.out.println(Messages.getString("WekaScoringMeta.Log.ModelSourcedFromField")
            + " " + m_fieldNameToLoadModelFrom);
      }
    }
    
    retval.append(XMLHandler.addTagValue("cache_loaded_models", 
        m_cacheLoadedModels));
    
    WekaScoringModel temp = (m_fileNameFromField)
      ? m_defaultModel
      : m_model;

    // can we save the model as XML?
    if (temp != null 
        && Const.isEmpty(m_modelFileName)) {

      try {
        // Convert model to base64 encoding
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(bao);
        ObjectOutputStream oo = new ObjectOutputStream(bos);
        oo.writeObject(temp);
        oo.flush();
        byte[] model = bao.toByteArray();
        String base64model = XMLHandler.addTagValue("weka_scoring_model",
                                                    model);
        String modType = (m_fileNameFromField) ? "default" : "";
        System.out.println("Serializing " + modType + " model.");
        System.out.println(Messages.getString("WekaScoringMeta.Log.SizeOfModel")
                       + " " + base64model.length());
        //        System.err.println("Size of base64 model "+base64model.length());
        retval.append(base64model);
        oo.close();
      } catch (Exception ex) { 
        System.out.println(Messages.getString("WekaScoringMeta.Log.Base64SerializationProblem"));
        //        System.err.println("Problem serializing model to base64 (Meta.getXML())");
      }
    } else {
      if (!Const.isEmpty(m_modelFileName)) {
        /*m_log.logBasic("[WekaScoringMeta] ", 
                       Messages.getString("WekaScoringMeta.Log.ModelSourcedFromFile")
                       + " " + m_modelFileName); */

        System.out.println(Messages.getString("WekaScoringMeta.Log.ModelSourcedFromFile")
            + " " + m_modelFileName);

        /*logBasic(Messages.getString("WekaScoringMeta.Log.ModelSourcedFromFile")
            + " " + m_modelFileName); */
        //logBasic(lm);
      }
      /*      System.err.println("Model will be sourced from file "
              + m_modelFileName); */
      // save the model file name
      retval.append(XMLHandler.addTagValue("model_file_name",
                                           m_modelFileName));
    }
      
    retval.append("</" + XML_TAG + ">");
    return retval.toString();
  }
  
  /**
   * Check for equality
   *
   * @param obj an <code>Object</code> to compare with
   * @return true if equal to the supplied object
   */
  public boolean equals(Object obj) {       
    if (obj != null && (obj.getClass().equals(this.getClass()))) {
      WekaScoringMeta m = (WekaScoringMeta)obj;
      return (getXML() == m.getXML());
    }
    
    return false;
  }

  /**
   * Hash code method
   *
   * @return the hash code for this object
   */
  public int hashCode() {
    return getXML().hashCode();
  }

  /**
   * Clone this step's meta data
   *
   * @return the cloned meta data
   */
  public Object clone() {
    WekaScoringMeta retval = (WekaScoringMeta) super.clone();
    // deep copy the model (if any)
    if (m_model != null) {
      try {
        SerializedObject so = new SerializedObject(m_model);
        WekaScoringModel copy = (WekaScoringModel)so.getObject();
        copy.setLog(getLog());
        retval.setModel(copy);
      } catch (Exception ex) {
        logError(Messages.getString("WekaScoringMeta.Log.DeepCopyingError"));
        //        System.err.println("Problem deep copying scoring model (meta.clone())");
      }
    }
    
    // deep copy the default model (if any)
    if (m_defaultModel != null) {
      try {
        SerializedObject so = new SerializedObject(m_defaultModel);
        WekaScoringModel copy = (WekaScoringModel)so.getObject();
        copy.setLog(getLog());
        retval.setDefaultModel(copy);
      } catch (Exception ex) {
        logError(Messages.getString("WekaScoringMeta.Log.DeepCopyingError"));
        //        System.err.println("Problem deep copying scoring model (meta.clone())");
      }
    }
    
    return retval;
  }

  public void setDefault() {
    m_modelFileName = null;
    m_outputProbabilities = false;
  }

  /**
   * Loads the meta data for this (configured) step
   * from XML.
   *
   * @param stepnode the step to load
   * @exception KettleXMLException if an error occurs
   */
  public void loadXML(Node stepnode, 
                      List<DatabaseMeta> databases, 
                      Map<String, Counter> counters)
    throws KettleXMLException {
    
    // Make sure that all Weka packages have been loaded
    weka.core.WekaPackageManager.loadPackages(false);

    int nrModels = 
      XMLHandler.countNodes(stepnode, XML_TAG);

    if (nrModels > 0) {
      Node wekanode = 
        XMLHandler.getSubNodeByNr(stepnode, XML_TAG, 0);
      
      String temp = XMLHandler.getTagValue(wekanode, "file_name_from_field");
      if (temp.equalsIgnoreCase("N")) {
        m_fileNameFromField = false;
      } else {
        m_fileNameFromField = true;
      }
      
      if (m_fileNameFromField) {
        m_fieldNameToLoadModelFrom = 
          XMLHandler.getTagValue(wekanode, "field_name_to_load_from");
      }
      
      temp = XMLHandler.getTagValue(wekanode, "cache_loaded_models");
      if (temp.equalsIgnoreCase("N")) {
        m_cacheLoadedModels = false;
      } else {
        m_cacheLoadedModels = true;
      }

      // try and get the XML-based model
      boolean success = false;
      try {
        String base64modelXML = XMLHandler.getTagValue(wekanode,
                                                       "weka_scoring_model");
        //        System.err.println("Got base64 string...");
        //            System.err.println(base64modelXML);
        deSerializeBase64Model(base64modelXML);
        success = true;
        
        String modType = (m_fileNameFromField) ? "default" : "";
        logBasic("Deserializing " + modType + " model.");
        //        System.err.println("Successfully de-serialized model!");
        logDetailed(Messages.getString("WekaScoringMeta.Log.DeserializationSuccess"));
      } catch (Exception ex) {
        success = false;
      }

      if (!success) {
        // fall back and try and grab a model file name
        m_modelFileName = 
          XMLHandler.getTagValue(wekanode, "model_file_name");
      }

      temp = XMLHandler.getTagValue(wekanode, "output_probabilities");
      if (temp.equalsIgnoreCase("N")) {
        m_outputProbabilities = false;
      } else {
        m_outputProbabilities = true;
      }

      temp = XMLHandler.getTagValue(wekanode, "update_model");
      if (temp.equalsIgnoreCase("N")) {
        m_updateIncrementalModel = false;
      } else {
        m_updateIncrementalModel = true;
      }

      if (m_updateIncrementalModel) {
        m_savedModelFileName = 
          XMLHandler.getTagValue(wekanode, "model_export_file_name");
      }      
    }

    // check the model status. If no model and we have
    // a file name, try and load here. Otherwise, loading
    // wont occur until the transformation starts or the
    // user opens the configuration gui in Spoon. This affects
    // the result of the getFields method and has an impact
    // on downstream steps that need to know what we produce
    WekaScoringModel temp = (m_fileNameFromField) ? m_defaultModel : m_model;
    if (temp == null && !Const.isEmpty(m_modelFileName)) {
      try {
        loadModelFile();
      } catch (Exception ex) {
        throw new KettleXMLException("Problem de-serializing model "
                                          + "file using supplied file name!"); 
      }
    }
  }

  protected void loadModelFile() throws Exception {
    File modelFile = 
      new File(m_modelFileName);
    if (modelFile.exists()) {
      if (m_fileNameFromField) {
        logBasic("loading default model from file.");
        m_defaultModel = WekaScoringData.loadSerializedModel(modelFile, getLog());
      } else {
        logBasic("loading model from file.");
        m_model = WekaScoringData.loadSerializedModel(modelFile, getLog());
      }
    }
  }


  protected void deSerializeBase64Model(String base64modelXML) 
    throws Exception {
    byte[] model = XMLHandler.stringToBinary(base64modelXML);
    //    System.err.println("Got model byte array ok.");
    //    System.err.println("Length of array "+model.length);
            
    // now de-serialize
    ByteArrayInputStream bis = new ByteArrayInputStream(model);
    ObjectInputStream ois = new ObjectInputStream(bis);
    
    if (m_fileNameFromField) {
      m_defaultModel = (WekaScoringModel)ois.readObject();
    } else {
      m_model = (WekaScoringModel)ois.readObject();
    }
    ois.close();
  }

  /**
   * Read this step's configuration from a repository
   *
   * @param rep the repository to access
   * @param id_step the id for this step
   * @exception KettleException if an error occurs
   */
  public void readRep(Repository rep, 
                      ObjectId id_step, 
                      List<DatabaseMeta> databases, 
                      Map<String, Counter> counters) 
    throws KettleException {
    
    // Make sure that all Weka packages have been loaded
    weka.core.WekaPackageManager.loadPackages(false);
    
    m_fileNameFromField = 
      rep.getStepAttributeBoolean(id_step, 0, "file_name_from_field");

    if (m_fileNameFromField) {
      m_fieldNameToLoadModelFrom = 
        rep.getStepAttributeString(id_step, 0, "field_name_to_load_from");
    }
    
    m_cacheLoadedModels = 
      rep.getStepAttributeBoolean(id_step, 0, "cache_loaded_models");

    // try and get a filename first as this overrides any model stored
    // in the repository
    boolean success = false;
    try {
      m_modelFileName = 
        rep.getStepAttributeString(id_step, 0, "model_file_name");
      success = true;
      if (m_modelFileName == null || Const.isEmpty(m_modelFileName)) {
        success = false;
      }
    } catch (KettleException ex) {
      success = false;
    }

    if (!success) {
      // try and get the model itself...
      try {
        String base64XMLModel = rep.getStepAttributeString(id_step, 0, "weka_scoring_model");
        logDebug(Messages.getString("WekaScoringMeta.Log.SizeOfModel")
                       + " " + base64XMLModel.length());
          //        System.err.println("Size of base64 string read " + base64XMLModel.length());
        //        System.err.println(xmlModel);
        if (base64XMLModel != null && base64XMLModel.length() > 0) {
          // try to de-serialize
          deSerializeBase64Model(base64XMLModel);
          success = true;
        } else {
          success = false;
        }
      } catch (Exception ex) {
        ex.printStackTrace();
        success = false;
      }
    }

    m_outputProbabilities = 
      rep.getStepAttributeBoolean(id_step, 0, "output_probabilities");

    m_updateIncrementalModel = 
      rep.getStepAttributeBoolean(id_step, 0, "update_model");

    if (m_updateIncrementalModel) {
      m_savedModelFileName =
        rep.getStepAttributeString(id_step, 0, "model_export_file_name");
    }    
    
    // check the model status. If no model and we have
    // a file name, try and load here. Otherwise, loading
    // wont occur until the transformation starts or the
    // user opens the configuration gui in Spoon. This affects
    // the result of the getFields method and has an impact
    // on downstream steps that need to know what we produce
    WekaScoringModel temp = (m_fileNameFromField) ? m_defaultModel : m_model;
    if (temp == null && !Const.isEmpty(m_modelFileName)) {
      try {
        loadModelFile();
      } catch (Exception ex) {
        throw new KettleException("Problem de-serializing model "
                                  + "file using supplied file name!"); 
      }
    }        
  }

  /**
   * Save this step's meta data to a repository
   *
   * @param rep the repository to save to
   * @param id_transformation transformation id
   * @param id_step step id
   * @exception KettleException if an error occurs
   */
  public void saveRep(Repository rep, 
                      ObjectId id_transformation, 
                      ObjectId id_step)
    throws KettleException {

    rep.saveStepAttribute(id_transformation,
                          id_step, 0,
                          "output_probabilities",
                          m_outputProbabilities);

    rep.saveStepAttribute(id_transformation,
                          id_step, 0,
                          "update_model",
                          m_updateIncrementalModel);

    if (m_updateIncrementalModel) {
      // any file name to save the changed model to?
      if (!Const.isEmpty(m_savedModelFileName)) {
        rep.saveStepAttribute(id_transformation,
                              id_step, 0,
                              "model_export_file_name",
                              m_savedModelFileName);
      }
    }
    
    rep.saveStepAttribute(id_transformation,
       id_step, 0, "file_name_from_field", m_fileNameFromField);
    if (m_fileNameFromField) {
      rep.saveStepAttribute(id_transformation, id_step, 0, 
          "field_name_to_load_from", m_fieldNameToLoadModelFrom);
    }
    
    rep.saveStepAttribute(id_transformation, id_step, 0, 
        "cache_loaded_models", m_cacheLoadedModels);
    
    WekaScoringModel temp = (m_fileNameFromField)
      ? m_defaultModel
      : m_model;
       
    if (temp != null 
        && Const.isEmpty(m_modelFileName)) {
      try {
        // Convert model to base64 encoding
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(bao);
        ObjectOutputStream oo = new ObjectOutputStream(bos);
        oo.writeObject(temp);
        oo.flush();
        byte[] model = bao.toByteArray();
        String base64XMLModel = KettleDatabaseRepository.byteArrayToString(model);

        String modType = (m_fileNameFromField) ? "default" : "";
        logBasic("Serializing " + modType + " model.");
        
        // String xmlModel = XStream.serialize(m_model);
        rep.saveStepAttribute(id_transformation,
                              id_step, 0,
                              "weka_scoring_model",
                              base64XMLModel);
        oo.close();
      } catch (Exception ex) {
        logError(Messages.getString("WekaScoringDialog.Log.Base64SerializationProblem"));
        //        System.err.println("Problem serializing model to base64 (Meta.saveRep())");
      }
    } else {
      // either XStream is not present or user wants to source from
      // file
      if (!Const.isEmpty(m_modelFileName)) {
        logBasic(Messages.getString("WekaScoringMeta.Log.ModelSourcedFromFile")
                       + " " + m_modelFileName);
      }
      /*      System.err.println("Model will be sourced from file "
              + m_modelFileName); */
      rep.saveStepAttribute(id_transformation, 
                            id_step, 0, 
                            "model_file_name",
                            m_modelFileName);
    }
  }

  /**
   * Generates row meta data to represent
   * the fields output by this step
   *
   * @param row the meta data for the output produced
   * @param origin the name of the step to be used as the origin
   * @param info The input rows metadata that enters the step through 
   * the specified channels in the same order as in method getInfoSteps(). 
   * The step metadata can then choose what to do with it: ignore it or not.
   * @param nextStep if this is a non-null value, it's the next step in 
   * the transformation. The one who's asking, the step where the data is 
   * targetted towards.
   * @param space not sure what this is :-)
   * @exception KettleStepException if an error occurs
   */
  public void getFields(RowMetaInterface row, 
                        String origin, 
                        RowMetaInterface[] info, 
                        StepMeta nextStep, 
                        VariableSpace space) 
    throws KettleStepException {
    
    if (m_model == null && !Const.isEmpty(getSerializedModelFileName())) {
      // see if we can load from a file.
      
      String modName = getSerializedModelFileName();
      modName = space.environmentSubstitute(modName);
      File modelFile = null;
      if (modName.startsWith("file:")) {
        try {
          modelFile = 
            new File(new java.net.URI(modName));
        } catch (Exception ex) {
          throw new KettleStepException("Malformed URI for model file");
        }
      } else {
        modelFile = new File(modName);
      }  
      if (!modelFile.exists()) {
        throw new KettleStepException("Serialized model file does "
                                  + "not exist on disk!");
      }
      
      try {
        WekaScoringModel model = 
          WekaScoringData.loadSerializedModel(modelFile, getLog());
        setModel(model);
      } catch (Exception ex) {
        throw new KettleStepException("Problem de-serializing model file");
      }
    }
    
    if (m_model != null) {
      Instances header = m_model.getHeader();
      String classAttName = null;
      boolean supervised = m_model.isSupervisedLearningModel();

      if (supervised) {
        classAttName = header.classAttribute().name();
      
        if (header.classAttribute().isNumeric() ||
            !m_outputProbabilities) {
          int valueType = (header.classAttribute().isNumeric())
            ? ValueMetaInterface.TYPE_NUMBER
            : ValueMetaInterface.TYPE_STRING;

          ValueMetaInterface newVM = 
            new ValueMeta(classAttName
                          + "_predicted",
                          valueType);
          newVM.setOrigin(origin);
          row.addValueMeta(newVM);
          //          System.err.println("Adding " + newVM.getName());
          logDebug("Adding " + newVM.getName());
        } else {
          for (int i = 0; i < header.classAttribute().numValues(); i++) {
            String classVal = header.classAttribute().value(i);
            ValueMetaInterface newVM = 
              new ValueMeta(classAttName + 
                            ":" + classVal + "_predicted_prob",
                            ValueMetaInterface.TYPE_NUMBER);
            newVM.setOrigin(origin);
            row.addValueMeta(newVM);
            logDebug("Adding " + newVM.getName());
            //            System.err.println("Adding "+newVM.getName());
          }
        }
      } else {
        if (m_outputProbabilities) {
          try {
            int numClusters = 
              ((WekaScoringClusterer)m_model).numberOfClusters();
            for (int i = 0; i < numClusters; i++) {
              ValueMetaInterface newVM = 
                new ValueMeta("cluster_" + i + "_predicted_prob",
                              ValueMetaInterface.TYPE_NUMBER);
              newVM.setOrigin(origin);
              row.addValueMeta(newVM);
              logDebug("Adding " + newVM.getName());
              //              System.err.println("Adding "+newVM.getName());              
            }
          } catch (Exception ex) {
            throw new KettleStepException("Problem with clustering model: "
                                          + "unable to get number of clusters");
          }
        } else {
          ValueMetaInterface newVM = 
            new ValueMeta("cluster#_predicted",
                          ValueMetaInterface.TYPE_NUMBER);
          newVM.setOrigin(origin);
          row.addValueMeta(newVM);
          logDebug("Adding " + newVM.getName());
          //          System.err.println("Adding " + newVM.getName());
        }
      }
    }
  }

  /**
   * Check the settings of this step and put findings
   * in a remarks list.
   *
   * @param remarks the list to put the remarks in. 
   * see <code>org.pentaho.di.core.CheckResult</code>
   * @param transmeta the transform meta data
   * @param stepMeta the step meta data
   * @param prev the fields coming from a previous step
   * @param input the input step names
   * @param output the output step names
   * @param info the fields that are used as information by the step
   */
  public void check(List<CheckResultInterface> remarks, 
                    TransMeta transmeta,
                    StepMeta stepMeta, 
                    RowMetaInterface prev, 
                    String[] input, 
                    String[] output,
                    RowMetaInterface info) {

    CheckResult cr;

    if ((prev == null) || (prev.size() == 0)) {
      cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING,
          "Not receiving any fields from previous steps!", stepMeta);
      remarks.add(cr);
    } else {
      cr = new CheckResult(CheckResult.TYPE_RESULT_OK,
          "Step is connected to previous one, receiving " + prev.size() +
          " fields", stepMeta);
      remarks.add(cr);
    }

    // See if we have input streams leading to this step!
    if (input.length > 0) {
      cr = new CheckResult(CheckResult.TYPE_RESULT_OK,
          "Step is receiving info from other steps.", stepMeta);
      remarks.add(cr);
    } else {
      cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR,
          "No input received from other steps!", stepMeta);
      remarks.add(cr);
    }

    if (m_model == null) {
      if (!Const.isEmpty(m_modelFileName)) {
        File f = new File(m_modelFileName);
        if (!f.exists()) {
          cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR,
                               "Step does not have access to a "
                               + "usable model!", stepMeta);
          remarks.add(cr);
        }
      }
    }
  }

  /**
   * Get the UI for this step.
   *
   * @param shell a <code>Shell</code> value
   * @param meta a <code>StepMetaInterface</code> value
   * @param transMeta a <code>TransMeta</code> value
   * @param name a <code>String</code> value
   * @return a <code>StepDialogInterface</code> value
   */
  public StepDialogInterface getDialog(Shell shell, 
                                       StepMetaInterface meta,
                                       TransMeta transMeta, 
                                       String name) {

    // Not sure how this works in Kettle. Guessing that
    // reflection is used to look for this method (as it
    // is not defined in BaseStepMeta or StepMetaInterface. If
    // this method is ommitted, Kettle seems to look for a UI
    // class in org.pentaho.di.ui.

    return new WekaScoringDialog(shell, meta, transMeta, name);
  }

  /**
   * Get the executing step, needed by Trans to launch a step.
   *
   * @param stepMeta the step info
   * @param stepDataInterface the step data interface linked 
   * to this step. Here the step can store temporary data, 
   * database connections, etc.
   * @param cnr the copy number to get.
   * @param tr the transformation info.
   * @param trans the launching transformation
   * @return a <code>StepInterface</code> value
   */
  public StepInterface getStep(StepMeta stepMeta, 
                               StepDataInterface stepDataInterface, 
                               int cnr, 
                               TransMeta tr, 
                               Trans trans) {

    return new WekaScoring(stepMeta, stepDataInterface, cnr, tr, trans);
  }

  /**
   * Get a new instance of the appropriate data class. This 
   * data class implements the StepDataInterface. It basically 
   * contains the persisting data that needs to live on, even 
   * if a worker thread is terminated.
   *
   * @return a <code>StepDataInterface</code> value
   */
  public StepDataInterface getStepData() {

    return new WekaScoringData();
  }
}
                                     