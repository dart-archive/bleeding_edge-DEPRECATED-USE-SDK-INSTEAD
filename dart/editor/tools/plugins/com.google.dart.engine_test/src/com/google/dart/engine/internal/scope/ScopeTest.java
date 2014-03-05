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
package com.google.dart.engine.internal.scope;

import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.ErrorSeverity;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.resolver.ResolverTestCase;

import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.element.ElementFactory.localVariableElement;

public class ScopeTest extends ResolverTestCase {
  /**
   * A non-abstract subclass that can be used for testing purposes.
   */
  private static class TestScope extends Scope {
    /**
     * The listener that is to be informed when an error is encountered.
     */
    private AnalysisErrorListener errorListener;

    private TestScope(AnalysisErrorListener errorListener) {
      this.errorListener = errorListener;
    }

    @Override
    public AnalysisErrorListener getErrorListener() {
      return errorListener;
    }

    @Override
    protected Element lookup(Identifier identifier, String name, LibraryElement referencingLibrary) {
      return localLookup(name, referencingLibrary);
    }
  }

  public void test_define_duplicate() {
    GatheringErrorListener errorListener = new GatheringErrorListener();
    TestScope scope = new TestScope(errorListener);
    VariableElement element1 = localVariableElement(identifier("v1"));
    VariableElement element2 = localVariableElement(identifier("v1"));
    scope.define(element1);
    scope.define(element2);
    errorListener.assertErrorsWithSeverities(ErrorSeverity.ERROR);
  }

  public void test_define_normal() {
    GatheringErrorListener errorListener = new GatheringErrorListener();
    TestScope scope = new TestScope(errorListener);
    VariableElement element1 = localVariableElement(identifier("v1"));
    VariableElement element2 = localVariableElement(identifier("v2"));
    scope.define(element1);
    scope.define(element2);
    errorListener.assertNoErrors();
  }

  public void test_getErrorListener() throws Exception {
    GatheringErrorListener errorListener = new GatheringErrorListener();
    TestScope scope = new TestScope(errorListener);
    assertEquals(errorListener, scope.getErrorListener());
  }

  public void test_isPrivateName_nonPrivate() {
    assertFalse(Scope.isPrivateName("Public"));
  }

  public void test_isPrivateName_private() {
    assertTrue(Scope.isPrivateName("_Private"));
  }
}
