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

import com.google.dart.engine.element.ExportElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Instances of the class {@code ExportDirective} represent an export directive.
 * 
 * <pre>
 * exportDirective ::=
 *     {@link Annotation metadata} 'export' {@link StringLiteral libraryUri} {@link Combinator combinator}* ';'
 * </pre>
 * 
 * @coverage dart.engine.ast
 */
public class ExportDirective extends NamespaceDirective {
  /**
   * Initialize a newly created export directive.
   * 
   * @param comment the documentation comment associated with this directive
   * @param metadata the annotations associated with the directive
   * @param keyword the token representing the 'export' keyword
   * @param libraryUri the URI of the library being exported
   * @param combinators the combinators used to control which names are exported
   * @param semicolon the semicolon terminating the directive
   */
  public ExportDirective(Comment comment, List<Annotation> metadata, Token keyword,
      StringLiteral libraryUri, List<Combinator> combinators, Token semicolon) {
    super(comment, metadata, keyword, libraryUri, combinators, semicolon);
  }

  @Override
  public <R> R accept(AstVisitor<R> visitor) {
    return visitor.visitExportDirective(this);
  }

  @Override
  public ExportElement getElement() {
    return (ExportElement) super.getElement();
  }

  @Override
  public LibraryElement getUriElement() {
    ExportElement exportElement = getElement();
    if (exportElement != null) {
      return exportElement.getExportedLibrary();
    }
    return null;
  }

  @Override
  public void visitChildren(AstVisitor<?> visitor) {
    super.visitChildren(visitor);
    getCombinators().accept(visitor);
  }
}
