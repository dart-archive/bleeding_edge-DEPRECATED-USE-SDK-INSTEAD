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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.EmbeddedHtmlScriptElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.html.ast.XmlTagNode;

/**
 * Instances of the class {@code EmbeddedHtmlScriptElementImpl} implement an
 * {@link EmbeddedHtmlScriptElement}.
 * 
 * @coverage dart.engine.element
 */
public class EmbeddedHtmlScriptElementImpl extends HtmlScriptElementImpl implements
    EmbeddedHtmlScriptElement {

  /**
   * The library defined by the script tag's content.
   */
  private LibraryElement scriptLibrary;

  /**
   * Initialize a newly created script element to have the specified tag name and offset.
   * 
   * @param node the XML node from which this element is derived (not {@code null})
   */
  public EmbeddedHtmlScriptElementImpl(XmlTagNode node) {
    super(node);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitEmbeddedHtmlScriptElement(this);
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.EMBEDDED_HTML_SCRIPT;
  }

  @Override
  public LibraryElement getScriptLibrary() {
    return scriptLibrary;
  }

  /**
   * Set the script library defined by the script tag's content.
   * 
   * @param scriptLibrary the library or {@code null} if none
   */
  public void setScriptLibrary(LibraryElementImpl scriptLibrary) {
    scriptLibrary.setEnclosingElement(this);
    this.scriptLibrary = scriptLibrary;
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    safelyVisitChild(scriptLibrary, visitor);
  }
}
