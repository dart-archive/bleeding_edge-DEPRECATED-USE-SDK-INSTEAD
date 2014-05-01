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
package com.google.dart.engine.utilities.ast;

import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;

/**
 * This recursive Ast visitor is used to run over {@link Expression}s to determine if the expression
 * is composed by at least one deferred {@link PrefixedIdentifier}.
 * 
 * @see PrefixedIdentifier#isDeferred()
 */
public class DeferredLibraryReferenceDetector extends RecursiveAstVisitor<Void> {
  private boolean result = false;

  /**
   * Return the result, {@code true} if the visitor found a {@link PrefixedIdentifier} that returned
   * {@code true} to the {@link PrefixedIdentifier#isDeferred()} query.
   * 
   * @return {@code true} if the visitor found a {@link PrefixedIdentifier} that returned
   *         {@code true} to the {@link PrefixedIdentifier#isDeferred()} query
   */
  public boolean getResult() {
    return result;
  }

  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    // If result is already true, skip.
    if (!result) {
      // Set result to true if isDeferred() is true
      if (node.isDeferred()) {
        result = true;
      }
    }
    return null;
  }
}
