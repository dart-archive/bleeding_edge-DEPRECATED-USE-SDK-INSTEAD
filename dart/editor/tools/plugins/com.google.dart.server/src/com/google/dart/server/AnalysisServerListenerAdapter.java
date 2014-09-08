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
package com.google.dart.server;

import com.google.dart.server.generated.types.AnalysisError;
import com.google.dart.server.generated.types.AnalysisStatus;
import com.google.dart.server.generated.types.CompletionSuggestion;
import com.google.dart.server.generated.types.HighlightRegion;
import com.google.dart.server.generated.types.NavigationRegion;
import com.google.dart.server.generated.types.Occurrences;
import com.google.dart.server.generated.types.Outline;
import com.google.dart.server.generated.types.OverrideMember;
import com.google.dart.server.generated.types.SearchResult;

import java.util.List;

/**
 * This adapter class provides default implementations for the methods described by the
 * {@code AnalysisServerListener} interface.
 */
public class AnalysisServerListenerAdapter implements AnalysisServerListener {

  @Override
  public void computedCompletion(String completionId, int replacementOffset, int replacementLength,
      List<CompletionSuggestion> completions, boolean isLast) {
  }

  @Override
  public void computedErrors(String file, List<AnalysisError> errors) {
  }

  @Override
  public void computedHighlights(String file, List<HighlightRegion> highlights) {
  }

  @Override
  public void computedNavigation(String file, List<NavigationRegion> targets) {
  }

  @Override
  public void computedOccurrences(String file, List<Occurrences> occurrencesArray) {
  }

  @Override
  public void computedOutline(String file, Outline outline) {
  }

  @Override
  public void computedOverrides(String file, List<OverrideMember> overrides) {
  }

  @Override
  public void computedSearchResults(String searchId, List<SearchResult> results, boolean last) {
  }

  @Override
  public void flushedResults(List<String> files) {
  }

  @Override
  public void serverConnected() {
  }

  @Override
  public void serverError(boolean isFatal, String message, String stackTrace) {
  }

  @Override
  public void serverStatus(AnalysisStatus status) {
  }

}
