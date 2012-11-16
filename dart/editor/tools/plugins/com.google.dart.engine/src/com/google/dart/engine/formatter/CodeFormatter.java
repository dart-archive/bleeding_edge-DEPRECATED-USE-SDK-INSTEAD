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

import com.google.dart.engine.formatter.edit.EditOperation;
import com.google.dart.engine.formatter.edit.EditRecorder;

/**
 * Base source code formatter.
 * 
 * @param <D> the document type
 * @param <R> an (optional) return result type
 */
public abstract class CodeFormatter<D, R> {

  /**
   * Specifies the kind of code snippet to format.
   */
  public static enum Kind {
    /**
     * Kind used to format a compilation unit.
     */
    COMPILATION_UNIT;
  }

  /**
   * Format the given source string, describing edits to an {@link EditRecorder} callback.
   * 
   * @param kind the kind of the code snippet to format.
   * @param source the source to format.
   * @param offset the offset at which to start recording the edits. Must not be less than zero.
   * @param end the end offset at which to stop recording the edits. Must not be less than zero and
   *          must not exceed the length of the source string.
   * @param indentationLevel the initial indentation level, used to shift left/right the entire
   *          source fragment. An initial indentation level of zero or below has no effect.
   * @return an edit operation that applies the edit
   * @throws IllegalArgumentException if recorder is null, offset or length are less than 0 or
   *           length is greater than source length.
   * @throws FormatterException if an error occurs during formatting
   */
  public abstract EditOperation<D, R> format(Kind kind, String source, int offset, int end,
      int indentationLevel) throws FormatterException;

}
