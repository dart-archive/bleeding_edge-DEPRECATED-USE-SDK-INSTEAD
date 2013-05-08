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
package com.google.dart.java2dart.processor;

import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.java2dart.Context;

import java.util.ArrayList;

/**
 * Simplifies generated of Dart constructors.
 * <ul>
 * <li>if exactly one constructor that just calls super, then remove it</li>
 * </ul>
 */
public class ConstructorSemanticProcessor extends SemanticProcessor {
  public ConstructorSemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(CompilationUnit unit) {
    unit.accept(new GeneralizingASTVisitor<Void>() {
      ArrayList<ConstructorDeclaration> allConstructors = new ArrayList<ConstructorDeclaration>();

      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        allConstructors.clear();
        Void result = super.visitClassDeclaration(node);

        // Remove constructor if only one with no param and all it does is call super
        if (allConstructors.size() == 1) {
          ConstructorDeclaration constructor = allConstructors.get(0);
          if (hasNoParamAndOnlyCallsSuper(constructor)) {
            node.getMembers().remove(allConstructors.remove(0));
          }
        }

        return result;
      }

      @Override
      public Void visitConstructorDeclaration(ConstructorDeclaration node) {
        allConstructors.add(node);
        return super.visitConstructorDeclaration(node);
      }
    });
  }

  /**
   * Answer {@code true} if the constructor does not have any parameters and does not have any
   * statements other than a single call to the super constructor.
   */
  private boolean hasNoParamAndOnlyCallsSuper(ConstructorDeclaration constructor) {
    if (constructor.getParameters().getParameters().size() > 0) {
      return false;
    }
    if (!(constructor.getBody() instanceof BlockFunctionBody)) {
      return false;
    }
    BlockFunctionBody body = (BlockFunctionBody) constructor.getBody();
    if (body.getBlock().getStatements().size() > 0) {
      return false;
    }
    NodeList<ConstructorInitializer> initializers = constructor.getInitializers();
    if (initializers.size() == 0) {
      return true;
    }
    if (initializers.size() > 1) {
      return false;
    }
    ConstructorInitializer initializer = initializers.get(0);
    if (!(initializer instanceof SuperConstructorInvocation)) {
      return false;
    }
    SuperConstructorInvocation superInitializer = (SuperConstructorInvocation) initializer;
    if (superInitializer.getArgumentList().getArguments().size() > 0) {
      return false;
    }
    return true;
  }
}
