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
* Copyright 2006 - 2017 Pentaho Corporation.  All rights reserved.
*/

package org.pentaho.di.scoring;

import org.pentaho.di.core.annotations.KettleLifecyclePlugin;
import org.pentaho.di.core.lifecycle.KettleLifecycleListener;
import org.pentaho.di.core.lifecycle.LifecycleException;

@KettleLifecyclePlugin( id = "WekaScoringLifecycleListener", name = "WekaScoringLifecycleListener" )
public class WekaScoringLifecycleListener implements KettleLifecycleListener {

  @Override public void onEnvironmentInit() throws LifecycleException {
    System.setProperty( "weka.core.logging.Logger", "weka.core.logging.ConsoleLogger" );
    weka.core.WekaPackageManager.loadPackages( false );
  }

  @Override public void onEnvironmentShutdown() {
    // Noop
  }

}
