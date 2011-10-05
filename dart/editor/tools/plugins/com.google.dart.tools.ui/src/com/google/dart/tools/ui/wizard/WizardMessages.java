/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.wizard;

import org.eclipse.osgi.util.NLS;

public class WizardMessages extends NLS {
  private static final String BUNDLE_NAME = "com.google.dart.tools.ui.wizard.WizardMessages"; //$NON-NLS-1$
  public static String NewApplicationWizard_title;
  public static String NewApplicationWizardPage_browse;
  public static String NewApplicationWizardPage_directory;
  public static String NewApplicationWizardPage_message;
  public static String NewApplicationWizardPage_name;
  public static String NewApplicationWizardPage_title;
  public static String NewFileWizard_newFile;
  public static String NewFileWizardPage_browse;
  public static String NewFileWizardPage_directory;
  public static String NewFileWizardPage_title;
  public static String NewFileWizardPage_fileNameLabel;
  public static String NewFileWizardPage_newDartFile;
  public static String NewFileWizardPage_newHTMLFile;
  public static String NewFileWizardPage_newCSSFile;
  public static String NewFileWizardPage_newEmptyFile;
  public static String NewFileWizardPage_status1;
  public static String NewFileWizardPage_status2;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, WizardMessages.class);
  }

  private WizardMessages() {
  }
}
