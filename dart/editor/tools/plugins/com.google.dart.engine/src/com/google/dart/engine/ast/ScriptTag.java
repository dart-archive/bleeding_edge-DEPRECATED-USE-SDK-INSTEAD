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
package com.google.dart.engine.ast;

import com.google.dart.engine.scanner.Token;

/**
 * Instances of the class {@code ScriptTag} represent the script tag that can optionally occur at
 * the beginning of a compilation unit.
 * 
 * <pre>
 * scriptTag ::=
 *     '#!' (~NEWLINE)* NEWLINE
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ScriptTag extends AstNode {
  /**
   * The token representing this script tag.
   */
  private Token scriptTag;

  /**
   * Initialize a newly created script tag.
   * 
   * @param scriptTag the token representing this script tag
   */
  public ScriptTag(Token scriptTag) {
    this.scriptTag = scriptTag;
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitScriptTag(this);
  }

  @Override
  public Token getBeginToken() {
    return scriptTag;
  }

  @Override
  public Token getEndToken() {
    return scriptTag;
  }

  /**
   * Return the token representing this script tag.
   * 
   * @return the token representing this script tag
   */
  public Token getScriptTag() {
    return scriptTag;
  }

  /**
   * Set the token representing this script tag to the given script tag.
   * 
   * @param scriptTag the token representing this script tag
   */
  public void setScriptTag(Token scriptTag) {
    this.scriptTag = scriptTag;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    // There are no children to visit.
  }
}
