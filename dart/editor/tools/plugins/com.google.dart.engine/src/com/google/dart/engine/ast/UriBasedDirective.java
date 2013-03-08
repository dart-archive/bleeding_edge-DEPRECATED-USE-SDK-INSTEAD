/*
 * Copyright (c) 2013, the Dart project authors.
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

import java.util.List;

/**
 * The abstract class {@code UriBasedDirective} defines the behavior common to nodes that represent
 * a directive that references a URI.
 * 
 * <pre>
 * uriBasedDirective ::=
 *     {@link ExportDirective exportDirective}
 *   | {@link ImportDirective importDirective}
 *   | {@link PartDirective partDirective}
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public abstract class UriBasedDirective extends Directive {
  /**
   * The URI referenced by this directive.
   */
  private StringLiteral uri;

  /**
   * Initialize a newly create URI-based directive.
   * 
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   * @param uri the URI referenced by this directive
   */
  public UriBasedDirective(Comment comment, List<Annotation> metadata, StringLiteral uri) {
    super(comment, metadata);
    this.uri = becomeParentOf(uri);
  }

  /**
   * Return the URI referenced by this directive.
   * 
   * @return the URI referenced by this directive
   */
  public StringLiteral getUri() {
    return uri;
  }

  /**
   * Set the URI referenced by this directive to the given URI.
   * 
   * @param uri the URI referenced by this directive
   */
  public void setUri(StringLiteral uri) {
    this.uri = becomeParentOf(uri);
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(uri, visitor);
  }
}
