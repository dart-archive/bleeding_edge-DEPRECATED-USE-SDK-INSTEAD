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
package com.google.dart.engine.element;

import com.google.common.base.Objects;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;

/**
 * Contains some properties of {@link Element}, which can be accessed without costly access to the
 * {@link Element} itself.
 */
public class ElementProxy {
  /**
   * @return the {@link Source} which contains given {@link Element}, may be <code>null</code>.
   */
  private static Source findSource(Element element) {
    while (element != null) {
      if (element instanceof LibraryElement) {
        element = ((LibraryElement) element).getDefiningCompilationUnit();
      }
      if (element instanceof CompilationUnitElement) {
        return ((CompilationUnitElement) element).getSource();
      }
      element = element.getEnclosingElement();
    }
    return null;
  }

  private final AnalysisContext context;
  private final Source source;
  private final ElementLocation location;
  private final ElementKind kind;
  private final String name;
  private final int nameOffset;

  /**
   * Initializes {@link ElementProxy} using information about {@link Element}.
   */
  public ElementProxy(AnalysisContext context, Source source, ElementLocation location,
      ElementKind kind, String name, int nameOffset) {
    this.context = context;
    this.source = source;
    this.location = location;
    this.kind = kind;
    this.name = name;
    this.nameOffset = nameOffset;
  }

  /**
   * Initializes {@link ElementProxy} using existing {@link Element} instance.
   */
  public ElementProxy(Element element) {
    context = element.getContext();
    source = findSource(element);
    location = element.getLocation();
    kind = element.getKind();
    name = element.getName();
    nameOffset = element.getNameOffset();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ElementProxy)) {
      return false;
    }
    ElementProxy proxy = (ElementProxy) obj;
    return proxy.getContext() == context && Objects.equal(location, proxy.location)
        && Objects.equal(source, proxy.source) && kind == proxy.kind
        && Objects.equal(name, proxy.name) && nameOffset == proxy.nameOffset;
  }

  /**
   * @return the {@link AnalysisContext} in which defines this {@link Element}.
   */
  public AnalysisContext getContext() {
    return context;
  }

  /**
   * @see Element#getKind()
   */
  public ElementKind getKind() {
    return kind;
  }

  /**
   * @return the {@link ElementLocation} of this {@link Element}.
   */
  public ElementLocation getLocation() {
    return location;
  }

  /**
   * @see Element#getName()
   */
  public String getName() {
    return name;
  }

  /**
   * @see Element#getNameOffset()
   */
  public int getNameOffset() {
    return nameOffset;
  }

  /**
   * @return the {@link Source} which defines this {@link Element}.
   */
  public Source getSource() {
    return source;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(context, location);
  }

  /**
   * Returns the actual {@link Element} instance, may be loaded from disk. This method may be slow.
   * 
   * @return the actual {@link Element} instance.
   */
  public Element requestElement() {
    return context.getElement(location);
  }
}
