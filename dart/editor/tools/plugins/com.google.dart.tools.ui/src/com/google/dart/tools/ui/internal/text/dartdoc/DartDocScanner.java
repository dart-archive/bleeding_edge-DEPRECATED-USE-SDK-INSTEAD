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
package com.google.dart.tools.ui.internal.text.dartdoc;

import com.google.dart.tools.ui.internal.text.functions.DartCommentScanner;
import com.google.dart.tools.ui.text.IColorManager;
import com.google.dart.tools.ui.text.IDartColorConstants;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.IPreferenceStore;

// TODO write this scanner
public class DartDocScanner extends DartCommentScanner {

  private static String[] fgTokenProperties = {
      IDartColorConstants.JAVADOC_KEYWORD, IDartColorConstants.JAVADOC_TAG,
      IDartColorConstants.JAVADOC_LINK, IDartColorConstants.JAVADOC_DEFAULT, TASK_TAG};

  public DartDocScanner(IColorManager manager, IPreferenceStore store) {
    this(manager, store, null);
  }

  public DartDocScanner(IColorManager manager, IPreferenceStore store, Preferences coreStore) {
    super(manager, store, coreStore, IDartColorConstants.JAVADOC_DEFAULT, fgTokenProperties);
  }

}
