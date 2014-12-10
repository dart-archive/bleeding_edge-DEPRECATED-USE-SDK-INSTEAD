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
package com.google.dart.tools.core.completion;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.server.AnalysisServer;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.AnalysisServerListenerAdapter;
import com.google.dart.server.GetSuggestionsConsumer;
import com.google.dart.server.generated.types.CompletionSuggestion;
import com.google.dart.server.generated.types.RequestError;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This listener processes notifications from {@link AnalysisServer} and collects any completion
 * suggestions received for the given completion request. Clients should instantiate the collector
 * then call {@link #requestSuggestions(String, int, long)} to begin collecting completions from the
 * server.
 */
public class DartSuggestionReceiver {

  /**
   * The server providing the suggestions.
   */
  private final AnalysisServer server;

  /**
   * Response handler for the completion.getSuggestions request.
   */
  GetSuggestionsConsumer consumer = new GetSuggestionsConsumer() {
    @Override
    public void computedCompletionId(String completionId) {
      DartSuggestionReceiver.this.completionId = completionId;
      if (latch != null) {
        server.addAnalysisServerListener(listener);
      }
    }

    @Override
    public void onError(RequestError requestError) {
      latch.countDown();
    }
  };

  /**
   * The completion identifier returned by the server.
   */
  private String completionId;

  /**
   * Notification handler for collecting suggestions from the server.
   */
  private AnalysisServerListener listener = new AnalysisServerListenerAdapter() {
    @Override
    public void computedCompletion(String completionId, int replacementOffset,
        int replacementLength, List<CompletionSuggestion> completions, boolean isLast) {
      if (completionId.equals(DartSuggestionReceiver.this.completionId)) {
        DartSuggestionReceiver.this.replacementOffset = replacementOffset;
        DartSuggestionReceiver.this.replacementLength = replacementLength;
        DartSuggestionReceiver.this.suggestions = completions;
        if (isLast) {
          server.removeAnalysisServerListener(this);
          latch.countDown();
        }
      }
    }
  };

  /**
   * The replacement offset for the current completions.
   */
  private int replacementOffset;

  /**
   * The replacement length for the current completions.
   */
  private int replacementLength;

  /**
   * The suggestions returned by the server or {@code null} if none.
   */
  private List<CompletionSuggestion> suggestions;

  /**
   * The latch used by {@link #listener} to notify {@link #requestSuggestions(String, int, long)}
   * when suggestions are available or {@code null} if no request has been made or if the request is
   * complete.
   */
  private CountDownLatch latch;

  public DartSuggestionReceiver(AnalysisServer server) {
    this.server = server;
  }

  /**
   * Answer the replacement length for the current completions.
   */
  public int getReplacementLength() {
    return replacementLength;
  }

  /**
   * Answer the replacement offset for the current completions.
   */
  public int getReplacementOffset() {
    return replacementOffset;
  }

  /**
   * Answer the current suggestions returned by the server or {@code null} if none
   */
  public List<CompletionSuggestion> getSuggestions() {
    return suggestions;
  }

  /**
   * Send a completion request to the server. Since the server sends multiple notifications
   * containing suggestions, each superseding the prior notification, this method returns either the
   * final suggestions when they are received or the best available suggestions if the final
   * suggestions have not been received with the given amount of time.
   * 
   * @param file The absolute path of the file in which the completion is requested
   * @param offset The offset in the file where the completion is requested
   * @param millisToWait the maximum # of milliseconds to wait for the final list of completions.
   * @return the completion suggestions or {@code null} if none
   */
  public List<CompletionSuggestion> requestSuggestions(String file, int offset, long millisToWait) {
    latch = new CountDownLatch(1);
    server.completion_getSuggestions(file, offset, consumer);
    Uninterruptibles.awaitUninterruptibly(latch, millisToWait, TimeUnit.MILLISECONDS);
    latch = null;
    server.removeAnalysisServerListener(listener);
    return suggestions;
  }
}
