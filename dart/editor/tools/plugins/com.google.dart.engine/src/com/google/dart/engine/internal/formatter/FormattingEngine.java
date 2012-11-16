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
package com.google.dart.engine.internal.formatter;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.formatter.CodeFormatter.Kind;
import com.google.dart.engine.formatter.CodeFormatterOptions;
import com.google.dart.engine.formatter.edit.EditRecorder;
import com.google.dart.engine.scanner.Token;

/**
 * An AST visitor that drives formatting heuristics.
 */
public class FormattingEngine extends RecursiveASTVisitor<Void> {

  @SuppressWarnings("unused")
  private final CodeFormatterOptions options;
  private EditRecorder<?, ?> recorder;
  @SuppressWarnings("unused")
  private Kind kind;

  public FormattingEngine(CodeFormatterOptions options) {
    this.options = options;
  }

  public void format(String source, ASTNode node, Token start, Kind kind,
      EditRecorder<?, ?> recorder) {

    this.kind = kind;
    this.recorder = recorder;

    recorder.setSource(source);
    recorder.setStart(start);

    node.accept(this);
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    if (node.getDocumentationComment() != null) {
      node.getDocumentationComment().accept(this);
    }
    recorder.advance(node.getClassKeyword());
    recorder.space();
    if (node.getName() != null) {
      node.getName().accept(this);
      if (node.getTypeParameters() != null) {
        node.getTypeParameters().accept(this);
      }
      recorder.space();
    }
    if (node.getExtendsClause() != null) {
      node.getExtendsClause().accept(this);
      recorder.space();
    }
    if (node.getImplementsClause() != null) {
      node.getImplementsClause().accept(this);
      recorder.space();
    }
    recorder.advance(node.getLeftBracket());
    recorder.indent();
    NodeList<ClassMember> members = node.getMembers();
    for (ClassMember member : members) {
      recorder.newline();
      member.accept(this);
    }
    recorder.unIndent();
    recorder.newline();
    recorder.advance(node.getRightBracket());
    return null;
  }

  @Override
  public Void visitExtendsClause(ExtendsClause node) {
    recorder.advance(node.getKeyword());
    recorder.space();
    node.getSuperclass().accept(this);
    return null;
  }

  @Override
  public Void visitImplementsClause(ImplementsClause node) {
    recorder.advance(node.getKeyword());
    recorder.space();
    visitNodeList(node.getInterfaces());
    return null;
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    recorder.advance(node.getToken());
    return null;
  }

  @Override
  public Void visitTypeName(TypeName node) {
    node.getName().accept(this);
    if (node.getTypeArguments() != null) {
      node.getTypeArguments().accept(this);
    }
    return null;
  }

  private void visitNodeList(NodeList<? extends ASTNode> nodes) {
    visitNodeList(nodes, ",");
  }

  private void visitNodeList(NodeList<? extends ASTNode> nodes, String separatedBy) {
    nodes.get(0).accept(this);
    for (int i = 1; i < nodes.size(); i++) {
      recorder.advance(separatedBy);
      recorder.space();
      nodes.get(i).accept(this);
    }
  }

}
