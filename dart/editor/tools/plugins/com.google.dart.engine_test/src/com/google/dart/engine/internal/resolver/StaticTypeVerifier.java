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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CommentReference;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Instances of the class {@code StaticTypeVerifier} verify that all of the nodes in an AST
 * structure that should have a static type associated with them do have a static type.
 */
public class StaticTypeVerifier extends GeneralizingASTVisitor<Void> {
  /**
   * A list containing all of the AST Expression nodes that were not resolved.
   */
  private ArrayList<Expression> unresolvedExpressions = new ArrayList<Expression>();

  /**
   * A list containing all of the AST TypeName nodes that were not resolved.
   */
  private ArrayList<TypeName> unresolvedTypes = new ArrayList<TypeName>();

  /**
   * Counter for the number of Expression nodes visited that are resolved.
   */
  int resolvedExpressionCount = 0;

  /**
   * Counter for the number of TypeName nodes visited that are resolved.
   */
  int resolvedTypeCount = 0;

  /**
   * Initialize a newly created verifier to verify that all of the nodes in an AST structure that
   * should have a static type associated with them do have a static type.
   */
  public StaticTypeVerifier() {
    super();
  }

  /**
   * Assert that all of the visited nodes have a static type associated with them.
   */
  public void assertResolved() {
    if (!unresolvedExpressions.isEmpty() || !unresolvedTypes.isEmpty()) {
      int unresolvedExpressionCount = unresolvedExpressions.size();
      int unresolvedTypeCount = unresolvedTypes.size();
      PrintStringWriter writer = new PrintStringWriter();
      writer.print("Failed to associate types with nodes: ");
      writer.print(unresolvedExpressionCount);
      writer.print("/");
      writer.print(resolvedExpressionCount + unresolvedExpressionCount);
      writer.print(" Expressions and ");
      writer.print(unresolvedTypeCount);
      writer.print("/");
      writer.print(resolvedTypeCount + unresolvedTypeCount);
      writer.println(" TypeNames.");
      if (unresolvedTypeCount > 0) {
        writer.println("TypeNames:");
        for (TypeName identifier : unresolvedTypes) {
          writer.print("  ");
          writer.print(identifier.toString());
          writer.print(" (");
          writer.print(getFileName(identifier));
          writer.print(" : ");
          writer.print(identifier.getOffset());
          writer.println(")");
        }
      }
      if (unresolvedExpressionCount > 0) {
        writer.println("Expressions:");
        for (Expression identifier : unresolvedExpressions) {
          writer.print("  ");
          writer.print(identifier.toString());
          writer.print(" (");
          writer.print(getFileName(identifier));
          writer.print(" : ");
          writer.print(identifier.getOffset());
          writer.println(")");
        }
      }
      Assert.fail(writer.toString());
    }
  }

  @Override
  public Void visitCommentReference(CommentReference node) {
    // Do nothing.
    return null;
  }

  @Override
  public Void visitExpression(Expression node) {
    node.visitChildren(this);
    if (node.getStaticType() == null) {
      unresolvedExpressions.add(node);
    } else {
      resolvedExpressionCount++;
    }
    return null;
  }

  @Override
  public Void visitLibraryIdentifier(LibraryIdentifier node) {
    // Do nothing, LibraryIdentifiers and children don't have an associated static type.
    return null;
  }

  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    // In cases where we have a prefixed identifier where the prefix is dynamic, we don't want to
    // assert that the node will have a type.
    if (node.getStaticType() == null
        && node.getPrefix().getStaticType() == DynamicTypeImpl.getInstance()) {
      return null;
    }
    return super.visitPrefixedIdentifier(node);
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    // In cases where identifiers are being used for something other than an expressions,
    // then they can be ignored.
    ASTNode parent = node.getParent();
    if (parent instanceof MethodInvocation && node == ((MethodInvocation) parent).getMethodName()) {
      return null;
    } else if (parent instanceof RedirectingConstructorInvocation
        && node == ((RedirectingConstructorInvocation) parent).getConstructorName()) {
      return null;
    } else if (parent instanceof SuperConstructorInvocation
        && node == ((SuperConstructorInvocation) parent).getConstructorName()) {
      return null;
    } else if (parent instanceof ConstructorName && node == ((ConstructorName) parent).getName()) {
      return null;
    } else if (parent instanceof Label && node == ((Label) parent).getLabel()) {
      return null;
    } else if (parent instanceof ImportDirective && node == ((ImportDirective) parent).getPrefix()) {
      return null;
    } else if (node.getElement() instanceof PrefixElement) {
      // Prefixes don't have a type.
      return null;
    }
    return super.visitSimpleIdentifier(node);
  }

  @Override
  public Void visitTypeName(TypeName node) {
    // Note: do not visit children from this node, the child SimpleIdentifier in TypeName
    // (i.e. "String") does not have a static type defined.
    if (node.getType() == null) {
      unresolvedTypes.add(node);
    } else {
      resolvedTypeCount++;
    }
    return null;
  }

  private String getFileName(ASTNode node) {
    // TODO (jwren) there are two copies of this method, one here and one in ResolutionVerifier,
    // they should be resolved into a single method
    if (node != null) {
      ASTNode root = node.getRoot();
      if (root instanceof CompilationUnit) {
        CompilationUnit rootCU = ((CompilationUnit) root);
        if (rootCU.getElement() != null) {
          return rootCU.getElement().getSource().getFullName();
        } else {
          return "<unknown file- CompilationUnit.getElement() returned null>";
        }
      } else {
        return "<unknown file- CompilationUnit.getRoot() is not a CompilationUnit>";
      }
    }
    return "<unknown file- ASTNode is null>";
  }
}
