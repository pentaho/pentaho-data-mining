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
 *    ArffOutputMeta.java
 *    Copyright 2007 Pentaho Corporation.  All rights reserved. 
 *
 */

package org.pentaho.di.arff;

import java.util.List;
import java.util.Map;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.dm.commons.ArffMeta;
import org.w3c.dom.Node;

/**
 * Contains the meta data for the ArffOutput step.
 * 
 * @author Mark Hall (mhall{[at]}pentaho.org)
 * @version 1.0
 */
@Step(id = "ArffOutput", image = "AO.png", name = "Arff Output", description = "Writes data in ARFF format to a file", categoryDescription = "Data Mining")
public class ArffOutputMeta extends BaseStepMeta implements StepMetaInterface {

  protected static Class<?> PKG = ArffOutputMeta.class;

  // Meta data for the output fields
  protected ArffMeta[] m_outputFields;

  // Filename for the ARFF file
  protected String m_fileName;

  // Character encoding to use (if not default encoding)
  protected String m_encoding;

  // Newline character sequence
  protected String m_newLine;

  // File format (DOS/UNIX)
  protected String m_fileFormat;

  // Relation name line for the ARFF file
  protected String m_relationName;

  // Output instances in sparse format
  protected boolean m_outputSparseInstances;

  /**
   * The anme of the field to use to set the weights. Null indicates no weight
   * setting (i.e. equal weights)
   */
  protected String m_weightField;

  /**
   * Creates a new <code>ArffOutputMeta</code> instance.
   */
  public ArffOutputMeta() {
    super(); // allocate BaseStepMeta
    setDefault();
  }

  /**
   * Allocate an array to hold meta data for the output fields
   * 
   * @param num number of meta data objects to allocate
   */
  public void allocate(int num) {
    m_outputFields = new ArffMeta[num];
  }

  /**
   * Set the name of the field to use to set instance weights from.
   * 
   * @param wfn the name of the field to use to set instance weights or null for
   *          equal weights.
   */
  public void setWeightFieldName(String wfn) {
    m_weightField = wfn;
  }

  /**
   * Gets the name of the field to use to set instance weights.
   * 
   * @return the name of the field to use or null if all instances are to
   *         receive equal weights
   */
  public String getWeightFieldName() {
    return m_weightField;
  }

  /**
   * Set whether to output instances in sparse format
   * 
   * @param o true if instances are to be output in sparse format
   */
  public void setOutputSparseIntsances(boolean o) {
    m_outputSparseInstances = o;
  }

  /**
   * Get whether to output instances in sparse format
   * 
   * @return true if instances are to be output in sparse format
   */
  public boolean getOutputSparseInstance() {
    return m_outputSparseInstances;
  }

  /**
   * Set the relation name to use
   * 
   * @param r the relation name
   */
  public void setRelationName(String r) {
    m_relationName = r;
  }

  /**
   * Get the relation name for the current ARFF file
   * 
   * @return the relation name
   */
  public String getRelationName() {
    return m_relationName;
  }

  /**
   * Set the filename to use
   * 
   * @param fn the filename to use
   */
  public void setFileName(String fn) {
    m_fileName = fn;
  }

  /**
   * Get the filename of the current ARFF file
   * 
   * @return the filename
   */
  public String getFileName() {
    return m_fileName;
  }

  /**
   * Set the character encoding to use
   * 
   * @param e the character encoding to use
   */
  public void setEncoding(String e) {
    m_encoding = e;
  }

  /**
   * Get the character encoding in use
   * 
   * @return a <code>String</code> value
   */
  public String getEncoding() {
    return m_encoding;
  }

  /**
   * Set the file format to use (DOS/UNIX)
   * 
   * @param ff the file format to use
   */
  public void setFileFormat(String ff) {
    m_fileFormat = ff;
  }

  /**
   * Get the file format in use
   * 
   * @return return the file format as a String (DOS or UNIX)
   */
  public String getFileFormat() {
    return m_fileFormat;
  }

  /**
   * Set the array of meta data for the output fields
   * 
   * @param am an array of ArffMeta
   */
  public void setOutputFields(ArffMeta[] am) {
    m_outputFields = am;
  }

  /**
   * Get the meta data for the output fields
   * 
   * @return an array of ArffMeta
   */
  public ArffMeta[] getOutputFields() {
    return m_outputFields;
  }

  /**
   * Sets up the ArffMeta array based on the incomming Kettle row format.
   * 
   * @param rmi a <code>RowMetaInterface</code> value
   */
  public void setupArffMeta(RowMetaInterface rmi) {
    if (rmi != null) {
      allocate(rmi.size());
      // initialize the output fields to all incoming fields with
      // corresponding arff types
      for (int i = 0; i < m_outputFields.length; i++) {
        ValueMetaInterface inField = rmi.getValueMeta(i);
        int fieldType = inField.getType();
        switch (fieldType) {
        case ValueMetaInterface.TYPE_NUMBER:
        case ValueMetaInterface.TYPE_INTEGER:
        case ValueMetaInterface.TYPE_BOOLEAN:
          m_outputFields[i] = new ArffMeta(inField.getName(), fieldType,
              ArffMeta.NUMERIC);
          break;
        case ValueMetaInterface.TYPE_STRING:
          m_outputFields[i] = new ArffMeta(inField.getName(), fieldType,
              ArffMeta.NOMINAL);

          // check for indexed values
          if (inField.getStorageType() == ValueMetaInterface.STORAGE_TYPE_INDEXED) {
            Object[] legalVals = inField.getIndex();
            StringBuffer temp = new StringBuffer();
            boolean first = true;
            for (Object l : legalVals) {
              if (first) {
                temp.append(l.toString().trim());
                first = false;
              } else {
                temp.append(",").append(l.toString().trim());
              }
            }
            m_outputFields[i].setNominalVals(temp.toString());
          }

          // -1);
          break;
        case ValueMetaInterface.TYPE_DATE:
          m_outputFields[i] = new ArffMeta(inField.getName(), fieldType,
              ArffMeta.DATE);
          m_outputFields[i].setDateFormat(inField.getDateFormat().toPattern());
          break;
        }
      }
    }
  }

  /**
   * Return the XML describing this (configured) step
   * 
   * @return a <code>String</code> containing the XML
   */
  @Override
  public String getXML() {
    StringBuffer retval = new StringBuffer(100);

    if (m_fileName != null) {

      retval.append(XMLHandler.addTagValue("arff_file_name", m_fileName));
      retval.append(XMLHandler.addTagValue("relation_name", m_relationName));
      retval.append(XMLHandler.addTagValue("file_format", m_fileFormat));
      retval.append(XMLHandler.addTagValue("encoding", m_encoding));

      retval.append(XMLHandler.addTagValue("output_sparse_instances",
          m_outputSparseInstances));

      if (!Const.isEmpty(m_weightField)) {
        retval.append(XMLHandler.addTagValue("weight_field", m_weightField));
      }

      retval.append("    <arff>" + Const.CR);
      if (m_outputFields != null) {
        for (int i = 0; i < m_outputFields.length; i++) {
          if (m_outputFields[i] != null) {
            retval.append("        ").append(m_outputFields[i].getXML())
                .append(Const.CR);
          }
        }
      }
      retval.append("    </arff>" + Const.CR);

    }
    return retval.toString();
  }

  /**
   * Loads the meta data for this (configured) step from XML.
   * 
   * @param stepnode the step to load
   * @exception KettleXMLException if an error occurs
   */
  public void loadXML(Node stepnode, List<DatabaseMeta> databases,
      Map<String, Counter> counters) throws KettleXMLException {

    m_fileName = XMLHandler.getTagValue(stepnode, "arff_file_name");
    m_relationName = XMLHandler.getTagValue(stepnode, "relation_name");
    m_fileFormat = XMLHandler.getTagValue(stepnode, "file_format");
    m_encoding = XMLHandler.getTagValue(stepnode, "encoding");
    m_weightField = XMLHandler.getTagValue(stepnode, "weight_field");

    String osi = XMLHandler.getTagValue(stepnode, "output_sparse_instances");
    if (!Const.isEmpty(osi)) {
      m_outputSparseInstances = osi.equalsIgnoreCase("Y");
    }

    m_newLine = getNewLine();

    Node fields = XMLHandler.getSubNode(stepnode, "arff");
    int nrfields = XMLHandler.countNodes(fields, ArffMeta.XML_TAG);

    allocate(nrfields);

    for (int i = 0; i < nrfields; i++) {
      Node fnode = XMLHandler.getSubNodeByNr(fields, ArffMeta.XML_TAG, i);
      m_outputFields[i] = new ArffMeta(fnode);
    }
  }

  /**
   * Read this step's configuration from a repository
   * 
   * @param rep the repository to access
   * @param id_step the id for this step
   * @exception KettleException if an error occurs
   */
  public void readRep(Repository rep, ObjectId id_step,
      List<DatabaseMeta> databases, Map<String, Counter> counters)
      throws KettleException {

    m_fileName = rep.getStepAttributeString(id_step, 0, "arff_file_name");
    m_relationName = rep.getStepAttributeString(id_step, 0, "relation_name");
    m_fileFormat = rep.getStepAttributeString(id_step, 0, "file_format");
    m_encoding = rep.getStepAttributeString(id_step, 0, "encoding");
    m_weightField = rep.getStepAttributeString(id_step, 0, "weight_field");
    m_outputSparseInstances = rep.getStepAttributeBoolean(id_step, 0,
        "output_sparse_instances");

    m_newLine = getNewLine();

    int nrFields = rep.countNrStepAttributes(id_step, "arff_field");
    allocate(nrFields);

    for (int i = 0; i < nrFields; i++) {
      m_outputFields[i] = new ArffMeta(rep, id_step, i);
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
  public void saveRep(Repository rep, ObjectId id_transformation,
      ObjectId id_step) throws KettleException {

    if (m_fileName != null) {
      rep.saveStepAttribute(id_transformation, id_step, 0, "arff_file_name",
          m_fileName);
      rep.saveStepAttribute(id_transformation, id_step, 0, "relation_name",
          m_fileName);
      rep.saveStepAttribute(id_transformation, id_step, 0, "file_format",
          m_fileFormat);
      rep.saveStepAttribute(id_transformation, id_step, 0, "encoding",
          m_encoding);
      rep.saveStepAttribute(id_transformation, id_step, 0,
          "output_sparse_instances", m_outputSparseInstances);
      if (!Const.isEmpty(m_weightField)) {
        rep.saveStepAttribute(id_transformation, id_step, 0, "weight_field",
            m_weightField);
      }
    }

    if (m_outputFields != null) {
      for (int i = 0; i < m_outputFields.length; i++) {
        if (m_outputFields[i] != null) {
          m_outputFields[i].saveRep(rep, id_transformation, id_step, i);
        }
      }
    }
  }

  /**
   * Generates row meta data to represent the fields output by this step
   * 
   * @param row the meta data for the output produced
   * @param origin the name of the step to be used as the origin
   * @param info The input rows metadata that enters the step through the
   *          specified channels in the same order as in method getInfoSteps().
   *          The step metadata can then choose what to do with it: ignore it or
   *          not.
   * @param nextStep if this is a non-null value, it's the next step in the
   *          transformation. The one who's asking, the step where the data is
   *          targetted towards.
   * @param space not sure what this is :-)
   * @exception KettleStepException if an error occurs
   */
  @Override
  public void getFields(RowMetaInterface row, String origin,
      RowMetaInterface[] info, StepMeta nextStep, VariableSpace space)
      throws KettleStepException {

    // Nothing to do!
  }

  /**
   * Check the settings of this step and put findings in a remarks list.
   * 
   * @param remarks the list to put the remarks in. see
   *          <code>org.pentaho.di.core.CheckResult</code>
   * @param transmeta the transform meta data
   * @param stepMeta the step meta data
   * @param prev the fields coming from a previous step
   * @param input the input step names
   * @param output the output step names
   * @param info the fields that are used as information by the step
   */
  public void check(List<CheckResultInterface> remarks, TransMeta transmeta,
      StepMeta stepMeta, RowMetaInterface prev, String[] input,
      String[] output, RowMetaInterface info) {

    CheckResult cr;

    if ((prev == null) || (prev.size() == 0)) {
      cr = new CheckResult(CheckResult.TYPE_RESULT_WARNING,
          "Not receiving any fields from previous steps!", stepMeta);
      remarks.add(cr);
    } else {
      cr = new CheckResult(CheckResult.TYPE_RESULT_OK,
          "Step is connected to previous one, receiving " + prev.size()
              + " fields", stepMeta);
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
  }

  /**
   * Check for equality
   * 
   * @param obj an <code>Object</code> to compare with
   * @return true if equal to the supplied object
   */
  @Override
  public boolean equals(Object obj) {
    if (obj != null && (obj.getClass().equals(this.getClass()))) {
      ArffOutputMeta m = (ArffOutputMeta) obj;
      return (getXML() == m.getXML());
    }

    return false;
  }

  /**
   * Clone this step's meta data
   * 
   * @return the cloned meta data
   */
  @Override
  public Object clone() {
    ArffOutputMeta retval = (ArffOutputMeta) super.clone();
    if (m_outputFields != null) {
      retval.allocate(m_outputFields.length);
      for (int i = 0; i < m_outputFields.length; i++) {
        retval.getOutputFields()[i] = (ArffMeta) m_outputFields[i].clone();
      }
    } else {
      retval.allocate(0);
    }
    return retval;
  }

  /**
   * Set default values
   */
  public void setDefault() {
    m_fileFormat = "DOS";
    m_fileName = "file";
    m_relationName = "NewRelation";
    m_weightField = null;
    m_newLine = getNewLine();
  }

  protected String getNewLine() {
    String nl = System.getProperty("line.separator");
    if (m_fileFormat != null) {
      if (m_fileFormat.equalsIgnoreCase("DOS")) {
        nl = "\r\n";
      } else if (m_fileFormat.equalsIgnoreCase("UNIX")) {
        nl = "\n";
      }
    }

    return nl;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.pentaho.di.trans.step.BaseStepMeta#getDialogClassName()
   */
  @Override
  public String getDialogClassName() {
    return "org.pentaho.di.arff.ArffOutputDialog";
  }

  /**
   * Get the executing step, needed by Trans to launch a step.
   * 
   * @param stepMeta the step info
   * @param stepDataInterface the step data interface linked to this step. Here
   *          the step can store temporary data, database connections, etc.
   * @param cnr the copy number to get.
   * @param tr the transformation info.
   * @param trans the launching transformation
   * @return a <code>StepInterface</code> value
   */
  public StepInterface getStep(StepMeta stepMeta,
      StepDataInterface stepDataInterface, int cnr, TransMeta tr, Trans trans) {
    return new ArffOutput(stepMeta, stepDataInterface, cnr, tr, trans);
  }

  /**
   * Get a new instance of the appropriate data class. This data class
   * implements the StepDataInterface. It basically contains the persisting data
   * that needs to live on, even if a worker thread is terminated.
   * 
   * @return a <code>StepDataInterface</code> value
   */
  public StepDataInterface getStepData() {
    return new ArffOutputData();
  }
}
