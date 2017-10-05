/*
 * Copyright (c) 2007 - 2017 Pentaho Corporation.  All rights reserved. 
 * This software was developed by Pentaho Corporation and is provided under the terms 
 * of the GNU Lesser General Public License, Version 2.1. You may not use 
 * this file except in compliance with the license. If you need a copy of the license, 
 * please go to http://www.gnu.org/licenses/lgpl-2.1.txt. The Original Code is Pentaho 
 * DM Commons.  The Initial Developer is Pentaho Corporation.
 *
 * Software distributed under the GNU Lesser Public License is distributed on an "AS IS" 
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to 
 * the license for the specific language governing your rights and limitations.
 */

/*
 *    ArffMeta.java
 *    Copyright 2007 - 2017 Pentaho Corporation.  All rights reserved. 
 *
 */

package org.pentaho.dm.commons;

import java.util.ArrayList;
import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.w3c.dom.Node;

/**
 * Contains the meta data for one field being converted to ARFF
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}org)
 * @version $Revision: 1.0 $
 */
public class ArffMeta implements Cloneable {

  // some constants for ARFF data types
  public static final int NUMERIC = 0;
  public static final int NOMINAL = 1;
  public static final int DATE = 2;
  public static final int STRING = 3;

  // the name of this field
  private final String m_fieldName;

  // the Kettle data type (as defined in ValueMetaInterface)
  private int m_kettleType;

  // the ARFF data type (as defined by the constants above)
  private int m_arffType;

  // the format for date fields
  private String m_dateFormat;

  private String m_nominalVals;

  public static final String XML_TAG = "arff_field";

  /**
   * Constructor
   * 
   * @param fieldName the name of this field
   * @param kettleType the Kettle data type
   * @param arffType the ARFF data type
   */
  public ArffMeta(String fieldName, int kettleType, int arffType) {

    m_fieldName = fieldName;
    m_kettleType = kettleType;
    m_arffType = arffType;
    // m_precision = precision;
  }

  /**
   * Construct using data stored in repository
   * 
   * @param rep the repository
   * @param id_step the id of the step
   * @param nr the step number
   * @exception KettleException if an error occurs
   */
  public ArffMeta(Repository rep, ObjectId id_step, int nr)
      throws KettleException {
    m_fieldName = rep.getStepAttributeString(id_step, nr, "field_name");
    m_kettleType = (int) rep
        .getStepAttributeInteger(id_step, nr, "kettle_type");
    m_arffType = (int) rep.getStepAttributeInteger(id_step, nr, "arff_type");
    m_dateFormat = rep.getStepAttributeString(id_step, nr, "date_format");
    m_nominalVals = rep.getStepAttributeString(id_step, nr, "nominal_vals");
  }

  /**
   * Construct from an XML node
   * 
   * @param uniNode a XML node
   */
  public ArffMeta(Node arffNode) {
    String temp;
    m_fieldName = XMLHandler.getTagValue(arffNode, "field_name");
    temp = XMLHandler.getTagValue(arffNode, "kettle_type");
    try {
      m_kettleType = Integer.parseInt(temp);
    } catch (Exception ex) {
      // ignore - shouldn't actually get here
    }
    temp = XMLHandler.getTagValue(arffNode, "arff_type");
    try {
      m_arffType = Integer.parseInt(temp);
    } catch (Exception ex) {
      // ignore
    }

    m_dateFormat = XMLHandler.getTagValue(arffNode, "date_format");
    m_nominalVals = XMLHandler.getTagValue(arffNode, "nominal_vals");
  }

  /**
   * Get the name of this field
   * 
   * @return the name of the field
   */
  public String getFieldName() {
    return m_fieldName;
  }

  /**
   * Get the Kettle data type (as defined in ValueMetaInterface)
   * 
   * @return the Kettle data type of this field
   */
  public int getKettleType() {
    return m_kettleType;
  }

  /**
   * Get the ARFF data type
   * 
   * @return the ARFF data type of this field
   */
  public int getArffType() {
    return m_arffType;
  }

  public void setDateFormat(String dateFormat) {
    m_dateFormat = dateFormat;
  }

  public String getDateFormat() {
    return m_dateFormat;
  }

  /**
   * Set a comma-separated list of legal values for a nominal attribute
   * 
   * @param vals the legal values
   */
  public void setNominalVals(String vals) {
    m_nominalVals = vals;
  }

  /**
   * Get a comma-separated list of legal values for a nominal attribute
   * 
   * @return the legal values
   */
  public String getNominalVals() {
    return m_nominalVals;
  }

  /**
   * Make a copy
   * 
   * @return a copy of this UnivariateStatsMetaFunction.
   */
  @Override
  public Object clone() {
    try {
      ArffMeta retval = (ArffMeta) super.clone();

      return retval;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  /**
   * Get the XML representation of this field
   * 
   * @return a <code>String</code> value
   */
  public String getXML() {
    String xml = ("<" + XML_TAG + ">");

    xml += XMLHandler.addTagValue("field_name", m_fieldName);
    xml += XMLHandler.addTagValue("kettle_type", m_kettleType);
    xml += XMLHandler.addTagValue("arff_type", m_arffType);
    xml += XMLHandler.addTagValue("date_format", m_dateFormat);
    xml += XMLHandler.addTagValue("nominal_vals", m_nominalVals);

    xml += ("</" + XML_TAG + ">");

    return xml;
  }

  /**
   * Save this ArffMeta to a repository
   * 
   * @param rep the repository to save to
   * @param id_transformation the transformation id
   * @param id_step the step id
   * @param nr the step number
   * @exception KettleException if an error occurs
   */
  public void saveRep(Repository rep, ObjectId id_transformation,
      ObjectId id_step, int nr) throws KettleException {

    rep.saveStepAttribute(id_transformation, id_step, nr, "field_name",
        m_fieldName);

    rep.saveStepAttribute(id_transformation, id_step, nr, "kettle_type",
        m_kettleType);
    rep.saveStepAttribute(id_transformation, id_step, nr, "arff_type",
        m_arffType);
    rep.saveStepAttribute(id_transformation, id_step, nr, "date_format",
        m_dateFormat);
    rep.saveStepAttribute(id_transformation, id_step, nr, "nominal_vals",
        m_nominalVals);
  }

  public static List<String> stringToVals(String vals) {
    List<String> nomVals = new ArrayList<String>();

    if (!Const.isEmpty(vals)) {
      String[] parts = vals.split(",");
      for (String p : parts) {
        nomVals.add(p.trim());
      }
    }

    return nomVals;
  }
}
