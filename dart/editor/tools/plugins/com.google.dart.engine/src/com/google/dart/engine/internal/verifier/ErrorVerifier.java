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
package com.google.dart.engine.internal.verifier;

import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

/**
 * Instances of the class {@code ErrorVerifier} traverse an AST structure looking for additional
 * errors and warnings not covered by the parser and resolver.
 */
public class ErrorVerifier extends RecursiveASTVisitor<Void> {
  /**
   * The error reporter by which errors will be reported.
   */
  private ErrorReporter errorReporter;

  public ErrorVerifier(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    TypeName typeName = node.getConstructorName().getType();
    Type createdType = typeName.getType();
    if (createdType instanceof InterfaceType) {
      if (((InterfaceType) createdType).getElement().isAbstract()) {
        if (((KeywordToken) node.getKeyword()).getKeyword() == Keyword.CONST) {
          errorReporter.reportError(StaticWarningCode.CONST_WITH_ABSTRACT_CLASS, typeName);
        } else {
          errorReporter.reportError(StaticWarningCode.NEW_WITH_ABSTRACT_CLASS, typeName);
        }
      }
    } else {
      errorReporter.reportError(CompileTimeErrorCode.NON_CONSTANT_MAP_KEY, typeName);
    }
    return null;
  }
}
