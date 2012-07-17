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

import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.search.MatchKind;
import com.google.dart.tools.core.search.MatchQuality;
import com.google.dart.tools.core.search.SearchEngine;
import com.google.dart.tools.core.search.SearchEngineFactory;
import com.google.dart.tools.core.search.SearchException;
import com.google.dart.tools.core.search.SearchFilter;
import com.google.dart.tools.core.search.SearchMatch;
import com.google.dart.tools.core.search.SearchScope;
import com.google.dart.tools.core.search.SearchScopeFactory;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallerMethodWrapper extends MethodWrapper {

  /**
   * Value of the expand with constructors mode.
   */
  private boolean fExpandWithConstructors;

  /**
   * Tells whether the expand with constructors mode has been set.
   */
  private boolean isExpandWithConstructorsSet;

  public CallerMethodWrapper(MethodWrapper parent, MethodCall methodCall) {
    super(parent, methodCall);
  }

  @Override
  public boolean canHaveChildren() {
    DartElement member = getMember();
    switch (member.getElementType()) {
      case DartElement.FIELD:
      case DartElement.FUNCTION:
      case DartElement.FUNCTION_TYPE_ALIAS:
      case DartElement.METHOD:
      case DartElement.TYPE:
      case DartElement.VARIABLE:
        return true;
      default:
        return false;
    }
  }

  @Override
  public MethodWrapper createMethodWrapper(MethodCall methodCall) {
    return new CallerMethodWrapper(this, methodCall);
  }

  /**
   * Returns the value of expand with constructors mode.
   * 
   * @return <code>true</code> if in expand with constructors mode, <code>false</code> otherwise or
   *         if not yet set
   */
  public boolean getExpandWithConstructors() {
    return isExpandWithConstructorsSet && fExpandWithConstructors;
  }

  /**
   * Tells whether the expand with constructors mode has been set.
   * 
   * @return <code>true</code> if expand with constructors mode has been set explicitly,
   *         <code>false</code> otherwise
   */
  public boolean isExpandWithConstructorsSet() {
    return isExpandWithConstructorsSet;
  }

  /**
   * Sets the expand with constructors mode.
   * 
   * @param value <code>true</code> if in expand with constructors mode, <code>false</code>
   *          otherwise
   */
  public void setExpandWithConstructors(boolean value) {
    fExpandWithConstructors = value;
    isExpandWithConstructorsSet = true;

  }

  /**
   * @return The result of the search for children
   */
  @Override
  protected Map<String, MethodCall> findChildren(IProgressMonitor progressMonitor) {
    try {

      IProgressMonitor monitor = new SubProgressMonitor(
          progressMonitor,
          95,
          SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);

      checkCanceled(progressMonitor);

      DartElement member = getMember();
      SearchEngine searchEngine = SearchEngineFactory.createSearchEngine();
      SearchScope defaultSearchScope = getSearchScope();
      boolean isWorkspaceScope = SearchScopeFactory.createWorkspaceScope().equals(
          defaultSearchScope);
      SearchScope scope = isWorkspaceScope ? getAccurateSearchScope(
          defaultSearchScope,
          (CompilationUnitElement) member) : defaultSearchScope;
      SearchFilter f = null;
      List<SearchMatch> matches;
      switch (member.getElementType()) {
        case DartElement.METHOD:
          matches = searchEngine.searchReferences((Method) member, scope, f, monitor);
          break;
        case DartElement.FIELD:
          matches = searchEngine.searchReferences((Field) member, scope, f, monitor);
          break;
        case DartElement.FUNCTION:
          if (member.getElementName().isEmpty()) {
            DartFunction func = (DartFunction) member;
            matches = new ArrayList<SearchMatch>();
            matches.add(new SearchMatch(
                MatchQuality.EXACT,
                MatchKind.FUNCTION_EXECUTION,
                member.getParent(),
                func.getSourceRange()));
          } else {
            matches = searchEngine.searchReferences((DartFunction) member, scope, f, monitor);
          }
          break;
        case DartElement.FUNCTION_TYPE_ALIAS:
          matches = searchEngine.searchReferences((DartFunctionTypeAlias) member, scope, f, monitor);
          break;
        case DartElement.VARIABLE:
          DartVariableDeclaration varDecl = (DartVariableDeclaration) member;
          matches = searchEngine.searchReferences(varDecl, scope, f, monitor);
          break;
        default:
          matches = new ArrayList<SearchMatch>();
      }

      checkCanceled(progressMonitor);
      CallSearchResultCollector searchResults = new CallSearchResultCollector();
      for (SearchMatch match : matches) {
        DartElement element = match.getElement();
        SourceRange range = match.getSourceRange();
        searchResults.addMember(
            element,
            element,
            range.getOffset(),
            range.getOffset() + range.getLength());
      }

      monitor.done();
      return searchResults.getCallers();

    } catch (CoreException e) {
      DartToolsPlugin.log(e);
      return new HashMap<String, MethodCall>(0);
    } catch (SearchException e) {
      DartToolsPlugin.log(e);
      return new HashMap<String, MethodCall>(0);
    }
  }

  protected SearchScope getSearchScope() {
    return CallHierarchy.getDefault().getSearchScope();
  }

  @Override
  protected String getTaskName() {
    return CallHierarchyMessages.CallerMethodWrapper_taskname;
  }

  private SearchScope getAccurateSearchScope(SearchScope defaultSearchScope,
      CompilationUnitElement member) throws DartModelException {
    if (!member.isPrivate()) {
      return defaultSearchScope;
    }

    if (member.getCompilationUnit() != null) {
      return SearchScopeFactory.createProjectScope(member.getCompilationUnit().getDartProject());
    } else {
      return defaultSearchScope;
    }
  }

}
