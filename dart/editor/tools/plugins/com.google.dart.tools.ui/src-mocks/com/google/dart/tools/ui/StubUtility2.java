/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui;

import com.google.dart.compiler.resolver.ClassElement;
import com.google.dart.core.dom.IFunctionBinding;
import com.google.dart.tools.core.dom.AST;
import com.google.dart.tools.core.dom.rewrite.ASTRewrite;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.ui.internal.text.dart.ImportRewrite;

/**
 * TODO(brianwilkerson): This is a temporary interface, used to resolve compilation errors.
 */
public class StubUtility2 extends StubUtility {
  public static FunctionDeclaration createImplementationStub(CompilationUnit fCompilationUnit,
      ASTRewrite rewrite, ImportRewrite importRewrite, IFunctionBinding iFunctionBinding,
      String name, boolean b, CodeGenerationSettings settings) {
    return null;
  }

  public static IFunctionBinding[] getOverridableMethods(AST ast, ClassElement binding, boolean b) {
    return null;
  }
}
