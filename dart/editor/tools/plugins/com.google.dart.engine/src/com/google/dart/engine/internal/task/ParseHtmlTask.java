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
package com.google.dart.engine.internal.task;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.ast.visitor.RecursiveXmlVisitor;
import com.google.dart.engine.html.parser.HtmlParseResult;
import com.google.dart.engine.html.parser.HtmlParser;
import com.google.dart.engine.html.scanner.HtmlScanResult;
import com.google.dart.engine.html.scanner.HtmlScanner;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;

import java.net.URI;
import java.util.ArrayList;

/**
 * Instances of the class {@code ParseHtmlTask} parse a specific source as an HTML file.
 */
public class ParseHtmlTask extends AnalysisTask {
  /**
   * The source to be parsed.
   */
  private Source source;

  /**
   * The time at which the contents of the source were last modified.
   */
  private long modificationTime = -1L;

  /**
   * The line information that was produced.
   */
  private LineInfo lineInfo;

  /**
   * The HTML unit that was produced by parsing the source.
   */
  private HtmlUnit unit;

  /**
   * An array containing the sources of the libraries that are referenced within the HTML.
   */
  private Source[] referencedLibraries = Source.EMPTY_ARRAY;

  /**
   * The name of the 'src' attribute in a HTML tag.
   */
  private static final String ATTRIBUTE_SRC = "src";

  /**
   * The name of the 'type' attribute in a HTML tag.
   */
  private static final String ATTRIBUTE_TYPE = "type";

  /**
   * The name of the 'script' tag in an HTML file.
   */
  private static final String TAG_SCRIPT = "script";

  /**
   * The value of the 'type' attribute of a 'script' tag that indicates that the script is written
   * in Dart.
   */
  private static final String TYPE_DART = "application/dart";

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param source the source to be parsed
   */
  public ParseHtmlTask(InternalAnalysisContext context, Source source) {
    super(context);
    this.source = source;
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    return visitor.visitParseHtmlTask(this);
  }

  /**
   * Return the HTML unit that was produced by parsing the source.
   * 
   * @return the HTML unit that was produced by parsing the source
   */
  public HtmlUnit getHtmlUnit() {
    return unit;
  }

  /**
   * Return the line information that was produced, or {@code null} if the task has not yet been
   * performed or if an exception occurred.
   * 
   * @return the line information that was produced
   */
  public LineInfo getLineInfo() {
    return lineInfo;
  }

  /**
   * Return the time at which the contents of the source that was parsed were last modified, or a
   * negative value if the task has not yet been performed or if an exception occurred.
   * 
   * @return the time at which the contents of the source that was parsed were last modified
   */
  public long getModificationTime() {
    return modificationTime;
  }

  /**
   * Return an array containing the sources of the libraries that are referenced within the HTML.
   * 
   * @return the sources of the libraries that are referenced within the HTML
   */
  public Source[] getReferencedLibraries() {
    return referencedLibraries;
  }

  /**
   * Return the source that was or is to be parsed.
   * 
   * @return the source was or is to be parsed
   */
  public Source getSource() {
    return source;
  }

  @Override
  protected String getTaskDescription() {
    if (source == null) {
      return "parse as html null source";
    }
    return "parse as html " + source.getFullName();
  }

  @Override
  protected void internalPerform() throws AnalysisException {
    HtmlScanner scanner = new HtmlScanner(source);
    try {
      source.getContents(scanner);
    } catch (Exception exception) {
      throw new AnalysisException(exception);
    }
    HtmlScanResult scannerResult = scanner.getResult();
    modificationTime = scannerResult.getModificationTime();
    lineInfo = new LineInfo(scannerResult.getLineStarts());
    HtmlParseResult result = new HtmlParser(source).parse(scannerResult);
    unit = result.getHtmlUnit();
    referencedLibraries = getLibrarySources();
  }

  /**
   * Return the sources of libraries that are referenced in the specified HTML file.
   * 
   * @return the sources of libraries that are referenced in the HTML file
   */
  private Source[] getLibrarySources() {
    final ArrayList<Source> libraries = new ArrayList<Source>();
    unit.accept(new RecursiveXmlVisitor<Void>() {
      @Override
      public Void visitXmlTagNode(XmlTagNode node) {
        if (node.getTag().getLexeme().equalsIgnoreCase(TAG_SCRIPT)) {
          boolean isDartScript = false;
          XmlAttributeNode scriptAttribute = null;
          for (XmlAttributeNode attribute : node.getAttributes()) {
            if (attribute.getName().getLexeme().equalsIgnoreCase(ATTRIBUTE_SRC)) {
              scriptAttribute = attribute;
            } else if (attribute.getName().getLexeme().equalsIgnoreCase(ATTRIBUTE_TYPE)) {
              if (attribute.getText().equalsIgnoreCase(TYPE_DART)) {
                isDartScript = true;
              }
            }
          }
          if (isDartScript && scriptAttribute != null) {
            try {
              URI uri = new URI(null, null, scriptAttribute.getText(), null);
              String fileName = uri.getPath();
              Source librarySource = getContext().getSourceFactory().resolveUri(source, fileName);
              if (librarySource.exists()) {
                libraries.add(librarySource);
              }
            } catch (Exception exception) {
              AnalysisEngine.getInstance().getLogger().logInformation(
                  "Invalid URL ('" + scriptAttribute.getText() + "') in script tag in '"
                      + source.getFullName() + "'",
                  exception);
            }
          }
        }
        return super.visitXmlTagNode(node);
      }
    });
    if (libraries.isEmpty()) {
      return Source.EMPTY_ARRAY;
    }
    return libraries.toArray(new Source[libraries.size()]);
  }
}
