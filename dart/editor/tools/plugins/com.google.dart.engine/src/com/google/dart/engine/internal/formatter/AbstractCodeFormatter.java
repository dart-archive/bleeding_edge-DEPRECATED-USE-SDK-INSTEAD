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
package com.google.dart.engine.internal.formatter;

import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.formatter.CodeFormatter;
import com.google.dart.engine.formatter.CodeFormatterOptions;
import com.google.dart.engine.formatter.FormatterException;
import com.google.dart.engine.formatter.edit.EditBuilder;
import com.google.dart.engine.formatter.edit.EditOperation;
import com.google.dart.engine.formatter.edit.EditRecorder;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;

import java.util.List;

/**
 * Abstract Dart {@link CodeFormatter} implementation.
 * 
 * @param <D> the document type
 * @param <R> an (optional) return result type
 */
public class AbstractCodeFormatter<D, R> extends CodeFormatter<D, R> implements
    AnalysisErrorListener {

  private final CodeFormatterOptions options;
  private final EditRecorder<D, R> recorder;
  private final EditBuilder<D, R> builder;

  private final List<AnalysisError> errors = Lists.newArrayList();

  /**
   * Create a formatter instance.
   * 
   * @param options formatting options.
   * @param recorder a callback used to collect edits that describe the changes required to apply
   *          formatting to the given source. Must not be {@code null}.
   */
  public AbstractCodeFormatter(CodeFormatterOptions options, EditRecorder<D, R> recorder,
      EditBuilder<D, R> builder) {
    this.options = options;
    this.recorder = recorder;
    this.builder = builder;
  }

  @Override
  public EditOperation<D, R> format(Kind kind, String source, int offset, int end,
      int indentationLevel) throws FormatterException {

    validate(source, offset, end);

    Token start = tokenize(source);
    checkForErrors();

    ASTNode node = parse(kind, start);
    checkForErrors();

    doFormat(kind, source, start, node);

    return builder.buildEdit(recorder.getEdits());
  }

  @Override
  public void onError(AnalysisError error) {
    errors.add(error);
  }

  private void checkForErrors() throws FormatterException {
    if (errors.size() > 0) {
      throw FormatterException.forError(errors);
    }
  }

  private void doFormat(Kind kind, String source, Token start, ASTNode node) {
    FormattingEngine formatter = new FormattingEngine(options);
    formatter.format(source, node, start, kind, recorder);
  }

  private ASTNode parse(Kind kind, Token start) {

    Parser parser = new Parser(null, this);
    ASTNode node;

    switch (kind) {
      case COMPILATION_UNIT:
        node = parser.parseCompilationUnit(start);
        break;
      default:
        throw new IllegalArgumentException("Unsupported format kind: " + kind);
    }
    return node;
  }

  private Token tokenize(String source) {
    StringScanner scanner = new StringScanner(null, source, this);
    Token start = scanner.tokenize();
    return start;
  }

  private void validate(String source, int offset, int end) {
    if (offset < 0 || end < 0 || end > source.length()) {
      throw new IllegalArgumentException();
    }
  }
}
