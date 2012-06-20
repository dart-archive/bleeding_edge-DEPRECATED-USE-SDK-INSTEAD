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
package com.google.dart.tools.ui.internal.callhierarchy;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.core.search.SearchScopeFactory;
import com.google.dart.tools.core.utilities.compiler.DartCompilerUtilities;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.internal.util.StringMatcher;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class CallHierarchy {
  private static final String PREF_USE_IMPLEMENTORS = "PREF_USE_IMPLEMENTORS"; //$NON-NLS-1$
  private static final String PREF_USE_FILTERS = "PREF_USE_FILTERS"; //$NON-NLS-1$
  private static final String PREF_FILTERS_LIST = "PREF_FILTERS_LIST"; //$NON-NLS-1$

  private static final String DEFAULT_IGNORE_FILTERS = ""; //$NON-NLS-1$
  private static CallHierarchy SINGLETON;

  public static boolean arePossibleInputElements(List<?> elements) {
    if (elements.size() < 1) {
      return false;
    }
    for (Iterator<?> iter = elements.iterator(); iter.hasNext();) {
      if (!isPossibleInputElement(iter.next())) {
        return false;
      }
    }
    return true;
  }

  public static CallLocation getCallLocation(Object element) {
    CallLocation callLocation = null;

    if (element instanceof MethodWrapper) {
      MethodWrapper methodWrapper = (MethodWrapper) element;
      MethodCall methodCall = methodWrapper.getMethodCall();

      if (methodCall != null) {
        callLocation = methodCall.getFirstCallLocation();
      }
    } else if (element instanceof CallLocation) {
      callLocation = (CallLocation) element;
    }

    return callLocation;
  }

  public static CallHierarchy getDefault() {
    if (SINGLETON == null) {
      SINGLETON = new CallHierarchy();
    }
    return SINGLETON;
  }

  public static boolean isPossibleInputElement(Object element) {
    return element instanceof CompilationUnitElement;
  }

  static DartUnit getCompilationUnitNode(CompilationUnitElement member, boolean resolveBindings) {
    CompilationUnit typeRoot = member.getCompilationUnit();
    try {
      if (typeRoot.exists() && typeRoot.getBuffer() != null) {
        if (resolveBindings) {
          return DartCompilerUtilities.resolveUnit(typeRoot);
        } else {
          return DartCompilerUtilities.parseUnit(typeRoot);
        }
      }
    } catch (DartModelException e) {
      DartToolsPlugin.log(e);
    }
    return null;
  }

  /**
   * Parses the comma separated string into an array of {@link StringMatcher} objects.
   * 
   * @param listString the string to parse
   * @return an array of {@link StringMatcher} objects
   */
  private static StringMatcher[] parseList(String listString) {
    List<StringMatcher> list = new ArrayList<StringMatcher>(10);
    StringTokenizer tokenizer = new StringTokenizer(listString, ","); //$NON-NLS-1$

    while (tokenizer.hasMoreTokens()) {
      String textFilter = tokenizer.nextToken().trim();
      list.add(new StringMatcher(textFilter, false, false));
    }

    return list.toArray(new StringMatcher[list.size()]);
  }

  private SearchScope searchScope;
  private StringMatcher[] filters;

  private CallHierarchy() {
  }

  public MethodWrapper[] getCalleeRoots(DartElement[] members) {
    return getRoots(members, false);
  }

  public MethodWrapper[] getCallerRoots(DartElement[] members) {
    return getRoots(members, true);
  }

  /**
   * Returns the current filters as a string.
   * 
   * @return returns the filters
   */
  public String getFilters() {
    IPreferenceStore settings = DartToolsPlugin.getDefault().getPreferenceStore();

    return settings.getString(PREF_FILTERS_LIST);
  }

  public Collection<DartElement> getImplementingMethods(Method method) {
    if (isSearchUsingImplementorsEnabled()) {
      DartElement[] result = Implementors.getInstance().searchForImplementors(
          new DartElement[] {method},
          new NullProgressMonitor());

      if ((result != null) && (result.length > 0)) {
        return Arrays.asList(result);
      }
    }

    return new ArrayList<DartElement>(0);
  }

  public Collection<DartElement> getInterfaceMethods(Method method) {
    if (isSearchUsingImplementorsEnabled()) {
      DartElement[] result = Implementors.getInstance().searchForInterfaces(
          new DartElement[] {method},
          new NullProgressMonitor());

      if ((result != null) && (result.length > 0)) {
        return Arrays.asList(result);
      }
    }

    return new ArrayList<DartElement>(0);
  }

  public SearchScope getSearchScope() {
    if (searchScope == null) {
      searchScope = SearchScopeFactory.createWorkspaceScope();
    }

    return searchScope;
  }

  public boolean isFilterEnabled() {
    IPreferenceStore settings = DartToolsPlugin.getDefault().getPreferenceStore();
    return settings.getBoolean(PREF_USE_FILTERS);
  }

  /**
   * Checks whether the fully qualified name is ignored by the set filters.
   * 
   * @param fullyQualifiedName the fully qualified name
   * @return <code>true</code> if the fully qualified name is ignored
   */
  public boolean isIgnored(String fullyQualifiedName) {
    if ((getIgnoreFilters() != null) && (getIgnoreFilters().length > 0)) {
      for (int i = 0; i < getIgnoreFilters().length; i++) {
        String fullyQualifiedName1 = fullyQualifiedName;

        if (getIgnoreFilters()[i].match(fullyQualifiedName1)) {
          return true;
        }
      }
    }

    return false;
  }

  public boolean isSearchUsingImplementorsEnabled() {
    IPreferenceStore settings = DartToolsPlugin.getDefault().getPreferenceStore();

    return settings.getBoolean(PREF_USE_IMPLEMENTORS);
  }

  public void setFilterEnabled(boolean filterEnabled) {
    IPreferenceStore settings = DartToolsPlugin.getDefault().getPreferenceStore();
    settings.setValue(PREF_USE_FILTERS, filterEnabled);
  }

  public void setFilters(String filters) {
    this.filters = null;

    IPreferenceStore settings = DartToolsPlugin.getDefault().getPreferenceStore();
    settings.setValue(PREF_FILTERS_LIST, filters);
  }

  public void setSearchScope(SearchScope searchScope) {
    this.searchScope = searchScope;
  }

  public void setSearchUsingImplementorsEnabled(boolean enabled) {
    IPreferenceStore settings = DartToolsPlugin.getDefault().getPreferenceStore();

    settings.setValue(PREF_USE_IMPLEMENTORS, enabled);
  }

  private void addRoot(DartElement member, ArrayList<MethodWrapper> roots, boolean callers) {
    MethodCall methodCall = new MethodCall(member);
    MethodWrapper root;
    if (callers) {
      root = new CallerMethodWrapper(null, methodCall);
    } else {
      root = new CalleeMethodWrapper(null, methodCall);
    }
    roots.add(root);
  }

  /**
   * Returns filters for packages which should not be included in the search results.
   * 
   * @return StringMatcher[]
   */
  private StringMatcher[] getIgnoreFilters() {
    if (filters == null) {
      String filterString = null;

      if (isFilterEnabled()) {
        filterString = getFilters();

        if (filterString == null) {
          filterString = DEFAULT_IGNORE_FILTERS;
        }
      }

      if (filterString != null) {
        filters = parseList(filterString);
      } else {
        filters = null;
      }
    }

    return filters;
  }

  private MethodWrapper[] getRoots(DartElement[] members, boolean callers) {
    ArrayList<MethodWrapper> roots = new ArrayList<MethodWrapper>();
    for (int i = 0; i < members.length; i++) {
      DartElement member = members[i];
      if (member instanceof Type) {
        Type type = (Type) member;
        try {
          Method[] constructors = DartModelUtil.getConstructorsOfType(type);
          if (constructors.length == 0) {
            addRoot(member, roots, callers); // IType is a stand-in for the non-existing default constructor
          } else {
            for (int j = 0; j < constructors.length; j++) {
              Method constructor = constructors[j];
              addRoot(constructor, roots, callers);
            }
          }
        } catch (DartModelException e) {
          DartToolsPlugin.log(e);
        }
      } else {
        addRoot(member, roots, callers);
      }
    }
    return roots.toArray(new MethodWrapper[roots.size()]);
  }
}
