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
package com.google.dart.engine.ast;

import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Instances of the class {@code EnumConstantDeclaration} represent the declaration of an enum
 * constant.
 */
public class EnumConstantDeclaration extends Declaration {
  /**
   * The name of the constant.
   */
  private SimpleIdentifier name;

  /**
   * Initialize a newly created enum constant declaration.
   * 
   * @param comment the documentation comment associated with this declaration
   * @param metadata the annotations associated with this declaration
   * @param name the name of the constant
   */
  public EnumConstantDeclaration(Comment comment, List<Annotation> metadata, SimpleIdentifier name) {
    super(comment, metadata);
    this.name = becomeParentOf(name);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitEnumConstantDeclaration(this);
  }

  @Override
  public FieldElement getElement() {
    return name == null ? null : (FieldElement) name.getStaticElement();
  }

  @Override
  public Token getEndToken() {
    return name.getEndToken();
  }

  /**
   * Return the name of the constant.
   * 
   * @return the name of the constant
   */
  public SimpleIdentifier getName() {
    return name;
  }

  /**
   * Set the name of the constant to the given name.
   * 
   * @param name the name of the constant
   */
  public void setName(SimpleIdentifier name) {
    this.name = becomeParentOf(name);
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(name, visitor);
  }

  @Override
  protected Token getFirstTokenAfterCommentAndMetadata() {
    return name.getBeginToken();
  }
}
