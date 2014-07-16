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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.server.SearchIdConsumer;
import com.google.dart.server.SearchResult;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.SearchResultsListener;
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniProposalProvider;

import org.eclipse.core.runtime.IProgressMonitor;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Provider for class elements.
 */
public class TopLevelElementProvider_NEW extends OmniProposalProvider {

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

  private final List<OmniElement> results = Lists.newArrayList();
  private boolean searchComplete;

  public TopLevelElementProvider_NEW(IProgressMonitor progressMonitor) {
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
    return doSearch(pattern);
  }

  @Override
  public String getId() {
    return getClass().getName();
  }

  @Override
  public String getName() {
    return "Top-level declarations";
  }

  /**
   * Check if search is complete.
   */
  public boolean isSearchComplete() {
    return searchComplete;
  }

  private OmniElement[] doSearch(String _pattern) {
    String pattern = _pattern + ".*";
    //
    searchComplete = false;
    results.clear();
    final CountDownLatch latch = new CountDownLatch(1);
    DartCore.getAnalysisServer().searchTopLevelDeclarations(pattern, new SearchIdConsumer() {
      @Override
      public void computedSearchId(String searchId) {
        DartCore.getAnalysisServerData().addSearchResultsListener(
            searchId,
            new SearchResultsListener() {
              @Override
              public void computedSearchResults(SearchResult[] searchResults, boolean last) {
                for (SearchResult searchResult : searchResults) {
                  results.add(new TopLevelElement_NEW(
                      TopLevelElementProvider_NEW.this,
                      searchResult));
                }
                if (last) {
                  latch.countDown();
                }
              }
            });
      }
    });
    Uninterruptibles.awaitUninterruptibly(latch, 30, TimeUnit.SECONDS);
    searchComplete = true;
    return results.toArray(new OmniElement[results.size()]);
  }
}
