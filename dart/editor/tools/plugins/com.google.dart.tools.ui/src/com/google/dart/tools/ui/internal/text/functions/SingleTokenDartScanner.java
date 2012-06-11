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
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.text.IColorManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.rules.IRule;

import java.util.List;

/**
 *
 */
public final class SingleTokenDartScanner extends AbstractDartScanner {

  private final String[] fProperty;

  public SingleTokenDartScanner(IColorManager manager, IPreferenceStore store, String property) {
    super(manager, store);
    fProperty = new String[] {property};
    initialize();
  }

  /*
   * @see AbstractDartScanner#createRules()
   */
  @Override
  protected List<? extends IRule> createRules() {
    setDefaultReturnToken(getToken(fProperty[0]));
    return null;
  }

  /*
   * @see AbstractDartScanner#getTokenProperties()
   */
  @Override
  protected String[] getTokenProperties() {
    return fProperty;
  }
}
