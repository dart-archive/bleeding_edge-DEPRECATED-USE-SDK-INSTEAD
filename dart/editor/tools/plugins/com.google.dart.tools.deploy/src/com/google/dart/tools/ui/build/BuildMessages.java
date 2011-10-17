/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.build;

import org.eclipse.osgi.util.NLS;

/**
 * I18N strings for the build package.
 */
public class BuildMessages extends NLS {
  private static final String BUNDLE_NAME = "com.google.dart.tools.ui.build.BuildMessages"; //$NON-NLS-1$

  public static String CleanLibrariesAction_rebuildAll;
  public static String CleanLibrariesJob_cleanLibrariesProgress;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, BuildMessages.class);
  }

  private BuildMessages() {

  }

}
