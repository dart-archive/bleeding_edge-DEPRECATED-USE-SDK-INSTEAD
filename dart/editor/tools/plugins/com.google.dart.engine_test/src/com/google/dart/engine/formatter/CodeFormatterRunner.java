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
import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.formatter.edit.EditBuilder;
import com.google.dart.engine.formatter.edit.EditOperation;
import com.google.dart.engine.formatter.edit.EditRecorder;
import com.google.dart.engine.formatter.edit.EditStore;
import com.google.dart.engine.internal.formatter.AbstractCodeFormatter;
import com.google.dart.engine.internal.formatter.BasicEditStore;

/**
 * Helper used to run the {@link CodeFormatter}.
 */
public class CodeFormatterRunner {

  private static class DefaultScanner implements Scanner {

  }
  private static class StringEditBuilder implements EditBuilder<String, String> {

    @Override
    public EditOperation<String, String> buildEdit(Iterable<Edit> edits) {
      return new StringEditOperation(edits);
    }

  }

  private static class StringEditOperation implements EditOperation<String, String> {

    private final Iterable<Edit> edits;

    StringEditOperation(Iterable<Edit> edits) {
      this.edits = edits;
    }

    @Override
    public String applyTo(String document) {

      StringBuilder builder = new StringBuilder(document);

      for (Edit edit : edits) {
        builder.replace(edit.offset, edit.offset + edit.length, edit.replacement);
      }

      return builder.toString();
    }

  }

  private static class StringEditRecorder extends EditRecorder<String, String> {

    StringEditRecorder(CodeFormatterOptions options, Scanner scanner, EditStore editStore) {
      super(getDefaultFormatterOptions(), scanner, editStore);
    }

  }

  private static class StringEditStore extends BasicEditStore {

  }

  private static final String NEW_LINE = System.getProperty("line.separator");

  private static final int DEFAULT_INDENTATION_LEVEL = 0;

  public static CodeFormatterRunner getDefault() {
    return new CodeFormatterRunner(getDefaultFormatter());
  }

  private static EditBuilder<String, String> getDefaultBuilder() {
    return new StringEditBuilder();
  }

  private static CodeFormatter<String, String> getDefaultFormatter() {
    CodeFormatterOptions preferences = getDefaultFormatterOptions();
    EditBuilder<String, String> builder = getDefaultBuilder();
    EditRecorder<String, String> recorder = getDefaultRecorder();
    CodeFormatter<String, String> codeFormatter = new AbstractCodeFormatter<String, String>(
        preferences,
        recorder,
        builder);
    return codeFormatter;
  }

  private static CodeFormatterOptions getDefaultFormatterOptions() {
    CodeFormatterOptions preferences = new CodeFormatterOptions();
    return preferences;
  }

  private static EditRecorder<String, String> getDefaultRecorder() {
    return new StringEditRecorder(
        getDefaultFormatterOptions(),
        getDefaultScanner(),
        getDefaultStore());
  }

  private static Scanner getDefaultScanner() {
    return new DefaultScanner();
  }

  private static StringEditStore getDefaultStore() {
    return new StringEditStore();
  }

  private final CodeFormatter<String, String> codeFormatter;

  public CodeFormatterRunner(CodeFormatter<String, String> formatter) {
    this.codeFormatter = formatter;
  }

  public String format(String source, Kind kind) throws FormatterException {
    return format(source, kind, DEFAULT_INDENTATION_LEVEL);
  }

  public String format(String source, Kind kind, int indentationLevel) throws FormatterException {
    return format(source, kind, indentationLevel, 0, source.length());
  }

  public String format(String source, Kind kind, int indentationLevel, int offset, int length)
      throws FormatterException {
    return format(source, kind, indentationLevel, offset, length, NEW_LINE);
  }

  public String format(String source, Kind kind, int indentationLevel, int offset, int length,
      String lineSeparator) throws FormatterException {
    EditOperation<String, String> operation = codeFormatter.format(
        kind,
        source,
        offset,
        length,
        indentationLevel);
    return operation.applyTo(source);
  }
}
