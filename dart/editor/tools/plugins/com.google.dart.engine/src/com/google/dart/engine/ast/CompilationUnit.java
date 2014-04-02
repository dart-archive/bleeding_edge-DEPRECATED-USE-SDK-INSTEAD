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

import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.utilities.source.LineInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Instances of the class {@code CompilationUnit} represent a compilation unit.
 * <p>
 * While the grammar restricts the order of the directives and declarations within a compilation
 * unit, this class does not enforce those restrictions. In particular, the children of a
 * compilation unit will be visited in lexical order even if lexical order does not conform to the
 * restrictions of the grammar.
 * 
 * <pre>
 * compilationUnit ::=
 *     directives declarations
 * 
 * directives ::=
 *     {@link ScriptTag scriptTag}? {@link LibraryDirective libraryDirective}? namespaceDirective* {@link PartDirective partDirective}*
 *   | {@link PartOfDirective partOfDirective}
 * 
 * namespaceDirective ::=
 *     {@link ImportDirective importDirective}
 *   | {@link ExportDirective exportDirective}
 * 
 * declarations ::=
 *     {@link CompilationUnitMember compilationUnitMember}*
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class CompilationUnit extends AstNode {
  /**
   * The first token in the token stream that was parsed to form this compilation unit.
   */
  private Token beginToken;

  /**
   * The script tag at the beginning of the compilation unit, or {@code null} if there is no script
   * tag in this compilation unit.
   */
  private ScriptTag scriptTag;

  /**
   * The directives contained in this compilation unit.
   */
  private NodeList<Directive> directives = new NodeList<Directive>(this);

  /**
   * The declarations contained in this compilation unit.
   */
  private NodeList<CompilationUnitMember> declarations = new NodeList<CompilationUnitMember>(this);

  /**
   * The last token in the token stream that was parsed to form this compilation unit. This token
   * should always have a type of {@link TokenType.EOF}.
   */
  private Token endToken;

  /**
   * The element associated with this compilation unit, or {@code null} if the AST structure has not
   * been resolved.
   */
  private CompilationUnitElement element;

  /**
   * The line information for this compilation unit.
   */
  private LineInfo lineInfo;

  /**
   * Initialize a newly created compilation unit to have the given directives and declarations.
   * 
   * @param beginToken the first token in the token stream
   * @param scriptTag the script tag at the beginning of the compilation unit
   * @param directives the directives contained in this compilation unit
   * @param declarations the declarations contained in this compilation unit
   * @param endToken the last token in the token stream
   */
  public CompilationUnit(Token beginToken, ScriptTag scriptTag, List<Directive> directives,
      List<CompilationUnitMember> declarations, Token endToken) {
    this.beginToken = beginToken;
    this.scriptTag = becomeParentOf(scriptTag);
    this.directives.addAll(directives);
    this.declarations.addAll(declarations);
    this.endToken = endToken;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitCompilationUnit(this);
  }

  @Override
  public Token getBeginToken() {
    return beginToken;
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

  /**
   * Return the element associated with this compilation unit, or {@code null} if the AST structure
   * has not been resolved.
   * 
   * @return the element associated with this compilation unit
   */
  public CompilationUnitElement getElement() {
    return element;
  }

  @Override
  public Token getEndToken() {
    return endToken;
  }

  @Override
  public int getLength() {
    Token endToken = getEndToken();
    if (endToken == null) {
      return 0;
    }
    return endToken.getOffset() + endToken.getLength();
  }

  /**
   * Return the line information for this compilation unit.
   * 
   * @return the line information for this compilation unit
   */
  public LineInfo getLineInfo() {
    return lineInfo;
  }

  @Override
  public int getOffset() {
    return 0;
  }

  /**
   * Return the script tag at the beginning of the compilation unit, or {@code null} if there is no
   * script tag in this compilation unit.
   * 
   * @return the script tag at the beginning of the compilation unit
   */
  public ScriptTag getScriptTag() {
    return scriptTag;
  }

  /**
   * Set the element associated with this compilation unit to the given element.
   * 
   * @param element the element associated with this compilation unit
   */
  public void setElement(CompilationUnitElement element) {
    this.element = element;
  }

  /**
   * Set the line information for this compilation unit to the given line information.
   * 
   * @param errors the line information to associate with this compilation unit
   */
  public void setLineInfo(LineInfo lineInfo) {
    this.lineInfo = lineInfo;
  }

  /**
   * Set the script tag at the beginning of the compilation unit to the given script tag.
   * 
   * @param scriptTag the script tag at the beginning of the compilation unit
   */
  public void setScriptTag(ScriptTag scriptTag) {
    this.scriptTag = becomeParentOf(scriptTag);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    safelyVisitChild(scriptTag, visitor);
    if (directivesAreBeforeDeclarations()) {
      directives.accept(visitor);
      declarations.accept(visitor);
    } else {
      for (AstNode child : getSortedDirectivesAndDeclarations()) {
        child.accept(visitor);
      }
    }
  }

  /**
   * Return {@code true} if all of the directives are lexically before any declarations.
   * 
   * @return {@code true} if all of the directives are lexically before any declarations
   */
  private boolean directivesAreBeforeDeclarations() {
    if (directives.isEmpty() || declarations.isEmpty()) {
      return true;
    }
    Directive lastDirective = directives.get(directives.size() - 1);
    CompilationUnitMember firstDeclaration = declarations.get(0);
    return lastDirective.getOffset() < firstDeclaration.getOffset();
  }

  /**
   * Return an array containing all of the directives and declarations in this compilation unit,
   * sorted in lexical order.
   * 
   * @return the directives and declarations in this compilation unit in the order in which they
   *         appeared in the original source
   */
  private AstNode[] getSortedDirectivesAndDeclarations() {
    ArrayList<AstNode> childList = new ArrayList<AstNode>();
    childList.addAll(directives);
    childList.addAll(declarations);
    AstNode[] children = childList.toArray(new AstNode[childList.size()]);
    Arrays.sort(children, AstNode.LEXICAL_ORDER);
    return children;
  }
}
