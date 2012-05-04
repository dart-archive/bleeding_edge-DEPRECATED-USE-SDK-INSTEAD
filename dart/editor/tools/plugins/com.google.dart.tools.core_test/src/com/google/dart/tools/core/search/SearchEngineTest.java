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
package com.google.dart.tools.core.search;

import com.google.dart.tools.core.internal.search.listener.GatheringSearchListener;
import com.google.dart.tools.core.internal.search.scope.WorkspaceSearchScope;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;

import static com.google.dart.tools.core.test.util.MoneyProjectUtilities.getMoneyProject;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;

import java.util.List;

public class SearchEngineTest extends TestCase {
  public void test_SearchEngine_searchConstructorDeclarations() throws Exception {
    getMoneyProject();
    SearchEngine engine = SearchEngineFactory.createSearchEngine();
    GatheringSearchListener listener = new GatheringSearchListener();
    engine.searchConstructorDeclarations(
        new WorkspaceSearchScope(),
        SearchPatternFactory.createPrefixPattern("Simpl", true),
        null,
        listener,
        new NullProgressMonitor());
    List<SearchMatch> matches = listener.getMatches();
    assertEquals(2, matches.size());
  }

  public void test_SearchEngine_searchReferences_field() throws Exception {
    DartProject moneyProject = getMoneyProject();
    DartLibrary[] libraries = moneyProject.getDartLibraries();
    assertNotNull(libraries);
    assertEquals(1, libraries.length);
    Type type = libraries[0].getCompilationUnit("simple_money.dart").getType("SimpleMoney");
    Field field = type.getField("amount");
    SearchEngine engine = SearchEngineFactory.createSearchEngine();
    GatheringSearchListener listener = new GatheringSearchListener();
    engine.searchReferences(
        field,
        new WorkspaceSearchScope(),
        null,
        listener,
        new NullProgressMonitor());
    List<SearchMatch> matches = listener.getMatches();
    assertEquals(4, matches.size());
  }

  public void test_SearchEngine_searchReferences_method() throws Exception {
    DartProject moneyProject = getMoneyProject();
    DartLibrary[] libraries = moneyProject.getDartLibraries();
    assertNotNull(libraries);
    assertEquals(1, libraries.length);
    Type type = libraries[0].getCompilationUnit("simple_money.dart").getType("SimpleMoney");
    Method method = type.getMethod("getAmount", new String[0]);
    SearchEngine engine = SearchEngineFactory.createSearchEngine();
    GatheringSearchListener listener = new GatheringSearchListener();
    engine.searchReferences(
        method,
        new WorkspaceSearchScope(),
        null,
        listener,
        new NullProgressMonitor());
    List<SearchMatch> matches = listener.getMatches();
    assertEquals(2, matches.size());
  }

  public void test_SearchEngine_searchReferences_type() throws Exception {
    DartProject moneyProject = getMoneyProject();
    DartLibrary[] libraries = moneyProject.getDartLibraries();
    assertNotNull(libraries);
    assertEquals(1, libraries.length);
    Type type = libraries[0].getCompilationUnit("simple_money.dart").getType("SimpleMoney");
    SearchEngine engine = SearchEngineFactory.createSearchEngine();
    GatheringSearchListener listener = new GatheringSearchListener();
    engine.searchReferences(
        type,
        new WorkspaceSearchScope(),
        null,
        listener,
        new NullProgressMonitor());
    List<SearchMatch> matches = listener.getMatches();
    assertEquals(10, matches.size()); // I believe that this should eventually be 17.
  }

  public void test_SearchEngine_searchTypeDeclarations() throws Exception {
    getMoneyProject();
    SearchEngine engine = SearchEngineFactory.createSearchEngine();
    GatheringSearchListener listener = new GatheringSearchListener();
    engine.searchTypeDeclarations(
        new WorkspaceSearchScope(),
        SearchPatternFactory.createPrefixPattern("Money", true),
        null,
        listener,
        new NullProgressMonitor());
    List<SearchMatch> matches = listener.getMatches();
    assertEquals(1, matches.size());
    assertTrue(isType(matches.get(0), "Money"));
  }

  private boolean isType(SearchMatch match, String typeName) {
    DartElement element = match.getElement();
    return (element instanceof Type) && typeName.equals(element.getElementName());
  }
}
