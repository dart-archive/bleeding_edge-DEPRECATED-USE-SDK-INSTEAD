/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.formatter;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.scanner.Token;

import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.TextEdit;

/**
 * An AST visitor that drives formatting heuristics.
 */
public class FormattingEngine extends RecursiveASTVisitor<Void> {

  // currently unused but needed later
  @SuppressWarnings("unused")
  private IRegion[] regions;
  @SuppressWarnings("unused")
  private int kind;

  private DefaultCodeFormatterOptions preferences;
  private EditRecorder recorder;

  public FormattingEngine(DefaultCodeFormatterOptions preferences) {
    this.preferences = preferences;
  }

  public TextEdit format(String source, ASTNode node, Token start, int kind, IRegion[] regions) {
    this.regions = regions;
    this.kind = kind;
    this.recorder = new EditRecorder(source, start, regions, preferences);
    node.accept(this);
    return recorder.buildEditCommands();
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
    recorder.unindent();
    recorder.newline();
    recorder.advance(node.getRightBracket());
    return null;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnit node) {
    if (node.getScriptTag() != null) {
      node.getScriptTag().accept(this);
      recorder.newline();
    }
    NodeList<Directive> directives = node.getDirectives();
    if (!directives.isEmpty()) {
      node.getDirectives().accept(this);
      recorder.newline();
    }
    NodeList<CompilationUnitMember> nodes = node.getDeclarations();
    for (CompilationUnitMember element : nodes) {
      element.accept(this);
    }
    return null;
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    recorder.advance(node.getToken());
    return null;
  }
}
