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

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.UriBasedDirective;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.html.ast.HtmlScriptTagNode;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.visitor.RecursiveXmlVisitor;
import com.google.dart.engine.html.parser.HtmlParser;
import com.google.dart.engine.html.scanner.AbstractScanner;
import com.google.dart.engine.html.scanner.StringScanner;
import com.google.dart.engine.html.scanner.Token;
import com.google.dart.engine.internal.context.InternalAnalysisContext;
import com.google.dart.engine.internal.context.RecordingErrorListener;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.source.LineInfo;

import java.net.URI;
import java.net.URISyntaxException;
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
  private long modificationTime;

  /**
   * The contents of the source.
   */
  private CharSequence content;

  /**
   * The line information that was produced.
   */
  private LineInfo lineInfo;

  /**
   * The HTML unit that was produced by parsing the source.
   */
  private HtmlUnit unit;

  /**
   * The errors that were produced by scanning and parsing the source.
   */
  private AnalysisError[] errors = AnalysisError.NO_ERRORS;

  /**
   * An array containing the sources of the libraries that are referenced within the HTML.
   */
  private Source[] referencedLibraries = Source.EMPTY_ARRAY;

  /**
   * The name of the 'src' attribute in a HTML tag.
   */
  private static final String ATTRIBUTE_SRC = "src";

  /**
   * The name of the 'script' tag in an HTML file.
   */
  private static final String TAG_SCRIPT = "script";

  /**
   * Initialize a newly created task to perform analysis within the given context.
   * 
   * @param context the context in which the task is to be performed
   * @param source the source to be parsed
   * @param modificationTime the time at which the contents of the source were last modified
   * @param content the contents of the source
   */
  public ParseHtmlTask(InternalAnalysisContext context, Source source, long modificationTime,
      CharSequence content) {
    super(context);
    this.source = source;
    this.modificationTime = modificationTime;
    this.content = content;
  }

  @Override
  public <E> E accept(AnalysisTaskVisitor<E> visitor) throws AnalysisException {
    return visitor.visitParseHtmlTask(this);
  }

  /**
   * Return the errors that were produced by scanning and parsing the source, or {@code null} if the
   * task has not yet been performed or if an exception occurred.
   * 
   * @return the errors that were produced by scanning and parsing the source
   */
  public AnalysisError[] getErrors() {
    return errors;
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
    try {
      AbstractScanner scanner = new StringScanner(source, content);
      scanner.setPassThroughElements(new String[] {TAG_SCRIPT});
      Token token = scanner.tokenize();
      lineInfo = new LineInfo(scanner.getLineStarts());
      final RecordingErrorListener errorListener = new RecordingErrorListener();
      unit = new HtmlParser(source, errorListener).parse(token, lineInfo);
      unit.accept(new RecursiveXmlVisitor<Void>() {
        @Override
        public Void visitHtmlScriptTagNode(HtmlScriptTagNode node) {
          resolveScriptDirectives(node.getScript(), errorListener);
          return null;
        }
      });
      errors = errorListener.getErrorsForSource(source);
      referencedLibraries = getLibrarySources();
    } catch (Exception exception) {
      throw new AnalysisException(exception);
    }
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
      public Void visitHtmlScriptTagNode(HtmlScriptTagNode node) {
        XmlAttributeNode scriptAttribute = null;
        for (XmlAttributeNode attribute : node.getAttributes()) {
          if (attribute.getName().equalsIgnoreCase(ATTRIBUTE_SRC)) {
            scriptAttribute = attribute;
          }
        }
        if (scriptAttribute != null) {
          try {
            URI uri = new URI(null, null, scriptAttribute.getText(), null);
            String fileName = uri.getPath();
            Source librarySource = getContext().getSourceFactory().resolveUri(source, fileName);
            if (getContext().exists(librarySource)) {
              libraries.add(librarySource);
            }
          } catch (URISyntaxException e) {
            // ignored - invalid URI reported during resolution phase
          }
        }
        return super.visitHtmlScriptTagNode(node);
      }
    });
    if (libraries.isEmpty()) {
      return Source.EMPTY_ARRAY;
    }
    return libraries.toArray(new Source[libraries.size()]);
  }

  /**
   * Resolves directives in the given {@link CompilationUnit}.
   */
  private void resolveScriptDirectives(CompilationUnit script, AnalysisErrorListener errorListener) {
    if (script == null) {
      return;
    }
    AnalysisContext analysisContext = getContext();
    for (Directive directive : script.getDirectives()) {
      if (directive instanceof UriBasedDirective) {
        ParseDartTask.resolveDirective(
            analysisContext,
            source,
            (UriBasedDirective) directive,
            errorListener);
      }
    }
  }
}
