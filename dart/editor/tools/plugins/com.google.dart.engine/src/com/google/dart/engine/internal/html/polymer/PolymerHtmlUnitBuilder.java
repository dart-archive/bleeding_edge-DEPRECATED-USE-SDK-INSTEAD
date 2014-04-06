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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ElementAnnotation;
import com.google.dart.engine.element.ExternalHtmlScriptElement;
import com.google.dart.engine.element.FieldElement;
import com.google.dart.engine.element.HtmlScriptElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.element.polymer.PolymerAttributeElement;
import com.google.dart.engine.element.polymer.PolymerTagDartElement;
import com.google.dart.engine.element.polymer.PolymerTagHtmlElement;
import com.google.dart.engine.element.visitor.RecursiveElementVisitor;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.PolymerCode;
import com.google.dart.engine.html.ast.HtmlScriptTagNode;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.ast.visitor.RecursiveXmlVisitor;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.element.HtmlElementImpl;
import com.google.dart.engine.internal.element.polymer.PolymerAttributeElementImpl;
import com.google.dart.engine.internal.element.polymer.PolymerTagDartElementImpl;
import com.google.dart.engine.internal.element.polymer.PolymerTagHtmlElementImpl;
import com.google.dart.engine.internal.resolver.TypeProvider;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;

import java.util.List;
import java.util.Set;

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

  private class NameToken {
    private final int offset;
    private final String value;

    public NameToken(int offset, String value) {
      this.offset = offset;
      this.value = value;
    }
  }

  /**
   * These names are forbidden to use as a custom tag name.
   * <p>
   * http://w3c.github.io/webcomponents/spec/custom/#concepts
   */
  private static final Set<String> FORBIDDEN_TAG_NAMES = Sets.newHashSet(new String[] {
      "annotation-xml", "color-profile", "font-face", "font-face-src", "font-face-uri",
      "font-face-format", "font-face-name", "missing-glyph",});

  @VisibleForTesting
  public static boolean isValidAttributeName(String name) {
    // cannot be empty
    if (name.isEmpty()) {
      return false;
    }
    // check characters
    int length = name.length();
    for (int i = 0; i < length; i++) {
      char c = name.charAt(i);
      if (i == 0) {
        if (!Character.isLetter(c)) {
          return false;
        }
      } else {
        if (!(Character.isLetterOrDigit(c) || c == '_')) {
          return false;
        }
      }
    }
    return true;
  }

  @VisibleForTesting
  public static boolean isValidTagName(String name) {
    // cannot be empty
    if (name.isEmpty()) {
      return false;
    }
    // check for forbidden name
    if (FORBIDDEN_TAG_NAMES.contains(name)) {
      return false;
    }
    // check characters
    int length = name.length();
    boolean hasDash = false;
    for (int i = 0; i < length; i++) {
      char c = name.charAt(i);
      // check for '-'
      if (c == '-') {
        hasDash = true;
      }
      // check character
      if (i == 0) {
        if (hasDash) {
          return false;
        }
        if (!Character.isLetter(c)) {
          return false;
        }
      } else {
        if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_')) {
          return false;
        }
      }
    }
    if (!hasDash) {
      return false;
    }
    return true;
  }

  private final InternalAnalysisContext context;
  private final TypeProvider typeProvider;
  private final AnalysisErrorListener errorListener;
  private final Source source;
  private final LineInfo lineInfo;
  private final HtmlUnit unit;

  private final List<PolymerTagHtmlElement> tagHtmlElements = Lists.newArrayList();

  private XmlTagNode elementNode;
  private String elementName;
  private PolymerTagHtmlElementImpl htmlElement;
  private PolymerTagDartElementImpl dartElement;

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
  public Void visitXmlTagNode(XmlTagNode node) {
    if (node.getTag().equals("polymer-element")) {
      createTagHtmlElement(node);
    }
    // visit children
    return super.visitXmlTagNode(node);
  }

  private void createAttributeElements() {
    // prepare "attributes" attribute
    XmlAttributeNode attributesAttribute = elementNode.getAttribute("attributes");
    if (attributesAttribute == null) {
      return;
    }
    // check if there is a Dart part to resolve against it
    if (dartElement == null) {
      // TODO(scheglov) maybe report error (if it is allowed at all to have element without Dart part)
      return;
    }
    // prepare value of the "attributes" attribute
    String attributesText = attributesAttribute.getText();
    if (attributesText.trim().isEmpty()) {
      reportErrorForAttribute(attributesAttribute, PolymerCode.EMPTY_ATTRIBUTES);
      return;
    }
    // prepare attribute name tokens
    List<NameToken> nameTokens = Lists.newArrayList();
    {
      int index = 0;
      int textOffset = attributesAttribute.getTextOffset();
      int nameOffset = -1;
      StringBuilder nameBuilder = new StringBuilder();
      while (index < attributesText.length()) {
        char c = attributesText.charAt(index++);
        if (Character.isWhitespace(c)) {
          if (nameOffset != -1) {
            nameTokens.add(new NameToken(nameOffset, nameBuilder.toString()));
            nameBuilder = new StringBuilder();
            nameOffset = -1;
          }
          continue;
        }
        if (nameOffset == -1) {
          nameOffset = textOffset + index - 1;
        }
        nameBuilder.append(c);
      }
      if (nameOffset != -1) {
        nameTokens.add(new NameToken(nameOffset, nameBuilder.toString()));
        nameBuilder = new StringBuilder();
      }
    }
    // create attributes for name tokens
    List<PolymerAttributeElement> attributes = Lists.newArrayList();
    Set<String> definedNames = Sets.newHashSet();
    ClassElement classElement = dartElement.getClassElement();
    for (NameToken nameToken : nameTokens) {
      int offset = nameToken.offset;
      // prepare name
      String name = nameToken.value;
      if (!isValidAttributeName(name)) {
        reportErrorForNameToken(nameToken, PolymerCode.INVALID_ATTRIBUTE_NAME, name);
        continue;
      }
      if (!definedNames.add(name)) {
        reportErrorForNameToken(nameToken, PolymerCode.DUPLICATE_ATTRIBUTE_DEFINITION, name);
        continue;
      }
      // create attribute
      PolymerAttributeElementImpl attribute = new PolymerAttributeElementImpl(name, offset);
      attributes.add(attribute);
      // resolve field
      FieldElement field = classElement.getField(name);
      if (field == null) {
        reportErrorForNameToken(
            nameToken,
            PolymerCode.UNDEFINED_ATTRIBUTE_FIELD,
            name,
            classElement.getDisplayName());
        continue;
      }
      if (!isPublishedField(field)) {
        reportErrorForNameToken(
            nameToken,
            PolymerCode.ATTRIBUTE_FIELD_NOT_PUBLISHED,
            name,
            classElement.getDisplayName());
      }
      attribute.setField(field);
    }
    htmlElement.setAttributes(attributes.toArray(new PolymerAttributeElement[attributes.size()]));
  }

  private void createTagHtmlElement(XmlTagNode node) {
    this.elementNode = node;
    this.elementName = null;
    this.htmlElement = null;
    this.dartElement = null;
    // prepare 'name' attribute
    XmlAttributeNode nameAttribute = node.getAttribute("name");
    if (nameAttribute == null) {
      reportErrorForToken(node.getTagToken(), PolymerCode.MISSING_TAG_NAME);
      return;
    }
    // prepare name
    elementName = nameAttribute.getText();
    if (!isValidTagName(elementName)) {
      reportErrorForAttributeValue(nameAttribute, PolymerCode.INVALID_TAG_NAME, elementName);
      return;
    }
    // TODO(scheglov) Maybe check that at least one of "template" or "script" children.
    // TODO(scheglov) Maybe check if more than one top-level "template".
    // create HTML element
    int nameOffset = nameAttribute.getTextOffset();
    htmlElement = new PolymerTagHtmlElementImpl(elementName, nameOffset);
    // bind to the corresponding Dart element
    dartElement = findTagDartElement();
    if (dartElement != null) {
      htmlElement.setDartElement(dartElement);
      dartElement.setHtmlElement(htmlElement);
    }
    // TODO(scheglov) create attributes
    createAttributeElements();
    // done
    tagHtmlElements.add(htmlElement);
  }

  /**
   * Returns the {@link PolymerTagDartElement} that corresponds to the Polymer custom tag declared
   * by the given {@link XmlTagNode}.
   */
  private PolymerTagDartElementImpl findTagDartElement() {
    LibraryElement dartLibraryElement = getDartUnitElement();
    if (dartLibraryElement == null) {
      return null;
    }
    return findTagDartElement_inLibrary(dartLibraryElement);
  }

  /**
   * Returns the {@link PolymerTagDartElementImpl} declared in the given {@link LibraryElement} with
   * the {@link #elementName}. Maybe {@code null}.
   */
  private PolymerTagDartElementImpl findTagDartElement_inLibrary(LibraryElement library) {
    try {
      library.accept(new RecursiveElementVisitor<Void>() {
        @Override
        public Void visitPolymerTagDartElement(PolymerTagDartElement element) {
          if (element.getName().equals(elementName)) {
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
  private LibraryElement getDartUnitElement() {
    // TODO(scheglov) Maybe check if more than one "script".
    for (XmlTagNode child : elementNode.getTagNodes()) {
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

  private boolean isPublishedAnnotation(ElementAnnotation annotation) {
    Element element = annotation.getElement();
    if (element != null && element.getName().equals("published")) {
      return true;
    }
    return false;
  }

  private boolean isPublishedField(FieldElement field) {
    ElementAnnotation[] annotations = field.getMetadata();
    for (ElementAnnotation annotation : annotations) {
      if (isPublishedAnnotation(annotation)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Reports an error on the attribute's value, or (if absent) on the attribute's name.
   */
  private void reportErrorForAttribute(XmlAttributeNode node, ErrorCode errorCode,
      Object... arguments) {
    reportErrorForOffset(node.getOffset(), node.getLength(), errorCode, arguments);
  }

  /**
   * Reports an error on the attribute's value, or (if absent) on the attribute's name.
   */
  private void reportErrorForAttributeValue(XmlAttributeNode node, ErrorCode errorCode,
      Object... arguments) {
    Token valueToken = node.getValueToken();
    if (valueToken == null || valueToken.isSynthetic()) {
      reportErrorForAttribute(node, errorCode, arguments);
    } else {
      reportErrorForToken(valueToken, errorCode, arguments);
    }
  }

  private void reportErrorForNameToken(NameToken token, ErrorCode errorCode, Object... arguments) {
    int offset = token.offset;
    int length = token.value.length();
    reportErrorForOffset(offset, length, errorCode, arguments);
  }

  private void reportErrorForOffset(int offset, int length, ErrorCode errorCode,
      Object... arguments) {
    errorListener.onError(new AnalysisError(source, offset, length, errorCode, arguments));
  }

  private void reportErrorForToken(Token token, ErrorCode errorCode, Object... arguments) {
    int offset = token.getOffset();
    int length = token.getLength();
    reportErrorForOffset(offset, length, errorCode, arguments);
  }
}
