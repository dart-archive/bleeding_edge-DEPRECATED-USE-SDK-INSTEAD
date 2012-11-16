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
 * Formatter options.
 */
public class CodeFormatterOptions {

  /**
   * OS line separator.
   */
  public static final String OS_LINE_SEPARATOR = System.getProperty("line.separator"); //$NON-NLS-1$

  public static CodeFormatterOptions getDefaults() {
    CodeFormatterOptions options = new CodeFormatterOptions();
    applyDefaults(options);
    return options;
  }

  /**
   * Apply formatter defaults.
   * <p>
   * Where defined, defaults are derived from the <a
   * href="http://www.dartlang.org/articles/style-guide/">style guide</a>.
   * 
   * @param options the options to update
   */
  private static void applyDefaults(CodeFormatterOptions options) {
    options.line_separator = OS_LINE_SEPARATOR;
    options.initial_indentation_level = 0;
    options.indent_per_level = 2;
    options.tab_size = 2;
    options.page_width = 80;
  }

  public String line_separator;

  public int initial_indentation_level;
  public int indent_per_level;
  public int tab_size;
  public int page_width;

}
