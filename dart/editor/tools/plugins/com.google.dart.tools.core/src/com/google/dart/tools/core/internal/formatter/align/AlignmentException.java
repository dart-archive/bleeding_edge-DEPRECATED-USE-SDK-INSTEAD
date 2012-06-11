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
package com.google.dart.tools.core.internal.formatter.align;

/**
 * Instances of the class <code>AlignmentException</code> implement an exception used to backtrack
 * and break available alignments When the exception is thrown, it is assumed that some alignment
 * will be changed.
 */
public class AlignmentException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  public static final int LINE_TOO_LONG = 1;
  public static final int ALIGN_TOO_SMALL = 2;

  int reason;
  int value;
  public int relativeDepth;

  public AlignmentException(int reason, int relativeDepth) {
    this(reason, 0, relativeDepth);
  }

  public AlignmentException(int reason, int value, int relativeDepth) {
    this.reason = reason;
    this.value = value;
    this.relativeDepth = relativeDepth;
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer(10);
    switch (this.reason) {
      case LINE_TOO_LONG:
        buffer.append("LINE_TOO_LONG"); //$NON-NLS-1$
        break;
      case ALIGN_TOO_SMALL:
        buffer.append("ALIGN_TOO_SMALL"); //$NON-NLS-1$
        break;
    }
    buffer.append("<relativeDepth: ") //$NON-NLS-1$
    .append(this.relativeDepth).append(">\n"); //$NON-NLS-1$
    return buffer.toString();
  }
}
