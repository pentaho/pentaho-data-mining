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
* Copyright 2006 - 2016 Pentaho Corporation.  All rights reserved.
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
