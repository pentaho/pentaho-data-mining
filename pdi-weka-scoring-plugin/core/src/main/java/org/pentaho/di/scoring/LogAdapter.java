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

package org.pentaho.di.scoring;

import java.io.Serializable;

import org.pentaho.di.core.logging.LogChannelInterface;

import weka.gui.Logger;

/**
 * Adapts Kettle logging to Weka's Logger interface
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}org)
 * @version $Revision: 1.0 $
 */
public class LogAdapter implements Serializable, Logger {

  /**
   * For serialization
   */
  private static final long serialVersionUID = 4861213857483800216L;

  private transient LogChannelInterface m_log;

  public LogAdapter( LogChannelInterface log ) {
    m_log = log;
  }

  public void statusMessage( String message ) {
    m_log.logDetailed( message );
  }

  public void logMessage( String message ) {
    m_log.logBasic( message );
  }
}
