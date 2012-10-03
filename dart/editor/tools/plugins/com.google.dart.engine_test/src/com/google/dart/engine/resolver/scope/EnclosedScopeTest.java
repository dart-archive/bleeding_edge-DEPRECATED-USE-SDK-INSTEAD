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
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.resolver.ResolverTestCase;

import static com.google.dart.engine.ast.ASTFactory.identifier;

public class EnclosedScopeTest extends ResolverTestCase {
  public void test_define_duplicate() {
    final LibraryElement definingLibrary = createTestLibrary();
    final GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope rootScope = new Scope() {
      @Override
      public LibraryElement getDefiningLibrary() {
        return definingLibrary;
      }

      @Override
      public AnalysisErrorListener getErrorListener() {
        return errorListener;
      }

      @Override
      public Element lookup(String name, LibraryElement referencingLibrary) {
        return null;
      }
    };
    EnclosedScope scope = new EnclosedScope(rootScope);
    VariableElement element1 = new VariableElementImpl(identifier("v1"));
    VariableElement element2 = new VariableElementImpl(identifier("v1"));
    scope.define(element1);
    scope.define(element2);
    errorListener.assertErrors(ErrorSeverity.ERROR);
  }

  public void test_define_hiding() {
    final LibraryElement definingLibrary = createTestLibrary();
    final GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope rootScope = new Scope() {
      @Override
      public LibraryElement getDefiningLibrary() {
        return definingLibrary;
      }

      @Override
      public AnalysisErrorListener getErrorListener() {
        return errorListener;
      }

      @Override
      public Element lookup(String name, LibraryElement referencingLibrary) {
        return null;
      }
    };
    EnclosedScope outerScope = new EnclosedScope(rootScope);
    EnclosedScope innerScope = new EnclosedScope(outerScope);
    VariableElement element1 = new VariableElementImpl(identifier("v1"));
    VariableElement element2 = new VariableElementImpl(identifier("v1"));
    outerScope.define(element1);
    innerScope.define(element2);
    errorListener.assertErrors(ErrorSeverity.WARNING);
  }

  public void test_define_normal() {
    final LibraryElement definingLibrary = createTestLibrary();
    final GatheringErrorListener errorListener = new GatheringErrorListener();
    Scope rootScope = new Scope() {
      @Override
      public LibraryElement getDefiningLibrary() {
        return definingLibrary;
      }

      @Override
      public AnalysisErrorListener getErrorListener() {
        return errorListener;
      }

      @Override
      public Element lookup(String name, LibraryElement referencingLibrary) {
        return null;
      }
    };
    EnclosedScope outerScope = new EnclosedScope(rootScope);
    EnclosedScope innerScope = new EnclosedScope(outerScope);
    VariableElement element1 = new VariableElementImpl(identifier("v1"));
    VariableElement element2 = new VariableElementImpl(identifier("v2"));
    outerScope.define(element1);
    innerScope.define(element2);
    errorListener.assertNoErrors();
  }
}
