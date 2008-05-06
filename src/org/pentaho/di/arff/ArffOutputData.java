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

package org.pentaho.di.arff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.RowDataUtil; 

import weka.core.Utils;

/**
 * Holds temporary data and has routines for writing
 * the ARFF file. This class writes rows to a temporary
 * file while, at the same time, collects values
 * for nominal attributes in an array of Maps. Once
 * the last row has been processed, the ARFF header
 * is written and then the temporary file is appended.
 *
 * @author Mark Hall (mhall{[at]}pentaho.org)
 * @version 1.0
 */
public class ArffOutputData extends BaseStepData 
  implements StepDataInterface {
  
  // the output data format
  protected RowMetaInterface m_outputRowMeta;

  // indexes of fields being output
  protected int[] m_outputFieldIndexes;

  // meta data for the ARFF fields
  protected ArffMeta[] m_arffMeta;

  // array of treemaps to hold nominal values
  protected Map<String, String>[] m_nominalVals;

  protected File m_tempFile;
  protected String m_headerFileName;
  protected OutputStream m_dataOut;
  protected OutputStream m_headerOut;

  protected byte[] m_separator = ",".getBytes();
  protected byte[] m_newLine = "\n".getBytes();
  protected byte[] m_missing = "?".getBytes();

  // Is there a specific character encoding being used?
  protected boolean m_hasEncoding;

  // byte buffer for fast copy
  static final int BUFF_SIZE = 100000;
  static final byte[] m_buffer = new byte[BUFF_SIZE];

  public ArffOutputData() {
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
   * Set whether an encoding is in use.
   *
   * @param e true if an encoding is in use
   */
  public void setHasEncoding(boolean e) {
    m_hasEncoding = e;
  }

  /**
   * Returns true if a specific character encoding
   * is in use.
   *
   * @return true if an encoding other than the default
   * encoding is in use.
   */
  public boolean getHasEncoding() {
    return m_hasEncoding;
  }

  /**
   * Set the binary line terminator to use
   *
   * @param nl the line terminator
   */
  public void setBinaryNewLine(byte[] nl) {
    m_newLine = nl;
  }
  
  /**
   * Set the binary separator to use
   *
   * @param s binary field separator
   */
  public void setBinarySeparator(byte[] s) {
    m_separator = s;
  }

  /**
   * Set the binary missing value to use
   *
   * @param m binary missing value
   */
  public void setBinaryMissing(byte[] m) {
    m_missing = m;
  }

  /**
   * Set the indexes of the fields to output to the ARFF file
   *
   * @param outputFieldIndexes array of indexes
   * @param arffMeta array of arff metas
   */
  public void setOutputFieldIndexes(int[] outputFieldIndexes,
                                    ArffMeta[] arffMeta) {
    m_outputFieldIndexes = outputFieldIndexes;
    m_arffMeta = arffMeta;

    // initialize any necesary tree maps
    m_nominalVals = (TreeMap<String, String>[])
      new TreeMap[m_outputFieldIndexes.length];
    for (int i = 0; i < m_outputFieldIndexes.length; i++) {
      if (m_outputFieldIndexes[i] >= 0) {
        if (m_arffMeta[i].getArffType() == ArffMeta.NOMINAL) {
          m_nominalVals[i] = new TreeMap<String, String>();
        }
      }
    }
  }

  /**
   * Open files ready to write to
   *
   * @param filename the name of the ARFF file to write to
   * @exception IOException if an error occurs
   */
  public void openFiles(String filename) throws IOException {
    m_headerFileName = filename;
    OutputStream os = new FileOutputStream(m_headerFileName);
    m_headerOut = new BufferedOutputStream(os);

    // tempfile to write the data to.
    // at the end of the stream we will write the
    // header to the requested file name and then
    // append this temp file to the end of it.
    String tempPrefix = "" + Math.random() + "arffOut";
    m_tempFile = File.createTempFile(tempPrefix, null);
    OutputStream os2 = new FileOutputStream(m_tempFile);
    m_dataOut = new BufferedOutputStream(os2);
  }


  /**
   * Convert a String to an array of bytes using an
   * (optional) encoding.
   *
   * @param encoding the character encoding to use
   * @param string the String to convert
   * @return the String as an array of bytes
   * @exception KettleValueException if an error occurs
   */
  private byte[] convertStringToBinaryString(String encoding,
                                             String string)
    throws KettleValueException {
    
    if (!getHasEncoding()) {
      return string.getBytes();
    }

    try {
      return string.getBytes(encoding);
    } catch(UnsupportedEncodingException e) {
      throw new KettleValueException("Unable to convert String to "
                                     + "Binary with specified string "
                                     + "encoding ["
                                     + encoding + "]", e);   
    }
  }

  /**
   * Convert and write a row of data to the ARFF file.
   *
   * @param r the Kettle row
   * @param encoding an (optional) character encoding to use
   * @exception IOException if an error occurs
   * @exception KettleStepException if an error occurs
   */
  public void writeRow(Object[] r, String encoding) 
    throws IOException, KettleStepException {
    // write data to temp file and update header hash
    // trees (if necessary)

    for (int i = 0; i < m_outputFieldIndexes.length; i++) {
      if (i != 0) {
        m_dataOut.write(m_separator);
      }
      if (m_outputFieldIndexes[i] >= 0) {
        writeField(i, r[m_outputFieldIndexes[i]], encoding);
      }
    }
    m_dataOut.write(m_newLine);
  }

  /**
   * Write a field to the ARFF file
   *
   * @param index the index of the field to write
   * @param value the actual Kettle value
   * @param encoding an (optional) character encoding to use
   * @exception KettleStepException if an error occurs
   */
  private void writeField(int index,
                          Object value,
                          String encoding) throws KettleStepException {
    try {
      ValueMetaInterface v = 
        m_outputRowMeta.getValueMeta(m_outputFieldIndexes[index]);
      
      byte[] str;
      
      str = formatField(index, v, value, encoding);

      //      m_data.write(m_enclosure);
      m_dataOut.write(str);
      //      m_data.write(m_enclosure);

    } catch (Exception ex) {
      throw new KettleStepException("Problem writing field content "
                                    + "to file", ex);
    }
    
  }
  
  /**
   * Format a Kettle value for writing.
   *
   * @param index the index of the value to format
   * @param v <code>ValueMetaInterface</code> for the field in
   * question
   * @param value the actual value
   * @param encoding an (optional) character encoding
   * @return the formatted value as an array of bytes
   * @exception KettleValueException if an error occurs
   */
  private byte [] formatField(int index, 
                              ValueMetaInterface v,
                              Object value,
                              String encoding) throws KettleValueException {

    // Check for missing value (null or empty string)
    // This seems to only consider empty string ("")
    // to be a null/missing value if the actual type
    // is String; for other types it returns false if
    // the value is "" (Kettle 3.0).
    if (v.isNull(value)) {
      return m_missing;
    }

    if(m_arffMeta[index].
       getKettleType() == ValueMetaInterface.TYPE_STRING) {
      String svalue = (value instanceof String)
        ? (String)value
        : v.getString(value);

      // enclose in quotes if necessary
      svalue = Utils.quote(svalue);

      // check to see if we've seen this value before, if not
      // then update the hash tree
      if (!m_nominalVals[index].containsKey(svalue)) {
        m_nominalVals[index].put(svalue, svalue);
      }
      //      return svalue.getBytes();
      return convertStringToBinaryString(encoding, Const.
                                         trimToType(svalue, 
                                                    v.getTrimType()));
    } else if (m_arffMeta[index].
               getKettleType() == ValueMetaInterface.TYPE_DATE) {
      // isNull bug workaround
      String temp = v.getString(value);
      if (temp == null || temp.length() == 0) {
        return m_missing;
      }
      temp = Utils.quote(temp);
      return convertStringToBinaryString(encoding, Const.
                                         trimToType(temp, 
                                                    v.getTrimType()));
    } else {
      // isNull bug workaround
      String temp = v.getString(value);
      if (temp == null || temp.length() == 0) {
        return m_missing;
      }
      return v.getBinaryString(value);
    }
  }


  /**
   * Writes the ARFF header and appends the temporary file
   *
   * @param relationName the ARFF relation name
   * @param encoding an (optional) character encoding
   * @exception KettleStepException if an error occurs
   */
  public void finishOutput(String relationName,
                           String encoding) throws KettleStepException { 

    if (m_headerOut == null) {
      // can't do anything
      return;
    }

    relationName = Utils.quote(relationName);
    relationName = "@relation " + relationName;
    byte[] rn = null;
    byte[] atAtt = null;
    byte[] atData = null;
    if (m_hasEncoding && encoding != null) {
      if (Const.isEmpty(encoding)) {
        rn = relationName.getBytes();
        atAtt = "@attribute ".getBytes();
        atData = "@data".getBytes();
      } else {
        try {
          rn = relationName.getBytes(encoding);
          atAtt = "@attribute ".getBytes(encoding);
          atData = "@data".getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
          throw new KettleStepException("Unable to write header with "
                                        + "specified string encoding ["
                                        + encoding + "]", e);
        }
      }
    } else {
      rn = relationName.getBytes();
      atAtt = "@attribute ".getBytes();
      atData = "@data".getBytes();
    }

    try {
      // write the header
      m_headerOut.write(rn);
      m_headerOut.write(m_newLine);
      
      // now write the attributes
      for (int i = 0; i < m_outputFieldIndexes.length; i++) {
        if (m_outputFieldIndexes[i] >= 0) {
          if (m_arffMeta[i].getArffType() == ArffMeta.NOMINAL) {
          m_headerOut.write(atAtt);
          writeBinaryNominalAttString(i, encoding);
          } else if (m_arffMeta[i].getArffType() == ArffMeta.NUMERIC) {
            m_headerOut.write(atAtt);
            writeBinaryNumericAttString(i, encoding);
          } else {
            m_headerOut.write(atAtt);
            writeBinaryDateAttString(i, encoding);
          }
        }
      }
      
      m_headerOut.write(atData);
      m_headerOut.write(m_newLine);

      m_dataOut.flush();
      m_dataOut.close();
    } catch (IOException ex) {
      throw new KettleStepException("Problem writing values to "
                                    +"file.", ex);
    } finally {
      try {
        closeFiles();
      } catch (IOException ex) {
        throw new KettleStepException("Problem closing files...", ex);
      }
    }

    // now append the temporary file to the header file
    InputStream is = null;
    OutputStream os = null;
    try {
      is = new FileInputStream(m_tempFile);
      // open the header file for appending
      os = new FileOutputStream(m_headerFileName, true);
      
      while (true) {
        synchronized (m_buffer) {
          int amountRead = is.read(m_buffer);
          if (amountRead == -1) {
            break;
          }
          os.write(m_buffer, 0, amountRead); 
        }
      } 
    } catch (IOException ex) {
      throw new KettleStepException("Problem copying temp file", ex);
    } finally {
      try {
        if (is != null) {
          is.close();
          // Try and clean up by deleting the temp file
          m_tempFile.delete();
        }
        if (os != null) {
          os.close();
        }
      } catch (IOException ex) {
        throw new KettleStepException("Problem closing files...", ex);
      }
    }
  }

  /**
   * Writes an attribute declaration for a numeric attribute
   *
   * @param index the index of the attribute/field
   * @param encoding an (optional) character encoding
   * @exception IOException if an error occurs
   * @exception KettleStepException if an error occurs
   */
  private void writeBinaryNumericAttString(int index, String encoding)
    throws IOException, KettleStepException {
    byte[] attName = null;
    byte[] attType = null;

    if (m_hasEncoding && encoding != null) {
      if (Const.isEmpty(encoding)) {
        attName = 
          Utils.quote(m_arffMeta[index].getFieldName()).getBytes();
        attType = " numeric".getBytes();
      } else {
        try {
          attName = 
            Utils.quote(m_arffMeta[index].getFieldName()).
            getBytes(encoding);
          attType = " numeric".getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
          throw new KettleStepException("Unable to write header with "
                                        + "specified string encoding ["
                                        + encoding + "]", e);
        }
      }
    } else {
      attName = 
        Utils.quote(m_arffMeta[index].getFieldName()).getBytes();
      attType = " numeric".getBytes();
    }

    m_headerOut.write(attName);
    m_headerOut.write(attType);
    m_headerOut.write(m_newLine);
  }

  /**
   * Writes an attribute declaration for a date attribute
   *
   * @param index the index of the attribute/field
   * @param encoding an (optional) character encoding
   * @exception IOException if an error occurs
   * @exception KettleStepException if an error occurs
   */
  private void writeBinaryDateAttString(int index, String encoding) 
    throws IOException, KettleStepException {
    byte[] attName = null;
    byte[] attType = null;
    byte[] dateFormat = null;

    ValueMetaInterface v =
      m_outputRowMeta.getValueMeta(m_outputFieldIndexes[index]);
    String dateF = v.getDateFormat().toPattern();
    dateF = Utils.quote(dateF);

    if (m_hasEncoding && encoding != null) {
      if (Const.isEmpty(encoding)) {
        attName = 
          Utils.quote(m_arffMeta[index].getFieldName()).getBytes();
        attType = " date ".getBytes();
        dateFormat = dateF.getBytes();
      } else {
        try {
          attName = 
            Utils.quote(m_arffMeta[index].getFieldName()).
            getBytes(encoding);
          attType = " date ".getBytes(encoding);
          dateFormat = dateF.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
          throw new KettleStepException("Unable to write header with "
                                        + "specified string encoding ["
                                        + encoding + "]", e);
        }
      }
    } else {
      attName = 
        Utils.quote(m_arffMeta[index].getFieldName()).getBytes();
      attType = " date ".getBytes();
      dateFormat = dateF.getBytes();      
    }

    m_headerOut.write(attName);
    m_headerOut.write(attType);
    m_headerOut.write(dateFormat);
    m_headerOut.write(m_newLine);
  }

  /**
   * Writes an attribute declaration for a nominal attribute
   *
   * @param index the index of the attribute/field
   * @param encoding an (optional) character encoding
   * @exception IOException if an error occurs
   * @exception KettleStepException if an error occurs
   */
  private void writeBinaryNominalAttString(int index, String encoding) 
    throws IOException, KettleStepException {
    byte[] attName = null;
    byte[] lcurly = null;
    byte[] rcurly = null;

    if (m_hasEncoding && encoding != null) {
      if (Const.isEmpty(encoding)) {
        attName = 
          Utils.quote(m_arffMeta[index].getFieldName()).getBytes();
        lcurly = " {".getBytes();
        rcurly = "}".getBytes();
      } else {
        try {
          attName = 
            Utils.quote(m_arffMeta[index].getFieldName()).
              getBytes(encoding);
          lcurly = " {".getBytes(encoding);
          rcurly = "}".getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
          throw new KettleStepException("Unable to write header with "
                                        + "specified string encoding ["
                                        + encoding + "]", e);
        }
      }
    } else {
      attName = 
        Utils.quote(m_arffMeta[index].getFieldName()).getBytes();
      lcurly = " {".getBytes();
      rcurly = "}".getBytes();
    }
    m_headerOut.write(attName);
    m_headerOut.write(lcurly);

    // get keys from corresponding hash tree
    Set<String> keySet = m_nominalVals[index].keySet();
    Iterator<String> ksi = keySet.iterator();
    byte[] nomVal = null;
    while (ksi.hasNext()) {
      String next = ksi.next();
      // value was quoted when added into hash table.
      //      next = Utils.quote(next);
      if (m_hasEncoding && encoding != null) {
        if (Const.isEmpty(encoding)) {
          nomVal = next.getBytes();
        } else {
          nomVal = next.getBytes(encoding);
        }
      } else {
        nomVal = next.getBytes();
      }
      
      m_headerOut.write(nomVal);
      if (ksi.hasNext()) {
        m_headerOut.write(m_separator);
      }
    }
    
    m_headerOut.write(rcurly);
    m_headerOut.write(m_newLine);
  }

  /**
   * Flush and close all files
   *
   * @exception IOException if an error occurs
   */
  public void closeFiles() throws IOException {
    if (m_dataOut != null) {
      m_dataOut.flush();
      m_dataOut.close();
      m_dataOut = null;
    }

    if (m_headerOut != null) {
      m_headerOut.flush();
      m_headerOut.close();
      m_headerOut = null;
    }
  }
}