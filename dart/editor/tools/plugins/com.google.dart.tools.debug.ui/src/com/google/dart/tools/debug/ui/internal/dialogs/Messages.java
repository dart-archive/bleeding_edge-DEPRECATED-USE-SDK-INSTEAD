/*
 * Copyright 2012 Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.tools.debug.ui.internal.dialogs;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.google.dart.tools.debug.ui.internal.dialogs.messages"; //$NON-NLS-1$

  public static String ManageLaunchesDialog_createLaunch;
  public static String ManageLaunchesDialog_manageLaunches;
  public static String ManageLaunchesDialog_launchRun;
  public static String ManageLaunchesDialog_Name_required_for_launch_configuration;
  public static String ManageLaunchesDialog_Launch_configuration_already_exists_with_this_name;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {

  }

}
