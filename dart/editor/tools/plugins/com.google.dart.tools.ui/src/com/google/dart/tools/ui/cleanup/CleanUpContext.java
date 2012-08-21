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
package com.google.dart.tools.ui.cleanup;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;

import org.eclipse.core.runtime.Assert;

/**
 * The context that contains all information required by a clean up to create a fix.
 */
public class CleanUpContext {

  private final CompilationUnit unit;

  private final DartUnit ast;

  /**
   * Creates a new clean up context.
   * 
   * @param unit the compilation unit
   * @param ast the AST, can be <code>null</code> if {@link CleanUpRequirements#requiresAST()}
   *          returns <code>false</code>. The AST is guaranteed to contain changes made by previous
   *          clean ups only if {@link CleanUpRequirements#requiresFreshAST()} returns
   *          <code>true</code>.
   */
  public CleanUpContext(CompilationUnit unit, DartUnit ast) {
    Assert.isLegal(unit != null);
    this.unit = unit;
    this.ast = ast;
  }

  /**
   * An AST built from the compilation unit to fix.
   * <p>
   * Can be <code>null</code> if {@link CleanUpRequirements#requiresAST()} returns
   * <code>false</code>. The AST is guaranteed to contain changes made by previous clean ups only if
   * {@link CleanUpRequirements#requiresFreshAST()} returns <code>true</code>.
   * </p>
   * <p>
   * Clients should check the AST API level and do nothing if they are given an AST they can't
   * handle (see {@link org.eclipse.jdt.core.dom.AST#apiLevel()}).
   * 
   * @return an AST or <code>null</code> if none required
   */
  public DartUnit getAST() {
    return ast;
  }

  /**
   * The compilation unit to clean up.
   * 
   * @return the compilation unit to clean up
   */
  public CompilationUnit getCompilationUnit() {
    return unit;
  }
}
