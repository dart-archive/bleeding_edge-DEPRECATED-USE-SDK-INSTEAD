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

package com.google.dart.server.internal.local.computer;

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AsExpression;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NativeClause;
import com.google.dart.engine.ast.NativeFunctionBody;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.RecursiveAstVisitor;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.VariableElement;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.type.Type;
import com.google.dart.server.HighlightRegion;
import com.google.dart.server.HighlightType;

import java.util.List;

/**
 * A computer for {@link HighlightRegion}s in a Dart {@link CompilationUnit}.
 * 
 * @coverage dart.server.local
 */
public class DartUnitHighlightsComputer {
  private final CompilationUnit unit;
  private final List<HighlightRegion> regions = Lists.newArrayList();

  public DartUnitHighlightsComputer(CompilationUnit unit) {
    this.unit = unit;
  }

  /**
   * Returns the computed {@link HighlightRegion}s, not {@code null}.
   */
  public HighlightRegion[] compute() {
    unit.accept(new RecursiveAstVisitor<Void>() {
      @Override
      public Void visitAnnotation(Annotation node) {
        addIdentifierRegion_annotation(node);
        return super.visitAnnotation(node);
      }

      @Override
      public Void visitAsExpression(AsExpression node) {
        addRegion_token(node.getAsOperator(), HighlightType.BUILT_IN);
        return super.visitAsExpression(node);
      }

      @Override
      public Void visitCatchClause(CatchClause node) {
        addRegion_token(node.getOnKeyword(), HighlightType.BUILT_IN);
        return super.visitCatchClause(node);
      }

      @Override
      public Void visitClassDeclaration(ClassDeclaration node) {
        addRegion_token(node.getAbstractKeyword(), HighlightType.BUILT_IN);
        return super.visitClassDeclaration(node);
      }

      @Override
      public Void visitConstructorDeclaration(ConstructorDeclaration node) {
        addRegion_token(node.getExternalKeyword(), HighlightType.BUILT_IN);
        addRegion_token(node.getFactoryKeyword(), HighlightType.BUILT_IN);
        return super.visitConstructorDeclaration(node);
      }

      @Override
      public Void visitExportDirective(ExportDirective node) {
        addRegion_token(node.getKeyword(), HighlightType.BUILT_IN);
        return super.visitExportDirective(node);
      }

      @Override
      public Void visitFieldDeclaration(FieldDeclaration node) {
        addRegion_token(node.getStaticKeyword(), HighlightType.BUILT_IN);
        return super.visitFieldDeclaration(node);
      }

      @Override
      public Void visitFunctionDeclaration(FunctionDeclaration node) {
        addRegion_token(node.getExternalKeyword(), HighlightType.BUILT_IN);
        addRegion_token(node.getPropertyKeyword(), HighlightType.BUILT_IN);
        return super.visitFunctionDeclaration(node);
      }

      @Override
      public Void visitFunctionTypeAlias(FunctionTypeAlias node) {
        addRegion_token(node.getKeyword(), HighlightType.BUILT_IN);
        return super.visitFunctionTypeAlias(node);
      }

      @Override
      public Void visitImplementsClause(ImplementsClause node) {
        addRegion_token(node.getKeyword(), HighlightType.BUILT_IN);
        return super.visitImplementsClause(node);
      }

      @Override
      public Void visitImportDirective(ImportDirective node) {
        addRegion_token(node.getKeyword(), HighlightType.BUILT_IN);
        return super.visitImportDirective(node);
      }

      @Override
      public Void visitLibraryDirective(LibraryDirective node) {
        addRegion_token(node.getKeyword(), HighlightType.BUILT_IN);
        return super.visitLibraryDirective(node);
      }

      @Override
      public Void visitMethodDeclaration(MethodDeclaration node) {
        addRegion_token(node.getExternalKeyword(), HighlightType.BUILT_IN);
        addRegion_token(node.getModifierKeyword(), HighlightType.BUILT_IN);
        addRegion_token(node.getOperatorKeyword(), HighlightType.BUILT_IN);
        addRegion_token(node.getPropertyKeyword(), HighlightType.BUILT_IN);
        return super.visitMethodDeclaration(node);
      }

      @Override
      public Void visitNativeClause(NativeClause node) {
        addRegion_token(node.getKeyword(), HighlightType.BUILT_IN);
        return super.visitNativeClause(node);
      }

      @Override
      public Void visitNativeFunctionBody(NativeFunctionBody node) {
        addRegion_token(node.getNativeToken(), HighlightType.BUILT_IN);
        return super.visitNativeFunctionBody(node);
      }

      @Override
      public Void visitPartDirective(PartDirective node) {
        addRegion_token(node.getKeyword(), HighlightType.BUILT_IN);
        return super.visitPartDirective(node);
      }

      @Override
      public Void visitPartOfDirective(PartOfDirective node) {
        addRegion_tokenStart_tokenEnd(
            node.getPartToken(),
            node.getOfToken(),
            HighlightType.BUILT_IN);
        return super.visitPartOfDirective(node);
      }

      @Override
      public Void visitSimpleIdentifier(SimpleIdentifier node) {
        addIdentifierRegion(node);
        return super.visitSimpleIdentifier(node);
      }

    });
    return regions.toArray(new HighlightRegion[regions.size()]);
  }

  private void addIdentifierRegion(SimpleIdentifier node) {
    if (addIdentifierRegion_class(node)) {
      return;
    }
    if (addIdentifierRegion_constructor(node)) {
      return;
    }
    if (addIdentifierRegion_dynamicType(node)) {
      return;
    }
  }

  private void addIdentifierRegion_annotation(Annotation node) {
    ArgumentList arguments = node.getArguments();
    if (arguments == null) {
      addRegion_node(node, HighlightType.ANNOTATION);
    } else {
      addRegion_nodeStart_tokenEnd(node, arguments.getBeginToken(), HighlightType.ANNOTATION);
      addRegion_token(arguments.getEndToken(), HighlightType.ANNOTATION);
    }
  }

  private boolean addIdentifierRegion_class(SimpleIdentifier node) {
    Element element = node.getStaticElement();
    if (!(element instanceof ClassElement)) {
      return false;
    }
    return addRegion_node(node, HighlightType.CLASS);
  }

  private boolean addIdentifierRegion_constructor(SimpleIdentifier node) {
    Element element = node.getStaticElement();
    if (!(element instanceof ConstructorElement)) {
      return false;
    }
    return addRegion_node(node, HighlightType.CONSTRUCTOR);
  }

  private boolean addIdentifierRegion_dynamicType(SimpleIdentifier node) {
    // should be variable
    Element element = node.getStaticElement();
    if (!(element instanceof VariableElement)) {
      return false;
    }
    // has propagated type
    if (node.getPropagatedType() != null) {
      return false;
    }
    // has dynamic static type
    Type staticType = node.getStaticType();
    if (staticType == null || !staticType.isDynamic()) {
      return false;
    }
    // OK
    return addRegion_node(node, HighlightType.DYNAMIC_TYPE);
  }

  private void addRegion(int offset, int length, HighlightType type) {
    regions.add(new HighlightRegionImpl(offset, length, type));
  }

  private boolean addRegion_node(AstNode node, HighlightType type) {
    int offset = node.getOffset();
    int length = node.getLength();
    addRegion(offset, length, type);
    return true;
  }

  private void addRegion_nodeStart_tokenEnd(AstNode a, Token b, HighlightType type) {
    int offset = a.getOffset();
    int end = b.getEnd();
    addRegion(offset, end - offset, type);
  }

  private void addRegion_token(Token token, HighlightType type) {
    if (token != null) {
      int offset = token.getOffset();
      int length = token.getLength();
      addRegion(offset, length, type);
    }
  }

  private void addRegion_tokenStart_tokenEnd(Token a, Token b, HighlightType type) {
    int offset = a.getOffset();
    int end = b.getEnd();
    addRegion(offset, end - offset, type);
  }
}
