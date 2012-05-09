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
package com.google.dart.tools.internal.corext.refactoring.rename;

import com.google.dart.tools.core.model.CompilationUnitElement;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.SourceRange;

import org.eclipse.core.runtime.Assert;

/**
 * Wrapper around {@link CompilationUnitElement} in {@link DartFunction}. Right now just
 * {@link DartVariableDeclaration} and local {@link DartFunction}.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public final class FunctionLocalElement {
  private final CompilationUnitElement element;
  private final SourceRange visibleRange;

  public FunctionLocalElement(DartFunction function) throws DartModelException {
    this(function, function.getVisibleRange());
    Assert.isLegal(function.isLocal());
  }

  public FunctionLocalElement(DartVariableDeclaration variable) throws DartModelException {
    this(variable, variable.getVisibleRange());
    Assert.isLegal(variable.isLocal());
  }

  private FunctionLocalElement(CompilationUnitElement element, SourceRange visibleRange) {
    this.element = element;
    this.visibleRange = visibleRange;
  }

  /**
   * @return the wrapped {@link CompilationUnitElement}.
   */
  public CompilationUnitElement getElement() {
    return element;
  }

  /**
   * @return the {@link DartElement#getElementName()} result.
   */
  public String getElementName() {
    return element.getElementName();
  }

  /**
   * @return the {@link SourceRange} in which this {@link DartElement} is visible.
   */
  public SourceRange getVisibleRange() {
    return visibleRange;
  }
}
