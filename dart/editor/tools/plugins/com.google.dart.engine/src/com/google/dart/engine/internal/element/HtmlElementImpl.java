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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.element.HtmlScriptElement;
import com.google.dart.engine.element.polymer.PolymerTagHtmlElement;
import com.google.dart.engine.internal.element.polymer.PolymerTagHtmlElementImpl;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code HtmlElementImpl} implement an {@link HtmlElement}.
 * 
 * @coverage dart.engine.element
 */
public class HtmlElementImpl extends ElementImpl implements HtmlElement {
  /**
   * An empty array of HTML file elements.
   */
  public static final HtmlElement[] EMPTY_ARRAY = new HtmlElement[0];

  /**
   * The analysis context in which this library is defined.
   */
  private AnalysisContext context;

  /**
   * The scripts contained in or referenced from script tags in the HTML file.
   */
  private HtmlScriptElement[] scripts = HtmlScriptElementImpl.EMPTY_ARRAY;

  /**
   * The {@link PolymerTagHtmlElement}s defined in the HTML file.
   */
  private PolymerTagHtmlElement[] polymerTags = PolymerTagHtmlElement.EMPTY_ARRAY;

  /**
   * The source that corresponds to this HTML file.
   */
  private Source source;

  /**
   * The element associated with Dart pieces in this HTML unit or {@code null} if the receiver is
   * not resolved.
   */
  private CompilationUnitElement angularCompilationUnit;

  /**
   * Initialize a newly created HTML element to have the given name.
   * 
   * @param context the analysis context in which the HTML file is defined
   * @param name the name of this element
   */
  public HtmlElementImpl(AnalysisContext context, String name) {
    super(name, -1);
    this.context = context;
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitHtmlElement(this);
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (object == null) {
      return false;
    }
    return getClass() == object.getClass() && source.equals(((HtmlElementImpl) object).getSource());
  }

  /**
   * Return the element associated with Dart pieces in this HTML unit.
   * 
   * @return the element or {@code null} if the receiver is not resolved
   */
  @Override
  public CompilationUnitElement getAngularCompilationUnit() {
    return angularCompilationUnit;
  }

  @Override
  public AnalysisContext getContext() {
    return context;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.HTML;
  }

  @Override
  public PolymerTagHtmlElement[] getPolymerTags() {
    return polymerTags;
  }

  @Override
  public HtmlScriptElement[] getScripts() {
    return scripts;
  }

  @Override
  public Source getSource() {
    return source;
  }

  @Override
  public int hashCode() {
    return source.hashCode();
  }

  /**
   * Set the {@link CompilationUnitElement} associated with this Angular HTML file.
   */
  public void setAngularCompilationUnit(CompilationUnitElement angularCompilationUnit) {
    this.angularCompilationUnit = angularCompilationUnit;
  }

  /**
   * Set the {@link PolymerTagHtmlElement}s defined in the HTML file.
   */
  public void setPolymerTags(PolymerTagHtmlElement[] polymerTags) {
    if (polymerTags.length == 0) {
      this.polymerTags = PolymerTagHtmlElement.EMPTY_ARRAY;
      return;
    }
    for (PolymerTagHtmlElement tag : polymerTags) {
      ((PolymerTagHtmlElementImpl) tag).setEnclosingElement(this);
    }
    this.polymerTags = polymerTags;
  }

  /**
   * Set the scripts contained in the HTML file to the given scripts.
   * 
   * @param scripts the scripts
   */
  public void setScripts(HtmlScriptElement[] scripts) {
    if (scripts.length == 0) {
      this.scripts = HtmlScriptElementImpl.EMPTY_ARRAY;
      return;
    }
    for (HtmlScriptElement script : scripts) {
      ((HtmlScriptElementImpl) script).setEnclosingElement(this);
    }
    this.scripts = scripts;
  }

  /**
   * Set the source that corresponds to this HTML file to the given source.
   * 
   * @param source the source that corresponds to this HTML file
   */
  public void setSource(Source source) {
    this.source = source;
  }

  @Override
  public void visitChildren(ElementVisitor<?> visitor) {
    super.visitChildren(visitor);
    safelyVisitChildren(scripts, visitor);
    safelyVisitChildren(polymerTags, visitor);
  }

  @Override
  protected void appendTo(StringBuilder builder) {
    if (source == null) {
      builder.append("{HTML file}");
    } else {
      builder.append(source.getFullName());
    }
  }

  @Override
  protected String getIdentifier() {
    return source.getEncoding();
  }
}
