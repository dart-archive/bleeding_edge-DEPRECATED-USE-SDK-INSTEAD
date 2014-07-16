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
package com.google.dart.tools.ui.internal.util;

import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.PreferenceConstants;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import java.util.StringTokenizer;

public class TypeFilter implements IPropertyChangeListener {

  public static TypeFilter getDefault() {
    return DartToolsPlugin.getDefault().getTypeFilter();
  }

  public static boolean isFiltered(char[] fullTypeName) {
    return getDefault().filter(new String(fullTypeName));
  }

//public static boolean isFiltered(TypeNameMatch match) {
//  return getDefault().filter(match.getFullyQualifiedName());
//}

  public static boolean isFiltered(String fullTypeName) {
    return getDefault().filter(fullTypeName);
  }

  private StringMatcher[] fStringMatchers;

  public TypeFilter() {
    DartX.todo();
    fStringMatchers = null;
    PreferenceConstants.getPreferenceStore().addPropertyChangeListener(this);
  }

  public void dispose() {
    PreferenceConstants.getPreferenceStore().removePropertyChangeListener(this);
    fStringMatchers = null;
  }

  public boolean filter(String fullTypeName) {
    StringMatcher[] matchers = getStringMatchers();
    for (int i = 0; i < matchers.length; i++) {
      StringMatcher curr = matchers[i];
      if (curr.match(fullTypeName)) {
        return true;
      }
    }
    return false;
  }

  public boolean hasFilters() {
    return getStringMatchers().length > 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse
   * .jface.util.PropertyChangeEvent)
   */
  @Override
  public synchronized void propertyChange(PropertyChangeEvent event) {
    if (PreferenceConstants.TYPEFILTER_ENABLED.equals(event.getProperty())) {
      fStringMatchers = null;
    }
  }

  private synchronized StringMatcher[] getStringMatchers() {
    if (fStringMatchers == null) {
      String str = PreferenceConstants.getPreferenceStore().getString(
          PreferenceConstants.TYPEFILTER_ENABLED);
      StringTokenizer tok = new StringTokenizer(str, ";"); //$NON-NLS-1$
      int nTokens = tok.countTokens();

      fStringMatchers = new StringMatcher[nTokens];
      for (int i = 0; i < nTokens; i++) {
        String curr = tok.nextToken();
        if (curr.length() > 0) {
          fStringMatchers[i] = new StringMatcher(curr, false, false);
        }
      }
    }
    return fStringMatchers;
  }

}
