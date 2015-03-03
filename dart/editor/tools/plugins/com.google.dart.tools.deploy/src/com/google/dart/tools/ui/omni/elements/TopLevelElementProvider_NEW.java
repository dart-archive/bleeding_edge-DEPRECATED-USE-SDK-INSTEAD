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
import com.google.dart.server.FindTopLevelDeclarationsConsumer;
import com.google.dart.server.generated.types.RequestError;
import com.google.dart.server.generated.types.SearchResult;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.model.SearchResultsListener;
import com.google.dart.tools.ui.omni.OmniElement;
import com.google.dart.tools.ui.omni.OmniProposalProvider;
import com.google.dart.tools.ui.omni.util.CamelUtil;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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

  private static final long WAIT_MS = 20;

  private static String getIdentifierCharacters(String str) {
    int length = str.length();
    StringBuilder buf = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      char c = str.charAt(i);
      if (Character.isJavaIdentifierPart(c)) {
        buf.append(c);
      }
    }
    return buf.toString();
  }

  private final Object lock = new Object();

  private final List<OmniElement> results = Lists.newArrayList();

  private String currentRequestPattern = null;
  private CountDownLatch currentRequestLatch;

  private boolean searchComplete;

  private final List<ProviderCompleteListener> listeners = Lists.newArrayList();

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
    if (StringUtils.isBlank(pattern)) {
      return new OmniElement[0];
    }
    // initiate a new request, if a new pattern
    if (!StringUtils.equals(currentRequestPattern, pattern)) {
      currentRequestPattern = pattern;
      currentRequestLatch = new CountDownLatch(1);
      doSearch();
      // wait a little, if results are ready quickly
      Uninterruptibles.awaitUninterruptibly(currentRequestLatch, WAIT_MS, TimeUnit.MILLISECONDS);
    }
    // return current results
    synchronized (lock) {
      return results.toArray(new OmniElement[results.size()]);
    }
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

  /**
   * If the current search is not complete yet, the given listener will be notified when the search
   * is complete.
   */
  public void onComplete(ProviderCompleteListener listener) {
    synchronized (lock) {
      if (searchComplete) {
        return;
      }
    }
    synchronized (listeners) {
      listeners.add(listener);
    }
  }

  private void doSearch() {
    String text = getIdentifierCharacters(currentRequestPattern);
    final String pattern = "^" + CamelUtil.getCamelCaseRegExp(text) + ".*";
    final Pattern patternObj = Pattern.compile(pattern);
    //
    searchComplete = false;
    final List<OmniElement> newResults = Lists.newArrayList();
    final CountDownLatch latch = currentRequestLatch;
    DartCore.getAnalysisServer().search_findTopLevelDeclarations(
        pattern,
        new FindTopLevelDeclarationsConsumer() {
          @Override
          public void computedSearchId(String searchId) {
            DartCore.getAnalysisServerData().addSearchResultsListener(
                searchId,
                new SearchResultsListener() {
                  @Override
                  public void computedSearchResults(List<SearchResult> searchResults, boolean last) {
                    if (latch == currentRequestLatch) {
                      for (SearchResult searchResult : searchResults) {
                        newResults.add(new TopLevelElement_NEW(
                            TopLevelElementProvider_NEW.this,
                            patternObj,
                            searchResult));
                      }
                      if (last) {
                        synchronized (lock) {
                          results.clear();
                          results.addAll(newResults);
                          searchComplete = true;
                          currentRequestLatch.getCount();
                        }
                        synchronized (listeners) {
                          for (ProviderCompleteListener listener : listeners) {
                            listener.complete(TopLevelElementProvider_NEW.this);
                          }
                          listeners.clear();
                        }
                      }
                    }
                  }
                });
          }

          @Override
          public void onError(RequestError requestError) {
            if (latch == currentRequestLatch) {
              synchronized (results) {
                results.clear();
                searchComplete = true;
                currentRequestLatch.getCount();
              }
              synchronized (listeners) {
                for (ProviderCompleteListener listener : listeners) {
                  listener.complete(TopLevelElementProvider_NEW.this);
                }
                listeners.clear();
              }
            }
          }
        });
  }
}
