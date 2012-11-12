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
import com.google.dart.engine.ast.visitor.RecursiveASTVisitor;
import com.google.dart.engine.formatter.CodeFormatter.Kind;
import com.google.dart.engine.formatter.CodeFormatterOptions;
import com.google.dart.engine.formatter.EditRecorder;
import com.google.dart.engine.scanner.Token;

/**
 * An AST visitor that drives formatting heuristics.
 */
public class FormattingEngine extends RecursiveASTVisitor<Void> {

  @SuppressWarnings("unused")
  private final CodeFormatterOptions options;
  @SuppressWarnings("unused")
  private EditRecorder<?> recorder;
  @SuppressWarnings("unused")
  private Kind kind;

  public FormattingEngine(CodeFormatterOptions options) {
    this.options = options;
  }

  public void format(String source, ASTNode node, Token start, Kind kind, EditRecorder<?> recorder) {
    this.kind = kind;
    this.recorder = recorder;
    node.accept(this);
  }

}
