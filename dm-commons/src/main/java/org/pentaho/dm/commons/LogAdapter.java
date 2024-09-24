/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/

/*
 *    LogAdapter.java
 *    Copyright 2008 - 2017 Hitachi Vantara.  All rights reserved. 
 *
 */
 
package org.pentaho.dm.commons;

import java.io.PrintStream;
import java.io.Serializable;

import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.logging.MetricsInterface;
import org.pentaho.di.core.logging.LogLevel;

import weka.gui.Logger;

/**
 * Adapts Kettle logging to Weka's Logger interface and PrintStream.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: 1.0 $
 */
public class LogAdapter extends PrintStream 
  implements Serializable, Logger, LogChannelInterface {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 4861213857483800216L;
  
  private transient LogChannelInterface m_log;

  public LogAdapter(LogChannelInterface log) {
    super(System.out);
    m_log = log;
  }

  /**
   * Weka Logger method
   * 
   * @param message log message for the status area
   */
  public void statusMessage(String message) {
    m_log.logDetailed(message);
  }

  /**
   * Weka Logger method
   * 
   * @param message log message for the log area
   */
  public void logMessage(String message) {
    m_log.logBasic(message);
  }
  
  /**
   * PrintStream method
   * 
   * @param string the log message
   */
  public void println(String string) {
    // make sure that the global weka log picks it up
    System.out.println(string);    
    statusMessage(string);
  }
  
  /**
   * PrintStream method
   * 
   * @param obj the log message
   */
  public void println(Object obj) {
    println(obj.toString());
  }
  
  /**
   * PrintStream method
   * 
   * @param string the log message
   */
  public void print(String string){
    // make sure that the global weka log picks it up
    System.out.print(string);
    statusMessage(string);      
  }
  
  /**
   * PrintStream method
   * 
   * @param obj the log message
   */
  public void print(Object obj) {
    print(obj.toString());
  }

  @Override
  public String getContainerObjectId() {
    return m_log.getContainerObjectId();
  }

  @Override
  public String getLogChannelId() {
    return m_log.getLogChannelId();
  }

  @Override
  public LogLevel getLogLevel() {
    return m_log.getLogLevel();
  }

  @Override
  public boolean isBasic() {
    return m_log.isBasic();
  }

  @Override
  public boolean isDebug() {
    return m_log.isDebug();
  }

  @Override
  public boolean isDetailed() {
    return m_log.isDetailed();
  }

  @Override
  public boolean isError() {
    return m_log.isError();
  }

  @Override
  public boolean isRowLevel() {
    return m_log.isRowLevel();
  }

  @Override
  public void logBasic(String arg0) {
    m_log.logBasic(arg0);
  }

  @Override
  public void logBasic(String arg0, Object... arg1) {
    m_log.logBasic(arg0, arg1);    
  }

  @Override
  public void logDebug(String arg0) {
    m_log.logDebug(arg0);
  }

  @Override
  public void logDebug(String arg0, Object... arg1) {
    m_log.logDebug(arg0, arg1);
  }

  @Override
  public void logDetailed(String arg0) {
    m_log.logDetailed(arg0);
  }

  @Override
  public void logDetailed(String arg0, Object... arg1) {
    m_log.logDetailed(arg0, arg1);
  }

  @Override
  public void logError(String arg0) {
    m_log.logError(arg0);
  }

  @Override
  public void logError(String arg0, Throwable arg1) {
    m_log.logError(arg0, arg1);    
  }

  @Override
  public void logError(String arg0, Object... arg1) {
    m_log.logError(arg0, arg1);
  }

  @Override
  public void logMinimal(String arg0) {
    m_log.logMinimal(arg0);
  }

  @Override
  public void logMinimal(String arg0, Object... arg1) {
    m_log.logMinimal(arg0, arg1);
  }

  @Override
  public void logRowlevel(String arg0) {
    m_log.logRowlevel(arg0);
  }

  @Override
  public void logRowlevel(String arg0, Object... arg1) {
    m_log.logRowlevel(arg0, arg1);
  }

  @Override
  public void setContainerObjectId(String arg0) {
    m_log.setContainerObjectId(arg0);    
  }

  @Override
  public void setLogLevel(LogLevel arg0) {
    m_log.setLogLevel(arg0);    
  }

  @Override
  public void snap(MetricsInterface metric, long... value) {
    m_log.snap(metric, value);
  }

  @Override
  public void snap(MetricsInterface metric, String subject, long... value) {
    m_log.snap(metric, subject, value);
  }

  @Override
  public boolean isForcingSeparateLogging() {
    return m_log.isForcingSeparateLogging();
  }

  @Override
  public void setForcingSeparateLogging(boolean forcingSeparateLogging) {
    m_log.setForcingSeparateLogging(forcingSeparateLogging);
  }

  @Override
  public boolean isGatheringMetrics() {
    return m_log.isGatheringMetrics();
  }

  @Override
  public void setGatheringMetrics(boolean gatheringMetrics) {
    m_log.setGatheringMetrics(gatheringMetrics);
  }

  @Override
  public String getFilter() {
    return m_log.getFilter();
  }

  @Override
  public void setFilter(String filter) {
    m_log.setFilter(filter);
  }
}
