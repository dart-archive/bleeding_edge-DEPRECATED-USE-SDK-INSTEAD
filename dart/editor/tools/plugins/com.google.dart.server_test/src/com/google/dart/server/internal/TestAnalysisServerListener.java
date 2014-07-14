/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.ArrayUtilities;
import com.google.dart.server.AnalysisError;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.AnalysisStatus;
import com.google.dart.server.CompletionSuggestion;
import com.google.dart.server.Element;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.Occurrences;
import com.google.dart.server.Outline;
import com.google.dart.server.OverrideMember;
import com.google.dart.server.SearchResult;
import com.google.dart.server.ServerStatus;
import com.google.dart.server.internal.asserts.NavigationRegionsAssert;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestAnalysisServerListener implements AnalysisServerListener {
  private final Map<String, CompletionSuggestion[]> completionsMap = Maps.newHashMap();
  private final Map<String, SearchResult[]> searchResultsMap = Maps.newHashMap();
  private final List<AnalysisServerError> serverErrors = Lists.newArrayList();
  private final Map<String, AnalysisError[]> sourcesErrors = Maps.newHashMap();
  private final Map<String, HighlightRegion[]> highlightsMap = Maps.newHashMap();
  private final Map<String, NavigationRegion[]> navigationMap = Maps.newHashMap();
  private final Map<String, Occurrences[]> occurrencesMap = Maps.newHashMap();
  private final Map<String, Outline> outlineMap = Maps.newHashMap();
  private final Map<String, OverrideMember[]> overridesMap = Maps.newHashMap();
  private boolean serverConnected = false;
  private ServerStatus serverStatus = null;

  /**
   * Assert that the number of errors that have been gathered matches the number of errors that are
   * given and that they have the expected error codes. The order in which the errors were gathered
   * is ignored.
   * 
   * @param file the file to check errors for
   * @param expectedErrorCodes the error codes of the errors that should have been gathered
   * @throws AssertionFailedError if a different number of errors have been gathered than were
   *           expected
   */
  public synchronized void assertErrorsWithAnalysisErrors(String file,
      AnalysisError... expectedErrors) {
    AnalysisError[] errors = getErrors(file);
    assertErrorsWithAnalysisErrors(errors, expectedErrors);
  }

  /**
   * Returns {@link NavigationRegionsAssert} for the given file.
   */
  public synchronized NavigationRegionsAssert assertNavigationRegions(String file) {
    return new NavigationRegionsAssert(getNavigationRegions(file));
  }

  /**
   * Asserts that there was no {@link AnalysisServerError} reported.
   */
  public synchronized void assertNoServerErrors() {
    assertThat(serverErrors).isEmpty();
  }

  public synchronized void assertServerConnected(boolean expectedConnected) {
    Assert.assertEquals(expectedConnected, serverConnected);
  }

  public void assertServerErrors(List<AnalysisServerError> expectedErrors) {
    Assert.assertEquals(expectedErrors.size(), serverErrors.size());
    for (int i = 0; i < expectedErrors.size(); i++) {
      Assert.assertTrue(expectedErrors.get(i).equals(serverErrors.get(i)));
    }
  }

  public synchronized void assertServerStatus(ServerStatus expectedStatus) {
    AnalysisStatus actualAnalysisStatus = serverStatus.getAnalysisStatus();
    AnalysisStatus expectedAnalysisStatus = expectedStatus.getAnalysisStatus();
    Assert.assertEquals(expectedAnalysisStatus.isAnalyzing(), actualAnalysisStatus.isAnalyzing());
    Assert.assertEquals(
        expectedAnalysisStatus.getAnalysisTarget(),
        actualAnalysisStatus.getAnalysisTarget());
  }

  /**
   * Removes all of reported {@link NavigationRegion}s.
   */
  public synchronized void clearNavigationRegions() {
    navigationMap.clear();
  }

  /**
   * Removes all of reported {@link Occurrences}s.
   */
  public synchronized void clearOccurrences() {
    occurrencesMap.clear();
  }

  /**
   * Removes all of reported {@link OverrideMember}.
   */
  public synchronized void clearOverrides() {
    overridesMap.clear();
  }

  @Override
  public synchronized void computedCompletion(String completionId,
      CompletionSuggestion[] completions, boolean last) {
    CompletionSuggestion[] value = completionsMap.get(completionId);
    if (value == null) {
      completionsMap.put(completionId, completions);
    } else {
      List<CompletionSuggestion> completionsAsList = Arrays.asList(completions);
      completionsAsList.addAll(Arrays.asList(value));
      completionsMap.put(
          completionId,
          completionsAsList.toArray(new CompletionSuggestion[completionsAsList.size()]));
    }
  }

  @Override
  public synchronized void computedErrors(String file, AnalysisError[] errors) {
    sourcesErrors.put(file, errors);
  }

  @Override
  public synchronized void computedHighlights(String file, HighlightRegion[] highlights) {
    highlightsMap.put(file, highlights);
  }

  @Override
  public synchronized void computedNavigation(String file, NavigationRegion[] targets) {
    navigationMap.put(file, targets);
  }

  @Override
  public void computedOccurrences(String file, Occurrences[] occurrencesArray) {
    occurrencesMap.put(file, occurrencesArray);
  }

  @Override
  public synchronized void computedOutline(String file, Outline outline) {
    outlineMap.put(file, outline);
  }

  @Override
  public void computedOverrides(String file, OverrideMember[] overrides) {
    overridesMap.put(file, overrides);
  }

  @Override
  public void computedSearchResults(String searchId, SearchResult[] results, boolean last) {
    searchResultsMap.put(searchId, results);
  }

  /**
   * Returns a navigation {@link Element} at the given position.
   */
  public synchronized Element findNavigationElement(String file, int offset) {
    NavigationRegion[] regions = getNavigationRegions(file);
    if (regions != null) {
      for (NavigationRegion navigationRegion : regions) {
        if (navigationRegion.containsInclusive(offset)) {
          return navigationRegion.getTargets()[0];
        }
      }
    }
    return null;
  }

  /**
   * Returns {@link CompletionSuggestion[]} for the given completion id, maybe {@code null} if have
   * not been ever notified.
   */
  public synchronized CompletionSuggestion[] getCompletions(String completionId) {
    return completionsMap.get(completionId);
  }

  /**
   * Returns {@link AnalysisError} for the given file, may be empty, but not {@code null}.
   */
  public synchronized AnalysisError[] getErrors(String file) {
    AnalysisError[] errors = sourcesErrors.get(file);
    if (errors == null) {
      return AnalysisError.NO_ERRORS;
    }
    return errors;
  }

  /**
   * Returns {@link HighlightRegion}s for the given file, maybe {@code null} if have not been ever
   * notified.
   */
  public synchronized HighlightRegion[] getHighlightRegions(String file) {
    return highlightsMap.get(file);
  }

  /**
   * Returns {@link NavigationRegion}s for the given file, maybe {@code null} if have not been ever
   * notified.
   */
  public synchronized NavigationRegion[] getNavigationRegions(String file) {
    return navigationMap.get(file);
  }

  /**
   * Returns {@link Occurrences}s for the given file, maybe {@code null} if have not been ever
   * notified.
   */
  public synchronized Occurrences[] getOccurrences(String file) {
    return occurrencesMap.get(file);
  }

  /**
   * Returns {@link Outline} for the given {@link Source}, maybe {@code null} if have not been ever
   * notified.
   */
  public synchronized Outline getOutline(String file) {
    return outlineMap.get(file);
  }

  /**
   * Returns {@link OverrideMember}s for the given file, maybe {@code null} if have not been ever
   * notified.
   */
  public synchronized OverrideMember[] getOverrides(String file) {
    return overridesMap.get(file);
  }

  /**
   * Returns {@link SearchResult[]} for the given search id, maybe {@code null} if have not been
   * ever notified.
   */
  public SearchResult[] getSearchResults(String searchId) {
    return searchResultsMap.get(searchId);
  }

  @Override
  public synchronized void serverConnected() {
    serverConnected = true;
  }

  @Override
  public synchronized void serverError(boolean isFatal, String message, String stackTrace) {
    serverErrors.add(new AnalysisServerError(isFatal, message, stackTrace));
  }

  @Override
  public synchronized void serverStatus(ServerStatus status) {
    this.serverStatus = status;
  }

  /**
   * Assert that the array of actual {@link AnalysisError}s match the array of expected
   * {@link AnalysisError}s.
   * 
   * @param actualErrors the actual set of errors that were created for some analysis
   * @param expectedErrors the expected array of errors
   */
  private void assertErrorsWithAnalysisErrors(AnalysisError[] actualErrors,
      AnalysisError[] expectedErrors) {
    if (actualErrors == null && expectedErrors == null) {
      return;
    }

    // assert that the arrays have the same length
    Assert.assertEquals(expectedErrors.length, actualErrors.length);

    // assert that the actualErrors contains all of the expected errors
    for (AnalysisError expectedError : expectedErrors) {
      // individual calls to assert each error are made for better messaging when there is a failure
      Assert.assertTrue(ArrayUtilities.contains(actualErrors, expectedError));
    }
  }

}
