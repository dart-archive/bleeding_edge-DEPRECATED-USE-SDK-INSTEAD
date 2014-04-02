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

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.CommentReference;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorFieldInitializer;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.ExportDirective;
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
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.element.PrefixElement;
import com.google.dart.engine.internal.type.DynamicTypeImpl;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.io.PrintStringWriter;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Instances of the class {@code StaticTypeVerifier} verify that all of the nodes in an AST
 * structure that should have a static type associated with them do have a static type.
 */
public class StaticTypeVerifier extends GeneralizingAstVisitor<Void> {
  /**
   * A list containing all of the AST Expression nodes that were not resolved.
   */
  private ArrayList<Expression> unresolvedExpressions = new ArrayList<Expression>();

  /**
   * A list containing all of the AST Expression nodes for which a propagated type was computed but
   * where that type was not more specific than the static type.
   */
  private ArrayList<Expression> invalidlyPropagatedExpressions = new ArrayList<Expression>();

  /**
   * A list containing all of the AST TypeName nodes that were not resolved.
   */
  private ArrayList<TypeName> unresolvedTypes = new ArrayList<TypeName>();

  /**
   * Counter for the number of Expression nodes visited that are resolved.
   */
  int resolvedExpressionCount = 0;

  /**
   * Counter for the number of Expression nodes visited that have propagated type information.
   */
  int propagatedExpressionCount = 0;

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
    if (!unresolvedExpressions.isEmpty() /*|| !invalidlyPropagatedExpressions.isEmpty()*/
        || !unresolvedTypes.isEmpty()) {
      @SuppressWarnings("resource")
      PrintStringWriter writer = new PrintStringWriter();
      int unresolvedTypeCount = unresolvedTypes.size();
      if (unresolvedTypeCount > 0) {
        writer.print("Failed to resolve ");
        writer.print(unresolvedTypeCount);
        writer.print(" of ");
        writer.print(resolvedTypeCount + unresolvedTypeCount);
        writer.println(" type names:");
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
      int unresolvedExpressionCount = unresolvedExpressions.size();
      if (unresolvedExpressionCount > 0) {
        writer.println("Failed to resolve ");
        writer.print(unresolvedExpressionCount);
        writer.print(" of ");
        writer.print(resolvedExpressionCount + unresolvedExpressionCount);
        writer.println(" expressions:");
        for (Expression expression : unresolvedExpressions) {
          writer.print("  ");
          writer.print(expression.toString());
          writer.print(" (");
          writer.print(getFileName(expression));
          writer.print(" : ");
          writer.print(expression.getOffset());
          writer.println(")");
        }
      }
      int invalidlyPropagatedExpressionCount = invalidlyPropagatedExpressions.size();
      if (invalidlyPropagatedExpressionCount > 0) {
        writer.println("Incorrectly propagated ");
        writer.print(invalidlyPropagatedExpressionCount);
        writer.print(" of ");
        writer.print(propagatedExpressionCount);
        writer.println(" expressions:");
        for (Expression expression : invalidlyPropagatedExpressions) {
          writer.print("  ");
          writer.print(expression.toString());
          writer.print(" [");
          writer.print(expression.getStaticType().getDisplayName());
          writer.print(", ");
          writer.print(expression.getPropagatedType().getDisplayName());
          writer.println("]");
          writer.print("    ");
          writer.print(getFileName(expression));
          writer.print(" : ");
          writer.print(expression.getOffset());
          writer.println(")");
        }
      }
      Assert.fail(writer.toString());
    }
  }

  @Override
  public Void visitBreakStatement(BreakStatement node) {
    // Don't visit the children because the identifier (if it exists) is not expected to have a type.
    return null;
  }

  @Override
  public Void visitCommentReference(CommentReference node) {
    // Do nothing.
    return null;
  }

  @Override
  public Void visitContinueStatement(ContinueStatement node) {
    // Don't visit the children because the identifier (if it exists) is not expected to have a type.
    return null;
  }

  @Override
  public Void visitExportDirective(ExportDirective node) {
    // Don't visit the children because they are not expected to have a type.
    return null;
  }

  @Override
  public Void visitExpression(Expression node) {
    node.visitChildren(this);
    Type staticType = node.getStaticType();
    if (staticType == null) {
      unresolvedExpressions.add(node);
    } else {
      resolvedExpressionCount++;
      Type propagatedType = node.getPropagatedType();
      if (propagatedType != null) {
        propagatedExpressionCount++;
        if (!propagatedType.isMoreSpecificThan(staticType)) {
          invalidlyPropagatedExpressions.add(node);
        }
      }
    }
    return null;
  }

  @Override
  public Void visitImportDirective(ImportDirective node) {
    // Don't visit the children because they are not expected to have a type.
    return null;
  }

  @Override
  public Void visitLabel(Label node) {
    // Don't visit the children because the identifier is not expected to have a type.
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
    AstNode parent = node.getParent();
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
    } else if (parent instanceof ConstructorFieldInitializer
        && node == ((ConstructorFieldInitializer) parent).getFieldName()) {
      return null;
    } else if (node.getStaticElement() instanceof PrefixElement) {
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

  private String getFileName(AstNode node) {
    // TODO (jwren) there are two copies of this method, one here and one in ResolutionVerifier,
    // they should be resolved into a single method
    if (node != null) {
      AstNode root = node.getRoot();
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
