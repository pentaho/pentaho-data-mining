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
