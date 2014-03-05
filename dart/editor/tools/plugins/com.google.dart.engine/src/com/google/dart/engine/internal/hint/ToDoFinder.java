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
package com.google.dart.engine.internal.hint;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.error.TodoCode;
import com.google.dart.engine.internal.error.ErrorReporter;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;

import java.util.regex.Matcher;

/**
 * Instances of the class {@code ToDoFinder} find to-do comments in Dart code.
 */
public class ToDoFinder {
  /**
   * The error reporter by which to-do comments will be reported.
   */
  private ErrorReporter errorReporter;

  /**
   * Initialize a newly created to-do finder to report to-do comments to the given reporter.
   * 
   * @param errorReporter the error reporter by which to-do comments will be reported
   */
  public ToDoFinder(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  /**
   * Search the comments in the given compilation unit for to-do comments and report an error for
   * each.
   * 
   * @param unit the compilation unit containing the to-do comments
   */
  public void findIn(CompilationUnit unit) {
    gatherTodoComments(unit.getBeginToken());
  }

  /**
   * Search the comment tokens reachable from the given token and create errors for each to-do
   * comment.
   * 
   * @param token the head of the list of tokens being searched
   */
  private void gatherTodoComments(Token token) {
    while (token != null && token.getType() != TokenType.EOF) {
      Token commentToken = token.getPrecedingComments();
      while (commentToken != null) {
        if (commentToken.getType() == TokenType.SINGLE_LINE_COMMENT
            || commentToken.getType() == TokenType.MULTI_LINE_COMMENT) {
          scrapeTodoComment(commentToken);
        }
        commentToken = commentToken.getNext();
      }
      token = token.getNext();
    }
  }

  /**
   * Look for user defined tasks in comments and convert them into info level analysis issues.
   * 
   * @param commentToken the comment token to analyze
   */
  private void scrapeTodoComment(Token commentToken) {
    Matcher matcher = TodoCode.TODO_REGEX.matcher(commentToken.getLexeme());
    if (matcher.find()) {
      int offset = commentToken.getOffset() + matcher.start() + matcher.group(1).length();
      int length = matcher.group(2).length();
      errorReporter.reportErrorForOffset(TodoCode.TODO, offset, length, matcher.group(2));
    }
  }
}
