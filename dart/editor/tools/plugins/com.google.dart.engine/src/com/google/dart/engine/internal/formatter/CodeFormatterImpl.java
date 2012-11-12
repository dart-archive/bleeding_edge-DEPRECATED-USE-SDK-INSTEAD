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

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.formatter.CodeFormatter;
import com.google.dart.engine.formatter.CodeFormatterOptions;
import com.google.dart.engine.formatter.EditRecorder;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;

/**
 * Dart {@link CodeFormatter} implementation.
 */
public class CodeFormatterImpl extends CodeFormatter implements AnalysisErrorListener {

  private final CodeFormatterOptions options;

  // Bail out on lexing/parsing errors (for now)
  private boolean hasError;

  /**
   * Create a formatter instance.
   * 
   * @param options formatting options
   */
  public CodeFormatterImpl(CodeFormatterOptions options) {
    this.options = options;
  }

  @Override
  public void format(Kind kind, String source, int offset, int end, int indentationLevel,
      EditRecorder<?> recorder) {

    validate(source, offset, end);

    StringScanner scanner = new StringScanner(null, source, this);
    Token start = scanner.tokenize();

    if (hasError) {
      //TODO (pquitslund): signal failure
      return;
    }

    Parser parser = new Parser(null, this);
    ASTNode node;

    switch (kind) {
      case COMPILATION_UNIT:
        node = parser.parseCompilationUnit(start);
        break;
      default:
        throw new IllegalArgumentException("Unsupported format kind: " + kind);
    }

    if (hasError) {
      //TODO (pquitslund): signal failure
      return;
    }

    FormattingEngine formatter = new FormattingEngine(options);
    formatter.format(source, node, start, kind, recorder);

  }

  @Override
  public void onError(AnalysisError error) {
    hasError = true;
  }

  private void validate(String source, int offset, int end) {
    if (offset < 0 || end < 0 || end > source.length()) {
      throw new IllegalArgumentException();
    }
  }
}
