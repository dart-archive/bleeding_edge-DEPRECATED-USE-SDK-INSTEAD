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
package com.google.dart.engine.internal.scope;

import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.LabelElement;
import com.google.dart.engine.internal.element.LabelElementImpl;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.TokenType;

/**
 * Instances of the class {@code LabelScope} represent a scope in which a single label is defined.
 * 
 * @coverage dart.engine.resolver
 */
public class LabelScope {
  /**
   * The label scope enclosing this label scope.
   */
  private LabelScope outerScope;

  /**
   * The label defined in this scope.
   */
  private String label;

  /**
   * The element to which the label resolves.
   */
  private LabelElement element;

  /**
   * The marker used to look up a label element for an unlabeled {@code break} or {@code continue}.
   */
  public static final String EMPTY_LABEL = "";

  /**
   * The label element returned for scopes that can be the target of an unlabeled {@code break} or
   * {@code continue}.
   */
  private static final SimpleIdentifier EMPTY_LABEL_IDENTIFIER = new SimpleIdentifier(
      new StringToken(TokenType.IDENTIFIER, "", 0));

  /**
   * Initialize a newly created scope to represent the potential target of an unlabeled
   * {@code break} or {@code continue}.
   * 
   * @param outerScope the label scope enclosing the new label scope
   * @param onSwitchStatement {@code true} if this label is associated with a {@code switch}
   *          statement
   * @param onSwitchMember {@code true} if this label is associated with a {@code switch} member
   */
  public LabelScope(LabelScope outerScope, boolean onSwitchStatement, boolean onSwitchMember) {
    this(outerScope, EMPTY_LABEL, new LabelElementImpl(
        EMPTY_LABEL_IDENTIFIER,
        onSwitchStatement,
        onSwitchMember));
  }

  /**
   * Initialize a newly created scope to represent the given label.
   * 
   * @param outerScope the label scope enclosing the new label scope
   * @param label the label defined in this scope
   * @param element the element to which the label resolves
   */
  public LabelScope(LabelScope outerScope, String label, LabelElement element) {
    this.outerScope = outerScope;
    this.label = label;
    this.element = element;
  }

  /**
   * Return the label element corresponding to the given label, or {@code null} if the given label
   * is not defined in this scope.
   * 
   * @param targetLabel the label being looked up
   * @return the label element corresponding to the given label
   */
  public LabelElement lookup(String targetLabel) {
    if (label.equals(targetLabel)) {
      return element;
    } else if (outerScope != null) {
      return outerScope.lookup(targetLabel);
    } else {
      return null;
    }
  }
}
