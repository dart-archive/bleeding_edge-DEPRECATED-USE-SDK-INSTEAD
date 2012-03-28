/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.filesview;

import org.eclipse.osgi.util.NLS;

/**
 * Files view messages.
 */
public class FilesViewMessages extends NLS {
  private static final String BUNDLE_NAME = "com.google.dart.tools.ui.internal.filesview.FilesViewMessages"; //$NON-NLS-1$
  public static String IgnoreResourcesAction_do_analyze_label;
  public static String IgnoreResourcesAction_dont_analyze_label;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, FilesViewMessages.class);
  }

  private FilesViewMessages() {
  }
}
