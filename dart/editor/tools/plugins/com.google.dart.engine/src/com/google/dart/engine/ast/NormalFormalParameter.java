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

/**
 * The abstract class {@code NormalFormalParameter} defines the behavior common to formal parameters
 * that are required (are not optional).
 * 
 * <pre>
 * normalFormalParameter ::=
 *     {@link FunctionSignature functionSignature}
 *   | {@link FieldFormalParameter fieldFormalParameter}
 *   | {@link SimpleFormalParameter simpleFormalParameter}
 * </pre>
 */
public abstract class NormalFormalParameter extends FormalParameter {
  /**
   * The name of the parameter being declared.
   */
  private SimpleIdentifier identifier;

  /**
   * Initialize a newly created formal parameter.
   */
  public NormalFormalParameter() {
  }

  /**
   * Initialize a newly created formal parameter.
   * 
   * @param identifier the name of the parameter being declared
   */
  public NormalFormalParameter(SimpleIdentifier identifier) {
    this.identifier = becomeParentOf(identifier);
  }

  /**
   * Return the name of the parameter being declared.
   * 
   * @return the name of the parameter being declared
   */
  public SimpleIdentifier getIdentifier() {
    return identifier;
  }

  /**
   * Set the name of the parameter being declared to the given identifier.
   * 
   * @param identifier the name of the parameter being declared
   */
  public void setIdentifier(SimpleIdentifier identifier) {
    this.identifier = becomeParentOf(identifier);
  }
}
