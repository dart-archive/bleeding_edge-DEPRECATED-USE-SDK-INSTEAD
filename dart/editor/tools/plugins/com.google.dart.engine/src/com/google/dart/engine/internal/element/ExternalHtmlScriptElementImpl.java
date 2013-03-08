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
import com.google.dart.engine.element.ExternalHtmlScriptElement;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code ExternalHtmlScriptElementImpl} implement an
 * {@link ExternalHtmlScriptElement}.
 * 
 * @coverage dart.engine.element
 */
public class ExternalHtmlScriptElementImpl extends HtmlScriptElementImpl implements
    ExternalHtmlScriptElement {

  /**
   * The source specified in the {@code source} attribute or {@code null} if unspecified.
   */
  private Source scriptSource;

  /**
   * Initialize a newly created script element to have the specified tag name and offset.
   * 
   * @param node the XML node from which this element is derived (not {@code null})
   */
  public ExternalHtmlScriptElementImpl(XmlTagNode node) {
    super(node);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitExternalHtmlScriptElement(this);
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.EXTERNAL_HTML_SCRIPT;
  }

  @Override
  public Source getScriptSource() {
    return scriptSource;
  }

  /**
   * Set the source specified in the {@code source} attribute.
   * 
   * @param scriptSource the script source or {@code null} if unspecified
   */
  public void setScriptSource(Source scriptSource) {
    this.scriptSource = scriptSource;
  }
}
