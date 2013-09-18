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
 *    LogAdapter.java
 *    Copyright 2008 Pentaho Corporation.  All rights reserved. 
 *
 */

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

  public LogAdapter(LogChannelInterface log) {
    m_log = log;
  }

  public void statusMessage(String message) {
    m_log.logDetailed(message);
  }

  public void logMessage(String message) {
    m_log.logBasic(message);
  }
}
