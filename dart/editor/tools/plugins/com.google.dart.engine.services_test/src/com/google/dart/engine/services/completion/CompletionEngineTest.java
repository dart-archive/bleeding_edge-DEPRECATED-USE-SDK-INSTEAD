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
package com.google.dart.engine.services.completion;

import com.google.common.base.Joiner;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.internal.completion.CompletionProposalImpl;
import com.google.dart.engine.services.util.LocationSpec;
import com.google.dart.engine.services.util.MockCompletionRequestor;
import com.google.dart.engine.source.Source;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.Collection;

/**
 * Short, specific code completion tests.
 */
public class CompletionEngineTest extends TestCase {

  public void test001() throws Exception {
    String source = Joiner.on("\n").join("void r(var v) {", "  v.to!1;", "}");
    test(source, "1+toString");
  }

  /**
   * Run a set of completion tests on the given <code>originalSource</code>. The source string has
   * completion points embedded in it, which are identified by '!X' where X is a single character.
   * Each X is matched to positive or negative results in the array of
   * <code>validationStrings</code>. Validation strings contain the name of a prediction with a two
   * character prefix. The first character of the prefix corresponds to an X in the
   * <code>originalSource</code>. The second character is either a '+' or a '-' indicating whether
   * the string is a positive or negative result.
   * 
   * @param originalSource The source for a completion test that contains completion points
   * @param validationStrings The positive and negative predictions
   */
  private void test(String originalSource, String... results) throws URISyntaxException {
    Collection<LocationSpec> completionTests = LocationSpec.from(originalSource, results);
    assertTrue(
        "Expected exclamation point ('!') within the source"
            + " denoting the position at which code completion should occur",
        !completionTests.isEmpty());
    Source source = mock(Source.class);
    CompilationUnit compilationUnit = mock(CompilationUnit.class);
    CompilationUnitElement compilationUnitElement = mock(CompilationUnitElement.class);
    when(compilationUnit.getElement()).thenReturn(compilationUnitElement);
    when(compilationUnitElement.getSource()).thenReturn(source);
    for (LocationSpec test : completionTests) {
      MockCompletionRequestor requestor = new MockCompletionRequestor();
      CompletionFactory factory = new CompletionFactory() {
        @Override
        public CompletionProposal createCompletionProposal(ProposalKind kind) {
          CompletionProposalImpl prop = new CompletionProposalImpl();
          prop.setKind(kind);
          return prop;
        }
      };
      CompletionEngine engine = new CompletionEngine(requestor, factory);
      engine.complete(new AssistContext(compilationUnit, test.testLocation, 0));
      if (test.positiveResults.size() > 0) {
        assertTrue("Expected code completion suggestions", requestor.validate());
      }
      for (String result : test.positiveResults) {
        requestor.assertSuggested(result);
      }
      for (String result : test.negativeResults) {
        requestor.assertNotSuggested(result);
      }
    }
  }
}
