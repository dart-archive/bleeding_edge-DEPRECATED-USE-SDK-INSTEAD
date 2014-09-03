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

import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.GetSuggestionsConsumer;
import com.google.dart.server.MockAnalysisServer;
import com.google.dart.server.generated.types.CompletionSuggestion;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class DartCompletionReceiverTest extends TestCase {
  class MockServer extends MockAnalysisServer {
    AnalysisServerListener listener;
    Runnable serverResponse;
    GetSuggestionsConsumer consumer;

    public MockServer(Runnable serverResponse) {
      this.serverResponse = serverResponse;
    }

    @Override
    public void addAnalysisServerListener(AnalysisServerListener listener) {
      this.listener = listener;
    }

    @Override
    public void completion_getSuggestions(String file, Integer offset,
        GetSuggestionsConsumer consumer) {
      assertEquals(testFile, file);
      assertEquals(testOffset, offset.intValue());
      this.consumer = consumer;
      // Send results asynchronously
      Thread thread = new Thread(getClass().getSimpleName() + " suggestion results") {
        @Override
        public void run() {
          serverResponse.run();
        };
      };
      thread.setDaemon(true);
      thread.start();
    }

    @Override
    public void removeAnalysisServerListener(AnalysisServerListener listener) {
      if (this.listener == listener) {
        this.listener = null;
      }
    }
  }

  MockServer server;
  DartCompletionReceiver receiver;
  String testFile = "/test.dart";
  int testOffset = 17;
  String completionId = "id27";

  public void test_no_completion() {
    server = new MockServer(new Runnable() {
      @Override
      public void run() {
        // ignored
      }
    });
    receiver = new DartCompletionReceiver(server);
    List<CompletionSuggestion> result = receiver.requestCompletions(testFile, testOffset, 1);
    assertNull(result);
  }

  public void test_no_suggestions() {
    server = new MockServer(new Runnable() {
      @Override
      public void run() {
        server.consumer.computedCompletionId(completionId);
      }
    });
    receiver = new DartCompletionReceiver(server);
    List<CompletionSuggestion> result = receiver.requestCompletions(testFile, testOffset, 1);
    assertNull(result);
  }

  public void test_some_suggestions() {
    final List<CompletionSuggestion> completions0 = new ArrayList<CompletionSuggestion>();
    final List<CompletionSuggestion> completions1 = new ArrayList<CompletionSuggestion>();
    final List<CompletionSuggestion>[] partialResults = new List[3];

    completions0.add(new CompletionSuggestion(
        "kind",
        "relevance",
        "completion0",
        1,
        2,
        false,
        false,
        "",
        "",
        null,
        null,
        null,
        null,
        0,
        0,
        null,
        null));
    completions1.add(new CompletionSuggestion(
        "kind",
        "relevance",
        "completion1",
        1,
        2,
        false,
        false,
        "",
        "",
        null,
        null,
        null,
        null,
        0,
        0,
        null,
        null));

    server = new MockServer(new Runnable() {
      @Override
      public void run() {
        server.consumer.computedCompletionId(completionId);
        if (server.listener != null) {
          server.listener.computedCompletion("anotherId", 28, 0, completions0, true);
          partialResults[0] = receiver.getCompletions();
          server.listener.computedCompletion(completionId, testOffset, 0, completions1, false);
          partialResults[1] = receiver.getCompletions();
        }
      }
    });
    receiver = new DartCompletionReceiver(server);
    long start = System.currentTimeMillis();
    int millisToWait = 10;
    List<CompletionSuggestion> result = receiver.requestCompletions(
        testFile,
        testOffset,
        millisToWait);
    long delta = System.currentTimeMillis() - start;
    assertEquals(completions1, result);
    assertNull(partialResults[0]);
    assertEquals(completions1, partialResults[1]);
    assertNull(partialResults[2]);
    assertTrue(delta >= millisToWait);
  }

  public void test_suggestions() {
    final List<CompletionSuggestion> completions0 = new ArrayList<CompletionSuggestion>();
    final List<CompletionSuggestion> completions1 = new ArrayList<CompletionSuggestion>();
    final List<CompletionSuggestion> completions2 = new ArrayList<CompletionSuggestion>();
    final List<CompletionSuggestion>[] partialResults = new List[3];

    completions0.add(new CompletionSuggestion(
        "kind",
        "relevance",
        "completion0",
        1,
        2,
        false,
        false,
        "",
        "",
        null,
        null,
        null,
        null,
        0,
        0,
        null,
        null));
    completions1.add(new CompletionSuggestion(
        "kind",
        "relevance",
        "completion1",
        1,
        2,
        false,
        false,
        "",
        "",
        null,
        null,
        null,
        null,
        0,
        0,
        null,
        null));
    completions2.add(new CompletionSuggestion(
        "kind",
        "relevance",
        "completion2",
        1,
        2,
        false,
        false,
        "",
        "",
        null,
        null,
        null,
        null,
        0,
        0,
        null,
        null));

    server = new MockServer(new Runnable() {
      @Override
      public void run() {
        server.consumer.computedCompletionId(completionId);
        if (server.listener != null) {
          server.listener.computedCompletion("anotherId", 28, 0, completions0, true);
          partialResults[0] = receiver.getCompletions();
          server.listener.computedCompletion(completionId, testOffset, 0, completions1, false);
          partialResults[1] = receiver.getCompletions();
          server.listener.computedCompletion(completionId, testOffset, 0, completions2, true);
          partialResults[2] = receiver.getCompletions();
        }
      }
    });
    receiver = new DartCompletionReceiver(server);
    long start = System.currentTimeMillis();
    int millisToWait = 50000;
    List<CompletionSuggestion> result = receiver.requestCompletions(
        testFile,
        testOffset,
        millisToWait);
    long delta = System.currentTimeMillis() - start;
    assertEquals(completions2, result);
    assertNull(partialResults[0]);
    assertEquals(completions1, partialResults[1]);
    assertEquals(completions2, partialResults[2]);
    assertTrue(delta < millisToWait);
  }
}
