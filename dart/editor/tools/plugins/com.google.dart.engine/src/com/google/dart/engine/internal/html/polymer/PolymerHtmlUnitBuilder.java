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

package com.google.dart.engine.internal.html.polymer;

import com.google.common.collect.Lists;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ExternalHtmlScriptElement;
import com.google.dart.engine.element.HtmlScriptElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.polymer.PolymerTagDartElement;
import com.google.dart.engine.element.polymer.PolymerTagHtmlElement;
import com.google.dart.engine.element.visitor.RecursiveElementVisitor;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.html.ast.HtmlScriptTagNode;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.ast.visitor.RecursiveXmlVisitor;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.element.HtmlElementImpl;
import com.google.dart.engine.internal.element.polymer.PolymerTagDartElementImpl;
import com.google.dart.engine.internal.element.polymer.PolymerTagHtmlElementImpl;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;

import java.util.List;

/**
 * Instances of the class {@link PolymerHtmlUnitBuilder} build Polymer specific elements.
 */
public class PolymerHtmlUnitBuilder extends RecursiveXmlVisitor<Void> {
  private static class FoundTagDartElementError extends Error {
    private final PolymerTagDartElementImpl result;

    public FoundTagDartElementError(PolymerTagDartElementImpl result) {
      this.result = result;
    }
  }

  private final InternalAnalysisContext context;
  private final TypeProvider typeProvider;
  private final AnalysisErrorListener errorListener;

  private final Source source;
  private final LineInfo lineInfo;
  private final HtmlUnit unit;

  private final List<PolymerTagHtmlElement> tagHtmlElements = Lists.newArrayList();

  public PolymerHtmlUnitBuilder(InternalAnalysisContext context,
      AnalysisErrorListener errorListener, Source source, LineInfo lineInfo, HtmlUnit unit)
      throws AnalysisException {
    this.context = context;
    this.typeProvider = context.getTypeProvider();
    this.errorListener = errorListener;
    this.source = source;
    this.lineInfo = lineInfo;
    this.unit = unit;
  }

  /**
   * Builds Polymer specific HTML elements.
   */
  public void build() throws AnalysisException {
    unit.accept(this);
    // set Polymer tags
    HtmlElementImpl unitElement = (HtmlElementImpl) unit.getElement();
    unitElement.setPolymerTags(tagHtmlElements.toArray(new PolymerTagHtmlElement[tagHtmlElements.size()]));
  }

  @Override
  public Void visitXmlAttributeNode(XmlAttributeNode node) {
    // TODO(scheglov) implement it
    return super.visitXmlAttributeNode(node);
  }

  @Override
  public Void visitXmlTagNode(XmlTagNode node) {
    // TODO(scheglov) implement it
    if (node.getTag().equals("polymer-element")) {
      createTagHtmlElement(node);
    }
    // visit children
    return super.visitXmlTagNode(node);
  }

  private void createTagHtmlElement(XmlTagNode node) {
    XmlAttributeNode nameAttribute = node.getAttribute("name");
    if (nameAttribute == null) {
      // TODO(scheglov) report error
      return;
    }
    String name = nameAttribute.getText();
    if (name.isEmpty()) {
      // TODO(scheglov) report error
      return;
    }
    // TODO(scheglov) Check for invalid custom tag name: completely invalid; or '-' is missing.
    // TODO(scheglov) Maybe check that at least one of "template" or "script" children.
    // TODO(scheglov) Maybe check if more than one top-level "template".
    int nameOffset = nameAttribute.getTextOffset();
    PolymerTagHtmlElementImpl element = new PolymerTagHtmlElementImpl(name, nameOffset);
    PolymerTagDartElementImpl dartElement = findTagDartElement(node, name);
    if (dartElement != null) {
      element.setDartElement(dartElement);
      dartElement.setHtmlElement(element);
    }
    // TODO(scheglov) create attributes
    tagHtmlElements.add(element);
  }

  /**
   * Returns the {@link PolymerTagDartElement} that corresponds to the Polymer custom tag declared
   * by the given {@link XmlTagNode}.
   */
  private PolymerTagDartElementImpl findTagDartElement(XmlTagNode node, String name) {
    LibraryElement dartLibraryElement = getDartUnitElement(node);
    if (dartLibraryElement == null) {
      return null;
    }
    return findTagDartElement_inLibrary(dartLibraryElement, name);
  }

  /**
   * Returns the {@link PolymerTagDartElementImpl} declared in the given {@link LibraryElement} with
   * the same name as given. Maybe {@code null}.
   */
  private PolymerTagDartElementImpl findTagDartElement_inLibrary(LibraryElement library,
      final String tagName) {
    try {
      library.accept(new RecursiveElementVisitor<Void>() {
        @Override
        public Void visitPolymerTagDartElement(PolymerTagDartElement element) {
          if (element.getName().equals(tagName)) {
            throw new FoundTagDartElementError((PolymerTagDartElementImpl) element);
          }
          return null;
        }
      });
    } catch (FoundTagDartElementError e) {
      return e.result;
    }
    return null;
  }

  /**
   * Returns the only {@link LibraryElement} referenced by a direct {@code script} child. Maybe
   * {@code null} if none.
   */
  private LibraryElement getDartUnitElement(XmlTagNode node) {
    // TODO(scheglov) Maybe check if more than one "script".
    for (XmlTagNode child : node.getTagNodes()) {
      if (child instanceof HtmlScriptTagNode) {
        HtmlScriptElement scriptElement = ((HtmlScriptTagNode) child).getScriptElement();
        if (scriptElement instanceof ExternalHtmlScriptElement) {
          Source scriptSource = ((ExternalHtmlScriptElement) scriptElement).getScriptSource();
          if (scriptSource != null) {
            return context.getLibraryElement(scriptSource);
          }
        }
      }
    }
    return null;
  }
}
