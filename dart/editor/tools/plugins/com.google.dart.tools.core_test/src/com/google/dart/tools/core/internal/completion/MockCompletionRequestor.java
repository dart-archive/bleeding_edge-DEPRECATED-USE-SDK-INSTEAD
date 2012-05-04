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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.tools.core.completion.CompletionContext;
import com.google.dart.tools.core.completion.CompletionMetrics;
import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.core.completion.CompletionRequestor;
import com.google.dart.tools.core.problem.Problem;

import static junit.framework.Assert.fail;

import java.util.Collection;
import java.util.HashSet;

class MockCompletionRequestor extends CompletionRequestor {
  private int state = 0;
  private int exceptionCount = 0;
  private long resolveLibraryTime = 0;
  Collection<String> suggestions = new HashSet<String>();

  @Override
  public void accept(CompletionProposal proposal) {
    CompletionEngineTest.assertEquals("Expected accept to be called after acceptContext", 2, state);
    suggestions.add(new String(proposal.getCompletion()));
  }

  @Override
  public void acceptContext(CompletionContext context) {
    CompletionEngineTest.assertEquals(
        "Expected acceptContext to be called after beginReporting",
        1,
        state++);
    super.acceptContext(context);
  }

  /**
   * Assert that the receiver does NOT contain the specified suggestion
   */
  public void assertNotSuggested(String suggestion) {
    if (suggestions.contains(suggestion)) {
      CompletionEngineTest.fail("Invalid suggestion: " + suggestion);
    }
  }

  /**
   * Assert that the receiver contains the specified suggestion
   */
  public void assertSuggested(String suggestion) {
    if (!suggestions.contains(suggestion)) {
      CompletionEngineTest.fail("Expected suggestion: " + suggestion);
    }
  }

  @Override
  public void beginReporting() {
    CompletionEngineTest.assertEquals("Expected beginReporting to be called first", 0, state++);
    super.beginReporting();
  }

  @Override
  public void completionFailure(Problem problem) {
    CompletionEngineTest.assertEquals(
        "Expected completionFailure to be called after acceptContext",
        2,
        state);
    super.completionFailure(problem);
  }

  @Override
  public void endReporting() {
    CompletionEngineTest.assertEquals(
        "Expected endReporting to be called after beginReporting",
        2,
        state++);
    super.endReporting();
  }

  public int getExceptionCount() {
    return exceptionCount;
  }

  @Override
  public CompletionMetrics getMetrics() {
    return new CompletionMetrics() {
      @Override
      public void completionException(Exception e) {
        exceptionCount++;
      }

      @Override
      public void resolveLibraryTime(long ms) {
        resolveLibraryTime += ms;
      }
    };
  }

  public long getResolveLibraryTime() {
    return resolveLibraryTime;
  }

  /**
   * Validate the requestor content
   * 
   * @return <code>true</code> if suggestions were generated, or <code>false</code> none
   */
  public boolean validate() {
    if (state == 0) {
      return false;
    }
    if (state == 3) {
      return suggestions.size() > 0;
    }
    if (exceptionCount > 0) {
      return false;
    }
    fail("Expected endReporting to have been called (current state = " + state + ")");
    return false;
  }
}
