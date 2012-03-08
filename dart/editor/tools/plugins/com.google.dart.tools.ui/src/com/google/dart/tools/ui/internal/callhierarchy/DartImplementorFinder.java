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

import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeHierarchy;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchException;
import com.google.dart.tools.core.search.SearchFilter;
import com.google.dart.tools.core.search.SearchListener;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.IProgressMonitor;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class DartImplementorFinder implements IImplementorFinder {

  private class ImplementorsSearchListener implements SearchListener {
    Collection<Type> implementors = new HashSet<Type>();

    @Override
    public void matchFound(SearchMatch match) {
      if (match.getElement() instanceof Type) {
        implementors.add((Type) match.getElement());
      }
    }

    @Override
    public void searchComplete() {
      // Ignored
    }
  }

  @Override
  public Collection<Type> findImplementingTypes(Type type, IProgressMonitor progressMonitor) {
    SearchEngine searchEngine = SearchEngineFactory.createSearchEngine();
    SearchScope scope = CallHierarchy.getDefault().getSearchScope();
    ImplementorsSearchListener listener = new ImplementorsSearchListener();
    SearchFilter filter = null;
    try {
      searchEngine.searchImplementors(type, scope, filter, listener, progressMonitor);
    } catch (SearchException ex) {
      DartToolsPlugin.log(ex);
    }
    return listener.implementors;
  }

  @Override
  public Collection<Type> findInterfaces(Type type, IProgressMonitor progressMonitor) {
    try {
      TypeHierarchy typeHierarchy = type.newSupertypeHierarchy(progressMonitor);
      Type[] interfaces = typeHierarchy.getAllSuperInterfaces(type);
      HashSet<Type> result = new HashSet<Type>(Arrays.asList(interfaces));
      return result;
    } catch (DartModelException e) {
      DartToolsPlugin.log(e);
    }
    return null;
  }
}
