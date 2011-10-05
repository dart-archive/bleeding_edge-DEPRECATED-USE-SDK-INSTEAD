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
package com.google.dart.tools.ui.internal.text.folding;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class FoldingMessages extends NLS {

  private static final String BUNDLE_NAME = FoldingMessages.class.getName();

  public static String DefaultJavaFoldingPreferenceBlock_title;

  public static String DefaultJavaFoldingPreferenceBlock_comments;
  //	public static String DefaultJavaFoldingPreferenceBlock_innerTypes;
  public static String DefaultJavaFoldingPreferenceBlock_methods;
//	public static String DefaultJavaFoldingPreferenceBlock_imports;
  public static String DefaultJavaFoldingPreferenceBlock_headers;
  public static String EmptyJavaFoldingPreferenceBlock_emptyCaption;
  public static String JavaFoldingStructureProviderRegistry_warning_providerNotFound_resetToDefault;
  static {
    NLS.initializeMessages(BUNDLE_NAME, FoldingMessages.class);
  }

  private FoldingMessages() {
    // Do not instantiate
  }
}
