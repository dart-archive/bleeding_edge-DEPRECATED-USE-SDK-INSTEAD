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

import com.google.dart.engine.formatter.CodeFormatter.Kind;
import com.google.dart.engine.internal.formatter.CodeFormatterImpl;

/**
 * Helper used to run the {@link CodeFormatter}.
 */
public class CodeFormatterRunner {

  private class TestEditRecorder extends EditRecorder<String> {

    StringBuilder builder = new StringBuilder();

    @Override
    public String buildEdit() {
      return builder.toString();
    }
  }

  private static final String NEW_LINE = System.getProperty("line.separator");
  private static final int DEFAULT_INDENTATION_LEVEL = 0;

  public static CodeFormatterRunner getDefault() {
    return new CodeFormatterRunner(getDefaultFormatter());
  }

  private static CodeFormatter getDefaultFormatter() {
    CodeFormatterOptions preferences = getDefaultFormatterOptions();
    CodeFormatter codeFormatter = new CodeFormatterImpl(preferences);
    return codeFormatter;
  }

  private static CodeFormatterOptions getDefaultFormatterOptions() {
    CodeFormatterOptions preferences = new CodeFormatterOptions();
    return preferences;
  }

  private final CodeFormatter codeFormatter;

  public CodeFormatterRunner(CodeFormatter formatter) {
    this.codeFormatter = formatter;
  }

  public String format(String source, Kind kind) {
    return format(source, kind, DEFAULT_INDENTATION_LEVEL);
  }

  public String format(String source, Kind kind, int indentationLevel) {
    return format(source, kind, indentationLevel, 0, source.length());
  }

  public String format(String source, Kind kind, int indentationLevel, int offset, int length) {
    return format(source, kind, indentationLevel, offset, length, NEW_LINE);
  }

  public String format(String source, Kind kind, int indentationLevel, int offset, int length,
      String lineSeparator) {
    TestEditRecorder recorder = new TestEditRecorder();
    codeFormatter.format(kind, source, offset, length, indentationLevel, recorder);
    return recorder.buildEdit();
  }

}
