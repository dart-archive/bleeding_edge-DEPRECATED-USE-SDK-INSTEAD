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
package com.google.dart.tools.core.search;

import com.google.dart.tools.core.internal.search.scope.WorkspaceSearchScope;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.Type;

import static com.google.dart.tools.core.test.util.MoneyProjectUtilities.getMoneyProject;

import junit.framework.TestCase;

import org.eclipse.core.runtime.NullProgressMonitor;

import java.util.ArrayList;
import java.util.List;

public class SearchEngineTest extends TestCase {
  /**
   * Instances of the class <code>GatheringSearchListener</code> implement a search listener that
   * gathers search matches for later inspection.
   */
  private class GatheringSearchListener implements SearchListener {
    /**
     * A list containing the matches that were found.
     */
    private final List<SearchMatch> matches = new ArrayList<SearchMatch>();

    /**
     * Return a list containing the matches that were found.
     * 
     * @return a list containing the matches that were found
     */
    public List<SearchMatch> getMatches() {
      return matches;
    }

    @Override
    public void matchFound(SearchMatch match) {
      matches.add(match);
    }
  }

  public void test_SearchEngine_searchConstructorDeclarations() throws Exception {
    getMoneyProject();
    SearchEngine engine = SearchEngineFactory.createSearchEngine();
    GatheringSearchListener listener = new GatheringSearchListener();
    engine.searchConstructorDeclarations(new WorkspaceSearchScope(),
        SearchPatternFactory.createPrefixPattern("Mone", true), null, listener,
        new NullProgressMonitor());
    List<SearchMatch> matches = listener.getMatches();
    assertEquals(1, matches.size());
  }

  public void test_SearchEngine_searchTypeDeclarations() throws Exception {
    getMoneyProject();
    SearchEngine engine = SearchEngineFactory.createSearchEngine();
    GatheringSearchListener listener = new GatheringSearchListener();
    engine.searchTypeDeclarations(new WorkspaceSearchScope(),
        SearchPatternFactory.createPrefixPattern("Money", true), null, listener,
        new NullProgressMonitor());
    List<SearchMatch> matches = listener.getMatches();
    assertEquals(2, matches.size());
    assertTrue((isType(matches.get(0), "Money") && isType(matches.get(1), "MoneyTest"))
        || (isType(matches.get(0), "MoneyTest") && isType(matches.get(1), "Money")));
  }

  private boolean isType(SearchMatch match, String typeName) {
    DartElement element = match.getElement();
    return (element instanceof Type) && typeName.equals(element.getElementName());
  }
}
