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
package com.google.dart.tools.core.internal.problem;

import com.google.dart.compiler.ErrorCode;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.compiler.Util;
import com.google.dart.tools.core.internal.util.Messages;

/**
 * Instances of the class <code>DefaultProblem</code>
 */
public class DefaultProblem extends CategorizedProblem {
  private char[] fileName;
  private ErrorCode id;
  private int startPosition;
  private int endPosition;
  private int line;
  public int column;
  private int severity;
  private String[] arguments;
  private String message;

  // cannot directly point to IJavaModelMarker constants from within batch compiler
//  public static final String MARKER_TYPE_TASK = "com.google.dart.core.task"; //$NON-NLS-1$

  public static final Object[] EMPTY_VALUES = {};

  public DefaultProblem(char[] originatingFileName, String message, ErrorCode id,
      String[] stringArguments, int severity, int startPosition, int endPosition, int line,
      int column) {

    this.fileName = originatingFileName;
    this.message = message;
    this.id = id;
    this.arguments = stringArguments;
    this.severity = severity;
    this.startPosition = startPosition;
    this.endPosition = endPosition;
    this.line = line;
    this.column = column;
  }

  public String errorReportSource(char[] unitSource) {
    //extra from the source the inaccurate     token
    //and "highlight" it using some underneath ^^^^^
    //put some context around too.

    //this code assumes that the font used in the console is fixed size

    //sanity .....
    if ((this.startPosition > this.endPosition)
        || ((this.startPosition < 0) && (this.endPosition < 0)) || unitSource.length == 0) {
      return Messages.problem_noSourceInformation;
    }

    StringBuffer errorBuffer = new StringBuffer();
    errorBuffer.append(' ').append(
        Messages.bind(Messages.problem_atLine, String.valueOf(this.line)));
    errorBuffer.append(Util.LINE_SEPARATOR);
    errorBuffer.append('\t');

    char c;
    final char SPACE = '\u0020';
    final char MARK = '^';
    final char TAB = '\t';
    //the next code tries to underline the token.....
    //it assumes (for a good display) that token source does not
    //contain any \r \n. This is false on statements !
    //(the code still works but the display is not optimal !)

    // expand to line limits
    int length = unitSource.length, begin, end;
    for (begin = this.startPosition >= length ? length - 1 : this.startPosition; begin > 0; begin--) {
      if ((c = unitSource[begin - 1]) == '\n' || c == '\r') {
        break;
      }
    }
    for (end = this.endPosition >= length ? length - 1 : this.endPosition; end + 1 < length; end++) {
      if ((c = unitSource[end + 1]) == '\r' || c == '\n') {
        break;
      }
    }

    // trim left and right spaces/tabs
    while ((c = unitSource[begin]) == ' ' || c == '\t') {
      begin++;
      //while ((c = unitSource[end]) == ' ' || c == '\t') end--; TODO (philippe) should also trim right, but all tests are to be updated
    }

    // copy source
    errorBuffer.append(unitSource, begin, end - begin + 1);
    errorBuffer.append(Util.LINE_SEPARATOR).append("\t"); //$NON-NLS-1$

    // compute underline
    for (int i = begin; i < this.startPosition; i++) {
      errorBuffer.append((unitSource[i] == TAB) ? TAB : SPACE);
    }
    for (int i = this.startPosition; i <= (this.endPosition >= length ? length - 1
        : this.endPosition); i++) {
      errorBuffer.append(MARK);
    }
    return errorBuffer.toString();
  }

  /**
   * Answer back the original arguments recorded into the problem.
   * 
   * @return java.lang.String[]
   */
  @Override
  public String[] getArguments() {
    return this.arguments;
  }

  @Override
  public int getCategoryID() {
//  return ProblemReporter.getProblemCategory(this.severity, this.id);
    DartCore.notYetImplemented();
    return CategorizedProblem.CAT_SYNTAX;
  }

  @Override
  public ErrorCode getID() {
    return this.id;
  }

  /**
   * Answers a readable name for the category which this problem belongs to, or null if none could
   * be found. FOR TESTING PURPOSE
   * 
   * @return java.lang.String
   */
  public String getInternalCategoryMessage() {
    switch (getCategoryID()) {
      case CAT_UNSPECIFIED:
        return "unspecified"; //$NON-NLS-1$
      case CAT_BUILDPATH:
        return "buildpath"; //$NON-NLS-1$
      case CAT_SYNTAX:
        return "syntax"; //$NON-NLS-1$
      case CAT_IMPORT:
        return "import"; //$NON-NLS-1$
      case CAT_TYPE:
        return "type"; //$NON-NLS-1$
      case CAT_MEMBER:
        return "member"; //$NON-NLS-1$
      case CAT_INTERNAL:
        return "internal"; //$NON-NLS-1$
      case CAT_JAVADOC:
        return "javadoc"; //$NON-NLS-1$
      case CAT_CODE_STYLE:
        return "code style"; //$NON-NLS-1$
      case CAT_POTENTIAL_PROGRAMMING_PROBLEM:
        return "potential programming problem"; //$NON-NLS-1$
      case CAT_NAME_SHADOWING_CONFLICT:
        return "name shadowing conflict"; //$NON-NLS-1$
      case CAT_DEPRECATION:
        return "deprecation"; //$NON-NLS-1$
      case CAT_UNNECESSARY_CODE:
        return "unnecessary code"; //$NON-NLS-1$
      case CAT_UNCHECKED_RAW:
        return "unchecked/raw"; //$NON-NLS-1$
      case CAT_NLS:
        return "nls"; //$NON-NLS-1$
      case CAT_RESTRICTION:
        return "restriction"; //$NON-NLS-1$
    }
    return null;
  }

  /**
   * Returns the marker type associated to this problem.
   * 
   * @see org.eclipse.jdt.core.compiler.CategorizedProblem#getMarkerType()
   */
  @Override
  public String getMarkerType() {
//  return this.id == Problem.Task
//    ? MARKER_TYPE_TASK
//    : MARKER_TYPE_PROBLEM;
    DartCore.notYetImplemented();
    return DartCore.DART_PROBLEM_MARKER_TYPE;
  }

  /**
   * Answer a localized, human-readable message string which describes the problem.
   * 
   * @return java.lang.String
   */
  @Override
  public String getMessage() {
    return message;
  }

  /**
   * Answer the file name in which the problem was found.
   * 
   * @return char[]
   */
  @Override
  public char[] getOriginatingFileName() {
    return fileName;
  }

  /**
   * Answer the line number in source where the problem begins.
   * 
   * @return int
   */
  public int getSourceColumnNumber() {
    return column;
  }

  /**
   * Answer the end position of the problem (inclusive), or -1 if unknown.
   * 
   * @return int
   */
  @Override
  public int getSourceEnd() {
    return endPosition;
  }

  /**
   * Answer the line number in source where the problem begins.
   * 
   * @return int
   */
  @Override
  public int getSourceLineNumber() {
    return line;
  }

  /**
   * Answer the start position of the problem (inclusive), or -1 if unknown.
   * 
   * @return int
   */
  @Override
  public int getSourceStart() {
    return startPosition;
  }

  @Override
  public boolean isError() {
    return (severity & ProblemSeverities.Error) != 0;
  }

  @Override
  public boolean isInfo() {
    return (severity & ProblemSeverities.Info) != 0;
  }

  @Override
  public boolean isWarning() {
    return (severity & ProblemSeverities.Warning) != 0;
  }

  public void setOriginatingFileName(char[] fileName) {
    this.fileName = fileName;
  }

  /**
   * Set the end position of the problem (inclusive), or -1 if unknown. Used for shifting problem
   * positions.
   * 
   * @param sourceEnd the new value of the sourceEnd of the receiver
   */
  @Override
  public void setSourceEnd(int sourceEnd) {
    endPosition = sourceEnd;
  }

  /**
   * Set the line number in source where the problem begins.
   * 
   * @param lineNumber the new value of the line number of the receiver
   */
  @Override
  public void setSourceLineNumber(int lineNumber) {

    line = lineNumber;
  }

  /**
   * Set the start position of the problem (inclusive), or -1 if unknown. Used for shifting problem
   * positions.
   * 
   * @param sourceStart the new value of the source start position of the receiver
   */
  @Override
  public void setSourceStart(int sourceStart) {
    startPosition = sourceStart;
  }

  @Override
  public String toString() {
    String s = "Pb(" + (id /* & Problem.IgnoreCategoriesMask */) + ") "; //$NON-NLS-1$ //$NON-NLS-2$
    if (message != null) {
      s += message;
    } else {
      if (arguments != null) {
        for (int i = 0; i < arguments.length; i++) {
          s += " " + arguments[i]; //$NON-NLS-1$
        }
      }
    }
    return s;
  }
}
