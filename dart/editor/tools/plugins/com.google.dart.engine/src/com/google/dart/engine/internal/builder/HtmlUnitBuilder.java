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
package com.google.dart.engine.internal.builder;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.HtmlScriptElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.HtmlWarningCode;
import com.google.dart.engine.html.ast.HtmlScriptTagNode;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.ast.visitor.XmlVisitor;
import com.google.dart.engine.html.scanner.TokenType;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.RecordingErrorListener;
import com.google.dart.engine.internal.element.EmbeddedHtmlScriptElementImpl;
import com.google.dart.engine.internal.element.ExternalHtmlScriptElementImpl;
import com.google.dart.engine.internal.element.HtmlElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.resolver.Library;
import com.google.dart.engine.internal.resolver.LibraryResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.io.UriUtilities;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Instances of the class {@code HtmlUnitBuilder} build an element model for a single HTML unit.
 */
public class HtmlUnitBuilder implements XmlVisitor<Void> {
  private static final String SRC = "src";

  /**
   * The analysis context in which the element model will be built.
   */
  private final InternalAnalysisContext context;

  /**
   * The error listener to which errors will be reported.
   */
  private RecordingErrorListener errorListener;

  /**
   * The modification time of the source for which an element is being built.
   */
  private long modificationStamp;

  /**
   * The HTML element being built.
   */
  private HtmlElementImpl htmlElement;

  /**
   * The elements in the path from the HTML unit to the current tag node.
   */
  private ArrayList<XmlTagNode> parentNodes;

  /**
   * The script elements being built.
   */
  private ArrayList<HtmlScriptElement> scripts;

  /**
   * A set of the libraries that were resolved while resolving the HTML unit.
   */
  private Set<Library> resolvedLibraries = new HashSet<Library>();

  /**
   * Initialize a newly created HTML unit builder.
   * 
   * @param context the analysis context in which the element model will be built
   */
  public HtmlUnitBuilder(InternalAnalysisContext context) {
    this.context = context;
    this.errorListener = new RecordingErrorListener();
  }

  /**
   * Build the HTML element for the given source.
   * 
   * @param source the source describing the compilation unit
   * @param modificationStamp the modification time of the source for which an element is being
   *          built
   * @param unit the AST structure representing the HTML
   * @throws AnalysisException if the analysis could not be performed
   */
  public HtmlElementImpl buildHtmlElement(Source source, long modificationStamp, HtmlUnit unit)
      throws AnalysisException {
    this.modificationStamp = modificationStamp;
    HtmlElementImpl result = new HtmlElementImpl(context, source.getShortName());
    result.setSource(source);
    htmlElement = result;
    unit.accept(this);
    htmlElement = null;
    unit.setElement(result);
    return result;
  }

  /**
   * Return the listener to which analysis errors will be reported.
   * 
   * @return the listener to which analysis errors will be reported
   */
  public RecordingErrorListener getErrorListener() {
    return errorListener;
  }

  /**
   * Return an array containing information about all of the libraries that were resolved.
   * 
   * @return an array containing the libraries that were resolved
   */
  public Set<Library> getResolvedLibraries() {
    return resolvedLibraries;
  }

  @Override
  public Void visitHtmlScriptTagNode(HtmlScriptTagNode node) {
    if (parentNodes.contains(node)) {
      return reportCircularity(node);
    }
    parentNodes.add(node);
    try {
      Source htmlSource = htmlElement.getSource();
      XmlAttributeNode scriptAttribute = getScriptSourcePath(node);
      String scriptSourcePath = scriptAttribute == null ? null : scriptAttribute.getText();
      if (node.getAttributeEnd().getType() == TokenType.GT && scriptSourcePath == null) {
        EmbeddedHtmlScriptElementImpl script = new EmbeddedHtmlScriptElementImpl(node);
        try {
          LibraryResolver resolver = new LibraryResolver(context);
          LibraryElementImpl library = (LibraryElementImpl) resolver.resolveEmbeddedLibrary(
              htmlSource,
              modificationStamp,
              node.getScript(),
              true);
          script.setScriptLibrary(library);
          resolvedLibraries.addAll(resolver.getResolvedLibraries());
          errorListener.addAll(resolver.getErrorListener());
        } catch (AnalysisException exception) {
          //TODO (danrubel): Handle or forward the exception
          AnalysisEngine.getInstance().getLogger().logError(
              "Could not resolve script tag",
              exception);
        }
        node.setScriptElement(script);
        scripts.add(script);
      } else {
        ExternalHtmlScriptElementImpl script = new ExternalHtmlScriptElementImpl(node);
        if (scriptSourcePath != null) {
          try {
            scriptSourcePath = UriUtilities.encode(scriptSourcePath);
            // Force an exception to be thrown if the URI is invalid so that we can report the
            // problem.
            new URI(scriptSourcePath);
            Source scriptSource = context.getSourceFactory().resolveUri(
                htmlSource,
                scriptSourcePath);
            script.setScriptSource(scriptSource);
            if (!context.exists(scriptSource)) {
              reportValueError(
                  HtmlWarningCode.URI_DOES_NOT_EXIST,
                  scriptAttribute,
                  scriptSourcePath);
            }
          } catch (URISyntaxException exception) {
            reportValueError(HtmlWarningCode.INVALID_URI, scriptAttribute, scriptSourcePath);
          }
        }
        node.setScriptElement(script);
        scripts.add(script);
      }
    } finally {
      parentNodes.remove(node);
    }
    return null;
  }

  @Override
  public Void visitHtmlUnit(HtmlUnit node) {
    parentNodes = new ArrayList<XmlTagNode>();
    scripts = new ArrayList<HtmlScriptElement>();
    try {
      node.visitChildren(this);
      htmlElement.setScripts(scripts.toArray(new HtmlScriptElement[scripts.size()]));
    } finally {
      scripts = null;
      parentNodes = null;
    }
    return null;
  }

  @Override
  public Void visitXmlAttributeNode(XmlAttributeNode node) {
    return null;
  }

  @Override
  public Void visitXmlTagNode(XmlTagNode node) {
    if (parentNodes.contains(node)) {
      return reportCircularity(node);
    }
    parentNodes.add(node);
    try {
      node.visitChildren(this);
    } finally {
      parentNodes.remove(node);
    }
    return null;
  }

  /**
   * Return the first source attribute for the given tag node, or {@code null} if it does not exist.
   * 
   * @param node the node containing attributes
   * @return the source attribute contained in the given tag
   */
  private XmlAttributeNode getScriptSourcePath(XmlTagNode node) {
    for (XmlAttributeNode attribute : node.getAttributes()) {
      if (attribute.getName().equals(SRC)) {
        return attribute;
      }
    }
    return null;
  }

  private Void reportCircularity(XmlTagNode node) {
    //
    // This should not be possible, but we have an error report that suggests that it happened at
    // least once. This code will guard against infinite recursion and might help us identify the
    // cause of the issue.
    //
    StringBuilder builder = new StringBuilder();
    builder.append("Found circularity in XML nodes: ");
    boolean first = true;
    for (XmlTagNode pathNode : parentNodes) {
      if (first) {
        first = false;
      } else {
        builder.append(", ");
      }
      String tagName = pathNode.getTag();
      if (pathNode == node) {
        builder.append("*");
        builder.append(tagName);
        builder.append("*");
      } else {
        builder.append(tagName);
      }
    }
    AnalysisEngine.getInstance().getLogger().logError(builder.toString());
    return null;
  }

  /**
   * Report an error with the given error code at the given location. Use the given arguments to
   * compose the error message.
   * 
   * @param errorCode the error code of the error to be reported
   * @param offset the offset of the first character to be highlighted
   * @param length the number of characters to be highlighted
   * @param arguments the arguments used to compose the error message
   */
  private void reportErrorForOffset(ErrorCode errorCode, int offset, int length,
      Object... arguments) {
    errorListener.onError(new AnalysisError(
        htmlElement.getSource(),
        offset,
        length,
        errorCode,
        arguments));
  }

  /**
   * Report an error with the given error code at the location of the value of the given attribute.
   * Use the given arguments to compose the error message.
   * 
   * @param errorCode the error code of the error to be reported
   * @param offset the offset of the first character to be highlighted
   * @param length the number of characters to be highlighted
   * @param arguments the arguments used to compose the error message
   */
  private void reportValueError(ErrorCode errorCode, XmlAttributeNode attribute,
      Object... arguments) {
    int offset = attribute.getValueToken().getOffset() + 1;
    int length = attribute.getValueToken().getLength() - 2;
    reportErrorForOffset(errorCode, offset, length, arguments);
  }
}
