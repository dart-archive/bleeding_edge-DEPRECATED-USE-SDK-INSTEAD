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
package com.google.dart.tools.core.internal.search;

import com.google.dart.tools.core.model.DartClassTypeAlias;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchException;
import com.google.dart.tools.core.search.SearchFilter;
import com.google.dart.tools.core.search.SearchListener;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.search.SearchPattern;
import com.google.dart.tools.core.search.SearchScope;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.List;

/**
 * Instances of the class <code>SearchEngineImpl</code> implement a search engine that uses the new
 * index to obtain results.
 */
public class SearchEngineImpl implements SearchEngine {

  @Override
  public List<SearchMatch> searchFunctionDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove
    return null;
  }

  @Override
  public void searchFunctionDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    //TODO(pquitslund): remove

  }

  @Override
  public List<SearchMatch> searchImplementors(Type type, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove
    return null;
  }

  @Override
  public void searchImplementors(Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove

  }

  @Override
  public List<SearchMatch> searchReferences(DartClassTypeAlias type, SearchScope scope,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove
    return null;
  }

  @Override
  public void searchReferences(DartClassTypeAlias type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove

  }

  @Override
  public List<SearchMatch> searchReferences(DartFunction function, SearchScope scope,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove
    return null;
  }

  @Override
  public void searchReferences(DartFunction function, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove

  }

  @Override
  public List<SearchMatch> searchReferences(DartFunctionTypeAlias alias, SearchScope scope,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove
    return null;
  }

  @Override
  public void searchReferences(DartFunctionTypeAlias alias, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove

  }

  @Override
  public List<SearchMatch> searchReferences(DartImport imprt, SearchScope scope,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove
    return null;
  }

  @Override
  public void searchReferences(DartImport imprt, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove

  }

  @Override
  public List<SearchMatch> searchReferences(DartVariableDeclaration variable, SearchScope scope,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove
    return null;
  }

  @Override
  public void searchReferences(DartVariableDeclaration variable, SearchScope scope,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    //TODO(pquitslund): remove

  }

  @Override
  public List<SearchMatch> searchReferences(Field field, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove
    return null;
  }

  @Override
  public void searchReferences(Field field, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove

  }

  @Override
  public List<SearchMatch> searchReferences(IFile file, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove
    return null;
  }

  @Override
  public void searchReferences(IFile file, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove

  }

  @Override
  public List<SearchMatch> searchReferences(Method method, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove
    return null;
  }

  @Override
  public void searchReferences(Method method, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove

  }

  @Override
  public List<SearchMatch> searchReferences(Type type, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove
    return null;
  }

  @Override
  public void searchReferences(Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove

  }

  @Override
  public List<SearchMatch> searchSubtypes(Type type, SearchScope scope, SearchFilter filter,
      IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove
    return null;
  }

  @Override
  public void searchSubtypes(Type type, SearchScope scope, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove

  }

  @Override
  public List<SearchMatch> searchTypeDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove
    return null;
  }

  @Override
  public void searchTypeDeclarations(SearchScope scope, SearchPattern pattern, SearchFilter filter,
      SearchListener listener, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove

  }

  @Override
  public List<SearchMatch> searchVariableDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, IProgressMonitor monitor) throws SearchException {
    //TODO(pquitslund): remove
    return null;
  }

  @Override
  public void searchVariableDeclarations(SearchScope scope, SearchPattern pattern,
      SearchFilter filter, SearchListener listener, IProgressMonitor monitor)
      throws SearchException {
    //TODO(pquitslund): remove

  }

}
