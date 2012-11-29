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

/**
 * Instances of the class {@code MixinApplication} represent the application of a mixing in a class
 * declaration.
 * 
 * <pre>
 * mixinApplication ::=
 *     '=' {@link TypeName mixin}
 * </pre>
 */
public class MixinApplication extends ASTNode {
  /**
   * The equal sign before the mixin name.
   */
  private Token equals;

  /**
   * The name of the mixin that is being applied.
   */
  private TypeName mixin;

  /**
   * Initialize a newly created mixin application.
   */
  public MixinApplication() {
  }

  /**
   * Initialize a newly created mixin application.
   * 
   * @param equals the equal sign before the mixin name
   * @param superclass the name of the mixin that is being applied
   */
  public MixinApplication(Token equals, TypeName superclass) {
    this.equals = equals;
    this.mixin = becomeParentOf(superclass);
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitMixinApplication(this);
  }

  @Override
  public Token getBeginToken() {
    return equals;
  }

  @Override
  public Token getEndToken() {
    return mixin.getEndToken();
  }

  /**
   * Return the equal sign before the mixin name.
   * 
   * @return the equal sign before the mixin name
   */
  public Token getEquals() {
    return equals;
  }

  /**
   * Return the name of the mixin that is being applied.
   * 
   * @return the name of the mixin that is being applied
   */
  public TypeName getSuperclass() {
    return mixin;
  }

  /**
   * Set the equal sign before the mixin name to the given token.
   * 
   * @param equals the equal sign before the mixin name
   */
  public void setEquals(Token equals) {
    this.equals = equals;
  }

  /**
   * Set the name of the mixin that is being applied to the given name.
   * 
   * @param mixin the name of the mixin that is being applied
   */
  public void setSuperclass(TypeName mixin) {
    this.mixin = becomeParentOf(mixin);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    safelyVisitChild(mixin, visitor);
  }
}
