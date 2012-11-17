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
package com.google.dart.engine.resolver.scope;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.ErrorSeverity;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.resolver.ResolverTestCase;

import static com.google.dart.engine.ast.ASTFactory.identifier;

public class ScopeTest extends ResolverTestCase {
  /**
   * A non-abstract subclass that can be used for testing purposes.
   */
  private static class TestScope extends Scope {
    /**
     * The element representing the library in which this scope is enclosed.
     */
    private LibraryElement definingLibrary;

    /**
     * The listener that is to be informed when an error is encountered.
     */
    private AnalysisErrorListener errorListener;

    /**
     * The element that should be returned from {@link #lookup(String, LibraryElement)} as if it
     * were defined in an outer scope.
     */
    private Element lookupResult;

    private TestScope(LibraryElement definingLibrary, AnalysisErrorListener errorListener) {
      this.definingLibrary = definingLibrary;
      this.errorListener = errorListener;
    }

    @Override
    public LibraryElement getDefiningLibrary() {
      return definingLibrary;
    }

    @Override
    public AnalysisErrorListener getErrorListener() {
      return errorListener;
    }

    public void setLookupResult(Element element) {
      lookupResult = element;
    }

    @Override
    protected Element lookup(String name, LibraryElement referencingLibrary) {
      if (lookupResult != null) {
        return lookupResult;
      }
      return localLookup(name, referencingLibrary);
    }
  }

  public void test_define_duplicate() {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    TestScope scope = new TestScope(definingLibrary, errorListener);
    VariableElement element1 = new VariableElementImpl(identifier("v1"));
    VariableElement element2 = new VariableElementImpl(identifier("v1"));
    scope.define(element1);
    scope.define(element2);
    errorListener.assertErrors(ErrorSeverity.ERROR);
  }

  public void test_define_hiding() {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    TestScope scope = new TestScope(definingLibrary, errorListener);
    VariableElement element1 = new VariableElementImpl(identifier("v1"));
    VariableElement element2 = new VariableElementImpl(identifier("v1"));
    scope.setLookupResult(element1);
    scope.define(element2);
    errorListener.assertErrors(ErrorSeverity.WARNING);
  }

  public void test_define_normal() {
    LibraryElement definingLibrary = createTestLibrary();
    GatheringErrorListener errorListener = new GatheringErrorListener();
    TestScope scope = new TestScope(definingLibrary, errorListener);
    VariableElement element1 = new VariableElementImpl(identifier("v1"));
    VariableElement element2 = new VariableElementImpl(identifier("v2"));
    scope.define(element1);
    scope.define(element2);
    errorListener.assertNoErrors();
  }

  public void test_getDefiningLibrary() throws Exception {
    LibraryElement definingLibrary = createTestLibrary();
    Scope scope = new TestScope(definingLibrary, null);
    assertEquals(definingLibrary, scope.getDefiningLibrary());
  }

  public void test_getErrorListener() throws Exception {
    LibraryElement definingLibrary = new LibraryElementImpl(identifier("test"));
    GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope scope = new TestScope(definingLibrary, errorListener);
    assertEquals(errorListener, scope.getErrorListener());
  }

  public void test_isPrivateName_nonPrivate() {
    assertFalse(Scope.isPrivateName("Public"));
  }

  public void test_isPrivateName_private() {
    assertTrue(Scope.isPrivateName("_Private"));
  }
}
