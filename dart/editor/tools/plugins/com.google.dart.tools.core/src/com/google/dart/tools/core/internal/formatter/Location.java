/*
 * Copyright (c) 2011, the Dart project authors.
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

import com.google.dart.compiler.parser.DartScanner;

/**
 * Instances of the class <code>Location</code> maintain positional information both in original
 * source and in the output source. It remembers source offsets, line/column and indentation level.
 */
public class Location {
  public int inputOffset;
  /** deprecated */
  public int inputColumn;
  public int outputLine;
  public int outputColumn;
  public int outputIndentationLevel;
  public boolean needSpace;
  public boolean pendingSpace;
  public int nlsTagCounter;
  public int lastLocalDeclarationSourceStart;
  public int numberOfIndentations;
  DartScanner.State scannerState;

  // chunk management
  public int lastNumberOfNewLines;

  // edits management
  int editsIndex;
  OptimizedReplaceEdit textEdit;

  public Location(Scribe scribe, int sourceRestart) {
    update(scribe, sourceRestart);
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("output (column=" + outputColumn); //$NON-NLS-1$
    buffer.append(", line=" + outputLine); //$NON-NLS-1$
    buffer.append(", indentation level=" + outputIndentationLevel); //$NON-NLS-1$
    buffer.append(") input (offset=" + inputOffset); //$NON-NLS-1$
    buffer.append(", column=" + inputColumn); //$NON-NLS-1$
    buffer.append(')');
    return buffer.toString();
  }

  public void update(Scribe scribe, int sourceRestart) {
    outputColumn = scribe.column;
    outputLine = scribe.line;
    inputOffset = sourceRestart;
    inputColumn = scribe.getCurrentIndentation(sourceRestart) + 1;
    outputIndentationLevel = scribe.indentationLevel;
    lastNumberOfNewLines = scribe.lastNumberOfNewLines;
    needSpace = scribe.needSpace;
    pendingSpace = scribe.pendingSpace;
    editsIndex = scribe.editsIndex;
    nlsTagCounter = scribe.nlsTagCounter;
    numberOfIndentations = scribe.numberOfIndentations;
    textEdit = scribe.getLastEdit();
    scannerState = scribe.scanner.getState();
  }
}
