/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.omni.elements;

import com.google.common.collect.Sets;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.search.SearchEngine;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.search.SearchFilter;
import com.google.dart.engine.search.SearchListener;
import com.google.dart.engine.search.SearchMatch;
import com.google.dart.engine.search.SearchPatternFactory;
import com.google.dart.engine.search.SearchScope;
import com.google.dart.engine.search.SearchScopeFactory;
import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.omni.OmniBoxMessages;
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniProposalProvider;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.dialogs.SearchPattern;

import java.util.ArrayList;
import java.util.Set;

/**
 * Provider for class elements.
 */
public class TypeProvider extends OmniProposalProvider {

  /**
   * Place holder to indicate that a search is still in progress.
   */
  public final class SearchInProgressPlaceHolder extends HeaderElement {

    private SearchInProgressPlaceHolder(OmniProposalProvider provider) {
      super(provider);
    }

    @Override
    public void execute(String text) {
      //no-op
    }

    @Override
    public String getId() {
      return "";
    }

    @Override
    public String getLabel() {
      return "  searching...";
    }
  }

  /**
   * Filters out resources that have been marked as ignored.
   * <p>
   * TODO(pquitslund): remove once index removes un-analyzed sources
   */
  private static class DartIgnoreFilter implements SearchFilter {
    @Override
    public boolean passes(SearchMatch match) {

      //TODO (pquitslund): implement filtering ignores

      // Element element = match.getElement();
      //TODO(danrubel): need to get the corresponding resource from project manager?
      // IResource resource = ...;
      // return resource == null || DartCore.isAnalyzed(resource);

      return true;
    }
  }

  private static final DartIgnoreFilter IGNORE_FILTER = new DartIgnoreFilter();

  @SuppressWarnings("unused")
  private final IProgressMonitor progressMonitor;

  private final SearchScope searchScope = SearchScopeFactory.createUniverseScope();

  private final ArrayList<OmniElement> results = new ArrayList<OmniElement>();
  private final Set<Element> uniqueElements = Sets.newHashSet();

  private boolean searchComplete;
  private boolean searchStarted;

  private OmniElement searchPlaceHolderElement;

  public TypeProvider(IProgressMonitor progressMonitor) {
    this.progressMonitor = progressMonitor;
  }

  @Override
  public OmniElement getElementForId(String id) {
    OmniElement[] elements = getElements(id);
    if (elements.length == 0) {
      return null;
    }
    return elements[0];
  }

  @Override
  public OmniElement[] getElements(String pattern) {

    com.google.dart.engine.search.SearchPattern searchPattern = null;
    SearchPattern sp = new SearchPattern();
    sp.setPattern(pattern);
    int matchRule = sp.getMatchRule();
    switch (matchRule) {
      case SearchPattern.RULE_CAMELCASE_MATCH:
        searchPattern = SearchPatternFactory.createCamelCasePattern(pattern, false);
        break;
      case SearchPattern.RULE_PATTERN_MATCH:
        searchPattern = SearchPatternFactory.createWildcardPattern(pattern, false);
        break;
      case SearchPattern.RULE_PREFIX_MATCH:
        searchPattern = SearchPatternFactory.createPrefixPattern(pattern, false);
        break;
      default:
        searchPattern = SearchPatternFactory.createExactPattern(pattern, false);
        break;
    }

    try {
      return doSearch(searchPattern, pattern);
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
    return new OmniElement[0];
  }

  @Override
  public String getId() {
    return getClass().getName();
  }

  @Override
  public String getName() {
    return OmniBoxMessages.OmniBox_Types;
  }

  /**
   * Check if search is complete.
   */
  public boolean isSearchComplete() {
    return searchComplete;
  }

  private OmniElement[] doSearch(com.google.dart.engine.search.SearchPattern searchPattern,
      final String filterText) {

    InstrumentationBuilder instrumentation = Instrumentation.builder("Omni-ClassProvider.doSearch");
    try {
      instrumentation.metric("searchStarted", String.valueOf(searchStarted));

      if (!searchStarted) {

        searchStarted = true;

        searchPlaceHolderElement = new SearchInProgressPlaceHolder(this);

        results.add(searchPlaceHolderElement);

        Index globalIndex = DartCore.getProjectManager().getIndex();
        SearchEngine engine = SearchEngineFactory.createSearchEngine(globalIndex);
        engine.searchTypeDeclarations(
            getSearchScope(),
            searchPattern,
            IGNORE_FILTER,
            new SearchListener() {

              //TODO (pquitslund): consider adding progress reporting

              @Override
              public void matchFound(SearchMatch match) {
                Element element = match.getElement();
                // TODO(scheglov) may be do something smarter with duplicates
                if (!uniqueElements.add(element)) {
                  return;
                }
                // OK, add omni element
                results.add(new com.google.dart.tools.ui.omni.elements.TypeElement(
                    TypeProvider.this,
                    element));
              }

              @Override
              public void searchComplete() {
                searchComplete = true;
                results.remove(searchPlaceHolderElement);
              }
            });

      }

      instrumentation.metric("Results-Size", results.size());
      return results.toArray(new OmniElement[results.size()]);
    } finally {
      instrumentation.log();

    }
  }

  private SearchScope getSearchScope() {
    return searchScope;
  }
}
