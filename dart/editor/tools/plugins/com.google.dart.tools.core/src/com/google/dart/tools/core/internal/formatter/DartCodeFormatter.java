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
package com.google.dart.tools.core.internal.formatter;

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.formatter.CodeFormatter;
import com.google.dart.tools.core.formatter.DefaultCodeFormatterConstants;
import com.google.dart.tools.core.internal.util.Util;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.TextEdit;

import java.util.Map;

// TODO(messick) This is a replacement for DefaultCodeFormatter
public class DartCodeFormatter extends CodeFormatter {

  private DefaultCodeFormatterOptions preferences;

  // Bail out on syntax errors. We may be able to recover from some, so this may be too simple.
  private boolean hasSyntaxError;

  private AnalysisErrorListener listener = new AnalysisErrorListener() {
    @Override
    public void onError(AnalysisError event) {
      hasSyntaxError = true;
    }
  };

  public DartCodeFormatter(DefaultCodeFormatterOptions preferences) {
    this(preferences, null);
  }

  public DartCodeFormatter(DefaultCodeFormatterOptions defaultCodeFormatterOptions,
      Map<String, String> options) {
    if (options != null) {
      this.preferences = new DefaultCodeFormatterOptions(options);
    } else {
      this.preferences = new DefaultCodeFormatterOptions(
          DefaultCodeFormatterConstants.getDartConventionsSettings());
    }
    if (defaultCodeFormatterOptions != null) {
      this.preferences.set(defaultCodeFormatterOptions.getMap());
    }
  }

  public DartCodeFormatter(Map<String, String> options) {
    this(null, options);
  }

  @Override
  public String createIndentationString(final int indentationLevel) {
    if (indentationLevel < 0) {
      throw new IllegalArgumentException();
    }
    int tabs = 0;
    int spaces = 0;
    switch (preferences.tab_char) {
      case DefaultCodeFormatterOptions.SPACE:
        spaces = indentationLevel * preferences.tab_size;
        break;
      case DefaultCodeFormatterOptions.TAB:
        tabs = indentationLevel;
        break;
      case DefaultCodeFormatterOptions.MIXED:
        int tabSize = preferences.tab_size;
        if (tabSize != 0) {
          int spaceEquivalents = indentationLevel * preferences.indentation_size;
          tabs = spaceEquivalents / tabSize;
          spaces = spaceEquivalents % tabSize;
        }
        break;
      default:
        return Util.EMPTY_STRING;
    }
    if (tabs == 0 && spaces == 0) {
      return Util.EMPTY_STRING;
    }
    StringBuffer buffer = new StringBuffer(tabs + spaces);
    for (int i = 0; i < tabs; i++) {
      buffer.append('\t');
    }
    for (int i = 0; i < spaces; i++) {
      buffer.append(' ');
    }
    return buffer.toString();
  }

  @Override
  public TextEdit format(int kind, String source, int offset, int length, int indentationLevel,
      String lineSeparator) {
    return format(
        kind,
        source,
        new IRegion[] {new Region(offset, length)},
        indentationLevel,
        lineSeparator);
  }

  @Override
  public TextEdit format(int kind, String source, IRegion[] regions, int indentationLevel,
      String lineSeparator) {
    if (!DartCoreDebug.ENABLE_FORMATTER) {
      return null;
    }
    if (!regionsSatisfiesPreconditions(regions, source.length())) {
      throw new IllegalArgumentException();
    }
    if (lineSeparator != null) {
      preferences.line_separator = lineSeparator;
    } else {
      preferences.line_separator = System.getProperty("line.separator"); //$NON-NLS-1$
    }
    preferences.initial_indentation_level = indentationLevel;
    hasSyntaxError = false;
    StringScanner scanner = new StringScanner(null, source, listener);
    Token start = scanner.tokenize();
    if (hasSyntaxError) {
      return null;
    }
    Parser parser = new Parser(null, listener);
    ASTNode node;
    switch (kind) {
      case K_CLASS_BODY_DECLARATIONS:
      case K_COMPILATION_UNIT:
        node = parser.parseCompilationUnit(start);
        break;
      case K_EXPRESSION:
        node = parser.parseExpression(start);
        break;
      case K_STATEMENTS:
        node = parser.parseStatement(start); // TODO(messick) single or plural?
        break;
      case K_JAVA_DOC:
      case K_MULTI_LINE_COMMENT:
      case K_SINGLE_LINE_COMMENT:
        return formatComment(source, indentationLevel, lineSeparator, regions);
      default:
        return null;
    }
    if (hasSyntaxError) {
      return null;
    }
    FormattingEngine formatter = new FormattingEngine(preferences);
    return formatter.format(source, node, start, kind, regions);
  }

  private TextEdit formatComment(String source, int indentationLevel, String lineSeparator,
      IRegion[] regions) {
    // TODO(messick) Format a single comment. No parsing has been done at this point.
    return null;
  }

  /**
   * Return true iff all <code>regions</code> are within maxLength, <code>regions</code> are not
   * overlapping, and <code>regions</code> are sorted.
   */
  private boolean regionsSatisfiesPreconditions(IRegion[] regions, int maxLength) {
    int regionsLength = regions == null ? 0 : regions.length;
    if (regionsLength == 0) {
      return false;
    }
    IRegion first = regions[0];
    if (first.getOffset() < 0 || first.getLength() < 0
        || first.getOffset() + first.getLength() > maxLength) {
      return false;
    }
    int lastOffset = first.getOffset() + first.getLength() - 1;
    for (int i = 1; i < regionsLength; i++) {
      IRegion current = regions[i];
      if (lastOffset > current.getOffset()) {
        return false;
      }
      if (current.getOffset() < 0 || current.getLength() < 0
          || current.getOffset() + current.getLength() > maxLength) {
        return false;
      }
      lastOffset = current.getOffset() + current.getLength() - 1;
    }
    return true;
  }
}
