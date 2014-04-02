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

import com.google.dart.engine.element.Element;
import com.google.dart.engine.source.Source;

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
   * The content of the URI.
   */
  private String uriContent;

  /**
   * The source to which the URI was resolved.
   */
  private Source source;

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
   * Return the source to which the URI was resolved, or {@code null} if the AST structure has not
   * been resolved or if this URI could not be resolved.
   * 
   * @return the source to which the URI was resolved
   */
  public Source getSource() {
    return source;
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
   * Return the content of the URI, or {@code null} if the AST structure has not been resolved or if
   * the URI was not a simple string literal (such as a URI that uses string interpolation).
   * 
   * @return the content of the URI
   */
  public String getUriContent() {
    return uriContent;
  }

  /**
   * Return the element associated with the URI of this directive, or {@code null} if the AST
   * structure has not been resolved or if the URI could not be resolved. Examples of the latter
   * case include a directive that contains an invalid URL or a URL that does not exist.
   * 
   * @return the element associated with this directive
   */
  public abstract Element getUriElement();

  /**
   * Set the source to which the URI was resolved to the given source.
   * 
   * @param source the source to which the URI was resolved
   */
  public void setSource(Source source) {
    this.source = source;
  }

  /**
   * Set the URI referenced by this directive to the given URI.
   * 
   * @param uri the URI referenced by this directive
   */
  public void setUri(StringLiteral uri) {
    this.uri = becomeParentOf(uri);
  }

  /**
   * Set the content of the URI to the given value.
   * 
   * @param uriContent the content of the URI
   */
  public void setUriContent(String uriContent) {
    this.uriContent = uriContent;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChild(uri, visitor);
  }
}
