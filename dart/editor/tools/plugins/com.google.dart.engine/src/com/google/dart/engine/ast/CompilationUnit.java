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
package com.google.dart.engine.ast;

import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Instances of the class <code>CompilationUnit</code> represent a compilation unit. The script tag
 * is not represented in the AST structure.
 * 
 * <pre>
 * compilationUnit ::=
 *     scriptTag? directives compilationUnitMember*
 * 
 * scriptTag ::=
 *     '#!' (~NEWLINE)* NEWLINE
 * 
 * directives ::=
 *     {@link LibraryDirective libraryDirective}? {@link ImportDirective importDirective}* {@link SourceDirective sourceDirective}* {@link ResourceDirective resourceDirective}*
 * 
 * compilationUnitMember ::=
 *     {@link ClassDeclaration classDeclaration}
 *   | {@link InterfaceDeclaration interfaceDeclaration}
 *   | {@link TypeAlias typeAlias}
 *   | {@link FunctionDeclaration functionDeclaration}
 *   | {@link MethodDeclaration getOrSetDeclaration}
 *   | {@link VariableDeclaration constantsDeclaration}
 *   | {@link VariableDeclaration variablesDeclaration}
 * </pre>
 */
public class CompilationUnit extends ASTNode {
  /**
   * The directives contained in this compilation unit.
   */
  private NodeList<Directive> directives = new NodeList<Directive>(this);

  /**
   * The declarations contained in this compilation unit.
   */
  private NodeList<CompilationUnitMember> declarations = new NodeList<CompilationUnitMember>(this);

  /**
   * Initialize a newly created compilation unit to have the given directives and declarations.
   */
  public CompilationUnit() {
  }

  /**
   * Initialize a newly created compilation unit to have the given directives and declarations.
   * 
   * @param directives the directives contained in this compilation unit
   * @param declarations the declarations contained in this compilation unit
   */
  public CompilationUnit(List<Directive> directives, List<CompilationUnitMember> declarations) {
    this.directives.addAll(directives);
    this.declarations.addAll(declarations);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitCompilationUnit(this);
  }

  @Override
  public Token getBeginToken() {
    // TODO(brianwilkerson) Consider keeping a pointer to the first and last tokens in the token
    // stream. Doing so would resolve the issue of what to do if both directives and declarations
    // are empty.
    if (!directives.isEmpty()) {
      return directives.getBeginToken();
    }
    return declarations.getBeginToken();
  }

  /**
   * Return the declarations contained in this compilation unit.
   * 
   * @return the declarations contained in this compilation unit
   */
  public NodeList<CompilationUnitMember> getDeclarations() {
    return declarations;
  }

  /**
   * Return the directives contained in this compilation unit.
   * 
   * @return the directives contained in this compilation unit
   */
  public NodeList<Directive> getDirectives() {
    return directives;
  }

  @Override
  public Token getEndToken() {
    return declarations.getEndToken();
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    directives.accept(visitor);
    declarations.accept(visitor);
  }
}
