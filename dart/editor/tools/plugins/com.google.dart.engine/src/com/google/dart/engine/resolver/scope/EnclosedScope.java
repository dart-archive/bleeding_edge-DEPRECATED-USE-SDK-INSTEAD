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
import com.google.dart.engine.error.AnalysisErrorListener;

/**
 * Instances of the class {@code EnclosedScope} implement a scope that is lexically enclosed in
 * another scope.
 */
public class EnclosedScope extends Scope {
  /**
   * The scope in which this scope is lexically enclosed.
   */
  private Scope enclosingScope;

  /**
   * Initialize a newly created scope enclosed within another scope.
   * 
   * @param enclosingScope the scope in which this scope is lexically enclosed
   */
  public EnclosedScope(Scope enclosingScope) {
    this.enclosingScope = enclosingScope;
  }

  @Override
  public LibraryElement getDefiningLibrary() {
    return enclosingScope.getDefiningLibrary();
  }

  @Override
  public AnalysisErrorListener getErrorListener() {
    return enclosingScope.getErrorListener();
  }

  /**
   * Return the scope in which this scope is lexically enclosed.
   * 
   * @return the scope in which this scope is lexically enclosed
   */
  protected Scope getEnclosingScope() {
    return enclosingScope;
  }

  @Override
  protected Element lookup(String name, LibraryElement referencingLibrary) {
    Element element = localLookup(name, referencingLibrary);
    if (element != null) {
      return element;
    }
    return enclosingScope.lookup(name, referencingLibrary);
  }
}
