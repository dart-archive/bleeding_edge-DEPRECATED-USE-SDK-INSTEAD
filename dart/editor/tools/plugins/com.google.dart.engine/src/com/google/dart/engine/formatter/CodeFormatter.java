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
package com.google.dart.engine.formatter;

/**
 * Base source code formatter.
 */
public abstract class CodeFormatter {

  /**
   * Specifies the kind of code snippet to format.
   */
  public static enum Kind {
    /**
     * Kind used to format a compilation unit.
     */
    COMPILATION_UNIT,
    /**
     * Unknown kind.
     */
    UNKNOWN;
  }

  /**
   * Format the given source string, describing edits to an {@link EditRecorder} callback.
   * 
   * @param kind the kind of the code snippet to format.
   * @param source the source to format.
   * @param offset the offset at which to start recording the edits. Must not be less than zero.
   * @param length the length at which to stop recording the edits. Must not be less than zero and
   *          must not exceed the length of the source string.
   * @param indentationLevel the initial indentation level, used to shift left/right the entire
   *          source fragment. An initial indentation level of zero or below has no effect.
   * @param recorder a callback used to collect edits that describe the changes required to apply
   *          formatting to the given source. Must not be {@code null}.
   * @throws IllegalArgumentException if recorder is null, offset or length are less than 0 or
   *           length is greater than source length.
   */
  public abstract void format(Kind kind, String source, int offset, int length,
      int indentationLevel, EditRecorder<?> recorder);

}
