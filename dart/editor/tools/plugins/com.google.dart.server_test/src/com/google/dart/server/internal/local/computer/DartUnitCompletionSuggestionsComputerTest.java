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

package com.google.dart.server.internal.local.computer;

import com.google.common.collect.Lists;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.source.Source;
import com.google.dart.server.CompletionSuggestion;
import com.google.dart.server.CompletionSuggestionKind;
import com.google.dart.server.CompletionSuggestionsConsumer;
import com.google.dart.server.internal.local.AbstractLocalServerTest;

import org.apache.commons.lang3.StringUtils;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DartUnitCompletionSuggestionsComputerTest extends AbstractLocalServerTest {
  private String contextId;
  private Source source;
  private int completionOffset;
  private List<CompletionSuggestion> suggestions = Lists.newArrayList();

  public void test_class_comment_block() throws Exception {
    createContextWithSingleSource(makeSource(//
        "/**",
        " * 000",
        " * 111",
        " * 222",
        " */",
        "class AAA {",
        "}",
        "main() {",
        "  AA!",
        "}"));
    computeProposals();
    // AAA
    {
      CompletionSuggestion suggestion = assertHasSuggestion("AAA");
      assertSame(CompletionSuggestionKind.CLASS, suggestion.getKind());
      assertEquals(makeSource(//
          "/**",
          " * 000",
          " * 111",
          " * 222",
          " */"), suggestion.getComment());
    }
  }

  public void test_class_comment_lines() throws Exception {
    createContextWithSingleSource(makeSource(//
        "/// 000",
        "/// 111",
        "/// 222",
        "class AAA {",
        "}",
        "main() {",
        "  AA!",
        "}"));
    computeProposals();
    // AAA
    {
      CompletionSuggestion suggestion = assertHasSuggestion("AAA");
      assertSame(CompletionSuggestionKind.CLASS, suggestion.getKind());
      assertEquals("/// 000\n" + "/// 111\n" + "/// 222\n", suggestion.getComment());
    }
  }

  public void test_computeDocumentationComment_exception() throws Exception {
    Element element = mock(Element.class);
    Error exception = new Error();
    when(element.computeDocumentationComment()).thenThrow(exception);
    try {
      DartUnitCompletionSuggestionsComputer.computeDocumentationComment(element);
    } catch (Throwable e) {
      assertSame(exception, e);
    }
  }

  public void test_computeDocumentationComment_OK() throws Exception {
    Element element = mock(Element.class);
    when(element.computeDocumentationComment()).thenReturn("my comment");
    assertEquals(
        "my comment",
        DartUnitCompletionSuggestionsComputer.computeDocumentationComment(element));
  }

  public void test_method() throws Exception {
    createContextWithSingleSource(makeSource(//
        "class A {",
        "  foo(int a, List<String> b, c) {}",
        "  String bar(int a, [double b]) {}",
        "  baz(int a, {double b}) {}",
        "}",
        "main(A a) {",
        "  a.!",
        "}"));
    computeProposals();
    // foo
    {
      CompletionSuggestion suggestion = assertHasSuggestion("foo");
      assertSame(CompletionSuggestionKind.METHOD, suggestion.getKind());
      assertEquals("A", suggestion.getDeclaringType());
      assertEquals(completionOffset, suggestion.getLocation());
      assertThat(suggestion.getParameterNames()).isEqualTo(new String[] {"a", "b", "c"});
      assertThat(suggestion.getParameterTypes()).isEqualTo(
          new String[] {"int", "List<String>", "dynamic"});
      assertEquals(3, suggestion.getPositionalParameterCount());
      assertThat(suggestion.getRelevance()).isGreaterThanOrEqualTo(
          CompletionSuggestion.RELEVANCE_DEFAULT);
      assertEquals(3, suggestion.getReplacementLength());
      assertEquals(0, suggestion.getReplacementLengthIdentifier());
      assertEquals("dynamic", suggestion.getReturnType());
      assertEquals(false, suggestion.hasNamed());
      assertEquals(false, suggestion.hasPositional());
      assertEquals(false, suggestion.isDeprecated());
      assertEquals(false, suggestion.isPotentialMatch());
    }
    // bar
    {
      CompletionSuggestion suggestion = assertHasSuggestion("bar");
      assertSame(CompletionSuggestionKind.METHOD, suggestion.getKind());
      assertEquals("A", suggestion.getDeclaringType());
      assertEquals(completionOffset, suggestion.getLocation());
      assertThat(suggestion.getParameterNames()).isEqualTo(new String[] {"a", "b"});
      assertThat(suggestion.getParameterTypes()).isEqualTo(new String[] {"int", "double"});
      assertEquals(1, suggestion.getPositionalParameterCount());
      assertThat(suggestion.getRelevance()).isGreaterThanOrEqualTo(
          CompletionSuggestion.RELEVANCE_DEFAULT);
      assertEquals(3, suggestion.getReplacementLength());
      assertEquals(0, suggestion.getReplacementLengthIdentifier());
      assertEquals("String", suggestion.getReturnType());
      assertEquals(false, suggestion.hasNamed());
      assertEquals(true, suggestion.hasPositional());
      assertEquals(false, suggestion.isDeprecated());
      assertEquals(false, suggestion.isPotentialMatch());
    }
    // baz
    {
      CompletionSuggestion suggestion = assertHasSuggestion("baz");
      assertSame(CompletionSuggestionKind.METHOD, suggestion.getKind());
      assertThat(suggestion.getParameterNames()).isEqualTo(new String[] {"a", "b"});
      assertThat(suggestion.getParameterTypes()).isEqualTo(new String[] {"int", "double"});
      assertEquals(1, suggestion.getPositionalParameterCount());
      assertEquals(true, suggestion.hasNamed());
      assertEquals(false, suggestion.hasPositional());
      assertEquals(false, suggestion.isDeprecated());
      assertEquals(false, suggestion.isPotentialMatch());
    }
  }

  public void test_optionalArgument_positional() throws Exception {
    createContextWithSingleSource(makeSource(//
        "f([int x]) {}",
        "main() {",
        "  f(!);",
        "}"));

    computeProposals();
    // [int x]
    {
      CompletionSuggestion suggestion = assertHasSuggestion("x");
      assertSame(CompletionSuggestionKind.OPTIONAL_ARGUMENT, suggestion.getKind());
      assertEquals(0, suggestion.getReplacementLength());
      assertEquals(0, suggestion.getReplacementLengthIdentifier());
      assertEquals("int", suggestion.getParameterType());
      assertEquals("x", suggestion.getParameterName());
    }
  }

  public void test_parameter_replacementLengthIdentifier() throws Exception {
    createContextWithSingleSource(makeSource(//
        "main(int foo) {",
        "  fo!.toString();",
        "}"));

    computeProposals();
    // foo
    {
      CompletionSuggestion suggestion = assertHasSuggestion("foo");
      assertSame(CompletionSuggestionKind.PARAMETER, suggestion.getKind());
      assertEquals(3, suggestion.getReplacementLength());
      assertEquals(2, suggestion.getReplacementLengthIdentifier());
    }
  }

  private void addTestSource(String code) {
    this.completionOffset = code.indexOf('!');
    code = StringUtils.remove(code, '!');
    this.source = addSource(contextId, "/test.dart", code);
  }

  private CompletionSuggestion assertHasSuggestion(String exCompletion) {
    for (CompletionSuggestion suggestion : suggestions) {
      if (suggestion.getCompletion().equals(exCompletion)) {
        return suggestion;
      }
    }
    fail("No suggestion '" + exCompletion + "' in\n" + StringUtils.join(suggestions, "\n"));
    return null;
  }

  private void computeProposals() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    server.computeCompletionSuggestions(
        contextId,
        source,
        completionOffset,
        new CompletionSuggestionsConsumer() {
          @Override
          public void computed(CompletionSuggestion[] _suggestions) {
            Collections.addAll(suggestions, _suggestions);
            latch.countDown();
          }
        });
    latch.await(600, TimeUnit.SECONDS);
  }

  private void createContextWithSingleSource(String code) {
    createTestContext();
    addTestSource(code);
  }

  private void createTestContext() {
    this.contextId = createContext("test");
  }
}
