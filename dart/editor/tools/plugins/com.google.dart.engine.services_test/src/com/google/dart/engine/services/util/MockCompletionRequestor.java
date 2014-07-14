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
package com.google.dart.engine.services.util;

import com.google.dart.engine.services.completion.CompletionProposal;
import com.google.dart.engine.services.completion.CompletionRequestor;
import com.google.dart.engine.services.completion.CompletionTests;

import static junit.framework.Assert.fail;

import java.util.Collection;
import java.util.HashSet;

public class MockCompletionRequestor implements CompletionRequestor {
  private int state = 0;
  private int exceptionCount = 0;
  private long resolveLibraryTime = 0;
  Collection<String> suggestions = new HashSet<String>();

  @Override
  public void accept(CompletionProposal proposal) {
    CompletionTests.assertEquals("Expected accept to be called after beginReporting", 1, state);
    String suggestion = proposal.getCompletion();
    suggestions.add(suggestion);
    suggestions.add(suggestion + ":" + proposal.getKind());
    suggestions.add(suggestion + ",rel=" + proposal.getRelevance());
    suggestions.add(suggestion + ",potential=" + proposal.isPotentialMatch() + ",declaringType="
        + proposal.getDeclaringType());
  }

  /**
   * Assert that the receiver does NOT contain the specified suggestion
   */
  public void assertNotSuggested(String suggestion, char testId) {
    if (suggestions.contains(suggestion)) {
      CompletionTests.fail("Test " + testId + " invalid suggestion: " + suggestion);
    }
  }

  /**
   * Assert that the receiver contains the specified suggestion
   */
  public void assertSuggested(String suggestion, char testId) {
    if (!suggestions.contains(suggestion)) {
      CompletionTests.fail("Test " + testId + " expected suggestion: " + suggestion);
    }
  }

  @Override
  public void beginReporting() {
    CompletionTests.assertEquals("Expected beginReporting to be called first", 0, state++);
  }

//  @Override
//  public void completionFailure(Problem problem) {
//    CompletionEngineTest.assertEquals(
//        "Expected completionFailure to be called after acceptContext",
//        2,
//        state);
//    super.completionFailure(problem);
//  }

  @Override
  public void endReporting() {
    CompletionTests.assertEquals(
        "Expected endReporting to be called after beginReporting",
        1,
        state++);
  }

  public int getExceptionCount() {
    return exceptionCount;
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
    if (state == 2) {
      return suggestions.size() > 0;
    }
    if (exceptionCount > 0) {
      return false;
    }
    fail("Expected endReporting to have been called (current state = " + state + ")");
    return false;
  }
}
