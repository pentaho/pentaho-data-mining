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
 *    ArffMeta.java
 *    Copyright 2007 Pentaho Corporation.  All rights reserved. 
 *
 */

package org.pentaho.di.arff;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;

import org.w3c.dom.Node;

/**
 * Contains the meta data for one field being output
 * to the ARFF file
 *
 * @author Mark Hall (mhall{[at]}pentaho.org)
 * @version 1.0
 */
public class ArffMeta implements Cloneable {

  // some constants for ARFF data types
  public static int NUMERIC = 0;
  public static int NOMINAL = 1;
  public static int DATE = 2;

  // the name of this field
  private String m_fieldName;

  // the Kettle data type (as defined in ValueMetaInterface)
  private int m_kettleType;

  // the ARFF data type (as defined by the constants above)
  private int m_arffType;

  public static final String XML_TAG = "arff_field";
  
  /**
   * Constructor
   *
   * @param fieldName the name of this field
   * @param kettleType the Kettle data type
   * @param arffType the ARFF data type
   */
  public ArffMeta(String fieldName, 
                  int kettleType, 
                  int arffType) {

    m_fieldName = fieldName;
    m_kettleType = kettleType;
    m_arffType = arffType;
    //    m_precision = precision;
  }

  /**
   * Construct using data stored in repository
   *
   * @param rep the repository
   * @param id_step the id of the step
   * @param nr the step number
   * @exception KettleException if an error occurs
   */
  public ArffMeta(Repository rep,
                  ObjectId id_step,
                  int nr) throws KettleException {
    m_fieldName = 
      rep.getStepAttributeString(id_step, nr, "field_name");
    m_kettleType = 
      (int)rep.getStepAttributeInteger(id_step, nr, "kettle_type");
    m_arffType = 
      (int)rep.getStepAttributeInteger(id_step, nr, "arff_type");
  }

  /**
   * Construct from an XML node
   *
   * @param uniNode a XML node
   */
  public ArffMeta(Node arffNode) {
    String temp;
    m_fieldName = 
      XMLHandler.getTagValue(arffNode, "field_name");
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


  /**
   * Make a copy
   *
   * @return a copy of this UnivariateStatsMetaFunction.
   */
  public Object clone() {
    try {
      ArffMeta retval = 
        (ArffMeta) super.clone();

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
                      ObjectId id_step,
                      int nr) throws KettleException {

    rep.saveStepAttribute(id_transformation, id_step, nr, 
                          "field_name",
                          m_fieldName);

    rep.saveStepAttribute(id_transformation, id_step, nr, 
                          "kettle_type",
                          m_kettleType);
    rep.saveStepAttribute(id_transformation, id_step, nr, 
                          "arff_type",
                          m_arffType);
  }
}
