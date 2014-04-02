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

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.EmptyFunctionBody;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.Context.ConstructorDescription;

import static com.google.dart.java2dart.util.AstFactory.emptyFunctionBody;
import static com.google.dart.java2dart.util.AstFactory.identifier;
import static com.google.dart.java2dart.util.AstFactory.redirectingConstructorInvocation;
import static com.google.dart.java2dart.util.AstFactory.simpleFormalParameter;
import static com.google.dart.java2dart.util.AstFactory.typeName;

import org.eclipse.jdt.core.dom.IMethodBinding;

import java.util.Iterator;
import java.util.List;

/**
 * Simplifies generated Dart constructors.
 * <ul>
 * <li>If exactly one constructor that is default and just calls super, then remove it.</li>
 * <li>If constructor has block body with no statements, then removes the body.</li>
 * <li>If constructor has redirection marker invocation and no other statements, then replace the
 * marker with the actual redirecting constructor invocation and removes the body.</li>
 * <li>If constructor is an enum constructor with redirecting constructor invocation, then make its
 * "name" and "ordinal" field formal initializing parameters the normal formal parameters and pass
 * them into the redirecting constructor invocation.</li>
 * </ul>
 */
public class ConstructorSemanticProcessor extends SemanticProcessor {

  /**
   * @return {@code true} if the given {@link ConstructorDeclaration} has no or empty body.
   */
  private static boolean hasEmptyBody(ConstructorDeclaration constructor) {
    FunctionBody body = constructor.getBody();
    // no body at all
    if (body == null) {
      return true;
    }
    if (body instanceof EmptyFunctionBody) {
      return true;
    }
    // block body without statements
    if (body instanceof BlockFunctionBody) {
      Block block = ((BlockFunctionBody) body).getBlock();
      return block.getStatements().isEmpty();
    }
    // expression body (probably never happens)
    return false;
  }

  /**
   * @return {@code true} if the constructor is default, has empty body, no initializers or one
   *         initializer calling default super constructor.
   */
  private static boolean hasNoParamAndOnlyCallsSuper(ConstructorDeclaration constructor) {
    // no parameters
    if (!constructor.getParameters().getParameters().isEmpty()) {
      return false;
    }
    // empty body
    if (!hasEmptyBody(constructor)) {
      return false;
    }
    // at most one initializer allowed
    NodeList<ConstructorInitializer> initializers = constructor.getInitializers();
    if (initializers.size() == 0) {
      return true;
    }
    if (initializers.size() > 1) {
      return false;
    }
    // check that the only initializer is "super" constructor invocation
    ConstructorInitializer initializer = initializers.get(0);
    if (!(initializer instanceof SuperConstructorInvocation)) {
      return false;
    }
    SuperConstructorInvocation superInitializer = (SuperConstructorInvocation) initializer;
    if (!superInitializer.getArgumentList().getArguments().isEmpty()) {
      return false;
    }
    // OK, there is only default "super" constructor invocation
    return true;
  }

  public ConstructorSemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(CompilationUnit unit) {
    unit.accept(new GeneralizingAstVisitor<Void>() {
      List<ConstructorDeclaration> allConstructors = Lists.newArrayList();;

      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        allConstructors.clear();
        super.visitClassDeclaration(node);
        // remove constructor if it is default and calls default super constructor
        if (allConstructors.size() == 1) {
          ConstructorDeclaration constructor = allConstructors.get(0);
          if (hasNoParamAndOnlyCallsSuper(constructor)) {
            node.getMembers().remove(allConstructors.remove(0));
          }
        }
        // done
        return null;
      }

      @Override
      public Void visitConstructorDeclaration(ConstructorDeclaration node) {
        allConstructors.add(node);
        replaceThisInvocationMarkerWithRedirection(node);
        // remove empty body
        if (hasEmptyBody(node)) {
          node.setBody(emptyFunctionBody());
        }
        // done
        return null;
      }
    });
  }

  /**
   * If the given {@link ConstructorDeclaration} has only statement with marker for "this"
   * constructor redirection, then replace it with the real redirection. This redirection will still
   * have temporary name, will be replace with the real name during constructors rename step.
   */
  private void replaceThisInvocationMarkerWithRedirection(ConstructorDeclaration node) {
    // prepare statements
    FunctionBody body = node.getBody();
    if (!(body instanceof BlockFunctionBody)) {
      return;
    }
    Block block = ((BlockFunctionBody) body).getBlock();
    NodeList<Statement> statements = block.getStatements();
    // we support here only one statement
    if (statements.size() != 1) {
      return;
    }
    Statement statement = statements.get(0);
    // the statement should be "thisConstructorRedirection" marker invocation
    if (!(statement instanceof ExpressionStatement)) {
      return;
    }
    Expression expression = ((ExpressionStatement) statement).getExpression();
    if (!(expression instanceof MethodInvocation)) {
      return;
    }
    MethodInvocation methodInvocation = (MethodInvocation) expression;
    if (methodInvocation.getTarget() != null
        || !methodInvocation.getMethodName().getName().equals("thisConstructorRedirection")) {
      return;
    }
    // add redirecting constructor invocation
    List<ConstructorInitializer> initializers = node.getInitializers();
    RedirectingConstructorInvocation redirect = redirectingConstructorInvocation(
        "thisConstructorRedirection",
        methodInvocation.getArgumentList().getArguments());
    initializers.add(redirect);
    // remove speculative "super" constructor invocation
    for (Iterator<ConstructorInitializer> iter = initializers.iterator(); iter.hasNext();) {
      ConstructorInitializer initializer = iter.next();
      if (initializer instanceof SuperConstructorInvocation) {
        iter.remove();
      }
    }
    // remove body
    node.setBody(emptyFunctionBody());
    // record constructor invocation
    IMethodBinding binding = (IMethodBinding) context.getNodeBinding(methodInvocation);
    ConstructorDescription description = context.getConstructorDescription(binding);
    description.redirectingInvocations.add(redirect);
    // tweak enum constructor
    if (description.isEnum) {
      List<Expression> arguments = redirect.getArgumentList().getArguments();
      updateRedirectingEnumConstructorParameters(node, arguments);
    }
  }

  /**
   * When we translate Java enum constructor, we generate field formal parameters "name" and
   * "ordinal". However if constructor is actually redirecting constructor, there parameters should
   * not be field parameters, they should be normal ones and passed into
   * {@link RedirectingConstructorInvocation}.
   */
  private void updateRedirectingEnumConstructorParameters(ConstructorDeclaration node,
      List<Expression> arguments) {
    NodeList<FormalParameter> parameters = node.getParameters().getParameters();
    parameters.set(0, simpleFormalParameter(typeName("String"), identifier("name")));
    parameters.set(1, simpleFormalParameter(typeName("int"), identifier("ordinal")));
    arguments.add(0, identifier("name"));
    arguments.add(1, identifier("ordinal"));
  }
}
