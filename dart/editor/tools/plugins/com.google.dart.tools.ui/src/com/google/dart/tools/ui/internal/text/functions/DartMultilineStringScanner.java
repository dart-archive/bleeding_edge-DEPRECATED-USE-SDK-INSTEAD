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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.text.IColorManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * A scanner for multi-line strings
 */
public class DartMultilineStringScanner extends AbstractDartScanner {

  private final String defaultTokenProperty;
  private final String[] property;

  /**
   * @param manager
   * @param store
   */
  public DartMultilineStringScanner(IColorManager manager, IPreferenceStore store,
      String defaultTokenProperty) {
    super(manager, store);
    this.defaultTokenProperty = defaultTokenProperty;
    property = new String[] {defaultTokenProperty};
    initialize();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.dart.tools.ui.internal.text.functions.AbstractDartScanner#createRules()
   */
  @Override
  protected List<MultiLineRule> createRules() {
    Token defaultToken = getToken(defaultTokenProperty);
    List<MultiLineRule> list = new ArrayList<MultiLineRule>();

    list.add(new MultiLineRule("\"\"\"", "\"\"\"", defaultToken)); //$NON-NLS-2$ //$NON-NLS-1$
    list.add(new MultiLineRule("'''", "'''", defaultToken)); //$NON-NLS-2$ //$NON-NLS-1$

    setDefaultReturnToken(defaultToken);
    return list;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.google.dart.tools.ui.internal.text.functions.AbstractDartScanner#getTokenProperties()
   */
  @Override
  protected String[] getTokenProperties() {

    return property;
  }

}
