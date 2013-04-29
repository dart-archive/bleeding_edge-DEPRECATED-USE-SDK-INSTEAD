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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.HtmlScriptElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.HtmlWarningCode;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.ast.visitor.XmlVisitor;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.html.scanner.TokenType;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.EmbeddedHtmlScriptElementImpl;
import com.google.dart.engine.internal.element.ExternalHtmlScriptElementImpl;
import com.google.dart.engine.internal.element.HtmlElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;
import com.google.dart.engine.utilities.source.LineInfo.Location;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Instances of the class {@code HtmlUnitBuilder} build an element model for a single HTML unit.
 */
public class HtmlUnitBuilder implements XmlVisitor<Void> {
  private static final String APPLICATION_DART_IN_DOUBLE_QUOTES = "\"application/dart\"";
  private static final String APPLICATION_DART_IN_SINGLE_QUOTES = "'application/dart'";
  private static final String SCRIPT = "script";
  private static final String SRC = "src";
  private static final String TYPE = "type";

  /**
   * The analysis context in which the element model will be built.
   */
  private final InternalAnalysisContext context;

  /**
   * The error listener to which errors will be reported.
   */
  private AnalysisErrorListener errorListener;

  /**
   * The line information associated with the source for which an element is being built, or
   * {@code null} if we are not building an element.
   */
  private LineInfo lineInfo;

  /**
   * The HTML element being built.
   */
  private HtmlElementImpl htmlElement;

  /**
   * The script elements being built.
   */
  private ArrayList<HtmlScriptElement> scripts;

  /**
   * Initialize a newly created HTML unit builder.
   * 
   * @param context the analysis context in which the element model will be built
   * @param errorListener the error listener to which errors will be reported
   */
  public HtmlUnitBuilder(InternalAnalysisContext context, AnalysisErrorListener errorListener) {
    this.context = context;
    this.errorListener = errorListener;
  }

  /**
   * Build the HTML element for the given source.
   * 
   * @param source the source describing the compilation unit
   * @return the HTML element that was built
   * @throws AnalysisException if the analysis could not be performed
   */
  public HtmlElementImpl buildHtmlElement(Source source) throws AnalysisException {
    return buildHtmlElement(source, context.parseHtmlUnit(source));
  }

  /**
   * Build the HTML element for the given source.
   * 
   * @param source the source describing the compilation unit
   * @param unit the AST structure representing the HTML
   * @throws AnalysisException if the analysis could not be performed
   */
  public HtmlElementImpl buildHtmlElement(Source source, HtmlUnit unit) throws AnalysisException {
    lineInfo = context.computeLineInfo(source);
    HtmlElementImpl result = new HtmlElementImpl(context, source.getShortName());
    result.setSource(source);
    htmlElement = result;
    unit.accept(this);
    htmlElement = null;
    unit.setElement(result);
    return result;
  }

  @Override
  public Void visitHtmlUnit(HtmlUnit node) {
    scripts = new ArrayList<HtmlScriptElement>();
    node.visitChildren(this);
    htmlElement.setScripts(scripts.toArray(new HtmlScriptElement[scripts.size()]));
    scripts = null;
    return null;
  }

  @Override
  public Void visitXmlAttributeNode(XmlAttributeNode node) {
    return null;
  }

  @Override
  public Void visitXmlTagNode(XmlTagNode node) {
    if (isScriptNode(node)) {
      Source htmlSource = htmlElement.getSource();
      XmlAttributeNode scriptAttribute = getScriptSourcePath(node);
      String scriptSourcePath = scriptAttribute == null ? null : scriptAttribute.getText();
      if (node.getAttributeEnd().getType() == TokenType.GT && scriptSourcePath == null) {
        EmbeddedHtmlScriptElementImpl script = new EmbeddedHtmlScriptElementImpl(node);
        String contents = node.getContent();

        //TODO (danrubel): Move scanning embedded scripts into AnalysisContextImpl
        // so that clients such as builder can scan, parse, and get errors without resolving
        int attributeEnd = node.getAttributeEnd().getEnd();
        Location location = lineInfo.getLocation(attributeEnd);
        StringScanner scanner = new StringScanner(htmlSource, contents, errorListener);
        scanner.setSourceStart(location.getLineNumber(), location.getColumnNumber(), attributeEnd);
        com.google.dart.engine.scanner.Token firstToken = scanner.tokenize();
        int[] lineStarts = scanner.getLineStarts();

        //TODO (danrubel): Move parsing embedded scripts into AnalysisContextImpl
        // so that clients such as builder can scan, parse, and get errors without resolving
        Parser parser = new Parser(null, errorListener);
        CompilationUnit unit = parser.parseCompilationUnit(firstToken);
        unit.setLineInfo(new LineInfo(lineStarts));

        try {
          CompilationUnitBuilder builder = new CompilationUnitBuilder();
          CompilationUnitElementImpl elem = builder.buildCompilationUnit(htmlSource, unit);
          LibraryElementImpl library = new LibraryElementImpl(context, null);
          library.setDefiningCompilationUnit(elem);
          script.setScriptLibrary(library);
        } catch (AnalysisException exception) {
          //TODO (danrubel): Handle or forward the exception
          exception.printStackTrace();
        }

        scripts.add(script);
      } else {
        ExternalHtmlScriptElementImpl script = new ExternalHtmlScriptElementImpl(node);
        if (scriptSourcePath != null) {
          try {
            new URI(scriptSourcePath);
            Source scriptSource = context.getSourceFactory().resolveUri(
                htmlSource,
                scriptSourcePath);
            script.setScriptSource(scriptSource);
            if (!scriptSource.exists()) {
              reportError(
                  HtmlWarningCode.URI_DOES_NOT_EXIST,
                  scriptAttribute.getOffset() + 1,
                  scriptSourcePath.length());
            }
          } catch (URISyntaxException exception) {
            reportError(
                HtmlWarningCode.INVALID_URI,
                scriptAttribute.getOffset() + 1,
                scriptSourcePath.length());
          }
        }
        scripts.add(script);
      }
    } else {
      node.visitChildren(this);
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
      if (attribute.getName().getLexeme().equals(SRC)) {
        return attribute;
      }
    }
    return null;
  }

  /**
   * Determine if the specified node is a Dart script.
   * 
   * @param node the node to be tested (not {@code null})
   * @return {@code true} if the node is a Dart script
   */
  private boolean isScriptNode(XmlTagNode node) {
    if (node.getTagNodes().size() != 0 || !node.getTag().getLexeme().equals(SCRIPT)) {
      return false;
    }
    for (XmlAttributeNode attribute : node.getAttributes()) {
      if (attribute.getName().getLexeme().equals(TYPE)) {
        Token valueToken = attribute.getValue();
        if (valueToken != null) {
          String value = valueToken.getLexeme();
          if (value.equals(APPLICATION_DART_IN_DOUBLE_QUOTES)
              || value.equals(APPLICATION_DART_IN_SINGLE_QUOTES)) {
            return true;
          }
        }
      }
    }
    return false;
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
  private void reportError(ErrorCode errorCode, int offset, int length, Object... arguments) {
    errorListener.onError(new AnalysisError(
        htmlElement.getSource(),
        offset,
        length,
        errorCode,
        arguments));
  }
}
