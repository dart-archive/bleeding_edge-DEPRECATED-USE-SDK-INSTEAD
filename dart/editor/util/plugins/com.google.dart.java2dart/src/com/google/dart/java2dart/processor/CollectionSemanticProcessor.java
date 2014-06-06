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
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.java2dart.Context;
import com.google.dart.java2dart.util.JavaUtils;
import com.google.dart.java2dart.util.TokenFactory;

import static com.google.dart.java2dart.util.AstFactory.assignmentExpression;
import static com.google.dart.java2dart.util.AstFactory.functionExpression;
import static com.google.dart.java2dart.util.AstFactory.functionExpressionInvocation;
import static com.google.dart.java2dart.util.AstFactory.identifier;
import static com.google.dart.java2dart.util.AstFactory.indexExpression;
import static com.google.dart.java2dart.util.AstFactory.instanceCreationExpression;
import static com.google.dart.java2dart.util.AstFactory.methodInvocation;
import static com.google.dart.java2dart.util.AstFactory.propertyAccess;
import static com.google.dart.java2dart.util.AstFactory.typeName;
import static com.google.dart.java2dart.util.TokenFactory.token;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.List;

/**
 * {@link SemanticProcessor} for Java <code>java.util</code> collections.
 */
public class CollectionSemanticProcessor extends SemanticProcessor {
  public CollectionSemanticProcessor(Context context) {
    super(context);
  }

  @Override
  public void process(final CompilationUnit unit) {
    unit.accept(new GeneralizingAstVisitor<Void>() {
      @Override
      public Void visitCompilationUnit(CompilationUnit node) {
        List<CompilationUnitMember> declarations = Lists.newArrayList(unit.getDeclarations());
        for (CompilationUnitMember member : declarations) {
          member.accept(this);
        }
        return null;
      }

      @Override
      public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
        super.visitInstanceCreationExpression(node);
        Object binding = context.getNodeBinding(node);
        if (binding instanceof IMethodBinding) {
          IMethodBinding methodBinding = (IMethodBinding) binding;
          ITypeBinding declaringClass = methodBinding.getDeclaringClass();
          // new HashSet(Map) -> new Set.from(Map)
          if (isMethodInClass2(methodBinding, "<init>(java.util.Collection)", "java.util.HashSet")) {
            node.getConstructorName().setName(identifier("from"));
            return null;
          }
          // new HashSet(5) -> new HashSet()
          if (JavaUtils.getQualifiedName(declaringClass).equals("java.util.HashSet")) {
            node.getArgumentList().getArguments().clear();
            return null;
          }
          // new HashMap(Map) -> new HashMap.from(Map)
          if (isMethodInClass2(methodBinding, "<init>(java.util.Map)", "java.util.HashMap")) {
            node.getConstructorName().setName(identifier("from"));
            return null;
          }
          // new HashMap(5) -> new HashMap()
          if (JavaUtils.getQualifiedName(declaringClass).equals("java.util.HashMap")) {
            ((SimpleIdentifier) node.getConstructorName().getType().getName()).setToken(token("HashMap"));
            node.getArgumentList().getArguments().clear();
            return null;
          }
          // new ArrayList(Collection) -> new List.from(Iterable)
          if (isMethodInClass2(methodBinding, "<init>(java.util.Collection)", "java.util.ArrayList")) {
            node.getConstructorName().setName(identifier("from"));
            return null;
          }
          // new ArrayList(5) -> new List()
          if (isMethodInClass2(methodBinding, "<init>(int)", "java.util.ArrayList")) {
            node.getArgumentList().getArguments().clear();
            return null;
          }
          // translate java.util.Comparator to function expression
          if (methodBinding.isConstructor() && declaringClass.isAnonymous()) {
            ITypeBinding[] intfs = declaringClass.getInterfaces();
            if (intfs.length == 1
                && JavaUtils.getQualifiedName(intfs[0]).equals("java.util.Comparator")) {
              ClassDeclaration innerClass = context.getAnonymousDeclaration(node);
              if (innerClass != null) {
                unit.getDeclarations().remove(innerClass);
                List<ClassMember> innerMembers = innerClass.getMembers();
                MethodDeclaration compareMethod = (MethodDeclaration) innerMembers.get(0);
                FunctionExpression functionExpression = functionExpression(
                    compareMethod.getParameters(),
                    compareMethod.getBody());
                // don't add ";" at the end of ExpressionFunctionBody
                if (compareMethod.getBody() instanceof ExpressionFunctionBody) {
                  ExpressionFunctionBody expressionFunctionBody = (ExpressionFunctionBody) compareMethod.getBody();
                  expressionFunctionBody.setSemicolon(null);
                }
                // do replace
                replaceNode(node, functionExpression);
              }
            }
          }
        }
        // done
        return null;
      }

      @Override
      public Void visitMethodInvocation(MethodInvocation node) {
        super.visitMethodInvocation(node);
        Expression target = node.getTarget();
        SimpleIdentifier nameNode = node.getMethodName();
        List<Expression> args = node.getArgumentList().getArguments();
        if (isMethodInClass(node, "compare", "java.util.Comparator")) {
          replaceNode(node, functionExpressionInvocation(target, args));
          return null;
        }
        if (isMethodInClass(node, "size", "java.util.Collection")
            || isMethodInClass(node, "size", "java.util.Map")) {
          replaceNode(node, propertyAccess(target, nameNode));
          nameNode.setToken(token("length"));
          return null;
        }
        if (isMethodInClass(node, "isEmpty", "java.util.Collection")) {
          replaceNode(node, propertyAccess(target, nameNode));
          return null;
        }
        if (isMethodInClass(node, "get", "java.util.List")
            || isMethodInClass(node, "get", "java.util.Map")) {
          replaceNode(node, indexExpression(target, args.get(0)));
          return null;
        }
        if (isMethodInClass(node, "toArray", "java.util.Collection")) {
          replaceNode(
              node,
              instanceCreationExpression(Keyword.NEW, typeName("List"), "from", target));
          return null;
        }
        if (isMethodInClass(node, "iterator", "java.util.Collection")) {
          replaceNode(
              node,
              instanceCreationExpression(Keyword.NEW, typeName("JavaIterator"), target));
          return null;
        }
        if (isMethodInClass(node, "hasNext", "java.util.Iterator")) {
          replaceNode(node, propertyAccess(target, nameNode));
          return null;
        }
        if (isMethodInClass(node, "containsAll", "java.util.Collection")) {
          replaceNode(node, methodInvocation("javaCollectionContainsAll", target, args.get(0)));
          return null;
        }
        if (isMethodInClass(node, "isEmpty", "java.util.Map")) {
          replaceNode(node, propertyAccess(target, nameNode));
          return null;
        }
        if (isMethodInClass(node, "put", "java.util.Map")) {
          if (node.getParent() instanceof ExpressionStatement) {
            IndexExpression indexExpression = indexExpression(target, args.get(0));
            AssignmentExpression assignment = assignmentExpression(
                indexExpression,
                TokenType.EQ,
                args.get(1));
            replaceNode(node, assignment);
          } else {
            replaceNode(node, methodInvocation("javaMapPut", target, args.get(0), args.get(1)));
          }
          return null;
        }
        if (isMethodInClass(node, "entrySet", "java.util.Map")) {
          replaceNode(node, methodInvocation("getMapEntrySet", target));
          return null;
        }
        if (isMethodInClass(node, "values", "java.util.Map")) {
          replaceNode(node, propertyAccess(target, nameNode));
          return null;
        }
        if (isMethodInClass(node, "keySet", "java.util.Map")) {
          nameNode.setToken(token("keys"));
          replaceNode(node, methodInvocation(propertyAccess(target, nameNode), "toSet"));
          return null;
        }
        if (isMethodInClass2(node, "remove(int)", "java.util.List")) {
          nameNode.setToken(TokenFactory.token("removeAt"));
          return null;
        }
        if (isMethodInClass2(node, "add(int,java.lang.Object)", "java.util.List")) {
          nameNode.setToken(TokenFactory.token("insert"));
          return null;
        }
        if (isMethodInClass(node, "set", "java.util.List")) {
          replaceNode(node, methodInvocation("javaListSet", target, args.get(0), args.get(1)));
          return null;
        }
        if (isMethodInClass(node, "putAll", "java.util.Map")) {
          nameNode.setToken(TokenFactory.token("addAll"));
          return null;
        }
        if (isMethodInClass(node, "addAll", "java.util.Collections")) {
          replaceNode(node, methodInvocation(args.get(0), "addAll", args.get(1)));
          return null;
        }
        if (isMethodInClass(node, "unmodifiableList", "java.util.Collections")) {
          replaceNode(
              node,
              instanceCreationExpression(Keyword.NEW, typeName("UnmodifiableListView"), args.get(0)));
          return null;
        }
        if (isMethodInClass(node, "sort", "java.util.Arrays")) {
          if (args.size() == 1) {
            replaceNode(node, methodInvocation(args.get(0), "sort"));
          } else {
            replaceNode(node, methodInvocation(args.get(0), "sort", args.get(1)));
          }
          return null;
        }
        if (isMethodInClass(node, "hashCode", "java.util.Arrays")) {
          nameNode.setToken(token("makeHashCode"));
          return null;
        }
        if (isMethodInClass(node, "sort", "java.util.Collections")) {
          replaceNode(node, methodInvocation(args.get(0), "sort", args.get(1)));
          return null;
        }
        if (isMethodInClass(node, "add", "org.apache.commons.lang3.ArrayUtils") && args.size() == 3) {
          nameNode.setToken(TokenFactory.token("addAt"));
          return null;
        }
        if (isMethodInClass(node, "noneOf", "java.util.EnumSet")) {
          replaceNode(node, instanceCreationExpression(Keyword.NEW, typeName("HashSet")));
          return null;
        }
        return null;
      }

      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        Object binding = context.getNodeBinding(node);
        if (JavaUtils.isTypeNamed(binding, "java.util.Arrays")) {
          replaceNode(node, identifier("JavaArrays"));
          return null;
        }
        return super.visitSimpleIdentifier(node);
      }

      @Override
      public Void visitTypeName(TypeName node) {
        super.visitTypeName(node);
        ITypeBinding binding = context.getNodeTypeBinding(node);
        if (node.getName() instanceof SimpleIdentifier) {
          SimpleIdentifier nameNode = (SimpleIdentifier) node.getName();
          String name = nameNode.getName();
          if (JavaUtils.isTypeNamed(binding, "java.util.Collection")) {
            nameNode.setToken(token("Iterable"));
            return null;
          }
          if (JavaUtils.isTypeNamed(binding, "java.util.ArrayList")) {
            nameNode.setToken(token("List"));
            return null;
          }
          if (JavaUtils.isTypeNamed(binding, "java.util.LinkedList")) {
            nameNode.setToken(token("Queue"));
            return null;
          }
          if ("EnumSet".equals(name)) {
            nameNode.setToken(token("HashSet"));
            return null;
          }
          if ("HashSet".equals(name)) {
            nameNode.setToken(token("HashSet"));
            return null;
          }
          if ("HashMap".equals(name)) {
            nameNode.setToken(token("HashMap"));
            return null;
          }
          if (JavaUtils.isTypeNamed(binding, "java.util.Map.Entry")) {
            nameNode.setToken(token("MapEntry"));
            return null;
          }
          if (JavaUtils.isTypeNamed(binding, "java.util.Iterator")) {
            nameNode.setToken(token("JavaIterator"));
            return null;
          }
        }
        return null;
      }
    });
  }
}
