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

import java.util.ArrayList;
import java.util.List;

/**
 * Instances of the class <code>Comment</code> represent a comment within the source code.
 * 
 * <pre>
 * comment ::=
 *     endOfLineComment
 *   | blockComment
 *   | documentationComment
 * 
 * endOfLineComment ::=
 *     '//' (CHARACTER - EOL)* EOL
 * 
 * blockComment ::=
 *     '/*' CHARACTER* '&star;/'
 * 
 * documentationComment ::=
 *     '/**' (CHARACTER | {@link CommentReference commentReference})* '&star;/'
 *   | ('///' (CHARACTER - EOL)* EOL)+
 * </pre>
 */
public class Comment extends ASTNode {
  /**
   * The enumeration <code>CommentType</code> encodes all the different types of comments that are
   * recognized by the parser.
   */
  private enum CommentType {
    /**
     * An end-of-line comment.
     */
    END_OF_LINE,

    /**
     * A block comment.
     */
    BLOCK,

    /**
     * A documentation comment.
     */
    DOCUMENTATION;
  }

  /**
   * Create a block comment.
   * 
   * @param tokens the tokens representing the comment
   * @return the block comment that was created
   */
  public static Comment createBlockComment(Token[] tokens) {
    return new Comment(tokens, CommentType.BLOCK, null);
  }

  /**
   * Create a documentation comment.
   * 
   * @param tokens the tokens representing the comment
   * @return the documentation comment that was created
   */
  public static Comment createDocumentationComment(Token[] tokens) {
    return new Comment(tokens, CommentType.DOCUMENTATION, new ArrayList<CommentReference>());
  }

  /**
   * Create a documentation comment.
   * 
   * @param tokens the tokens representing the comment
   * @param references the references embedded within the documentation comment
   * @return the documentation comment that was created
   */
  public static Comment createDocumentationComment(Token[] tokens, List<CommentReference> references) {
    return new Comment(tokens, CommentType.DOCUMENTATION, references);
  }

  /**
   * Create an end-of-line comment.
   * 
   * @param tokens the tokens representing the comment
   * @return the end-of-line comment that was created
   */
  public static Comment createEndOfLineComment(Token[] tokens) {
    return new Comment(tokens, CommentType.END_OF_LINE, null);
  }

  /**
   * The tokens representing the comment.
   */
  private Token[] tokens;

  /**
   * The type of the comment.
   */
  private CommentType type;

  /**
   * The references embedded within the documentation comment. This list will be empty unless this
   * is a documentation comment that has references embedded within it.
   */
  private NodeList<CommentReference> references = new NodeList<CommentReference>(this);

  /**
   * Initialize a newly created comment.
   * 
   * @param tokens the tokens representing the comment
   * @param type the type of the comment
   * @param references the references embedded within the documentation comment
   */
  private Comment(Token[] tokens, CommentType type, List<CommentReference> references) {
    this.tokens = tokens;
    this.type = type;
    this.references.addAll(references);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitComment(this);
  }

  @Override
  public Token getBeginToken() {
    return tokens[0];
  }

  @Override
  public Token getEndToken() {
    return tokens[tokens.length - 1];
  }

  /**
   * Return the references embedded within the documentation comment.
   * 
   * @return the references embedded within the documentation comment
   */
  public NodeList<CommentReference> getReferences() {
    return references;
  }

  /**
   * Return <code>true</code> if this is a block comment.
   * 
   * @return <code>true</code> if this is a block comment
   */
  public boolean isBlock() {
    return type == CommentType.BLOCK;
  }

  /**
   * Return <code>true</code> if this is a documentation comment.
   * 
   * @return <code>true</code> if this is a documentation comment
   */
  public boolean isDocumentation() {
    return type == CommentType.DOCUMENTATION;
  }

  /**
   * Return <code>true</code> if this is an end-of-line comment.
   * 
   * @return <code>true</code> if this is an end-of-line comment
   */
  public boolean isEndOfLine() {
    return type == CommentType.END_OF_LINE;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    references.accept(visitor);
  }
}
